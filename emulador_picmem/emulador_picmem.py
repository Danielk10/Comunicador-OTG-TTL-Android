#!/usr/bin/env python3
"""
Emulador del Protocolo PICMEM v3 (PIC16F628A) para Comunicador-OTG-TTL-Android.
Simula el comportamiento de hardware del firmware PICMEM v3 sobre un pseudo-terminal (PTY)
para pruebas de integración automatizadas y verificación de protocolos I2C y SPI.
"""

import os
import pty
import select
import sys
import time
import termios

RESP_OK = 0x4B   # 'K'
RESP_ERR = 0x58  # 'X'
RESP_END = 0x55  # 'U'

def read_exact(fd, n, timeout=3.0):
    buf = bytearray()
    deadline = time.time() + timeout
    while len(buf) < n:
        remaining = deadline - time.time()
        if remaining <= 0:
            raise TimeoutError(f"Timeout esperando {n} bytes (leídos {len(buf)})")
        r, _, _ = select.select([fd], [], [], remaining)
        if not r:
            raise TimeoutError(f"Timeout esperando datos en el puerto virtual")
        chunk = os.read(fd, n - len(buf))
        if not chunk:
            raise ConnectionError("Puerto cerrado por el cliente")
        buf.extend(chunk)
    return bytes(buf)

def run_emulator():
    base_dir = os.path.dirname(os.path.abspath(__file__))
    symlink_path = os.path.join(base_dir, "vtty")

    # Memorias virtuales en RAM:
    # 1. EEPROM I2C (32 KB, equivalente a 24C256)
    i2c_mem = bytearray(b"\xFF" * (32 * 1024))
    # Pre-cargar algunos datos de prueba en 0x0000
    test_str = b"PICMEM_v3_I2C_VIRTUAL_TEST_DATA_2026"
    i2c_mem[0:len(test_str)] = test_str

    # 2. Flash SPI (2 MB, equivalente a W25Q16)
    spi_mem = bytearray(b"\xFF" * (2 * 1024 * 1024))
    spi_str = b"PICMEM_v3_SPI_FLASH_W25Q16_DUMP_TEST"
    spi_mem[0:len(spi_str)] = spi_str

    # Abrir par de pseudo-terminales
    master_fd, slave_fd = pty.openpty()
    slave_name = os.ttyname(slave_fd)

    # Configurar modo RAW (sin echo, sin traducción CR/LF)
    attrs = termios.tcgetattr(master_fd)
    attrs[0] = 0 # iflag
    attrs[1] = 0 # oflag
    attrs[2] = termios.CS8 | termios.CREAD | termios.CLOCAL # cflag
    attrs[3] = 0 # lflag
    termios.tcsetattr(master_fd, termios.TCSANOW, attrs)

    # Crear o actualizar symlink vtty
    if os.path.islink(symlink_path) or os.path.exists(symlink_path):
        try:
            os.remove(symlink_path)
        except OSError:
            pass
    os.symlink(slave_name, symlink_path)

    print("==========================================================")
    print("      EMULADOR DE FIRMWARE PICMEM v3 (PIC16F628A)")
    print("==========================================================")
    print(f"[EMU] Puerto PTY activo: {slave_name}")
    print(f"[EMU] Enlace simbólico: {symlink_path}")
    print("[EMU] Esperando comandos desde la app o cliente serial...")
    sys.stdout.flush()

    try:
        while True:
            r, _, _ = select.select([master_fd], [], [], 0.5)
            if not r:
                continue

            try:
                cmd_byte = os.read(master_fd, 1)
            except OSError:
                break

            if not cmd_byte:
                continue

            b = cmd_byte[0]

            # ── 1. PING ('?' = 0x3F) ──────────────────────────────────────────
            if b == 0x3F:
                response = b"PICMEM v3 OK\r\n"
                os.write(master_fd, response)
                print(f"[EMU] PING recibido -> Enviado: {response.decode(errors='ignore').strip()}")
                sys.stdout.flush()
                continue

            # ── 2. COMANDOS I2C ('I' = 0x49) ──────────────────────────────────
            elif b == 0x49:
                sub = read_exact(master_fd, 1)[0]

                # I2C Scan ('S' = 0x53)
                if sub == 0x53:
                    # Direcciones detectadas: 0x50 (EEPROM) y 0x68 (RTC/Mock), fin 0xFF
                    resp = bytes([0x50, 0x68, 0xFF])
                    os.write(master_fd, resp)
                    print("[EMU] I2C Scan -> Detectados 0x50, 0x68, FF")
                    sys.stdout.flush()

                # I2C Read ('R' = 0x52)
                elif sub == 0x52:
                    # Formato: <addr_len> <chip_addr> <A1> <A0> <LH> <LL>
                    params = read_exact(master_fd, 6)
                    addr_len, chip_addr, a1, a0, lh, ll = params
                    address = (a1 << 8) | a0
                    length = (lh << 8) | ll
                    if length == 0:
                        length = 65536

                    end_addr = min(address + length, len(i2c_mem))
                    data = i2c_mem[address:end_addr]
                    # Responder datos + token fin RESP_END (0x55)
                    os.write(master_fd, data + bytes([RESP_END]))
                    print(f"[EMU] I2C Read: addr=0x{address:04X}, len={length} -> enviados {len(data)}B + 0x55")
                    sys.stdout.flush()

                # I2C Write ('W' = 0x57)
                elif sub == 0x57:
                    # Formato: <addr_len> <chip_addr> <A1> <A0> <LH> <LL> [datos...]
                    params = read_exact(master_fd, 6)
                    addr_len, chip_addr, a1, a0, lh, ll = params
                    address = (a1 << 8) | a0
                    length = (lh << 8) | ll
                    data = read_exact(master_fd, length)

                    end_addr = min(address + length, len(i2c_mem))
                    i2c_mem[address:end_addr] = data[:end_addr - address]
                    os.write(master_fd, bytes([RESP_OK]))
                    print(f"[EMU] I2C Write: addr=0x{address:04X}, len={length} -> RESP_OK (0x4B)")
                    sys.stdout.flush()

                # I2C Full Dump ('F' = 0x46)
                elif sub == 0x46:
                    # Formato: <addr_len> <chip_addr> <LH> <LL>
                    params = read_exact(master_fd, 4)
                    addr_len, chip_addr, lh, ll = params
                    length = (lh << 8) | ll
                    if length == 0:
                        length = len(i2c_mem)

                    data = i2c_mem[:min(length, len(i2c_mem))]
                    os.write(master_fd, data + bytes([RESP_END]))
                    print(f"[EMU] I2C Full Dump: len={length} -> enviados {len(data)}B + 0x55")
                    sys.stdout.flush()

            # ── 3. COMANDOS SPI ('P' = 0x50) ──────────────────────────────────
            elif b == 0x50:
                sub = read_exact(master_fd, 1)[0]

                # JEDEC ID ('J' = 0x4A)
                if sub == 0x4A:
                    # Winbond W25Q16 (0xEF, 0x40, 0x15)
                    resp = bytes([0xEF, 0x40, 0x15])
                    os.write(master_fd, resp)
                    print("[EMU] SPI JEDEC ID -> Winbond W25Q16 (EF 40 15)")
                    sys.stdout.flush()

                # Status Register ('S' = 0x53)
                elif sub == 0x53:
                    os.write(master_fd, bytes([0x00]))
                    print("[EMU] SPI Status -> 0x00")
                    sys.stdout.flush()

                # SPI Read ('R' = 0x52)
                elif sub == 0x52:
                    # Formato: <addr_len> <opcode> <A2> <A1> <A0> <LH> <LL>
                    params = read_exact(master_fd, 7)
                    addr_len, opcode, a2, a1, a0, lh, ll = params
                    address = (a2 << 16) | (a1 << 8) | a0
                    length = (lh << 8) | ll
                    if length == 0:
                        length = 65536

                    end_addr = min(address + length, len(spi_mem))
                    data = spi_mem[address:end_addr]
                    os.write(master_fd, data + bytes([RESP_END]))
                    print(f"[EMU] SPI Read: addr=0x{address:06X}, len={length} -> enviados {len(data)}B + 0x55")
                    sys.stdout.flush()

                # SPI Write ('W' = 0x57)
                elif sub == 0x57:
                    # Formato: <addr_len> <opcode> <A2> <A1> <A0> <LH> <LL> [datos...]
                    params = read_exact(master_fd, 7)
                    addr_len, opcode, a2, a1, a0, lh, ll = params
                    address = (a2 << 16) | (a1 << 8) | a0
                    length = (lh << 8) | ll
                    data = read_exact(master_fd, length)

                    end_addr = min(address + length, len(spi_mem))
                    spi_mem[address:end_addr] = data[:end_addr - address]
                    os.write(master_fd, bytes([RESP_OK]))
                    print(f"[EMU] SPI Write: addr=0x{address:06X}, len={length} -> RESP_OK (0x4B)")
                    sys.stdout.flush()

                # SPI Chip Erase ('E' = 0x45)
                elif sub == 0x45:
                    for i in range(len(spi_mem)):
                        spi_mem[i] = 0xFF
                    os.write(master_fd, bytes([RESP_OK]))
                    print("[EMU] SPI Chip Erase -> Memoria limpiada a 0xFF, RESP_OK (0x4B)")
                    sys.stdout.flush()

                # SPI Full Dump ('F' = 0x46)
                elif sub == 0x46:
                    # Enviar 64KB iniciales de prueba para validar flujo + 0x55
                    chunk_sz = 65536
                    data = spi_mem[:chunk_sz]
                    os.write(master_fd, data + bytes([RESP_END]))
                    print(f"[EMU] SPI Full Dump -> enviados {len(data)}B + 0x55")
                    sys.stdout.flush()

    except KeyboardInterrupt:
        print("\n[EMU] Cerrando emulador...")
    finally:
        os.close(master_fd)
        os.close(slave_fd)
        if os.path.islink(symlink_path):
            os.remove(symlink_path)
        print("[EMU] Emulador detenido correctamente.")

if __name__ == '__main__':
    run_emulator()

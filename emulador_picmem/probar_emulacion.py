#!/usr/bin/env python3
"""
Script de pruebas automatizadas contra el emulador virtual PICMEM v3.
Valida el flujo completo de:
1. Ping
2. I2C Scan
3. I2C Write
4. I2C Read y verificación de integridad
5. SPI JEDEC ID
6. SPI Status
7. SPI Write
8. SPI Read y verificación
9. SPI Chip Erase
"""

import os
import sys
import time

def read_exact(fd, n, timeout=3.0):
    buf = bytearray()
    deadline = time.time() + timeout
    while len(buf) < n:
        if time.time() > deadline:
            raise TimeoutError(f"Timeout leyendo {n} bytes (leídos {len(buf)})")
        try:
            chunk = os.read(fd, n - len(buf))
            if chunk:
                buf.extend(chunk)
            else:
                time.sleep(0.01)
        except BlockingIOError:
            time.sleep(0.01)
    return bytes(buf)

def main():
    base_dir = os.path.dirname(os.path.abspath(__file__))
    vtty_path = os.path.join(base_dir, "vtty")

    if not os.path.exists(vtty_path):
        print(f"[ERROR] No se encontró el puerto virtual {vtty_path}")
        sys.exit(1)

    real_tty = os.path.realpath(vtty_path)
    print(f"[TEST] Conectando a {real_tty}...")

    fd = os.open(real_tty, os.O_RDWR | os.O_NOCTTY)

    try:
        # 1. Test Ping
        print("\n--- 1. Probando PING (0x3F) ---")
        os.write(fd, b"\x3F")
        resp = read_exact(fd, 14, timeout=2.0)
        print(f"Respuesta recibida: {resp}")
        assert b"PICMEM v3 OK" in resp, f"Respuesta inesperada: {resp}"
        print("✓ PING OK!")

        # 2. Test I2C Scan
        print("\n--- 2. Probando I2C Scan ('IS') ---")
        os.write(fd, b"IS")
        scan_resp = read_exact(fd, 3, timeout=2.0)
        print(f"I2C Scan bytes: {scan_resp.hex(' ')}")
        assert scan_resp[0] == 0x50, "Dirección 0x50 no detectada"
        assert scan_resp[1] == 0x68, "Dirección 0x68 no detectada"
        assert scan_resp[2] == 0xFF, "Token fin 0xFF no recibido"
        print("✓ I2C Scan OK!")

        # 3. Test I2C Write
        print("\n--- 3. Probando I2C Write ('IW') ---")
        test_payload = b"HOLA_MUNDO_I2C_TEST_PAYLOAD"
        addr = 0x0100
        length = len(test_payload)
        # Formato: 'I' 'W' <addr_len=2> <chip_addr=0xA0> <A1> <A0> <LH> <LL> [datos]
        cmd = bytes([0x49, 0x57, 0x02, 0xA0, (addr >> 8) & 0xFF, addr & 0xFF, (length >> 8) & 0xFF, length & 0xFF]) + test_payload
        os.write(fd, cmd)
        ack = read_exact(fd, 1, timeout=2.0)
        print(f"Respuesta Write ACK: {ack.hex()} ('{chr(ack[0])}')")
        assert ack == b"\x4B", f"Se esperaba ACK 0x4B ('K'), recibido {ack}"
        print("✓ I2C Write OK!")

        # 4. Test I2C Read
        print("\n--- 4. Probando I2C Read ('IR') y verificación ---")
        # Formato: 'I' 'R' <addr_len=2> <chip_addr=0xA0> <A1> <A0> <LH> <LL>
        cmd = bytes([0x49, 0x52, 0x02, 0xA0, (addr >> 8) & 0xFF, addr & 0xFF, (length >> 8) & 0xFF, length & 0xFF])
        os.write(fd, cmd)
        read_data = read_exact(fd, length, timeout=2.0)
        end_token = read_exact(fd, 1, timeout=2.0)
        print(f"Datos leídos: {read_data}")
        print(f"Token fin: {end_token.hex()} ('{chr(end_token[0])}')")
        assert read_data == test_payload, f"Datos leídos no coinciden: {read_data} != {test_payload}"
        assert end_token == b"\x55", f"Se esperaba token RESP_END 0x55, recibido {end_token}"
        print("✓ I2C Read y verificación OK!")

        # 5. Test SPI JEDEC ID
        print("\n--- 5. Probando SPI JEDEC ID ('PJ') ---")
        os.write(fd, b"PJ")
        jedec = read_exact(fd, 3, timeout=2.0)
        print(f"JEDEC ID recibido: {jedec.hex(' ')}")
        assert jedec == bytes([0xEF, 0x40, 0x15]), f"JEDEC ID inesperado: {jedec.hex()}"
        print("✓ SPI JEDEC ID OK (Winbond W25Q16 detectado)!")

        # 6. Test SPI Status
        print("\n--- 6. Probando SPI Status ('PS') ---")
        os.write(fd, b"PS")
        st = read_exact(fd, 1, timeout=2.0)
        print(f"Status byte: 0x{st[0]:02X}")
        assert st == b"\x00", "Status inesperado"
        print("✓ SPI Status OK!")

        # 7. Test SPI Write
        print("\n--- 7. Probando SPI Write ('PW') ---")
        spi_payload = b"SPI_FLASH_PAYLOAD_TEST_1234567890"
        spi_addr = 0x001000
        spi_len = len(spi_payload)
        # Formato: 'P' 'W' <addr_len=3> <opcode=0x02> <A2> <A1> <A0> <LH> <LL> [datos]
        cmd = bytes([
            0x50, 0x57, 0x03, 0x02,
            (spi_addr >> 16) & 0xFF, (spi_addr >> 8) & 0xFF, spi_addr & 0xFF,
            (spi_len >> 8) & 0xFF, spi_len & 0xFF
        ]) + spi_payload
        os.write(fd, cmd)
        ack = read_exact(fd, 1, timeout=2.0)
        print(f"Respuesta SPI Write ACK: {ack.hex()} ('{chr(ack[0])}')")
        assert ack == b"\x4B", f"Se esperaba ACK 0x4B, recibido {ack}"
        print("✓ SPI Write OK!")

        # 8. Test SPI Read
        print("\n--- 8. Probando SPI Read ('PR') y verificación ---")
        # Formato: 'P' 'R' <addr_len=3> <opcode=0x03> <A2> <A1> <A0> <LH> <LL>
        cmd = bytes([
            0x50, 0x52, 0x03, 0x03,
            (spi_addr >> 16) & 0xFF, (spi_addr >> 8) & 0xFF, spi_addr & 0xFF,
            (spi_len >> 8) & 0xFF, spi_len & 0xFF
        ])
        os.write(fd, cmd)
        data = read_exact(fd, spi_len, timeout=2.0)
        end_token = read_exact(fd, 1, timeout=2.0)
        print(f"Datos SPI leídos: {data}")
        assert data == spi_payload, f"Datos SPI no coinciden: {data} != {spi_payload}"
        assert end_token == b"\x55", f"Token fin no coincide: {end_token}"
        print("✓ SPI Read y verificación OK!")

        # 9. Test SPI Erase
        print("\n--- 9. Probando SPI Chip Erase ('PE') ---")
        os.write(fd, b"PE")
        ack = read_exact(fd, 1, timeout=2.0)
        assert ack == b"\x4B", f"Se esperaba 0x4B en erase, recibido {ack}"
        # Leer nuevamente para comprobar que ahora es 0xFF
        os.write(fd, cmd)
        data_erased = read_exact(fd, spi_len, timeout=2.0)
        end_token = read_exact(fd, 1, timeout=2.0)
        assert data_erased == bytes([0xFF] * spi_len), f"La memoria no quedó borrada: {data_erased.hex()}"
        print("✓ SPI Chip Erase OK (memoria limpia a 0xFF)!")

        print("\n==========================================================")
        print("  ¡TODAS LAS PRUEBAS DEL EMULADOR PICMEM v3 PASARON CON ÉXITO!")
        print("==========================================================")

    finally:
        os.close(fd)

if __name__ == '__main__':
    main()

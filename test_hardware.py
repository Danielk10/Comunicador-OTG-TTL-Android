import serial
import time
import sys

COM_PORT = 'COM6'
BAUD_RATE = 9600

def log(msg):
    print(f"[TEST] {msg}")

def test_hardware():
    try:
        ser = serial.Serial(COM_PORT, BAUD_RATE, timeout=2.0)
        log(f"Conectado a {COM_PORT} a {BAUD_RATE} baud.")
        time.sleep(2) # Wait for Arduino/PIC reset if any
    except Exception as e:
        log(f"Fallo al abrir puerto: {e}")
        return

    # 1. PING
    log("Enviando Ping (0x3F)...")
    ser.write(b'\x3F')
    
    resp = b''
    start = time.time()
    while time.time() - start < 1.0:
        if ser.in_waiting > 0:
            resp += ser.read(ser.in_waiting)
            if b'PICMEM' in resp:
                break
    log(f"Respuesta Ping: {resp}")

    # 2. I2C Scan
    log("Escaneando I2C (0x49 0x53)...")
    ser.write(b'\x49\x53')
    i2c_addrs = []
    start = time.time()
    while time.time() - start < 2.0:
        b = ser.read(1)
        if len(b) > 0:
            val = b[0]
            if val == 0xFF:
                log(f"Fin de escaneo I2C.")
                break
            elif val != 0x55 and val != 0x58:
                i2c_addrs.append(val)
    log(f"Direcciones I2C encontradas: {[hex(x) for x in i2c_addrs]}")

    # 3. SPI JEDEC
    log("Leyendo JEDEC ID (0x50 0x4A)...")
    ser.write(b'\x50\x4A')
    jedec = ser.read(3)
    log(f"SPI JEDEC ID: {[hex(x) for x in jedec]}")

    # 4. Leer primeros 64 bytes de I2C (asumiendo 24C128, block 0, size=64)
    # Comando READ Normal I2C: 'I' 'R' [LenL] [LenH] [AddrL] [AddrH] (AddrH solo si addr_len=2)
    # Len = 64 (0x40 0x00), Addr = 0 (0x00 0x00) -> 49 52 40 00 00 00
    if len(i2c_addrs) > 0:
        log("Leyendo 64 bytes de I2C...")
        cmd = bytearray([0x49, 0x52, 0x40, 0x00, 0x00, 0x00]) 
        ser.write(cmd)
        data = ser.read(64)
        end_token = ser.read(1)
        log(f"Leídos {len(data)} bytes. Token fin: {end_token}")
        log(f"Datos I2C: {data.hex()}")
    else:
        log("Saltando lectura I2C (no detectado).")

    # 5. Leer primeros 64 bytes de SPI (asumiendo address de 3 bytes)
    # Comando READ Normal SPI: 'P' 'R' [LenL] [LenH] [AddrL] [AddrM] [AddrH]
    # Len = 64 (0x40 0x00), Addr = 0 -> 50 52 40 00 00 00 00
    if len(jedec) == 3 and jedec[0] != 0xFF and jedec[0] != 0:
        log("Leyendo 64 bytes de SPI...")
        cmd = bytearray([0x50, 0x52, 0x40, 0x00, 0x00, 0x00, 0x00])
        ser.write(cmd)
        data = ser.read(64)
        end_token = ser.read(1)
        log(f"Leídos {len(data)} bytes. Token fin: {end_token}")
        log(f"Datos SPI: {data.hex()}")
    else:
        log("Saltando lectura SPI (JEDEC inválido).")

    ser.close()
    log("Puerto cerrado. Pruebas finalizadas.")

if __name__ == '__main__':
    test_hardware()

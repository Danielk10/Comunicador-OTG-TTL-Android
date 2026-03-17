import serial
import time
import sys

PORT = 'COM6'
BAUD = 9600

def robust_read_all(ser, expected_size, timeout_per_kb=1.5):
    """Lee datos de forma continua hasta alcanzar el tamaño o timeout."""
    start_time = time.time()
    data = bytearray()
    last_len = 0
    total_timeout = max(10, expected_size * timeout_per_kb / 1024)
    
    print(f"Leyendo {expected_size} bytes...")
    while len(data) < expected_size:
        if ser.in_waiting > 0:
            bytes_to_read = ser.in_waiting
            data.extend(ser.read(bytes_to_read))
            if len(data) // 1024 > last_len // 1024:
                print(f"Progreso: {len(data)} / {expected_size} bytes", end='\r')
            last_len = len(data)
        
        if (time.time() - start_time) > total_timeout:
            print(f"\nTimeout alcanzado. Recibidos: {len(data)} bytes")
            break
            
        time.sleep(0.001)
    
    print(f"\nLectura finalizada: {len(data)} bytes")
    return data

def run_v2():
    try:
        ser = serial.Serial(PORT, BAUD, timeout=1)
        ser.reset_input_buffer()
        print(f"--- Sistema V2 Conectado a {PORT} ---")

        # 1. PING & DETECT
        ser.write(b'\x3F')
        ping_resp = ser.read_until(b'\n')
        print(f"Ping: {ping_resp.hex(' ')} | '{ping_resp.decode(errors='ignore').strip()}'")
        
        ser.write(b'\x49\x53') # I2C Scan
        i2c_raw = ser.read_until(b'\xFF')
        if i2c_raw:
            print(f"I2C Scan found: {i2c_raw[:-1].hex(' ')}")
        else:
            print("I2C Scan: No devices found")

        ser.write(b'\x50\x4A') # JEDEC
        jedec = ser.read(3)
        print(f"SPI JEDEC: {jedec.hex().upper()}")
        
        # 2. BACKUP I2C 16KB
        print("\n--- INICIANDO BACKUP I2C (16KB) ---")
        ser.write(b'\x49\x46\x02\xA0\x40\x00')
        data_i2c = robust_read_all(ser, 16384)
        # Consumir el RESP_END (55)
        end_token = ser.read(1)
        print(f"Token fin I2C: {end_token.hex()}")
        
        with open("i2c_backup_v2.bin", "wb") as f:
            f.write(data_i2c)

        # Re-sincronizar
        time.sleep(1)
        ser.reset_input_buffer()

        # 3. BACKUP SPI 1MB
        print("\n--- INICIANDO BACKUP SPI (1MB) ---")
        ser.write(b'\x50\x46')
        data_spi = robust_read_all(ser, 1048576, timeout_per_kb=1.2) # 9600bps -> ~1.1s/KB
        end_token = ser.read(1)
        print(f"Token fin SPI: {end_token.hex()}")

        if len(data_spi) > 0:
            with open("spi_backup_v2.bin", "wb") as f:
                f.write(data_spi)
            print("Backup SPI guardado.")

        # 4. PRUEBA ESCRITURA I2C
        print("\n--- TEST ESCRITURA I2C ---")
        pattern = b'\xAA\x55\xAA\x55'
        ser.write(b'\x49\x57\x02\xA0\x00\x01\x00\x04' + pattern) # Dir 0x0001
        resp = ser.read(1)
        print(f"Respuesta escritura: {resp.hex()}")
        if resp == b'\x4B':
            ser.write(b'\x49\x52\x02\xA0\x00\x01\x00\x04')
            read_back = ser.read(4)
            print(f"Leído: {read_back.hex()} (Esperado: {pattern.hex()})")
            ser.read(1) # Consumir 55

        # 5. PRUEBA SPI CHIP ERASE
        print("\n--- TEST SPI CHIP ERASE ---")
        ser.write(b'\x50\x45')
        resp = ser.read(1)
        print(f"Respuesta Erase: {resp.hex()}")
        print("Esperando a que el chip esté listo...")
        for i in range(20):
            ser.write(b'\x50\x53')
            st = ser.read(1)
            if st and (st[0] & 0x01) == 0:
                print(f"Listo! Status: {st.hex()}")
                break
            print(f"Status: {st.hex() if st else '??'} (esperando...)", end='\r')
            time.sleep(1)

        ser.close()
        print("\nTest Finalizado.")

    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    run_v2()

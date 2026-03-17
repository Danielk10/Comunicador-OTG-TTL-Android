import serial
import time
import sys

PORT = 'COM6'
BAUD = 9600

def test_ping(ser):
    print("\n--- Test PING ---")
    ser.write(b'\x3F')
    resp = ser.read_until(b'\n')
    text = resp.decode(errors='ignore').strip()
    print(f"Ping: {text}")
    assert "PICMEM v3 OK" in text, "Error en Ping"

def test_i2c_scan(ser):
    print("\n--- Test I2C SCAN ---")
    ser.write(b'\x49\x53')
    resp = ser.read_until(b'\xFF')
    if resp:
        print(f"I2C dispositivos encontrados: {resp[:-1].hex(' ')}")
    else:
        print("I2C Scan sin respuesta")

def test_spi_jedec(ser):
    print("\n--- Test SPI JEDEC ---")
    ser.write(b'\x50\x4A')
    resp = ser.read(3)
    print(f"JEDEC ID: {resp.hex(' ').upper()}")
    return resp

def test_spi_status(ser):
    print("\n--- Test SPI STATUS ---")
    ser.write(b'\x50\x53')
    resp = ser.read(1)
    print(f"SPI Status: {resp.hex() if resp else 'No response'}")

def test_chunked_spi_read(ser, address, length, chunk_size=64):
    print(f"\n--- Test LECTURA SPI POR CHUNKS (addr={address}, len={length}, chunk={chunk_size}) ---")
    current_addr = address
    total_data = bytearray()
    
    while current_addr < address + length:
        to_read = min(chunk_size, address + length - current_addr)
        # SPI Read: 50 52 <addr_len> <opcode> <A2> <A1> <A0> <LH> <LL>
        # Asumiendo 3-byte address
        a2 = (current_addr >> 16) & 0xFF
        a1 = (current_addr >> 8) & 0xFF
        a0 = current_addr & 0xFF
        lh = (to_read >> 8) & 0xFF
        ll = to_read & 0xFF
        
        cmd = bytearray([0x50, 0x52, 0x03, 0x03, a2, a1, a0, lh, ll])
        ser.write(cmd)
        
        chunk_data = ser.read(to_read)
        token = ser.read(1)
        
        print(f"Chunk leído en {hex(current_addr)}, len={len(chunk_data)}, token={token.hex()}")
        if token != b'\x55':
            print(f"Error: Token de fin de lectura incorrecto ({token.hex()})")
            break
        
        total_data.extend(chunk_data)
        current_addr += to_read
        
    print(f"Total leído: {len(total_data)} bytes")
    return total_data

def run_all_tests():
    try:
        ser = serial.Serial(PORT, BAUD, timeout=2)
        ser.reset_input_buffer()
        print(f"Conectado a {PORT} a {BAUD} baudios")
        
        test_ping(ser)
        test_i2c_scan(ser)
        test_spi_jedec(ser)
        test_spi_status(ser)
        
        # Probar lectura en trozos de 64 bytes (simulando Android)
        # Leemos 256 bytes en total = 4 chunks de 64
        test_chunked_spi_read(ser, 0x000000, 256, chunk_size=64)

        ser.close()
        print("\nPruebas finalizadas con éxito.")
    except Exception as e:
        print(f"Excepción durante las pruebas: {e}")

if __name__ == "__main__":
    run_all_tests()

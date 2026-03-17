import serial
import time
import sys

# Configuración del puerto
PORT = 'COM6'
BAUD = 9600
TIMEOUT = 2  # Timeout de 2 segundos para comandos largos

def send_command(ser, cmd_bytes, expected_len=None, read_until=None):
    print(f"Enviando: {cmd_bytes.hex(' ').upper()}")
    ser.write(cmd_bytes)
    
    if read_until:
        data = ser.read_until(read_until)
        return data
    
    if expected_len:
        data = ser.read(expected_len)
        return data
    
    # Lectura por defecto hasta timeout o fin de flujo
    data = ser.read(1024)
    return data

def main():
    try:
        ser = serial.Serial(PORT, BAUD, timeout=TIMEOUT)
        print(f"Conectado a {PORT} at {BAUD} baud.")
        time.sleep(1) # Esperar a que el PIC se estabilice
    except Exception as e:
        print(f"Error al abrir el puerto: {e}")
        return

    try:
        # 1. Ping
        print("\n--- [1] PROBANDO PING ---")
        # El ping devuelve "PICMEM v3 OK\r\n"
        resp = send_command(ser, bytes([0x3F]), read_until=b'\n')
        print(f"Respuesta Ping: {resp.decode(errors='ignore').strip()}")

        # 2. I2C Scan
        print("\n--- [2] ESCANEO I2C ---")
        # Devuelve las direcciones + 0xFF
        resp = send_command(ser, bytes([0x49, 0x53]), read_until=b'\xFF')
        print(f"Direcciones detectadas: {resp.hex(' ').upper()}")

        # 3. I2C Read 16KB (24C128)
        # Comando: 49 46 <addr_len=02> <chip_addr=A0> <LH=40> <LL=00>
        # Longitud 16KB = 16384 bytes = 0x4000
        print("\n--- [3] LECTURA EEPROM I2C (16KB) ---")
        # El volcado completo I2C devuelve todos los bytes + 0x55 al final
        # Aumentamos timeout temporalmente para lectura larga
        ser.timeout = 30
        resp = send_command(ser, bytes([0x49, 0x46, 0x02, 0xA0, 0x40, 0x00]), read_until=b'\x55')
        print(f"Bytes recibidos (incluyendo 0x55): {len(resp)}")
        if len(resp) > 1:
            print(f"Primeros 16 bytes: {resp[:16].hex(' ').upper()}")
        ser.timeout = TIMEOUT

        # 4. SPI JEDEC ID
        print("\n--- [4] SPI JEDEC ID ---")
        # Devuelve 3 bytes
        resp = send_command(ser, bytes([0x50, 0x4A]), expected_len=3)
        print(f"Respuesta JEDEC ID: {resp.hex(' ').upper()}")
        if resp == b'\xEF\x40\x14':
            print("MEMORIA IDENTIFICADA CORRECTAMENTE: GD25Q80PCP (Winbond/GigaDevice match)")
        elif resp == b'\xFF\xFF\xFF' or resp == b'\x00\x00\x00':
            print("ERROR: Memoria no detectada.")
        else:
            print("MEMORIA DESCONOCIDA Detectada.")

        # 5. SPI Read 1MB (Full Flash Dump)
        # El comando 0x50 0x46 detecta tamaño y lee todo + 0x55
        print("\n--- [5] LECTURA FLASH SPI (1MBaprox) ---")
        print("Esto tomará aproximadamente 107 segundos a 9600 baud...")
        ser.timeout = 150 # Timeout largo para 1MB
        resp = send_command(ser, bytes([0x50, 0x46]), read_until=b'\x55')
        print(f"Bytes recibidos (incluyendo 0x55): {len(resp)}")
        if len(resp) > 1:
            print(f"Primeros 16 bytes: {resp[:16].hex(' ').upper()}")
        ser.timeout = TIMEOUT

    except KeyboardInterrupt:
        print("\nPrueba interrumpida por el usuario.")
    finally:
        ser.close()
        print("\nConexión cerrada.")

if __name__ == "__main__":
    main()

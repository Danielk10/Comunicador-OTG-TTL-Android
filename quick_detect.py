import serial
import time

# Protocolo PICMEM v3 - Detección Rápida
PORT = 'COM6'
BAUD = 9600

def main():
    try:
        ser = serial.Serial(PORT, BAUD, timeout=2)
        print(f"Conectado a {PORT}...")
        time.sleep(0.5)
        ser.reset_input_buffer()

        # 1. Ping
        ser.write(bytes([0x3F]))
        resp = ser.read_until(b'\n')
        print(f"Ping: {resp.decode(errors='ignore').strip()}")

        # 2. I2C Scan
        ser.write(bytes([0x49, 0x53]))
        resp = ser.read_until(b'\xFF')
        if resp:
            addrs = [f"0x{b:02X}" for b in resp[:-1]]
            print(f"I2C Scan: {', '.join(addrs)}")
        else:
            print("I2C Scan: No se detectaron dispositivos.")

        # 3. SPI JEDEC ID
        ser.write(bytes([0x50, 0x4A]))
        resp = ser.read(3)
        if resp:
            print(f"SPI JEDEC ID: {resp.hex(' ').upper()}")
            if resp == b'\xEF\x40\x14' or resp == b'\xC8\x40\x14':
                print("Resultado: Memoria GD25Q80 detectada correctamente.")
        else:
            print("SPI JEDEC ID: Sin respuesta.")

        # 4. Leer Status Register SPI (por si acaso)
        ser.write(bytes([0x50, 0x53]))
        resp = ser.read(1)
        if resp:
             print(f"SPI Status: 0x{resp.hex().upper()}")

        ser.close()
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    main()

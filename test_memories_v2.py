import serial
import time
import sys

# Configuración del puerto
PORT = 'COM6'
BAUD = 9600
TIMEOUT = 5

def clear_buffer(ser):
    """Limpia el buffer de entrada para evitar leer basura de comandos previos."""
    ser.reset_input_buffer()

def send_command(ser, cmd_bytes, desc):
    print(f"\n[COMANDO] {desc}")
    print(f"Enviando: {cmd_bytes.hex(' ').upper()}")
    clear_buffer(ser)
    ser.write(cmd_bytes)

def read_response_until(ser, end_byte, max_len=None):
    """Lee datos hasta encontrar el end_byte o alcanzar max_len."""
    data = bytearray()
    start_time = time.time()
    while True:
        if ser.in_waiting > 0:
            b = ser.read(1)
            data.extend(b)
            if b == end_byte:
                return data
            if max_len and len(data) >= max_len:
                return data
        
        # Timeout de seguridad
        if time.time() - start_time > ser.timeout:
            return data

def read_fixed_length(ser, length):
    """Lee una cantidad fija de bytes."""
    return ser.read(length)

def progress_read(ser, total_size, desc):
    """Lee datos con una barra de progreso informativa."""
    print(f"\n[LECTURA] {desc} ({total_size} bytes)")
    data = bytearray()
    last_print = 0
    start_time = time.time()
    
    # El PIC envía los datos seguidos y un 0x55 al final (según protocolo)
    # Sin embargo, para mayor seguridad leemos el tamaño esperado + el token 0x55
    expected_with_token = total_size + 1
    
    while len(data) < expected_with_token:
        if ser.in_waiting > 0:
            chunk = ser.read(ser.in_waiting)
            data.extend(chunk)
            
            # Log simple cada 10%
            percent = int((len(data) / expected_with_token) * 100)
            if percent % 10 == 0 and percent > last_print:
                print(f"Progreso: {percent}% ({len(data)} bytes)")
                last_print = percent

        
        # Si no hay datos por mucho tiempo, abortar
        if time.time() - start_time > 10: # 10 segundos sin actividad
            if len(data) > 0:
                start_time = time.time() # Reset si al menos estamos recibiendo
            else:
                print("\nError: Timeout durante la lectura.")
                break
                
    print(f"\nLectura finalizada. Total recibidos: {len(data)} bytes.")
    return data

def main():
    try:
        ser = serial.Serial(PORT, BAUD, timeout=TIMEOUT)
        print(f"Conectado a {PORT} a {BAUD} baudios.")
        time.sleep(2) # Espera inicial
    except Exception as e:
        print(f"Error: No se pudo abrir el puerto {PORT}. {e}")
        return

    try:
        # 1. PING
        send_command(ser, bytes([0x3F]), "PING")
        ping_resp = read_response_until(ser, b'\n')
        print(f"Respuesta Ping: {ping_resp.decode(errors='ignore').strip()}")

        # 2. I2C SCAN
        send_command(ser, bytes([0x49, 0x53]), "I2C SCAN")
        scan_resp = read_response_until(ser, b'\xFF')
        print(f"Dispositivos I2C: {scan_resp.hex(' ').upper()}")

        # 3. SPI JEDEC ID
        send_command(ser, bytes([0x50, 0x4A]), "SPI JEDEC ID")
        jedec_resp = read_fixed_length(ser, 3)
        print(f"Respuesta JEDEC: {jedec_resp.hex(' ').upper()}")
        
        if jedec_resp == b'\xEF\x40\x14' or jedec_resp == b'\xC8\x40\x14':
            print("INFO: Memoria GD25Q80PCP (o compatible) detectada.")
        elif jedec_resp == b'\xFF\xFF\xFF':
            print("ADVERTENCIA: JEDEC devolvió FF FF FF. Revisa que el PIC esté en IDLE (LED apagado) antes de empezar.")

        # 4. LECTURA I2C (16KB = 16384 bytes)
        send_command(ser, bytes([0x49, 0x46, 0x02, 0xA0, 0x40, 0x00]), "I2C FULL DUMP (16KB)")
        i2c_data = progress_read(ser, 16384, "EEPROM I2C")
        
        # 5. LECTURA SPI (LIMITADA PARA PRUEBA RÁPIDA)
        send_command(ser, bytes([0x50, 0x46]), "SPI FULL AUTO DUMP (Prueba de 4KB)")
        # Le pedimos 4KB solamente para no esperar 18 minutos en esta prueba
        spi_data = progress_read(ser, 4096, "FLASH SPI")


    except KeyboardInterrupt:
        print("\nPrueba cancelada por el usuario.")
    finally:
        ser.close()
        print("\nPuerto cerrado.")

if __name__ == "__main__":
    main()

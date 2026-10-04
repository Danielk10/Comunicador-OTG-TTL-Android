# 🔌 Emulador de Hardware PICMEM v3 (PIC16F628A)

Este subdirectorio contiene el entorno de simulación local para el firmware **PICMEM v3** utilizado por **Comunicador-OTG-TTL-Android**.

Permite emular un programador físico conectado por puerto serie USB sobre un pseudo-terminal virtual (`vtty` / `/dev/pts/N`), sin necesidad de hardware conectado.

---

## 🛠️ Capacidades del Emulador

1. **Protocolo Binario Completo:**
   - **Ping (`0x3F`):** Responde `PICMEM v3 OK\r\n`.
   - **I2C Bus Scan (`49 53`):** Simula detección de EEPROM en `0x50` y chip RTC en `0x68`, finalizando con `0xFF`.
   - **I2C Read / Write (`49 52` / `49 57`):** Lectura y escritura en memoria virtual de 32 KB (24C256) con confirmaciones `RESP_OK` (`0x4B`) y `RESP_END` (`0x55`).
   - **I2C Full Dump (`49 46`):** Volcado secuencial completo de la EEPROM virtual.
   - **SPI JEDEC ID (`50 4A`):** Devuelve la firma `EF 40 15` de Winbond W25Q16 (2 MB).
   - **SPI Status (`50 53`):** Devuelve el registro de estado `0x00`.
   - **SPI Read / Write (`50 52` / `50 57`):** Lectura y escritura en memoria Flash virtual de 2 MB.
   - **SPI Chip Erase (`50 45`):** Limpia la memoria Flash virtual a `0xFF` devolviendo `0x4B`.

---

## 🚀 Ejecución de Pruebas

Para arrancar el emulador y ejecutar la suite de validación:

```bash
chmod +x probar_emulacion.sh
./probar_emulacion.sh
```

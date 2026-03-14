# PICMEM v3 — Documentación del Protocolo

**Firmware para PIC16F628A** · Compilador SDCC · Oscilador INTRC 4 MHz · UART 9600 baud
**Terminal:** [Serial USB Terminal](https://play.google.com/store/apps/details?id=de.kai_morich.serial_usb_terminal) en **modo HEX**

---

## Tabla de Contenidos

1. [Conexiones de hardware](#1-conexiones-de-hardware)
2. [Configuración de la terminal](#2-configuración-de-la-terminal)
3. [Protocolo binario](#3-protocolo-binario)
4. [Tokens de respuesta](#4-tokens-de-respuesta)
5. [Comandos generales](#5-comandos-generales)
6. [Comandos I2C](#6-comandos-i2c)
7. [Comandos SPI](#7-comandos-spi)
8. [Referencia rápida de bytes](#8-referencia-rápida-de-bytes)
9. [Guía de chips comunes](#9-guía-de-chips-comunes)
10. [LED indicador](#10-led-indicador)
11. [Notas de compilación](#11-notas-de-compilación)
12. [Solución de problemas](#12-solución-de-problemas)

---

## 1. Conexiones de hardware

### Diagrama de pines PIC16F628A (DIP-18)

```
         +--────────────+
  RA2 ───┤ 1          18├─── RA1  (SCL I2C)
  RA3 ───┤ 2          17├─── RA0  (SDA I2C)
  RA4 ───┤ 3          16├─── RA7
MCLR/RA5─┤ 4          15├─── RA6  (MOSI SPI)
   GND ──┤ 5 (VSS)    14├─── VDD (+5V)
   RB0 ──┤ 6          13├─── RB7
RX RB1 ──┤ 7          12├─── RB6
TX RB2 ──┤ 8          11├─── RB5
   RB3 ──┤ 9          10├─── RB4  *** LED ***
         +──────────────+
```

### Asignación de señales

| Pin | Nombre | Función       | Dirección      | Notas                             |
|-----|--------|---------------|----------------|-----------------------------------|
| 17  | RA0    | SDA (I2C)     | E/S open-drain | Pull-up 4.7 kΩ externo requerido  |
| 18  | RA1    | SCL (I2C)     | E/S open-drain | Pull-up 4.7 kΩ externo requerido  |
| 1   | RA2    | CS  (SPI)     | Salida         | Activo bajo                       |
| 2   | RA3    | SCK (SPI)     | Salida         | Reloj SPI Modo 0                  |
| 4   | RA5    | MISO (SPI)    | Entrada        | Dato chip → PIC                   |
| 15  | RA6    | MOSI (SPI)    | Salida         | Dato PIC → chip                   |
| 7   | RB1    | RX (UART)     | Entrada        | 9600 baud                         |
| 8   | RB2    | TX (UART)     | Salida         | 9600 baud                         |
| 10  | RB4    | LED indicador | Salida         | 330 Ω en serie, cátodo a GND      |

### LED indicador — circuito

```
RB4 (pin 10) ──[330 Ω]──[►|]── GND
                         LED
```

### Pull-ups I2C

```
VCC
 |      |
4.7k   4.7k
 |      |
SDA    SCL
```

### Flash SPI — conexión típica (W25Qxx SOIC-8)

```
PIC RA2 (CS)   ──── /CS   pin1   VCC ──── /WP   pin3
PIC RA3 (SCK)  ──── CLK   pin6   VCC ──── /HOLD pin7
PIC RA6 (MOSI) ──── DI    pin5
PIC RA5 (MISO) ──── DO    pin2
                    VCC   pin8
                    GND   pin4
```

---

## 2. Configuración de la terminal

### Serial USB Terminal — pasos

1. Conectar adaptador USB-Serie (CP2102, CH340 o FT232)
2. Abrir **Serial USB Terminal**
3. **Conexión → Configurar:**
   - Baud rate: **9600** — Data bits: **8** — Stop bits: **1** — Parity: **None** — Flow: **None**
4. **Terminal → Configurar:**
   - Receive mode: **HEX**
   - Send mode: **HEX**
   - Newline TX: **ninguno**
5. Presionar **Conectar**

### Botones de macro recomendados (M1–M6)

| Botón | Etiqueta    | Bytes HEX                          | Acción               |
|-------|-------------|------------------------------------|----------------------|
| M1    | PING        | `3F`                               | Verificar conexión   |
| M2    | I2C SCAN    | `49 53`                            | Escanear bus I2C     |
| M3    | JEDEC ID    | `50 4A`                            | Identificar Flash    |
| M4    | SPI STATUS  | `50 53`                            | Leer Status Register |
| M5    | FULL DUMP   | `50 46`                            | Volcar Flash entera  |
| M6    | READ 256    | `50 52 03 03 00 00 00 01 00`       | Primeros 256 bytes   |

---

## 3. Protocolo binario

Todo el protocolo es **binario puro**. La estructura general de un comando es:

```
[CMD] [SUB] [parámetros...] [datos si aplica]
```

**Reglas importantes:**

- Timeout entre bytes consecutivos: **~262 ms**. Si expira → PIC responde `58` y vuelve a esperar.
- La longitud siempre se envía en **2 bytes MSB primero** (big-endian).
- Los datos de lectura llegan sin delimitador; solo `55` al final marca el fin.
- El LED (RB4) se enciende durante lecturas y parpadea al completar escrituras.

---

## 4. Tokens de respuesta

| Byte | ASCII | Significado                                        |
|------|-------|----------------------------------------------------|
| `4B` | `K`   | **RESP_OK** — escritura o erase exitoso            |
| `58` | `X`   | **RESP_ERR** — error, NACK, timeout o chip ausente |
| `55` | `U`   | **RESP_END** — fin del flujo de datos de lectura   |
| `FF` | —     | Fin de lista en I2C Bus Scan                       |

---

## 5. Comandos generales

### 5.1 Ping (`3F`)

```
Enviar:  3F
Recibir: 50 49 43 4D 45 4D 20 76 33 20 4F 4B 0D 0A
         P  I  C  M  E  M     v  3     O  K  \r \n
```

---

## 6. Comandos I2C

Implementación: bit-banging open-drain ~50 kHz.

### 6.1 I2C Bus Scan (`49 53`)

Escanea 0x08–0x77, devuelve las direcciones que responden ACK.

```
Enviar:  49 53
Recibir: [addr_7bit]... FF
```

**Ejemplo** — chip en 0x50 (24C256) y DS1307 en 0x68:
```
Recibir: 50 68 FF
```

> La dirección de escritura de 8 bits = `addr_7bit << 1`.
> Ejemplo: `50` (hex) → `0xA0` en 8 bits (dirección de escritura 24Cxx).

---

### 6.2 I2C Read — Lectura parcial (`49 52`)

```
Enviar:  49 52 <addr_len> <chip_addr> <A1> <A0> <LH> <LL>
Recibir: [datos...] 55   o   58 si NACK
```

| Campo       | Descripción                                                   |
|-------------|---------------------------------------------------------------|
| `addr_len`  | `01` = 8-bit (24C01–24C16) · `02` = 16-bit (24C32–24C512)   |
| `chip_addr` | Dir I2C con R/W=0. Típico: `A0`                              |
| `A1` `A0`   | Dirección interna MSB/LSB (si addr_len=1, usar `00` para A1) |
| `LH` `LL`   | Longitud en bytes MSB primero (máx 65535)                    |

**Ejemplos:**
```
# 256 bytes desde 0x0000 — 24C256 (0xA0, 16-bit addr):
49 52 02 A0 00 00 01 00

# 128 bytes desde 0x0080 — 24C256 (0xA0):
49 52 02 A0 00 80 00 80

# 32 bytes desde 0x40 — 24C04 (0xA0, 8-bit addr):
49 52 01 A0 00 40 00 20

# 65535 bytes desde 0x0000 (maximo por comando):
49 52 02 A0 00 00 FF FF
```

---

### 6.3 I2C Write — Escritura (`49 57`)

```
Enviar:  49 57 <addr_len> <chip_addr> <A1> <A0> <LH> <LL> [datos...]
Recibir: 4B (OK) o 58 (ERR)
```

**Tamaños de pagina por modelo:**

| Modelo        | Bytes/pagina |
|---------------|--------------|
| 24C01–24C16   | 8 bytes      |
| 24C32–24C64   | 32 bytes     |
| 24C128–24C512 | 64 bytes     |

**Ejemplos:**
```
# 4 bytes (DE AD BE EF) en 0x0000 — 24C256:
49 57 02 A0 00 00 00 04 DE AD BE EF

# 8 bytes en 0x0008 — 24C08 (8-bit addr):
49 57 01 A0 00 08 00 08 01 02 03 04 05 06 07 08
```

---

### 6.4 I2C Full Dump — Volcado completo (`49 46`)

Sequential read desde dirección 0 hasta el final de la EEPROM.

```
Enviar:  49 46 <addr_len> <chip_addr> <LH> <LL>
Recibir: [todos los bytes] 55   o   58 si NACK
```

**Tamaños de EEPROM:**

| Modelo  | Tamaño | LH   | LL   | addr_len |
|---------|--------|------|------|----------|
| 24C01   | 128 B  | `00` | `80` | `01`     |
| 24C02   | 256 B  | `01` | `00` | `01`     |
| 24C04   | 512 B  | `02` | `00` | `01`     |
| 24C08   | 1 KB   | `04` | `00` | `01`     |
| 24C16   | 2 KB   | `08` | `00` | `01`     |
| 24C32   | 4 KB   | `10` | `00` | `02`     |
| 24C64   | 8 KB   | `20` | `00` | `02`     |
| 24C128  | 16 KB  | `40` | `00` | `02`     |
| 24C256  | 32 KB  | `80` | `00` | `02`     |
| 24C512  | 64 KB  | `FF` | `FF` | `02`     |

**Ejemplos:**
```
# Volcar 24C256 (32 KB, 0xA0):
49 46 02 A0 80 00

# Volcar 24C02 (256 bytes, 0xA0):
49 46 01 A0 01 00
```

---

## 7. Comandos SPI

Implementación: bit-banging Modo 0 (CPOL=0, CPHA=0) ~100 kHz.

### 7.1 JEDEC ID (`50 4A`)

```
Enviar:  50 4A
Recibir: [Manufacturer] [MemType] [Capacity]
```

**IDs comunes Winbond W25Qxx:**

| Respuesta     | Modelo  | Tamano |
|---------------|---------|--------|
| `EF 40 13`    | W25Q40  | 512 KB |
| `EF 40 14`    | W25Q80  | 1 MB   |
| `EF 40 15`    | W25Q16  | 2 MB   |
| `EF 40 16`    | W25Q32  | 4 MB   |
| `EF 40 17`    | W25Q64  | 8 MB   |
| `EF 40 18`    | W25Q128 | 16 MB  |
| `C2 20 15`    | MX25L16 | 2 MB   |
| `C2 20 16`    | MX25L32 | 4 MB   |

> Si recibes `FF FF FF` o `00 00 00` → chip no conectado o error en CS/SCK/MOSI.

---

### 7.2 Status Register (`50 53`)

```
Enviar:  50 53
Recibir: [1 byte status]
```

**Mapa de bits:**

| Bit | Nombre | Descripcion                              |
|-----|--------|------------------------------------------|
| 0   | WIP    | Write In Progress (1=ocupado, esperar)   |
| 1   | WEL    | Write Enable Latch (1=escritura habilitada) |
| 2-4 | BP0-2  | Block Protect                            |
| 7   | SRP    | Status Register Protect                  |

`00` = listo · `01` = escribiendo · `02` = WEL habilitado

---

### 7.3 Chip Erase (`50 45`)

```
Enviar:  50 45
Recibir: 4B (OK) o 58 (timeout >5s)
```

> **ADVERTENCIA:** Borra TODA la Flash irreversiblemente.
> Tiempos: W25Q32 ~30s · W25Q128 ~120s.
> El PIC tiene timeout de 5s; chips lentos pueden dar ERR aunque el erase continue.
> Verificar con `50 53` (bit WIP=0 = completado).

---

### 7.4 SPI Read — Lectura parcial (`50 52`)

```
Enviar:  50 52 <addr_len> <opcode> <A2> <A1> <A0> <LH> <LL>
Recibir: [datos...] 55
```

| Campo      | Descripcion                                               |
|------------|-----------------------------------------------------------|
| `addr_len` | Bytes de direccion: `01`, `02` o `03`                    |
| `opcode`   | Opcode de lectura: `03` = Read Normal (universal)        |
| `A2 A1 A0` | Direccion de inicio MSB primero (rellenar con `00` si no se usa) |
| `LH LL`    | Longitud MSB primero (maximo 65535 bytes por comando)    |

**Ejemplos:**
```
# 256 bytes desde 0x000000 — Flash W25Qxx (3-bytes addr):
50 52 03 03 00 00 00 01 00

# 256 bytes desde 0x001000 — Flash (sector 1):
50 52 03 03 00 10 00 01 00

# 65535 bytes desde 0x000000 (maximo por comando):
50 52 03 03 00 00 00 FF FF

# 64 bytes desde 0x0040 — EEPROM SPI 25LCxx (2-bytes addr):
50 52 02 03 00 00 40 00 40

# 32 bytes desde 0x80 — EEPROM SPI pequena (1-byte addr):
50 52 01 03 00 00 80 00 20
```

**Lectura manual de Flash W25Q32 (4 MB) en bloques de 64 KB:**
```
Bloque 0  (0x000000): 50 52 03 03 00 00 00 FF FF → 65535B + 55
Bloque 1  (0x010000): 50 52 03 03 01 00 00 FF FF → 65535B + 55
Bloque 2  (0x020000): 50 52 03 03 02 00 00 FF FF → 65535B + 55
... (64 bloques total para W25Q32)
```

---

### 7.5 SPI Write — Page Program (`50 57`)

```
Enviar:  50 57 <addr_len> <opcode> <A2> <A1> <A0> <LH> <LL> [datos...]
Recibir: 4B (OK) o 58 (timeout WIP)
```

> **NOTAS IMPORTANTES:**
> - Flash NOR debe estar BORRADA (0xFF) antes de programar.
> - Maximo **256 bytes/pagina** por comando en Flash NOR. No cruzar fronteras de pagina (multiplos de 0x100).
> - WREN (0x06) se envia automaticamente antes de escribir.
> - EEPROM SPI (25LCxx): 16–64 bytes/pagina segun modelo.

**Opcodes de escritura:**

| Opcode | Nombre       | Chips                        |
|--------|--------------|------------------------------|
| `02`   | Page Program | Flash NOR (W25Qxx, MX25Lxx) |
| `02`   | Write        | EEPROM SPI (25LCxx, M95xx)  |

**Ejemplos:**
```
# 4 bytes (DE AD BE EF) en 0x000000 — Flash W25Qxx:
50 57 03 02 00 00 00 00 04 DE AD BE EF

# 256 bytes en pagina 0 (0x000000) — Flash W25Qxx:
50 57 03 02 00 00 00 01 00 [256 bytes de datos]

# 4 bytes en 0x0010 — EEPROM SPI 25LC010A (1-byte addr):
50 57 01 02 00 00 10 00 04 AA BB CC DD
```

---

### 7.6 Full Flash Dump — Volcado completo automatico (`50 46`)

Detecta automaticamente el tamano de la Flash via JEDEC y la vuelca completa.

```
Enviar:  50 46
Recibir: [todos los bytes de la Flash] 55   o   58 si chip no detectado
```

**Proceso interno:**
1. Lee JEDEC ID (opcode `9F`)
2. Valida fabricante (≠ 00, ≠ FF) y capacity (0x10–0x1C)
3. Calcula `flash_size = 1 << capacity_byte`
4. Lee en bloques de 256 bytes con opcode `03` desde 0x000000
5. Envia `55` al terminar

**Tiempos a 9600 baud:**

| Chip    | Tamano | Tiempo aprox.     |
|---------|--------|-------------------|
| W25Q08  | 1 MB   | ~107 segundos     |
| W25Q16  | 2 MB   | ~215 segundos     |
| W25Q32  | 4 MB   | ~430 seg (~7 min) |
| W25Q64  | 8 MB   | ~14 minutos       |
| W25Q128 | 16 MB  | ~28 minutos       |

---

## 8. Referencia rápida de bytes

### Tabla de comandos

| CMD  | SUB  | Descripcion           | Parametros adicionales         |
|------|------|-----------------------|--------------------------------|
| `3F` | —    | Ping                  | (ninguno)                      |
| `49` | `53` | I2C Scan              | (ninguno)                      |
| `49` | `52` | I2C Read              | al ca A1 A0 LH LL              |
| `49` | `57` | I2C Write             | al ca A1 A0 LH LL [datos]      |
| `49` | `46` | I2C Full Dump         | al ca LH LL                    |
| `50` | `4A` | SPI JEDEC ID          | (ninguno)                      |
| `50` | `53` | SPI Status Register   | (ninguno)                      |
| `50` | `45` | SPI Chip Erase        | (ninguno)                      |
| `50` | `52` | SPI Read              | al op A2 A1 A0 LH LL           |
| `50` | `57` | SPI Write             | al op A2 A1 A0 LH LL [datos]   |
| `50` | `46` | SPI Full Flash Dump   | (ninguno)                      |

**Leyenda:** al=addr_len · ca=chip_addr · op=opcode · A2..A0=direccion · LH/LL=longitud MSB/LSB

### Calculo de LH LL para la longitud

| Bytes    | LH   | LL   |
|----------|------|------|
| 32       | `00` | `20` |
| 64       | `00` | `40` |
| 128      | `00` | `80` |
| 256      | `01` | `00` |
| 512      | `02` | `00` |
| 1024     | `04` | `00` |
| 4096     | `10` | `00` |
| 32768    | `80` | `00` |
| 65535    | `FF` | `FF` |

---

## 9. Guia de chips comunes

### EEPROMs I2C (24Cxx)

| Chip   | Tamano  | addr_len | chip_addr | Pag. |
|--------|---------|----------|-----------|------|
| 24C01  | 128 B   | `01`     | `A0`      | 8 B  |
| 24C02  | 256 B   | `01`     | `A0`      | 8 B  |
| 24C04  | 512 B   | `01`     | `A0`      | 16 B |
| 24C08  | 1 KB    | `01`     | `A0`      | 16 B |
| 24C16  | 2 KB    | `01`     | `A0`      | 16 B |
| 24C32  | 4 KB    | `02`     | `A0`      | 32 B |
| 24C64  | 8 KB    | `02`     | `A0`      | 32 B |
| 24C128 | 16 KB   | `02`     | `A0`      | 64 B |
| 24C256 | 32 KB   | `02`     | `A0`      | 64 B |
| 24C512 | 64 KB   | `02`     | `A0`      | 128B |

> Direccion `A0` asume A2=A1=A0 del chip = GND.

### Flash SPI NOR (W25Qxx)

| Chip    | JEDEC      | Tamano | Sector | Pagina |
|---------|------------|--------|--------|--------|
| W25Q08  | EF 40 14   | 1 MB   | 4 KB   | 256 B  |
| W25Q16  | EF 40 15   | 2 MB   | 4 KB   | 256 B  |
| W25Q32  | EF 40 16   | 4 MB   | 4 KB   | 256 B  |
| W25Q64  | EF 40 17   | 8 MB   | 4 KB   | 256 B  |
| W25Q128 | EF 40 18   | 16 MB  | 4 KB   | 256 B  |
| MX25L32 | C2 20 16   | 4 MB   | 4 KB   | 256 B  |

### EEPROMs SPI (25LCxx)

| Chip     | Tamano  | addr_len | Pagina |
|----------|---------|----------|--------|
| 25LC010A | 128 B   | `01`     | 16 B   |
| 25LC020A | 256 B   | `01`     | 16 B   |
| 25LC080  | 1 KB    | `02`     | 16 B   |
| 25LC256  | 32 KB   | `02`     | 64 B   |
| M95256   | 32 KB   | `02`     | 64 B   |

---

## 10. LED indicador

LED en RB4 (pin 10 DIP-18), activo alto, resistencia 330 Ω en serie.

| Patron LED                | Significado                              |
|---------------------------|------------------------------------------|
| 2 parpadeos al encender   | Sistema inicializado correctamente       |
| Encendido continuo        | Lectura de memoria en curso (I2C o SPI)  |
| 1 parpadeo al terminar    | Escritura completada (I2C Write / SPI Write) |
| 3 parpadeos al terminar   | Chip Erase completado                    |
| Apagado                   | Esperando comandos                       |

---

## 11. Notas de compilacion

### Comando

```bash
sdcc --std-c99 -mpic14 -p16f628a --use-non-free pic_firmware_v3.c
```

### Warnings corregidos de v2 a v3

| Warning en v2                          | Correccion en v3                              |
|----------------------------------------|-----------------------------------------------|
| unreferenced variable `wip_count`      | Variable eliminada                            |
| unreferenced variable `status`         | Variable eliminada                            |
| overflow constant conversion (linea 592)| Cast explicito `(uint8_t)` en todos los shifts|
| Constante `UART_TIMEOUT_COUNT` sin usar | Eliminada; se usa rollover natural de uint16_t|

### Warnings normales (inofensivos)

- **`Message[1304] Page selection not needed`**: El PIC16F628A tiene 2 KB de Flash
  (una sola pagina de programa). SDCC genera `PAGESEL` preventivamente; el ensamblador
  las omite. No afecta el funcionamiento.
- **`Relocation symbol _cinit has no section`**: Normal en proyectos SDCC/gputils para PIC14.

---

## 12. Solucion de problemas

| Sintoma                         | Causa probable                    | Solucion                              |
|---------------------------------|-----------------------------------|---------------------------------------|
| PIC no responde al ping `3F`    | UART no conectado                 | Verificar RX-TX cruzados, baud=9600   |
| Responde `58` a todos los cmd   | FERR o OERR en UART               | El PIC se auto-recupera; reintentar   |
| I2C Scan devuelve solo `FF`     | Sin pull-ups o chip desconectado  | Verificar resistencias 4.7 kOhm       |
| I2C Read devuelve `58`          | Direccion I2C incorrecta          | Usar I2C Scan primero                 |
| JEDEC devuelve `FF FF FF`       | Chip SPI no conectado             | Verificar CS, SCK, MOSI, MISO, VCC   |
| Full Dump devuelve `58`         | JEDEC invalido                    | Verificar conexiones SPI              |
| Escritura no persiste en Flash  | Sector no borrado previamente     | Hacer Sector Erase o Chip Erase antes |
| LED no parpadea al arrancar     | Circuito LED incorrecto           | Verificar polaridad y 330 Ohm en serie|
| Chip Erase da timeout           | Chip muy grande (>4 MB)           | Normal; verificar con `50 53` bit WIP |

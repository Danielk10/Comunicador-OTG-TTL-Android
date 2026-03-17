# OTG Flash EEPROM - Programador Universal de Memorias

![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)
![Android](https://img.shields.io/badge/Android-23%2B-green.svg)
![Hardware](https://img.shields.io/badge/Hardware-PIC16F628A-orange.svg)
![Protocol](https://img.shields.io/badge/Protocol-I2C%20%7C%20SPI-blue.svg)

**OTG Flash EEPROM** es una aplicación Android profesional que convierte tu smartphone en un programador y lector universal de memorias EEPROM. Utilizando un microcontrolador PIC16F628A como puente de comunicación, la aplicación permite grabar y leer memorias I2C (24Cxx) y SPI Flash (25Cxx, W25Qxx) directamente desde tu dispositivo móvil.

---

## 🎯 Características Principales

### ✨ Funcionalidades de la Aplicación

- **📖 Lectura de memorias (Sistema por Chunks)**: Extrae el contenido de EEPROMs I2C y Flash SPI garantizando el 100% de la integridad de los datos, evitando el desbordamiento del puerto serie usando un ingenioso sistema de páginas (chunks).
- **✍️ Escritura de firmware segura**: Programa memorias con archivos .hex o .bin respetando los límites de los sectores de las memorias (Page Programs).
- **🔍 Escáner Automático**: Detección inteligente de chips conectados al bus I2C y lectura nativa del JEDEC ID en chips SPI. El modelo del chip en el selector de la App se autocompleta con base en este escaneo.
- **🛡️ Verificación de datos integrada**: Compara el buffer local de escritura con el buffer leído directamente desde la memoria para verificar empíricamente la autenticidad del quemado byte por byte.
- **🔍 Visor hexadecimal en tiempo real**: Visualiza los datos mientras se leen/escriben, optimizado para archivos grandes (Throttled render window) para no saturar la memoria RAM del celular (Prevención de ANR).
- **💾 Exportación de archivos**: Guarda dumps de manera nativa en formato .bin y .hex en la carpeta de descargas del dispositivo.
- **🔌 Conexión USB OTG nativa**: Gestión de la interfaz a 9600 baudios compatible con CH340, CP2102 y FTDI, cumpliendo las directrices más estrictas de permisos USB de Android 13+.

### 🛠️ Arquitectura del Sistema

El proyecto utiliza una arquitectura de dos capas:

1. **Aplicación Android (El Cerebro)**:
   - Interfaz Material Design con reportes log en tiempo real y copiables.
   - Procesamiento e inyección de datos Intel HEX y Binarios puros.
   - Manejo de estado asíncrono y control de tiempo de espera (timeouts).
   - Control del puerto serial mediante [usb-serial-for-android](https://github.com/mik3y/usb-serial-for-android).

2. **Firmware PIC16F628A (El Intérprete)**:
   - Recibe comandos seriales desde Android o cualquier script Python.
   - Interfaz I2C y SPI por hardware (bit-banging ultrarrápido).
   - LED indicador de estado visual (RB4).
   - Código en lenguaje C estricto (C99 compilado en SDCC), 0 warnings.
   - Protocolo binario documentado en [PICMEM_v3_Protocolo.md](PICMEM_v3_Protocolo.md).

---

## 📦 Componentes Necesarios

### Hardware Requerido

| Componente | Especificación | Cantidad |
|------------|---------------|----------|
| PIC16F628A | Microcontrolador DIP-18 | 1 |
| Adaptador USB-Serial | CH340, CP2102 o FTDI | 1 |
| Cable OTG | Micro-USB o USB-C según tu Android | 1 |
| Resistencias Pull-Up | 4.7 kΩ (para I2C) | 2 |
| LED | Cualquier color | 1 |
| Resistencia LED | 330 Ω | 1 |
| Oscilador | (Opcional, el código corre a 4MHz con oscilador interno) | - |

### Software Necesario

- **Android**: Versión 6.0 (API 23) a Android 13+ (API 33+) con soporte USB OTG.
- **Terminal serial o Script (opcional)**: [Serial USB Terminal](https://play.google.com/store/apps/details?id=de.kai_morich.serial_usb_terminal) o usar los scripts en Python adjuntos (`test_protocol_v3.py`).

---

## 🔌 Conexión del Hardware

### Diagrama de Pines PIC16F628A (DIP-18)

```
         +──────────────+
  RA2 ───┤ 1          18├─── RA1  (SCL I2C)
  RA3 ───┤ 2          17├─── RA0  (SDA I2C)
  RA4 ───┤ 3          16├─── RA7
MCLR/RA5─┤ 4          15├─── RA6  (MOSI SPI)
   GND ──┤ 5 (VSS)    14├─── VDD (+5V)
   RB0 ──┤ 6          13├─── RB7
RX RB1 ──┤ 7          12├─── RB6
TX RB2 ──┤ 8          11├─── RB5
   RB3 ──┤ 9          10├─── RB4  (LED)
         +──────────────+
```

### Conexiones Básicas

#### Comunicación Serial (PIC ↔ Adaptador USB)
- **Pin 7 (RB1/RX PIC)** → TX del adaptador USB-Serial
- **Pin 8 (RB2/TX PIC)** → RX del adaptador USB-Serial
- **GND** → GND compartido

#### LED Indicador
- **Pin 10 (RB4)** → [Resistencia 330Ω] → [LED ánodo]

#### Memorias I2C (24Cxx)
- **Pin 17 (RA0/SDA)** → Pin SDA de la memoria + Pull-up 4.7kΩ a VCC
- **Pin 18 (RA1/SCL)** → Pin SCL de la memoria + Pull-up 4.7kΩ a VCC

#### Flash SPI (W25Qxx, 25Cxx)
- **Pin 1 (RA2/CS)** → Pin /CS de la Flash
- **Pin 2 (RA3/SCK)** → Pin CLK de la Flash
- **Pin 4 (RA5/MISO)** → Pin DO de la Flash
- **Pin 15 (RA6/MOSI)** → Pin DI de la Flash

---

## 🚀 Guía de Uso Rápida

1. **Firmware del PIC**: Descarga `pic_firmware_v3.hex` y grábalo en el microcontrolador.
2. **App en Android**: Conecta el USB-Serial por OTG y abre la App.
3. **Detección Automática**: Pulsa **Escanear / ID**. El Log mostrará los dispositivos I2C en el bus o el JEDEC de la memoria SPI, autoseleccionando el chip en el menú desplegable.
4. **Respaldo (Full Dump)**: Pulsa **Full Dump** o **Leer Memoria**. El sistema irá solicitando bloques (chunks) de 64 bytes hasta guardar el chip entero en RAM.
5. **Visor y Guardado**: Podrás previsualizar en vivo un fragmento del código y al final usar **Guardar Memoria** para exportarlo como Binario e Intel Hex en tu carpeta de Descargas.

---

## 🏗️ Arquitectura del Código Android

Tras una rigurosa auditoría (Marzo 2026), el código se encuentra limpio de dependencias muertas y warnings en su empaquetado.

### Estructura de Paquetes Optimizada

```
com.mobincube.pronosticos_parley_copy.sc_55UCEB/
├── usb/
│   └── UsbSerialManager.java          # Wrapper Serial robusto (Permisos Android 13+)
├── eeprom/
│   ├── EepromProtocol.java            # Interfaz de comandos
│   ├── I2cProtocol.java               # Direccionamiento 8-bits/16-bits para 24Cxx
│   └── SpiProtocol.java               # Opcodes SPI / Flash Erase
├── file/
│   ├── FileManager.java               # Exportador nativo SD (.hex y .bin)
│   └── IntelHexFormat.java            # Lector estricto con rellenado de espacio (0xFF)
├── exception/
│   └── HexParseException.java         # Control de errores de formato
└── ui/
    ├── MainActivity.java              # Máquina de estados principal Asíncrona
    ├── HexViewerHelper.java           # Render hexadecimal optimizado (Prevención de ANR)
    └── LogHelper.java                 # Log visual de usuario (Copiable)
```

---

## 🐛 Resolución de Problemas

*   **El escaneo I2C no encuentra el chip:** Asegúrate de instalar físicamente las **resistencias pull-up de 4.7kΩ** desde SDA y SCL hacia VCC (+5V o +3.3V según tu chip). Es mandatorio en el protocolo I2C.
*   **SPI Flash devuelve FF FF FF:** El chip Flash no responde. Verifica que los pines `/WP` y `/HOLD` del chip SPI estén conectados también a VCC.
*   **Timeouts en la lectura/escritura:** Los baudios (9600) son lentos pero muy estables. Si desconectas el chip a mitad del proceso, la app esperará hasta agotar su *Timeout* (10 segundos) antes de reiniciar su estado. Esto está diseñado como un seguro contra cuelgues.

---

## 📜 Licencia

Este proyecto se distribuye bajo la **[Licencia Apache 2.0](LICENSE)**.

---

## 👨‍💻 Autor

**Daniel Diamon**
- Email: [danielpdiamon@gmail.com](mailto:danielpdiamon@gmail.com)
- GitHub: [github.com/Danielk10](https://github.com/Danielk10)

**¿Te gusta el proyecto? ¡Dale una ⭐ en GitHub!**

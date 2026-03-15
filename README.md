# 📱 OTG Flash EEPROM - Programador Universal de Memorias

![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)
![Android](https://img.shields.io/badge/Android-23%2B-green.svg)
![Hardware](https://img.shields.io/badge/Hardware-PIC16F628A-orange.svg)
![Protocol](https://img.shields.io/badge/Protocol-I2C%20%7C%20SPI-blue.svg)

**OTG Flash EEPROM** es una aplicación Android profesional que convierte tu smartphone en un programador y lector universal de memorias EEPROM. Utilizando un microcontrolador PIC16F628A como puente de comunicación, la aplicación permite grabar y leer memorias I2C (24Cxx) y SPI Flash (25Cxx, W25Qxx) directamente desde tu dispositivo móvil.

---

## 🎯 Características Principales

### ✨ Funcionalidades de la Aplicación

- **📖 Lectura de memorias**: Extrae el contenido completo de EEPROMs I2C y Flash SPI
- **✍️ Escritura de firmware**: Programa memorias con archivos .hex o .bin
- **🔍 Visor hexadecimal en tiempo real**: Visualiza los datos mientras se leen/escriben
- **💾 Exportación de archivos**: Guarda dumps en formato .bin y .hex
- **🖥️ Terminal serial integrada**: Herramienta de diagnóstico y comunicación directa
- **📊 Soporte múltiples memorias**: 
  - **EEPROMs I2C**: 24C01 hasta 24C512 (128 bytes a 256 KB / 2 Mbit)
  - **EEPROM SPI**: 25LC series (128 bytes a 4 MB)
  - **Flash SPI NOR**: W25Qxx, MX25Lxx (1 MB hasta 16 MB)
- **🔌 Conexión USB OTG**: Compatible con adaptadores CH340, CP2102, FTDI
- **📱 Interfaz intuitiva**: Diseño Material Design optimizado para Android

### 🛠️ Arquitectura del Sistema

El proyecto utiliza una arquitectura de dos capas:

1. **Aplicación Android (El Cerebro)**: 
   - Interfaz de usuario con Material Design
   - Procesamiento de archivos Intel HEX y binarios
   - Comunicación USB OTG a 9600 baudios
   - Gestión de permisos y almacenamiento

2. **Firmware PIC16F628A (El Intérprete)**:
   - Recibe comandos seriales desde Android
   - Traduce a señales I2C/SPI mediante bit-banging
   - Controla directamente las memorias EEPROM
   - LED indicador de estado de operación

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
| Cristal/Oscilador | 4 MHz (opcional, se usa oscilador interno) | - |
| Protoboard | Para montaje de prototipo | 1 |
| Cables Dupont | Macho-macho, macho-hembra | Varios |

### Software Necesario

- **Android**: Versión 6.0 (API 23) o superior con soporte USB OTG
- **Para compilar firmware**: SDCC (Small Device C Compiler) o C PIC Compiler
- **Para programar PIC**: Cualquier programador compatible (PICkit, TL866, etc.)

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
- **LED cátodo** → GND

#### Memorias I2C (24Cxx)
- **Pin 17 (RA0/SDA)** → Pin SDA de la memoria + Pull-up 4.7kΩ a VCC
- **Pin 18 (RA1/SCL)** → Pin SCL de la memoria + Pull-up 4.7kΩ a VCC

```
VCC (+5V o +3.3V según la memoria)
 │      │
4.7k   4.7k
 │      │
SDA    SCL
```

#### Flash SPI (W25Qxx, 25Cxx)
- **Pin 1 (RA2/CS)** → Pin /CS de la Flash
- **Pin 2 (RA3/SCK)** → Pin CLK de la Flash
- **Pin 4 (RA5/MISO)** → Pin DO de la Flash
- **Pin 15 (RA6/MOSI)** → Pin DI de la Flash

---

## 🚀 Guía de Uso Rápida

### Paso 1: Preparación del Hardware

1. **Programa el PIC16F628A**:
   - Descarga el archivo `pic_firmware_v3.hex` de este repositorio
   - Usa tu programador favorito (PICkit2/3/4, TL866, etc.)
   - Configura los fuses según la configuración del código fuente

2. **Monta el circuito**:
   - Conecta el PIC según el diagrama de pines
   - Instala las resistencias pull-up para I2C (4.7kΩ)
   - Conecta el LED indicador con su resistencia (330Ω)
   - Conecta la memoria que deseas programar

3. **Alimentación**:
   - Alimenta el circuito con 5V (desde el adaptador USB-Serial o fuente externa)
   - Verifica que el LED parpadee 2 veces al encender (señal de inicio correcto)

### Paso 2: Instalación de la App

1. Descarga la última versión desde Google Play Store
2. Instala en tu dispositivo Android
3. Concede permisos de USB y almacenamiento cuando se soliciten

### Paso 3: Conexión y Uso

1. **Conectar**:
   - Conecta el adaptador USB-Serial al puerto OTG de tu Android
   - Abre la aplicación OTG Flash EEPROM
   - Presiona el botón "Conectar"
   - Acepta el permiso USB
   - Verás "Conectado: 9600 bps" en verde

2. **Leer una Memoria**:
   - Selecciona el protocolo (I2C o SPI)
   - Elige el modelo exacto de tu memoria del listado
   - Presiona "Leer Memoria"
   - El visor hexadecimal mostrará los datos en tiempo real
   - Los archivos se guardarán automáticamente en `Descargas/rom/`

3. **Escribir una Memoria**:
   - Selecciona el protocolo y modelo de memoria
   - Presiona "Cargar Archivo (.bin / .hex)"
   - Selecciona tu archivo de firmware
   - Verifica los datos en el visor de previsualización
   - Presiona "Escribir Memoria"
   - Espera a que finalice (el LED del PIC parpadeará al completar)

### Paso 4: Terminal Serial (Avanzado)

Para usuarios avanzados, la aplicación incluye una terminal serial que permite enviar comandos directos al firmware PIC:

- Abre el menú y selecciona "Terminal"
- Configura el modo HEX para enviar comandos binarios
- Consulta el documento `PICMEM_v3_Protocolo.md` para comandos disponibles

---

## 📖 Documentación Detallada

### Protocolo de Comunicación

El sistema utiliza un protocolo binario eficiente documentado en detalle en:
- **[PICMEM_v3_Protocolo.md](PICMEM_v3_Protocolo.md)** - Especificación completa del protocolo

Este documento incluye:
- Descripción de todos los comandos disponibles
- Formato de bytes y parámetros
- Ejemplos de uso desde terminal
- Tiempos de operación estimados
- Guía de resolución de problemas

### Código Fuente del Firmware

- **[pic_firmware_v3.c](pic_firmware_v3.c)** - Código fuente completo en C
- **[pic_firmware_v3.hex](pic_firmware_v3.hex)** - Firmware precompilado listo para grabar

**Compilación del firmware**:
```bash
sdcc --std-c99 -mpic14 -p16f628a --use-non-free pic_firmware_v3.c
```

O usa la aplicación **C PIC Compiler** disponible en Play Store para compilar directamente desde Android.

---

## 🏗️ Arquitectura del Código Android

El proyecto sigue principios de Programación Orientada a Objetos (POO) con separación clara de responsabilidades:

### Estructura de Paquetes

```
com.mobincube.pronosticos_parley_copy.sc_55UCEB/
├── usb/
│   └── UsbSerialManager.java          # Gestión de comunicación USB OTG
├── eeprom/
│   ├── EepromProtocol.java            # Interfaz abstracta de protocolo
│   ├── I2cProtocol.java               # Implementación protocolo I2C
│   └── SpiProtocol.java               # Implementación protocolo SPI
├── file/
│   ├── FileManager.java               # Gestión de archivos (save/load)
│   └── IntelHexFormat.java            # Parser formato Intel HEX
├── ui/
│   ├── MainActivity.java              # Actividad principal
│   ├── TerminalActivity.java          # Terminal serial
│   ├── HexViewerHelper.java           # Visor hexadecimal
│   └── LogHelper.java                 # Sistema de logging
└── exception/
    └── HexFormatException.java        # Excepciones personalizadas
```

### Componentes Clave

#### 1. Capa de Comunicación USB
- **UsbSerialManager**: Maneja permisos, configuración 9600 8N1, lectura/escritura de bytes

#### 2. Capa de Protocolos
- **I2cProtocol**: Calcula direcciones de bloque para memorias 24Cxx, gestiona page writes
- **SpiProtocol**: Maneja comandos JEDEC ID, status register, read/write para Flash SPI

#### 3. Capa de Archivos
- **FileManager**: Guardado en `Descargas/rom/`, generación de nombres únicos con timestamp
- **IntelHexFormat**: Parser estricto con validación de checksums, extracción de datos limpios

#### 4. Capa de UI
- **MainActivity**: Control de estado, progress bars, gestión de operaciones asíncronas
- **HexViewerHelper**: Renderizado optimizado con throttling para evitar congelamiento
- **TerminalActivity**: Envío/recepción en modo texto y hexadecimal

---

## 🎨 Características de la Interfaz

- **Material Design 3**: Interfaz moderna y fluida
- **Temas claro/oscuro**: Adaptación automática al sistema
- **Visor hex en tiempo real**: Actualización optimizada sin bloqueos
- **Progress bars detalladas**: Información de progreso por páginas
- **Logs visuales**: Depuración en pantalla para usuarios avanzados
- **Selección de memoria guiada**: Menús desplegables con modelos comunes

---

## ⚠️ Consideraciones Importantes

### Seguridad y Respaldos

- **SIEMPRE haz respaldo** antes de escribir en una memoria
- Verifica el modelo exacto de tu memoria antes de programar
- Las memorias Flash SPI requieren borrado previo (sector o chip completo)
- El borrado completo de chip (Chip Erase) es irreversible

### Compatibilidad

- **Android 6.0+** con soporte USB OTG
- **Adaptadores USB-Serial soportados**: CH340, CH341, CP210x, FTDI FT232, PL2303
- **Velocidad fija**: 9600 baudios (no modificable por limitación del firmware)

### Limitaciones Conocidas

- Velocidad de transferencia limitada a 9600 baud (aprox. ~1 KB/s)
- Memorias Flash grandes (>4MB) pueden tardar varios minutos
- Chip Erase en Flash >4MB puede dar timeout (pero continúa en background)
- Máximo 65535 bytes por comando individual

---

## 🐛 Resolución de Problemas

### El PIC no responde

- **Problema**: App muestra "Error de comunicación" o timeouts
- **Solución**: 
  - Verifica las conexiones RX/TX (deben estar cruzadas)
  - Confirma alimentación de 5V estable
  - Prueba comando PING (3F) desde terminal
  - Verifica que el firmware esté correctamente grabado

### I2C no detecta la memoria

- **Problema**: Escaneo I2C no encuentra dispositivos
- **Solución**:
  - Verifica resistencias pull-up de 4.7kΩ en SDA y SCL
  - Confirma conexión de SDA y SCL a pines correctos
  - Prueba con otro chip I2C conocido (ej: DS1307 RTC)
  - Mide voltaje en SDA/SCL (debe ser ~5V en reposo)

### SPI Flash devuelve FF FF FF

- **Problema**: JEDEC ID devuelve FF FF FF o 00 00 00
- **Solución**:
  - Revisa todas las conexiones SPI (CS, SCK, MOSI, MISO)
  - Verifica polaridad de VCC y GND
  - Confirma que la Flash no esté protegida contra escritura
  - Prueba leyendo Status Register (debe dar respuesta válida)

### Escritura no se guarda

- **Problema**: Datos escritos no persisten después de leer
- **Solución**:
  - **Flash SPI**: Borra el sector/chip ANTES de escribir (las Flash NOR requieren estar en 0xFF)
  - **EEPROM I2C**: Respeta los tamaños de página según modelo
  - Espera tiempo de escritura (5-10ms EEPROM, 100ms+ Flash)

---

## 📜 Licencia

Este proyecto se distribuye bajo la **[Licencia Apache 2.0](LICENSE)**.

Esto significa que puedes:
- ✅ Usar el software comercialmente
- ✅ Modificar el código fuente
- ✅ Distribuir versiones modificadas
- ✅ Usar en proyectos privados
- ✅ Conceder sublicencias

Con las siguientes condiciones:
- 📄 Incluir copia de la licencia y copyright
- 📝 Documentar cambios realizados al código original
- 🔒 No usar marcas registradas del proyecto sin permiso

---

## 👨‍💻 Autor

**Daniel Diamon**
- Email: [danielpdiamon@gmail.com](mailto:danielpdiamon@gmail.com)
- GitHub: [github.com/Danielk10](https://github.com/Danielk10)

---

## 🤝 Contribuciones

¡Las contribuciones son bienvenidas! Si quieres mejorar el proyecto:

1. Haz fork del repositorio
2. Crea una rama para tu feature (`git checkout -b feature/nueva-caracteristica`)
3. Commit tus cambios (`git commit -am 'Agregar nueva característica'`)
4. Push a la rama (`git push origin feature/nueva-caracteristica`)
5. Abre un Pull Request

### Áreas donde puedes contribuir

- 🔧 Soporte para nuevos modelos de memoria
- ⚡ Optimización de velocidad de transferencia
- 🌐 Traducciones a otros idiomas
- 📱 Mejoras en la interfaz de usuario
- 🐛 Reportes y corrección de bugs
- 📚 Documentación y tutoriales

---

## 🔗 Enlaces Útiles

- **Documentación del Protocolo**: [PICMEM_v3_Protocolo.md](PICMEM_v3_Protocolo.md)
- **Código Fuente Firmware**: [pic_firmware_v3.c](pic_firmware_v3.c)
- **Firmware Compilado**: [pic_firmware_v3.hex](pic_firmware_v3.hex)
- **Repositorio GitHub**: [https://github.com/Danielk10/Comunicador-OTG-TTL-Android](https://github.com/Danielk10/Comunicador-OTG-TTL-Android)

---

## ⭐ Agradecimientos

- Proyecto basado en **usb-serial-for-android** de mik3y
- Documentación de protocolo I2C: Microchip 24Cxx datasheets
- Documentación de protocolo SPI: Winbond W25Qxx datasheets
- Compilador SDCC: Small Device C Compiler Team

---

## 📊 Estado del Proyecto

- ✅ Soporte completo I2C (24C01 a 24C512 - hasta 2 Mbit / 256 KB)
- ✅ Soporte completo SPI Flash NOR (W25Q08 a W25Q128 - hasta 16 MB)
- ✅ Soporte EEPROM SPI (25LC series - hasta 4 MB)
- ✅ Parser Intel HEX con validación
- ✅ Visor hexadecimal en tiempo real
- ✅ Terminal serial integrada
- ✅ Exportación .bin y .hex
- 🔄 En desarrollo: Soporte Microwire (93Cxx)
- 🔄 En desarrollo: Editor hexadecimal

---

**¿Te gusta el proyecto? ¡Dale una ⭐ en GitHub!**

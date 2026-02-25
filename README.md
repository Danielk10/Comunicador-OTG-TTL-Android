# 📱 Comunicador OTG TTL Android

![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)
![Android](https://img.shields.io/badge/Android-Ready-green.svg)
![Hardware](https://img.shields.io/badge/Hardware-PIC16F628A-orange.svg)

**Comunicador OTG TTL** es una herramienta avanzada de ingeniería de hardware para Android. Su propósito principal es convertir un dispositivo Android en un **Programador y Lector universal de memorias EEPROM (I2C y SPI)** y en una **Terminal Serial de diagnóstico**.

Para lograr esto, la aplicación utiliza una arquitectura de "puente" de dos partes:
1. **La App Android (El Cerebro):** Maneja la interfaz de usuario, procesa archivos `.hex` o `.bin`, y se comunica por USB OTG usando un adaptador Serial (CH340, CP2102, FTDI, etc.).
2. **El Microcontrolador PIC16F628A (El Intérprete):** Actúa como intermediario. Recibe comandos seriales (UART) sencillos desde Android y los traduce a señales eléctricas de alta velocidad (I2C o SPI) mediante técnicas de *Bit-Banging* para comunicarse físicamente con los chips de memoria.

---

## 🧠 Arquitectura Interna

El proyecto está rigurosamente estructurado siguiendo principios POO (Programación Orientada a Objetos):

### 1. Capa de Comunicación (`.usb`)
* **`UsbSerialManager`**: El motor físico. Solicita permisos de Android, negocia la conexión nativa a **9600 baudios (8N1)** y expone métodos para que la App envíe y reciba arrays de bytes.

### 2. Capa de Dominio / Protocolos (`.eeprom`)
* **`EepromProtocol`**: Interfaz abstracta.
* **`I2cProtocol`**: Matemática para hablar con memorias **24Cxx**. Inyecta dinámicamente direcciones de bloque (Block Select P0/P1) para memorias grandes.
* **`SpiProtocol`**: Maneja memorias **25Cxx**. Calcula direcciones de 16-bits o 24-bits dinámicamente enviando los "OpCodes" correctos (0x03, 0x02) y habilitando la escritura.

### 3. Capa de Archivos (`.file` & `.exception`)
* **`FileManager`**: Guarda de forma limpia las extracciones de memoria en la carpeta `Descargas/rom` generando archivos `.bin` y `.hex`.
* **`IntelHexFormat`**: Parsea archivos `.hex` estrictamente. Verifica sumas de control (Checksums) y aísla los bytes de datos útiles defendiendo la aplicación contra archivos corruptos.

### 4. Capa Gráfica Visual (`.ui`)
* **`MainActivity`**: Interfaz principal. Delega tareas sucias, maneja el control de estado de lectura/escritura y el avance del progreso dividiendo la carga en "Páginas".
* **`HexViewerHelper`**: Visor de datos hexadecimal asíncrono con límite de cuadros (framerate throttling) para evitar que la interfaz se congele.
* **`LogHelper`**: Caja de registro visual ("Log") para depuración en pantalla.
* **`TerminalActivity`**: Terminal serial clásica como herramienta de diagnóstico.

---

## 🛠️ Guía Rápida de Uso

### Paso 1: Hardware
1. Conecta los pines `TX`/`RX` del adaptador USB-Serial a los pines `RB1`/`RB2` del PIC16F628A.
2. Alimenta el sistema con el voltaje requerido por la memoria (usualmente +5V o +3.3V).
3. **Memorias I2C (24Cxx)**: SDA a `RA0`, SCL a `RA1` (Usa resistencias Pull-Up de 4.7kΩ).
4. **Memorias SPI (25Cxx)**: CS a `RA2`, SCK a `RA3`, MISO a `RA5`, MOSI a `RA6`.

### Paso 2: Conectar
1. Conecta el adaptador usando un cable OTG al teléfono.
2. Abre la App y acepta los permisos USB.
3. Presiona **Conectar**. El estado debe cambiar a "Conectado: 9600 bps" en verde.

### Paso 3: Leer Memoria (Dumping)
1. Selecciona el protocolo (I2C o SPI) y la familia exacta de tu chip en el listado.
2. Presiona **Leer Memoria**. El **Visor Hexadecimal** mostrará la memoria en tiempo real.
3. Al finalizar, tus archivos se habrán guardado en la carpeta de `Descargas/rom/` de tu teléfono.

### Paso 4: Escribir Firmware
1. Sigue los pasos para seleccionar tu chip.
2. Presiona **Cargar Archivo Local (.bin / .hex)** y selecciona tu archivo.
3. Verifica los datos en el Visor Hexadecimal de pre-visualización.
4. Presiona **Escribir Memoria**. La app escribirá asincrónicamente con verificación del PIC chip.

---

## 📄 Licencia

Este proyecto se distribuye bajo la licencia **[Apache License 2.0](LICENSE)**.
Garantiza el uso, modificación y distribución abierta incluyendo cualquier garantía legal de la autoría.

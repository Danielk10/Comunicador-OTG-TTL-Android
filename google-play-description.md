# Descripción para Google Play Store

## Descripción Corta (80 caracteres máximo)

```
Crea tu propio programador universal de memorias EEPROM I2C/SPI usando PIC16F628A por USB OTG
```

---

## Descripción Larga (4000 caracteres máximo)

```
OTG FLASH EEPROM - PROGRAMADOR UNIVERSAL DE MEMORIAS EEPROM

¿Quieres leer, escribir y programar memorias EEPROM I2C y Flash SPI directamente desde tu Android? ¡OTG Flash EEPROM es la solución que necesitas!

Convierte tu smartphone en un programador universal de memorias utilizando un microcontrolador PIC16F628A como puente de comunicación. Ideal para electrónica, reparación de dispositivos, desarrollo de firmware y proyectos DIY.

✨ CARACTERÍSTICAS PRINCIPALES

✅ SOPORTE MUTIPLE DE PROTOCOLOS
• Memorias I2C: Serie 24Cxx (24C01 hasta 24C512)
• Flash SPI: Winbond W25Qxx, Macronix MX25Lxx
• EEPROM SPI: Serie 25LCxx, ST M95xxx
• Rangos: Desde 128 bytes hasta 16 MB

📖 LECTURA COMPLETA
• Extrae el contenido completo de cualquier memoria
• Visor hexadecimal en tiempo real
• Exportación automática en formato .bin y .hex
• Guardado en Descargas/rom/ de tu dispositivo
• Volcado rápido con detección automática de tamaño

✍️ ESCRITURA
• Soporta archivos Intel HEX (.hex) y binarios (.bin)
• Validación automática de checksums
• Escritura por páginas con verificación
• Control de progreso en tiempo real

🔧 CONSTRUYE TU PROPIO PROGRAMADOR

La aplicación utiliza un PIC16F628A como puente USB-Serial a I2C/SPI. El firmware completo está disponible en el repositorio de GitHub del proyecto.

Componentes necesarios (bajo costo):
• 1x PIC16F628A (microcontrolador)
• 1x Adaptador USB-Serial (CH340, CP2102 o FTDI)
• 2x Resistencias 4.7kΩ (pull-up para I2C)
• 1x LED + resistencia 330Ω (indicador)
• 1x Cable OTG para tu Android
• Protoboard y cables

Esquemas de conexión, código fuente y firmware compilado disponibles en:
https://github.com/Danielk10/Comunicador-OTG-TTL-Android

🛠️ ARQUITECTURA

• Separación clara de capas (USB, Protocolos, Archivos, UI)
• Parser Intel HEX con validación estricta
• Gestión optimizada de memoria
• Visor hex sin bloqueos con throttling
• Material Design 3

💻 APLICACIONES PRÁCTICAS

✓ Backup de firmware de routers y dispositivos IoT
✓ Programación de memorias en sistemas embebidos
✓ Reparación de televisores y electrodomésticos
✓ Desarrollo de proyectos con Arduino/PIC
✓ Recuperación de datos de memorias extraídas
✓ Educación en electrónica digital

🔌 COMPATIBILIDAD

• Android 6.0 (API 23) o superior
• Soporte USB OTG obligatorio
• Adaptadores soportados: CH340, CH341, CP210x, FTDI FT232, PL2303
• Velocidad de comunicación: 9600 baudios 8N1

📚 DOCUMENTACIÓN

• Diagramas de conexión del hardware
• Código fuente del firmware en C (SDCC)

🔓 PROYECTO OPEN SOURCE

Esta aplicación es completamente de código abierto bajo licencia Apache 2.0:
• Código fuente completo de la app Android
• Firmware del PIC16F628A en C
• Documentación del protocolo
• Esquemas de conexión
• Sin restricciones para uso comercial o educativo

Ver en su repositorio GitHub:
https://github.com/Danielk10/Comunicador-OTG-TTL-Android

⚠️ IMPORTANTE

• Requiere construir el hardware PIC16F628A (ver documentación)
• Conocimientos básicos de electrónica recomendados
• Siempre haz backup antes de escribir memorias
• Verifica el voltaje de alimentación de tus memorias (3.3V o 5V)
• Las memorias Flash SPI requieren borrado previo

¡Descarga ahora y comienza a programar memorias EEPROM desde tu Android!

Perfecto para técnicos en electrónica, reparadores, estudiantes de ingeniería, makers y desarrolladores de hardware.

```

---

## Capturas de Pantalla Sugeridas (7-8 imágenes)

1. **Pantalla principal** - Mostrando selección de protocolo y memoria
2. **Visor hexadecimal** - Datos de lectura en tiempo real
3. **Proceso de escritura** - Barra de progreso y confirmación
4. **Terminal serial** - Comandos y respuestas en modo HEX
5. **Selección de archivo** - Carga de .hex/.bin
6. **Configuración de memoria** - Lista de memorias soportadas
7. **Estado de conexión** - Indicador USB conectado
8. **Diagrama de hardware** - Conexiones del PIC16F628A

---

## Video Promocional (Sugerencia de Contenido - 30 segundos)

1. **0-5s**: Logo de la app + texto "OTG Flash EEPROM"
2. **5-10s**: Montaje rápido del hardware PIC + conexión USB OTG
3. **10-15s**: Lectura de memoria con visor hex en tiempo real
4. **15-20s**: Escritura de firmware con barra de progreso
5. **20-25s**: Terminal serial enviando comandos
6. **25-30s**: Texto final "100% Open Source" + GitHub link

---

## Palabras Clave para ASO (App Store Optimization)

**Primarias:**
- EEPROM programmer
- Flash memory programmer
- I2C SPI reader
- PIC programmer
- USB OTG programmer

**Secundarias:**
- 24C programmer
- W25Q flasher
- Serial memory tool
- Firmware backup
- BIOS reader
- Electronics repair
- Embedded systems
- DIY electronics
- Hardware hacking
- Memory dump

**Long-tail:**
- Android EEPROM programmer app
- Read write 24C256 Android
- SPI Flash dump Android OTG
- PIC16F628A USB bridge
- CH340 serial programmer
- Intel HEX programmer Android

---

## Categoría en Google Play

**Primaria:** Herramientas
**Secundaria:** Educación

---

## Clasificación de Contenido

- **PEGI:** 3+ (apto para todas las edades)
- **ESRB:** Everyone
- **Sin contenido sensible**
- **Herramienta técnica educativa**

---

## Información Adicional para Google Play

### ¿Qué hay de nuevo? (Release Notes)

**Versión 1.0.1 (Actual)**
• Lanzamiento inicial
• Soporte completo I2C (24Cxx series)
• Soporte completo SPI Flash (W25Qxx, MX25Lxx)
• Soporte EEPROM SPI (25LCxx)
• Terminal serial integrada
• Visor hexadecimal optimizado
• Parser Intel HEX con validación
• Exportación .bin y .hex
• Material Design 3
• Soporte temas claro/oscuro

### Contacto del Desarrollador

- **Email:** danielpdiamon@gmail.com
- **Sitio Web:** https://github.com/Danielk10/Comunicador-OTG-TTL-Android
- **Política de Privacidad:** https://todoandroid.42web.io/politica-de-privacidad.html

### Permisos Requeridos (con Justificación)

1. **USB Permission** - Esencial para comunicarse con adaptador USB-Serial
2. **Read/Write External Storage** - Para cargar archivos .hex/.bin y guardar dumps
3. **Internet** - Solo para anuncios AdMob (funcionalidad principal no requiere Internet)
4. **Access Network State** - Verificar conectividad para AdMob

---

## Estrategia de Monetización

**Modelo Actual:** Anuncios AdMob
- Banners no intrusivos
- Sin interrupciones en operaciones críticas
- Sin paywall en funcionalidades

**Modelo Futuro (Opcional):**
- Versión Pro sin anuncios
- Funciones avanzadas: editor hexadecimal, scripts automatizados
- Soporte prioritario

---

## FAQ para Usuarios

**P: ¿Necesito conocimientos de electrónica?**
R: Se recomienda conocimiento básico. La documentación incluye diagramas detallados.

**P: ¿Dónde consigo el hardware?**
R: Los componentes están disponibles en tiendas de electrónica. Lista completa en GitHub.

**P: ¿Funciona sin Internet?**
R: Sí, toda la funcionalidad principal es offline. Internet solo para anuncios.

**P: ¿Es seguro?**
R: Sí, código 100% open source auditable. No recopila datos personales.

**P: ¿Soporta mi adaptador USB?**
R: Compatible con CH340, CP2102, FTDI, PL2303 y chips similares.

**P: ¿Puedo dañar mi memoria?**
R: Si se usa correctamente, no. Siempre haz backup y verifica voltajes.
```
# Notas de Lanzamiento - OTG Flash EEPROM v1.2.0

Esta versión oficial (**v1.2.0**, código de versión **6**) presenta un rediseño completo de la interfaz de usuario hacia un formato compacto en una sola pantalla sin scroll, integra publicidad de Google AdMob (banner y nativo) en estricto cumplimiento con las políticas y directrices 2026 de Google, y genera la compilación oficial de producción firmada con el almacén de claves `eeprom.jks`.

---

## 📱 Nueva Interfaz Compacta (Single Screen sin Scroll)

- **Diseño sin Scroll General**: Se eliminó el scroll vertical de la pantalla principal para mantener accesibles y visibles en todo momento todos los controles esenciales en cualquier tamaño de dispositivo.
- **Barra de Conexión en Fila Única**: Indicador de estado de conexión, etiqueta y botones de conexión/desconexión unificados horizontalmente en una tarjeta compacta de 48dp.
- **Configuración de Protocolo y Modelo en Columnas Paralelas**: Selección de protocolo (I2C / SPI) y modelo/capacidad de memoria dispuestos lado a lado para optimizar el espacio vertical.
- **Botonera de Acciones en Cuadrícula Eficiente**: 
  - Fila 1: *Leer*, *Escribir*, *Borrar*, *Verificar*.
  - Fila 2: *Escanear*, *Full Dump*, *Exportar .bin / .hex*.
- **Terminal de Registro Dinámica**: El visor de log ocupa todo el espacio vertical sobrante (`layout_weight="1"`) con scroll interno independiente para revisar las trazas de comunicación con el hardware.

---

## 📢 Integración Google AdMob (Políticas y Dimensiones 2026)

- **Banner en Parte Inferior**: Anuncio tipo Banner (`AdSize.BANNER`) centrado en el fondo de la pantalla (`ca-app-pub-5141499161332805/7125155396`).
- **Anuncio Nativo Integrado**: 
  - Ad Unit: `ca-app-pub-5141499161332805/7975202632`.
  - Ubicación estratégica entre la botonera de acciones y el log de operaciones.
  - Diseño nativo integrado visualmente con la paleta oscura de la aplicación (`#0D1117`, `#161B22`, `#30363D`, `#238636`).
  - Cumplimiento 100% con políticas de Google: Distintivo oficial de anuncio (`Ad`), tipografía proporcional, botón de llamada a la acción (CTA) diferenciado e inicialización asíncrona que oculta el contenedor hasta su carga efectiva.
- **Optimización de SDK**: Banderas `OPTIMIZE_INITIALIZATION` y `OPTIMIZE_AD_LOADING` activadas en el manifiesto junto al permiso `AD_ID`.

---

## 🔐 Firma de Producción Oficial

- Almacén de claves: `/home/danielpdiamon/eeprom.jks`
- Alias de producción: `eeprom`
- Esquemas de firma verificados: **v1** (JAR signing) y **v2** (Full APK signature).
- Compatibilidad: Android 6.0 (API 23) a Android 17 (API 37).

---

## 📦 Artefactos de la Versión

- **`app-release.apk`**: Paquete APK oficial firmado para distribución e instalación directa en dispositivos físicos.
- **`app-release.aab`**: Android App Bundle firmado para publicación en Google Play Console.

# Notas de Lanzamiento - OTG Flash EEPROM v1.1.0

Esta versión oficial (`v1.1.0`, versión de compilación `5`) actualiza integralmente la infraestructura de compilación del proyecto, moderniza el soporte del SDK de Android, actualiza las librerías del sistema e introduce firma criptográfica y optimización de entornos temporales.

---

## 🚀 Nuevas Características y Mejoras

* **Actualización del SDK de Android y Plataformas:**
  - `compileSdk` y `targetSdk` actualizados a **API 37 (Android 17 / Cinnamon Bun)**.
  - Compatibilidad mantenida desde `minSdk 23` (Android 6.0 Marshmallow).
  - Android Build Tools actualizado a **37.0.0**.
  - Android NDK actualizado a **30.0.14904198 rc1** (`30.0.14904198`).
  - CMake actualizado a **4.1.2**.

* **Modernización del Toolchain de Gradle:**
  - Android Gradle Plugin (AGP) actualizado a **9.2.1**.
  - Gradle Wrapper actualizado a **9.6.0**.
  - `org.gradle.configuration-cache=true` habilitado para acelerar compilaciones incrementales.

* **Actualización de Librerías y Dependencias:**
  - Actualización de `usb-serial-for-android` a la versión **3.11.0** para mayor estabilidad en la comunicación USB-Serial OTG (CH340, FTDI, CP210x, PL2303).
  - Componentes AndroidX y Material Design actualizados (`appcompat:1.7.1`, `material:1.14.0`, `constraintlayout:2.2.1`, `activity:1.13.0`).

* **Configuración de Firma Release Oficial:**
  - Integración de firma release automatizada mediante `keystore.properties` (`keystore.jks` con alias `eeprom`).
  - Soporte para variables de entorno de firma (`SIGNING_STORE_FILE`, etc.) para pipelines de CI/CD.
  - Protección estricta en `.gitignore` para prevenir filtraciones de credenciales.

* **Redirección de Almacenamiento y Caché en `/tmp`:**
  - Redirección completa de la carpeta de compilación hacia `/tmp/calculo` para mantener limpio el espacio de trabajo.
  - Redirección automática de la caché de Gradle (`GRADLE_USER_HOME`) a `/tmp/.gradle` para optimizar entornos con disco persistente limitado (como Cloud Shell).

* **Documentación Técnica Integral:**
  - Incorporación de `GEMINI.md` con las instrucciones completas de instalación del SDK y compilación.
  - Actualización del `README.md` con la nueva sección de compilación y enlaces oficiales.

---

## 📦 Artefactos de la Versión

- **`app-release.apk`**: Paquete APK firmado y listo para instalación directa en dispositivos Android (API 23+).
- **`app-release.aab`**: Android App Bundle firmado y optimizado para distribución en Google Play Store.

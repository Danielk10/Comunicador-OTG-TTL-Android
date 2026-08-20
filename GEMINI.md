# Instrucciones de Compilación, SDK y Configuración del Proyecto

Este documento describe la configuración técnica, instalación del SDK de Android, gestión de dependencias, configuración de firma y compilación del proyecto **Comunicador-OTG-TTL-Android (OTG Flash EEPROM)**.

---

## 1. Versiones y Configuración del Entorno

El proyecto se encuentra actualizado y alineado con los estándares modernos de Android:

- **Android SDK:**
  - `compileSdk`: 37 (Android 17 / Cinnamon Bun)
  - `targetSdk`: 37
  - `minSdk`: 23 (Android 6.0 Marshmallow)
- **Android Build Tools:** `37.0.0`
- **Android NDK:** `30.0.14904198 rc1` (`30.0.14904198`)
- **CMake:** `4.1.2`
- **Android Gradle Plugin (AGP):** `9.2.1`
- **Gradle:** `9.6.0` (Gradle Wrapper)
- **Java:** Compatibilidad Java 11 (compilación con OpenJDK 21)
- **Librería USB Serial:** `usb-serial-for-android` versión `3.11.0`
- **AndroidX & UI:** `appcompat:1.7.1`, `material:1.14.0`, `constraintlayout:2.2.1`, `activity:1.13.0`

---

## 2. Instalación del SDK de Android y NDK

Para preparar el entorno de compilación de forma limpia (especialmente optimizado para entornos como Google Cloud Shell con espacio persistente limitado), se proporciona el script automatizado `setup-sdk.sh`.

### Ejecutar el script:
```bash
bash setup-sdk.sh
```

### ¿Qué hace el script?
1. Descarga las herramientas de línea de comandos de Android (`cmdline-tools` versión `13114758`).
2. Instala el SDK, NDK (`30.0.14904198`), Build-Tools (`37.0.0`), Platform-Tools y Plataformas Android (23 y 37) en la ruta temporal `/tmp/android-sdk`.
3. Acepta automáticamente las licencias oficiales de Android.
4. Genera el archivo `local.properties` apuntando a `sdk.dir=/tmp/android-sdk`.
5. Asegura los permisos de ejecución del Gradle Wrapper (`chmod +x gradlew`).

---

## 3. Redirección de Rutas de Salida y Caché en `/tmp`

Para evitar saturar el almacenamiento local persistente:

- **Carpeta de Compilación (Build Directory):** Configurada en `app/build.gradle` para redirigir toda la compilación a:
  ```
  /tmp/calculo
  ```
- **Ruta de salida de APKs generados:**
  - Debug: `/tmp/calculo/outputs/apk/debug/app-debug.apk`
  - Release (firmado): `/tmp/calculo/outputs/apk/release/app-release.apk`
- **Ruta de salida de AAB (Android App Bundle para Google Play Store):**
  - Release (firmado): `/tmp/calculo/outputs/bundle/release/app-release.aab`
- **Caché de Gradle y Configuration Cache:**
  - `org.gradle.configuration-cache=true` habilitado en `gradle.properties`.
  - Se puede redirigir el directorio global de caché de Gradle mediante la variable de entorno:
    ```bash
    export GRADLE_USER_HOME=/tmp/.gradle
    ```

---

## 4. Configuración de Firma (Signing Configs)

La firma de la aplicación está completamente configurada para generar paquetes Release firmados listos para distribución o publicación en Google Play Store.

### Credenciales de la Clave:
- **Archivo Keystore:** `/ruta/a/tu/keystore.jks`
- **Alias:** `tu_alias`
- **Contraseña de Keystore / Store Password:** `********`
- **Contraseña de Alias / Key Password:** `********`

### Configuración en `keystore.properties`:
En la raíz del proyecto se crea el archivo `keystore.properties`:
```properties
storeFile=/ruta/a/tu/keystore.jks
storePassword=********
keyAlias=tu_alias
keyPassword=********
```

### Soporte en CI/CD y Variables de Entorno:
Si `keystore.properties` no está presente (por ejemplo en pipelines de CI/CD), `app/build.gradle` lee automáticamente las siguientes variables de entorno:
- `SIGNING_STORE_FILE`
- `SIGNING_STORE_PASSWORD`
- `SIGNING_KEY_ALIAS`
- `SIGNING_KEY_PASSWORD`

### Seguridad y `.gitignore`:
El archivo `.gitignore` está configurado para excluir estrictamente:
- `keystore.properties`
- `*.jks` y `*.keystore`
- `local.properties` y `/local.properties`
- `*.apk` y `*.aab`
- Archivos `.json` de credenciales
- Archivos y directorios temporales de compilación (`/build`, `.gradle`, etc.)

Esto previene que las credenciales o claves privadas sean subidas accidentalmente a GitHub.

---

## 5. Comandos de Compilación

Una vez instalado el SDK, compila el proyecto con Gradle:

### Compilar APK Debug:
```bash
./gradlew assembleDebug
```
*Salida:* `/tmp/calculo/outputs/apk/debug/app-debug.apk`

### Compilar APK Release (Firmado):
```bash
./gradlew assembleRelease
```
*Salida:* `/tmp/calculo/outputs/apk/release/app-release.apk`

### Compilar Android App Bundle (AAB para Google Play Store, Firmado):
```bash
./gradlew bundleRelease
```
*Salida:* `/tmp/calculo/outputs/bundle/release/app-release.aab`

### Limpiar compilación:
```bash
./gradlew clean
```

---

## 6. Publicación Automatizada en Google Play Store

El proyecto cuenta con el script oficial `upload_play_store.py` y la guía [GUIA_PUBLICACION_PLAY_STORE.md](GUIA_PUBLICACION_PLAY_STORE.md) para automatizar la publicación en Google Play Console mediante la API de Google Play Developer.

### Requisitos:
```bash
pip install google-api-python-client google-auth-httplib2 google-auth-oauthlib
```

### Ejecutar Publicación:
```bash
python upload_play_store.py \
  --package_name com.mobincube.keystore.jks_parley_copy.sc_55UCEB \
  --aab_path /tmp/calculo/outputs/bundle/release/app-release.aab \
  --service_account_json /ruta/a/tu/google-play-api.json \
  --track production \
  --release_notes "- Actualización a Android SDK 37 (Android 17) con compatibilidad mejorada.
- Actualización de la librería USB Serial a la versión 3.11.0 para mayor estabilidad en la conexión OTG.
- Mejoras internas de optimización, estabilidad y rendimiento del sistema." \
  --release_notes_en "- Updated to Android SDK 37 (Android 17) with improved compatibility.
- Updated USB Serial library to version 3.11.0 for enhanced OTG connection stability.
- Internal optimizations, stability and system performance improvements."
```


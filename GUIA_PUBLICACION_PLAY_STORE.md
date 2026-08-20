# Guía de Automatización para Google Play Store 🚀

Esta guía explica cómo utilizar el script `upload_play_store.py` para publicar actualizaciones automáticamente en la Google Play Store para el proyecto **OTG Flash EEPROM (Comunicador-OTG-TTL-Android)**.

---

## 🛠️ Requisitos Previos

Asegúrate de tener instaladas las dependencias oficiales de Google en Python:

```bash
pip install google-api-python-client google-auth-httplib2 google-auth-oauthlib
```

---

## 📂 Archivos Involucrados

1. **`upload_play_store.py`**: Script de publicación automatizada en Google Play Developer API.
2. **`pc-api-6650547003605444910-569-9d23413fdc95.json`**: Clave de la cuenta de servicio de Google Cloud (ubicada en `/home/danielpdiamon/` y protegida en `.gitignore`).

---

## 🚀 Cómo Usar el Script

Una vez generado y firmado el archivo `.aab` con `./gradlew bundleRelease`, ejecuta:

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

---

## 🎛️ Parámetros del Script

| Parámetro | Descripción | Valor por defecto / Ejemplo |
| :--- | :--- | :--- |
| `--package_name` | ID de la aplicación en Google Play | `com.mobincube.keystore.jks_parley_copy.sc_55UCEB` |
| `--aab_path` | Ruta absoluta al archivo `.aab` firmado | `/tmp/calculo/outputs/bundle/release/app-release.aab` |
| `--service_account_json` | Ruta al archivo JSON de credenciales de Google Cloud | `/ruta/a/tu/google-play-api.json` |
| `--track` | Pista de publicación en Google Play | `production`, `beta`, `alpha` o `internal` |
| `--release_notes` | Notas de versión estructuradas para español (`es-419` y `es-ES`) | `"- Cambio 1.\n- Cambio 2."` |
| `--release_notes_en` | (Opcional) Notas de versión estructuradas para inglés (`en-US`) | `"- Change 1.\n- Change 2."` |

---

## 🔑 Firma Release y Compilación Previa

Para generar el archivo `.aab` de producción:
```bash
./gradlew bundleRelease
```
El archivo de salida se generará en `/tmp/calculo/outputs/bundle/release/app-release.aab` firmado con las credenciales de `keystore.properties` (`/ruta/a/tu/keystore.jks`).

---

## 💡 Notas de Seguridad

- Los archivos `.jks`, `.keystore`, `.json` de cuentas de servicio y `keystore.properties` están estrictamente excluidos en `.gitignore` para evitar cualquier filtración de claves privadas o credenciales a GitHub.

# Notas de Lanzamiento - Comunicador-OTG-TTL-Android v1.3.0 (versionCode 8)

## 🌐 Notas para Google Play Console (Bilingüe)

### Español (`es-419` / `es-ES`)
- Copia ultrarrápida Zero-Copy con FileChannel.transferTo a nivel de kernel.
- Volcado de memoria optimizado con ByteBuffer y FileChannel sin duplicación en RAM.
- Protección continua con WakeLock en transferencias USB evitando interrupciones.
- Declaración oficial de hardware USB Host y largeHeap para volcados pesados.
- Firma oficial con keystore dedicado y compatibilidad completa con Android API 37.

### English (`en-US`)
- Ultra-fast kernel-level Zero-Copy file transfers via FileChannel.transferTo.
- Memory dumps optimized with direct ByteBuffer and FileChannel avoiding RAM duplication.
- Continuous WakeLock protection during USB operations preventing power drops.
- Official USB Host hardware feature declaration and largeHeap enabled.
- Production signing configured with dedicated keystore and full Android API 37 support.

---

## 🛠️ Detalle Técnico de Cambios

1. **Copia y Exportación Zero-Copy (`FileManager.java`)**:
   - `copyFileZeroCopy()` con `inChannel.transferTo()` a través de syscall `sendfile`.
   - `saveMemoryDump()` con `ByteBuffer` y `FileChannel` para archivos BIN y HEX.
2. **Protección WakeLock (`MainActivity.java`)**:
   - `PARTIAL_WAKE_LOCK` durante operaciones USB críticas.
3. **Manifiesto y Estabilidad (`AndroidManifest.xml`)**:
   - `<uses-feature android:name="android.hardware.usb.host" android:required="false" />`.
   - `android:largeHeap="true"`, `tools:targetApi="37"`.
   - Eliminación de `AlarmManagerSchedulerBroadcastReceiver` con `tools:node="remove"`.
4. **Firma y Configuración (`app/build.gradle` y `keystore.properties`)**:
   - `compileSdk release(37)`, `targetSdk 37`, `versionCode 8`, `versionName "1.3.0"`.
   - Configuración con `firma_otg_flash_eeprom.jks`.

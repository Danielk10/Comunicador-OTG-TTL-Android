# Comunicador OTG TTL Android

Aplicación Android para comunicación serial con microcontroladores mediante USB OTG (On-The-Go) usando la librería `usb-serial-for-android`.

## Descripción

Esta aplicación permite la comunicación bidireccional entre dispositivos Android y microcontroladores a través de una conexión USB OTG utilizando conversores USB-Serial TTL. Soporta múltiples chipsets populares como FTDI, CH340, CP210x, PL2303 y dispositivos CDC/ACM.

## Estado del proyecto

- ✅ Proyecto Android configurado con Gradle
- ✅ Dependencia `usb-serial-for-android` añadida via catálogo de versiones
- ✅ Estructura Java básica y UI creadas (MainActivity, UsbSerialManager, UsbSerialListener, layout, strings, device_filter)
- ⏳ Pendiente: pruebas en hardware real y ajustes finos de protocolo según tu microcontrolador

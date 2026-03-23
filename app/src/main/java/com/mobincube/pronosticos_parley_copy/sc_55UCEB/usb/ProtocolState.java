package com.mobincube.pronosticos_parley_copy.sc_55UCEB.usb;

public enum ProtocolState {
    IDLE,
    READING,
    WRITING,
    ERASING,
    VERIFYING,
    PINGING,
    SCANNING_ID,
    SCANNING_BOTH_I2C,   // Fase 1: escaneo I2C del scan combinado
    SCANNING_BOTH_SPI,   // Fase 2: JEDEC SPI del scan combinado
    FULL_DUMPING
}

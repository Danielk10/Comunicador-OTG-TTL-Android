package com.diamon.ttl.eeprom;

public class SpiProtocol implements EepromProtocol {

    private final int[] spiSizes = { 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536, 131072, 262144,
            524288 };

    @Override
    public byte getCommandPrefix() {
        return 'P';
    }

    @Override
    public byte[] buildReadCommand(int address, int length, int modelIdx) {
        byte addrLen = getAddrLen(modelIdx);
        byte spiOpcode = (byte) 0x03; // READ array
        if (modelIdx == 2) {
            spiOpcode |= ((address >> 8) & 0x01) << 3;
        }
        byte addrHi = (byte) ((address >> 16) & 0xFF);
        byte addrMid = (byte) ((address >> 8) & 0xFF);
        byte addrLo = (byte) (address & 0xFF);

        return new byte[] { getCommandPrefix(), 'R', addrLen, spiOpcode, addrHi, addrMid, addrLo, (byte) length };
    }

    @Override
    public byte[] buildWriteCommandBase(int address, int length, int modelIdx) {
        byte addrLen = getAddrLen(modelIdx);
        byte spiOpcode = (byte) 0x02; // WRITE array
        if (modelIdx == 2) {
            spiOpcode |= ((address >> 8) & 0x01) << 3;
        }
        byte addrHi = (byte) ((address >> 16) & 0xFF);
        byte addrMid = (byte) ((address >> 8) & 0xFF);
        byte addrLo = (byte) (address & 0xFF);

        return new byte[] { getCommandPrefix(), 'W', addrLen, spiOpcode, addrHi, addrMid, addrLo, (byte) length };
    }

    private byte getAddrLen(int modelIdx) {
        if (modelIdx < 3)
            return 1;
        if (modelIdx >= 10)
            return 3;
        return 2;
    }

    @Override
    public int getPageSize(int modelIndex) {
        if (modelIndex <= 1)
            return 8; // 25010 / 25020
        if (modelIndex <= 4)
            return 16; // 25040 / 80 / 160
        if (modelIndex <= 6)
            return 32; // 25320 / 25640
        if (modelIndex <= 8)
            return 64; // 25128 / 25256
        if (modelIndex == 9)
            return 128; // 25512
        return 256; // 1Mb, 2Mb, 4Mb
    }

    @Override
    public int getTotalSize(int modelIndex) {
        return spiSizes[modelIndex];
    }

    @Override
    public String getHardwareInstructions() {
        return "Conexiones SPI (25Cxx):\n" +
                "• PIC RA2  → EEPROM CS   (Pin 1)\n" +
                "• PIC RA5  → EEPROM MISO (Pin 2)\n" +
                "• PIC RA6  → EEPROM MOSI (Pin 5)\n" +
                "• PIC RA3  → EEPROM SCK  (Pin 6)\n" +
                "• PIC GND  → EEPROM GND  (Pin 4)\n" +
                "• PIC VCC  → EEPROM VCC  (Pin 8)\n\n" +
                "⚠ EXIGENCIAS DE HARDWARE:\n" +
                "- No requiere Pull-Ups en líneas de datos.\n" +
                "- WP / Write Protect (Pin 3) → Conectar a VCC.\n" +
                "- HOLD (Pin 7) → Conectar a VCC para evitar pausas.\n" +
                "- Tensión: Verificar si la memoria es de 3.3V o 5V.";
    }
}

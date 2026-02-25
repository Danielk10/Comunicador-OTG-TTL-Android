package com.diamon.ttl.eeprom;

public class I2cProtocol implements EepromProtocol {

    private final int[] i2cSizes = { 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536, 131072, 262144 };

    @Override
    public byte getCommandPrefix() {
        return 'I';
    }

    @Override
    public byte[] buildReadCommand(int address, int length, int modelIdx) {
        byte addrLen = (byte) (modelIdx <= 4 ? 1 : 2);
        byte chipAddr = generateChipAddr(address, modelIdx);
        byte addrHi = (byte) ((address >> 8) & 0xFF);
        byte addrLo = (byte) (address & 0xFF);

        return new byte[] { getCommandPrefix(), 'R', addrLen, chipAddr, addrHi, addrLo, (byte) length };
    }

    @Override
    public byte[] buildWriteCommandBase(int address, int length, int modelIdx) {
        byte addrLen = (byte) (modelIdx <= 4 ? 1 : 2);
        byte chipAddr = generateChipAddr(address, modelIdx);
        byte addrHi = (byte) ((address >> 8) & 0xFF);
        byte addrLo = (byte) (address & 0xFF);

        return new byte[] { getCommandPrefix(), 'W', addrLen, chipAddr, addrHi, addrLo, (byte) length };
    }

    private byte generateChipAddr(int address, int modelIdx) {
        byte chipAddr = (byte) 0xA0;
        if (modelIdx >= 2 && modelIdx <= 4) {
            byte blockBits = (byte) (((address >> 8) & 0x07) << 1);
            chipAddr |= blockBits;
        } else if (modelIdx >= 10) {
            byte blockBits = (byte) (((address >> 16) & 0x03) << 1);
            chipAddr |= blockBits;
        }
        return chipAddr;
    }

    @Override
    public int getPageSize(int modelIndex) {
        if (modelIndex <= 1)
            return 8; // 24C01 / 24C02
        if (modelIndex <= 4)
            return 16; // 24C04 / 24C08 / 24C16
        if (modelIndex <= 6)
            return 32; // 24C32 / 24C64
        if (modelIndex <= 9)
            return 64; // 24C128 / 256 / 512
        return 256; // 1Mb, 2Mb
    }

    @Override
    public int getTotalSize(int modelIndex) {
        return i2cSizes[modelIndex];
    }

    @Override
    public String getHardwareInstructions() {
        return "Conexiones I2C (24Cxx):\n" +
                "• PIC RA0  → EEPROM SDA (Pin 5)\n" +
                "• PIC RA1  → EEPROM SCL (Pin 6)\n" +
                "• PIC GND  → EEPROM GND (Pin 4)\n" +
                "• PIC VCC  → EEPROM VCC (Pin 8)\n\n" +
                "⚠ EXIGENCIAS DE HARDWARE:\n" +
                "- SDA y SCL REQUIEREN resistencias Pull-Up externas hacia VCC.\n" +
                "- Valor ideal: 4.7kΩ (rango aceptable: 2.2kΩ a 10kΩ).\n" +
                "- Pines A0, A1, A2 (1, 2, 3) → Conectar a GND.\n" +
                "- WP (Pin 7) → Conectar a GND para permitir escritura.";
    }
}

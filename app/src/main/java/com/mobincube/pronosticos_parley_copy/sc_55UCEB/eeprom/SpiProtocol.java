package com.mobincube.pronosticos_parley_copy.sc_55UCEB.eeprom;

public class SpiProtocol implements EepromProtocol {

    // ── FIX #6 ────────────────────────────────────────────────────────────────
    // Se añaden modelos Flash NOR (W25Q08 → W25Q128) en índices 13–17.
    // Índices  0–12 : EEPROM SPI 25Cxx / 25LCxx  (hasta 512 KB)
    // Índices 13–17 : Flash NOR W25Qxx / MX25Lxx (1 MB – 16 MB)
    // ─────────────────────────────────────────────────────────────────────────
    private final int[] spiSizes = {
        /*  0 */  128,        // 25Cxx  1Kb  (128 B)
        /*  1 */  256,        // 25Cxx  2Kb  (256 B)
        /*  2 */  512,        // 25Cxx  4Kb  (512 B)
        /*  3 */  1024,       // 25Cxx  8Kb  (1 KB)
        /*  4 */  2048,       // 25Cxx 16Kb  (2 KB)
        /*  5 */  4096,       // 25Cxx 32Kb  (4 KB)
        /*  6 */  8192,       // 25Cxx 64Kb  (8 KB)
        /*  7 */  16384,      // 25Cxx 128Kb (16 KB)
        /*  8 */  32768,      // 25Cxx 256Kb (32 KB)
        /*  9 */  65536,      // 25Cxx 512Kb (64 KB)
        /* 10 */  131072,     // 25Cxx   1Mb (128 KB)
        /* 11 */  262144,     // 25Cxx   2Mb (256 KB)
        /* 12 */  524288,     // 25Cxx   4Mb (512 KB)
        /* 13 */  1048576,    // W25Q08  / MX25L80   (1 MB)  <- Flash NOR
        /* 14 */  2097152,    // W25Q16  / MX25L16   (2 MB)
        /* 15 */  4194304,    // W25Q32  / MX25L32   (4 MB)
        /* 16 */  8388608,    // W25Q64  / MX25L64   (8 MB)
        /* 17 */  16777216    // W25Q128 / MX25L128  (16 MB)
    };

    @Override
    public byte getCommandPrefix() {
        return 'P';
    }

    @Override
    public byte[] buildReadCommand(int address, int length, int modelIdx) {
        byte addrLen   = getAddrLen(modelIdx);
        byte spiOpcode = (byte) 0x03; // READ array
        if (modelIdx == 2) {
            spiOpcode |= ((address >> 8) & 0x01) << 3;
        }
        byte addrHi  = (byte) ((address >> 16) & 0xFF);
        byte addrMid = (byte) ((address >>  8) & 0xFF);
        byte addrLo  = (byte) ( address        & 0xFF);
        byte lenHi   = (byte) ((length  >>  8) & 0xFF);
        byte lenLo   = (byte) ( length         & 0xFF);

        return new byte[] { getCommandPrefix(), 'R',
                addrLen, spiOpcode, addrHi, addrMid, addrLo, lenHi, lenLo };
    }

    @Override
    public byte[] buildWriteCommandBase(int address, int length, int modelIdx) {
        byte addrLen   = getAddrLen(modelIdx);
        byte spiOpcode = (byte) 0x02; // WRITE / PAGE PROGRAM
        if (modelIdx == 2) {
            spiOpcode |= ((address >> 8) & 0x01) << 3;
        }
        byte addrHi  = (byte) ((address >> 16) & 0xFF);
        byte addrMid = (byte) ((address >>  8) & 0xFF);
        byte addrLo  = (byte) ( address        & 0xFF);
        byte lenHi   = (byte) ((length  >>  8) & 0xFF);
        byte lenLo   = (byte) ( length         & 0xFF);

        return new byte[] { getCommandPrefix(), 'W',
                addrLen, spiOpcode, addrHi, addrMid, addrLo, lenHi, lenLo };
    }

    // ── FIX #6 ────────────────────────────────────────────────────────────────
    // Threshold corregido de < 10 a < 13.
    //   Indices  0-12: EEPROM SPI  -> sin Chip Erase nativo (borrado via 0xFF)
    //   Indices 13-17: Flash NOR   -> soportan 'P''E' (50 45 -> opcode 0xC7)
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public byte[] buildEraseCommand(int modelIndex) {
        if (modelIndex < 13) {
            return null; // EEPROM SPI: usar sobrescritura con 0xFF
        }
        return new byte[] { getCommandPrefix(), 'E' }; // Flash NOR: Chip Erase
    }

    @Override
    public byte[] buildPingCommand() {
        return new byte[] { 0x3F }; // '?'
    }

    @Override
    public byte[] buildScanOrIdCommand() {
        return new byte[] { getCommandPrefix(), 'J' }; // 'P' 'J' (JEDEC ID)
    }

    @Override
    public byte[] buildFullDumpCommand(int modelIndex) {
        return new byte[] { getCommandPrefix(), 'F' }; // 'P' 'F' (Full Auto Dump)
    }

    @Override
    public int getPageSize(int modelIndex) {
        if (modelIndex <= 1)  return 8;    // 25010 / 25020
        if (modelIndex <= 4)  return 16;   // 25040 / 25080 / 25160
        if (modelIndex <= 6)  return 32;   // 25320 / 25640
        if (modelIndex <= 8)  return 64;   // 25128 / 25256
        if (modelIndex == 9)  return 128;  // 25512
        if (modelIndex <= 12) return 256;  // 25Cxx 1Mb-4Mb
        return 256;                        // Flash NOR W25Qxx: 256 B/pagina
    }

    @Override
    public int getTotalSize(int modelIndex) {
        return spiSizes[modelIndex];
    }

    private byte getAddrLen(int modelIdx) {
        if (modelIdx <  3) return 1;   // 25Cxx <= 4Kb: 1 byte
        if (modelIdx < 13) return 2;   // 25Cxx 8Kb-512Kb: 2 bytes
        return 3;                      // Flash NOR W25Qxx: 3 bytes (24 bits)
    }

    @Override
    public String getHardwareInstructions() {
        return "Conexiones SPI (25Cxx / W25Qxx):\n" +
                "• PIC RA2 (Pin 1)  → CHIP CS   (Pin 1)\n" +
                "• PIC RA3 (Pin 2)  → CHIP SCK  (Pin 6)\n" +
                "• PIC RA5 (Pin 4)  → CHIP MISO (Pin 2)\n" +
                "• PIC RA6 (Pin 15) → CHIP MOSI (Pin 5)\n" +
                "• PIC GND (Pin 5)  → CHIP GND  (Pin 4)\n" +
                "• PIC VDD (Pin 14) → CHIP VCC  (Pin 8)\n\n" +
                "⚠ EXIGENCIAS DE HARDWARE:\n" +
                "- WP (Pin 3) y HOLD (Pin 7) → Conectar a VCC.\n" +
                "- Tension: Verificar si la memoria es de 3.3V o 5V.\n\n" +
                "Flash NOR (W25Q08-W25Q128): usar modelos indices 13-17 del spinner.\n" +
                "El Chip Erase puede tardar hasta 2 minutos en chips de 16 MB.";
    }
}

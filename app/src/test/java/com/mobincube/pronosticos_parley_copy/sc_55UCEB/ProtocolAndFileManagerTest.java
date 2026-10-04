package com.mobincube.pronosticos_parley_copy.sc_55UCEB;

import com.mobincube.pronosticos_parley_copy.sc_55UCEB.eeprom.I2cProtocol;
import com.mobincube.pronosticos_parley_copy.sc_55UCEB.eeprom.SpiProtocol;
import com.mobincube.pronosticos_parley_copy.sc_55UCEB.file.FileManager;
import com.mobincube.pronosticos_parley_copy.sc_55UCEB.file.IntelHexFormat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;

import static org.junit.Assert.*;

public class ProtocolAndFileManagerTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testI2cProtocolCommands() {
        I2cProtocol proto = new I2cProtocol();

        // 1. Ping
        assertArrayEquals(new byte[] { 0x3F }, proto.buildPingCommand());

        // 2. Scan
        assertArrayEquals(new byte[] { 'I', 'S' }, proto.buildScanOrIdCommand());

        // 3. Read 24C256 (index 8, 32KB, 16-bit address)
        // addr=0x0100, len=64
        byte[] readCmd = proto.buildReadCommand(0x0100, 64, 8);
        assertNotNull(readCmd);
        assertEquals(8, readCmd.length);
        assertEquals('I', readCmd[0]);
        assertEquals('R', readCmd[1]);
        assertEquals(2, readCmd[2]); // 16-bit addr
        assertEquals((byte) 0xA0, readCmd[3]);
        assertEquals(0x01, readCmd[4]);
        assertEquals(0x00, readCmd[5]);
        assertEquals(0x00, readCmd[6]);
        assertEquals(0x40, readCmd[7]);

        // 4. Write
        byte[] writeCmd = proto.buildWriteCommandBase(0x0200, 16, 8);
        assertEquals(8, writeCmd.length);
        assertEquals('I', writeCmd[0]);
        assertEquals('W', writeCmd[1]);
    }

    @Test
    public void testSpiProtocolCommands() {
        SpiProtocol proto = new SpiProtocol();

        // 1. JEDEC ID
        assertArrayEquals(new byte[] { 'P', 'J' }, proto.buildScanOrIdCommand());

        // 2. Read W25Q16 (index 14, 2MB, 24-bit addr)
        // addr=0x010000, len=128
        byte[] readCmd = proto.buildReadCommand(0x010000, 128, 14);
        assertNotNull(readCmd);
        assertEquals(9, readCmd.length);
        assertEquals('P', readCmd[0]);
        assertEquals('R', readCmd[1]);
        assertEquals(3, readCmd[2]); // 24-bit addr
        assertEquals(0x03, readCmd[3]); // opcode
        assertEquals(0x01, readCmd[4]); // addrHi
        assertEquals(0x00, readCmd[5]); // addrMid
        assertEquals(0x00, readCmd[6]); // addrLo
        assertEquals(0x00, readCmd[7]); // lenHi
        assertEquals((byte) 0x80, readCmd[8]); // lenLo

        // 3. Erase Flash NOR (index >= 13)
        byte[] eraseCmd = proto.buildEraseCommand(14);
        assertNotNull(eraseCmd);
        assertArrayEquals(new byte[] { 'P', 'E' }, eraseCmd);

        // 4. Erase EEPROM SPI (index < 13 -> null, software 0xFF)
        assertNull(proto.buildEraseCommand(5));
    }

    @Test
    public void testIntelHexFormatRoundTrip() throws Exception {
        byte[] original = new byte[256];
        for (int i = 0; i < original.length; i++) {
            original[i] = (byte) (i & 0xFF);
        }

        String hexStr = IntelHexFormat.generateIntelHex(original);
        assertNotNull(hexStr);
        assertTrue(hexStr.startsWith(":"));
        assertTrue(hexStr.contains(":00000001FF")); // EOF record

        byte[] parsed = IntelHexFormat.parseIntelHex(hexStr.getBytes(java.nio.charset.StandardCharsets.UTF_8), 256);
        assertArrayEquals(original, parsed);
    }

    @Test
    public void testZeroCopyTransfer() throws Exception {
        File src = tempFolder.newFile("source.bin");
        File dst = tempFolder.newFile("dest.bin");

        byte[] testData = new byte[65536];
        for (int i = 0; i < testData.length; i++) {
            testData[i] = (byte) ((i * 31) & 0xFF);
        }
        Files.write(src.toPath(), testData);

        long copied = FileManager.copyFileZeroCopy(src, dst);
        assertEquals(testData.length, copied);
        assertEquals(src.length(), dst.length());

        byte[] dstData = Files.readAllBytes(dst.toPath());
        assertArrayEquals(testData, dstData);
    }
}

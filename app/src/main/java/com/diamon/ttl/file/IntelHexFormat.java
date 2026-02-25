package com.diamon.ttl.file;

import com.diamon.ttl.exception.HexParseException;
import java.nio.charset.StandardCharsets;

public class IntelHexFormat {

    public static byte[] parseIntelHex(byte[] fileData, int targetSize) throws HexParseException {
        String content = new String(fileData, StandardCharsets.UTF_8);
        String[] lines = content.split("\\r?\\n");

        byte[] buffer = new byte[targetSize];
        java.util.Arrays.fill(buffer, (byte) 0xFF);

        int extendedLinearAddress = 0;
        int extendedSegmentAddress = 0;
        int highestAddress = 0;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty())
                continue;

            if (!line.startsWith(":")) {
                throw new HexParseException("Línea no inicia con ':': " + line);
            }
            if (line.length() < 11) {
                throw new HexParseException("Línea muy corta: " + line);
            }

            try {
                int len = Integer.parseInt(line.substring(1, 3), 16);
                int addr = Integer.parseInt(line.substring(3, 7), 16);
                int type = Integer.parseInt(line.substring(7, 9), 16);

                // Checksum validation
                int checksum = 0;
                for (int i = 1; i < line.length() - 1; i += 2) {
                    checksum += Integer.parseInt(line.substring(i, i + 2), 16);
                }
                if ((checksum & 0xFF) != 0) {
                    // Ignorable or throw, returning warning via callback is harder here, let's keep
                    // it quiet or print stack
                    System.err.println("Advertencia del parser: Checksum inválido en línea -> " + line);
                }

                if (type == 0x00) { // Data record
                    int baseAddress = (extendedLinearAddress << 16) + (extendedSegmentAddress << 4);
                    int finalAddress = baseAddress + addr;

                    for (int i = 0; i < len; i++) {
                        int dataByte = Integer.parseInt(line.substring(9 + i * 2, 11 + i * 2), 16);
                        if (finalAddress + i < targetSize) {
                            buffer[finalAddress + i] = (byte) dataByte;
                            if (finalAddress + i > highestAddress) {
                                highestAddress = finalAddress + i;
                            }
                        }
                    }
                } else if (type == 0x01) { // EOF
                    break;
                } else if (type == 0x02) { // Ext Segment
                    extendedSegmentAddress = Integer.parseInt(line.substring(9, 13), 16);
                } else if (type == 0x04) { // Ext Linear
                    extendedLinearAddress = Integer.parseInt(line.substring(9, 13), 16);
                }
            } catch (Exception e) {
                throw new HexParseException("Error parseando los valores de la línea: " + line, e);
            }
        }

        int actualSize = highestAddress + 1;
        if (actualSize == 0)
            return new byte[0];

        // Ajustamos al bloque mínimo (16 bytes) para no romper el buffer UART de
        // refilón
        if (actualSize % 16 != 0) {
            actualSize = ((actualSize / 16) + 1) * 16;
        }

        byte[] trimmedBuffer = new byte[actualSize];
        System.arraycopy(buffer, 0, trimmedBuffer, 0, actualSize);
        return trimmedBuffer;
    }

    public static String generateIntelHex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int address = 0;

        while (address < data.length) {
            int len = Math.min(16, data.length - address);
            // Si pasamos los 64KB, generar registro Extended Linear Address (Type 04)
            if ((address % 65536) == 0 && address > 0) {
                int extAddr = address >> 16;
                String extRec = String.format("02000004%04X", extAddr);
                int extChecksum = calculateHexChecksum(extRec);
                sb.append(":").append(extRec).append(String.format("%02X", extChecksum)).append("\n");
            }

            int addr16 = address & 0xFFFF;
            StringBuilder rec = new StringBuilder();
            rec.append(String.format("%02X%04X%02X", len, addr16, 0x00)); // Type 00 (Data)
            for (int i = 0; i < len; i++) {
                rec.append(String.format("%02X", data[address + i]));
            }
            int checksum = calculateHexChecksum(rec.toString());
            sb.append(":").append(rec.toString()).append(String.format("%02X", checksum)).append("\n");

            address += len;
        }
        // EOF record
        sb.append(":00000001FF\n");
        return sb.toString();
    }

    private static int calculateHexChecksum(String hexRecord) {
        int sum = 0;
        for (int i = 0; i < hexRecord.length(); i += 2) {
            sum += Integer.parseInt(hexRecord.substring(i, i + 2), 16);
        }
        return (256 - (sum & 0xFF)) & 0xFF; // Complemento a 2
    }
}

package com.mobincube.pronosticos_parley_copy.sc_55UCEB.file;

import android.os.Environment;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

public class FileManager {

    @SuppressWarnings("deprecation")
    public static File saveMemoryDump(byte[] eepromData) throws IOException {
        if (eepromData == null || eepromData.length == 0) {
            throw new IllegalArgumentException("El buffer de datos está vacío.");
        }

        File env = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File romDir = new File(env, "rom");
        if (!romDir.exists() && !romDir.mkdirs()) {
            throw new IOException("No se pudo crear el directorio de destino en Descargas/rom");
        }

        long timestamp = System.currentTimeMillis();

        // 1. Guardar BIN usando FileChannel y ByteBuffer (NIO directo)
        String fileNameBin = "eeprom_dump_" + timestamp + ".bin";
        File fileBin = new File(romDir, fileNameBin);
        try (FileOutputStream fosBin = new FileOutputStream(fileBin);
             FileChannel outChannel = fosBin.getChannel()) {
            ByteBuffer buffer = ByteBuffer.wrap(eepromData);
            while (buffer.hasRemaining()) {
                outChannel.write(buffer);
            }
        }

        // 2. Guardar HEX usando FileChannel
        String fileNameHex = "eeprom_dump_" + timestamp + ".hex";
        File fileHex = new File(romDir, fileNameHex);
        String hexData = IntelHexFormat.generateIntelHex(eepromData);
        byte[] hexBytes = hexData.getBytes(StandardCharsets.UTF_8);
        try (FileOutputStream fosHex = new FileOutputStream(fileHex);
             FileChannel outChannel = fosHex.getChannel()) {
            ByteBuffer buffer = ByteBuffer.wrap(hexBytes);
            while (buffer.hasRemaining()) {
                outChannel.write(buffer);
            }
        }

        // Retornar directorio destino
        return romDir;
    }

    /**
     * Copia de archivos Zero-Copy utilizando la llamada de sistema sendfile() a través de FileChannel.transferTo().
     * Los bloques de datos se transfieren a nivel de kernel sin copias intermedias en el Heap de la JVM.
     */
    public static long copyFileZeroCopy(File source, File destination) throws IOException {
        if (!source.exists()) {
            throw new IOException("Archivo de origen no existe: " + source.getAbsolutePath());
        }
        try (FileInputStream fis = new FileInputStream(source);
             FileChannel inChannel = fis.getChannel();
             FileOutputStream fos = new FileOutputStream(destination);
             FileChannel outChannel = fos.getChannel()) {
            long size = inChannel.size();
            long transferred = 0;
            while (transferred < size) {
                long n = inChannel.transferTo(transferred, size - transferred, outChannel);
                if (n <= 0) break;
                transferred += n;
            }
            return transferred;
        }
    }
}

package com.diamon.ttl.file;

import android.os.Environment;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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

        // Save BIN
        String fileNameBin = "eeprom_dump_" + timestamp + ".bin";
        File fileBin = new File(romDir, fileNameBin);
        try (FileOutputStream fosBin = new FileOutputStream(fileBin)) {
            fosBin.write(eepromData);
        }

        // Save HEX
        String fileNameHex = "eeprom_dump_" + timestamp + ".hex";
        File fileHex = new File(romDir, fileNameHex);
        try (FileOutputStream fosHex = new FileOutputStream(fileHex)) {
            String hexData = IntelHexFormat.generateIntelHex(eepromData);
            fosHex.write(hexData.getBytes(StandardCharsets.UTF_8));
        }

        // Return the directory where files were saved
        return romDir;
    }
}

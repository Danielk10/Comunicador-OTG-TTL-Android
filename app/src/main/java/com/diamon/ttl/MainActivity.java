package com.diamon.ttl;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity implements UsbSerialListener {

    private UsbSerialManager serialManager;

    private Button btnConnect, btnDisconnect, btnRead, btnWrite, btnSave;
    private Spinner spinnerBaudRate, spinnerProtocol, spinnerModel;
    private TextView tvStatusLabel, tvInstructions, tvHexViewer, tvLog;
    private ScrollView scrollLog;
    private View statusDot, layoutStatus;
    private ProgressBar progressBar;

    private boolean isReading = false;
    private boolean isWriting = false;

    // EEPROM Buffer and State Machine
    private byte[] eepromBuffer;
    private byte[] writeDataBuffer;
    private int currentAddress = 0;
    private int totalSize = 0;
    private ByteArrayOutputStream readStream;
    private final int CHUNK_SIZE = 16; // 16 bytes per chunk to be safe with page sizes
    private Handler taskHandler = new Handler(Looper.getMainLooper());
    private Runnable timeoutRunnable;

    private final int[] i2cSizes = { 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536 };
    private final int[] spiSizes = { 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536, 131072 };

    private static final int PICK_FILE_REQUEST = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupSpinners();

        serialManager = new UsbSerialManager(this, this);
        updateUIState(false);

        btnConnect.setOnClickListener(v -> connectSerial());
        btnDisconnect.setOnClickListener(v -> disconnectSerial());
        btnRead.setOnClickListener(v -> startRead());
        btnWrite.setOnClickListener(v -> startWrite());
        btnSave.setOnClickListener(v -> saveBuffer());

        timeoutRunnable = () -> {
            if (isReading) {
                isReading = false;
                Toast.makeText(this, "Tiempo de espera agotado leyendo EEPROM", Toast.LENGTH_SHORT).show();
            }
            if (isWriting) {
                isWriting = false;
                Toast.makeText(this, "Tiempo de espera agotado escribiendo EEPROM", Toast.LENGTH_SHORT).show();
            }
            updateUIState(serialManager.isConnected());
            progressBar.setVisibility(View.GONE);
        };
    }

    private void initViews() {
        btnConnect = findViewById(R.id.btnConnect);
        btnDisconnect = findViewById(R.id.btnDisconnect);
        btnRead = findViewById(R.id.btnRead);
        btnWrite = findViewById(R.id.btnWrite);
        btnSave = findViewById(R.id.btnSave);

        spinnerBaudRate = findViewById(R.id.spinnerBaudRate);
        spinnerProtocol = findViewById(R.id.spinnerProtocol);
        spinnerModel = findViewById(R.id.spinnerModel);

        tvStatusLabel = findViewById(R.id.tvStatusLabel);
        tvInstructions = findViewById(R.id.tvInstructions);
        tvHexViewer = findViewById(R.id.tvHexViewer);
        tvLog = findViewById(R.id.tvLog);
        scrollLog = findViewById(R.id.scrollLog);

        statusDot = findViewById(R.id.statusDot);
        layoutStatus = findViewById(R.id.layoutStatus);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> baudAdapter = ArrayAdapter.createFromResource(this, R.array.baudrates,
                android.R.layout.simple_spinner_item);
        baudAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBaudRate.setAdapter(baudAdapter);
        spinnerBaudRate.setSelection(4); // 9600

        ArrayAdapter<CharSequence> protAdapter = ArrayAdapter.createFromResource(this, R.array.eeprom_protocols,
                android.R.layout.simple_spinner_item);
        protAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProtocol.setAdapter(protAdapter);

        spinnerProtocol.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateModelSpinner(position);
                updateInstructions(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void updateModelSpinner(int protocolIndex) {
        int arrayResId = (protocolIndex == 0) ? R.array.eeprom_i2c_sizes : R.array.eeprom_spi_sizes;
        ArrayAdapter<CharSequence> modelAdapter = ArrayAdapter.createFromResource(this, arrayResId,
                android.R.layout.simple_spinner_item);
        modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerModel.setAdapter(modelAdapter);
    }

    private void updateInstructions(int protocolIndex) {
        if (protocolIndex == 0) {
            tvInstructions.setText(
                    "Conexiones I2C (24Cxx):\n- PIC RA0 -> EEPROM SDA\n- PIC RA1 -> EEPROM SCL\n- PIC GND/VCC -> EEPROM GND/VCC\n*(Se requieren resistencias pull-up externas en SDA y SCL)*");
        } else {
            tvInstructions.setText(
                    "Conexiones SPI (25Cxx):\n- PIC RA2 -> EEPROM CS\n- PIC RA3 -> EEPROM SCK\n- PIC RA5 -> EEPROM MISO (DO)\n- PIC RA6 -> EEPROM MOSI (DI)\n- PIC GND/VCC -> EEPROM GND/VCC");
        }
    }

    private void connectSerial() {
        int baudRate = Integer.parseInt(spinnerBaudRate.getSelectedItem().toString());
        serialManager.setSerialParameters(baudRate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
        log("Conectando puerto serial a " + baudRate + " baudios...");
        serialManager.connect();
    }

    private void disconnectSerial() {
        log("Desconectando puerto serial...");
        serialManager.disconnect();
    }

    private void log(final String message) {
        runOnUiThread(() -> {
            if (tvLog == null || scrollLog == null)
                return;
            String time = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                    .format(new java.util.Date());
            tvLog.append("[" + time + "] " + message + "\n");
            scrollLog.post(() -> scrollLog.fullScroll(View.FOCUS_DOWN));
        });
    }

    // =========================================================================
    // LECTURA
    // =========================================================================

    private void startRead() {
        if (!serialManager.isConnected()) {
            Toast.makeText(this, "Debe conectar el dispositivo primero", Toast.LENGTH_SHORT).show();
            return;
        }
        isReading = true;
        currentAddress = 0;
        readStream = new ByteArrayOutputStream();

        int proto = spinnerProtocol.getSelectedItemPosition();
        int model = spinnerModel.getSelectedItemPosition();
        totalSize = (proto == 0) ? i2cSizes[model] : spiSizes[model];

        log("Iniciando lectura de " + totalSize + " bytes " + (proto == 0 ? "I2C" : "SPI") + "...");

        progressBar.setMax(totalSize);
        progressBar.setProgress(0);
        progressBar.setVisibility(View.VISIBLE);
        tvHexViewer.setText("Dirección | 00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F | ASCII\n" +
                "------------------------------------------------------------------\nLeyendo...");

        updateUIState(true);
        requestNextReadChunk();
    }

    private void requestNextReadChunk() {
        if (!isReading)
            return;
        if (currentAddress >= totalSize) {
            finishRead();
            return;
        }

        int len = Math.min(CHUNK_SIZE, totalSize - currentAddress);
        int proto = spinnerProtocol.getSelectedItemPosition();

        byte[] cmd;
        if (proto == 0) { // I2C
            // 'I' 'R' <addr_len> <chip_addr> <addr_hi> <addr_lo> <len>
            int modelIdx = spinnerModel.getSelectedItemPosition();
            byte addrLen = (byte) (modelIdx >= 4 ? 2 : 1); // 24c16 y menor usan 1 byte para dirección
            byte chipAddr = (byte) 0xA0;
            if (addrLen == 1) {
                // Las EEPROMS I2C pequeñas unen la parte alta de la address con el device pin
                chipAddr |= ((currentAddress >> 8) & 0x07) << 1;
            }
            byte addrHi = (byte) ((currentAddress >> 8) & 0xFF);
            byte addrLo = (byte) (currentAddress & 0xFF);

            cmd = new byte[] { 'I', 'R', addrLen, chipAddr, addrHi, addrLo, (byte) len };
        } else { // SPI
            // 'P' 'R' <addr_hi> <addr_lo> <len>
            byte addrHi = (byte) ((currentAddress >> 8) & 0xFF);
            byte addrLo = (byte) (currentAddress & 0xFF);
            cmd = new byte[] { 'P', 'R', addrHi, addrLo, (byte) len };
        }

        serialManager.sendData(cmd);
        resetTimeout();
    }

    private void finishRead() {
        isReading = false;
        eepromBuffer = readStream.toByteArray();
        progressBar.setVisibility(View.GONE);
        taskHandler.removeCallbacks(timeoutRunnable);
        log("Lectura completada exitosamente.");
        Toast.makeText(this, "Lectura completada", Toast.LENGTH_SHORT).show();
        updateUIState(true);
        renderHexViewer(eepromBuffer);
    }

    // =========================================================================
    // ESCRITURA
    // =========================================================================

    private void startWrite() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, PICK_FILE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                prepareWriteData(uri);
            }
        }
    }

    private void prepareWriteData(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            if (is == null)
                return;

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            byte[] rawFileData = buffer.toByteArray();
            is.close();

            int proto = spinnerProtocol.getSelectedItemPosition();
            int model = spinnerModel.getSelectedItemPosition();
            totalSize = (proto == 0) ? i2cSizes[model] : spiSizes[model];

            String fileName = "Archivo";
            android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex);
                }
                cursor.close();
            }

            log("Cargado archivo: " + fileName + " (" + rawFileData.length + " bytes)");

            if (fileName.toLowerCase().endsWith(".hex") || (rawFileData.length > 0 && rawFileData[0] == ':')) {
                log("Detectado formato Intel Hex. Parseando...");
                try {
                    writeDataBuffer = parseIntelHex(rawFileData, totalSize);
                    log("Intel Hex procesado. " + writeDataBuffer.length + " bytes resultantes.");
                } catch (Exception ex) {
                    log("Error parseando Intel Hex: " + ex.getMessage());
                    log("Tratando contenido como Binario Puro...");
                    writeDataBuffer = rawFileData;
                }
            } else {
                log("Detectado formato Binario Puro.");
                writeDataBuffer = rawFileData;
            }

            if (writeDataBuffer.length > totalSize) {
                log("Error: El archivo de escritura (" + writeDataBuffer.length
                        + " bytes) es más grande que el límite de la memoria (" + totalSize + " bytes).");
                Toast.makeText(this, "Archivo excede la capacidad de la memoria", Toast.LENGTH_LONG).show();
                return;
            }

            log("Iniciando escritura de " + writeDataBuffer.length + " bytes en memoria...");
            isWriting = true;
            currentAddress = 0;
            progressBar.setMax(writeDataBuffer.length);
            progressBar.setProgress(0);
            progressBar.setVisibility(View.VISIBLE);
            tvHexViewer.setText("Escribiendo...");

            updateUIState(true);
            sendNextWriteChunk();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al leer el archivo: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void sendNextWriteChunk() {
        if (!isWriting)
            return;
        if (currentAddress >= writeDataBuffer.length) {
            finishWrite();
            return;
        }

        int len = Math.min(CHUNK_SIZE, writeDataBuffer.length - currentAddress);
        int proto = spinnerProtocol.getSelectedItemPosition();

        ByteArrayOutputStream cmdStream = new ByteArrayOutputStream();

        try {
            if (proto == 0) { // I2C
                // 'I' 'W' <addr_len> <chip_addr> <addr_hi> <addr_lo> <len> <data...>
                int modelIdx = spinnerModel.getSelectedItemPosition();
                byte addrLen = (byte) (modelIdx >= 4 ? 2 : 1);
                byte chipAddr = (byte) 0xA0;
                if (addrLen == 1) {
                    chipAddr |= ((currentAddress >> 8) & 0x07) << 1;
                }
                byte addrHi = (byte) ((currentAddress >> 8) & 0xFF);
                byte addrLo = (byte) (currentAddress & 0xFF);

                cmdStream.write(new byte[] { 'I', 'W', addrLen, chipAddr, addrHi, addrLo, (byte) len });
            } else { // SPI
                // 'P' 'W' <addr_hi> <addr_lo> <len> <data...>
                byte addrHi = (byte) ((currentAddress >> 8) & 0xFF);
                byte addrLo = (byte) (currentAddress & 0xFF);
                cmdStream.write(new byte[] { 'P', 'W', addrHi, addrLo, (byte) len });
            }
            // Adjuntar datos
            cmdStream.write(writeDataBuffer, currentAddress, len);
        } catch (Exception e) {
            e.printStackTrace();
        }

        serialManager.sendData(cmdStream.toByteArray());
        resetTimeout();
    }

    private void finishWrite() {
        isWriting = false;
        progressBar.setVisibility(View.GONE);
        taskHandler.removeCallbacks(timeoutRunnable);
        log("Escritura completada exitosamente.");
        Toast.makeText(this, "Escritura completada", Toast.LENGTH_SHORT).show();
        tvHexViewer.setText("Escritura finalizada con éxito.");
        updateUIState(true);
    }

    // =========================================================================
    // GUARDADO (EXPORT)
    // =========================================================================

    private void saveBuffer() {
        if (eepromBuffer == null || eepromBuffer.length == 0) {
            log("Error: No hay datos en el buffer para guardar.");
            Toast.makeText(this, "No hay datos leídos para guardar.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            File env = android.os.Environment
                    .getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
            File romDir = new File(env, "rom");
            if (!romDir.exists())
                romDir.mkdirs();

            // Guardar BIN
            String fileNameBin = "eeprom_dump_" + System.currentTimeMillis() + ".bin";
            File fileBin = new File(romDir, fileNameBin);
            FileOutputStream fosBin = new FileOutputStream(fileBin);
            fosBin.write(eepromBuffer);
            fosBin.close();
            log("Guardado BIN: " + fileBin.getName());

            // Guardar HEX
            String fileNameHex = "eeprom_dump_" + System.currentTimeMillis() + ".hex";
            File fileHex = new File(romDir, fileNameHex);
            FileOutputStream fosHex = new FileOutputStream(fileHex);
            fosHex.write(generateIntelHex(eepromBuffer).getBytes(StandardCharsets.UTF_8));
            fosHex.close();
            log("Guardado HEX: " + fileHex.getName());

            Toast.makeText(this, "Archivos guardados en Descargas/rom/", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            log("Error guardando archivos: " + e.getMessage());
            Toast.makeText(this, "Error guardando: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private byte[] parseIntelHex(byte[] fileData, int targetSize) throws Exception {
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
                throw new Exception("Línea no inicia con ':': " + line);
            }
            if (line.length() < 11)
                throw new Exception("Línea muy corta: " + line);

            int len = Integer.parseInt(line.substring(1, 3), 16);
            int addr = Integer.parseInt(line.substring(3, 7), 16);
            int type = Integer.parseInt(line.substring(7, 9), 16);

            // Validar checksum
            int checksum = 0;
            for (int i = 1; i < line.length() - 1; i += 2) {
                checksum += Integer.parseInt(line.substring(i, i + 2), 16);
            }
            if ((checksum & 0xFF) != 0) {
                log("Advertencia del parser: Checksum inválido en línea -> " + line);
            }

            if (type == 0x00) { // Data record
                int baseAddress = (extendedLinearAddress << 16) + (extendedSegmentAddress << 4);
                int finalAddress = baseAddress + addr;

                for (int i = 0; i < len; i++) {
                    int dataByte = Integer.parseInt(line.substring(9 + i * 2, 11 + i * 2), 16);
                    if (finalAddress + i < targetSize) {
                        buffer[finalAddress + i] = (byte) dataByte;
                        if (finalAddress + i > highestAddress)
                            highestAddress = finalAddress + i;
                    }
                }
            } else if (type == 0x01) { // EOF
                break;
            } else if (type == 0x02) { // Ext Segment
                extendedSegmentAddress = Integer.parseInt(line.substring(9, 13), 16);
            } else if (type == 0x04) { // Ext Linear
                extendedLinearAddress = Integer.parseInt(line.substring(9, 13), 16);
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

    private String generateIntelHex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int address = 0;
        while (address < data.length) {
            int len = Math.min(16, data.length - address);
            // Si pasamos los 64KB, generar registro Extended Linear Address (Type 04)
            if ((address % 65536) == 0 && address > 0) { // Only generate if address is not 0 and is a multiple of 64KB
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

    private int calculateHexChecksum(String hexRecord) {
        int sum = 0;
        for (int i = 0; i < hexRecord.length(); i += 2) {
            sum += Integer.parseInt(hexRecord.substring(i, i + 2), 16);
        }
        return (256 - (sum & 0xFF)) & 0xFF; // Complemento a 2
    }

    // =========================================================================
    // UTILIDADES
    // =========================================================================

    private void resetTimeout() {
        taskHandler.removeCallbacks(timeoutRunnable);
        taskHandler.postDelayed(timeoutRunnable, 5000); // 5 segundos timeout
    }

    private void renderHexViewer(byte[] data) {
        StringBuilder sb = new StringBuilder();
        sb.append("Dirección | 00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F | ASCII\n");
        sb.append("------------------------------------------------------------------\n");
        for (int i = 0; i < data.length; i += 16) {
            sb.append(String.format("%08X  | ", i));

            // Hex bytes
            for (int j = 0; j < 16; j++) {
                if (i + j < data.length) {
                    sb.append(String.format("%02X ", data[i + j]));
                } else {
                    sb.append("   ");
                }
            }
            sb.append("| ");

            // ASCII chars
            for (int j = 0; j < 16; j++) {
                if (i + j < data.length) {
                    char c = (char) data[i + j];
                    if (c >= 32 && c <= 126) {
                        sb.append(c);
                    } else {
                        sb.append('.');
                    }
                }
            }
            sb.append("\n");
        }
        tvHexViewer.setText(sb.toString());
    }

    private void updateUIState(final boolean connected) {
        runOnUiThread(() -> {
            btnConnect.setEnabled(!connected);
            btnDisconnect.setEnabled(connected);
            spinnerBaudRate.setEnabled(!connected);

            btnRead.setEnabled(connected && !isReading && !isWriting);
            btnWrite.setEnabled(connected && !isReading && !isWriting);
            btnSave.setEnabled(!isReading && !isWriting && eepromBuffer != null);

            if (connected) {
                statusDot.setBackgroundColor(Color.parseColor("#3FB950"));
                tvStatusLabel.setText(R.string.status_connected);
                tvStatusLabel.setTextColor(Color.parseColor("#3FB950"));
                if (layoutStatus != null)
                    layoutStatus.setBackgroundColor(Color.parseColor("#112211"));
            } else {
                statusDot.setBackgroundColor(Color.parseColor("#F85149"));
                tvStatusLabel.setText(R.string.status_disconnected);
                tvStatusLabel.setTextColor(Color.parseColor("#F85149"));
                if (layoutStatus != null)
                    layoutStatus.setBackgroundColor(Color.parseColor("#1A0A0A"));
                isReading = isWriting = false;
                taskHandler.removeCallbacks(timeoutRunnable);
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_open_terminal) {
            startActivity(new Intent(this, TerminalActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSerialConnect() {
        runOnUiThread(() -> {
            log("Puerto USB inicializado con éxito.");
            Toast.makeText(this, "USB conectado", Toast.LENGTH_SHORT).show();
            updateUIState(true);
        });
    }

    @Override
    public void onSerialConnectError(Exception e) {
        runOnUiThread(() -> {
            log("Error de conexión USB: " + e.getMessage());
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            updateUIState(false);
        });
    }

    @Override
    public void onSerialRead(byte[] data) {
        if (isReading) {
            try {
                readStream.write(data);
                int totalReadSoFar = readStream.size();
                int expectedForThisChunk = Math.min(CHUNK_SIZE, totalSize - currentAddress);

                // Mostrar progreso en vivo en el visor Hex
                runOnUiThread(() -> {
                    byte[] currentBuffer = readStream.toByteArray();
                    renderHexViewer(currentBuffer);
                });

                if (totalReadSoFar >= currentAddress + expectedForThisChunk) {
                    currentAddress += expectedForThisChunk;
                    runOnUiThread(() -> progressBar.setProgress(currentAddress));
                    requestNextReadChunk();
                } else {
                    resetTimeout();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (isWriting) {
            // El PIC debería responder con una 'K' (Acknowledge Ok) cuando termina de
            // escribir el chunk de datos
            for (byte b : data) {
                if (b == 'K') {
                    int expectedForThisChunk = Math.min(CHUNK_SIZE, writeDataBuffer.length - currentAddress);
                    currentAddress += expectedForThisChunk;
                    runOnUiThread(() -> progressBar.setProgress(currentAddress));
                    sendNextWriteChunk();
                    break;
                }
            }
        }
    }

    @Override
    public void onSerialIoError(Exception e) {
        runOnUiThread(() -> {
            log("Error I/O: " + e.getMessage());
            Toast.makeText(this, "Error I/O: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            updateUIState(false);
        });
    }

    @Override
    public void onSerialDisconnect() {
        runOnUiThread(() -> {
            log("Dispositivo desconectado.");
            Toast.makeText(this, "Desconectado", Toast.LENGTH_SHORT).show();
            updateUIState(false);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        serialManager.cleanup();
        taskHandler.removeCallbacksAndMessages(null);
    }
}

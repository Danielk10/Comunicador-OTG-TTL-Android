package com.mobincube.pronosticos_parley_copy.sc_55UCEB;

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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.mobincube.pronosticos_parley_copy.sc_55UCEB.eeprom.EepromProtocol;
import com.mobincube.pronosticos_parley_copy.sc_55UCEB.eeprom.I2cProtocol;
import com.mobincube.pronosticos_parley_copy.sc_55UCEB.eeprom.SpiProtocol;
import com.mobincube.pronosticos_parley_copy.sc_55UCEB.exception.HexParseException;
import com.mobincube.pronosticos_parley_copy.sc_55UCEB.file.FileManager;
import com.mobincube.pronosticos_parley_copy.sc_55UCEB.file.IntelHexFormat;
import com.mobincube.pronosticos_parley_copy.sc_55UCEB.ui.HexViewerHelper;
import com.mobincube.pronosticos_parley_copy.sc_55UCEB.ui.LogHelper;
import com.mobincube.pronosticos_parley_copy.sc_55UCEB.usb.ProtocolState;
import com.mobincube.pronosticos_parley_copy.sc_55UCEB.usb.UsbSerialListener;
import com.mobincube.pronosticos_parley_copy.sc_55UCEB.usb.UsbSerialManager;
import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity implements UsbSerialListener {

    private UsbSerialManager serialManager;

    private Button btnConnect, btnDisconnect, btnRead, btnWrite, btnErase, btnVerify, btnSave, btnClearLog, btnScan, btnFullDump;
    private Spinner spinnerProtocol, spinnerModel;
    private TextView tvStatusLabel, tvInstructions;
    private View statusDot, layoutStatus;
    private ProgressBar progressBar;

    private ProtocolState state = ProtocolState.IDLE;

    private LogHelper logHelper;
    private HexViewerHelper hexHelper;

    private byte[] eepromBuffer;
    private byte[] writeDataBuffer;
    private int currentAddress = 0;
    private int totalSize = 0;
    private ByteArrayOutputStream readStream;
    private final int MAX_WRITE_CHUNK_SIZE = 64;
    private final int READ_CHUNK_SIZE = 64;

    private Handler taskHandler = new Handler(Looper.getMainLooper());
    private Runnable timeoutRunnable;

    private final I2cProtocol i2cProtocol = new I2cProtocol();
    private final SpiProtocol spiProtocol = new SpiProtocol();

    private androidx.activity.result.ActivityResultLauncher<Intent> filePickerLauncher;


    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.about_title)
                .setMessage(R.string.about_message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void openHardwareInfo(int type) {
        Intent intent = new Intent(this, com.mobincube.pronosticos_parley_copy.sc_55UCEB.ui.HardwareInfoActivity.class);
        intent.putExtra("type", type);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        filePickerLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            prepareWriteData(uri);
                        }
                    }
                });

        initViews();
        setupSpinners();

        serialManager = new UsbSerialManager(this, this);
        updateUIState(false);

        btnConnect.setOnClickListener(v -> connectSerial());
        btnDisconnect.setOnClickListener(v -> disconnectSerial());
        btnRead.setOnClickListener(v -> startRead());
        btnWrite.setOnClickListener(v -> startWrite());
        btnErase.setOnClickListener(v -> startErase());
        btnVerify.setOnClickListener(v -> startVerify());
        btnSave.setOnClickListener(v -> saveBuffer());
        btnClearLog.setOnClickListener(v -> clearLog());
        btnScan.setOnClickListener(v -> startScan());
        btnFullDump.setOnClickListener(v -> startFullDump());

        timeoutRunnable = () -> {
            if (state == ProtocolState.READING) {
                state = ProtocolState.IDLE;
                Toast.makeText(this, "Tiempo de espera agotado leyendo EEPROM", Toast.LENGTH_SHORT).show();
            }
            if (state == ProtocolState.WRITING) {
                state = ProtocolState.IDLE;
                Toast.makeText(this, "Tiempo de espera agotado escribiendo memoria", Toast.LENGTH_SHORT).show();
            }
            if (state == ProtocolState.ERASING) {
                state = ProtocolState.IDLE;
                Toast.makeText(this, "Tiempo de espera agotado borrando memoria", Toast.LENGTH_SHORT).show();
            }
            if (state == ProtocolState.VERIFYING) {
                state = ProtocolState.IDLE;
                Toast.makeText(this, "Tiempo de espera agotado verificando memoria", Toast.LENGTH_SHORT).show();
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
        btnErase = findViewById(R.id.btnErase);
        btnVerify = findViewById(R.id.btnVerify);
        btnSave = findViewById(R.id.btnSave);
        btnClearLog = findViewById(R.id.btnClearLog);
        btnScan = findViewById(R.id.btnScan);
        btnFullDump = findViewById(R.id.btnFullDump);

        spinnerProtocol = findViewById(R.id.spinnerProtocol);
        spinnerModel = findViewById(R.id.spinnerModel);

        tvStatusLabel = findViewById(R.id.tvStatusLabel);
        tvInstructions = findViewById(R.id.tvInstructions);

        TextView tvHexViewer = findViewById(R.id.tvHexViewer);
        TextView tvLog = findViewById(R.id.tvLog);
        ScrollView scrollLog = findViewById(R.id.scrollLog);

        statusDot = findViewById(R.id.statusDot);
        layoutStatus = findViewById(R.id.layoutStatus);
        progressBar = findViewById(R.id.progressBar);

        logHelper = new LogHelper(this, tvLog, scrollLog);
        hexHelper = new HexViewerHelper(this, tvHexViewer);
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> protAdapter = ArrayAdapter.createFromResource(this, R.array.eeprom_protocols,
                android.R.layout.simple_spinner_item);
        protAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProtocol.setAdapter(protAdapter);

        spinnerProtocol.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateModelSpinner(position);
                tvInstructions.setText(getActiveProtocol().getHardwareInstructions());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private EepromProtocol getActiveProtocol() {
        return spinnerProtocol.getSelectedItemPosition() == 0 ? i2cProtocol : spiProtocol;
    }

    private void updateModelSpinner(int protocolIndex) {
        int arrayResId = (protocolIndex == 0) ? R.array.eeprom_i2c_sizes : R.array.eeprom_spi_sizes;
        ArrayAdapter<CharSequence> modelAdapter = ArrayAdapter.createFromResource(this, arrayResId,
                android.R.layout.simple_spinner_item);
        modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerModel.setAdapter(modelAdapter);
    }

    private void connectSerial() {
        int baudRate = 9600;
        serialManager.setSerialParameters(baudRate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
        log("Conectando puerto serial a " + baudRate + " baudios...");
        serialManager.connect();
    }

    private void disconnectSerial() {
        log("Desconectando puerto serial...");
        serialManager.disconnect();
    }

    private void log(final String message) {
        if (logHelper != null)
            logHelper.log(message);
    }

    // =========================================================================
    // LECTURA
    // =========================================================================

    private void startRead() {
        if (!serialManager.isConnected()) {
            Toast.makeText(this, "Debe conectar el dispositivo primero", Toast.LENGTH_SHORT).show();
            return;
        }
        state = ProtocolState.READING;
        currentAddress = 0;
        readStream = new ByteArrayOutputStream();

        int model = spinnerModel.getSelectedItemPosition();
        totalSize = getActiveProtocol().getTotalSize(model);

        log("Iniciando lectura de " + totalSize + " bytes...");

        progressBar.setMax(totalSize);
        progressBar.setProgress(0);
        progressBar.setVisibility(View.VISIBLE);
        hexHelper.setText("Dirección | 00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F | ASCII\n" +
                "------------------------------------------------------------------\nLeyendo...");

        updateUIState(true);
        requestNextReadChunk();
    }

    private void requestNextReadChunk() {
        if (state != ProtocolState.READING)
            return;
        if (currentAddress >= totalSize) {
            finishRead();
            return;
        }

        int len = Math.min(READ_CHUNK_SIZE, totalSize - currentAddress);
        int modelIdx = spinnerModel.getSelectedItemPosition();

        byte[] cmd = getActiveProtocol().buildReadCommand(currentAddress, len, modelIdx);

        serialManager.sendData(cmd);
        resetTimeout();
    }

    private void finishRead() {
        state = ProtocolState.IDLE;
        eepromBuffer = readStream.toByteArray();
        progressBar.setVisibility(View.GONE);
        taskHandler.removeCallbacks(timeoutRunnable);
        log("Lectura completada exitosamente.");
        Toast.makeText(this, "Lectura completada", Toast.LENGTH_SHORT).show();
        updateUIState(true);
        hexHelper.renderNow(eepromBuffer);
    }

    // =========================================================================
    // ESCRITURA
    // =========================================================================

    private void startWrite() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        filePickerLauncher.launch(intent);
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

            int model = spinnerModel.getSelectedItemPosition();
            totalSize = getActiveProtocol().getTotalSize(model);

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

            if (fileName.toLowerCase(java.util.Locale.ROOT).endsWith(".hex")
                    || (rawFileData.length > 0 && rawFileData[0] == ':')) {
                log("Detectado formato Intel Hex. Parseando...");
                try {
                    writeDataBuffer = IntelHexFormat.parseIntelHex(rawFileData, totalSize);
                    log("Intel Hex procesado. " + writeDataBuffer.length + " bytes resultantes.");
                } catch (HexParseException ex) {
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
            state = ProtocolState.WRITING;
            currentAddress = 0;
            progressBar.setMax(writeDataBuffer.length);
            progressBar.setProgress(0);
            progressBar.setVisibility(View.VISIBLE);
            hexHelper.setText("Escribiendo...");

            updateUIState(true);
            sendNextWriteChunk();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al leer el archivo: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void sendNextWriteChunk() {
        if (state != ProtocolState.WRITING)
            return;
        if (currentAddress >= writeDataBuffer.length) {
            finishWrite();
            return;
        }

        int model = spinnerModel.getSelectedItemPosition();
        int pageSize = getActiveProtocol().getPageSize(model);

        int bytesToNextBoundary = pageSize - (currentAddress % pageSize);
        int chunkLimit = Math.min(bytesToNextBoundary, MAX_WRITE_CHUNK_SIZE);
        int len = Math.min(chunkLimit, writeDataBuffer.length - currentAddress);

        try {
            ByteArrayOutputStream cmdStream = new ByteArrayOutputStream();
            byte[] baseCmd = getActiveProtocol().buildWriteCommandBase(currentAddress, len, model);
            cmdStream.write(baseCmd);
            cmdStream.write(writeDataBuffer, currentAddress, len);

            serialManager.sendData(cmdStream.toByteArray());
            resetTimeout();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void finishWrite() {
        state = ProtocolState.IDLE;
        progressBar.setVisibility(View.GONE);
        taskHandler.removeCallbacks(timeoutRunnable);
        log("Escritura completada exitosamente.");
        Toast.makeText(this, "Escritura completada", Toast.LENGTH_SHORT).show();
        hexHelper.setText("Escritura finalizada con éxito.");
        updateUIState(true);
    }

    // =========================================================================
    // BORRADO
    // =========================================================================

    private void startErase() {
        if (!serialManager.isConnected()) return;

        int modelIdx = spinnerModel.getSelectedItemPosition();
        byte[] cmd = getActiveProtocol().buildEraseCommand(modelIdx);

        if (cmd == null) {
            // I2C Erase: Write 0xFF to everything
            log("Iniciando borrado I2C (Sobrescritura con 0xFF)...");
            totalSize = getActiveProtocol().getTotalSize(modelIdx);
            writeDataBuffer = new byte[totalSize];
            java.util.Arrays.fill(writeDataBuffer, (byte) 0xFF);
            state = ProtocolState.WRITING;
            currentAddress = 0;
            progressBar.setMax(totalSize);
            progressBar.setProgress(0);
            progressBar.setVisibility(View.VISIBLE);
            updateUIState(true);
            sendNextWriteChunk();
        } else {
            // SPI Native Erase
            log("Iniciando borrado de chip SPI...");
            state = ProtocolState.ERASING;
            serialManager.sendData(cmd);
            progressBar.setIndeterminate(true);
            progressBar.setVisibility(View.VISIBLE);
            updateUIState(true);
            resetTimeout();
        }
    }

    private void finishErase() {
        state = ProtocolState.IDLE;
        progressBar.setVisibility(View.GONE);
        progressBar.setIndeterminate(false);
        taskHandler.removeCallbacks(timeoutRunnable);
        log("Borrado completado exitosamente.");
        Toast.makeText(this, "Borrado completado", Toast.LENGTH_SHORT).show();
        hexHelper.setText("Memoria borrada.");
        updateUIState(serialManager.isConnected());
    }

    // =========================================================================
    // VERIFICACIÓN
    // =========================================================================

    private void startVerify() {
        if (!serialManager.isConnected() || eepromBuffer == null || writeDataBuffer == null) {
            Toast.makeText(this, "Debe tener datos leídos y datos de archivo cargados", Toast.LENGTH_SHORT).show();
            return;
        }
        log("Verificando datos...");
        int size = Math.min(eepromBuffer.length, writeDataBuffer.length);
        int errors = 0;
        for (int i = 0; i < size; i++) {
            if (eepromBuffer[i] != writeDataBuffer[i]) {
                if (errors < 10) {
                    log(String.format("Error en 0x%X: Leído 0x%02X, Esperado 0x%02X", i, eepromBuffer[i], writeDataBuffer[i]));
                }
                errors++;
            }
        }
        if (errors == 0) {
            log("VERIFICACIÓN EXITOSA: Los datos coinciden.");
            Toast.makeText(this, "Verificación exitosa", Toast.LENGTH_SHORT).show();
        } else {
            log("ERROR DE VERIFICACIÓN: " + errors + " diferencias encontradas.");
            Toast.makeText(this, "Error: " + errors + " diferencias", Toast.LENGTH_LONG).show();
        }
    }

    private void startScan() {
        if (!serialManager.isConnected()) return;
        state = ProtocolState.SCANNING_ID;
        readStream = new ByteArrayOutputStream(); // Buffer para respuesta
        byte[] cmd = getActiveProtocol().buildScanOrIdCommand();
        log("Escaneando bus I2C o JEDEC ID...");
        serialManager.sendData(cmd);
        resetTimeout();
        updateUIState(true);
    }

    private void startFullDump() {
        if (!serialManager.isConnected()) return;
        state = ProtocolState.FULL_DUMPING;
        currentAddress = 0;
        readStream = new ByteArrayOutputStream();
        int model = spinnerModel.getSelectedItemPosition();
        totalSize = getActiveProtocol().getTotalSize(model);
        
        log("Iniciando Volcado Completo Automático...");
        progressBar.setMax(totalSize);
        progressBar.setProgress(0);
        progressBar.setVisibility(View.VISIBLE);
        hexHelper.setText("Recibiendo flujo continuo de datos...");

        byte[] cmd = getActiveProtocol().buildFullDumpCommand(model);
        serialManager.sendData(cmd);
        updateUIState(true);
        resetTimeout();
    }

    private void clearLog() {
        if (logHelper != null) logHelper.clear();
        log("Log limpiado.");
    }

    // =========================================================================
    // GUARDADO (EXPORT)
    // =========================================================================

    private void saveBuffer() {
        try {
            File romDir = FileManager.saveMemoryDump(eepromBuffer);
            log("Guardado BIN/HEX en " + romDir.getAbsolutePath());
            Toast.makeText(this, "Archivos guardados en Descargas/rom/", Toast.LENGTH_LONG).show();
        } catch (IllegalArgumentException e) {
            log("Error: " + e.getMessage());
            Toast.makeText(this, "No hay datos leídos para guardar.", Toast.LENGTH_SHORT).show();
        } catch (java.io.IOException e) {
            log("Error I/O guardando archivos: " + e.getMessage());
            Toast.makeText(this, "Error guardando: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // =========================================================================
    // UTILIDADES
    // =========================================================================

    private void resetTimeout() {
        taskHandler.removeCallbacks(timeoutRunnable);
        taskHandler.postDelayed(timeoutRunnable, 5000); // 5 segundos timeout
    }

    private void updateUIState(final boolean connected) {
        runOnUiThread(() -> {
            btnConnect.setEnabled(!connected);
            btnDisconnect.setEnabled(connected);

            boolean isBusy = state != ProtocolState.IDLE;

            btnRead.setEnabled(connected && !isBusy);
            btnWrite.setEnabled(connected && !isBusy);
            btnErase.setEnabled(connected && !isBusy);
            btnScan.setEnabled(connected && !isBusy);
            btnFullDump.setEnabled(connected && !isBusy);
            btnVerify.setEnabled(!isBusy && eepromBuffer != null && writeDataBuffer != null);
            btnSave.setEnabled(!isBusy && eepromBuffer != null);

            if (connected) {
                statusDot.setBackgroundColor(
                        androidx.core.content.ContextCompat.getColor(this, R.color.status_connected));
                tvStatusLabel.setText(R.string.status_connected);
                tvStatusLabel
                        .setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.status_connected));
                if (layoutStatus != null)
                    layoutStatus.setBackgroundColor(Color.parseColor("#112211"));
            } else {
                statusDot.setBackgroundColor(
                        androidx.core.content.ContextCompat.getColor(this, R.color.status_disconnected));
                tvStatusLabel.setText(R.string.status_disconnected);
                tvStatusLabel
                        .setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.status_disconnected));
                if (layoutStatus != null)
                    layoutStatus.setBackgroundColor(Color.parseColor("#1A0A0A"));
                state = ProtocolState.IDLE;
                taskHandler.removeCallbacks(timeoutRunnable);
                progressBar.setVisibility(View.GONE);
                progressBar.setIndeterminate(false);
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
        int id = item.getItemId();
        if (id == R.id.action_about) {
            showAboutDialog();
            return true;
        } else if (id == R.id.action_pinout_pic) {
            openHardwareInfo(0);
            return true;
        } else if (id == R.id.action_i2c_conn) {
            openHardwareInfo(1);
            return true;
        } else if (id == R.id.action_spi_conn) {
            openHardwareInfo(2);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSerialConnect() {
        runOnUiThread(() -> {
            log("Puerto USB inicializado con éxito.");
            // Iniciar Ping automático al conectar
            state = ProtocolState.PINGING;
            readStream = new ByteArrayOutputStream(); // Buffer para el Ping
            serialManager.sendData(getActiveProtocol().buildPingCommand());
            resetTimeout();
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
        if (state == ProtocolState.READING) {
            try {
                int expectedForThisChunk = Math.min(READ_CHUNK_SIZE, totalSize - currentAddress);
                
                for (byte b : data) {
                    int val = b & 0xFF;
                    
                    if (val == 0x58) { // RESP_ERR
                        state = ProtocolState.IDLE;
                        log("Error de lectura: El PIC devolvió RESP_ERR.");
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            updateUIState(true);
                        });
                        return;
                    }

                    // Si ya recibimos todos los datos del chunk, el siguiente byte DEBE ser RESP_END (0x55)
                    if (readStream.size() >= (currentAddress + expectedForThisChunk)) {
                        if (val == 0x55) { // RESP_END
                            currentAddress += expectedForThisChunk;
                            runOnUiThread(() -> progressBar.setProgress(currentAddress));
                            
                            if (currentAddress >= totalSize) {
                                finishRead();
                            } else {
                                requestNextReadChunk();
                            }
                            return;
                        }
                    } else {
                        readStream.write(b);
                        // Mostrar progreso en vivo
                        byte[] currentBuffer = readStream.toByteArray();
                        hexHelper.renderThrottled(currentBuffer);
                    }
                }
                resetTimeout();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (state == ProtocolState.WRITING) {
            // El PIC responde con 'K' (0x4B) al terminar un chunk
            for (byte b : data) {
                if (b == 0x4B) { // RESP_OK ('K')
                    int model = spinnerModel.getSelectedItemPosition();
                    int pageSize = getActiveProtocol().getPageSize(model);
                    int bytesToNextBoundary = pageSize - (currentAddress % pageSize);
                    int chunkLimit = Math.min(bytesToNextBoundary, MAX_WRITE_CHUNK_SIZE);
                    int expectedForThisChunk = Math.min(chunkLimit, writeDataBuffer.length - currentAddress);

                    currentAddress += expectedForThisChunk;
                    runOnUiThread(() -> progressBar.setProgress(currentAddress));
                    sendNextWriteChunk();
                    break;
                } else if (b == 0x58) { // RESP_ERR
                    state = ProtocolState.IDLE;
                    log("Error de escritura (NACK/Error en el PIC).");
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        updateUIState(true);
                    });
                }
            }
        } else if (state == ProtocolState.ERASING) {
            for (byte b : data) {
                if (b == 0x4B) { // RESP_OK
                    finishErase();
                    break;
                } else if (b == 0x58) { // RESP_ERR
                    state = ProtocolState.IDLE;
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        progressBar.setIndeterminate(false);
                        log("Error durante el borrado (RESP_ERR).");
                        Toast.makeText(MainActivity.this, "Error al borrar", Toast.LENGTH_SHORT).show();
                    });
                    updateUIState(true);
                }
            }
        } else if (state == ProtocolState.PINGING) {
            readStream.write(data, 0, data.length);
            String incoming = new String(readStream.toByteArray(), StandardCharsets.UTF_8);
            if (incoming.contains("PICMEM v3 OK")) {
                state = ProtocolState.IDLE;
                taskHandler.removeCallbacks(timeoutRunnable);
                runOnUiThread(() -> {
                    log("Firmware v3 detectado y verificado (Ping OK).");
                    Toast.makeText(MainActivity.this, "PICMEM v3 Detectado", Toast.LENGTH_SHORT).show();
                    updateUIState(true);
                });
            }
        } else if (state == ProtocolState.SCANNING_ID) {
            if (getActiveProtocol() instanceof I2cProtocol) {
                // I2C Scan: Lista de direcciones encontradas terminada en 0xFF
                StringBuilder sb = new StringBuilder("Dispositivos I2C encontrados: ");
                for (byte b : data) {
                    if ((b & 0xFF) == 0xFF) { // Fin de lista (0xFF)
                        state = ProtocolState.IDLE;
                        taskHandler.removeCallbacks(timeoutRunnable);
                        if (readStream != null && readStream.size() > 0) {
                            log("Escaneo finalizado.");
                        } else {
                            log(sb.toString());
                        }
                        break;
                    } else if ((b & 0xFF) != 0x55) { // Evitar RESP_END si aparece
                        sb.append(String.format("0x%02X ", b));
                    }
                }
            } else {
                // SPI JEDEC ID: 3 bytes (ManID, MemType, MemCap)
                readStream.write(data, 0, data.length);
                byte[] accumulated = readStream.toByteArray();
                if (accumulated.length >= 3) {
                    state = ProtocolState.IDLE;
                    taskHandler.removeCallbacks(timeoutRunnable);
                    String info = String.format("SPI JEDEC ID: %02X %02X %02X", accumulated[0], accumulated[1], accumulated[2]);
                    log(info);
                }
            }
        } else if (state == ProtocolState.FULL_DUMPING) {
            for (byte b : data) {
                int val = b & 0xFF;
                if (val == 0x58) { // RESP_ERR
                    state = ProtocolState.IDLE;
                    log("Error: El PIC abortó el volcado completo (RESP_ERR).");
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        updateUIState(true);
                    });
                    return;
                }
                
                if (val == 0x55 && readStream.size() >= totalSize) { // RESP_END
                    finishRead();
                    return;
                } else if (readStream.size() < totalSize) {
                    readStream.write(b);
                    if (readStream.size() % 64 == 0) {
                        runOnUiThread(() -> progressBar.setProgress(readStream.size()));
                        hexHelper.renderThrottled(readStream.toByteArray());
                    }
                }
            }
            resetTimeout();
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

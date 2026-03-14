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

    private static final String TAG = "MainActivity";

    private UsbSerialManager serialManager;

    private Button btnConnect, btnDisconnect, btnRead, btnWrite, btnErase,
                   btnVerify, btnSave, btnClearLog, btnScan, btnFullDump;
    private Spinner spinnerProtocol, spinnerModel;
    private TextView tvStatusLabel, tvInstructions;
    private View statusDot, layoutStatus;

    // ─── NOTA: ProgressBar ELIMINADA de la actividad principal.
    // Solo se usa la barra del PopupWindow (popupProgressBar en popup_hex_viewer.xml).
    // ─────────────────────────────────────────────────────────

    private volatile ProtocolState state = ProtocolState.IDLE;

    private LogHelper logHelper;
    private HexViewerHelper hexHelper;

    private byte[] eepromBuffer;
    private byte[] writeDataBuffer;
    private volatile int currentAddress = 0;
    private volatile int totalSize      = 0;
    private ByteArrayOutputStream readStream;

    private final int MAX_WRITE_CHUNK_SIZE = 64;
    private final int READ_CHUNK_SIZE      = 64;

    // ─── Handler siempre en el hilo principal ───────────────────────────────
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable timeoutRunnable;

    private final I2cProtocol i2cProtocol = new I2cProtocol();
    private final SpiProtocol spiProtocol = new SpiProtocol();

    private androidx.activity.result.ActivityResultLauncher<Intent> filePickerLauncher;

    // =========================================================================
    // LIFECYCLE
    // =========================================================================

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
                        if (uri != null) prepareWriteData(uri);
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

        // Timeout genérico: si en 10 s no llega respuesta, resetear estado
        timeoutRunnable = () -> {
            if (state != ProtocolState.IDLE) {
                log("⚠ Tiempo de espera agotado (estado: " + state + ").");
                Toast.makeText(this, "Timeout — sin respuesta del PIC", Toast.LENGTH_SHORT).show();
                state = ProtocolState.IDLE;
                updateUIState(serialManager.isConnected());
                hexHelper.dismiss();
            }
        };
    }

    private void initViews() {
        btnConnect    = findViewById(R.id.btnConnect);
        btnDisconnect = findViewById(R.id.btnDisconnect);
        btnRead       = findViewById(R.id.btnRead);
        btnWrite      = findViewById(R.id.btnWrite);
        btnErase      = findViewById(R.id.btnErase);
        btnVerify     = findViewById(R.id.btnVerify);
        btnSave       = findViewById(R.id.btnSave);
        btnClearLog   = findViewById(R.id.btnClearLog);
        btnScan       = findViewById(R.id.btnScan);
        btnFullDump   = findViewById(R.id.btnFullDump);

        spinnerProtocol = findViewById(R.id.spinnerProtocol);
        spinnerModel    = findViewById(R.id.spinnerModel);

        tvStatusLabel   = findViewById(R.id.tvStatusLabel);
        tvInstructions  = findViewById(R.id.tvInstructions);

        TextView tvLog     = findViewById(R.id.tvLog);
        ScrollView scrollLog = findViewById(R.id.scrollLog);
        statusDot   = findViewById(R.id.statusDot);
        layoutStatus = findViewById(R.id.layoutStatus);

        logHelper = new LogHelper(this, tvLog, scrollLog);
        hexHelper = new HexViewerHelper(this);
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> protAdapter = ArrayAdapter.createFromResource(
                this, R.array.eeprom_protocols, android.R.layout.simple_spinner_item);
        protAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProtocol.setAdapter(protAdapter);

        spinnerProtocol.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                updateModelSpinner(pos);
                tvInstructions.setText(getActiveProtocol().getHardwareInstructions());
            }
            @Override
            public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private EepromProtocol getActiveProtocol() {
        return spinnerProtocol.getSelectedItemPosition() == 0 ? i2cProtocol : spiProtocol;
    }

    private void updateModelSpinner(int protocolIndex) {
        int arrayResId = (protocolIndex == 0) ? R.array.eeprom_i2c_sizes : R.array.eeprom_spi_sizes;
        ArrayAdapter<CharSequence> modelAdapter = ArrayAdapter.createFromResource(
                this, arrayResId, android.R.layout.simple_spinner_item);
        modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerModel.setAdapter(modelAdapter);
    }

    // =========================================================================
    // CONEXIÓN SERIAL
    // =========================================================================

    private void connectSerial() {
        serialManager.setSerialParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
        log("Conectando a 9600 baud...");
        serialManager.connect();
    }

    private void disconnectSerial() {
        log("Desconectando...");
        serialManager.disconnect();
    }

    private void log(final String message) {
        if (logHelper != null) logHelper.log(message);
    }

    // =========================================================================
    // LECTURA (chunk-based)
    // Protocolo: 49 52 ... → [datos 64B] 55 → siguiente chunk → ... → 55 final
    // =========================================================================

    private void startRead() {
        if (!serialManager.isConnected()) {
            Toast.makeText(this, "Conecte el dispositivo primero", Toast.LENGTH_SHORT).show();
            return;
        }
        state = ProtocolState.READING;
        currentAddress = 0;
        readStream = new ByteArrayOutputStream();

        int model = spinnerModel.getSelectedItemPosition();
        totalSize = getActiveProtocol().getTotalSize(model);

        log("Leyendo " + totalSize + " bytes en bloques de " + READ_CHUNK_SIZE + " B...");
        hexHelper.showPopup("Leyendo memoria...", totalSize);
        updateUIState(true);
        requestNextReadChunk();
    }

    private void requestNextReadChunk() {
        if (state != ProtocolState.READING) return;
        if (currentAddress >= totalSize) {
            finishRead();
            return;
        }
        int len = Math.min(READ_CHUNK_SIZE, totalSize - currentAddress);
        int modelIdx = spinnerModel.getSelectedItemPosition();
        byte[] cmd = getActiveProtocol().buildReadCommand(currentAddress, len, modelIdx);
        serialManager.sendData(cmd);
        resetTimeout(8000);   // 8 s por chunk
    }

    // ─── CORRECCIÓN CRÍTICA: finishRead() siempre en hilo principal ──────────
    private void finishRead() {
        mainHandler.post(() -> {
            state = ProtocolState.IDLE;
            cancelTimeout();
            eepromBuffer = readStream.toByteArray();
            log("✓ Lectura completada: " + eepromBuffer.length + " bytes.");
            Toast.makeText(this, "Lectura completada", Toast.LENGTH_SHORT).show();
            updateUIState(true);
            hexHelper.renderNow(eepromBuffer);
            hexHelper.updateProgress(eepromBuffer.length);
        });
    }

    // =========================================================================
    // ESCRITURA
    // Protocolo: 49 57 ... + [datos] → 4B (OK por chunk) → siguiente chunk
    // =========================================================================

    private void startWrite() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        filePickerLauncher.launch(intent);
    }

    private void prepareWriteData(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            if (is == null) return;

            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] tmp = new byte[1024];
            int n;
            while ((n = is.read(tmp, 0, tmp.length)) != -1) buf.write(tmp, 0, n);
            buf.flush();
            byte[] rawData = buf.toByteArray();
            is.close();

            int model    = spinnerModel.getSelectedItemPosition();
            totalSize    = getActiveProtocol().getTotalSize(model);
            String fName = "Archivo";

            android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (idx != -1) fName = cursor.getString(idx);
                cursor.close();
            }

            log("Archivo: " + fName + " (" + rawData.length + " B)");

            if (fName.toLowerCase(java.util.Locale.ROOT).endsWith(".hex") ||
                    (rawData.length > 0 && rawData[0] == ':')) {
                log("Formato Intel Hex detectado, parseando...");
                try {
                    writeDataBuffer = IntelHexFormat.parseIntelHex(rawData, totalSize);
                    log("Hex procesado: " + writeDataBuffer.length + " bytes.");
                } catch (HexParseException ex) {
                    log("Error Hex: " + ex.getMessage() + " — tratando como binario.");
                    writeDataBuffer = rawData;
                }
            } else {
                writeDataBuffer = rawData;
            }

            if (writeDataBuffer.length > totalSize) {
                log("Error: archivo (" + writeDataBuffer.length + " B) > memoria (" + totalSize + " B).");
                Toast.makeText(this, "Archivo excede la capacidad", Toast.LENGTH_LONG).show();
                return;
            }

            log("Escribiendo " + writeDataBuffer.length + " bytes...");
            state = ProtocolState.WRITING;
            currentAddress = 0;
            hexHelper.showPopup("Escribiendo memoria...", writeDataBuffer.length);
            updateUIState(true);
            sendNextWriteChunk();

        } catch (Exception e) {
            Log.e(TAG, "prepareWriteData", e);
            Toast.makeText(this, "Error leyendo archivo: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void sendNextWriteChunk() {
        if (state != ProtocolState.WRITING) return;
        if (currentAddress >= writeDataBuffer.length) {
            finishWrite();
            return;
        }
        int model              = spinnerModel.getSelectedItemPosition();
        int pageSize           = getActiveProtocol().getPageSize(model);
        int bytesToNextBound   = pageSize - (currentAddress % pageSize);
        int chunkLimit         = Math.min(bytesToNextBound, MAX_WRITE_CHUNK_SIZE);
        int len                = Math.min(chunkLimit, writeDataBuffer.length - currentAddress);

        try {
            ByteArrayOutputStream cmdStream = new ByteArrayOutputStream();
            cmdStream.write(getActiveProtocol().buildWriteCommandBase(currentAddress, len, model));
            cmdStream.write(writeDataBuffer, currentAddress, len);
            serialManager.sendData(cmdStream.toByteArray());
            resetTimeout(8000);
        } catch (Exception e) {
            Log.e(TAG, "sendNextWriteChunk", e);
        }
    }

    // ─── CORRECCIÓN CRÍTICA: finishWrite() siempre en hilo principal ─────────
    private void finishWrite() {
        mainHandler.post(() -> {
            state = ProtocolState.IDLE;
            cancelTimeout();
            log("✓ Escritura completada: " + writeDataBuffer.length + " bytes.");
            Toast.makeText(this, "Escritura completada", Toast.LENGTH_SHORT).show();
            updateUIState(true);
            hexHelper.updateProgress(writeDataBuffer.length);
            hexHelper.renderNow(writeDataBuffer);
        });
    }

    // =========================================================================
    // BORRADO
    // I2C: sobrescritura con 0xFF (reusa flujo de escritura)
    // SPI: 50 45 → esperar 4B (OK) o 58 (ERR)
    // =========================================================================

    private void startErase() {
        if (!serialManager.isConnected()) return;
        int modelIdx = spinnerModel.getSelectedItemPosition();
        byte[] cmd   = getActiveProtocol().buildEraseCommand(modelIdx);

        if (cmd == null) {
            // I2C: rellenar con 0xFF
            log("Borrando I2C (sobrescritura con 0xFF)...");
            totalSize      = getActiveProtocol().getTotalSize(modelIdx);
            writeDataBuffer = new byte[totalSize];
            java.util.Arrays.fill(writeDataBuffer, (byte) 0xFF);
            state          = ProtocolState.WRITING;
            currentAddress = 0;
            hexHelper.showPopup("Borrando I2C (0xFF)...", totalSize);
            updateUIState(true);
            sendNextWriteChunk();
        } else {
            // SPI Chip Erase nativo
            log("Iniciando Chip Erase SPI...");
            state = ProtocolState.ERASING;
            serialManager.sendData(cmd);
            hexHelper.showPopup("Borrando chip SPI... (puede tardar minutos)", 1);
            updateUIState(true);
            resetTimeout(180000);  // 3 min para chips grandes
        }
    }

    // ─── CORRECCIÓN CRÍTICA: finishErase() siempre en hilo principal ─────────
    private void finishErase() {
        mainHandler.post(() -> {
            state = ProtocolState.IDLE;
            cancelTimeout();
            log("✓ Borrado completado.");
            Toast.makeText(this, "Borrado completado", Toast.LENGTH_SHORT).show();
            updateUIState(serialManager.isConnected());
            hexHelper.dismiss();
        });
    }

    // =========================================================================
    // VERIFICACIÓN (local, sin UART)
    // =========================================================================

    private void startVerify() {
        if (eepromBuffer == null || writeDataBuffer == null) {
            Toast.makeText(this, "Se necesitan datos leídos y de archivo", Toast.LENGTH_SHORT).show();
            return;
        }
        log("Verificando...");
        int size   = Math.min(eepromBuffer.length, writeDataBuffer.length);
        int errors = 0;
        for (int i = 0; i < size; i++) {
            if (eepromBuffer[i] != writeDataBuffer[i]) {
                if (errors < 10)
                    log(String.format("  Diff 0x%06X: leído 0x%02X esperado 0x%02X",
                            i, eepromBuffer[i] & 0xFF, writeDataBuffer[i] & 0xFF));
                errors++;
            }
        }
        if (errors == 0) {
            log("✓ VERIFICACIÓN OK: los datos coinciden.");
            Toast.makeText(this, "Verificación exitosa", Toast.LENGTH_SHORT).show();
        } else {
            log("✗ " + errors + " diferencias encontradas.");
            Toast.makeText(this, errors + " diferencias", Toast.LENGTH_LONG).show();
        }
    }

    // =========================================================================
    // SCAN / ID
    // I2C Scan : 49 53 → [addr7]... FF
    // SPI JEDEC: 50 4A → [Mfr][Type][Cap]
    // =========================================================================

    private void startScan() {
        if (!serialManager.isConnected()) return;
        state = ProtocolState.SCANNING_ID;
        readStream = new ByteArrayOutputStream();
        byte[] cmd = getActiveProtocol().buildScanOrIdCommand();
        log((getActiveProtocol() instanceof I2cProtocol) ? "Escaneando bus I2C..." : "Leyendo JEDEC ID...");
        serialManager.sendData(cmd);
        resetTimeout(10000);   // Scan puede tardar hasta ~0.5 s × 112 dir
        updateUIState(true);
    }

    // =========================================================================
    // FULL DUMP
    // I2C: 49 46 addr_len chip_addr LH LL → [bytes] 55
    // SPI: 50 46 → (JEDEC auto) → [bytes] 55
    // =========================================================================

    private void startFullDump() {
        if (!serialManager.isConnected()) return;
        state = ProtocolState.FULL_DUMPING;
        currentAddress = 0;
        readStream = new ByteArrayOutputStream();

        int model = spinnerModel.getSelectedItemPosition();
        totalSize = getActiveProtocol().getTotalSize(model);

        // Para SPI el PIC calcula el tamaño por JEDEC; totalSize es solo para la barra
        if (getActiveProtocol() instanceof I2cProtocol && totalSize > 65535)
            totalSize = 65535;

        log("Volcado completo iniciado (" + totalSize + " bytes estimados)...");
        hexHelper.showPopup("Volcado completo...", totalSize);

        byte[] cmd = getActiveProtocol().buildFullDumpCommand(model);
        serialManager.sendData(cmd);
        updateUIState(true);
        resetTimeout(15000);   // Timeout inicial; se renueva con cada paquete recibido
    }

    // =========================================================================
    // GUARDAR
    // =========================================================================

    private void clearLog() {
        if (logHelper != null) logHelper.clear();
        log("Log limpiado.");
    }

    private void saveBuffer() {
        try {
            File romDir = FileManager.saveMemoryDump(eepromBuffer);
            log("Guardado en " + romDir.getAbsolutePath());
            Toast.makeText(this, "Guardado en Descargas/rom/", Toast.LENGTH_LONG).show();
        } catch (IllegalArgumentException e) {
            log("Error: " + e.getMessage());
            Toast.makeText(this, "No hay datos para guardar", Toast.LENGTH_SHORT).show();
        } catch (java.io.IOException e) {
            log("Error I/O: " + e.getMessage());
            Toast.makeText(this, "Error guardando: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // =========================================================================
    // TIMEOUT
    // =========================================================================

    private void resetTimeout(long millis) {
        mainHandler.removeCallbacks(timeoutRunnable);
        mainHandler.postDelayed(timeoutRunnable, millis);
    }

    private void cancelTimeout() {
        mainHandler.removeCallbacks(timeoutRunnable);
    }

    // =========================================================================
    // UI STATE
    // =========================================================================

    private void updateUIState(final boolean connected) {
        // Siempre en hilo principal
        runOnUiThread(() -> {
            btnConnect.setEnabled(!connected);
            btnDisconnect.setEnabled(connected);

            boolean busy = (state != ProtocolState.IDLE);

            btnRead.setEnabled(connected && !busy);
            btnWrite.setEnabled(connected && !busy);
            btnErase.setEnabled(connected && !busy);
            btnScan.setEnabled(connected && !busy);
            btnFullDump.setEnabled(connected && !busy);
            btnVerify.setEnabled(!busy && eepromBuffer != null && writeDataBuffer != null);
            btnSave.setEnabled(!busy && eepromBuffer != null);
            spinnerProtocol.setEnabled(!busy);
            spinnerModel.setEnabled(!busy);

            if (connected) {
                if (statusDot != null)
                    statusDot.setBackgroundColor(
                            androidx.core.content.ContextCompat.getColor(this, R.color.status_connected));
                if (tvStatusLabel != null) {
                    tvStatusLabel.setText(R.string.status_connected);
                    tvStatusLabel.setTextColor(
                            androidx.core.content.ContextCompat.getColor(this, R.color.status_connected));
                }
                if (layoutStatus != null)
                    layoutStatus.setBackgroundColor(Color.parseColor("#112211"));
            } else {
                if (statusDot != null)
                    statusDot.setBackgroundColor(
                            androidx.core.content.ContextCompat.getColor(this, R.color.status_disconnected));
                if (tvStatusLabel != null) {
                    tvStatusLabel.setText(R.string.status_disconnected);
                    tvStatusLabel.setTextColor(
                            androidx.core.content.ContextCompat.getColor(this, R.color.status_disconnected));
                }
                if (layoutStatus != null)
                    layoutStatus.setBackgroundColor(Color.parseColor("#1A0A0A"));
                state = ProtocolState.IDLE;
                cancelTimeout();
            }
        });
    }

    // =========================================================================
    // USB SERIAL CALLBACKS  (todos en hilo de fondo — NO tocar UI directamente)
    // =========================================================================

    @Override
    public void onSerialConnect() {
        // Ping automático al conectar
        state = ProtocolState.PINGING;
        readStream = new ByteArrayOutputStream();
        serialManager.sendData(getActiveProtocol().buildPingCommand());
        resetTimeout(3000);
        updateUIState(true);
        log("Puerto USB abierto. Enviando ping...");
    }

    @Override
    public void onSerialConnectError(Exception e) {
        log("Error de conexión: " + e.getMessage());
        runOnUiThread(() ->
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
        updateUIState(false);
    }

    @Override
    public void onSerialRead(byte[] data) {
        // ─── IMPORTANTE: este método corre en el hilo de SerialInputOutputManager.
        // NO se deben tocar vistas directamente. Usar mainHandler.post() o
        // runOnUiThread() para cualquier operación de UI.
        // Los métodos log(), hexHelper.renderXxx(), hexHelper.updateProgress()
        // ya usan runOnUiThread internamente.
        // finishRead/Write/Erase usan mainHandler.post().
        // ──────────────────────────────────────────────────────────────────────

        switch (state) {

            // -----------------------------------------------------------------
            // PING — espera "PICMEM v3 OK\r\n"
            // -----------------------------------------------------------------
            case PINGING: {
                readStream.write(data, 0, data.length);
                String incoming = new String(readStream.toByteArray(), StandardCharsets.UTF_8);
                if (incoming.contains("PICMEM")) {
                    state = ProtocolState.IDLE;
                    cancelTimeout();
                    log("✓ Firmware detectado: " +
                            incoming.replace("\r", "").replace("\n", " ").trim());
                    runOnUiThread(() ->
                            Toast.makeText(this, "PICMEM v3 Detectado", Toast.LENGTH_SHORT).show());
                    updateUIState(true);
                }
                break;
            }

            // -----------------------------------------------------------------
            // READING — lectura por chunks de READ_CHUNK_SIZE
            // Protocolo por chunk: → cmd → [N bytes datos] → 0x55(RESP_END)
            // -----------------------------------------------------------------
            case READING: {
                int expectedThisChunk = Math.min(READ_CHUNK_SIZE, totalSize - currentAddress);

                for (byte b : data) {
                    int val = b & 0xFF;

                    if (val == 0x58) { // RESP_ERR del PIC
                        state = ProtocolState.IDLE;
                        log("✗ Error de lectura I2C/SPI (RESP_ERR).");
                        cancelTimeout();
                        runOnUiThread(() -> {
                            updateUIState(true);
                            hexHelper.dismiss();
                        });
                        return;
                    }

                    // ¿Ya recibimos los N bytes del chunk? → el siguiente es RESP_END
                    if (readStream.size() >= (currentAddress + expectedThisChunk)) {
                        if (val == 0x55) { // RESP_END
                            currentAddress += expectedThisChunk;
                            final int prog = currentAddress;
                            runOnUiThread(() -> hexHelper.updateProgress(prog));
                            hexHelper.renderThrottled(readStream.toByteArray());

                            if (currentAddress >= totalSize) {
                                finishRead();   // → mainHandler.post → hilo UI
                            } else {
                                requestNextReadChunk();
                            }
                            return;
                        }
                        // 0x55 como dato antes de tiempo → escribir igualmente
                    }
                    readStream.write(b);
                }
                resetTimeout(8000);
                break;
            }

            // -----------------------------------------------------------------
            // WRITING — chunk enviado; espera 0x4B (OK) para enviar el siguiente
            // -----------------------------------------------------------------
            case WRITING: {
                for (byte b : data) {
                    if (b == 0x4B) { // RESP_OK
                        int model        = spinnerModel.getSelectedItemPosition();
                        int pageSize     = getActiveProtocol().getPageSize(model);
                        int toNext       = pageSize - (currentAddress % pageSize);
                        int chunkLimit   = Math.min(toNext, MAX_WRITE_CHUNK_SIZE);
                        int expectedChunk = Math.min(chunkLimit, writeDataBuffer.length - currentAddress);

                        currentAddress += expectedChunk;
                        final int prog = currentAddress;
                        runOnUiThread(() -> hexHelper.updateProgress(prog));

                        if (currentAddress >= writeDataBuffer.length) {
                            finishWrite();  // → mainHandler.post → hilo UI
                        } else {
                            sendNextWriteChunk();
                        }
                        return;

                    } else if (b == 0x58) { // RESP_ERR
                        state = ProtocolState.IDLE;
                        log("✗ Error de escritura (NACK del dispositivo).");
                        cancelTimeout();
                        runOnUiThread(() -> {
                            updateUIState(true);
                            hexHelper.dismiss();
                        });
                        return;
                    }
                }
                resetTimeout(8000);
                break;
            }

            // -----------------------------------------------------------------
            // ERASING — espera 0x4B (OK) o 0x58 (ERR)
            // -----------------------------------------------------------------
            case ERASING: {
                for (byte b : data) {
                    if (b == 0x4B) {
                        finishErase();  // → mainHandler.post → hilo UI
                        return;
                    } else if (b == 0x58) {
                        state = ProtocolState.IDLE;
                        log("✗ Error durante el borrado (RESP_ERR).");
                        cancelTimeout();
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Error al borrar", Toast.LENGTH_SHORT).show();
                            updateUIState(true);
                            hexHelper.dismiss();
                        });
                        return;
                    }
                }
                // Chip Erase puede tardar; renovar timeout mientras no llegue respuesta
                resetTimeout(180000);
                break;
            }

            // -----------------------------------------------------------------
            // SCANNING_ID — I2C Scan (→ [addr]... 0xFF) o JEDEC (→ 3 bytes)
            // -----------------------------------------------------------------
            case SCANNING_ID: {
                if (getActiveProtocol() instanceof I2cProtocol) {
                    // I2C Scan: lista de dir. terminada en 0xFF
                    for (byte b : data) {
                        int val = b & 0xFF;
                        if (val == 0xFF) {
                            state = ProtocolState.IDLE;
                            cancelTimeout();
                            byte[] found = readStream.toByteArray();
                            StringBuilder sb = new StringBuilder("I2C Scan → ");
                            if (found.length == 0) {
                                sb.append("Sin dispositivos.");
                            } else {
                                for (byte addr : found)
                                    sb.append(String.format("0x%02X ", addr & 0xFF));
                            }
                            log(sb.toString());
                            updateUIState(true);
                            return;
                        }
                        if (val != 0x55 && val != 0x58) readStream.write(b);
                    }
                } else {
                    // SPI JEDEC ID: 3 bytes [Mfr][MemType][Capacity]
                    readStream.write(data, 0, data.length);
                    if (readStream.toByteArray().length >= 3) {
                        state = ProtocolState.IDLE;
                        cancelTimeout();
                        byte[] j = readStream.toByteArray();
                        log(String.format("JEDEC ID: %02X %02X %02X",
                                j[0] & 0xFF, j[1] & 0xFF, j[2] & 0xFF));
                        updateUIState(true);
                    }
                }
                break;
            }

            // -----------------------------------------------------------------
            // FULL_DUMPING
            // I2C: 49 46 → [N bytes exactos] → 0x55
            // SPI: 50 46 → (JEDEC auto) → [M bytes] → 0x55
            //
            // ESTRATEGIA:
            //   • Escribir todos los bytes hasta totalSize.
            //   • Después de totalSize bytes, el siguiente 0x55 es RESP_END.
            //   • Un 0x58 en cualquier momento = error.
            // -----------------------------------------------------------------
            case FULL_DUMPING: {
                for (byte b : data) {
                    int val = b & 0xFF;

                    // Error del PIC
                    if (val == 0x58 && readStream.size() == 0) {
                        // Solo tratar como error si llega inmediatamente (chip no detectado)
                        state = ProtocolState.IDLE;
                        log("✗ Error Full Dump (RESP_ERR). Verifica chip y selección de modelo.");
                        cancelTimeout();
                        runOnUiThread(() -> {
                            updateUIState(true);
                            hexHelper.dismiss();
                        });
                        return;
                    }

                    // RESP_END: llega DESPUÉS de que hayamos recibido todos los datos
                    if (val == 0x55 && readStream.size() >= totalSize) {
                        finishRead();   // → mainHandler.post → hilo UI
                        return;
                    }

                    // Guardar byte de datos (límite por totalSize para seguridad)
                    if (readStream.size() < totalSize) {
                        readStream.write(b);

                        // Actualizar progreso en UI cada 256 bytes
                        int sz = readStream.size();
                        if ((sz & 0xFF) == 0) {
                            final int prog = sz;
                            runOnUiThread(() -> hexHelper.updateProgress(prog));
                        }
                        // Actualizar visor hex cada 2 KB
                        if ((sz & 0x7FF) == 0) {
                            hexHelper.renderThrottled(readStream.toByteArray());
                        }
                    }
                }
                // Datos recibidos → renovar timeout (5 s desde el último paquete)
                resetTimeout(5000);
                break;
            }

            default:
                break;
        }
    }

    @Override
    public void onSerialIoError(Exception e) {
        // Llamado desde hilo de SerialInputOutputManager — usar runOnUiThread
        Log.e(TAG, "onSerialIoError", e);
        state = ProtocolState.IDLE;
        cancelTimeout();
        log("Error I/O serial: " + e.getMessage());
        runOnUiThread(() -> {
            Toast.makeText(this, "Error de comunicación serial", Toast.LENGTH_SHORT).show();
            updateUIState(false);
            hexHelper.dismiss();
        });
    }

    @Override
    public void onSerialDisconnect() {
        state = ProtocolState.IDLE;
        cancelTimeout();
        log("Dispositivo desconectado.");
        runOnUiThread(() -> {
            Toast.makeText(this, "Desconectado", Toast.LENGTH_SHORT).show();
            updateUIState(false);
            hexHelper.dismiss();
        });
    }

    // =========================================================================
    // MENÚ
    // =========================================================================

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

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.about_title)
                .setMessage(R.string.about_message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void openHardwareInfo(int type) {
        Intent intent = new Intent(this,
                com.mobincube.pronosticos_parley_copy.sc_55UCEB.ui.HardwareInfoActivity.class);
        intent.putExtra("type", type);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        serialManager.cleanup();
        mainHandler.removeCallbacksAndMessages(null);
    }
}

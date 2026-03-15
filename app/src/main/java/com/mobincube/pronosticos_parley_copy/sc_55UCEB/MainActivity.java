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
import androidx.appcompat.app.AppCompatActivity;

import com.mobincube.pronosticos_parley_copy.sc_55UCEB.eeprom.EepromProtocol;
import com.mobincube.pronosticos_parley_copy.sc_55UCEB.eeprom.I2cProtocol;
import com.mobincube.pronosticos_parley_copy.sc_55UCEB.eeprom.SpiProtocol;
import com.mobincube.pronosticos_parley_copy.sc_55UCEB.exception.HexParseException;
import com.mobincube.pronosticos_parley_copy.sc_55UCEB.file.FileManager;
import com.mobincube.pronosticos_parley_copy.sc_55UCEB.file.IntelHexFormat;
import com.mobincube.pronosticos_parley_copy.sc_55UCEB.ui.AboutActivity;
import com.mobincube.pronosticos_parley_copy.sc_55UCEB.ui.FirmwareActivity;
import com.mobincube.pronosticos_parley_copy.sc_55UCEB.ui.HexViewerHelper;
import com.mobincube.pronosticos_parley_copy.sc_55UCEB.ui.LogHelper;
import com.mobincube.pronosticos_parley_copy.sc_55UCEB.ui.PrivacyPolicyActivity;
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
    private Spinner  spinnerProtocol, spinnerModel;
    private TextView tvStatusLabel,   tvInstructions;
    private View     statusDot,        layoutStatus;

    // ── Estado de la máquina ────────────────────────────────────────────────
    private volatile ProtocolState state = ProtocolState.IDLE;

    // ── Caché del protocolo activo ──────────────────────────────────────────
    private volatile EepromProtocol cachedProtocol;
    private volatile int            cachedModelIndex;
    private volatile int            cachedPageSize;

    // ── Full Dump SPI ───────────────────────────────────────────────────────
    private volatile boolean pendingFullDump = false;

    // ── Helpers de UI ───────────────────────────────────────────────────────
    private LogHelper       logHelper;
    private HexViewerHelper hexHelper;

    // ── Buffers y contadores ────────────────────────────────────────────────
    private byte[]                writeDataBuffer;
    private byte[]                eepromBuffer;
    private volatile int          currentAddress = 0;
    private volatile int          totalSize      = 0;
    private ByteArrayOutputStream readStream;

    private static final int MAX_WRITE_CHUNK = 64;
    private static final int READ_CHUNK      = 64;

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

        btnConnect.setOnClickListener(v    -> connectSerial());
        btnDisconnect.setOnClickListener(v -> disconnectSerial());
        btnRead.setOnClickListener(v       -> startRead());
        btnWrite.setOnClickListener(v      -> startWrite());
        btnErase.setOnClickListener(v      -> startErase());
        btnVerify.setOnClickListener(v     -> startVerify());
        btnSave.setOnClickListener(v       -> saveBuffer());
        btnClearLog.setOnClickListener(v   -> clearLog());
        btnScan.setOnClickListener(v       -> startScan());
        btnFullDump.setOnClickListener(v   -> startFullDump());

        timeoutRunnable = () -> {
            if (state != ProtocolState.IDLE) {
                log("⚠ Timeout sin respuesta (estado: " + state + ").");
                if (state == ProtocolState.FULL_DUMPING)
                    log("  SPI: verifica que el modelo coincida con el chip real.");
                Toast.makeText(this, "Timeout — sin respuesta del PIC", Toast.LENGTH_SHORT).show();
                state = ProtocolState.IDLE;
                pendingFullDump = false;
                updateUIState(serialManager.isConnected());
                hexHelper.dismiss();
            }
        };
    }

    // ── initViews ─────────────────────────────────────────────────────────
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
        statusDot       = findViewById(R.id.statusDot);
        layoutStatus    = findViewById(R.id.layoutStatus);

        TextView   tvLog   = findViewById(R.id.tvLog);
        ScrollView scroll  = findViewById(R.id.scrollLog);
        logHelper  = new LogHelper(this, tvLog, scroll);
        hexHelper  = new HexViewerHelper(this);
    }

    // ── Spinners ──────────────────────────────────────────────────────────
    private void setupSpinners() {
        ArrayAdapter<CharSequence> pa = ArrayAdapter.createFromResource(
                this, R.array.eeprom_protocols, android.R.layout.simple_spinner_item);
        pa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProtocol.setAdapter(pa);

        spinnerProtocol.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                updateModelSpinner(pos);
                tvInstructions.setText(getActiveProtocol().getHardwareInstructions());
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private EepromProtocol getActiveProtocol() {
        return spinnerProtocol.getSelectedItemPosition() == 0 ? i2cProtocol : spiProtocol;
    }

    private void updateModelSpinner(int protocolIndex) {
        int res = (protocolIndex == 0) ? R.array.eeprom_i2c_sizes : R.array.eeprom_spi_sizes;
        ArrayAdapter<CharSequence> a = ArrayAdapter.createFromResource(
                this, res, android.R.layout.simple_spinner_item);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerModel.setAdapter(a);
    }

    private void cacheProtocol() {
        cachedProtocol   = getActiveProtocol();
        cachedModelIndex = spinnerModel.getSelectedItemPosition();
        cachedPageSize   = cachedProtocol.getPageSize(cachedModelIndex);
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

    private void log(String msg) { if (logHelper != null) logHelper.log(msg); }

    // =========================================================================
    // LECTURA por chunks
    // =========================================================================

    private void startRead() {
        if (!serialManager.isConnected()) {
            Toast.makeText(this, "Conecte el dispositivo primero", Toast.LENGTH_SHORT).show();
            return;
        }
        cacheProtocol();
        totalSize      = cachedProtocol.getTotalSize(cachedModelIndex);
        currentAddress = 0;
        readStream     = new ByteArrayOutputStream();
        state          = ProtocolState.READING;

        log("Leyendo " + totalSize + " bytes [" + spinnerModel.getSelectedItem() + "]...");
        hexHelper.showPopup("Leyendo memoria...", totalSize);
        updateUIState(true);
        requestNextReadChunk();
    }

    private void requestNextReadChunk() {
        if (state != ProtocolState.READING) return;
        if (currentAddress >= totalSize)    { finishRead(); return; }
        int len = Math.min(READ_CHUNK, totalSize - currentAddress);
        serialManager.sendData(
                cachedProtocol.buildReadCommand(currentAddress, len, cachedModelIndex));
        resetTimeout(10000);
    }

    private void finishRead() {
        mainHandler.post(() -> {
            state = ProtocolState.IDLE;
            cancelTimeout();
            eepromBuffer = readStream.toByteArray();
            log("✓ Lectura completada: " + eepromBuffer.length + " bytes.");
            Toast.makeText(this, "Lectura completada", Toast.LENGTH_SHORT).show();
            updateUIState(true);
            hexHelper.renderFinal(eepromBuffer);
            hexHelper.updateProgress(eepromBuffer.length);
        });
    }

    // =========================================================================
    // ESCRITURA por chunks
    // =========================================================================

    private void startWrite() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("*/*");
        filePickerLauncher.launch(i);
    }

    private void prepareWriteData(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            if (is == null) return;
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] tmp = new byte[1024]; int n;
            while ((n = is.read(tmp, 0, tmp.length)) != -1) buf.write(tmp, 0, n);
            buf.flush(); is.close();
            byte[] raw = buf.toByteArray();

            cacheProtocol();
            int memSize = cachedProtocol.getTotalSize(cachedModelIndex);
            String fname = "Archivo";
            android.database.Cursor c =
                    getContentResolver().query(uri, null, null, null, null);
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (idx != -1) fname = c.getString(idx);
                c.close();
            }
            log("Archivo: " + fname + " (" + raw.length + " B)");

            if (fname.toLowerCase(java.util.Locale.ROOT).endsWith(".hex")
                    || (raw.length > 0 && raw[0] == ':')) {
                log("Intel Hex detectado, parseando...");
                try {
                    writeDataBuffer = IntelHexFormat.parseIntelHex(raw, memSize);
                    log("Hex → " + writeDataBuffer.length + " bytes.");
                } catch (HexParseException ex) {
                    log("Hex parse error: " + ex.getMessage() + " — usando binario.");
                    writeDataBuffer = raw;
                }
            } else {
                writeDataBuffer = raw;
            }

            if (writeDataBuffer.length > memSize) {
                log("Error: archivo (" + writeDataBuffer.length + " B) > memoria (" + memSize + " B).");
                Toast.makeText(this, "Archivo excede la capacidad", Toast.LENGTH_LONG).show();
                return;
            }

            totalSize = writeDataBuffer.length;
            state = ProtocolState.WRITING;
            currentAddress = 0;
            log("Escribiendo " + totalSize + " bytes...");
            hexHelper.showPopup("Escribiendo memoria...", totalSize);
            updateUIState(true);
            sendNextWriteChunk();

        } catch (Exception e) {
            Log.e(TAG, "prepareWriteData", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void sendNextWriteChunk() {
        if (state != ProtocolState.WRITING) return;
        if (currentAddress >= writeDataBuffer.length) { finishWrite(); return; }

        int toNext = cachedPageSize - (currentAddress % cachedPageSize);
        int limit  = Math.min(toNext, MAX_WRITE_CHUNK);
        int len    = Math.min(limit, writeDataBuffer.length - currentAddress);

        try {
            ByteArrayOutputStream cs = new ByteArrayOutputStream();
            cs.write(cachedProtocol.buildWriteCommandBase(currentAddress, len, cachedModelIndex));
            cs.write(writeDataBuffer, currentAddress, len);
            serialManager.sendData(cs.toByteArray());
            resetTimeout(10000);
        } catch (Exception e) { Log.e(TAG, "sendNextWriteChunk", e); }
    }

    private void finishWrite() {
        mainHandler.post(() -> {
            state = ProtocolState.IDLE;
            cancelTimeout();
            log("✓ Escritura completada: " + writeDataBuffer.length + " bytes.");
            Toast.makeText(this, "Escritura completada", Toast.LENGTH_SHORT).show();
            updateUIState(true);
            hexHelper.updateProgress(writeDataBuffer.length);
            hexHelper.renderFinal(writeDataBuffer);
        });
    }

    // =========================================================================
    // BORRADO
    // =========================================================================

    private void startErase() {
        if (!serialManager.isConnected()) return;
        cacheProtocol();
        byte[] cmd = cachedProtocol.buildEraseCommand(cachedModelIndex);

        if (cmd == null) {
            int sz = cachedProtocol.getTotalSize(cachedModelIndex);
            log("Borrando I2C (" + sz + " B con 0xFF)...");
            writeDataBuffer = new byte[sz];
            java.util.Arrays.fill(writeDataBuffer, (byte) 0xFF);
            totalSize = sz; state = ProtocolState.WRITING; currentAddress = 0;
            hexHelper.showPopup("Borrando I2C (0xFF)...", sz);
            updateUIState(true);
            sendNextWriteChunk();
        } else {
            log("Chip Erase SPI (puede tardar varios minutos)...");
            state = ProtocolState.ERASING;
            serialManager.sendData(cmd);
            hexHelper.showPopup("Borrando chip SPI...", 1);
            updateUIState(true);
            resetTimeout(300000);
        }
    }

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
    // VERIFICACIÓN (local)
    // =========================================================================

    private void startVerify() {
        if (eepromBuffer == null || writeDataBuffer == null) {
            Toast.makeText(this, "Se necesitan datos leídos y archivo cargado", Toast.LENGTH_SHORT).show();
            return;
        }
        log("Verificando datos...");
        int size = Math.min(eepromBuffer.length, writeDataBuffer.length);
        int errors = 0;
        for (int i = 0; i < size; i++) {
            if (eepromBuffer[i] != writeDataBuffer[i]) {
                if (errors < 10) log(String.format("  Diff 0x%06X: leído=0x%02X esperado=0x%02X",
                        i, eepromBuffer[i] & 0xFF, writeDataBuffer[i] & 0xFF));
                errors++;
            }
        }
        if (errors == 0) {
            log("✓ VERIFICACIÓN OK: datos coinciden perfectamente.");
            Toast.makeText(this, "Verificación exitosa", Toast.LENGTH_SHORT).show();
        } else {
            log("✗ " + errors + " diferencias encontradas.");
            Toast.makeText(this, errors + " diferencias", Toast.LENGTH_LONG).show();
        }
    }

    // =========================================================================
    // SCAN / JEDEC ID
    // =========================================================================

    private void startScan() {
        if (!serialManager.isConnected()) return;
        cacheProtocol();
        state = ProtocolState.SCANNING_ID;
        readStream = new ByteArrayOutputStream();
        byte[] cmd = cachedProtocol.buildScanOrIdCommand();
        log((cachedProtocol instanceof I2cProtocol) ? "Escaneando bus I2C..." : "Leyendo JEDEC ID...");
        serialManager.sendData(cmd);
        resetTimeout(12000);
        updateUIState(true);
    }

    // =========================================================================
    // FULL DUMP
    // =========================================================================

    private void startFullDump() {
        if (!serialManager.isConnected()) return;
        cacheProtocol();

        if (cachedProtocol instanceof I2cProtocol) {
            int sz = cachedProtocol.getTotalSize(cachedModelIndex);
            if (sz > 65535) sz = 65535;
            totalSize = sz;
            currentAddress = 0;
            readStream = new ByteArrayOutputStream();
            state = ProtocolState.READING;

            log("Volcado I2C (" + sz + " B en bloques de " + READ_CHUNK + " B)...");
            hexHelper.showPopup("Volcado completo I2C...", sz);
            updateUIState(true);
            requestNextReadChunk();

        } else {
            pendingFullDump = true;
            state = ProtocolState.SCANNING_ID;
            readStream = new ByteArrayOutputStream();
            log("Detectando chip SPI via JEDEC antes del volcado...");
            serialManager.sendData(new byte[]{0x50, 0x4A});
            resetTimeout(3000);
            updateUIState(true);
        }
    }

    private void startFullDumpAfterJedec(byte mfr, byte memType, byte cap) {
        int capVal = cap & 0xFF;
        if (mfr == (byte) 0xFF || mfr == 0x00 || capVal < 0x10 || capVal > 0x1C) {
            log(String.format("✗ JEDEC inválido (%02X %02X %02X). Verifica CS/SCK/MOSI/MISO.",
                    mfr & 0xFF, memType & 0xFF, capVal));
            state = ProtocolState.IDLE;
            pendingFullDump = false;
            updateUIState(serialManager.isConnected());
            return;
        }

        totalSize = 1 << capVal;
        int sizeKB = totalSize / 1024;
        log(String.format("Chip detectado: JEDEC %02X %02X %02X → %s KB",
                mfr & 0xFF, memType & 0xFF, capVal,
                sizeKB >= 1024 ? (sizeKB / 1024) + " MB (" + sizeKB + " KB)" : sizeKB + " KB"));

        state = ProtocolState.FULL_DUMPING;
        currentAddress = 0;
        readStream = new ByteArrayOutputStream();
        pendingFullDump = false;

        mainHandler.post(() -> {
            hexHelper.showPopup("Volcado completo SPI (" + sizeKB + " KB)...", totalSize);
            serialManager.sendData(new byte[]{0x50, 0x46});
            resetTimeout(20000);
        });
    }

    // =========================================================================
    // GUARDAR
    // =========================================================================

    private void clearLog() {
        if (logHelper != null) { logHelper.clear(); log("Log limpiado."); }
    }

    private void saveBuffer() {
        try {
            File dir = FileManager.saveMemoryDump(eepromBuffer);
            log("Guardado en " + dir.getAbsolutePath());
            Toast.makeText(this, "Guardado en Descargas/rom/", Toast.LENGTH_LONG).show();
        } catch (IllegalArgumentException e) {
            Toast.makeText(this, "No hay datos para guardar", Toast.LENGTH_SHORT).show();
        } catch (java.io.IOException e) {
            log("Error I/O guardando: " + e.getMessage());
        }
    }

    // =========================================================================
    // TIMEOUT
    // =========================================================================

    private void resetTimeout(long ms) {
        mainHandler.removeCallbacks(timeoutRunnable);
        mainHandler.postDelayed(timeoutRunnable, ms);
    }

    private void cancelTimeout() { mainHandler.removeCallbacks(timeoutRunnable); }

    // =========================================================================
    // UI STATE
    // =========================================================================

    private void updateUIState(final boolean connected) {
        runOnUiThread(() -> {
            btnConnect.setEnabled(!connected);
            btnDisconnect.setEnabled(connected);
            boolean busy = state != ProtocolState.IDLE;
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
                if (statusDot != null) statusDot.setBackgroundColor(
                        androidx.core.content.ContextCompat.getColor(this, R.color.status_connected));
                if (tvStatusLabel != null) {
                    tvStatusLabel.setText(R.string.status_connected);
                    tvStatusLabel.setTextColor(
                            androidx.core.content.ContextCompat.getColor(this, R.color.status_connected));
                }
                if (layoutStatus != null)
                    layoutStatus.setBackgroundColor(Color.parseColor("#112211"));
            } else {
                if (statusDot != null) statusDot.setBackgroundColor(
                        androidx.core.content.ContextCompat.getColor(this, R.color.status_disconnected));
                if (tvStatusLabel != null) {
                    tvStatusLabel.setText(R.string.status_disconnected);
                    tvStatusLabel.setTextColor(
                            androidx.core.content.ContextCompat.getColor(this, R.color.status_disconnected));
                }
                if (layoutStatus != null)
                    layoutStatus.setBackgroundColor(Color.parseColor("#1A0A0A"));
                state = ProtocolState.IDLE;
                pendingFullDump = false;
                cancelTimeout();
            }
        });
    }

    // =========================================================================
    // USB SERIAL CALLBACKS
    // =========================================================================

    @Override
    public void onSerialConnect() {
        cacheProtocol();
        state = ProtocolState.PINGING;
        readStream = new ByteArrayOutputStream();
        serialManager.sendData(cachedProtocol.buildPingCommand());
        resetTimeout(3000);
        updateUIState(true);
        log("Puerto USB abierto. Ping enviado...");
    }

    @Override
    public void onSerialConnectError(Exception e) {
        log("Error de conexión: " + e.getMessage());
        runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
        updateUIState(false);
    }

    @Override
    public void onSerialRead(byte[] data) {
        if (data == null || data.length == 0) return;

        switch (state) {

            case PINGING: {
                if (readStream == null) readStream = new ByteArrayOutputStream();
                readStream.write(data, 0, data.length);
                String s = new String(readStream.toByteArray(), StandardCharsets.UTF_8);
                if (s.contains("PICMEM")) {
                    state = ProtocolState.IDLE;
                    cancelTimeout();
                    log("✓ Firmware: " + s.replace("\r", "").replace("\n", " ").trim());
                    runOnUiThread(() ->
                            Toast.makeText(this, "PICMEM Detectado", Toast.LENGTH_SHORT).show());
                    updateUIState(true);
                }
                break;
            }

            case READING: {
                if (readStream == null) break;

                int expectedThisChunk = Math.min(READ_CHUNK, totalSize - currentAddress);
                int chunkReceived     = readStream.size() - currentAddress;

                for (byte b : data) {
                    int val = b & 0xFF;

                    if (chunkReceived == 0 && val == 0x58) {
                        state = ProtocolState.IDLE;
                        cancelTimeout();
                        final int addr = currentAddress;
                        log("✗ RESP_ERR en 0x" + Integer.toHexString(addr)
                                + ". Verifica chip y conexiones.");
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Error leyendo en 0x"
                                    + Integer.toHexString(addr), Toast.LENGTH_SHORT).show();
                            updateUIState(true);
                            hexHelper.dismiss();
                        });
                        return;
                    }

                    if (chunkReceived < expectedThisChunk) {
                        readStream.write(b);
                        chunkReceived++;

                    } else {
                        if (val == 0x55) {
                            currentAddress += expectedThisChunk;
                            final int prog = currentAddress;
                            runOnUiThread(() -> hexHelper.updateProgress(prog));
                            hexHelper.renderThrottled(readStream.toByteArray());

                            if (currentAddress >= totalSize) {
                                finishRead();
                            } else {
                                requestNextReadChunk();
                            }
                            return;
                        } else {
                            Log.w(TAG, "Byte inesperado 0x" + Integer.toHexString(val)
                                    + " tras chunk, addr=0x" + Integer.toHexString(currentAddress));
                        }
                    }
                }
                resetTimeout(10000);
                break;
            }

            case WRITING: {
                for (byte b : data) {
                    if (b == 0x4B) {
                        int toNext   = cachedPageSize - (currentAddress % cachedPageSize);
                        int limit    = Math.min(toNext, MAX_WRITE_CHUNK);
                        int expected = Math.min(limit, writeDataBuffer.length - currentAddress);
                        currentAddress += expected;
                        final int prog = currentAddress;
                        runOnUiThread(() -> hexHelper.updateProgress(prog));

                        if (currentAddress >= writeDataBuffer.length) {
                            finishWrite();
                        } else {
                            sendNextWriteChunk();
                        }
                        return;

                    } else if (b == 0x58) {
                        state = ProtocolState.IDLE;
                        cancelTimeout();
                        log("✗ Error de escritura (NACK del dispositivo).");
                        runOnUiThread(() -> {
                            updateUIState(true);
                            hexHelper.dismiss();
                        });
                        return;
                    }
                }
                resetTimeout(10000);
                break;
            }

            case ERASING: {
                for (byte b : data) {
                    if (b == 0x4B) { finishErase(); return; }
                    if (b == 0x58) {
                        state = ProtocolState.IDLE;
                        cancelTimeout();
                        log("✗ Error Chip Erase (RESP_ERR).");
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Error al borrar", Toast.LENGTH_SHORT).show();
                            updateUIState(true);
                            hexHelper.dismiss();
                        });
                        return;
                    }
                }
                resetTimeout(300000);
                break;
            }

            case SCANNING_ID: {
                if (readStream == null) readStream = new ByteArrayOutputStream();

                if (cachedProtocol instanceof I2cProtocol) {
                    for (byte b : data) {
                        int val = b & 0xFF;
                        if (val == 0xFF) {
                            state = ProtocolState.IDLE;
                            cancelTimeout();
                            byte[] found = readStream.toByteArray();
                            StringBuilder sb = new StringBuilder("I2C Scan → ");
                            if (found.length == 0) sb.append("Sin dispositivos.");
                            else for (byte a : found)
                                    sb.append(String.format("0x%02X ", a & 0xFF));
                            log(sb.toString());
                            updateUIState(true);
                            return;
                        }
                        if (val != 0x55 && val != 0x58) readStream.write(b);
                    }
                } else {
                    readStream.write(data, 0, data.length);
                    byte[] j = readStream.toByteArray();
                    if (j.length >= 3) {
                        state = ProtocolState.IDLE;
                        cancelTimeout();

                        if (pendingFullDump) {
                            startFullDumpAfterJedec(j[0], j[1], j[2]);
                        } else {
                            log(String.format("JEDEC ID: %02X %02X %02X",
                                    j[0] & 0xFF, j[1] & 0xFF, j[2] & 0xFF));
                            updateUIState(true);
                        }
                    }
                }
                break;
            }

            case FULL_DUMPING: {
                if (readStream == null) break;

                for (byte b : data) {
                    int val = b & 0xFF;

                    if (val == 0x58 && readStream.size() == 0) {
                        state = ProtocolState.IDLE;
                        cancelTimeout();
                        log("✗ Full Dump abortado: chip no detectado o JEDEC inválido.");
                        runOnUiThread(() -> {
                            updateUIState(true);
                            hexHelper.dismiss();
                        });
                        return;
                    }

                    if (val == 0x55 && readStream.size() >= totalSize) {
                        finishRead();
                        return;
                    }

                    if (readStream.size() < totalSize) {
                        readStream.write(b);
                        int sz = readStream.size();

                        if ((sz & 0xFF) == 0) {
                            final int prog = sz;
                            runOnUiThread(() -> hexHelper.updateProgress(prog));
                        }
                        if ((sz & 0xFFF) == 0) {
                            hexHelper.renderThrottled(readStream.toByteArray());
                        }
                    }
                }
                resetTimeout(5000);
                break;
            }

            default: break;
        }
    }

    @Override
    public void onSerialIoError(Exception e) {
        Log.e(TAG, "onSerialIoError", e);
        state = ProtocolState.IDLE;
        pendingFullDump = false;
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
        pendingFullDump = false;
        cancelTimeout();
        log("Dispositivo desconectado.");
        runOnUiThread(() -> {
            Toast.makeText(this, "Desconectado", Toast.LENGTH_SHORT).show();
            updateUIState(false);
            hexHelper.dismiss();
        });
    }

    // =========================================================================
    // MENÚ  ← sección actualizada con todos los ítems nuevos
    // =========================================================================

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        // ── Diagramas de hardware ─────────────────────────────────────────
        if (id == R.id.action_pinout_pic) { openHardwareInfo(0); return true; }
        if (id == R.id.action_i2c_conn)  { openHardwareInfo(1); return true; }
        if (id == R.id.action_spi_conn)  { openHardwareInfo(2); return true; }

        // ── Firmware ──────────────────────────────────────────────────────
        if (id == R.id.action_firmware) {
            startActivity(new Intent(this, FirmwareActivity.class));
            return true;
        }

        // ── Política de privacidad ────────────────────────────────────────
        if (id == R.id.action_privacy) {
            startActivity(new Intent(this, PrivacyPolicyActivity.class));
            return true;
        }

        // ── Acerca de ─────────────────────────────────────────────────────
        if (id == R.id.action_about) {
            startActivity(new Intent(this, AboutActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /** Abre el diagrama de hardware correspondiente */
    private void openHardwareInfo(int type) {
        startActivity(new Intent(this,
                com.mobincube.pronosticos_parley_copy.sc_55UCEB.ui.HardwareInfoActivity.class)
                .putExtra("type", type));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        serialManager.cleanup();
        mainHandler.removeCallbacksAndMessages(null);
    }
}

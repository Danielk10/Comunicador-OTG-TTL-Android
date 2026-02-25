package com.diamon.ttl.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.diamon.ttl.R;
import com.diamon.ttl.usb.UsbSerialListener;
import com.diamon.ttl.usb.UsbSerialManager;
import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TerminalActivity extends AppCompatActivity implements UsbSerialListener {

    private UsbSerialManager serialManager;

    private Button btnConnect;
    private Button btnDisconnect;
    private Button btnSend;
    private Button btnClear;

    private EditText editTextSend;
    private TextView textViewReceive;
    private ScrollView scrollView;

    private Spinner spinnerBaudRate;

    private Button btnAllOn, btnAllOff, btnTask1, btnTask2;

    // Indicador de estado visual
    private View statusDot;
    private TextView tvStatusLabel;
    private View layoutStatus;

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terminal);

        btnConnect = findViewById(R.id.btnConnect);
        btnDisconnect = findViewById(R.id.btnDisconnect);
        btnSend = findViewById(R.id.btnSend);
        btnClear = findViewById(R.id.btnClear);

        editTextSend = findViewById(R.id.editTextSend);
        textViewReceive = findViewById(R.id.textViewReceive);
        scrollView = findViewById(R.id.scrollView);

        spinnerBaudRate = findViewById(R.id.spinnerBaudRate);

        btnAllOn = findViewById(R.id.btnAllOn);
        btnAllOff = findViewById(R.id.btnAllOff);
        btnTask1 = findViewById(R.id.btnTask1);
        btnTask2 = findViewById(R.id.btnTask2);

        statusDot = findViewById(R.id.statusDot);
        tvStatusLabel = findViewById(R.id.tvStatusLabel);
        layoutStatus = findViewById(R.id.layoutStatus);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.baudrates, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBaudRate.setAdapter(adapter);
        spinnerBaudRate.setSelection(4); // 9600 por defecto

        serialManager = new UsbSerialManager(this, this);

        updateUIState(false);

        btnConnect.setOnClickListener(v -> connectSerial());
        btnDisconnect.setOnClickListener(v -> disconnectSerial());
        btnSend.setOnClickListener(v -> sendData());
        btnClear.setOnClickListener(v -> textViewReceive.setText(""));

        View.OnClickListener taskListener = v -> {
            int id = v.getId();
            if (id == R.id.btnAllOn)
                serialManager.sendText("A");
            else if (id == R.id.btnAllOff)
                serialManager.sendText("O");
            else if (id == R.id.btnTask1)
                serialManager.sendText("T");
            else if (id == R.id.btnTask2)
                serialManager.sendText("F");
        };

        btnAllOn.setOnClickListener(taskListener);
        btnAllOff.setOnClickListener(taskListener);
        btnTask1.setOnClickListener(taskListener);
        btnTask2.setOnClickListener(taskListener);

        appendToReceive("[Sistema] Comunicador OTG TTL listo.\n");
        appendToReceive("[Sistema] Conecta el adaptador USB-TTL para comenzar.\n");
    }

    private void connectSerial() {
        String baudRateStr = spinnerBaudRate.getSelectedItem().toString();
        int baudRate = Integer.parseInt(baudRateStr);
        serialManager.setSerialParameters(baudRate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
        serialManager.connect();
    }

    private void disconnectSerial() {
        serialManager.disconnect();
    }

    private void sendData() {
        String text = editTextSend.getText().toString().trim();
        if (!text.isEmpty()) {
            appendToReceive("[" + timeFormat.format(new Date()) + "] >> " + text + "\n");
            serialManager.sendText(text + "\r\n");
            editTextSend.setText("");
        }
    }

    private void updateUIState(final boolean connected) {
        runOnUiThread(() -> {
            btnConnect.setEnabled(!connected);
            btnDisconnect.setEnabled(connected);
            btnSend.setEnabled(connected);
            spinnerBaudRate.setEnabled(!connected);

            btnAllOn.setEnabled(connected);
            btnAllOff.setEnabled(connected);
            btnTask1.setEnabled(connected);
            btnTask2.setEnabled(connected);

            // Actualizar indicador visual de conexion
            if (connected) {
                statusDot.setBackgroundColor(Color.parseColor("#3FB950"));
                tvStatusLabel.setText("Conectado");
                tvStatusLabel.setTextColor(Color.parseColor("#3FB950"));
                if (layoutStatus != null)
                    layoutStatus.setBackgroundColor(Color.parseColor("#112211"));
            } else {
                statusDot.setBackgroundColor(Color.parseColor("#F85149"));
                tvStatusLabel.setText("Desconectado");
                tvStatusLabel.setTextColor(Color.parseColor("#F85149"));
                if (layoutStatus != null)
                    layoutStatus.setBackgroundColor(Color.parseColor("#1A0A0A"));
            }
        });
    }

    private void appendToReceive(final String text) {
        runOnUiThread(() -> {
            textViewReceive.append(text);
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        });
    }

    @Override
    public void onSerialConnect() {
        Toast.makeText(this, "USB conectado correctamente", Toast.LENGTH_SHORT).show();
        appendToReceive("[" + timeFormat.format(new Date()) + "] Conexion establecida.\n");
        updateUIState(true);
    }

    @Override
    public void onSerialConnectError(final Exception e) {
        runOnUiThread(() -> {
            Toast.makeText(TerminalActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            appendToReceive("[" + timeFormat.format(new Date()) + "] Error al conectar: " + e.getMessage() + "\n");
            updateUIState(false);
        });
    }

    @Override
    public void onSerialRead(byte[] data) {
        // Usar UTF-8 explicito para evitar comportamiento dependiente de la plataforma
        String text = new String(data, StandardCharsets.UTF_8);
        appendToReceive("[" + timeFormat.format(new Date()) + "] " + text);
    }

    @Override
    public void onSerialIoError(final Exception e) {
        runOnUiThread(() -> {
            Toast.makeText(TerminalActivity.this, "Error I/O: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            appendToReceive("[" + timeFormat.format(new Date()) + "] Error I/O: " + e.getMessage() + "\n");
        });
    }

    @Override
    public void onSerialDisconnect() {
        Toast.makeText(this, "Dispositivo desconectado", Toast.LENGTH_SHORT).show();
        appendToReceive("[" + timeFormat.format(new Date()) + "] Dispositivo desconectado.\n");
        updateUIState(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        serialManager.cleanup();
    }
}

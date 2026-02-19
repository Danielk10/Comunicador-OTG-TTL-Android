package com.diamon.ttl;

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

import com.hoho.android.usbserial.driver.UsbSerialPort;

public class MainActivity extends AppCompatActivity implements UsbSerialListener {

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.baudrates, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBaudRate.setAdapter(adapter);
        spinnerBaudRate.setSelection(4); // 9600

        serialManager = new UsbSerialManager(this, this);

        updateUIState(false);

        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectSerial();
            }
        });

        btnDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnectSerial();
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendData();
            }
        });

        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textViewReceive.setText("");
            }
        });

        View.OnClickListener taskListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.btnAllOn)
                    serialManager.sendText("A");
                else if (v.getId() == R.id.btnAllOff)
                    serialManager.sendText("O");
                else if (v.getId() == R.id.btnTask1)
                    serialManager.sendText("T");
                else if (v.getId() == R.id.btnTask2)
                    serialManager.sendText("F");
            }
        };

        btnAllOn.setOnClickListener(taskListener);
        btnAllOff.setOnClickListener(taskListener);
        btnTask1.setOnClickListener(taskListener);
        btnTask2.setOnClickListener(taskListener);

        appendToReceive("USB TTL Picard Communicator ready.\n");
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
        String text = editTextSend.getText().toString();
        if (!text.isEmpty()) {
            serialManager.sendText(text + "\r\n");
            editTextSend.setText("");
        }
    }

    private void updateUIState(final boolean connected) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnConnect.setEnabled(!connected);
                btnDisconnect.setEnabled(connected);
                btnSend.setEnabled(connected);
                spinnerBaudRate.setEnabled(!connected);

                btnAllOn.setEnabled(connected);
                btnAllOff.setEnabled(connected);
                btnTask1.setEnabled(connected);
                btnTask2.setEnabled(connected);
            }
        });
    }

    private void appendToReceive(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textViewReceive.append(text);
                scrollView.post(new Runnable() {
                    @Override
                    public void run() {
                        scrollView.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        });
    }

    @Override
    public void onSerialConnect() {
        Toast.makeText(this, "Conectado", Toast.LENGTH_SHORT).show();
        updateUIState(true);
    }

    @Override
    public void onSerialConnectError(final Exception e) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                updateUIState(false);
            }
        });
    }

    @Override
    public void onSerialRead(byte[] data) {
        String text = new String(data);
        appendToReceive(text);
    }

    @Override
    public void onSerialIoError(final Exception e) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Error I/O: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onSerialDisconnect() {
        Toast.makeText(this, "Desconectado", Toast.LENGTH_SHORT).show();
        updateUIState(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        serialManager.cleanup();
    }
}

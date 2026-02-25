package com.diamon.ttl.usb;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.List;

public class UsbSerialManager implements SerialInputOutputManager.Listener {

    private static final String TAG = "UsbSerialManager";
    private static final String ACTION_USB_PERMISSION = "com.diamon.ttl.USB_PERMISSION";

    private final Context context;
    private final UsbManager usbManager;
    private final UsbSerialListener listener;

    private UsbSerialPort usbSerialPort;
    private SerialInputOutputManager ioManager;

    private int baudRate = 9600;
    private int dataBits = 8;
    private int stopBits = UsbSerialPort.STOPBITS_1;
    private int parity = UsbSerialPort.PARITY_NONE;

    private boolean connected = false;

    public UsbSerialManager(Context context, UsbSerialListener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        this.usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
    }

    public void setSerialParameters(int baudRate, int dataBits, int stopBits, int parity) {
        this.baudRate = baudRate;
        this.dataBits = dataBits;
        this.stopBits = stopBits;
        this.parity = parity;
    }

    public void connect() {
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
        if (availableDrivers.isEmpty()) {
            if (listener != null) {
                listener.onSerialConnectError(new Exception("No se encontró ningún dispositivo USB-Serial"));
            }
            return;
        }

        UsbSerialDriver driver = availableDrivers.get(0);
        UsbDevice device = driver.getDevice();

        if (!usbManager.hasPermission(device)) {
            requestPermission(device);
        } else {
            connectToDevice(driver);
        }
    }

    private void requestPermission(UsbDevice device) {
        PendingIntent permissionIntent;
        Intent intent = new Intent(ACTION_USB_PERMISSION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_MUTABLE);
        } else {
            permissionIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        }

        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(usbPermissionReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            context.registerReceiver(usbPermissionReceiver, filter);
        }

        usbManager.requestPermission(device, permissionIntent);
    }

    private final BroadcastReceiver usbPermissionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            String action = intent.getAction();
            if (!ACTION_USB_PERMISSION.equals(action))
                return;

            UsbDevice device;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice.class);
            } else {
                device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            }

            if (device == null) {
                if (listener != null)
                    listener.onSerialConnectError(new Exception("Dispositivo USB nulo"));
                return;
            }

            if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                List<UsbSerialDriver> drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
                for (UsbSerialDriver d : drivers) {
                    if (d.getDevice().equals(device)) {
                        connectToDevice(d);
                        break;
                    }
                }
            } else {
                if (listener != null)
                    listener.onSerialConnectError(new Exception("Permiso USB denegado"));
            }
        }
    };

    private void connectToDevice(UsbSerialDriver driver) {
        UsbDeviceConnection connection = usbManager.openDevice(driver.getDevice());
        if (connection == null) {
            if (listener != null)
                listener.onSerialConnectError(new Exception("No se pudo abrir el dispositivo USB"));
            return;
        }

        try {
            usbSerialPort = driver.getPorts().get(0);
            usbSerialPort.open(connection);
            usbSerialPort.setParameters(baudRate, dataBits, stopBits, parity);

            ioManager = new SerialInputOutputManager(usbSerialPort, this);
            ioManager.start();

            connected = true;
            if (listener != null)
                listener.onSerialConnect();

        } catch (IOException e) {
            Log.e(TAG, "Error abriendo puerto serial", e);
            if (listener != null)
                listener.onSerialConnectError(e);
        }
    }

    public void sendData(byte[] data) {
        if (!connected || usbSerialPort == null) {
            if (listener != null)
                listener.onSerialIoError(new Exception("Puerto serial no conectado"));
            return;
        }

        try {
            usbSerialPort.write(data, 1000);
        } catch (IOException e) {
            Log.e(TAG, "Error enviando datos", e);
            if (listener != null)
                listener.onSerialIoError(e);
        }
    }

    public void sendText(String text) {
        sendData(text.getBytes());
    }

    public void disconnect() {
        if (ioManager != null) {
            ioManager.stop();
            ioManager = null;
        }

        if (usbSerialPort != null) {
            try {
                usbSerialPort.close();
            } catch (IOException e) {
                Log.e(TAG, "Error cerrando puerto", e);
            }
            usbSerialPort = null;
        }

        connected = false;
        if (listener != null)
            listener.onSerialDisconnect();
    }

    public boolean isConnected() {
        return connected;
    }

    public void cleanup() {
        try {
            context.unregisterReceiver(usbPermissionReceiver);
        } catch (Exception ignored) {
        }
        disconnect();
    }

    @Override
    public void onNewData(byte[] data) {
        if (listener != null)
            listener.onSerialRead(data);
    }

    @Override
    public void onRunError(Exception e) {
        Log.e(TAG, "Error en SerialInputOutputManager", e);
        if (listener != null)
            listener.onSerialIoError(e);
    }
}

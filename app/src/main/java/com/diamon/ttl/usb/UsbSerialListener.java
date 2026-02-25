package com.diamon.ttl.usb;

public interface UsbSerialListener {
    void onSerialConnect();

    void onSerialConnectError(Exception e);

    void onSerialRead(byte[] data);

    void onSerialIoError(Exception e);

    void onSerialDisconnect();
}

package com.mobincube.pronosticos_parley_copy.sc_55UCEB.usb;

public interface UsbSerialListener {
    void onSerialConnect();

    void onSerialConnectError(Exception e);

    void onSerialRead(byte[] data);

    void onSerialIoError(Exception e);

    void onSerialDisconnect();
}

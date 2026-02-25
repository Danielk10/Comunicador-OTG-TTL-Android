package com.diamon.ttl.ui;

import android.app.Activity;
import android.widget.TextView;

public class HexViewerHelper {

    private final Activity activity;
    private final TextView tvHexViewer;
    private long lastUiUpdateTime = 0;
    private static final int UI_THROTTLE_MS = 250;

    public HexViewerHelper(Activity activity, TextView tvHexViewer) {
        this.activity = activity;
        this.tvHexViewer = tvHexViewer;
    }

    public void renderThrottled(byte[] data) {
        long now = System.currentTimeMillis();
        // Evitar ANR congelando la Interfaz (Actualiza solo X veces por seg)
        if (now - lastUiUpdateTime > UI_THROTTLE_MS) {
            lastUiUpdateTime = now;
            renderNow(data);
        }
    }

    public void renderNow(final byte[] data) {
        activity.runOnUiThread(() -> {
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
        });
    }

    public void setText(String text) {
        activity.runOnUiThread(() -> tvHexViewer.setText(text));
    }
}

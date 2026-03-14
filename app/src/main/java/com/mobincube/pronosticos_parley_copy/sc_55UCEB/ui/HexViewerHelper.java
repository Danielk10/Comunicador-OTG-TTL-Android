package com.mobincube.pronosticos_parley_copy.sc_55UCEB.ui;

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
        if (data == null) return;
        activity.runOnUiThread(() -> {
            StringBuilder sb = new StringBuilder();
            sb.append("Dirección | 00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F | ASCII\n");
            sb.append("------------------------------------------------------------------\n");

            char[] hexChars = "0123456789ABCDEF".toCharArray();

            for (int i = 0; i < data.length; i += 16) {
                // Address (8 digits)
                for (int n = 28; n >= 0; n -= 4) {
                    sb.append(hexChars[(i >> n) & 0x0F]);
                }
                sb.append("  | ");

                // Hex bytes
                for (int j = 0; j < 16; j++) {
                    if (i + j < data.length) {
                        int b = data[i + j] & 0xFF;
                        sb.append(hexChars[b >>> 4]);
                        sb.append(hexChars[b & 0x0F]);
                        sb.append(' ');
                    } else {
                        sb.append("   ");
                    }
                }
                sb.append("| ");

                // ASCII chars
                for (int j = 0; j < 16; j++) {
                    if (i + j < data.length) {
                        int b = data[i + j] & 0xFF;
                        if (b >= 32 && b <= 126) {
                            sb.append((char) b);
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

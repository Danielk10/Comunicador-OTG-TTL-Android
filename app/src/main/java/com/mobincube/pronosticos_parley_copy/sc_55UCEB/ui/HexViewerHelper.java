package com.mobincube.pronosticos_parley_copy.sc_55UCEB.ui;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mobincube.pronosticos_parley_copy.sc_55UCEB.R;

public class HexViewerHelper {

    private final Activity activity;
    private PopupWindow popupWindow;
    private TextView tvHexViewer;
    private ProgressBar progressBar;
    private TextView tvTitle;
    
    private long lastUiUpdateTime = 0;
    private static final int UI_THROTTLE_MS = 300;

    public HexViewerHelper(Activity activity) {
        this.activity = activity;
    }

    public void showPopup(String title, int maxProgress) {
        activity.runOnUiThread(() -> {
            if (popupWindow != null && popupWindow.isShowing()) {
                tvTitle.setText(title);
                progressBar.setMax(maxProgress);
                progressBar.setProgress(0);
                return;
            }

            View popupView = LayoutInflater.from(activity).inflate(R.layout.popup_hex_viewer, null);
            popupWindow = new PopupWindow(popupView, 
                    ViewGroup.LayoutParams.MATCH_PARENT, 
                    ViewGroup.LayoutParams.WRAP_CONTENT, 
                    false); // Focusable false para que no bloquee interacciones abajo
            
            popupWindow.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            popupWindow.setOutsideTouchable(false);
            
            tvHexViewer = popupView.findViewById(R.id.tvHexViewerPopup);
            progressBar = popupView.findViewById(R.id.popupProgressBar);
            tvTitle = popupView.findViewById(R.id.tvPopupTitle);
            
            tvTitle.setText(title);
            progressBar.setMax(maxProgress);
            progressBar.setProgress(0);
            tvHexViewer.setText("Iniciando transferencia...");

            popupView.findViewById(R.id.btnClosePopup).setOnClickListener(v -> popupWindow.dismiss());

            // Mostrar en la parte superior para no tapar el LOG
            popupWindow.showAtLocation(activity.getWindow().getDecorView(), Gravity.TOP, 0, 150);
        });
    }

    public void updateProgress(int progress) {
        activity.runOnUiThread(() -> {
            if (progressBar != null) progressBar.setProgress(progress);
        });
    }

    public void dismiss() {
        activity.runOnUiThread(() -> {
            if (popupWindow != null) popupWindow.dismiss();
        });
    }

    public void renderThrottled(byte[] data) {
        long now = System.currentTimeMillis();
        if (now - lastUiUpdateTime > UI_THROTTLE_MS) {
            lastUiUpdateTime = now;
            renderNow(data);
        }
    }

    public void renderNow(final byte[] data) {
        if (data == null || tvHexViewer == null) return;
        activity.runOnUiThread(() -> {
            StringBuilder sb = new StringBuilder();
            sb.append("Dirección | 00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F | ASCII\n");
            sb.append("------------------------------------------------------------------\n");

            char[] hexChars = "0123456789ABCDEF".toCharArray();
            for (int i = 0; i < data.length; i += 16) {
                // Address
                for (int n = 28; n >= 0; n -= 4) {
                    sb.append(hexChars[(i >> n) & 0x0F]);
                }
                sb.append("  | ");

                // Hex
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

                // ASCII
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
        activity.runOnUiThread(() -> {
            if (tvHexViewer != null) tvHexViewer.setText(text);
        });
    }
}

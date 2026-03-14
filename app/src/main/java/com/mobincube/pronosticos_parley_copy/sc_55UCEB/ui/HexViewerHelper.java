package com.mobincube.pronosticos_parley_copy.sc_55UCEB.ui;

import android.app.Activity;
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

    // ── FIX #7 ────────────────────────────────────────────────────────────────
    // Limitar el hex viewer a las últimas MAX_RENDER_BYTES durante la
    // transferencia. Para dumps de 4 MB+ un StringBuilder completo genera
    // cadenas de 20 MB+ en el hilo UI → ANR garantizado.
    // ─────────────────────────────────────────────────────────────────────────
    private static final int MAX_RENDER_BYTES = 4096; // 4 KB visibles

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
                    false); // Focusable false: no bloquea interacciones debajo

            popupWindow.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            popupWindow.setOutsideTouchable(false);

            tvHexViewer = popupView.findViewById(R.id.tvHexViewerPopup);
            progressBar = popupView.findViewById(R.id.popupProgressBar);
            tvTitle     = popupView.findViewById(R.id.tvPopupTitle);

            tvTitle.setText(title);
            progressBar.setMax(maxProgress);
            progressBar.setProgress(0);
            tvHexViewer.setText("Iniciando transferencia...");

            popupView.findViewById(R.id.btnClosePopup)
                    .setOnClickListener(v -> popupWindow.dismiss());

            // Mostrar en la parte superior para no tapar el log
            popupWindow.showAtLocation(
                    activity.getWindow().getDecorView(), Gravity.TOP, 0, 150);
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

    // ── renderThrottled ───────────────────────────────────────────────────────
    // Llamado frecuentemente durante la transferencia. Aplica throttle temporal
    // Y ventana de datos para evitar ANR.
    // ─────────────────────────────────────────────────────────────────────────
    public void renderThrottled(byte[] data) {
        long now = System.currentTimeMillis();
        if (now - lastUiUpdateTime > UI_THROTTLE_MS) {
            lastUiUpdateTime = now;
            renderNow(data);
        }
    }

    // ── renderFinal ───────────────────────────────────────────────────────────
    // FIX #7: llamado desde finishRead() / finishWrite() en MainActivity.
    // Muestra las últimas MAX_RENDER_BYTES del dump y añade un aviso si el
    // archivo es más grande, para que el usuario sepa que debe exportarlo.
    // ─────────────────────────────────────────────────────────────────────────
    public void renderFinal(byte[] data) {
        if (data == null || data.length == 0) return;
        final boolean truncated = data.length > MAX_RENDER_BYTES;
        final int offset = truncated ? data.length - MAX_RENDER_BYTES : 0;
        final byte[] window = new byte[Math.min(data.length, MAX_RENDER_BYTES)];
        System.arraycopy(data, offset, window, 0, window.length);

        activity.runOnUiThread(() -> {
            if (tvHexViewer == null) return;
            StringBuilder sb = new StringBuilder();
            if (truncated) {
                sb.append(String.format(
                        "[ Mostrando últimos %d B de %d B totales — usa Exportar para el archivo completo ]\n\n",
                        MAX_RENDER_BYTES, data.length));
            }
            appendHexDump(sb, window, offset);
            tvHexViewer.setText(sb.toString());
        });
    }

    // ── renderNow ─────────────────────────────────────────────────────────────
    // FIX #7: usa windowedData() para limitar el tamaño del string generado.
    // Solo muestra las últimas MAX_RENDER_BYTES → seguro para dumps >1 MB.
    // ─────────────────────────────────────────────────────────────────────────
    public void renderNow(final byte[] data) {
        if (data == null || tvHexViewer == null) return;

        final boolean truncated = data.length > MAX_RENDER_BYTES;
        final int offset = truncated ? data.length - MAX_RENDER_BYTES : 0;
        final byte[] window = new byte[Math.min(data.length, MAX_RENDER_BYTES)];
        System.arraycopy(data, offset, window, 0, window.length);

        activity.runOnUiThread(() -> {
            if (tvHexViewer == null) return;
            StringBuilder sb = new StringBuilder();
            if (truncated) {
                sb.append(String.format("[ ... %d B — mostrando últimos %d B ]\n\n",
                        data.length, MAX_RENDER_BYTES));
            }
            appendHexDump(sb, window, offset);
            tvHexViewer.setText(sb.toString());
        });
    }

    // ── appendHexDump ─────────────────────────────────────────────────────────
    // Genera el volcado hex+ASCII de 'data' con direcciones absolutas
    // (baseOffset indica la dirección real del primer byte de la ventana).
    // ─────────────────────────────────────────────────────────────────────────
    private void appendHexDump(StringBuilder sb, byte[] data, int baseOffset) {
        sb.append("Direccion | 00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F | ASCII\n");
        sb.append("------------------------------------------------------------------\n");

        final char[] H = "0123456789ABCDEF".toCharArray();
        for (int i = 0; i < data.length; i += 16) {
            // Dirección absoluta (baseOffset + i)
            int addr = baseOffset + i;
            for (int n = 28; n >= 0; n -= 4) sb.append(H[(addr >> n) & 0x0F]);
            sb.append("  | ");

            // Hex
            for (int j = 0; j < 16; j++) {
                if (i + j < data.length) {
                    int b = data[i + j] & 0xFF;
                    sb.append(H[b >>> 4]);
                    sb.append(H[b & 0x0F]);
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
                    sb.append(b >= 32 && b <= 126 ? (char) b : '.');
                }
            }
            sb.append('\n');
        }
    }

    public void setText(String text) {
        activity.runOnUiThread(() -> {
            if (tvHexViewer != null) tvHexViewer.setText(text);
        });
    }
}

package com.mobincube.pronosticos_parley_copy.sc_55UCEB.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mobincube.pronosticos_parley_copy.sc_55UCEB.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Pantalla de gestión del firmware PICMEM v3.
 *
 * Permite:
 *  1. Guardar pic_firmware_v3.c desde assets → Descargas/rom/
 *  2. Guardar pic_firmware_v3.hex desde assets → Descargas/rom/
 *  3. Abrir el repositorio GitHub en el navegador
 *
 * Los archivos deben estar en app/src/main/assets/:
 *   • pic_firmware_v3.c
 *   • pic_firmware_v3.hex
 */
public class FirmwareActivity extends AppCompatActivity {

    private static final String REPO_URL =
            "https://github.com/Danielk10/Comunicador-OTG-TTL-Android";
    private static final String ASSET_C   = "pic_firmware_v3.c";
    private static final String ASSET_HEX = "pic_firmware_v3.hex";

    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firmware);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Firmware PIC16F628A");
        }

        tvStatus = findViewById(R.id.tvDownloadStatus);

        // ── Guardar .c ────────────────────────────────────────────────
        findViewById(R.id.btnDownloadC).setOnClickListener(v ->
                saveAssetToDownloads(ASSET_C));

        // ── Guardar .hex ──────────────────────────────────────────────
        findViewById(R.id.btnDownloadHex).setOnClickListener(v ->
                saveAssetToDownloads(ASSET_HEX));

        // ── Abrir GitHub ──────────────────────────────────────────────
        findViewById(R.id.btnOpenGithub).setOnClickListener(v ->
                openUrl(REPO_URL));
    }

    // ── Copiar asset a Descargas/rom/ ──────────────────────────────────────
    private void saveAssetToDownloads(String assetName) {
        try {
            // Crear directorio de destino
            File romDir = getRomDir();
            File destFile = new File(romDir, assetName);

            // Copiar desde assets
            try (InputStream in = getAssets().open(assetName);
                 OutputStream out = new FileOutputStream(destFile)) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            }

            showStatus("✓ Guardado: " + destFile.getAbsolutePath(), true);

        } catch (IOException e) {
            // El archivo puede no existir en assets del APK de prueba
            showStatus("✗ No encontrado en assets: " + assetName
                    + "\n  Descarga desde GitHub el archivo completo.", false);
        }
    }

    /** Devuelve (y crea si no existe) el directorio Descargas/rom/ */
    @SuppressWarnings("deprecation")
    private File getRomDir() throws IOException {
        File dir = new File(
                android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_DOWNLOADS),
                "rom");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("No se pudo crear Descargas/rom/");
        }
        return dir;
    }

    private void showStatus(String msg, boolean success) {
        tvStatus.setText(msg);
        tvStatus.setTextColor(success ? 0xFF3FB950 : 0xFFF85149);
        tvStatus.setVisibility(View.VISIBLE);
        if (success) {
            Toast.makeText(this, "Guardado en Descargas/rom/", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, msg.split("\n")[0], Toast.LENGTH_LONG).show();
        }
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo abrir: " + url, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

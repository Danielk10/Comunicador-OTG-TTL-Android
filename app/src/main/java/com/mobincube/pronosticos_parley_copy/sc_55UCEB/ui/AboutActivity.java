package com.mobincube.pronosticos_parley_copy.sc_55UCEB.ui;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mobincube.pronosticos_parley_copy.sc_55UCEB.R;

/**
 * Pantalla "Acerca de" con:
 *   • Versión dinámica de la app
 *   • Licencia Apache 2.0 con link
 *   • Repositorio GitHub con link
 *   • Bibliotecas usadas con sus links
 *   • Datos del desarrollador (email / GitHub)
 *   • Acceso a Política de Privacidad
 */
public class AboutActivity extends AppCompatActivity {

    private static final String REPO_URL =
            "https://github.com/Danielk10/Comunicador-OTG-TTL-Android";
    private static final String LICENSE_URL =
            "https://www.apache.org/licenses/LICENSE-2.0";
    private static final String LIB_USB_URL =
            "https://github.com/mik3y/usb-serial-for-android";
    private static final String LIB_MATERIAL_URL =
            "https://github.com/material-components/material-components-android";
    private static final String LIB_SDCC_URL =
            "https://sdcc.sourceforge.net";
    private static final String DEV_EMAIL =
            "danielpdiamon@gmail.com";
    private static final String DEV_GITHUB =
            "https://github.com/Danielk10";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Acerca de");
        }

        // ── Versión dinámica ──────────────────────────────────────────
        TextView tvVersion = findViewById(R.id.tvVersion);
        try {
            PackageInfo pi = getPackageInfoCompat();
            long vCode = androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(pi);
            tvVersion.setText("Versión " + pi.versionName
                    + "  (build " + vCode + ")");
        } catch (PackageManager.NameNotFoundException e) {
            tvVersion.setText("Versión 1.0.1");
        }

        // ── Licencia ──────────────────────────────────────────────────
        TextView tvLicense = findViewById(R.id.tvLicenseLink);
        tvLicense.setOnClickListener(v -> openUrl(LICENSE_URL));

        // ── Repositorio ───────────────────────────────────────────────
        TextView tvRepo = findViewById(R.id.tvRepoLink);
        tvRepo.setOnClickListener(v -> openUrl(REPO_URL));
        findViewById(R.id.btnOpenRepo).setOnClickListener(v -> openUrl(REPO_URL));

        // ── Biblioteca: usb-serial-for-android ───────────────────────
        TextView tvLibUsb = findViewById(R.id.tvLibUsbLink);
        tvLibUsb.setOnClickListener(v -> openUrl(LIB_USB_URL));

        // ── Biblioteca: Material Components ──────────────────────────
        TextView tvLibMaterial = findViewById(R.id.tvLibMaterialLink);
        tvLibMaterial.setOnClickListener(v -> openUrl(LIB_MATERIAL_URL));

        // ── Biblioteca: SDCC ──────────────────────────────────────────
        TextView tvLibSdcc = findViewById(R.id.tvLibSdccLink);
        tvLibSdcc.setOnClickListener(v -> openUrl(LIB_SDCC_URL));

        // ── Desarrollador: email ──────────────────────────────────────
        TextView tvEmail = findViewById(R.id.tvDevEmail);
        tvEmail.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO,
                    Uri.parse("mailto:" + DEV_EMAIL));
            intent.putExtra(Intent.EXTRA_SUBJECT, "OTG Flash EEPROM");
            try {
                startActivity(Intent.createChooser(intent, "Enviar email"));
            } catch (Exception e) {
                Toast.makeText(this, DEV_EMAIL, Toast.LENGTH_LONG).show();
            }
        });

        // ── Desarrollador: GitHub ─────────────────────────────────────
        TextView tvGithub = findViewById(R.id.tvDevGithub);
        tvGithub.setOnClickListener(v -> openUrl(DEV_GITHUB));

        // ── Política de privacidad ────────────────────────────────────
        findViewById(R.id.btnPrivacy).setOnClickListener(v ->
                startActivity(new Intent(this, PrivacyPolicyActivity.class)));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("deprecation")
    private PackageInfo getPackageInfoCompat() throws PackageManager.NameNotFoundException {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            return getPackageManager().getPackageInfo(getPackageName(), PackageManager.PackageInfoFlags.of(0));
        } else {
            return getPackageManager().getPackageInfo(getPackageName(), 0);
        }
    }

    // ── Utilidad ──────────────────────────────────────────────────────────
    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo abrir: " + url, Toast.LENGTH_LONG).show();
        }
    }
}

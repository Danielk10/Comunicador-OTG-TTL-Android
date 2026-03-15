package com.mobincube.pronosticos_parley_copy.sc_55UCEB.ui;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ScrollView;
import androidx.appcompat.app.AppCompatActivity;

public class HardwareInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int type = getIntent().getIntExtra("type", 0);

        // DiagramView necesita un ScrollView padre para contenido largo
        DiagramView diagramView = new DiagramView(this);
        diagramView.setType(type);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(0xFF0D1117);
        scrollView.addView(diagramView, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));
        setContentView(scrollView);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            String title;
            switch (type) {
                case 1:  title = "Diagrama I2C — 24Cxx";  break;
                case 2:  title = "Diagrama SPI — W25Qxx / 25LCxx"; break;
                default: title = "PIC16F628A — Pinout DIP-18";     break;
            }
            getSupportActionBar().setTitle(title);
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

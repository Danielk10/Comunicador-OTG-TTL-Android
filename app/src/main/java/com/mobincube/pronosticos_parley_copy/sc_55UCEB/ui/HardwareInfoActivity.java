package com.mobincube.pronosticos_parley_copy.sc_55UCEB.ui;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;

public class HardwareInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        int type = getIntent().getIntExtra("type", 0);
        
        DiagramView diagramView = new DiagramView(this);
        diagramView.setType(type);
        setContentView(diagramView);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            String title = "Hardware Info";
            switch (type) {
                case 0: title = "PIC 16F628A Pinout"; break;
                case 1: title = "Conexiones I2C"; break;
                case 2: title = "Conexiones SPI"; break;
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

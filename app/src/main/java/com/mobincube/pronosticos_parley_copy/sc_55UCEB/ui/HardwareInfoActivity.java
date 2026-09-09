package com.mobincube.keystore.jks_parley_copy.sc_55UCEB.ui;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ScrollView;
import androidx.appcompat.app.AppCompatActivity;

import com.mobincube.keystore.jks_parley_copy.sc_55UCEB.R;

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
                case 1:  title = getString(R.string.title_diagram_i2c);  break;
                case 2:  title = getString(R.string.title_diagram_spi); break;
                default: title = getString(R.string.title_diagram_pinout);     break;
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

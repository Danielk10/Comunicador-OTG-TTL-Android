package com.mobincube.pronosticos_parley_copy.sc_55UCEB.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;

import com.mobincube.pronosticos_parley_copy.sc_55UCEB.graficos.Graficos2D;
import com.mobincube.pronosticos_parley_copy.sc_55UCEB.graficos.Textura2D;
import com.mobincube.pronosticos_parley_copy.sc_55UCEB.nucleo.Graficos;
import com.mobincube.pronosticos_parley_copy.sc_55UCEB.nucleo.Textura;

public class DiagramView extends View {
    private Graficos graficos;
    private Textura textura;
    private int type = 0; // 0: PIC, 1: I2C, 2: SPI

    public DiagramView(Context context) {
        super(context);
    }

    public DiagramView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setType(int type) {
        this.type = type;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        int width = getWidth();
        int height = getHeight();
        
        if (width <= 0 || height <= 0) return;

        // Inicializar el sistema gráfico si no existe o si cambió el tamaño
        if (textura == null || textura.getAncho() != width || textura.getAlto() != height) {
            textura = new Textura2D(width, height, Graficos.FormatoTextura.ARGB8888);
            graficos = new Graficos2D(textura);
        }

        graficos.limpiar(Color.parseColor("#0D1117")); // Fondo oscuro profesional
        
        switch (type) {
            case 0:
                dibujarPinoutPIC(graficos);
                break;
            case 1:
                dibujarI2C(graficos);
                break;
            case 2:
                dibujarSPI(graficos);
                break;
        }

        // Volcar el bitmap de la textura al canvas de Android
        canvas.drawBitmap(textura.getBipmap(), 0, 0, null);
    }

    private void dibujarPinoutPIC(Graficos g) {
        float w = g.getAncho();
        float h = g.getAlto();
        float centerX = w / 2;
        float centerY = h / 2;
        float chipW = 140;
        float chipH = 450;
        
        int colorText = Color.parseColor("#E6EDF3");
        int colorPinLeft = Color.parseColor("#58A6FF");
        int colorPinRight = Color.parseColor("#FF7B72");
        int colorChip = Color.parseColor("#161B22");
        int colorLine = Color.parseColor("#30363D");

        g.getLapiz().setTextSize(26);
        g.dibujarTexto("PINOUT PIC 16F628A (DIP-18)", centerX - 180, 60, colorText);
        
        // Cuerpo del chip
        g.dibujarRectangulo(centerX - chipW/2, centerY - chipH/2, chipW, chipH, colorChip);
        g.dibujarRectangulo(centerX - 25, centerY - chipH/2, 50, 25, Color.parseColor("#30363D")); // Notch
        // Borde del chip
        g.dibujarLinea(centerX - chipW/2, centerY - chipH/2, centerX + chipW/2, centerY - chipH/2, colorLine);
        g.dibujarLinea(centerX - chipW/2, centerY + chipH/2, centerX + chipW/2, centerY + chipH/2, colorLine);
        g.dibujarLinea(centerX - chipW/2, centerY - chipH/2, centerX - chipW/2, centerY + chipH/2, colorLine);
        g.dibujarLinea(centerX + chipW/2, centerY - chipH/2, centerX + chipW/2, centerY + chipH/2, colorLine);

        String[] left = {
            "1: RA2 (SPI CS)", "2: RA3 (SCK)", "3: RA4 (T0)", "4: RA5 (MISO)", "5: VSS (GND)", 
            "6: RB0 (INT)", "7: RB1 (RX)", "8: RB2 (TX)", "9: RB3 (CCP)"
        };
        String[] right = {
            "18: RA1 (SCL)", "17: RA0 (SDA)", "16: OSC2/CLK", "15: OSC1/CLK", "14: VDD (VCC)", 
            "13: RB7 (PGC)", "12: RB6 (PGD)", "11: RB5", "10: RB4 (LED)"
        };

        g.getLapiz().setTextSize(18);
        for (int i = 0; i < 9; i++) {
            float y = centerY - chipH/2 + 45 + (i * 45);
            // Pines físicos
            g.dibujarRectangulo(centerX - chipW/2 - 25, y - 6, 25, 12, Color.parseColor("#8B949E"));
            g.dibujarTexto(left[i], centerX - chipW/2 - 185, y + 6, colorPinLeft);
            
            // Lado derecho
            g.dibujarRectangulo(centerX + chipW/2, y - 6, 25, 12, Color.parseColor("#8B949E"));
            g.dibujarTexto(right[i], centerX + chipW/2 + 35, y + 6, colorPinRight);
        }
    }

    private void dibujarI2C(Graficos g) {
        float w = g.getAncho();
        float h = g.getAlto();
        float centerX = w / 2;
        float centerY = h / 2;
        
        int colorText = Color.parseColor("#E6EDF3");
        int colorI2CLine = Color.parseColor("#79C0FF");
        int colorNote = Color.parseColor("#FFA657");
        int colorChipPIC = Color.parseColor("#1F6FEB");
        int colorChipMEM = Color.parseColor("#238636");

        g.getLapiz().setTextSize(26);
        g.dibujarTexto("CONEXIÓN I2C (EEPROM 24xx)", centerX - 180, 60, colorText);
        
        g.getLapiz().setTextSize(18);
        g.dibujarTexto("PIC RA0 (Pin 17) -> SDA (MEM Pin 5)", 60, 140, colorText);
        g.dibujarTexto("PIC RA1 (Pin 18) -> SCL (MEM Pin 6)", 60, 180, colorText);
        g.dibujarTexto("⚠ NOTA: Requiere R. Pull-Up 4.7k a VCC", 60, 230, colorNote);
        
        // Bloque PIC
        g.dibujarRectangulo(100, 320, 120, 180, colorChipPIC);
        g.dibujarTexto("PIC 16F628", 110, 310, colorText);
        
        // Bloque EEPROM
        g.dibujarRectangulo(w - 220, 320, 120, 180, colorChipMEM);
        g.dibujarTexto("EEPROM 24xx", w - 215, 310, colorText);
        
        // Líneas de conexión
        g.dibujarLinea(220, 360, w - 220, 360, colorI2CLine); // SDA
        g.dibujarTexto("SDA", centerX - 20, 355, colorI2CLine);
        
        g.dibujarLinea(220, 440, w - 220, 440, colorI2CLine); // SCL
        g.dibujarTexto("SCL", centerX - 20, 435, colorI2CLine);
        
        // Pull ups simbolicos
        g.dibujarLinea(centerX, 360, centerX, 300, colorNote);
        g.dibujarLinea(centerX + 20, 440, centerX + 20, 300, colorNote);
        g.dibujarTexto("Pull-ups (4.7k) a VCC", centerX - 70, 290, colorNote);
    }

    private void dibujarSPI(Graficos g) {
        float w = g.getAncho();
        float h = g.getAlto();
        float centerX = w / 2;
        
        int colorText = Color.parseColor("#E6EDF3");
        int colorSPILine = Color.parseColor("#D2A8FF");
        int colorChipPIC = Color.parseColor("#1F6FEB");
        int colorChipMEM = Color.parseColor("#238636");
        int colorAlert = Color.parseColor("#F85149");

        g.getLapiz().setTextSize(26);
        g.dibujarTexto("CONEXIÓN SPI (EEPROM 25xx)", centerX - 180, 60, colorText);
        
        g.getLapiz().setTextSize(18);
        g.dibujarTexto("PIC RA2 (P1) -> CS   (M1)", 60, 120, colorText);
        g.dibujarTexto("PIC RA3 (P2) -> SCK  (M6)", 60, 150, colorText);
        g.dibujarTexto("PIC RA5 (P4) <- MISO (M2)", 60, 180, colorText);
        g.dibujarTexto("PIC RA6 (P15)-> MOSI (M5)", 60, 210, colorText);
        g.dibujarTexto("⚠ WP y HOLD a VCC", 60, 250, colorAlert);
        
        // Bloques
        g.dibujarRectangulo(80, 320, 120, 300, colorChipPIC);
        g.dibujarTexto("PIC 16F628", 90, 310, colorText);
        
        g.dibujarRectangulo(w - 200, 320, 120, 300, colorChipMEM);
        g.dibujarTexto("EEPROM 25xx", w - 195, 310, colorText);
        
        // Conexiones
        float startX = 200;
        float endX = w - 200;
        
        g.dibujarLinea(startX, 350, endX, 350, colorSPILine); // CS
        g.dibujarTexto("CS", centerX - 15, 345, colorSPILine);
        
        g.dibujarLinea(startX, 410, endX, 410, colorSPILine); // SCK
        g.dibujarTexto("SCK", centerX - 15, 405, colorSPILine);
        
        g.dibujarLinea(startX, 470, endX, 470, colorSPILine); // MISO
        g.dibujarTexto("MISO", centerX - 15, 465, colorSPILine);
        
        g.dibujarLinea(startX, 530, endX, 530, colorSPILine); // MOSI
        g.dibujarTexto("MOSI", centerX - 15, 525, colorSPILine);
    }
}

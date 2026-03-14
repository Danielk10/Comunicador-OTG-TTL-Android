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

        graficos.limpiar(Color.WHITE);
        
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
        float chipW = 120;
        float chipH = 400;
        
        g.getLapiz().setTextSize(24);
        g.dibujarTexto("PIC 16F628A PINOUT (DIP-18)", centerX - 160, 50, Color.BLACK);
        
        // Cuerpo del chip
        g.dibujarRectangulo(centerX - chipW/2, centerY - chipH/2, chipW, chipH, Color.DKGRAY);
        g.dibujarRectangulo(centerX - 20, centerY - chipH/2, 40, 20, Color.LTGRAY); // Notch
        
        String[] pinesIzquierda = {"RA2/CS", "RA3/SCK", "RA4", "RA5/MISO", "VSS/GND", "RB0", "RB1/RX", "RB2/TX", "RB3"};
        String[] pinesDerecha = {"RA1/SCL", "RA0/SDA", "AN0/RA0", "AN1/RA1", "OSC2", "OSC1", "VDD/VCC", "RB7", "RB6", "RB5"};
        // Wait, PIC16F628A has 18 pins. 9 left, 9 right.
        
        String[] left = {
            "1: RA2 (SPI CS)", "2: RA3 (SCK)", "3: RA4", "4: RA5 (MISO)", "5: VSS (GND)", 
            "6: RB0", "7: RB1 (RX)", "8: RB2 (TX)", "9: RB3"
        };
        String[] right = {
            "18: RA1 (SCL)", "17: RA0 (SDA)", "16: RA7", "15: RA6 (MOSI)", "14: VDD (VCC)", 
            "13: RB7", "12: RB6", "11: RB5", "10: RB4 (LED)"
        };

        g.getLapiz().setTextSize(18);
        for (int i = 0; i < 9; i++) {
            float y = centerY - chipH/2 + 30 + (i * 42);
            // Pines
            g.dibujarRectangulo(centerX - chipW/2 - 20, y - 5, 20, 10, Color.GRAY);
            g.dibujarTexto(left[i], centerX - chipW/2 - 170, y + 5, Color.BLUE);
            
            // Lado derecho (invertido en orden fisico)
            float yRight = centerY - chipH/2 + 30 + (i * 42);
            g.dibujarRectangulo(centerX + chipW/2, yRight - 5, 20, 10, Color.GRAY);
            g.dibujarTexto(right[i], centerX + chipW/2 + 25, yRight + 5, Color.RED);
        }
    }

    private void dibujarI2C(Graficos g) {
        float centerX = g.getAncho() / 2;
        float centerY = g.getAlto() / 2;
        
        g.getLapiz().setTextSize(24);
        g.dibujarTexto("CONEXIONES I2C (EEPROM 24xx)", centerX - 180, 50, Color.BLACK);
        
        g.getLapiz().setTextSize(18);
        g.dibujarTexto("PIC RA0 -> SDA (EEPROM Pin 5)", 50, 150, Color.DKGRAY);
        g.dibujarTexto("PIC RA1 -> SCL (EEPROM Pin 6)", 50, 200, Color.DKGRAY);
        g.dibujarTexto("Requerido: Pull-ups 4.7k a VCC", 50, 250, Color.RED);
        
        // Diagrama simple
        g.dibujarRectangulo(50, 300, 100, 150, Color.BLUE); // PIC
        g.dibujarTexto("PIC", 75, 290, Color.BLUE);
        
        g.dibujarRectangulo(centerX + 50, 300, 100, 150, Color.RED); // EEPROM
        g.dibujarTexto("EEPROM", centerX + 60, 290, Color.RED);
        
        // Lineas
        g.dibujarLinea(150, 340, centerX + 50, 340, Color.BLACK); // SDA
        g.dibujarTexto("SDA", centerX - 20, 335, Color.BLACK);
        
        g.dibujarLinea(150, 400, centerX + 50, 400, Color.BLACK); // SCL
        g.dibujarTexto("SCL", centerX - 20, 395, Color.BLACK);
    }

    private void dibujarSPI(Graficos g) {
        float centerX = g.getAncho() / 2;
        
        g.getLapiz().setTextSize(24);
        g.dibujarTexto("CONEXIONES SPI (EEPROM 25xx)", centerX - 180, 50, Color.BLACK);
        
        g.getLapiz().setTextSize(18);
        g.dibujarTexto("PIC RA2 -> CS (EEPROM Pin 1)", 50, 120, Color.DKGRAY);
        g.dibujarTexto("PIC RA3 -> SCK (EEPROM Pin 6)", 50, 160, Color.DKGRAY);
        g.dibujarTexto("PIC RA5 <- SO (EEPROM Pin 2)", 50, 200, Color.DKGRAY);
        g.dibujarTexto("PIC RA6 -> SI (EEPROM Pin 5)", 50, 240, Color.DKGRAY);
        
        // Diagrama
        g.dibujarRectangulo(50, 300, 100, 250, Color.BLUE); // PIC
        g.dibujarRectangulo(centerX + 50, 300, 100, 250, Color.RED); // EEPROM
        
        g.dibujarLinea(150, 330, centerX + 50, 330, Color.BLACK); // CS
        g.dibujarTexto("CS", centerX - 20, 325, Color.BLACK);
        
        g.dibujarLinea(150, 390, centerX + 50, 390, Color.BLACK); // SCK
        g.dibujarTexto("SCK", centerX - 20, 385, Color.BLACK);
        
        g.dibujarLinea(150, 450, centerX + 50, 450, Color.BLACK); // MISO/SO
        g.dibujarTexto("MISO", centerX - 20, 445, Color.BLACK);
        
        g.dibujarLinea(150, 510, centerX + 50, 510, Color.BLACK); // MOSI/SI
        g.dibujarTexto("MOSI", centerX - 20, 505, Color.BLACK);
    }
}

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
        float centerX = g.getAncho() / 2;
        float centerY = g.getAlto() / 2;
        
        int colorText = Color.parseColor("#E6EDF3");
        g.getLapiz().setTextSize(26);
        g.dibujarTexto("PINOUT PIC 16F628A (DIP-18)", centerX - 180, 60, colorText);
        
        String[] left = {
            "1: RA2 (CS)", "2: RA3 (SCK)", "3: RA4", "4: RA5 (MISO)", "5: VSS (GND)", 
            "6: RB0", "7: RB1 (RX)", "8: RB2 (TX)", "9: RB3"
        };
        String[] right = {
            "18: RA1 (SCL)", "17: RA0 (SDA)", "16: OSC2", "15: OSC1 (MOSI)", "14: VDD (VCC)", 
            "13: RB7", "12: RB6", "11: RB5", "10: RB4"
        };

        dibujarChipDIP(g, centerX, centerY, 18, "PIC 16F628A", left, right);
    }

    private void dibujarChipDIP(Graficos g, float x, float y, int numPins, String label, String[] leftLabels, String[] rightLabels) {
        int halfPins = numPins / 2;
        float pinSpacing = 45;
        float chipW = 140;
        float chipH = pinSpacing * halfPins + 40;
        float startY = y - chipH / 2;
        
        int colorChip = Color.parseColor("#161B22");
        int colorPin = Color.parseColor("#8B949E");
        int colorText = Color.parseColor("#E6EDF3");
        int colorLabel = Color.parseColor("#79C0FF");
        
        // Cuerpo del chip (negro)
        g.dibujarRectangulo(x - chipW/2, startY, chipW, chipH, colorChip);
        // Notch
        g.dibujarRectangulo(x - 20, startY, 40, 15, Color.parseColor("#0D1117"));
        
        g.getLapiz().setTextSize(18);
        for (int i = 0; i < halfPins; i++) {
            float py = startY + 35 + (i * pinSpacing);
            // Pines metálicos
            g.dibujarRectangulo(x - chipW/2 - 20, py - 6, 20, 12, colorPin);
            g.dibujarRectangulo(x + chipW/2, py - 6, 20, 12, colorPin);
            
            // Labels
            if (leftLabels != null && i < leftLabels.length) {
                g.dibujarTexto(leftLabels[i], x - chipW/2 - 160, py + 6, colorText);
            }
            if (rightLabels != null && i < rightLabels.length) {
                g.dibujarTexto(rightLabels[i], x + chipW/2 + 30, py + 6, colorText);
            }
        }
        
        g.getLapiz().setTextSize(16);
        g.dibujarTexto(label, x - 50, y + chipH/2 + 25, colorLabel);
    }

    private void dibujarI2C(Graficos g) {
        float w = g.getAncho();
        float centerX = w / 2;
        
        int colorText = Color.parseColor("#E6EDF3");
        int colorWire = Color.parseColor("#79C0FF");
        int colorPower = Color.parseColor("#FF7B72");
        int colorGND = Color.parseColor("#8B949E");
        int colorResistor = Color.parseColor("#FFA657");

        g.getLapiz().setTextSize(26);
        g.dibujarTexto("CIRCUITO REAL I2C (EEPROM 24xx)", centerX - 180, 60, colorText);
        
        float picX = 180;
        float memX = w - 150;
        float chipsY = 400;

        dibujarChipDIP(g, picX, chipsY, 18, "PIC 16F628A", null, null);
        dibujarChipDIP(g, memX, chipsY, 8, "EEPROM 24xx", null, null);
        
        // Coordenadas calculadas para pines
        float picR_X = picX + 70 + 20;
        float picL_X = picX - 70 - 20;
        float memR_X = memX + 70 + 20;
        float memL_X = memX - 70 - 20;
        
        // PIC 16F628A (DIP-18)
        // Right side (18, 17, 16, 15, 14, 13, 12, 11, 10)
        float picP18y = chipsY - (18 * 45 / 4) + 35 + (0 * 45); // Pin 18 (RA1/SCL)
        float picP17y = chipsY - (18 * 45 / 4) + 35 + (1 * 45); // Pin 17 (RA0/SDA)
        float picP14y = chipsY - (18 * 45 / 4) + 35 + (4 * 45); // Pin 14 (VDD)
        // Left side (1, 2, 3, 4, 5, 6, 7, 8, 9)
        float picP5y = chipsY - (18 * 45 / 4) + 35 + (4 * 45); // Pin 5 (VSS/GND)
        
        // EEPROM 24xx (DIP-8)
        // Right side (8, 7, 6, 5)
        float memP8y = chipsY - (8 * 45 / 4) + 35 + (0 * 45); // Pin 8 (VCC)
        float memP6y = chipsY - (8 * 45 / 4) + 35 + (2 * 45); // Pin 6 (SCL)
        float memP5y = chipsY - (8 * 45 / 4) + 35 + (3 * 45); // Pin 5 (SDA)
        // Left side (1, 2, 3, 4)
        float memP4y = chipsY - (8 * 45 / 4) + 35 + (3 * 45); // Pin 4 (VSS/GND)

        // SDA: PIC P17 -> MEM P5
        g.dibujarLinea(picR_X, picP17y, memR_X - 15, picP17y, colorWire);
        g.dibujarLinea(memR_X - 15, picP17y, memR_X - 15, memP5y, colorWire);
        g.dibujarLinea(memR_X - 15, memP5y, memR_X, memP5y, colorWire);
        g.dibujarTexto("SDA", centerX - 20, picP17y - 5, colorWire);
        
        // SCL: PIC P18 -> MEM P6
        g.dibujarLinea(picR_X, picP18y, memR_X - 35, picP18y, colorWire);
        g.dibujarLinea(memR_X - 35, picP18y, memR_X - 35, memP6y, colorWire);
        g.dibujarLinea(memR_X - 35, memP6y, memR_X, memP6y, colorWire);
        g.dibujarTexto("SCL", centerX - 20, picP18y - 5, colorWire);
        
        // PULL-UPS
        // SDA Pull-up
        g.dibujarRectangulo(centerX - 10, picP17y - 40, 20, 10, colorResistor);
        g.dibujarLinea(centerX, picP17y, centerX, picP17y - 30, colorResistor);
        g.dibujarLinea(centerX, picP17y - 50, centerX, picP17y - 70, colorPower); // To VCC
        g.dibujarTexto("4.7k", centerX + 15, picP17y - 35, colorResistor);
        
        // SCL Pull-up
        g.dibujarRectangulo(centerX - 30, picP18y - 40, 20, 10, colorResistor);
        g.dibujarLinea(centerX - 20, picP18y, centerX - 20, picP18y - 30, colorResistor);
        g.dibujarLinea(centerX - 20, picP18y - 50, centerX - 20, picP18y - 70, colorPower); // To VCC

        // VCC connections
        g.dibujarLinea(picR_X, picP14y, picR_X + 50, picP14y, colorPower);
        g.dibujarLinea(picR_X + 50, picP14y, picR_X + 50, memP8y, colorPower);
        g.dibujarLinea(picR_X + 50, memP8y, memR_X, memP8y, colorPower);
        g.dibujarLinea(picR_X + 50, picP14y, picR_X + 50, picP17y - 70, colorPower); // Connect pull-ups to VCC
        g.dibujarTexto("VCC", picR_X + 60, picP14y, colorPower);

        // GND connections
        g.dibujarLinea(picL_X, picP5y, picL_X - 50, picP5y, colorGND);
        g.dibujarLinea(picL_X - 50, picP5y, picL_X - 50, memP4y, colorGND);
        g.dibujarLinea(picL_X - 50, memP4y, memL_X, memP4y, colorGND);
        g.dibujarTexto("GND", picL_X - 60, picP5y, colorGND);
    }

    private void dibujarSPI(Graficos g) {
        float w = g.getAncho();
        float centerX = w / 2;
        
        int colorText = Color.parseColor("#E6EDF3");
        int colorWire = Color.parseColor("#D2A8FF");
        int colorPower = Color.parseColor("#FF7B72");
        int colorGND = Color.parseColor("#8B949E");
        int colorAlert = Color.parseColor("#F85149");

        g.getLapiz().setTextSize(26);
        g.dibujarTexto("CIRCUITO REAL SPI (EEPROM 25xx)", centerX - 180, 60, colorText);
        
        float picX = 180;
        float memX = w - 150;
        float chipsY = 400;

        dibujarChipDIP(g, picX, chipsY, 18, "PIC 16F628A", null, null);
        dibujarChipDIP(g, memX, chipsY, 8, "EEPROM 25xx", null, null);
        
        float picR_X = picX + 70 + 20;
        float picL_X = picX - 70 - 20;
        float memR_X = memX + 70 + 20;
        float memL_X = memX - 70 - 20;
        
        // Pines PIC 16F628A (DIP-18)
        // Left side (1, 2, 3, 4, 5, 6, 7, 8, 9)
        float picP1y = chipsY - (18 * 45 / 4) + 35 + (0 * 45); // Pin 1 (RA2/CS)
        float picP2y = chipsY - (18 * 45 / 4) + 35 + (1 * 45); // Pin 2 (RA3/SCK)
        float picP4y = chipsY - (18 * 45 / 4) + 35 + (3 * 45); // Pin 4 (RA5/MISO)
        float picP5y = chipsY - (18 * 45 / 4) + 35 + (4 * 45); // Pin 5 (VSS/GND)
        // Right side (18, 17, 16, 15, 14, 13, 12, 11, 10)
        float picP15y = chipsY - (18 * 45 / 4) + 35 + (3 * 45); // Pin 15 (OSC1/MOSI)
        float picP14y = chipsY - (18 * 45 / 4) + 35 + (4 * 45); // Pin 14 (VDD/VCC)

        // Pines EEPROM 25xx (DIP-8)
        // Left side (1, 2, 3, 4)
        float memP1y = chipsY - (8 * 45 / 4) + 35 + (0 * 45); // Pin 1 (CS)
        float memP2y = chipsY - (8 * 45 / 4) + 35 + (1 * 45); // Pin 2 (MISO/SO)
        float memP3y = chipsY - (8 * 45 / 4) + 35 + (2 * 45); // Pin 3 (WP)
        float memP4y = chipsY - (8 * 45 / 4) + 35 + (3 * 45); // Pin 4 (VSS/GND)
        // Right side (8, 7, 6, 5)
        float memP8y = chipsY - (8 * 45 / 4) + 35 + (0 * 45); // Pin 8 (VCC)
        float memP7y = chipsY - (8 * 45 / 4) + 35 + (1 * 45); // Pin 7 (HOLD)
        float memP6y = chipsY - (8 * 45 / 4) + 35 + (2 * 45); // Pin 6 (SCK)
        float memP5y = chipsY - (8 * 45 / 4) + 35 + (3 * 45); // Pin 5 (MOSI/SI)

        // CS: PIC P1 -> MEM P1
        g.dibujarLinea(picL_X, picP1y, 50, picP1y, colorWire);
        g.dibujarLinea(50, picP1y, 50, memP1y, colorWire);
        g.dibujarLinea(50, memP1y, memL_X, memP1y, colorWire);
        g.dibujarTexto("CS", 60, picP1y - 5, colorWire);
        
        // MISO: PIC RA5 (P4) <- MEM SO (P2)
        g.dibujarLinea(picL_X, picP4y, 70, picP4y, colorWire);
        g.dibujarLinea(70, picP4y, 70, memP2y, colorWire);
        g.dibujarLinea(70, memP2y, memL_X, memP2y, colorWire);
        g.dibujarTexto("MISO", 80, picP4y - 5, colorWire);
        
        // MOSI: PIC OSC1 (P15) -> MEM SI (P5)
        g.dibujarLinea(picR_X, picP15y, w - 50, picP15y, colorWire);
        g.dibujarLinea(w - 50, picP15y, w - 50, memP5y, colorWire);
        g.dibujarLinea(w - 50, memP5y, memR_X, memP5y, colorWire);
        g.dibujarTexto("MOSI", w - 100, picP15y - 5, colorWire);
        
        // SCK: PIC RA3 (P2) -> MEM SCK (P6)
        g.dibujarLinea(picL_X, picP2y, 30, picP2y, colorWire);
        g.dibujarLinea(30, picP2y, 30, 700, colorWire);
        g.dibujarLinea(30, 700, w - 30, 700, colorWire);
        g.dibujarLinea(w - 30, 700, w - 30, memP6y, colorWire);
        g.dibujarLinea(w - 30, memP6y, memR_X, memP6y, colorWire);
        g.dibujarTexto("SCK", centerX, 695, colorWire);

        // VCC connections
        g.dibujarLinea(picR_X, picP14y, picR_X + 50, picP14y, colorPower);
        g.dibujarLinea(picR_X + 50, picP14y, picR_X + 50, memP8y, colorPower);
        g.dibujarLinea(picR_X + 50, memP8y, memR_X, memP8y, colorPower);
        g.dibujarTexto("VCC", picR_X + 60, picP14y, colorPower);

        // GND connections
        g.dibujarLinea(picL_X, picP5y, picL_X - 50, picP5y, colorGND);
        g.dibujarLinea(picL_X - 50, picP5y, picL_X - 50, memP4y, colorGND);
        g.dibujarLinea(picL_X - 50, memP4y, memL_X, memP4y, colorGND);
        g.dibujarTexto("GND", picL_X - 60, picP5y, colorGND);

        // WP and HOLD to VCC
        g.dibujarLinea(memL_X, memP3y, memL_X - 20, memP3y, colorAlert);
        g.dibujarLinea(memL_X - 20, memP3y, memL_X - 20, memP8y - 50, colorAlert);
        g.dibujarLinea(memL_X - 20, memP8y - 50, memR_X - 20, memP8y - 50, colorAlert);
        g.dibujarLinea(memR_X - 20, memP8y - 50, memR_X - 20, memP7y, colorAlert);
        g.dibujarLinea(memR_X - 20, memP7y, memR_X, memP7y, colorAlert);
        g.dibujarLinea(memR_X - 20, memP8y - 50, memR_X, memP8y - 50, colorAlert); // Connect to VCC line
        g.dibujarTexto("WP (P3) y HOLD (P7) a VCC", centerX - 100, 780, colorAlert);
    }
}

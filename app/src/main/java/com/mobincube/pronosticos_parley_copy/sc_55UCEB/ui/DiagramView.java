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

        graficos.limpiar(Color.parseColor("#F8F9FA")); // Tema claro profesional
        
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
        
        int colorText = Color.parseColor("#24292F");
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
        int colorText = Color.parseColor("#24292F");
        int colorLabel = Color.parseColor("#0969DA");
        
        // Sombra suave
        g.dibujarRectangulo(x - chipW/2 + 5, startY + 5, chipW, chipH, Color.LTGRAY);
        // Cuerpo del chip (negro)
        g.dibujarRectangulo(x - chipW/2, startY, chipW, chipH, colorChip);
        // Notch
        g.dibujarRectangulo(x - 20, startY, 40, 15, Color.BLACK);
        
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

    private void dibujarAdaptadorTTL(Graficos g, float x, float y) {
        float w = 180;
        float h = 100;
        int colorBody = Color.parseColor("#0366D6");
        int colorText = Color.WHITE;
        
        // Cuerpo adaptador
        g.dibujarRectangulo(x - w/2, y - h/2, w, h, colorBody);
        g.dibujarRectangulo(x - w/2 - 30, y - 20, 30, 40, Color.BLACK); // Conector USB
        
        g.getLapiz().setTextSize(18);
        g.dibujarTexto("USB-TTL", x - 40, y + 10, colorText);
        
        // Pines de salida
        float py = y - h/2 + 25;
        g.getLapiz().setTextSize(14);
        g.dibujarTexto("VCC", x + w/2 + 10, py, Color.RED);
        g.dibujarTexto("TX", x + w/2 + 10, py + 25, Color.BLACK);
        g.dibujarTexto("RX", x + w/2 + 10, py + 50, Color.BLACK);
        g.dibujarTexto("GND", x + w/2 + 10, py + 75, Color.BLACK);
    }

    private void dibujarI2C(Graficos g) {
        float w = g.getAncho();
        float centerX = w / 2;
        
        int colorText = Color.parseColor("#24292F");
        int colorWire = Color.parseColor("#0969DA");
        int colorPower = Color.RED;
        int colorGND = Color.BLACK;
        int colorResistor = Color.parseColor("#AF7E00");
        int colorAlert = Color.parseColor("#9A6700");

        g.getLapiz().setTextSize(26);
        g.dibujarTexto("DIAGRAMA DE CONEXIÓN I2C", centerX - 180, 60, colorText);
        
        float adapterX = 120;
        float adapterY = 150;
        float picX = 220;
        float memX = w - 180;
        float chipsY = 450;

        // Componentes
        dibujarAdaptadorTTL(g, adapterX, adapterY);
        dibujarChipDIP(g, picX, chipsY, 18, "PIC 16F628A", null, null);
        dibujarChipDIP(g, memX, chipsY, 8, "EEPROM 24xx", null, null);

        // Instrucciones en pantalla
        g.getLapiz().setTextSize(18);
        g.dibujarTexto("1. Conectar USB-TTL: TX -> P7 (RX), RX -> P8 (TX)", 50, 250, colorText);
        g.dibujarTexto("2. I2C SDA: P17 (PIC) -> P5 (MEM)", 50, 280, colorText);
        g.dibujarTexto("3. I2C SCL: P18 (PIC) -> P6 (MEM)", 50, 310, colorText);
        g.dibujarTexto("⚠ Pull-ups 4.7k requeridas en SDA y SCL", 50, 340, colorAlert);

        // Coordenadas calculadas para pines
        float adapterVCC_X = adapterX + 90 + 10;
        float adapterVCC_Y = adapterY - 50 + 25;
        float adapterTX_Y = adapterY - 50 + 50;
        float adapterRX_Y = adapterY - 50 + 75;
        float adapterGND_X = adapterX + 90 + 10;
        float adapterGND_Y = adapterY - 50 + 100;

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
        float picP7y = chipsY - (18 * 45 / 4) + 35 + (6 * 45); // Pin 7 (RB1/RX)
        float picP8y = chipsY - (18 * 45 / 4) + 35 + (7 * 45); // Pin 8 (RB2/TX)
        
        // EEPROM 24xx (DIP-8)
        // Right side (8, 7, 6, 5)
        float memP8y = chipsY - (8 * 45 / 4) + 35 + (0 * 45); // Pin 8 (VCC)
        float memP6y = chipsY - (8 * 45 / 4) + 35 + (2 * 45); // Pin 6 (SCL)
        float memP5y = chipsY - (8 * 45 / 4) + 35 + (3 * 45); // Pin 5 (SDA)
        // Left side (1, 2, 3, 4)
        float memP4y = chipsY - (8 * 45 / 4) + 35 + (3 * 45); // Pin 4 (VSS/GND)

        // Conexiones Adaptador -> PIC
        // VCC
        g.dibujarLinea(adapterVCC_X, adapterVCC_Y, adapterVCC_X + 50, adapterVCC_Y, colorPower);
        g.dibujarLinea(adapterVCC_X + 50, adapterVCC_Y, adapterVCC_X + 50, picP14y, colorPower);
        g.dibujarLinea(adapterVCC_X + 50, picP14y, picR_X, picP14y, colorPower);
        g.dibujarTexto("VCC", adapterVCC_X + 60, adapterVCC_Y - 10, colorPower);

        // GND
        g.dibujarLinea(adapterGND_X, adapterGND_Y, adapterGND_X + 50, adapterGND_Y, colorGND);
        g.dibujarLinea(adapterGND_X + 50, adapterGND_Y, adapterGND_X + 50, picP5y, colorGND);
        g.dibujarLinea(adapterGND_X + 50, picP5y, picL_X, picP5y, colorGND);
        g.dibujarTexto("GND", adapterGND_X + 60, adapterGND_Y + 10, colorGND);

        // TX (Adapter) -> RX (PIC P7)
        g.dibujarLinea(adapterVCC_X, adapterTX_Y, picL_X - 30, adapterTX_Y, Color.BLACK);
        g.dibujarLinea(picL_X - 30, adapterTX_Y, picL_X - 30, picP7y, Color.BLACK);
        g.dibujarLinea(picL_X - 30, picP7y, picL_X, picP7y, Color.BLACK);
        g.dibujarTexto("TX", adapterVCC_X + 10, adapterTX_Y - 10, Color.BLACK);

        // RX (Adapter) -> TX (PIC P8)
        g.dibujarLinea(adapterVCC_X, adapterRX_Y, picL_X - 10, adapterRX_Y, Color.BLACK);
        g.dibujarLinea(picL_X - 10, adapterRX_Y, picL_X - 10, picP8y, Color.BLACK);
        g.dibujarLinea(picL_X - 10, picP8y, picL_X, picP8y, Color.BLACK);
        g.dibujarTexto("RX", adapterVCC_X + 10, adapterRX_Y - 10, Color.BLACK);

        // I2C SDA: PIC P17 -> MEM P5
        g.dibujarLinea(picR_X, picP17y, memR_X - 15, picP17y, colorWire);
        g.dibujarLinea(memR_X - 15, picP17y, memR_X - 15, memP5y, colorWire);
        g.dibujarLinea(memR_X - 15, memP5y, memR_X, memP5y, colorWire);
        g.dibujarTexto("SDA", centerX - 20, picP17y - 5, colorWire);
        
        // I2C SCL: PIC P18 -> MEM P6
        g.dibujarLinea(picR_X, picP18y, memR_X - 35, picP18y, colorWire);
        g.dibujarLinea(memR_X - 35, picP18y, memR_X - 35, memP6y, colorWire);
        g.dibujarLinea(memR_X - 35, memP6y, memR_X, memP6y, colorWire);
        g.dibujarTexto("SCL", centerX - 20, picP18y - 5, colorWire);
        
        // PULL-UPS
        // SDA Pull-up
        g.dibujarRectangulo(centerX - 10, picP17y - 50, 40, 15, colorResistor); // Resistor symbol
        g.dibujarLinea(centerX + 10, picP17y, centerX + 10, picP17y - 40, colorWire);
        g.dibujarLinea(centerX + 10, picP17y - 65, centerX + 10, adapterVCC_Y, colorPower); // To VCC bus
        g.dibujarTexto("4.7k", centerX + 55, picP17y - 45, colorResistor);
        
        // SCL Pull-up
        g.dibujarRectangulo(centerX - 30, picP18y - 50, 40, 15, colorResistor); // Resistor symbol
        g.dibujarLinea(centerX - 10, picP18y, centerX - 10, picP18y - 40, colorWire);
        g.dibujarLinea(centerX - 10, picP18y - 65, centerX - 10, adapterVCC_Y, colorPower); // To VCC bus

        // VCC bus to EEPROM
        g.dibujarLinea(adapterVCC_X + 50, picP14y, adapterVCC_X + 50, memP8y, colorPower);
        g.dibujarLinea(adapterVCC_X + 50, memP8y, memR_X, memP8y, colorPower);
        
        // GND bus to EEPROM
        g.dibujarLinea(adapterGND_X + 50, picP5y, adapterGND_X + 50, memP4y, colorGND);
        g.dibujarLinea(adapterGND_X + 50, memP4y, memL_X, memP4y, colorGND);
    }

    private void dibujarSPI(Graficos g) {
        float w = g.getAncho();
        float centerX = w / 2;
        
        int colorText = Color.parseColor("#24292F");
        int colorWire = Color.parseColor("#8250DF");
        int colorPower = Color.RED;
        int colorGND = Color.BLACK;
        int colorAlert = Color.RED;

        g.getLapiz().setTextSize(26);
        g.dibujarTexto("DIAGRAMA DE CONEXIÓN SPI", centerX - 180, 60, colorText);
        
        float adapterX = 120;
        float adapterY = 150;
        float picX = 220;
        float memX = w - 180;
        float chipsY = 500;

        dibujarAdaptadorTTL(g, adapterX, adapterY);
        dibujarChipDIP(g, picX, chipsY, 18, "PIC 16F628A", null, null);
        dibujarChipDIP(g, memX, chipsY, 8, "EEPROM 25xx", null, null);

        g.getLapiz().setTextSize(18);
        g.dibujarTexto("Conexiones SPI Directas:", 50, 280, colorText);
        g.dibujarTexto("- P1 (CS), P2 (SCK), P4 (MISO), P15 (MOSI)", 50, 310, colorText);
        g.dibujarTexto("⚠ WP(3) y HOLD(7) -> VCC", 50, 340, colorAlert);

        // Coordenadas calculadas para pines
        float adapterVCC_X = adapterX + 90 + 10;
        float adapterVCC_Y = adapterY - 50 + 25;
        float adapterGND_X = adapterX + 90 + 10;
        float adapterGND_Y = adapterY - 50 + 100;

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

        // Conexiones Adaptador -> PIC (VCC, GND)
        // VCC
        g.dibujarLinea(adapterVCC_X, adapterVCC_Y, adapterVCC_X + 50, adapterVCC_Y, colorPower);
        g.dibujarLinea(adapterVCC_X + 50, adapterVCC_Y, adapterVCC_X + 50, picP14y, colorPower);
        g.dibujarLinea(adapterVCC_X + 50, picP14y, picR_X, picP14y, colorPower);
        g.dibujarTexto("VCC", adapterVCC_X + 60, adapterVCC_Y - 10, colorPower);

        // GND
        g.dibujarLinea(adapterGND_X, adapterGND_Y, adapterGND_X + 50, adapterGND_Y, colorGND);
        g.dibujarLinea(adapterGND_X + 50, adapterGND_Y, adapterGND_X + 50, picP5y, colorGND);
        g.dibujarLinea(adapterGND_X + 50, picP5y, picL_X, picP5y, colorGND);
        g.dibujarTexto("GND", adapterGND_X + 60, adapterGND_Y + 10, colorGND);

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

        // VCC bus to EEPROM
        g.dibujarLinea(adapterVCC_X + 50, picP14y, adapterVCC_X + 50, memP8y, colorPower);
        g.dibujarLinea(adapterVCC_X + 50, memP8y, memR_X, memP8y, colorPower);

        // GND bus to EEPROM
        g.dibujarLinea(adapterGND_X + 50, picP5y, adapterGND_X + 50, memP4y, colorGND);
        g.dibujarLinea(adapterGND_X + 50, memP4y, memL_X, memP4y, colorGND);

        // WP and HOLD to VCC
        g.dibujarLinea(memL_X, memP3y, memL_X - 20, memP3y, colorAlert);
        g.dibujarLinea(memL_X - 20, memP3y, memL_X - 20, memP8y - 50, colorAlert);
        g.dibujarLinea(memL_X - 20, memP8y - 50, memR_X - 20, memP8y - 50, colorAlert);
        g.dibujarLinea(memR_X - 20, memP8y - 50, memR_X - 20, memP7y, colorAlert);
        g.dibujarLinea(memR_X - 20, memP7y, memR_X, memP7y, colorAlert);
        g.dibujarLinea(memR_X - 20, memP8y - 50, adapterVCC_X + 50, memP8y - 50, colorAlert); // Connect to VCC bus
        g.dibujarTexto("WP (P3) y HOLD (P7) a VCC", centerX - 100, 780, colorAlert);
    }
}

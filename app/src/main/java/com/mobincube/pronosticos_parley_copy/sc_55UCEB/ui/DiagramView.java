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
        float w = 200;
        float h = 120;
        int colorPCB = Color.parseColor("#D73A49"); // Rojo FTDI
        int colorText = Color.WHITE;
        int colorPin = Color.parseColor("#8B949E");
        
        // Cuerpo adaptador (PCB Roja)
        g.dibujarRectangulo(x - w/2, y - h/2, w, h, colorPCB);
        g.dibujarRectangulo(x - w/2 - 20, y - 15, 25, 30, Color.LTGRAY); // Conector USB
        
        // Chip FT232R (negro en el centro)
        g.dibujarRectangulo(x - 30, y - 25, 60, 50, Color.BLACK);
        g.getLapiz().setTextSize(14);
        g.dibujarTexto("FTDI", x - 15, y + 5, Color.WHITE);
        
        // 6 Pines en hilera (estilo FTDI Basic)
        String[] pins = {"DTR", "RX", "TX", "VCC", "CTS", "GND"};
        float pinX = x + w/2;
        float startY = y - h/2 + 20;
        g.getLapiz().setTextSize(12);
        
        for (int i = 0; i < 6; i++) {
            float py = startY + (i * 18);
            g.dibujarRectangulo(pinX, py - 5, 15, 10, colorPin);
            g.dibujarTexto(pins[i], pinX + 20, py + 4, Color.BLACK);
        }
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

        // Coordenadas calculadas para pines del FTDI (w=200, h=120)
        float adapterPinX = adapterX + 100;
        float adapterVCC_Y = adapterY - 60 + 20 + 3 * 18; // Pin VCC (idx 3)
        float adapterTX_Y = adapterY - 60 + 20 + 2 * 18;  // Pin TX (idx 2)
        float adapterRX_Y = adapterY - 60 + 20 + 1 * 18;  // Pin RX (idx 1)
        float adapterGND_Y = adapterY - 60 + 20 + 5 * 18; // Pin GND (idx 5)

        float picR_X = picX + 70 + 20;
        float picL_X = picX - 70 - 20;
        float memR_X = memX + 70 + 20;
        float memL_X = memX - 70 - 20;
        
        // PIC 16F628A (DIP-18)
        float picP18y = chipsY - (18 * 45 / 4) + 35 + (0 * 45); // Pin 18 (RA1/SCL)
        float picP17y = chipsY - (18 * 45 / 4) + 35 + (1 * 45); // Pin 17 (RA0/SDA)
        float picP14y = chipsY - (18 * 45 / 4) + 35 + (4 * 45); // Pin 14 (VDD)
        
        float picP5y = chipsY - (18 * 45 / 4) + 35 + (4 * 45); // Pin 5 (VSS/GND) - Izquierda
        float picP7y = chipsY - (18 * 45 / 4) + 35 + (6 * 45); // Pin 7 (RB1/RX) - Izquierda
        float picP8y = chipsY - (18 * 45 / 4) + 35 + (7 * 45); // Pin 8 (RB2/TX) - Izquierda
        
        // EEPROM 24xx (DIP-8)
        float memP8y = chipsY - (8 * 45 / 4) + 35 + (0 * 45); 
        float memP6y = chipsY - (8 * 45 / 4) + 35 + (2 * 45); 
        float memP5y = chipsY - (8 * 45 / 4) + 35 + (3 * 45); 
        float memP4y = chipsY - (8 * 45 / 4) + 35 + (3 * 45); 

        // Conexiones Adaptador -> PIC
        // VCC
        g.dibujarLinea(adapterPinX, adapterVCC_Y, adapterPinX + 50, adapterVCC_Y, colorPower);
        g.dibujarLinea(adapterPinX + 50, adapterVCC_Y, adapterPinX + 50, picP14y, colorPower);
        g.dibujarLinea(adapterPinX + 50, picP14y, picR_X, picP14y, colorPower);
        g.dibujarTexto("VCC", adapterPinX + 60, adapterVCC_Y - 5, colorPower);

        // GND
        g.dibujarLinea(adapterPinX, adapterGND_Y, adapterPinX + 30, adapterGND_Y, colorGND);
        g.dibujarLinea(adapterPinX + 30, adapterGND_Y, adapterPinX + 30, picP5y, colorGND);
        g.dibujarLinea(adapterPinX + 30, picP5y, picL_X, picP5y, colorGND);
        g.dibujarTexto("GND", adapterPinX + 60, adapterGND_Y + 10, colorGND);

        // TX (Adapter) -> RX (PIC P7)
        g.dibujarLinea(adapterPinX, adapterTX_Y, picL_X - 30, adapterTX_Y, Color.BLACK);
        g.dibujarLinea(picL_X - 30, adapterTX_Y, picL_X - 30, picP7y, Color.BLACK);
        g.dibujarLinea(picL_X - 30, picP7y, picL_X, picP7y, Color.BLACK);
        
        // RX (Adapter) -> TX (PIC P8)
        g.dibujarLinea(adapterPinX, adapterRX_Y, picL_X - 15, adapterRX_Y, Color.BLACK);
        g.dibujarLinea(picL_X - 15, adapterRX_Y, picL_X - 15, picP8y, Color.BLACK);
        g.dibujarLinea(picL_X - 15, picP8y, picL_X, picP8y, Color.BLACK);

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
        g.dibujarRectangulo(centerX - 10, picP17y - 50, 40, 15, colorResistor);
        g.dibujarLinea(centerX + 10, picP17y, centerX + 10, picP17y - 40, colorWire);
        g.dibujarLinea(centerX + 10, picP17y - 65, centerX + 10, adapterVCC_Y, colorPower); 
        
        g.dibujarRectangulo(centerX - 30, picP18y - 50, 40, 15, colorResistor);
        g.dibujarLinea(centerX - 10, picP18y, centerX - 10, picP18y - 40, colorWire);
        g.dibujarLinea(centerX - 10, picP18y - 65, centerX - 10, adapterVCC_Y, colorPower); 

        // VCC bus to EEPROM
        g.dibujarLinea(adapterPinX + 50, picP14y, adapterPinX + 50, memP8y, colorPower);
        g.dibujarLinea(adapterPinX + 50, memP8y, memR_X, memP8y, colorPower);
        
        // GND bus to EEPROM
        g.dibujarLinea(adapterPinX + 30, picP5y, adapterPinX + 30, memP4y, colorGND);
        g.dibujarLinea(adapterPinX + 30, memP4y, memL_X, memP4y, colorGND);
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

        // Coordenadas Adaptador FTDI
        float adapterPinX = adapterX + 100;
        float adapterVCC_Y = adapterY - 60 + 20 + 3 * 18;
        float adapterGND_Y = adapterY - 60 + 20 + 5 * 18;

        float picR_X = picX + 70 + 20;
        float picL_X = picX - 70 - 20;
        float memR_X = memX + 70 + 20;
        float memL_X = memX - 70 - 20;
        
        // Pines PIC 16F628A
        float picP1y = chipsY - (18 * 45 / 4) + 35 + (0 * 45); 
        float picP2y = chipsY - (18 * 45 / 4) + 35 + (1 * 45); 
        float picP4y = chipsY - (18 * 45 / 4) + 35 + (3 * 45); 
        float picP5y = chipsY - (18 * 45 / 4) + 35 + (4 * 45); 
        float picP15y = chipsY - (18 * 45 / 4) + 35 + (3 * 45); 
        float picP14y = chipsY - (18 * 45 / 4) + 35 + (4 * 45); 

        // Pines EEPROM 25xx
        float memP1y = chipsY - (8 * 45 / 4) + 35 + (0 * 45); 
        float memP2y = chipsY - (8 * 45 / 4) + 35 + (1 * 45); 
        float memP3y = chipsY - (8 * 45 / 4) + 35 + (2 * 45); 
        float memP4y = chipsY - (8 * 45 / 4) + 35 + (3 * 45); 
        float memP8y = chipsY - (8 * 45 / 4) + 35 + (0 * 45); 
        float memP7y = chipsY - (8 * 45 / 4) + 35 + (1 * 45); 
        float memP6y = chipsY - (8 * 45 / 4) + 35 + (2 * 45); 
        float memP5y = chipsY - (8 * 45 / 4) + 35 + (3 * 45); 

        // VCC / GND
        g.dibujarLinea(adapterPinX, adapterVCC_Y, adapterPinX + 50, adapterVCC_Y, colorPower);
        g.dibujarLinea(adapterPinX + 50, picP14y, picR_X, picP14y, colorPower);
        g.dibujarLinea(adapterPinX + 50, picP14y, adapterPinX + 50, memP8y, colorPower);
        g.dibujarLinea(adapterPinX + 50, memP8y, memR_X, memP8y, colorPower);

        g.dibujarLinea(adapterPinX, adapterGND_Y, adapterPinX + 30, adapterGND_Y, colorGND);
        g.dibujarLinea(adapterPinX + 30, picP5y, picL_X, picP5y, colorGND);
        g.dibujarLinea(adapterPinX + 30, picP5y, adapterPinX + 30, memP4y, colorGND);
        g.dibujarLinea(adapterPinX + 30, memP4y, memL_X, memP4y, colorGND);

        // SPI Signal lines... (Mantenemos la logica anterior)
        g.dibujarLinea(picL_X, picP1y, 50, picP1y, colorWire);
        g.dibujarLinea(50, picP1y, 50, memP1y, colorWire);
        g.dibujarLinea(50, memP1y, memL_X, memP1y, colorWire);
        g.dibujarTexto("CS", 60, picP1y - 5, colorWire);
        
        g.dibujarLinea(picL_X, picP4y, 70, picP4y, colorWire);
        g.dibujarLinea(70, picP4y, 70, memP2y, colorWire);
        g.dibujarLinea(70, memP2y, memL_X, memP2y, colorWire);
        
        g.dibujarLinea(picR_X, picP15y, w - 50, picP15y, colorWire);
        g.dibujarLinea(w - 50, memP5y, memR_X, memP5y, colorWire);
        g.dibujarLinea(w - 50, picP15y, w - 50, memP5y, colorWire);

        g.dibujarLinea(picL_X, picP2y, 30, picP2y, colorWire);
        g.dibujarLinea(30, 800, w - 30, 800, colorWire);
        g.dibujarLinea(30, picP2y, 30, 800, colorWire);
        g.dibujarLinea(w - 30, 800, w - 30, memP6y, colorWire);
        g.dibujarLinea(w - 30, memP6y, memR_X, memP6y, colorWire);

        // WP and HOLD
        g.dibujarLinea(memL_X, memP3y, memL_X - 15, memP3y, colorAlert);
        g.dibujarLinea(memL_X - 15, memP3y, memL_X - 15, memP8y - 30, colorAlert);
        g.dibujarLinea(memR_X - 15, memP7y, memR_X, memP7y, colorAlert);
        g.dibujarLinea(memR_X - 15, memP7y, memR_X - 15, memP8y - 30, colorAlert);
        g.dibujarLinea(memL_X - 15, memP8y - 30, adapterPinX + 50, memP8y - 30, colorAlert);
    }
}


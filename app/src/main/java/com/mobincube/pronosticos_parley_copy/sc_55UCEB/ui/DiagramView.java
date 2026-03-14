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
            "18: RA1 (SCL)", "17: RA0 (SDA)", "16: OSC2", "15: RA6 (MOSI)", "14: VDD (VCC)", 
            "13: RB7", "12: RB6", "11: RB5", "10: RB4"
        };

        dibujarChipDIP(g, centerX, centerY, 18, "PIC 16F628A", left, right);
    }

    private void dibujarCajaInstrucciones(Graficos g, float x, float y, float w, String title, String[] lines) {
        int colorBox = Color.parseColor("#EEFFFFFF"); // Semi-transparent white
        int colorBorder = Color.parseColor("#D0D7DE");
        int colorTitle = Color.parseColor("#0969DA");
        int colorText = Color.parseColor("#24292F");
        
        float padding = 20;
        float lineH = 28;
        float boxH = padding * 2 + lines.length * lineH + 30;
        
        // Shadow
        g.dibujarRectangulo(x + 5, y + 5, w, boxH, Color.argb(30, 0, 0, 0));
        // Box
        g.dibujarRectangulo(x, y, w, boxH, colorBox);
        // Border
        g.getLapiz().setStyle(android.graphics.Paint.Style.STROKE);
        g.getLapiz().setStrokeWidth(2);
        g.dibujarRectangulo(x, y, w, boxH, colorBorder);
        g.getLapiz().setStyle(android.graphics.Paint.Style.FILL);
        
        // Title
        g.getLapiz().setTextSize(20);
        g.getLapiz().setFakeBoldText(true);
        g.dibujarTexto(title, x + padding, y + padding + 15, colorTitle);
        g.getLapiz().setFakeBoldText(false);
        
        // Lines
        g.getLapiz().setTextSize(18);
        for (int i = 0; i < lines.length; i++) {
            g.dibujarTexto(lines[i], x + padding, y + padding + 45 + (i * lineH), colorText);
        }
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
        int colorPCB = Color.parseColor("#F1F5F9"); // Light gray background
        int colorBorder = Color.parseColor("#94A3B8");
        int colorText = Color.parseColor("#334155");
        int colorPin = Color.parseColor("#64748B");

        // Main Body
        g.dibujarRectangulo(x - 80, y - 60, 160, 120, colorPCB);
        g.getLapiz().setStyle(android.graphics.Paint.Style.STROKE);
        g.getLapiz().setStrokeWidth(2);
        g.dibujarRectangulo(x - 80, y - 60, 160, 120, colorBorder);
        g.getLapiz().setStyle(android.graphics.Paint.Style.FILL);

        // Label
        g.getLapiz().setTextSize(18);
        g.getLapiz().setFakeBoldText(true);
        g.dibujarTexto("ADAPTADOR TTL", x - 65, y - 30, colorText);
        g.getLapiz().setFakeBoldText(false);

        // Header Pins
        String[] labels = {"DTR", "RXD", "TXD", "VCC", "CTS", "GND"};
        float pinX = x + 80;
        float startY = y - 45;
        float stepY = 18;

        for (int i = 0; i < 6; i++) {
            float py = startY + (i * stepY);
            // Pin line
            g.dibujarLinea(pinX, py, pinX + 20, py, colorPin);
            // Label
            g.getLapiz().setTextSize(14);
            g.dibujarTexto(labels[i], pinX - 35, py + 5, colorText);
        }
    }

    private void dibujarI2C(Graficos g) {
        float w = g.getAncho();
        float centerX = w / 2;
        
        int colorText = Color.parseColor("#24292F");
        int colorSDA = Color.parseColor("#D97706"); // Amber
        int colorSCL = Color.parseColor("#059669"); // Emerald
        int colorPower = Color.parseColor("#DC2626"); // Red
        int colorGND = Color.parseColor("#1F2937"); // Dark gray
        int colorResistor = Color.parseColor("#92400E");
        int colorComment = Color.parseColor("#4B5563");
        float adapterX = 150;
        float adapterY = 150;
        float picX = w / 4 + 50;
        float memX = 3 * w / 4 - 50;
        float chipsY = 800;
        float boxX = w - 450;
        float boxY = 120;

        // Comprobación de protocolo (Firmeare v3): SDA=RA0(P17), SCL=RA1(P18)
        
        // Componentes simplificados
        dibujarAdaptadorTTL(g, adapterX, adapterY);
        dibujarChipDIP(g, picX, chipsY, 18, "PIC 16F628A", null, null);
        dibujarChipDIP(g, memX, chipsY, 8, "EEPROM 24xx", null, null);

        // Instrucciones en caja (Zona segura)
        String[] inst = {
            "• RX (PIC 7) <-> TXD Adaptador",
            "• TX (PIC 8) <-> RXD Adaptador",
            "• SDA (PIC 17) <-> PIN 5 (MEM)",
            "• SCL (PIC 18) <-> PIN 6 (MEM)",
            "• Resistencias 4.7kΩ de SDA/SCL a VCC",
            "• VCC y GND comunes a todos"
        };
        dibujarCajaInstrucciones(g, boxX, boxY, 400, "PROTOCOLO I2C", inst);

        // Coordenadas calculadas
        float adapterPinX = adapterX + 80;
        float adapterTX_Y = adapterY - 45 + 2 * 18;
        float adapterRX_Y = adapterY - 45 + 1 * 18;
        float adapterVCC_Y = adapterY - 45 + 3 * 18;
        float adapterGND_Y = adapterY - 45 + 5 * 18;

        float picR_X = picX + 70 + 20;
        float picL_X = picX - 70 - 20;
        float memR_X = memX + 70 + 20;
        float memL_X = memX - 70 - 20;

        // Pines PIC (DIP-18)
        float startY_PIC = chipsY - 180;
        float picP17y = startY_PIC + 1 * 45; // Pin 17 is next to Pin 18 (index 16)
        float picP18y = startY_PIC + 0 * 45; // Pin 18 is at top right (index 17)
        float picP7y = startY_PIC + 6 * 45;  // Pin 7
        float picP8y = startY_PIC + 7 * 45;  // Pin 8
        float picP14y = startY_PIC + 4 * 45; // VDD (Pin 14)
        float picP5y = startY_PIC + 4 * 45;  // VSS (Pin 5) - Left side index 4

        // Pines MEM (DIP-8)
        float startY_MEM = chipsY - 67.5f;
        // Index 0: P1, 1: P2, 2: P3, 3: P4 (Left)
        // Index 4: P8, 5: P7, 6: P6, 7: P5 (Right)
        float memP8y = startY_MEM + 0 * 45; // VCC
        float memP6y = startY_MEM + 2 * 45; // SCL
        float memP5y_real = startY_MEM + 3 * 45; // SDA
        
        float memP4y = startY_MEM + 3 * 45; // VSS (Pin 4) - Left side index 3

        // 1. ALIMENTACIÓN (VCC/GND)
        g.dibujarLinea(adapterPinX + 20, adapterGND_Y, 50, adapterGND_Y, colorGND);
        g.dibujarLinea(50, adapterGND_Y, 50, chipsY + 150, colorGND);
        g.dibujarLinea(50, chipsY + 150, picL_X, picP5y, colorGND); // GND loop
        g.dibujarLinea(50, chipsY + 150, memL_X, memP4y, colorGND);

        g.dibujarLinea(adapterPinX + 20, adapterVCC_Y, 30, adapterVCC_Y, colorPower);
        g.dibujarLinea(30, adapterVCC_Y, 30, chipsY + 170, colorPower);
        g.dibujarLinea(30, chipsY + 170, picR_X, picP14y, colorPower);
        g.dibujarLinea(30, chipsY + 170, memR_X, memP8y, colorPower);

        // 2. COMUNICACIÓN USB (RX/TX)
        // TX Adaptador -> RX PIC (P7)
        g.dibujarLinea(adapterPinX + 20, adapterTX_Y, adapterPinX + 60, adapterTX_Y, Color.BLACK);
        g.dibujarLinea(adapterPinX + 60, adapterTX_Y, adapterPinX + 60, chipsY + 120, Color.BLACK);
        g.dibujarLinea(adapterPinX + 60, chipsY + 120, picL_X, picP7y, Color.BLACK);

        // RX Adaptador -> TX PIC (P8)
        g.dibujarLinea(adapterPinX + 20, adapterRX_Y, adapterPinX + 80, adapterRX_Y, Color.BLACK);
        g.dibujarLinea(adapterPinX + 80, adapterRX_Y, adapterPinX + 80, chipsY + 140, Color.BLACK);
        g.dibujarLinea(adapterPinX + 80, chipsY + 140, picL_X, picP8y, Color.BLACK);

        // 3. I2C SIGNALS
        // SCL (P18 -> P6)
        g.dibujarLinea(picR_X, picP18y, w - 50, picP18y, colorSCL);
        g.dibujarLinea(w - 50, picP18y, w - 50, memP6y, colorSCL);
        g.dibujarLinea(w - 50, memP6y, memR_X, memP6y, colorSCL);
        
        // SDA (P17 -> P5)
        g.dibujarLinea(picR_X, picP17y, w - 80, picP17y, colorSDA);
        g.dibujarLinea(w - 80, picP17y, w - 80, memP5y_real, colorSDA);
        g.dibujarLinea(w - 80, memP5y_real, memR_X, memP5y_real, colorSDA);

        // Pull-ups (Esquemáticos)
        float resY = chipsY - 120;
        g.dibujarRectangulo(w - 85, resY, 10, 30, colorResistor);
        g.dibujarRectangulo(w - 55, resY, 10, 30, colorResistor);
        g.dibujarLinea(w - 80, picP17y, w - 80, resY + 30, colorSDA);
        g.dibujarLinea(w - 80, resY, w - 80, adapterVCC_Y, colorPower);
        g.dibujarLinea(w - 50, picP18y, w - 50, resY + 30, colorSCL);
        g.dibujarLinea(w - 50, resY, w - 50, adapterVCC_Y, colorPower);
    }

    private void dibujarSPI(Graficos g) {
        float w = g.getAncho();
        float centerX = w / 2;
        
        int colorText = Color.parseColor("#334155");
        int colorCS = Color.parseColor("#EA580C");
        int colorSCK = Color.parseColor("#16A34A");
        int colorMOSI = Color.parseColor("#2563EB");
        int colorMISO = Color.parseColor("#9333EA");
        int colorPower = Color.parseColor("#DC2626");
        int colorGND = Color.parseColor("#1F2937");
        int colorAlert = Color.parseColor("#991B1B");

        g.getLapiz().setTextSize(24);
        g.getLapiz().setFakeBoldText(true);
        g.dibujarTexto("ESQUEMA SPI (PICMASTER)", centerX - 150, 60, colorText);
        g.getLapiz().setFakeBoldText(false);
        
        float adapterX = 150;
        float adapterY = 150;
        float picX = w / 4 + 50;
        float memX = 3 * w / 4 - 50;
        float chipsY = 800;
        float boxX = w - 450;
        float boxY = 120;

        // Protocolo (Firmware v3): CS=RA2(P1), SCK=RA3(P2), MISO=RA5(P4), MOSI=RA6(P15)

        dibujarAdaptadorTTL(g, adapterX, adapterY);
        dibujarChipDIP(g, picX, chipsY, 18, "PIC Master", null, null);
        dibujarChipDIP(g, memX, chipsY, 8, "EEPROM 25xx", null, null);

        String[] inst = {
            "• CS (PIC 1) <-> PIN 1 (MEM)",
            "• SCK (PIC 2) <-> PIN 6 (MEM)",
            "• MISO (PIC 4) <-> PIN 2 (MEM)",
            "• MOSI (PIC 15) <-> PIN 5 (MEM)",
            "• P3(WP) / P7(HOLD) a VCC",
            "• GND y VCC comunes"
        };
        dibujarCajaInstrucciones(g, boxX, boxY, 400, "PROTOCOLO SPI", inst);

        // Coordenadas
        float adapterPinX = adapterX + 80;
        float adapterTX_Y = adapterY - 45 + 2 * 18;
        float adapterRX_Y = adapterY - 45 + 1 * 18;
        float adapterVCC_Y = adapterY - 45 + 3 * 18;
        float adapterGND_Y = adapterY - 45 + 5 * 18;

        float picR_X = picX + 70 + 20;
        float picL_X = picX - 70 - 20;
        float memR_X = memX + 70 + 20;
        float memL_X = memX - 70 - 20;

        float startY_PIC = chipsY - 180;
        float picP1y = startY_PIC + 0 * 45; // Pin 1
        float picP2y = startY_PIC + 1 * 45; // Pin 2
        float picP4y = startY_PIC + 3 * 45; // Pin 4
        float picP15y = startY_PIC + 3 * 45; // Pin 15 (Right side index 3)
        float picP14y = startY_PIC + 4 * 45; // VDD (Pin 14)
        float picP5y = startY_PIC + 4 * 45;  // VSS (Pin 5)

        float startY_MEM = chipsY - 67.5f;
        float memP1y = startY_MEM + 0 * 45; // Pin 1
        float memP2y = startY_MEM + 1 * 45; // Pin 2
        float memP3y = startY_MEM + 2 * 45; // Pin 3 (WP)
        float memP4y = startY_MEM + 3 * 45; // Pin 4 (GND)
        float memP8y = startY_MEM + 0 * 45; // Pin 8 (VCC)
        float memP7y = startY_MEM + 1 * 45; // Pin 7 (HOLD)
        float memP6y = startY_MEM + 2 * 45; // Pin 6 (SCK)
        float memP5y = startY_MEM + 3 * 45; // Pin 5 (MOSI)

        // 1. ALIMENTACIÓN
        g.dibujarLinea(adapterPinX + 20, adapterGND_Y, 50, adapterGND_Y, colorGND);
        g.dibujarLinea(50, adapterGND_Y, 50, chipsY + 150, colorGND);
        g.dibujarLinea(50, chipsY + 150, picL_X, picP5y, colorGND);
        g.dibujarLinea(50, chipsY + 150, memL_X, memP4y, colorGND);

        g.dibujarLinea(adapterPinX + 20, adapterVCC_Y, 30, adapterVCC_Y, colorPower);
        g.dibujarLinea(30, adapterVCC_Y, 30, chipsY + 170, colorPower);
        g.dibujarLinea(30, chipsY + 170, picR_X, picP14y, colorPower);
        g.dibujarLinea(30, chipsY + 170, memR_X, memP8y, colorPower);

        // 2. SPI SIGNALS
        // CS (P1 -> P1)
        g.dibujarLinea(picL_X, picP1y, picL_X - 40, picP1y, colorCS);
        g.dibujarLinea(picL_X - 40, picP1y, picL_X - 40, chipsY + 100, colorCS);
        g.dibujarLinea(picL_X - 40, chipsY + 100, memL_X - 20, chipsY + 100, colorCS);
        g.dibujarLinea(memL_X - 20, chipsY + 100, memL_X - 20, memP1y, colorCS);
        g.dibujarLinea(memL_X - 20, memP1y, memL_X, memP1y, colorCS);

        // SCK (P2 -> P6)
        g.dibujarLinea(picL_X, picP2y, picL_X - 60, picP2y, colorSCK);
        g.dibujarLinea(picL_X - 60, picP2y, picL_X - 60, chipsY + 130, colorSCK);
        g.dibujarLinea(picL_X - 60, chipsY + 130, memR_X + 20, chipsY + 130, colorSCK);
        g.dibujarLinea(memR_X + 20, chipsY + 130, memR_X + 20, memP6y, colorSCK);
        g.dibujarLinea(memR_X + 20, memP6y, memR_X, memP6y, colorSCK);

        // MISO (P4 -> P2)
        g.dibujarLinea(picL_X, picP4y, picL_X - 80, picP4y, colorMISO);
        g.dibujarLinea(picL_X - 80, picP4y, picL_X - 80, chipsY + 150, colorMISO);
        g.dibujarLinea(picL_X - 80, chipsY + 150, memL_X - 40, chipsY + 150, colorMISO);
        g.dibujarLinea(memL_X - 40, chipsY + 150, memL_X - 40, memP2y, colorMISO);
        g.dibujarLinea(memL_X - 40, memP2y, memL_X, memP2y, colorMISO);

        // MOSI (P15 -> P5)
        g.dibujarLinea(picR_X, picP15y, memR_X - 10, picP15y, colorMOSI);
        g.dibujarLinea(memR_X - 10, picP15y, memR_X - 10, memP5y, colorMOSI);
        g.dibujarLinea(memR_X - 10, memP5y, memR_X, memP5y, colorMOSI);

        // WP/HOLD to VCC
        g.dibujarLinea(memL_X, memP3y, memL_X - 15, memP3y, colorAlert);
        g.dibujarLinea(memR_X, memP7y, memR_X + 15, memP7y, colorAlert);
        g.dibujarLinea(memL_X - 15, memP3y, memL_X - 15, memP1y - 30, colorAlert);
        g.dibujarLinea(memR_X + 15, memP7y, memR_X + 15, memP1y - 30, colorAlert);
        g.dibujarLinea(memL_X - 15, memP1y - 30, memR_X + 15, memP1y - 30, colorAlert);
        g.dibujarLinea(memR_X + 15, memP1y - 30, memR_X, memP8y, colorPower);
    }
}

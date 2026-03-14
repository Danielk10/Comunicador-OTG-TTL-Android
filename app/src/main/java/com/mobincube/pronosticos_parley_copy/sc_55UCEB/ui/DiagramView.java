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
        float w = 220;
        float h = 130;
        int colorPCB = Color.parseColor("#0051A8"); // Blue PCB (CH340 style)
        int colorPin = Color.parseColor("#E5E5E5"); // Silver/Nickel pins
        int colorGold = Color.parseColor("#D4AF37");
        
        // Shadow
        g.dibujarRectangulo(x - w/2 + 6, y - h/2 + 6, w, h, Color.argb(40, 0, 0, 0));
        
        // PCB Body
        g.dibujarRectangulo(x - w/2, y - h/2, w, h, colorPCB);
        
        // USB Connector (Silver)
        g.dibujarRectangulo(x - w/2 - 30, y - 20, 35, 40, Color.parseColor("#B0B0B0"));
        g.dibujarRectangulo(x - w/2 - 25, y - 15, 25, 30, Color.parseColor("#808080"));
        
        // CH340G Chip
        g.dibujarRectangulo(x - 40, y - 30, 80, 50, Color.parseColor("#1A1A1A"));
        g.getLapiz().setTextSize(14);
        g.dibujarTexto("CH340G", x - 25, y + 5, Color.WHITE);
        
        // Oscillator (Crystalline Silver)
        g.dibujarRectangulo(x + 20, y - 50, 40, 20, Color.parseColor("#C0C0C0"));
        
        // Header Pins (6 pins)
        String[] pins = {"DTR", "RXD", "TXD", "VCC", "CTS", "GND"};
        int[] pinColors = {Color.GRAY, Color.YELLOW, Color.YELLOW, Color.RED, Color.GRAY, Color.BLACK};
        float pinX = x + w/2;
        float startY = y - h/2 + 20;
        
        for (int i = 0; i < 6; i++) {
            float py = startY + (i * 18);
            // Pin base (gold/yellow)
            g.dibujarRectangulo(pinX - 5, py - 6, 10, 12, colorGold);
            // Pin extension
            g.dibujarRectangulo(pinX, py - 4, 25, 8, colorPin);
            
            g.getLapiz().setTextSize(14);
            g.dibujarTexto(pins[i], pinX - 45, py + 5, Color.WHITE);
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

        g.getLapiz().setTextSize(28);
        g.getLapiz().setFakeBoldText(true);
        g.dibujarTexto("CONEXIÓN I2C (24xx EEPROM)", centerX - 200, 60, colorText);
        g.getLapiz().setFakeBoldText(false);
        
        float adapterX = 150;
        float adapterY = 180;
        float picX = 250;
        float memX = w - 250;
        float chipsY = 850;
        float boxX = w - 450;
        float boxY = 150;

        // Componentes
        dibujarAdaptadorTTL(g, adapterX, adapterY);
        dibujarChipDIP(g, picX, chipsY, 18, "PIC 16F628A", null, null);
        
        // Memoria con Pin 1 marcado
        dibujarChipDIP(g, memX, chipsY, 8, "EEPROM 24xx", null, null);
        g.getLapiz().setColor(Color.WHITE);
        g.getCanvas().drawCircle(memX - 55, chipsY - 80, 4, g.getLapiz()); // Pin 1 dot

        // Instrucciones profesionales en caja (Top Right area)
        String[] inst = {
            "• RX (PIN 7) <-> TXD Adaptador",
            "• TX (PIN 8) <-> RXD Adaptador",
            "• SDA (PIN 17) <-> PIN 5 (MEM)",
            "• SCL (PIN 18) <-> PIN 6 (MEM)",
            "• Pull-ups 4.7kΩ de SDA/SCL a VCC",
            "• GND y VCC comunes"
        };
        dibujarCajaInstrucciones(g, boxX, boxY, 400, "CONEXIÓN I2C", inst);

        // Coordenadas calculadas
        float adapterPinX = adapterX + 110;
        float adapterVCC_Y = adapterY - 65 + 20 + 3 * 18;
        float adapterTX_Y = adapterY - 65 + 20 + 2 * 18;
        float adapterRX_Y = adapterY - 65 + 20 + 1 * 18;
        float adapterGND_Y = adapterY - 65 + 20 + 5 * 18;

        float picR_X = picX + 70 + 20;
        float picL_X = picX - 70 - 20;
        float memR_X = memX + 70 + 20;
        float memL_X = memX - 70 - 20;
        
        // Coordenadas Pines PIC
        float picP18y = chipsY - (18 * 45 / 4) + 35 + (0 * 45); 
        float picP17y = chipsY - (18 * 45 / 4) + 35 + (1 * 45); 
        float picP14y = chipsY - (18 * 45 / 4) + 35 + (4 * 45); 
        float picP5y = chipsY - (18 * 45 / 4) + 35 + (4 * 45); // VSS Izquierda
        float picP7y = chipsY - (18 * 45 / 4) + 35 + (6 * 45); // RX Izquierda
        float picP8y = chipsY - (18 * 45 / 4) + 35 + (7 * 45); // TX Izquierda
        
        // Coordenadas Pines EEPROM
        float memP8y = chipsY - (8 * 45 / 4) + 35 + (0 * 45); 
        float memP6y = chipsY - (8 * 45 / 4) + 35 + (2 * 45); 
        float memP5y = chipsY - (8 * 45 / 4) + 35 + (3 * 45); 
        float memP4y = chipsY - (8 * 45 / 4) + 35 + (3 * 45); 

        // 1. ALIMENTACIÓN (VCC)
        g.dibujarLinea(adapterPinX + 25, adapterVCC_Y, adapterPinX + 70, adapterVCC_Y, colorPower);
        g.dibujarLinea(adapterPinX + 70, adapterVCC_Y, adapterPinX + 70, picP14y, colorPower);
        g.dibujarLinea(adapterPinX + 70, picP14y, picR_X, picP14y, colorPower);
        g.dibujarLinea(adapterPinX + 70, picP14y, adapterPinX + 70, memP8y, colorPower);
        g.dibujarLinea(adapterPinX + 70, memP8y, memR_X, memP8y, colorPower);
        g.dibujarTexto("VCC", adapterPinX + 75, adapterVCC_Y - 5, colorPower);

        // 2. TIERRA (GND)
        g.dibujarLinea(adapterPinX + 25, adapterGND_Y, adapterPinX + 45, adapterGND_Y, colorGND);
        g.dibujarLinea(adapterPinX + 45, adapterGND_Y, adapterPinX + 45, picP5y, colorGND);
        g.dibujarLinea(adapterPinX + 45, picP5y, picL_X, picP5y, colorGND);
        g.dibujarLinea(adapterPinX + 45, picP5y, adapterPinX + 45, memP4y, colorGND);
        g.dibujarLinea(adapterPinX + 45, memP4y, memL_X, memP4y, colorGND);
        g.dibujarTexto("GND", adapterPinX + 48, adapterGND_Y + 15, colorGND);

        // 3. COMUNICACIÓN USB (TX/RX)
        // TXD Adaptador -> RX PIC (P7)
        float routeX = adapterPinX + 50;
        g.dibujarLinea(adapterPinX + 25, adapterTX_Y, routeX, adapterTX_Y, Color.BLACK);
        g.dibujarLinea(routeX, adapterTX_Y, routeX, chipsY + 120, Color.BLACK); // Go below everything
        g.dibujarLinea(routeX, chipsY + 120, picL_X - 35, chipsY + 120, Color.BLACK);
        g.dibujarLinea(picL_X - 35, chipsY + 120, picL_X - 35, picP7y, Color.BLACK);
        g.dibujarLinea(picL_X - 35, picP7y, picL_X, picP7y, Color.BLACK);
        
        // RXD Adaptador -> TX PIC (P8)
        float routeX2 = adapterPinX + 35;
        g.dibujarLinea(adapterPinX + 25, adapterRX_Y, routeX2, adapterRX_Y, Color.BLACK);
        g.dibujarLinea(routeX2, adapterRX_Y, routeX2, chipsY + 140, Color.BLACK); // Go below everything
        g.dibujarLinea(routeX2, chipsY + 140, picL_X - 15, chipsY + 140, Color.BLACK);
        g.dibujarLinea(picL_X - 15, chipsY + 140, picL_X - 15, picP8y, Color.BLACK);
        g.dibujarLinea(picL_X - 15, picP8y, picL_X, picP8y, Color.BLACK);

        // 4. I2C BUS (SDA/SCL)
        // SDA: PIC P17 -> MEM P5
        float busX = centerX + 50;
        g.dibujarLinea(picR_X, picP17y, busX, picP17y, colorSDA);
        g.dibujarLinea(busX, picP17y, busX, memP5y, colorSDA);
        g.dibujarLinea(busX, memP5y, memR_X, memP5y, colorSDA);
        // SCL: PIC P18 -> MEM P6
        float busX2 = centerX + 80;
        g.dibujarLinea(picR_X, picP18y, busX2, picP18y, colorSCL);
        g.dibujarLinea(busX2, picP18y, busX2, memP6y, colorSCL);
        g.dibujarLinea(busX2, memP6y, memR_X, memP6y, colorSCL);

        // 5. PULL-UPS (Realistas) - Conectando al bus de VCC (adapterPinX + 70)
        float resX1 = busX - 10;
        float resX2 = busX2 - 10;
        float resY = chipsY - 120;
        
        // SDA Resistor
        g.dibujarRectangulo(resX1 - 5, resY, 10, 30, colorResistor);
        g.dibujarLinea(resX1, picP17y, resX1, resY + 30, colorSDA);
        g.dibujarLinea(resX1, resY, resX1, adapterVCC_Y, colorPower);
        
        // SCL Resistor
        g.dibujarRectangulo(resX2 - 5, resY, 10, 30, colorResistor);
        g.dibujarLinea(resX2, picP18y, resX2, resY + 30, colorSCL);
        g.dibujarLinea(resX2, resY, resX2, adapterVCC_Y, colorPower);
        
        g.getLapiz().setTextSize(14);
        g.dibujarTexto("4.7k", resX1 + 10, resY + 20, colorComment);
        g.dibujarTexto("4.7k", resX2 + 10, resY + 20, colorComment);
    }

    private void dibujarSPI(Graficos g) {
        float w = g.getAncho();
        float centerX = w / 2;
        
        int colorText = Color.parseColor("#24292F");
        int colorCS = Color.parseColor("#EA580C");   // Orange
        int colorSCK = Color.parseColor("#16A34A");  // Green
        int colorMOSI = Color.parseColor("#2563EB"); // Blue
        int colorMISO = Color.parseColor("#9333EA"); // Purple
        int colorPower = Color.parseColor("#DC2626"); // Red
        int colorGND = Color.parseColor("#1F2937");   // Dark gray
        int colorAlert = Color.parseColor("#991B1B"); // Dark red
        int colorComment = Color.parseColor("#4B5563");

        g.getLapiz().setTextSize(28);
        g.getLapiz().setFakeBoldText(true);
        g.dibujarTexto("CONEXIÓN SPI (25xx EEPROM)", centerX - 200, 60, colorText);
        g.getLapiz().setFakeBoldText(false);
        
        float adapterX = 150;
        float adapterY = 180;
        float picX = 250;
        float memX = w - 250;
        float chipsY = 850;
        float boxX = w - 470;
        float boxY = 150;

        // Componentes
        dibujarAdaptadorTTL(g, adapterX, adapterY);
        dibujarChipDIP(g, picX, chipsY, 18, "PIC 16F628A", null, null);
        
        // Memoria con Pin 1 marcado
        dibujarChipDIP(g, memX, chipsY, 8, "EEPROM 25xx", null, null);
        g.getLapiz().setColor(Color.WHITE);
        g.getCanvas().drawCircle(memX - 55, chipsY - 80, 4, g.getLapiz()); // Pin 1 dot

        // Instrucciones profesionales en caja
        String[] inst = {
            "• RX (PIN 7) <-> TXD Adaptador",
            "• TX (PIN 8) <-> RXD Adaptador",
            "• CS (P1) <-> P1 | SCK (P2) <-> P2",
            "• MOSI (P15) <-> P5 | MISO (P4) <-> P2",
            "• ⚠ P3 (WP) y P7 (HOLD) a VCC",
            "• VCC/GND Comunes a PIC y MEM"
        };
        dibujarCajaInstrucciones(g, boxX, boxY, 440, "CONEXIÓN SPI", inst);

        // Coordenadas calculadas
        float adapterPinX = adapterX + 110;
        float adapterTX_Y = adapterY - 65 + 20 + 2 * 18;
        float adapterRX_Y = adapterY - 65 + 20 + 1 * 18;
        float adapterVCC_Y = adapterY - 65 + 20 + 3 * 18;
        float adapterGND_Y = adapterY - 65 + 20 + 5 * 18;

        float picR_X = picX + 70 + 20;
        float picL_X = picX - 70 - 20;
        float memR_X = memX + 70 + 20;
        float memL_X = memX - 70 - 20;
        
        // Coordenadas PIC
        float picP1y = chipsY - (18 * 45 / 4) + 35 + (0 * 45); 
        float picP2y = chipsY - (18 * 45 / 4) + 35 + (1 * 45); 
        float picP4y = chipsY - (18 * 45 / 4) + 35 + (3 * 45); 
        float picP5y = chipsY - (18 * 45 / 4) + 35 + (4 * 45); 
        float picP15y = chipsY - (18 * 45 / 4) + 35 + (3 * 45); 
        float picP14y = chipsY - (18 * 45 / 4) + 35 + (4 * 45); 

        // Coordenadas EEPROM
        float memP1y = chipsY - (8 * 45 / 4) + 35 + (0 * 45); 
        float memP2y = chipsY - (8 * 45 / 4) + 35 + (1 * 45); 
        float memP3y = chipsY - (8 * 45 / 4) + 35 + (2 * 45); 
        float memP4y = chipsY - (8 * 45 / 4) + 35 + (3 * 45); 
        float memP8y = chipsY - (8 * 45 / 4) + 35 + (0 * 45); 
        float memP7y = chipsY - (8 * 45 / 4) + 35 + (1 * 45); 
        float memP6y = chipsY - (8 * 45 / 4) + 35 + (2 * 45); 
        float memP5y = chipsY - (8 * 45 / 4) + 35 + (3 * 45); 

        // 1. ALIMENTACIÓN
        g.dibujarLinea(adapterPinX + 25, adapterVCC_Y, adapterPinX + 70, adapterVCC_Y, colorPower);
        g.dibujarLinea(adapterPinX + 70, picP14y, picR_X, picP14y, colorPower);
        g.dibujarLinea(adapterPinX + 70, picP14y, adapterPinX + 70, memP8y, colorPower);
        g.dibujarLinea(adapterPinX + 70, memP8y, memR_X, memP8y, colorPower);

        g.dibujarLinea(adapterPinX + 25, adapterGND_Y, adapterPinX + 45, adapterGND_Y, colorGND);
        g.dibujarLinea(adapterPinX + 45, picP5y, picL_X, picP5y, colorGND);
        g.dibujarLinea(adapterPinX + 45, picP5y, adapterPinX + 45, memP4y, colorGND);
        g.dibujarLinea(adapterPinX + 45, memP4y, memL_X, memP4y, colorGND);

        // 2. SPI SIGNALS
        // CS: PIC P1 -> MEM P1
        g.dibujarLinea(picL_X, picP1y, 40, picP1y, colorCS);
        g.dibujarLinea(40, picP1y, 40, memP1y, colorCS);
        g.dibujarLinea(40, memP1y, memL_X, memP1y, colorCS);
        g.dibujarTexto("CS", 50, picP1y - 5, colorCS);
        
        // SCK: PIC P2 -> MEM P6 (Moving bus to very outer right to avoid box)
        float busX_SCK = w - 15;
        g.dibujarLinea(picL_X, picP2y, 25, picP2y, colorSCK);
        g.dibujarLinea(25, picP2y, 25, chipsY + 120, colorSCK);
        g.dibujarLinea(25, chipsY + 120, busX_SCK, chipsY + 120, colorSCK);
        g.dibujarLinea(busX_SCK, chipsY + 120, busX_SCK, memP6y, colorSCK);
        g.dibujarLinea(busX_SCK, memP6y, memR_X, memP6y, colorSCK);
        g.dibujarTexto("SCK", 30, chipsY + 115, colorSCK);

        // MOSI: PIC P15 -> MEM P5
        g.dibujarLinea(picR_X, picP15y, memR_X - 15, picP15y, colorMOSI);
        g.dibujarLinea(memR_X - 15, picP15y, memR_X - 15, memP5y, colorMOSI);
        g.dibujarLinea(memR_X - 15, memP5y, memR_X, memP5y, colorMOSI);
        g.dibujarTexto("MOSI", memR_X + 25, memP5y - 5, colorMOSI);

        // MISO: PIC P4 -> MEM P2
        g.dibujarLinea(picL_X, picP4y, 60, picP4y, colorMISO);
        g.dibujarLinea(60, picP4y, 60, memP2y, colorMISO);
        g.dibujarLinea(60, memP2y, memL_X, memP2y, colorMISO);
        g.dibujarTexto("MISO", 70, picP4y - 5, colorMISO);

        // 3. COMUNICACIÓN USB (TX/RX) - Added for SPI too
        float rtX = adapterPinX + 50;
        g.dibujarLinea(adapterPinX + 25, adapterTX_Y, rtX, adapterTX_Y, Color.BLACK);
        g.dibujarLinea(rtX, adapterTX_Y, rtX, chipsY + 140, Color.BLACK);
        g.dibujarLinea(rtX, chipsY + 140, picL_X - 35, chipsY + 140, Color.BLACK);
        g.dibujarLinea(picL_X - 35, chipsY + 140, picL_X - 35, chipsY - (18 * 45 / 4) + 35 + (6 * 45), Color.BLACK); // P7
        g.dibujarLinea(picL_X - 35, chipsY - (18 * 45 / 4) + 35 + (6 * 45), picL_X, chipsY - (18 * 45 / 4) + 35 + (6 * 45), Color.BLACK);

        float rtX2 = adapterPinX + 35;
        g.dibujarLinea(adapterPinX + 25, adapterRX_Y, rtX2, adapterRX_Y, Color.BLACK);
        g.dibujarLinea(rtX2, adapterRX_Y, rtX2, chipsY + 160, Color.BLACK);
        g.dibujarLinea(rtX2, chipsY + 160, picL_X - 15, chipsY + 160, Color.BLACK);
        g.dibujarLinea(picL_X - 15, chipsY + 160, picL_X - 15, chipsY - (18 * 45 / 4) + 35 + (7 * 45), Color.BLACK); // P8
        g.dibujarLinea(picL_X - 15, chipsY - (18 * 45 / 4) + 35 + (7 * 45), picL_X, chipsY - (18 * 45 / 4) + 35 + (7 * 45), Color.BLACK);

        // 3. WP and HOLD (Tie to VCC)
        g.dibujarLinea(memL_X, memP3y, memL_X - 15, memP3y, colorAlert);
        g.dibujarLinea(memL_X - 15, memP3y, memL_X - 15, memP8y - 30, colorAlert);
        g.dibujarLinea(memR_X - 15, memP7y, memR_X, memP7y, colorAlert);
        g.dibujarLinea(memR_X - 15, memP7y, memR_X - 15, memP8y - 30, colorAlert);
        g.dibujarLinea(memL_X - 15, memP8y - 30, adapterPinX + 70, memP8y - 30, colorAlert);
        g.dibujarTexto("WP/HOLD -> VCC", memX - 60, memP8y - 40, colorAlert);
    }
}


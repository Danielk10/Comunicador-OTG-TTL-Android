package com.mobincube.pronosticos_parley_copy.sc_55UCEB.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

/**
 * DiagramView v3 — Dibuja directamente sobre Canvas de Android.
 *
 * Correcciones respecto a la versión anterior:
 *  • Eliminada dependencia de Graficos2D / Textura2D (usaba doble buffer innecesario).
 *  • Pin 16 corregido: era "OSC2" → ahora "RA7"  (oscilador INTRC libera RA6/RA7).
 *  • Pin 10 correcto: "RB4  LED ★" (indicador de estado).
 *  • Diagrama I2C: etiquetas SDA/SCL en pines correctos (SDA=RA0/P17, SCL=RA1/P18).
 *  • Diagrama SPI: etiquetas CS/SCK/MISO/MOSI correctas según firmware v3.
 *  • Diagrama SPI: /WP y /HOLD claramente indicados con conexión a VCC.
 *  • Colores diferenciados por señal para facilitar identificación visual.
 *
 * Pinout PIC16F628A DIP-18 (firmware PICMEM v3):
 *   Pin  1 = RA2   → CS   (SPI, salida)
 *   Pin  2 = RA3   → SCK  (SPI, salida)
 *   Pin  3 = RA4   → libre
 *   Pin  4 = RA5   → MISO (SPI, entrada) [MCLRE=0 → RA5 es I/O]
 *   Pin  5 = VSS   → GND
 *   Pin  6 = RB0   → libre
 *   Pin  7 = RB1   → RX UART (entrada)
 *   Pin  8 = RB2   → TX UART (salida)
 *   Pin  9 = RB3   → libre
 *   Pin 10 = RB4   → LED indicador (salida)
 *   Pin 11 = RB5   → libre
 *   Pin 12 = RB6   → libre
 *   Pin 13 = RB7   → libre
 *   Pin 14 = VDD   → +5V
 *   Pin 15 = RA6   → MOSI (SPI, salida)  [INTRC → RA6 es I/O]
 *   Pin 16 = RA7   → libre               [INTRC → RA7 es I/O]
 *   Pin 17 = RA0   → SDA  (I2C, open-drain)
 *   Pin 18 = RA1   → SCL  (I2C, open-drain)
 */
public class DiagramView extends View {

    // ── Tipo de diagrama ──────────────────────────────────────────────────
    private int type = 0; // 0=PIC Pinout, 1=I2C, 2=SPI

    // ── Colores de señal ──────────────────────────────────────────────────
    static final int C_SDA   = 0xFFFF8C00; // Naranja  – SDA I2C
    static final int C_SCL   = 0xFF00C853; // Verde    – SCL I2C
    static final int C_CS    = 0xFFFF6D00; // Naranja oscuro – /CS SPI
    static final int C_SCK   = 0xFF00BFA5; // Verde azulado  – SCK SPI
    static final int C_MOSI  = 0xFF2979FF; // Azul     – MOSI SPI
    static final int C_MISO  = 0xFFAA00FF; // Morado   – MISO SPI
    static final int C_VCC   = 0xFFF44336; // Rojo     – VCC/+5V
    static final int C_GND   = 0xFF546E7A; // Gris-azul– GND
    static final int C_UART  = 0xFFFFEB3B; // Amarillo – UART RX/TX
    static final int C_LED   = 0xFFFFD700; // Dorado   – LED indicador
    static final int C_NC    = 0xFF444444; // Gris oscuro – sin función asignada
    static final int C_TITLE = 0xFF58A6FF; // Azul claro  – títulos
    static final int C_TEXT  = 0xFFCDD9E5; // Blanco humo – texto general
    static final int C_SUB   = 0xFF8B949E; // Gris       – subtítulos
    static final int C_CHIP  = 0xFF161B22; // Casi negro – cuerpo del chip
    static final int C_BORD  = 0xFF4A90D9; // Azul       – borde del chip
    static final int C_PIN   = 0xFFA0A8B0; // Metal      – pin metálico
    static final int C_ROW1  = 0xFF161B22; // Filas alternas
    static final int C_ROW2  = 0xFF0D1117;
    static final int C_HDRB  = 0xFF1C2128; // Fondo encabezado de sección

    // ── Constructor ───────────────────────────────────────────────────────
    public DiagramView(Context context) {
        super(context);
    }

    public DiagramView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    // ── API pública ───────────────────────────────────────────────────────
    public void setType(int type) {
        this.type = type;
        invalidate();
    }

    // ── Medida mínima de altura ───────────────────────────────────────────
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int minH;
        switch (type) {
            case 1:  minH = (int) dp(720); break;
            case 2:  minH = (int) dp(760); break;
            default: minH = (int) dp(620); break;
        }
        int h = Math.max(getMeasuredHeight(), minH);
        setMeasuredDimension(getMeasuredWidth(), h);
    }

    // ── onDraw ─────────────────────────────────────────────────────────────
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(0xFF0D1117); // fondo oscuro uniforme
        switch (type) {
            case 0: drawPinout(canvas); break;
            case 1: drawI2C(canvas);   break;
            case 2: drawSPI(canvas);   break;
        }
    }

    // ======================================================================
    // TIPO 0 — PIC16F628A DIP-18 Pinout
    // ======================================================================
    private void drawPinout(Canvas canvas) {
        float W = getWidth();
        float cx = W / 2f;
        float y = dp(10);

        // Título
        drawCenteredText(canvas, "PIC16F628A  —  DIP-18 Pinout", cx, y + dp(22),
                makeText(17, C_TITLE, true));
        drawCenteredText(canvas, "Firmware PICMEM v3  ·  INTRC 4 MHz  ·  UART 9600 baud",
                cx, y + dp(40), makeText(9, C_SUB, false));

        // ── Cuerpo del chip ──────────────────────────────────────────────
        float chipW  = dp(68);
        float chipH  = dp(240);
        float chipT  = y + dp(55);
        float chipL  = cx - chipW / 2f;
        float chipR  = cx + chipW / 2f;

        canvas.drawRoundRect(new RectF(chipL, chipT, chipR, chipT + chipH),
                dp(5), dp(5), fill(C_CHIP));
        canvas.drawRoundRect(new RectF(chipL, chipT, chipR, chipT + chipH),
                dp(5), dp(5), stroke(C_BORD, 1.5f));
        // Muesca superior
        canvas.drawArc(new RectF(cx - dp(10), chipT - dp(6), cx + dp(10), chipT + dp(6)),
                0, -180, false, stroke(C_BORD, 1.5f));
        // Etiqueta chip
        drawCenteredText(canvas, "PIC16F628A", cx, chipT + chipH * 0.44f,
                makeText(13, C_TEXT, true));
        drawCenteredText(canvas, "DIP-18", cx, chipT + chipH * 0.57f,
                makeText(9, C_TITLE, false));

        // ── Pines ────────────────────────────────────────────────────────
        // Lado izquierdo: pines 1-9 (arriba→abajo)
        String[] lLabel = {
            "RA2  CS-SPI",
            "RA3  SCK-SPI",
            "RA4",
            "RA5  MISO-SPI",
            "VSS  GND",
            "RB0",
            "RB1  RX-UART",
            "RB2  TX-UART",
            "RB3"
        };
        int[] lColor = {C_CS, C_SCK, C_NC, C_MISO, C_GND, C_NC, C_UART, C_UART, C_NC};
        int[] lNum   = {1, 2, 3, 4, 5, 6, 7, 8, 9};

        // Lado derecho: pines 18-10 (arriba→abajo)
        String[] rLabel = {
            "SCL-I2C  RA1",
            "SDA-I2C  RA0",
            "RA7",
            "MOSI-SPI  RA6",
            "+5V  VDD",
            "RB7",
            "RB6",
            "RB5",
            "LED★  RB4"
        };
        int[] rColor = {C_SCL, C_SDA, C_NC, C_MOSI, C_VCC, C_NC, C_NC, C_NC, C_LED};
        int[] rNum   = {18, 17, 16, 15, 14, 13, 12, 11, 10};

        float pinSp  = chipH / 9f;
        float wLen   = dp(20);
        float tSz    = Math.min(sp(8f), pinSp * 0.38f);
        Paint tPaint = makeText(0, C_TEXT, false);
        tPaint.setTextSize(tSz);
        tPaint.setTypeface(Typeface.MONOSPACE);
        Paint numPaint = makeText(0, 0xFFCCCCCC, true);
        numPaint.setTextSize(tSz * 0.85f);

        for (int i = 0; i < 9; i++) {
            float pinY = chipT + pinSp * (i + 0.5f);

            // ── izquierda
            canvas.drawLine(chipL - wLen, pinY, chipL, pinY, stroke(lColor[i], 2.5f));
            canvas.drawRect(chipL - wLen - dp(5), pinY - dp(3),
                    chipL - wLen, pinY + dp(3), fill(C_PIN));
            numPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(String.valueOf(lNum[i]),
                    chipL - wLen - dp(8), pinY + tSz * 0.37f, numPaint);
            tPaint.setColor(lColor[i]);
            tPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(lLabel[i],
                    chipL - wLen - dp(20), pinY + tSz * 0.37f, tPaint);

            // ── derecha
            canvas.drawLine(chipR, pinY, chipR + wLen, pinY, stroke(rColor[i], 2.5f));
            canvas.drawRect(chipR + wLen, pinY - dp(3),
                    chipR + wLen + dp(5), pinY + dp(3), fill(C_PIN));
            numPaint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(String.valueOf(rNum[i]),
                    chipR + wLen + dp(8), pinY + tSz * 0.37f, numPaint);
            tPaint.setColor(rColor[i]);
            tPaint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(rLabel[i],
                    chipR + wLen + dp(20), pinY + tSz * 0.37f, tPaint);
        }

        // ── Leyenda de colores
        float legY = chipT + chipH + dp(18);
        drawLegend(canvas, dp(12), legY, W - dp(12));
    }

    // ======================================================================
    // TIPO 1 — Conexiones I2C
    // ======================================================================
    private void drawI2C(Canvas canvas) {
        float W  = getWidth();
        float cx = W / 2f;
        float y  = dp(10);

        drawCenteredText(canvas, "Conexiones I2C", cx, y + dp(22),
                makeText(16, C_TITLE, true));
        drawCenteredText(canvas, "PIC16F628A ↔ EEPROM 24Cxx  (bus ~50 kHz bit-banging)",
                cx, y + dp(40), makeText(9, C_SUB, false));
        y += dp(50);

        // ── Tabla de conexiones ──────────────────────────────────────────
        y = drawSectionHeader(canvas, dp(10), y, W - dp(10), "Tabla de conexiones");
        Object[][] conns = {
            {"Adaptador TX",      "→  PIC RB1 / Pin 7  (RX)",  C_UART},
            {"Adaptador RX",      "←  PIC RB2 / Pin 8  (TX)",  C_UART},
            {"PIC RA0 / Pin 17",  "↔  SDA → Pin 5 EEPROM",     C_SDA },
            {"PIC RA1 / Pin 18",  "↔  SCL → Pin 6 EEPROM",     C_SCL },
            {"PIC VDD / Pin 14",  "→  VCC → Pin 8 EEPROM",     C_VCC },
            {"PIC VSS / Pin 5",   "→  GND → Pin 4 EEPROM",     C_GND },
            {"4.7 kΩ Pull-up",    "SDA → VCC  (OBLIGATORIO)",  C_SDA },
            {"4.7 kΩ Pull-up",    "SCL → VCC  (OBLIGATORIO)",  C_SCL },
            {"EEPROM Pin 1,2,3",  "A0, A1, A2 → GND",          C_GND },
            {"EEPROM Pin 7",      "WP → GND   (escritura ON)",  C_GND },
        };
        y = drawConnTable(canvas, dp(10), y, W - dp(10), conns);

        // ── Chip DIP-8 EEPROM 24Cxx ──────────────────────────────────────
        y += dp(12);
        y = drawSectionHeader(canvas, dp(10), y, W - dp(10),
                "EEPROM 24Cxx — DIP-8 / SOP-8");
        y = drawChip8(canvas, W, y,
                new String[]{"A0  (1)", "A1  (2)", "A2  (3)", "GND (4)"},
                new String[]{"VCC (8)", "WP  (7)", "SCL (6)", "SDA (5)"},
                new int[]   {C_NC,      C_NC,      C_NC,      C_GND    },
                new int[]   {C_VCC,     C_GND,     C_SCL,     C_SDA    },
                "24Cxx EEPROM");

        // ── Notas ─────────────────────────────────────────────────────────
        y += dp(12);
        y = drawSectionHeader(canvas, dp(10), y, W - dp(10), "⚠  Notas importantes");
        String[] notes = {
            "• Sin Pull-ups 4.7 kΩ en SDA/SCL el bus I2C NO funciona",
            "• A0-A1-A2 a GND → dirección base del chip = 0xA0",
            "• TX del PIC → RX del adaptador (cables CRUZADOS)",
            "• Verificar voltaje de la EEPROM (3.3 V ó 5 V según modelo)",
            "• 24C01-24C16: addr_len=1   |   24C32-24C512: addr_len=2",
        };
        drawNotes(canvas, dp(10), y, notes);
    }

    // ======================================================================
    // TIPO 2 — Conexiones SPI
    // ======================================================================
    private void drawSPI(Canvas canvas) {
        float W  = getWidth();
        float cx = W / 2f;
        float y  = dp(10);

        drawCenteredText(canvas, "Conexiones SPI", cx, y + dp(22),
                makeText(16, C_TITLE, true));
        drawCenteredText(canvas, "PIC16F628A ↔ Flash W25Qxx / EEPROM 25LCxx  (Modo 0, ~100 kHz)",
                cx, y + dp(40), makeText(9, C_SUB, false));
        y += dp(50);

        // ── Tabla de conexiones ──────────────────────────────────────────
        y = drawSectionHeader(canvas, dp(10), y, W - dp(10), "Tabla de conexiones");
        Object[][] conns = {
            {"Adaptador TX",      "→  PIC RB1 / Pin 7  (RX)",        C_UART},
            {"Adaptador RX",      "←  PIC RB2 / Pin 8  (TX)",        C_UART},
            {"PIC RA2 / Pin 1",   "→  /CS   → Pin 1 Flash  (activo bajo)", C_CS  },
            {"PIC RA3 / Pin 2",   "→  CLK   → Pin 6 Flash",          C_SCK },
            {"PIC RA5 / Pin 4",   "←  DO    ← Pin 2 Flash  (MISO)",  C_MISO},
            {"PIC RA6 / Pin 15",  "→  DI    → Pin 5 Flash  (MOSI)",  C_MOSI},
            {"PIC VDD / Pin 14",  "→  VCC   → Pin 8 Flash",          C_VCC },
            {"PIC VSS / Pin 5",   "→  GND   → Pin 4 Flash",          C_GND },
            {"VCC",               "→  /WP   → Pin 3 Flash  (desproteger)", C_VCC },
            {"VCC",               "→  /HOLD → Pin 7 Flash  (habilitar)",   C_VCC },
        };
        y = drawConnTable(canvas, dp(10), y, W - dp(10), conns);

        // ── Chip SOIC-8 Flash / EEPROM SPI ───────────────────────────────
        y += dp(12);
        y = drawSectionHeader(canvas, dp(10), y, W - dp(10),
                "Flash SPI W25Qxx / EEPROM 25LCxx — SOIC-8");
        y = drawChip8(canvas, W, y,
                new String[]{"/CS   (1)", "DO    (2)", "/WP   (3)", "GND   (4)"},
                new String[]{"VCC   (8)", "/HOLD (7)", "CLK   (6)", "DI    (5)"},
                new int[]   {C_CS,        C_MISO,      C_VCC,       C_GND      },
                new int[]   {C_VCC,       C_VCC,       C_SCK,       C_MOSI     },
                "Flash / EEPROM SPI");

        // ── Notas ─────────────────────────────────────────────────────────
        y += dp(12);
        y = drawSectionHeader(canvas, dp(10), y, W - dp(10), "⚠  Notas importantes");
        String[] notes = {
            "• /WP y /HOLD DEBEN ir a VCC (si quedan flotantes el chip falla)",
            "• Flash NOR (W25Qxx): BORRAR sector/chip ANTES de escribir",
            "• EEPROM SPI (25LCxx): no requiere borrado previo",
            "• Pin 4 = RA5/MCLR: configurado como E/S (MCLRE=0 en config word)",
            "• Pin 15 = RA6: I/O libre gracias al oscilador INTRC seleccionado",
            "• JEDEC cmd 50 4A: obtiene fabricante + tipo + capacidad del chip",
        };
        drawNotes(canvas, dp(10), y, notes);
    }

    // ======================================================================
    // HELPERS DE DIBUJO
    // ======================================================================

    /** Encabezado de sección con fondo oscuro */
    private float drawSectionHeader(Canvas canvas,
                                    float x, float y, float maxX, String title) {
        float h = dp(26);
        canvas.drawRect(x, y, maxX, y + h, fill(C_HDRB));
        Paint tp = makeText(11, C_TITLE, true);
        canvas.drawText(title, x + dp(8), y + dp(19), tp);
        return y + h;
    }

    /** Tabla de conexiones (dos columnas + punto de color) */
    private float drawConnTable(Canvas canvas,
                                float x, float y, float maxX,
                                Object[][] rows) {
        float rowH  = dp(25);
        float col1W = (maxX - x) * 0.43f;
        Paint lp    = new Paint(Paint.ANTI_ALIAS_FLAG);
        lp.setTypeface(Typeface.MONOSPACE);
        lp.setTextSize(sp(9f));

        for (int i = 0; i < rows.length; i++) {
            float ry = y + i * rowH;
            canvas.drawRect(x, ry, maxX, ry + rowH,
                    fill(i % 2 == 0 ? C_ROW1 : C_ROW2));
            int c = (int) rows[i][2];
            // punto de color
            canvas.drawCircle(x + col1W - dp(8), ry + rowH / 2f, dp(4), fill(c));
            // columna izquierda
            lp.setColor(c);
            lp.setTextAlign(Paint.Align.LEFT);
            canvas.drawText((String) rows[i][0], x + dp(6), ry + dp(17), lp);
            // columna derecha
            lp.setColor(C_TEXT);
            canvas.drawText((String) rows[i][1], x + col1W + dp(4), ry + dp(17), lp);
        }
        return y + rows.length * rowH;
    }

    /** Chip DIP-8 / SOIC-8 con 4 pines por lado */
    private float drawChip8(Canvas canvas, float W, float startY,
                             String[] lLabels, String[] rLabels,
                             int[] lColors, int[] rColors,
                             String chipName) {
        float cx    = W / 2f;
        float cW    = dp(90);
        float cH    = dp(110);
        float cL    = cx - cW / 2f;
        float cR    = cx + cW / 2f;
        float pinSp = cH / 4f;
        float wLen  = dp(22);

        canvas.drawRoundRect(new RectF(cL, startY, cR, startY + cH),
                dp(4), dp(4), fill(C_CHIP));
        canvas.drawRoundRect(new RectF(cL, startY, cR, startY + cH),
                dp(4), dp(4), stroke(C_BORD, 1.5f));
        // muesca
        canvas.drawArc(new RectF(cx - dp(9), startY - dp(5), cx + dp(9), startY + dp(5)),
                0, -180, false, stroke(C_BORD, 1.5f));
        // nombre
        drawCenteredText(canvas, chipName, cx, startY + cH / 2f + sp(5),
                makeText(11, C_TEXT, true));

        Paint pl = new Paint(Paint.ANTI_ALIAS_FLAG);
        pl.setTypeface(Typeface.MONOSPACE);
        pl.setTextSize(sp(9f));

        for (int i = 0; i < 4; i++) {
            float py = startY + pinSp * (i + 0.5f);
            // izquierda
            canvas.drawLine(cL - wLen, py, cL, py, stroke(lColors[i], 2.5f));
            pl.setColor(lColors[i]);
            pl.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(lLabels[i], cL - wLen - dp(4), py + sp(4), pl);
            // derecha
            canvas.drawLine(cR, py, cR + wLen, py, stroke(rColors[i], 2.5f));
            pl.setColor(rColors[i]);
            pl.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(rLabels[i], cR + wLen + dp(4), py + sp(4), pl);
        }
        return startY + cH;
    }

    /** Notas en lista (una por línea) */
    private float drawNotes(Canvas canvas, float x, float y, String[] notes) {
        Paint np = new Paint(Paint.ANTI_ALIAS_FLAG);
        np.setTextSize(sp(9.5f));
        np.setColor(C_TEXT);
        float lineH = dp(21);
        for (String note : notes) {
            canvas.drawText(note, x + dp(6), y + dp(15), np);
            y += lineH;
        }
        return y;
    }

    /** Leyenda de colores en dos filas de 5 */
    private void drawLegend(Canvas canvas, float x, float y, float maxX) {
        Object[][] items = {
            {"SDA I2C", C_SDA}, {"SCL I2C", C_SCL},
            {"CS SPI",  C_CS }, {"SCK SPI", C_SCK },
            {"MOSI",    C_MOSI},{"MISO",    C_MISO},
            {"VCC +5V", C_VCC}, {"GND",     C_GND },
            {"RX/TX",   C_UART},{"LED",     C_LED },
        };
        float iW = (maxX - x) / 5f;
        float iH = dp(20);
        Paint lp = new Paint(Paint.ANTI_ALIAS_FLAG);
        lp.setTextSize(sp(8.5f));
        for (int i = 0; i < items.length; i++) {
            int col = i % 5, row = i / 5;
            float ix = x + col * iW;
            float iy = y + row * iH;
            canvas.drawRect(ix, iy + dp(3), ix + dp(13), iy + dp(13),
                    fill((int) items[i][1]));
            lp.setColor((int) items[i][1]);
            canvas.drawText((String) items[i][0], ix + dp(17), iy + dp(13), lp);
        }
    }

    // ── Utilidades de Paint ───────────────────────────────────────────────

    private Paint fill(int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.FILL);
        p.setColor(color);
        return p;
    }

    private Paint stroke(int color, float dpWidth) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(dp(dpWidth));
        p.setColor(color);
        p.setStrokeCap(Paint.Cap.ROUND);
        return p;
    }

    private Paint makeText(float spSize, int color, boolean bold) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        if (spSize > 0) p.setTextSize(sp(spSize));
        p.setColor(color);
        p.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.MONOSPACE);
        return p;
    }

    private void drawCenteredText(Canvas canvas, String text, float cx, float y, Paint p) {
        p.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(text, cx, y, p);
    }

    private float dp(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private float sp(float sp) {
        return sp * getResources().getDisplayMetrics().scaledDensity;
    }
}

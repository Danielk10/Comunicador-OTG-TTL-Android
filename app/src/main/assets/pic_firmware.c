#include <pic16f628a.h>
#include <stdint.h>

// =============================================================================
// CONFIGURACIÓN CORREGIDA
// =============================================================================
// INTRC (oscilador interno 4MHz, RA6/RA7 como I/O)
// WDTE  = OFF  (sin watchdog)
// PWRTE = ON   (power-up timer habilitado)
// MCLRE = OFF  (RA5 como I/O, sin pin de reset externo) ← CORREGIDO
// BOREN = OFF  (sin brownout reset)
// LVP   = OFF  (sin programación de bajo voltaje)
// CPD   = OFF  (sin protección de datos)
// CP    = OFF  (sin protección de código)
//
// Config word: 0x3F10
//   Bit 0  FOSC0  = 0 ┐
//   Bit 1  FOSC1  = 0 ├─ FOSC = 100 = INTRC I/O
//   Bit 4  FOSC2  = 1 ┘
//   Bit 2  WDTE   = 0    WDT deshabilitado
//   Bit 3  PWRTE  = 0    PWRT habilitado (activo bajo)
//   Bit 5  MCLRE  = 0    *** MCLR DESHABILITADO (era el bug!) ***
//   Bit 6  BOREN  = 0    Brownout deshabilitado
//   Bit 7  LVP    = 0    LVP deshabilitado
//   Bits 8-13      = 1   Sin protección de código/datos
// =============================================================================
__code uint16_t __at (0x2007) configword = 0x3F10;

// =============================================================================
// Prototipos
// =============================================================================
void UART_Init(void);
void UART_Write(char data);
char UART_Read(void);
uint8_t UART_Available(void);
void UART_Flush_Errors(void);
void UART_Write_Text(const char *text);
void Delay_ms(uint16_t ms);

// Pines de salida para tareas (se evitan RB1/RX y RB2/TX)
#define PIN_TASK1 RB0
#define PIN_TASK2 RB3
#define PIN_TASK3 RB4
#define PIN_TASK4 RB5

// =============================================================================
// MAIN
// =============================================================================
void main(void) {
    char cmd;

    CMCON = 0x07;   // Comparadores analógicos OFF (todos los pines digitales)
    TRISB = 0x02;   // RB1=RX (Entrada), demás como Salida
    PORTB = 0x00;   // Todas las salidas en LOW

    UART_Init();

    // Pequeña pausa para que el UART se estabilice después del power-up
    Delay_ms(100);

    // Limpiar posibles errores de recepción acumulados durante el arranque
    UART_Flush_Errors();

    UART_Write_Text("PIC16F628A Listo\r\n");

    while(1) {
        // Primero verificar y limpiar errores de UART
        UART_Flush_Errors();

        // Verificar si hay dato recibido
        if(RCIF) {
            cmd = RCREG;    // Leer directamente el registro (ya sabemos que RCIF=1)

            // Verificar error de trama (framing error)
            // FERR se limpia al leer RCREG, pero el dato puede ser incorrecto
            if(FERR) {
                // Dato con error de trama, descartar
                continue;
            }

            switch(cmd) {
                case '1':
                    PIN_TASK1 = 1;
                    UART_Write_Text("RB0 ON\r\n");
                    break;

                case '0':
                    PIN_TASK1 = 0;
                    UART_Write_Text("RB0 OFF\r\n");
                    break;

                case 'A':
                case 'a':   // Aceptar minúsculas también
                    PORTB |= 0x39; // RB0, RB3, RB4, RB5 ON (evitar RB1,RB2,RB6,RB7)
                    UART_Write_Text("TODO ON\r\n");
                    break;

                case 'O':
                case 'o':
                    PORTB &= 0x06; // Apagar todo excepto pines UART (RB1,RB2)
                    UART_Write_Text("TODO OFF\r\n");
                    break;

                case 'T':
                case 't':
                    UART_Write_Text("Tarea 1...\r\n");
                    for(uint8_t i = 0; i < 3; i++) {
                        PIN_TASK1 = 1; Delay_ms(150); PIN_TASK1 = 0; Delay_ms(50);
                        PIN_TASK2 = 1; Delay_ms(150); PIN_TASK2 = 0; Delay_ms(50);
                        PIN_TASK3 = 1; Delay_ms(150); PIN_TASK3 = 0; Delay_ms(50);
                        PIN_TASK4 = 1; Delay_ms(150); PIN_TASK4 = 0; Delay_ms(50);
                    }
                    UART_Write_Text("Tarea 1 OK\r\n");
                    break;

                case 'F':
                case 'f':
                    UART_Write_Text("Tarea 2...\r\n");
                    for(uint8_t i = 0; i < 5; i++) {
                        PORTB |= 0x39; Delay_ms(200);
                        PORTB &= 0x06; Delay_ms(200);
                    }
                    UART_Write_Text("Tarea 2 OK\r\n");
                    break;

                case '?':   // Comando de diagnóstico: verificar que el PIC responde
                    UART_Write_Text("OK\r\n");
                    break;

                case 'S':   // Status: reportar estado actual de los pines
                case 's':
                    UART_Write_Text("RB0=");
                    UART_Write(PIN_TASK1 ? '1' : '0');
                    UART_Write_Text(" RB3=");
                    UART_Write(PIN_TASK2 ? '1' : '0');
                    UART_Write_Text(" RB4=");
                    UART_Write(PIN_TASK3 ? '1' : '0');
                    UART_Write_Text(" RB5=");
                    UART_Write(PIN_TASK4 ? '1' : '0');
                    UART_Write_Text("\r\n");
                    break;

                default:
                    // Eco del carácter no reconocido para diagnóstico
                    UART_Write_Text("CMD? ");
                    UART_Write(cmd);
                    UART_Write_Text("\r\n");
                    break;
            }
        }
    }
}

// =============================================================================
// UART_Init — Inicializar comunicación serial a 9600 baud
// =============================================================================
void UART_Init(void) {
    // Configurar pines de UART
    TRISB |= 0x02;     // RB1 (RX) como entrada
    TRISB &= ~0x04;    // RB2 (TX) como salida

    SPBRG = 25;         // 9600 baudios @ 4MHz (BRGH=1: baud = 4000000 / (16*(25+1)) = 9615)
    BRGH  = 1;           // Alta velocidad de baudios
    SYNC  = 0;           // Modo asíncrono
    TXEN  = 1;           // Habilitar transmisor
    SPEN  = 1;           // Habilitar puerto serial (configura RB1/RB2 como UART)
    CREN  = 1;           // Habilitar receptor continuo

    // Limpiar flags
    TXIF = 0;
    RCIF = 0;
}

// =============================================================================
// UART_Write — Enviar un byte por UART
// =============================================================================
void UART_Write(char data) {
    while(!TRMT);       // Esperar a que el TSR esté vacío (transmisión completa)
    TXREG = data;
}

// =============================================================================
// UART_Read — Leer un byte del UART (bloqueante)
// =============================================================================
char UART_Read(void) {
    // Primero limpiar errores
    UART_Flush_Errors();

    // Esperar a que haya un dato disponible
    while(!RCIF);
    return RCREG;
}

// =============================================================================
// UART_Available — Verificar si hay dato disponible sin bloquear
// =============================================================================
uint8_t UART_Available(void) {
    return RCIF ? 1 : 0;
}

// =============================================================================
// UART_Flush_Errors — Limpiar errores de UART (OERR y FERR)
// =============================================================================
void UART_Flush_Errors(void) {
    char dummy;

    // OERR (Overrun Error): Se debe resetear CREN para limpiar
    // Cuando OERR=1, no se pueden recibir más datos
    if(OERR) {
        CREN = 0;       // Deshabilitar receptor
        CREN = 1;       // Re-habilitar receptor (limpia OERR)
    }

    // FERR (Framing Error): Se limpia al leer RCREG
    // Leer los datos pendientes para limpiar el error
    if(FERR) {
        dummy = RCREG;  // Leer y descartar dato corrupto
        (void)dummy;    // Evitar warning de variable no usada
    }
}

// =============================================================================
// UART_Write_Text — Enviar una cadena de texto por UART
// =============================================================================
void UART_Write_Text(const char *text) {
    while(*text) {
        UART_Write(*text++);
    }
}

// =============================================================================
// Delay_ms — Retardo calibrado para oscilador interno de 4MHz (1 MIPS)
// =============================================================================
// A 4MHz, cada instrucción toma 1µs (excepto saltos = 2µs)
// El loop interno se calibra para ~1ms por iteración
// =============================================================================
void Delay_ms(uint16_t ms) {
    uint16_t i;
    uint8_t j;
    for(i = 0; i < ms; i++) {
        // ~1ms: loop interno de ~250 iteraciones × ~4µs cada una
        for(j = 0; j < 250; j++) {
            __asm
                nop
                nop
            __endasm;
        }
    }
}

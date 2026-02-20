#include <pic16f628a.h>
#include <stdint.h>

// Config: INTRC, No WDT, PWRT ON, No MCLR, No BOREN, No LVP
__code uint16_t __at (0x2007) configword = 0x3F30;

void UART_Init(void);
void UART_Write(char data);
char UART_Read(void);
void UART_Write_Text(char *text);
void Delay_ms(uint16_t ms);

// Pines de salida para tareas (Evitamos RB1/RX y RB2/TX)
#define PIN_TASK1 RB0
#define PIN_TASK2 RB3
#define PIN_TASK3 RB4
#define PIN_TASK4 RB5

void main(void) {
    char cmd;

    CMCON = 0x07; // Comparadores analógicos OFF
    TRISB = 0x02; // RB1=RX (Entrada), los demás Salida
    PORTB = 0x00;

    UART_Init();
    UART_Write_Text("PIC 16F628A: Sistema de Tareas Listo
");

    while(1) {
        if(RCIF) { // PIR1bits.RCIF -> RCIF
            cmd = UART_Read();
            
            switch(cmd) {
                case '1':
                    PIN_TASK1 = 1;
                    UART_Write_Text("RB0 ON
");
                    break;
                case '0':
                    PIN_TASK1 = 0;
                    UART_Write_Text("RB0 OFF
");
                    break;
                case 'A':
                    PORTB |= 0xF9; // RB0, RB3-RB7 ON
                    UART_Write_Text("TODO ON
");
                    break;
                case 'O':
                    PORTB &= 0x06; // Apagar todo excepto UART
                    UART_Write_Text("TODO OFF
");
                    break;
                case 'T':
                    UART_Write_Text("Iniciando Tarea 1...
");
                    for(uint8_t i=0; i<3; i++) {
                        PIN_TASK1 = 1; Delay_ms(100); PIN_TASK1 = 0;
                        PIN_TASK2 = 1; Delay_ms(100); PIN_TASK2 = 0;
                        PIN_TASK3 = 1; Delay_ms(100); PIN_TASK3 = 0;
                        PIN_TASK4 = 1; Delay_ms(100); PIN_TASK4 = 0;
                    }
                    UART_Write_Text("Tarea 1 completada
");
                    break;
                case 'F':
                    UART_Write_Text("Iniciando Tarea 2...
");
                    for(uint8_t i=0; i<5; i++) {
                        PORTB |= 0xF9; Delay_ms(200);
                        PORTB &= 0x06; Delay_ms(200);
                    }
                    UART_Write_Text("Tarea 2 completada
");
                    break;
                default:
                    break;
            }
        }
    }
}

void UART_Init(void) {
    SPBRG = 25;    // 9600 baud @ 4MHz
    BRGH = 1;      // TXSTAbits.BRGH -> BRGH
    SYNC = 0;      // TXSTAbits.SYNC -> SYNC
    TXEN = 1;      // TXSTAbits.TXEN -> TXEN
    SPEN = 1;      // RCSTAbits.SPEN -> SPEN
    CREN = 1;      // RCSTAbits.CREN -> CREN
}

void UART_Write(char data) {
    while(!TRMT);  // TXSTAbits.TRMT -> TRMT
    TXREG = data;
}

char UART_Read(void) {
    if(OERR) {     // RCSTAbits.OERR -> OERR
        CREN = 0;
        CREN = 1;
    }
    while(!RCIF);  // PIR1bits.RCIF -> RCIF
    return RCREG;
}

void UART_Write_Text(char *text) {
    while(*text) UART_Write(*text++);
}

void Delay_ms(uint16_t ms) {
    uint16_t i, j;
    for(i=0; i<ms; i++)
        for(j=0; j<100; j++); 
}
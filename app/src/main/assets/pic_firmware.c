/*
 * PIC 16F628A Advance Multi-Pin Task Firmware
 * Compiler: SDCC
 * Oscillator: Internal 4MHz
 *
 * Command Protocol:
 * 'A' -> Todo ON (RB0, RB3, RB4, RB5)
 * 'O' -> Todo OFF
 * 'T' -> Tarea 1: Secuencia de luces (Kitt)
 * 'F' -> Tarea 2: Parpadeo alternado
 * '1' -> LED RB0 ON
 * '0' -> LED RB0 OFF
 */

#include <pic16f628a.h>
#include <stdint.h>

// Config: INTRC, No WDT, PWRT ON, No MCLR, No BOREN, No LVP
static __code uint16_t __at (0x2007) configword = 0x3F30;

void UART_Init(void);
void UART_Write(char data);
char UART_Read(void);
void UART_Write_Text(char *text);
void Delay_ms(uint16_t ms);

// Pines de salida para tareas (Evitamos RB1/RB2 que son UART)
#define PIN_TASK1 RB0
#define PIN_TASK2 RB3
#define PIN_TASK3 RB4
#define PIN_TASK4 RB5

void main(void) {
    char cmd;

    CMCON = 0x07; // Analog comparators OFF
    TRISB = 0x02; // RB1=RX (Input), others Output
    PORTB = 0x00;

    UART_Init();
    UART_Write_Text("PIC 16F628A: Sistema de Tareas Listo\r\n");

    while(1) {
        if(PIR1bits.RCIF) {
            cmd = UART_Read();
            
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
                    PORTB |= 0xF9; // Encender RB0, RB3, RB4, RB5, RB6, RB7 (ignorando RB1/RB2)
                    UART_Write_Text("TODO ON\r\n");
                    break;
                case 'O':
                    PORTB &= 0x06; // Apagar todo excepto RB1/RB2
                    UART_Write_Text("TODO OFF\r\n");
                    break;
                case 'T':
                    UART_Write_Text("Iniciando Tarea 1 (Secuencia)...\r\n");
                    for(uint8_t i=0; i<3; i++) {
                        PIN_TASK1 = 1; Delay_ms(100); PIN_TASK1 = 0;
                        PIN_TASK2 = 1; Delay_ms(100); PIN_TASK2 = 0;
                        PIN_TASK3 = 1; Delay_ms(100); PIN_TASK3 = 0;
                        PIN_TASK4 = 1; Delay_ms(100); PIN_TASK4 = 0;
                    }
                    UART_Write_Text("Tarea 1 completada\r\n");
                    break;
                case 'F':
                    UART_Write_Text("Iniciando Tarea 2 (Flash)...\r\n");
                    for(uint8_t i=0; i<5; i++) {
                        PORTB |= 0xF9; Delay_ms(200);
                        PORTB &= 0x06; Delay_ms(200);
                    }
                    UART_Write_Text("Tarea 2 completada\r\n");
                    break;
                case '\r': // Ignorar retorno de carro
                case '\n': // Ignorar salto de línea
                    break;
                default:
                    UART_Write_Text("CMD Desconocido\r\n");
                    break;
            }
        }
    }
}

void UART_Init(void) {
    SPBRG = 25; // 9600 baud @ 4MHz
    TXSTAbits.BRGH = 1;
    TXSTAbits.SYNC = 0;
    TXSTAbits.TXEN = 1;
    RCSTAbits.SPEN = 1;
    RCSTAbits.CREN = 1;
}

void UART_Write(char data) {
    while(!TXSTAbits.TRMT);
    TXREG = data;
}

char UART_Read(void) {
    if(RCSTAbits.OERR) { // Handle Overrun Error
        RCSTAbits.CREN = 0;
        RCSTAbits.CREN = 1;
    }
    while(!PIR1bits.RCIF);
    return RCREG;
}

void UART_Write_Text(char *text) {
    while(*text) UART_Write(*text++);
}

void Delay_ms(uint16_t ms) {
    uint16_t i, j;
    for(i=0; i<ms; i++)
        for(j=0; j<100; j++); // Aproximado para 4MHz
}

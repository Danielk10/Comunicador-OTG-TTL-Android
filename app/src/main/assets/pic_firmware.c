#include <pic16f628a.h>
#include <stdint.h>

// Config: INTRC, No WDT, PWRT ON, No MCLR, No BOREN, No LVP
__code uint16_t __at (0x2007) configword = 0x3F30;

void UART_Init(void);
void UART_Write(char data);
char UART_Read(void);
void UART_Write_Text(char *text);
void Delay_ms(uint16_t ms);

// Pines de salida para tareas (se evita RB1/RX y RB2/TX)
#define PIN_TASK1 RB0
#define PIN_TASK2 RB3
#define PIN_TASK3 RB4
#define PIN_TASK4 RB5

void main(void) {
    char cmd;

    CMCON = 0x07; // Comparadores analogicos OFF
    TRISB = 0x02; // RB1=RX (Entrada), demas Salida
    PORTB = 0x00;

    UART_Init();
    UART_Write_Text("PIC 16F628A: Sistema de Tareas Listo\r\n");

    while(1) {
        if(RCIF) { // Verificar si hay dato recibido
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
                    PORTB |= 0xF9; // RB0, RB3-RB7 ON
                    UART_Write_Text("TODO ON\r\n");
                    break;
                case 'O':
                    PORTB &= 0x06; // Apagar todo excepto pines UART
                    UART_Write_Text("TODO OFF\r\n");
                    break;
                case 'T':
                    UART_Write_Text("Iniciando Tarea 1...\r\n");
                    for(uint8_t i=0; i<3; i++) {
                        PIN_TASK1 = 1; Delay_ms(100); PIN_TASK1 = 0;
                        PIN_TASK2 = 1; Delay_ms(100); PIN_TASK2 = 0;
                        PIN_TASK3 = 1; Delay_ms(100); PIN_TASK3 = 0;
                        PIN_TASK4 = 1; Delay_ms(100); PIN_TASK4 = 0;
                    }
                    UART_Write_Text("Tarea 1 completada\r\n");
                    break;
                case 'F':
                    UART_Write_Text("Iniciando Tarea 2...\r\n");
                    for(uint8_t i=0; i<5; i++) {
                        PORTB |= 0xF9; Delay_ms(200);
                        PORTB &= 0x06; Delay_ms(200);
                    }
                    UART_Write_Text("Tarea 2 completada\r\n");
                    break;
                default:
                    break;
            }
        }
    }
}

void UART_Init(void) {
    SPBRG = 25;    // 9600 baudios @ 4MHz (BRGH=1: baud = Fosc / (16*(SPBRG+1)))
    BRGH = 1;      // Alta velocidad de baudios
    SYNC = 0;      // Modo asincrono
    TXEN = 1;      // Habilitar transmisor
    SPEN = 1;      // Habilitar puerto serial
    CREN = 1;      // Habilitar receptor continuo
}

void UART_Write(char data) {
    while(!TRMT);  // Esperar a que el registro de transmision este vacio
    TXREG = data;
}

char UART_Read(void) {
    if(OERR) {     // Limpiar error de desbordamiento si ocurre
        CREN = 0;
        CREN = 1;
    }
    while(!RCIF);  // Esperar dato recibido
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
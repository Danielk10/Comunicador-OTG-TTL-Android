/*
 * PIC 16F628A UART Communication Firmware
 * Compiler: SDCC
 * Oscillator: Internal 4MHz
 * Baud Rate: 9600
 *
 * Connections:
 * RA2 (Vref) -> RX (Data from Android)
 * RA3 (Cmp1) -> TX (Data to Android) - Note: 16F628A hardware UART uses RB1(RX) and RB2(TX)
 */

#include <pic16f628a.h>
#include <stdint.h>

// Configuration Bits
// Internal Oscillator, No Watchdog, Power Up Timer On, Master Clear Off, Brown Out Off, Low Voltage Prog Off, Data EE Protect Off, Code Protect Off
static __code uint16_t __at (0x2007) configword = 0x3F30; // _INTRC_OSC_NOCLKOUT & _WDT_OFF & _PWRTE_ON & _MCLRE_OFF & _BOREN_OFF & _LVP_OFF & _CPD_OFF & _CP_OFF

// Function Prototypes
void UART_Init(void);
void UART_Write(char data);
char UART_Read(void);
void UART_Write_Text(char *text);

void main(void) {
    // Variable declarations make sure they are at top of block for C compat
    char received_char;

    // Initialization
    CMCON = 0x07; // Disable Comparators
    TRISB = 0x02; // RB1 input (RX), RB2 output (TX), others output
    PORTB = 0x00;

    UART_Init();

    // Startup Message
    UART_Write_Text("PIC 16F628A Ready\r\n");

    while(1) {
        if(PIR1bits.RCIF) {
            received_char = UART_Read();
            
            // Echo back the character
            UART_Write(received_char);
            
            // Toggle LED on RB0 for visual feedback based on received data
            if (received_char == '1') {
                PORTBbits.RB0 = 1;
                UART_Write_Text(" -> LED ON\r\n");
            } else if (received_char == '0') {
                PORTBbits.RB0 = 0;
                UART_Write_Text(" -> LED OFF\r\n");
            } else {
                 UART_Write_Text(" -> Echo\r\n");
            }
        }
    }
}

void UART_Init(void) {
    // SPBRG = ((Fosc / BaudRate) / 16) - 1
    // Fosc = 4000000, BaudRate = 9600
    // SPBRG = ((4000000 / 9600) / 16) - 1 = 25.04 -> 25
    SPBRG = 25;
    
    TXSTAbits.BRGH = 1;  // High Baud Rate Select
    TXSTAbits.SYNC = 0;  // Asynchronous Mode
    TXSTAbits.TXEN = 1;  // Enable Transmission
    
    RCSTAbits.SPEN = 1;  // Serial Port Enable
    RCSTAbits.CREN = 1;  // Continuous Receive Enable
}

void UART_Write(char data) {
    while(!TXSTAbits.TRMT); // Wait only if buffer is full, actually TRMT shows if shift register is empty. TXIF is for buffer.
    TXREG = data;
}

char UART_Read(void) {
    while(!PIR1bits.RCIF); // Wait for data
    return RCREG;
}

void UART_Write_Text(char *text) {
    int i; 
    for(i=0; text[i]!='\0'; i++) {
        UART_Write(text[i]);
    }
}

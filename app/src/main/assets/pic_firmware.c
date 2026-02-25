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
void Delay_us(void);

// Funciones I2C
void I2C_Init(void);
void I2C_Start(void);
void I2C_Stop(void);
void I2C_Ack(void);
void I2C_Nack(void);
uint8_t I2C_Write(uint8_t data);
uint8_t I2C_Read(uint8_t ack);

// Funciones SPI
void SPI_Init(void);
void SPI_Write(uint8_t data);
uint8_t SPI_Read(void);


// Pines de salida para tareas (se evitan RB1/RX y RB2/TX)
#define PIN_TASK1 RB0
#define PIN_TASK2 RB3
#define PIN_TASK3 RB4
#define PIN_TASK4 RB5

// =============================================================================
// I2C y SPI Pines (Puerto A)
// =============================================================================
// I2C
#define I2C_SDA_PIN RA0
#define I2C_SDA_TRIS TRISA0
#define I2C_SCL_PIN RA1
#define I2C_SCL_TRIS TRISA1

// SPI
#define SPI_CS_PIN RA2
#define SPI_CS_TRIS TRISA2
#define SPI_SCK_PIN RA3
#define SPI_SCK_TRIS TRISA3
#define SPI_MISO_PIN RA5
#define SPI_MISO_TRIS TRISA5
#define SPI_MOSI_PIN RA6
#define SPI_MOSI_TRIS TRISA6

// =============================================================================
// MAIN
// =============================================================================
void main(void) {
    char cmd;
    uint8_t addr_hi, addr_lo, len, data, i;

    CMCON = 0x07;   // Comparadores analógicos OFF (todos los pines digitales)
    TRISB = 0x02;   // RB1=RX (Entrada), demás como Salida
    PORTB = 0x00;   // Todas las salidas en LOW
    
    // Inicializar pines de I2C y SPI
    I2C_Init();
    SPI_Init();

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

                // =============================================================
                // Comandos I2C EEPROM (24Cxx)
                // Formato: 'I' 'R'/'W' <addr_len> <chip_addr> <addr_hi> <addr_lo> <len/data...>
                // addr_len = 1 (small EEPROM 24c01-16), 2 (large EEPROM 24c32-512)
                // =============================================================
                case 'I': {
                    char op = UART_Read();
                    uint8_t addr_len = UART_Read();
                    uint8_t chip_addr = UART_Read();
                    addr_hi = UART_Read();
                    addr_lo = UART_Read();

                    if(op == 'R') {
                        len = UART_Read();
                        I2C_Start();
                        I2C_Write(chip_addr);
                        if(addr_len == 2) I2C_Write(addr_hi);
                        I2C_Write(addr_lo);
                        I2C_Start(); // Repeated start
                        I2C_Write(chip_addr | 0x01); // Lectura
                        for(i = 0; i < len; i++) {
                            data = I2C_Read(i == (len - 1) ? 0 : 1);
                            UART_Write(data);
                        }
                        I2C_Stop();
                    } else if(op == 'W') {
                        len = UART_Read();
                        I2C_Start();
                        I2C_Write(chip_addr);
                        if(addr_len == 2) I2C_Write(addr_hi);
                        I2C_Write(addr_lo);
                        for(i = 0; i < len; i++) {
                            data = UART_Read();
                            I2C_Write(data);
                        }
                        I2C_Stop();
                        Delay_ms(5); // Ciclo de escritura EEPROM I2C
                        UART_Write('K'); // Acknowledge OK
                    }
                    break;
                }

                // =============================================================
                // Comandos SPI EEPROM (25Cxx)
                // Formato: 'P' 'R'/'W' <addr_hi> <addr_lo> <len/data...>
                // (Usamos 'P' porque 'S' ya se usa para Status)
                // =============================================================
                case 'P': {
                    char op = UART_Read();
                    addr_hi = UART_Read();
                    addr_lo = UART_Read();

                    if(op == 'R') {
                        len = UART_Read();
                        SPI_CS_PIN = 0;
                        SPI_Write(0x03); // Comando Leer (Read Data)
                        SPI_Write(addr_hi);
                        SPI_Write(addr_lo);
                        for(i = 0; i < len; i++) {
                            data = SPI_Read();
                            UART_Write(data);
                        }
                        SPI_CS_PIN = 1;
                    } else if(op == 'W') {
                        len = UART_Read();
                        // Habilitar escritura (WREN)
                        SPI_CS_PIN = 0;
                        SPI_Write(0x06); // WREN
                        SPI_CS_PIN = 1;
                        
                        // Escribir datos (Page Program)
                        SPI_CS_PIN = 0;
                        SPI_Write(0x02); // Comando Escibir (Page Program)
                        SPI_Write(addr_hi);
                        SPI_Write(addr_lo);
                        for(i = 0; i < len; i++) {
                            data = UART_Read();
                            SPI_Write(data);
                        }
                        SPI_CS_PIN = 1;
                        
                        // Esperar estado Ready (wip = 0)
                        data = 0x01;
                        while(data & 0x01) {
                            SPI_CS_PIN = 0;
                            SPI_Write(0x05); // Read Status Register
                            data = SPI_Read();
                            SPI_CS_PIN = 1;
                        }
                        UART_Write('K'); // Acknowledge OK
                    }
                    break;
                }

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

// =============================================================================
// Delay_us — Retardo muy corto para I2C y SPI
// =============================================================================
void Delay_us(void) {
    __asm
        nop
        nop
        nop
        nop
    __endasm;
}

// =============================================================================
// Implementación I2C Bit-Banging
// Para un bus I2C, la línea debe dejarse como entrada (Alta impedancia) 
// para enviar un '1' lógico (debido a la resistencia pull-up externa o interna).
// Para un '0' lógico, la configuramos como salida y ponemos a bajo (0V).
// Aquí simplificaremos: SCL siempre salida activa, SDA como in/out.
// NOTA: Se asumen resistencias de pull-up externas en SDA y SCL.
// =============================================================================
void I2C_Init(void) {
    I2C_SDA_PIN = 0;
    I2C_SCL_PIN = 0;
    I2C_SDA_TRIS = 1; // SDA Alta impedancia (Pull-Up = 1)
    I2C_SCL_TRIS = 1; // SCL Alta impedancia (Pull-Up = 1)
}

void I2C_Start(void) {
    I2C_SDA_TRIS = 1;
    I2C_SCL_TRIS = 1;
    Delay_us();
    I2C_SDA_TRIS = 0; // SDA baja
    I2C_SDA_PIN = 0;
    Delay_us();
    I2C_SCL_TRIS = 0; // SCL baja
    I2C_SCL_PIN = 0;
    Delay_us();
}

void I2C_Stop(void) {
    I2C_SDA_TRIS = 0; 
    I2C_SDA_PIN = 0;
    Delay_us();
    I2C_SCL_TRIS = 1; // SCL alta
    Delay_us();
    I2C_SDA_TRIS = 1; // SDA alta
    Delay_us();
}

uint8_t I2C_Write(uint8_t data) {
    uint8_t ack_bit, i;
    for(i = 0; i < 8; i++) {
        if(data & 0x80) {
            I2C_SDA_TRIS = 1; // 1 (alta impedancia)
        } else {
            I2C_SDA_TRIS = 0; // 0
            I2C_SDA_PIN = 0;
        }
        Delay_us();
        I2C_SCL_TRIS = 1; // SCL Alta
        Delay_us();
        I2C_SCL_TRIS = 0; // SCL Baja
        I2C_SCL_PIN = 0;
        data <<= 1;
    }
    // Leer ACK
    I2C_SDA_TRIS = 1; // Liberar SDA
    Delay_us();
    I2C_SCL_TRIS = 1; // SCL alta para leer
    Delay_us();
    ack_bit = I2C_SDA_PIN; // Leer bit de ack del esclavo
    I2C_SCL_TRIS = 0; // SCL baja
    I2C_SCL_PIN = 0;
    return ack_bit; // 0 = ACK, 1 = NACK
}

uint8_t I2C_Read(uint8_t ack) {
    uint8_t data = 0, i;
    I2C_SDA_TRIS = 1; // Liberar SDA
    for(i = 0; i < 8; i++) {
        data <<= 1;
        Delay_us();
        I2C_SCL_TRIS = 1; // SCL Alta
        Delay_us();
        if(I2C_SDA_PIN) data |= 0x01; // Leer pin
        I2C_SCL_TRIS = 0; // SCL Baja
        I2C_SCL_PIN = 0;
    }
    // Enviar ACK/NACK
    if(ack) {
        I2C_SDA_TRIS = 0;
        I2C_SDA_PIN = 0; // ACK (baja)
    } else {
        I2C_SDA_TRIS = 1; // NACK (alta)
    }
    Delay_us();
    I2C_SCL_TRIS = 1;
    Delay_us();
    I2C_SCL_TRIS = 0;
    I2C_SCL_PIN = 0;
    
    return data;
}

// =============================================================================
// Implementación SPI Bit-Banging (Modo 0, 0: SCK inactivo bajo, dato capturado en flanco de subida)
// =============================================================================
void SPI_Init(void) {
    SPI_CS_TRIS = 0;   // CS Salida
    SPI_SCK_TRIS = 0;  // SCK Salida
    SPI_MOSI_TRIS = 0; // MOSI Salida
    SPI_MISO_TRIS = 1; // MISO Entrada
    
    SPI_CS_PIN = 1;    // Inactivo Alto
    SPI_SCK_PIN = 0;   // Reloj inactivo Bajo
    SPI_MOSI_PIN = 0;
}

void SPI_Write(uint8_t data) {
    uint8_t i;
    for(i = 0; i < 8; i++) {
        if(data & 0x80) SPI_MOSI_PIN = 1;
        else            SPI_MOSI_PIN = 0;
        Delay_us();
        SPI_SCK_PIN = 1; // Flanco subida (esclavo lee)
        Delay_us();
        SPI_SCK_PIN = 0; // Flanco bajada
        data <<= 1;
    }
}

uint8_t SPI_Read(void) {
    uint8_t data = 0, i;
    SPI_MOSI_PIN = 0;
    for(i = 0; i < 8; i++) {
        data <<= 1;
        Delay_us();
        SPI_SCK_PIN = 1; // Flanco subida (esclavo cambia o mantiene dato)
        Delay_us();
        if(SPI_MISO_PIN) data |= 0x01; // Leemos en el estado alto (Modo 0) o flanco bajada
        SPI_SCK_PIN = 0; // Flanco bajada
    }
    return data;
}

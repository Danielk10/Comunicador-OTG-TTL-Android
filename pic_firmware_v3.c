/*
 * ============================================================================
 * pic_firmware_v3.c  —  PIC16F628A  Memory Reader (I2C + SPI)
 * Compilador: SDCC  (sdcc --std-c99 -mpic14 -p16f628a pic_firmware_v3.c)
 * Oscilador:  INTRC 4 MHz   |   UART: 9600 baud
 * ============================================================================
 *
 * CAMBIOS v3 respecto a v2
 * ========================
 *  • LED indicador en RB4 (pin 10 DIP-18)
 *      – Parpadeo doble al arrancar  (sistema listo)
 *      – Encendido continuo durante lectura de memoria
 *      – Parpadeo simple al completar escritura / triple al completar erase
 *  • Corregidos TODOS los warnings del log de v2:
 *      – Variables 'wip_count' y 'status' eliminadas (no se usaban)
 *      – Constante UART_TIMEOUT_COUNT eliminada (se usaba rollover uint16_t)
 *      – Cast explícito (uint8_t) en shifts de SPI_Transfer e I2C (warning 158)
 *  • Nuevo comando 'P' 'F': Full Flash Dump automático
 *      Lee JEDEC ID, calcula tamaño (2^capacity bytes), vuelca toda la Flash
 *  • Nuevo comando 'I' 'F': Full EEPROM I2C Dump desde dirección 0
 * ============================================================================
 */

#include <pic16f628a.h>
#include <stdint.h>

/* ============================================================================
 * CONFIGURATION WORD 0x3F10
 *   FOSC=100(INTRC,RA6/RA7 I/O)  WDTE=0  PWRTE=0(activo-bajo)
 *   MCLRE=0(RA5 I/O)  BOREN=0  LVP=0  CP/CPD=1(sin proteccion)
 * ========================================================================== */
__code uint16_t __at(0x2007) configword = 0x3F10;

/* ============================================================================
 * Pines I2C — bit-banging open-drain
 * REQUISITO: resistencias pull-up externas 4.7 kOhm en SDA y SCL
 * ========================================================================== */
#define SDA          RA0
#define SDA_DIR      TRISA0
#define SCL          RA1
#define SCL_DIR      TRISA1

/* ============================================================================
 * Pines SPI — bit-banging Modo 0 (CPOL=0, CPHA=0)
 * ========================================================================== */
#define SPI_CS       RA2
#define SPI_CS_DIR   TRISA2
#define SPI_SCK      RA3
#define SPI_SCK_DIR  TRISA3
#define SPI_MISO     RA5
#define SPI_MISO_DIR TRISA5
#define SPI_MOSI     RA6
#define SPI_MOSI_DIR TRISA6

/* ============================================================================
 * LED indicador — RB4 = pin 10 fisico DIP-18
 * Circuito: RB4 --[330 Ohm]-- LED --[GND]
 * ========================================================================== */
#define LED          RB4
#define LED_DIR      TRISB4

/* ============================================================================
 * Tokens del protocolo binario
 * ========================================================================== */
#define RESP_OK   ((uint8_t)0x4B)   /* 'K'  exito                           */
#define RESP_ERR  ((uint8_t)0x58)   /* 'X'  error / NACK / timeout          */
#define RESP_END  ((uint8_t)0x55)   /* 'U'  fin de flujo de datos           */

/* Timeout SPI WIP polling: 5000 iter x 1 ms = 5 segundos maximo            */
#define SPI_WIP_TIMEOUT  5000U

/* Macros CS con delays de setup/hold                                        */
#define SPI_CS_Assert()  do { Delay_us(); SPI_CS = 0; Delay_us(); } while(0)
#define SPI_CS_Release() do { Delay_us(); SPI_CS = 1; Delay_us(); } while(0)

/* Variable global de timeout UART                                           */
static uint8_t g_uart_timeout;

/* ============================================================================
 * Prototipos
 * ========================================================================== */
void    UART_Init(void);
void    UART_Write(uint8_t d);
uint8_t UART_Read(void);
void    UART_Write_Text(const char *s);
void    UART_Flush_Errors(void);
void    Delay_ms(uint16_t ms);
void    Delay_us(void);
void    LED_Blink(uint8_t times);
void    I2C_Init(void);
void    I2C_Start(void);
void    I2C_Stop(void);
uint8_t I2C_Write_Byte(uint8_t d);
uint8_t I2C_Read_Byte(uint8_t send_ack);
void    SPI_Init(void);
uint8_t SPI_Transfer(uint8_t d);
uint8_t SPI_WaitReady(void);


/* ============================================================================
 * MAIN
 * ========================================================================== */
void main(void) {
    /* Todas las variables declaradas al inicio del bloque (SDCC/C89) */
    uint8_t  cmd, op;
    uint8_t  addr_len, chip_addr, addr_hi, addr_md, addr_lo;
    uint8_t  spi_opcode, data, ack, err;
    uint8_t  len_hi, len_lo;
    uint8_t  jedec_mfr, jedec_type, jedec_cap;
    uint8_t  sa;          /* scan address I2C                                */
    uint8_t  blk;         /* contador de bloque para Full Dump               */
    uint16_t len, i;
    uint32_t flash_size;
    uint32_t byte_addr;

    /* ---------------------------------------------------------------------- */
    /* Inicializacion del hardware                                             */
    /* ---------------------------------------------------------------------- */
    CMCON  = 0x07;   /* Comparadores OFF -> todos los pines digitales         */
    TRISB  = 0x02;   /* RB1(RX)=entrada; RB4(LED)=salida; demas=salida       */
    PORTB  = 0x00;   /* Salidas en LOW                                        */
    LED_DIR = 0;     /* RB4 como salida (redundante, pero explicito)          */
    LED     = 0;

    I2C_Init();
    SPI_Init();
    UART_Init();

    Delay_ms(100);
    UART_Flush_Errors();

    LED_Blink(2);                    /* 2 parpadeos = sistema listo           */
    UART_Write_Text("PICMEM v3\r\n");

    /* ======================================================================= */
    /* Bucle principal                                                          */
    /* ======================================================================= */
    while(1) {

        UART_Flush_Errors();
        if(!RCIF) continue;

        /* -------------------------------------------------------------------
         * BUG FIX critico: verificar FERR *antes* de leer RCREG.
         * En el PIC, leer RCREG borra FERR automaticamente, por lo que
         * checkearlo despues siempre daria 0 y bytes corruptos pasarian.
         * ----------------------------------------------------------------- */
        if(FERR) {
            data = RCREG;       /* Descartar dato corrupto (borra FERR)       */
            (void)data;
            UART_Write(RESP_ERR);
            continue;
        }
        cmd = RCREG;
        g_uart_timeout = 0;

        switch(cmd) {

            /* ================================================================
             * 3F ('?') — Ping / Identificacion
             * Respuesta: "PICMEM v3 OK\r\n"
             * ============================================================== */
            case '?':
                UART_Write_Text("PICMEM v3 OK\r\n");
                break;

            /* ================================================================
             * 49 ('I') — Operaciones I2C
             * ============================================================== */
            case 'I': {
                op = UART_Read();
                if(g_uart_timeout) { UART_Write(RESP_ERR); break; }

                /* -----------------------------------------------------------
                 * 49 53 — I2C Bus Scan
                 * Formato  : 49 53
                 * Respuesta: [addr_7bit]... FF
                 *   Cada byte es la dir de 7 bits de un dispositivo que
                 *   respondio ACK. 0xFF marca fin de lista.
                 * Ejemplo — scan completo:  49 53
                 * --------------------------------------------------------- */
                if(op == 'S') {
                    for(sa = 0x08; sa <= 0x77; sa++) {
                        I2C_Start();
                        ack = I2C_Write_Byte((uint8_t)(sa << 1));
                        I2C_Stop();
                        if(ack == 0) UART_Write(sa);
                        Delay_ms(2);
                    }
                    UART_Write(0xFF);
                    break;
                }

                /* -----------------------------------------------------------
                 * 49 46 — I2C Full EEPROM Dump (desde direccion 0)
                 * Formato  : 49 46 <addr_len> <chip_addr> <LH> <LL>
                 *   addr_len : 1 = dir 8-bit  (24C01..24C16)
                 *              2 = dir 16-bit (24C32..24C512)
                 *   chip_addr: dir I2C R/W=0 (ej: A0)
                 *   LH,LL    : tamano total en bytes (MSB first)
                 * Respuesta: [todos los bytes] RESP_END  o  RESP_ERR
                 * Ejemplo — volcar 32768B de 24C256 (0xA0):
                 *   49 46 02 A0 80 00
                 * --------------------------------------------------------- */
                if(op == 'F') {
                    addr_len  = UART_Read();
                    chip_addr = UART_Read();
                    len_hi    = UART_Read();
                    len_lo    = UART_Read();
                    if(g_uart_timeout) { UART_Write(RESP_ERR); break; }
                    len = ((uint16_t)len_hi << 8) | (uint16_t)len_lo;

                    LED = 1;
                    err = 0;
                    /* Posicionar puntero en 0x0000 */
                    I2C_Start();
                    if(I2C_Write_Byte(chip_addr & 0xFE) != 0) err = 1;
                    if(!err && (addr_len >= 2)) {
                        if(I2C_Write_Byte(0x00) != 0) err = 1;
                    }
                    if(!err) {
                        if(I2C_Write_Byte(0x00) != 0) err = 1;
                    }
                    if(err) {
                        I2C_Stop(); LED = 0;
                        UART_Write(RESP_ERR);
                        break;
                    }
                    /* Repeated START + modo lectura */
                    I2C_Start();
                    if(I2C_Write_Byte(chip_addr | 0x01) != 0) {
                        I2C_Stop(); LED = 0;
                        UART_Write(RESP_ERR);
                        break;
                    }
                    for(i = 0; i < len; i++) {
                        data = I2C_Read_Byte((i < (len - 1)) ? 1 : 0);
                        UART_Write(data);
                    }
                    I2C_Stop();
                    LED = 0;
                    UART_Write(RESP_END);
                    break;
                }

                /* Parametros comunes para R y W */
                addr_len  = UART_Read();
                chip_addr = UART_Read();
                addr_hi   = UART_Read();
                addr_lo   = UART_Read();
                len_hi    = UART_Read();
                len_lo    = UART_Read();
                if(g_uart_timeout) { UART_Write(RESP_ERR); break; }
                len = ((uint16_t)len_hi << 8) | (uint16_t)len_lo;

                /* -----------------------------------------------------------
                 * 49 52 — I2C Read (lectura parcial con direccion)
                 * Formato  : 49 52 <addr_len> <chip_addr> <A1> <A0> <LH> <LL>
                 * Respuesta: [len bytes] RESP_END  o  RESP_ERR si NACK
                 * Ejemplo — leer 256B desde 0x0080 en 24C256 (0xA0):
                 *   49 52 02 A0 00 80 01 00
                 * --------------------------------------------------------- */
                if(op == 'R') {
                    err = 0;
                    LED = 1;
                    I2C_Start();
                    if(I2C_Write_Byte(chip_addr & 0xFE) != 0) err = 1;
                    if(!err && (addr_len >= 2)) {
                        if(I2C_Write_Byte(addr_hi) != 0) err = 1;
                    }
                    if(!err) {
                        if(I2C_Write_Byte(addr_lo) != 0) err = 1;
                    }
                    if(err) {
                        I2C_Stop(); LED = 0;
                        UART_Write(RESP_ERR);
                        break;
                    }
                    I2C_Start();   /* Repeated START */
                    if(I2C_Write_Byte(chip_addr | 0x01) != 0) {
                        I2C_Stop(); LED = 0;
                        UART_Write(RESP_ERR);
                        break;
                    }
                    for(i = 0; i < len; i++) {
                        data = I2C_Read_Byte((i < (len - 1)) ? 1 : 0);
                        UART_Write(data);
                    }
                    I2C_Stop();
                    LED = 0;
                    UART_Write(RESP_END);
                }

                /* -----------------------------------------------------------
                 * 49 57 — I2C Write (page write)
                 * Formato  : 49 57 <addr_len> <chip_addr> <A1> <A0> <LH> <LL>
                 *            + [datos...]
                 * Respuesta: RESP_OK  o  RESP_ERR
                 * Paginas maximas por comando:
                 *   24C01-16 -> 8B  24C32-64 -> 32B  24C128-512 -> 64B
                 * Ejemplo — escribir 4B en 0x0000 (24C256, 0xA0):
                 *   49 57 02 A0 00 00 00 04 DE AD BE EF
                 * --------------------------------------------------------- */
                else if(op == 'W') {
                    err = 0;
                    I2C_Start();
                    if(I2C_Write_Byte(chip_addr & 0xFE) != 0) err = 1;
                    if(!err && (addr_len >= 2)) {
                        if(I2C_Write_Byte(addr_hi) != 0) err = 1;
                    }
                    if(!err) {
                        if(I2C_Write_Byte(addr_lo) != 0) err = 1;
                    }
                    /* Siempre drenar todos los bytes UART aunque haya error */
                    for(i = 0; i < len; i++) {
                        data = UART_Read();
                        if(!err) {
                            if(I2C_Write_Byte(data) != 0) err = 1;
                        }
                    }
                    I2C_Stop();
                    if(!err) {
                        Delay_ms(10);
                        LED_Blink(1);
                        UART_Write(RESP_OK);
                    } else {
                        UART_Write(RESP_ERR);
                    }
                }
                else {
                    UART_Write(RESP_ERR);   /* Sub-comando desconocido       */
                }
                break;
            }

            /* ================================================================
             * 50 ('P') — Operaciones SPI
             * ============================================================== */
            case 'P': {
                op = UART_Read();
                if(g_uart_timeout) { UART_Write(RESP_ERR); break; }

                /* -----------------------------------------------------------
                 * 50 4A — JEDEC ID
                 * Formato  : 50 4A
                 * Respuesta: [Manufacturer 1B][MemType 1B][Capacity 1B]
                 * IDs tipicos:
                 *   EF 40 14 = W25Q08  (1 MB)
                 *   EF 40 15 = W25Q16  (2 MB)
                 *   EF 40 16 = W25Q32  (4 MB)
                 *   EF 40 17 = W25Q64  (8 MB)
                 *   EF 40 18 = W25Q128 (16 MB)
                 *   C2 20 15 = MX25L16 (2 MB)
                 *   1F 45 01 = AT25SF081
                 * --------------------------------------------------------- */
                if(op == 'J') {
                    SPI_CS_Assert();
                    SPI_Transfer(0x9F);
                    UART_Write(SPI_Transfer(0xFF));   /* Manufacturer         */
                    UART_Write(SPI_Transfer(0xFF));   /* Memory Type          */
                    UART_Write(SPI_Transfer(0xFF));   /* Capacity             */
                    SPI_CS_Release();
                    break;
                }

                /* -----------------------------------------------------------
                 * 50 53 — Read Status Register (RDSR)
                 * Formato  : 50 53
                 * Respuesta: [1 byte status]
                 *   bit0=WIP  (1=escribiendo, no enviar comandos)
                 *   bit1=WEL  (1=escritura habilitada tras WREN)
                 *   bit2-4=BP (Block Protect)
                 *   bit7=SRWD (Status Register Write Disable)
                 * --------------------------------------------------------- */
                if(op == 'S') {
                    SPI_CS_Assert();
                    SPI_Transfer(0x05);
                    UART_Write(SPI_Transfer(0xFF));
                    SPI_CS_Release();
                    break;
                }

                /* -----------------------------------------------------------
                 * 50 45 — Chip Erase completo (WREN + 0xC7 + esperar WIP)
                 * Formato  : 50 45
                 * Respuesta: RESP_OK o RESP_ERR (timeout >5s)
                 * ADVERTENCIA: borra TODA la memoria del chip.
                 * --------------------------------------------------------- */
                if(op == 'E') {
                    SPI_CS_Assert(); SPI_Transfer(0x06); SPI_CS_Release();
                    Delay_us();
                    SPI_CS_Assert(); SPI_Transfer(0xC7); SPI_CS_Release();
                    if(SPI_WaitReady()) {
                        LED_Blink(3);
                        UART_Write(RESP_OK);
                    } else {
                        UART_Write(RESP_ERR);
                    }
                    break;
                }

                /* -----------------------------------------------------------
                 * 50 46 — Full Flash Dump (volcado completo automatico)
                 * Formato  : 50 46
                 * Respuesta: [todos los bytes de la Flash] RESP_END
                 *            RESP_ERR si JEDEC no valido o chip ausente
                 *
                 * Funcionamiento:
                 *   1. Lee JEDEC ID
                 *   2. Valida fabricante (!=0x00, !=0xFF) y capacity (0x10..0x1C)
                 *   3. Calcula tamano = 2^capacity bytes
                 *   4. Lee en bloques de 256B con opcode 0x03 (READ DATA)
                 *   5. Envia RESP_END al terminar
                 *
                 * Tiempos estimados a 9600 baud (incluye overhead SPI):
                 *   512 KB -> ~55 s    1 MB  -> ~107 s
                 *   2 MB   -> ~215 s   4 MB  -> ~436 s (~7 min)
                 *   8 MB   -> ~14 min  16 MB -> ~28 min
                 * --------------------------------------------------------- */
                if(op == 'F') {
                    /* Leer JEDEC ID */
                    SPI_CS_Assert();
                    SPI_Transfer(0x9F);
                    jedec_mfr  = SPI_Transfer(0xFF);
                    jedec_type = SPI_Transfer(0xFF);
                    jedec_cap  = SPI_Transfer(0xFF);
                    SPI_CS_Release();
                    (void)jedec_type;   /* No usamos el tipo para calcular    */

                    /* Validar: fabricante conocido, capacity entre 1 y 256 MB */
                    if(jedec_mfr == 0xFF || jedec_mfr == 0x00 ||
                       jedec_cap < 0x10  || jedec_cap > 0x1C) {
                        UART_Write(RESP_ERR);
                        break;
                    }

                    /* flash_size = 2^jedec_cap bytes
                     * Ej: 0x16 -> 1<<22 = 4194304 bytes = 4 MB             */
                    flash_size = (uint32_t)1UL << jedec_cap;

                    LED = 1;
                    byte_addr = 0;

                    while(byte_addr < flash_size) {
                        SPI_CS_Assert();
                        SPI_Transfer(0x03);   /* READ DATA opcode            */
                        SPI_Transfer((uint8_t)((byte_addr >> 16) & 0xFF));
                        SPI_Transfer((uint8_t)((byte_addr >>  8) & 0xFF));
                        SPI_Transfer((uint8_t)( byte_addr        & 0xFF));
                        /* 256 bytes por bloque: 0..254 + ultimo fuera       */
                        for(blk = 0; blk < 255; blk++) {
                            UART_Write(SPI_Transfer(0xFF));
                        }
                        UART_Write(SPI_Transfer(0xFF));   /* byte 256        */
                        SPI_CS_Release();
                        byte_addr += 256;
                    }

                    LED = 0;
                    UART_Write(RESP_END);
                    break;
                }

                /* Parametros comunes para R y W */
                addr_len   = UART_Read();
                spi_opcode = UART_Read();
                addr_hi    = UART_Read();
                addr_md    = UART_Read();
                addr_lo    = UART_Read();
                len_hi     = UART_Read();
                len_lo     = UART_Read();
                if(g_uart_timeout) { UART_Write(RESP_ERR); break; }
                len = ((uint16_t)len_hi << 8) | (uint16_t)len_lo;

                /* -----------------------------------------------------------
                 * 50 52 — SPI Read (lectura parcial con direccion)
                 * Formato  : 50 52 <addr_len> <opcode> <A2> <A1> <A0> <LH> <LL>
                 * Respuesta: [len bytes] RESP_END
                 * Ejemplos:
                 *  Flash 3-bytes, leer 256B desde 0x001000:
                 *    50 52 03 03 00 10 00 01 00
                 *  EEPROM SPI 2-bytes, leer 64B desde 0x0040:
                 *    50 52 02 03 00 00 40 00 40
                 *  EEPROM SPI 1-byte, leer 32B desde 0x80:
                 *    50 52 01 03 00 00 80 00 20
                 * --------------------------------------------------------- */
                if(op == 'R') {
                    LED = 1;
                    SPI_CS_Assert();
                    SPI_Transfer(spi_opcode);
                    if(addr_len >= 3) SPI_Transfer(addr_hi);
                    if(addr_len >= 2) SPI_Transfer(addr_md);
                    SPI_Transfer(addr_lo);
                    for(i = 0; i < len; i++) {
                        UART_Write(SPI_Transfer(0xFF));
                    }
                    SPI_CS_Release();
                    LED = 0;
                    UART_Write(RESP_END);
                }

                /* -----------------------------------------------------------
                 * 50 57 — SPI Write / Page Program
                 * Formato  : 50 57 <addr_len> <opcode> <A2> <A1> <A0> <LH> <LL>
                 *            + [datos...]
                 * Respuesta: RESP_OK  o  RESP_ERR (timeout WIP)
                 * Notas:
                 *   Sector borrado (0xFF) requerido antes de programar
                 *   Flash NOR: max 256 bytes/pagina por comando
                 *   EEPROM SPI (25LCxx): 16-256 bytes/pagina segun modelo
                 *   WREN (0x06) enviado automaticamente
                 * Ejemplo — escribir 4B en 0x000000:
                 *   50 57 03 02 00 00 00 00 04 DE AD BE EF
                 * --------------------------------------------------------- */
                else if(op == 'W') {
                    SPI_CS_Assert(); SPI_Transfer(0x06); SPI_CS_Release();
                    Delay_us();
                    SPI_CS_Assert();
                    SPI_Transfer(spi_opcode);
                    if(addr_len >= 3) SPI_Transfer(addr_hi);
                    if(addr_len >= 2) SPI_Transfer(addr_md);
                    SPI_Transfer(addr_lo);
                    for(i = 0; i < len; i++) {
                        data = UART_Read();
                        SPI_Transfer(data);
                    }
                    SPI_CS_Release();
                    if(SPI_WaitReady()) {
                        LED_Blink(1);
                        UART_Write(RESP_OK);
                    } else {
                        UART_Write(RESP_ERR);
                    }
                }
                else {
                    UART_Write(RESP_ERR);
                }
                break;
            }

            default:
                UART_Write_Text("CMD?\r\n");
                break;
        }
    }
}


/* ============================================================================
 * UART_Init — 9600 baud @ 4 MHz INTRC
 * SPBRG=25, BRGH=1 -> baud = 4000000/(16*26) = 9615 bd (error 0.16%)
 * ========================================================================== */
void UART_Init(void) {
    TRISB |=  0x02;
    TRISB &= ~0x04;
    SPBRG  = 25;
    BRGH   = 1;
    SYNC   = 0;
    TXEN   = 1;
    SPEN   = 1;
    CREN   = 1;
    TXIF   = 0;
    RCIF   = 0;
}

/* ============================================================================
 * UART_Write — Enviar un byte (espera TSR vacio)
 * ========================================================================== */
void UART_Write(uint8_t d) {
    while(!TRMT);
    TXREG = d;
}

/* ============================================================================
 * UART_Read — Leer un byte con timeout por rollover de uint16_t (~262 ms)
 * g_uart_timeout=1 si se agota la espera; retorna 0x00.
 * Cada iter ~4 us @ 4 MHz -> 65536 iter x 4 us = ~262 ms
 * FIX: sin constante externa (eliminaba warning 158 en v2)
 * ========================================================================== */
uint8_t UART_Read(void) {
    uint16_t t = 0;
    UART_Flush_Errors();
    while(!RCIF) {
        /* t++ da vuelta de 65535 a 0 -> timeout natural sin magic number    */
        if(++t == 0) {
            g_uart_timeout = 1;
            return 0x00;
        }
    }
    g_uart_timeout = 0;
    return RCREG;
}

/* ============================================================================
 * UART_Write_Text — Enviar cadena terminada en nulo
 * ========================================================================== */
void UART_Write_Text(const char *s) {
    while(*s) UART_Write((uint8_t)*s++);
}

/* ============================================================================
 * UART_Flush_Errors — Limpiar OERR (Overrun) y FERR (Framing)
 * ========================================================================== */
void UART_Flush_Errors(void) {
    uint8_t dummy;
    if(OERR) { CREN = 0; CREN = 1; }
    if(FERR)  { dummy = RCREG; (void)dummy; }
}

/* ============================================================================
 * Delay_ms — Retardo en ms calibrado para 4 MHz (1 MIPS)
 * ~250 iter x ~4 us = ~1 ms por iteracion externa
 * ========================================================================== */
void Delay_ms(uint16_t ms) {
    uint16_t i;
    uint8_t  j;
    for(i = 0; i < ms; i++) {
        for(j = 0; j < 250; j++) {
            __asm
                nop
                nop
            __endasm;
        }
    }
}

/* ============================================================================
 * Delay_us — Retardo corto ~4 us @ 4 MHz para timing I2C/SPI
 * ========================================================================== */
void Delay_us(void) {
    __asm
        nop
        nop
        nop
        nop
    __endasm;
}

/* ============================================================================
 * LED_Blink — Parpadear el LED N veces (150 ms ON / 100 ms OFF)
 * ========================================================================== */
void LED_Blink(uint8_t times) {
    uint8_t n;
    for(n = 0; n < times; n++) {
        LED = 1; Delay_ms(150);
        LED = 0; Delay_ms(100);
    }
}


/* ============================================================================
 * I2C — Bit-banging open-drain
 * '1' logico: TRIS=1 (alta impedancia, pull-up externo sube la linea)
 * '0' logico: TRIS=0 + LAT=0 (drena a GND)
 * LAT de SDA y SCL siempre = 0; solo se cambia TRIS.
 * ========================================================================== */
void I2C_Init(void) {
    SDA = 0; SCL = 0;
    SDA_DIR = 1;
    SCL_DIR = 1;
}

void I2C_Start(void) {
    SDA_DIR = 1; Delay_us();
    SCL_DIR = 1; Delay_us();
    SDA_DIR = 0; SDA = 0; Delay_us();  /* START: SDA baja con SCL alta       */
    SCL_DIR = 0; SCL = 0; Delay_us();
}

void I2C_Stop(void) {
    SDA_DIR = 0; SDA = 0; Delay_us();
    SCL_DIR = 1; Delay_us();
    SDA_DIR = 1; Delay_us();           /* STOP: SDA sube con SCL alta        */
}

/* Retorna 0=ACK, 1=NACK */
uint8_t I2C_Write_Byte(uint8_t d) {
    uint8_t i, ack;
    for(i = 0; i < 8; i++) {
        if(d & 0x80) { SDA_DIR = 1;           }  /* bit '1': alta impedancia */
        else          { SDA_DIR = 0; SDA = 0; }  /* bit '0': drenar a GND   */
        Delay_us();
        SCL_DIR = 1; Delay_us();
        SCL_DIR = 0; SCL = 0;
        d = (uint8_t)(d << 1);         /* cast explicito: evita warning 158  */
    }
    SDA_DIR = 1;               /* Liberar SDA para que esclavo la controle   */
    Delay_us();
    SCL_DIR = 1; Delay_us();
    ack = SDA;                 /* 0=ACK, 1=NACK                              */
    SCL_DIR = 0; SCL = 0;
    return ack;
}

/* send_ack=1 -> ACK (mas bytes), send_ack=0 -> NACK (ultimo byte) */
uint8_t I2C_Read_Byte(uint8_t send_ack) {
    uint8_t i, dat = 0;
    SDA_DIR = 1;
    for(i = 0; i < 8; i++) {
        dat = (uint8_t)(dat << 1);     /* cast explicito: evita warning 158  */
        Delay_us();
        SCL_DIR = 1; Delay_us();
        if(SDA) dat |= 0x01;
        SCL_DIR = 0; SCL = 0;
    }
    if(send_ack) { SDA_DIR = 0; SDA = 0; }   /* ACK:  bajar SDA             */
    else          { SDA_DIR = 1;         }    /* NACK: liberar SDA           */
    Delay_us();
    SCL_DIR = 1; Delay_us();
    SCL_DIR = 0; SCL = 0;
    SDA_DIR = 1;
    return dat;
}


/* ============================================================================
 * SPI — Bit-banging Modo 0 (CPOL=0, CPHA=0)
 * SCK inactivo=0, MOSI cambia en bajada, MISO se captura en subida.
 * ========================================================================== */
void SPI_Init(void) {
    SPI_CS_DIR   = 0;
    SPI_SCK_DIR  = 0;
    SPI_MOSI_DIR = 0;
    SPI_MISO_DIR = 1;
    SPI_CS   = 1;
    SPI_SCK  = 0;
    SPI_MOSI = 0;
}

/* SPI_Transfer — Full-duplex 8 bits MSB primero.
 * Para solo leer, pasar d=0xFF (dummy TX).
 * FIX warning 158: cast explícito a uint8_t en ambos shifts */
uint8_t SPI_Transfer(uint8_t d) {
    uint8_t i, recv = 0;
    for(i = 0; i < 8; i++) {
        SPI_MOSI = (d & 0x80) ? 1 : 0;
        d    = (uint8_t)(d    << 1);   /* cast explicito: evita warning 158  */
        Delay_us();
        SPI_SCK = 1;
        Delay_us();
        recv = (uint8_t)(recv << 1);   /* cast explicito: evita warning 158  */
        if(SPI_MISO) recv |= 0x01;
        SPI_SCK = 0;
    }
    return recv;
}

/* SPI_WaitReady — Esperar que WIP (bit0 Status Register) se limpie.
 * Retorna 1=listo, 0=timeout. Maximo SPI_WIP_TIMEOUT ms.          */
uint8_t SPI_WaitReady(void) {
    uint16_t t = 0;
    uint8_t  st;
    do {
        SPI_CS_Assert();
        SPI_Transfer(0x05);
        st = SPI_Transfer(0xFF);
        SPI_CS_Release();
        Delay_ms(1);
    } while((st & 0x01) && (++t < SPI_WIP_TIMEOUT));
    return (t < SPI_WIP_TIMEOUT) ? 1 : 0;
}

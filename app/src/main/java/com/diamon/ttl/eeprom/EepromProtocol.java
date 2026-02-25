package com.diamon.ttl.eeprom;

public interface EepromProtocol {

    /**
     * Devuelve el prefijo de comando (e.g. 'I' para I2C, 'P' para SPI)
     */
    byte getCommandPrefix();

    /**
     * Construye el comando de lectura (incluye opcode, dirección, longitud, etc.)
     * 
     * @param address    La dirección actual
     * @param length     Cantidad de bytes a leer en este bloque
     * @param modelIndex El índice del spinner de modelo
     * @return El arreglo de bytes listo para enviarse por USB
     */
    byte[] buildReadCommand(int address, int length, int modelIndex);

    /**
     * Construye el comando de escritura base (opcode, dirección, longitud)
     * (Usualmente se le anexan los datos reales a este byte array después)
     * 
     * @param address    La dirección actual
     * @param length     Cantidad de bytes a escribir
     * @param modelIndex El índice del modelo de EEPROM
     * @return El ByteArray base estructurado (sin el payload)
     */
    byte[] buildWriteCommandBase(int address, int length, int modelIndex);

    /**
     * Obtiene el tamaño de la página física dependienta del chip
     * 
     * @param modelIndex Índice del modelo seleccionado
     * @return El tamaño de página límite en bytes
     */
    int getPageSize(int modelIndex);

    /**
     * Devuelve la cantidad límite de memoria del chip seleccionado
     */
    int getTotalSize(int modelIndex);

    /**
     * Instrucciones de hardware ricas en texto
     */
    String getHardwareInstructions();
}

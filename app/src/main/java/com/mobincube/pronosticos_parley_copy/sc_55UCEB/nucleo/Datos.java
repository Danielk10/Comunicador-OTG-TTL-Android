package com.mobincube.pronosticos_parley_copy.sc_55UCEB.nucleo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface Datos {

    public InputStream leerDatoInterno(String nombre) throws IOException;

    public OutputStream escribirDatoInterno(String nombre) throws IOException;

    public InputStream leerAsset(String nombre) throws IOException;
}

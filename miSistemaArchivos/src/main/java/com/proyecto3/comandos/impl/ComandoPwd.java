package com.proyecto3.comandos.impl;

import com.proyecto3.comandos.Comando;
import com.proyecto3.nucleo.Directorio;
import com.proyecto3.sesion.Sesion;

public class ComandoPwd implements Comando {

    private final Sesion sesion;

    public ComandoPwd(Sesion sesion) {
        this.sesion = sesion;
    }

    @Override
    public String getNombre() { return "pwd"; }

    @Override
    public String getAyuda() {
        return "pwd - Muestra el directorio de trabajo actual";
    }

    @Override
    public String ejecutar(String[] args) {
        if (!sesion.estaAutenticado()) return "No hay sesión activa";

        try {
            Directorio dirActual = new Directorio(sesion.getDisco(), sesion.getAsignador(),
                sesion.getTablaInodos(), sesion.getInodoDirectorioTrabajo());
            return dirActual.obtenerRutaAbsoluta(sesion.getSuperbloque());
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}

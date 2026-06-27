package com.proyecto3.comandos.impl;

import com.proyecto3.comandos.Comando;
import com.proyecto3.nucleo.*;
import com.proyecto3.sesion.Sesion;

public class ComandoCd implements Comando {

    private final Sesion sesion;

    public ComandoCd(Sesion sesion) {
        this.sesion = sesion;
    }

    @Override
    public String getNombre() { return "cd"; }

    @Override
    public String getAyuda() {
        return "cd <ruta> - Cambia el directorio de trabajo";
    }

    @Override
    public String ejecutar(String[] args) {
        if (!sesion.estaAutenticado()) return "No hay sesión activa";
        if (args.length < 1) return "Uso: " + getAyuda();

        String ruta = args[0];

        try {
            Directorio dirActual = new Directorio(sesion.getDisco(), sesion.getAsignador(),
                sesion.getTablaInodos(), sesion.getInodoDirectorioTrabajo());

            int nuevoInodo = dirActual.navegar(ruta, sesion.getSuperbloque());
            sesion.setInodoDirectorioTrabajo(nuevoInodo);

            Directorio dirDestino = new Directorio(sesion.getDisco(), sesion.getAsignador(),
                sesion.getTablaInodos(), nuevoInodo);
            return dirDestino.obtenerRutaAbsoluta(sesion.getSuperbloque());
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}

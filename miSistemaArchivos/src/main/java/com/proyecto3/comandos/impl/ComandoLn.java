package com.proyecto3.comandos.impl;

import com.proyecto3.comandos.Comando;
import com.proyecto3.nucleo.*;
import com.proyecto3.sesion.Sesion;

public class ComandoLn implements Comando {

    private final Sesion sesion;

    public ComandoLn(Sesion sesion) {
        this.sesion = sesion;
    }

    @Override
    public String getNombre() { return "ln"; }

    @Override
    public String getAyuda() {
        return "ln <origen> <destino> - Crea un enlace duro al archivo origen";
    }

    @Override
    public String ejecutar(String[] args) {
        if (!sesion.estaAutenticado()) return "No hay sesión activa";
        if (args.length < 2) return "Uso: " + getAyuda();

        String rutaOrigen = args[0];
        String rutaDestino = args[1];

        try {
            Directorio dirActual = new Directorio(sesion.getDisco(), sesion.getAsignador(),
                sesion.getTablaInodos(), sesion.getInodoDirectorioTrabajo());

            String nombreOrigen;
            int inodoDirOrigen;

            int lastSepO = Math.max(rutaOrigen.lastIndexOf('/'), rutaOrigen.lastIndexOf('\\'));
            if (lastSepO >= 0) {
                nombreOrigen = rutaOrigen.substring(lastSepO + 1);
                String rutaDir = rutaOrigen.substring(0, lastSepO);
                if (rutaDir.isEmpty()) rutaDir = "/";
                inodoDirOrigen = dirActual.navegar(rutaDir, sesion.getSuperbloque());
            } else {
                nombreOrigen = rutaOrigen;
                inodoDirOrigen = sesion.getInodoDirectorioTrabajo();
            }

            Directorio dirOrigen = new Directorio(sesion.getDisco(), sesion.getAsignador(),
                sesion.getTablaInodos(), inodoDirOrigen);

            EntradaDirectorio entradaOrigen = dirOrigen.buscarEntrada(nombreOrigen);
            if (entradaOrigen == null) return "Error: archivo origen no encontrado: " + rutaOrigen;

            Inodo inodoOrigen = sesion.getTablaInodos().getInodo(entradaOrigen.getNumeroInodo());
            if (inodoOrigen.esDirectorio()) {
                return "Error: no se pueden crear enlaces duros a directorios";
            }

            String nombreDestino;
            int inodoDirDestino;

            int lastSepD = Math.max(rutaDestino.lastIndexOf('/'), rutaDestino.lastIndexOf('\\'));
            if (lastSepD >= 0) {
                nombreDestino = rutaDestino.substring(lastSepD + 1);
                String rutaDir = rutaDestino.substring(0, lastSepD);
                if (rutaDir.isEmpty()) rutaDir = "/";
                inodoDirDestino = dirActual.navegar(rutaDir, sesion.getSuperbloque());
            } else {
                nombreDestino = rutaDestino;
                inodoDirDestino = sesion.getInodoDirectorioTrabajo();
            }

            Directorio dirDestino = new Directorio(sesion.getDisco(), sesion.getAsignador(),
                sesion.getTablaInodos(), inodoDirDestino);

            if (dirDestino.buscarEntrada(nombreDestino) != null) {
                return "Error: '" + nombreDestino + "' ya existe";
            }

            dirDestino.agregarEntrada(nombreDestino, inodoOrigen.getNumero());
            dirDestino.guardar();

            inodoOrigen.setEnlaces(inodoOrigen.getEnlaces() + 1);
            inodoOrigen.setFechaModificacion(System.currentTimeMillis());
            sesion.getTablaInodos().guardarEnDisco();

            return "Enlace '" + nombreDestino + "' -> inodo " + inodoOrigen.getNumero()
                + " (" + inodoOrigen.getEnlaces() + " enlaces)";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}

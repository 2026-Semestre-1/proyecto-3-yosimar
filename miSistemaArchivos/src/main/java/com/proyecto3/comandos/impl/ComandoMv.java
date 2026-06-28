package com.proyecto3.comandos.impl;

import com.proyecto3.comandos.Comando;
import com.proyecto3.nucleo.*;
import com.proyecto3.sesion.Sesion;

public class ComandoMv implements Comando {

    private final Sesion sesion;

    public ComandoMv(Sesion sesion) {
        this.sesion = sesion;
    }

    @Override
    public String getNombre() { return "mv"; }

    @Override
    public String getAyuda() {
        return "mv <origen> <destino> - Mueve/renombra un archivo o directorio";
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

            if (nombreOrigen.isEmpty()) return "Error: ruta origen inválida";

            Directorio dirOrigen = new Directorio(sesion.getDisco(), sesion.getAsignador(),
                sesion.getTablaInodos(), inodoDirOrigen);

            EntradaDirectorio entradaOrigen = dirOrigen.buscarEntrada(nombreOrigen);
            if (entradaOrigen == null) return "Error: '" + rutaOrigen + "' no encontrado";

            Inodo inodoOrigen = sesion.getTablaInodos().getInodo(entradaOrigen.getNumeroInodo());

            String nombreDestino;
            int inodoDirDestino;
            int lastSepD = Math.max(rutaDestino.lastIndexOf('/'), rutaDestino.lastIndexOf('\\'));

            if (lastSepD >= 0) {
                nombreDestino = rutaDestino.substring(lastSepD + 1);
                String rutaDirDestino = rutaDestino.substring(0, lastSepD);
                if (rutaDirDestino.isEmpty()) rutaDirDestino = "/";
                inodoDirDestino = dirActual.navegar(rutaDirDestino, sesion.getSuperbloque());
            } else {
                nombreDestino = rutaDestino;
                inodoDirDestino = sesion.getInodoDirectorioTrabajo();
            }

            boolean moverADirectorio = false;
            int inodoDestFinal = inodoDirDestino;
            String nombreFinal = nombreDestino;

            if (nombreDestino.isEmpty()) {
                moverADirectorio = true;
                nombreFinal = nombreOrigen;
            } else {
                Directorio dirDestinoTmp = new Directorio(sesion.getDisco(), sesion.getAsignador(),
                    sesion.getTablaInodos(), inodoDirDestino);
                EntradaDirectorio entradaDest = dirDestinoTmp.buscarEntrada(nombreDestino);
                if (entradaDest != null) {
                    Inodo inodoDest = sesion.getTablaInodos().getInodo(entradaDest.getNumeroInodo());
                    if (inodoDest.esDirectorio()) {
                        moverADirectorio = true;
                        inodoDestFinal = inodoDest.getNumero();
                        nombreFinal = nombreOrigen;
                    }
                }
            }

            if (moverADirectorio) {
                Directorio dirDestino = new Directorio(sesion.getDisco(), sesion.getAsignador(),
                    sesion.getTablaInodos(), inodoDestFinal);
                if (dirDestino.buscarEntrada(nombreFinal) != null) {
                    return "Error: '" + nombreFinal + "' ya existe en el destino";
                }

                dirOrigen.eliminarEntrada(nombreOrigen);
                dirOrigen.guardar();

                dirDestino.agregarEntrada(nombreFinal, inodoOrigen.getNumero());
                dirDestino.guardar();

                if (inodoOrigen.esDirectorio()) {
                    Directorio dirMovido = new Directorio(sesion.getDisco(), sesion.getAsignador(),
                        sesion.getTablaInodos(), inodoOrigen.getNumero());
                    dirMovido.actualizarPadre(inodoDestFinal);
                    dirMovido.guardar();
                }

                return "'" + nombreOrigen + "' movido a directorio destino";
            }

            if (inodoDirOrigen == inodoDirDestino) {
                EntradaDirectorio existente = dirOrigen.buscarEntrada(nombreDestino);
                if (existente != null && !existente.getNombre().equals(nombreOrigen)) {
                    dirOrigen.eliminarEntrada(nombreDestino);
                }
                dirOrigen.eliminarEntrada(nombreOrigen);
                dirOrigen.agregarEntrada(nombreDestino, inodoOrigen.getNumero());
                dirOrigen.guardar();
            } else {
                Directorio dirDestino = new Directorio(sesion.getDisco(), sesion.getAsignador(),
                    sesion.getTablaInodos(), inodoDirDestino);

                EntradaDirectorio existente = dirDestino.buscarEntrada(nombreDestino);
                if (existente != null) {
                    dirDestino.eliminarEntrada(nombreDestino);
                }

                dirOrigen.eliminarEntrada(nombreOrigen);
                dirOrigen.guardar();

                dirDestino.agregarEntrada(nombreDestino, inodoOrigen.getNumero());
                dirDestino.guardar();

                if (inodoOrigen.esDirectorio()) {
                    Directorio dirMovido = new Directorio(sesion.getDisco(), sesion.getAsignador(),
                        sesion.getTablaInodos(), inodoOrigen.getNumero());
                    dirMovido.actualizarPadre(inodoDirDestino);
                    dirMovido.guardar();
                }
            }

            return "'" + rutaOrigen + "' movido a '" + rutaDestino + "'";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}

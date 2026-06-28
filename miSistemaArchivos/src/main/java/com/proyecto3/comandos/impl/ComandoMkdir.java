package com.proyecto3.comandos.impl;

import com.proyecto3.comandos.Comando;
import com.proyecto3.nucleo.*;
import com.proyecto3.sesion.Sesion;

public class ComandoMkdir implements Comando {

    private final Sesion sesion;

    public ComandoMkdir(Sesion sesion) {
        this.sesion = sesion;
    }

    @Override
    public String getNombre() { return "mkdir"; }

    @Override
    public String getAyuda() {
        return "mkdir <ruta> - Crea un nuevo directorio";
    }

    @Override
    public String ejecutar(String[] args) {
        if (!sesion.estaAutenticado()) return "No hay sesión activa";
        if (args.length < 1) return "Uso: " + getAyuda();

        String ruta = args[0];

        try {
            Directorio dirActual = new Directorio(sesion.getDisco(), sesion.getAsignador(),
                sesion.getTablaInodos(), sesion.getInodoDirectorioTrabajo());

            String nombreDir;
            int inodoPadre;

            int lastSep = Math.max(ruta.lastIndexOf('/'), ruta.lastIndexOf('\\'));
            if (lastSep >= 0) {
                nombreDir = ruta.substring(lastSep + 1);
                String rutaPadre = ruta.substring(0, lastSep);
                if (rutaPadre.isEmpty()) rutaPadre = "/";
                inodoPadre = dirActual.navegar(rutaPadre, sesion.getSuperbloque());
            } else {
                nombreDir = ruta;
                inodoPadre = sesion.getInodoDirectorioTrabajo();
            }

            if (nombreDir.isEmpty()) return "Error: nombre de directorio vacío";

            Directorio dirPadre = new Directorio(sesion.getDisco(), sesion.getAsignador(),
                sesion.getTablaInodos(), inodoPadre);

            if (dirPadre.buscarEntrada(nombreDir) != null) {
                return "Error: '" + nombreDir + "' ya existe";
            }

            Inodo nuevoInodo = sesion.getTablaInodos().asignarInodo();
            nuevoInodo.setTipo(Inodo.DIRECTORIO);
            nuevoInodo.setUid(sesion.getUsuarioActual().getUid());
            nuevoInodo.setGid(sesion.getUsuarioActual().getGid());
            nuevoInodo.setPermisos((short) 0755);

            Directorio nuevoDir = new Directorio(sesion.getDisco(), sesion.getAsignador(),
                sesion.getTablaInodos(), nuevoInodo.getNumero());
            nuevoDir.inicializarDirectorio(inodoPadre);
            nuevoDir.guardar();

            dirPadre.agregarEntrada(nombreDir, nuevoInodo.getNumero());
            dirPadre.guardar();

            return "Directorio '" + nombreDir + "' creado";
        } catch (Exception e) {
            return "Error al crear directorio: " + e.getMessage();
        }
    }
}

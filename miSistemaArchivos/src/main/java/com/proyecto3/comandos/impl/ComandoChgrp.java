package com.proyecto3.comandos.impl;

import com.proyecto3.comandos.Comando;
import com.proyecto3.nucleo.*;
import com.proyecto3.seguridad.Grupo;
import com.proyecto3.sesion.Sesion;

public class ComandoChgrp implements Comando {

    private final Sesion sesion;

    public ComandoChgrp(Sesion sesion) {
        this.sesion = sesion;
    }

    @Override
    public String getNombre() { return "chgrp"; }

    @Override
    public String getAyuda() {
        return "chgrp <grupo> <ruta> - Cambia el grupo de un archivo/directorio";
    }

    @Override
    public String ejecutar(String[] args) {
        if (!sesion.estaAutenticado()) return "No hay sesión activa";
        if (!sesion.esRoot()) return "chgrp: solo root puede cambiar el grupo";
        if (args.length < 2) return "Uso: " + getAyuda();

        String nombreGrupo = args[0];
        String ruta = args[1];

        try {
            Grupo nuevoGrupo = sesion.getGestorUsuarios().getGrupoPorNombre(nombreGrupo);
            if (nuevoGrupo == null) return "Error: grupo no encontrado: " + nombreGrupo;

            Directorio dirActual = new Directorio(sesion.getDisco(), sesion.getAsignador(),
                sesion.getTablaInodos(), sesion.getInodoDirectorioTrabajo());

            String nombreArchivo;
            int inodoDir;

            int lastSep = Math.max(ruta.lastIndexOf('/'), ruta.lastIndexOf('\\'));
            if (lastSep >= 0) {
                nombreArchivo = ruta.substring(lastSep + 1);
                String rutaDir = ruta.substring(0, lastSep);
                if (rutaDir.isEmpty()) rutaDir = "/";
                inodoDir = dirActual.navegar(rutaDir, sesion.getSuperbloque());
            } else {
                nombreArchivo = ruta;
                inodoDir = sesion.getInodoDirectorioTrabajo();
            }

            Directorio dirArchivo = new Directorio(sesion.getDisco(), sesion.getAsignador(),
                sesion.getTablaInodos(), inodoDir);

            EntradaDirectorio entrada = dirArchivo.buscarEntrada(nombreArchivo);
            if (entrada == null) return "Error: archivo no encontrado: " + ruta;

            Inodo inodo = sesion.getTablaInodos().getInodo(entrada.getNumeroInodo());

            int gidAnterior = inodo.getGid();
            inodo.setGid(nuevoGrupo.getGid());
            inodo.setFechaModificacion(System.currentTimeMillis());
            sesion.getTablaInodos().guardarEnDisco();

            return "Grupo de '" + nombreArchivo + "' cambiado de gid " + gidAnterior
                + " a " + nombreGrupo + " (gid " + nuevoGrupo.getGid() + ")";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}

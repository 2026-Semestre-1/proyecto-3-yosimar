package com.proyecto3.comandos.impl;

import com.proyecto3.comandos.Comando;
import com.proyecto3.nucleo.*;
import com.proyecto3.sesion.Sesion;

public class ComandoTouch implements Comando {

    private final Sesion sesion;

    public ComandoTouch(Sesion sesion) {
        this.sesion = sesion;
    }

    @Override
    public String getNombre() { return "touch"; }

    @Override
    public String getAyuda() {
        return "touch <ruta> - Crea un archivo vacío";
    }

    @Override
    public String ejecutar(String[] args) {
        if (!sesion.estaAutenticado()) return "No hay sesión activa";
        if (args.length < 1) return "Uso: " + getAyuda();

        String ruta = args[0];

        try {
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

            if (nombreArchivo.isEmpty()) return "Error: nombre de archivo vacío";

            Directorio dirDestino = new Directorio(sesion.getDisco(), sesion.getAsignador(),
                sesion.getTablaInodos(), inodoDir);

            Inodo inodoPadre = sesion.getTablaInodos().getInodo(inodoDir);
            if (!PermisoUtil.verificar(inodoPadre, sesion, PermisoUtil.BIT_ESCRITURA)) {
                return "touch: permiso denegado";
            }

            if (dirDestino.buscarEntrada(nombreArchivo) != null) {
                return "Error: '" + nombreArchivo + "' ya existe";
            }

            Inodo nuevoInodo = sesion.getTablaInodos().asignarInodo();
            nuevoInodo.setTipo(Inodo.ARCHIVO);
            nuevoInodo.setUid(sesion.getUsuarioActual().getUid());
            nuevoInodo.setGid(sesion.getUsuarioActual().getGid());
            nuevoInodo.setPermisos((short) 064);
            nuevoInodo.setTamanio(0);

            dirDestino.agregarEntrada(nombreArchivo, nuevoInodo.getNumero());
            dirDestino.guardar();

            sesion.getTablaArchivosAbiertos().abrir(nuevoInodo.getNumero(),
                TablaArchivosAbiertos.MODO_ESCRITURA,
                sesion.getUsuarioActual().getNombre(), ruta);

            return "Archivo '" + nombreArchivo + "' creado (inodo " + nuevoInodo.getNumero() + ")";
        } catch (Exception e) {
            return "Error al crear archivo: " + e.getMessage();
        }
    }
}

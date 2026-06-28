package com.proyecto3.comandos.impl;

import com.proyecto3.comandos.Comando;
import com.proyecto3.nucleo.*;
import com.proyecto3.seguridad.Usuario;
import com.proyecto3.sesion.Sesion;

public class ComandoChown implements Comando {

    private final Sesion sesion;

    public ComandoChown(Sesion sesion) {
        this.sesion = sesion;
    }

    @Override
    public String getNombre() { return "chown"; }

    @Override
    public String getAyuda() {
        return "chown <usuario> <ruta> - Cambia el dueño de un archivo/directorio";
    }

    @Override
    public String ejecutar(String[] args) {
        if (!sesion.estaAutenticado()) return "No hay sesión activa";
        if (!sesion.esRoot()) return "Error: solo root puede cambiar el dueño";
        if (args.length < 2) return "Uso: " + getAyuda();

        String nombreUsuario = args[0];
        String ruta = args[1];

        try {
            Usuario nuevoDueno = sesion.getGestorUsuarios().getUsuarioPorNombre(nombreUsuario);
            if (nuevoDueno == null) return "Error: usuario no encontrado: " + nombreUsuario;

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
            int uidAnterior = inodo.getUid();
            inodo.setUid(nuevoDueno.getUid());
            inodo.setFechaModificacion(System.currentTimeMillis());
            sesion.getTablaInodos().guardarEnDisco();

            return "Dueño de '" + nombreArchivo + "' cambiado de uid " + uidAnterior
                + " a " + nombreUsuario + " (uid " + nuevoDueno.getUid() + ")";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}

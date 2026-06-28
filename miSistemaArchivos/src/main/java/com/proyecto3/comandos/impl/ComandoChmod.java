package com.proyecto3.comandos.impl;

import com.proyecto3.comandos.Comando;
import com.proyecto3.nucleo.*;
import com.proyecto3.sesion.Sesion;

public class ComandoChmod implements Comando {

    private final Sesion sesion;

    public ComandoChmod(Sesion sesion) {
        this.sesion = sesion;
    }

    @Override
    public String getNombre() { return "chmod"; }

    @Override
    public String getAyuda() {
        return "chmod <modo> <ruta> - Cambia los permisos de un archivo/directorio (ej: 755)";
    }

    @Override
    public String ejecutar(String[] args) {
        if (!sesion.estaAutenticado()) return "No hay sesión activa";
        if (args.length < 2) return "Uso: " + getAyuda();

        String modoStr = args[0];
        String ruta = args[1];

        try {
            short modo;
            try {
                modo = Short.parseShort(modoStr, 8);
            } catch (NumberFormatException e) {
                return "Error: modo invalido '" + modoStr + "'. Use octal (ej: 755)";
            }
            if (modo < 0 || modo > 077) {
                return "Error: modo invalido. Use octal (ej: 755)";
            }

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

            if (inodo.getUid() != sesion.getUsuarioActual().getUid() && !sesion.esRoot()) {
                return "Error: solo el dueño o root puede cambiar permisos";
            }

            inodo.setPermisos(modo);
            inodo.setFechaModificacion(System.currentTimeMillis());
            sesion.getTablaInodos().guardarEnDisco();

            return "Permisos de '" + nombreArchivo + "' cambiados a " + modoStr;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}

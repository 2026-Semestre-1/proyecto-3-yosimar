package com.proyecto3.comandos.impl;

import com.proyecto3.comandos.Comando;
import com.proyecto3.nucleo.*;
import com.proyecto3.sesion.Sesion;

public class ComandoLess implements Comando {

    private final Sesion sesion;

    public ComandoLess(Sesion sesion) {
        this.sesion = sesion;
    }

    @Override
    public String getNombre() { return "less"; }

    @Override
    public String getAyuda() {
        return "less <ruta> - Muestra el contenido de un archivo con paginación";
    }

    @Override
    public String ejecutar(String[] args) {
        if (!sesion.estaAutenticado()) return "No hay sesión activa";
        if (args.length < 1) return "Uso: " + getAyuda();

        try {
            Directorio dirActual = new Directorio(sesion.getDisco(), sesion.getAsignador(),
                sesion.getTablaInodos(), sesion.getInodoDirectorioTrabajo());

            String ruta = args[0];
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
            if (entrada == null) {
                return "Error: archivo no encontrado: " + ruta;
            }

            Inodo inodo = sesion.getTablaInodos().getInodo(entrada.getNumeroInodo());
            if (!inodo.esArchivo()) {
                return "Error: '" + nombreArchivo + "' no es un archivo";
            }

            if (!PermisoUtil.verificar(inodo, sesion, PermisoUtil.BIT_LECTURA)) {
                return "less: permiso denegado";
            }

            sesion.getTablaArchivosAbiertos().abrir(inodo.getNumero(),
                TablaArchivosAbiertos.MODO_LECTURA,
                sesion.getUsuarioActual().getNombre(), ruta);

            String contenido = GestorArchivos.leerDatosComoTexto(inodo, sesion.getDisco(),
                sesion.getAsignador());

            StringBuilder sb = new StringBuilder();
            sb.append("------ ").append(nombreArchivo);
            sb.append(" (").append(inodo.getTamanio()).append(" bytes) ------\n");
            if (!contenido.isEmpty()) {
                sb.append(contenido);
            } else {
                sb.append("(archivo vacio)\n");
            }

            int lineas = Math.max(1, contenido.split("\n", -1).length);
            sb.append("\n------ ").append(lineas).append(" linea(s) ------");

            return sb.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}

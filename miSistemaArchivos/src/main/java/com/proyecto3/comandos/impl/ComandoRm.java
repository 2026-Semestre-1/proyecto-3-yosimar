package com.proyecto3.comandos.impl;

import com.proyecto3.comandos.Comando;
import com.proyecto3.nucleo.*;
import com.proyecto3.sesion.Sesion;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ComandoRm implements Comando {

    private final Sesion sesion;

    public ComandoRm(Sesion sesion) {
        this.sesion = sesion;
    }

    @Override
    public String getNombre() { return "rm"; }

    @Override
    public String getAyuda() {
        return "rm [-R] <nombre|patrón|ruta> - Elimina archivos/directorios. -R para recursivo";
    }

    @Override
    public String ejecutar(String[] args) {
        if (!sesion.estaAutenticado()) return "No hay sesión activa";

        boolean recursivo = false;
        List<String> nombres = new ArrayList<>();
        for (String arg : args) {
            if ("-R".equals(arg)) {
                recursivo = true;
            } else {
                nombres.add(arg);
            }
        }

        if (nombres.isEmpty()) return "Uso: " + getAyuda();

        StringBuilder resultado = new StringBuilder();
        try {
            Directorio dirActual = new Directorio(sesion.getDisco(), sesion.getAsignador(),
                sesion.getTablaInodos(), sesion.getInodoDirectorioTrabajo());

            Inodo inodoCwd = sesion.getTablaInodos().getInodo(sesion.getInodoDirectorioTrabajo());
            if (!PermisoUtil.verificar(inodoCwd, sesion, PermisoUtil.BIT_ESCRITURA)) {
                return "rm: permiso denegado";
            }

            for (String patron : nombres) {
            if (".".equals(patron) || "..".equals(patron)) {
                resultado.append("Error: no se puede borrar '").append(patron).append("'\n");
                continue;
            }
            if ("/".equals(patron)) {
                resultado.append("Error: no se puede borrar el directorio raíz\n");
                continue;
            }
                if (patron.contains("/") || patron.contains("\\")) {
                    eliminarPorRuta(dirActual, patron, recursivo, resultado);
                } else {
                    List<EntradaDirectorio> todas = dirActual.listarEntradas();
                    List<EntradaDirectorio> coincidencias = buscarCoincidencias(todas, patron);
                    if (coincidencias.isEmpty()) {
                        resultado.append("Sin coincidencias para: ").append(patron).append("\n");
                        continue;
                    }
                    for (EntradaDirectorio entrada : coincidencias) {
                        eliminarEntrada(dirActual, entrada, recursivo, resultado);
                    }
                }
            }

            dirActual.guardar();
            return resultado.toString().trim();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private void eliminarPorRuta(Directorio dirActual, String ruta, boolean recursivo,
                                  StringBuilder resultado) throws Exception {
        int lastSep = Math.max(ruta.lastIndexOf('/'), ruta.lastIndexOf('\\'));
        if (lastSep < 0) return;

        String nombreArchivo = ruta.substring(lastSep + 1);
        String rutaDir = ruta.substring(0, lastSep);
        if (rutaDir.isEmpty()) rutaDir = "/";

        int inodoDir = dirActual.navegar(rutaDir, sesion.getSuperbloque());
        Directorio dirPadre = new Directorio(sesion.getDisco(), sesion.getAsignador(),
            sesion.getTablaInodos(), inodoDir);

        EntradaDirectorio entrada = dirPadre.buscarEntrada(nombreArchivo);
        if (entrada == null) {
            resultado.append("Sin coincidencias para: ").append(ruta).append("\n");
            return;
        }

        eliminarEntrada(dirPadre, entrada, recursivo, resultado);
        dirPadre.guardar();
    }

    private List<EntradaDirectorio> buscarCoincidencias(List<EntradaDirectorio> entradas, String patron) {
        List<EntradaDirectorio> result = new ArrayList<>();
        String regex = patron
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".");
        Pattern p = Pattern.compile("^" + regex + "$");

        for (EntradaDirectorio e : entradas) {
            if (".".equals(e.getNombre()) || "..".equals(e.getNombre())) continue;
            if (p.matcher(e.getNombre()).matches()) {
                result.add(e);
            }
        }
        return result;
    }

    private void eliminarEntrada(Directorio dir, EntradaDirectorio entrada, boolean recursivo,
                                  StringBuilder resultado) throws Exception {
        Inodo inodo = sesion.getTablaInodos().getInodo(entrada.getNumeroInodo());

        if (inodo.esDirectorio()) {
            if (!recursivo) {
                resultado.append("Error: '").append(entrada.getNombre())
                    .append("' es un directorio. Use -R para eliminar recursivo\n");
                return;
            }

            Directorio subDir = new Directorio(sesion.getDisco(), sesion.getAsignador(),
                sesion.getTablaInodos(), entrada.getNumeroInodo());
            List<EntradaDirectorio> subEntradas = new ArrayList<>(subDir.listarEntradas());

            for (EntradaDirectorio sub : subEntradas) {
                if (".".equals(sub.getNombre()) || "..".equals(sub.getNombre())) continue;
                eliminarEntrada(subDir, sub, true, resultado);
            }

            liberarBloquesInodo(inodo);
            sesion.getTablaInodos().liberarInodo(inodo.getNumero());
            dir.eliminarEntrada(entrada.getNombre());
            resultado.append("Eliminado directorio: ").append(entrada.getNombre()).append("\n");
        } else {
            int enlaces = inodo.getEnlaces() - 1;
            if (enlaces > 0) {
                inodo.setEnlaces(enlaces);
                inodo.setFechaModificacion(System.currentTimeMillis());
                dir.eliminarEntrada(entrada.getNombre());
                resultado.append("Eliminado enlace: ").append(entrada.getNombre())
                    .append(" (").append(enlaces).append(" enlaces restantes)\n");
            } else {
                liberarBloquesInodo(inodo);
                sesion.getTablaInodos().liberarInodo(inodo.getNumero());
                dir.eliminarEntrada(entrada.getNombre());
                resultado.append("Eliminado: ").append(entrada.getNombre()).append("\n");
            }
        }
    }

    private void liberarBloquesInodo(Inodo inodo) throws Exception {
        for (int i = 0; i < Inodo.PUNTEROS_DIRECTOS; i++) {
            int bloque = inodo.getPunteroDirecto(i);
            if (bloque != Inodo.BLOQUE_NULO) {
                sesion.getAsignador().liberar(bloque);
            }
        }
        sesion.getAsignador().guardarEnDisco();
    }
}

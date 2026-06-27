package com.proyecto3.comandos.impl;

import com.proyecto3.comandos.Comando;
import com.proyecto3.nucleo.*;
import com.proyecto3.sesion.Sesion;
import java.util.ArrayList;
import java.util.List;

public class ComandoLs implements Comando {

    private final Sesion sesion;

    public ComandoLs(Sesion sesion) {
        this.sesion = sesion;
    }

    @Override
    public String getNombre() { return "ls"; }

    @Override
    public String getAyuda() {
        return "ls [-R] [ruta] - Lista el contenido del directorio";
    }

    @Override
    public String ejecutar(String[] args) {
        if (!sesion.estaAutenticado()) return "No hay sesión activa";

        boolean recursivo = false;
        String ruta = null;
        for (String arg : args) {
            if ("-R".equals(arg)) {
                recursivo = true;
            } else {
                ruta = arg;
            }
        }

        try {
            int inodoObjetivo = sesion.getInodoDirectorioTrabajo();
            if (ruta != null) {
                Directorio dirActual = new Directorio(sesion.getDisco(), sesion.getAsignador(),
                    sesion.getTablaInodos(), sesion.getInodoDirectorioTrabajo());
                inodoObjetivo = dirActual.navegar(ruta, sesion.getSuperbloque());
            }

            StringBuilder sb = new StringBuilder();
            listarDirectorio(inodoObjetivo, "", recursivo, sb);
            return sb.toString().trim();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private void listarDirectorio(int numInodo, String prefijo, boolean recursivo,
                                   StringBuilder sb) throws Exception {
        Directorio dir = new Directorio(sesion.getDisco(), sesion.getAsignador(),
            sesion.getTablaInodos(), numInodo);
        List<EntradaDirectorio> entradas = dir.listarEntradas();

        List<EntradaDirectorio> normales = new ArrayList<>();
        List<EntradaDirectorio> subdirs = new ArrayList<>();
        for (EntradaDirectorio e : entradas) {
            if (".".equals(e.getNombre()) || "..".equals(e.getNombre())) continue;
            Inodo inodo = sesion.getTablaInodos().getInodo(e.getNumeroInodo());
            if (inodo.esDirectorio()) {
                subdirs.add(e);
            } else {
                normales.add(e);
            }
        }

        for (EntradaDirectorio e : normales) {
            Inodo inodo = sesion.getTablaInodos().getInodo(e.getNumeroInodo());
            sb.append(prefijo).append(formatoEntrada(e, inodo)).append("\n");
        }
        for (EntradaDirectorio e : subdirs) {
            Inodo inodo = sesion.getTablaInodos().getInodo(e.getNumeroInodo());
            sb.append(prefijo).append(formatoEntrada(e, inodo)).append("\n");
            if (recursivo) {
                listarDirectorio(e.getNumeroInodo(), prefijo + "  ", true, sb);
            }
        }
    }

    private String formatoEntrada(EntradaDirectorio e, Inodo inodo) {
        String tipo = inodo.esDirectorio() ? "d" : "-";
        String permisos = String.format("%03o", inodo.getPermisos());
        return String.format("%s %s %5d %s", tipo, permisos, inodo.getTamanio(), e.getNombre());
    }
}

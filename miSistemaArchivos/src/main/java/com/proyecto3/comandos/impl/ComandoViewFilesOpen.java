package com.proyecto3.comandos.impl;

import com.proyecto3.comandos.Comando;
import com.proyecto3.nucleo.TablaArchivosAbiertos;
import com.proyecto3.sesion.Sesion;

public class ComandoViewFilesOpen implements Comando {

    private final Sesion sesion;

    public ComandoViewFilesOpen(Sesion sesion) {
        this.sesion = sesion;
    }

    @Override
    public String getNombre() { return "viewFilesOpen"; }

    @Override
    public String getAyuda() {
        return "viewFilesOpen - Lista los archivos actualmente abiertos";
    }

    @Override
    public String ejecutar(String[] args) {
        if (!sesion.estaAutenticado()) return "No hay sesión activa";

        TablaArchivosAbiertos tabla = sesion.getTablaArchivosAbiertos();
        if (tabla == null) return "No hay archivos abiertos";

        var abiertos = tabla.listar();

        if (abiertos.isEmpty()) {
            return "No hay archivos abiertos";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Total de archivos abiertos: ").append(abiertos.size()).append("\n");
        int idx = 0;
        for (var e : abiertos) {
            sb.append(String.format("  [%d] inodo=%-4d modo=%-8s usuario=%-10s archivo=%s\n",
                idx++, e.getNumInodo(), e.getModo(), e.getUsuario(), e.getRuta()));
        }
        return sb.toString().trim();
    }
}

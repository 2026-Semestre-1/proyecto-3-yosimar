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
        var abiertos = tabla.listar();

        if (abiertos.isEmpty()) {
            return "No hay archivos abiertos";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-6s %-6s %-8s\n", "INODO", "MODO", "POS"));
        sb.append("──────────────────────\n");
        for (var e : abiertos) {
            sb.append(String.format("%-6d %-6s %-8d\n",
                e.getNumInodo(), e.getModo(), e.getPosicion()));
        }
        sb.append("Total: ").append(abiertos.size()).append(" archivo(s) abierto(s)");

        return sb.toString().trim();
    }
}

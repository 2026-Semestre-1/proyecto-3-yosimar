package com.proyecto3.comandos.impl;

import com.proyecto3.comandos.Comando;
import com.proyecto3.nucleo.*;
import com.proyecto3.sesion.Sesion;

public class ComandoInfoFS implements Comando {

    private final Sesion sesion;

    public ComandoInfoFS(Sesion sesion) {
        this.sesion = sesion;
    }

    @Override
    public String getNombre() { return "infoFS"; }

    @Override
    public String getAyuda() {
        return "infoFS - Muestra información del sistema de archivos";
    }

    @Override
    public String ejecutar(String[] args) {
        if (sesion.getDisco() == null || !sesion.getDisco().estaAbierto()) {
            return "No hay disco cargado";
        }

        try {
            Superbloque sb = sesion.getSuperbloque();
            AsignadorBloques asignador = sesion.getAsignador();
            TablaInodos tablaInodos = sesion.getTablaInodos();
            LayoutDisco layout = sb.getLayout();

            long totalBloques = sb.getTotalBloques();
            long bloquesLibres = asignador.contarLibresEnDisco();
            long bloquesOcupados = totalBloques - bloquesLibres;

            int totalInodos = sb.getTotalInodos();
            int inodosLibres = tablaInodos.contarLibres();
            int inodosOcupados = totalInodos - inodosLibres;

            double usoBloques = totalBloques > 0 ? 100.0 * bloquesOcupados / totalBloques : 0;
            double usoInodos = totalInodos > 0 ? 100.0 * inodosOcupados / totalInodos : 0;

            StringBuilder sbInfo = new StringBuilder();
            sbInfo.append("─────── Sistema de Archivos ───────\n");
            sbInfo.append(String.format("  Nombre:             %s\n", sb.getNombreFs()));
            sbInfo.append(String.format("  Magic:              0x%08X\n", sb.getMagic()));
            sbInfo.append("\n");
            sbInfo.append(String.format("  Tamaño total:       %d bytes (%.2f MB)\n",
                sb.getTamanioTotal(), sb.getTamanioTotal() / (1024.0 * 1024.0)));
            sbInfo.append(String.format("  Tamaño de bloque:   %d bytes\n", sb.getTamanioBloque()));
            sbInfo.append(String.format("  Total de bloques:   %d\n", totalBloques));
            sbInfo.append(String.format("  Bloques libres:     %d\n", bloquesLibres));
            sbInfo.append(String.format("  Bloques ocupados:   %d (%.1f%%)\n", bloquesOcupados, usoBloques));
            sbInfo.append("\n");
            sbInfo.append(String.format("  Total de inodos:    %d\n", totalInodos));
            sbInfo.append(String.format("  Inodos libres:      %d\n", inodosLibres));
            sbInfo.append(String.format("  Inodos ocupados:    %d (%.1f%%)\n", inodosOcupados, usoInodos));
            sbInfo.append("\n");
            sbInfo.append("  Layout:\n");
            sbInfo.append(String.format("    Superbloque:      bloque 0\n"));
            sbInfo.append(String.format("    Bitmap:           bloques %d..%d\n",
                layout.getBloqueInicioBitmap(),
                layout.getBloqueInicioInodos() - 1));
            sbInfo.append(String.format("    Tabla inodos:     bloques %d..%d\n",
                layout.getBloqueInicioInodos(),
                layout.getBloqueInicioDatos() - 1));
            sbInfo.append(String.format("    Datos:            bloques %d..%d\n",
                layout.getBloqueInicioDatos(), totalBloques - 1));
            sbInfo.append(String.format("    Inodo raíz:       %d\n", sb.getInodoRaiz()));

            return sbInfo.toString().trim();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}

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

            long totalBloques = sb.getTotalBloques();
            long bloquesLibres = asignador.contarLibresEnDisco();
            long bloquesOcupados = totalBloques - bloquesLibres;
            int tamanioBloque = sb.getTamanioBloque();

            long espacioUsadoBytes = bloquesOcupados * tamanioBloque;
            long espacioLibreBytes = bloquesLibres * tamanioBloque;

            int totalInodos = sb.getTotalInodos();
            int inodosLibres = tablaInodos.contarLibres();
            int inodosOcupados = totalInodos - inodosLibres;

            String usadoStr;
            if (espacioUsadoBytes >= 1024 * 1024) {
                usadoStr = String.format("%.1f MB", espacioUsadoBytes / (1024.0 * 1024.0));
            } else {
                usadoStr = String.format("%.1f KB", espacioUsadoBytes / 1024.0);
            }
            String libreStr;
            if (espacioLibreBytes >= 1024 * 1024) {
                libreStr = String.format("%.1f MB", espacioLibreBytes / (1024.0 * 1024.0));
            } else {
                libreStr = String.format("%.1f KB", espacioLibreBytes / 1024.0);
            }

            StringBuilder sbInfo = new StringBuilder();
            sbInfo.append("Nombre del FileSystem: ").append(sb.getNombreFs()).append("\n");
            sbInfo.append(String.format("Tamaño total:          %d MB (%d bytes)\n",
                sb.getTamanioTotal() / (1024 * 1024), sb.getTamanioTotal()));
            sbInfo.append(String.format("Tamaño de bloque:      %d bytes\n", tamanioBloque));
            sbInfo.append(String.format("Total de bloques:      %d\n", totalBloques));
            sbInfo.append(String.format("Bloques ocupados:      %d\n", bloquesOcupados));
            sbInfo.append(String.format("Bloques libres:        %d\n", bloquesLibres));
            sbInfo.append(String.format("Total de inodos:       %d\n", totalInodos));
            sbInfo.append(String.format("Inodos usados:         %d\n", inodosOcupados));
            sbInfo.append(String.format("Espacio utilizado:     %s\n", usadoStr));
            sbInfo.append(String.format("Espacio disponible:    %s\n", libreStr));

            return sbInfo.toString().trim();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}

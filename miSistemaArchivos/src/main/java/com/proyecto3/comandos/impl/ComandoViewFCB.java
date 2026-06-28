package com.proyecto3.comandos.impl;

import com.proyecto3.comandos.Comando;
import com.proyecto3.nucleo.*;
import com.proyecto3.sesion.Sesion;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ComandoViewFCB implements Comando {

    private final Sesion sesion;

    public ComandoViewFCB(Sesion sesion) {
        this.sesion = sesion;
    }

    @Override
    public String getNombre() { return "viewFCB"; }

    @Override
    public String getAyuda() {
        return "viewFCB <ruta> - Muestra la información del inodo (FCB)";
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

            Directorio dirArchivo = new Directorio(sesion.getDisco(), sesion.getAsignador(),
                sesion.getTablaInodos(), inodoDir);

            EntradaDirectorio entrada = dirArchivo.buscarEntrada(nombreArchivo);
            if (entrada == null) return "Error: no encontrado: " + ruta;

            Inodo inodo = sesion.getTablaInodos().getInodo(entrada.getNumeroInodo());
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            String tipo = inodo.esDirectorio() ? "Directorio" : "Archivo";

            StringBuilder sb = new StringBuilder();
            sb.append("─────── FCB: ").append(nombreArchivo).append(" ───────\n");
            sb.append(String.format("  Inodo:          %d\n", inodo.getNumero()));
            sb.append(String.format("  Tipo:           %s\n", tipo));
            sb.append(String.format("  Permisos:       %04o\n", inodo.getPermisos()));
            sb.append(String.format("  Dueño (uid):    %d\n", inodo.getUid()));
            sb.append(String.format("  Grupo (gid):    %d\n", inodo.getGid()));
            sb.append(String.format("  Tamaño:         %d bytes\n", inodo.getTamanio()));
            sb.append(String.format("  Enlaces:        %d\n", inodo.getEnlaces()));
            sb.append(String.format("  Creado:         %s\n", sdf.format(new Date(inodo.getFechaCreacion()))));
            sb.append(String.format("  Modificado:     %s\n", sdf.format(new Date(inodo.getFechaModificacion()))));
            sb.append(String.format("  Abierto:        %s\n", inodo.isAbierto() ? "si" : "no"));

            if (!inodo.esDirectorio()) {
                sb.append("  Bloques directos:\n");
                for (int i = 0; i < Inodo.PUNTEROS_DIRECTOS; i++) {
                    int bloque = inodo.getPunteroDirecto(i);
                    if (bloque != Inodo.BLOQUE_NULO) {
                        sb.append(String.format("    [%d] bloque %d\n", i, bloque));
                    }
                }
                int indirecto = inodo.getPunteroIndirecto();
                if (indirecto != Inodo.BLOQUE_NULO) {
                    sb.append(String.format("    indirecto: bloque %d\n", indirecto));
                }
            }

            return sb.toString().trim();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}

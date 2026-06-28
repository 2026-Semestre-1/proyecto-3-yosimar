package com.proyecto3.comandos.impl;

import com.proyecto3.comandos.Comando;
import com.proyecto3.nucleo.*;
import com.proyecto3.sesion.Sesion;
import java.util.ArrayList;
import java.util.List;

public class ComandoWhereis implements Comando {

    private final Sesion sesion;

    public ComandoWhereis(Sesion sesion) {
        this.sesion = sesion;
    }

    @Override
    public String getNombre() { return "whereis"; }

    @Override
    public String getAyuda() {
        return "whereis [-R] <nombre> - Busca archivos/directorios por nombre. -R desde raíz";
    }

    @Override
    public String ejecutar(String[] args) {
        if (!sesion.estaAutenticado()) return "No hay sesión activa";
        if (args.length < 1) return "Uso: " + getAyuda();

        boolean desdeRaiz = false;
        String nombre;
        int argIdx = 0;

        if ("-R".equals(args[0])) {
            desdeRaiz = true;
            argIdx = 1;
        }

        if (argIdx >= args.length) return "Uso: " + getAyuda();
        nombre = args[argIdx];

        try {
            int inicio = desdeRaiz ? sesion.getSuperbloque().getInodoRaiz()
                : sesion.getInodoDirectorioTrabajo();

            List<String> resultados = new ArrayList<>();
            buscarRecursivo(inicio, "/", nombre, resultados);

            if (resultados.isEmpty()) {
                return "No se encontraron coincidencias para: " + nombre;
            }

            StringBuilder sb = new StringBuilder();
            for (String r : resultados) {
                sb.append(r).append("\n");
            }
            sb.append(resultados.size()).append(" coincidencia(s)");
            return sb.toString().trim();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private void buscarRecursivo(int inodoActual, String rutaActual, String nombre,
                                  List<String> resultados) throws Exception {
        Directorio dir = new Directorio(sesion.getDisco(), sesion.getAsignador(),
            sesion.getTablaInodos(), inodoActual);

        for (EntradaDirectorio e : dir.listarEntradas()) {
            if (".".equals(e.getNombre()) || "..".equals(e.getNombre())) continue;

            String rutaCompleta = rutaActual.equals("/") ? "/" + e.getNombre()
                : rutaActual + "/" + e.getNombre();

            if (e.getNombre().equals(nombre)) {
                resultados.add(rutaCompleta);
            }

            Inodo inodoHijo = sesion.getTablaInodos().getInodo(e.getNumeroInodo());
            if (inodoHijo.esDirectorio()) {
                buscarRecursivo(e.getNumeroInodo(), rutaCompleta, nombre, resultados);
            }
        }
    }
}

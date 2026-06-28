package com.proyecto3.comandos.impl;

import com.proyecto3.comandos.Comando;
import com.proyecto3.nucleo.*;
import com.proyecto3.sesion.Sesion;

public class ComandoWhereis implements Comando {

    private final Sesion sesion;

    public ComandoWhereis(Sesion sesion) {
        this.sesion = sesion;
    }

    @Override
    public String getNombre() { return "whereis"; }

    @Override
    public String getAyuda() {
        return "whereis <nombre> - Busca archivos/directorios por nombre desde la raíz";
    }

    @Override
    public String ejecutar(String[] args) {
        if (!sesion.estaAutenticado()) return "No hay sesión activa";
        if (args.length < 1) return "Uso: " + getAyuda();

        String nombre = args[0];

        try {
            java.util.List<String> resultados = new java.util.ArrayList<>();
            buscarRecursivo(sesion.getSuperbloque().getInodoRaiz(), "/", nombre, resultados);

            if (resultados.isEmpty()) {
                return "whereis: '" + nombre + "' no encontrado";
            }

            StringBuilder sb = new StringBuilder();
            for (String r : resultados) {
                sb.append(r).append("\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private void buscarRecursivo(int inodoActual, String rutaActual, String nombre,
                                  java.util.List<String> resultados) throws Exception {
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

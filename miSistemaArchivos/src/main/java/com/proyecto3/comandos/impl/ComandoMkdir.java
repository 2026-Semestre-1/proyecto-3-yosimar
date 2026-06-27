package com.proyecto3.comandos.impl;

import com.proyecto3.comandos.Comando;
import com.proyecto3.nucleo.*;
import com.proyecto3.sesion.Sesion;

public class ComandoMkdir implements Comando {

    private final Sesion sesion;

    public ComandoMkdir(Sesion sesion) {
        this.sesion = sesion;
    }

    @Override
    public String getNombre() { return "mkdir"; }

    @Override
    public String getAyuda() {
        return "mkdir <nombre> - Crea un nuevo directorio";
    }

    @Override
    public String ejecutar(String[] args) {
        if (!sesion.estaAutenticado()) return "No hay sesión activa";
        if (args.length < 1) return "Uso: " + getAyuda();

        String nombre = args[0];
        if (nombre.contains("/")) return "Error: el nombre no puede contener '/'";

        try {
            Directorio dirActual = new Directorio(sesion.getDisco(), sesion.getAsignador(),
                sesion.getTablaInodos(), sesion.getInodoDirectorioTrabajo());

            if (dirActual.buscarEntrada(nombre) != null) {
                return "Error: '" + nombre + "' ya existe";
            }

            Inodo nuevoInodo = sesion.getTablaInodos().asignarInodo();
            nuevoInodo.setTipo(Inodo.DIRECTORIO);
            nuevoInodo.setUid(sesion.getUsuarioActual().getUid());
            nuevoInodo.setGid(sesion.getUsuarioActual().getGid());
            nuevoInodo.setPermisos((short) 0755);

            Directorio nuevoDir = new Directorio(sesion.getDisco(), sesion.getAsignador(),
                sesion.getTablaInodos(), nuevoInodo.getNumero());
            nuevoDir.inicializarDirectorio(sesion.getInodoDirectorioTrabajo());
            nuevoDir.guardar();

            dirActual.agregarEntrada(nombre, nuevoInodo.getNumero());
            dirActual.guardar();

            return "Directorio '" + nombre + "' creado";
        } catch (Exception e) {
            return "Error al crear directorio: " + e.getMessage();
        }
    }
}

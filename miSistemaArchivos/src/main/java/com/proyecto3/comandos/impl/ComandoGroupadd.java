package com.proyecto3.comandos.impl;

import com.proyecto3.comandos.Comando;
import com.proyecto3.seguridad.GestorUsuarios;
import com.proyecto3.seguridad.Grupo;
import com.proyecto3.sesion.Sesion;

public class ComandoGroupadd implements Comando {

    private Sesion sesion;

    public ComandoGroupadd(Sesion sesion) {
        this.sesion = sesion;
    }

    @Override
    public String getNombre() { return "groupadd"; }

    @Override
    public String getAyuda() {
        return "groupadd <nombre> - Crea un nuevo grupo (requiere root)";
    }

    @Override
    public String ejecutar(String[] args) {
        if (!sesion.estaAutenticado()) return "No hay sesión activa";
        if (!sesion.esRoot()) return "Solo root puede crear grupos";

        if (args.length < 1) return "Uso: " + getAyuda();

        String nombre = args[0];
        GestorUsuarios gu = sesion.getGestorUsuarios();
        if (gu.getGrupoPorNombre(nombre) != null) {
            return "Error: el grupo '" + nombre + "' ya existe";
        }

        Grupo g = gu.crearGrupo(nombre);
        try {
            gu.guardarEnDisco(sesion.getDisco(), sesion.getAsignador(), sesion.getTablaInodos());
        } catch (Exception e) {
            return "Error al guardar en disco: " + e.getMessage();
        }

        return "Grupo '" + nombre + "' creado (gid=" + g.getGid() + ")";
    }
}

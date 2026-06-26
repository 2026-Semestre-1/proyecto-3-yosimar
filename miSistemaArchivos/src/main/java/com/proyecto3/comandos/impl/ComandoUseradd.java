package com.proyecto3.comandos.impl;

import com.proyecto3.comandos.Comando;
import com.proyecto3.seguridad.GestorUsuarios;
import com.proyecto3.seguridad.Usuario;
import com.proyecto3.sesion.Sesion;

public class ComandoUseradd implements Comando {

    private Sesion sesion;

    public ComandoUseradd(Sesion sesion) {
        this.sesion = sesion;
    }

    @Override
    public String getNombre() { return "useradd"; }

    @Override
    public String getAyuda() {
        return "useradd <nombre> <nombreCompleto> <password> <gid> - Crea un nuevo usuario (requiere root)";
    }

    @Override
    public String ejecutar(String[] args) {
        if (!sesion.estaAutenticado()) return "No hay sesión activa";
        if (!sesion.esRoot()) return "Solo root puede crear usuarios";

        if (args.length < 4) {
            return "Uso: " + getAyuda();
        }

        String nombre = args[0];
        String nombreCompleto = args[1];
        String password = args[2];
        int gid;
        try {
            gid = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            return "Error: gid debe ser un número";
        }

        GestorUsuarios gu = sesion.getGestorUsuarios();
        if (gu.getUsuarioPorNombre(nombre) != null) {
            return "Error: el usuario '" + nombre + "' ya existe";
        }
        if (gu.getGrupoPorGid(gid) == null) {
            return "Error: el grupo con gid=" + gid + " no existe";
        }

        Usuario u = gu.crearUsuario(nombre, nombreCompleto, password, gid, sesion.getInodoDirectorioTrabajo());
        try {
            gu.guardarEnDisco(sesion.getDisco(), sesion.getAsignador(), sesion.getTablaInodos());
        } catch (Exception e) {
            return "Error al guardar en disco: " + e.getMessage();
        }

        return "Usuario '" + nombre + "' creado (uid=" + u.getUid() + ", gid=" + gid + ")";
    }
}

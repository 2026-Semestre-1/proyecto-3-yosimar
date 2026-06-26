package com.proyecto3.comandos.impl;

import com.proyecto3.comandos.Comando;
import com.proyecto3.seguridad.Usuario;
import com.proyecto3.sesion.Sesion;

public class ComandoWhoami implements Comando {

    private Sesion sesion;

    public ComandoWhoami(Sesion sesion) {
        this.sesion = sesion;
    }

    @Override
    public String getNombre() { return "whoami"; }

    @Override
    public String getAyuda() {
        return "whoami - Muestra el usuario actual";
    }

    @Override
    public String ejecutar(String[] args) {
        if (!sesion.estaAutenticado()) return "Nadie (sin sesión)";

        Usuario u = sesion.getUsuarioActual();
        return u.getNombre() + " (uid=" + u.getUid() + ", gid=" + u.getGid() + ")";
    }
}

package com.proyecto3.comandos.impl;

import com.proyecto3.comandos.Comando;
import com.proyecto3.seguridad.GestorUsuarios;
import com.proyecto3.seguridad.Usuario;
import com.proyecto3.sesion.Sesion;

public class ComandoSu implements Comando {

    private Sesion sesion;

    public ComandoSu(Sesion sesion) {
        this.sesion = sesion;
    }

    @Override
    public String getNombre() { return "su"; }

    @Override
    public String getAyuda() {
        return "su <usuario> - Cambia al usuario especificado";
    }

    @Override
    public String ejecutar(String[] args) {
        if (args.length < 1) return "Uso: " + getAyuda();

        String nombre = args[0];
        GestorUsuarios gu = sesion.getGestorUsuarios();
        Usuario u = gu.getUsuarioPorNombre(nombre);
        if (u == null) return "Usuario '" + nombre + "' no encontrado";

        if (sesion.esRoot()) {
            sesion.su(nombre);
            return "Ahora eres '" + nombre + "' (uid=" + u.getUid() + ")";
        }

        String password = args.length >= 2 ? args[1] : "";
        if (password.isEmpty()) return "Contraseña requerida para su";

        if (!u.verificarPassword(password)) return "Contraseña incorrecta";

        sesion.su(nombre);
        return "Ahora eres '" + nombre + "' (uid=" + u.getUid() + ")";
    }
}

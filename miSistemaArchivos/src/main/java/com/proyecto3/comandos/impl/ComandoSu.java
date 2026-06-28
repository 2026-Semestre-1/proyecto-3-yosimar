package com.proyecto3.comandos.impl;

import com.proyecto3.comandos.Comando;
import com.proyecto3.seguridad.Usuario;
import com.proyecto3.sesion.Sesion;

public class ComandoSu implements Comando {

    private final Sesion sesion;

    public ComandoSu(Sesion sesion) {
        this.sesion = sesion;
    }

    @Override
    public String getNombre() { return "su"; }

    @Override
    public String getAyuda() {
        return "su [<usuario>] - Cambia al usuario especificado (sin args: root)";
    }

    @Override
    public String ejecutar(String[] args) {
        String nombreDestino = (args.length >= 1) ? args[0] : "root";

        Usuario u = sesion.getGestorUsuarios().getUsuarioPorNombre(nombreDestino);
        if (u == null) return "su: usuario '" + nombreDestino + "' no encontrado";

        String password = args.length >= 2 ? args[1] : "";
        if (password.isEmpty()) return "su: se requiere contraseña";

        if (!u.verificarPassword(password)) return "su: autenticación fallida";

        sesion.su(nombreDestino);
        return "";
    }
}

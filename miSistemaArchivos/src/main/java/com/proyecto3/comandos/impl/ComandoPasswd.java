package com.proyecto3.comandos.impl;

import com.proyecto3.comandos.Comando;
import com.proyecto3.seguridad.GestorUsuarios;
import com.proyecto3.seguridad.Usuario;
import com.proyecto3.sesion.Sesion;

public class ComandoPasswd implements Comando {

    private Sesion sesion;

    public ComandoPasswd(Sesion sesion) {
        this.sesion = sesion;
    }

    @Override
    public String getNombre() { return "passwd"; }

    @Override
    public String getAyuda() {
        return "passwd [<usuario>] - Cambia la contraseña (root puede cambiar cualquiera)";
    }

    @Override
    public String ejecutar(String[] args) {
        if (!sesion.estaAutenticado()) return "No hay sesión activa";

        GestorUsuarios gu = sesion.getGestorUsuarios();

        if (args.length >= 1 && sesion.esRoot()) {
            String nombre = args[0];
            String nuevaPass = args.length >= 2 ? args[1] : "";
            if (nuevaPass.isEmpty()) return "Debe especificar la nueva contraseña";

            Usuario u = gu.getUsuarioPorNombre(nombre);
            if (u == null) return "Usuario '" + nombre + "' no encontrado";

            u.setHashPassword(Usuario.hashPassword(nuevaPass));
            try {
                gu.guardarEnDisco(sesion.getDisco(), sesion.getAsignador(), sesion.getTablaInodos());
            } catch (Exception e) {
                return "Error al guardar: " + e.getMessage();
            }
            return "Contraseña de '" + nombre + "' cambiada";
        }

        String nuevaPass = args.length >= 1 ? args[0] : "";
        if (nuevaPass.isEmpty()) return "Uso: passwd <nuevaContraseña>";

        Usuario actual = sesion.getUsuarioActual();
        actual.setHashPassword(Usuario.hashPassword(nuevaPass));
        try {
            gu.guardarEnDisco(sesion.getDisco(), sesion.getAsignador(), sesion.getTablaInodos());
        } catch (Exception e) {
            return "Error al guardar: " + e.getMessage();
        }
        return "Contraseña cambiada para '" + actual.getNombre() + "'";
    }
}

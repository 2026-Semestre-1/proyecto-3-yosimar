package com.proyecto3.nucleo;

import com.proyecto3.seguridad.GestorUsuarios;
import com.proyecto3.seguridad.Usuario;
import com.proyecto3.sesion.Sesion;

public class PermisoUtil {

    public static final int BIT_LECTURA = 4;
    public static final int BIT_ESCRITURA = 2;
    public static final int BIT_EJECUCION = 1;

    public static boolean verificar(Inodo inodo, Sesion sesion, int bitRequerido) {
        if (!sesion.estaAutenticado()) return false;

        if (sesion.esRoot()) return true;

        Usuario usuario = sesion.getUsuarioActual();
        short permisos = inodo.getPermisos();

        int digitoDueno = (permisos >> 3) & 07;
        int digitoGrupo = permisos & 07;

        if (usuario.getUid() == inodo.getUid()) {
            return (digitoDueno & bitRequerido) != 0;
        }

        if (usuario.getGid() == inodo.getGid()) {
            return (digitoGrupo & bitRequerido) != 0;
        }

        return false;
    }

    public static String mensajeDenegado(String operacion) {
        return operacion + ": permiso denegado";
    }
}

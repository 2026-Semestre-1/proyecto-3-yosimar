package com.proyecto3.comandos.impl;

import com.proyecto3.comandos.Comando;
import com.proyecto3.nucleo.*;
import com.proyecto3.seguridad.GestorUsuarios;
import com.proyecto3.seguridad.Usuario;
import com.proyecto3.sesion.Sesion;

public class ComandoUseradd implements Comando {

    public static final String __USERADD_PROMPT_NAME__ = "__USERADD_PROMPT_NAME__";
    public static final String __USERADD_PROMPT_PASS__ = "__USERADD_PROMPT_PASS__";
    public static final String __USERADD_PROMPT_CONFIRM__ = "__USERADD_PROMPT_CONFIRM__";

    private final Sesion sesion;
    private String usernamePendiente;
    private String nombreCompletoPendiente;
    private String passwordPendiente;

    public ComandoUseradd(Sesion sesion) {
        this.sesion = sesion;
    }

    @Override
    public String getNombre() { return "useradd"; }

    @Override
    public String getAyuda() {
        return "useradd <username> - Crea un nuevo usuario (requiere root)";
    }

    @Override
    public String ejecutar(String[] args) {
        if (!sesion.estaAutenticado()) return "No hay sesión activa";
        if (!sesion.esRoot()) return "useradd: se requieren privilegios de root";
        if (args.length < 1) return "Uso: " + getAyuda();

        String username = args[0];
        if (username.isEmpty() || username.contains(" ")) {
            return "Error: el username no puede estar vacío ni contener espacios";
        }

        GestorUsuarios gu = sesion.getGestorUsuarios();
        if (gu.getUsuarioPorNombre(username) != null) {
            return "Error: el usuario '" + username + "' ya existe";
        }

        if (args.length >= 4) {
            return crearUsuario(username, args[1], args[2]);
        }

        usernamePendiente = username;
        nombreCompletoPendiente = null;
        passwordPendiente = null;
        return __USERADD_PROMPT_NAME__;
    }

    public String procesarNombreCompleto(String nombreCompleto) {
        if (nombreCompleto == null || nombreCompleto.trim().isEmpty()) {
            return "Nombre completo no puede estar vacío. Intente de nuevo:";
        }
        nombreCompletoPendiente = nombreCompleto.trim();
        return __USERADD_PROMPT_PASS__;
    }

    public String procesarPassword(String password) {
        if (password == null || password.isEmpty()) {
            return "Password no puede estar vacía. Intente de nuevo:";
        }
        passwordPendiente = password;
        return __USERADD_PROMPT_CONFIRM__;
    }

    public String procesarConfirmar(String confirmacion) {
        if (!passwordPendiente.equals(confirmacion)) {
            passwordPendiente = null;
            return "Error: las contraseñas no coinciden. Password:";
        }

        String resultado = crearUsuario(usernamePendiente, nombreCompletoPendiente, passwordPendiente);
        usernamePendiente = null;
        nombreCompletoPendiente = null;
        passwordPendiente = null;
        return resultado;
    }

    private String crearUsuario(String username, String nombreCompleto, String password) {
        GestorUsuarios gu = sesion.getGestorUsuarios();

        try {
            Directorio dirRaiz = new Directorio(sesion.getDisco(), sesion.getAsignador(),
                sesion.getTablaInodos(), sesion.getSuperbloque().getInodoRaiz());

            EntradaDirectorio entradaHome = dirRaiz.buscarEntrada("home");
            int inodoHomeDir;
            if (entradaHome == null) {
                Inodo inodoHome = sesion.getTablaInodos().asignarInodo();
                inodoHome.setTipo(Inodo.DIRECTORIO);
                inodoHome.setUid(GestorUsuarios.UID_ROOT);
                inodoHome.setGid(GestorUsuarios.GID_ROOT);
                inodoHome.setPermisos((short) 075);
                inodoHomeDir = inodoHome.getNumero();

                Directorio dirHome = new Directorio(sesion.getDisco(), sesion.getAsignador(),
                    sesion.getTablaInodos(), inodoHomeDir);
                dirHome.inicializarDirectorio(sesion.getSuperbloque().getInodoRaiz());
                dirHome.guardar();

                dirRaiz.agregarEntrada("home", inodoHomeDir);
                dirRaiz.guardar();
            } else {
                inodoHomeDir = entradaHome.getNumeroInodo();
            }

            Inodo inodoUserHome = sesion.getTablaInodos().asignarInodo();
            inodoUserHome.setTipo(Inodo.DIRECTORIO);
            inodoUserHome.setUid(sesion.getUsuarioActual().getUid());
            inodoUserHome.setGid(sesion.getUsuarioActual().getGid());
            inodoUserHome.setPermisos((short) 075);
            int inodoUserHomeNum = inodoUserHome.getNumero();

            Directorio dirUserHome = new Directorio(sesion.getDisco(), sesion.getAsignador(),
                sesion.getTablaInodos(), inodoUserHomeNum);
            dirUserHome.inicializarDirectorio(inodoHomeDir);
            dirUserHome.guardar();

            Directorio dirHome = new Directorio(sesion.getDisco(), sesion.getAsignador(),
                sesion.getTablaInodos(), inodoHomeDir);
            dirHome.agregarEntrada(username, inodoUserHomeNum);
            dirHome.guardar();

            int gidPrimario = sesion.getUsuarioActual().getGid();
            Usuario u = gu.crearUsuario(username, nombreCompleto, password, gidPrimario,
                inodoUserHomeNum);
            gu.guardarEnDisco(sesion.getDisco(), sesion.getAsignador(), sesion.getTablaInodos());

            return "Usuario '" + username + "' creado exitosamente (uid=" + u.getUid()
                + ").\nHome creado: /home/" + username;

        } catch (Exception e) {
            return "Error al crear usuario: " + e.getMessage();
        }
    }

    public boolean estaEnProceso() {
        return usernamePendiente != null;
    }
}

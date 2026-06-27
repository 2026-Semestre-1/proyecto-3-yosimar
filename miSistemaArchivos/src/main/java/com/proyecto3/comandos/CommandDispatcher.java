package com.proyecto3.comandos;

import com.proyecto3.comandos.impl.*;
import com.proyecto3.sesion.Sesion;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandDispatcher {

    private final Sesion sesion;
    private final Map<String, Comando> comandos;

    public CommandDispatcher(Sesion sesion) {
        this.sesion = sesion;
        this.comandos = new HashMap<>();

        comandos.put("format", new ComandoFormat());
        comandos.put("whoami", new ComandoWhoami(sesion));
        comandos.put("pwd", new ComandoPwd(sesion));
        comandos.put("cd", new ComandoCd(sesion));
        comandos.put("mkdir", new ComandoMkdir(sesion));
        comandos.put("ls", new ComandoLs(sesion));
        comandos.put("rm", new ComandoRm(sesion));

        if (sesion.estaAutenticado()) {
            comandos.put("useradd", new ComandoUseradd(sesion));
            comandos.put("groupadd", new ComandoGroupadd(sesion));
            comandos.put("passwd", new ComandoPasswd(sesion));
            comandos.put("su", new ComandoSu(sesion));
        }
    }

    public String despachar(String linea) {
        if (linea == null || linea.trim().isEmpty()) return "";

        String[] partes = parsearLinea(linea);
        if (partes.length == 0) return "";

        String nombreCmd = partes[0].toLowerCase();
        String[] args = new String[partes.length - 1];
        System.arraycopy(partes, 1, args, 0, args.length);

        if ("clear".equals(nombreCmd)) return null;

        if ("exit".equals(nombreCmd)) return "__EXIT__";

        Comando cmd = comandos.get(nombreCmd);
        if (cmd == null) {
            if (esComandoPendiente(nombreCmd)) {
                return nombreCmd + ": aún no implementado";
            }
            return "comando no encontrado: " + nombreCmd;
        }

        if (requiereLogin(nombreCmd) && !sesion.estaAutenticado()) {
            return "Debe iniciar sesión primero. Use: login <usuario> <password>";
        }

        if ("su".equals(nombreCmd) || "useradd".equals(nombreCmd) || "groupadd".equals(nombreCmd)
            || "passwd".equals(nombreCmd)) {
            actualizarComandosSesion();
        }

        if ("format".equals(nombreCmd)) {
            String resultado = cmd.ejecutar(args);
            if (!resultado.startsWith("Error")) {
                cargarDiscoFormateado(args[0]);
            }
            flushDisco();
            return resultado;
        }

        String resultado = cmd.ejecutar(args);
        flushDisco();
        return resultado;
    }

    private void actualizarComandosSesion() {
        comandos.put("useradd", new ComandoUseradd(sesion));
        comandos.put("groupadd", new ComandoGroupadd(sesion));
        comandos.put("passwd", new ComandoPasswd(sesion));
        comandos.put("su", new ComandoSu(sesion));
    }

    private void flushDisco() {
        try {
            if (sesion.getDisco() != null && sesion.getDisco().estaAbierto()) {
                sesion.getAsignador().guardarEnDisco();
                sesion.getTablaInodos().guardarEnDisco();
            }
        } catch (Exception ignored) {
        }
    }

    private boolean requiereLogin(String nombreCmd) {
        return !"format".equals(nombreCmd) && !"exit".equals(nombreCmd) && !"clear".equals(nombreCmd);
    }

    private boolean esComandoPendiente(String nombre) {
        return java.util.Set.of("touch", "cat", "less", "mv", "ln", "whereis",
            "chown", "chgrp", "chmod", "viewFilesOpen", "viewFCB", "infoFS", "note")
            .contains(nombre);
    }

    public String procesarLogin(String usuario, String password) {
        if (sesion.login(usuario, password)) {
            comandos.put("useradd", new ComandoUseradd(sesion));
            comandos.put("groupadd", new ComandoGroupadd(sesion));
            comandos.put("passwd", new ComandoPasswd(sesion));
            comandos.put("su", new ComandoSu(sesion));
            return null;
        }
        return "Login fallido: usuario o contraseña incorrectos";
    }

    public String getPrompt() {
        if (!sesion.estaAutenticado()) return "login: ";
        try {
            com.proyecto3.nucleo.Directorio dir = new com.proyecto3.nucleo.Directorio(
                sesion.getDisco(), sesion.getAsignador(), sesion.getTablaInodos(),
                sesion.getInodoDirectorioTrabajo());
            String ruta = dir.obtenerRutaAbsoluta(sesion.getSuperbloque());
            String nombreFs = sesion.getSuperbloque().getNombreFs();
            return sesion.getUsuarioActual().getNombre() + "@" + nombreFs + ":" + ruta + " $ ";
        } catch (Exception e) {
            return sesion.getUsuarioActual().getNombre() + "@?:? $ ";
        }
    }

    public String getNombreUsuario() {
        if (!sesion.estaAutenticado()) return "sin sesión";
        return sesion.getUsuarioActual().getNombre() + " (uid=" + sesion.getUsuarioActual().getUid() + ")";
    }

    public boolean estaAutenticado() {
        return sesion.estaAutenticado();
    }

    public Sesion getSesion() { return sesion; }

    private void cargarDiscoFormateado(String ruta) {
        try {
            if (sesion.getDisco().estaAbierto()) {
                sesion.getDisco().cerrar();
            }
            sesion.getDisco().abrirDisco(ruta);

            com.proyecto3.nucleo.Superbloque sb = sesion.getSuperbloque();
            sb.cargar(sesion.getDisco());

            com.proyecto3.nucleo.LayoutDisco layout = sb.getLayout();

            com.proyecto3.nucleo.AsignadorBloques nuevoAb = new com.proyecto3.nucleo.AsignadorBloques(
                sesion.getDisco(), layout);
            nuevoAb.cargarDeDisco();
            sesion.setAsignador(nuevoAb);

            com.proyecto3.nucleo.TablaInodos nuevaTi = new com.proyecto3.nucleo.TablaInodos(
                sesion.getDisco(), layout, sb.getTotalInodos());
            nuevaTi.cargarDeDisco();
            sesion.setTablaInodos(nuevaTi);

            sesion.getGestorUsuarios().cargarDeDisco(sesion.getDisco(), nuevaTi, nuevoAb);

            sesion.setInodoDirectorioTrabajo(sb.getInodoRaiz());
            sesion.logout();
        } catch (Exception e) {
            System.err.println("Error cargando disco formateado: " + e.getMessage());
        }
    }

    private String[] parsearLinea(String linea) {
        List<String> tokens = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean entreComillas = false;

        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);
            if (c == '"') {
                entreComillas = !entreComillas;
            } else if (c == ' ' && !entreComillas) {
                if (actual.length() > 0) {
                    tokens.add(actual.toString());
                    actual.setLength(0);
                }
            } else {
                actual.append(c);
            }
        }
        if (actual.length() > 0) {
            tokens.add(actual.toString());
        }
        return tokens.toArray(new String[0]);
    }
}

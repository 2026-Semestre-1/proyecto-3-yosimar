package com.proyecto3.comandos;

import com.proyecto3.comandos.impl.*;
import com.proyecto3.nucleo.DiscoVirtual;
import com.proyecto3.nucleo.TablaArchivosAbiertos;
import com.proyecto3.sesion.Sesion;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandDispatcher {

    private final Sesion sesion;
    private final TablaArchivosAbiertos tablaArchivosAbiertos;
    private final Map<String, Comando> comandos;
    private ComandoNote editorActivo;
    private ComandoUseradd useraddActivo;
    private String promptCache;
    private int promptInodoCache;

    public CommandDispatcher(Sesion sesion, TablaArchivosAbiertos tablaArchivosAbiertos) {
        this.sesion = sesion;
        this.tablaArchivosAbiertos = tablaArchivosAbiertos;
        this.comandos = new HashMap<>();
        this.editorActivo = null;

        comandos.put("format", new ComandoFormat());
        comandos.put("whoami", new ComandoWhoami(sesion));
        comandos.put("pwd", new ComandoPwd(sesion));
        comandos.put("cd", new ComandoCd(sesion));
        comandos.put("mkdir", new ComandoMkdir(sesion));
        comandos.put("ls", new ComandoLs(sesion));
        comandos.put("rm", new ComandoRm(sesion));
        comandos.put("touch", new ComandoTouch(sesion));
        comandos.put("cat", new ComandoCat(sesion));
        comandos.put("less", new ComandoLess(sesion));
        comandos.put("viewfilesopen", new ComandoViewFilesOpen(sesion));
        comandos.put("chmod", new ComandoChmod(sesion));
        comandos.put("chown", new ComandoChown(sesion));
        comandos.put("chgrp", new ComandoChgrp(sesion));
        comandos.put("ln", new ComandoLn(sesion));
        comandos.put("whereis", new ComandoWhereis(sesion));
        comandos.put("note", new ComandoNote(sesion));
        comandos.put("mv", new ComandoMv(sesion));
        comandos.put("viewfcb", new ComandoViewFCB(sesion));
        comandos.put("infofs", new ComandoInfoFS(sesion));

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

        if ("format".equals(nombreCmd)) {
            String resultado = cmd.ejecutar(args);
            if (!resultado.startsWith("Error")) {
                cargarDiscoFormateado(args[0]);
            }
            flushDisco();
            return resultado;
        }

        String resultado = cmd.ejecutar(args);

        if ("note".equals(nombreCmd)) {
            ComandoNote note = (ComandoNote) cmd;
            if (note.estaActivo()) {
                editorActivo = note;
                String display = resultado;
                if (display.startsWith(ComandoNote.__NOTE_ENTER__)) {
                    display = display.substring(ComandoNote.__NOTE_ENTER__.length());
                }
                flushDisco();
                return display;
            }
        }

        if ("useradd".equals(nombreCmd)) {
            if (resultado != null && resultado.startsWith(ComandoUseradd.__USERADD_PROMPT_NAME__)) {
                useraddActivo = (ComandoUseradd) cmd;
                flushDisco();
                return "Nombre completo: ";
            }
        }

        flushDisco();
        return resultado;
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

    private void flushDisco() {
        try {
            if (sesion.getDisco() != null && sesion.getDisco().estaAbierto()
                && sesion.getAsignador() != null && sesion.getTablaInodos() != null) {
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
        return java.util.Set.of()
            .contains(nombre);
    }

    public boolean tieneEditorActivo() { return editorActivo != null && editorActivo.estaActivo(); }

    public boolean tieneUseraddActivo() { return useraddActivo != null && useraddActivo.estaEnProceso(); }

    public String getNombreArchivoEditor() {
        return editorActivo != null ? editorActivo.getNombreArchivo() : "";
    }

    public String procesarUseradd(String linea) {
        if (useraddActivo == null) return null;
        String result = useraddActivo.procesarNombreCompleto(linea);
        if (ComandoUseradd.__USERADD_PROMPT_PASS__.equals(result)) {
            return "Password: ";
        }
        if (ComandoUseradd.__USERADD_PROMPT_NAME__.equals(result)) {
            return "Nombre completo: ";
        }
        if (!ComandoUseradd.__USERADD_PROMPT_CONFIRM__.equals(result)) {
            useraddActivo = null;
            return result;
        }
        return "Confirmar password: ";
    }

    public String procesarUseraddPassword(String linea) {
        if (useraddActivo == null) return null;
        String result = useraddActivo.procesarPassword(linea);
        if (ComandoUseradd.__USERADD_PROMPT_CONFIRM__.equals(result)) {
            return "Confirmar password: ";
        }
        if (ComandoUseradd.__USERADD_PROMPT_PASS__.equals(result)) {
            return "Password: ";
        }
        useraddActivo = null;
        return result;
    }

    public String procesarUseraddConfirm(String linea) {
        if (useraddActivo == null) return null;
        String result = useraddActivo.procesarConfirmar(linea);
        if (ComandoUseradd.__USERADD_PROMPT_PASS__.equals(result)) {
            useraddActivo = null;
            return result;
        }
        useraddActivo = null;
        flushDisco();
        return result;
    }

    public String procesarEditor(String linea) {
        if (editorActivo == null) return null;
        String resultado = editorActivo.procesarComandoEditor(linea);

        if (resultado != null && resultado.startsWith(ComandoNote.__NOTE_EXIT__)) {
            String mensaje = resultado.substring(ComandoNote.__NOTE_EXIT__.length());
            editorActivo = null;
            flushDisco();
            return mensaje;
        }

        return resultado;
    }

    public String getPrompt() {
        if (!sesion.estaAutenticado()) return "login: ";

        int cwdActual = sesion.getInodoDirectorioTrabajo();
        if (promptCache != null && promptInodoCache == cwdActual) {
            return promptCache;
        }

        try {
            com.proyecto3.nucleo.Directorio dir = new com.proyecto3.nucleo.Directorio(
                sesion.getDisco(), sesion.getAsignador(), sesion.getTablaInodos(), cwdActual);
            String ruta = dir.obtenerRutaAbsoluta(sesion.getSuperbloque());
            String nombreFs = sesion.getSuperbloque().getNombreFs();
            promptCache = sesion.getUsuarioActual().getNombre() + "@" + nombreFs + ":" + ruta + " $ ";
            promptInodoCache = cwdActual;
            return promptCache;
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
            if (sesion.getDisco() == null) {
                sesion.setDisco(new DiscoVirtual(512));
            } else if (sesion.getDisco().estaAbierto()) {
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

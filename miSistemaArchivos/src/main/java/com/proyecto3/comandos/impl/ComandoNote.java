package com.proyecto3.comandos.impl;

import com.proyecto3.comandos.Comando;
import com.proyecto3.nucleo.*;
import com.proyecto3.sesion.Sesion;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ComandoNote implements Comando {

    public static final String __NOTE_ENTER__ = "__NOTE_ENTER__";
    public static final String __NOTE_EXIT__ = "__NOTE_EXIT__";

    private final Sesion sesion;

    private String nombreArchivo;
    private int inodoArchivo = -1;
    private int inodoDirPadre;
    private List<String> buffer;
    private boolean modificado;
    private boolean activo;

    public ComandoNote(Sesion sesion) {
        this.sesion = sesion;
        this.buffer = new ArrayList<>();
    }

    @Override
    public String getNombre() { return "note"; }

    @Override
    public String getAyuda() {
        return "note <ruta> - Editor de texto. :d N del linea | :e N txt editar | :show | :w guardar | :q salir";
    }

    @Override
    public String ejecutar(String[] args) {
        if (!sesion.estaAutenticado()) return "No hay sesión activa";
        if (args.length < 1) return "Uso: " + getAyuda();

        try {
            String ruta = args[0];
            Directorio dirActual = new Directorio(sesion.getDisco(), sesion.getAsignador(),
                sesion.getTablaInodos(), sesion.getInodoDirectorioTrabajo());

            int lastSep = Math.max(ruta.lastIndexOf('/'), ruta.lastIndexOf('\\'));
            if (lastSep >= 0) {
                nombreArchivo = ruta.substring(lastSep + 1);
                String rutaDir = ruta.substring(0, lastSep);
                if (rutaDir.isEmpty()) rutaDir = "/";
                inodoDirPadre = dirActual.navegar(rutaDir, sesion.getSuperbloque());
            } else {
                nombreArchivo = ruta;
                inodoDirPadre = sesion.getInodoDirectorioTrabajo();
            }

            if (nombreArchivo.isEmpty()) return "Error: nombre de archivo vacío";

            Directorio dirPadre = new Directorio(sesion.getDisco(), sesion.getAsignador(),
                sesion.getTablaInodos(), inodoDirPadre);

            EntradaDirectorio entrada = dirPadre.buscarEntrada(nombreArchivo);
            buffer.clear();

            if (entrada != null) {
                Inodo inodo = sesion.getTablaInodos().getInodo(entrada.getNumeroInodo());
                if (inodo.esDirectorio()) return "Error: '" + nombreArchivo + "' es un directorio";

                if (!PermisoUtil.verificar(inodo, sesion, PermisoUtil.BIT_LECTURA)) {
                    return "note: permiso denegado para leer";
                }

                inodoArchivo = inodo.getNumero();
                String contenido = GestorArchivos.leerDatosComoTexto(inodo,
                    sesion.getDisco(), sesion.getAsignador());
                for (String linea : contenido.split("\n", -1)) {
                    buffer.add(linea);
                }
            } else {
                inodoArchivo = -1;
            }

            modificado = false;
            activo = true;

            return __NOTE_ENTER__ + mostrarBuffer();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    public String procesarComandoEditor(String linea) {
        if (linea == null || linea.trim().isEmpty()) return "";

        String cmd = linea.trim();

        try {
            if (":wq".equals(cmd)) {
                guardar();
                activo = false;
                return __NOTE_EXIT__ + "[guardado] " + nombreArchivo;
            }

            if (":q".equals(cmd)) {
                if (modificado) {
                    return "Hay cambios sin guardar. Use :wq para guardar y salir, o :q! para salir sin guardar.";
                }
                activo = false;
                return __NOTE_EXIT__ + "[salir] editor cerrado";
            }

            if (":q!".equals(cmd)) {
                activo = false;
                return __NOTE_EXIT__ + "[salir sin guardar] " + nombreArchivo;
            }

            if (":w".equals(cmd)) {
                guardar();
                return "[guardado] " + nombreArchivo;
            }

            if (":show".equals(cmd)) {
                return mostrarBuffer();
            }

            if (cmd.startsWith(":d ")) {
                try {
                    int n = Integer.parseInt(cmd.substring(3).trim());
                    return eliminarLinea(n);
                } catch (NumberFormatException e) {
                    return "Uso: :d <numero>   (1-" + buffer.size() + ")";
                }
            }

            if (cmd.startsWith(":e ")) {
                String[] partes = cmd.substring(3).trim().split(" ", 2);
                if (partes.length < 2) return "Uso: :e <numero> <texto>";
                try {
                    int n = Integer.parseInt(partes[0]);
                    return editarLinea(n, partes[1]);
                } catch (NumberFormatException e) {
                    return "Uso: :e <numero> <texto>";
                }
            }

            if (cmd.startsWith(":i ")) {
                String[] partes = cmd.substring(3).trim().split(" ", 2);
                if (partes.length < 2) return "Uso: :i <numero> <texto>";
                try {
                    int n = Integer.parseInt(partes[0]);
                    return insertarLinea(n, partes[1]);
                } catch (NumberFormatException e) {
                    return "Uso: :i <numero> <texto>";
                }
            }

            if (cmd.startsWith(":")) {
                return "Comandos: :d N | :e N txt | :i N txt | :show | :w | :q | :wq | :q!";
            }

            buffer.add(cmd);
            modificado = true;
            return "";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private String mostrarBuffer() {
        if (buffer.isEmpty() && !modificado) {
            return "\n[archivo vacío - escriba para agregar líneas, :w para guardar, :q para salir]\n";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < buffer.size(); i++) {
            sb.append(String.format("%3d ", i + 1)).append(buffer.get(i)).append("\n");
        }
        if (buffer.isEmpty()) sb.append("(vacío)\n");
        return sb.toString();
    }

    private String eliminarLinea(int n) {
        int idx = n - 1;
        if (idx < 0 || idx >= buffer.size()) {
            return "Línea inválida. Hay " + buffer.size() + " línea(s).";
        }
        buffer.remove(idx);
        modificado = true;
        return "[eliminada línea " + n + "]";
    }

    private String editarLinea(int n, String texto) {
        int idx = n - 1;
        if (idx < 0 || idx >= buffer.size()) {
            return "Línea inválida. Hay " + buffer.size() + " línea(s).";
        }
        buffer.set(idx, texto);
        modificado = true;
        return "[editada línea " + n + "]";
    }

    private String insertarLinea(int n, String texto) {
        int idx = n - 1;
        if (idx < 0) idx = 0;
        if (idx > buffer.size()) idx = buffer.size();
        buffer.add(idx, texto);
        modificado = true;
        return "[insertada línea " + n + "]";
    }

    private void guardar() throws Exception {
        if (inodoArchivo >= 0) {
            Inodo inodoExistente = sesion.getTablaInodos().getInodo(inodoArchivo);
            if (!PermisoUtil.verificar(inodoExistente, sesion, PermisoUtil.BIT_ESCRITURA)) {
                throw new Exception("note: permiso denegado para escribir");
            }
        }
        String contenido = "";
        for (int i = 0; i < buffer.size(); i++) {
            if (i > 0) contenido += "\n";
            contenido += buffer.get(i);
        }

        byte[] datos = contenido.getBytes(StandardCharsets.UTF_8);

        Directorio dirPadre = new Directorio(sesion.getDisco(), sesion.getAsignador(),
            sesion.getTablaInodos(), inodoDirPadre);

        if (inodoArchivo < 0) {
            Inodo nuevoInodo = sesion.getTablaInodos().asignarInodo();
            nuevoInodo.setTipo(Inodo.ARCHIVO);
            nuevoInodo.setUid(sesion.getUsuarioActual().getUid());
            nuevoInodo.setGid(sesion.getUsuarioActual().getGid());
            nuevoInodo.setPermisos((short) 064);

            GestorArchivos.escribirDatosInodo(nuevoInodo, datos,
                sesion.getDisco(), sesion.getAsignador());

            dirPadre.agregarEntrada(nombreArchivo, nuevoInodo.getNumero());
            dirPadre.guardar();

            inodoArchivo = nuevoInodo.getNumero();
            sesion.getTablaArchivosAbiertos().abrir(inodoArchivo,
                TablaArchivosAbiertos.MODO_ESCRITURA,
                sesion.getUsuarioActual().getNombre(), nombreArchivo);
        } else {
            Inodo inodo = sesion.getTablaInodos().getInodo(inodoArchivo);
            GestorArchivos.escribirDatosInodo(inodo, datos,
                sesion.getDisco(), sesion.getAsignador());
        }

        sesion.getTablaInodos().guardarEnDisco();
        modificado = false;
    }

    public boolean estaActivo() { return activo; }

    public String getNombreArchivo() { return nombreArchivo; }
}

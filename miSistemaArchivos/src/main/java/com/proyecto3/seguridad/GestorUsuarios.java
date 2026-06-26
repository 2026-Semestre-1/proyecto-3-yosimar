package com.proyecto3.seguridad;

import com.proyecto3.nucleo.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class GestorUsuarios {

    public static final int UID_ROOT = 0;
    public static final int GID_ROOT = 0;
    //  Estos son los inodos para usuarios y grupos
    // Inodo 1 = usuarios, Inodo 2 = grupos
    public static final int SISTEMA_INODO_USUARIOS = 1;
    public static final int SISTEMA_INODO_GRUPOS = 2;

    private final List<Usuario> usuarios;
    private final List<Grupo> grupos;
    private int siguienteUid;
    private int siguienteGid;

    public GestorUsuarios() {
        this.usuarios = new ArrayList<>();
        this.grupos = new ArrayList<>();
        this.siguienteUid = 1;
        this.siguienteGid = 1;
    }

    public void inicializarSistema(String passwordRoot, int inodoHomeRoot,
                                    DiscoVirtual disco, AsignadorBloques asignador,
                                    TablaInodos tablaInodos) throws IOException {
        Grupo rootGroup = new Grupo(GID_ROOT, "root");
        grupos.add(rootGroup);

        Usuario root = new Usuario(UID_ROOT, "root", "Administrador del sistema",
            passwordRoot, GID_ROOT, inodoHomeRoot);
        usuarios.add(root);

        siguienteUid = 1;
        siguienteGid = 1;

        guardarEnDisco(disco, asignador, tablaInodos);
    }

    public void cargarDeDisco(DiscoVirtual disco, TablaInodos tablaInodos,
                               AsignadorBloques asignador) throws IOException {
        usuarios.clear();
        grupos.clear();

        cargarUsuarios(disco, tablaInodos, asignador);
        cargarGrupos(disco, tablaInodos, asignador);

        siguienteUid = 1;
        for (Usuario u : usuarios) {
            if (u.getUid() >= siguienteUid) siguienteUid = u.getUid() + 1;
        }
        siguienteGid = 1;
        for (Grupo g : grupos) {
            if (g.getGid() >= siguienteGid) siguienteGid = g.getGid() + 1;
        }
    }

    /**
     * Método para guardar la información de usuarios y grupos en el disco.
     * @param disco contexto del disco virtual
     * @param asignador asignador de bloques del sistema de archivos
     * @param tablaInodos tabla de inodos del sistema de archivos
     * @throws IOException
     */
    public void guardarEnDisco(DiscoVirtual disco, AsignadorBloques asignador,
                                TablaInodos tablaInodos) throws IOException {
        guardarUsuarios(disco, asignador, tablaInodos);
        guardarGrupos(disco, asignador, tablaInodos);
    }

    /**
     * Método para cargar la información de usuarios desde el disco.
     * @param disco contexto del disco virtual
     * @param tablaInodos tabla de inodos del sistema de archivos
     * @param asignador asignador de bloques del sistema de archivos
     * @throws IOException
     */
    private void cargarUsuarios(DiscoVirtual disco, TablaInodos tablaInodos,
                                 AsignadorBloques asignador) throws IOException {
        Inodo inodo = tablaInodos.getInodo(SISTEMA_INODO_USUARIOS);
        if (inodo.esLibre() || inodo.getTamanio() == 0) return;

        byte[] datos = leerDatosInodo(inodo, disco);
        ByteBuffer buf = ByteBuffer.wrap(datos);
        buf.order(ByteOrder.BIG_ENDIAN);

        int count = buf.getInt();
        for (int i = 0; i < count; i++) {
            byte[] userBytes = new byte[Usuario.TAMANIO];
            buf.get(userBytes);
            Usuario u = new Usuario();
            u.deserializar(userBytes);
            usuarios.add(u);
        }
    }

    /**
     * Método para guardar la información de usuarios en el disco.
     * @param disco contexto del disco virtual
     * @param asignador asignador de bloques del sistema de archivos
     * @param tablaInodos tabla de inodos del sistema de archivos
     * @throws IOException
     */
    private void guardarUsuarios(DiscoVirtual disco, AsignadorBloques asignador,
                                  TablaInodos tablaInodos) throws IOException {
        int dataSize = 4 + usuarios.size() * Usuario.TAMANIO;
        ByteBuffer buf = ByteBuffer.allocate(dataSize);
        buf.order(ByteOrder.BIG_ENDIAN);

        buf.putInt(usuarios.size());
        for (Usuario u : usuarios) {
            buf.put(u.serializar());
        }

        // Guardar los datos en el inodo 1
        escribirDatosInodo(SISTEMA_INODO_USUARIOS, buf.array(), disco, asignador, tablaInodos);
    }

    /**
     * Método para cargar la información de grupos desde el disco.
     * @param disco contexto del disco virtual
     * @param tablaInodos tabla de inodos del sistema de archivos
     * @param asignador asignador de bloques del sistema de archivos
     * @throws IOException
     */
    private void cargarGrupos(DiscoVirtual disco, TablaInodos tablaInodos,
                               AsignadorBloques asignador) throws IOException {
        Inodo inodo = tablaInodos.getInodo(SISTEMA_INODO_GRUPOS);
        if (inodo.esLibre() || inodo.getTamanio() == 0) return;

        byte[] datos = leerDatosInodo(inodo, disco);
        ByteBuffer buf = ByteBuffer.wrap(datos);
        buf.order(ByteOrder.BIG_ENDIAN);

        int count = buf.getInt();
        for (int i = 0; i < count; i++) {
            byte[] groupBytes = new byte[Grupo.TAMANIO];
            buf.get(groupBytes);
            Grupo g = new Grupo();
            g.deserializar(groupBytes);
            grupos.add(g);
        }
    }

    /**
     * Método para guardar la información de grupos en el disco.
     * @param disco contexto del disco virtual
     * @param asignador asignador de bloques del sistema de archivos
     * @param tablaInodos tabla de inodos del sistema de archivos
     * @throws IOException
     */
    private void guardarGrupos(DiscoVirtual disco, AsignadorBloques asignador,
                                TablaInodos tablaInodos) throws IOException {
        int dataSize = 4 + grupos.size() * Grupo.TAMANIO;
        ByteBuffer buf = ByteBuffer.allocate(dataSize);
        buf.order(ByteOrder.BIG_ENDIAN);

        buf.putInt(grupos.size());
        for (Grupo g : grupos) {
            buf.put(g.serializar());
        }

        escribirDatosInodo(SISTEMA_INODO_GRUPOS, buf.array(), disco, asignador, tablaInodos);
    }

    /**
     * Método para leer los datos de un inodo desde el disco.
     * @param inodo inodo del cual leer los datos
     * @param disco contexto del disco virtual
     * @return arreglo de bytes con los datos leídos
     * @throws IOException
     */
    private byte[] leerDatosInodo(Inodo inodo, DiscoVirtual disco) throws IOException {
        int tamanioBloque = disco.getTamanioBloque();
        int bloquesNecesarios = (int) Math.ceil((double) inodo.getTamanio() / tamanioBloque);

        byte[] todos = new byte[(int) inodo.getTamanio()];
        int offset = 0;
        int bloquesLeidos = 0;

        for (int i = 0; i < Inodo.PUNTEROS_DIRECTOS && bloquesLeidos < bloquesNecesarios; i++) {
            int numBloque = inodo.getPunteroDirecto(i);
            if (numBloque == Inodo.BLOQUE_NULO) break;
            byte[] bloque = disco.leerBloque(numBloque);
            int copiar = Math.min(tamanioBloque, todos.length - offset);
            System.arraycopy(bloque, 0, todos, offset, copiar);
            offset += copiar;
            bloquesLeidos++;
        }

        if (bloquesLeidos < bloquesNecesarios && inodo.getPunteroIndirecto() != Inodo.BLOQUE_NULO) {
            byte[] bloqueIndirecto = disco.leerBloque(inodo.getPunteroIndirecto());
            ByteBuffer bufIndirecto = ByteBuffer.wrap(bloqueIndirecto);
            bufIndirecto.order(ByteOrder.BIG_ENDIAN);

            int ptrsPorBloque = tamanioBloque / 4;
            for (int i = 0; i < ptrsPorBloque && bloquesLeidos < bloquesNecesarios; i++) {
                int numBloque = bufIndirecto.getInt();
                if (numBloque == Inodo.BLOQUE_NULO) break;
                byte[] bloque = disco.leerBloque(numBloque);
                int copiar = Math.min(tamanioBloque, todos.length - offset);
                System.arraycopy(bloque, 0, todos, offset, copiar);
                offset += copiar;
                bloquesLeidos++;
            }
        }

        return todos;
    }

    /**
     * Método para escribir datos en un inodo del disco.
     * @param numInodo número del inodo donde escribir los datos
     * @param datos arreglo de bytes con los datos a escribir
     * @param disco contexto del disco virtual
     * @param asignador asignador de bloques del sistema de archivos
     * @param tablaInodos tabla de inodos del sistema de archivos
     * @throws IOException
     */
    private void escribirDatosInodo(int numInodo, byte[] datos, DiscoVirtual disco,
                                     AsignadorBloques asignador, TablaInodos tablaInodos)
                                     throws IOException {
        Inodo inodo = tablaInodos.getInodo(numInodo);
        if (inodo.esLibre()) {
            inodo = tablaInodos.asignarInodo();
            inodo.setNumero(numInodo);
            inodo.setTipo(Inodo.ARCHIVO);
            inodo.setUid(UID_ROOT);
            inodo.setGid(GID_ROOT);
            inodo.setPermisos((short) 0600);
        }

        for (int i = 0; i < Inodo.PUNTEROS_DIRECTOS; i++) {
            int bloque = inodo.getPunteroDirecto(i);
            if (bloque != Inodo.BLOQUE_NULO) {
                asignador.liberar(bloque);
                inodo.setPunteroDirecto(i, Inodo.BLOQUE_NULO);
            }
        }
        if (inodo.getPunteroIndirecto() != Inodo.BLOQUE_NULO) {
            int indirectoBlock = inodo.getPunteroIndirecto();
            byte[] bloqueIndirecto = disco.leerBloque(indirectoBlock);
            ByteBuffer bufIndirecto = ByteBuffer.wrap(bloqueIndirecto);
            bufIndirecto.order(ByteOrder.BIG_ENDIAN);
            int ptrsPorBloque = disco.getTamanioBloque() / 4;
            for (int i = 0; i < ptrsPorBloque; i++) {
                int ptr = bufIndirecto.getInt();
                if (ptr != Inodo.BLOQUE_NULO) {
                    asignador.liberar(ptr);
                }
            }
            asignador.liberar(indirectoBlock);
            inodo.setPunteroIndirecto(Inodo.BLOQUE_NULO);
        }

        int tamanioBloque = disco.getTamanioBloque();
        int bloquesNecesarios = Math.max(1, (int) Math.ceil((double) datos.length / tamanioBloque));

        int offset = 0;
        int bloquesAsignados = 0;

        for (int i = 0; i < Inodo.PUNTEROS_DIRECTOS && bloquesAsignados < bloquesNecesarios; i++) {
            int nuevoBloque = asignador.asignar();
            inodo.setPunteroDirecto(i, nuevoBloque);
            byte[] bloque = new byte[tamanioBloque];
            int copiar = Math.min(tamanioBloque, datos.length - offset);
            System.arraycopy(datos, offset, bloque, 0, copiar);
            disco.escribirBloque(nuevoBloque, bloque);
            offset += copiar;
            bloquesAsignados++;
        }

        if (bloquesAsignados < bloquesNecesarios) {
            int indirectoBlock = asignador.asignar();
            inodo.setPunteroIndirecto(indirectoBlock);

            byte[] bloqueIndirecto = new byte[tamanioBloque];
            ByteBuffer bufIndirecto = ByteBuffer.wrap(bloqueIndirecto);
            bufIndirecto.order(ByteOrder.BIG_ENDIAN);
            int ptrsPorBloque = tamanioBloque / 4;

            int idxIndirecto = 0;
            while (bloquesAsignados < bloquesNecesarios && idxIndirecto < ptrsPorBloque) {
                int nuevoBloque = asignador.asignar();
                bufIndirecto.putInt(idxIndirecto * 4, nuevoBloque);
                byte[] bloque = new byte[tamanioBloque];
                int copiar = Math.min(tamanioBloque, datos.length - offset);
                System.arraycopy(datos, offset, bloque, 0, copiar);
                disco.escribirBloque(nuevoBloque, bloque);
                offset += copiar;
                bloquesAsignados++;
                idxIndirecto++;
            }

            disco.escribirBloque(indirectoBlock, bloqueIndirecto);
        }

        inodo.setTamanio(datos.length);
        inodo.setFechaModificacion(System.currentTimeMillis());
        tablaInodos.guardarEnDisco();
    }

    public Usuario crearUsuario(String nombre, String nombreCompleto, String password, int gid, int inodoHome) {
        Usuario u = new Usuario(siguienteUid++, nombre, nombreCompleto, password, gid, inodoHome);
        usuarios.add(u);
        return u;
    }

    public Grupo crearGrupo(String nombre) {
        Grupo g = new Grupo(siguienteGid++, nombre);
        grupos.add(g);
        return g;
    }

    public Usuario getUsuarioPorUid(int uid) {
        for (Usuario u : usuarios) {
            if (u.getUid() == uid) return u;
        }
        return null;
    }

    public Usuario getUsuarioPorNombre(String nombre) {
        for (Usuario u : usuarios) {
            if (u.getNombre().equals(nombre)) return u;
        }
        return null;
    }

    public Grupo getGrupoPorGid(int gid) {
        for (Grupo g : grupos) {
            if (g.getGid() == gid) return g;
        }
        return null;
    }

    public Grupo getGrupoPorNombre(String nombre) {
        for (Grupo g : grupos) {
            if (g.getNombre().equals(nombre)) return g;
        }
        return null;
    }

    public boolean esRoot(int uid) { return uid == UID_ROOT; }

    public List<Usuario> getUsuarios() { return usuarios; }
    public List<Grupo> getGrupos() { return grupos; }
    public Usuario getRoot() { return getUsuarioPorUid(UID_ROOT); }
    public Grupo getGrupoRoot() { return getGrupoPorGid(GID_ROOT); }
}

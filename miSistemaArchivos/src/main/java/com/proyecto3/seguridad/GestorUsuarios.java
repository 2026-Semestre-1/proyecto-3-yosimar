package com.proyecto3.seguridad;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase que gestiona los usuarios y grupos del sistema de archivos
 * Permite crear, buscar y gestionar usuarios y grupos
 */
public class GestorUsuarios {

    public static final int UID_ROOT = 0;
    public static final int GID_ROOT = 0;

    private final List<Usuario> usuarios;
    private final List<Grupo> grupos;
    private int siguienteUid;
    private int siguienteGid;

    /**
     * Constructor de la clase GestorUsuarios
     * Inicializa las listas de usuarios y grupos, y los contadores de UID y GID
     */
    public GestorUsuarios() {
        this.usuarios = new ArrayList<>();
        this.grupos = new ArrayList<>();
        this.siguienteUid = 1;
        this.siguienteGid = 1;
    }

    /**
     * Inicializa el sistema de usuarios y grupos con el usuario root y el grupo root
     * @param passwordRoot la contraseña del usuario root 
     * @param inodoHomeRoot el número de inodo del directorio home del usuario root
     * @throws IOException
     */
    public void inicializarSistema(String passwordRoot, int inodoHomeRoot) throws IOException {
        Grupo rootGroup = new Grupo(GID_ROOT, "root"); // Se crea el grupo root con GID 0 y nombre "root"
        grupos.add(rootGroup); // Se agrega el grupo root a la lista de grupos

        // Se crea el usuario root con UID 0, nombre "root", nombre completo "Administrador del sistema", la contraseña proporcionada, GID 0 y el inodo home proporcionado
        Usuario root = new Usuario(UID_ROOT, "root", "Administrador del sistema",
            passwordRoot, GID_ROOT, inodoHomeRoot);
        usuarios.add(root); // Se agrega el usuario root a la lista de usuarios

        // Se inicializan los contadores de UID y GID para los próximos usuarios y grupos que se creen
        siguienteUid = 1;
        siguienteGid = 1;
    }

    /**
     * Crea un nuevo usuario en el sistema
     * @param nombre el nombre del usuario
     * @param nombreCompleto el nombre completo del usuario
     * @param password la contraseña del usuario
     * @param gid el identificador del grupo al que pertenece el usuario
     * @param inodoHome el número de inodo del directorio home del usuario
     * @return el usuario creado
     */
    public Usuario crearUsuario(String nombre, String nombreCompleto, String password, int gid, int inodoHome) {
        Usuario u = new Usuario(siguienteUid++, nombre, nombreCompleto, password, gid, inodoHome);
        usuarios.add(u);
        return u;
    }

    /**
     * Crea un nuevo grupo en el sistema
     * @param nombre el nombre del grupo
     * @return el grupo creado
     */
    public Grupo crearGrupo(String nombre) {
        Grupo g = new Grupo(siguienteGid++, nombre);
        grupos.add(g);
        return g;
    }

    /**
     * Obtiene un usuario por su identificador único (UID)
     * @param uid el identificador único del usuario
     * @return el usuario encontrado o null si no existe
     */
    public Usuario getUsuarioPorUid(int uid) {
        for (Usuario u : usuarios) {
            if (u.getUid() == uid) return u;
        }
        return null;
    }

    /**
     * Obtiene un usuario por su nombre
     * @param nombre el nombre del usuario
     * @return el usuario encontrado o null si no existe
     */
    public Usuario getUsuarioPorNombre(String nombre) {
        for (Usuario u : usuarios) {
            if (u.getNombre().equals(nombre)) return u;
        }
        return null;
    }

    /**
     * Obtiene un grupo por su identificador único (GID)
     * @param gid el identificador único del grupo
     * @return el grupo encontrado o null si no existe
     */
    public Grupo getGrupoPorGid(int gid) {
        for (Grupo g : grupos) {
            if (g.getGid() == gid) return g;
        }
        return null;
    }

    /**
     * Obtiene un grupo por su nombre
     * @param nombre el nombre del grupo
     * @return el grupo encontrado o null si no existe
     */
    public Grupo getGrupoPorNombre(String nombre) {
        for (Grupo g : grupos) {
            if (g.getNombre().equals(nombre)) return g;
        }
        return null;
    }

    public boolean esRoot(int uid) {
        return uid == UID_ROOT;
    }

    public List<Usuario> getUsuarios() { return usuarios; }
    public List<Grupo> getGrupos() { return grupos; }
    public Usuario getRoot() { return getUsuarioPorUid(UID_ROOT); }
    public Grupo getGrupoRoot() { return getGrupoPorGid(GID_ROOT); }
}

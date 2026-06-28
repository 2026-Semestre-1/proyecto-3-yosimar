package com.proyecto3.sesion;

import com.proyecto3.nucleo.*;
import com.proyecto3.seguridad.GestorUsuarios;
import com.proyecto3.seguridad.Usuario;

/**
 * Clase que representa una sesión de usuario en el sistema de archivos.
 * Mantiene información sobre el usuario actual, el directorio de trabajo y proporciona métodos para autenticación.
 */
public class Sesion {

    private final DiscoVirtual disco;
    private final Superbloque superbloque;
    private AsignadorBloques asignador;
    private TablaInodos tablaInodos;
    private final GestorUsuarios gestorUsuarios;
    private TablaArchivosAbiertos tablaArchivosAbiertos;

    private Usuario usuarioActual;
    private int inodoDirectorioTrabajo;

    /**
     * Constructor de la clase Sesion
     * @param disco contexto del disco virtual
     * @param superbloque superbloque del sistema de archivos
     * @param asignador asignador de bloques del sistema de archivos
     * @param tablaInodos tabla de inodos del sistema de archivos
     * @param gestorUsuarios gestor de usuarios del sistema de archivos
     */
    public Sesion(DiscoVirtual disco, Superbloque superbloque, AsignadorBloques asignador,
                  TablaInodos tablaInodos, GestorUsuarios gestorUsuarios) {
        this.disco = disco;
        this.superbloque = superbloque;
        this.asignador = asignador;
        this.tablaInodos = tablaInodos;
        this.gestorUsuarios = gestorUsuarios;
        this.tablaArchivosAbiertos = null;
        this.inodoDirectorioTrabajo = superbloque.getInodoRaiz();
    }

    /**
     * Método para autenticar a un usuario en el sistema de archivos.
     * @param nombreUsuario nombre del usuario a autenticar
     * @param password contraseña del usuario
     * @return true si la autenticación es exitosa, false en caso contrario
     */
    public boolean login(String nombreUsuario, String password) {
        Usuario u = gestorUsuarios.getUsuarioPorNombre(nombreUsuario);
        if (u != null && u.verificarPassword(password)) {
            this.usuarioActual = u;
            this.inodoDirectorioTrabajo = u.getInodoHome();
            return true;
        }
        return false;
    }

    /**
     * Método para cambiar al usuario root.
     * @param nombreUsuario nombre del usuario a cambiar
     * @return true si el cambio es exitoso, false en caso contrario
     */
    public boolean su(String nombreUsuario) {
        Usuario u = gestorUsuarios.getUsuarioPorNombre(nombreUsuario);
        if (u != null) {
            this.usuarioActual = u;
            return true;
        }
        return false;
    }

    /**
     * Método para cerrar la sesión del usuario actual.
     */
    public void logout() {
        this.usuarioActual = null;
    }

    public boolean estaAutenticado() {
        return usuarioActual != null;
    }

    public boolean esRoot() {
        return estaAutenticado() && usuarioActual.getUid() == GestorUsuarios.UID_ROOT;
    }

    public Usuario getUsuarioActual() { return usuarioActual; }
    public int getInodoDirectorioTrabajo() { return inodoDirectorioTrabajo; }
    public void setInodoDirectorioTrabajo(int inodo) { this.inodoDirectorioTrabajo = inodo; }

    public DiscoVirtual getDisco() { return disco; }
    public Superbloque getSuperbloque() { return superbloque; }
    public AsignadorBloques getAsignador() { return asignador; }
    public void setAsignador(AsignadorBloques ab) { this.asignador = ab; }
    public TablaInodos getTablaInodos() { return tablaInodos; }
    public void setTablaInodos(TablaInodos ti) { this.tablaInodos = ti; }
    public GestorUsuarios getGestorUsuarios() { return gestorUsuarios; }
    public TablaArchivosAbiertos getTablaArchivosAbiertos() { return tablaArchivosAbiertos; }
    public void setTablaArchivosAbiertos(TablaArchivosAbiertos t) { this.tablaArchivosAbiertos = t; }
}

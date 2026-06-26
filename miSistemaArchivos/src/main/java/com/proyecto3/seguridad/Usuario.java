package com.proyecto3.seguridad;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Clase que representa un usuario en el sistema de archivos
 * Un usuario tiene un identificador único (uid), un nombre, un nombre completo, una contraseña y un grupo al que pertenece
 */
public class Usuario {

    public static final int TAMANIO = 192;
    public static final int MAX_NOMBRE = 32;
    public static final int MAX_COMPLETO = 64;

    private int uid;
    private String nombre;
    private String nombreCompleto;
    private String hashPassword;
    private int gid;
    private int inodoHome;

    /**
     * Constructor por defecto
     */
    public Usuario() {}

    /**
     * Constructor con parámetros
     * @param uid el identificador del usuario
     * @param nombre el nombre del usuario
     * @param nombreCompleto el nombre completo del usuario
     * @param password la contraseña del usuario
     * @param gid el identificador del grupo al que pertenece
     * @param inodoHome el número de inodo del directorio home del usuario
     */
    public Usuario(int uid, String nombre, String nombreCompleto, String password, int gid, int inodoHome) {
        this.uid = uid;
        this.nombre = nombre;
        this.nombreCompleto = nombreCompleto;
        this.hashPassword = hashPassword(password);
        this.gid = gid;
        this.inodoHome = inodoHome;
    }

    /**
     * Genera un hash SHA-256 de la contraseña para almacenarla de forma segura
     * @param password la contraseña en texto plano
     * @return el hash SHA-256 de la contraseña
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 no disponible", e);
        }
    }

    /**
     * Verifica si la contraseña proporcionada coincide con el hash almacenado
     * @param password la contraseña en texto plano a verificar
     * @return true si la contraseña coincide, false en caso contrario
     */
    public boolean verificarPassword(String password) {
        return hashPassword(password).equals(this.hashPassword);
    }

    /**
     * Serializa el usuario a un arreglo de bytes para poder guardarlo en disco
     * @return un arreglo de bytes que representa el usuario
     */
    public byte[] serializar() {
        ByteBuffer buf = ByteBuffer.allocate(TAMANIO);
        buf.order(ByteOrder.BIG_ENDIAN);

        // Se toma toda la información del usuario para retornarla
        buf.putInt(uid);
        buf.put(cadenaABytes(nombre, MAX_NOMBRE));
        buf.put(cadenaABytes(nombreCompleto, MAX_COMPLETO));
        buf.put(cadenaABytes(hashPassword != null ? hashPassword : "", 64));
        buf.putInt(gid);
        buf.putInt(inodoHome);

        // Se retorna el arreglo de bytes que representa al usuario
        return buf.array();
    }

    /**
     * Deserializa un arreglo de bytes para reconstruir el usuario
     * @param datos el arreglo de bytes que representa el usuario
     */
    public void deserializar(byte[] datos) {
        ByteBuffer buf = ByteBuffer.wrap(datos);
        buf.order(ByteOrder.BIG_ENDIAN);

        // Se toma toda la información del usuario del arreglo de bytes y se asigna a los atributos del usuario
        this.uid = buf.getInt();
        this.nombre = bytesACadena(buf, MAX_NOMBRE);
        this.nombreCompleto = bytesACadena(buf, MAX_COMPLETO);
        this.hashPassword = bytesACadena(buf, 64);
        this.gid = buf.getInt();
        this.inodoHome = buf.getInt();
    }

    // Accesores y mutadores para los atributos del usuario
    private byte[] cadenaABytes(String s, int max) {
        byte[] arr = new byte[max];
        if (s != null) {
            byte[] src = s.getBytes(StandardCharsets.UTF_8);
            System.arraycopy(src, 0, arr, 0, Math.min(src.length, max));
        }
        return arr;
    }

    private String bytesACadena(ByteBuffer buf, int max) {
        byte[] arr = new byte[max];
        buf.get(arr);
        int fin = 0;
        while (fin < max && arr[fin] != 0) fin++;
        return new String(arr, 0, fin, StandardCharsets.UTF_8);
    }

    public int getUid() { return uid; }
    public void setUid(int u) { this.uid = u; }
    public String getNombre() { return nombre; }
    public void setNombre(String n) { this.nombre = n; }
    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String n) { this.nombreCompleto = n; }
    public String getHashPassword() { return hashPassword; }
    public void setHashPassword(String h) { this.hashPassword = h; }
    public int getGid() { return gid; }
    public void setGid(int g) { this.gid = g; }
    public int getInodoHome() { return inodoHome; }
    public void setInodoHome(int i) { this.inodoHome = i; }

    @Override
    public String toString() {
        return nombre + ":" + uid + ":" + gid;
    }
}

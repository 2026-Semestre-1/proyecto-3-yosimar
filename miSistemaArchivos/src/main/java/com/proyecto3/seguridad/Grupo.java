package com.proyecto3.seguridad;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Clase que representa un grupo de usuarios en el sistema de archivos
 * Un grupo tiene un identificador único (gid) y un nombre
 */
public class Grupo {

    public static final int TAMANIO = 64;
    public static final int MAX_NOMBRE = 32;

    private int gid;
    private String nombre;

    /**
     * Constructor por defecto
     */
    public Grupo() {}

    /**
     * Constructor con parámetros
     * @param gid el identificador del grupo
     * @param nombre el nombre del grupo
     */
    public Grupo(int gid, String nombre) {
        this.gid = gid;
        this.nombre = nombre;
    }

    /**
     * Serializa el grupo a un arreglo de bytes para poder guardarlo en disco
     * @return un arreglo de bytes que representa el grupo
     */
    public byte[] serializar() {
        ByteBuffer buf = ByteBuffer.allocate(TAMANIO);
        buf.order(ByteOrder.BIG_ENDIAN);

        buf.putInt(gid);
        byte[] nombreBytes = new byte[MAX_NOMBRE];
        if (nombre != null) {
            byte[] src = nombre.getBytes(StandardCharsets.UTF_8);
            System.arraycopy(src, 0, nombreBytes, 0, Math.min(src.length, MAX_NOMBRE));
        }
        buf.put(nombreBytes);

        return buf.array();
    }

    /**
     * Deserializa un arreglo de bytes para reconstruir el grupo
     * @param datos el arreglo de bytes que representa el grupo
     */
    public void deserializar(byte[] datos) {
        ByteBuffer buf = ByteBuffer.wrap(datos);
        buf.order(ByteOrder.BIG_ENDIAN);

        // Se toma el gid y el nombre del grupo del arreglo de bytes
        this.gid = buf.getInt();
        byte[] nombreBytes = new byte[MAX_NOMBRE];
        buf.get(nombreBytes);
        int fin = 0;
        while (fin < MAX_NOMBRE && nombreBytes[fin] != 0) fin++;
        this.nombre = new String(nombreBytes, 0, fin, StandardCharsets.UTF_8);
    }

    // Accesores y mutadores para los atributos del grupo
    public int getGid() { return gid; }
    public void setGid(int g) { this.gid = g; }
    public String getNombre() { return nombre; }
    public void setNombre(String n) { this.nombre = n; }
}

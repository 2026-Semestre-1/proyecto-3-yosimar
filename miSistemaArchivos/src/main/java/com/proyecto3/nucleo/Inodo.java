package com.proyecto3.nucleo;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Clase que representa un inodo en el sistema de archivos
 * Un inodo contiene toda la información necesaria para acceder a un archivo o directorio
 */
public class Inodo {

    public static final int TAMANIO = 256;
    public static final int PUNTEROS_DIRECTOS = 10;
    public static final int LIBRE = 0;
    public static final int ARCHIVO = 1;
    public static final int DIRECTORIO = 2;

    public static final int BLOQUE_NULO = -1;

    private int numero;
    private int tipo;
    private int uid;
    private int gid;
    private short permisos;
    private long tamanio;
    private long fechaCreacion;
    private long fechaModificacion;
    private boolean abierto;
    private int enlaces;
    private final int[] punterosDirectos;
    private int punteroIndirecto;

    /**
     * Constructor de la clase Inodo
     */
    public Inodo() {
        this.tipo = LIBRE;
        this.permisos = 0777;
        this.punterosDirectos = new int[PUNTEROS_DIRECTOS];
        this.punteroIndirecto = BLOQUE_NULO;
        // se pone punteros en nlo (-1) para indicar que no apuntan a ningún bloque
        for (int i = 0; i < PUNTEROS_DIRECTOS; i++) {
            punterosDirectos[i] = BLOQUE_NULO;
        }
        long ahora = System.currentTimeMillis();
        this.fechaCreacion = ahora;
        this.fechaModificacion = ahora;
        this.enlaces = 1;
    }

    /**
     * Serializa el inodo a un arreglo de bytes para poder guardarlo en disco
     * @return un arreglo de bytes que representa el inodo
     */
    public byte[] serializar() {
        ByteBuffer buf = ByteBuffer.allocate(TAMANIO);
        buf.order(ByteOrder.BIG_ENDIAN);

        // Tomamos toda la información del inodo para retornarla 
        buf.putInt(numero);
        buf.put((byte) tipo);
        buf.putInt(uid);
        buf.putInt(gid);
        buf.putShort(permisos);
        buf.putLong(tamanio);
        buf.putLong(fechaCreacion);
        buf.putLong(fechaModificacion);
        buf.put((byte) (abierto ? 1 : 0));
        buf.putInt(enlaces);
        for (int i = 0; i < PUNTEROS_DIRECTOS; i++) {
            buf.putInt(punterosDirectos[i]);
        }
        buf.putInt(punteroIndirecto);

        return buf.array(); // se retorna como arreglo de bytes
        /**
         * la estructura del retorno es:
         * numero (4 bytes) + tipo (1 byte) + uid (4 bytes) 
         * gid (4 bytes) + permisos (2 bytes) + tamanio (8 bytes)
         * fechaCreacion (8 bytes) + fechaModificacion (8 bytes)
         * abierto (1 byte) + enlaces (4 bytes) + punterosDirectos (10 * 4 bytes) + punteroIndirecto (4 bytes)
         * Total: 4 + 1 + 4 + 4 + 2 + 8 + 8 + 8 + 1 + 4 + 40 + 4 = 256 bytes
         */
    }

    /**
     * Deserializa un arreglo de bytes a un inodo
     * @param datos el arreglo de bytes que representa el inodo
     */
    public void deserializar(byte[] datos) {
        ByteBuffer buf = ByteBuffer.wrap(datos);
        buf.order(ByteOrder.BIG_ENDIAN);

        // Tomamos cada info del inodo del arreglo de bytes y la asignamos a los atributos del inodo
        this.numero = buf.getInt();
        this.tipo = buf.get() & 0xFF;
        this.uid = buf.getInt();
        this.gid = buf.getInt();
        this.permisos = buf.getShort();
        this.tamanio = buf.getLong();
        this.fechaCreacion = buf.getLong();
        this.fechaModificacion = buf.getLong();
        this.abierto = buf.get() != 0;
        this.enlaces = buf.getInt();
        for (int i = 0; i < PUNTEROS_DIRECTOS; i++) {
            this.punterosDirectos[i] = buf.getInt();
        }
        this.punteroIndirecto = buf.getInt();
    }

    // Accesores y mutadores para los atributos del inodo
    public boolean esLibre() { return tipo == LIBRE; }
    public boolean esArchivo() { return tipo == ARCHIVO; }
    public boolean esDirectorio() { return tipo == DIRECTORIO; }

    public int getNumero() { return numero; }
    public void setNumero(int n) { this.numero = n; }
    public int getTipo() { return tipo; }
    public void setTipo(int t) { this.tipo = t; }
    public int getUid() { return uid; }
    public void setUid(int u) { this.uid = u; }
    public int getGid() { return gid; }
    public void setGid(int g) { this.gid = g; }
    public short getPermisos() { return permisos; }
    public void setPermisos(short p) { this.permisos = p; }
    public long getTamanio() { return tamanio; }
    public void setTamanio(long t) { this.tamanio = t; }
    public long getFechaCreacion() { return fechaCreacion; }
    public long getFechaModificacion() { return fechaModificacion; }
    public void setFechaModificacion(long f) { this.fechaModificacion = f; }
    public boolean isAbierto() { return abierto; }
    public void setAbierto(boolean a) { this.abierto = a; }
    public int getEnlaces() { return enlaces; }
    public void setEnlaces(int e) { this.enlaces = e; }
    public int getPunteroDirecto(int i) { return punterosDirectos[i]; }
    public void setPunteroDirecto(int i, int bloque) { this.punterosDirectos[i] = bloque; }
    public int getPunteroIndirecto() { return punteroIndirecto; }
    public void setPunteroIndirecto(int bloque) { this.punteroIndirecto = bloque; }
    public int[] getPunterosDirectos() { return punterosDirectos; }
}

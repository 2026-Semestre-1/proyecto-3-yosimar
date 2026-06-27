package com.proyecto3.nucleo;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Representa una entrada en un directorio, que contiene un nombre y un número de inodo.
 */
public class EntradaDirectorio {

    public static final int MAX_NOMBRE = 128;

    private String nombre;
    private int numeroInodo;

    public EntradaDirectorio() {}

    /**
     * Crea una nueva entrada de directorio con el nombre y número de inodo especificados.
     * @param nombre El nombre de la entrada.
     * @param numeroInodo El número del inodo asociado a la entrada.
     */
    public EntradaDirectorio(String nombre, int numeroInodo) {
        this.nombre = nombre;
        this.numeroInodo = numeroInodo;
    }

    /**
     * Serializa la entrada de directorio en un array de bytes.
     * @return El array de bytes con la entrada serializada.
     */
    public byte[] serializar() {
        byte[] nombreBytes = nombre.getBytes(StandardCharsets.UTF_8);
        int len = Math.min(nombreBytes.length, MAX_NOMBRE - 1);
        byte[] buf = new byte[1 + len + 4];
        buf[0] = (byte) len;
        System.arraycopy(nombreBytes, 0, buf, 1, len);
        ByteBuffer bb = ByteBuffer.wrap(buf, 1 + len, 4);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.putInt(numeroInodo);
        return buf;
    }

    /**
     * Deserializa una entrada de directorio desde un array de bytes.
     * @param datos El array de bytes que contiene la entrada serializada.
     * @param offset El desplazamiento en el array donde comienza la entrada.
     * @param limite El límite del array de bytes.
     * @return La entrada de directorio deserializada, o null si no se puede deserializar.
     */
    public static EntradaDirectorio deserializar(byte[] datos, int offset, int limite) {
        if (offset + 1 > limite) return null;
        int nameLen = datos[offset] & 0xFF;
        if (nameLen == 0 || offset + 1 + nameLen + 4 > limite) return null;
        String nombre = new String(datos, offset + 1, nameLen, StandardCharsets.UTF_8);
        ByteBuffer bb = ByteBuffer.wrap(datos, offset + 1 + nameLen, 4);
        bb.order(ByteOrder.BIG_ENDIAN);
        int inodo = bb.getInt();
        return new EntradaDirectorio(nombre, inodo);
    }

    public static int tamanoSerializado(byte[] datos, int offset) {
        int nameLen = datos[offset] & 0xFF;
        return 1 + nameLen + 4;
    }

    public String getNombre() { return nombre; }
    public void setNombre(String n) { this.nombre = n; }
    public int getNumeroInodo() { return numeroInodo; }
    public void setNumeroInodo(int i) { this.numeroInodo = i; }

    @Override
    public String toString() {
        return nombre + " -> inodo " + numeroInodo;
    }
}

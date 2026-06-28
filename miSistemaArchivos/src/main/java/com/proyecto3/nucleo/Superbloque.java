package com.proyecto3.nucleo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Clase que representa el superbloque de un sistema de archivos
 * Tiene toda la información necesaria para poder acceder a los bloques de datos
 * y a los inodos
 */
public class Superbloque {

    public static final int MAGIC = 0x534F4653; // "SOFS" en hexadecimal, SOFS = Sistema de Archivos
    public static final int TAMANIO = 512;
    public static final int MAX_NOMBRE = 32;

    private String nombreFs;
    private long tamanioTotal;
    private int tamanioBloque;
    private int totalBloques;
    private int bloqueInicioBitmap;
    private int bloqueInicioInodos;
    private int bloqueInicioDatos;
    private int totalInodos;
    private int inodoRaiz;

    public Superbloque() {
        this.nombreFs = "miSistemaArchivos"; // por defecto
    }

    /**
     * Guarda el superbloque en el disco virtual
     * 
     * @param disco el disco virtual donde se va a guardar el superbloque
     * @throws IOException
     */
    public void guardar(DiscoVirtual disco) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(TAMANIO); // 512 bytes de buffer
        buf.order(ByteOrder.BIG_ENDIAN); // orden big endian para escribir

        buf.putInt(MAGIC); // el magic number al inicio

        byte[] nombreBytes = new byte[MAX_NOMBRE];
        byte[] src = nombreFs.getBytes(StandardCharsets.UTF_8);
        int len = Math.min(src.length, MAX_NOMBRE - 1);
        System.arraycopy(src, 0, nombreBytes, 0, len);
        buf.put(nombreBytes);
        // Ya aqui cargamos el resto de los campos del superbloque
        buf.putLong(tamanioTotal);
        buf.putInt(tamanioBloque);
        buf.putInt(totalBloques);
        buf.putInt(bloqueInicioBitmap);
        buf.putInt(bloqueInicioInodos);
        buf.putInt(bloqueInicioDatos);
        buf.putInt(totalInodos);
        buf.putInt(inodoRaiz);

        // Se pega todo el buffer en el bloque 0 del disco virtual
        disco.escribirBloque(0, buf.array());
    }

    /**
     * Carga el superbloque desde el disco virtual
     * 
     * @param disco el disco virtual desde donde se va a cargar el superbloque
     * @throws IOException
     */
    public void cargar(DiscoVirtual disco) throws IOException {
        byte[] datos = disco.leerBloque(0);
        ByteBuffer buf = ByteBuffer.wrap(datos);
        buf.order(ByteOrder.BIG_ENDIAN);

        int magic = buf.getInt(); // getInt nos dará el magic number del superbloque
        if (magic != MAGIC) { // Esto basicamente nos sirve para validar si el disco fue escrito por este
                              // sistema de archivos o no
            throw new IOException("No es un disco del sistema de archivos válido (magic=" +
                    Integer.toHexString(magic) + ", esperado=" + Integer.toHexString(MAGIC) + ")");
        }

        byte[] nombreBytes = new byte[MAX_NOMBRE];
        buf.get(nombreBytes);
        int fin = 0;
        // Tomamos el nombre del sistema de archivos hasta el primer byte nulo o hasta
        // MAX_NOMBRE
        while (fin < MAX_NOMBRE && nombreBytes[fin] != 0)
            fin++;
        this.nombreFs = new String(nombreBytes, 0, fin, StandardCharsets.UTF_8);

        // Cargamos el resto de los campos del superbloque
        this.tamanioTotal = buf.getLong();
        this.tamanioBloque = buf.getInt();
        this.totalBloques = buf.getInt();
        this.bloqueInicioBitmap = buf.getInt();
        this.bloqueInicioInodos = buf.getInt();
        this.bloqueInicioDatos = buf.getInt();
        this.totalInodos = buf.getInt();
        this.inodoRaiz = buf.getInt();
    }

    /**
     * Formatea el disco virtual con un nuevo sistema de archivos
     * 
     * @param nombreFs
     * @param disco
     * @param tamanioBytes
     * @param totalInodos
     * @return
     * @throws IOException
     */
    public String formatearCon(String nombreFs, DiscoVirtual disco, long tamanioBytes,
            int totalInodos) throws IOException {
        LayoutDisco layout = new LayoutDisco(tamanioBytes, disco.getTamanioBloque(), totalInodos);

        // Tomamos los datos del layout y los guardamos en el superbloque
        this.nombreFs = nombreFs;
        this.tamanioTotal = tamanioBytes;
        this.tamanioBloque = disco.getTamanioBloque();
        this.totalBloques = layout.getTotalBloques();
        this.bloqueInicioBitmap = layout.getBloqueInicioBitmap();
        this.bloqueInicioInodos = layout.getBloqueInicioInodos();
        this.bloqueInicioDatos = layout.getBloqueInicioDatos();
        this.totalInodos = totalInodos;
        this.inodoRaiz = 0;

        // Simplemente se guarda segun la información que se ha cargado en el superbloque
        guardar(disco);
        return nombreFs;
    }

    // Getters y setters para los campos del superbloque
    public LayoutDisco getLayout() {
        return new LayoutDisco(tamanioTotal, tamanioBloque, totalInodos);
    }

    public String getNombreFs() {
        return nombreFs;
    }

    public int getMagic() {
        return MAGIC;
    }

    public void setNombreFs(String n) {
        this.nombreFs = n;
    }

    public long getTamanioTotal() {
        return tamanioTotal;
    }

    public int getTamanioBloque() {
        return tamanioBloque;
    }

    public int getTotalBloques() {
        return totalBloques;
    }

    public int getBloqueInicioBitmap() {
        return bloqueInicioBitmap;
    }

    public int getBloqueInicioInodos() {
        return bloqueInicioInodos;
    }

    public int getBloqueInicioDatos() {
        return bloqueInicioDatos;
    }

    public int getTotalInodos() {
        return totalInodos;
    }

    public int getInodoRaiz() {
        return inodoRaiz;
    }

    public void setInodoRaiz(int inodo) {
        this.inodoRaiz = inodo;
    }
}

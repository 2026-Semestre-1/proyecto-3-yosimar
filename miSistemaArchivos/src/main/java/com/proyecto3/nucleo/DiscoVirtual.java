package com.proyecto3.nucleo;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Clase que representa un disco virtual utilizando un archivo en el sistema de archivos del host.
 * Permite crear, abrir, leer y escribir bloques de datos.
 */
public class DiscoVirtual {

    private final int tamanioBloque; // Tamaño del bloque en bytes
    private RandomAccessFile archivo;
    private boolean abierto;

    /**
     * Constructor por defecto que crea un disco virtual con un tamaño de bloque de 512 bytes.
     */
    public DiscoVirtual() {
        this(512);
    }

    /**
     * Constructor que permite especificar el tamaño del bloque en bytes.
     * @param tamanioBloque
     */
    public DiscoVirtual(int tamanioBloque) {
        if (tamanioBloque < 1) {
            throw new IllegalArgumentException("El tamaño de bloque debe ser positivo");
        }
        this.tamanioBloque = tamanioBloque;
        this.abierto = false;
    }

    /**
     * Crea un nuevo disco virtual con el tamaño especificado en bytes. Si el archivo ya existe, se sobrescribirá.
     * @param ruta
     * @param tamanioBytes
     * @throws IOException
     */
    public void crearDisco(String ruta, long tamanioBytes) throws IOException {
        archivo = new RandomAccessFile(ruta, "rw");
        archivo.setLength(tamanioBytes);
        abierto = true;
    }

    /**
     * Abre un disco virtual existente.
     * @param ruta
     * @throws IOException
     */
    public void abrirDisco(String ruta) throws IOException {
        archivo = new RandomAccessFile(ruta, "rw");
        abierto = true;
    }

    /**
     * Lee un bloque del disco virtual.
     * @param numBloque
     * @return
     * @throws IOException
     */
    public byte[] leerBloque(int numBloque) throws IOException {
        verificarAbierto();
        byte[] datos = new byte[tamanioBloque];
        archivo.seek((long) numBloque * tamanioBloque);
        archivo.readFully(datos);
        return datos;
    }

    /**
     * Escribe un bloque en el disco virtual. Si los datos son menores que el tamaño del bloque, se rellenará con ceros.
     * @param numBloque
     * @param datos
     * @throws IOException
     */
    public void escribirBloque(int numBloque, byte[] datos) throws IOException {
        verificarAbierto();
        if (datos.length > tamanioBloque) {
            throw new IllegalArgumentException(
                "Los datos (" + datos.length + " bytes) exceden el tamaño de bloque (" + tamanioBloque + " bytes)"
            );
        }
        // Se crea un bloque completo, rellenando con ceros si los datos son menores que el tamaño del bloque
        byte[] bloque = new byte[tamanioBloque];
        System.arraycopy(datos, 0, bloque, 0, datos.length); // Copia los datos al bloque, dejando el resto como ceros
        archivo.seek((long) numBloque * tamanioBloque);
        archivo.write(bloque); // Escribe el bloque completo en el disco virtual
    }

    /**
     * Cierra el disco virtual, asegurándose de liberar los recursos asociados
     * Después de cerrar, el disco no se puede usar hasta que se vuelva a abrir o crear
     * @throws IOException
     */
    public void cerrar() throws IOException {
        if (archivo != null) {
            archivo.close();
            archivo = null;
        }
        abierto = false;
    }

    /**
     * @return true si el disco está abierto, false en caso contrario
     */
    public boolean estaAbierto() {
        return abierto && archivo != null;
    }

    public int getTamanioBloque() {
        return tamanioBloque;
    }

    public long getTotalBloques() throws IOException {
        verificarAbierto();
        return archivo.length() / tamanioBloque;
    }

    private void verificarAbierto() {
        if (!estaAbierto()) {
            throw new IllegalStateException("El disco no está abierto");
        }
    }
}

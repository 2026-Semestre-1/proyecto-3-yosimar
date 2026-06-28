package com.proyecto3.nucleo;

import java.io.IOException;
import java.util.Arrays;

/**
 * Clase que se encarga de la asignación y liberación de bloques en el sistema de archivos
 */
public class AsignadorBloques {

    private final DiscoVirtual disco;
    private final LayoutDisco layout;
    private final byte[] bitmap; // Este bitmap representa los bloques libres y ocupados del disco virtual, es la forma más eficiente de llevar el control de los bloques
    private final int totalBits;
    private int bloquesLibres;

    /**
     * Constructor de la clase AsignadorBloques
     * @param disco el disco virtual donde se va a gestionar la asignación de bloques
     * @param layout el layout del disco virtual
     */
    public AsignadorBloques(DiscoVirtual disco, LayoutDisco layout) {
        this.disco = disco;
        this.layout = layout;
        this.totalBits = layout.getTotalBloques();
        int bytesBitmap = layout.getBloquesBitmap() * disco.getTamanioBloque();
        this.bitmap = new byte[bytesBitmap];
        this.bloquesLibres = totalBits;
    }

    /**
     * Inicializa el bitmap con todos los bloques marcados como libres
     * @throws IOException
     */
    public void inicializarBitmap() throws IOException {
        Arrays.fill(bitmap, (byte) 0);
        int bloquesMetadata = layout.getBloqueInicioDatos();
        for (int i = 0; i < bloquesMetadata; i++) {
            marcarOcupado(i);
        }
    }

    /**
     * Carga el bitmap desde el disco virtual
     * @throws IOException
     */
    public void cargarDeDisco() throws IOException {
        int bloques = layout.getBloquesBitmap();
        int inicio = layout.getBloqueInicioBitmap();
        int tamanioBloque = disco.getTamanioBloque();
        int offset = 0;

        for (int i = 0; i < bloques; i++) {
            byte[] bloque = disco.leerBloque(inicio + i);
            int copiar = Math.min(tamanioBloque, bitmap.length - offset);
            System.arraycopy(bloque, 0, bitmap, offset, copiar);
            offset += copiar;
        }
        contarLibres();
    }

    /**
     * Guarda el bitmap en el disco virtual
     * @throws IOException
     */
    public void guardarEnDisco() throws IOException {
        int bloques = layout.getBloquesBitmap();
        int inicio = layout.getBloqueInicioBitmap();
        int tamanioBloque = disco.getTamanioBloque();
        int offset = 0;

        // por cada bloque del bitmap, se escribe en el disco virtual
        for (int i = 0; i < bloques; i++) {
            byte[] bloque = new byte[tamanioBloque];
            int copiar = Math.min(tamanioBloque, bitmap.length - offset);
            if (copiar > 0) {
                System.arraycopy(bitmap, offset, bloque, 0, copiar);
            }
            disco.escribirBloque(inicio + i, bloque);
            offset += copiar;
        }
    }

    /**
     * Asigna un bloque libre
     * @return el número del bloque asignado
     * @throws IOException
     */
    public int asignar() throws IOException {
        for (int i = 0; i < totalBits; i++) {
            if (!estaOcupado(i)) {
                marcarOcupado(i);
                return i;
            }
        }
        throw new IOException("Disco lleno: no hay bloques libres");
    }

    /**
     * Libera un bloque asignado
     * @param numBloque el número del bloque a liberar
     * @throws IOException
     */
    public void liberar(int numBloque) throws IOException {
        if (numBloque < 0 || numBloque >= totalBits) {
            throw new IllegalArgumentException("Número de bloque fuera de rango: " + numBloque);
        }
        marcarLibre(numBloque);
    }

    /**
     * Verifica si un bloque está ocupado
     * @param numBloque el número del bloque a verificar
     * @return true si el bloque está ocupado, false en caso contrario
     */
    public boolean estaOcupado(int numBloque) {
        int byteIdx = numBloque / 8;
        int bitIdx = numBloque % 8;
        return (bitmap[byteIdx] & (1 << bitIdx)) != 0;
    }

    // getters para obtener información del bitmap y los bloques
    public int getBloquesLibres() { return bloquesLibres; }
    public int getBloquesOcupados() { return totalBits - bloquesLibres; }
    public int getTotalBloques() { return totalBits; }

    /**
     * Marca un bloque como ocupado en el bitmap
     * @param numBloque el número del bloque a marcar como ocupado
     */
    private void marcarOcupado(int numBloque) {
        int byteIdx = numBloque / 8;
        int bitIdx = numBloque % 8;
        if ((bitmap[byteIdx] & (1 << bitIdx)) == 0) {
            bitmap[byteIdx] |= (1 << bitIdx);
            bloquesLibres--;
        }
    }

    /**
     * Marca un bloque como libre en el bitmap
     * @param numBloque el número del bloque a marcar como libre
     */
    private void marcarLibre(int numBloque) {
        int byteIdx = numBloque / 8;
        int bitIdx = numBloque % 8;
        if ((bitmap[byteIdx] & (1 << bitIdx)) != 0) {
            bitmap[byteIdx] &= ~(1 << bitIdx);
            bloquesLibres++;
        }
    }

    /**
     * Cuenta los bloques libres en el bitmap
     */
    private void contarLibres() {
        bloquesLibres = 0;
        for (int i = 0; i < totalBits; i++) {
            if (!estaOcupado(i)) {
                bloquesLibres++;
            }
        }
    }

    public long contarLibresEnDisco() {
        contarLibres();
        return bloquesLibres;
    }
}

package com.proyecto3.nucleo;

/**
 * Clase para calcular el layout completo del disco virtual.
 */
public class LayoutDisco {

    public static final int BLOQUE_SUPERBLOQUE = 0;

    private final int totalBloques;
    private final int bloquesBitmap;
    private final int bloquesInodos;
    private final int bloquesDatos;
    private final int bloqueInicioBitmap;
    private final int bloqueInicioInodos;
    private final int bloqueInicioDatos;

    /**
     * Constructor para calcular el layout del disco basado en el tamaño total, tamaño de bloque y número de inodos.
     * @param tamanioBytes
     * @param tamanioBloque
     * @param totalInodos
     */
    public LayoutDisco(long tamanioBytes, int tamanioBloque, int totalInodos) {
        this.totalBloques = (int) (tamanioBytes / tamanioBloque); // Calcula el número total de bloques en el disco

        int bloquesSuperbloque = 1; // El superbloque ocupa 1 bloque

        // El inicio del bitmap va luego del superbloque
        this.bloqueInicioBitmap = BLOQUE_SUPERBLOQUE + bloquesSuperbloque;

        // 
        int bitsPorBloque = tamanioBloque * 8;// cada bloque puede almacenar bitsPorBloque bits en el bitmap
        this.bloquesBitmap = (int) Math.ceil((double) totalBloques / bitsPorBloque);// Numero de bloques que se necesitan para el bitmap

        // El inicio de los inodos va luego del bitmap
        this.bloqueInicioInodos = bloqueInicioBitmap + bloquesBitmap;

        // Cada inodo va a ocupar 256 bytes
        int bytesInodo = 256;
        int inodosPorBloque = tamanioBloque / bytesInodo; // Numero de inodos que caben en un bloque
        this.bloquesInodos = (int) Math.ceil((double) totalInodos / inodosPorBloque); // Numero de bloques que se necesitan para los inodos

        // El inicio de los datos va luego de los inodos
        this.bloqueInicioDatos = bloqueInicioInodos + bloquesInodos;
        this.bloquesDatos = totalBloques - bloqueInicioDatos;

        // Asegurar que el disco tiene espacio suficiente para el bitmap, los inodos y al menos un bloque de datos
        if (bloquesDatos < 1) {
            throw new IllegalArgumentException(
                "Disco demasiado pequeño: no hay bloques de datos disponibles. " +
                "Aumente el tamaño del disco o reduzca el número de inodos."
            );
        }
    }

    // getters
    public int getTotalBloques() { return totalBloques; }
    public int getBloquesBitmap() { return bloquesBitmap; }
    public int getBloquesInodos() { return bloquesInodos; }
    public int getBloquesDatos() { return bloquesDatos; }
    public int getBloqueInicioBitmap() { return bloqueInicioBitmap; }
    public int getBloqueInicioInodos() { return bloqueInicioInodos; }
    public int getBloqueInicioDatos() { return bloqueInicioDatos; }
}

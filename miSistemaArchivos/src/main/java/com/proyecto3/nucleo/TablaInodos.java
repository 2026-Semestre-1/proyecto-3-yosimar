package com.proyecto3.nucleo;

import java.io.IOException;

/**
 * Clase que representa la tabla de inodos en el sistema de archivos
 * La tabla de inodos contiene todos los inodos del sistema de archivos
 */
public class TablaInodos {

    // La tabla necesita acceso al disco virtual y al layout del disco para poder leer y escribir los inodos
    private final DiscoVirtual disco;
    private final LayoutDisco layout;
    private final Inodo[] inodos;
    private final int totalInodos;

    /**
     * Constructor de la clase TablaInodos
     * @param disco el disco virtual donde se va a gestionar la tabla de inodos
     * @param layout el layout del disco virtual 
     * @param totalInodos   el número total de inodos que tendrá la tabla
     */
    public TablaInodos(DiscoVirtual disco, LayoutDisco layout, int totalInodos) {
        this.disco = disco;
        this.layout = layout;
        this.totalInodos = totalInodos;
        this.inodos = new Inodo[totalInodos];
        for (int i = 0; i < totalInodos; i++) {
            inodos[i] = new Inodo();
            inodos[i].setNumero(i);
        }
    }

    /**
     * Inicializa la tabla de inodos con inodos libres y los guarda en el disco virtual
     * @throws IOException 
     */
    public void inicializarTabla() throws IOException {
        for (int i = 0; i < totalInodos; i++) {
            inodos[i] = new Inodo();
            inodos[i].setNumero(i);
        }
        guardarEnDisco();
    }

    /**
     * Carga la tabla de inodos desde el disco virtual
     * @throws IOException
     */
    public void cargarDeDisco() throws IOException {
        int inicio = layout.getBloqueInicioInodos();
        int tamanioBloque = disco.getTamanioBloque();
        int inodosPorBloque = tamanioBloque / Inodo.TAMANIO;

        // Por cada inodo, se calcula el numero de bloque y offset dentro de dicho bloque
        for (int i = 0; i < totalInodos; i++) {
            int numBloque = inicio + (i / inodosPorBloque);
            int offsetDentro = (i % inodosPorBloque) * Inodo.TAMANIO;

            // Se lee el bloque del disco virtual y se copia la información del inodo en un arreglo de bytes
            byte[] bloque = disco.leerBloque(numBloque);
            byte[] datosInodo = new byte[Inodo.TAMANIO];
            System.arraycopy(bloque, offsetDentro, datosInodo, 0, Inodo.TAMANIO);
            inodos[i].deserializar(datosInodo);
        }
    }

    /**
     * Guarda la tabla de inodos en el disco virtual
     * @throws IOException
     */
    public void guardarEnDisco() throws IOException {
        int inicio = layout.getBloqueInicioInodos();
        int tamanioBloque = disco.getTamanioBloque();
        int inodosPorBloque = tamanioBloque / Inodo.TAMANIO;

        // Por cada inodo, se calcula el numero de bloque y offset dentro de dicho bloque
        for (int i = 0; i < totalInodos; i++) {
            int numBloque = inicio + (i / inodosPorBloque);
            int offsetDentro = (i % inodosPorBloque) * Inodo.TAMANIO;

            // Se lee el bloque del disco virtual, se serializa el inodo y se copia la información del inodo en el bloque
            byte[] bloque = disco.leerBloque(numBloque);
            byte[] datosInodo = inodos[i].serializar();
            System.arraycopy(datosInodo, 0, bloque, offsetDentro, Inodo.TAMANIO);
            disco.escribirBloque(numBloque, bloque);
        }
    }

    /**
     * Asigna un inodo libre de la tabla
     * @return el inodo asignado
     * @throws IOException
     */
    public Inodo asignarInodo() throws IOException {
        for (int i = 0; i < totalInodos; i++) {
            if (inodos[i].esLibre()) {
                // Si el inodo esta libre, se asigna y se retorna
                Inodo inodo = new Inodo();
                inodo.setNumero(i);
                inodos[i] = inodo;
                return inodo;
            }
        }
        throw new IOException("Tabla de inodos llena: no hay inodos libres");
    }

    /**
     * Libera un inodo asignado de la tabla
     * @param numero el número del inodo a liberar
     * @throws IOException
     */
    public void liberarInodo(int numero) throws IOException {
        if (numero < 0 || numero >= totalInodos) {
            throw new IllegalArgumentException("Número de inodo fuera de rango: " + numero);
        }
        inodos[numero] = new Inodo();
        inodos[numero].setNumero(numero);
    }

    // Accesores y mutadores para los atributos de la tabla de inodos
    public Inodo getInodo(int numero) {
        if (numero < 0 || numero >= totalInodos) {
            throw new IllegalArgumentException("Número de inodo fuera de rango: " + numero);
        }
        return inodos[numero];
    }

    public int getTotalInodos() { return totalInodos; }

    public Inodo[] getTodos() { return inodos; }
}

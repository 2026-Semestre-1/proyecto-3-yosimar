package com.proyecto3.nucleo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase que representa un directorio en el sistema de archivos.
 */
public class Directorio {

    public static final String PUNTO = ".";
    public static final String PUNTO_PUNTO = "..";

    private final DiscoVirtual disco;
    private final AsignadorBloques asignador;
    private final TablaInodos tablaInodos;
    private final Inodo inodoDirectorio;
    private final List<EntradaDirectorio> entradas;

    /**
     * Constructor de la clase Directorio.
     *
     * @param disco        El disco virtual donde se encuentra el directorio.
     * @param asignador    El asignador de bloques para gestionar el espacio en disco.
     * @param tablaInodos  La tabla de inodos que contiene los inodos del sistema de archivos.
     * @param numeroInodo  El número del inodo que representa este directorio.
     * @throws IOException Si ocurre un error al acceder al disco o a la tabla de inodos.
     */
    public Directorio(DiscoVirtual disco, AsignadorBloques asignador, TablaInodos tablaInodos,
                      int numeroInodo) throws IOException {
        this.disco = disco;
        this.asignador = asignador;
        this.tablaInodos = tablaInodos;
        this.inodoDirectorio = tablaInodos.getInodo(numeroInodo);
        this.entradas = new ArrayList<>();

        if (!inodoDirectorio.esDirectorio()) {
            throw new IOException("El inodo " + numeroInodo + " no es un directorio");
        }

        cargarEntradas();
    }

    /**
     * Carga las entradas del directorio desde el disco.
     * @throws IOException
     */
    private void cargarEntradas() throws IOException {
        entradas.clear();
        long tamanio = inodoDirectorio.getTamanio();
        if (tamanio == 0) return;

        byte[] datos = leerDatosInodo(inodoDirectorio);
        int limite = (int) Math.min(tamanio, datos.length);

        int offset = 0;
        while (offset < limite) {
            EntradaDirectorio entrada = EntradaDirectorio.deserializar(datos, offset, limite);
            if (entrada == null) break;
            entradas.add(entrada);
            offset += EntradaDirectorio.tamanoSerializado(datos, offset);
            if (offset >= limite) break;
        }
    }

    /**
     * Guarda los cambios en el directorio en el disco.
     * @throws IOException
     */
    public void guardar() throws IOException {
        byte[] datos = serializarEntradas();
        escribirDatosInodo(inodoDirectorio, datos);

        inodoDirectorio.setTamanio(datos.length);
        inodoDirectorio.setFechaModificacion(System.currentTimeMillis());
        tablaInodos.guardarEnDisco();
    }

    /**
     * Serializa las entradas del directorio.
     * @return El array de bytes con los datos serializados.
     */
    private byte[] serializarEntradas() {
        List<byte[]> partes = new ArrayList<>();
        int total = 0;
        for (EntradaDirectorio e : entradas) {
            byte[] ser = e.serializar();
            partes.add(ser);
            total += ser.length;
        }
        byte[] resultado = new byte[total];
        int offset = 0;
        for (byte[] p : partes) {
            System.arraycopy(p, 0, resultado, offset, p.length);
            offset += p.length;
        }
        return resultado;
    }

    /**
     * Agrega una nueva entrada al directorio.
     * @param nombre    El nombre de la entrada.
     * @param numeroInodo El número del inodo asociado a la entrada.
     * @throws IOException Si ocurre un error al agregar la entrada.
     */
    public void agregarEntrada(String nombre, int numeroInodo) throws IOException {
        EntradaDirectorio existente = buscarEntrada(nombre);
        if (existente != null) {
            throw new IOException("La entrada '" + nombre + "' ya existe");
        }
        entradas.add(new EntradaDirectorio(nombre, numeroInodo));
    }

    /**
     * Elimina una entrada del directorio.
     * @param nombre El nombre de la entrada a eliminar.
     * @throws IOException Si ocurre un error al eliminar la entrada.
     */
    public void eliminarEntrada(String nombre) throws IOException {
        EntradaDirectorio entrada = buscarEntrada(nombre);
        if (entrada == null) {
            throw new IOException("Entrada '" + nombre + "' no encontrada");
        }
        entradas.remove(entrada);
    }

    /**
     * Busca una entrada en el directorio por su nombre.
     * @param nombre El nombre de la entrada a buscar.
     * @return La entrada encontrada o null si no se encuentra.
     */
    public EntradaDirectorio buscarEntrada(String nombre) {
        for (EntradaDirectorio e : entradas) {
            if (e.getNombre().equals(nombre)) return e;
        }
        return null;
    }

    /**
     * Lista todas las entradas del directorio.
     * @return Una lista de entradas del directorio.
     */
    public List<EntradaDirectorio> listarEntradas() {
        return new ArrayList<>(entradas);
    }

    /**
     * Inicializa un directorio con las entradas "." y "..".
     * @param inodoPadre El número del inodo del directorio padre.
     * @throws IOException
     */
    public void inicializarDirectorio(int inodoPadre) throws IOException {
        entradas.clear();
        entradas.add(new EntradaDirectorio(PUNTO, inodoDirectorio.getNumero()));
        entradas.add(new EntradaDirectorio(PUNTO_PUNTO, inodoPadre));
    }

    public int getNumeroInodo() { return inodoDirectorio.getNumero(); }

    public int getInodoPadre() {
        for (EntradaDirectorio e : entradas) {
            if (PUNTO_PUNTO.equals(e.getNombre())) return e.getNumeroInodo();
        }
        return inodoDirectorio.getNumero();
    }

    /**
     * Resuelve una ruta de directorio en sus componentes.
     * @param ruta La ruta a resolver.
     * @return Una lista de componentes de la ruta.
     * @throws IOException
     */
    public java.util.List<String> resolverRuta(String ruta) throws IOException {
        List<String> partes = new ArrayList<>();
        if (ruta == null || ruta.isEmpty()) return partes;

        boolean absoluta = ruta.startsWith("/");
        String limpia = ruta.replace('\\', '/');

        for (String parte : limpia.split("/")) {
            if (parte.isEmpty()) continue;
            partes.add(parte);
        }

        return partes;
    }

    /**
     * Navega a través de la ruta especificada y devuelve el número de inodo del directorio final.
     * @param ruta  La ruta a navegar.
     * @param superbloque El superbloque del sistema de archivos.
     * @return El número de inodo del directorio final.
     * @throws IOException
     */
    public int navegar(String ruta, Superbloque superbloque) throws IOException {
        if (ruta == null || ruta.isEmpty()) return inodoDirectorio.getNumero();

        boolean absoluta = ruta.startsWith("/");
        String limpia = ruta.replace('\\', '/');
        String[] partes = limpia.split("/");
        List<String> componentes = new ArrayList<>();
        for (String p : partes) {
            if (!p.isEmpty()) componentes.add(p);
        }

        if (componentes.isEmpty()) {
            return absoluta ? superbloque.getInodoRaiz() : inodoDirectorio.getNumero();
        }

        int inodoActual = absoluta ? superbloque.getInodoRaiz() : inodoDirectorio.getNumero();

        for (String nombre : componentes) {
            Directorio dirActual = new Directorio(disco, asignador, tablaInodos, inodoActual);
            EntradaDirectorio entrada = dirActual.buscarEntrada(nombre);
            if (entrada == null) {
                throw new IOException("Ruta no encontrada: '" + nombre + "' en '" + ruta + "'");
            }
            Inodo inodoDestino = tablaInodos.getInodo(entrada.getNumeroInodo());
            if (!inodoDestino.esDirectorio()) {
                throw new IOException("'" + nombre + "' no es un directorio");
            }
            inodoActual = entrada.getNumeroInodo();
        }

        return inodoActual;
    }

    /**
     * Obtiene la ruta absoluta del directorio actual.
     * @param superbloque El superbloque del sistema de archivos.
     * @return La ruta absoluta del directorio actual.
     * @throws IOException
     */
    public String obtenerRutaAbsoluta(Superbloque superbloque) throws IOException {
        if (inodoDirectorio.getNumero() == superbloque.getInodoRaiz()) {
            return "/";
        }

        List<String> nombres = new ArrayList<>();
        int actual = inodoDirectorio.getNumero();

        while (actual != superbloque.getInodoRaiz()) {
            Directorio dir = new Directorio(disco, asignador, tablaInodos, actual);
            String nombreEnPadre = null;
            int padre = dir.getInodoPadre();

            Directorio dirPadre = new Directorio(disco, asignador, tablaInodos, padre);
            for (EntradaDirectorio e : dirPadre.listarEntradas()) {
                if (e.getNumeroInodo() == actual && !PUNTO.equals(e.getNombre())
                    && !PUNTO_PUNTO.equals(e.getNombre())) {
                    nombreEnPadre = e.getNombre();
                    break;
                }
            }

            if (nombreEnPadre == null) break;
            nombres.add(0, nombreEnPadre);
            actual = padre;
        }

        StringBuilder sb = new StringBuilder("/");
        for (int i = 0; i < nombres.size(); i++) {
            if (i > 0) sb.append("/");
            sb.append(nombres.get(i));
        }
        return sb.toString();
    }

    /**
     * Lee los datos de un inodo y devuelve un array de bytes con su contenido.
     * @param inodo El inodo del cual leer los datos.
     * @return Un array de bytes con el contenido del inodo.
     * @throws IOException Si ocurre un error al leer los datos.
     */
    private byte[] leerDatosInodo(Inodo inodo) throws IOException {
        int tamanioBloque = disco.getTamanioBloque();
        int bloquesNecesarios = (int) Math.ceil((double) inodo.getTamanio() / tamanioBloque);
        bloquesNecesarios = Math.max(1, bloquesNecesarios);

        byte[] todos = new byte[Math.max(tamanioBloque, (int) inodo.getTamanio())];
        int offset = 0;
        int bloquesLeidos = 0;

        for (int i = 0; i < Inodo.PUNTEROS_DIRECTOS && bloquesLeidos < bloquesNecesarios; i++) {
            int numBloque = inodo.getPunteroDirecto(i);
            if (numBloque == Inodo.BLOQUE_NULO) break;
            byte[] bloque = disco.leerBloque(numBloque);
            int copiar = Math.min(tamanioBloque, todos.length - offset);
            System.arraycopy(bloque, 0, todos, offset, copiar);
            offset += copiar;
            bloquesLeidos++;
        }

        return todos;
    }

    /**
     * Escribe datos en un inodo.
     * @param inodo El inodo en el cual escribir los datos.
     * @param datos Los datos a escribir.
     * @throws IOException Si ocurre un error al escribir los datos.
     */
    private void escribirDatosInodo(Inodo inodo, byte[] datos) throws IOException {
        for (int i = 0; i < Inodo.PUNTEROS_DIRECTOS; i++) {
            int bloque = inodo.getPunteroDirecto(i);
            if (bloque != Inodo.BLOQUE_NULO) {
                asignador.liberar(bloque);
                inodo.setPunteroDirecto(i, Inodo.BLOQUE_NULO);
            }
        }

        int tamanioBloque = disco.getTamanioBloque();
        int bloquesNecesarios = Math.max(1, (int) Math.ceil((double) datos.length / tamanioBloque));
        int offset = 0;

        for (int i = 0; i < Inodo.PUNTEROS_DIRECTOS && i < bloquesNecesarios; i++) {
            int nuevoBloque = asignador.asignar();
            inodo.setPunteroDirecto(i, nuevoBloque);
            byte[] bloque = new byte[tamanioBloque];
            int copiar = Math.min(tamanioBloque, datos.length - offset);
            System.arraycopy(datos, offset, bloque, 0, copiar);
            disco.escribirBloque(nuevoBloque, bloque);
            offset += copiar;
        }

        inodo.setTamanio(datos.length);
        inodo.setFechaModificacion(System.currentTimeMillis());
        asignador.guardarEnDisco();
    }
}

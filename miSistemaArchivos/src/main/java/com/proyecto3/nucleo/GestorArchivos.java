package com.proyecto3.nucleo;

import java.io.IOException;

public class GestorArchivos {

    public static byte[] leerDatosInodo(Inodo inodo, DiscoVirtual disco,
                                        AsignadorBloques asignador) throws IOException {
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

    public static void escribirDatosInodo(Inodo inodo, byte[] datos, DiscoVirtual disco,
                                           AsignadorBloques asignador) throws IOException {
        int tamanioBloque = disco.getTamanioBloque();

        for (int i = 0; i < Inodo.PUNTEROS_DIRECTOS; i++) {
            int bloque = inodo.getPunteroDirecto(i);
            if (bloque != Inodo.BLOQUE_NULO) {
                asignador.liberar(bloque);
                inodo.setPunteroDirecto(i, Inodo.BLOQUE_NULO);
            }
        }

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

    public static String leerDatosComoTexto(Inodo inodo, DiscoVirtual disco,
                                            AsignadorBloques asignador) throws IOException {
        if (inodo.getTamanio() == 0) return "";
        byte[] datos = leerDatosInodo(inodo, disco, asignador);
        int limite = (int) Math.min(inodo.getTamanio(), datos.length);
        return new String(datos, 0, limite, java.nio.charset.StandardCharsets.UTF_8);
    }
}

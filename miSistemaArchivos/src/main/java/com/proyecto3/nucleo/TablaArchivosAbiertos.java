package com.proyecto3.nucleo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TablaArchivosAbiertos {

    public static final String MODO_LECTURA = "r";
    public static final String MODO_ESCRITURA = "w";

    private final Map<Integer, EntradaTabla> abiertos;

    public TablaArchivosAbiertos() {
        this.abiertos = new HashMap<>();
    }

    public void abrir(int numInodo, String modo) {
        abiertos.put(numInodo, new EntradaTabla(numInodo, modo));
    }

    public void cerrar(int numInodo) {
        abiertos.remove(numInodo);
    }

    public boolean estaAbierto(int numInodo) {
        return abiertos.containsKey(numInodo);
    }

    public EntradaTabla obtener(int numInodo) {
        return abiertos.get(numInodo);
    }

    public List<EntradaTabla> listar() {
        return new ArrayList<>(abiertos.values());
    }

    public int contarAbiertos() {
        return abiertos.size();
    }

    public static class EntradaTabla {
        private final int numInodo;
        private int posicion;
        private final String modo;

        public EntradaTabla(int numInodo, String modo) {
            this.numInodo = numInodo;
            this.posicion = 0;
            this.modo = modo;
        }

        public int getNumInodo() { return numInodo; }
        public int getPosicion() { return posicion; }
        public void setPosicion(int pos) { this.posicion = pos; }
        public String getModo() { return modo; }
    }
}

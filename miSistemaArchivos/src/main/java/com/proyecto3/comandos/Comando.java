package com.proyecto3.comandos;

/**
 * Interfaz que define la estructura de un comando del sistema de archivos
 */
public interface Comando {
    String getNombre();
    String ejecutar(String[] args);
    String getAyuda();
}

package com.proyecto3;

import com.proyecto3.nucleo.DiscoVirtual;
import com.proyecto3.nucleo.LayoutDisco;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Principal {

    public static void main(String[] args) {
        if (args.length >= 1 && ("--test".equals(args[0]) || "-t".equals(args[0]))) {
            ejecutarPruebaDisco();
            return;
        }

        System.out.println("miSistemaArchivos - Proyecto 3");
        System.out.println("Uso: java -jar miSistemaArchivos.jar [archivo.fs]");
        System.out.println("     java -jar miSistemaArchivos.jar --test  (prueba de DiscoVirtual)");

        String rutaDisco = args.length >= 1 ? args[0] : "miDiscoDuro.fs";
        System.out.println("Abriendo disco: " + rutaDisco);
    }

    private static void ejecutarPruebaDisco() {
        String ruta = "prueba_disco.fs";
        long tamanio = 10L * 1024 * 1024; // 10 MB

        System.out.println("=== PRUEBA DE DiscoVirtual ===");
        System.out.println("Tamaño de bloque: 512 bytes");
        System.out.println("Tamaño del disco: 10 MB");

        try {
            DiscoVirtual disco = new DiscoVirtual(512);

            System.out.print("Creando disco de prueba... ");
            disco.crearDisco(ruta, tamanio);
            System.out.println("OK (" + disco.getTotalBloques() + " bloques)");

            String mensaje = "Hola, este es un bloque de prueba para el sistema de archivos!";
            byte[] datosOriginales = Arrays.copyOf(mensaje.getBytes(StandardCharsets.UTF_8), 512);

            System.out.print("Escribiendo bloque 0... ");
            disco.escribirBloque(0, datosOriginales);
            System.out.println("OK");

            System.out.print("Cerrando disco... ");
            disco.cerrar();
            System.out.println("OK");

            System.out.print("Reabriendo disco... ");
            disco.abrirDisco(ruta);
            System.out.println("OK");

            System.out.print("Leyendo bloque 0... ");
            byte[] datosLeidos = disco.leerBloque(0);
            System.out.println("OK");

            boolean iguales = Arrays.equals(datosOriginales, datosLeidos);

            String textoLeido = new String(datosLeidos, StandardCharsets.UTF_8).trim();
            System.out.println("Contenido leído: \"" + textoLeido + "\"");

            System.out.print("Cerrando disco... ");
            disco.cerrar();
            System.out.println("OK");

            LayoutDisco layout = new LayoutDisco(tamanio, 512, 1024);
            System.out.println("\n=== LAYOUT DEL DISCO (10 MB, 1024 inodos) ===");
            System.out.println("Total de bloques:      " + layout.getTotalBloques());
            System.out.println("Bloque Superbloque:     " + LayoutDisco.BLOQUE_SUPERBLOQUE);
            System.out.println("Bitmap:                 bloques " + layout.getBloqueInicioBitmap() + " a " + (layout.getBloqueInicioBitmap() + layout.getBloquesBitmap() - 1) + " (" + layout.getBloquesBitmap() + " bloques)");
            System.out.println("Tabla de inodos:        bloques " + layout.getBloqueInicioInodos() + " a " + (layout.getBloqueInicioInodos() + layout.getBloquesInodos() - 1) + " (" + layout.getBloquesInodos() + " bloques)");
            System.out.println("Datos:                  bloques " + layout.getBloqueInicioDatos() + " a " + (layout.getTotalBloques() - 1) + " (" + layout.getBloquesDatos() + " bloques)");

            System.out.println("\n=== PRUEBA " + (iguales ? "EXITOSA" : "FALLIDA") + " ===");

        } catch (IOException e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        } finally {
            new File(ruta).delete();
        }
    }
}

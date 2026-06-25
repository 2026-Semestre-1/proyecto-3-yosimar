package com.proyecto3;

import com.proyecto3.comandos.impl.ComandoFormat;
import com.proyecto3.nucleo.*;
import com.proyecto3.seguridad.GestorUsuarios;
import com.proyecto3.seguridad.Usuario;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class Principal {

    private static final SimpleDateFormat FECHA = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        if (args.length == 0) {
            mostrarAyuda();
            return;
        }

        String comando = args[0];
        String[] resto = new String[args.length - 1];
        System.arraycopy(args, 1, resto, 0, resto.length);

        switch (comando) {
            case "--test-disco":
                pruebaDiscoVirtual();
                break;
            case "--test-format":
                pruebaFormat(resto);
                break;
            case "--info-fs":
                infoFS(resto);
                break;
            case "--view-fcb":
                viewFCB(resto);
                break;
            default:
                System.out.println("Comando desconocido: " + comando);
                mostrarAyuda();
        }
    }

    private static void mostrarAyuda() {
        System.out.println("miSistemaArchivos - Proyecto 3, Principios de SO");
        System.out.println();
        System.out.println("  --test-disco          Prueba de DiscoVirtual (crear, escribir, leer)");
        System.out.println("  --test-format <fs> <MB> <inodos> <pass>");
        System.out.println("                        Formatea un disco y muestra estado");
        System.out.println("  --info-fs <archivo.fs>");
        System.out.println("                        Muestra información del sistema de archivos");
        System.out.println("  --view-fcb <archivo.fs> <inodo>");
        System.out.println("                        Muestra el FCB del inodo indicado");
    }

    private static void pruebaDiscoVirtual() {
        String ruta = "prueba_disco.fs";
        long tamanio = 10L * 1024 * 1024;

        System.out.println("=== PRUEBA DE DiscoVirtual ===");
        System.out.println("Tamaño de bloque: 512 bytes | Disco: 10 MB");

        try {
            DiscoVirtual disco = new DiscoVirtual(512);
            disco.crearDisco(ruta, tamanio);
            System.out.println("Creado: " + disco.getTotalBloques() + " bloques");

            byte[] original = "Bloque de prueba — Sistema de Archivos Proyecto 3".getBytes(StandardCharsets.UTF_8);
            byte[] datos = new byte[512];
            System.arraycopy(original, 0, datos, 0, original.length);
            disco.escribirBloque(0, datos);
            disco.cerrar();

            disco.abrirDisco(ruta);
            byte[] leido = disco.leerBloque(0);
            boolean ok = Arrays.equals(datos, leido);
            disco.cerrar();

            System.out.println("Escribir → Cerrar → Abrir → Leer: " + (ok ? "EXITOSO" : "FALLIDO"));
        } catch (IOException e) {
            System.err.println("ERROR: " + e.getMessage());
        } finally {
            new File(ruta).delete();
        }
    }

    private static void pruebaFormat(String[] args) {
        String ruta = args.length >= 1 ? args[0] : "formato_prueba.fs";
        long tamanioMB = args.length >= 2 ? Long.parseLong(args[1]) : 10;
        int inodos = args.length >= 3 ? Integer.parseInt(args[2]) : 1024;
        String pass = args.length >= 4 ? args[3] : "admin123";

        System.out.println("=== PRUEBA DE FORMAT ===");

        ComandoFormat cmd = new ComandoFormat();
        String resultado = cmd.ejecutar(new String[]{ruta, String.valueOf(tamanioMB), String.valueOf(inodos), pass});
        System.out.println(resultado);

        if (cmd.getSuperbloque() != null) {
            System.out.println("\n=== VERIFICACIÓN (reabriendo disco) ===");
            try {
                DiscoVirtual disco = new DiscoVirtual(512);
                disco.abrirDisco(ruta);

                Superbloque sb = new Superbloque();
                sb.cargar(disco);
                System.out.println("Magic:  0x" + Integer.toHexString(Superbloque.MAGIC) + " (OK)");
                System.out.println("Nombre: " + sb.getNombreFs());
                System.out.println("Inodo raíz: " + sb.getInodoRaiz());

                LayoutDisco layout = sb.getLayout();
                AsignadorBloques ab = new AsignadorBloques(disco, layout);
                ab.cargarDeDisco();

                TablaInodos ti = new TablaInodos(disco, layout, sb.getTotalInodos());
                ti.cargarDeDisco();

                Inodo raiz = ti.getInodo(sb.getInodoRaiz());
                System.out.println("Inodo raíz: tipo=" + (raiz.esDirectorio() ? "directorio" : "?") +
                    ", uid=" + raiz.getUid() + ", gid=" + raiz.getGid() +
                    ", permisos=" + raiz.getPermisos());

                GestorUsuarios gu = cmd.getGestorUsuarios();
                if (gu != null) {
                    Usuario root = gu.getRoot();
                    System.out.println("Root: " + root.getNombre() + " (uid=" + root.getUid() +
                        ", gid=" + root.getGid() + ", home=" + root.getInodoHome() + ")");
                    System.out.println("Password verificado: " +
                        (root.verificarPassword(pass) ? "SI" : "NO"));
                }

                System.out.println("Bloques libres: " + ab.getBloquesLibres() + " / " + ab.getTotalBloques());

                disco.cerrar();
                System.out.println("VERIFICACIÓN EXITOSA");
            } catch (IOException e) {
                System.err.println("ERROR en verificación: " + e.getMessage());
            }
        }

        new File(ruta).delete();
    }

    private static void infoFS(String[] args) {
        if (args.length < 1) {
            System.out.println("Uso: --info-fs <archivo.fs>");
            return;
        }
        String ruta = args[0];
        File archivo = new File(ruta);
        if (!archivo.exists()) {
            System.out.println("Archivo no encontrado: " + ruta);
            return;
        }

        try {
            DiscoVirtual disco = new DiscoVirtual(512);
            disco.abrirDisco(ruta);

            Superbloque sb = new Superbloque();
            sb.cargar(disco);

            LayoutDisco layout = sb.getLayout();

            AsignadorBloques ab = new AsignadorBloques(disco, layout);
            ab.cargarDeDisco();

            TablaInodos ti = new TablaInodos(disco, layout, sb.getTotalInodos());
            ti.cargarDeDisco();
            int inodosUsados = 0;
            for (Inodo inodo : ti.getTodos()) {
                if (!inodo.esLibre()) inodosUsados++;
            }

            System.out.println("=== infoFS: " + sb.getNombreFs() + " ===");
            System.out.println("Archivo:           " + ruta);
            System.out.println("Tamaño total:      " + sb.getTamanioTotal() + " bytes (" +
                (sb.getTamanioTotal() / 1024 / 1024) + " MB)");
            System.out.println("Tamaño de bloque:  " + sb.getTamanioBloque() + " bytes");
            System.out.println("Total bloques:     " + sb.getTotalBloques());
            System.out.println("Bloques libres:    " + ab.getBloquesLibres());
            System.out.println("Bloques ocupados:  " + ab.getBloquesOcupados());
            System.out.println("Total inodos:      " + sb.getTotalInodos());
            System.out.println("Inodos usados:     " + inodosUsados);
            System.out.println("Inodo raíz:        " + sb.getInodoRaiz());
            System.out.println("Layout:");
            System.out.println("  Superbloque:     bloque " + LayoutDisco.BLOQUE_SUPERBLOQUE);
            System.out.println("  Bitmap:          bloques " + sb.getBloqueInicioBitmap() +
                " a " + (sb.getBloqueInicioInodos() - 1));
            System.out.println("  Tabla inodos:    bloques " + sb.getBloqueInicioInodos() +
                " a " + (sb.getBloqueInicioDatos() - 1));
            System.out.println("  Datos:           bloques " + sb.getBloqueInicioDatos() +
                " a " + (sb.getTotalBloques() - 1));

            disco.cerrar();
        } catch (IOException e) {
            System.err.println("ERROR: " + e.getMessage());
        }
    }

    private static void viewFCB(String[] args) {
        if (args.length < 2) {
            System.out.println("Uso: --view-fcb <archivo.fs> <numInodo>");
            return;
        }
        String ruta = args[0];
        int numInodo = Integer.parseInt(args[1]);

        try {
            DiscoVirtual disco = new DiscoVirtual(512);
            disco.abrirDisco(ruta);

            Superbloque sb = new Superbloque();
            sb.cargar(disco);

            LayoutDisco layout = sb.getLayout();

            TablaInodos ti = new TablaInodos(disco, layout, sb.getTotalInodos());
            ti.cargarDeDisco();

            if (numInodo >= sb.getTotalInodos()) {
                System.out.println("Inodo " + numInodo + " fuera de rango (total: " + sb.getTotalInodos() + ")");
                disco.cerrar();
                return;
            }

            Inodo inodo = ti.getInodo(numInodo);

            System.out.println("=== viewFCB: Inodo " + numInodo + " ===");
            if (inodo.esLibre()) {
                System.out.println("  Estado: LIBRE (no asignado)");
            } else {
                String tipo = inodo.esDirectorio() ? "Directorio" :
                              inodo.esArchivo() ? "Archivo" : "Desconocido";
                System.out.println("  Tipo:              " + tipo);
                System.out.println("  Dueño (uid):       " + inodo.getUid());
                System.out.println("  Grupo (gid):       " + inodo.getGid());
                System.out.println("  Permisos:          " + inodo.getPermisos());
                System.out.println("  Tamaño:            " + inodo.getTamanio() + " bytes");
                System.out.println("  Enlaces:           " + inodo.getEnlaces());
                System.out.println("  Abierto:           " + (inodo.isAbierto() ? "Sí" : "No"));
                System.out.println("  Creado:            " + FECHA.format(new Date(inodo.getFechaCreacion())));
                System.out.println("  Modificado:        " + FECHA.format(new Date(inodo.getFechaModificacion())));
                System.out.println("  Punteros directos:");
                for (int i = 0; i < Inodo.PUNTEROS_DIRECTOS; i++) {
                    int ptr = inodo.getPunteroDirecto(i);
                    if (ptr != Inodo.BLOQUE_NULO) {
                        System.out.println("    [" + i + "] = bloque " + ptr);
                    }
                }
                System.out.println("  Puntero indirecto: " +
                    (inodo.getPunteroIndirecto() != Inodo.BLOQUE_NULO ?
                     "bloque " + inodo.getPunteroIndirecto() : "ninguno"));
            }

            disco.cerrar();
        } catch (IOException e) {
            System.err.println("ERROR: " + e.getMessage());
        }
    }
}

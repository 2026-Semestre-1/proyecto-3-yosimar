package com.proyecto3;

import com.proyecto3.comandos.impl.*;
import com.proyecto3.nucleo.*;
import com.proyecto3.seguridad.GestorUsuarios;
import com.proyecto3.seguridad.Grupo;
import com.proyecto3.seguridad.Usuario;
import com.proyecto3.sesion.Sesion;
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
            case "--test-fase3":
                pruebaFase3(resto);
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
        System.out.println("  --test-fase3          Prueba de usuarios y sesiones (format + login + CRUD)");
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

    private static void pruebaFase3(String[] args) {
        String ruta = "fase3_prueba.fs";
        System.out.println("=== PRUEBA FASE 3: Usuarios, Grupos y Sesiones ===");

        try {
            System.out.println("\n--- 3.1 Formatear disco ---");
            ComandoFormat cmdFormat = new ComandoFormat();
            String res = cmdFormat.ejecutar(new String[]{ruta, "10", "256", "root123"});
            System.out.println(res);

            System.out.println("\n--- 3.2 Crear Sesión y login como root ---");
            DiscoVirtual disco = new DiscoVirtual(512);
            disco.abrirDisco(ruta);

            Superbloque sb = new Superbloque();
            sb.cargar(disco);

            LayoutDisco layout = sb.getLayout();

            AsignadorBloques ab = new AsignadorBloques(disco, layout);
            ab.cargarDeDisco();

            TablaInodos ti = new TablaInodos(disco, layout, sb.getTotalInodos());
            ti.cargarDeDisco();

            GestorUsuarios gu = new GestorUsuarios();
            gu.cargarDeDisco(disco, ti, ab);

            Sesion sesion = new Sesion(disco, sb, ab, ti, gu);

            System.out.print("Login root... ");
            boolean loginOk = sesion.login("root", "root123");
            System.out.println(loginOk ? "OK" : "FALLÓ");

            System.out.print("whoami: ");
            ComandoWhoami whoami = new ComandoWhoami(sesion);
            System.out.println(whoami.ejecutar(new String[]{}));

            System.out.println("\n--- 3.3 Crear grupo 'estudiantes' ---");
            ComandoGroupadd groupadd = new ComandoGroupadd(sesion);
            System.out.println(groupadd.ejecutar(new String[]{"estudiantes"}));

            System.out.print("whoami (sigue root): ");
            System.out.println(whoami.ejecutar(new String[]{}));

            System.out.println("\n--- 3.4 Crear usuario 'juan' en grupo 'estudiantes' ---");
            ComandoUseradd useradd = new ComandoUseradd(sesion);
            int gidEstudiantes = gu.getGrupoPorNombre("estudiantes").getGid();
            System.out.println(useradd.ejecutar(new String[]{"juan", "Juan Pérez", "juan123", String.valueOf(gidEstudiantes)}));

            System.out.println("\n--- 3.5 Cambiar contraseña de juan (como root) ---");
            ComandoPasswd passwd = new ComandoPasswd(sesion);
            System.out.println(passwd.ejecutar(new String[]{"juan", "nuevapass456"}));

            System.out.println("\n--- 3.6 Hacer su a juan ---");
            ComandoSu su = new ComandoSu(sesion);
            System.out.println(su.ejecutar(new String[]{"juan"}));

            System.out.print("whoami (ahora juan): ");
            System.out.println(whoami.ejecutar(new String[]{}));

            System.out.println("\n--- 3.7 juan cambia su propia contraseña ---");
            ComandoPasswd passwdJuan = new ComandoPasswd(sesion);
            System.out.println(passwdJuan.ejecutar(new String[]{"clavefinal789"}));

            System.out.println("\n--- 3.8 Volver a root ---");
            System.out.println(su.ejecutar(new String[]{"root", "root123"}));
            System.out.print("whoami: ");
            System.out.println(whoami.ejecutar(new String[]{}));

            System.out.println("\n--- 3.9 Crear otro grupo 'profesores' y usuario 'maria' ---");
            System.out.println(groupadd.ejecutar(new String[]{"profesores"}));
            int gidProfes = gu.getGrupoPorNombre("profesores").getGid();
            System.out.println(useradd.ejecutar(new String[]{"maria", "María López", "maria123", String.valueOf(gidProfes)}));

            disco.cerrar();

            System.out.println("\n--- 3.10 VERIFICACIÓN: reabrir disco y comprobar persistencia ---");
            DiscoVirtual disco2 = new DiscoVirtual(512);
            disco2.abrirDisco(ruta);

            Superbloque sb2 = new Superbloque();
            sb2.cargar(disco2);

            LayoutDisco layout2 = sb2.getLayout();
            AsignadorBloques ab2 = new AsignadorBloques(disco2, layout2);
            ab2.cargarDeDisco();
            TablaInodos ti2 = new TablaInodos(disco2, layout2, sb2.getTotalInodos());
            ti2.cargarDeDisco();

            GestorUsuarios gu2 = new GestorUsuarios();
            gu2.cargarDeDisco(disco2, ti2, ab2);

            System.out.println("Usuarios cargados del disco:");
            for (Usuario u : gu2.getUsuarios()) {
                System.out.println("  " + u.getNombre() + " (uid=" + u.getUid() + ", gid=" + u.getGid() + ")");
            }
            System.out.println("Grupos cargados del disco:");
            for (Grupo g : gu2.getGrupos()) {
                System.out.println("  " + g.getNombre() + " (gid=" + g.getGid() + ")");
            }

            Sesion sesion2 = new Sesion(disco2, sb2, ab2, ti2, gu2);
            boolean loginRoot2 = sesion2.login("root", "root123");
            System.out.println("Login root: " + (loginRoot2 ? "OK" : "FALLÓ"));
            boolean loginJuan = sesion2.login("juan", "clavefinal789");
            System.out.println("Login juan con nueva clave: " + (loginJuan ? "OK" : "FALLÓ"));
            boolean loginMaria = sesion2.login("maria", "maria123");
            System.out.println("Login maria: " + (loginMaria ? "OK" : "FALLÓ"));

            disco2.cerrar();

            System.out.println("\n=== PRUEBA FASE 3 EXITOSA ===");
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        } finally {
            new File(ruta).delete();
        }
    }
}

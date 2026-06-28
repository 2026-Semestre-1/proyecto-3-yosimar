package com.proyecto3;

import com.proyecto3.comandos.CommandDispatcher;
import com.proyecto3.nucleo.*;
import com.proyecto3.seguridad.GestorUsuarios;
import com.proyecto3.sesion.Sesion;
import com.proyecto3.ui.ShellFrame;
import java.io.File;
import javax.swing.*;

public class Principal {

    private static final String TEST_PASSWORD = "admin";

    public static void main(String[] args) {
        if (args.length >= 2 && "--test".equals(args[0])) {
            modoTest(args);
            return;
        }

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) {}

            if (args.length >= 1) {
                String ruta = args[0];
                if (new File(ruta).exists()) {
                    new ShellFrame(ruta);
                } else {
                    new ShellFrame();
                }
            } else {
                new ShellFrame();
            }
        });
    }

    private static void modoTest(String[] args) {
        if (args.length < 2) {
            System.err.println("Uso: --test <comando> [args...]");
            System.exit(1);
        }

        String nombreComando = args[1];
        String[] resto = new String[args.length - 2];
        System.arraycopy(args, 2, resto, 0, resto.length);

        String discoRuta = null;

        if ("format".equals(nombreComando)) {
            ejecutarComando(null, nombreComando, resto);
            return;
        }

        if (resto.length >= 1) {
            discoRuta = resto[0];
            resto = new String[resto.length - 1];
            System.arraycopy(args, 3, resto, 0, resto.length);
        }

        if (discoRuta == null || !new File(discoRuta).exists()) {
            System.err.println("Archivo de disco no encontrado: " + discoRuta);
            System.exit(1);
        }

        DiscoVirtual disco = new DiscoVirtual(512);
        Superbloque sb = new Superbloque();
        AsignadorBloques ab = null;
        TablaInodos ti = null;
        GestorUsuarios gu = new GestorUsuarios();

        try {
            disco.abrirDisco(discoRuta);
            sb.cargar(disco);

            LayoutDisco layout = sb.getLayout();
            ab = new AsignadorBloques(disco, layout);
            ab.cargarDeDisco();

            ti = new TablaInodos(disco, layout, sb.getTotalInodos());
            ti.cargarDeDisco();

            gu.cargarDeDisco(disco, ti, ab);

            Sesion sesion = new Sesion(disco, sb, ab, ti, gu);
            sesion.setTablaArchivosAbiertos(new TablaArchivosAbiertos());
            if (!sesion.login("root", TEST_PASSWORD)) {
                sesion.login("root", "root123");
            }
            if (!sesion.estaAutenticado()) {
                sesion.login("root", "admin123");
            }

            CommandDispatcher dispatcher = new CommandDispatcher(sesion, sesion.getTablaArchivosAbiertos());
            StringBuilder linea = new StringBuilder(nombreComando);
            for (String a : resto) {
                linea.append(" \"").append(a).append("\"");
            }
            String resultado = dispatcher.despachar(linea.toString());
            if (resultado != null) {
                System.out.println(resultado);
            }

            disco.cerrar();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void ejecutarComando(String discoRuta, String nombreComando, String[] args) {
        DiscoVirtual disco = new DiscoVirtual(512);
        Superbloque sb = new Superbloque();
        GestorUsuarios gu = new GestorUsuarios();
        AsignadorBloques ab = null;
        TablaInodos ti = null;

        try {
            if (discoRuta != null && new File(discoRuta).exists()) {
                disco.abrirDisco(discoRuta);
                sb.cargar(disco);
                LayoutDisco layout = sb.getLayout();
                ab = new AsignadorBloques(disco, layout);
                ab.cargarDeDisco();
                ti = new TablaInodos(disco, layout, sb.getTotalInodos());
                ti.cargarDeDisco();
                gu.cargarDeDisco(disco, ti, ab);
            }

            Sesion sesion = new Sesion(disco, sb, ab != null ? ab : new AsignadorBloques(disco, new LayoutDisco(1024*1024,512,64)),
                ti, gu);
            sesion.setTablaArchivosAbiertos(new TablaArchivosAbiertos());

            if (discoRuta != null && disco.estaAbierto()) {
                if (!sesion.login("root", TEST_PASSWORD)) {
                    sesion.login("root", "root123");
                }
                if (!sesion.estaAutenticado()) {
                    sesion.login("root", "admin123");
                }
            }

            CommandDispatcher dispatcher = new CommandDispatcher(sesion, sesion.getTablaArchivosAbiertos());
            StringBuilder linea = new StringBuilder(nombreComando);
            for (String a : args) {
                linea.append(" \"").append(a).append("\"");
            }
            String resultado = dispatcher.despachar(linea.toString());
            if (resultado != null) {
                System.out.println(resultado);
            }

            if (disco.estaAbierto()) disco.cerrar();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}

package com.proyecto3.ui;

import com.proyecto3.nucleo.*;
import com.proyecto3.seguridad.GestorUsuarios;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class ShellFrame extends JFrame {

    private final JTabbedPane tabbedPane;
    private final DiscoVirtual disco;
    private final Superbloque superbloque;
    private AsignadorBloques asignador;
    private TablaInodos tablaInodos;
    private final GestorUsuarios gestorUsuarios;
    private final TablaArchivosAbiertos tablaArchivosAbiertos;
    private boolean discoCargado;

    public ShellFrame() {
        this(null);
    }

    public ShellFrame(String rutaDisco) {
        super("miSistemaArchivos — Terminal");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 550);
        setLocationRelativeTo(null);

        disco = new DiscoVirtual(512);
        superbloque = new Superbloque();
        asignador = null;
        tablaInodos = null;
        gestorUsuarios = new GestorUsuarios();
        tablaArchivosAbiertos = new TablaArchivosAbiertos();
        discoCargado = false;

        tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(new Color(30, 30, 30));
        tabbedPane.setForeground(new Color(200, 200, 200));
        tabbedPane.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JPanel tabBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabBar.setBackground(new Color(30, 30, 30));
        tabbedPane.addTab("  +  ", null);

        JButton btnNueva = new JButton("+");
        btnNueva.setBackground(new Color(50, 50, 50));
        btnNueva.setForeground(new Color(180, 255, 160));
        btnNueva.setFont(new Font("Monospaced", Font.BOLD, 14));
        btnNueva.setBorder(new EmptyBorder(2, 10, 2, 10));
        btnNueva.setFocusPainted(false);
        btnNueva.addActionListener(e -> nuevaTerminal());
        tabbedPane.setTabComponentAt(0, btnNueva);

        tabbedPane.addChangeListener(e -> {
            if (tabbedPane.getSelectedIndex() == 0) {
                tabbedPane.setSelectedIndex(tabbedPane.getTabCount() > 1 ? 1 : 0);
                return;
            }
            Component c = tabbedPane.getSelectedComponent();
            if (c instanceof TerminalTab tt) {
                tt.enfocarEntrada();
            }
        });

        add(tabbedPane, BorderLayout.CENTER);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                enfocarTerminalActual();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    if (disco.estaAbierto()) {
                        disco.cerrar();
                    }
                } catch (Exception ignored) {}
            }
        });

        if (rutaDisco != null) {
            cargarDisco(rutaDisco);
        }

        nuevaTerminal();
        setVisible(true);
        enfocarTerminalActual();
    }

    private void cargarDisco(String ruta) {
        try {
            if (disco != null && disco.estaAbierto()) {
                disco.cerrar();
            }
            disco.abrirDisco(ruta);
            superbloque.cargar(disco);

            LayoutDisco layout = superbloque.getLayout();
            asignador = new AsignadorBloques(disco, layout);
            asignador.cargarDeDisco();

            tablaInodos = new TablaInodos(disco, layout, superbloque.getTotalInodos());
            tablaInodos.cargarDeDisco();

            gestorUsuarios.cargarDeDisco(disco, tablaInodos, asignador);
            discoCargado = true;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error al abrir el disco:\n" + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
            discoCargado = false;
        }
    }

    private void nuevaTerminal() {
        TerminalTab tab = new TerminalTab(
            discoCargado ? disco : null,
            superbloque,
            discoCargado ? asignador : null,
            discoCargado ? tablaInodos : null,
            gestorUsuarios,
            tablaArchivosAbiertos
        );

        int count = tabbedPane.getTabCount();
        int insertPos = count - 1;
        String titulo = "Terminal " + count;
        tabbedPane.insertTab(titulo, null, tab, null, insertPos);

        JPanel tabTitle = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        tabTitle.setOpaque(false);
        JLabel label = new JLabel("T" + count);
        label.setForeground(new Color(200, 200, 200));
        label.setFont(new Font("Monospaced", Font.PLAIN, 11));
        tabTitle.add(label);

        JButton btnCerrar = new JButton("x");
        btnCerrar.setFont(new Font("Monospaced", Font.BOLD, 10));
        btnCerrar.setPreferredSize(new Dimension(20, 18));
        btnCerrar.setBorder(null);
        btnCerrar.setBackground(new Color(60, 60, 60));
        btnCerrar.setForeground(Color.WHITE);
        btnCerrar.setFocusPainted(false);
        btnCerrar.addActionListener(e -> {
            if (tabbedPane.getTabCount() <= 2) {
                int op = JOptionPane.showConfirmDialog(this,
                    "¿Cerrar el programa?", "Salir",
                    JOptionPane.YES_NO_OPTION);
                if (op == JOptionPane.YES_OPTION) {
                    dispose();
                    System.exit(0);
                }
            } else {
                tabbedPane.remove(tab);
            }
        });
        tabTitle.add(btnCerrar);

        tabbedPane.setTabComponentAt(insertPos, tabTitle);
        tabbedPane.setSelectedIndex(insertPos);
        tab.enfocarEntrada();
    }

    private void enfocarTerminalActual() {
        SwingUtilities.invokeLater(() -> {
            Component c = tabbedPane.getSelectedComponent();
            if (c instanceof TerminalTab tt) {
                tt.enfocarEntrada();
            }
        });
    }

    @Override
    public void dispose() {
        try {
            if (disco.estaAbierto()) disco.cerrar();
        } catch (Exception ignored) {}
        super.dispose();
    }
}

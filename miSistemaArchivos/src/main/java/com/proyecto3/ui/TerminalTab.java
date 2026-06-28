package com.proyecto3.ui;

import com.proyecto3.comandos.CommandDispatcher;
import com.proyecto3.nucleo.*;
import com.proyecto3.seguridad.GestorUsuarios;
import com.proyecto3.sesion.Sesion;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class TerminalTab extends JPanel {

    private static final Color BG_DARK = new Color(20, 20, 20);
    private static final Color FG_TERMINAL = new Color(180, 255, 160);
    private static final Color FG_WHITE = new Color(220, 220, 220);
    private static final Color BG_HEADER = new Color(40, 40, 40);
    private static final Font MONO_FONT = new Font("Monospaced", Font.PLAIN, 13);

    private final DiscoVirtual disco;
    private final Superbloque superbloque;
    private final AsignadorBloques asignador;
    private final TablaInodos tablaInodos;
    private final GestorUsuarios gestorUsuarios;
    private Sesion sesion;
    private CommandDispatcher dispatcher;

    private JLabel labelPrompt;
    private JLabel labelUsuario;
    private JTextArea areaSalida;
    private JTextField campoEntrada;
    private JPanel panelHeader;

    private boolean esperandoUsuario;
    private boolean esperandoPassword;
    private boolean esperandoFormatPass;
    private String loginUsuario;
    private String formatArgs;

    public TerminalTab(DiscoVirtual disco, Superbloque superbloque, AsignadorBloques asignador,
                       TablaInodos tablaInodos, GestorUsuarios gestorUsuarios) {
        this.disco = disco;
        this.superbloque = superbloque;
        this.asignador = asignador;
        this.tablaInodos = tablaInodos;
        this.gestorUsuarios = gestorUsuarios;
        this.esperandoUsuario = false;
        this.esperandoPassword = false;
        this.esperandoFormatPass = false;

        initUI();
        configurarSesion();
    }

    private void configurarSesion() {
        sesion = new Sesion(disco, superbloque, asignador, tablaInodos, gestorUsuarios);
        dispatcher = new CommandDispatcher(sesion);

        if (disco == null || !disco.estaAbierto()) {
            appendLn("No hay disco cargado.", FG_WHITE);
            appendLn("Use: format <archivo.fs> <MB> <inodos>", Color.ORANGE);
            appendLn("Ejemplo: format miDiscoDuro.fs 10 256", Color.ORANGE);
            appendLn("Luego se le pedirá la contraseña de root.", Color.ORANGE);
            append("\n");
        }

        actualizarPrompt();
        pedirLoginInicial();
    }

    private void pedirLoginInicial() {
        if (disco == null || !disco.estaAbierto()) return;
        appendLn("Sistema de archivos: " + superbloque.getNombreFs(), FG_WHITE);
        appendLn("Inicie sesión para continuar.", FG_WHITE);
        append("login: ");
        esperandoUsuario = true;
    }

    @SuppressWarnings("unused")
    private void initUI() {
        setLayout(new BorderLayout());

        panelHeader = new JPanel(new BorderLayout());
        panelHeader.setBackground(BG_HEADER);
        panelHeader.setBorder(new EmptyBorder(4, 8, 4, 8));

        labelPrompt = new JLabel(" ");
        labelPrompt.setForeground(FG_WHITE);
        labelPrompt.setFont(MONO_FONT);

        labelUsuario = new JLabel("sin sesión");
        labelUsuario.setForeground(FG_TERMINAL);
        labelUsuario.setFont(MONO_FONT);

        panelHeader.add(labelPrompt, BorderLayout.CENTER);
        panelHeader.add(labelUsuario, BorderLayout.EAST);

        areaSalida = new JTextArea();
        areaSalida.setBackground(BG_DARK);
        areaSalida.setForeground(FG_TERMINAL);
        areaSalida.setFont(MONO_FONT);
        areaSalida.setEditable(false);
        areaSalida.setFocusable(false);
        areaSalida.setCaretColor(FG_TERMINAL);

        JScrollPane scroll = new JScrollPane(areaSalida);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setBackground(BG_DARK);
        scroll.getVerticalScrollBar().setForeground(BG_HEADER);

        campoEntrada = new JTextField();
        campoEntrada.setBackground(BG_DARK);
        campoEntrada.setForeground(FG_TERMINAL);
        campoEntrada.setFont(MONO_FONT);
        campoEntrada.setCaretColor(FG_TERMINAL);
        campoEntrada.setBorder(new EmptyBorder(4, 8, 4, 8));

        campoEntrada.addActionListener(this::procesarEntrada);
        campoEntrada.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_UP) {
                    e.consume();
                }
            }
        });

        add(panelHeader, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(campoEntrada, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(700, 450));
    }

    private void procesarEntrada(ActionEvent e) {
        String entrada = campoEntrada.getText();
        campoEntrada.setText("");

        if (entrada.isEmpty()) {
            append(dispatcher.estaAutenticado() ? dispatcher.getPrompt() : "login: ");
            return;
        }

        if (!dispatcher.estaAutenticado() && !esperandoUsuario && !esperandoPassword && !esperandoFormatPass) {
            if (entrada.startsWith("format ")) {
                String[] partes = entrada.split(" ", 4);
                if (partes.length == 3) {
                    formatArgs = entrada;
                    append("password de root: ");
                    esperandoFormatPass = true;
                    return;
                }
                if (partes.length == 4) {
                    ejecutarComando(entrada);
                    return;
                }
                appendLn("Uso: format <archivo.fs> <MB> <inodos>", Color.ORANGE);
                appendLn("Ejemplo: format miDiscoDuro.fs 10 256", Color.ORANGE);
                return;
            }
            appendLn("Debe formatear un disco primero: format <archivo.fs> <MB> <inodos>", Color.ORANGE);
            appendLn("Luego se le pedirá la contraseña de root.", Color.ORANGE);
            return;
        }

        if (esperandoUsuario) {
            appendLn(entrada, FG_WHITE);
            loginUsuario = entrada;
            append("password: ");
            esperandoUsuario = false;
            esperandoPassword = true;
            return;
        }

        if (esperandoPassword) {
            appendLn("****", FG_WHITE);
            esperandoPassword = false;
            String error = dispatcher.procesarLogin(loginUsuario, entrada);
            if (error != null) {
                appendLn(error, Color.ORANGE);
                append("login: ");
                esperandoUsuario = true;
                loginUsuario = null;
            } else {
                appendLn("Sesión iniciada como " + loginUsuario + ".", FG_TERMINAL);
                appendLn("Comandos: mkdir, cd, pwd, ls, rm, touch, cat, less, note, chmod, chown, chgrp, ln, whereis, viewFilesOpen, whoami, su, useradd, groupadd, passwd, clear, exit", Color.GRAY);
                loginUsuario = null;
                actualizarPrompt();
                append(dispatcher.getPrompt());
            }
            return;
        }

        if (esperandoFormatPass) {
            appendLn("****", FG_WHITE);
            esperandoFormatPass = false;
            String cmdConPass = formatArgs + " " + entrada;
            ejecutarComando(cmdConPass);
            return;
        }

        if (dispatcher.tieneEditorActivo()) {
            procesarEntradaEditor(entrada);
            return;
        }

        ejecutarComando(entrada);
    }

    private void procesarEntradaEditor(String entrada) {
        String result = dispatcher.procesarEditor(entrada);
        if (result == null) return;

        if (!result.isEmpty()) {
            appendLn(result, FG_TERMINAL);
        }

        actualizarPrompt();

        if (dispatcher.tieneEditorActivo()) {
            append("[note:" + dispatcher.getNombreArchivoEditor() + "] ");
        } else {
            append(dispatcher.estaAutenticado() ? dispatcher.getPrompt()
                : ((disco != null && disco.estaAbierto()) ? "login: " : ""));
        }
    }

    private void ejecutarComando(String linea) {
        String entrada = linea.trim();

        if (dispatcher.estaAutenticado()) {
            append(dispatcher.getPrompt() + entrada + "\n");
        } else if (entrada.toLowerCase().startsWith("format ")) {
            append(entrada + "\n");
        }

        String resultado = dispatcher.despachar(entrada);

        if (resultado == null) {
            areaSalida.setText("");
            actualizarPrompt();
            append(dispatcher.estaAutenticado() ? dispatcher.getPrompt()
                : (disco != null && disco.estaAbierto() ? "login: " : ""));
            return;
        }

        if ("__EXIT__".equals(resultado)) {
            Container parent = getParent();
            if (parent instanceof JTabbedPane tab) {
                if (tab.getTabCount() <= 1) {
                    int opcion = JOptionPane.showConfirmDialog(
                        SwingUtilities.getWindowAncestor(this),
                        "¿Cerrar el programa?", "Salir",
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (opcion == JOptionPane.YES_OPTION) {
                        try { disco.cerrar(); } catch (Exception ignored) {}
                        System.exit(0);
                    }
                } else {
                    tab.remove(this);
                    try {
                        if (tab.getTabCount() == 0) {
                            disco.cerrar();
                            System.exit(0);
                        }
                    } catch (Exception ignored) {}
                }
            }
            return;
        }

        if (!resultado.isEmpty()) {
            appendLn(resultado, FG_TERMINAL);
        }

        actualizarPrompt();

        if (dispatcher.tieneEditorActivo()) {
            append("[note:" + dispatcher.getNombreArchivoEditor() + "] ");
        } else {
            append(dispatcher.estaAutenticado() ? dispatcher.getPrompt()
                : ((disco != null && disco.estaAbierto()) ? "login: " : ""));
        }
    }

    private void actualizarPrompt() {
        SwingUtilities.invokeLater(() -> {
            if (dispatcher.estaAutenticado()) {
                labelPrompt.setText(dispatcher.getPrompt());
                labelUsuario.setText(dispatcher.getNombreUsuario());
            } else {
                labelPrompt.setText(disco != null && disco.estaAbierto()
                    ? "login: " : "sin disco | use format");
                labelUsuario.setText("sin sesión");
            }
        });
    }

    private void append(String texto) {
        SwingUtilities.invokeLater(() -> {
            areaSalida.append(texto);
            areaSalida.setCaretPosition(areaSalida.getDocument().getLength());
        });
    }

    private void appendLn(String texto, Color color) {
        SwingUtilities.invokeLater(() -> {
            int start = areaSalida.getDocument().getLength();
            areaSalida.append(texto + "\n");
            areaSalida.setCaretPosition(areaSalida.getDocument().getLength());
        });
    }

    public void enfocarEntrada() {
        SwingUtilities.invokeLater(() -> campoEntrada.requestFocusInWindow());
    }
}

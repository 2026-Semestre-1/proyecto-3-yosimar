package com.proyecto3.comandos.impl;

import com.proyecto3.comandos.Comando;
import com.proyecto3.nucleo.*;
import com.proyecto3.seguridad.GestorUsuarios;
import com.proyecto3.seguridad.Usuario;
import java.io.File;
import java.io.IOException;

public class ComandoFormat implements Comando {

    private DiscoVirtual disco;
    private Superbloque superbloque;
    private AsignadorBloques asignador;
    private TablaInodos tablaInodos;
    private GestorUsuarios gestorUsuarios;

    public void setDisco(DiscoVirtual disco) { this.disco = disco; }
    public Superbloque getSuperbloque() { return superbloque; }
    public AsignadorBloques getAsignador() { return asignador; }
    public TablaInodos getTablaInodos() { return tablaInodos; }
    public GestorUsuarios getGestorUsuarios() { return gestorUsuarios; }

    @Override
    public String getNombre() { return "format"; }

    @Override
    public String getAyuda() {
        return "format <nombre.fs> <tamañoMB> <inodos> <passwordRoot> - Formatea un nuevo disco virtual";
    }

    @Override
    public String ejecutar(String[] args) {
        if (args.length < 4) {
            return "Uso: format <nombre.fs> <tamañoMB> <inodos> <passwordRoot>";
        }

        String ruta = args[0];
        long tamanioMB;
        int totalInodos;
        try {
            tamanioMB = Long.parseLong(args[1]);
            totalInodos = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            return "Error: tamañoMB e inodos deben ser números";
        }

        if (tamanioMB < 1) return "Error: tamaño mínimo 1 MB";
        if (totalInodos < 1) return "Error: mínimo 1 inodo";

        String passwordRoot = args[3];

        try {
            if (disco == null) {
                disco = new DiscoVirtual(512);
            }

            long tamanioBytes = tamanioMB * 1024 * 1024;
            disco.crearDisco(ruta, tamanioBytes);

            superbloque = new Superbloque();
            superbloque.formatearCon("miSistemaArchivos", disco, tamanioBytes, totalInodos);

            LayoutDisco layout = superbloque.getLayout();

            asignador = new AsignadorBloques(disco, layout);
            asignador.inicializarBitmap();

            tablaInodos = new TablaInodos(disco, layout, totalInodos);
            tablaInodos.inicializarTabla();

            Inodo inodoRaiz = tablaInodos.asignarInodo();
            inodoRaiz.setTipo(Inodo.DIRECTORIO);
            inodoRaiz.setUid(GestorUsuarios.UID_ROOT);
            inodoRaiz.setGid(GestorUsuarios.GID_ROOT);
            inodoRaiz.setPermisos((short) 077);
            int inodoRaizNum = inodoRaiz.getNumero();
            superbloque.setInodoRaiz(inodoRaizNum);
            superbloque.guardar(disco);

            Directorio dirRaiz = new Directorio(disco, asignador, tablaInodos, inodoRaizNum);
            dirRaiz.inicializarDirectorio(inodoRaizNum);
            dirRaiz.guardar();

            gestorUsuarios = new GestorUsuarios();
            gestorUsuarios.inicializarSistema(passwordRoot, inodoRaizNum,
                disco, asignador, tablaInodos);

            Inodo inodoRootHome = tablaInodos.asignarInodo();
            inodoRootHome.setTipo(Inodo.DIRECTORIO);
            inodoRootHome.setUid(GestorUsuarios.UID_ROOT);
            inodoRootHome.setGid(GestorUsuarios.GID_ROOT);
            inodoRootHome.setPermisos((short) 075);
            int inodoRootHomeNum = inodoRootHome.getNumero();

            Directorio dirRootHome = new Directorio(disco, asignador, tablaInodos, inodoRootHomeNum);
            dirRootHome.inicializarDirectorio(inodoRaizNum);
            dirRootHome.guardar();

            dirRaiz.agregarEntrada("root", inodoRootHomeNum);
            dirRaiz.guardar();

            Usuario rootUser = gestorUsuarios.getRoot();
            if (rootUser != null) {
                rootUser.setInodoHome(inodoRootHomeNum);
                gestorUsuarios.guardarEnDisco(disco, asignador, tablaInodos);
            }

            tablaInodos.guardarEnDisco();
            asignador.guardarEnDisco();

            StringBuilder sb = new StringBuilder();
            sb.append("Disco formateado exitosamente: ").append(ruta).append("\n");
            sb.append("  Tamaño total:      ").append(tamanioMB).append(" MB (").append(tamanioBytes).append(" bytes)\n");
            sb.append("  Tamaño de bloque:   ").append(disco.getTamanioBloque()).append(" bytes\n");
            sb.append("  Total de bloques:   ").append(layout.getTotalBloques()).append("\n");
            sb.append("  Inodos:             ").append(totalInodos).append("\n");
            sb.append("  Bloques libres:     ").append(asignador.getBloquesLibres()).append("\n");
            sb.append("  Inodo raíz:         ").append(inodoRaizNum).append("\n");
            sb.append("  Usuario root:       creado (uid=0)\n");
            sb.append("  Grupo root:         creado (gid=0)");

            return sb.toString();

        } catch (IOException e) {
            return "Error al formatear: " + e.getMessage();
        }
    }
}

package views.components;

import java.util.Locale;
import java.util.Objects;

import entities.SesionUsuario;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

public final class MenuCompartido {

    private enum Icono {
        MIS_EVENTOS("M3 4h18v17H3z M7 2v4 M17 2v4 M3 9h18 M7 13h3 M7 17h3"),
        CREAR_EVENTO("M12 5v14 M5 12h14"),
        PROVEEDORES("M3 10l1-6h16l1 6 M5 10v10h14V10 M9 20v-6h6v6"),
        INVITADOS("M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2 M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8 M22 21v-2a4 4 0 0 0-3-3.87 M16 3.13a4 4 0 0 1 0 7.75"),
        MENSAJES("M3 5h18v14H3z M3 7l9 7 9-7"),
        AVISOS("M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9Z M10 21h4"),
        SERVICIOS("M3 8h18v13H3z M8 8V5h8v3 M3 13h18 M10 13v2h4v-2"),
        PROPUESTAS("M4 4h16v13H8l-4 4z M8 9h8 M8 13h5"),
        PUBLICACION("M6 3h9l4 4v14H6z M15 3v5h4 M9 12h7 M9 16h7"),
        SANCIONES("M12 3v18 M5 7h14 M6 7l-4 7h8L6 7z M18 7l-4 7h8l-4-7z M8 21h8"),
        MESA_REGALOS("M20 12v9H4v-9 M2 7h20v5H2z M12 7v14 M12 7H8a2.5 2.5 0 1 1 2.5-2.5C10.5 6 12 7 12 7 M12 7h4a2.5 2.5 0 1 0-2.5-2.5C13.5 6 12 7 12 7");

        private final String trazado;

        Icono(String trazado) {
            this.trazado = trazado;
        }
    }

    public enum Pagina {
        MIS_EVENTOS,
        CREAR_EVENTO,
        INVITADOS,
        CATALOGO,
        MIS_SERVICIOS,
        MIS_INVITACIONES,
        MESA_REGALOS,
        SOLICITUDES_PROVEEDOR,
        AVISOS_PROVEEDOR,
        PROPUESTAS_ORGANIZADOR,
        PUBLICACION_EVENTOS,
        SANCIONES_PROVEEDOR
    }

    private MenuCompartido() {
    }

    public static VBox crear(
            SesionUsuario sesion,
            Pagina paginaActiva,
            Runnable abrirEventos,
            Runnable abrirInvitados,
            Runnable abrirCatalogo,
            Runnable abrirServicios,
            Runnable cerrarSesion) {

        return crear(
                sesion,
                paginaActiva,
                abrirEventos,
                abrirInvitados,
                abrirCatalogo,
                abrirServicios,
                cerrarSesion,
                null);
    }

    public static VBox crear(
            SesionUsuario sesion,
            Pagina paginaActiva,
            Runnable abrirEventos,
            Runnable abrirInvitados,
            Runnable abrirCatalogo,
            Runnable abrirServicios,
            Runnable cerrarSesion,
            Runnable abrirMisEventos) {
        return crear(
                sesion,
                paginaActiva,
                abrirEventos,
                abrirInvitados,
                abrirCatalogo,
                abrirServicios,
                cerrarSesion,
                abrirMisEventos,
                null,
                null);
    }

    public static VBox crear(
            SesionUsuario sesion,
            Pagina paginaActiva,
            Runnable abrirEventos,
            Runnable abrirInvitados,
            Runnable abrirCatalogo,
            Runnable abrirServicios,
            Runnable cerrarSesion,
            Runnable abrirMisEventos,
            Runnable abrirSolicitudes,
            Runnable abrirPropuestas) {
        return crear(sesion, paginaActiva, abrirEventos, abrirInvitados, abrirCatalogo,
                abrirServicios, cerrarSesion, abrirMisEventos, abrirSolicitudes, abrirPropuestas, null);
    }

    /**
     * Igual que el anterior; agrega la opción de sanciones (solo
     * administrador).
     */
    public static VBox crear(
            SesionUsuario sesion,
            Pagina paginaActiva,
            Runnable abrirEventos,
            Runnable abrirInvitados,
            Runnable abrirCatalogo,
            Runnable abrirServicios,
            Runnable cerrarSesion,
            Runnable abrirMisEventos,
            Runnable abrirSolicitudes,
            Runnable abrirPropuestas,
            Runnable abrirSanciones) {
        return crear(
                sesion,
                paginaActiva,
                abrirEventos,
                abrirInvitados,
                abrirCatalogo,
                abrirServicios,
                cerrarSesion,
                abrirMisEventos,
                abrirSolicitudes,
                abrirPropuestas,
                abrirSanciones,
                null);
    }

    /**
     * Igual que el anterior; agrega la opción de avisos del proveedor.
     */
    public static VBox crear(
            SesionUsuario sesion,
            Pagina paginaActiva,
            Runnable abrirEventos,
            Runnable abrirInvitados,
            Runnable abrirCatalogo,
            Runnable abrirServicios,
            Runnable cerrarSesion,
            Runnable abrirMisEventos,
            Runnable abrirSolicitudes,
            Runnable abrirPropuestas,
            Runnable abrirSanciones,
            Runnable abrirAvisos) {
        return crear(sesion, paginaActiva, abrirEventos, abrirInvitados, abrirCatalogo,
                abrirServicios, cerrarSesion, abrirMisEventos, abrirSolicitudes,
                abrirPropuestas, abrirSanciones, abrirAvisos, null);
    }

    public static VBox crear(
            SesionUsuario sesion,
            Pagina paginaActiva,
            Runnable abrirEventos,
            Runnable abrirInvitados,
            Runnable abrirCatalogo,
            Runnable abrirServicios,
            Runnable cerrarSesion,
            Runnable abrirMisEventos,
            Runnable abrirSolicitudes,
            Runnable abrirPropuestas,
            Runnable abrirSanciones,
            Runnable abrirAvisos,
            Runnable abrirPublicacion) {
        VBox menu = new VBox(7);
        menu.getStyleClass().add("sidebar");
        menu.setPrefWidth(250);
        menu.setMinWidth(250);

        // Todas las pantallas utilizan exactamente la misma imagen
        ImageView logo = crearLogo();

        VBox marca = new VBox(10, logo,
                etiqueta("GESTIÓN DE EVENTOS", "brand-caption"));
        marca.setAlignment(Pos.CENTER);
        VBox.setMargin(marca, new Insets(0, 0, 38, 0));

        menu.getChildren().addAll(
                marca,
                etiqueta("MENÚ PRINCIPAL", "menu-title"));

        boolean proveedor = sesion != null
                && "PROVEEDOR".equalsIgnoreCase(sesion.getTipoUsuario());

        if (proveedor) {
            agregarOpcion(menu, Icono.SERVICIOS, "Mis servicios",
                    paginaActiva == Pagina.MIS_SERVICIOS,
                    abrirServicios);
            agregarOpcion(menu, Icono.MENSAJES, "Solicitudes recibidas",
                    paginaActiva == Pagina.SOLICITUDES_PROVEEDOR,
                    abrirSolicitudes);
            agregarOpcion(menu, Icono.AVISOS, "Mis avisos",
                    paginaActiva == Pagina.AVISOS_PROVEEDOR,
                    abrirAvisos);
        } else {
            if (abrirMisEventos != null) {
                agregarOpcion(menu, Icono.MIS_EVENTOS, "Mis eventos",
                        paginaActiva == Pagina.MIS_EVENTOS,
                        abrirMisEventos);
            }

            agregarOpcion(menu, Icono.CREAR_EVENTO, "Crear evento",
                    paginaActiva == Pagina.CREAR_EVENTO,
                    abrirEventos);

            agregarOpcion(menu, Icono.PROVEEDORES, "Proveedores",
                    paginaActiva == Pagina.CATALOGO,
                    abrirCatalogo);

            agregarOpcion(menu, Icono.INVITADOS, "Invitados",
                    paginaActiva == Pagina.INVITADOS,
                    abrirInvitados);
            agregarOpcion(menu, Icono.PROPUESTAS, "Propuestas recibidas",
                    paginaActiva == Pagina.PROPUESTAS_ORGANIZADOR,
                    abrirPropuestas);
        }

        if (sesion != null && "ADMINISTRADOR".equalsIgnoreCase(sesion.getTipoUsuario())) {
            agregarOpcion(menu, Icono.PUBLICACION, "Publicación",
                    paginaActiva == Pagina.PUBLICACION_EVENTOS,
                    abrirPublicacion);

            agregarOpcion(menu, Icono.SANCIONES, "Sanciones",
                    paginaActiva == Pagina.SANCIONES_PROVEEDOR,
                    abrirSanciones);
        }

        Region espacio = new Region();
        VBox.setVgrow(espacio, Priority.ALWAYS);

        menu.getChildren().add(espacio);
        if (sesion != null) {
            String nombre = nombreSesion(sesion);
            Label nombrePerfil = etiqueta(nombre, "profile-name");
            nombrePerfil.setMaxWidth(Double.MAX_VALUE);
            nombrePerfil.setTextOverrun(OverrunStyle.ELLIPSIS);
            VBox perfil = new VBox(4,
                    nombrePerfil,
                    etiqueta(rolSesion(sesion), "hint"));
            perfil.setMinWidth(0);
            HBox.setHgrow(perfil, Priority.ALWAYS);
            if (cerrarSesion != null) {
                Label salir = etiqueta("Cerrar sesión", "logout-link");
                salir.setOnMouseClicked(event -> cerrarSesion.run());
                salir.setStyle("-fx-cursor: hand;");
                perfil.getChildren().add(salir);
            }

            Label avatar = etiqueta(iniciales(nombre), "avatar");
            avatar.setMinSize(42, 42);
            avatar.setPrefSize(42, 42);
            avatar.setMaxSize(42, 42);

            HBox cuenta = new HBox(12, avatar, perfil);
            cuenta.setAlignment(Pos.CENTER_LEFT);
            cuenta.setMaxWidth(Double.MAX_VALUE);
            cuenta.getStyleClass().add("sidebar-bottom");
            menu.getChildren().add(cuenta);
        }
        return menu;
    }

    /**
     * Menú del invitado (CU-16 y CU-20). El invitado no tiene cuenta: entra
     * desde "Soy invitado" en el login y se identifica con su correo y
     * teléfono.
     */
    public static VBox crearInvitado(
            Pagina paginaActiva,
            Runnable abrirInvitaciones,
            Runnable abrirMesaRegalos,
            Runnable salir) {

        VBox menu = new VBox(7);
        menu.getStyleClass().add("sidebar");
        menu.setPrefWidth(250);
        menu.setMinWidth(250);

        VBox marca = new VBox(10, crearLogo(),
                etiqueta("GESTIÓN DE EVENTOS", "brand-caption"));
        marca.setAlignment(Pos.CENTER);
        VBox.setMargin(marca, new Insets(0, 0, 38, 0));

        menu.getChildren().addAll(
                marca,
                etiqueta("MENÚ DEL INVITADO", "menu-title"));

        agregarOpcion(menu, Icono.MENSAJES, "Mis invitaciones",
                paginaActiva == Pagina.MIS_INVITACIONES,
                abrirInvitaciones);
        agregarOpcion(menu, Icono.MESA_REGALOS, "Mesa de regalos",
                paginaActiva == Pagina.MESA_REGALOS,
                abrirMesaRegalos);

        Region espacio = new Region();
        VBox.setVgrow(espacio, Priority.ALWAYS);
        menu.getChildren().add(espacio);

        VBox perfil = new VBox(4,
                etiqueta("Invitado", "profile-name"),
                etiqueta("Sin cuenta", "hint"));
        perfil.setMinWidth(0);
        HBox.setHgrow(perfil, Priority.ALWAYS);
        if (salir != null) {
            Label enlace = etiqueta("Cerrar sesión", "logout-link");
            enlace.setOnMouseClicked(event -> salir.run());
            enlace.setStyle("-fx-cursor: hand;");
            perfil.getChildren().add(enlace);
        }
        Label avatar = etiqueta("IN", "avatar");
        avatar.setMinSize(42, 42);
        avatar.setPrefSize(42, 42);
        avatar.setMaxSize(42, 42);
        HBox cuenta = new HBox(12, avatar, perfil);
        cuenta.setAlignment(Pos.CENTER_LEFT);
        cuenta.setMaxWidth(Double.MAX_VALUE);
        cuenta.getStyleClass().add("sidebar-bottom");
        menu.getChildren().add(cuenta);
        return menu;
    }

    public static ImageView crearLogo() {
        ImageView logo = new ImageView(new Image(
                Objects.requireNonNull(
                        MenuCompartido.class.getResource("/images/logo-nexteventux.png"),
                        "No se encontró /images/logo-nexteventux.png")
                        .toExternalForm()));
        logo.setFitWidth(190);
        logo.setPreserveRatio(true);
        logo.setSmooth(true);
        return logo;
    }

    public static HBox crearBarraSuperior(
            SesionUsuario sesion,
            String titulo) {

        return crearBarraSuperior(sesion, titulo, null, null);
    }

    public static HBox crearBarraSuperior(
            SesionUsuario sesion,
            String titulo,
            String textoRegreso,
            Runnable alRegresar) {

        HBox barra = new HBox(18);
        if (alRegresar != null) {
            Button volver = new Button("← " + textoRegreso);
            volver.getStyleClass().add("btn-secondary");
            volver.setOnAction(evento -> alRegresar.run());
            barra.getChildren().add(volver);
        }
        barra.getChildren().addAll(
                etiqueta("Inicio /", "breadcrumb"),
                etiqueta(titulo, "breadcrumb-current"));
        barra.getChildren().add(espacioHorizontal());
        if (sesion != null) {
            barra.getChildren().add(
                    etiqueta(nombreSesion(sesion), "user-badge"));
        }
        configurarBarraSuperior(barra);
        return barra;
    }

    public static HBox crearBarraSuperior(
            String titulo,
            String nombreUsuario) {

        HBox barra = new HBox(
                18,
                etiqueta("Inicio /", "breadcrumb"),
                etiqueta(titulo, "breadcrumb-current"));
        barra.getChildren().add(espacioHorizontal());
        if (nombreUsuario != null && !nombreUsuario.isBlank()) {
            barra.getChildren().add(etiqueta(nombreUsuario, "user-badge"));
        }
        configurarBarraSuperior(barra);
        return barra;
    }

    private static void configurarBarraSuperior(HBox barra) {
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.setPrefHeight(78);
        barra.setMinHeight(78);
        barra.getStyleClass().add("topbar");
    }

    private static void agregarOpcion(
            VBox menu,
            Icono icono,
            String texto,
            boolean activa,
            Runnable accion) {

        if (accion == null) {
            return;
        }

        SVGPath grafico = new SVGPath();
        grafico.setContent(icono.trazado);
        grafico.setScaleX(0.75);
        grafico.setScaleY(0.75);
        grafico.getStyleClass().add("menu-icon");

        StackPane contenedorIcono = new StackPane(grafico);
        contenedorIcono.setMinSize(20, 20);
        contenedorIcono.setPrefSize(20, 20);
        contenedorIcono.setMaxSize(20, 20);

        Label etiquetaTexto = etiqueta(texto, "menu-label");
        HBox item = new HBox(8, contenedorIcono, etiquetaTexto);
        item.setAlignment(Pos.CENTER_LEFT);
        item.getStyleClass().add("menu-item");
        item.setMaxWidth(Double.MAX_VALUE);

        if (activa) {
            item.getStyleClass().add("active");
        }

        item.setOnMouseClicked(event -> accion.run());
        item.setStyle("-fx-cursor: hand;");

        menu.getChildren().add(item);
    }

    private static Label etiqueta(String texto, String estilo) {
        Label label = new Label(texto);
        label.getStyleClass().add(estilo);
        return label;
    }

    private static String iniciales(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "U";
        }

        String[] partes = nombre.trim().split("\\s+");

        String resultado = partes[0].substring(0, 1);

        if (partes.length > 1) {
            resultado += partes[partes.length - 1].substring(0, 1);
        }

        return resultado.toUpperCase(Locale.ROOT);
    }

    private static String nombreSesion(SesionUsuario sesion) {
        if (sesion == null || sesion.getNombre() == null
                || sesion.getNombre().isBlank()) {
            return "Usuario";
        }
        return sesion.getNombre().trim();
    }

    private static String rolSesion(SesionUsuario sesion) {
        if (sesion == null || sesion.getTipoUsuario() == null
                || sesion.getTipoUsuario().isBlank()) {
            return "Usuario";
        }

        String rol = sesion.getTipoUsuario().trim().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(rol.charAt(0)) + rol.substring(1);
    }

    private static Region espacioHorizontal() {
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        return espacio;
    }
}

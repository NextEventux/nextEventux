package views;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import controllers.ControllerProveedores;
import entities.ImagenServicio;
import entities.PreguntaServicio;
import entities.Servicio;
import entities.SesionUsuario;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

public class ViewListaServiciosProveedor {

    private final ControllerProveedores controller;
    private final int idProveedor;
    private final SesionUsuario sesion;
    private final Runnable cerrarSesion;
    private final Consumer<Integer> abrirDetalle;
    private final Runnable nuevoServicio;
    private final Runnable abrirSolicitudes;

    private final List<ServicioVista> servicios
            = new ArrayList<>();

    private final TextField busqueda
            = new TextField();

    private final ToggleGroup grupoFiltros
            = new ToggleGroup();

    private final ToggleButton filtroTodos
            = crearFiltro("Todos", "TODOS");

    private final ToggleButton filtroDisponibles
            = crearFiltro("Disponibles", "DISPONIBLES");

    private final ToggleButton filtroPreguntas
            = crearFiltro("Con preguntas", "PREGUNTAS");

    private final ToggleButton filtroSinFotos
            = crearFiltro("Sin fotos", "SIN_FOTOS");

    private final TilePane galeria
            = new TilePane(18, 18);

    private final Label resumen
            = new Label();

    public ViewListaServiciosProveedor(
            ControllerProveedores controller,
            int idProveedor,
            SesionUsuario sesion,
            Runnable cerrarSesion,
            Consumer<Integer> abrirDetalle,
            Runnable nuevoServicio,
            Runnable abrirSolicitudes) {

        this.controller = controller;
        this.idProveedor = idProveedor;
        this.sesion = sesion;
        this.cerrarSesion = cerrarSesion;
        this.abrirDetalle = abrirDetalle;
        this.nuevoServicio = nuevoServicio;
        this.abrirSolicitudes = abrirSolicitudes;
    }

    public Parent crearPantalla() {

        BorderPane raiz
                = new BorderPane();

        raiz
                .getStyleClass()
                .add("app");

        VBox sidebar
                = crearMenu();

        raiz.setLeft(sidebar);

        BorderPane principal
                = new BorderPane();

        principal.setTop(
                crearBarraSuperior());

        VBox contenido
                = new VBox(
                        24,
                        crearEncabezado(),
                        crearHerramientas(),
                        galeria);

        contenido
                .getStyleClass()
                .addAll(
                        "content",
                        "provider-gallery-content");

        contenido.setMaxWidth(1320);

        galeria
                .getStyleClass()
                .add("provider-gallery");

        galeria.setPrefTileWidth(270);
        galeria.setPrefTileHeight(285);
        galeria.setTileAlignment(Pos.TOP_LEFT);

        StackPane centrado
                = new StackPane(contenido);

        centrado.setAlignment(
                Pos.TOP_CENTER);

        ScrollPane scroll
                = new ScrollPane(centrado);

        scroll.setFitToWidth(true);

        scroll.setHbarPolicy(
                ScrollPane.ScrollBarPolicy.NEVER);

        scroll
                .getStyleClass()
                .add("content-scroll");

        principal.setCenter(scroll);

        raiz.setCenter(principal);

        raiz
                .widthProperty()
                .addListener(
                        (observable, anterior, actual) -> {

                            sidebar.setVisible(
                                    actual.doubleValue() >= 1000);

                            sidebar.setManaged(
                                    sidebar.isVisible());
                        });

        configurarFiltros();
        recargarServicios();

        return raiz;
    }

    private HBox crearEncabezado() {

        resumen
                .getStyleClass()
                .add("provider-gallery-summary");

        VBox titulo
                = new VBox(
                        7,
                        etiqueta(
                                "Mis servicios",
                                "page-title"),
                        resumen);

        Button nuevo
                = new Button(
                        "+ Nuevo servicio");

        nuevo
                .getStyleClass()
                .add("btn-primary");

        nuevo.setOnAction(
                event -> {

                    if (nuevoServicio != null) {
                        nuevoServicio.run();
                    }
                });

        HBox encabezado
                = new HBox(
                        20,
                        titulo,
                        espacioHorizontal(),
                        nuevo);

        encabezado.setAlignment(
                Pos.CENTER_LEFT);

        return encabezado;
    }

    private HBox crearHerramientas() {

        busqueda.setPromptText(
                "Buscar por nombre, categoría o ciudad...");

        busqueda
                .getStyleClass()
                .add("provider-search");

        busqueda.setPrefWidth(310);

        HBox filtros
                = new HBox(
                        22,
                        filtroTodos,
                        filtroDisponibles,
                        filtroPreguntas,
                        filtroSinFotos);

        filtros.setAlignment(
                Pos.CENTER_LEFT);

        HBox barra
                = new HBox(
                        24,
                        busqueda,
                        filtros);

        barra.setAlignment(
                Pos.CENTER_LEFT);

        barra
                .getStyleClass()
                .add("provider-gallery-tools");

        HBox.setHgrow(
                filtros,
                Priority.ALWAYS);

        return barra;
    }

    private ToggleButton crearFiltro(
            String texto,
            String valor) {

        ToggleButton filtro
                = new ToggleButton(texto);

        filtro.setUserData(valor);

        filtro
                .getStyleClass()
                .add("provider-filter");

        return filtro;
    }

    private void configurarFiltros() {

        ToggleButton[] filtros = {
            filtroTodos,
            filtroDisponibles,
            filtroPreguntas,
            filtroSinFotos
        };

        for (ToggleButton filtro : filtros) {

            filtro.setToggleGroup(
                    grupoFiltros);
        }

        filtroTodos.setSelected(true);

        grupoFiltros
                .selectedToggleProperty()
                .addListener(
                        (observable, anterior, actual) -> {

                            if (actual == null
                            && anterior != null) {

                                anterior.setSelected(true);
                                return;
                            }

                            aplicarFiltros();
                        });

        busqueda
                .textProperty()
                .addListener(
                        (observable, anterior, actual)
                        -> aplicarFiltros());
    }

    private void recargarServicios() {

        servicios.clear();

        try {

            List<Servicio> encontrados
                    = controller
                            .listarServiciosProveedor(
                                    idProveedor);

            for (Servicio servicio
                    : encontrados) {

                servicios.add(
                        crearVista(
                                servicio));
            }

        } catch (Exception e) {

            servicios.clear();
        }

        actualizarContadores();
        aplicarFiltros();
    }

    private ServicioVista crearVista(
            Servicio servicio) {

        String portada
                = null;

        int preguntasPendientes
                = 0;

        try {

            List<ImagenServicio> imagenes
                    = controller
                            .listarImagenesServicio(
                                    servicio.getIdServicio());

            portada
                    = imagenes
                            .stream()
                            .min(
                                    Comparator.comparingInt(
                                            ImagenServicio::getOrdenImagen))
                            .map(
                                    ImagenServicio::getRuta)
                            .orElse(null);

        } catch (Exception e) {

            portada = null;
        }

        try {

            List<PreguntaServicio> preguntas
                    = controller
                            .listarPreguntasPublicas(
                                    servicio.getIdServicio());

            for (PreguntaServicio pregunta
                    : preguntas) {

                if (!"RESPONDIDA"
                        .equalsIgnoreCase(
                                pregunta.getEstado())) {

                    preguntasPendientes++;
                }
            }

        } catch (Exception e) {

            preguntasPendientes = 0;
        }

        return new ServicioVista(
                servicio,
                portada,
                preguntasPendientes);
    }

    private void actualizarContadores() {

        int disponibles
                = 0;

        int conPreguntas
                = 0;

        int sinFotos
                = 0;

        for (ServicioVista vista
                : servicios) {

            if ("DISPONIBLE"
                    .equalsIgnoreCase(
                            vista.servicio.getEstado())) {

                disponibles++;
            }

            if (vista.preguntasPendientes > 0) {
                conPreguntas++;
            }

            if (vista.portada == null) {
                sinFotos++;
            }
        }

        filtroTodos.setText(
                "Todos  " + servicios.size());

        filtroDisponibles.setText(
                "Disponibles  " + disponibles);

        filtroPreguntas.setText(
                "Con preguntas  " + conPreguntas);

        filtroSinFotos.setText(
                "Sin fotos  " + sinFotos);

        resumen.setText(
                servicios.size() == 1
                ? "1 servicio publicado o en preparación"
                : servicios.size()
                + " servicios publicados o en preparación");
    }

    private void aplicarFiltros() {

        galeria
                .getChildren()
                .clear();

        String texto
                = busqueda
                        .getText()
                        .trim()
                        .toLowerCase();

        String filtro
                = "TODOS";

        if (grupoFiltros.getSelectedToggle()
                != null) {

            filtro
                    = String.valueOf(
                            grupoFiltros
                                    .getSelectedToggle()
                                    .getUserData());
        }

        for (ServicioVista vista
                : servicios) {

            Servicio servicio
                    = vista.servicio;

            if (!coincideBusqueda(
                    servicio,
                    texto)) {

                continue;
            }

            if (!coincideFiltro(
                    vista,
                    filtro)) {

                continue;
            }

            galeria
                    .getChildren()
                    .add(
                            crearTarjeta(
                                    vista));
        }

        galeria
                .getChildren()
                .add(
                        crearTarjetaNueva());
    }

    private boolean coincideBusqueda(
            Servicio servicio,
            String texto) {

        if (texto.isBlank()) {
            return true;
        }

        String nombre
                = seguro(
                        servicio.getNombre())
                        .toLowerCase();

        String categoria
                = seguro(
                        servicio.getCategoria())
                        .toLowerCase();

        String ciudad
                = seguro(
                        servicio.getCiudadZona())
                        .toLowerCase();

        return nombre.contains(texto)
                || categoria.contains(texto)
                || ciudad.contains(texto);
    }

    private boolean coincideFiltro(
            ServicioVista vista,
            String filtro) {

        switch (filtro) {

            case "DISPONIBLES":

                return "DISPONIBLE"
                        .equalsIgnoreCase(
                                vista.servicio.getEstado());

            case "PREGUNTAS":

                return vista.preguntasPendientes > 0;

            case "SIN_FOTOS":

                return vista.portada == null;

            default:

                return true;
        }
    }

    private VBox crearTarjeta(
            ServicioVista vista) {

        Servicio servicio
                = vista.servicio;

        StackPane portada
                = crearPortada(
                        vista);

        Label nombre
                = etiqueta(
                        servicio.getNombre(),
                        "provider-tile-name");

        nombre.setWrapText(true);

        Label meta
                = etiqueta(
                        textoCategoria(
                                servicio),
                        "provider-tile-meta");

        Label precio
                = etiqueta(
                        formatearPrecio(
                                servicio.getPrecio()),
                        "provider-tile-price");

        Label estado
                = crearEstado(
                        servicio.getEstado());

        HBox pie
                = new HBox(
                        10,
                        precio,
                        espacioHorizontal(),
                        estado);

        pie.setAlignment(
                Pos.CENTER_LEFT);

        VBox tarjeta
                = new VBox(
                        portada,
                        nombre,
                        meta,
                        pie);

        tarjeta.setPrefWidth(270);
        tarjeta.setMaxWidth(270);

        tarjeta
                .getStyleClass()
                .add("provider-tile");

        VBox.setMargin(
                nombre,
                new Insets(
                        12,
                        0,
                        0,
                        0));

        VBox.setMargin(
                meta,
                new Insets(
                        4,
                        0,
                        0,
                        0));

        VBox.setMargin(
                pie,
                new Insets(
                        10,
                        0,
                        0,
                        0));

        if (vista.preguntasPendientes > 0) {

            String texto
                    = vista.preguntasPendientes == 1
                            ? "● 1 pregunta sin responder"
                            : "● "
                            + vista.preguntasPendientes
                            + " preguntas sin responder";

            Label aviso
                    = etiqueta(
                            texto,
                            "provider-tile-pending");

            tarjeta
                    .getChildren()
                    .add(aviso);

            VBox.setMargin(
                    aviso,
                    new Insets(
                            6,
                            0,
                            0,
                            0));
        }

        tarjeta.setOnMouseClicked(
                event -> {

                    if (abrirDetalle != null) {

                        abrirDetalle.accept(
                                servicio.getIdServicio());
                    }
                });

        return tarjeta;
    }

    private StackPane crearPortada(
            ServicioVista vista) {

        StackPane portada
                = new StackPane();

        portada.setPrefHeight(170);
        portada.setMinHeight(170);
        portada.setMaxHeight(170);

        portada
                .getStyleClass()
                .add("provider-cover");

        if (vista.portada != null
                && !vista.portada.isBlank()) {

            try {

                ImageView imagen
                        = new ImageView(
                                new Image(
                                        vista.portada,
                                        true));

                imagen.setSmooth(true);
                imagen.setPreserveRatio(false);

                imagen
                        .fitWidthProperty()
                        .bind(
                                portada
                                        .widthProperty());

                imagen
                        .fitHeightProperty()
                        .bind(
                                portada
                                        .heightProperty());

                Rectangle recorte
                        = new Rectangle();

                recorte
                        .widthProperty()
                        .bind(
                                portada
                                        .widthProperty());

                recorte
                        .heightProperty()
                        .bind(
                                portada
                                        .heightProperty());

                recorte.setArcWidth(24);
                recorte.setArcHeight(24);

                imagen.setClip(
                        recorte);

                portada
                        .getChildren()
                        .add(imagen);

                Label foto
                        = etiqueta(
                                "FOTO DEL SERVICIO",
                                "provider-cover-label");

                StackPane.setAlignment(
                        foto,
                        Pos.BOTTOM_LEFT);

                StackPane.setMargin(
                        foto,
                        new Insets(
                                0,
                                0,
                                10,
                                12));

                portada
                        .getChildren()
                        .add(foto);

                return portada;

            } catch (Exception e) {

                // Si la imagen no carga se muestra el icono
            }
        }

        portada
                .getStyleClass()
                .add("provider-cover-empty");

        Label icono
                = etiqueta(
                        iconoCategoria(
                                vista.servicio.getCategoria()),
                        "provider-cover-icon");

        Label ayuda
                = etiqueta(
                        "Sin foto todavía",
                        "provider-cover-help");

        StackPane.setAlignment(
                ayuda,
                Pos.BOTTOM_LEFT);

        StackPane.setMargin(
                ayuda,
                new Insets(
                        0,
                        0,
                        11,
                        13));

        portada
                .getChildren()
                .addAll(
                        icono,
                        ayuda);

        return portada;
    }

    private StackPane crearTarjetaNueva() {

        VBox contenido
                = new VBox(
                        6,
                        etiqueta(
                                "+",
                                "provider-new-plus"),
                        etiqueta(
                                "Nuevo servicio",
                                "provider-new-text"));

        contenido.setAlignment(
                Pos.CENTER);

        StackPane nuevo
                = new StackPane(
                        contenido);

        nuevo.setPrefWidth(270);
        nuevo.setMaxWidth(270);
        nuevo.setPrefHeight(170);
        nuevo.setMinHeight(170);
        nuevo.setMaxHeight(170);

        nuevo
                .getStyleClass()
                .add("provider-new-tile");

        nuevo.setOnMouseClicked(
                event -> {

                    if (nuevoServicio != null) {
                        nuevoServicio.run();
                    }
                });

        return nuevo;
    }

    private Label crearEstado(
            String estado) {

        Label etiqueta
                = new Label();

        etiqueta
                .getStyleClass()
                .add("service-status");

        if ("DISPONIBLE"
                .equalsIgnoreCase(estado)) {

            etiqueta.setText(
                    "● Disponible");

            etiqueta
                    .getStyleClass()
                    .add("status-available");

        } else if ("BORRADOR"
                .equalsIgnoreCase(estado)) {

            etiqueta.setText(
                    "● Borrador");

            etiqueta
                    .getStyleClass()
                    .add("status-draft");

        } else if ("RETIRADO"
                .equalsIgnoreCase(estado)) {

            etiqueta.setText(
                    "● Retirado");

            etiqueta
                    .getStyleClass()
                    .add("status-retired");

        } else {

            etiqueta.setText(
                    seguro(estado));
        }

        return etiqueta;
    }

    private String textoCategoria(
            Servicio servicio) {

        String categoria
                = formatearCategoria(
                        servicio.getCategoria());

        String ciudad
                = seguro(
                        servicio.getCiudadZona());

        if (ciudad.isBlank()) {
            return categoria;
        }

        return categoria
                + " · "
                + ciudad;
    }

    private String formatearCategoria(
            String categoria) {

        if (categoria == null
                || categoria.isBlank()) {

            return "Sin categoría";
        }

        String texto
                = categoria
                        .toLowerCase()
                        .replace(
                                '_',
                                ' ');

        return Character.toUpperCase(
                texto.charAt(0))
                + texto.substring(1);
    }

    private String formatearPrecio(
            double precio) {

        DecimalFormatSymbols simbolos
                = new DecimalFormatSymbols(
                        new Locale(
                                "es",
                                "CO"));

        simbolos.setGroupingSeparator('.');

        return new DecimalFormat(
                "$#,##0",
                simbolos)
                .format(precio);
    }

    private String iconoCategoria(
            String categoria) {

        if (categoria == null) {
            return "✦";
        }

        switch (categoria.toUpperCase()) {

            case "FOTOGRAFIA":
                return "📷";

            case "DECORACION":
                return "✿";

            case "CATERING":
            case "ALIMENTACION":
                return "🍽";

            case "MUSICA":
                return "♫";

            case "ENTRETENIMIENTO":
                return "★";

            case "LOGISTICA":
                return "▣";

            default:
                return "✦";
        }
    }

    private VBox crearMenu() {

        VBox menu
                = new VBox(7);

        menu
                .getStyleClass()
                .add("sidebar");

        menu.setPrefWidth(250);
        menu.setMinWidth(250);

        VBox marca
                = new VBox(
                        5,
                        etiqueta(
                                "🎪",
                                "brand-icon"),
                        etiqueta(
                                "nextEventux",
                                "brand"),
                        etiqueta(
                                "GESTIÓN DE EVENTOS",
                                "brand-caption"));

        marca.setAlignment(
                Pos.CENTER);

        VBox.setMargin(
                marca,
                new Insets(
                        0,
                        0,
                        38,
                        0));

        menu
                .getChildren()
                .addAll(
                        marca,
                        etiqueta(
                                "MENÚ PRINCIPAL",
                                "menu-title"));

        String[] opciones = {
            "⌂  Inicio",
            "♧  Mis servicios",
            "✉  Solicitudes recibidas",
            "⚙  Configuración"
        };

        for (String opcion : opciones) {

            Label item
                    = etiqueta(
                            opcion,
                            "menu-item");

            item.setMaxWidth(Double.MAX_VALUE);

            if (opcion.contains("Solicitudes recibidas")) {
                item.setOnMouseClicked(
                        event -> abrirSolicitudes.run());
            }

            if (opcion.contains(
                    "Mis servicios")) {

                item
                        .getStyleClass()
                        .add("active");
            }

            menu
                    .getChildren()
                    .add(item);
        }

        Region espacio
                = new Region();

        VBox.setVgrow(
                espacio,
                Priority.ALWAYS);

        Label salir
                = etiqueta(
                        "Cerrar sesión",
                        "logout-link");

        if (cerrarSesion != null) {

            salir.setOnMouseClicked(
                    event -> cerrarSesion.run());
        }

        VBox perfil
                = new VBox(
                        4,
                        etiqueta(
                                nombreSesion(),
                                "profile-name"),
                        etiqueta(
                                rolSesion(),
                                "hint"),
                        salir);

        HBox cuenta
                = new HBox(
                        12,
                        etiqueta(
                                inicialesSesion(),
                                "avatar"),
                        perfil);

        cuenta.setAlignment(
                Pos.CENTER_LEFT);

        cuenta
                .getStyleClass()
                .add("sidebar-bottom");

        menu
                .getChildren()
                .addAll(
                        espacio,
                        cuenta);

        return menu;
    }

    private HBox crearBarraSuperior() {

        HBox barra
                = new HBox(
                        18,
                        etiqueta(
                                "Inicio / Proveedores /",
                                "breadcrumb"),
                        etiqueta(
                                "Mis servicios",
                                "breadcrumb-current"),
                        espacioHorizontal(),
                        etiqueta(
                                "♧",
                                "notification"),
                        etiqueta(
                                nombreSesion(),
                                "user-badge"));

        barra.setAlignment(
                Pos.CENTER_LEFT);

        barra
                .getStyleClass()
                .add("topbar");

        return barra;
    }

    private String nombreSesion() {

        if (sesion == null
                || sesion.getNombre() == null
                || sesion.getNombre().isBlank()) {

            return "Proveedor";
        }

        return sesion.getNombre();
    }

    private String rolSesion() {

        if (sesion == null
                || sesion.getTipoUsuario() == null
                || sesion.getTipoUsuario().isBlank()) {

            return "Proveedor";
        }

        String rol
                = sesion
                        .getTipoUsuario()
                        .toLowerCase();

        return Character.toUpperCase(
                rol.charAt(0))
                + rol.substring(1);
    }

    private String inicialesSesion() {

        String[] partes
                = nombreSesion()
                        .trim()
                        .split("\\s+");

        StringBuilder iniciales
                = new StringBuilder();

        for (String parte : partes) {

            if (!parte.isBlank()) {

                iniciales.append(
                        Character.toUpperCase(
                                parte.charAt(0)));
            }

            if (iniciales.length() == 2) {
                break;
            }
        }

        return iniciales.length() == 0
                ? "PR"
                : iniciales.toString();
    }

    private String seguro(
            String texto) {

        return texto == null
                ? ""
                : texto;
    }

    private Label etiqueta(
            String texto,
            String clase) {

        Label label
                = new Label(texto);

        label
                .getStyleClass()
                .add(clase);

        return label;
    }

    private Region espacioHorizontal() {

        Region espacio
                = new Region();

        HBox.setHgrow(
                espacio,
                Priority.ALWAYS);

        return espacio;
    }

    private static class ServicioVista {

        private final Servicio servicio;
        private final String portada;
        private final int preguntasPendientes;

        private ServicioVista(
                Servicio servicio,
                String portada,
                int preguntasPendientes) {

            this.servicio
                    = servicio;

            this.portada
                    = portada;

            this.preguntasPendientes
                    = preguntasPendientes;
        }
    }
}

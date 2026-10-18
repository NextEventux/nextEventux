package views;

import controllers.ControllerProveedores;
import entities.Servicio;
import entities.SesionUsuario;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class ViewListaServiciosProveedor {

    private final ControllerProveedores controller;
    private final int idProveedor;
    private final SesionUsuario sesion;
    private final Runnable cerrarSesion;
    private final Consumer<Integer> abrirDetalle;
    private final Runnable nuevoServicio;

    private final ListView<Servicio> listaServicios =
            new ListView<>();

    public ViewListaServiciosProveedor(
            ControllerProveedores controller,
            int idProveedor,
            SesionUsuario sesion,
            Runnable cerrarSesion,
            Consumer<Integer> abrirDetalle,
            Runnable nuevoServicio) {

        this.controller = controller;
        this.idProveedor = idProveedor;
        this.sesion = sesion;
        this.cerrarSesion = cerrarSesion;
        this.abrirDetalle = abrirDetalle;
        this.nuevoServicio = nuevoServicio;
    }

    public Parent crearPantalla() {

        BorderPane raiz = new BorderPane();

        raiz
                .getStyleClass()
                .add("app");

        VBox sidebar = crearMenu();

        raiz.setLeft(sidebar);

        BorderPane principal = new BorderPane();

        principal.setTop(
                crearBarraSuperior());

        VBox contenido = new VBox(30);

        contenido
                .getStyleClass()
                .add("content");

        contenido.setMaxWidth(1320);

        VBox titulo = new VBox(
                8,
                etiqueta(
                        "Mis servicios",
                        "page-title"),
                etiqueta(
                        "Consulta y administra los servicios que ofreces.",
                        "muted"));

        Button nuevo =
                new Button("+ Nuevo servicio");

        nuevo
                .getStyleClass()
                .add("btn-primary");

        nuevo.setOnAction(
                event -> {

                    if (nuevoServicio != null) {
                        nuevoServicio.run();
                    }
                });

        HBox encabezado =
                new HBox(
                        20,
                        titulo,
                        espacioHorizontal(),
                        nuevo);

        encabezado.setAlignment(
                Pos.CENTER_LEFT);

        VBox tarjeta =
                crearTarjetaServicios();

        contenido
                .getChildren()
                .addAll(
                        encabezado,
                        tarjeta);

        StackPane centrado =
                new StackPane(contenido);

        centrado.setAlignment(
                Pos.TOP_CENTER);

        ScrollPane scroll =
                new ScrollPane(centrado);

        scroll.setFitToWidth(true);

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

        configurarLista();
        recargarServicios();

        return raiz;
    }

    private VBox crearTarjetaServicios() {

        VBox tarjeta =
                new VBox(22);

        tarjeta
                .getStyleClass()
                .add("card");

        HBox titulo =
                new HBox(
                        13,
                        etiqueta(
                                "♧",
                                "heading-icon"),
                        etiqueta(
                                "Servicios del proveedor",
                                "card-title"));

        titulo.setAlignment(
                Pos.CENTER_LEFT);

        Label ayuda =
                etiqueta(
                        "Selecciona un servicio para consultar o modificar su información.",
                        "muted");

        ayuda.setWrapText(true);

        listaServicios
                .getStyleClass()
                .add("publication-list");

        listaServicios.setPrefHeight(520);

        tarjeta
                .getChildren()
                .addAll(
                        titulo,
                        ayuda,
                        listaServicios);

        return tarjeta;
    }

    private void configurarLista() {

        listaServicios.setCellFactory(
                lista -> new ListCell<>() {

                    @Override
                    protected void updateItem(
                            Servicio servicio,
                            boolean empty) {

                        super.updateItem(
                                servicio,
                                empty);

                        if (empty
                                || servicio == null) {

                            setText(null);
                            setGraphic(null);
                            return;
                        }

                        Label icono =
                                etiqueta(
                                        iconoCategoria(
                                                servicio.getCategoria()),
                                        "service-category-icon");

                        VBox datos =
                                new VBox(
                                        5,
                                        etiqueta(
                                                servicio.getNombre(),
                                                "publication-name"),
                                        etiqueta(
                                                textoCategoria(
                                                        servicio),
                                                "muted"));

                        Label precio =
                                etiqueta(
                                        formatearPrecio(
                                                servicio.getPrecio()),
                                        "publication-price");

                        Label estado =
                                crearEstado(
                                        servicio.getEstado());

                        HBox fila =
                                new HBox(
                                        14,
                                        icono,
                                        datos,
                                        espacioHorizontal(),
                                        precio,
                                        estado);

                        fila.setAlignment(
                                Pos.CENTER_LEFT);

                        fila.setPadding(
                                new Insets(
                                        10,
                                        12,
                                        10,
                                        12));

                        setText(null);
                        setGraphic(fila);
                    }
                });

        listaServicios.setOnMouseClicked(
                event -> {

                    Servicio servicio =
                            listaServicios
                                    .getSelectionModel()
                                    .getSelectedItem();

                    if (servicio != null
                            && abrirDetalle != null) {

                        abrirDetalle.accept(
                                servicio.getIdServicio());
                    }
                });
    }

    private Label crearEstado(
            String estado) {

        Label etiqueta =
                new Label();

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

            return etiqueta;
        }

        if ("BORRADOR"
                .equalsIgnoreCase(estado)) {

            etiqueta.setText(
                    "● Borrador");

            etiqueta
                    .getStyleClass()
                    .add("status-draft");

            return etiqueta;
        }

        if ("RETIRADO"
                .equalsIgnoreCase(estado)) {

            etiqueta.setText(
                    "● Retirado");

            etiqueta
                    .getStyleClass()
                    .add("status-retired");

            return etiqueta;
        }

        etiqueta.setText(
                estado == null
                        ? "Sin estado"
                        : estado);

        return etiqueta;
    }

    private String textoCategoria(
            Servicio servicio) {

        String categoria =
                formatearCategoria(
                        servicio.getCategoria());

        String ciudad =
                servicio.getCiudadZona();

        if (ciudad == null
                || ciudad.isBlank()) {

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

        String texto =
                categoria
                        .toLowerCase()
                        .replace('_', ' ');

        return Character.toUpperCase(
                texto.charAt(0))
                + texto.substring(1);
    }

    private String iconoCategoria(
            String categoria) {

        if (categoria == null) {
            return "◇";
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
                return "◇";
        }
    }

    private String formatearPrecio(
            double precio) {

        DecimalFormatSymbols simbolos =
                new DecimalFormatSymbols(
                        new Locale(
                                "es",
                                "CO"));

        simbolos.setGroupingSeparator('.');

        DecimalFormat formato =
                new DecimalFormat(
                        "$#,##0",
                        simbolos);

        return formato.format(precio);
    }

    private void recargarServicios() {

        try {

            List<Servicio> servicios =
                    controller
                            .listarServiciosProveedor(
                                    idProveedor);

            listaServicios
                    .getItems()
                    .setAll(servicios);

        } catch (Exception e) {

            listaServicios
                    .getItems()
                    .clear();
        }
    }

    private VBox crearMenu() {

        VBox menu = new VBox(7);

        menu
                .getStyleClass()
                .add("sidebar");

        menu.setPrefWidth(250);
        menu.setMinWidth(250);

        VBox marca =
                new VBox(
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
            "⚙  Configuración"
        };

        for (String opcion : opciones) {

            Label item =
                    etiqueta(
                            opcion,
                            "menu-item");

            item.setMaxWidth(
                    Double.MAX_VALUE);

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

        Region espacio =
                new Region();

        VBox.setVgrow(
                espacio,
                Priority.ALWAYS);

        Label salir =
                etiqueta(
                        "Cerrar sesión",
                        "logout-link");

        if (cerrarSesion != null) {

            salir.setOnMouseClicked(
                    event -> cerrarSesion.run());
        }

        VBox perfil =
                new VBox(
                        4,
                        etiqueta(
                                nombreSesion(),
                                "profile-name"),
                        etiqueta(
                                rolSesion(),
                                "hint"),
                        salir);

        HBox cuenta =
                new HBox(
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

        HBox barra =
                new HBox(
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

        String rol =
                sesion
                        .getTipoUsuario()
                        .toLowerCase();

        return Character.toUpperCase(
                rol.charAt(0))
                + rol.substring(1);
    }

    private String inicialesSesion() {

        String[] partes =
                nombreSesion()
                        .trim()
                        .split("\\s+");

        StringBuilder iniciales =
                new StringBuilder();

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

    private Label etiqueta(
            String texto,
            String clase) {

        Label label =
                new Label(texto);

        label
                .getStyleClass()
                .add(clase);

        return label;
    }

    private Region espacioHorizontal() {

        Region espacio =
                new Region();

        HBox.setHgrow(
                espacio,
                Priority.ALWAYS);

        return espacio;
    }
}
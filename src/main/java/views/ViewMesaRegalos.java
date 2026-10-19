package views;

import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

/**
 * CU-20 - Interactuar con la mesa de regalos virtual (vista del invitado).
 *
 * Corresponde a mostrarMesaRegalos() de ViewInvitados en el diagrama de clases.
 * Flujo del diagrama VP-04: el invitado selecciona un regalo; si está disponible lo
 * reserva y queda asociado a él; si otro invitado ya lo reservó, se le avisa.
 *
 * Por ahora es solo la interfaz con datos de prueba (cargarDatosMock()); todavía no
 * guarda nada. Después se conectará con ControllerInvitados.
 */
public final class ViewMesaRegalos {

    private static final String CSS = "/styles/invitados.css";
    private static final String LOGO = "/images/logo-nexteventux.png";
    private static final double ANCHO_PANEL = 300;
    /** Ancho de ventana desde el cual el panel derecho va al lado de los regalos. */
    private static final double ANCHO_PANEL_LATERAL = 1300;
    private static final double ANCHO_TARJETA = 212;
    private static final Locale ES = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", ES);

    private static final String DISPONIBLE = "DISPONIBLE";
    private static final String RESERVADO = "RESERVADO";
    private static final String MIO = "MIO";
    /** Rangos del filtro de presupuesto (precio estimado del regalo, en pesos). */
    private static final String[] PRESUPUESTOS = {
            "Cualquier precio", "Hasta $30.000", "$30.000 a $60.000", "$60.000 a $100.000", "Más de $100.000"};

    /** Regalo de la mesa tal como lo ve el invitado (Regalo + Reserva_regalo). */
    public record RegaloMesa(int idRegalo, String nombre, String descripcion, BigDecimal precio, String estado) {
    }

    /** Evento cuya mesa de regalos puede ver el invitado. */
    public record EventoMesa(int idEvento, String nombre, LocalDateTime fecha, String lugar) {
        @Override
        public String toString() {
            return nombre;
        }
    }

    private final NumberFormat pesos = NumberFormat.getNumberInstance(ES);

    private List<RegaloMesa> regalos = List.of();
    private final TextField correo = new TextField();
    private final ComboBox<EventoMesa> selectorEvento = new ComboBox<>();
    private final TextField buscador = new TextField();
    private final ComboBox<String> filtro = new ComboBox<>();
    private final ComboBox<String> presupuesto = new ComboBox<>();
    private final FlowPane tarjetas = new FlowPane(16, 16);
    private final VBox misReservas = new VBox(10);
    private final Label mensaje = new Label();

    private final Label disponiblesValor = new Label("0");
    private final Label reservadosValor = new Label("0");
    private final Label misReservasValor = new Label("0");
    private final Label detalleEvento = new Label();

    public ViewMesaRegalos() {
        pesos.setMaximumFractionDigits(0);

        correo.setId("correoInvitado");
        correo.setPromptText("Ej. camila@email.com");
        correo.setText("camila@email.com");
        correo.setPrefWidth(260);
        correo.setMinWidth(180);
        correo.setOnAction(e -> consultar());

        selectorEvento.setId("selectorEvento");
        selectorEvento.setPromptText("Selecciona un evento");
        selectorEvento.setPrefWidth(240);
        selectorEvento.setMinWidth(180);
        selectorEvento.valueProperty().addListener((obs, anterior, actual) -> mostrarEvento(actual));

        buscador.setId("buscarRegalo");
        buscador.setPromptText("Buscar regalo...");
        buscador.setPrefWidth(190);
        buscador.setMinWidth(150);
        buscador.textProperty().addListener((obs, anterior, actual) -> pintarRegalos());

        filtro.setId("filtroRegalos");
        filtro.getItems().addAll("Todos", "Disponibles", "Reservados", "Mis reservas");
        filtro.getSelectionModel().selectFirst();
        filtro.setPrefWidth(160);
        filtro.setMinWidth(150);
        filtro.valueProperty().addListener((obs, anterior, actual) -> pintarRegalos());

        presupuesto.setId("filtroPresupuesto");
        presupuesto.getItems().addAll(PRESUPUESTOS);
        presupuesto.getSelectionModel().selectFirst();
        presupuesto.setPrefWidth(200);
        presupuesto.setMinWidth(190);
        presupuesto.valueProperty().addListener((obs, anterior, actual) -> pintarRegalos());

        mensaje.setId("mensaje");
        mensaje.setWrapText(true);
        mensaje.setVisible(false);
        mensaje.setManaged(false);

        tarjetas.setId("tarjetasRegalos");
        tarjetas.setPrefWrapLength(ANCHO_TARJETA * 3 + 32);
        detalleEvento.getStyleClass().add("muted");
        detalleEvento.setWrapText(true);
    }

    // ------------------------------------------------------------------
    // Datos de prueba (basados en seed.sql). Se reemplazan por el Controller.
    // ------------------------------------------------------------------
    private List<EventoMesa> eventosMock() {
        return List.of(new EventoMesa(2, "Baby Shower Valentina", LocalDateTime.of(2026, 12, 5, 14, 0), "Jardín Los Pinos"));
    }

    private List<RegaloMesa> regalosMock() {
        return List.of(
                new RegaloMesa(1, "Pañitos para bebé", "Paquete de pañitos húmedos", new BigDecimal("25000"), RESERVADO),
                new RegaloMesa(2, "Kit de biberones", "Kit de biberones para recién nacido", new BigDecimal("80000"), MIO),
                new RegaloMesa(3, "Cobija para bebé", "Cobija suave para recién nacido", new BigDecimal("60000"), DISPONIBLE),
                new RegaloMesa(4, "Ropa de bebé", "Conjunto de ropa para recién nacido", new BigDecimal("50000"), DISPONIBLE));
    }

    // ------------------------------------------------------------------
    // Construcción de la pantalla
    // ------------------------------------------------------------------
    public Parent crearPantalla() {
        BorderPane raiz = new BorderPane();
        raiz.getStyleClass().add("app");
        URL css = getClass().getResource(CSS);
        if (css != null) raiz.getStylesheets().add(css.toExternalForm());

        VBox sidebar = crearMenu();
        raiz.setLeft(sidebar);

        BorderPane principal = new BorderPane();
        principal.setTop(crearBarraSuperior());

        VBox contenido = new VBox(25);
        contenido.getStyleClass().add("content");
        contenido.setMaxWidth(1320);

        VBox titulo = new VBox(8, etiqueta("Mesa de regalos", "page-title"),
                etiqueta("Elige el regalo que quieres obsequiar en el evento.", "muted"));

        VBox regalosCard = crearTarjetaRegalos();
        regalosCard.setMinWidth(0);
        GridPane.setValignment(regalosCard, VPos.TOP);
        VBox tarjetaReservas = crearTarjetaMisReservas();
        VBox tarjetaInfo = informacion("Información importante",
                "Al reservar un regalo queda registrado a tu nombre para que el organizador lo sepa. "
                        + "Si cambias de idea, puedes cancelar tu reserva.");
        tarjetaInfo.getStyleClass().add("highlight");
        Region[] panel = {tarjetaReservas, tarjetaInfo};

        VBox panelVertical = new VBox(20);
        panelVertical.setPrefWidth(ANCHO_PANEL);
        panelVertical.setMinWidth(ANCHO_PANEL);
        HBox panelHorizontal = new HBox(20);

        GridPane columnas = new GridPane();
        columnas.setHgap(20);
        columnas.setVgap(25);
        ColumnConstraints izquierda = new ColumnConstraints();
        izquierda.setHgrow(Priority.ALWAYS);
        izquierda.setMinWidth(0);
        ColumnConstraints derecha = new ColumnConstraints(ANCHO_PANEL);

        GridPane estadisticas = crearEstadisticas();
        contenido.getChildren().addAll(titulo, mensaje, crearTarjetaBusqueda(), estadisticas, columnas);

        StackPane centrado = new StackPane(contenido);
        centrado.setAlignment(Pos.TOP_CENTER);
        ScrollPane scroll = new ScrollPane(centrado);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("content-scroll");
        principal.setCenter(scroll);
        raiz.setCenter(principal);

        // Mismo comportamiento que las otras pantallas: con ventana angosta el panel baja.
        boolean[] lateralActual = {false};
        boolean[] inicializado = {false};
        Runnable organizar = () -> {
            double ancho = raiz.getWidth() <= 0 ? 1440 : raiz.getWidth();
            boolean lateral = ancho >= ANCHO_PANEL_LATERAL;
            if (inicializado[0] && lateral == lateralActual[0]) return;
            inicializado[0] = true;
            lateralActual[0] = lateral;
            panelVertical.getChildren().clear();
            panelHorizontal.getChildren().clear();
            columnas.getChildren().clear();
            columnas.getColumnConstraints().setAll(izquierda);
            columnas.add(regalosCard, 0, 0);
            GridPane.setHgrow(regalosCard, Priority.ALWAYS);
            if (lateral) {
                columnas.getColumnConstraints().add(derecha);
                for (Region r : panel) {
                    r.setPrefWidth(Region.USE_COMPUTED_SIZE);
                    HBox.setHgrow(r, Priority.NEVER);
                }
                panelVertical.getChildren().addAll(panel);
                columnas.add(panelVertical, 1, 0);
            } else {
                for (Region r : panel) {
                    r.setPrefWidth(0);
                    r.setMaxWidth(Double.MAX_VALUE);
                    HBox.setHgrow(r, Priority.ALWAYS);
                }
                panelHorizontal.getChildren().addAll(panel);
                columnas.add(panelHorizontal, 0, 1);
                GridPane.setHgrow(panelHorizontal, Priority.ALWAYS);
            }
        };
        organizar.run();
        raiz.widthProperty().addListener((obs, anterior, actual) -> {
            organizar.run();
            sidebar.setVisible(actual.doubleValue() >= 1000);
            sidebar.setManaged(sidebar.isVisible());
        });

        consultar();
        return raiz;
    }

    private VBox crearMenu() {
        VBox menu = new VBox(7);
        menu.getStyleClass().add("sidebar");
        menu.setPrefWidth(250);
        menu.setMinWidth(250);
        VBox marca = new VBox(10, crearLogo(), etiqueta("GESTIÓN DE EVENTOS", "brand-caption"));
        marca.setAlignment(Pos.CENTER);
        VBox.setMargin(marca, new Insets(0, 0, 38, 0));
        menu.getChildren().addAll(marca, etiqueta("MENÚ DEL INVITADO", "menu-title"));
        String[] opciones = {"⌂  Inicio", "✉  Mis invitaciones", "♡  Mesa de regalos", "⚙  Mi perfil"};
        for (String opcion : opciones) {
            Label item = etiqueta(opcion, "menu-item");
            item.setMaxWidth(Double.MAX_VALUE);
            if (opcion.contains("Mesa de regalos")) item.getStyleClass().add("active");
            menu.getChildren().add(item);
        }
        Region espacio = new Region();
        VBox.setVgrow(espacio, Priority.ALWAYS);
        VBox perfil = new VBox(4, etiqueta("Invitado", "profile-name"), etiqueta("Mi cuenta", "hint"));
        HBox cuenta = new HBox(12, etiqueta("IN", "avatar"), perfil);
        cuenta.setAlignment(Pos.CENTER_LEFT);
        cuenta.getStyleClass().add("sidebar-bottom");
        menu.getChildren().addAll(espacio, cuenta);
        return menu;
    }

    private Node crearLogo() {
        URL imagen = getClass().getResource(LOGO);
        if (imagen == null) return new Region();
        ImageView logo = new ImageView(new Image(imagen.toExternalForm()));
        logo.setFitWidth(190);
        logo.setPreserveRatio(true);
        logo.setSmooth(true);
        return logo;
    }

    private HBox crearBarraSuperior() {
        HBox barra = new HBox(18, etiqueta("Inicio /", "breadcrumb"),
                etiqueta("Mesa de regalos", "breadcrumb-current"), espacioHorizontal(),
                etiqueta("♧", "notification"), etiqueta("Invitado", "user-badge"));
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.getStyleClass().add("topbar");
        return barra;
    }

    private VBox crearTarjetaBusqueda() {
        VBox textos = new VBox(6, etiqueta("Mesa de regalos del evento", "card-title"), detalleEvento);
        textos.setMinWidth(0);
        HBox.setHgrow(textos, Priority.ALWAYS);

        Button consultar = new Button("Consultar");
        consultar.setId("consultarMesa");
        consultar.getStyleClass().add("btn-primary");
        consultar.setMinWidth(Region.USE_PREF_SIZE);
        consultar.setOnAction(e -> consultar());

        HBox cabecera = new HBox(16, etiqueta("♡", "heading-icon"), textos);
        cabecera.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(correo, Priority.ALWAYS);
        correo.setMaxWidth(Double.MAX_VALUE);
        VBox campoCorreo = new VBox(8, etiqueta("Tu correo", "field-label"), correo);
        HBox.setHgrow(campoCorreo, Priority.ALWAYS);
        VBox campoEvento = new VBox(8, etiqueta("Evento", "field-label"), selectorEvento);
        HBox fila = new HBox(14, campoCorreo, campoEvento, consultar);
        fila.setAlignment(Pos.BOTTOM_LEFT);
        VBox tarjeta = new VBox(20, cabecera, fila);
        tarjeta.getStyleClass().add("card");
        return tarjeta;
    }

    private GridPane crearEstadisticas() {
        GridPane grid = new GridPane();
        grid.setHgap(20);
        for (int i = 0; i < 3; i++) {
            ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(100.0 / 3);
            c.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(c);
        }
        grid.add(estadistica("Disponibles", disponiblesValor, "Regalos que puedes elegir", "♡"), 0, 0);
        grid.add(estadistica("Reservados", reservadosValor, "Ya elegidos por otros invitados", "✓"), 1, 0);
        grid.add(estadistica("Mis reservas", misReservasValor, "Regalos a tu nombre", "★"), 2, 0);
        return grid;
    }

    private VBox estadistica(String titulo, Label valor, String descripcion, String icono) {
        valor.getStyleClass().add("stat-value");
        Label nombre = etiqueta(titulo, "field-label");
        nombre.setMinWidth(0);
        HBox encabezado = new HBox(10, nombre, espacioHorizontal(), etiqueta(icono, "heading-icon"));
        encabezado.setAlignment(Pos.CENTER_LEFT);
        Label detalle = etiqueta(descripcion, "hint");
        detalle.setWrapText(true);
        VBox tarjeta = new VBox(8, encabezado, valor, detalle);
        tarjeta.getStyleClass().add("info-card");
        tarjeta.setMaxWidth(Double.MAX_VALUE);
        tarjeta.setMinWidth(0);
        return tarjeta;
    }

    private VBox crearTarjetaRegalos() {
        Label ayuda = etiqueta("Selecciona el regalo que quieres obsequiar.", "muted");
        ayuda.setWrapText(true);
        VBox textos = new VBox(6, etiqueta("Regalos", "card-title"), ayuda);
        textos.setMinWidth(0);
        HBox.setHgrow(buscador, Priority.ALWAYS);
        buscador.setMaxWidth(Double.MAX_VALUE);
        HBox filtros = new HBox(12, buscador, filtro, presupuesto);
        filtros.setAlignment(Pos.CENTER_LEFT);
        VBox tarjeta = new VBox(18, textos, filtros, tarjetas);
        tarjeta.getStyleClass().add("card");
        tarjeta.setMaxHeight(Region.USE_PREF_SIZE);
        return tarjeta;
    }

    private VBox crearTarjetaMisReservas() {
        VBox tarjeta = new VBox(16, etiqueta("Mis reservas", "card-title"), misReservas);
        tarjeta.getStyleClass().add("info-card");
        return tarjeta;
    }

    // ------------------------------------------------------------------
    // Comportamiento (solo interfaz)
    // ------------------------------------------------------------------
    private void consultar() {
        String texto = correo.getText() == null ? "" : correo.getText().trim().toLowerCase(Locale.ROOT);
        List<EventoMesa> eventos = "camila@email.com".equals(texto) ? eventosMock() : List.of();
        selectorEvento.getItems().setAll(eventos);
        if (eventos.isEmpty()) {
            mostrarMensaje("No hay mesas de regalos para \"" + correo.getText().trim() + "\".", false);
            selectorEvento.setValue(null);
            mostrarEvento(null);
        } else {
            ocultarMensaje();
            selectorEvento.setValue(eventos.get(0));
        }
    }

    private void mostrarEvento(EventoMesa evento) {
        if (evento == null) {
            regalos = List.of();
            detalleEvento.setText("Escribe tu correo para ver las mesas de regalos de tus eventos.");
        } else {
            regalos = regalosMock();
            detalleEvento.setText(evento.fecha().format(FECHA) + " · " + evento.lugar());
        }
        pintarRegalos();
    }

    private void pintarRegalos() {
        String texto = buscador.getText() == null ? "" : buscador.getText().trim().toLowerCase(ES);
        String vista = filtro.getValue() == null ? "Todos" : filtro.getValue();
        tarjetas.getChildren().clear();
        int disponibles = 0, reservados = 0, mios = 0;
        for (RegaloMesa regalo : regalos) {
            switch (regalo.estado()) {
                case DISPONIBLE -> disponibles++;
                case MIO -> mios++;
                default -> reservados++;
            }
            boolean coincideTexto = texto.isEmpty() || (regalo.nombre() + " " + regalo.descripcion()).toLowerCase(ES).contains(texto);
            boolean coincideFiltro = switch (vista) {
                case "Disponibles" -> regalo.estado().equals(DISPONIBLE);
                case "Reservados" -> regalo.estado().equals(RESERVADO);
                case "Mis reservas" -> regalo.estado().equals(MIO);
                default -> true;
            };
            if (coincideTexto && coincideFiltro && dentroDelPresupuesto(regalo.precio()))
                tarjetas.getChildren().add(tarjetaRegalo(regalo));
        }
        if (tarjetas.getChildren().isEmpty()) {
            Label vacio = etiqueta(regalos.isEmpty() ? "No hay regalos para mostrar." : "Ningún regalo coincide con la búsqueda.", "muted");
            tarjetas.getChildren().add(vacio);
        }
        disponiblesValor.setText(String.valueOf(disponibles));
        reservadosValor.setText(String.valueOf(reservados));
        misReservasValor.setText(String.valueOf(mios));
        pintarMisReservas();
    }

    private boolean dentroDelPresupuesto(BigDecimal precio) {
        if (precio == null) return true;
        double valor = precio.doubleValue();
        String rango = presupuesto.getValue() == null ? PRESUPUESTOS[0] : presupuesto.getValue();
        if (rango.equals(PRESUPUESTOS[1])) return valor <= 30000;
        if (rango.equals(PRESUPUESTOS[2])) return valor > 30000 && valor <= 60000;
        if (rango.equals(PRESUPUESTOS[3])) return valor > 60000 && valor <= 100000;
        if (rango.equals(PRESUPUESTOS[4])) return valor > 100000;
        return true;
    }

    private Node tarjetaRegalo(RegaloMesa regalo) {
        Label inicial = new Label(regalo.nombre().substring(0, 1).toUpperCase(ES));
        inicial.getStyleClass().add("gift-thumb");
        inicial.setMaxWidth(Double.MAX_VALUE);
        Label estado = pill(regalo.estado());
        StackPane imagen = new StackPane(inicial, estado);
        StackPane.setAlignment(estado, Pos.TOP_RIGHT);
        StackPane.setMargin(estado, new Insets(8));
        Label nombre = etiqueta(regalo.nombre(), "summary-value");
        nombre.setStyle("-fx-font-size: 14px;");
        nombre.setWrapText(true);
        Label descripcion = etiqueta(regalo.descripcion(), "hint");
        descripcion.setWrapText(true);
        descripcion.setMinHeight(32);
        Label precio = etiqueta("$" + pesos.format(regalo.precio()), "gift-price");

        Button accion = new Button();
        accion.setMaxWidth(Double.MAX_VALUE);
        switch (regalo.estado()) {
            case DISPONIBLE -> {
                accion.setText("Reservar");
                accion.getStyleClass().add("btn-primary");
                accion.setOnAction(e -> aviso("Reservar regalo", "Reservarás \"" + regalo.nombre() + "\"."));
            }
            case MIO -> {
                accion.setText("Cancelar reserva");
                accion.getStyleClass().add("btn-secondary");
                accion.setOnAction(e -> aviso("Cancelar reserva", "Cancelarás tu reserva de \"" + regalo.nombre() + "\"."));
            }
            default -> {
                accion.setText("No disponible");
                accion.getStyleClass().add("btn-secondary");
                accion.setDisable(true);
            }
        }
        VBox tarjeta = new VBox(10, imagen, nombre, descripcion, precio, accion);
        tarjeta.getStyleClass().add("gift-card");
        if (regalo.estado().equals(MIO)) tarjeta.getStyleClass().add("mine");
        tarjeta.setPrefWidth(ANCHO_TARJETA);
        tarjeta.setMaxWidth(ANCHO_TARJETA);
        return tarjeta;
    }

    private void pintarMisReservas() {
        misReservas.getChildren().clear();
        for (RegaloMesa regalo : regalos) {
            if (!regalo.estado().equals(MIO)) continue;
            Label nombre = etiqueta(regalo.nombre(), "summary-value");
            nombre.setWrapText(true);
            VBox textos = new VBox(4, nombre, etiqueta("$" + pesos.format(regalo.precio()), "hint"));
            textos.setMinWidth(0);
            HBox.setHgrow(textos, Priority.ALWAYS);
            Button cancelar = new Button("Cancelar");
            cancelar.getStyleClass().addAll("btn-danger", "btn-small");
            cancelar.setMinWidth(Region.USE_PREF_SIZE);
            cancelar.setOnAction(e -> aviso("Cancelar reserva", "Cancelarás tu reserva de \"" + regalo.nombre() + "\"."));
            HBox item = new HBox(10, textos, cancelar);
            item.setAlignment(Pos.CENTER_LEFT);
            item.getStyleClass().add("invitation-item");
            misReservas.getChildren().add(item);
        }
        if (misReservas.getChildren().isEmpty()) {
            Label vacio = etiqueta("Todavía no has reservado regalos.", "muted");
            vacio.setWrapText(true);
            misReservas.getChildren().add(vacio);
        }
    }

    private void aviso(String titulo, String texto) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle(titulo);
        alerta.setHeaderText(titulo);
        alerta.setContentText(texto + "\n\nEsta acción se guardará cuando la pantalla se conecte con la base de datos.");
        URL css = getClass().getResource(CSS);
        if (css != null) alerta.getDialogPane().getStylesheets().add(css.toExternalForm());
        if (correo.getScene() != null) alerta.initOwner(correo.getScene().getWindow());
        alerta.getDialogPane().lookupButton(ButtonType.OK).getStyleClass().add("btn-primary");
        alerta.showAndWait();
    }

    private Label pill(String estado) {
        String texto = switch (estado) {
            case DISPONIBLE -> "DISPONIBLE";
            case MIO -> "TU RESERVA";
            default -> "RESERVADO";
        };
        String clase = switch (estado) {
            case DISPONIBLE -> "status-confirmada";
            case MIO -> "status-enviada";
            default -> "status-revocada";
        };
        Label pill = new Label(texto);
        pill.getStyleClass().setAll("label", "status", clase);
        pill.setMinWidth(Region.USE_PREF_SIZE);
        return pill;
    }

    private void mostrarMensaje(String texto, boolean exito) {
        mensaje.setText(texto);
        mensaje.getStyleClass().setAll("label", "message", exito ? "success" : "error");
        mensaje.setVisible(true);
        mensaje.setManaged(true);
    }

    private void ocultarMensaje() {
        mensaje.setVisible(false);
        mensaje.setManaged(false);
    }

    // ------------------------------------------------------------------
    // Utilidades (mismas que las otras vistas)
    // ------------------------------------------------------------------
    private Label etiqueta(String texto, String estilo) {
        Label label = new Label(texto);
        label.getStyleClass().add(estilo);
        return label;
    }

    private Region espacioHorizontal() {
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        return espacio;
    }

    private VBox informacion(String titulo, String texto) {
        Label descripcion = etiqueta(texto, "muted");
        descripcion.setWrapText(true);
        Label encabezado = etiqueta(titulo, "card-title");
        encabezado.setWrapText(true);
        VBox tarjeta = new VBox(14, encabezado, descripcion);
        tarjeta.getStyleClass().add("info-card");
        return tarjeta;
    }
}

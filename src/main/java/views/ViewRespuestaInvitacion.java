package views;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
 * CU-16 - Gestionar respuesta a la invitación (vista del invitado).
 *
 * Corresponde a mostrarRespuesta() de ViewInvitados en el diagrama de clases.
 * Flujo del diagrama VP-04: el invitado consulta su invitación y responde si asistirá.
 *
 * Por ahora es solo la interfaz con datos de prueba (cargarDatosMock()); todavía no
 * guarda nada. Después se conectará con ControllerInvitados.gestionarRespuestaInvitacion().
 */
public final class ViewRespuestaInvitacion {

    private static final String CSS = "/styles/invitados.css";
    private static final String LOGO = "/images/logo-nexteventux.png";
    private static final double ANCHO_PANEL = 300;
    /** Ancho de ventana desde el cual el panel derecho va al lado del detalle. */
    private static final double ANCHO_PANEL_LATERAL = 1300;
    private static final Locale ES = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter FECHA_LARGA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", ES);
    private static final DateTimeFormatter FECHA_CORTA = DateTimeFormatter.ofPattern("d MMM yyyy", ES);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("h:mm a", ES);

    /** Invitación tal como la ve el invitado (datos de Invitacion, Invitado, Evento y Lugar). */
    public record InvitacionRecibida(int idInvitacion, int idInvitado, String evento, String tipoEvento,
                                     LocalDateTime inicio, LocalDateTime fin, String lugar, String direccion,
                                     String mensaje, String estado, int ocupados, int capacidad) {
    }

    private final ObservableList<InvitacionRecibida> invitaciones = FXCollections.observableArrayList();
    private final ToggleGroup respuesta = new ToggleGroup();

    private final TextField correo = new TextField();
    private final VBox listaInvitaciones = new VBox(10);
    private final Label cantidad = new Label();
    private final Label mensaje = new Label();

    private final Label detalleEvento = new Label();
    private final Label detalleTipo = new Label();
    private final Label detalleFecha = new Label();
    private final Label detalleHora = new Label();
    private final Label detalleLugar = new Label();
    private final Label detalleDireccion = new Label();
    private final Label detalleMensaje = new Label();
    private final Label detalleEstado = new Label();
    private final Label resumenCupos = new Label();
    private final ProgressBar barraCupos = new ProgressBar(0);

    private InvitacionRecibida seleccionada;

    public ViewRespuestaInvitacion() {
        correo.setId("correoInvitado");
        correo.setPromptText("Ej. sofia@email.com");
        correo.setText("sofia@email.com");
        correo.setOnAction(e -> consultar());
        HBox.setHgrow(correo, Priority.ALWAYS);

        mensaje.setId("mensaje");
        mensaje.setWrapText(true);
        mensaje.setVisible(false);
        mensaje.setManaged(false);

        detalleEvento.getStyleClass().add("page-title");
        detalleEvento.setStyle("-fx-font-size: 24px;");
        detalleEvento.setWrapText(true);
        detalleMensaje.getStyleClass().add("quote");
        detalleMensaje.setWrapText(true);
        detalleMensaje.setMaxWidth(Double.MAX_VALUE);
        barraCupos.setMaxWidth(Double.MAX_VALUE);
        barraCupos.getStyleClass().add("cupos");
        resumenCupos.getStyleClass().add("hint");

        cargarDatosMock();
    }

    // ------------------------------------------------------------------
    // Datos de prueba (basados en seed.sql). Se reemplazan por el Controller.
    // ------------------------------------------------------------------
    private void cargarDatosMock() {
        invitaciones.setAll(
                new InvitacionRecibida(1, 1, "Boda Laura y Daniel", "Boda",
                        LocalDateTime.of(2026, 11, 15, 15, 0), LocalDateTime.of(2026, 11, 15, 23, 0),
                        "Salón Primavera", "Calle 100 #15-20",
                        "Te invitamos a celebrar con nosotros este día especial.", "PENDIENTE", 3, 150),
                new InvitacionRecibida(3, 5, "Baby Shower Valentina", "Baby shower",
                        LocalDateTime.of(2026, 12, 5, 14, 0), LocalDateTime.of(2026, 12, 5, 19, 0),
                        "Jardín Los Pinos", "Carrera 7 #120-15",
                        "Acompáñanos en el baby shower de Valentina.", "CONFIRMADO", 4, 250));
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

        VBox titulo = new VBox(8, etiqueta("Mis invitaciones", "page-title"),
                etiqueta("Consulta tus invitaciones y confirma si asistirás al evento.", "muted"));

        VBox detalle = new VBox(25, crearTarjetaDetalle(), crearTarjetaRespuesta());
        detalle.setMinWidth(0);
        VBox tarjetaLista = crearTarjetaLista();
        VBox tarjetaInfo = informacion("Información importante",
                "Si confirmas tu asistencia se reservará tu cupo en el evento. Si indicas que no asistirás, "
                        + "tu cupo se liberará y el organizador recibirá el aviso.");
        tarjetaInfo.getStyleClass().add("highlight");
        Region[] panel = {tarjetaLista, tarjetaInfo};

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
        GridPane.setValignment(detalle, VPos.TOP);

        contenido.getChildren().addAll(titulo, mensaje, crearTarjetaBusqueda(), columnas);

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
            if (lateral) {
                columnas.getColumnConstraints().add(derecha);
                for (Region r : panel) {
                    r.setPrefWidth(Region.USE_COMPUTED_SIZE);
                    HBox.setHgrow(r, Priority.NEVER);
                }
                panelVertical.getChildren().addAll(panel);
                columnas.add(detalle, 0, 0);
                columnas.add(panelVertical, 1, 0);
            } else {
                for (Region r : panel) {
                    r.setPrefWidth(0);
                    r.setMaxWidth(Double.MAX_VALUE);
                    HBox.setHgrow(r, Priority.ALWAYS);
                }
                panelHorizontal.getChildren().addAll(panel);
                columnas.add(panelHorizontal, 0, 0);
                columnas.add(detalle, 0, 1);
            }
            GridPane.setHgrow(detalle, Priority.ALWAYS);
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
            if (opcion.contains("Mis invitaciones")) item.getStyleClass().add("active");
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
                etiqueta("Mis invitaciones", "breadcrumb-current"), espacioHorizontal(),
                etiqueta("♧", "notification"), etiqueta("Invitado", "user-badge"));
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.getStyleClass().add("topbar");
        return barra;
    }

    private VBox crearTarjetaBusqueda() {
        Label ayuda = etiqueta("Escribe el correo con el que recibiste la invitación.", "muted");
        ayuda.setWrapText(true);
        VBox textos = new VBox(6, etiqueta("Buscar mis invitaciones", "card-title"), ayuda);
        textos.setMinWidth(0);
        HBox.setHgrow(textos, Priority.ALWAYS);

        correo.setPrefWidth(300);
        correo.setMinWidth(200);
        Button consultar = new Button("Consultar");
        consultar.setId("consultarInvitaciones");
        consultar.getStyleClass().add("btn-primary");
        consultar.setMinWidth(Region.USE_PREF_SIZE);
        consultar.setOnAction(e -> consultar());

        HBox fila = new HBox(16, etiqueta("✉", "heading-icon"), textos, correo, consultar);
        fila.setAlignment(Pos.CENTER_LEFT);
        VBox tarjeta = new VBox(fila);
        tarjeta.getStyleClass().add("card");
        return tarjeta;
    }

    private VBox crearTarjetaLista() {
        cantidad.getStyleClass().add("hint");
        VBox encabezado = new VBox(4, etiqueta("Tus invitaciones", "card-title"), cantidad);
        VBox tarjeta = new VBox(16, encabezado, listaInvitaciones);
        tarjeta.getStyleClass().add("info-card");
        return tarjeta;
    }

    private VBox crearTarjetaDetalle() {
        detalleEstado.setMinWidth(Region.USE_PREF_SIZE);
        HBox cabecera = new HBox(16, new VBox(6, detalleEvento, detalleTipo), espacioHorizontal(), detalleEstado);
        cabecera.setAlignment(Pos.TOP_LEFT);
        detalleTipo.getStyleClass().add("muted");

        GridPane datos = new GridPane();
        datos.setHgap(23);
        datos.setVgap(18);
        for (int i = 0; i < 2; i++) {
            ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(50);
            datos.getColumnConstraints().add(c);
        }
        datos.add(dato("▦", "Fecha", detalleFecha), 0, 0);
        datos.add(dato("◷", "Hora", detalleHora), 1, 0);
        datos.add(dato("⌂", "Lugar", detalleLugar), 0, 1);
        datos.add(dato("➤", "Dirección", detalleDireccion), 1, 1);

        VBox tarjeta = new VBox(22, cabecera, new Separator(), datos,
                etiqueta("Mensaje del organizador", "section-title"), detalleMensaje,
                new VBox(8, resumenCupos, barraCupos));
        tarjeta.getStyleClass().add("card");
        return tarjeta;
    }

    private HBox dato(String icono, String titulo, Label valor) {
        valor.getStyleClass().add("summary-value");
        valor.setStyle("-fx-font-size: 14px;");
        valor.setWrapText(true);
        VBox textos = new VBox(4, etiqueta(titulo, "hint"), valor);
        textos.setMinWidth(0);
        HBox fila = new HBox(12, etiqueta(icono, "heading-icon"), textos);
        fila.setAlignment(Pos.CENTER_LEFT);
        return fila;
    }

    private VBox crearTarjetaRespuesta() {
        HBox titulo = new HBox(13, etiqueta("✓", "heading-icon"), etiqueta("¿Asistirás al evento?", "card-title"));
        titulo.setAlignment(Pos.CENTER_LEFT);

        ToggleButton si = opcion("Sí, asistiré", "Confirmo mi asistencia y reservo mi cupo.", "ASISTIRE");
        ToggleButton no = opcion("No podré asistir", "Rechazo la invitación y libero mi cupo.", "NO_ASISTIRE");
        si.setId("respuestaSi");
        no.setId("respuestaNo");
        GridPane opciones = new GridPane();
        opciones.setHgap(16);
        for (int i = 0; i < 2; i++) {
            ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(50);
            opciones.getColumnConstraints().add(c);
        }
        opciones.add(si, 0, 0);
        opciones.add(no, 1, 0);

        Label notaAcompanantes = etiqueta("Después de confirmar podrás registrar a tus acompañantes.", "hint");
        notaAcompanantes.setWrapText(true);

        Button cancelar = new Button("Cancelar");
        cancelar.setId("cancelarRespuesta");
        cancelar.getStyleClass().add("btn-secondary");
        cancelar.setOnAction(e -> respuesta.selectToggle(null));
        Button enviar = new Button("Enviar respuesta");
        enviar.setId("enviarRespuesta");
        enviar.getStyleClass().add("btn-primary");
        enviar.disableProperty().bind(respuesta.selectedToggleProperty().isNull());
        enviar.setOnAction(e -> enviarRespuesta());
        HBox acciones = new HBox(12, notaAcompanantes, espacioHorizontal(), cancelar, enviar);
        acciones.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(notaAcompanantes, Priority.SOMETIMES);

        VBox tarjeta = new VBox(22, titulo,
                etiqueta("Elige una opción y envía tu respuesta al organizador.", "muted"),
                opciones, new Separator(), acciones);
        tarjeta.getStyleClass().add("card");
        return tarjeta;
    }

    private ToggleButton opcion(String titulo, String descripcion, String valor) {
        Label t = etiqueta(titulo, "option-title");
        Label d = etiqueta(descripcion, "hint");
        d.setWrapText(true);
        VBox contenido = new VBox(6, t, d);
        ToggleButton boton = new ToggleButton();
        boton.setGraphic(contenido);
        boton.setUserData(valor);
        boton.setToggleGroup(respuesta);
        boton.getStyleClass().add("option-card");
        boton.setMaxWidth(Double.MAX_VALUE);
        // Alto fijo: evita que el texto ajustado haga crecer la tarjeta.
        boton.setMinHeight(84);
        boton.setPrefHeight(84);
        boton.setMaxHeight(84);
        contenido.setMinWidth(0);
        boton.setAlignment(Pos.CENTER_LEFT);
        return boton;
    }

    // ------------------------------------------------------------------
    // Comportamiento (solo interfaz)
    // ------------------------------------------------------------------
    private void consultar() {
        String texto = correo.getText() == null ? "" : correo.getText().trim().toLowerCase(Locale.ROOT);
        List<InvitacionRecibida> encontradas = "sofia@email.com".equals(texto) ? invitaciones : List.of();
        listaInvitaciones.getChildren().clear();
        for (InvitacionRecibida inv : encontradas) listaInvitaciones.getChildren().add(itemInvitacion(inv));
        int n = encontradas.size();
        cantidad.setText(n + (n == 1 ? " invitación encontrada" : " invitaciones encontradas"));
        if (n == 0) {
            Label vacio = etiqueta("No encontramos invitaciones para ese correo.", "muted");
            vacio.setWrapText(true);
            listaInvitaciones.getChildren().add(vacio);
            mostrarMensaje("No hay invitaciones asociadas a \"" + correo.getText().trim() + "\".", false);
            seleccionar(null);
        } else {
            ocultarMensaje();
            seleccionar(encontradas.get(0));
        }
    }

    private Node itemInvitacion(InvitacionRecibida inv) {
        Label nombre = etiqueta(inv.evento(), "summary-value");
        nombre.setStyle("-fx-font-size: 13px;");
        nombre.setWrapText(true);
        Label fecha = etiqueta(inv.inicio().format(FECHA_CORTA) + " · " + inv.lugar(), "hint");
        fecha.setWrapText(true);
        Label estado = pill(inv.estado());
        VBox item = new VBox(6, nombre, fecha, estado);
        item.setMinWidth(0);
        item.getStyleClass().add("invitation-item");
        item.setUserData(inv);
        item.setOnMouseClicked(e -> seleccionar(inv));
        return item;
    }

    private void seleccionar(InvitacionRecibida inv) {
        seleccionada = inv;
        respuesta.selectToggle(null);
        for (Node n : listaInvitaciones.getChildren()) {
            n.getStyleClass().remove("selected");
            if (inv != null && n.getUserData() == inv) n.getStyleClass().add("selected");
        }
        if (inv == null) {
            detalleEvento.setText("Sin invitación seleccionada");
            for (Label l : new Label[]{detalleTipo, detalleFecha, detalleHora, detalleLugar, detalleDireccion, detalleMensaje})
                l.setText("-");
            detalleEstado.setText("");
            detalleEstado.setVisible(false);
            resumenCupos.setText("");
            barraCupos.setProgress(0);
            return;
        }
        detalleEvento.setText(inv.evento());
        detalleTipo.setText(inv.tipoEvento() + " · Invitación #" + inv.idInvitacion());
        String fecha = inv.inicio().format(FECHA_LARGA);
        detalleFecha.setText(Character.toUpperCase(fecha.charAt(0)) + fecha.substring(1));
        detalleHora.setText(inv.inicio().format(HORA) + " – " + inv.fin().format(HORA));
        detalleLugar.setText(inv.lugar());
        detalleDireccion.setText(inv.direccion());
        detalleMensaje.setText("“" + inv.mensaje() + "”");
        detalleEstado.setText(textoEstado(inv.estado()));
        detalleEstado.getStyleClass().setAll("label", "status", claseEstado(inv.estado()));
        detalleEstado.setVisible(true);
        int libres = inv.capacidad() - inv.ocupados();
        resumenCupos.setText("Cupos del lugar: " + inv.ocupados() + " ocupados de " + inv.capacidad()
                + " (" + libres + " disponibles)");
        barraCupos.setProgress(inv.capacidad() == 0 ? 0 : (double) inv.ocupados() / inv.capacidad());
    }

    private void enviarRespuesta() {
        if (seleccionada == null || respuesta.getSelectedToggle() == null) return;
        boolean asiste = "ASISTIRE".equals(respuesta.getSelectedToggle().getUserData());
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle("Respuesta a la invitación");
        alerta.setHeaderText(asiste ? "Confirmarás tu asistencia" : "Indicarás que no asistirás");
        alerta.setContentText("Evento: " + seleccionada.evento() + "\n\n"
                + "La respuesta se guardará cuando la pantalla se conecte con la base de datos.");
        URL css = getClass().getResource(CSS);
        if (css != null) alerta.getDialogPane().getStylesheets().add(css.toExternalForm());
        if (correo.getScene() != null) alerta.initOwner(correo.getScene().getWindow());
        alerta.getDialogPane().lookupButton(ButtonType.OK).getStyleClass().add("btn-primary");
        alerta.showAndWait();
    }

    private static String textoEstado(String estado) {
        return switch (estado) {
            case "CONFIRMADO" -> "ASISTIRÁ";
            case "RECHAZADO" -> "NO ASISTIRÁ";
            default -> "SIN RESPONDER";
        };
    }

    private static String claseEstado(String estado) {
        return switch (estado) {
            case "CONFIRMADO" -> "status-confirmada";
            case "RECHAZADO" -> "status-rechazada";
            default -> "status-pendiente";
        };
    }

    private Label pill(String estado) {
        Label pill = new Label(textoEstado(estado));
        pill.getStyleClass().setAll("label", "status", claseEstado(estado));
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
    // Utilidades (mismas que ViewEventos y ViewInvitados)
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

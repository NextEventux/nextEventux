package views;

import controllers.ControllerEventos;
import entities.Evento;
import entities.Organizador;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public class ViewEventos {
    private final ControllerEventos controller;
    private Runnable abrirPublicacion = () -> {};
    private Runnable abrirCancelacion = () -> {};
    private Runnable abrirEdicion = () -> {};
    private final TextField nombre = campo("nombre", "Ej. Festival NextEventux");
    private final ComboBox<String> tipo = new ComboBox<>();
    private final ComboBox<Organizador> organizador = new ComboBox<>();
    private final DatePicker fechaInicio = new DatePicker();
    private final DatePicker fechaFin = new DatePicker();
    private final TextField horaInicio = campo("horaInicio", "09:00");
    private final TextField horaFin = campo("horaFin", "18:00");
    private final TextField lugar = campo("lugar", "Ej. Bogotá, Colombia");
    private final TextField descripcion = campo("descripcion", "Descripción opcional del evento");
    private final TextField presupuesto = campo("presupuesto", "Ej. 15000000");
    private final TextField invitados = campo("invitados", "Ej. 100");
    private final Label resumenPresupuesto = new Label("$0 COP");
    private final Label resumenInvitados = new Label("0");
    private final Label mensaje = new Label();
    private final NumberFormat formatoCOP = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-CO"));

    public ViewEventos(ControllerEventos controller) {
        this.controller = controller;
        formatoCOP.setMaximumFractionDigits(2);
        organizador.setId("organizador");
        organizador.setPromptText("Seleccionar organizador");
        organizador.setMaxWidth(Double.MAX_VALUE);
        organizador.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Organizador item, boolean empty) {
                super.updateItem(item, empty);
                setText(item == null ? organizador.getPromptText() : item.nombre());
            }
        });
        tipo.setId("tipo");
        tipo.setPromptText("Seleccionar tipo");
        tipo.getItems().addAll("Corporativo", "Social", "Cultural", "Otro");
        tipo.setMaxWidth(Double.MAX_VALUE);
        tipo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(item == null ? "Seleccionar tipo" : item);
            }
        });
        fechaInicio.setId("fechaInicio");
        fechaFin.setId("fechaFin");
        fechaInicio.setEditable(false);
        fechaFin.setEditable(false);
        fechaInicio.setMaxWidth(Double.MAX_VALUE);
        fechaFin.setMaxWidth(Double.MAX_VALUE);
        mensaje.setId("mensaje");
        mensaje.setWrapText(true);
        mensaje.setVisible(false);
        mensaje.setManaged(false);
        resumenPresupuesto.setId("resumenPresupuesto");
        resumenInvitados.setId("resumenInvitados");
        presupuesto.textProperty().addListener((observable, anterior, actual) -> actualizarResumen());
        invitados.textProperty().addListener((observable, anterior, actual) -> actualizarResumen());
    }

    public ViewEventos(ControllerEventos controller, Runnable abrirPublicacion) {
        this(controller);
        this.abrirPublicacion = abrirPublicacion;
    }

    public ViewEventos(ControllerEventos controller, Runnable abrirPublicacion, Runnable abrirCancelacion) {
        this(controller, abrirPublicacion);
        this.abrirCancelacion = abrirCancelacion;
    }

    public ViewEventos(ControllerEventos controller, Runnable abrirPublicacion, Runnable abrirCancelacion, Runnable abrirEdicion) {
        this(controller, abrirPublicacion, abrirCancelacion);
        this.abrirEdicion = abrirEdicion;
    }

    public Parent crearPantalla() {
        BorderPane raiz = new BorderPane();
        raiz.getStyleClass().add("app");
        VBox sidebar = crearMenu();
        raiz.setLeft(sidebar);
        BorderPane principal = new BorderPane();
        principal.setTop(crearBarraSuperior());
        VBox contenido = new VBox(30);
        contenido.getStyleClass().add("content");
        contenido.setMaxWidth(1320);
        VBox titulo = new VBox(8, etiqueta("Crear nuevo evento", "page-title"),
                etiqueta("Completa la información para registrar tu próximo evento.", "muted"));
        HBox encabezado = new HBox(20, titulo, espacioHorizontal(), etiqueta("● En planeación", "status"));
        encabezado.setAlignment(Pos.CENTER_LEFT);
        VBox formulario = crearFormulario();
        VBox panel = crearPanelDerecho();
        GridPane columnas = new GridPane();
        columnas.setHgap(25);
        columnas.setVgap(25);
        ColumnConstraints izquierda = new ColumnConstraints();
        izquierda.setHgrow(Priority.ALWAYS);
        ColumnConstraints derecha = new ColumnConstraints(290);
        columnas.getColumnConstraints().addAll(izquierda, derecha);
        columnas.add(formulario, 0, 0);
        columnas.add(panel, 1, 0);
        GridPane.setHgrow(formulario, Priority.ALWAYS);
        formulario.setMinWidth(0);
        contenido.getChildren().addAll(encabezado, columnas);
        StackPane centrado = new StackPane(contenido);
        centrado.setAlignment(Pos.TOP_CENTER);
        ScrollPane scroll = new ScrollPane(centrado);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("content-scroll");
        principal.setCenter(scroll);
        raiz.setCenter(principal);
        // En ventanas pequeñas el panel baja y el menú deja espacio al formulario.
        raiz.widthProperty().addListener((observable, anterior, actual) -> {
            boolean ancho = actual.doubleValue() >= 1200;
            if (ancho != (GridPane.getColumnIndex(panel) == 1)) {
                columnas.getChildren().clear();
                columnas.getColumnConstraints().setAll(izquierda);
                if (ancho) columnas.getColumnConstraints().add(derecha);
                columnas.add(formulario, 0, 0);
                columnas.add(panel, ancho ? 1 : 0, ancho ? 0 : 1);
                panel.setMaxWidth(Double.MAX_VALUE);
                GridPane.setHgrow(panel, Priority.ALWAYS);
            }
            sidebar.setVisible(actual.doubleValue() >= 1000);
            sidebar.setManaged(sidebar.isVisible());
        });
        cargarOrganizadores();
        return raiz;
    }

    private void cargarOrganizadores() {
        try {
            organizador.getItems().setAll(controller.listarOrganizadores());
            if (organizador.getItems().isEmpty()) {
                mostrarMensaje("No hay organizadores registrados. Registra un organizador antes de crear el evento.", false);
            }
        } catch (IllegalStateException e) {
            mostrarMensaje(e.getMessage(), false);
        }
    }

    private VBox crearMenu() {
        VBox menu = new VBox(7);
        menu.getStyleClass().add("sidebar");
        menu.setPrefWidth(250);
        menu.setMinWidth(250);
        ImageView logo = new ImageView(new Image(
                getClass().getResource("/images/nexteventux-logo.png").toExternalForm()));
        // Excluir el margen blanco al mostrar la imagen, sin modificar el archivo original.
        logo.setViewport(new Rectangle2D(75, 225, 1110, 725));
        logo.setFitWidth(210);
        logo.setFitHeight(138);
        logo.setPreserveRatio(true);
        logo.setSmooth(true);
        logo.setAccessibleText("Logo de NextEventux");
        VBox marca = new VBox(5, logo, etiqueta("GESTIÓN DE EVENTOS", "brand-caption"));
        marca.setAlignment(Pos.CENTER);
        VBox.setMargin(marca, new Insets(0, 0, 38, 0));
        menu.getChildren().addAll(marca, etiqueta("MENÚ PRINCIPAL", "menu-title"));
        String[] opciones = {"⌂  Inicio", "▦  Mis eventos", "＋  Crear evento", "♧  Proveedores", "♙  Invitados", "⚙  Configuración"};
        for (String opcion : opciones) {
            Label item = etiqueta(opcion, "menu-item");
            item.setMaxWidth(Double.MAX_VALUE);
            if (opcion.contains("Crear evento")) item.getStyleClass().add("active");
            menu.getChildren().add(item);
        }
        Button publicaciones = new Button("↑ Subir al repositorio");
        publicaciones.setId("abrirPublicacion");
        publicaciones.getStyleClass().add("btn-secondary");
        publicaciones.setMaxWidth(Double.MAX_VALUE);
        publicaciones.setOnAction(event -> abrirPublicacion.run());
        menu.getChildren().add(publicaciones);
        Button cancelar = new Button("Cancelar evento");
        cancelar.setId("abrirCancelacion"); cancelar.getStyleClass().add("btn-secondary");
        cancelar.setMaxWidth(Double.MAX_VALUE); cancelar.setOnAction(event -> abrirCancelacion.run());
        menu.getChildren().add(cancelar);
        Button editar = new Button("Editar evento"); editar.setId("abrirEdicion"); editar.getStyleClass().add("btn-secondary");
        editar.setMaxWidth(Double.MAX_VALUE); editar.setOnAction(event -> abrirEdicion.run()); menu.getChildren().add(editar);
        Region espacio = new Region();
        VBox.setVgrow(espacio, Priority.ALWAYS);
        VBox perfil = new VBox(4, etiqueta("Organizador", "profile-name"), etiqueta("Mi cuenta", "hint"));
        HBox cuenta = new HBox(12, etiqueta("SC", "avatar"), perfil);
        cuenta.setAlignment(Pos.CENTER_LEFT);
        cuenta.getStyleClass().add("sidebar-bottom");
        menu.getChildren().addAll(espacio, cuenta);
        return menu;
    }

    private HBox crearBarraSuperior() {
        MenuItem publicar = new MenuItem("Subir al repositorio");
        publicar.setOnAction(event -> abrirPublicacion.run());
        MenuItem cancelar = new MenuItem("Cancelar evento");
        cancelar.setOnAction(event -> abrirCancelacion.run());
        MenuItem editar = new MenuItem("Editar evento"); editar.setOnAction(event -> abrirEdicion.run());
        MenuButton eventos = new MenuButton("Eventos", null, editar, publicar, cancelar);
        eventos.setId("menuEventos");
        HBox barra = new HBox(18, etiqueta("Inicio / Mis eventos /", "breadcrumb"),
                etiqueta("Crear evento", "breadcrumb-current"), espacioHorizontal(), eventos,
                etiqueta("♧", "notification"), etiqueta("Organizador", "user-badge"));
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.getStyleClass().add("topbar");
        return barra;
    }

    private VBox crearFormulario() {
        VBox tarjeta = new VBox(22);
        tarjeta.getStyleClass().add("card");
        HBox titulo = new HBox(13, etiqueta("▤", "heading-icon"), etiqueta("Información del evento", "card-title"));
        titulo.setAlignment(Pos.CENTER_LEFT);
        Label ayuda = etiqueta("Ingresa los datos generales de tu evento. Los campos marcados con * son obligatorios.", "muted");
        ayuda.setWrapText(true);
        GridPane datos = grilla();
        datos.add(grupo("Nombre del evento *", nombre, null), 0, 0, 2, 1);
        datos.add(grupo("Tipo de evento *", tipo, null), 0, 1);
        datos.add(grupo("Organizador *", organizador, null), 1, 1);
        datos.add(grupo("Fecha de inicio *", fechaInicio, null), 0, 2);
        datos.add(grupo("Hora de inicio *", horaInicio, "Formato de 24 horas: HH:mm"), 1, 2);
        datos.add(grupo("Fecha de fin *", fechaFin, null), 0, 3);
        datos.add(grupo("Hora de fin *", horaFin, "Formato de 24 horas: HH:mm"), 1, 3);
        datos.add(grupo("Lugar del evento *", lugar, null), 0, 4, 2, 1);
        datos.add(grupo("Descripción", descripcion, null), 0, 5, 2, 1);
        GridPane recursos = grilla();
        recursos.add(grupo("Presupuesto total (COP) *", presupuesto, "Ingresa el presupuesto asignado."), 0, 0);
        recursos.add(grupo("Invitados iniciales *", invitados, "Número inicial de invitados."), 1, 0);
        Button cancelar = new Button("Cancelar");
        cancelar.setId("cancelar");
        cancelar.getStyleClass().add("btn-secondary");
        cancelar.setOnAction(event -> limpiarFormulario());
        Button crear = new Button("+ Crear evento");
        crear.setId("crear");
        crear.getStyleClass().add("btn-primary");
        crear.setDefaultButton(true);
        crear.setOnAction(event -> crearDesdeFormulario());
        HBox acciones = new HBox(12, cancelar, crear);
        acciones.setAlignment(Pos.CENTER_RIGHT);
        tarjeta.getChildren().addAll(titulo, ayuda, datos, new Separator(),
                etiqueta("Presupuesto y asistentes", "section-title"), recursos, mensaje, acciones);
        return tarjeta;
    }

    private VBox crearPanelDerecho() {
        VBox panel = new VBox(20);
        panel.setPrefWidth(290);
        panel.setMinWidth(260);
        VBox inicio = informacion("✦ Tu evento comienza aquí", "Registra la información inicial de tu evento para comenzar su planeación.");
        inicio.getStyleClass().add("highlight");
        VBox resumen = new VBox(14, etiqueta("Resumen del evento", "card-title"),
                filaResumen("Estado", new Label("En planeación")), new Separator(),
                filaResumen("Presupuesto", resumenPresupuesto), new Separator(),
                filaResumen("Invitados", resumenInvitados));
        resumen.getStyleClass().add("info-card");
        panel.getChildren().addAll(inicio, resumen, informacion("ⓘ Información importante",
                "Tu evento comenzará en estado En planeación. Sus datos se guardarán y estarán disponibles después de cerrar la aplicación."));
        return panel;
    }

    private void crearDesdeFormulario() {
        try {
            if (organizador.getValue() == null)
                throw new IllegalArgumentException("Selecciona un organizador.");
            if (fechaInicio.getValue() == null || fechaFin.getValue() == null)
                throw new IllegalArgumentException("Selecciona las fechas de inicio y fin.");
            LocalTime inicio = leerHora(horaInicio.getText(), "inicio");
            LocalTime fin = leerHora(horaFin.getText(), "fin");
            BigDecimal valor;
            try {
                valor = new BigDecimal(presupuesto.getText().trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Ingresa un presupuesto numérico; usa punto para los decimales.");
            }
            Evento datos = new Evento(organizador.getValue().id(), nombre.getText().trim(),
                    tipo.getValue(), descripcion.getText().trim(), LocalDateTime.of(fechaInicio.getValue(), inicio),
                    LocalDateTime.of(fechaFin.getValue(), fin), lugar.getText().trim(), valor,
                    leerEntero(invitados.getText(), "invitados iniciales"));
            controller.crearEvento(datos);
            mostrarMensaje("Evento creado correctamente. Número de evento: " + datos.getIdEvento() + ".", true);
        } catch (IllegalArgumentException | IllegalStateException e) {
            mostrarMensaje(e.getMessage(), false);
        }
    }

    private LocalTime leerHora(String texto, String campo) {
        try {
            return LocalTime.parse(texto.trim(), DateTimeFormatter.ofPattern("HH:mm")
                    .withResolverStyle(java.time.format.ResolverStyle.STRICT));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Ingresa una hora de " + campo + " válida en formato HH:mm.");
        }
    }

    private int leerEntero(String texto, String campo) {
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Ingresa un número entero válido para " + campo + ".");
        }
    }

    private void actualizarResumen() {
        try {
            BigDecimal valor = new BigDecimal(presupuesto.getText().trim());
            resumenPresupuesto.setText(valor.signum() < 0 ? "$0 COP" : "$" + formatoCOP.format(valor) + " COP");
        } catch (NumberFormatException e) {
            resumenPresupuesto.setText("$0 COP");
        }
        try {
            int cantidad = Integer.parseInt(invitados.getText().trim());
            resumenInvitados.setText(Integer.toString(Math.max(0, cantidad)));
        } catch (NumberFormatException e) {
            resumenInvitados.setText("0");
        }
    }

    private void limpiarFormulario() {
        for (TextField campo : new TextField[]{nombre, horaInicio, horaFin, lugar, descripcion, presupuesto, invitados}) campo.clear();
        organizador.getSelectionModel().clearSelection();
        tipo.getSelectionModel().clearSelection();
        fechaInicio.setValue(null);
        fechaFin.setValue(null);
        mensaje.setVisible(false);
        mensaje.setManaged(false);
        nombre.requestFocus();
    }

    private void mostrarMensaje(String texto, boolean exito) {
        mensaje.setText(texto);
        mensaje.getStyleClass().setAll("message", exito ? "success" : "error");
        mensaje.setVisible(true);
        mensaje.setManaged(true);
    }

    private static TextField campo(String id, String ayuda) {
        TextField campo = new TextField();
        campo.setId(id);
        campo.setPromptText(ayuda);
        return campo;
    }

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

    private GridPane grilla() {
        GridPane grid = new GridPane();
        grid.setHgap(23);
        grid.setVgap(23);
        for (int i = 0; i < 2; i++) {
            ColumnConstraints columna = new ColumnConstraints();
            columna.setPercentWidth(50);
            columna.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(columna);
        }
        return grid;
    }

    private VBox grupo(String titulo, javafx.scene.Node control, String ayuda) {
        VBox grupo = new VBox(9, etiqueta(titulo, "field-label"), control);
        if (ayuda != null) grupo.getChildren().add(etiqueta(ayuda, "hint"));
        return grupo;
    }

    private VBox informacion(String titulo, String texto) {
        Label descripcion = etiqueta(texto, "muted");
        descripcion.setWrapText(true);
        VBox tarjeta = new VBox(14, etiqueta(titulo, "card-title"), descripcion);
        tarjeta.getStyleClass().add("info-card");
        return tarjeta;
    }

    private HBox filaResumen(String titulo, Label valor) {
        valor.getStyleClass().add("summary-value");
        HBox fila = new HBox(10, etiqueta(titulo, "hint"), espacioHorizontal(), valor);
        fila.setAlignment(Pos.CENTER_LEFT);
        return fila;
    }
}

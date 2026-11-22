package views;

import controllers.ControllerMarketing;
import entities.Campana;
import entities.Segmentacion;
import entities.SesionUsuario;
import java.math.BigDecimal;
import java.util.function.Function;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;
import views.components.MenuCompartido;

// cu-23: pantalla para crear campañas de marketing y seguir su estado y sus métricas.
public class ViewCampana {
    private static final double SEGUNDOS_ACTUALIZACION = 2;

    private final ControllerMarketing controller;
    private final SesionUsuario sesion;
    private final Runnable abrirMarketing;
    private final Runnable cerrarSesion;
    private final Runnable abrirSanciones;
    private final Runnable abrirPublicacion;

    private final ComboBox<Segmentacion> segmento = new ComboBox<>();
    private final Label usuariosEstimados = new Label("Usuarios estimados: selecciona un segmento");
    private final TextField nombre = new TextField();
    private final TextField tipoOferta = new TextField();
    private final DatePicker fechaInicio = new DatePicker();
    private final DatePicker fechaFin = new DatePicker();
    private final TextField presupuesto = new TextField();
    private final Label mensaje = new Label();
    private final TableView<Campana> tabla = new TableView<>();

    public ViewCampana(
            ControllerMarketing controller,
            SesionUsuario sesion,
            Runnable abrirMarketing,
            Runnable cerrarSesion,
            Runnable abrirSanciones,
            Runnable abrirPublicacion) {
        this.controller = controller;
        this.sesion = sesion;
        this.abrirMarketing = abrirMarketing;
        this.cerrarSesion = cerrarSesion;
        this.abrirSanciones = abrirSanciones;
        this.abrirPublicacion = abrirPublicacion;
        configurarCampos();
        configurarTabla();
    }

    public Parent crearPantalla() {
        BorderPane raiz = new BorderPane();
        raiz.getStyleClass().add("app");
        raiz.getStylesheets().add(
                getClass().getResource("/styles/marketing.css").toExternalForm());
        raiz.setLeft(MenuCompartido.crear(
                sesion,
                MenuCompartido.Pagina.MARKETING,
                null, null, null, null,
                cerrarSesion,
                null, null, null,
                abrirSanciones,
                null,
                abrirPublicacion,
                abrirMarketing));

        BorderPane principal = new BorderPane();
        principal.setTop(MenuCompartido.crearBarraSuperior(
                sesion, "Marketing / Crear campaña", "Segmentos", abrirMarketing));

        VBox contenido = new VBox(30);
        contenido.getStyleClass().add("content");
        contenido.setMaxWidth(1320);
        VBox titulo = new VBox(8,
                etiqueta("Crear campaña", "page-title"),
                etiqueta("Define la oferta, su vigencia y el presupuesto para un segmento de clientes.", "muted"));

        GridPane columnas = new GridPane();
        columnas.setHgap(25);
        columnas.setVgap(25);
        ColumnConstraints izquierda = new ColumnConstraints();
        izquierda.setHgrow(Priority.ALWAYS);
        ColumnConstraints derecha = new ColumnConstraints(290);
        columnas.getColumnConstraints().addAll(izquierda, derecha);
        VBox formulario = crearFormulario();
        formulario.setMinWidth(0);
        columnas.add(formulario, 0, 0);
        columnas.add(crearPanelInformacion(), 1, 0);
        GridPane.setHgrow(formulario, Priority.ALWAYS);

        contenido.getChildren().addAll(titulo, columnas, crearTarjetaCampanas());

        StackPane centrado = new StackPane(contenido);
        centrado.setAlignment(Pos.TOP_CENTER);
        ScrollPane scroll = new ScrollPane(centrado);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("content-scroll");
        principal.setCenter(scroll);
        raiz.setCenter(principal);

        cargarSegmentos();
        cargarCampanas();
        iniciarActualizacion(raiz);
        return raiz;
    }

    // ---------- Resumen consolidado (paso 5) ----------

    public static String construirTextoResumen(Campana campana) {
        return "Campaña: " + campana.getNombre() + "\n"
                + "Segmento: " + (campana.getNombreSegmento() == null
                        ? FormatoUi.SIN_DATO : campana.getNombreSegmento()) + "\n"
                + "Oferta: " + campana.getTipoOferta() + "\n"
                + "Vigencia: " + FormatoUi.fecha(campana.getFechaInicio())
                + " a " + FormatoUi.fecha(campana.getFechaFin()) + "\n"
                + "Presupuesto asignado: " + FormatoUi.moneda(campana.getPresupuestoMaximo()) + "\n"
                + "Alcance (visualizaciones): " + campana.getVisualizaciones() + "\n"
                + "Conversión (contrataciones): " + campana.getConversiones();
    }

    public static void mostrarResumen(Campana campana) {
        Alert dialogo = new Alert(Alert.AlertType.INFORMATION);
        dialogo.setTitle("Resumen de la campaña");
        dialogo.setHeaderText("La campaña terminó");
        dialogo.setContentText(construirTextoResumen(campana));
        dialogo.show();
    }

    // ---------- Acciones ----------

    private void crearCampana() {
        BigDecimal monto = null;
        String textoMonto = presupuesto.getText();
        if (textoMonto != null && !textoMonto.isBlank()) {
            try {
                monto = new BigDecimal(textoMonto.trim());
            } catch (NumberFormatException e) {
                mostrarMensaje("El presupuesto debe ser un valor numérico.", "error");
                return;
            }
        }
        Segmentacion elegido = segmento.getValue();
        try {
            Campana creada = controller.crearCampana(new Campana(
                    sesion.getIdUsuario(),
                    elegido == null ? 0 : elegido.getIdSegmento(),
                    nombre.getText(),
                    tipoOferta.getText(),
                    fechaInicio.getValue(),
                    fechaFin.getValue(),
                    monto));
            mostrarMensaje("Campaña «" + creada.getNombre() + "» programada. Se activará el "
                    + FormatoUi.fecha(creada.getFechaInicio()) + ".", "success");
            limpiarFormulario();
            cargarCampanas();
        } catch (IllegalArgumentException | IllegalStateException e) {
            mostrarMensaje(e.getMessage(), "error");
        }
    }

    private void pausarCampana() {
        Campana seleccionada = exigirSeleccion();
        if (seleccionada == null) {
            return;
        }
        try {
            controller.pausarCampana(sesion.getIdUsuario(), seleccionada.getIdCampana());
            mostrarMensaje("Campaña pausada. Las métricas registradas se conservan.", "success");
            cargarCampanas();
        } catch (IllegalArgumentException | IllegalStateException e) {
            mostrarMensaje(e.getMessage(), "error");
        }
    }

    private void reanudarCampana() {
        Campana seleccionada = exigirSeleccion();
        if (seleccionada == null) {
            return;
        }
        try {
            controller.reanudarCampana(sesion.getIdUsuario(), seleccionada.getIdCampana());
            mostrarMensaje("Campaña reactivada.", "success");
            cargarCampanas();
        } catch (IllegalArgumentException | IllegalStateException e) {
            mostrarMensaje(e.getMessage(), "error");
        }
    }

    private void verResumen() {
        Campana seleccionada = exigirSeleccion();
        if (seleccionada != null) {
            mostrarResumen(seleccionada);
        }
    }

    private Campana exigirSeleccion() {
        Campana seleccionada = tabla.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            mostrarMensaje("Selecciona una campaña de la tabla.", "error");
        }
        return seleccionada;
    }

    // ---------- Carga de datos ----------

    private void cargarSegmentos() {
        try {
            segmento.setItems(FXCollections.observableArrayList(controller.listarSegmentosActivos()));
        } catch (IllegalStateException e) {
            mostrarMensaje(e.getMessage(), "error");
        }
    }

    private void cargarCampanas() {
        Campana seleccionada = tabla.getSelectionModel().getSelectedItem();
        try {
            tabla.setItems(FXCollections.observableArrayList(controller.listarCampanas()));
            if (seleccionada != null) {
                for (Campana campana : tabla.getItems()) {
                    if (campana.getIdCampana().equals(seleccionada.getIdCampana())) {
                        tabla.getSelectionModel().select(campana);
                        break;
                    }
                }
            }
        } catch (IllegalStateException e) {
            mostrarMensaje(e.getMessage(), "error");
        }
    }

    // La tabla se actualiza sola mientras la pantalla está visible para mostrar las métricas en vivo.
    private void iniciarActualizacion(Parent raiz) {
        Timeline actualizacion = new Timeline(
                new KeyFrame(Duration.seconds(SEGUNDOS_ACTUALIZACION), e -> cargarCampanas()));
        actualizacion.setCycleCount(Animation.INDEFINITE);
        raiz.sceneProperty().addListener((obs, anterior, nueva) -> {
            if (nueva == null) {
                actualizacion.stop();
            } else {
                actualizacion.play();
            }
        });
        if (raiz.getScene() != null) {
            actualizacion.play();
        }
    }

    // ---------- Construcción de la pantalla ----------

    private void configurarCampos() {
        segmento.setId("segmento");
        segmento.setPromptText("Selecciona un segmento");
        segmento.setMaxWidth(Double.MAX_VALUE);
        segmento.setConverter(new StringConverter<>() {
            @Override
            public String toString(Segmentacion s) {
                return s == null ? "" : s.getNombre() + " (" + s.getCantidadClientes() + " clientes)";
            }

            @Override
            public Segmentacion fromString(String texto) {
                return null;
            }
        });
        segmento.valueProperty().addListener((obs, anterior, nuevo) -> usuariosEstimados.setText(
                nuevo == null ? "Usuarios estimados: selecciona un segmento"
                        : "Usuarios estimados: " + nuevo.getCantidadClientes()));
        usuariosEstimados.setId("usuariosEstimados");
        usuariosEstimados.getStyleClass().add("hint");

        nombre.setId("nombre");
        nombre.setPromptText("Ej. Bodas de fin de año");
        tipoOferta.setId("tipoOferta");
        tipoOferta.setPromptText("Ej. Descuento del 10 %");
        fechaInicio.setId("fechaInicio");
        fechaFin.setId("fechaFin");
        fechaInicio.setMaxWidth(Double.MAX_VALUE);
        fechaFin.setMaxWidth(Double.MAX_VALUE);
        presupuesto.setId("presupuesto");
        presupuesto.setPromptText("Ej. 1500000");

        mensaje.setId("mensaje");
        mensaje.setWrapText(true);
        mensaje.setVisible(false);
        mensaje.setManaged(false);
    }

    private void configurarTabla() {
        tabla.setId("tablaCampanas");
        tabla.setPlaceholder(new Label("Aún no hay campañas creadas."));
        tabla.setPrefHeight(300);
        tabla.getColumns().add(columna("Campaña", 0.19, false, Campana::getNombre));
        tabla.getColumns().add(columna("Segmento", 0.17, false,
                c -> c.getNombreSegmento() == null ? FormatoUi.SIN_DATO : c.getNombreSegmento()));
        tabla.getColumns().add(columna("Estado", 0.10, true, c -> textoEstado(c.getEstado())));
        tabla.getColumns().add(columna("Vigencia", 0.19, true,
                c -> FormatoUi.fecha(c.getFechaInicio()) + " a " + FormatoUi.fecha(c.getFechaFin())));
        tabla.getColumns().add(columna("Presupuesto", 0.13, true,
                c -> FormatoUi.moneda(c.getPresupuestoMaximo())));
        tabla.getColumns().add(columna("Alcance", 0.09, true,
                c -> String.valueOf(c.getVisualizaciones())));
        tabla.getColumns().add(columna("Contrataciones", 0.13, true,
                c -> String.valueOf(c.getConversiones())));
    }

    private TableColumn<Campana, String> columna(String titulo, double proporcion, boolean centrada,
            Function<Campana, String> valor) {
        TableColumn<Campana, String> columna = new TableColumn<>(titulo);
        columna.setCellValueFactory(datos -> new ReadOnlyStringWrapper(valor.apply(datos.getValue())));
        columna.prefWidthProperty().bind(tabla.widthProperty().multiply(proporcion));
        columna.setSortable(false);
        if (centrada) {
            columna.getStyleClass().add("centrado");
        }
        return columna;
    }

    private VBox crearFormulario() {
        VBox tarjeta = new VBox(18);
        tarjeta.getStyleClass().add("card");
        Button crear = new Button("Crear campaña");
        crear.setId("crear");
        crear.getStyleClass().add("btn-primary");
        crear.setOnAction(event -> crearCampana());
        HBox acciones = new HBox(12, crear);
        acciones.setAlignment(Pos.CENTER_RIGHT);
        tarjeta.getChildren().addAll(
                etiqueta("Datos de la campaña", "card-title"),
                campo("Segmento objetivo", segmento),
                usuariosEstimados,
                campo("Nombre de la campaña", nombre),
                campo("Tipo de oferta", tipoOferta),
                new HBox(18, crecer(campo("Fecha de inicio", fechaInicio)),
                        crecer(campo("Fecha de fin", fechaFin))),
                campo("Presupuesto máximo (COP)", presupuesto),
                mensaje,
                acciones);
        return tarjeta;
    }

    private VBox crearPanelInformacion() {
        Label texto = etiqueta("La campaña queda programada y se activa sola en la fecha de inicio. "
                + "Mientras está activa se registran las visualizaciones y las contrataciones. "
                + "En la fecha de fin se desactiva y se muestra su resumen. "
                + "El presupuesto indicado se descuenta del disponible de marketing al crearla. "
                + "Solo reciben la oferta los clientes del segmento que aceptan comunicaciones de marketing.",
                "muted");
        texto.setWrapText(true);
        VBox panel = new VBox(14, etiqueta("ⓘ Cómo funciona", "card-title"), texto);
        panel.getStyleClass().add("info-card");
        panel.setPrefWidth(290);
        panel.setMinWidth(260);
        return panel;
    }

    private VBox crearTarjetaCampanas() {
        Button pausar = new Button("Pausar campaña");
        pausar.setId("pausar");
        pausar.getStyleClass().add("btn-secondary");
        pausar.setOnAction(event -> pausarCampana());
        Button reanudar = new Button("Reactivar campaña");
        reanudar.setId("reanudar");
        reanudar.getStyleClass().add("btn-secondary");
        reanudar.setOnAction(event -> reanudarCampana());
        Button resumen = new Button("Ver resumen");
        resumen.setId("resumen");
        resumen.getStyleClass().add("btn-secondary");
        resumen.setOnAction(event -> verResumen());
        HBox acciones = new HBox(12, pausar, reanudar, resumen);
        acciones.setAlignment(Pos.CENTER_RIGHT);

        VBox tarjeta = new VBox(18, etiqueta("Campañas", "card-title"), tabla, acciones);
        tarjeta.getStyleClass().add("card");
        return tarjeta;
    }

    private VBox campo(String titulo, Node control) {
        return new VBox(8, etiqueta(titulo, "field-label"), control);
    }

    private VBox crecer(VBox campo) {
        HBox.setHgrow(campo, Priority.ALWAYS);
        campo.setMaxWidth(Double.MAX_VALUE);
        return campo;
    }

    private void limpiarFormulario() {
        segmento.setValue(null);
        nombre.clear();
        tipoOferta.clear();
        fechaInicio.setValue(null);
        fechaFin.setValue(null);
        presupuesto.clear();
    }

    private void mostrarMensaje(String texto, String estilo) {
        mensaje.setText(texto);
        mensaje.getStyleClass().setAll("message", estilo);
        mensaje.setVisible(true);
        mensaje.setManaged(true);
    }

    private static String textoEstado(String estado) {
        return estado == null ? FormatoUi.SIN_DATO
                : estado.charAt(0) + estado.substring(1).toLowerCase();
    }

    private Label etiqueta(String texto, String estilo) {
        Label label = new Label(texto);
        label.getStyleClass().add(estilo);
        return label;
    }
}

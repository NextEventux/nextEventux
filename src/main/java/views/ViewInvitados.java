package views;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import java.net.URL;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * CU17 - Gestionar invitaciones.
 *
 * Sigue el mismo patrón que ViewEventos: no extiende Application; construye la
 * pantalla en crearPantalla() y quien la abre la pone en una Scene.
 *
 * Por ahora trabaja con datos mock (ver cargarDatosMock()). Cuando exista
 * ControllerInvitados, ese método se reemplaza por la consulta al controlador
 * (View -> Controller -> Service -> Repository -> H2). Esta clase no contiene SQL.
 */
public class ViewInvitados {

    private static final String CSS = "/styles/invitados.css";
    private static final String LOGO = "/images/logo-carpa.png";
    private static final String TODOS = "Todos los eventos";
    private static final double ANCHO_PANEL = 270;
    /** Ancho de ventana desde el cual el panel derecho va al lado de la tabla. */
    private static final double ANCHO_PANEL_LATERAL = 1400;
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Fila de la tabla. Refleja las columnas de la tabla Invitacion más los nombres a mostrar. */
    public record FilaInvitacion(int idInvitacion, int idEvento, String nombreEvento, int idLugar,
                                 String nombreLugar, LocalDateTime fechaEnvio, String mensaje,
                                 String estadoRespuesta) {
    }

    /** Datos del evento que se muestran en el panel lateral. */
    public record DetalleEvento(int idEvento, String nombre, String fecha, String lugar, String asistentes) {
    }

    private final ObservableList<FilaInvitacion> invitaciones = FXCollections.observableArrayList();
    private final FilteredList<FilaInvitacion> filtradas = new FilteredList<>(invitaciones, fila -> true);
    private final ObservableList<DetalleEvento> eventos = FXCollections.observableArrayList();

    private final ComboBox<String> selectorEvento = new ComboBox<>();
    private final TextField buscador = new TextField();
    private final TableView<FilaInvitacion> tabla = new TableView<>(filtradas);
    private final Label cantidadRegistros = new Label();

    private final Label totalValor = new Label("0");
    private final Label enviadasValor = new Label("0");
    private final Label pendientesValor = new Label("0");
    private final Label respuestasValor = new Label("0");

    private final Label detalleNombre = new Label();
    private final Label detalleFecha = new Label();
    private final Label detalleLugar = new Label();
    private final Label detalleAsistentes = new Label();

    public ViewInvitados() {
        cargarDatosMock();

        selectorEvento.setId("selectorEvento");
        selectorEvento.getItems().add(TODOS);
        for (DetalleEvento evento : eventos) selectorEvento.getItems().add(evento.nombre());
        selectorEvento.getSelectionModel().selectFirst();
        selectorEvento.setPrefWidth(280);
        selectorEvento.setMinWidth(200);
        selectorEvento.valueProperty().addListener((obs, anterior, actual) -> actualizarVista());

        buscador.setId("buscador");
        buscador.setPromptText("Buscar invitación...");
        buscador.setPrefWidth(230);
        buscador.setMinWidth(150);
        buscador.textProperty().addListener((obs, anterior, actual) -> actualizarVista());

        cantidadRegistros.setId("cantidadRegistros");
        configurarTabla();
        actualizarVista();
    }

    // ------------------------------------------------------------------
    // Datos mock: punto único a reemplazar por ControllerInvitados.
    // ------------------------------------------------------------------
    private void cargarDatosMock() {
        eventos.setAll(
                new DetalleEvento(1, "Festival NextEventux", "20 de octubre de 2026", "Bogotá, Colombia", "500"),
                new DetalleEvento(2, "Baby Shower de Valentina", "Por confirmar", "Bogotá, Colombia", "Por confirmar"));
        invitaciones.setAll(
                new FilaInvitacion(1, 1, "Festival NextEventux", 1, "Bogotá, Colombia",
                        LocalDateTime.of(2026, 10, 20, 10, 0),
                        "Te invitamos a celebrar con nosotros este día especial.", "ENVIADA"),
                new FilaInvitacion(2, 1, "Festival NextEventux", 1, "Bogotá, Colombia",
                        LocalDateTime.of(2026, 10, 20, 15, 0),
                        "Esperamos contar contigo en nuestra gran celebración.", "ENVIADA"),
                new FilaInvitacion(3, 2, "Baby Shower de Valentina", 1, "Bogotá, Colombia",
                        LocalDateTime.of(2026, 11, 1, 9, 0),
                        "Acompáñanos en el baby shower de Valentina.", "ENVIADA"));
    }

    // ------------------------------------------------------------------
    // Construcción de la pantalla
    // ------------------------------------------------------------------
    public Parent crearPantalla() {
        BorderPane raiz = new BorderPane();
        raiz.getStyleClass().add("app");
        conectarCss(raiz);

        VBox sidebar = crearMenu();
        raiz.setLeft(sidebar);

        BorderPane principal = new BorderPane();
        principal.setTop(crearBarraSuperior());

        VBox contenido = new VBox(25);
        contenido.getStyleClass().add("content");
        contenido.setMaxWidth(1320);

        VBox titulo = new VBox(8, etiqueta("Invitaciones", "page-title"),
                etiqueta("Gestiona las invitaciones enviadas para tus eventos.", "muted"));

        VBox tablaCard = crearTarjetaTabla();
        tablaCard.setMinWidth(0);
        tablaCard.setMaxHeight(Region.USE_PREF_SIZE);
        GridPane.setValignment(tablaCard, VPos.TOP);

        VBox tarjetaDetalles = crearTarjetaDetalles();
        VBox tarjetaAcciones = crearTarjetaAcciones();
        VBox tarjetaInfo = informacion("Información importante",
                "Las invitaciones podrán enviarse a los invitados y su estado de respuesta se actualizará "
                        + "cuando confirmen o rechacen la invitación.");
        tarjetaInfo.getStyleClass().add("highlight");
        Region[] tarjetasPanel = {tarjetaDetalles, tarjetaAcciones, tarjetaInfo};

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
        contenido.getChildren().addAll(titulo, crearTarjetaEvento(), estadisticas, columnas);

        StackPane centrado = new StackPane(contenido);
        centrado.setAlignment(Pos.TOP_CENTER);
        ScrollPane scroll = new ScrollPane(centrado);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("content-scroll");
        principal.setCenter(scroll);
        raiz.setCenter(principal);

        // Igual que ViewEventos: con ventana angosta el panel baja debajo de la tabla
        // (sus tarjetas quedan en fila) y con ventana muy angosta se oculta el menú.
        Runnable[] organizar = new Runnable[1];
        boolean[] lateralActual = {false};
        boolean[] inicializado = {false};
        organizar[0] = () -> {
            double ancho = raiz.getWidth() <= 0 ? 1440 : raiz.getWidth();
            organizarEstadisticas(estadisticas, ancho >= 1280);
            boolean lateral = ancho >= ANCHO_PANEL_LATERAL;
            if (inicializado[0] && lateral == lateralActual[0]) return;
            inicializado[0] = true;
            lateralActual[0] = lateral;
            panelVertical.getChildren().clear();
            panelHorizontal.getChildren().clear();
            columnas.getChildren().clear();
            columnas.getColumnConstraints().setAll(izquierda);
            columnas.add(tablaCard, 0, 0);
            GridPane.setHgrow(tablaCard, Priority.ALWAYS);
            if (lateral) {
                columnas.getColumnConstraints().add(derecha);
                for (Region tarjeta : tarjetasPanel) {
                    tarjeta.setMaxWidth(Double.MAX_VALUE);
                    tarjeta.setPrefWidth(Region.USE_COMPUTED_SIZE);
                    HBox.setHgrow(tarjeta, Priority.NEVER);
                }
                panelVertical.getChildren().addAll(tarjetasPanel);
                columnas.add(panelVertical, 1, 0);
            } else {
                for (Region tarjeta : tarjetasPanel) {
                    tarjeta.setMaxWidth(Double.MAX_VALUE);
                    tarjeta.setPrefWidth(0);
                    HBox.setHgrow(tarjeta, Priority.ALWAYS);
                }
                panelHorizontal.getChildren().addAll(tarjetasPanel);
                columnas.add(panelHorizontal, 0, 1);
                GridPane.setHgrow(panelHorizontal, Priority.ALWAYS);
            }
        };
        organizar[0].run();
        raiz.widthProperty().addListener((obs, anterior, actual) -> {
            organizar[0].run();
            sidebar.setVisible(actual.doubleValue() >= 1000);
            sidebar.setManaged(sidebar.isVisible());
        });
        return raiz;
    }

    private void conectarCss(Parent raiz) {
        URL css = getClass().getResource(CSS);
        if (css != null) {
            raiz.getStylesheets().add(css.toExternalForm());
        } else {
            System.err.println("[ViewInvitados] No se encontró " + CSS + " en el classpath.");
        }
    }

    private VBox crearMenu() {
        VBox menu = new VBox(7);
        menu.getStyleClass().add("sidebar");
        menu.setPrefWidth(250);
        menu.setMinWidth(250);
        VBox marca = new VBox(5, crearLogo(), etiqueta("nextEventux", "brand"),
                etiqueta("GESTIÓN DE EVENTOS", "brand-caption"));
        marca.setAlignment(Pos.CENTER);
        VBox.setMargin(marca, new Insets(0, 0, 38, 0));
        menu.getChildren().addAll(marca, etiqueta("MENÚ PRINCIPAL", "menu-title"));
        String[] opciones = {"⌂  Inicio", "▦  Mis eventos", "＋  Crear evento", "♧  Proveedores", "♙  Invitados", "⚙  Configuración"};
        for (String opcion : opciones) {
            Label item = etiqueta(opcion, "menu-item");
            item.setMaxWidth(Double.MAX_VALUE);
            if (opcion.contains("Invitados")) item.getStyleClass().add("active");
            menu.getChildren().add(item);
        }
        Region espacio = new Region();
        VBox.setVgrow(espacio, Priority.ALWAYS);
        VBox perfil = new VBox(4, etiqueta("Organizador", "profile-name"), etiqueta("Mi cuenta", "hint"));
        HBox cuenta = new HBox(12, etiqueta("SC", "avatar"), perfil);
        cuenta.setAlignment(Pos.CENTER_LEFT);
        cuenta.getStyleClass().add("sidebar-bottom");
        menu.getChildren().addAll(espacio, cuenta);
        return menu;
    }

    /** Carpa del logo de NextEventux (misma identidad que la pantalla de eventos). */
    private Node crearLogo() {
        URL imagen = getClass().getResource(LOGO);
        if (imagen == null) {
            System.err.println("[ViewInvitados] No se encontró " + LOGO + " en el classpath.");
            return new Region();
        }
        ImageView logo = new ImageView(new Image(imagen.toExternalForm()));
        logo.setFitWidth(76);
        logo.setPreserveRatio(true);
        logo.setSmooth(true);
        return logo;
    }

    private HBox crearBarraSuperior() {
        HBox barra = new HBox(18, etiqueta("Inicio / Mis eventos /", "breadcrumb"),
                etiqueta("Invitados", "breadcrumb-current"), espacioHorizontal(),
                etiqueta("♧", "notification"), etiqueta("Organizador", "user-badge"));
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.getStyleClass().add("topbar");
        return barra;
    }

    private VBox crearTarjetaEvento() {
        Label ayuda = etiqueta("Selecciona un evento para gestionar sus invitaciones.", "muted");
        ayuda.setWrapText(true);
        VBox textos = new VBox(6, etiqueta("Evento seleccionado", "card-title"), ayuda);
        textos.setMinWidth(0);
        HBox.setHgrow(textos, Priority.ALWAYS);

        Button nueva = new Button("+ Nueva invitación");
        nueva.setId("nuevaInvitacion");
        nueva.getStyleClass().add("btn-primary");
        nueva.setMinWidth(Region.USE_PREF_SIZE);
        nueva.setOnAction(e -> nuevaInvitacion());

        HBox fila = new HBox(16, etiqueta("▦", "heading-icon"), textos, selectorEvento, nueva);
        fila.setAlignment(Pos.CENTER_LEFT);
        VBox tarjeta = new VBox(fila);
        tarjeta.getStyleClass().add("card");
        return tarjeta;
    }

    private GridPane crearEstadisticas() {
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.getChildren().addAll(
                estadistica("Total invitaciones", totalValor, "Registradas", "▤"),
                estadistica("Enviadas", enviadasValor, "Invitaciones enviadas", "➤"),
                estadistica("Pendientes", pendientesValor, "Sin respuesta", "◷"),
                estadistica("Respuestas", respuestasValor, "Con confirmación", "✓"));
        organizarEstadisticas(grid, true);
        return grid;
    }

    /** Cuatro tarjetas en fila; en ventanas angostas, cuadrícula de 2 x 2. */
    private void organizarEstadisticas(GridPane grid, boolean enFila) {
        int columnas = enFila ? 4 : 2;
        if (grid.getColumnConstraints().size() == columnas) return;
        grid.getColumnConstraints().clear();
        for (int i = 0; i < columnas; i++) {
            ColumnConstraints columna = new ColumnConstraints();
            columna.setPercentWidth(100.0 / columnas);
            columna.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(columna);
        }
        for (int i = 0; i < grid.getChildren().size(); i++) {
            Node tarjeta = grid.getChildren().get(i);
            GridPane.setConstraints(tarjeta, i % columnas, i / columnas);
        }
    }

    private VBox estadistica(String titulo, Label valor, String descripcion, String icono) {
        valor.getStyleClass().add("stat-value");
        Label nombre = etiqueta(titulo, "field-label");
        nombre.setWrapText(true);
        nombre.setMinWidth(0);
        HBox encabezado = new HBox(10, nombre, espacioHorizontal(), etiqueta(icono, "heading-icon"));
        encabezado.setAlignment(Pos.CENTER_LEFT);
        VBox tarjeta = new VBox(8, encabezado, valor, etiqueta(descripcion, "hint"));
        tarjeta.getStyleClass().add("info-card");
        tarjeta.setMaxWidth(Double.MAX_VALUE);
        tarjeta.setMinWidth(0);
        return tarjeta;
    }

    private VBox crearTarjetaTabla() {
        VBox textos = new VBox(6, etiqueta("Invitaciones", "card-title"),
                etiqueta("Lista de invitaciones del evento seleccionado.", "muted"));
        textos.setMinWidth(0);
        cantidadRegistros.getStyleClass().add("user-badge");
        cantidadRegistros.setMinWidth(Region.USE_PREF_SIZE);
        HBox encabezado = new HBox(14, textos, espacioHorizontal(), buscador, cantidadRegistros);
        encabezado.setAlignment(Pos.CENTER_LEFT);

        VBox tarjeta = new VBox(22, encabezado, tabla);
        tarjeta.getStyleClass().add("card");
        return tarjeta;
    }

    @SuppressWarnings("deprecation") // CONSTRAINED_RESIZE_POLICY sigue funcionando en JavaFX 17 y 21
    private void configurarTabla() {
        tabla.setId("tablaInvitaciones");
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setFixedCellSize(58);
        tabla.setPlaceholder(etiqueta("No hay invitaciones que coincidan con la búsqueda.", "muted"));
        // La tabla crece con sus filas; el desplazamiento lo hace el ScrollPane de la página.
        tabla.prefHeightProperty().bind(Bindings.createDoubleBinding(
                () -> 50 + 58.0 * Math.max(filtradas.size(), 2), filtradas));
        tabla.minHeightProperty().bind(tabla.prefHeightProperty());
        tabla.maxHeightProperty().bind(tabla.prefHeightProperty());

        TableColumn<FilaInvitacion, Number> id = new TableColumn<>("ID");
        id.setCellValueFactory(c -> new javafx.beans.property.SimpleIntegerProperty(c.getValue().idInvitacion()));
        id.setMinWidth(40);
        id.setPrefWidth(45);

        TableColumn<FilaInvitacion, String> evento = columnaTexto("Evento", FilaInvitacion::nombreEvento, 110, 190);
        TableColumn<FilaInvitacion, String> lugar = columnaTexto("Lugar", FilaInvitacion::nombreLugar, 90, 130);
        TableColumn<FilaInvitacion, String> fecha = columnaTexto("Fecha de envío",
                fila -> fila.fechaEnvio().format(FORMATO_FECHA), 95, 130);

        TableColumn<FilaInvitacion, String> estado = columnaTexto("Estado", FilaInvitacion::estadoRespuesta, 95, 100);
        estado.setCellFactory(col -> new TableCell<>() {
            private final Label pill = new Label();
            { pill.setMinWidth(Region.USE_PREF_SIZE); }
            @Override
            protected void updateItem(String valor, boolean vacio) {
                super.updateItem(valor, vacio);
                if (vacio || valor == null) {
                    setGraphic(null);
                } else {
                    pill.setText(valor);
                    pill.getStyleClass().setAll("label", "status", claseEstado(valor));
                    setGraphic(pill);
                }
                setText(null);
            }
        });

        TableColumn<FilaInvitacion, FilaInvitacion> acciones = new TableColumn<>("Acciones");
        acciones.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue()));
        acciones.setMinWidth(170);
        acciones.setPrefWidth(170);
        acciones.setSortable(false);
        acciones.setCellFactory(col -> new TableCell<>() {
            private final Button editar = new Button("Editar");
            private final Button eliminar = new Button("Eliminar");
            private final HBox caja = new HBox(8, editar, eliminar);
            {
                editar.getStyleClass().addAll("btn-secondary", "btn-small");
                eliminar.getStyleClass().addAll("btn-danger", "btn-small");
                editar.setMinWidth(Region.USE_PREF_SIZE);
                eliminar.setMinWidth(Region.USE_PREF_SIZE);
                caja.setAlignment(Pos.CENTER_LEFT);
                editar.setOnAction(e -> editarInvitacion(getItem()));
                eliminar.setOnAction(e -> eliminarInvitacion(getItem()));
            }
            @Override
            protected void updateItem(FilaInvitacion fila, boolean vacio) {
                super.updateItem(fila, vacio);
                setGraphic(vacio || fila == null ? null : caja);
                setText(null);
            }
        });

        tabla.getColumns().addAll(java.util.List.of(id, evento, lugar, fecha, estado, acciones));
    }

    private TableColumn<FilaInvitacion, String> columnaTexto(String titulo,
            java.util.function.Function<FilaInvitacion, String> valor, double min, double pref) {
        TableColumn<FilaInvitacion, String> columna = new TableColumn<>(titulo);
        columna.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(valor.apply(c.getValue())));
        columna.setMinWidth(min);
        columna.setPrefWidth(pref);
        // Si la columna queda angosta, el texto baja a una segunda línea en vez de cortarse con "...".
        columna.setCellFactory(col -> {
            TableCell<FilaInvitacion, String> celda = new TableCell<>() {
                @Override
                protected void updateItem(String texto, boolean vacio) {
                    super.updateItem(texto, vacio);
                    setText(vacio ? null : texto);
                }
            };
            celda.setWrapText(true);
            return celda;
        });
        return columna;
    }

    private VBox crearTarjetaDetalles() {
        detalleNombre.getStyleClass().add("section-title");
        detalleNombre.setWrapText(true);
        Button ver = new Button("Ver detalles del evento");
        ver.setId("verDetallesEvento");
        ver.getStyleClass().add("btn-secondary");
        ver.setMaxWidth(Double.MAX_VALUE);
        ver.setWrapText(true);
        ver.setOnAction(e -> mostrarAviso("Detalles del evento",
                "Próximamente podrás ver toda la información de \"" + eventoMostrado().nombre() + "\"."));
        VBox tarjeta = new VBox(14, etiqueta("Detalles del evento", "card-title"), detalleNombre,
                filaResumen("Fecha", detalleFecha), new Separator(),
                filaResumen("Lugar", detalleLugar), new Separator(),
                filaResumen("Asistentes", detalleAsistentes), ver);
        tarjeta.getStyleClass().add("info-card");
        return tarjeta;
    }

    private VBox crearTarjetaAcciones() {
        Label texto = etiqueta("Gestiona las invitaciones de manera rápida y sencilla.", "muted");
        texto.setWrapText(true);
        Button nueva = new Button("+ Nueva invitación");
        nueva.setId("nuevaInvitacionRapida");
        nueva.getStyleClass().add("btn-primary");
        nueva.setMaxWidth(Double.MAX_VALUE);
        nueva.setOnAction(e -> nuevaInvitacion());
        Button importar = new Button("Importar lista");
        importar.setId("importarLista");
        importar.getStyleClass().add("btn-secondary");
        importar.setMaxWidth(Double.MAX_VALUE);
        importar.setOnAction(e -> mostrarAviso("Importar lista",
                "La importación de listas de invitados estará disponible próximamente."));
        VBox tarjeta = new VBox(14, etiqueta("Acciones rápidas", "card-title"), texto, nueva, importar);
        tarjeta.getStyleClass().add("info-card");
        return tarjeta;
    }

    // ------------------------------------------------------------------
    // Comportamiento
    // ------------------------------------------------------------------
    private void actualizarVista() {
        String eventoElegido = selectorEvento.getValue();
        String[] palabras = normalizar(buscador.getText()).split("\\s+");
        filtradas.setPredicate(fila -> perteneceAlEvento(fila, eventoElegido) && coincide(fila, palabras));

        int n = filtradas.size();
        cantidadRegistros.setText(n + (n == 1 ? " registro" : " registros"));

        int total = 0, enviadas = 0, pendientes = 0, respuestas = 0;
        for (FilaInvitacion fila : invitaciones) {
            if (!perteneceAlEvento(fila, eventoElegido)) continue;
            total++;
            String estado = fila.estadoRespuesta() == null ? "" : fila.estadoRespuesta().toUpperCase(Locale.ROOT);
            switch (estado) {
                case "ENVIADA" -> enviadas++;
                case "PENDIENTE" -> pendientes++;
                case "CONFIRMADA", "ACEPTADA", "RECHAZADA" -> respuestas++;
                default -> { }
            }
        }
        totalValor.setText(String.valueOf(total));
        enviadasValor.setText(String.valueOf(enviadas));
        pendientesValor.setText(String.valueOf(pendientes));
        respuestasValor.setText(String.valueOf(respuestas));

        DetalleEvento evento = eventoMostrado();
        detalleNombre.setText(evento.nombre());
        detalleFecha.setText(evento.fecha());
        detalleLugar.setText(evento.lugar());
        detalleAsistentes.setText(evento.asistentes());
    }

    /** Cada palabra escrita debe aparecer en algún dato de la invitación. */
    private boolean coincide(FilaInvitacion fila, String[] palabras) {
        String texto = normalizar(textoBuscable(fila));
        for (String palabra : palabras) if (!palabra.isEmpty() && !texto.contains(palabra)) return false;
        return true;
    }

    private boolean perteneceAlEvento(FilaInvitacion fila, String evento) {
        return evento == null || TODOS.equals(evento) || evento.equals(fila.nombreEvento());
    }

    /** Evento del panel lateral: el seleccionado, o el primero si se ven todos. */
    private DetalleEvento eventoMostrado() {
        String elegido = selectorEvento.getValue();
        for (DetalleEvento evento : eventos) if (evento.nombre().equals(elegido)) return evento;
        return eventos.get(0);
    }

    private String textoBuscable(FilaInvitacion fila) {
        return fila.idInvitacion() + " " + fila.nombreEvento() + " " + fila.nombreLugar() + " "
                + fila.fechaEnvio().format(FORMATO_FECHA) + " " + fila.estadoRespuesta() + " "
                + (fila.mensaje() == null ? "" : fila.mensaje());
    }

    /** Minúsculas y sin tildes, para que "bogota" encuentre "Bogotá". */
    private static String normalizar(String texto) {
        if (texto == null) return "";
        String sinTildes = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT);
    }

    private static String claseEstado(String estado) {
        return switch (estado.toUpperCase(Locale.ROOT)) {
            case "ENVIADA" -> "status-enviada";
            case "CONFIRMADA", "ACEPTADA" -> "status-confirmada";
            case "RECHAZADA" -> "status-rechazada";
            default -> "status-pendiente";
        };
    }

    private void nuevaInvitacion() {
        String evento = selectorEvento.getValue();
        String destino = evento == null || TODOS.equals(evento) ? "el evento que elijas" : "\"" + evento + "\"";
        mostrarAviso("Nueva invitación",
                "El formulario para crear una invitación para " + destino + " estará disponible próximamente.");
    }

    private void editarInvitacion(FilaInvitacion fila) {
        if (fila == null) return;
        mostrarAviso("Editar invitación", "Seleccionaste la invitación #" + fila.idInvitacion() + ".\n\n"
                + descripcion(fila));
    }

    private void eliminarInvitacion(FilaInvitacion fila) {
        if (fila == null) return;
        mostrarAviso("Eliminar invitación", "Seleccionaste para eliminar la invitación #" + fila.idInvitacion()
                + ".\n\n" + descripcion(fila) + "\n\nLa eliminación real se habilitará al conectar la base de datos.");
    }

    private String descripcion(FilaInvitacion fila) {
        return "Evento: " + fila.nombreEvento() + "\nLugar: " + fila.nombreLugar()
                + "\nFecha de envío: " + fila.fechaEnvio().format(FORMATO_FECHA)
                + "\nEstado: " + fila.estadoRespuesta()
                + "\nMensaje: " + (fila.mensaje() == null ? "-" : fila.mensaje());
    }

    private void mostrarAviso(String titulo, String texto) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle(titulo);
        alerta.setHeaderText(titulo);
        alerta.setContentText(texto);
        if (tabla.getScene() != null) {
            alerta.initOwner(tabla.getScene().getWindow());
            URL css = getClass().getResource(CSS);
            if (css != null) alerta.getDialogPane().getStylesheets().add(css.toExternalForm());
        }
        alerta.showAndWait();
    }

    // ------------------------------------------------------------------
    // Utilidades (mismas que ViewEventos)
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

    private HBox filaResumen(String titulo, Label valor) {
        valor.getStyleClass().add("summary-value");
        valor.setWrapText(true);
        valor.setMinWidth(0);
        valor.setTextAlignment(javafx.scene.text.TextAlignment.RIGHT);
        Label nombre = etiqueta(titulo, "hint");
        nombre.setMinWidth(Region.USE_PREF_SIZE);
        HBox fila = new HBox(10, nombre, espacioHorizontal(), valor);
        fila.setAlignment(Pos.CENTER_LEFT);
        return fila;
    }
}
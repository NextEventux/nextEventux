package views;

import controllers.ControllerProveedores;
import entities.DisponibilidadServicio;
import entities.Servicio;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.Date;
import java.util.List;

public class ViewProveedores {

    private final ControllerProveedores controller;
    private final int idProveedor;
    private final Runnable abrirEventos;

    private final ListView<Servicio> listaServicios = new ListView<>();
    private final ListView<DisponibilidadServicio> listaDisponibilidades =
            new ListView<>();

    private final TextField nombre =
            campo("nombreServicio", "Ej. Fotografia premium");

    private final TextField tipo =
            campo("tipoServicio", "Ej. Servicio individual");

    private final TextField categoria =
            campo("categoriaServicio", "Ej. FOTOGRAFIA");

    private final TextField precio =
            campo("precioServicio", "Ej. 850000");

    private final TextField ciudad =
            campo("ciudadServicio", "Ej. Bogota");

    private final TextField anticipacion =
            campo("anticipacionServicio", "Ej. 5");

    private final TextField cantidad =
            campo("cantidadServicio", "Opcional");

    private final TextArea descripcion = new TextArea();

    private final DatePicker fecha = new DatePicker();

    private final TextField horaInicio =
            campo("horaInicioServicio", "09:00");

    private final TextField horaFin =
            campo("horaFinServicio", "13:00");

    private final Label estadoServicio =
            new Label("Nuevo servicio");

    public ViewProveedores(
            ControllerProveedores controller,
            int idProveedor,
            Runnable abrirEventos) {

        this.controller = controller;
        this.idProveedor = idProveedor;
        this.abrirEventos = abrirEventos;

        descripcion.setPromptText(
                "Describe brevemente el servicio");

        descripcion.setWrapText(true);
        descripcion.setPrefRowCount(3);

        fecha.setEditable(false);
        fecha.setMaxWidth(Double.MAX_VALUE);
    }

    public Parent crearPantalla() {

        BorderPane raiz = new BorderPane();
        raiz.getStyleClass().add("app");

        VBox sidebar = crearMenu();
        raiz.setLeft(sidebar);

        BorderPane principal = new BorderPane();

        principal.setTop(
                crearBarraSuperior());

        VBox contenido = new VBox(30);
        contenido.getStyleClass().add("content");
        contenido.setMaxWidth(1320);

        VBox titulo = new VBox(
                8,
                etiqueta(
                        "Gestionar servicios",
                        "page-title"),
                etiqueta(
                        "Administra los servicios que ofreces y sus horarios disponibles.",
                        "muted"));

        HBox encabezado = new HBox(
                20,
                titulo,
                espacioHorizontal(),
                etiqueta(
                        "● Proveedor activo",
                        "status"));

        encabezado.setAlignment(
                Pos.CENTER_LEFT);

        GridPane columnas = new GridPane();

        columnas.setHgap(25);
        columnas.setVgap(25);

        ColumnConstraints izquierda =
                new ColumnConstraints();

        izquierda.setHgrow(
                Priority.ALWAYS);

        ColumnConstraints derecha =
                new ColumnConstraints(290);

        columnas
                .getColumnConstraints()
                .addAll(
                        izquierda,
                        derecha);

        VBox gestion = crearGestionServicios();
        VBox panel = crearPanelDerecho();

        columnas.add(
                gestion,
                0,
                0);

        columnas.add(
                panel,
                1,
                0);

        GridPane.setHgrow(
                gestion,
                Priority.ALWAYS);

        gestion.setMinWidth(0);

        contenido
                .getChildren()
                .addAll(
                        encabezado,
                        columnas);

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

        configurarListas();
        recargarServicios();

        raiz.widthProperty().addListener(
                (observable, anterior, actual) -> {

                    boolean ancho =
                            actual.doubleValue() >= 1200;

                    if (ancho
                            != (GridPane.getColumnIndex(panel) == 1)) {

                        columnas
                                .getChildren()
                                .clear();

                        columnas
                                .getColumnConstraints()
                                .setAll(izquierda);

                        if (ancho) {
                            columnas
                                    .getColumnConstraints()
                                    .add(derecha);
                        }

                        columnas.add(
                                gestion,
                                0,
                                0);

                        columnas.add(
                                panel,
                                ancho ? 1 : 0,
                                ancho ? 0 : 1);

                        GridPane.setHgrow(
                                panel,
                                Priority.ALWAYS);
                    }

                    sidebar.setVisible(
                            actual.doubleValue() >= 1000);

                    sidebar.setManaged(
                            sidebar.isVisible());
                });

        return raiz;
    }

    private VBox crearMenu() {

        VBox menu = new VBox(7);
        menu.getStyleClass().add("sidebar");
        menu.setPrefWidth(250);
        menu.setMinWidth(250);

        VBox marca = new VBox(
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

        menu.getChildren().addAll(
                marca,
                etiqueta(
                        "MENÚ PRINCIPAL",
                        "menu-title"));

        String[] opciones = {
                "⌂  Inicio",
                "▦  Mis eventos",
                "＋  Crear evento",
                "♧  Proveedores",
                "♙  Invitados",
                "⚙  Configuración"
        };

        for (String opcion : opciones) {

            Label item =
                    etiqueta(
                            opcion,
                            "menu-item");

            item.setMaxWidth(
                    Double.MAX_VALUE);

            if (opcion.contains("Proveedores")) {
                item.getStyleClass().add("active");
            }

            if (opcion.contains("Crear evento")
                    && abrirEventos != null) {

                item.setOnMouseClicked(
                        event -> abrirEventos.run());
            }

            menu.getChildren().add(item);
        }

        Region espacio = new Region();

        VBox.setVgrow(
                espacio,
                Priority.ALWAYS);

        VBox perfil = new VBox(
                4,
                etiqueta(
                        "Proveedor",
                        "profile-name"),
                etiqueta(
                        "Mi cuenta",
                        "hint"));

        HBox cuenta = new HBox(
                12,
                etiqueta(
                        "PR",
                        "avatar"),
                perfil);

        cuenta.setAlignment(
                Pos.CENTER_LEFT);

        cuenta
                .getStyleClass()
                .add("sidebar-bottom");

        menu.getChildren().addAll(
                espacio,
                cuenta);

        return menu;
    }

    private HBox crearBarraSuperior() {

        HBox barra = new HBox(
                18,
                etiqueta(
                        "Inicio / Proveedores /",
                        "breadcrumb"),
                etiqueta(
                        "Gestionar servicios",
                        "breadcrumb-current"),
                espacioHorizontal(),
                etiqueta(
                        "♧",
                        "notification"),
                etiqueta(
                        "Proveedor",
                        "user-badge"));

        barra.setAlignment(
                Pos.CENTER_LEFT);

        barra
                .getStyleClass()
                .add("topbar");

        return barra;
    }

    private VBox crearGestionServicios() {

        VBox tarjeta = new VBox(22);
        tarjeta.getStyleClass().add("card");

        HBox titulo = new HBox(
                13,
                etiqueta(
                        "♧",
                        "heading-icon"),
                etiqueta(
                        "Servicios del proveedor",
                        "card-title"));

        titulo.setAlignment(
                Pos.CENTER_LEFT);

        Label ayuda = etiqueta(
                "Crea, modifica, publica o retira tus servicios y administra su disponibilidad.",
                "muted");

        ayuda.setWrapText(true);

        HBox cuerpo = new HBox(22);
        cuerpo
                .getStyleClass()
                .add("service-layout");

        VBox servicios = crearListaServicios();
        VBox formulario = crearFormulario();

        HBox.setHgrow(
                formulario,
                Priority.ALWAYS);

        cuerpo
                .getChildren()
                .addAll(
                        servicios,
                        formulario);

        tarjeta
                .getChildren()
                .addAll(
                        titulo,
                        ayuda,
                        cuerpo);

        return tarjeta;
    }

    private VBox crearListaServicios() {

        Label titulo = etiqueta(
                "Mis servicios",
                "section-title");

        listaServicios
                .getStyleClass()
                .add("service-list");

        listaServicios.setPrefWidth(270);
        listaServicios.setMinWidth(230);
        listaServicios.setPrefHeight(500);

        Button nuevo =
                new Button("+ Nuevo servicio");

        nuevo
                .getStyleClass()
                .add("btn-secondary");

        nuevo.setMaxWidth(
                Double.MAX_VALUE);

        nuevo.setOnAction(
                event -> {

                    listaServicios
                            .getSelectionModel()
                            .clearSelection();

                    limpiarFormulario();
                });

        VBox caja = new VBox(
                12,
                titulo,
                listaServicios,
                nuevo);

        caja.setPrefWidth(270);
        caja.setMinWidth(230);

        return caja;
    }

    private VBox crearFormulario() {

        Label titulo = etiqueta(
                "Información del servicio",
                "section-title");

        estadoServicio
                .getStyleClass()
                .add("service-state");

        GridPane datos = grilla();

        datos.add(
                grupo(
                        "Nombre *",
                        nombre,
                        null),
                0,
                0);

        datos.add(
                grupo(
                        "Tipo *",
                        tipo,
                        null),
                1,
                0);

        datos.add(
                grupo(
                        "Categoría *",
                        categoria,
                        null),
                0,
                1);

        datos.add(
                grupo(
                        "Precio base *",
                        precio,
                        "Valor en COP"),
                1,
                1);

        datos.add(
                grupo(
                        "Ciudad / zona *",
                        ciudad,
                        null),
                0,
                2);

        datos.add(
                grupo(
                        "Anticipación mínima *",
                        anticipacion,
                        "Número de días"),
                1,
                2);

        datos.add(
                grupo(
                        "Cantidad máxima",
                        cantidad,
                        "Campo opcional"),
                0,
                3);

        VBox grupoDescripcion =
                grupo(
                        "Descripción *",
                        descripcion,
                        null);

        datos.add(
                grupoDescripcion,
                0,
                4,
                2,
                1);

        Button guardar =
                new Button("Guardar");

        guardar
                .getStyleClass()
                .add("btn-primary");

        guardar.setOnAction(
                event -> guardarServicio());

        Button publicar =
                new Button("Publicar");

        publicar
                .getStyleClass()
                .add("btn-primary");

        publicar.setOnAction(
                event -> publicarServicio());

        Button retirar =
                new Button("Retirar");

        retirar
                .getStyleClass()
                .add("btn-danger");

        retirar.setOnAction(
                event -> retirarServicio());

        HBox acciones = new HBox(
                10,
                guardar,
                publicar,
                retirar);

        acciones.setAlignment(
                Pos.CENTER_RIGHT);

        VBox disponibilidad =
                crearDisponibilidad();

        VBox formulario = new VBox(
                18,
                titulo,
                estadoServicio,
                datos,
                acciones,
                new Separator(),
                disponibilidad);

        formulario
                .getStyleClass()
                .add("service-form");

        return formulario;
    }

    private VBox crearDisponibilidad() {

        Label titulo = etiqueta(
                "Disponibilidad",
                "section-title");

        GridPane datos = grilla();

        datos.add(
                grupo(
                        "Fecha *",
                        fecha,
                        null),
                0,
                0);

        datos.add(
                grupo(
                        "Hora inicio *",
                        horaInicio,
                        "Formato HH:mm"),
                1,
                0);

        datos.add(
                grupo(
                        "Hora fin *",
                        horaFin,
                        "Formato HH:mm"),
                0,
                1);

        Button agregar =
                new Button(
                        "+ Agregar disponibilidad");

        agregar
                .getStyleClass()
                .add("btn-secondary");

        agregar.setOnAction(
                event -> agregarDisponibilidad());

        listaDisponibilidades
                .getStyleClass()
                .add("availability-list");

        listaDisponibilidades
                .setPrefHeight(150);

        Button eliminar =
                new Button(
                        "Eliminar seleccionada");

        eliminar
                .getStyleClass()
                .add("btn-danger");

        eliminar.setOnAction(
                event -> eliminarDisponibilidad());

        return new VBox(
                12,
                titulo,
                datos,
                agregar,
                listaDisponibilidades,
                eliminar);
    }

    private VBox crearPanelDerecho() {

        VBox resumen = new VBox(
                18,
                etiqueta(
                        "Resumen",
                        "card-title"),
                filaResumen(
                        "Proveedor",
                        "#" + idProveedor),
                new Separator(),
                filaResumen(
                        "Servicios",
                        "Gestionados en H2"),
                new Separator(),
                filaResumen(
                        "Estado",
                        "Activo"));

        resumen
                .getStyleClass()
                .add("info-card");

        VBox ayuda = new VBox(
                14,
                etiqueta(
                        "ⓘ Información importante",
                        "card-title"),
                etiqueta(
                        "Para publicar un servicio debes completar sus datos y registrar al menos una disponibilidad.",
                        "muted"));

        ayuda
                .getStyleClass()
                .addAll(
                        "info-card",
                        "highlight");

        return new VBox(
                24,
                resumen,
                ayuda);
    }

    private HBox filaResumen(
            String texto,
            String valor) {

        return new HBox(
                10,
                etiqueta(
                        texto,
                        "muted"),
                espacioHorizontal(),
                etiqueta(
                        valor,
                        "summary-value"));
    }

    private void configurarListas() {

        listaServicios.setCellFactory(
                lista -> new ListCell<>() {

                    @Override
                    protected void updateItem(
                            Servicio servicio,
                            boolean empty) {

                        super.updateItem(
                                servicio,
                                empty);

                        if (empty || servicio == null) {
                            setText(null);
                            return;
                        }

                        setText(
                                servicio.getNombre()
                                + "\n$"
                                + String.format(
                                        "%,.0f",
                                        servicio.getPrecio())
                                + " · "
                                + servicio.getEstado());
                    }
                });

        listaServicios
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (observable, anterior, actual) -> {

                            if (actual != null) {
                                cargarServicio(actual);
                            }
                        });

        listaDisponibilidades.setCellFactory(
                lista -> new ListCell<>() {

                    @Override
                    protected void updateItem(
                            DisponibilidadServicio disponibilidad,
                            boolean empty) {

                        super.updateItem(
                                disponibilidad,
                                empty);

                        if (empty || disponibilidad == null) {
                            setText(null);
                            return;
                        }

                        setText(
                                new java.text.SimpleDateFormat("dd/MM/yyyy").format(disponibilidad.getFecha())
                                + "   "
                                + disponibilidad.getHorario());
                    }
                });
    }

    private void guardarServicio() {

        try {

            Servicio seleccionado =
                    listaServicios
                            .getSelectionModel()
                            .getSelectedItem();

            Servicio datos =
                    construirServicio();

            if (seleccionado == null) {

                datos.setEstado("BORRADOR");
                controller.crearServicio(datos);

                mostrarInformacion(
                        "Servicio creado",
                        "El servicio se guardó correctamente.");

            } else {

                datos.setEstado(
                        seleccionado.getEstado());

                controller.modificarServicio(
                        seleccionado.getIdServicio(),
                        datos);

                mostrarInformacion(
                        "Servicio actualizado",
                        "Los cambios se guardaron correctamente.");
            }

            int idGuardado =
                    seleccionado == null
                            ? datos.getIdServicio()
                            : seleccionado.getIdServicio();

            recargarServicios();
            seleccionarServicio(idGuardado);

        } catch (Exception e) {
            mostrarError(e.getMessage());
        }
    }

    private Servicio construirServicio() {

        validarCampo(
                nombre,
                "El nombre es obligatorio.");

        validarCampo(
                tipo,
                "El tipo es obligatorio.");

        validarCampo(
                categoria,
                "La categoria es obligatoria.");

        validarCampo(
                precio,
                "El precio es obligatorio.");

        validarCampo(
                ciudad,
                "La ciudad o zona es obligatoria.");

        validarCampo(
                anticipacion,
                "La anticipacion minima es obligatoria.");

        if (descripcion
                .getText()
                .trim()
                .isEmpty()) {

            throw new IllegalArgumentException(
                    "La descripcion es obligatoria.");
        }

        double valorPrecio;
        int diasAnticipacion;

        try {

            valorPrecio =
                    Double.parseDouble(
                            precio
                                    .getText()
                                    .trim());

            diasAnticipacion =
                    Integer.parseInt(
                            anticipacion
                                    .getText()
                                    .trim());

        } catch (NumberFormatException e) {

            throw new IllegalArgumentException(
                    "Revisa el precio y la anticipacion minima.");
        }

        Integer maximo = null;

        if (!cantidad
                .getText()
                .trim()
                .isEmpty()) {

            try {

                maximo =
                        Integer.valueOf(
                                cantidad
                                        .getText()
                                        .trim());

            } catch (NumberFormatException e) {

                throw new IllegalArgumentException(
                        "La cantidad maxima debe ser un numero.");
            }
        }

        Servicio servicio = new Servicio();

        servicio.setIdProveedor(
                idProveedor);

        servicio.setNombre(
                nombre.getText().trim());

        servicio.setTipo(
                tipo.getText().trim());

        servicio.setCategoria(
                categoria.getText().trim());

        servicio.setPrecio(
                valorPrecio);

        servicio.setCiudadZona(
                ciudad.getText().trim());

        servicio.setAnticipacionMinima(
                diasAnticipacion);

        servicio.setCantidadMaximaPersonas(
                maximo);

        servicio.setDescripcion(
                descripcion
                        .getText()
                        .trim());

        return servicio;
    }

    private void publicarServicio() {

        Servicio servicio =
                servicioSeleccionado();

        if (servicio == null) {
            return;
        }

        try {

            controller.publicarServicio(
                    servicio.getIdServicio());

            mostrarInformacion(
                    "Servicio publicado",
                    "El servicio ahora está disponible.");

            recargarServicios();

            seleccionarServicio(
                    servicio.getIdServicio());

        } catch (Exception e) {
            mostrarError(e.getMessage());
        }
    }

    private void retirarServicio() {

        Servicio servicio =
                servicioSeleccionado();

        if (servicio == null) {
            return;
        }

        try {

            controller.retirarServicio(
                    servicio.getIdServicio());

            mostrarInformacion(
                    "Servicio retirado",
                    "El servicio fue retirado correctamente.");

            recargarServicios();

            seleccionarServicio(
                    servicio.getIdServicio());

        } catch (Exception e) {
            mostrarError(e.getMessage());
        }
    }

    private void agregarDisponibilidad() {

        Servicio servicio =
                servicioSeleccionado();

        if (servicio == null) {
            return;
        }

        if (fecha.getValue() == null) {

            mostrarError(
                    "Selecciona una fecha.");

            return;
        }

        String inicio =
                horaInicio
                        .getText()
                        .trim();

        String fin =
                horaFin
                        .getText()
                        .trim();

        if (inicio.isEmpty()
                || fin.isEmpty()) {

            mostrarError(
                    "Debes indicar la hora inicial y final.");

            return;
        }

        try {

            controller.registrarDisponibilidadServicio(
                    servicio.getIdServicio(),
                    Date.valueOf(
                            fecha.getValue()),
                    inicio + " - " + fin);

            cargarDisponibilidades(
                    servicio.getIdServicio());

            fecha.setValue(null);
            horaInicio.clear();
            horaFin.clear();

            mostrarInformacion(
                    "Disponibilidad agregada",
                    "El horario quedó registrado.");

        } catch (Exception e) {
            mostrarError(e.getMessage());
        }
    }

    private void eliminarDisponibilidad() {

        DisponibilidadServicio disponibilidad =
                listaDisponibilidades
                        .getSelectionModel()
                        .getSelectedItem();

        if (disponibilidad == null) {

            mostrarError(
                    "Selecciona una disponibilidad.");

            return;
        }

        try {

            controller.eliminarDisponibilidad(
                    disponibilidad
                            .getIdDisponibilidad());

            Servicio servicio =
                    listaServicios
                            .getSelectionModel()
                            .getSelectedItem();

            if (servicio != null) {

                cargarDisponibilidades(
                        servicio.getIdServicio());
            }

        } catch (Exception e) {
            mostrarError(e.getMessage());
        }
    }

    private void cargarServicio(
            Servicio servicio) {

        nombre.setText(
                servicio.getNombre());

        tipo.setText(
                seguro(servicio.getTipo()));

        categoria.setText(
                seguro(servicio.getCategoria()));

        precio.setText(
                String.valueOf(
                        servicio.getPrecio()));

        ciudad.setText(
                seguro(
                        servicio.getCiudadZona()));

        anticipacion.setText(
                String.valueOf(
                        servicio.getAnticipacionMinima()));

        cantidad.setText(
                servicio.getCantidadMaximaPersonas() == null
                        ? ""
                        : String.valueOf(
                                servicio
                                        .getCantidadMaximaPersonas()));

        descripcion.setText(
                seguro(
                        servicio.getDescripcion()));

        estadoServicio.setText(
                "Estado: "
                + seguro(
                        servicio.getEstado()));

        cargarDisponibilidades(
                servicio.getIdServicio());
    }

    private void cargarDisponibilidades(
            int idServicio) {

        List<DisponibilidadServicio> disponibilidades =
                controller
                        .listarDisponibilidadesServicio(
                                idServicio);

        listaDisponibilidades
                .getItems()
                .setAll(disponibilidades);
    }

    private void recargarServicios() {

        try {

            listaServicios
                    .getItems()
                    .setAll(
                            controller
                                    .listarServiciosProveedor(
                                            idProveedor));

        } catch (Exception e) {
            mostrarError(e.getMessage());
        }
    }

    private void seleccionarServicio(
            int idServicio) {

        for (Servicio servicio
                : listaServicios.getItems()) {

            if (servicio.getIdServicio()
                    == idServicio) {

                listaServicios
                        .getSelectionModel()
                        .select(servicio);

                break;
            }
        }
    }

    private Servicio servicioSeleccionado() {

        Servicio servicio =
                listaServicios
                        .getSelectionModel()
                        .getSelectedItem();

        if (servicio == null) {

            mostrarError(
                    "Primero selecciona un servicio.");
        }

        return servicio;
    }

    private void limpiarFormulario() {

        nombre.clear();
        tipo.clear();
        categoria.clear();
        precio.clear();
        ciudad.clear();
        anticipacion.clear();
        cantidad.clear();
        descripcion.clear();

        fecha.setValue(null);
        horaInicio.clear();
        horaFin.clear();

        listaDisponibilidades
                .getItems()
                .clear();

        estadoServicio.setText(
                "Nuevo servicio");
    }

    private GridPane grilla() {

        GridPane grid = new GridPane();

        grid.setHgap(22);
        grid.setVgap(16);

        ColumnConstraints columna1 =
                new ColumnConstraints();

        ColumnConstraints columna2 =
                new ColumnConstraints();

        columna1.setPercentWidth(50);
        columna2.setPercentWidth(50);

        columna1.setHgrow(
                Priority.ALWAYS);

        columna2.setHgrow(
                Priority.ALWAYS);

        grid
                .getColumnConstraints()
                .addAll(
                        columna1,
                        columna2);

        return grid;
    }

    private VBox grupo(
            String titulo,
            Control control,
            String ayuda) {

        Label etiqueta =
                etiqueta(
                        titulo,
                        "field-label");

        VBox grupo = new VBox(
                7,
                etiqueta,
                control);

        if (ayuda != null) {

            grupo
                    .getChildren()
                    .add(
                            etiqueta(
                                    ayuda,
                                    "hint"));
        }

        control.setMaxWidth(
                Double.MAX_VALUE);

        return grupo;
    }

    private static TextField campo(
            String id,
            String prompt) {

        TextField campo =
                new TextField();

        campo.setId(id);
        campo.setPromptText(prompt);

        return campo;
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

        Region espacio = new Region();

        HBox.setHgrow(
                espacio,
                Priority.ALWAYS);

        return espacio;
    }

    private void validarCampo(
            TextField campo,
            String mensaje) {

        if (campo.getText() == null
                || campo
                        .getText()
                        .trim()
                        .isEmpty()) {

            throw new IllegalArgumentException(
                    mensaje);
        }
    }

    private String seguro(
            String valor) {

        return valor == null
                ? ""
                : valor;
    }

    private void mostrarError(
            String mensaje) {

        Alert alert =
                new Alert(
                        Alert.AlertType.ERROR);

        alert.setTitle(
                "NextEventux");

        alert.setHeaderText(
                "No se pudo completar la operación");

        alert.setContentText(
                mensaje == null
                        ? "Ocurrió un error."
                        : mensaje);

        alert.showAndWait();
    }

    private void mostrarInformacion(
            String titulo,
            String mensaje) {

        Alert alert =
                new Alert(
                        Alert.AlertType.INFORMATION);

        alert.setTitle(
                "NextEventux");

        alert.setHeaderText(titulo);
        alert.setContentText(mensaje);

        alert.showAndWait();
    }
}

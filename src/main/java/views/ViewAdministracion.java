
package views;

import views.eventos.ViewEventos;

import controllers.ControllerAdministracion;
import entities.Contratacion;
import entities.Organizador;
import entities.ResenaOrganizador;
import entities.SesionUsuario;
import services.OrganizadorNoDisponibleException;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Vista del caso de uso CU-026: Contratar organizador.
 *
 * El cliente busca organizadores verificados, compara sus perfiles y
 * reseñas, y envía una solicitud que queda Pendiente. También consulta
 * el estado de las solicitudes que ya envió.
 */
public class ViewAdministracion {

    /**
     * Criterios con los que se obtuvo la lista actual. La solicitud usa
     * estos valores, no los que el cliente haya editado después sin buscar.
     */
    private record BusquedaVigente(
            String tipoEvento,
            String ciudad,
            LocalDate fecha,
            BigDecimal presupuesto,
            String criterios) {
    }

    private final ControllerAdministracion controller;
    private final SesionUsuario sesion;
    private final Runnable cerrarSesion;
    private TabPane pestanasPrincipal;
    private Tab pestanaMisSolicitudes;

    private final TextField tipoEvento = new TextField();
    private final TextField ciudad = new TextField();
    private final DatePicker fechaAproximada = new DatePicker();
    private final TextField presupuesto = new TextField();
    private final TextField criterios = new TextField();

    private final ListView<Organizador> listaOrganizadores =
            new ListView<>();

    private final ListView<Contratacion> listaContrataciones =
            new ListView<>();

    private final Label detalleNombre =
            new Label("Selecciona un organizador");

    private final Label detalleEmpresa = new Label();
    private final Label detalleCiudad = new Label();
    private final Label detalleEspecialidad = new Label();
    private final Label detallePortafolio = new Label();
    private final Label detalleTarifa = new Label();
    private final Label detalleCalificacion = new Label();
    private final Label tituloResenas = new Label("Reseñas");
    private final Label detalleResenas = new Label();

    private final Label disponibilidad = new Label(
            "Busca organizadores para consultar su disponibilidad.");

    private final Button solicitarContratacion =
            new Button("Solicitar contratación");

    private Organizador organizadorSeleccionado;
    private BusquedaVigente busquedaVigente;

    private Label menuBuscar;
    private Label menuSolicitudes;

    public ViewAdministracion(
            ControllerAdministracion controller) {
        this(controller, null, null);
    }

    public ViewAdministracion(
            ControllerAdministracion controller,
            SesionUsuario sesion) {
        this(controller, sesion, null);
    }

    /**
     * @param cerrarSesion acción de «Cerrar sesión»; si es null no se muestra
     */
    public ViewAdministracion(
            ControllerAdministracion controller,
            SesionUsuario sesion,
            Runnable cerrarSesion) {

        if (controller == null) {
            throw new IllegalArgumentException(
                    "El controlador de administración es obligatorio.");
        }

        this.controller = controller;
        this.sesion = sesion;
        this.cerrarSesion = cerrarSesion;

        configurarFormulario();
    }

    private void configurarFormulario() {

        tipoEvento.setId("tipoEvento");
        tipoEvento.setPromptText(
                "Ej. Boda, cumpleaños o evento empresarial");

        ciudad.setId("ciudad");
        ciudad.setPromptText("Ciudad donde se realizará el evento");

        fechaAproximada.setId("fechaAproximada");
        fechaAproximada.setPromptText("Fecha aproximada");
        fechaAproximada.setEditable(false);
        fechaAproximada.setMaxWidth(Double.MAX_VALUE);
        fechaAproximada.setDayCellFactory(picker -> new javafx.scene.control.DateCell() {
            @Override
            public void updateItem(LocalDate fecha, boolean vacia) {
                super.updateItem(fecha, vacia);
                setDisable(vacia || fecha.isBefore(LocalDate.now()));
            }
        });

        presupuesto.setId("presupuestoEstimado");
        presupuesto.setPromptText("Ej. 2500000");

        criterios.setId("criteriosBusqueda");
        criterios.setPromptText(
                "Palabra clave opcional, por ejemplo: bodas, empresarial");

        listaOrganizadores.setId("listaOrganizadores");
        listaOrganizadores.setPlaceholder(
                new Label("Completa los criterios y pulsa Buscar."));
        listaOrganizadores.setPrefHeight(390);
        listaOrganizadores.setMinHeight(250);

        listaContrataciones.setId("listaContrataciones");
        listaContrataciones.setPlaceholder(
                new Label("Todavía no tienes solicitudes registradas."));
        listaContrataciones.setPrefHeight(390);

        detalleNombre.getStyleClass().add("card-title");
        detalleNombre.setWrapText(true);

        detallePortafolio.setWrapText(true);
        disponibilidad.setWrapText(true);

        tituloResenas.getStyleClass().add("section-title");
        detalleResenas.setId("resenasOrganizador");
        detalleResenas.getStyleClass().add("muted");
        detalleResenas.setWrapText(true);

        solicitarContratacion.setId("solicitarContratacion");
        solicitarContratacion.getStyleClass().add("btn-primary");
        solicitarContratacion.setDisable(true);
        solicitarContratacion.setMaxWidth(Double.MAX_VALUE);
        solicitarContratacion.setOnAction(
                event -> enviarSolicitud());

        configurarListaOrganizadores();
        configurarListaContrataciones();
        limpiarPerfil();
    }

    public Parent crearPantalla() {

        BorderPane raiz = new BorderPane();
        raiz.getStyleClass().add("app");

        Tab buscar = new Tab(
                "Buscar organizadores",
                crearVistaBusqueda());
        buscar.setClosable(false);

        Tab solicitudes = new Tab(
                "Mis solicitudes",
                crearVistaContrataciones());
        solicitudes.setClosable(false);

        this.pestanaMisSolicitudes = solicitudes;

        TabPane pestanas = new TabPane(buscar, solicitudes);
        this.pestanasPrincipal = pestanas;

        pestanas.getStyleClass().add("publicacion");
        pestanas.setTabClosingPolicy(
                TabPane.TabClosingPolicy.UNAVAILABLE);

        VBox sidebar = crearMenu(buscar, solicitudes);

        pestanas.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, anterior, actual) -> {

                    actualizarMenuActivo(actual, buscar, solicitudes);

                    if (actual == solicitudes) {
                        mostrarContrataciones();
                    }
                });

        BorderPane principal = new BorderPane();
        principal.setTop(crearBarraSuperior());

        VBox contenido = new VBox(25);
        contenido.getStyleClass().add("content");
        contenido.setMaxWidth(1320);

        Label titulo = etiqueta(
                "Contratar organizador", "page-title");

        Label subtitulo = etiqueta(
                "Encuentra, compara y solicita la contratación "
                + "de un organizador para tu evento.",
                "muted");

        subtitulo.setWrapText(true);

        VBox encabezado = new VBox(8, titulo, subtitulo);

        contenido.getChildren().addAll(encabezado, pestanas);
        VBox.setVgrow(pestanas, Priority.ALWAYS);

        StackPane centrado = new StackPane(contenido);
        centrado.setAlignment(Pos.TOP_CENTER);

        ScrollPane scroll = new ScrollPane(centrado);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("content-scroll");

        principal.setCenter(scroll);

        raiz.setLeft(sidebar);
        raiz.setCenter(principal);

        // En pantallas estrechas se oculta la barra lateral.
        raiz.widthProperty().addListener(
                (observable, anterior, actual) -> {

                    boolean mostrarSidebar =
                            actual.doubleValue() >= 1000;

                    sidebar.setVisible(mostrarSidebar);
                    sidebar.setManaged(mostrarSidebar);
                });

        return raiz;
    }

    // ---------------------------------------------------------
    // BARRA LATERAL
    // ---------------------------------------------------------

    private VBox crearMenu(Tab buscar, Tab solicitudes) {

        VBox menu = new VBox(7);
        menu.getStyleClass().add("sidebar");
        menu.setPrefWidth(250);
        menu.setMinWidth(250);

        ImageView logo = new ImageView(
                new Image(
                        getClass()
                                .getResource(
                                        "/images/nexteventux-logo.png")
                                .toExternalForm()));

        // Recorta el margen blanco, igual que en ViewEventos.
        logo.setViewport(
                new Rectangle2D(75, 225, 1110, 725));

        logo.setFitWidth(210);
        logo.setFitHeight(138);
        logo.setPreserveRatio(true);
        logo.setSmooth(true);
        logo.setAccessibleText("Logo de NextEventux");

        Label descripcionMarca = etiqueta(
                "GESTIÓN DE EVENTOS", "brand-caption");

        VBox marca = new VBox(5, logo, descripcionMarca);
        marca.setAlignment(Pos.CENTER);
        VBox.setMargin(
                marca, new Insets(0, 0, 38, 0));

        Label tituloMenu = etiqueta(
                "MENÚ PRINCIPAL", "menu-title");

        menuBuscar = etiqueta(
                "♧  Buscar organizadores", "menu-item");

        menuBuscar.setMaxWidth(Double.MAX_VALUE);
        menuBuscar.getStyleClass().add("active");
        menuBuscar.setOnMouseClicked(
                event -> seleccionarPestana(buscar));

        menuSolicitudes = etiqueta(
                "▤  Mis solicitudes", "menu-item");

        menuSolicitudes.setMaxWidth(Double.MAX_VALUE);
        menuSolicitudes.setOnMouseClicked(
                event -> seleccionarPestana(solicitudes));

        Region espacio = new Region();
        VBox.setVgrow(espacio, Priority.ALWAYS);

        String nombre = nombreCliente();

        String inicial = nombre.substring(0, 1)
                .toUpperCase(Locale.ROOT);

        Label avatar = etiqueta(inicial, "avatar");

        VBox perfil = new VBox(
                4,
                etiqueta(nombre, "profile-name"),
                etiqueta(PresentacionContratacionOrganizador.rol(sesion), "hint"));

        // Mismo enlace que usa MenuCompartido en las demás pantallas.
        if (cerrarSesion != null) {
            Label salir = etiqueta("Cerrar sesión", "logout-link");
            salir.setId("cerrarSesionCliente");
            salir.setStyle("-fx-cursor: hand;");
            salir.setOnMouseClicked(event -> cerrarSesion.run());
            perfil.getChildren().add(salir);
        }

        HBox cuenta = new HBox(12, avatar, perfil);
        cuenta.setAlignment(Pos.CENTER_LEFT);
        cuenta.getStyleClass().add("sidebar-bottom");

        menu.getChildren().addAll(
                marca,
                tituloMenu,
                menuBuscar,
                menuSolicitudes,
                espacio,
                cuenta);

        return menu;
    }

    private HBox crearBarraSuperior() {

        Label ruta = etiqueta(
                "Inicio /", "breadcrumb");

        Label actual = etiqueta(
                "Contratar organizador", "breadcrumb-current");

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);

        Label usuario = etiqueta(
                nombreCliente() + " · CLIENTE", "user-badge");

        HBox barra = new HBox(
                18, ruta, actual, espacio, usuario);

        barra.setAlignment(Pos.CENTER_LEFT);
        barra.getStyleClass().add("topbar");

        return barra;
    }

    private String nombreCliente() {

        return sesion == null
                || sesion.getNombre() == null
                || sesion.getNombre().isBlank()
                ? "Cliente"
                : sesion.getNombre();
    }

    private void seleccionarPestana(Tab pestana) {

        if (pestana.getTabPane() != null) {
            pestana.getTabPane()
                    .getSelectionModel()
                    .select(pestana);
        }
    }

    private void actualizarMenuActivo(
            Tab actual, Tab buscar, Tab solicitudes) {

        if (menuBuscar == null || menuSolicitudes == null) {
            return;
        }

        menuBuscar.getStyleClass().remove("active");
        menuSolicitudes.getStyleClass().remove("active");

        if (actual == buscar) {
            menuBuscar.getStyleClass().add("active");
        } else if (actual == solicitudes) {
            menuSolicitudes.getStyleClass().add("active");
        }
    }

    // ---------------------------------------------------------
    // PESTAÑA DE BÚSQUEDA
    // ---------------------------------------------------------

    private Parent crearVistaBusqueda() {

        VBox contenido = new VBox(22);
        contenido.setPadding(new Insets(0, 0, 10, 0));

        VBox tarjetaFiltros = new VBox(20);
        tarjetaFiltros.getStyleClass().add("card");

        HBox tituloFiltros = new HBox(
                12,
                etiqueta("⌕", "heading-icon"),
                etiqueta("Información del evento", "card-title"));

        tituloFiltros.setAlignment(Pos.CENTER_LEFT);

        Label ayuda = etiqueta(
                "Completa los campos obligatorios para encontrar "
                + "organizadores compatibles con tu evento.",
                "muted");

        ayuda.setWrapText(true);

        GridPane formulario = crearGrilla();

        formulario.add(
                grupo("Tipo de evento *", tipoEvento),
                0, 0);

        formulario.add(
                grupo("Ciudad *", ciudad),
                1, 0);

        formulario.add(
                grupo("Fecha aproximada *", fechaAproximada),
                0, 1);

        formulario.add(
                grupo("Presupuesto estimado (COP) *", presupuesto),
                1, 1);

        formulario.add(
                grupo("Criterios adicionales", criterios),
                0, 2, 2, 1);

        Button buscar = new Button("⌕  Buscar organizadores");
        buscar.setId("buscarOrganizadores");
        buscar.getStyleClass().add("btn-primary");
        buscar.setDefaultButton(true);
        buscar.setOnAction(
                event -> mostrarOrganizadores());

        HBox acciones = new HBox(buscar);
        acciones.setAlignment(Pos.CENTER_RIGHT);

        tarjetaFiltros.getChildren().addAll(
                tituloFiltros,
                ayuda,
                formulario,
                new Separator(),
                acciones);

        Label tituloResultados = etiqueta(
                "Organizadores disponibles", "section-title");

        VBox resultados = new VBox(
                12, tituloResultados, listaOrganizadores);

        VBox.setVgrow(listaOrganizadores, Priority.ALWAYS);

        VBox perfil = crearPanelPerfil();

        HBox columnas = new HBox(24, resultados, perfil);
        HBox.setHgrow(resultados, Priority.ALWAYS);

        perfil.setPrefWidth(320);
        perfil.setMinWidth(280);
        perfil.setMaxWidth(360);

        columnas.widthProperty().addListener(
                (observable, anterior, actual) -> {

                    if (actual.doubleValue() < 650) {
                        perfil.setPrefWidth(
                                Math.max(220, actual.doubleValue() * 0.4));
                    } else {
                        perfil.setPrefWidth(320);
                    }
                });

        columnas.setAlignment(Pos.TOP_LEFT);

        contenido.getChildren().addAll(
                tarjetaFiltros, columnas);

        return contenido;
    }

    private VBox crearPanelPerfil() {

        Label encabezado = etiqueta(
                "Perfil del organizador", "card-title");

        VBox panel = new VBox(
                15,
                encabezado,
                detalleNombre,
                new Separator(),
                detalleEmpresa,
                detalleCiudad,
                detalleEspecialidad,
                detallePortafolio,
                detalleTarifa,
                detalleCalificacion,
                new Separator(),
                tituloResenas,
                detalleResenas,
                new Separator(),
                disponibilidad,
                solicitarContratacion);

        panel.getStyleClass().add("info-card");
        panel.setMinWidth(0);

        detalleNombre.setWrapText(true);
        detalleEmpresa.setWrapText(true);
        detalleCiudad.setWrapText(true);
        detalleEspecialidad.setWrapText(true);
        detalleTarifa.setWrapText(true);
        detalleCalificacion.setWrapText(true);

        return panel;
    }

    private void configurarListaOrganizadores() {

        listaOrganizadores.setCellFactory(
                lista -> new ListCell<>() {

                    @Override
                    protected void updateItem(
                            Organizador item, boolean empty) {

                        super.updateItem(item, empty);

                        setText(empty || item == null
                                ? null
                                : PresentacionContratacionOrganizador.tarjeta(item));
                    }
                });

        // A2: elegir otro organizador de la lista muestra su perfil.
        listaOrganizadores.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, anterior, actual) ->
                        mostrarPerfil(actual));
    }

    /**
     * Pasos 1 y 2 (y A1): valida los campos y busca organizadores.
     */
    public void mostrarOrganizadores() {

        String tipo = tipoEvento.getText().trim();
        String ciudadBusqueda = ciudad.getText().trim();
        LocalDate fecha = fechaAproximada.getValue();
        String presupuestoIngresado = presupuesto.getText().trim();

        if (tipo.isEmpty()
                || ciudadBusqueda.isEmpty()
                || fecha == null
                || presupuestoIngresado.isEmpty()) {

            mostrarMensaje(
                    "Completa los campos obligatorios antes de buscar:\n\n"
                    + "- Tipo de evento\n"
                    + "- Ciudad\n"
                    + "- Fecha aproximada\n"
                    + "- Presupuesto estimado");

            return;
        }

        BigDecimal valorPresupuesto;

        try {
            valorPresupuesto = leerPresupuesto(
                    presupuestoIngresado);

        } catch (NumberFormatException e) {
            mostrarMensaje(
                    "Ingresa un presupuesto válido, por ejemplo: 2500000.");
            return;
        }

        if (valorPresupuesto.compareTo(BigDecimal.ZERO) < 0) {
            mostrarMensaje("El presupuesto no puede ser negativo.");
            return;
        }

        BusquedaVigente busqueda = new BusquedaVigente(
                tipo,
                ciudadBusqueda,
                fecha,
                valorPresupuesto,
                criterios.getText().trim());

        try {
            List<Organizador> coincidencias = buscar(busqueda);

            if (coincidencias.isEmpty()) {
                mostrarMensaje(PresentacionContratacionOrganizador.SIN_RESULTADOS);
            }

        } catch (RuntimeException e) {
            mostrarMensaje(
                    mensajeDeError(
                            e,
                            "No fue posible buscar organizadores."));
        }
    }

    /** Ejecuta la búsqueda y deja la lista y el perfil listos. */
    private List<Organizador> buscar(BusquedaVigente busqueda) {

        Map<String, Object> filtros = new HashMap<>();

        filtros.put("tipoEvento", busqueda.tipoEvento());
        filtros.put("ciudad", busqueda.ciudad());
        filtros.put("fechaAproximada", busqueda.fecha());
        filtros.put("presupuestoEstimado", busqueda.presupuesto());

        if (!busqueda.criterios().isEmpty()) {
            filtros.put("criterios", busqueda.criterios());
        }

        busquedaVigente = null;
        listaOrganizadores.getItems().clear();
        limpiarPerfil();

        List<Organizador> coincidencias =
                controller.consultarOrganizadores(filtros);

        busquedaVigente = busqueda;
        listaOrganizadores.getItems().setAll(coincidencias);

        return coincidencias;
    }

    /**
     * E2: el organizador ya no puede recibir la solicitud. Se vuelve a
     * consultar la lista con los mismos criterios para que el cliente
     * seleccione otro.
     */
    private void volverALaLista(String mensaje) {

        BusquedaVigente busqueda = busquedaVigente;

        organizadorSeleccionado = null;
        listaOrganizadores.getSelectionModel().clearSelection();
        limpiarPerfil();

        if (busqueda != null) {
            try {
                buscar(busqueda);
            } catch (RuntimeException e) {
                listaOrganizadores.getItems().clear();
            }
        }

        mostrarMensaje(mensaje);
    }

    /**
     * Paso 3: consulta y presenta el perfil seleccionado.
     */
    private void mostrarPerfil(Organizador organizador) {

        organizadorSeleccionado = null;
        solicitarContratacion.setDisable(true);

        if (organizador == null) {
            limpiarPerfil();
            return;
        }

        try {
            Organizador perfil =
                    controller.seleccionarOrganizador(
                            organizador.id());

            List<ResenaOrganizador> resenas =
                    controller.consultarResenas(perfil.id());

            detalleNombre.setText(perfil.nombre());

            detalleEmpresa.setText(
                    "Empresa: "
                    + PresentacionContratacionOrganizador.texto(
                            perfil.empresa(), "No registrada"));

            detalleCiudad.setText(
                    "Ciudad: "
                    + PresentacionContratacionOrganizador.texto(
                            perfil.ciudad(), "No registrada"));

            detalleEspecialidad.setText(
                    "Especialidad: "
                    + PresentacionContratacionOrganizador.texto(
                            perfil.especialidad(), "No registrada"));

            detallePortafolio.setText(
                    "Portafolio: "
                    + PresentacionContratacionOrganizador.texto(
                            perfil.portafolio(), "No registrado"));

            detalleTarifa.setText(
                    "Tarifa base: " + FormatoUi.moneda(perfil.tarifaBase()));

            detalleCalificacion.setText(
                    "Calificación: "
                    + PresentacionContratacionOrganizador.calificacion(perfil));

            detalleResenas.setText(
                    PresentacionContratacionOrganizador.resenas(resenas));

            if (busquedaVigente == null) {
                disponibilidad.setText(
                        "Busca nuevamente para comprobar la disponibilidad.");
                return;
            }

            boolean disponible =
                    controller.verificarDisponibilidadOrganizador(
                            perfil.id(), busquedaVigente.fecha());

            if (!disponible) {
                disponibilidad.setText(
                        "No está disponible para la fecha indicada. "
                        + "Selecciona otro organizador.");
                return;
            }

            organizadorSeleccionado = perfil;
            disponibilidad.setText(
                    "Disponible para el "
                    + FormatoUi.fecha(busquedaVigente.fecha()) + ".");
            solicitarContratacion.setDisable(false);

        } catch (OrganizadorNoDisponibleException e) {
            // Se difiere para no modificar la lista durante su propio evento.
            javafx.application.Platform.runLater(
                    () -> volverALaLista(e.getMessage()));

        } catch (RuntimeException e) {
            limpiarPerfil();

            mostrarMensaje(
                    mensajeDeError(
                            e,
                            "No se pudo consultar el perfil."));
        }
    }

    // ---------------------------------------------------------
    // CONFIRMACIÓN Y ENVÍO
    // ---------------------------------------------------------

    /**
     * Pasos 4 y 5: muestra el resumen, pide confirmar y registra.
     */
    private void enviarSolicitud() {

        Organizador organizador = organizadorSeleccionado;
        BusquedaVigente busqueda = busquedaVigente;

        if (organizador == null || busqueda == null) {
            mostrarMensaje("Busca y selecciona un organizador.");
            return;
        }

        Alert confirmacion = new Alert(
                Alert.AlertType.CONFIRMATION);

        confirmacion.setTitle("Confirmar solicitud de contratación");
        confirmacion.setHeaderText(
                "Revisa la información antes de enviar la solicitud");
        confirmacion.setContentText(
                PresentacionContratacionOrganizador.resumen(
                        organizador,
                        busqueda.tipoEvento(),
                        busqueda.ciudad(),
                        busqueda.fecha(),
                        busqueda.presupuesto(),
                        busqueda.criterios()));

        Optional<ButtonType> respuesta =
                confirmacion.showAndWait();

        if (respuesta.isEmpty()
                || respuesta.get() != ButtonType.OK) {
            return;
        }

        try {
            Contratacion datos = new Contratacion(
                    0,
                    organizador.id(),
                    busqueda.tipoEvento(),
                    busqueda.ciudad(),
                    busqueda.fecha(),
                    busqueda.presupuesto(),
                    busqueda.criterios());

            controller.contratarOrganizador(
                    organizador.id(),
                    datos);

            organizadorSeleccionado = null;
            listaOrganizadores.getSelectionModel().clearSelection();
            limpiarPerfil();

            // Mostrar la solicitud registrada con su estado.
            mostrarContrataciones();

            if (pestanasPrincipal != null
                    && pestanaMisSolicitudes != null) {

                pestanasPrincipal.getSelectionModel()
                        .select(pestanaMisSolicitudes);
            }

            mostrarMensaje(
                    PresentacionContratacionOrganizador.confirmacion(
                            datos, organizador.nombre()));

        } catch (OrganizadorNoDisponibleException e) {
            // E2: no se registró nada; el cliente elige otro organizador.
            volverALaLista(e.getMessage());

        } catch (RuntimeException e) {
            // E1 y validaciones: los datos y la selección se conservan
            // para que el cliente pueda intentarlo nuevamente.
            mostrarMensaje(
                    mensajeDeError(
                            e,
                            "No fue posible completar la solicitud. "
                            + "Inténtalo nuevamente."));
        }
    }

    // ---------------------------------------------------------
    // PESTAÑA DE SOLICITUDES
    // ---------------------------------------------------------

    private Parent crearVistaContrataciones() {

        VBox contenido = new VBox(20);
        contenido.getStyleClass().add("content");

        Label titulo = etiqueta(
                "Mis solicitudes de contratación", "page-title");

        Label ayuda = etiqueta(
                "Consulta las solicitudes enviadas y el estado "
                + "registrado para cada organizador.",
                "muted");

        ayuda.setWrapText(true);

        Button actualizar = new Button("Actualizar solicitudes");
        actualizar.getStyleClass().add("btn-secondary");
        actualizar.setOnAction(
                event -> mostrarContrataciones());

        listaContrataciones.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(listaContrataciones, Priority.ALWAYS);

        contenido.getChildren().addAll(
                titulo,
                ayuda,
                listaContrataciones,
                actualizar);

        return contenido;
    }

    private void configurarListaContrataciones() {

        listaContrataciones.setCellFactory(
                lista -> new ListCell<>() {

                    @Override
                    protected void updateItem(
                            Contratacion item, boolean empty) {

                        super.updateItem(item, empty);

                        setText(empty || item == null
                                ? null
                                : PresentacionContratacionOrganizador.solicitud(item));
                    }
                });
    }

    /**
     * Consulta las solicitudes del cliente autenticado.
     * El controlador limita el resultado a su identificador.
     */
    public void mostrarContrataciones() {

        try {
            listaContrataciones.getItems().setAll(
                    controller.consultarContrataciones(null));

        } catch (RuntimeException e) {
            mostrarMensaje(
                    mensajeDeError(
                            e,
                            "No se pudieron consultar las solicitudes."));
        }
    }

    // ---------------------------------------------------------
    // COMPONENTES AUXILIARES
    // ---------------------------------------------------------

    private GridPane crearGrilla() {

        GridPane grilla = new GridPane();
        grilla.setHgap(23);
        grilla.setVgap(20);

        for (int i = 0; i < 2; i++) {
            ColumnConstraints columna =
                    new ColumnConstraints();

            columna.setPercentWidth(50);
            columna.setHgrow(Priority.ALWAYS);

            grilla.getColumnConstraints().add(columna);
        }

        return grilla;
    }

    private VBox grupo(
            String titulo, javafx.scene.Node control) {

        return new VBox(
                9,
                etiqueta(titulo, "field-label"),
                control);
    }

    private Label etiqueta(
            String texto, String estilo) {

        Label label = new Label(texto);
        label.getStyleClass().add(estilo);

        return label;
    }

    private BigDecimal leerPresupuesto(String texto) {

        if (texto == null || texto.isBlank()) {
            throw new NumberFormatException(
                    "El presupuesto está vacío.");
        }

        String normalizado = texto
                .trim()
                .replace("$", "")
                .replace(" ", "")
                .replace(".", "")
                .replace(",", ".");

        return new BigDecimal(normalizado);
    }

    private String mensajeDeError(
            RuntimeException e, String predeterminado) {

        return e.getMessage() == null
                || e.getMessage().isBlank()
                ? predeterminado
                : e.getMessage();
    }

    private void limpiarPerfil() {

        detalleNombre.setText(
                "Selecciona un organizador");

        detalleEmpresa.setText("");
        detalleCiudad.setText("");
        detalleEspecialidad.setText("");
        detallePortafolio.setText("");
        detalleTarifa.setText("");
        detalleCalificacion.setText("");
        detalleResenas.setText("");

        disponibilidad.setText(
                "Busca organizadores para consultar su disponibilidad.");

        solicitarContratacion.setDisable(true);
    }

    public void mostrarMensaje(String mensaje) {

        Alert alerta = new Alert(
                Alert.AlertType.INFORMATION);

        alerta.setTitle("Contratar organizador");
        alerta.setHeaderText(null);

        alerta.setContentText(
                mensaje == null || mensaje.isBlank()
                        ? "No fue posible completar la operación."
                        : mensaje);

        alerta.showAndWait();
    }
}

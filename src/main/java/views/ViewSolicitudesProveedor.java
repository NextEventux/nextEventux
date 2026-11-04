package views;

import java.util.List;

import controllers.ControllerSolicitudes;
import entities.SolicitudEjecucion;
import entities.SesionUsuario;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import views.components.ComponentesUi;
import views.components.MenuCompartido;
import views.solicitudes.DatosPropuesta;
import views.solicitudes.DetalleSolicitud;
import views.solicitudes.FormularioMotivo;
import views.solicitudes.FormularioPropuesta;
import views.solicitudes.HistorialPropuestas;
import views.solicitudes.PanelConversacion;
import views.solicitudes.PresentacionSolicitud;
import views.solicitudes.TarjetaSolicitud;

public class ViewSolicitudesProveedor {

    private final ControllerSolicitudes controller;
    private final int idProveedor;
    private final int idUsuario;
    private final SesionUsuario sesion;
    private final Runnable abrirServicios;
    private final Runnable abrirSolicitudes;
    private final Runnable abrirAvisos;
    private final Runnable cerrarSesion;

    private final ListView<SolicitudEjecucion> lista
            = new ListView<>();
    private final VBox detalle = new VBox(16);
    private final VBox zona = new VBox(16);

    public ViewSolicitudesProveedor(
            ControllerSolicitudes controller,
            int idProveedor,
            int idUsuario,
            SesionUsuario sesion,
            Runnable abrirServicios,
            Runnable abrirSolicitudes,
            Runnable cerrarSesion,
            Runnable abrirAvisos) {

        this.controller = controller;
        this.idProveedor = idProveedor;
        this.idUsuario = idUsuario;
        this.sesion = sesion;
        this.abrirServicios = abrirServicios;
        this.abrirSolicitudes = abrirSolicitudes;
        this.abrirAvisos = abrirAvisos;
        this.cerrarSesion = cerrarSesion;
    }

    public Parent crearPantalla() {

        BorderPane raiz = new BorderPane();
        raiz.getStyleClass().add("app");

        VBox menu = crearMenu();
        raiz.setLeft(menu);

        BorderPane principal = new BorderPane();
        principal.setTop(crearBarraSuperior());
        principal.setCenter(crearContenido());
        raiz.setCenter(principal);

        raiz.widthProperty().addListener(
                (observable, anterior, actual) -> {
                    boolean mostrarMenu = actual.doubleValue() >= 1000;
                    menu.setVisible(mostrarMenu);
                    menu.setManaged(mostrarMenu);
                });

        lista.getSelectionModel().selectedItemProperty().addListener(
                (observable, anterior, actual) -> mostrarDetalle(actual));

        recargarSolicitudes();

        return raiz;
    }

    /** Lista de solicitudes a la izquierda y detalle de la elegida a la derecha. */
    private HBox crearContenido() {

        lista.getStyleClass().add("cu12-list");
        lista.setPrefWidth(380);
        lista.setMinWidth(320);
        lista.setCellFactory(vista -> new ListCell<>() {

            @Override
            protected void updateItem(
                    SolicitudEjecucion solicitud,
                    boolean vacia) {

                super.updateItem(solicitud, vacia);

                setText(null);
                setGraphic(vacia || solicitud == null
                        ? null
                        : TarjetaSolicitud.crear(solicitud));
            }
        });

        ScrollPane desplazable = new ScrollPane(detalle);
        desplazable.setFitToWidth(true);
        desplazable.getStyleClass().add("panel-desplazable");
        HBox.setHgrow(desplazable, Priority.ALWAYS);

        VBox izquierda = new VBox(14,
                ComponentesUi.titulo("Solicitudes recibidas"),
                lista);
        VBox.setVgrow(lista, Priority.ALWAYS);

        HBox contenido = new HBox(24, izquierda, desplazable);
        contenido.getStyleClass().add("cu12-content");
        contenido.setPadding(new Insets(30));

        mostrarDetalle(null);
        return contenido;
    }

    private void mostrarDetalle(SolicitudEjecucion solicitud) {

        detalle.getChildren().clear();

        if (solicitud == null) {
            detalle.getChildren().add(ComponentesUi.texto(
                    "Selecciona una solicitud para ver sus datos."));
            return;
        }

        zona.getChildren().clear();
        detalle.getChildren().addAll(
                DetalleSolicitud.crear(solicitud),
                crearAcciones(solicitud),
                zona);
    }

    private FlowPane crearAcciones(SolicitudEjecucion solicitud) {

        FlowPane acciones = new FlowPane(12, 12);
        acciones.setAlignment(Pos.CENTER_LEFT);

        if (PresentacionSolicitud.puedeEnviarPropuesta(solicitud.getEstado())) {
            acciones.getChildren().add(ComponentesUi.boton(
                    PresentacionSolicitud.textoBotonPropuesta(
                            solicitud.getEstado()),
                    "btn-primary",
                    this::enviarPropuestaSeleccionada));
        }

        acciones.getChildren().addAll(
                ComponentesUi.boton("Conversación", "btn-ghost",
                        this::mostrarConversacionSeleccionada),
                ComponentesUi.boton("Historial de propuestas", "btn-ghost",
                        this::mostrarHistorialSeleccionado));

        if (PresentacionSolicitud.puedeRechazar(solicitud.getEstado())) {
            acciones.getChildren().add(ComponentesUi.boton(
                    "Rechazar solicitud", "btn-danger-ghost",
                    this::rechazarSeleccionada));
        }

        return acciones;
    }

    private HBox crearBarraSuperior() {

        return MenuCompartido.crearBarraSuperior(
                sesion,
                "Solicitudes recibidas");
    }

    private VBox crearMenu() {
        return MenuCompartido.crear(
                sesion,
                MenuCompartido.Pagina.SOLICITUDES_PROVEEDOR,
                null,
                null,
                null,
                abrirServicios,
                cerrarSesion,
                null,
                abrirSolicitudes,
                null,
                null,
                abrirAvisos);
    }

    private void recargarSolicitudes() {

        try {
            List<SolicitudEjecucion> solicitudes
                    = controller.listarSolicitudesProveedor(idProveedor);

            SolicitudEjecucion elegida
                    = lista.getSelectionModel().getSelectedItem();

            lista.getItems().setAll(solicitudes);

            if (elegida != null) {
                solicitudes.stream()
                        .filter(item -> item.getIdSolicitud()
                                == elegida.getIdSolicitud())
                        .findFirst()
                        .ifPresent(lista.getSelectionModel()::select);
            }

        } catch (RuntimeException e) {
            detalle.getChildren().setAll(
                    ComponentesUi.banner(e.getMessage(), "banner-error"));
        }
    }

    private void rechazarSeleccionada() {

        SolicitudEjecucion solicitud
                = lista.getSelectionModel().getSelectedItem();

        if (solicitud == null) {
            return;
        }

        zona.getChildren().setAll(new FormularioMotivo(
                "Rechazar solicitud",
                "El organizador verá este motivo. La solicitud quedará cerrada.",
                "Rechazar solicitud",
                motivo -> {
                    controller.rechazarSolicitud(
                            idProveedor, solicitud.getIdSolicitud(), motivo);
                    recargarSolicitudes();
                    mostrarAviso("Solicitud rechazada.", "banner-ok");
                },
                () -> zona.getChildren().clear()));
    }

    private void enviarPropuestaSeleccionada() {

        SolicitudEjecucion solicitud
                = lista.getSelectionModel().getSelectedItem();

        if (solicitud == null) {
            return;
        }

        String aviso = "EN_NEGOCIACION".equalsIgnoreCase(solicitud.getEstado())
                ? "Se creará una nueva versión. La anterior queda en el "
                + "historial y deja de estar vigente."
                : null;

        zona.getChildren().setAll(new FormularioPropuesta(
                PresentacionSolicitud.textoBotonPropuesta(
                        solicitud.getEstado()),
                aviso,
                datos -> guardarPropuesta(solicitud, datos),
                () -> zona.getChildren().clear()));
    }

    private void guardarPropuesta(
            SolicitudEjecucion solicitud, DatosPropuesta datos) {

        controller.enviarPropuesta(
                idProveedor,
                solicitud.getIdSolicitud(),
                datos.precio(),
                datos.descripcion(),
                datos.condiciones(),
                datos.materialApoyo());

        recargarSolicitudes();
        mostrarAviso("Propuesta enviada. El organizador ya puede revisarla.",
                "banner-ok");
    }

    private void mostrarAviso(String mensaje, String estilo) {
        zona.getChildren().setAll(ComponentesUi.banner(mensaje, estilo));
    }

    private void mostrarHistorialSeleccionado() {

        SolicitudEjecucion solicitud
                = lista.getSelectionModel().getSelectedItem();

        if (solicitud == null) {
            return;
        }

        try {
            zona.getChildren().setAll(HistorialPropuestas.crear(
                    controller.listarHistorialPropuestas(
                            idProveedor, solicitud.getIdSolicitud())));
        } catch (RuntimeException e) {
            mostrarAviso(e.getMessage(), "banner-error");
        }
    }

    private void mostrarConversacionSeleccionada() {

        SolicitudEjecucion solicitud
                = lista.getSelectionModel().getSelectedItem();

        if (solicitud == null) {
            return;
        }

        int idSolicitud = solicitud.getIdSolicitud();

        zona.getChildren().setAll(new PanelConversacion(
                () -> controller.listarMensajes(idSolicitud, idUsuario),
                (texto, adjunto) -> controller.enviarMensaje(
                        idSolicitud, idUsuario, texto, adjunto),
                idUsuario,
                solicitud.getContexto().nombreOrganizador()));
    }
}

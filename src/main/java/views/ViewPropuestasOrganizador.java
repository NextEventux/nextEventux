package views;

import java.util.List;
import java.util.function.Consumer;

import controllers.ControllerSolicitudes;
import entities.PropuestaServicio;
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
import views.solicitudes.DetallePropuesta;
import views.solicitudes.FormularioMotivo;
import views.solicitudes.PanelConversacion;
import views.solicitudes.PresentacionSolicitud;
import views.solicitudes.TarjetaPropuesta;

public class ViewPropuestasOrganizador {

    private final ControllerSolicitudes controller;
    private final int idOrganizador;
    private final int idUsuario;
    private final SesionUsuario sesion;
    private final Runnable abrirEventos;
    private final Runnable abrirInvitados;
    private final Runnable abrirCatalogo;
    private final Runnable abrirMisEventos;
    private final Runnable abrirPropuestas;
    private final Runnable cerrarSesion;

    private final ListView<PropuestaServicio> lista
            = new ListView<>();
    private final VBox detalle = new VBox(16);
    private final VBox zona = new VBox(16);

    // CU-13: la aplicacion abre la pantalla de contratacion de la propuesta
    private Consumer<PropuestaServicio> abrirContratacion;

    public ViewPropuestasOrganizador conContratacion(
            Consumer<PropuestaServicio> abrirContratacion) {

        this.abrirContratacion = abrirContratacion;
        return this;
    }

    public ViewPropuestasOrganizador(
            ControllerSolicitudes controller,
            int idOrganizador,
            int idUsuario,
            SesionUsuario sesion,
            Runnable abrirEventos,
            Runnable abrirInvitados,
            Runnable abrirCatalogo,
            Runnable abrirMisEventos,
            Runnable abrirPropuestas,
            Runnable cerrarSesion) {

        this.controller = controller;
        this.idOrganizador = idOrganizador;
        this.idUsuario = idUsuario;
        this.sesion = sesion;
        this.abrirEventos = abrirEventos;
        this.abrirInvitados = abrirInvitados;
        this.abrirCatalogo = abrirCatalogo;
        this.abrirMisEventos = abrirMisEventos;
        this.abrirPropuestas = abrirPropuestas;
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

        recargarPropuestas();

        return raiz;
    }

    /** Lista de propuestas a la izquierda y detalle de la elegida a la derecha. */
    private HBox crearContenido() {

        lista.getStyleClass().add("cu12-list");
        lista.setPrefWidth(380);
        lista.setMinWidth(320);
        lista.setPlaceholder(ComponentesUi.texto(
                "Todavía no tienes propuestas. Aparecerán aquí cuando "
                + "un proveedor responda tus solicitudes."));
        lista.setCellFactory(vista -> new ListCell<>() {

            @Override
            protected void updateItem(
                    PropuestaServicio propuesta,
                    boolean vacia) {

                super.updateItem(propuesta, vacia);

                setText(null);
                setGraphic(vacia || propuesta == null
                        ? null
                        : TarjetaPropuesta.crear(propuesta));
            }
        });

        ScrollPane desplazable = new ScrollPane(detalle);
        desplazable.setFitToWidth(true);
        desplazable.getStyleClass().add("panel-desplazable");
        HBox.setHgrow(desplazable, Priority.ALWAYS);

        VBox izquierda = new VBox(14,
                ComponentesUi.titulo("Propuestas recibidas"),
                lista);
        VBox.setVgrow(lista, Priority.ALWAYS);

        HBox contenido = new HBox(24, izquierda, desplazable);
        contenido.getStyleClass().add("cu12-content");
        contenido.setPadding(new Insets(30));

        mostrarDetalle(null);
        return contenido;
    }

    private void mostrarDetalle(PropuestaServicio propuesta) {

        detalle.getChildren().clear();
        zona.getChildren().clear();

        if (propuesta == null) {
            detalle.getChildren().add(ComponentesUi.texto(
                    "Selecciona una propuesta para ver sus datos."));
            return;
        }

        detalle.getChildren().addAll(
                DetallePropuesta.crear(propuesta),
                crearAcciones(propuesta),
                zona);

        if (PresentacionSolicitud.propuestaCerrada(propuesta.getEstado())) {
            mostrarAviso(PresentacionSolicitud.mensajeCerrada(
                    propuesta.getEstado()), "banner-info");
        }
    }

    /** Una acción principal según el estado; las demás son discretas. */
    private FlowPane crearAcciones(PropuestaServicio propuesta) {

        String estado = propuesta.getEstado();
        FlowPane acciones = new FlowPane(12, 12);
        acciones.setAlignment(Pos.CENTER_LEFT);

        if (PresentacionSolicitud.puedeSeleccionar(estado)) {
            acciones.getChildren().add(ComponentesUi.boton(
                    "Seleccionar propuesta", "btn-primary",
                    () -> seleccionar(propuesta)));
        }
        if (PresentacionSolicitud.puedeContratar(estado)) {
            acciones.getChildren().add(ComponentesUi.boton(
                    "Contratar servicio", "btn-primary",
                    () -> contratar(propuesta)));
        }
        if (PresentacionSolicitud.puedeAjustar(estado)) {
            acciones.getChildren().add(ComponentesUi.boton(
                    "Solicitar ajuste", "btn-ghost",
                    () -> pedirAjuste(propuesta)));
        }

        acciones.getChildren().add(ComponentesUi.boton(
                "Conversación", "btn-ghost",
                () -> mostrarConversacion(propuesta)));

        if (PresentacionSolicitud.puedeRechazarPropuesta(estado)) {
            acciones.getChildren().add(ComponentesUi.boton(
                    "Rechazar propuesta", "btn-danger-ghost",
                    () -> pedirConfirmacionDeRechazo(propuesta)));
        }

        return acciones;
    }

    private VBox crearMenu() {
        return MenuCompartido.crear(
                sesion,
                MenuCompartido.Pagina.PROPUESTAS_ORGANIZADOR,
                abrirEventos,
                abrirInvitados,
                abrirCatalogo,
                null,
                cerrarSesion,
                abrirMisEventos,
                null,
                abrirPropuestas);
    }

    private HBox crearBarraSuperior() {
        return MenuCompartido.crearBarraSuperior(
                sesion,
                "Propuestas recibidas");
    }

    private void seleccionar(PropuestaServicio propuesta) {
        try {
            controller.seleccionarPropuesta(
                    idOrganizador, propuesta.getIdPropuesta());

            recargarPropuestas();
            mostrarAviso("Propuesta seleccionada. Ahora puedes contratar "
                    + "el servicio.", "banner-ok");

        } catch (RuntimeException e) {
            mostrarAviso(e.getMessage(), "banner-error");
        }
    }

    private void contratar(PropuestaServicio propuesta) {
        if (abrirContratacion == null) {
            mostrarAviso("La contratación de servicios no está habilitada.",
                    "banner-error");
            return;
        }

        abrirContratacion.accept(propuesta);
    }

    private void pedirConfirmacionDeRechazo(PropuestaServicio propuesta) {
        zona.getChildren().setAll(ComponentesUi.confirmacion(
                "Rechazar propuesta",
                "La negociación de esta solicitud se cerrará. "
                + "Esta acción no se puede deshacer.",
                "Rechazar propuesta",
                () -> rechazar(propuesta),
                () -> zona.getChildren().clear()));
    }

    private void rechazar(PropuestaServicio propuesta) {
        try {
            controller.rechazarPropuesta(
                    idOrganizador, propuesta.getIdPropuesta());

            recargarPropuestas();
            mostrarAviso("Propuesta rechazada.", "banner-ok");

        } catch (RuntimeException e) {
            mostrarAviso(e.getMessage(), "banner-error");
        }
    }

    /** Muestra un aviso bajo las acciones, o solo si no hay propuesta elegida. */
    private void mostrarAviso(String mensaje, String estilo) {
        if (!detalle.getChildren().contains(zona)) {
            detalle.getChildren().add(zona);
        }
        zona.getChildren().setAll(ComponentesUi.banner(mensaje, estilo));
    }

    private void pedirAjuste(PropuestaServicio propuesta) {
        zona.getChildren().setAll(new FormularioMotivo(
                "Solicitar ajuste a la versión "
                + propuesta.getNumeroVersion(),
                "El proveedor recibirá tu mensaje y podrá enviar una "
                + "nueva versión.",
                "Qué necesitas que cambie",
                "Enviar ajuste",
                "btn-primary",
                texto -> {
                    controller.solicitarAjuste(
                            idOrganizador, propuesta.getIdPropuesta(),
                            texto, null);
                    mostrarAviso("Ajuste enviado al proveedor.", "banner-ok");
                },
                () -> zona.getChildren().clear()));
    }

    private void mostrarConversacion(PropuestaServicio propuesta) {
        int idSolicitud = propuesta.getIdSolicitud();

        zona.getChildren().setAll(new PanelConversacion(
                () -> controller.listarMensajes(idSolicitud, idUsuario),
                (texto, adjunto) -> controller.enviarMensaje(
                        idSolicitud, idUsuario, texto, adjunto),
                idUsuario,
                propuesta.getContexto().nombreProveedor()));
    }

    private void recargarPropuestas() {

        try {
            PropuestaServicio elegida
                    = lista.getSelectionModel().getSelectedItem();

            List<PropuestaServicio> propuestas
                    = controller.listarPropuestasOrganizador(
                            idOrganizador);

            lista.getItems().setAll(propuestas);

            if (elegida != null) {
                propuestas.stream()
                        .filter(item -> item.getIdPropuesta()
                                == elegida.getIdPropuesta())
                        .findFirst()
                        .ifPresent(lista.getSelectionModel()::select);
            }

        } catch (RuntimeException e) {
            mostrarAviso(e.getMessage(), "banner-error");
        }
    }
}

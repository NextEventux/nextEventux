package views;

import java.util.List;

import controllers.ControllerAdministracion;
import entities.Contratacion;
import entities.SesionUsuario;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import views.components.ComponentesUi;
import views.components.MenuCompartido;

/**
 * Ampliación de CU-026: el organizador consulta las solicitudes de
 * contratación que le enviaron los clientes y las acepta o rechaza.
 * Una solicitud respondida ya no muestra acciones.
 */
public class ViewSolicitudesContratacion {

    private final ControllerAdministracion controller;
    private final SesionUsuario sesion;
    private final Runnable abrirEventos;
    private final Runnable abrirInvitados;
    private final Runnable abrirCatalogo;
    private final Runnable abrirMisEventos;
    private final Runnable abrirPropuestas;
    private final Runnable cerrarSesion;

    private final ListView<Contratacion> lista = new ListView<>();
    private final ChoiceBox<String> filtro = new ChoiceBox<>();
    private final VBox detalle = new VBox(16);
    private final VBox zona = new VBox(16);

    private List<Contratacion> solicitudes = List.of();

    public ViewSolicitudesContratacion(
            ControllerAdministracion controller,
            SesionUsuario sesion,
            Runnable abrirEventos,
            Runnable abrirInvitados,
            Runnable abrirCatalogo,
            Runnable abrirMisEventos,
            Runnable abrirPropuestas,
            Runnable cerrarSesion) {

        if (controller == null) {
            throw new IllegalArgumentException(
                    "El controlador de administración es obligatorio.");
        }

        this.controller = controller;
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

        VBox menu = MenuCompartido.crear(
                sesion,
                MenuCompartido.Pagina.SOLICITUDES_CONTRATACION,
                abrirEventos,
                abrirInvitados,
                abrirCatalogo,
                null,
                cerrarSesion,
                abrirMisEventos,
                null,
                abrirPropuestas);
        raiz.setLeft(menu);

        BorderPane principal = new BorderPane();
        principal.setTop(MenuCompartido.crearBarraSuperior(
                sesion, "Solicitudes de clientes"));
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

        recargar(null);
        return raiz;
    }

    /** Lista de solicitudes a la izquierda y detalle a la derecha. */
    private HBox crearContenido() {

        lista.setId("solicitudesContratacion");
        lista.getStyleClass().add("cu12-list");
        lista.setPrefWidth(380);
        lista.setMinWidth(320);
        lista.setPlaceholder(ComponentesUi.texto(
                "No tienes solicitudes en esta categoría. Las solicitudes "
                + "que envíen los clientes aparecerán aquí."));
        lista.setCellFactory(vista -> new ListCell<>() {

            {
                setPrefWidth(0);
            }

            @Override
            protected void updateItem(Contratacion solicitud, boolean vacia) {
                super.updateItem(solicitud, vacia);
                setText(vacia || solicitud == null
                        ? null
                        : PresentacionContratacionOrganizador.solicitudRecibida(solicitud));
                setWrapText(true);
            }
        });

        filtro.setId("filtroSolicitudesContratacion");
        filtro.getItems().setAll(
                PresentacionContratacionOrganizador.FILTRO_PENDIENTES,
                PresentacionContratacionOrganizador.FILTRO_RESPONDIDAS,
                PresentacionContratacionOrganizador.FILTRO_TODAS);
        filtro.setValue(PresentacionContratacionOrganizador.FILTRO_PENDIENTES);
        filtro.valueProperty().addListener((obs, anterior, actual) -> aplicarFiltro(null));

        ScrollPane desplazable = new ScrollPane(detalle);
        desplazable.setFitToWidth(true);
        desplazable.getStyleClass().add("panel-desplazable");
        HBox.setHgrow(desplazable, Priority.ALWAYS);

        VBox izquierda = new VBox(14,
                ComponentesUi.titulo("Solicitudes de clientes"),
                ComponentesUi.texto("Clientes que quieren contratarte para su evento."),
                filtro,
                lista,
                ComponentesUi.boton("Actualizar", "btn-ghost", () -> recargar(null)));
        VBox.setVgrow(lista, Priority.ALWAYS);

        HBox contenido = new HBox(24, izquierda, desplazable);
        contenido.getStyleClass().add("cu12-content");
        contenido.setPadding(new Insets(30));

        mostrarDetalle(null);
        return contenido;
    }

    /** Vuelve a consultar y conserva seleccionada la solicitud indicada. */
    private void recargar(Integer idSeleccionar) {

        try {
            solicitudes = controller.consultarSolicitudesRecibidas();
            aplicarFiltro(idSeleccionar);

        } catch (RuntimeException e) {
            solicitudes = List.of();
            lista.getItems().clear();
            mostrarDetalle(null);
            mostrarAviso(mensaje(e, "No se pudieron consultar las solicitudes."),
                    "banner-error");
        }
    }

    private void aplicarFiltro(Integer idSeleccionar) {

        lista.getItems().setAll(solicitudes.stream()
                .filter(s -> PresentacionContratacionOrganizador
                        .coincideFiltro(s, filtro.getValue()))
                .toList());

        if (idSeleccionar != null) {
            lista.getItems().stream()
                    .filter(s -> s.getIdContratacion() == idSeleccionar)
                    .findFirst()
                    .ifPresent(s -> lista.getSelectionModel().select(s));
        }
    }

    private void mostrarDetalle(Contratacion solicitud) {

        detalle.getChildren().clear();
        zona.getChildren().clear();

        if (solicitud == null) {
            detalle.getChildren().addAll(
                    ComponentesUi.texto("Selecciona una solicitud para ver sus datos."),
                    zona);
            return;
        }

        VBox tarjeta = ComponentesUi.tarjeta(
                ComponentesUi.encabezadoConChip(
                        "Solicitud #" + solicitud.getIdContratacion(),
                        ComponentesUi.chip(
                                PresentacionContratacionOrganizador.estado(solicitud.getEstado()),
                                PresentacionContratacionOrganizador.estiloEstado(solicitud.getEstado()))),
                ComponentesUi.dato("Cliente", solicitud.getNombreCliente()),
                ComponentesUi.dato("Tipo de evento", solicitud.getTipoEvento()),
                ComponentesUi.dato("Ciudad", solicitud.getCiudad()),
                ComponentesUi.dato("Fecha aproximada",
                        FormatoUi.fecha(solicitud.getFechaAproximada())),
                ComponentesUi.dato("Presupuesto estimado",
                        FormatoUi.moneda(solicitud.getPresupuestoEstimado())),
                ComponentesUi.dato("Criterios adicionales",
                        PresentacionContratacionOrganizador.texto(
                                solicitud.getCriteriosBusqueda(), "Ninguno")),
                ComponentesUi.dato("Recibida",
                        FormatoUi.fechaHora(solicitud.getFechaSolicitud())));

        detalle.getChildren().add(tarjeta);

        if (PresentacionContratacionOrganizador.puedeResponder(solicitud)) {
            detalle.getChildren().add(ComponentesUi.acciones(
                    ComponentesUi.boton("Aceptar solicitud", "btn-primary",
                            () -> pedirConfirmacion(solicitud, true)),
                    ComponentesUi.boton("Rechazar solicitud", "btn-danger-ghost",
                            () -> pedirConfirmacion(solicitud, false))));
        } else {
            zona.getChildren().add(ComponentesUi.banner(
                    "Esta solicitud ya fue respondida.", "banner-info"));
        }

        detalle.getChildren().add(zona);
    }

    /** Confirmación dentro de la pantalla, como en las demás vistas. */
    private void pedirConfirmacion(Contratacion solicitud, boolean aceptar) {

        zona.getChildren().setAll(ComponentesUi.confirmacion(
                aceptar ? "Aceptar solicitud" : "Rechazar solicitud",
                PresentacionContratacionOrganizador.preguntaRespuesta(solicitud, aceptar),
                aceptar ? "Sí, aceptar" : "Sí, rechazar",
                () -> responder(solicitud, aceptar),
                () -> zona.getChildren().clear()));
    }

    private void responder(Contratacion solicitud, boolean aceptar) {

        try {
            Contratacion respondida = aceptar
                    ? controller.aceptarSolicitud(solicitud.getIdContratacion())
                    : controller.rechazarSolicitud(solicitud.getIdContratacion());

            // Se muestra la solicitud respondida aunque el filtro sea Pendientes.
            filtro.setValue(PresentacionContratacionOrganizador.FILTRO_TODAS);
            recargar(respondida.getIdContratacion());
            mostrarAviso(PresentacionContratacionOrganizador
                    .confirmacionRespuesta(respondida), "banner-ok");

        } catch (RuntimeException e) {
            // Ya respondida, cuenta suspendida o error: se recarga el estado real.
            recargar(solicitud.getIdContratacion());
            mostrarAviso(mensaje(e, "No fue posible registrar la respuesta."),
                    "banner-error");
        }
    }

    private void mostrarAviso(String texto, String estilo) {
        zona.getChildren().setAll(ComponentesUi.banner(texto, estilo));
    }

    private String mensaje(RuntimeException e, String predeterminado) {
        return e.getMessage() == null || e.getMessage().isBlank()
                ? predeterminado
                : e.getMessage();
    }
}

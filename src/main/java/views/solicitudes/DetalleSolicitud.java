package views.solicitudes;

import entities.solicitudes.ContextoSolicitud;
import entities.solicitudes.SolicitudEjecucion;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import views.FormatoUi;
import views.components.ComponentesUi;

/** Datos de la solicitud seleccionada, agrupados en una tarjeta (CU-12). */
public final class DetalleSolicitud {

    private DetalleSolicitud() {
    }

    public static VBox crear(SolicitudEjecucion solicitud) {
        ContextoSolicitud contexto = solicitud.getContexto();

        FlowPane datos = new FlowPane(36, 14);
        datos.getChildren().addAll(
                ComponentesUi.dato("Servicio",
                        PresentacionSolicitud.nombreServicio(
                                contexto, solicitud.getIdServicio())),
                ComponentesUi.dato("Fecha del evento",
                        PresentacionSolicitud.fechaEvento(contexto)),
                ComponentesUi.dato("Lugar", contexto.ubicacion()),
                ComponentesUi.dato("Invitados",
                        PresentacionSolicitud.personas(contexto)),
                ComponentesUi.dato("Organizador", contexto.nombreOrganizador()),
                ComponentesUi.dato("Recibida el",
                        FormatoUi.fechaHora(solicitud.getFechaSolicitud())));

        VBox detalle = ComponentesUi.tarjeta(
                ComponentesUi.encabezadoConChip(
                        PresentacionSolicitud.nombreEvento(
                                contexto, solicitud.getIdEvento()),
                        ComponentesUi.chip(
                                PresentacionSolicitud.textoEstado(
                                        solicitud.getEstado()),
                                PresentacionSolicitud.estiloEstado(
                                        solicitud.getEstado()))),
                datos);

        agregarSiHay(detalle, "Lo que necesita", contexto.descripcionNecesidad());
        agregarSiHay(detalle, "Motivo del rechazo", solicitud.getMotivoRechazo());
        return detalle;
    }

    private static void agregarSiHay(VBox detalle, String etiqueta, String texto) {
        if (texto != null && !texto.isBlank()) {
            detalle.getChildren().add(ComponentesUi.dato(etiqueta, texto));
        }
    }
}

package views.solicitudes;

import entities.solicitudes.ContextoSolicitud;
import entities.solicitudes.SolicitudEjecucion;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import views.FormatoUi;
import views.components.ComponentesUi;

/** Tarjeta de la lista de solicitudes recibidas por el proveedor (CU-12). */
public final class TarjetaSolicitud {

    private TarjetaSolicitud() {
    }

    public static Node crear(SolicitudEjecucion solicitud) {
        ContextoSolicitud contexto = solicitud.getContexto();

        Label chip = ComponentesUi.chip(
                PresentacionSolicitud.textoEstado(solicitud.getEstado()),
                PresentacionSolicitud.estiloEstado(solicitud.getEstado()));

        Label servicio = ComponentesUi.texto(
                PresentacionSolicitud.nombreServicio(
                        contexto, solicitud.getIdServicio())
                + "  ·  " + PresentacionSolicitud.fechaEvento(contexto));

        Label recibida = ComponentesUi.texto(
                "Recibida el " + FormatoUi.fecha(
                        solicitud.getFechaSolicitud().toLocalDate()));

        return new VBox(8,
                ComponentesUi.encabezadoConChip(
                        PresentacionSolicitud.nombreEvento(
                                contexto, solicitud.getIdEvento()),
                        chip),
                servicio,
                recibida);
    }
}

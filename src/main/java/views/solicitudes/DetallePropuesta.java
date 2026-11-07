package views.solicitudes;

import entities.eventos.Evento;

import entities.ContextoSolicitud;
import entities.PropuestaServicio;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import views.FormatoUi;
import views.components.ComponentesUi;

/** Datos de la propuesta seleccionada y del evento al que responde (CU-12). */
public final class DetallePropuesta {

    private DetallePropuesta() {
    }

    public static VBox crear(PropuestaServicio propuesta) {
        ContextoSolicitud contexto = propuesta.getContexto();

        FlowPane evento = new FlowPane(36, 14);
        evento.getChildren().addAll(
                ComponentesUi.dato("Evento",
                        PresentacionSolicitud.nombreEvento(contexto)),
                ComponentesUi.dato("Fecha",
                        PresentacionSolicitud.fechaEvento(contexto)),
                ComponentesUi.dato("Lugar", contexto.ubicacion()),
                ComponentesUi.dato("Proveedor",
                        PresentacionSolicitud.nombreProveedor(contexto)),
                ComponentesUi.dato("Necesidad",
                        contexto.descripcionNecesidad()));

        FlowPane oferta = new FlowPane(36, 14);
        oferta.getChildren().addAll(
                ComponentesUi.dato("Precio final",
                        FormatoUi.moneda(propuesta.getValor())),
                ComponentesUi.dato("Versión",
                        String.valueOf(propuesta.getNumeroVersion())),
                ComponentesUi.dato("Enviada el",
                        FormatoUi.fechaHora(propuesta.getFechaEnvio())));

        return ComponentesUi.tarjeta(
                ComponentesUi.encabezadoConChip(
                        PresentacionSolicitud.nombreServicio(contexto),
                        ComponentesUi.chip(
                                PresentacionSolicitud.textoEstado(
                                        propuesta.getEstado()),
                                PresentacionSolicitud.estiloEstado(
                                        propuesta.getEstado()))),
                evento,
                oferta,
                ComponentesUi.dato("Qué incluye", propuesta.getDescripcion()),
                ComponentesUi.dato("Condiciones", propuesta.getCondiciones()),
                ComponentesUi.dato("Material de apoyo",
                        propuesta.getMaterialApoyo()));
    }
}

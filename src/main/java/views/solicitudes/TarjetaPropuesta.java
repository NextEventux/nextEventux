package views.solicitudes;

import entities.solicitudes.ContextoSolicitud;
import entities.solicitudes.PropuestaServicio;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import views.FormatoUi;
import views.components.ComponentesUi;

/** Tarjeta de la lista de propuestas que recibe el organizador (CU-12). */
public final class TarjetaPropuesta {

    private TarjetaPropuesta() {
    }

    public static Node crear(PropuestaServicio propuesta) {
        ContextoSolicitud contexto = propuesta.getContexto();

        Label chip = ComponentesUi.chip(
                PresentacionSolicitud.textoEstado(propuesta.getEstado()),
                PresentacionSolicitud.estiloEstado(propuesta.getEstado()));

        Label precio = ComponentesUi.subtitulo(
                FormatoUi.moneda(propuesta.getValor()));

        Label proveedor = ComponentesUi.texto(
                PresentacionSolicitud.nombreProveedor(contexto)
                + "  ·  versión " + propuesta.getNumeroVersion());

        return new VBox(8,
                ComponentesUi.encabezadoConChip(
                        PresentacionSolicitud.nombreServicio(contexto),
                        chip),
                ComponentesUi.texto(
                        PresentacionSolicitud.nombreEvento(contexto)),
                proveedor,
                precio);
    }
}

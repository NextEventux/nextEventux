package views.servicios;

import java.math.BigDecimal;

import entities.Servicio;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import views.FormatoUi;
import views.components.ComponentesUi;
import views.servicios.PresentacionServicio.Revision;

/**
 * Resumen que el proveedor revisa antes de publicar un servicio (CU-11).
 * Muestra lo que verán los organizadores y qué falta, y solo habilita
 * Publicar cuando el servicio cumple lo mínimo.
 */
public final class RevisionPublicacion extends VBox {

    public RevisionPublicacion(
            Servicio servicio,
            int disponibilidades,
            int imagenes,
            Runnable publicar,
            Runnable cancelar) {

        super(14);
        getStyleClass().add("tarjeta");

        Revision revision
                = PresentacionServicio.revisar(disponibilidades, imagenes);

        FlowPane datos = new FlowPane(36, 14);
        datos.getChildren().addAll(
                ComponentesUi.dato("Tipo", servicio.getTipo()),
                ComponentesUi.dato("Categoría", servicio.getCategoria()),
                ComponentesUi.dato("Precio base",
                        FormatoUi.moneda(BigDecimal.valueOf(
                                servicio.getPrecio()))),
                ComponentesUi.dato("Ciudad o zona", servicio.getCiudadZona()),
                ComponentesUi.dato("Anticipación mínima",
                        PresentacionServicio.anticipacion(
                                servicio.getAnticipacionMinima())),
                ComponentesUi.dato("Capacidad",
                        PresentacionServicio.capacidad(
                                servicio.getCantidadMaximaPersonas())),
                ComponentesUi.dato("Disponibilidad",
                        PresentacionServicio.cantidad(
                                disponibilidades, "horario", "horarios")),
                ComponentesUi.dato("Imágenes",
                        PresentacionServicio.cantidad(
                                imagenes, "imagen", "imágenes")));

        getChildren().addAll(
                ComponentesUi.subtitulo(
                        "Revisa «" + servicio.getNombre() + "» antes de publicar"),
                ComponentesUi.texto(
                        "Así lo verán los organizadores en el catálogo."),
                datos,
                ComponentesUi.dato("Descripción", servicio.getDescripcion()));

        if (revision.bloqueo() != null) {
            getChildren().add(
                    ComponentesUi.banner(revision.bloqueo(), "banner-error"));
        }
        if (revision.aviso() != null) {
            getChildren().add(
                    ComponentesUi.banner(revision.aviso(), "banner-aviso"));
        }

        Button botonPublicar = ComponentesUi.boton(
                "Publicar servicio", "btn-primary", publicar);
        botonPublicar.setDisable(!revision.puedePublicar());

        getChildren().add(ComponentesUi.acciones(
                botonPublicar,
                ComponentesUi.boton("Seguir editando", "btn-ghost", cancelar)));
    }
}

package views.solicitudes;

import java.util.List;

import entities.PropuestaServicio;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import views.FormatoUi;
import views.components.ComponentesUi;

/** Versiones de la propuesta de una solicitud, de la más reciente a la más antigua (CU-12). */
public final class HistorialPropuestas {

    private HistorialPropuestas() {
    }

    public static VBox crear(List<PropuestaServicio> versiones) {
        VBox historial = new VBox(12);

        if (versiones.isEmpty()) {
            historial.getChildren().add(ComponentesUi.banner(
                    "Esta solicitud todavía no tiene propuestas.",
                    "banner-info"));
            return historial;
        }

        historial.getChildren().add(
                ComponentesUi.subtitulo("Historial de propuestas"));
        for (PropuestaServicio version : versiones) {
            historial.getChildren().add(tarjeta(version));
        }
        return historial;
    }

    private static VBox tarjeta(PropuestaServicio version) {
        FlowPane datos = new FlowPane(36, 12);
        datos.getChildren().addAll(
                ComponentesUi.dato("Precio", FormatoUi.moneda(version.getValor())),
                ComponentesUi.dato("Enviada el",
                        FormatoUi.fechaHora(version.getFechaEnvio())));

        return ComponentesUi.tarjeta(
                ComponentesUi.encabezadoConChip(
                        "Versión " + version.getNumeroVersion(),
                        chipDe(version)),
                datos,
                ComponentesUi.dato("Qué incluye", version.getDescripcion()),
                ComponentesUi.dato("Condiciones", version.getCondiciones()),
                ComponentesUi.dato("Material de apoyo",
                        version.getMaterialApoyo()));
    }

    /** Una propuesta enviada se muestra como vigente o anterior; las demás, por su estado. */
    private static Label chipDe(PropuestaServicio version) {
        String estado = version.getEstado();

        if (estado == null || "ENVIADA".equalsIgnoreCase(estado)) {
            return version.isVigente()
                    ? ComponentesUi.chip("Vigente", "chip-info")
                    : ComponentesUi.chip("Anterior", "chip-neutro");
        }

        return ComponentesUi.chip(
                PresentacionSolicitud.textoEstado(estado),
                PresentacionSolicitud.estiloEstado(estado));
    }
}

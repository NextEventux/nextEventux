package views.solicitudes;

import entities.eventos.Evento;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import entities.eventos.EventoSolicitable;
import entities.solicitudes.NecesidadAbierta;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import views.FormatoUi;
import views.components.ComponentesUi;

/**
 * Formulario para pedir un servicio a un proveedor (CU-12). El organizador
 * elige su evento por nombre y una necesidad abierta de ese evento, o crea
 * una nueva; no escribe identificadores.
 */
public final class FormularioSolicitudServicio extends VBox {

    private final List<NecesidadAbierta> necesidades;
    private final Consumer<SolicitudNueva> enviar;

    private final ComboBox<EventoSolicitable> evento = new ComboBox<>();
    private final ComboBox<NecesidadAbierta> necesidad = new ComboBox<>();
    private final TextArea descripcion = new TextArea();
    private final VBox avisos = new VBox();

    /**
     * @param enviar acción que registra la solicitud; si lanza una
     *     excepción su mensaje se muestra en el formulario
     */
    public FormularioSolicitudServicio(
            String nombreServicio,
            List<EventoSolicitable> eventos,
            List<NecesidadAbierta> necesidades,
            Consumer<SolicitudNueva> enviar,
            Runnable cancelar) {

        super(14);
        this.necesidades = necesidades;
        this.enviar = enviar;
        getStyleClass().add("tarjeta");

        configurarEvento(eventos);
        configurarNecesidad();

        descripcion.setPromptText("Describe lo que necesitas");
        descripcion.setPrefRowCount(3);
        descripcion.setWrapText(true);

        getChildren().addAll(
                ComponentesUi.subtitulo("Solicitar " + nombreServicio),
                avisos,
                campo("Evento", evento),
                campo("Necesidad", necesidad),
                descripcion,
                ComponentesUi.acciones(
                        ComponentesUi.boton("Enviar solicitud", "btn-primary",
                                this::confirmar),
                        ComponentesUi.boton("Cancelar", "btn-ghost", cancelar)));

        actualizarNecesidades();
    }

    private void configurarEvento(List<EventoSolicitable> eventos) {
        evento.getItems().setAll(eventos);
        evento.setPromptText("Elige tu evento");
        evento.setMaxWidth(Double.MAX_VALUE);
        evento.setConverter(new StringConverter<>() {
            @Override
            public String toString(EventoSolicitable item) {
                return item == null ? "" : item.nombre() + "  ·  "
                        + FormatoUi.fecha(item.inicio() == null
                                ? null : item.inicio().toLocalDate());
            }

            @Override
            public EventoSolicitable fromString(String texto) {
                return null;
            }
        });
        evento.valueProperty().addListener(
                (observable, anterior, actual) -> actualizarNecesidades());
    }

    private void configurarNecesidad() {
        necesidad.setMaxWidth(Double.MAX_VALUE);
        necesidad.setConverter(new StringConverter<>() {
            @Override
            public String toString(NecesidadAbierta item) {
                return item == null ? "" : item.descripcion();
            }

            @Override
            public NecesidadAbierta fromString(String texto) {
                return null;
            }
        });
        necesidad.valueProperty().addListener(
                (observable, anterior, actual) -> mostrarDescripcion());
    }

    /** Ofrece las necesidades del evento elegido y la opción de crear una. */
    private void actualizarNecesidades() {
        List<NecesidadAbierta> opciones = new ArrayList<>();
        opciones.add(SolicitudNueva.NECESIDAD_NUEVA);

        EventoSolicitable elegido = evento.getValue();
        if (elegido != null) {
            for (NecesidadAbierta item : necesidades) {
                if (item.idEvento() == elegido.idEvento()) {
                    opciones.add(item);
                }
            }
        }

        necesidad.getItems().setAll(opciones);
        necesidad.setValue(SolicitudNueva.NECESIDAD_NUEVA);
        mostrarDescripcion();
    }

    private void mostrarDescripcion() {
        boolean nueva = necesidad.getValue() == SolicitudNueva.NECESIDAD_NUEVA;
        descripcion.setVisible(nueva);
        descripcion.setManaged(nueva);
    }

    private void confirmar() {
        try {
            enviar.accept(SolicitudNueva.crear(
                    evento.getValue(), necesidad.getValue(),
                    descripcion.getText()));
        } catch (RuntimeException e) {
            avisos.getChildren().setAll(
                    ComponentesUi.banner(e.getMessage(), "banner-error"));
        }
    }

    private static VBox campo(String etiqueta, Node control) {
        Label titulo = new Label(etiqueta);
        titulo.getStyleClass().add("dato-etiqueta");
        return new VBox(4, titulo, control);
    }
}

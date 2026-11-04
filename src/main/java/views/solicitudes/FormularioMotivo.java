package views.solicitudes;

import java.util.function.Consumer;

import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import views.components.ComponentesUi;

/**
 * Formulario que pide un motivo antes de rechazar algo (CU-12): una solicitud
 * recibida o una propuesta. Reemplaza a la ventana emergente y deja el motivo
 * visible en la misma pantalla.
 */
public final class FormularioMotivo extends VBox {

    private final TextArea motivo = new TextArea();
    private final VBox avisos = new VBox();

    /**
     * @param accion recibe el motivo escrito; si lanza una excepción su
     *     mensaje se muestra en el formulario
     */
    public FormularioMotivo(
            String titulo,
            String ayuda,
            String textoBoton,
            Consumer<String> accion,
            Runnable cancelar) {

        super(12);
        getStyleClass().add("tarjeta");

        motivo.setPromptText("Motivo");
        motivo.setPrefRowCount(3);
        motivo.setWrapText(true);

        getChildren().addAll(
                ComponentesUi.subtitulo(titulo),
                ComponentesUi.texto(ayuda),
                avisos,
                motivo,
                ComponentesUi.acciones(
                        ComponentesUi.boton(textoBoton, "btn-danger",
                                () -> confirmar(accion)),
                        ComponentesUi.boton("Cancelar", "btn-ghost", cancelar)));
    }

    private void confirmar(Consumer<String> accion) {
        if (motivo.getText() == null || motivo.getText().isBlank()) {
            avisos.getChildren().setAll(ComponentesUi.banner(
                    "Escribe el motivo para continuar.", "banner-error"));
            return;
        }

        try {
            accion.accept(motivo.getText().trim());
        } catch (RuntimeException e) {
            avisos.getChildren().setAll(
                    ComponentesUi.banner(e.getMessage(), "banner-error"));
        }
    }
}

package views.solicitudes;

import java.util.function.Consumer;

import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import views.components.ComponentesUi;

/**
 * Formulario que pide un texto obligatorio dentro de la pantalla (CU-12): el
 * motivo para rechazar una solicitud o lo que se pide ajustar en una
 * propuesta. Reemplaza a la ventana emergente.
 */
public final class FormularioMotivo extends VBox {

    private final TextArea motivo = new TextArea();
    private final VBox avisos = new VBox();

    /**
     * @param pregunta texto de ejemplo del campo
     * @param estiloBoton btn-danger si descarta algo, btn-primary si no
     * @param accion recibe el texto escrito; si lanza una excepción su
     *     mensaje se muestra en el formulario
     */
    public FormularioMotivo(
            String titulo,
            String ayuda,
            String pregunta,
            String textoBoton,
            String estiloBoton,
            Consumer<String> accion,
            Runnable cancelar) {

        super(12);
        getStyleClass().add("tarjeta");

        motivo.setPromptText(pregunta);
        motivo.setPrefRowCount(3);
        motivo.setWrapText(true);

        getChildren().addAll(
                ComponentesUi.subtitulo(titulo),
                ComponentesUi.texto(ayuda),
                avisos,
                motivo,
                ComponentesUi.acciones(
                        ComponentesUi.boton(textoBoton, estiloBoton,
                                () -> confirmar(accion)),
                        ComponentesUi.boton("Cancelar", "btn-ghost", cancelar)));
    }

    private void confirmar(Consumer<String> accion) {
        if (motivo.getText() == null || motivo.getText().isBlank()) {
            avisos.getChildren().setAll(ComponentesUi.banner(
                    "Escribe el texto para continuar.", "banner-error"));
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

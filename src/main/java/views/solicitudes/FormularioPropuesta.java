package views.solicitudes;

import java.util.function.Consumer;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import views.FormatoUi;
import views.components.ComponentesUi;

/**
 * Formulario para enviar una propuesta dentro de la pantalla (CU-12). Tiene
 * dos pasos: se escriben los datos y luego se muestra un resumen que el
 * proveedor confirma antes de enviar.
 */
public final class FormularioPropuesta extends VBox {

    private final Consumer<DatosPropuesta> enviar;
    private final Runnable cancelar;
    private final String avisoVersion;

    private final TextField precio = new TextField();
    private final TextArea descripcion = new TextArea();
    private final TextArea condiciones = new TextArea();
    private final TextField material = new TextField();

    /**
     * @param enviar acción que guarda la propuesta; si lanza una excepción
     *     su mensaje se muestra en el formulario
     * @param avisoVersion texto que explica qué versión se crea, o null
     */
    public FormularioPropuesta(
            String titulo,
            String avisoVersion,
            Consumer<DatosPropuesta> enviar,
            Runnable cancelar) {

        super(14);
        this.enviar = enviar;
        this.cancelar = cancelar;
        this.avisoVersion = avisoVersion;
        getStyleClass().add("tarjeta");

        precio.setPromptText("Por ejemplo 1500000");
        descripcion.setPromptText("Qué incluye el servicio");
        condiciones.setPromptText(
                "Anticipo, cancelación, tiempos de entrega…");
        material.setPromptText("Enlace https:// (opcional)");
        descripcion.setPrefRowCount(3);
        descripcion.setWrapText(true);
        condiciones.setPrefRowCount(3);
        condiciones.setWrapText(true);

        mostrarEdicion(titulo, null);
    }

    private void mostrarEdicion(String titulo, String error) {
        getChildren().clear();
        getChildren().add(ComponentesUi.subtitulo(titulo));

        if (avisoVersion != null) {
            getChildren().add(ComponentesUi.banner(avisoVersion, "banner-info"));
        }
        if (error != null) {
            getChildren().add(ComponentesUi.banner(error, "banner-error"));
        }

        getChildren().addAll(
                campo("Precio final (COP)", precio),
                campo("Qué incluye", descripcion),
                campo("Condiciones", condiciones),
                campo("Material de apoyo (enlace)", material),
                ComponentesUi.acciones(
                        ComponentesUi.boton("Revisar propuesta", "btn-primary",
                                () -> revisar(titulo)),
                        ComponentesUi.boton("Cancelar", "btn-ghost", cancelar)));
    }

    private void revisar(String titulo) {
        try {
            DatosPropuesta datos = DatosPropuesta.validar(
                    precio.getText(), descripcion.getText(),
                    condiciones.getText(), material.getText());
            mostrarResumen(titulo, datos, null);
        } catch (IllegalArgumentException e) {
            mostrarEdicion(titulo, e.getMessage());
        }
    }

    private void mostrarResumen(String titulo, DatosPropuesta datos, String error) {
        getChildren().clear();
        getChildren().add(ComponentesUi.subtitulo("Revisa tu propuesta"));
        getChildren().add(ComponentesUi.texto(
                "Confirma los datos antes de enviarlos al organizador."));

        if (error != null) {
            getChildren().add(ComponentesUi.banner(error, "banner-error"));
        }

        getChildren().addAll(
                ComponentesUi.dato("Precio final",
                        FormatoUi.moneda(datos.precio())),
                ComponentesUi.dato("Qué incluye", datos.descripcion()),
                ComponentesUi.dato("Condiciones", datos.condiciones()),
                ComponentesUi.dato("Material de apoyo", datos.materialApoyo()),
                ComponentesUi.acciones(
                        ComponentesUi.boton("Enviar propuesta", "btn-primary",
                                () -> confirmar(titulo, datos)),
                        ComponentesUi.boton("Volver a editar", "btn-ghost",
                                () -> mostrarEdicion(titulo, null))));
    }

    private void confirmar(String titulo, DatosPropuesta datos) {
        try {
            enviar.accept(datos);
        } catch (RuntimeException e) {
            mostrarResumen(titulo, datos, e.getMessage());
        }
    }

    private static VBox campo(String etiqueta, Node control) {
        Label titulo = new Label(etiqueta);
        titulo.getStyleClass().add("dato-etiqueta");
        return new VBox(4, titulo, control);
    }
}

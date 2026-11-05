package views.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import views.FormatoUi;

/**
 * Piezas pequeñas de pantalla que se repiten en los casos de uso de
 * proveedores: títulos, tarjetas, datos, chips y avisos dentro de la pantalla.
 * Los estilos están en {@code proveedores.css}.
 */
public final class ComponentesUi {

    private ComponentesUi() {
    }

    public static Label titulo(String texto) {
        return etiqueta(texto, "titulo-pantalla");
    }

    public static Label subtitulo(String texto) {
        return etiqueta(texto, "titulo-seccion");
    }

    /** Texto de apoyo que se ajusta al ancho disponible. */
    public static Label texto(String texto) {
        Label etiqueta = etiqueta(texto, "texto-apoyo");
        etiqueta.setWrapText(true);
        return etiqueta;
    }

    /** Aviso dentro de la pantalla; el estilo es banner-ok, -error, -info o -aviso. */
    public static Label banner(String mensaje, String estilo) {
        Label etiqueta = etiqueta(mensaje, "banner", estilo);
        etiqueta.setWrapText(true);
        etiqueta.setMaxWidth(Double.MAX_VALUE);
        return etiqueta;
    }

    /** Estado corto con color; el estilo es chip-ok, -aviso, -info, -error o -neutro. */
    public static Label chip(String texto, String estilo) {
        return etiqueta(texto, "chip", estilo);
    }

    public static VBox tarjeta(Node... nodos) {
        VBox tarjeta = new VBox(14, nodos);
        tarjeta.getStyleClass().add("tarjeta");
        tarjeta.setPadding(new Insets(20));
        return tarjeta;
    }

    /** Dato con su etiqueta pequeña encima; un valor vacío se muestra como "—". */
    public static VBox dato(String etiqueta, String valor) {
        Label titulo = etiqueta(etiqueta, "dato-etiqueta");

        Label contenido = etiqueta(
                valor == null || valor.isBlank() ? FormatoUi.SIN_DATO : valor,
                "dato-valor");
        contenido.setWrapText(true);

        return new VBox(2, titulo, contenido);
    }

    /**
     * Botón con el estilo que indica su peso: btn-primary para la acción
     * principal, btn-ghost para las secundarias y btn-danger-ghost para las
     * que descartan algo.
     */
    public static Button boton(String texto, String estilo, Runnable accion) {
        Button boton = new Button(texto);
        boton.getStyleClass().add(estilo);
        boton.setOnAction(e -> accion.run());
        return boton;
    }

    /** Fila de acciones alineadas a la izquierda. */
    public static HBox acciones(Node... botones) {
        HBox fila = new HBox(12, botones);
        fila.setAlignment(Pos.CENTER_LEFT);
        return fila;
    }

    /** Título a la izquierda y un chip de estado a la derecha. */
    public static HBox encabezadoConChip(String titulo, Label chip) {
        Label etiqueta = subtitulo(titulo);
        etiqueta.setWrapText(true);
        HBox.setHgrow(etiqueta, Priority.ALWAYS);
        etiqueta.setMaxWidth(Double.MAX_VALUE);

        HBox fila = new HBox(12, etiqueta, chip);
        fila.setAlignment(Pos.CENTER_LEFT);
        return fila;
    }

    /** Pregunta de confirmación dentro de la pantalla, sin ventana emergente. */
    public static VBox confirmacion(
            String titulo,
            String explicacion,
            String textoBoton,
            Runnable aceptar,
            Runnable cancelar) {

        return tarjeta(
                subtitulo(titulo),
                texto(explicacion),
                acciones(
                        boton(textoBoton, "btn-danger", aceptar),
                        boton("Cancelar", "btn-ghost", cancelar)));
    }

    private static Label etiqueta(String texto, String... estilos) {
        Label etiqueta = new Label(texto);
        etiqueta.getStyleClass().addAll(estilos);
        return etiqueta;
    }
}

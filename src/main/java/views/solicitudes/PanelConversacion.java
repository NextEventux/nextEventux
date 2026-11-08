package views.solicitudes;

import java.io.File;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import entities.solicitudes.MensajeSolicitud;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import views.FormatoUi;
import views.components.ComponentesUi;

/**
 * Conversación entre organizador y proveedor sobre una solicitud (CU-12):
 * mensajes en burbujas con su adjunto y un cuadro para escribir. El panel no
 * sabe de qué lado está; recibe cómo leer y cómo enviar los mensajes.
 */
public final class PanelConversacion extends VBox {

    private final Supplier<List<MensajeSolicitud>> cargar;
    private final BiConsumer<String, String> enviar;
    private final int idUsuarioActual;
    private final String interlocutor;

    private final VBox mensajes = new VBox(10);
    private final VBox avisos = new VBox();
    private final TextArea texto = new TextArea();
    private final TextField adjunto = new TextField();

    /**
     * @param cargar devuelve los mensajes de la solicitud
     * @param enviar guarda un mensaje nuevo (texto y adjunto, que puede ser vacío)
     * @param interlocutor nombre de la otra persona de la conversación
     */
    public PanelConversacion(
            Supplier<List<MensajeSolicitud>> cargar,
            BiConsumer<String, String> enviar,
            int idUsuarioActual,
            String interlocutor) {

        super(12);
        this.cargar = cargar;
        this.enviar = enviar;
        this.idUsuarioActual = idUsuarioActual;
        this.interlocutor = interlocutor;
        getStyleClass().add("tarjeta");

        ScrollPane desplazable = new ScrollPane(mensajes);
        desplazable.setFitToWidth(true);
        desplazable.setPrefHeight(260);
        desplazable.getStyleClass().add("panel-desplazable");

        getChildren().addAll(
                ComponentesUi.subtitulo("Conversación"),
                desplazable,
                avisos,
                crearRedaccion());

        recargar();
    }

    private void recargar() {
        mensajes.getChildren().clear();

        try {
            List<MensajeSolicitud> lista = cargar.get();

            if (lista.isEmpty()) {
                mensajes.getChildren().add(ComponentesUi.texto(
                        "Todavía no hay mensajes en esta solicitud."));
            }
            for (MensajeSolicitud mensaje : lista) {
                mensajes.getChildren().add(burbuja(mensaje));
            }
        } catch (RuntimeException e) {
            avisar(e.getMessage(), "banner-error");
        }
    }

    private Node burbuja(MensajeSolicitud mensaje) {
        boolean propio = mensaje.getIdUsuarioRemitente() == idUsuarioActual;

        VBox contenido = new VBox(4);
        contenido.getStyleClass().addAll("burbuja",
                propio ? "burbuja-propia" : "burbuja-ajena");

        Label autor = new Label(
                PresentacionSolicitud.autorMensaje(
                        mensaje, idUsuarioActual, interlocutor)
                + "  ·  " + FormatoUi.fechaHora(mensaje.getFechaHora()));
        autor.getStyleClass().add("dato-etiqueta");
        contenido.getChildren().add(autor);

        String ajuste = PresentacionSolicitud.textoAjuste(mensaje);
        if (ajuste != null) {
            contenido.getChildren().add(ComponentesUi.chip(ajuste, "chip-aviso"));
        }

        contenido.getChildren().add(ComponentesUi.texto(mensaje.getContenido()));

        Node adjuntoVisible = vistaAdjunto(mensaje.getAdjuntoUrl());
        if (adjuntoVisible != null) {
            contenido.getChildren().add(adjuntoVisible);
        }

        HBox fila = new HBox(contenido);
        fila.setAlignment(propio ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        return fila;
    }

    /** Muestra la imagen si el archivo existe; si no, su nombre y tipo. */
    private static Node vistaAdjunto(String valor) {
        AdjuntoMensaje adjunto = AdjuntoMensaje.de(valor);

        if (adjunto == null) {
            return null;
        }

        File archivo = new File(adjunto.ubicacion());
        if (adjunto.tipo() == AdjuntoMensaje.Tipo.IMAGEN && archivo.isFile()) {
            ImageView imagen = new ImageView(new Image(
                    archivo.toURI().toString(), 240, 0, true, true));
            return new VBox(4, imagen, ComponentesUi.texto(adjunto.etiqueta()));
        }

        return ComponentesUi.chip(adjunto.etiqueta(), "chip-neutro");
    }

    private VBox crearRedaccion() {
        texto.setPromptText("Escribe un mensaje");
        texto.setPrefRowCount(2);
        texto.setWrapText(true);
        adjunto.setPromptText("Enlace o archivo (opcional)");
        HBox.setHgrow(adjunto, Priority.ALWAYS);

        HBox fila = new HBox(10, adjunto,
                ComponentesUi.boton("Elegir archivo", "btn-ghost",
                        this::elegirArchivo));

        return new VBox(8, texto, fila,
                ComponentesUi.boton("Enviar mensaje", "btn-primary",
                        this::enviarMensaje));
    }

    private void elegirArchivo() {
        File archivo = new FileChooser().showOpenDialog(
                getScene() == null ? null : getScene().getWindow());

        if (archivo != null) {
            adjunto.setText(archivo.getAbsolutePath());
        }
    }

    private void enviarMensaje() {
        try {
            enviar.accept(texto.getText(), adjunto.getText());
            texto.clear();
            adjunto.clear();
            avisos.getChildren().clear();
            recargar();
        } catch (RuntimeException e) {
            avisar(e.getMessage(), "banner-error");
        }
    }

    private void avisar(String mensaje, String estilo) {
        avisos.getChildren().setAll(ComponentesUi.banner(mensaje, estilo));
    }
}

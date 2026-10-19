package views;

import entities.SesionUsuario;
import services.ServiceAvisosProveedor;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class ViewAvisosProveedor {
    private final ServiceAvisosProveedor service;
    private final SesionUsuario sesion;
    private final ListView<String> avisos = new ListView<>();
    private final Label mensaje = new Label();

    public ViewAvisosProveedor(ServiceAvisosProveedor service, SesionUsuario sesion) {
        this.service = service; this.sesion = sesion;
    }

    public Parent crearPantalla() {
        avisos.setId("avisosProveedor");
        avisos.setPlaceholder(new Label("No tienes avisos."));
        avisos.setCellFactory(lista -> new ListCell<>() {
            @Override protected void updateItem(String texto, boolean empty) {
                super.updateItem(texto, empty); setText(null);
                if (empty || texto == null) { setGraphic(null); return; }
                Label contenido = new Label(texto); contenido.setWrapText(true);
                contenido.maxWidthProperty().bind(avisos.widthProperty().subtract(40)); setGraphic(contenido);
            }
        });
        Button actualizar = new Button("Actualizar avisos"); actualizar.setId("actualizarAvisosProveedor");
        actualizar.setOnAction(e -> actualizar());
        Label titulo = new Label("Mis avisos"); titulo.getStyleClass().add("page-title");
        mensaje.setWrapText(true);
        VBox contenido = new VBox(18, titulo, actualizar, mensaje, avisos);
        contenido.getStyleClass().addAll("app", "content"); VBox.setVgrow(avisos, Priority.ALWAYS);
        actualizar(); return contenido;
    }

    private void actualizar() {
        avisos.getItems().clear(); mensaje.setText("");
        try { avisos.getItems().setAll(service.listar(sesion)); }
        catch (IllegalStateException e) { mensaje.setText(e.getMessage()); }
    }
}

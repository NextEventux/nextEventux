package views;

import java.util.List;

import controllers.ControllerSolicitudes;
import entities.SolicitudEjecucion;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ViewSolicitudesProveedor {

    private final ControllerSolicitudes controller;
    private final int idProveedor;
    private final Runnable volverServicios;

    private final ListView<SolicitudEjecucion> lista
            = new ListView<>();

    public ViewSolicitudesProveedor(
            ControllerSolicitudes controller,
            int idProveedor,
            Runnable volverServicios) {

        this.controller = controller;
        this.idProveedor = idProveedor;
        this.volverServicios = volverServicios;
    }

    public Parent crearPantalla() {

        BorderPane raiz = new BorderPane();

        raiz.getStyleClass().add("app");

        Label titulo = new Label("Solicitudes recibidas");
        titulo.setStyle(
                "-fx-font-size: 26px; -fx-font-weight: bold;");

        Button volver = new Button("Volver a mis servicios");

        volver.setOnAction(event -> volverServicios.run());

        HBox encabezado = new HBox(20, titulo, volver);
        encabezado.setAlignment(Pos.CENTER_LEFT);

        lista.setCellFactory(vista -> new ListCell<>() {

            @Override
            protected void updateItem(
                    SolicitudEjecucion solicitud,
                    boolean vacia) {

                super.updateItem(solicitud, vacia);

                if (vacia || solicitud == null) {
                    setText(null);
                    return;
                }

                setText(
                        "Solicitud #" + solicitud.getIdSolicitud()
                        + "\nEvento: " + solicitud.getIdEvento()
                        + " | Servicio: " + solicitud.getIdServicio()
                        + "\nFecha: " + solicitud.getFechaSolicitud()
                        + "\nEstado: " + solicitud.getEstado()
                );
            }
        });

        Button rechazar = new Button("Rechazar solicitud");

        rechazar.setOnAction(
                event -> rechazarSeleccionada());

        VBox contenido = new VBox(
                20,
                encabezado,
                lista,
                rechazar);

        contenido.setPadding(new Insets(30));
        VBox.setVgrow(lista, javafx.scene.layout.Priority.ALWAYS);

        raiz.setCenter(contenido);

        recargarSolicitudes();

        return raiz;
    }

    private void recargarSolicitudes() {

        try {
            List<SolicitudEjecucion> solicitudes
                    = controller.listarSolicitudesProveedor(idProveedor);

            lista.getItems().setAll(solicitudes);

        } catch (RuntimeException e) {
            mostrarError(e.getMessage());
        }
    }

    private void rechazarSeleccionada() {

        SolicitudEjecucion seleccionada
                = lista.getSelectionModel().getSelectedItem();

        if (seleccionada == null) {
            mostrarError("Selecciona una solicitud.");
            return;
        }

        if (!"PENDIENTE".equalsIgnoreCase(
                seleccionada.getEstado())) {

            mostrarError(
                    "Solo puedes rechazar solicitudes pendientes.");
            return;
        }

        TextInputDialog dialogo = new TextInputDialog();

        dialogo.setTitle("Rechazar solicitud");
        dialogo.setHeaderText(
                "Solicitud #" + seleccionada.getIdSolicitud());
        dialogo.setContentText("Motivo del rechazo:");

        dialogo.showAndWait().ifPresent(motivo -> {

            try {
                controller.rechazarSolicitud(
                        idProveedor,
                        seleccionada.getIdSolicitud(),
                        motivo);

                Alert confirmacion = new Alert(
                        Alert.AlertType.INFORMATION,
                        "Solicitud rechazada correctamente.",
                        ButtonType.OK);

                confirmacion.showAndWait();

                recargarSolicitudes();

            } catch (RuntimeException e) {
                mostrarError(e.getMessage());
            }
        });
    }

    private void mostrarError(String mensaje) {

        Alert alerta = new Alert(
                Alert.AlertType.ERROR,
                mensaje,
                ButtonType.OK);

        alerta.setHeaderText("No se pudo completar la operación");
        alerta.showAndWait();
    }
}

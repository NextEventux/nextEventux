package app;

import controllers.ControllerAutenticacion;
import controllers.ControllerEventos;
import entities.SesionUsuario;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import repositories.RepositoryAutenticacion;
import repositories.RepositoryEvento;
import services.ServiceAutenticacion;
import services.ServiceEventos;
import views.ViewEventos;
import views.ViewLogin;

public class NextEventuxApp extends Application {

    private BorderPane contenedor;
    private ControllerEventos controllerEventos;
    private ControllerAutenticacion controllerAutenticacion;

    @Override
    public void start(Stage stage) {

        controllerEventos = new ControllerEventos(
                new ServiceEventos(new RepositoryEvento()));

        controllerAutenticacion = new ControllerAutenticacion(
                new ServiceAutenticacion(new RepositoryAutenticacion()));

        contenedor = new BorderPane();

        Rectangle2D pantalla = Screen.getPrimary().getVisualBounds();

        double ancho = Math.min(1440, pantalla.getWidth());
        double alto = Math.min(900, pantalla.getHeight());

        Scene scene = new Scene(contenedor, ancho, alto);

        scene.getStylesheets().add(
                getClass().getResource("/styles/eventos.css").toExternalForm());

        scene.getStylesheets().add(
                getClass().getResource("/styles/login.css").toExternalForm());

        mostrarLogin();

        stage.setTitle("NextEventux");
        stage.setMinWidth(Math.min(700, pantalla.getWidth()));
        stage.setMinHeight(Math.min(500, pantalla.getHeight()));
        stage.setResizable(true);
        stage.setScene(scene);
        stage.show();
        stage.centerOnScreen();
    }

    private void mostrarLogin() {

        contenedor.setTop(null);

        contenedor.setCenter(
                new ViewLogin(
                        controllerAutenticacion,
                        this::procesarSesion)
                        .crearPantalla());
    }

    private void procesarSesion(SesionUsuario sesion) {

        String tipo = sesion.getTipoUsuario();

        if (!"ORGANIZADOR".equalsIgnoreCase(tipo)
                && !"PROVEEDOR".equalsIgnoreCase(tipo)) {

            mostrarLogin();
            return;
        }

        Label usuario = new Label(
                sesion.getNombre() + " - " + tipo);

        Button cerrarSesion = new Button("Cerrar sesión");

        cerrarSesion.setOnAction(
                event -> mostrarLogin());

        HBox barra = new HBox(20, usuario, cerrarSesion);

        barra.setAlignment(Pos.CENTER_RIGHT);
        barra.setPadding(new Insets(12));

        contenedor.setTop(barra);

        if ("ORGANIZADOR".equalsIgnoreCase(tipo)) {

            contenedor.setCenter(
                    new ViewEventos(controllerEventos)
                            .crearPantalla());

        } else {

            Label pendiente = new Label(
                    "Sesión iniciada correctamente como proveedor.\n"
                    + "El módulo de proveedores está pendiente de integración.");

            pendiente.setStyle("-fx-font-size: 18px;");
            pendiente.setWrapText(true);

            contenedor.setCenter(pendiente);
            BorderPane.setAlignment(
                    pendiente,
                    Pos.CENTER);
        }
    }
}

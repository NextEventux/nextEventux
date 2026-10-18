package app;

import controllers.ControllerAutenticacion;
import controllers.ControllerEventos;
import controllers.ControllerProveedores;
import entities.SesionUsuario;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import repositories.RepositoryAutenticacion;
import repositories.RepositoryDisponibilidadServicio;
import repositories.RepositoryEvento;
import repositories.RepositoryServicio;
import services.ServiceAutenticacion;
import services.ServiceEventos;
import services.ServiceProveedores;
import views.ViewEventos;
import views.ViewLogin;
import views.ViewProveedores;

public class NextEventuxApp extends Application {

    private StackPane contenedor;

    private ControllerEventos controllerEventos;
    private ControllerProveedores controllerProveedores;
    private ControllerAutenticacion controllerAutenticacion;

    private SesionUsuario sesionActual;

    @Override
    public void start(Stage stage) {

        RepositoryEvento repositoryEvento =
                new RepositoryEvento();

        controllerEventos =
                new ControllerEventos(
                        new ServiceEventos(
                                repositoryEvento));

        RepositoryServicio repositoryServicio =
                new RepositoryServicio();

        RepositoryDisponibilidadServicio repositoryDisponibilidad =
                new RepositoryDisponibilidadServicio();

        controllerProveedores =
                new ControllerProveedores(
                        new ServiceProveedores(
                                repositoryServicio,
                                repositoryDisponibilidad));

        controllerAutenticacion =
                new ControllerAutenticacion(
                        new ServiceAutenticacion(
                                new RepositoryAutenticacion()));

        contenedor =
                new StackPane();

        Scene scene =
                new Scene(
                        contenedor,
                        1200,
                        800);

        scene.getStylesheets().add(
                getClass()
                        .getResource(
                                "/styles/eventos.css")
                        .toExternalForm());

        scene.getStylesheets().add(
                getClass()
                        .getResource(
                                "/styles/proveedores.css")
                        .toExternalForm());

        scene.getStylesheets().add(
                getClass()
                        .getResource(
                                "/styles/login.css")
                        .toExternalForm());

        scene.setOnKeyPressed(event -> {

            if (event.getCode() == KeyCode.F11) {
                stage.setFullScreen(
                        !stage.isFullScreen());
            }
        });

        mostrarLogin();

        stage.setTitle("NextEventux");

        stage.setMinWidth(760);
        stage.setMinHeight(600);

        stage.setResizable(true);
        stage.setFullScreenExitHint("");

        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    private void mostrarLogin() {

        sesionActual = null;

        contenedor
                .getChildren()
                .setAll(
                        new ViewLogin(
                                controllerAutenticacion,
                                this::procesarSesion)
                                .crearPantalla());
    }

    private void procesarSesion(
            SesionUsuario sesion) {

        sesionActual = sesion;

        if ("PROVEEDOR".equalsIgnoreCase(
                sesion.getTipoUsuario())) {

            mostrarProveedores();
            return;
        }

        if ("ORGANIZADOR".equalsIgnoreCase(
                sesion.getTipoUsuario())) {

            mostrarEventos();
            return;
        }

        mostrarLogin();
    }

    private void mostrarEventos() {

        if (sesionActual == null
                || !"ORGANIZADOR".equalsIgnoreCase(
                        sesionActual.getTipoUsuario())) {

            mostrarLogin();
            return;
        }

        contenedor
                .getChildren()
                .setAll(
                        new ViewEventos(
                                controllerEventos,
                                null,
                                sesionActual,
                                this::mostrarLogin)
                                .crearPantalla());
    }

    private void mostrarProveedores() {

        if (sesionActual == null
                || !"PROVEEDOR".equalsIgnoreCase(
                        sesionActual.getTipoUsuario())) {

            mostrarLogin();
            return;
        }

        contenedor
                .getChildren()
                .setAll(
                        new ViewProveedores(
                                controllerProveedores,
                                sesionActual.getIdActor(),
                                null,
                                sesionActual,
                                this::mostrarLogin)
                                .crearPantalla());
    }

    public static void main(String[] args) {
        launch(args);
    }
}

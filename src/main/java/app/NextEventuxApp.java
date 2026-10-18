package app;

import controllers.ControllerAutenticacion;
import controllers.ControllerEventos;
import controllers.ControllerProveedores;
import entities.SesionUsuario;
import javafx.application.Application;
import javafx.scene.Scene;
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
                        1440,
                        1000);

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

        mostrarLogin();

        stage.setTitle("NextEventux");
        stage.setMinWidth(900);
        stage.setMinHeight(700);
        stage.setScene(scene);
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
                                null)
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
                                null)
                                .crearPantalla());
    }

    public static void main(String[] args) {
        launch(args);
    }
}

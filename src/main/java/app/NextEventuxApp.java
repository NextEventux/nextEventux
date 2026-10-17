package app;

import controllers.ControllerEventos;
import controllers.ControllerProveedores;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import repositories.RepositoryDisponibilidadServicio;
import repositories.RepositoryEvento;
import repositories.RepositoryServicio;
import services.ServiceEventos;
import services.ServiceProveedores;
import views.ViewEventos;
import views.ViewProveedores;

public class NextEventuxApp extends Application {

    private StackPane contenedor;
    private ControllerEventos controllerEventos;
    private ControllerProveedores controllerProveedores;

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

        contenedor = new StackPane();

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

        mostrarEventos();

        stage.setTitle("NextEventux");
        stage.setMinWidth(900);
        stage.setMinHeight(700);
        stage.setScene(scene);
        stage.show();
    }

    private void mostrarEventos() {

        contenedor
                .getChildren()
                .setAll(
                        new ViewEventos(
                                controllerEventos,
                                this::mostrarProveedores)
                                .crearPantalla());
    }

    private void mostrarProveedores() {

        contenedor
                .getChildren()
                .setAll(
                        new ViewProveedores(
                                controllerProveedores,
                                1,
                                this::mostrarEventos)
                                .crearPantalla());
    }

    public static void main(String[] args) {
        launch(args);
    }
}

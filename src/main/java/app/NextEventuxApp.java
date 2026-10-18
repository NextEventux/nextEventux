package app;

import controllers.ControllerEventos;
import controllers.ControllerPublicacion;
import repositories.RepositoryPublicacion;
import services.ServicePublicacion;
import views.ViewPublicacion;
import views.ViewCancelacion;
import views.ViewEdicion;
import javafx.scene.layout.BorderPane;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import repositories.RepositoryEvento;
import services.ServiceEventos;
import views.ViewEventos;

public class NextEventuxApp extends Application {
    private Scene scene;
    private Stage stage;
    private ControllerEventos controller;
    private ControllerPublicacion publicaciones;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        RepositoryEvento repository = new RepositoryEvento();
        controller = new ControllerEventos(new ServiceEventos(repository));
        publicaciones = new ControllerPublicacion(new ServicePublicacion(new RepositoryPublicacion(), repository));
        scene = new Scene(new BorderPane(), 1440, 1000);
        mostrarCrear();
        scene.getStylesheets().add(getClass().getResource("/styles/eventos.css").toExternalForm());
        stage.setMinWidth(800);
        stage.setMinHeight(650);
        stage.setScene(scene);
        stage.show();
    }

    private void mostrarCrear() {
        stage.setTitle("NextEventux | Crear evento");
        scene.setRoot(new ViewEventos(controller, this::mostrarPublicacion, this::mostrarCancelacion, this::mostrarEdicion).crearPantalla());
    }

    private void mostrarPublicacion() {
        stage.setTitle("NextEventux | Subir evento al repositorio");
        scene.setRoot(new ViewPublicacion(publicaciones, this::mostrarCrear).crearPantalla());
    }

    private void mostrarCancelacion() {
        stage.setTitle("NextEventux | Cancelar evento");
        scene.setRoot(new ViewCancelacion(controller, this::mostrarCrear).crearPantalla());
    }

    private void mostrarEdicion() {
        stage.setTitle("NextEventux | Editar evento");
        scene.setRoot(new ViewEdicion(controller, this::mostrarCrear).crearPantalla());
    }

    public static void main(String[] args) {
        launch(args);
    }
}

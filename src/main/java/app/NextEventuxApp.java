package app;

import controllers.ControllerEventos;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import repositories.RepositoryEvento;
import services.ServiceEventos;
import views.ViewEventos;

public class NextEventuxApp extends Application {
    @Override
    public void start(Stage stage) {
        RepositoryEvento repository = new RepositoryEvento();
        ControllerEventos controller = new ControllerEventos(new ServiceEventos(repository));
        Scene scene = new Scene(new ViewEventos(controller).crearPantalla(), 1440, 1000);
        scene.getStylesheets().add(getClass().getResource("/styles/eventos.css").toExternalForm());
        stage.setTitle("NextEventux | Crear evento");
        stage.setMinWidth(800);
        stage.setMinHeight(650);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

package app;

import controllers.ControllerEventos;
import controllers.ControllerInvitados;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import repositories.RepositoryEvento;
import services.ServiceEventos;
import services.ServiceInvitados;
import views.ViewEventos;
import views.ViewInvitados;

public class NextEventuxApp extends Application {
    @Override
    public void start(Stage stage) {
        RepositoryEvento repository = new RepositoryEvento();
        ControllerEventos controller = new ControllerEventos(new ServiceEventos(repository));
        Navegacion navegacion = new Navegacion()
                .registrar(Navegacion.Pantalla.EVENTOS, () -> new ViewEventos(controller).crearPantalla())
                .registrar(Navegacion.Pantalla.INVITADOS, () -> new ViewInvitados(new ControllerInvitados(new ServiceInvitados())).crearPantalla());
        Scene scene = navegacion.crearEscena(Navegacion.Pantalla.EVENTOS, 1440, 1000);
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

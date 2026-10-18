package views;

import app.Navegacion;
import controllers.ControllerEventos;
import controllers.ControllerInvitados;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import repositories.RepositoryEvento;
import services.ServiceEventos;
import services.ServiceInvitados;

/**
 * Abre la aplicación directamente en la pantalla de Invitaciones (CU-17).
 * El menú lateral permite ir a Crear evento (CU-01) y volver.
 */
public class LanzadorInvitados extends Application {
    @Override
    public void start(Stage stage) {
        ControllerEventos controller = new ControllerEventos(new ServiceEventos(new RepositoryEvento()));
        Navegacion navegacion = new Navegacion()
                .registrar(Navegacion.Pantalla.EVENTOS, () -> new ViewEventos(controller).crearPantalla())
                .registrar(Navegacion.Pantalla.INVITADOS, () -> new ViewInvitados(new ControllerInvitados(new ServiceInvitados())).crearPantalla());
        Scene scene = navegacion.crearEscena(Navegacion.Pantalla.INVITADOS, 1440, 900);
        scene.getStylesheets().add(getClass().getResource("/styles/eventos.css").toExternalForm());
        stage.setTitle("NextEventux | Invitaciones");
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

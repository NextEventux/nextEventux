package views;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Lanzador SOLO para probar el CU17 de forma aislada.
 * No reemplaza el Main del proyecto; se puede borrar cuando la vista
 * se abra desde el menu principal.
 */
public class LanzadorInvitados extends Application {
    @Override
    public void start(Stage stage) {
        Scene scene = new Scene(new ViewInvitados().crearPantalla(), 1440, 900);
        stage.setTitle("NextEventux - Invitaciones");
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

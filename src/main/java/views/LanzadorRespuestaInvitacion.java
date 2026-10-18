package views;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Abre la pantalla del invitado del CU-16 (Gestionar respuesta a la invitación). */
public class LanzadorRespuestaInvitacion extends Application {
    @Override
    public void start(Stage stage) {
        Scene scene = new Scene(new ViewRespuestaInvitacion().crearPantalla(), 1440, 900);
        stage.setTitle("NextEventux | Mis invitaciones");
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

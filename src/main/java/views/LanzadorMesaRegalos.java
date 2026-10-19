package views;

import controllers.ControllerInvitados;
import services.ServiceInvitados;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Abre la pantalla del invitado del CU-20 (Mesa de regalos virtual). */
public class LanzadorMesaRegalos extends Application {
    @Override
    public void start(Stage stage) {
        Scene scene = new Scene(new ViewMesaRegalos(new ControllerInvitados(new ServiceInvitados())).crearPantalla(), 1440, 900);
        stage.setTitle("NextEventux | Mesa de regalos");
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

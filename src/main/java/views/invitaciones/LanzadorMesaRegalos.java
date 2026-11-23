package views.invitaciones;

import app.AccesoInvitado;
import app.Navegacion;
import controllers.invitaciones.ControllerInvitados;
import inicializacion.CargarDatosEjemplo;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import persistencia.DatabaseConnection;
import services.invitaciones.ServiceInvitados;

import java.sql.Connection;

/**
 * Abre el acceso del invitado directamente en la pantalla del CU-20 (Mesa de regalos virtual).
 * Usa el mismo menú y la misma navegación que la aplicación completa.
 */
public class LanzadorMesaRegalos extends Application {

    @Override
    public void init() throws Exception {
        try (Connection connection = DatabaseConnection.getConnection()) {
            CargarDatosEjemplo.cargarSiEstaVacia(connection);
        }
    }

    @Override
    public void start(Stage stage) {
        AccesoInvitado acceso = new AccesoInvitado(
                new ControllerInvitados(new ServiceInvitados()), Platform::exit);
        Scene scene = acceso.crearEscena(Navegacion.Pantalla.MESA_REGALOS, 1440, 900);
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

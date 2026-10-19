package app;

import config.CargarDatosEjemplo;
import config.DatabaseConnection;
import controllers.ControllerMarketing;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import repositories.RepositoryMarketing;
import services.ServiceMarketing;
import views.ViewMarketing;

// Demostración de la pantalla de segmentos con una base propia y datos de ejemplo. 
public class MarketingDemoApp extends Application {
    private static final String URL_BASE_DEMO = "jdbc:h2:./data/marketing-demo";
    // El seed crea al administrador con este identificador.
    private static final int ID_ADMINISTRADOR_DEMO = 7;

    @Override
    public void start(Stage stage) throws Exception {
        prepararBaseDemo();
        ControllerMarketing controller = new ControllerMarketing(
                new ServiceMarketing(new RepositoryMarketing(URL_BASE_DEMO)));
        Scene scene = new Scene(new ViewMarketing(controller, ID_ADMINISTRADOR_DEMO).crearPantalla(), 1440, 900);
        scene.getStylesheets().add(getClass().getResource("/styles/eventos.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/styles/marketing.css").toExternalForm());
        stage.setTitle("NextEventux | Segmentos de clientes");
        stage.setMinWidth(900);
        stage.setMinHeight(650);
        stage.setScene(scene);
        stage.show();
    }

    // Carga los datos de ejemplo solo si la base de demostración está vacía.
    private void prepararBaseDemo() throws Exception {
        try (Connection connection = DatabaseConnection.getConnection(URL_BASE_DEMO)) {
            if (contarUsuarios(connection) == 0) {
                CargarDatosEjemplo.cargar(connection);
            }
        }
    }

    private int contarUsuarios(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM Usuario")) {
            result.next();
            return result.getInt(1);
        }
    }
}
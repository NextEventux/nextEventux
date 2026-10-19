package app;

import config.CargarDatosEjemplo;
import controllers.ControllerEventos;
import controllers.ControllerAdministracion;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import repositories.RepositoryEvento;
import repositories.RepositoryProveedor;
import services.ServiceAdministracion;
import services.ServiceEventos;
import views.ViewAdministracion;
import views.ViewEventos;

public class NextEventuxApp extends Application {

    @Override
    public void start(Stage stage) {

        // Preparar los escenarios de prueba del CU-028.
        // Los registros existentes se conservan.
        try {
            CargarDatosEjemplo.cargarProveedoresDemostracionCU28();

        } catch (Exception e) {
            e.printStackTrace();

            Alert alerta = new Alert(Alert.AlertType.ERROR);
            alerta.setTitle("Error de inicializacion");
            alerta.setHeaderText(
                    "No se pudieron preparar los datos de prueba del CU-028.");
            alerta.setContentText(
                    "Consulta la consola para conocer el detalle del error.");
            alerta.showAndWait();
        }

        RepositoryEvento repository = new RepositoryEvento();

        ControllerEventos controller = new ControllerEventos(
                new ServiceEventos(repository));

        ViewEventos vistaEventos = new ViewEventos(
                controller,
                () -> abrirVentanaAdministracion(stage));

        Scene scene = new Scene(
                vistaEventos.crearPantalla(), 1440, 1000);

        scene.getStylesheets().addAll(
                getClass().getResource("/styles/eventos.css")
                        .toExternalForm(),
                getClass().getResource("/styles/administracion.css")
                        .toExternalForm());

        stage.setTitle("NextEventux | Crear evento");
        stage.setMinWidth(800);
        stage.setMinHeight(650);
        stage.setScene(scene);
        stage.show();
    }

    private void abrirVentanaAdministracion(Stage ventanaPrincipal) {

        RepositoryProveedor repositoryProveedor =
                new RepositoryProveedor();

        ControllerAdministracion controllerAdministracion =
                new ControllerAdministracion(
                        new ServiceAdministracion(repositoryProveedor));

        Stage ventanaAdministracion = new Stage();
        ventanaAdministracion.initOwner(ventanaPrincipal);

        Scene sceneAdministracion = new Scene(
                new ViewAdministracion(controllerAdministracion)
                        .crearPantalla(),
                1440,
                1000);

        sceneAdministracion.getStylesheets().addAll(
                getClass().getResource("/styles/eventos.css")
                        .toExternalForm(),
                getClass().getResource("/styles/administracion.css")
                        .toExternalForm());

        ventanaAdministracion.setTitle(
                "NextEventux | Aprobar proveedores");

        ventanaAdministracion.setMinWidth(1050);
        ventanaAdministracion.setMinHeight(750);
        ventanaAdministracion.setScene(sceneAdministracion);
        ventanaAdministracion.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
package app;

import javafx.application.Application;
import javafx.stage.Stage;
import repositories.RepositoryMovimientoFinanciero;
import repositories.RepositoryPlanificacion;
import repositories.RepositoryServicio; // 1. Agregas esta importación
import services.ServiceFinanciera;
import controllers.ControllerFinanciera;
import views.ViewFinanciera;

public class Main extends Application {

    public static void main(String[] args) {
        // Esto arranca el motor gráfico de JavaFX
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        RepositoryMovimientoFinanciero repoMovimientos = new RepositoryMovimientoFinanciero();
        RepositoryPlanificacion repoPlanificacion = new RepositoryPlanificacion();
        RepositoryServicio repoServicios = new RepositoryServicio(); // 2. Creas el nuevo repositorio

        // 3. Le pasas los TRES repositorios al servicio
        ServiceFinanciera serviceFinanciera = new ServiceFinanciera(repoMovimientos, repoPlanificacion, repoServicios);
        ControllerFinanciera controllerFinanciera = new ControllerFinanciera(serviceFinanciera);
        
        ViewFinanciera viewFinanciera = new ViewFinanciera(controllerFinanciera);
        
        // Mostrar la vista gráfica del evento 1
        viewFinanciera.mostrarEstadoFinanciero(1);
    }
}
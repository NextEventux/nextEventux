package main.java;

import main.java.repositories.RepositoryMovimientoFinanciero;
import main.java.repositories.RepositoryPlanificacion;
import main.java.services.ServiceFinanciera;
import main.java.controllers.ControllerFinanciera;
import main.java.views.ViewFinanciera;

public class Main {
    public static void main(String[] args) {
        
        // 1. Instanciar la capa de Datos (Repositorios)
        RepositoryMovimientoFinanciero repoMovimientos = new RepositoryMovimientoFinanciero();
        RepositoryPlanificacion repoPlanificacion = new RepositoryPlanificacion();

        // 2. Instanciar la capa de Lógica (Servicios) inyectando los repositorios
        ServiceFinanciera serviceFinanciera = new ServiceFinanciera(repoMovimientos, repoPlanificacion);

        // 3. Instanciar la capa de Control (Controladores) inyectando el servicio
        ControllerFinanciera controllerFinanciera = new ControllerFinanciera(serviceFinanciera);

        // 4. Instanciar la capa de Presentación (Vistas) inyectando el controlador
        ViewFinanciera viewFinanciera = new ViewFinanciera(controllerFinanciera);

        // 5. Ejecutar el caso de uso
        viewFinanciera.mostrarEstadoFinanciero(1);
    }
}
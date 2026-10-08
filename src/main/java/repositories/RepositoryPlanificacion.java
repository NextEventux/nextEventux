package main.java.repositories;

import java.util.ArrayList;
import java.util.List;
import main.java.entities.Planificacion;

public class RepositoryPlanificacion {
    
    // Lista simulando la base de datos
    private List<Planificacion> baseDeDatosSimulada = new ArrayList<>();

    // Nuevo método para buscar la planificación de un evento específico
    public Planificacion buscarPlanificacionPorEvento(int idEvento) {
        for (Planificacion plan : baseDeDatosSimulada) {
            if (plan.getIdEvento() == idEvento) {
                return plan;
            }
        }
        return null;
    }

    public void guardarPlanificacion(Planificacion planificacion) {}
    public Planificacion buscarPlanificacion(int idPlanificacion) { return null; }
    public List<Planificacion> listarVersionesPlanificacion(int idPlanificacion) { return null; }
    public void guardarVersionPlanificacion(Planificacion planificacion) {}
}
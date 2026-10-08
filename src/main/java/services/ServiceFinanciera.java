package main.java.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import main.java.entities.MovimientoFinanciero;
import main.java.entities.Planificacion;
import main.java.repositories.RepositoryMovimientoFinanciero;
import main.java.repositories.RepositoryPlanificacion;

public class ServiceFinanciera {
    
    private RepositoryMovimientoFinanciero repoMovimientos;
    private RepositoryPlanificacion repoPlanificacion;

    // Inyectamos ambos repositorios
    public ServiceFinanciera(RepositoryMovimientoFinanciero repoMovimientos, RepositoryPlanificacion repoPlanificacion) {
        this.repoMovimientos = repoMovimientos;
        this.repoPlanificacion = repoPlanificacion;
    }

    public Map<String, Object> consultarEstadoFinanciero(int idEvento) {
        // 1. Traer datos de ambas tablas
        List<MovimientoFinanciero> movimientos = repoMovimientos.listarMovimientosPorEvento(idEvento);
        Planificacion planificacion = repoPlanificacion.buscarPlanificacionPorEvento(idEvento);
        
        // 2. Calcular balance
        double ingresos = 0.0;
        double egresos = 0.0;

        for (MovimientoFinanciero mov : movimientos) {
            if ("Ingreso".equalsIgnoreCase(mov.getTipo())) {
                ingresos += mov.getValor();
            } else if ("Egreso".equalsIgnoreCase(mov.getTipo())) {
                egresos += mov.getValor();
            }
        }

        // 3. Empaquetar todo en un Mapa
        Map<String, Object> estado = new HashMap<>();
        estado.put("Ingresos", ingresos);
        estado.put("Egresos", egresos);
        estado.put("Balance", ingresos - egresos);
        
        // Si existe una planificación, agregamos sus datos
        if (planificacion != null) {
            estado.put("Planificacion_Descripcion", planificacion.getDescripcion());
            estado.put("Planificacion_Version", planificacion.getVersion());
            estado.put("Planificacion_Estado", planificacion.getEstado());
        } else {
            estado.put("Planificacion_Descripcion", "No hay planificación registrada para este evento.");
        }
        
        return estado;
    }
}
package services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import entities.MovimientoFinanciero;
import entities.Planificacion;
import entities.Servicio;
import repositories.RepositoryMovimientoFinanciero;
import repositories.RepositoryPlanificacion;
import repositories.RepositoryServicio;

public class ServiceFinanciera {
    
    private RepositoryMovimientoFinanciero repoMovimientos;
    private RepositoryPlanificacion repoPlanificacion;
    private RepositoryServicio repoServicios;

    // Inyectamos los TRES repositorios
    public ServiceFinanciera(RepositoryMovimientoFinanciero repoMovimientos, 
                             RepositoryPlanificacion repoPlanificacion, 
                             RepositoryServicio repoServicios) {
        this.repoMovimientos = repoMovimientos;
        this.repoPlanificacion = repoPlanificacion;
        this.repoServicios = repoServicios;
    }

    public Map<String, Object> consultarEstadoFinanciero(int idEvento) {
        // 1. Traer datos de las tablas
        List<MovimientoFinanciero> movimientos = repoMovimientos.listarMovimientosPorEvento(idEvento);
        Planificacion planificacion = repoPlanificacion.buscarPlanificacionPorEvento(idEvento);
        
        // LO NUEVO: Traer los servicios
        List<Servicio> servicios = repoServicios.listarServiciosPorEvento(idEvento); 
        
        // 2. Calcular balance (Tu lógica original intacta)
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

        // LO NUEVO: Agregar las listas al mapa para que la vista gráfica pueda dibujarlas
        estado.put("Movimientos", movimientos);
        estado.put("Servicios", servicios);
        
        return estado;
    }
}
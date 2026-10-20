package services;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import entities.MovimientoFinanciero;
import entities.Planificacion;
import entities.Servicio;
import repositories.RepositoryEvento;
import repositories.RepositoryMovimientoFinanciero;
import repositories.RepositoryPlanificacion;
import repositories.RepositoryServicio;

public class ServiceFinanciera {

    private RepositoryMovimientoFinanciero repoMovimientos;
    private RepositoryPlanificacion repoPlanificacion;
    private RepositoryServicio repoServicios;
    private RepositoryEvento repoEventos;

    public ServiceFinanciera(RepositoryMovimientoFinanciero repoMovimientos,
                             RepositoryPlanificacion repoPlanificacion,
                             RepositoryServicio repoServicios,
                             RepositoryEvento repoEventos) {
        this.repoMovimientos = repoMovimientos;
        this.repoPlanificacion = repoPlanificacion;
        this.repoServicios = repoServicios;
        this.repoEventos = repoEventos;
    }

    // Se conserva el constructor de 3 repositorios para no romper Main ni otros lanzadores.
    public ServiceFinanciera(RepositoryMovimientoFinanciero repoMovimientos,
                             RepositoryPlanificacion repoPlanificacion,
                             RepositoryServicio repoServicios) {
        this(repoMovimientos, repoPlanificacion, repoServicios, new RepositoryEvento());
    }

    public Map<String, Object> consultarEstadoFinanciero(int idEvento) {
        // 1. Traer datos de las tablas
        List<MovimientoFinanciero> movimientos = repoMovimientos.listarMovimientosPorEvento(idEvento);
        Planificacion planificacion = repoPlanificacion.buscarPlanificacionPorEvento(idEvento);
        List<Servicio> servicios = repoServicios.listarServiciosPorEvento(idEvento);
        BigDecimal presupuestoEvento = repoEventos.buscarPresupuesto(idEvento);

        // 2. Calcular balance tolerando MAYÚSCULAS y minúsculas
        double ingresos = 0.0;
        double egresos = 0.0;

        for (MovimientoFinanciero mov : movimientos) {
            if (mov.getTipo() != null) {
                String tipo = mov.getTipo().trim().toUpperCase();
                if ("INGRESO".equals(tipo)) {
                    ingresos += mov.getValor();
                } else if ("EGRESO".equals(tipo)) {
                    egresos += mov.getValor();
                }
            }
        }

        // 3. Presupuesto: lo que el organizador definió vs. lo ya comprometido en contrataciones activas
        double presupuesto = presupuestoEvento != null ? presupuestoEvento.doubleValue() : 0.0;
        double comprometido = 0.0;
        for (Servicio servicio : servicios) {
            comprometido += servicio.getPrecio(); // valor acordado de cada contratación activa
        }
        double disponible = presupuesto - comprometido;
        double porcentajeUsado = presupuesto > 0 ? comprometido / presupuesto : 0.0;

        // 4. Empaquetar todo en un Mapa
        Map<String, Object> estado = new HashMap<>();
        estado.put("Ingresos", ingresos);
        estado.put("Egresos", egresos);
        estado.put("Balance", ingresos - egresos);

        estado.put("Presupuesto_Registrado", presupuestoEvento != null);
        estado.put("Presupuesto", presupuesto);
        estado.put("Comprometido", comprometido);
        estado.put("Disponible", disponible);
        estado.put("Porcentaje_Usado", porcentajeUsado);
        estado.put("Excede_Presupuesto", presupuestoEvento != null && comprometido > presupuesto);

        if (planificacion != null) {
            estado.put("Planificacion_Descripcion", planificacion.getDescripcion());
            estado.put("Planificacion_Version", planificacion.getVersion());
            estado.put("Planificacion_Estado", planificacion.getEstado());
        } else {
            estado.put("Planificacion_Descripcion", "No hay planificación registrada para este evento.");
            estado.put("Planificacion_Estado", "Desconocido");
        }

        estado.put("Movimientos", movimientos);
        estado.put("Servicios", servicios);

        return estado;
    }
}
package entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * CU-13: versión exacta de la propuesta seleccionada, junto con el servicio,
 * el proveedor y el evento a los que quedaría asociada la contratación.
 */
public record ResumenContratacionServicio(
        int idPropuesta,
        int numeroVersion,
        BigDecimal valor,
        String alcance,
        String condiciones,
        int idSolicitud,
        int idNecesidad,
        String necesidad,
        int idEvento,
        String nombreEvento,
        LocalDateTime fechaInicioEvento,
        LocalDateTime fechaFinEvento,
        int idServicio,
        String nombreServicio,
        int idProveedor,
        String nombreProveedor) {
}

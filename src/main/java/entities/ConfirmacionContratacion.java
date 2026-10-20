package entities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** CU-13: datos de la contratación formalizada, para mostrar la confirmación. */
public record ConfirmacionContratacion(
        int idContratacion,
        int idPropuesta,
        int numeroVersion,
        BigDecimal valor,
        String alcance,
        String condiciones,
        String nombreEvento,
        String nombreServicio,
        String nombreProveedor,
        LocalDate fechaServicio,
        LocalTime horaInicio,
        LocalTime horaFin,
        BigDecimal presupuestoDisponible,
        LocalDateTime fechaContratacion) {
}

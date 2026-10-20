package entities;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * CU-13: resultado de comparar la fecha y el horario del evento con la
 * disponibilidad y las contrataciones confirmadas del proveedor.
 * Si no hay disponibilidad, {@code disponible} es false y {@code mensaje} explica el motivo.
 */
public record DisponibilidadContratacion(
        boolean disponible,
        LocalDate fecha,
        LocalTime horaInicioRequerida,
        LocalTime horaFinRequerida,
        LocalTime horaInicioReservada,
        LocalTime horaFinReservada,
        String mensaje) {
}

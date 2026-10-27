package views;

import java.math.BigDecimal;

import entities.DisponibilidadContratacion;
import entities.PresupuestoContratacion;

/**
 * Reglas de presentación de la pantalla de contratación (CU-13).
 *
 * Decide si el organizador puede confirmar y qué textos mostrar a partir de
 * la disponibilidad y el presupuesto ya verificados. No usa JavaFX para que
 * pueda probarse sin abrir una ventana. Las reglas de negocio siguen en el
 * service; aquí solo se interpretan los resultados que este devuelve.
 */
public final class PresentacionContratacion {

    public static final String DISPONIBILIDAD_OK
            = "Disponibilidad verificada para el evento";

    private PresentacionContratacion() {
    }

    /** La contratación solo se habilita si todo fue verificado y es viable. */
    public static boolean puedeConfirmar(
            DisponibilidadContratacion disponibilidad,
            PresupuestoContratacion presupuesto) {

        return disponibilidad != null
                && disponibilidad.disponible()
                && presupuesto != null
                && presupuesto.suficiente();
    }

    /**
     * Motivo por el que no se puede confirmar, o {@code null} si se puede.
     * Se muestra junto al botón, no en una ventana emergente.
     */
    public static String motivoBloqueo(
            DisponibilidadContratacion disponibilidad,
            PresupuestoContratacion presupuesto) {

        if (disponibilidad == null) {
            return "No se pudo verificar la disponibilidad del proveedor.";
        }

        if (!disponibilidad.disponible()) {
            return textoDisponibilidad(disponibilidad);
        }

        if (presupuesto == null) {
            return "No se pudo verificar el presupuesto del evento.";
        }

        if (!presupuesto.suficiente()) {
            return textoPresupuesto(presupuesto);
        }

        return null;
    }

    public static String textoDisponibilidad(
            DisponibilidadContratacion disponibilidad) {

        if (disponibilidad == null) {
            return "No se pudo verificar la disponibilidad del proveedor.";
        }

        if (disponibilidad.disponible()) {
            return DISPONIBILIDAD_OK;
        }

        String mensaje = disponibilidad.mensaje();

        return mensaje == null || mensaje.isBlank()
                ? "El proveedor no está disponible para la fecha y el horario del evento."
                : mensaje;
    }

    public static String textoPresupuesto(PresupuestoContratacion presupuesto) {

        if (presupuesto == null) {
            return "No se pudo verificar el presupuesto del evento.";
        }

        if (presupuesto.suficiente()) {
            return "Presupuesto suficiente para esta contratación";
        }

        BigDecimal faltante = presupuesto.faltante();

        return faltante == null || faltante.signum() <= 0
                ? "El presupuesto del evento no alcanza para esta contratación."
                : "Al presupuesto del evento le faltan "
                + FormatoUi.moneda(faltante) + " para esta contratación.";
    }

    /** Presupuesto que tiene libre el evento antes de contratar. */
    public static BigDecimal presupuestoDisponibleAntes(
            PresupuestoContratacion presupuesto) {

        if (presupuesto == null
                || presupuesto.total() == null
                || presupuesto.comprometido() == null) {
            return null;
        }

        return presupuesto.total().subtract(presupuesto.comprometido());
    }

    /** Presupuesto que quedará libre después de contratar. */
    public static BigDecimal presupuestoDisponibleDespues(
            PresupuestoContratacion presupuesto) {

        return presupuesto == null ? null : presupuesto.disponibleDespues();
    }
}

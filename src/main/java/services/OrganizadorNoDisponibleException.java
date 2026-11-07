package services;

/**
 * CU-026: el organizador seleccionado ya no puede recibir la solicitud,
 * porque dejó de estar activo o verificado (E2) o porque ya no está
 * disponible para la fecha indicada.
 *
 * La vista la usa para devolver al cliente a la lista de resultados.
 * Extiende IllegalStateException para conservar el contrato anterior.
 */
public class OrganizadorNoDisponibleException extends IllegalStateException {

    /** E2: el organizador ya no está activo o verificado. */
    public static final String NO_ACTIVO_VERIFICADO =
            "No es posible enviar la solicitud a este organizador porque ya no "
            + "está activo o verificado. Regresa a la lista y selecciona otro organizador.";

    /** El organizador ya tiene un evento propio en la fecha aproximada. */
    public static final String SIN_DISPONIBILIDAD_FECHA =
            "El organizador ya no está disponible para la fecha indicada. "
            + "Regresa a la lista y selecciona otro organizador.";

    public OrganizadorNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}

package entities;

import java.time.LocalDateTime;

/**
 * Datos del evento, del servicio y de las personas involucradas en una
 * solicitud de ejecución (CU-12). Sirven para mostrar nombres, fechas y lugar
 * en lugar de identificadores. Cualquier campo puede ser null si el dato no
 * se registró.
 */
public record ContextoSolicitud(
        String nombreEvento,
        LocalDateTime inicioEvento,
        LocalDateTime finEvento,
        String ubicacion,
        Integer personas,
        String nombreServicio,
        String nombreOrganizador,
        String nombreProveedor,
        String descripcionNecesidad) {

    /** Contexto vacío para solicitudes cuyos datos no se consultaron. */
    public static ContextoSolicitud vacio() {
        return new ContextoSolicitud(
                null, null, null, null, null, null, null, null, null);
    }
}

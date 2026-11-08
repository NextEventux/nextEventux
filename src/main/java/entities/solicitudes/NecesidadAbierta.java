package entities.solicitudes;

/** Necesidad de un evento que aún no tiene propuesta seleccionada (CU-12). */
public record NecesidadAbierta(
        int idNecesidad,
        int idEvento,
        String descripcion) {
}

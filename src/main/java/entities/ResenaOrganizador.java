package entities;

import java.time.LocalDateTime;

/**
 * Reseña que un cliente dejó sobre un organizador (CU-026).
 * Se muestra en el perfil para que el cliente compare opciones.
 */
public record ResenaOrganizador(
        int calificacion,
        String comentario,
        LocalDateTime fecha) {
}

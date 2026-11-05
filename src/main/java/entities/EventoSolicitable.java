package entities;

import java.time.LocalDateTime;

/** Evento de un organizador para el que todavía puede pedir servicios (CU-12). */
public record EventoSolicitable(
        int idEvento,
        String nombre,
        LocalDateTime inicio) {
}

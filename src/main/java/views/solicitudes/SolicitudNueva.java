package views.solicitudes;

import entities.eventos.EventoSolicitable;
import entities.solicitudes.NecesidadAbierta;

/**
 * Lo que el organizador eligió al pedir un servicio (CU-12): el evento y
 * una necesidad existente o la descripción de una nueva. Valida antes de
 * llamar al controlador para avisar qué falta.
 */
public record SolicitudNueva(
        int idEvento,
        int idNecesidadExistente,
        String descripcionNecesidad) {

    /** Opción de la lista que significa "crear una necesidad nueva". */
    public static final NecesidadAbierta NECESIDAD_NUEVA
            = new NecesidadAbierta(0, 0, "Crear una necesidad nueva");

    public boolean esNecesidadNueva() {
        return idNecesidadExistente == 0;
    }

    /**
     * @param necesidad la elegida, o {@link #NECESIDAD_NUEVA}
     * @param descripcion solo se usa cuando la necesidad es nueva
     */
    public static SolicitudNueva crear(
            EventoSolicitable evento,
            NecesidadAbierta necesidad,
            String descripcion) {

        if (evento == null) {
            throw new IllegalArgumentException(
                    "Elige el evento para el que necesitas el servicio.");
        }
        if (necesidad == null) {
            throw new IllegalArgumentException(
                    "Elige una necesidad o crea una nueva.");
        }

        if (necesidad.idNecesidad() > 0) {
            return new SolicitudNueva(
                    evento.idEvento(), necesidad.idNecesidad(), null);
        }

        String texto = descripcion == null ? "" : descripcion.trim();
        if (texto.isEmpty()) {
            throw new IllegalArgumentException(
                    "Describe lo que necesitas para tu evento.");
        }
        if (texto.length() > 500) {
            throw new IllegalArgumentException(
                    "La descripción puede tener hasta 500 caracteres.");
        }

        return new SolicitudNueva(evento.idEvento(), 0, texto);
    }
}

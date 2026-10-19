package services;

import java.util.List;

import entities.PropuestaServicio;
import entities.SolicitudEjecucion;
import java.math.BigDecimal;
import repositories.RepositoryPropuestaServicio;
import repositories.RepositorySolicitudEjecucion;

public class ServiceSolicitudes {

    private final RepositorySolicitudEjecucion repository;
    private final RepositoryPropuestaServicio repositoryPropuestas;

    public ServiceSolicitudes(RepositorySolicitudEjecucion repository) {
        this(repository, new RepositoryPropuestaServicio());
    }

    public ServiceSolicitudes(
            RepositorySolicitudEjecucion repository,
            RepositoryPropuestaServicio repositoryPropuestas) {

        this.repository = repository;
        this.repositoryPropuestas = repositoryPropuestas;
    }

    public List<SolicitudEjecucion> listarSolicitudesProveedor(
            int idProveedor) {

        if (idProveedor <= 0) {
            throw new IllegalArgumentException(
                    "El proveedor no es valido.");
        }

        return repository.listarPorProveedor(idProveedor);
    }

    public void rechazarSolicitud(
            int idProveedor,
            int idSolicitud,
            String motivo) {

        if (idProveedor <= 0 || idSolicitud <= 0) {
            throw new IllegalArgumentException(
                    "Los identificadores no son validos.");
        }

        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException(
                    "Debes indicar el motivo del rechazo.");
        }

        String motivoLimpio = motivo.trim();

        if (motivoLimpio.length() > 500) {
            throw new IllegalArgumentException(
                    "El motivo no puede superar los 500 caracteres.");
        }

        boolean rechazada = repository.rechazarSolicitud(
                idProveedor,
                idSolicitud,
                motivoLimpio);

        if (!rechazada) {
            throw new IllegalStateException(
                    "La solicitud no existe, no pertenece al proveedor "
                    + "o ya fue respondida.");
        }
    }

    public int enviarPropuesta(
            int idProveedor,
            int idSolicitud,
            BigDecimal valor,
            String descripcion,
            String condiciones) {

        if (idProveedor <= 0 || idSolicitud <= 0) {
            throw new IllegalArgumentException(
                    "Los identificadores no son validos.");
        }

        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "El precio final debe ser mayor que cero.");
        }

        if (valor.scale() > 2
                && valor.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException(
                    "El precio no puede tener mas de dos decimales.");
        }

        if (valor.compareTo(new BigDecimal("9999999999.99")) > 0) {
            throw new IllegalArgumentException(
                    "El precio supera el valor permitido.");
        }

        if (descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException(
                    "Debes describir el alcance de la propuesta.");
        }

        String descripcionLimpia = descripcion.trim();

        if (descripcionLimpia.length() > 255) {
            throw new IllegalArgumentException(
                    "La descripcion no puede superar los 255 caracteres.");
        }

        if (condiciones == null || condiciones.isBlank()) {
            throw new IllegalArgumentException(
                    "Debes indicar las condiciones de la propuesta.");
        }

        String condicionesLimpias = condiciones.trim();

        if (condicionesLimpias.length() > 1000) {
            throw new IllegalArgumentException(
                    "Las condiciones no pueden superar los 1000 caracteres.");
        }

        return repositoryPropuestas.guardarNuevaVersion(
                idProveedor,
                idSolicitud,
                valor,
                descripcionLimpia,
                condicionesLimpias);
    }

    public List<PropuestaServicio> listarHistorialPropuestas(
            int idProveedor,
            int idSolicitud) {

        if (idProveedor <= 0 || idSolicitud <= 0) {
            throw new IllegalArgumentException(
                    "Los identificadores no son validos.");
        }

        return repositoryPropuestas.listarHistorial(
                idProveedor,
                idSolicitud);
    }
}

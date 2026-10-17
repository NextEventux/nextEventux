package services;

import entities.DisponibilidadServicio;
import entities.Servicio;
import repositories.RepositoryDisponibilidadServicio;
import repositories.RepositoryServicio;

import java.util.Date;
import java.util.List;

// Aqui va la logica relacionada con las acciones del modulo de proveedores
public class ServiceProveedores {

    private RepositoryServicio repositoryServicio;
    private RepositoryDisponibilidadServicio repositoryDisponibilidadServicio;

    public ServiceProveedores(
            RepositoryServicio repositoryServicio,
            RepositoryDisponibilidadServicio repositoryDisponibilidadServicio) {

        this.repositoryServicio = repositoryServicio;
        this.repositoryDisponibilidadServicio =
                repositoryDisponibilidadServicio;
    }

    public void crearServicio(Servicio datos) {

        if (datos == null) {
            throw new IllegalArgumentException(
                    "Los datos del servicio son obligatorios.");
        }

        if (datos.getEstado() == null
                || datos.getEstado().isBlank()) {
            datos.setEstado("BORRADOR");
        }

        repositoryServicio.guardarServicio(datos);
    }

    public void modificarServicio(
            int idServicio,
            Servicio datos) {

        Servicio existente =
                repositoryServicio.buscarServicio(idServicio);

        if (existente == null) {
            throw new IllegalArgumentException(
                    "El servicio no existe.");
        }

        datos.setIdServicio(idServicio);

        if (datos.getEstado() == null
                || datos.getEstado().isBlank()) {
            datos.setEstado(existente.getEstado());
        }

        repositoryServicio.actualizarServicio(datos);
    }

    public void registrarDisponibilidadServicio(
            int idServicio,
            Date fecha,
            String horario) {

        if (repositoryServicio.buscarServicio(idServicio) == null) {
            throw new IllegalArgumentException(
                    "El servicio no existe.");
        }

        if (fecha == null) {
            throw new IllegalArgumentException(
                    "La fecha es obligatoria.");
        }

        if (repositoryDisponibilidadServicio
                .existeCruceContratacion(
                        idServicio,
                        fecha,
                        horario)) {

            throw new IllegalArgumentException(
                    "Ese horario ya esta ocupado por una contratacion.");
        }

        if (repositoryDisponibilidadServicio
                .existeCruceDisponibilidad(
                        idServicio,
                        fecha,
                        horario)) {

            throw new IllegalArgumentException(
                    "Ese horario se cruza con otra disponibilidad.");
        }

        DisponibilidadServicio disponibilidad =
                new DisponibilidadServicio();

        disponibilidad.setIdServicio(idServicio);
        disponibilidad.setFecha(fecha);
        disponibilidad.setHorario(horario);

        repositoryDisponibilidadServicio
                .guardarDisponibilidad(disponibilidad);
    }

    public void publicarServicio(int idServicio) {

        Servicio servicio =
                repositoryServicio.buscarServicio(idServicio);

        if (servicio == null) {
            throw new IllegalArgumentException(
                    "El servicio no existe.");
        }

        validarServicioParaPublicar(servicio);

        if (repositoryDisponibilidadServicio
                .contarDisponibilidades(idServicio) == 0) {

            throw new IllegalArgumentException(
                    "Debe registrar al menos una disponibilidad.");
        }

        repositoryServicio.actualizarEstado(
                idServicio,
                "DISPONIBLE");
    }

    public void retirarServicio(int idServicio) {

        Servicio servicio =
                repositoryServicio.buscarServicio(idServicio);

        if (servicio == null) {
            throw new IllegalArgumentException(
                    "El servicio no existe.");
        }

        if (repositoryServicio
                .tieneSolicitudesPendientes(idServicio)) {

            throw new IllegalStateException(
                    "No se puede retirar el servicio porque tiene solicitudes pendientes.");
        }

        repositoryServicio.actualizarEstado(
                idServicio,
                "RETIRADO");
    }

    public Servicio buscarServicio(int idServicio) {
        return repositoryServicio.buscarServicio(idServicio);
    }

    public List<Servicio> listarServiciosProveedor(
            int idProveedor) {

        return repositoryServicio
                .listarServiciosProveedor(idProveedor);
    }

    public List<DisponibilidadServicio>
            listarDisponibilidadesServicio(
                    int idServicio) {

        return repositoryDisponibilidadServicio
                .listarDisponibilidadesServicio(idServicio);
    }

    public void eliminarDisponibilidad(
            int idDisponibilidad) {

        repositoryDisponibilidadServicio
                .eliminarDisponibilidad(idDisponibilidad);
    }

    private void validarServicioParaPublicar(
            Servicio servicio) {

        if (servicio.getIdProveedor() <= 0) {
            throw new IllegalArgumentException(
                    "El proveedor es obligatorio.");
        }

        if (servicio.getNombre() == null
                || servicio.getNombre().isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre es obligatorio.");
        }

        if (servicio.getTipo() == null
                || servicio.getTipo().isBlank()) {
            throw new IllegalArgumentException(
                    "El tipo es obligatorio.");
        }

        if (servicio.getDescripcion() == null
                || servicio.getDescripcion().isBlank()) {
            throw new IllegalArgumentException(
                    "La descripcion es obligatoria.");
        }

        if (servicio.getPrecio() <= 0) {
            throw new IllegalArgumentException(
                    "El precio debe ser mayor que cero.");
        }

        if (servicio.getCategoria() == null
                || servicio.getCategoria().isBlank()) {
            throw new IllegalArgumentException(
                    "La categoria es obligatoria.");
        }

        if (servicio.getCiudadZona() == null
                || servicio.getCiudadZona().isBlank()) {
            throw new IllegalArgumentException(
                    "La ciudad o zona es obligatoria.");
        }

        if (servicio.getAnticipacionMinima() < 0) {
            throw new IllegalArgumentException(
                    "La anticipacion minima no puede ser negativa.");
        }

        if (servicio.getCantidadMaximaPersonas() != null
                && servicio.getCantidadMaximaPersonas() <= 0) {

            throw new IllegalArgumentException(
                    "La cantidad maxima debe ser mayor que cero.");
        }
    }
}

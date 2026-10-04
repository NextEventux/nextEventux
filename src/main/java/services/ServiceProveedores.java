package services;

import entities.Servicio;
import repositories.RepositoryServicio;
import java.util.Date;
import entities.DisponibilidadServicio;
import repositories.RepositoryDisponibilidadServicio;

// Aqui va la logica relacionada con las acciones del modulo de proveedores
public class ServiceProveedores {

    // El service usa el repositorio para guardar y consultar la informacion
    private RepositoryServicio repositoryServicio;
    private RepositoryDisponibilidadServicio repositoryDisponibilidadServicio;

    public ServiceProveedores(
            RepositoryServicio repositoryServicio,
            RepositoryDisponibilidadServicio repositoryDisponibilidadServicio) {

        this.repositoryServicio = repositoryServicio;
        this.repositoryDisponibilidadServicio = repositoryDisponibilidadServicio;
    }

    // Recibe el servicio ya construido y lo manda al repositorio para guardarlo
    public void crearServicio(Servicio datos) {
        repositoryServicio.guardarServicio(datos);
    }

    // Registra una fecha y horario en los que el servicio puede estar disponible
    public void registrarDisponibilidadServicio(
            int idServicio,
            Date fecha,
            String horario) {

        DisponibilidadServicio disponibilidad = new DisponibilidadServicio();

        disponibilidad.setIdServicio(idServicio);
        disponibilidad.setFecha(fecha);
        disponibilidad.setHorario(horario);

        repositoryDisponibilidadServicio.guardarDisponibilidad(disponibilidad);
    }

}
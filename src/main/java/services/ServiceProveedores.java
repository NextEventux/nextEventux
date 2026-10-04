package services;

import entities.Servicio;
import repositories.RepositoryServicio;

// Aqui va la logica relacionada con las acciones del modulo de proveedores
public class ServiceProveedores {

    // El service usa el repositorio para guardar y consultar la informacion
    private RepositoryServicio repositoryServicio;

    public ServiceProveedores(RepositoryServicio repositoryServicio) {
        this.repositoryServicio = repositoryServicio;
    }

    // Recibe el servicio ya construido y lo manda al repositorio para guardarlo
    public void crearServicio(Servicio datos) {
        repositoryServicio.guardarServicio(datos);
    }
}
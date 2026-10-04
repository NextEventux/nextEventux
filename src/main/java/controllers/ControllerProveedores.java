package controllers;

import entities.Servicio;
import services.ServiceProveedores;
import java.util.Date;
import entities.DisponibilidadServicio;
import repositories.RepositoryDisponibilidadServicio;

// Recibe las acciones relacionadas con proveedores y las envia al service
public class ControllerProveedores {

    // El controller usa el service para ejecutar la logica del modulo
    private ServiceProveedores serviceProveedores;

    public ControllerProveedores(ServiceProveedores serviceProveedores) {
        this.serviceProveedores = serviceProveedores;
    }

    // Recibe los datos del servicio y solicita su creacion al service
    public void crearServicio(Servicio datos) {
        serviceProveedores.crearServicio(datos);
    }

    // Envia al service la fecha y horario disponibles para un servicio
    public void registrarDisponibilidadServicio(
            int idServicio,
            Date fecha,
            String horario) {

        serviceProveedores.registrarDisponibilidadServicio(
                idServicio,
                fecha,
                horario);
    }

}
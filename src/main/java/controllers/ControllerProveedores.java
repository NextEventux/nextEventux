package controllers;

import entities.Servicio;
import services.ServiceProveedores;

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
}
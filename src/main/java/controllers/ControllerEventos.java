package controllers;

import entities.Evento;
import services.ServiceEventos;

public class ControllerEventos {

    private final ServiceEventos serviceEventos;

    public ControllerEventos(ServiceEventos serviceEventos) {
        this.serviceEventos = serviceEventos;
    }

    public void crearEvento(Evento datos) {
        serviceEventos.crearEvento(datos);
    }
}
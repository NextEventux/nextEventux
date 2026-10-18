package controllers;

import entities.Evento;
import entities.Organizador;
import java.util.List;
import services.ServiceEventos;

public class ControllerEventos {

    private final ServiceEventos serviceEventos;

    public ControllerEventos(ServiceEventos serviceEventos) {
        this.serviceEventos = serviceEventos;
    }

    public List<Organizador> listarOrganizadores() {
        return serviceEventos.listarOrganizadores();
    }

    public void crearEvento(Evento datos) {
        serviceEventos.crearEvento(datos);
    }
}
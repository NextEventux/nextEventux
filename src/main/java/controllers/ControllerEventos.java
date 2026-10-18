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

    public List<Evento> listarEventos(int organizador) { return serviceEventos.listarEventos(organizador); }
    public void cancelarEvento(int evento, int organizador, boolean confirmado) { serviceEventos.cancelarEvento(evento, organizador, confirmado); }
    public List<String> listarAvisosCancelacion(int evento, int organizador) { return serviceEventos.listarAvisosCancelacion(evento, organizador); }

    public void crearEvento(Evento datos) {
        serviceEventos.crearEvento(datos);
    }
}
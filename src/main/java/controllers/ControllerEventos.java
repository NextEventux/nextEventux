package controllers;

import entities.Evento;
import entities.EdicionEvento;
import entities.Organizador;
import entities.SesionUsuario;
import java.util.List;
import services.ServiceEventos;

public class ControllerEventos {

    private final ServiceEventos serviceEventos;
    private final SesionUsuario sesion;

    public ControllerEventos(ServiceEventos serviceEventos) {
        this(serviceEventos,null);
    }

    public ControllerEventos(ServiceEventos serviceEventos, SesionUsuario sesion) {
        this.serviceEventos = serviceEventos;
        this.sesion = sesion;
    }

    public List<Organizador> listarOrganizadores() {
        return serviceEventos.listarOrganizadores();
    }

    public List<Evento> listarEventosParaCancelar() { return serviceEventos.listarEventosParaCancelar(sesion); }
    public String nombreOrganizadorCancelacion() { return serviceEventos.nombreOrganizador(sesion); }
    public void cancelarEvento(int evento, boolean confirmado) { serviceEventos.cancelarEvento(evento, sesion, confirmado); }
    public List<String> listarAvisosCancelacion(int evento) { return serviceEventos.listarAvisosCancelacion(evento, sesion); }

    public List<Evento> listarEventosParaEditar() { return serviceEventos.listarEventosParaEditar(sesion); }
    public String nombreOrganizadorEdicion() { return serviceEventos.nombreOrganizador(sesion); }
    public Evento consultarEventoParaEditar(int evento) { return serviceEventos.consultarEventoParaEditar(evento,sesion); }
    public void editarEvento(int evento,EdicionEvento datos) { serviceEventos.editarEvento(evento,sesion,datos); }

    public void crearEvento(Evento datos) {
        if (sesion != null) {
            serviceEventos.nombreOrganizador(sesion);
            if (datos != null && datos.getIdOrganizador() != sesion.getIdActor())
                throw new IllegalArgumentException("El evento debe pertenecer a tu cuenta.");
        }
        serviceEventos.crearEvento(datos);
    }
}
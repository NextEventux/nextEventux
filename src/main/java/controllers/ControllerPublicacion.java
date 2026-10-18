package controllers;

import entities.*;
import services.ServicePublicacion;
import java.util.List;

public class ControllerPublicacion {
    private final ServicePublicacion service;
    public ControllerPublicacion(ServicePublicacion service) { this.service = service; }
    public List<Organizador> listarOrganizadores() { return service.listarOrganizadores(); }
    public List<Administrador> listarAdministradores() { return service.listarAdministradores(); }
    public List<Evento> listarEventos(int organizador) { return service.listarEventos(organizador); }
    public void solicitar(int organizador, int evento) { service.solicitar(organizador, evento); }
    public void revisar(int solicitud, int administrador, boolean aprobar, String motivo) { service.revisar(solicitud, administrador, aprobar, motivo); }
    public List<SolicitudPublicacion> listarSolicitudes(Integer organizador) { return service.listarSolicitudes(organizador); }
    public List<PlantillaEvento> listarPlantillas() { return service.listarPlantillas(); }
    public List<String> notificacionesOrganizador(int organizador) { return service.notificacionesOrganizador(organizador); }
    public List<String> notificacionesAdministrador(int administrador) { return service.notificacionesAdministrador(administrador); }
}

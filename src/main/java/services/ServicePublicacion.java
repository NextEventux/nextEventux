package services;

import entities.*;
import repositories.RepositoryEvento;
import repositories.RepositoryPublicacion;
import java.util.List;

public class ServicePublicacion {
    private final RepositoryPublicacion repository;
    private final RepositoryEvento eventos;

    public ServicePublicacion(RepositoryPublicacion repository, RepositoryEvento eventos) {
        this.repository = repository;
        this.eventos = eventos;
    }

    public List<Organizador> listarOrganizadores() { return eventos.listarOrganizadores(); }
    public List<Administrador> listarAdministradores() { return repository.listarAdministradores(); }
    public List<Evento> listarEventos(int organizador) {
        return eventos.listarEventos().stream().filter(e -> e.getIdOrganizador() == organizador).toList();
    }
    public void solicitar(int organizador, int evento) {
        if (organizador <= 0 || evento <= 0) throw new IllegalArgumentException("Selecciona un organizador y un evento.");
        repository.solicitar(organizador, evento);
    }
    public void revisar(int solicitud, int administrador, boolean aprobar, String motivo) {
        if (solicitud <= 0 || administrador <= 0) throw new IllegalArgumentException("Selecciona un administrador y una solicitud.");
        if (!aprobar && (motivo == null || motivo.isBlank() || motivo.trim().length() > 250))
            throw new IllegalArgumentException("Escribe el motivo del rechazo, de hasta 250 caracteres.");
        repository.revisar(solicitud, administrador, aprobar, aprobar ? null : motivo.trim());
    }
    public List<SolicitudPublicacion> listarSolicitudes(Integer organizador) { return repository.listarSolicitudes(organizador); }
    public List<PlantillaEvento> listarPlantillas() { return repository.listarPlantillas(); }
    public List<String> notificacionesOrganizador(int organizador) {
        return repository.listarNotificaciones(repository.usuarioOrganizador(organizador));
    }
    public List<String> notificacionesAdministrador(int administrador) { return repository.listarNotificaciones(administrador); }
}

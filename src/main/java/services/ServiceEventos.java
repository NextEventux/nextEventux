package services;

import entities.Evento;
import repositories.RepositoryEvento;

public class ServiceEventos {

    private final RepositoryEvento repositoryEvento;

    public ServiceEventos(RepositoryEvento repositoryEvento) {
        this.repositoryEvento = repositoryEvento;
    }

    public void crearEvento(Evento datos) {
        if (!validarInformacionEvento(datos)) {
            throw new IllegalArgumentException("Los datos del evento son inválidos.");
        }

        datos.setEstado("En planeación");
        repositoryEvento.guardarEvento(datos);
    }

    public boolean validarInformacionEvento(Evento datos) {
        if (datos == null) {
            return false;
        }

        if (datos.getNombre() == null || datos.getNombre().isBlank()) {
            return false;
        }

        if (datos.getTipo() == null || datos.getTipo().isBlank()) {
            return false;
        }

        if (datos.getFecha() == null || datos.getFecha().isBlank()) {
            return false;
        }

        if (datos.getLugar() == null || datos.getLugar().isBlank()) {
            return false;
        }

        if (datos.getPresupuesto() < 0) {
            return false;
        }

        if (datos.getInvitadosIniciales() < 0) {
            return false;
        }

        return true;
    }
}
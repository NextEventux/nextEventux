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
        datos.setEstado(Evento.ESTADO_INICIAL);
        repositoryEvento.guardarEvento(datos);
    }

    public boolean validarInformacionEvento(Evento datos) {
        if (datos == null || datos.getIdOrganizador() <= 0) {
            return false;
        }
        if (!textoValido(datos.getNombre(), 150) || !textoValido(datos.getTipo(), 100)
                || !textoValido(datos.getUbicacion(), 255)) {
            return false;
        }
        if (datos.getDescripcion() != null && datos.getDescripcion().length() > 255) {
            return false;
        }
        if (datos.getFechaInicio() == null || datos.getFechaFin() == null
                || datos.getFechaFin().isBefore(datos.getFechaInicio())) {
            return false;
        }
        if (datos.getPresupuesto() == null || datos.getPresupuesto().signum() < 0
                || datos.getPresupuesto().compareTo(new java.math.BigDecimal("9999999999.99")) > 0) {
            return false;
        }
        try {
            datos.getPresupuesto().setScale(2, java.math.RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            return false;
        }
        return datos.getInvitadosIniciales() >= 0;
    }

    private boolean textoValido(String texto, int longitudMaxima) {
        return texto != null && !texto.isBlank() && texto.length() <= longitudMaxima;
    }
}

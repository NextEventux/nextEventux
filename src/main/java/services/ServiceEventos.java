package services;

import entities.Evento;
import entities.Organizador;
import java.util.List;
import repositories.RepositoryEvento;

public class ServiceEventos {
    private final RepositoryEvento repositoryEvento;

    public ServiceEventos(RepositoryEvento repositoryEvento) {
        this.repositoryEvento = repositoryEvento;
    }

    public List<Organizador> listarOrganizadores() {
        return repositoryEvento.listarOrganizadores();
    }

    public void crearEvento(Evento datos) {
        String error = obtenerError(datos);
        if (error != null) {
            throw new IllegalArgumentException(error);
        }
        datos.setEstado(Evento.ESTADO_INICIAL);
        repositoryEvento.guardarEvento(datos);
    }

    public boolean validarInformacionEvento(Evento datos) {
        return obtenerError(datos) == null;
    }

    private String obtenerError(Evento datos) {
        if (datos == null) return "Completa los datos del evento.";
        if (datos.getIdOrganizador() <= 0) return "Ingresa un identificador de organizador mayor que cero.";
        if (!textoValido(datos.getNombre(), 150)) return "Ingresa un nombre de hasta 150 caracteres.";
        if (!textoValido(datos.getTipo(), 100)) return "Selecciona el tipo de evento.";
        if (!textoValido(datos.getUbicacion(), 255)) return "Ingresa un lugar de hasta 255 caracteres.";
        if (datos.getDescripcion() != null && datos.getDescripcion().length() > 255)
            return "La descripción debe tener hasta 255 caracteres.";
        if (datos.getFechaInicio() == null || datos.getFechaFin() == null)
            return "Selecciona las fechas de inicio y fin.";
        if (datos.getFechaFin().isBefore(datos.getFechaInicio()))
            return "El fin del evento no puede ser anterior al inicio.";
        if (datos.getPresupuesto() == null || datos.getPresupuesto().signum() < 0)
            return "Ingresa un presupuesto mayor o igual a cero.";
        if (datos.getPresupuesto().compareTo(new java.math.BigDecimal("9999999999.99")) > 0)
            return "El presupuesto no puede superar 9999999999.99 COP.";
        try {
            datos.getPresupuesto().setScale(2, java.math.RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            return "El presupuesto debe tener como máximo dos decimales.";
        }
        if (datos.getInvitadosIniciales() == null || datos.getInvitadosIniciales() < 0) return "Los invitados iniciales deben ser mayores o iguales a cero.";
        return null;
    }

    private boolean textoValido(String texto, int longitudMaxima) {
        return texto != null && !texto.isBlank() && texto.length() <= longitudMaxima;
    }
}

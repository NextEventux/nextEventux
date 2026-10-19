package services;

import entities.Evento;
import entities.EdicionEvento;
import entities.Organizador;
import entities.SesionUsuario;
import java.util.List;
import repositories.RepositoryEvento;
import repositories.RepositoryCancelacion;

public class ServiceEventos {
    private final RepositoryEvento repositoryEvento;
    private final RepositoryCancelacion cancelaciones;

    public ServiceEventos(RepositoryEvento repositoryEvento) {
        this(repositoryEvento, new RepositoryCancelacion());
    }

    public ServiceEventos(RepositoryEvento repositoryEvento, RepositoryCancelacion cancelaciones) {
        this.repositoryEvento = repositoryEvento;
        this.cancelaciones = cancelaciones;
    }

    public List<Organizador> listarOrganizadores() {
        return repositoryEvento.listarOrganizadores();
    }

    public List<Evento> listarEventos(int organizador) {
        return repositoryEvento.listarEventos().stream().filter(e -> e.getIdOrganizador() == organizador).toList();
    }

    public void cancelarEvento(int evento, int organizador, boolean confirmado) {
        if (evento <= 0 || organizador <= 0) throw new IllegalArgumentException("Selecciona un organizador y un evento.");
        if (!confirmado) throw new IllegalArgumentException("Confirma explícitamente que deseas cancelar el evento.");
        cancelaciones.cancelar(evento, organizador);
    }

    public List<String> listarAvisosCancelacion(int evento, int organizador) {
        return cancelaciones.listarAvisos(evento, organizador);
    }

    public List<Evento> listarEventosParaEditar(SesionUsuario sesion) {
        return repositoryEvento.listarEventosPorOrganizador(validarSesionEdicion(sesion));
    }

    public String nombreOrganizadorEdicion(SesionUsuario sesion) {
        validarSesionEdicion(sesion);
        return sesion.getNombre();
    }

    private int validarSesionEdicion(SesionUsuario sesion) {
        if(sesion==null || sesion.getIdUsuario()<=0 || sesion.getIdActor()<=0
                || !"ORGANIZADOR".equalsIgnoreCase(sesion.getTipoUsuario())
                || !"ACTIVO".equalsIgnoreCase(sesion.getEstadoUsuario())
                || !"ACTIVO".equalsIgnoreCase(sesion.getEstadoActor())
                || !repositoryEvento.organizadorActivo(sesion.getIdUsuario(),sesion.getIdActor()))
            throw new IllegalStateException("Inicia sesión como organizador activo para editar tus eventos.");
        return sesion.getIdActor();
    }

    public Evento consultarEventoParaEditar(int evento, SesionUsuario sesion) {
        int organizador=validarSesionEdicion(sesion);
        Evento actual=repositoryEvento.buscarEvento(evento,organizador);
        if(actual==null) throw new IllegalArgumentException("Selecciona un evento existente de tu cuenta.");
        if(!Evento.ESTADO_INICIAL.equalsIgnoreCase(actual.getEstado()))
            throw new IllegalArgumentException("No se puede editar: el evento está en estado " + actual.getEstado() + ".");
        return actual;
    }

    public void editarEvento(int evento, SesionUsuario sesion, EdicionEvento datos) {
        int organizador=validarSesionEdicion(sesion);
        if(evento<=0) throw new IllegalArgumentException("Selecciona un evento de tu cuenta.");
        if(datos==null) throw new IllegalArgumentException("Completa los datos del evento.");
        String error=errorDatosGenerales(datos.nombre(),datos.tipo(),datos.inicio(),datos.fin(),datos.lugar());
        if(error!=null) throw new IllegalArgumentException(error);
        repositoryEvento.editarEvento(evento,organizador,datos);
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
        String general=errorDatosGenerales(datos.getNombre(),datos.getTipo(),datos.getFechaInicio(),datos.getFechaFin(),datos.getUbicacion());
        if(general!=null) return general;
        if (datos.getDescripcion() != null && datos.getDescripcion().length() > 255)
            return "La descripción debe tener hasta 255 caracteres.";
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

    private String errorDatosGenerales(String nombre,String tipo,java.time.LocalDateTime inicio,java.time.LocalDateTime fin,String lugar) {
        if(!textoValido(nombre,150)) return "Ingresa un nombre de hasta 150 caracteres.";
        if(!textoValido(tipo,100)) return "Selecciona el tipo de evento.";
        if(!textoValido(lugar,255)) return "Ingresa un lugar de hasta 255 caracteres.";
        if(inicio==null || fin==null) return "Selecciona las fechas de inicio y fin.";
        if(fin.isBefore(inicio)) return "El fin del evento no puede ser anterior al inicio.";
        return null;
    }

    private boolean textoValido(String texto, int longitudMaxima) {
        return texto != null && !texto.isBlank() && texto.length() <= longitudMaxima;
    }
}

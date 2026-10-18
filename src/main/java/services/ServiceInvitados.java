package services;

import entities.Evento;
import entities.Invitacion;
import entities.Invitado;
import entities.Lugar;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import repositories.RepositoryEvento;
import repositories.RepositoryInvitacion;
import repositories.RepositoryInvitado;
import repositories.RepositoryLugar;

/**
 * Reglas de negocio del módulo de invitados.
 * CU-17: crear, actualizar y revocar invitaciones y registrar sus invitados,
 * validando el aforo del lugar y que un invitado no tenga dos invitaciones activas
 * en el mismo evento (R40).
 */
public class ServiceInvitados {
    private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final RepositoryInvitacion repositoryInvitacion;
    private final RepositoryInvitado repositoryInvitado;
    private final RepositoryLugar repositoryLugar;
    private final RepositoryEvento repositoryEvento;

    public ServiceInvitados() {
        this(new RepositoryInvitacion(), new RepositoryInvitado(), new RepositoryLugar(), new RepositoryEvento());
    }

    public ServiceInvitados(RepositoryInvitacion repositoryInvitacion, RepositoryInvitado repositoryInvitado,
                            RepositoryLugar repositoryLugar, RepositoryEvento repositoryEvento) {
        this.repositoryInvitacion = repositoryInvitacion;
        this.repositoryInvitado = repositoryInvitado;
        this.repositoryLugar = repositoryLugar;
        this.repositoryEvento = repositoryEvento;
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------
    public List<Evento> listarEventos() {
        return repositoryEvento.listarEventos();
    }

    public List<Lugar> listarLugares() {
        return repositoryLugar.listarLugares();
    }

    public List<Invitacion> listarInvitaciones(int eventoId) {
        return repositoryInvitacion.listarInvitaciones(eventoId);
    }

    public List<Invitado> listarInvitados(int idInvitacion) {
        return repositoryInvitado.listarInvitados(idInvitacion);
    }

    public Map<Integer, Integer> contarAsistentesPorInvitacion(int eventoId) {
        return repositoryInvitado.contarAsistentesPorInvitacion(eventoId);
    }

    public int contarAsistentes(int eventoId, int idLugar) {
        return repositoryInvitado.contarAsistentes(eventoId, idLugar);
    }

    // ------------------------------------------------------------------
    // Invitaciones
    // ------------------------------------------------------------------
    public void crearInvitacion(int eventoId, Invitacion datos) {
        if (datos == null) throw new IllegalArgumentException("Completa los datos de la invitación.");
        Evento evento = buscarEventoAbierto(eventoId);
        buscarLugar(datos.getIdLugar());
        validarMensaje(datos.getMensaje());
        datos.setIdEvento(evento.getIdEvento());
        datos.setMensaje(datos.getMensaje().trim());
        datos.setFechaEnvio(LocalDateTime.now());
        datos.setEstadoRespuesta(Invitacion.ESTADO_ENVIADA);
        repositoryInvitacion.guardarInvitacion(datos);
    }

    public void actualizarInvitacion(int idInvitacion, Invitacion datos) {
        if (datos == null) throw new IllegalArgumentException("Completa los datos de la invitación.");
        Invitacion actual = buscarInvitacionActiva(idInvitacion);
        Lugar lugar = buscarLugar(datos.getIdLugar());
        validarMensaje(datos.getMensaje());
        if (lugar.getIdLugar() != actual.getIdLugar()) {
            int deEstaInvitacion = repositoryInvitado.contarAsistentesPorInvitacion(actual.getIdEvento())
                    .getOrDefault(idInvitacion, 0);
            int enNuevoLugar = repositoryInvitado.contarAsistentes(actual.getIdEvento(), lugar.getIdLugar());
            if (deEstaInvitacion + enNuevoLugar > lugar.getCapacidad()) {
                throw new IllegalArgumentException("No se puede cambiar a " + lugar.getNombre() + ": con "
                        + (deEstaInvitacion + enNuevoLugar) + " asistentes superaría su aforo de "
                        + lugar.getCapacidad() + ".");
            }
        }
        actual.setIdLugar(lugar.getIdLugar());
        actual.setMensaje(datos.getMensaje().trim());
        repositoryInvitacion.actualizarInvitacion(actual);
    }

    public void revocarInvitacion(int idInvitacion) {
        Invitacion actual = buscarInvitacionActiva(idInvitacion);
        actual.setEstadoRespuesta(Invitacion.ESTADO_REVOCADA);
        repositoryInvitacion.actualizarInvitacion(actual);
    }

    // ------------------------------------------------------------------
    // Invitados de una invitación
    // ------------------------------------------------------------------
    public void agregarInvitado(int idInvitacion, Invitado datos) {
        if (datos == null) throw new IllegalArgumentException("Completa los datos del invitado.");
        Invitacion invitacion = buscarInvitacionActiva(idInvitacion);
        String nombre = limpiar(datos.getNombre());
        String correo = limpiar(datos.getCorreo());
        String telefono = limpiar(datos.getTelefono());
        String direccion = limpiar(datos.getDireccion());
        if (nombre == null || nombre.length() > 100) throw new IllegalArgumentException("Ingresa el nombre del invitado (hasta 100 caracteres).");
        if (correo == null || correo.length() > 150 || !CORREO.matcher(correo).matches())
            throw new IllegalArgumentException("Ingresa un correo válido para el invitado (hasta 150 caracteres).");
        if (telefono != null && (telefono.length() > 30 || !telefono.matches("[0-9+()\\s-]+")))
            throw new IllegalArgumentException("El teléfono solo puede tener números, espacios y los signos + - ( ), hasta 30 caracteres.");
        if (direccion != null && direccion.length() > 255) throw new IllegalArgumentException("La dirección debe tener hasta 255 caracteres.");
        if (repositoryInvitado.existeCorreoEnEvento(invitacion.getIdEvento(), correo))
            throw new IllegalArgumentException("El invitado con correo " + correo
                    + " ya tiene una invitación activa para este evento.");
        Lugar lugar = buscarLugar(invitacion.getIdLugar());
        int asistentes = repositoryInvitado.contarAsistentes(invitacion.getIdEvento(), lugar.getIdLugar());
        if (asistentes + 1 > lugar.getCapacidad()) {
            throw new IllegalArgumentException("Se alcanzó el aforo de " + lugar.getNombre() + " ("
                    + lugar.getCapacidad() + " personas). Ajusta la lista de invitados o cambia el lugar.");
        }
        datos.setIdInvitacion(idInvitacion);
        datos.setNombre(nombre);
        datos.setCorreo(correo.toLowerCase());
        datos.setTelefono(telefono);
        datos.setDireccion(direccion);
        datos.setEstado(Invitado.ESTADO_PENDIENTE);
        repositoryInvitado.guardarInvitado(datos);
    }

    public void quitarInvitado(int idInvitado) {
        Invitado invitado = repositoryInvitado.buscarInvitado(idInvitado);
        if (invitado == null) throw new IllegalArgumentException("El invitado no existe.");
        if (repositoryInvitado.tieneRegistrosAsociados(idInvitado)) {
            throw new IllegalArgumentException(invitado.getNombre() + " tiene acompañantes, necesidades especiales "
                    + "o regalos registrados; no se puede quitar de la invitación.");
        }
        repositoryInvitado.eliminarInvitado(idInvitado);
    }

    // ------------------------------------------------------------------
    // Validaciones auxiliares
    // ------------------------------------------------------------------
    private Evento buscarEventoAbierto(int eventoId) {
        Evento evento = repositoryEvento.listarEventos().stream()
                .filter(e -> e.getIdEvento() != null && e.getIdEvento() == eventoId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Selecciona un evento registrado."));
        String estado = evento.getEstado() == null ? "" : evento.getEstado().toUpperCase();
        if (estado.equals("CANCELADO") || estado.equals("FINALIZADO")) {
            throw new IllegalArgumentException("No se pueden enviar invitaciones a un evento " + estado.toLowerCase() + ".");
        }
        return evento;
    }

    private Invitacion buscarInvitacionActiva(int idInvitacion) {
        Invitacion invitacion = repositoryInvitacion.buscarInvitacion(idInvitacion);
        if (invitacion == null) throw new IllegalArgumentException("La invitación no existe.");
        if (!invitacion.estaActiva()) throw new IllegalArgumentException("La invitación #" + idInvitacion + " fue revocada.");
        return invitacion;
    }

    private Lugar buscarLugar(int idLugar) {
        Lugar lugar = repositoryLugar.buscarLugar(idLugar);
        if (lugar == null) throw new IllegalArgumentException("Selecciona un lugar registrado.");
        return lugar;
    }

    private void validarMensaje(String mensaje) {
        if (mensaje == null || mensaje.isBlank() || mensaje.trim().length() > 500)
            throw new IllegalArgumentException("Escribe el mensaje de la invitación (hasta 500 caracteres).");
    }

    private static String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}

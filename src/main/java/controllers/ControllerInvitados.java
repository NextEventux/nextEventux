package controllers;

import entities.Evento;
import entities.Invitacion;
import entities.InvitacionRecibida;
import entities.Invitado;
import entities.Lugar;
import entities.Regalo;
import java.util.List;
import java.util.Map;
import services.ServiceInvitados;

/** Intermediario entre las vistas del módulo de invitados y ServiceInvitados (CU-16, CU-17 y CU-20). */
public class ControllerInvitados {

    private final ServiceInvitados serviceInvitados;

    public ControllerInvitados(ServiceInvitados serviceInvitados) {
        this.serviceInvitados = serviceInvitados;
    }

    public List<Evento> listarEventos() {
        return serviceInvitados.listarEventos();
    }

    public List<Lugar> listarLugares() {
        return serviceInvitados.listarLugares();
    }

    public List<Invitacion> listarInvitaciones(int eventoId) {
        return serviceInvitados.listarInvitaciones(eventoId);
    }

    public List<Invitado> listarInvitados(int idInvitacion) {
        return serviceInvitados.listarInvitados(idInvitacion);
    }

    public Map<Integer, Integer> contarAsistentesPorInvitacion(int eventoId) {
        return serviceInvitados.contarAsistentesPorInvitacion(eventoId);
    }

    public int contarAsistentes(int eventoId, int idLugar) {
        return serviceInvitados.contarAsistentes(eventoId, idLugar);
    }

    public void crearInvitacion(int eventoId, Invitacion datos) {
        serviceInvitados.crearInvitacion(eventoId, datos);
    }

    public void actualizarInvitacion(int idInvitacion, Invitacion datos) {
        serviceInvitados.actualizarInvitacion(idInvitacion, datos);
    }

    public void revocarInvitacion(int idInvitacion) {
        serviceInvitados.revocarInvitacion(idInvitacion);
    }

    public void agregarInvitado(int idInvitacion, Invitado datos) {
        serviceInvitados.agregarInvitado(idInvitacion, datos);
    }

    public void quitarInvitado(int idInvitado) {
        serviceInvitados.quitarInvitado(idInvitado);
    }

    public List<InvitacionRecibida> consultarInvitacionesPorCorreo(String correo, String telefono) {
        return serviceInvitados.consultarInvitacionesPorCorreo(correo, telefono);
    }

    public void gestionarRespuestaInvitacion(int idInvitado, String correo, String telefono, String respuesta) {
        serviceInvitados.gestionarRespuestaInvitacion(idInvitado, correo, telefono, respuesta);
    }

    public List<Regalo> listarRegalos(int idInvitacion) {
        return serviceInvitados.listarRegalos(idInvitacion);
    }

    public void agregarRegalo(int idInvitacion, Regalo datos) {
        serviceInvitados.agregarRegalo(idInvitacion, datos);
    }

    public void quitarRegalo(int idRegalo) {
        serviceInvitados.quitarRegalo(idRegalo);
    }

    public List<Regalo> consultarRegalosDisponibles(int idInvitado, String correo, String telefono) {
        return serviceInvitados.consultarRegalosDisponibles(idInvitado, correo, telefono);
    }

    public boolean validarDisponibilidadRegalo(int idRegalo) {
        return serviceInvitados.validarDisponibilidadRegalo(idRegalo);
    }

    public void reservarRegalo(int idInvitado, String correo, String telefono, int idRegalo) {
        serviceInvitados.reservarRegalo(idInvitado, correo, telefono, idRegalo);
    }

    public void cancelarReservaRegalo(int idInvitado, String correo, String telefono, int idRegalo) {
        serviceInvitados.cancelarReservaRegalo(idInvitado, correo, telefono, idRegalo);
    }
}

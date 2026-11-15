package controllers;

import entities.Campana;
import entities.Segmentacion;
import java.util.List;
import services.ServiceMarketing;

public class ControllerMarketing {

    private final ServiceMarketing serviceMarketing;

    public ControllerMarketing(ServiceMarketing serviceMarketing) {
        this.serviceMarketing = serviceMarketing;
    }

    public List<Segmentacion> segmentarClientes(int idAdministrador) {
        return serviceMarketing.segmentarClientes(idAdministrador);
    }

    public List<Segmentacion> listarSegmentosActivos() {
        return serviceMarketing.listarSegmentosActivos();
    }

    // cu-23: crear campaña de marketing

    public Campana crearCampana(Campana campana) {
        return serviceMarketing.crearCampana(campana);
    }

    public List<Campana> listarCampanas() {
        return serviceMarketing.listarCampanas();
    }

    public void pausarCampana(int idAdministrador, int idCampana) {
        serviceMarketing.pausarCampana(idAdministrador, idCampana);
    }

    public void reanudarCampana(int idAdministrador, int idCampana) {
        serviceMarketing.reanudarCampana(idAdministrador, idCampana);
    }
}
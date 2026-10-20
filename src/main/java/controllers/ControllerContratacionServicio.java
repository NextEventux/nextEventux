package controllers;

import entities.ConfirmacionContratacion;
import entities.DisponibilidadContratacion;
import entities.PresupuestoContratacion;
import entities.ResumenContratacionServicio;
import services.ServiceContratacionServicio;

// Recibe las acciones de CU-13 (contratar servicio) y las envia al service
public class ControllerContratacionServicio {

    private final ServiceContratacionServicio service;

    public ControllerContratacionServicio(
            ServiceContratacionServicio service) {

        this.service = service;
    }

    public ResumenContratacionServicio consultarResumen(
            int idOrganizador,
            int idPropuesta) {

        return service.consultarResumen(idOrganizador, idPropuesta);
    }

    public DisponibilidadContratacion verificarDisponibilidad(
            int idOrganizador,
            int idPropuesta) {

        return service.verificarDisponibilidad(idOrganizador, idPropuesta);
    }

    public PresupuestoContratacion verificarPresupuesto(
            int idOrganizador,
            int idPropuesta) {

        return service.verificarPresupuesto(idOrganizador, idPropuesta);
    }

    public ConfirmacionContratacion contratar(
            int idOrganizador,
            int idPropuesta) {

        return service.contratar(idOrganizador, idPropuesta);
    }
}

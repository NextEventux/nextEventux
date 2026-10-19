package controllers;

import java.util.List;

import entities.PropuestaServicio;
import entities.SolicitudEjecucion;
import java.math.BigDecimal;
import services.ServiceSolicitudes;

public class ControllerSolicitudes {

    private final ServiceSolicitudes service;

    public ControllerSolicitudes(ServiceSolicitudes service) {
        this.service = service;
    }

    public List<SolicitudEjecucion> listarSolicitudesProveedor(
            int idProveedor) {

        return service.listarSolicitudesProveedor(idProveedor);
    }

    public void rechazarSolicitud(
            int idProveedor,
            int idSolicitud,
            String motivo) {

        service.rechazarSolicitud(
                idProveedor,
                idSolicitud,
                motivo);
    }

    public int enviarPropuesta(
            int idProveedor,
            int idSolicitud,
            BigDecimal valor,
            String descripcion,
            String condiciones) {

        return service.enviarPropuesta(
                idProveedor,
                idSolicitud,
                valor,
                descripcion,
                condiciones);
    }

    public List<PropuestaServicio> listarHistorialPropuestas(
            int idProveedor,
            int idSolicitud) {

        return service.listarHistorialPropuestas(
                idProveedor,
                idSolicitud);
    }
}

package controllers;

import java.util.Date;
import java.util.List;

import entities.DisponibilidadServicio;
import entities.ImagenServicio;
import entities.PreguntaServicio;
import entities.Servicio;
import services.ServiceProveedores;

// Recibe las acciones relacionadas con proveedores y las envia al service
public class ControllerProveedores {

    private ServiceProveedores serviceProveedores;

    public ControllerProveedores(
            ServiceProveedores serviceProveedores) {

        this.serviceProveedores = serviceProveedores;
    }

    public void crearServicio(Servicio datos) {
        serviceProveedores.crearServicio(datos);
    }

    public void modificarServicio(
            int idServicio,
            Servicio datos) {

        serviceProveedores
                .modificarServicio(idServicio, datos);
    }

    public void registrarDisponibilidadServicio(
            int idServicio,
            Date fecha,
            String horario) {

        serviceProveedores
                .registrarDisponibilidadServicio(
                        idServicio,
                        fecha,
                        horario);
    }

    public void publicarServicio(int idServicio) {
        serviceProveedores
                .publicarServicio(idServicio);
    }

    public void retirarServicio(int idServicio) {
        serviceProveedores
                .retirarServicio(idServicio);
    }

    public Servicio buscarServicio(int idServicio) {
        return serviceProveedores
                .buscarServicio(idServicio);
    }

    public List<Servicio> listarServiciosDisponibles() {

        return serviceProveedores
                .listarServiciosDisponibles();
    }

    public List<Servicio> listarServiciosProveedor(
            int idProveedor) {

        return serviceProveedores
                .listarServiciosProveedor(idProveedor);
    }

    public List<DisponibilidadServicio>
            listarDisponibilidadesServicio(
                    int idServicio) {

        return serviceProveedores
                .listarDisponibilidadesServicio(idServicio);
    }

    public void eliminarDisponibilidad(
            int idDisponibilidad) {

        serviceProveedores
                .eliminarDisponibilidad(idDisponibilidad);
    }

    public void agregarImagenServicio(
            int idProveedor,
            int idServicio,
            String ruta) {

        serviceProveedores.agregarImagenServicio(
                idProveedor,
                idServicio,
                ruta);
    }

    public void eliminarImagenServicio(
            int idProveedor,
            int idImagen) {

        serviceProveedores.eliminarImagenServicio(
                idProveedor,
                idImagen);
    }

    public List<ImagenServicio> listarImagenesServicio(
            int idServicio) {

        return serviceProveedores
                .listarImagenesServicio(idServicio);
    }

    public void formularPreguntaPublica(
            int idServicio,
            int idOrganizador,
            String pregunta) {

        serviceProveedores.formularPreguntaPublica(
                idServicio,
                idOrganizador,
                pregunta);
    }

    public void responderPreguntaPublica(
            int idProveedor,
            int idPregunta,
            String respuesta) {

        serviceProveedores.responderPreguntaPublica(
                idProveedor,
                idPregunta,
                respuesta);
    }

    public List<PreguntaServicio> listarPreguntasPublicas(
            int idServicio) {

        return serviceProveedores
                .listarPreguntasPublicas(idServicio);
    }

}

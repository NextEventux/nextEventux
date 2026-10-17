package controllers;

import entities.DisponibilidadServicio;
import entities.Servicio;
import services.ServiceProveedores;

import java.util.Date;
import java.util.List;

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
}

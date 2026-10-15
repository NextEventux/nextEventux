package controllers;

import entities.Proveedor;
import services.ServiceAdministracion;

import java.util.List;

/** Recibe las acciones de la vista y las dirige al servicio de Administración. */
public class ControllerAdministracion {
    private final ServiceAdministracion serviceAdministracion;

    public ControllerAdministracion(ServiceAdministracion serviceAdministracion) {
        if (serviceAdministracion == null) {
            throw new IllegalArgumentException("El servicio de administración es obligatorio.");
        }
        this.serviceAdministracion = serviceAdministracion;
    }

    public List<Proveedor> consultarProveedoresPendientes() {
        return serviceAdministracion.consultarProveedoresPendientes();
    }

    public boolean verificarInformacionProveedor(int idProveedor) {
        return serviceAdministracion.verificarInformacionProveedor(idProveedor);
    }

    public void aprobarProveedor(int idProveedor) {
        serviceAdministracion.aprobarProveedor(idProveedor);
    }

    public void rechazarProveedor(int idProveedor, String motivo) {
        serviceAdministracion.rechazarProveedor(idProveedor, motivo);
    }
}

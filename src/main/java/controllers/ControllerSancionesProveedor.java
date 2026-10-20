package controllers;

import java.util.List;

import entities.SesionUsuario;
import repositories.RepositorySancionesProveedor.ContratoSancionable;
import repositories.RepositorySancionesProveedor.EventoProveedor;
import repositories.RepositorySancionesProveedor.ResumenProveedor;
import services.ServiceSancionesProveedor;
import services.ServiceSancionesProveedor.ResultadoSancion;
import services.ServiceSancionesProveedor.SolicitudSancion;

/**
 * Controlador de CU-09 (aplicar sanciones). Conecta la vista con el servicio.
 */
public class ControllerSancionesProveedor {

    private final ServiceSancionesProveedor servicio;
    private final SesionUsuario sesion;

    public ControllerSancionesProveedor(ServiceSancionesProveedor servicio, SesionUsuario sesion) {
        if (servicio == null) {
            throw new IllegalArgumentException("El servicio de sanciones es obligatorio.");
        }
        this.servicio = servicio;
        this.sesion = sesion;
    }

    public List<ResumenProveedor> buscarProveedores(String texto) {
        return servicio.buscarProveedores(sesion, texto);
    }

    public List<EventoProveedor> eventosDelProveedor(int idProveedor) {
        return servicio.eventosDelProveedor(sesion, idProveedor);
    }

    public List<ContratoSancionable> contratosActivos(int idProveedor, int idEvento) {
        return servicio.contratosActivos(sesion, idProveedor, idEvento);
    }

    public ResultadoSancion aplicarSancion(SolicitudSancion solicitud) {
        return servicio.aplicarSancion(sesion, solicitud);
    }
}

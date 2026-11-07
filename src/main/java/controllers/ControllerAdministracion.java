
package controllers;

import entities.Contratacion;
import entities.Organizador;
import entities.SesionUsuario;
import repositories.RepositoryAdministracion.UsuarioAdministrable;
import services.ServiceAdministracion;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador del modulo de administracion.
 * Conecta las acciones de la vista con el servicio.
 *
 * Operaciones de CU-026 (cliente) y CU-029 (administrador).
 */
public class ControllerAdministracion {

    private final ServiceAdministracion serviceAdministracion;
    private final SesionUsuario sesion;

    public ControllerAdministracion(
            ServiceAdministracion serviceAdministracion) {
        this(serviceAdministracion, null);
    }

    public ControllerAdministracion(
            ServiceAdministracion serviceAdministracion,
            SesionUsuario sesion) {

        if (serviceAdministracion == null) {
            throw new IllegalArgumentException(
                    "El servicio de administracion es obligatorio.");
        }

        this.serviceAdministracion = serviceAdministracion;
        this.sesion = sesion;
    }

    /**
     * Consulta los organizadores disponibles segun los filtros
     * ingresados por el cliente.
     */
    public List<Organizador> consultarOrganizadores(
            Map<String, Object> filtros) {

        validarSesionCliente();
        return serviceAdministracion.consultarOrganizadores(filtros);
    }

    /**
     * Consulta el perfil de un organizador seleccionado.
     */
    public Organizador seleccionarOrganizador(int idOrganizador) {

        validarSesionCliente();
        return serviceAdministracion.seleccionarOrganizador(idOrganizador);
    }

    /**
     * Consulta si el organizador esta disponible para la fecha indicada.
     */
    public boolean verificarDisponibilidadOrganizador(
            int idOrganizador, LocalDate fecha) {

        validarSesionCliente();

        return serviceAdministracion
                .verificarDisponibilidadOrganizador(idOrganizador, fecha);
    }

    /**
     * Envia la solicitud en nombre del cliente autenticado.
     * El identificador se toma de la sesion y no del formulario.
     */
    public void contratarOrganizador(
            int idOrganizador, Contratacion datos) {

        validarSesionCliente();

        if (datos == null) {
            throw new IllegalArgumentException(
                    "Completa los datos de la solicitud.");
        }

        datos.setIdCliente(sesion.getIdUsuario());

        serviceAdministracion.contratarOrganizador(
                idOrganizador, datos);
    }

    /**
     * Consulta unicamente las solicitudes del cliente autenticado.
     */
    public List<Contratacion> consultarContrataciones(
            Map<String, Object> filtros) {

        validarSesionCliente();

        Map<String, Object> filtrosSeguros = filtros == null
                ? new HashMap<>()
                : new HashMap<>(filtros);

        // El cliente solo puede consultar sus propias solicitudes.
        filtrosSeguros.put("idCliente", sesion.getIdUsuario());

        return serviceAdministracion
                .consultarContrataciones(filtrosSeguros);
    }

    // ------------------------------------------------------------------
    // CU-029: suspender usuario y ampliacion: reactivar usuario.
    // La autorizacion del administrador la verifica el servicio en la
    // base de datos; el controlador solo entrega la sesion autenticada.
    // ------------------------------------------------------------------

    public List<UsuarioAdministrable> consultarUsuarios(String filtro) {
        return serviceAdministracion.consultarUsuarios(sesion, filtro);
    }

    public UsuarioAdministrable consultarUsuario(int idUsuario) {
        return serviceAdministracion.consultarUsuario(sesion, idUsuario);
    }

    public boolean validarSuspension(int idUsuario, String motivo) {
        return serviceAdministracion.validarSuspension(sesion, idUsuario, motivo);
    }

    public void suspenderUsuario(int idUsuario, String motivo) {
        serviceAdministracion.suspenderUsuario(sesion, idUsuario, motivo);
    }

    public String validarReactivacion(int idUsuario) {
        return serviceAdministracion.validarReactivacion(sesion, idUsuario);
    }

    public void reactivarUsuario(int idUsuario) {
        serviceAdministracion.reactivarUsuario(sesion, idUsuario);
    }

    /**
     * Verifica que exista una sesion valida de cliente.
     */
    private void validarSesionCliente() {

        if (sesion == null
                || sesion.getIdUsuario() <= 0
                || !"CLIENTE".equalsIgnoreCase(sesion.getTipoUsuario())
                || !"ACTIVO".equalsIgnoreCase(sesion.getEstadoUsuario())) {

            throw new IllegalStateException(
                    "Inicia sesion con una cuenta de cliente activa "
                    + "para contratar un organizador.");
        }
    }
}

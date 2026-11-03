package controllers;

import entities.Contratacion;
import entities.Organizador;
import entities.SesionUsuario;
import services.ServiceAdministracion;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador del modulo de administracion.
 *
 * Conecta las acciones de las vistas con el servicio.
 *
 * Implementa las operaciones de CU-026 para clientes
 * y la consulta de solicitudes recibidas por organizadores.
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

        return serviceAdministracion.seleccionarOrganizador(
                idOrganizador);
    }

    /**
     * Consulta si el organizador esta disponible para la fecha indicada.
     */
    public boolean verificarDisponibilidadOrganizador(
            int idOrganizador, LocalDate fecha) {

        validarSesionCliente();

        return serviceAdministracion
                .verificarDisponibilidadOrganizador(
                        idOrganizador, fecha);
    }

    /**
     * Envia la solicitud en nombre del cliente autenticado.
     * El identificador del cliente se obtiene de la sesion.
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
     *
     * El ID del cliente no puede ser reemplazado mediante filtros
     * enviados desde la vista.
     */
    public List<Contratacion> consultarContrataciones(
            Map<String, Object> filtros) {

        validarSesionCliente();

        Map<String, Object> filtrosSeguros = filtros == null
                ? new HashMap<>()
                : new HashMap<>(filtros);

        filtrosSeguros.put(
                "idCliente", sesion.getIdUsuario());

        // Evita que el cliente consulte solicitudes de otros
        // organizadores mediante filtros manipulados.
        filtrosSeguros.remove("idOrganizador");

        return serviceAdministracion
                .consultarContrataciones(filtrosSeguros);
    }

    /**
     * CU-026:
     * Consulta exclusivamente las solicitudes recibidas
     * por el organizador autenticado.
     *
     * El identificador del organizador se obtiene de idActor
     * de la sesion, nunca de un parametro enviado por la vista.
     */
    public List<Contratacion> consultarSolicitudesRecibidas() {

        validarSesionOrganizador();

        return serviceAdministracion.consultarSolicitudesRecibidas(
                sesion.getIdActor());
    }

    /**
     * CU-026: el organizador autenticado responde
     * una solicitud de contratación.
     */
    public void responderSolicitud(
            int idSolicitud,
            String nuevoEstado) {

        validarSesionOrganizador();

        if (idSolicitud <= 0) {
            throw new IllegalArgumentException(
                    "Selecciona una solicitud válida.");
        }

        if (nuevoEstado == null
                || (!"ACEPTADA".equalsIgnoreCase(nuevoEstado)
                && !"RECHAZADA".equalsIgnoreCase(nuevoEstado))) {

            throw new IllegalArgumentException(
                    "Debes seleccionar aceptar o rechazar.");
        }

        serviceAdministracion.responderSolicitud(
                idSolicitud,
                sesion.getIdActor(),
                nuevoEstado);
    }

    /**
     * Verifica que exista una sesion valida de cliente.
     */
    private void validarSesionCliente() {

        if (sesion == null
                || sesion.getIdUsuario() <= 0
                || !"CLIENTE".equalsIgnoreCase(
                sesion.getTipoUsuario())
                || !"ACTIVO".equalsIgnoreCase(
                sesion.getEstadoUsuario())) {

            throw new IllegalStateException(
                    "Inicia sesion con una cuenta de cliente activa "
                            + "para contratar un organizador.");
        }
    }

    /**
     * Verifica que exista una sesion valida de organizador.
     *
     * Se comprueban tanto el estado de la cuenta como
     * el estado del perfil asociado.
     */
    private void validarSesionOrganizador() {

        if (sesion == null
                || sesion.getIdUsuario() <= 0
                || sesion.getIdActor() <= 0
                || !"ORGANIZADOR".equalsIgnoreCase(
                sesion.getTipoUsuario())
                || !"ACTIVO".equalsIgnoreCase(
                sesion.getEstadoUsuario())
                || !"ACTIVO".equalsIgnoreCase(
                sesion.getEstadoActor())) {

            throw new IllegalStateException(
                    "Inicia sesion con una cuenta de organizador "
                            + "activa para consultar las solicitudes recibidas.");
        }
    }
}

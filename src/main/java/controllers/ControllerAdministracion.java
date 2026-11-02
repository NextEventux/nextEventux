
package controllers;

import entities.Contratacion;
import entities.Organizador;
import entities.SesionUsuario;

import repositories.RepositoryAdministracion.UsuarioResumen;
import services.ServiceAdministracion;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador del modulo de administracion.
 *
 * CU-026: Contratar organizador.
 * CU-029: Suspender y reactivar usuarios.
 *
 * Conecta las acciones de las vistas con el servicio.
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

    // ==========================================================
    // CU-026: CONTRATAR ORGANIZADOR
    // ==========================================================

    public List<Organizador> consultarOrganizadores(
            Map<String, Object> filtros) {

        validarSesionCliente();

        return serviceAdministracion.consultarOrganizadores(filtros);
    }

    public Organizador seleccionarOrganizador(int idOrganizador) {

        validarSesionCliente();

        return serviceAdministracion.seleccionarOrganizador(
                idOrganizador);
    }

    public boolean verificarDisponibilidadOrganizador(
            int idOrganizador, LocalDate fecha) {

        validarSesionCliente();

        return serviceAdministracion
                .verificarDisponibilidadOrganizador(
                        idOrganizador, fecha);
    }

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

    public List<Contratacion> consultarContrataciones(
            Map<String, Object> filtros) {

        validarSesionCliente();

        Map<String, Object> filtrosSeguros = filtros == null
                ? new HashMap<>()
                : new HashMap<>(filtros);

        filtrosSeguros.put(
                "idCliente", sesion.getIdUsuario());

        return serviceAdministracion
                .consultarContrataciones(filtrosSeguros);
    }

    // ==========================================================
    // CU-029: GESTION DE USUARIOS
    // ==========================================================

    /**
     * Consulta los usuarios registrados.
     */
    public List<UsuarioResumen> consultarUsuarios(String filtro) {

        validarSesionAdministrador();

        return serviceAdministracion.consultarUsuarios(
                sesion, filtro);
    }

    /**
     * Consulta los datos del usuario seleccionado.
     */
    public UsuarioResumen consultarUsuario(int idUsuario) {

        validarSesionAdministrador();

        return serviceAdministracion.consultarUsuario(
                sesion, idUsuario);
    }

    /**
     * Comprueba si se cumplen las condiciones para suspender.
     */
    public boolean validarSuspension(
            int idUsuario, String motivo) {

        validarSesionAdministrador();

        return serviceAdministracion.validarSuspension(
                sesion, idUsuario, motivo);
    }

    /**
     * Ejecuta la suspension del usuario seleccionado.
     */
    public void suspenderUsuario(
            int idUsuario, String motivo) {

        validarSesionAdministrador();

        serviceAdministracion.suspenderUsuario(
                sesion, idUsuario, motivo);
    }

    /**
     * Ejecuta la reactivacion del usuario seleccionado.
     */
    public void reactivarUsuario(
            int idUsuario, String motivo) {

        validarSesionAdministrador();

        serviceAdministracion.reactivarUsuario(
                sesion, idUsuario, motivo);
    }

    // ==========================================================
    // VALIDACIONES DE SESION
    // ==========================================================

    /**
     * CU-026: acceso exclusivo para clientes activos.
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
     * CU-029: acceso exclusivo para administradores activos.
     */
    private void validarSesionAdministrador() {

        if (sesion == null
                || sesion.getIdUsuario() <= 0
                || !"ADMINISTRADOR".equalsIgnoreCase(
                sesion.getTipoUsuario())
                || !"ACTIVO".equalsIgnoreCase(
                sesion.getEstadoUsuario())) {

            throw new IllegalStateException(
                    "Solo un administrador con cuenta activa "
                            + "puede gestionar usuarios.");
        }
    }
}

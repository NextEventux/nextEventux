
package services;

import entities.Contratacion;
import entities.Organizador;
import repositories.RepositoryContratacion;
import repositories.RepositoryOrganizador;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Servicio de administracion.
 * Implementa la logica de negocio del caso de uso CU-026.
 */
public class ServiceAdministracion {

    private final RepositoryContratacion repositoryContratacion;
    private final RepositoryOrganizador repositoryOrganizador;

    public ServiceAdministracion(
            RepositoryContratacion repositoryContratacion,
            RepositoryOrganizador repositoryOrganizador) {

        if (repositoryContratacion == null
                || repositoryOrganizador == null) {
            throw new IllegalArgumentException(
                    "Los repositorios de administracion son obligatorios.");
        }

        this.repositoryContratacion = repositoryContratacion;
        this.repositoryOrganizador = repositoryOrganizador;
    }

    /**
     * Consulta organizadores activos y verificados que coincidan
     * con la ciudad, el tipo de evento, el presupuesto y la fecha.
     *
     * Filtros requeridos:
     * ciudad, tipoEvento, presupuestoEstimado y fechaAproximada.
     */
    public List<Organizador> consultarOrganizadores(
            Map<String, Object> filtros) {

        validarFiltrosBusqueda(filtros);

        return repositoryOrganizador.consultarOrganizadoresDisponibles(
                filtros);
    }

    /**
     * Recupera el perfil seleccionado y comprueba que el
     * organizador continue activo y verificado.
     */
    public Organizador seleccionarOrganizador(int idOrganizador) {

        if (idOrganizador <= 0) {
            throw new IllegalArgumentException(
                    "Selecciona un organizador valido.");
        }

        if (!repositoryOrganizador.organizadorActivoVerificado(
                idOrganizador)) {
            throw new OrganizadorNoDisponibleException(
                    OrganizadorNoDisponibleException.NO_ACTIVO_VERIFICADO);
        }

        Organizador organizador =
                repositoryOrganizador.buscarOrganizador(idOrganizador);

        if (organizador == null) {
            throw new OrganizadorNoDisponibleException(
                    OrganizadorNoDisponibleException.NO_ACTIVO_VERIFICADO);
        }

        return organizador;
    }

    /**
     * Reseñas disponibles del organizador para su perfil (paso 3).
     */
    public List<entities.ResenaOrganizador> consultarResenas(
            int idOrganizador) {

        if (idOrganizador <= 0) {
            throw new IllegalArgumentException(
                    "Selecciona un organizador válido.");
        }

        return repositoryOrganizador.listarResenas(idOrganizador);
    }

    // ------------------------------------------------------------------
    // Ampliación de CU-026: el organizador consulta y responde las
    // solicitudes de contratación que recibe. Estados:
    // PENDIENTE -> ACEPTADA | RECHAZADA (ambos finales).
    // ------------------------------------------------------------------

    /**
     * Solicitudes dirigidas al organizador de la cuenta indicada.
     */
    public List<Contratacion> consultarSolicitudesRecibidas(
            int idUsuarioOrganizador) {

        return repositoryContratacion
                .listarSolicitudesRecibidas(idUsuarioOrganizador);
    }

    /**
     * Acepta o rechaza una solicitud pendiente y notifica al cliente
     * en la misma transacción.
     */
    public Contratacion responderSolicitud(
            int idUsuarioOrganizador, int idSolicitud, boolean aceptar) {

        if (idSolicitud <= 0) {
            throw new IllegalArgumentException(
                    "Selecciona una solicitud válida.");
        }

        return repositoryContratacion.responderSolicitud(
                idSolicitud,
                idUsuarioOrganizador,
                aceptar ? "ACEPTADA" : "RECHAZADA");
    }

    /**
     * Comprueba la disponibilidad del organizador para la fecha
     * aproximada del evento.
     */
    public boolean verificarDisponibilidadOrganizador(
            int idOrganizador, LocalDate fecha) {

        if (idOrganizador <= 0) {
            throw new IllegalArgumentException(
                    "El organizador seleccionado no es valido.");
        }

        if (fecha == null) {
            throw new IllegalArgumentException(
                    "Ingresa la fecha aproximada del evento.");
        }

        return repositoryOrganizador
                .verificarDisponibilidadOrganizador(idOrganizador, fecha);
    }

    /**
     * Registra la solicitud de contratacion.
     *
     * La solicitud queda PENDIENTE y el repositorio se encarga
     * de guardarla y notificar internamente al organizador.
     */
    public void contratarOrganizador(
            int idOrganizador, Contratacion datos) {

        if (idOrganizador <= 0) {
            throw new IllegalArgumentException(
                    "Selecciona un organizador valido.");
        }

        if (datos == null) {
            throw new IllegalArgumentException(
                    "Los datos de la solicitud son obligatorios.");
        }

        validarFechaAproximada(datos.getFechaAproximada());

        // E2: se informa de inmediato si el organizador ya no esta disponible.
        seleccionarOrganizador(idOrganizador);

        datos.setIdOrganizador(idOrganizador);
        datos.setEstado("PENDIENTE");

        // El repositorio vuelve a comprobar estado, disponibilidad por fecha
        // y duplicados dentro de la misma transaccion del registro.
        repositoryContratacion.guardarContratacion(datos);
    }

    /**
     * Consulta las solicitudes de contratacion mediante filtros.
     */
    public List<Contratacion> consultarContrataciones(
            Map<String, Object> filtros) {

        return repositoryContratacion.listarContrataciones(filtros);
    }

    private void validarFiltrosBusqueda(
            Map<String, Object> filtros) {

        if (filtros == null) {
            throw new IllegalArgumentException(
                    "Ingresa los criterios de busqueda.");
        }

        Object ciudad = filtros.get("ciudad");
        Object tipoEvento = filtros.get("tipoEvento");
        Object presupuesto = filtros.get("presupuestoEstimado");
        Object fecha = filtros.get("fechaAproximada");

        if (ciudad == null || ciudad.toString().isBlank()
                || tipoEvento == null || tipoEvento.toString().isBlank()
                || presupuesto == null || fecha == null) {

            throw new IllegalArgumentException(
                    "Completa el tipo de evento, la ciudad, "
                    + "la fecha aproximada y el presupuesto.");
        }

        if (!(presupuesto instanceof Number)) {
            throw new IllegalArgumentException(
                    "El presupuesto debe ser numerico.");
        }

        BigDecimal valorPresupuesto =
                new BigDecimal(presupuesto.toString());

        if (valorPresupuesto.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "El presupuesto no puede ser negativo.");
        }

        if (!(fecha instanceof LocalDate)) {
            throw new IllegalArgumentException(
                    "La fecha aproximada no tiene un formato valido.");
        }

        validarFechaAproximada((LocalDate) fecha);

        if (ciudad.toString().trim().length() > 150
                || tipoEvento.toString().trim().length() > 100) {
            throw new IllegalArgumentException(
                    "La ciudad o el tipo de evento son demasiado largos.");
        }

        Object criterios = filtros.get("criterios");

        if (criterios != null && criterios.toString().trim().length()
                > RepositoryOrganizador.LONGITUD_MAXIMA_CRITERIOS) {
            throw new IllegalArgumentException(
                    "Los criterios adicionales no pueden superar "
                    + RepositoryOrganizador.LONGITUD_MAXIMA_CRITERIOS
                    + " caracteres.");
        }
    }

    /** CU-026: la fecha aproximada del evento no puede estar en el pasado. */
    private void validarFechaAproximada(LocalDate fecha) {

        if (fecha == null) {
            throw new IllegalArgumentException(
                    "Ingresa la fecha aproximada del evento.");
        }

        if (fecha.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "La fecha aproximada no puede ser anterior a hoy.");
        }
    }
}

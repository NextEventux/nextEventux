
package services;

import entities.Contratacion;
import entities.Organizador;
import entities.SesionUsuario;
import repositories.RepositoryAdministracion;
import repositories.RepositoryAdministracion.UsuarioAdministrable;
import repositories.RepositoryContratacion;
import repositories.RepositoryOrganizador;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Servicio de administracion.
 * Implementa la logica de negocio de CU-026 (contratar organizador),
 * CU-029 (suspender usuario) y la ampliacion de CU-029 (reactivar usuario).
 *
 * Autorizacion de CU-029: el esquema no tiene un permiso granular para
 * suspender usuarios (Permiso/Rol_permiso no lo definen y su gestion
 * corresponde a CU-31). Por eso se exige tipo_usuario ADMINISTRADOR y
 * estado ACTIVO, comprobados en la base de datos dentro de cada operacion.
 *
 * Concurrencia de CU-029: toda escritura bloquea primero todas las cuentas
 * de administrador en orden de id y despues la cuenta afectada. Con ese
 * orden fijo dos operaciones simultaneas se ejecutan una tras otra, nunca
 * se bloquean mutuamente y la segunda lee el resultado de la primera.
 */
public class ServiceAdministracion {

    public static final String ESTADO_ACTIVO = "ACTIVO";
    public static final String ESTADO_SUSPENDIDO = "SUSPENDIDO";
    public static final int MAX_MOTIVO = 255;
    static final int MAX_NOTIFICACION = 500;
    static final int MAX_FILTRO = 100;

    public static final String MOTIVO_OBLIGATORIO =
            "Indica el motivo de la suspensión.";
    public static final String MOTIVO_LARGO =
            "El motivo no puede superar " + MAX_MOTIVO + " caracteres.";
    public static final String YA_SUSPENDIDA =
            "La cuenta ya se encuentra suspendida.";
    public static final String NO_ACTIVA =
            "La cuenta no está activa y no puede suspenderse.";
    public static final String YA_ACTIVA =
            "La cuenta ya está activa.";
    public static final String NO_SUSPENDIDA =
            "La cuenta no está suspendida y no puede reactivarse.";
    public static final String AUTOSUSPENSION =
            "No puedes suspender tu propia cuenta.";
    public static final String ULTIMO_ADMIN =
            "No se puede suspender: debe quedar al menos un administrador activo.";
    public static final String HISTORIAL_INCONSISTENTE =
            "El historial de suspensiones de esta cuenta es inconsistente. "
            + "No se realizó ningún cambio; contacta al equipo técnico.";
    public static final String SOLO_ADMINISTRADOR =
            "Solo un administrador puede gestionar usuarios.";
    public static final String SESION_ADMIN_INVALIDA =
            "Tu sesión de administrador ya no es válida. Vuelve a iniciar sesión.";
    public static final String USUARIO_INEXISTENTE =
            "El usuario seleccionado no existe.";
    public static final String USUARIO_INVALIDO =
            "Selecciona un usuario válido.";
    public static final String ERROR_SUSPENSION =
            "No fue posible completar la suspensión. La cuenta conserva su estado anterior.";
    public static final String ERROR_REACTIVACION =
            "No fue posible completar la reactivación. La cuenta conserva su estado anterior.";
    public static final String ERROR_CONSULTA =
            "No se pudieron consultar los usuarios. Vuelve a intentarlo.";
    public static final String ERROR_VERIFICACION =
            "No se pudo verificar la cuenta seleccionada. Vuelve a intentarlo.";

    private final RepositoryContratacion repositoryContratacion;
    private final RepositoryOrganizador repositoryOrganizador;
    private final RepositoryAdministracion repositoryAdministracion;

    public ServiceAdministracion(
            RepositoryContratacion repositoryContratacion,
            RepositoryOrganizador repositoryOrganizador) {

        this(repositoryContratacion, repositoryOrganizador,
                new RepositoryAdministracion());
    }

    public ServiceAdministracion(
            RepositoryContratacion repositoryContratacion,
            RepositoryOrganizador repositoryOrganizador,
            RepositoryAdministracion repositoryAdministracion) {

        if (repositoryContratacion == null
                || repositoryOrganizador == null
                || repositoryAdministracion == null) {
            throw new IllegalArgumentException(
                    "Los repositorios de administracion son obligatorios.");
        }

        this.repositoryContratacion = repositoryContratacion;
        this.repositoryOrganizador = repositoryOrganizador;
        this.repositoryAdministracion = repositoryAdministracion;
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
            throw new IllegalStateException(
                    "El organizador no esta activo o no se encuentra verificado.");
        }

        Organizador organizador =
                repositoryOrganizador.buscarOrganizador(idOrganizador);

        if (organizador == null) {
            throw new IllegalStateException(
                    "No se encontro el perfil del organizador seleccionado.");
        }

        return organizador;
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

        // Verificar el estado actual antes de continuar.
        seleccionarOrganizador(idOrganizador);

        // La disponibilidad se comprueba nuevamente al enviar.
        if (!verificarDisponibilidadOrganizador(
                idOrganizador, datos.getFechaAproximada())) {
            throw new IllegalStateException(
                    "El organizador seleccionado no esta disponible "
                    + "para la fecha indicada. Busca otra opcion.");
        }

        datos.setIdOrganizador(idOrganizador);
        datos.setEstado("PENDIENTE");

        repositoryContratacion.guardarContratacion(datos);
    }

    /**
     * Consulta las solicitudes de contratacion mediante filtros.
     */
    public List<Contratacion> consultarContrataciones(
            Map<String, Object> filtros) {

        return repositoryContratacion.listarContrataciones(filtros);
    }

    // ------------------------------------------------------------------
    // CU-029: suspender usuario y ampliacion: reactivar usuario
    // ------------------------------------------------------------------

    /**
     * Lista las cuentas registradas con su estado actual.
     * El filtro busca por nombre o nombre de usuario; vacio lista todas.
     */
    public List<UsuarioAdministrable> consultarUsuarios(
            SesionUsuario sesion, String filtro) {

        validarAdministrador(sesion);

        String limpio = normalizar(filtro);
        if (limpio.length() > MAX_FILTRO) {
            throw new IllegalArgumentException(
                    "La búsqueda no puede superar " + MAX_FILTRO + " caracteres.");
        }
        String like = "%" + limpio.toUpperCase(Locale.ROOT)
                .replace("%", "").replace("_", "") + "%";

        try (Connection c = repositoryAdministracion.abrirConexion()) {
            verificarOperadorVigente(c, sesion.getIdUsuario());
            return repositoryAdministracion.listarUsuarios(c, like);
        } catch (SQLException e) {
            throw new IllegalStateException(ERROR_CONSULTA, e);
        }
    }

    /** Consulta la informacion y el estado actual de una cuenta. */
    public UsuarioAdministrable consultarUsuario(
            SesionUsuario sesion, int idUsuario) {

        validarAdministrador(sesion);
        validarIdUsuario(idUsuario);

        try (Connection c = repositoryAdministracion.abrirConexion()) {
            verificarOperadorVigente(c, sesion.getIdUsuario());
            return repositoryAdministracion.buscarUsuario(c, idUsuario)
                    .orElseThrow(() -> new IllegalArgumentException(USUARIO_INEXISTENTE));
        } catch (SQLException e) {
            throw new IllegalStateException(ERROR_VERIFICACION, e);
        }
    }

    /**
     * Comprueba, sin modificar nada, que la cuenta puede suspenderse.
     * Si motivo es null solo se revisa la cuenta (paso 2 del flujo basico);
     * en otro caso tambien se valida el motivo (paso 3).
     *
     * @return true si la suspension puede continuar
     */
    public boolean validarSuspension(
            SesionUsuario sesion, int idUsuario, String motivo) {

        validarAdministrador(sesion);
        validarIdUsuario(idUsuario);
        if (motivo != null) {
            normalizarMotivo(motivo);
        }

        try (Connection c = repositoryAdministracion.abrirConexion()) {
            verificarOperadorVigente(c, sesion.getIdUsuario());
            UsuarioAdministrable objetivo = repositoryAdministracion
                    .buscarUsuario(c, idUsuario)
                    .orElseThrow(() -> new IllegalArgumentException(USUARIO_INEXISTENTE));
            reglasSuspension(c, sesion.getIdUsuario(), objetivo);
            return true;
        } catch (SQLException e) {
            throw new IllegalStateException(ERROR_VERIFICACION, e);
        }
    }

    /**
     * Suspende la cuenta: cambia su estado a SUSPENDIDO, registra el motivo
     * en el historial y notifica al usuario, todo en una sola transaccion.
     * Si algo falla no queda ningun cambio (E2).
     */
    public void suspenderUsuario(
            SesionUsuario sesion, int idUsuario, String motivo) {

        validarAdministrador(sesion);
        validarIdUsuario(idUsuario);
        String motivoLimpio = normalizarMotivo(motivo);
        int idAdministrador = sesion.getIdUsuario();

        enTransaccion(ERROR_SUSPENSION, c -> {
            repositoryAdministracion.bloquearAdministradores(c);
            verificarOperadorVigente(c, idAdministrador);

            UsuarioAdministrable objetivo = repositoryAdministracion
                    .bloquearUsuario(c, idUsuario)
                    .orElseThrow(() -> new IllegalArgumentException(USUARIO_INEXISTENTE));
            reglasSuspension(c, idAdministrador, objetivo);

            if (repositoryAdministracion.actualizarEstadoUsuario(
                    c, idUsuario, ESTADO_SUSPENDIDO, ESTADO_ACTIVO) != 1) {
                throw new IllegalStateException(ERROR_SUSPENSION);
            }
            repositoryAdministracion.registrarSuspension(
                    c, idUsuario, idAdministrador, motivoLimpio);
            repositoryAdministracion.notificar(c, idUsuario, recortar(
                    "Tu cuenta fue suspendida por un administrador. Motivo: "
                    + motivoLimpio));
            return null;
        });
    }

    /**
     * Comprueba, sin modificar nada, que la cuenta puede reactivarse.
     *
     * @return el motivo de la suspension vigente, para mostrarlo en la confirmacion
     */
    public String validarReactivacion(SesionUsuario sesion, int idUsuario) {

        validarAdministrador(sesion);
        validarIdUsuario(idUsuario);

        try (Connection c = repositoryAdministracion.abrirConexion()) {
            verificarOperadorVigente(c, sesion.getIdUsuario());
            UsuarioAdministrable objetivo = repositoryAdministracion
                    .buscarUsuario(c, idUsuario)
                    .orElseThrow(() -> new IllegalArgumentException(USUARIO_INEXISTENTE));
            reglasReactivacion(c, objetivo);
            return repositoryAdministracion.motivoSuspensionVigente(c, idUsuario)
                    .orElseThrow(() -> new IllegalStateException(HISTORIAL_INCONSISTENTE));
        } catch (SQLException e) {
            throw new IllegalStateException(ERROR_VERIFICACION, e);
        }
    }

    /**
     * Ampliacion de CU-029 (no forma parte del flujo oficial): devuelve a
     * ACTIVO una cuenta suspendida y cierra su suspension vigente, sin
     * borrar el historial ni tocar roles, permisos ni el estado del actor
     * (Proveedor u Organizador).
     */
    public void reactivarUsuario(SesionUsuario sesion, int idUsuario) {

        validarAdministrador(sesion);
        validarIdUsuario(idUsuario);
        int idAdministrador = sesion.getIdUsuario();

        enTransaccion(ERROR_REACTIVACION, c -> {
            repositoryAdministracion.bloquearAdministradores(c);
            verificarOperadorVigente(c, idAdministrador);

            UsuarioAdministrable objetivo = repositoryAdministracion
                    .bloquearUsuario(c, idUsuario)
                    .orElseThrow(() -> new IllegalArgumentException(USUARIO_INEXISTENTE));
            reglasReactivacion(c, objetivo);

            if (repositoryAdministracion.cerrarSuspensionVigente(
                    c, idUsuario, idAdministrador) != 1) {
                throw new IllegalStateException(HISTORIAL_INCONSISTENTE);
            }
            if (repositoryAdministracion.actualizarEstadoUsuario(
                    c, idUsuario, ESTADO_ACTIVO, ESTADO_SUSPENDIDO) != 1) {
                throw new IllegalStateException(ERROR_REACTIVACION);
            }
            repositoryAdministracion.notificar(c, idUsuario,
                    "Tu cuenta fue reactivada por un administrador. "
                    + "Ya puedes iniciar sesión nuevamente.");
            return null;
        });
    }

    /** Reglas de CU-029 que se aplican igual en la validacion previa y en la operacion. */
    private void reglasSuspension(Connection c, int idAdministrador,
            UsuarioAdministrable objetivo) throws SQLException {

        if (objetivo.idUsuario() == idAdministrador) {
            throw new IllegalStateException(AUTOSUSPENSION);
        }
        String estado = normalizar(objetivo.estado()).toUpperCase(Locale.ROOT);
        if (ESTADO_SUSPENDIDO.equals(estado)) {
            throw new IllegalStateException(YA_SUSPENDIDA);
        }
        if (!ESTADO_ACTIVO.equals(estado)) {
            throw new IllegalStateException(NO_ACTIVA);
        }
        if ("ADMINISTRADOR".equalsIgnoreCase(objetivo.tipoUsuario())
                && repositoryAdministracion.contarAdministradoresActivos(c) - 1 < 1) {
            throw new IllegalStateException(ULTIMO_ADMIN);
        }
        if (repositoryAdministracion.contarSuspensionesVigentes(
                c, objetivo.idUsuario()) != 0) {
            throw new IllegalStateException(HISTORIAL_INCONSISTENTE);
        }
    }

    private void reglasReactivacion(Connection c, UsuarioAdministrable objetivo)
            throws SQLException {

        String estado = normalizar(objetivo.estado()).toUpperCase(Locale.ROOT);
        if (ESTADO_ACTIVO.equals(estado)) {
            throw new IllegalStateException(YA_ACTIVA);
        }
        if (!ESTADO_SUSPENDIDO.equals(estado)) {
            throw new IllegalStateException(NO_SUSPENDIDA);
        }
        // Una cuenta suspendida debe tener exactamente una suspension vigente.
        // No se crean ni se reparan registros automaticamente.
        if (repositoryAdministracion.contarSuspensionesVigentes(
                c, objetivo.idUsuario()) != 1) {
            throw new IllegalStateException(HISTORIAL_INCONSISTENTE);
        }
    }

    /** Prefiltro sobre la sesion; la verificacion definitiva se hace en la base de datos. */
    private static void validarAdministrador(SesionUsuario sesion) {
        if (sesion == null
                || sesion.getIdUsuario() <= 0
                || !"ADMINISTRADOR".equalsIgnoreCase(sesion.getTipoUsuario())) {
            throw new SecurityException(SOLO_ADMINISTRADOR);
        }
        if (!ESTADO_ACTIVO.equalsIgnoreCase(sesion.getEstadoUsuario())) {
            throw new SecurityException(SESION_ADMIN_INVALIDA);
        }
    }

    /**
     * Comprueba en la base de datos que quien opera sigue existiendo,
     * es ADMINISTRADOR y esta ACTIVO. La sesion puede estar desactualizada.
     */
    private void verificarOperadorVigente(Connection c, int idAdministrador)
            throws SQLException {
        UsuarioAdministrable operador = repositoryAdministracion
                .buscarUsuario(c, idAdministrador)
                .orElseThrow(() -> new SecurityException(SESION_ADMIN_INVALIDA));
        if (!"ADMINISTRADOR".equalsIgnoreCase(operador.tipoUsuario())
                || !ESTADO_ACTIVO.equalsIgnoreCase(normalizar(operador.estado()))) {
            throw new SecurityException(SESION_ADMIN_INVALIDA);
        }
    }

    static String normalizarMotivo(String motivo) {
        String limpio = normalizar(motivo);
        if (limpio.isEmpty()) {
            throw new IllegalArgumentException(MOTIVO_OBLIGATORIO);
        }
        if (limpio.length() > MAX_MOTIVO) {
            throw new IllegalArgumentException(MOTIVO_LARGO);
        }
        return limpio;
    }

    private static String normalizar(String texto) {
        return texto == null ? "" : texto.trim().replaceAll("\\s+", " ");
    }

    private static String recortar(String mensaje) {
        return mensaje.length() <= MAX_NOTIFICACION
                ? mensaje
                : mensaje.substring(0, MAX_NOTIFICACION);
    }

    private static void validarIdUsuario(int idUsuario) {
        if (idUsuario <= 0) {
            throw new IllegalArgumentException(USUARIO_INVALIDO);
        }
    }

    @FunctionalInterface
    private interface Operacion<T> {
        T ejecutar(Connection c) throws SQLException;
    }

    /**
     * Ejecuta la operacion en una transaccion. Ante cualquier error se
     * revierte todo; los errores de negocio conservan su mensaje y los
     * tecnicos se informan con mensajeError (RNF-04 y RNF-06).
     */
    private <T> T enTransaccion(String mensajeError, Operacion<T> operacion) {
        try (Connection c = repositoryAdministracion.abrirConexion()) {
            c.setAutoCommit(false);
            try {
                T resultado = operacion.ejecutar(c);
                c.commit();
                return resultado;
            } catch (SQLException | RuntimeException e) {
                try {
                    c.rollback();
                } catch (SQLException errorRollback) {
                    e.addSuppressed(errorRollback);
                }
                if (e instanceof RuntimeException runtime) {
                    throw runtime;
                }
                throw new IllegalStateException(mensajeError, e);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(mensajeError, e);
        }
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

        if (ciudad.toString().trim().length() > 150
                || tipoEvento.toString().trim().length() > 100) {
            throw new IllegalArgumentException(
                    "La ciudad o el tipo de evento son demasiado largos.");
        }
    }
}

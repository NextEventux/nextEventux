
package services;

import entities.Contratacion;
import entities.Organizador;
import entities.SesionUsuario;

import repositories.RepositoryContratacion;
import repositories.RepositoryOrganizador;
import repositories.RepositoryAdministracion;
import repositories.RepositoryAdministracion.UsuarioResumen;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Servicio de administracion.
 *
 * Implementa la logica de negocio de:
 * CU-026: Contratar organizador.
 * CU-029: Suspender usuario.
 */
public class ServiceAdministracion {

    private final RepositoryContratacion repositoryContratacion;
    private final RepositoryOrganizador repositoryOrganizador;
    private final RepositoryAdministracion repositoryAdministracion;

    /**
     * Constructor original de CU-026.
     * Se conserva para no romper las implementaciones existentes.
     */
    public ServiceAdministracion(
            RepositoryContratacion repositoryContratacion,
            RepositoryOrganizador repositoryOrganizador) {

        this(
                repositoryContratacion,
                repositoryOrganizador,
                new RepositoryAdministracion()
        );
    }

    /**
     * Constructor ampliado para integrar CU-029.
     */
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

    // ==========================================================
    // CU-026: CONTRATAR ORGANIZADOR
    // ==========================================================

    /**
     * Consulta organizadores activos y verificados que coincidan
     * con la ciudad, el tipo de evento, el presupuesto y la fecha.
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

        seleccionarOrganizador(idOrganizador);

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

    // ==========================================================
    // CU-029: SUSPENDER USUARIO
    // ==========================================================

    /**
     * Verifica que la operacion sea realizada por un
     * administrador con cuenta activa.
     */
    private void validarAdministrador(SesionUsuario sesion) {

        if (sesion == null
                || sesion.getIdUsuario() <= 0
                || !"ADMINISTRADOR".equalsIgnoreCase(
                sesion.getTipoUsuario())
                || !"ACTIVO".equalsIgnoreCase(
                sesion.getEstadoUsuario())) {

            throw new IllegalStateException(
                    "Solo un administrador activo puede suspender usuarios.");
        }
    }

    /**
     * Consulta los usuarios registrados en el sistema.
     */
    public List<UsuarioResumen> consultarUsuarios(
            SesionUsuario sesion, String filtro) {

        validarAdministrador(sesion);

        String texto = filtro == null ? "" : filtro.trim();

        if (texto.length() > 100) {
            throw new IllegalArgumentException(
                    "El filtro de busqueda no puede superar 100 caracteres.");
        }

        try (Connection conexion =
                     repositoryAdministracion.abrirConexion()) {

            return repositoryAdministracion.listarUsuarios(
                    conexion, texto);

        } catch (SQLException e) {

            throw new IllegalStateException(
                    "No fue posible consultar los usuarios.", e);
        }
    }

    /**
     * Consulta la informacion del usuario seleccionado.
     */
    public UsuarioResumen consultarUsuario(
            SesionUsuario sesion, int idUsuario) {

        validarAdministrador(sesion);
        validarIdUsuario(idUsuario);

        try (Connection conexion =
                     repositoryAdministracion.abrirConexion()) {

            return repositoryAdministracion
                    .bloquearUsuario(conexion, idUsuario)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No se encontro el usuario seleccionado."));

        } catch (SQLException e) {

            throw new IllegalStateException(
                    "No fue posible consultar el usuario.", e);
        }
    }

    /**
     * Valida que el usuario pueda ser suspendido.
     * Esta operacion no modifica la cuenta.
     */
    public boolean validarSuspension(
            SesionUsuario sesion,
            int idUsuario,
            String motivo) {

        validarAdministrador(sesion);
        validarIdUsuario(idUsuario);

        validarMotivoSuspension(motivo);

        try (Connection conexion =
                     repositoryAdministracion.abrirConexion()) {

            UsuarioResumen usuario = repositoryAdministracion
                    .bloquearUsuario(conexion, idUsuario)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "El usuario seleccionado no existe."));
            // 1.1. Impedir que el administrador se suspenda a si mismo.
            if (sesion.getIdUsuario() == idUsuario) {
                throw new IllegalStateException(
                        "No puedes suspender tu propia cuenta.");
            }

            // 1.2. Proteger todas las cuentas administrativas.
            if ("ADMINISTRADOR".equalsIgnoreCase(usuario.tipoUsuario())) {
                throw new IllegalStateException(
                        "No puedes suspender cuentas de otros administradores.");
            }

            if (!"ACTIVO".equalsIgnoreCase(usuario.estado())) {

                throw new IllegalStateException(
                        "El usuario ya no se encuentra activo. "
                                + "No es posible suspenderlo nuevamente.");
            }

            return true;

        } catch (SQLException e) {

            throw new IllegalStateException(
                    "No fue posible validar la suspension.", e);
        }
    }

    /**
     * Ejecuta la suspension completa del usuario.
     *
     * Actualiza el estado, registra el motivo y genera
     * una notificacion interna dentro de una transaccion.
     */
    public void suspenderUsuario(
            SesionUsuario sesion,
            int idUsuario,
            String motivo) {

        validarAdministrador(sesion);
        validarIdUsuario(idUsuario);

        String motivoLimpio = validarMotivoSuspension(motivo);

        try (Connection conexion =
                     repositoryAdministracion.abrirConexion()) {

            conexion.setAutoCommit(false);

            try {

                // 1. Recuperar y bloquear al usuario.
                UsuarioResumen usuario = repositoryAdministracion
                        .bloquearUsuario(conexion, idUsuario)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "El usuario seleccionado no existe."));

                // 2. Verificar su estado actual.
                if (!"ACTIVO".equalsIgnoreCase(usuario.estado())) {

                    throw new IllegalStateException(
                            "El usuario ya se encuentra suspendido "
                                    + "o no esta activo.");
                }

                // 3. Actualizar el estado de la cuenta.
                int actualizados = repositoryAdministracion
                        .actualizarEstadoUsuario(conexion, idUsuario);

                if (actualizados != 1) {

                    throw new IllegalStateException(
                            "No fue posible actualizar el estado del usuario.");
                }

                // 4. Registrar el motivo y el administrador responsable.
                repositoryAdministracion.registrarSuspension(
                        conexion,
                        idUsuario,
                        sesion.getIdUsuario(),
                        motivoLimpio
                );

                // 5. Generar la notificacion interna.
                String mensaje =
                        "Tu cuenta ha sido suspendida. Motivo: "
                                + motivoLimpio;

                if (mensaje.length() > 500) {
                    mensaje = mensaje.substring(0, 500);
                }

                repositoryAdministracion.notificarUsuario(
                        conexion,
                        idUsuario,
                        mensaje
                );

                // 6. Confirmar todas las operaciones.
                conexion.commit();

            } catch (SQLException | RuntimeException e) {

                // Si alguna operacion falla, revertir todo.
                try {
                    conexion.rollback();
                } catch (SQLException errorRollback) {
                    e.addSuppressed(errorRollback);
                }

                if (e instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }

                throw new IllegalStateException(
                        "No fue posible completar la suspension. "
                                + "La cuenta conserva su estado anterior.", e);
            }

        } catch (SQLException e) {

            throw new IllegalStateException(
                    "No fue posible completar la suspension.", e);
        }
    }

    /**
     * Reactiva una cuenta suspendida.
     *
     * Registra la operacion, conserva el historial de suspensiones
     * y genera una notificacion interna.
     * Todas las operaciones se ejecutan en una transaccion.
     */
    public void reactivarUsuario(
            SesionUsuario sesion,
            int idUsuario,
            String motivo) {

        validarAdministrador(sesion);
        validarIdUsuario(idUsuario);

        String motivoLimpio = motivo == null ? null : motivo.trim();

        if (motivoLimpio != null && motivoLimpio.isEmpty()) {
            motivoLimpio = null;
        }

        if (motivoLimpio != null && motivoLimpio.length() > 500) {
            throw new IllegalArgumentException(
                    "El motivo de reactivacion no puede superar los 500 caracteres.");
        }

        try (Connection conexion =
                     repositoryAdministracion.abrirConexion()) {

            conexion.setAutoCommit(false);

            try {

                // 1. Recuperar y bloquear la cuenta.
                UsuarioResumen usuario = repositoryAdministracion
                        .bloquearUsuario(conexion, idUsuario)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "El usuario seleccionado no existe."));

                // 2. Verificar que realmente este suspendido.
                if (!"SUSPENDIDO".equalsIgnoreCase(usuario.estado())) {
                    throw new IllegalStateException(
                            "La cuenta seleccionada no esta suspendida. "
                                    + "No es necesario reactivarla.");
                }

                // 3. Actualizar el estado de la cuenta.
                int actualizados = repositoryAdministracion
                        .actualizarEstadoUsuarioReactivacion(
                                conexion, idUsuario);

                if (actualizados != 1) {
                    throw new IllegalStateException(
                            "No fue posible actualizar el estado del usuario.");
                }

                // 4. Registrar quien realizo la reactivacion.
                repositoryAdministracion.registrarReactivacion(
                        conexion,
                        idUsuario,
                        sesion.getIdUsuario(),
                        motivoLimpio
                );

                // 5. Generar notificacion interna.
                String mensaje = "Tu cuenta ha sido reactivada. "
                        + "Ya puedes ingresar nuevamente a NextEventux.";

                repositoryAdministracion.notificarUsuario(
                        conexion,
                        idUsuario,
                        mensaje
                );

                // 6. Confirmar todas las operaciones.
                conexion.commit();

            } catch (SQLException | RuntimeException e) {

                // Revertir todos los cambios si algo falla.
                try {
                    conexion.rollback();
                } catch (SQLException errorRollback) {
                    e.addSuppressed(errorRollback);
                }

                if (e instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }

                throw new IllegalStateException(
                        "No fue posible completar la reactivacion. "
                                + "La cuenta conserva su estado anterior.",
                        e);
            }

        } catch (SQLException e) {

            throw new IllegalStateException(
                    "No fue posible completar la reactivacion.", e);
        }
    }

    /**
     * Valida el identificador del usuario.
     */
    private void validarIdUsuario(int idUsuario) {

        if (idUsuario <= 0) {
            throw new IllegalArgumentException(
                    "Selecciona un usuario valido.");
        }
    }

    /**
     * Valida el motivo obligatorio de la suspension.
     */
    private String validarMotivoSuspension(String motivo) {

        if (motivo == null || motivo.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Debes indicar el motivo de la suspension.");
        }

        String limpio = motivo.trim();

        if (limpio.length() > 500) {

            throw new IllegalArgumentException(
                    "El motivo no puede superar los 500 caracteres.");
        }

        return limpio;
    }

    // ==========================================================
    // VALIDACIONES DE CU-026
    // ==========================================================

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

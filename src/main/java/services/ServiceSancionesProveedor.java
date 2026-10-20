package services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import entities.SesionUsuario;
import repositories.RepositorySancionesProveedor;
import repositories.RepositorySancionesProveedor.ContratoSancionable;
import repositories.RepositorySancionesProveedor.EventoProveedor;
import repositories.RepositorySancionesProveedor.ProveedorSancionable;
import repositories.RepositorySancionesProveedor.ResumenProveedor;

/**
 * Logica de negocio de CU-09: aplicar sanciones a proveedores.
 *
 * Reglas:
 * - Solo un administrador activo puede aplicar sanciones.
 * - SUSPENSION: el proveedor pasa a SUSPENDIDO y deja de aparecer en contrataciones
 *   (CU-13 solo acepta proveedores APROBADO o VERIFICADO). No puede suspenderse dos veces.
 * - MULTA: se descuenta el porcentaje del valor acordado del contrato. Las multas
 *   se acumulan, pero la suma nunca puede superar el valor del contrato.
 * - Cada sanción y su notificacion interna se guardan en la misma transaccion.
 */
public class ServiceSancionesProveedor {

    public static final String SUSPENSION = "SUSPENSION";
    public static final String MULTA = "MULTA";

    public static final Set<String> TIPOS_FALTA = Set.of(
            "INASISTENCIA", "RETRASO", "INCUMPLIMIENTO_CONDICIONES", "CANCELACION_TARDIA");

    static final int MIN_JUSTIFICACION = 10;
    static final int MAX_JUSTIFICACION = 150;
    static final int MAX_MOTIVO = 255;

    private static final BigDecimal CIEN = new BigDecimal("100");

    /** porcentaje llega como texto para validarlo aqui (acepta coma o punto decimal). */
    public record SolicitudSancion(
            int idProveedor,
            int idEvento,
            String tipoFalta,
            String tipoSancion,
            int idContratacion,
            String porcentaje,
            String justificacion) { }

    public record ResultadoSancion(
            String tipoSancion,
            BigDecimal valorDescontado,
            BigDecimal saldoPendiente,
            String estadoProveedor,
            String mensaje) { }

    private final RepositorySancionesProveedor repositorio;

    public ServiceSancionesProveedor(RepositorySancionesProveedor repositorio) {
        if (repositorio == null) {
            throw new IllegalArgumentException("El repositorio de sanciones es obligatorio.");
        }
        this.repositorio = repositorio;
    }

    public List<ResumenProveedor> buscarProveedores(SesionUsuario sesion, String texto) {
        validarAdministrador(sesion);
        String limpio = normalizar(texto);
        if (limpio.length() > 100) {
            throw new IllegalArgumentException("La búsqueda no puede superar 100 caracteres.");
        }
        int id = limpio.matches("\\d{1,9}") ? Integer.parseInt(limpio) : -1;
        String like = "%" + limpio.toUpperCase(Locale.ROOT).replace("%", "").replace("_", "") + "%";
        try (Connection c = repositorio.abrirConexion()) {
            return repositorio.buscarProveedores(c, like, id);
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo buscar proveedores. Vuelve a intentarlo.", e);
        }
    }

    public List<EventoProveedor> eventosDelProveedor(SesionUsuario sesion, int idProveedor) {
        validarAdministrador(sesion);
        validarId(idProveedor, "el proveedor");
        try (Connection c = repositorio.abrirConexion()) {
            return repositorio.eventosDelProveedor(c, idProveedor);
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron cargar los eventos del proveedor.", e);
        }
    }

    public List<ContratoSancionable> contratosActivos(SesionUsuario sesion, int idProveedor, int idEvento) {
        validarAdministrador(sesion);
        validarId(idProveedor, "el proveedor");
        validarId(idEvento, "el evento");
        try (Connection c = repositorio.abrirConexion()) {
            return repositorio.contratosActivos(c, idProveedor, idEvento, null, false);
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron cargar los contratos del proveedor.", e);
        }
    }

    public ResultadoSancion aplicarSancion(SesionUsuario sesion, SolicitudSancion solicitud) {
        validarAdministrador(sesion);
        if (solicitud == null) {
            throw new IllegalArgumentException("La sanción es obligatoria.");
        }
        validarId(solicitud.idProveedor(), "el proveedor");
        validarId(solicitud.idEvento(), "el evento");

        String falta = normalizar(solicitud.tipoFalta()).toUpperCase(Locale.ROOT);
        if (!TIPOS_FALTA.contains(falta)) {
            throw new IllegalArgumentException("Selecciona un tipo de falta válido.");
        }

        String tipo = normalizar(solicitud.tipoSancion()).toUpperCase(Locale.ROOT);
        if (!SUSPENSION.equals(tipo) && !MULTA.equals(tipo)) {
            throw new IllegalArgumentException("Selecciona un tipo de sanción válido.");
        }

        String justificacion = normalizar(solicitud.justificacion());
        if (justificacion.length() < MIN_JUSTIFICACION) {
            throw new IllegalArgumentException(
                    "La justificación debe tener al menos " + MIN_JUSTIFICACION + " caracteres.");
        }
        if (justificacion.length() > MAX_JUSTIFICACION) {
            throw new IllegalArgumentException(
                    "La justificación no puede superar " + MAX_JUSTIFICACION + " caracteres.");
        }

        BigDecimal porcentaje = null;
        if (MULTA.equals(tipo)) {
            if (solicitud.idContratacion() <= 0) {
                throw new IllegalArgumentException("Selecciona el contrato al que se aplica la multa.");
            }
            porcentaje = parsePorcentaje(solicitud.porcentaje());
        }

        String motivo = construirMotivo(tipo, solicitud.idProveedor(), solicitud.idContratacion(),
                porcentaje, falta, justificacion);
        if (motivo.length() > MAX_MOTIVO) {
            throw new IllegalArgumentException("La justificación es demasiado larga para registrar la sanción.");
        }

        try (Connection c = repositorio.abrirConexion()) {
            c.setAutoCommit(false);
            try {
                ResultadoSancion resultado = MULTA.equals(tipo)
                        ? aplicarMulta(c, solicitud, porcentaje, motivo)
                        : aplicarSuspension(c, solicitud, motivo);
                c.commit();
                return resultado;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                if (e instanceof RuntimeException re) {
                    throw re;
                }
                throw new IllegalStateException("No se pudo aplicar la sanción. No se guardó ningún cambio.", e);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo aplicar la sanción. Vuelve a intentarlo.", e);
        }
    }

    private ResultadoSancion aplicarSuspension(Connection c, SolicitudSancion s, String motivo)
            throws SQLException {
        ProveedorSancionable proveedor = bloquearProveedorActivo(c, s.idProveedor(), s.idEvento());

        if ("SUSPENDIDO".equalsIgnoreCase(proveedor.estado())) {
            throw new IllegalArgumentException("Este proveedor ya se encuentra suspendido actualmente.");
        }

        repositorio.insertarSancion(c, s.idEvento(), motivo, BigDecimal.ZERO.setScale(2));
        repositorio.actualizarEstadoProveedor(c, s.idProveedor(), "SUSPENDIDO");
        repositorio.notificar(c, proveedor.idUsuario(), s.idEvento(),
                "Tu cuenta de proveedor fue suspendida. No recibirás nuevos contratos hasta que se levante la suspensión.");

        return new ResultadoSancion(SUSPENSION, BigDecimal.ZERO.setScale(2), null, "SUSPENDIDO",
                "Sanción aplicada. El proveedor fue suspendido y notificado.");
    }

    private ResultadoSancion aplicarMulta(Connection c, SolicitudSancion s, BigDecimal porcentaje, String motivo)
            throws SQLException {
        ProveedorSancionable proveedor = bloquearProveedorActivo(c, s.idProveedor(), s.idEvento());

        List<ContratoSancionable> contratos = repositorio.contratosActivos(
                c, s.idProveedor(), s.idEvento(), s.idContratacion(), true);
        if (contratos.isEmpty()) {
            throw new IllegalArgumentException(
                    "El contrato no existe, no pertenece a este proveedor o no está activo en ese evento.");
        }
        ContratoSancionable contrato = contratos.get(0);
        if (contrato.valorAcordado() == null) {
            throw new IllegalArgumentException("El contrato no tiene un valor acordado.");
        }

        BigDecimal previas = repositorio.multasAplicadas(c, s.idProveedor(), s.idContratacion());
        BigDecimal saldoAntes = contrato.valorAcordado().subtract(previas);
        BigDecimal valor = contrato.valorAcordado()
                .multiply(porcentaje)
                .divide(CIEN, 2, RoundingMode.HALF_UP);

        if (valor.signum() <= 0) {
            throw new IllegalArgumentException("La multa calculada es demasiado pequeña para registrarse.");
        }
        if (saldoAntes.signum() <= 0 || valor.compareTo(saldoAntes) > 0) {
            throw new IllegalArgumentException("La multa de $" + valor.toPlainString()
                    + " supera el saldo pendiente del contrato ($" + saldoAntes.toPlainString() + ").");
        }

        BigDecimal saldoDespues = saldoAntes.subtract(valor);
        repositorio.insertarSancion(c, s.idEvento(), motivo, valor);
        repositorio.notificar(c, proveedor.idUsuario(), s.idEvento(),
                "Se aplicó una multa de $" + valor.toPlainString() + " al contrato #"
                        + s.idContratacion() + ". Saldo pendiente: $" + saldoDespues.toPlainString() + ".");

        return new ResultadoSancion(MULTA, valor, saldoDespues, proveedor.estado(),
                "Multa aplicada. Saldo pendiente del contrato: $" + saldoDespues.toPlainString() + ".");
    }

    /** Bloquea el proveedor y comprueba que siga activo y tenga servicios en el evento. */
    private ProveedorSancionable bloquearProveedorActivo(Connection c, int idProveedor, int idEvento)
            throws SQLException {
        ProveedorSancionable proveedor = repositorio.bloquearProveedor(c, idProveedor)
                .orElseThrow(() -> new IllegalArgumentException("El proveedor no existe."));

        String estado = normalizar(proveedor.estado()).toUpperCase(Locale.ROOT);
        if (!"APROBADO".equals(estado) && !"VERIFICADO".equals(estado) && !"SUSPENDIDO".equals(estado)) {
            throw new IllegalArgumentException("El proveedor no está activo y no puede recibir sanciones.");
        }

        boolean vinculado = repositorio.eventosDelProveedor(c, idProveedor).stream()
                .anyMatch(e -> e.idEvento() == idEvento);
        if (!vinculado) {
            throw new IllegalArgumentException("El proveedor no tiene servicios en el evento seleccionado.");
        }
        return proveedor;
    }

    /**
     * Formato del campo motivo de la tabla Sancion (no hay columnas propias en el esquema):
     * CU09|MULTA|PROV=id|CONTRATO=id|PCT=x|FALTA=tipo|justificacion
     * CU09|SUSPENSION|PROV=id|FALTA=tipo|justificacion
     */
    static String construirMotivo(String tipo, int idProveedor, int idContratacion,
            BigDecimal porcentaje, String falta, String justificacion) {
        if (MULTA.equals(tipo)) {
            return "CU09|MULTA|PROV=" + idProveedor + "|CONTRATO=" + idContratacion
                    + "|PCT=" + porcentaje.toPlainString() + "|FALTA=" + falta + "|" + justificacion;
        }
        return "CU09|SUSPENSION|PROV=" + idProveedor + "|FALTA=" + falta + "|" + justificacion;
    }

    static BigDecimal parsePorcentaje(String texto) {
        String limpio = normalizar(texto).replace(',', '.');
        if (limpio.isEmpty()) {
            throw new IllegalArgumentException("Indica el porcentaje de la multa.");
        }
        BigDecimal valor;
        try {
            valor = new BigDecimal(limpio);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El porcentaje debe ser un número, por ejemplo 10 o 12,5.");
        }
        if (valor.signum() <= 0 || valor.compareTo(CIEN) > 0) {
            throw new IllegalArgumentException("El porcentaje debe ser mayor que 0 y no superar 100.");
        }
        if (valor.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("El porcentaje admite como máximo dos decimales.");
        }
        return valor;
    }

    static String normalizar(String texto) {
        return texto == null ? "" : texto.trim().replaceAll("\\s+", " ");
    }

    private static void validarId(int id, String entidad) {
        if (id <= 0) {
            throw new IllegalArgumentException("Selecciona " + entidad + " válido.");
        }
    }

    private static void validarAdministrador(SesionUsuario sesion) {
        if (sesion == null || !"ADMINISTRADOR".equalsIgnoreCase(sesion.getTipoUsuario())) {
            throw new SecurityException("Solo un administrador puede gestionar sanciones.");
        }
        if (sesion.getEstadoUsuario() != null && !"ACTIVO".equalsIgnoreCase(sesion.getEstadoUsuario())) {
            throw new SecurityException("La cuenta del administrador no está activa.");
        }
    }
}

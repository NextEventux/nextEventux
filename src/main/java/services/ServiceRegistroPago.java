package services;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import entities.SesionUsuario;
import entities.eventos.Evento;
import entities.finanzas.SaldoContratacion;
import entities.finanzas.SolicitudPago;
import entities.finanzas.TipoPago;
import repositories.RepositoryMovimientoFinanciero;
import repositories.eventos.RepositoryEvento;

/**
 * CU-10: registrar pago de una contratación.
 *
 * - Solo el organizador activo dueño del evento puede registrar pagos, y el evento no
 *   puede estar CANCELADO.
 * - Se listan las contrataciones que cuenta CU06 (no CANCELADA) con lo pagado y el saldo.
 * - Un pago nunca supera el saldo pendiente: valor acordado − pagos REGISTRADO.
 * - Los pagos programados (PENDIENTE) reservan su parte del saldo: un pago nuevo solo usa el
 *   saldo libre (saldo − pagos programados); para pagar lo programado se registra ese pago.
 * - Al confirmar se vuelven a aplicar todas las reglas y se exige que el saldo siga siendo
 *   el que vio el organizador; así un resumen viejo o repetido no se puede guardar.
 * - No se cambia la estructura de la base: el tipo de pago va al inicio de la descripción
 *   del movimiento y un pago programado (PENDIENTE) se registra cambiando su estado.
 */
public class ServiceRegistroPago {

    public static final int MAXIMO_DETALLE = 200;
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final RepositoryMovimientoFinanciero repoMovimientos;
    private final RepositoryEvento repoEventos;

    public ServiceRegistroPago(RepositoryMovimientoFinanciero repoMovimientos, RepositoryEvento repoEventos) {
        this.repoMovimientos = repoMovimientos;
        this.repoEventos = repoEventos;
    }

    /** Pasos 3 y 4: verifica el evento y lista sus contrataciones con lo pagado y el saldo. */
    public List<SaldoContratacion> listarContratacionesPorPagar(int idEvento, SesionUsuario sesion) {
        verificarEvento(idEvento, sesion);
        try {
            return repoMovimientos.listarSaldosPorEvento(idEvento);
        } catch (RuntimeException e) {
            throw new IllegalStateException("No se pudieron consultar las contrataciones. Vuelve a intentarlo.", e);
        }
    }

    /** Pasos 8 y 9: valida los datos del formulario y arma el resumen que se confirma. */
    public SolicitudPago validarPago(int idEvento, int idContratacion, TipoPago tipo, BigDecimal valor,
                                     LocalDate fecha, String detalle, boolean pagoProgramado,
                                     SesionUsuario sesion) {
        return validarPago(idEvento, idContratacion, tipo, valor, fecha, detalle, pagoProgramado,
                sesion, LocalDate.now());
    }

    /** Igual que el anterior, con la fecha de hoy explícita para poder probar las reglas de fecha. */
    public SolicitudPago validarPago(int idEvento, int idContratacion, TipoPago tipo, BigDecimal valor,
                                     LocalDate fecha, String detalle, boolean pagoProgramado,
                                     SesionUsuario sesion, LocalDate hoy) {
        SaldoContratacion contratacion = buscarContratacion(idEvento, idContratacion, sesion);
        if (!contratacion.admitePagos()) {
            throw new IllegalArgumentException(contratacion.tieneValor()
                    ? "Esta contratación ya está pagada."
                    : "Esta contratación no tiene valor acordado; no se pueden registrar pagos.");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("Selecciona el tipo de pago.");
        }
        validarValor(contratacion, tipo, valor, pagoProgramado);
        validarFecha(contratacion, fecha, hoy);
        if (detalle != null && detalle.trim().length() > MAXIMO_DETALLE) {
            throw new IllegalArgumentException(
                    "La descripción puede tener máximo " + MAXIMO_DETALLE + " caracteres.");
        }
        Integer idProgramado = null;
        if (pagoProgramado) {
            if (!contratacion.tienePagoProgramado()) {
                throw new IllegalArgumentException("Esta contratación no tiene pagos programados.");
            }
            if (valor.compareTo(contratacion.getValorPagoProgramado()) != 0) {
                throw new IllegalArgumentException("El pago programado es de "
                        + dinero(contratacion.getValorPagoProgramado())
                        + "; para otro valor registra un pago nuevo.");
            }
            idProgramado = contratacion.getIdPagoProgramado();
        }
        return new SolicitudPago(idEvento, contratacion, tipo, valor, fecha, detalle, idProgramado);
    }

    /**
     * Pasos 10 a 12: registra el pago confirmado y devuelve la contratación con su nuevo saldo.
     * - Vuelve a aplicar todas las validaciones, aunque el pago no venga de validarPago.
     * - Exige que el saldo y los pagos programados sigan siendo los del resumen; si cambiaron
     *   (por ejemplo, otro pago o un reintento del mismo resumen), no se guarda nada.
     * - El repositorio repite la verificación dentro de la transacción.
     * - Después de guardar no se hace otra consulta: el nuevo saldo se calcula con los datos
     *   ya validados, así que un pago guardado nunca se reporta como fallido.
     */
    public SaldoContratacion confirmarPago(SolicitudPago pago, SesionUsuario sesion) {
        if (pago == null || pago.getContratacion() == null) {
            throw new IllegalArgumentException("Revisa los datos del pago antes de confirmarlo.");
        }
        SaldoContratacion mostrada = pago.getContratacion();
        SolicitudPago validado = validarPago(pago.getIdEvento(), mostrada.getIdContratacion(), pago.getTipo(),
                pago.getValor(), pago.getFecha(), pago.getDetalle(), pago.esPagoProgramado(), sesion);
        SaldoContratacion actual = validado.getContratacion();
        boolean mismoSaldo = mostrada.getSaldo() != null
                && actual.getSaldo().compareTo(mostrada.getSaldo()) == 0
                && actual.getTotalProgramado().compareTo(mostrada.getTotalProgramado()) == 0;
        boolean mismoProgramado = java.util.Objects.equals(validado.getIdPagoProgramado(), pago.getIdPagoProgramado());
        if (!mismoSaldo || !mismoProgramado) {
            throw new IllegalStateException("El saldo de la contratación cambió desde que revisaste el pago. "
                    + "Vuelve a las contrataciones y revisa el pago.");
        }
        try {
            repoMovimientos.registrarPago(validado);
        } catch (IllegalStateException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new IllegalStateException("No se pudo registrar el pago. Vuelve a las contrataciones "
                    + "y revisa el saldo antes de intentarlo de nuevo.", e);
        }
        return saldoDespuesDelPago(validado);
    }

    /** Contratación con el pago ya sumado, calculada sin consultar de nuevo la base. */
    private static SaldoContratacion saldoDespuesDelPago(SolicitudPago pago) {
        SaldoContratacion antes = pago.getContratacion();
        BigDecimal programado = pago.esPagoProgramado()
                ? antes.getTotalProgramado().subtract(pago.getValor())
                : antes.getTotalProgramado();
        return new SaldoContratacion(antes.getIdContratacion(), antes.getIdPropuesta(), antes.getServicio(),
                antes.getProveedor(), antes.getFechaContratacion(), antes.getValorAcordado(),
                antes.getPagado().add(pago.getValor()), null, null, programado);
    }

    private SaldoContratacion buscarContratacion(int idEvento, int idContratacion, SesionUsuario sesion) {
        for (SaldoContratacion contratacion : listarContratacionesPorPagar(idEvento, sesion)) {
            if (contratacion.getIdContratacion() == idContratacion) {
                return contratacion;
            }
        }
        throw new IllegalArgumentException("Selecciona una contratación vigente de este evento.");
    }

    private static void validarValor(SaldoContratacion contratacion, TipoPago tipo, BigDecimal valor,
                                     boolean pagoProgramado) {
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException("Ingresa un valor mayor que cero.");
        }
        if (valor.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("El valor puede tener máximo 2 decimales.");
        }
        BigDecimal saldo = contratacion.getSaldo();
        if (valor.compareTo(saldo) > 0) {
            throw new IllegalArgumentException("El valor supera el saldo pendiente de " + dinero(saldo) + ".");
        }
        if (tipo == TipoPago.ANTICIPO && contratacion.getPagado().signum() > 0) {
            throw new IllegalArgumentException(
                    "Ya hay pagos registrados en esta contratación; usa Abono o Pago final.");
        }
        // Un pago nuevo solo puede usar el saldo libre; lo programado queda reservado.
        BigDecimal programado = contratacion.getTotalProgramado();
        if (!pagoProgramado && programado.signum() > 0) {
            if (tipo == TipoPago.PAGO_FINAL) {
                throw new IllegalArgumentException("Hay " + dinero(programado) + " en pagos programados; "
                        + "registra el pago programado para completar el pago.");
            }
            if (valor.compareTo(contratacion.getSaldoLibre()) > 0) {
                throw new IllegalArgumentException("Este pago supera el saldo libre de "
                        + dinero(contratacion.getSaldoLibre()) + ": hay " + dinero(programado)
                        + " en pagos programados. Registra el pago programado o ajusta el valor.");
            }
        }
        if (tipo == TipoPago.PAGO_FINAL && valor.compareTo(saldo) != 0) {
            throw new IllegalArgumentException(
                    "El pago final debe ser igual al saldo pendiente de " + dinero(saldo) + ".");
        }
    }

    private static void validarFecha(SaldoContratacion contratacion, LocalDate fecha, LocalDate hoy) {
        if (fecha == null) {
            throw new IllegalArgumentException("Ingresa la fecha del pago.");
        }
        if (fecha.isAfter(hoy)) {
            throw new IllegalArgumentException("La fecha del pago no puede ser futura.");
        }
        if (contratacion.getFechaContratacion() != null) {
            LocalDate contratada = contratacion.getFechaContratacion().toLocalDate();
            if (fecha.isBefore(contratada)) {
                throw new IllegalArgumentException("La fecha del pago no puede ser anterior a la contratación ("
                        + contratada.format(FORMATO_FECHA) + ").");
            }
        }
    }

    /** Formato de pesos colombianos para los mensajes: $ 1.800.000. */
    private static String dinero(BigDecimal valor) {
        NumberFormat formato = NumberFormat.getNumberInstance(new Locale("es", "CO"));
        formato.setMaximumFractionDigits(2);
        return "$ " + formato.format(valor);
    }

    /** Paso 3: organizador activo, evento propio y no CANCELADO. */
    private Evento verificarEvento(int idEvento, SesionUsuario sesion) {
        if (sesion == null
                || !"ORGANIZADOR".equalsIgnoreCase(sesion.getTipoUsuario())
                || !repoEventos.organizadorActivo(sesion.getIdUsuario(), sesion.getIdActor())) {
            throw new IllegalStateException("Inicia sesión como organizador activo para registrar pagos.");
        }
        if (idEvento <= 0) {
            throw new IllegalArgumentException("Selecciona uno de tus eventos para registrar pagos.");
        }
        Evento evento = repoEventos.buscarEvento(idEvento, sesion.getIdActor());
        if (evento == null) {
            throw new IllegalArgumentException("Selecciona un evento existente de tu cuenta.");
        }
        String estado = evento.getEstado() == null ? "" : evento.getEstado().trim();
        if ("CANCELADO".equalsIgnoreCase(estado)) {
            throw new IllegalStateException("No se pueden registrar pagos de un evento cancelado.");
        }
        return evento;
    }
}

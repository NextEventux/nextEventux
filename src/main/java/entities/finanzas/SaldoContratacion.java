package entities.finanzas;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * CU-10: una contratación del evento con lo acordado, lo pagado y el saldo pendiente.
 * - valorAcordado es null cuando la contratación no tiene valor acordado ni propuesta con valor.
 * - pagado suma solo los pagos REGISTRADO de la contratación.
 * - Si la contratación tiene un pago programado (PENDIENTE), se guarda el más antiguo
 *   para ofrecer registrarlo en lugar de crear otro movimiento.
 * - totalProgramado suma todos los pagos PENDIENTE: esa parte del saldo queda reservada,
 *   así que un pago nuevo solo puede usar el saldo libre (saldo − totalProgramado).
 */
public class SaldoContratacion {

    private final int idContratacion;
    private final int idPropuesta;
    private final String servicio;
    private final String proveedor;
    private final LocalDateTime fechaContratacion;
    private final BigDecimal valorAcordado;
    private final BigDecimal pagado;
    private final Integer idPagoProgramado;
    private final BigDecimal valorPagoProgramado;
    private final BigDecimal totalProgramado;

    /** Sin total programado explícito: se toma el valor del pago programado (o 0 si no hay). */
    public SaldoContratacion(int idContratacion, int idPropuesta, String servicio, String proveedor,
                             LocalDateTime fechaContratacion, BigDecimal valorAcordado, BigDecimal pagado,
                             Integer idPagoProgramado, BigDecimal valorPagoProgramado) {
        this(idContratacion, idPropuesta, servicio, proveedor, fechaContratacion, valorAcordado, pagado,
                idPagoProgramado, valorPagoProgramado, valorPagoProgramado);
    }

    public SaldoContratacion(int idContratacion, int idPropuesta, String servicio, String proveedor,
                             LocalDateTime fechaContratacion, BigDecimal valorAcordado, BigDecimal pagado,
                             Integer idPagoProgramado, BigDecimal valorPagoProgramado,
                             BigDecimal totalProgramado) {
        this.idContratacion = idContratacion;
        this.idPropuesta = idPropuesta;
        this.servicio = servicio;
        this.proveedor = proveedor;
        this.fechaContratacion = fechaContratacion;
        this.valorAcordado = valorAcordado;
        this.pagado = pagado == null ? BigDecimal.ZERO : pagado;
        this.idPagoProgramado = idPagoProgramado;
        this.valorPagoProgramado = valorPagoProgramado;
        this.totalProgramado = totalProgramado == null ? BigDecimal.ZERO : totalProgramado;
    }

    public int getIdContratacion() {
        return idContratacion;
    }

    public int getIdPropuesta() {
        return idPropuesta;
    }

    public String getServicio() {
        return servicio;
    }

    public String getProveedor() {
        return proveedor;
    }

    public LocalDateTime getFechaContratacion() {
        return fechaContratacion;
    }

    public BigDecimal getValorAcordado() {
        return valorAcordado;
    }

    public BigDecimal getPagado() {
        return pagado;
    }

    public Integer getIdPagoProgramado() {
        return idPagoProgramado;
    }

    public BigDecimal getValorPagoProgramado() {
        return valorPagoProgramado;
    }

    /** Suma de todos los pagos programados (PENDIENTE) de la contratación. */
    public BigDecimal getTotalProgramado() {
        return totalProgramado;
    }

    public boolean tieneValor() {
        return valorAcordado != null;
    }

    public boolean tienePagoProgramado() {
        return idPagoProgramado != null && valorPagoProgramado != null;
    }

    /** Saldo pendiente: valor acordado − pagado. null si no hay valor acordado. */
    public BigDecimal getSaldo() {
        return tieneValor() ? valorAcordado.subtract(pagado) : null;
    }

    /**
     * Saldo que pueden usar los pagos nuevos: saldo − pagos programados. Nunca es negativo.
     * null si no hay valor acordado.
     */
    public BigDecimal getSaldoLibre() {
        if (!tieneValor()) {
            return null;
        }
        BigDecimal libre = getSaldo().subtract(totalProgramado);
        return libre.signum() < 0 ? BigDecimal.ZERO : libre;
    }

    /** Solo se pueden registrar pagos si hay valor acordado y queda saldo. */
    public boolean admitePagos() {
        return tieneValor() && getSaldo().signum() > 0;
    }

    /** Motivo que muestra la lista cuando la contratación no admite pagos; null si los admite. */
    public String motivoSinPagos() {
        if (!tieneValor()) {
            return "Sin valor acordado";
        }
        return getSaldo().signum() > 0 ? null : "Pagada";
    }
}

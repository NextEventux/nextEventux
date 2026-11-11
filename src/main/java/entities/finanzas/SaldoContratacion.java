package entities.finanzas;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * CU-10: una contratación del evento con lo acordado, lo pagado y el saldo pendiente.
 * - valorAcordado es null cuando la contratación no tiene valor acordado ni propuesta con valor.
 * - pagado suma solo los pagos REGISTRADO de la contratación.
 * - Si la contratación tiene un pago programado (PENDIENTE), se guarda el más antiguo
 *   para ofrecer registrarlo en lugar de crear otro movimiento.
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

    public SaldoContratacion(int idContratacion, int idPropuesta, String servicio, String proveedor,
                             LocalDateTime fechaContratacion, BigDecimal valorAcordado, BigDecimal pagado,
                             Integer idPagoProgramado, BigDecimal valorPagoProgramado) {
        this.idContratacion = idContratacion;
        this.idPropuesta = idPropuesta;
        this.servicio = servicio;
        this.proveedor = proveedor;
        this.fechaContratacion = fechaContratacion;
        this.valorAcordado = valorAcordado;
        this.pagado = pagado == null ? BigDecimal.ZERO : pagado;
        this.idPagoProgramado = idPagoProgramado;
        this.valorPagoProgramado = valorPagoProgramado;
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

package entities.finanzas;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * CU-10: pago ya validado que el organizador revisa antes de confirmarlo (pasos 9 y 10).
 * Si idPagoProgramado no es null, al confirmar se registra ese pago PENDIENTE
 * en lugar de crear un movimiento nuevo.
 */
public class SolicitudPago {

    private final int idEvento;
    private final SaldoContratacion contratacion;
    private final TipoPago tipo;
    private final BigDecimal valor;
    private final LocalDate fecha;
    private final String detalle;
    private final Integer idPagoProgramado;

    public SolicitudPago(int idEvento, SaldoContratacion contratacion, TipoPago tipo, BigDecimal valor,
                         LocalDate fecha, String detalle, Integer idPagoProgramado) {
        this.idEvento = idEvento;
        this.contratacion = contratacion;
        this.tipo = tipo;
        this.valor = valor;
        this.fecha = fecha;
        this.detalle = detalle == null ? "" : detalle.trim();
        this.idPagoProgramado = idPagoProgramado;
    }

    public int getIdEvento() {
        return idEvento;
    }

    public SaldoContratacion getContratacion() {
        return contratacion;
    }

    public TipoPago getTipo() {
        return tipo;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public String getDetalle() {
        return detalle;
    }

    public Integer getIdPagoProgramado() {
        return idPagoProgramado;
    }

    public boolean esPagoProgramado() {
        return idPagoProgramado != null;
    }

    /** Texto que se guarda en el movimiento, por ejemplo "Abono: segunda cuota". */
    public String getDescripcion() {
        return tipo.describir(detalle);
    }

    /** Saldo que quedará en la contratación después de este pago. */
    public BigDecimal getSaldoRestante() {
        return contratacion.getSaldo().subtract(valor);
    }
}

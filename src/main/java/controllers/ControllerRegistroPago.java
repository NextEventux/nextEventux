package controllers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import entities.SesionUsuario;
import entities.finanzas.SaldoContratacion;
import entities.finanzas.SolicitudPago;
import entities.finanzas.TipoPago;
import services.ServiceRegistroPago;

/** CU-10: conecta la pantalla de registro de pagos con el servicio, usando la sesión actual. */
public class ControllerRegistroPago {

    private final ServiceRegistroPago serviceRegistroPago;
    private final SesionUsuario sesion;

    public ControllerRegistroPago(ServiceRegistroPago serviceRegistroPago, SesionUsuario sesion) {
        this.serviceRegistroPago = serviceRegistroPago;
        this.sesion = sesion;
    }

    public List<SaldoContratacion> listarContratacionesPorPagar(int idEvento) {
        return serviceRegistroPago.listarContratacionesPorPagar(idEvento, sesion);
    }

    public SolicitudPago validarPago(int idEvento, int idContratacion, TipoPago tipo, BigDecimal valor,
                                     LocalDate fecha, String detalle, boolean pagoProgramado) {
        return serviceRegistroPago.validarPago(idEvento, idContratacion, tipo, valor, fecha, detalle,
                pagoProgramado, sesion);
    }

    public SaldoContratacion confirmarPago(SolicitudPago pago) {
        return serviceRegistroPago.confirmarPago(pago, sesion);
    }
}

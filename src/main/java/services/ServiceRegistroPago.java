package services;

import java.util.List;
import entities.SesionUsuario;
import entities.eventos.Evento;
import entities.finanzas.SaldoContratacion;
import repositories.RepositoryMovimientoFinanciero;
import repositories.eventos.RepositoryEvento;

/**
 * CU-10: registrar pago de una contratación.
 *
 * - Solo el organizador activo dueño del evento puede registrar pagos, y el evento no
 *   puede estar CANCELADO.
 * - Se listan las contrataciones que cuenta CU06 (no CANCELADA) con lo pagado y el saldo.
 * - Un pago nunca supera el saldo pendiente: valor acordado − pagos REGISTRADO.
 * - No se cambia la estructura de la base: el tipo de pago va al inicio de la descripción
 *   del movimiento y un pago programado (PENDIENTE) se registra cambiando su estado.
 */
public class ServiceRegistroPago {

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

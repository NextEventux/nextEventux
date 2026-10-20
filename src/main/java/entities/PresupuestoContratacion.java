package entities;

import java.math.BigDecimal;

/**
 * CU-13: comprobación del presupuesto del evento contra el precio final acordado.
 * {@code comprometido} excluye lo que ya se había estimado para esta misma necesidad,
 * porque al contratar ese valor se sustituye por el precio acordado.
 */
public record PresupuestoContratacion(
        BigDecimal total,
        BigDecimal comprometido,
        BigDecimal valor,
        BigDecimal disponibleDespues,
        boolean suficiente,
        BigDecimal faltante,
        String mensaje) {
}

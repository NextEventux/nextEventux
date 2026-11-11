package entities.finanzas;

/**
 * CU-10: tipo de cada pago que el organizador hace a un proveedor.
 * El tipo se guarda al inicio de la descripción del movimiento
 * (por ejemplo "Abono: segunda cuota"), sin cambiar la base de datos.
 */
public enum TipoPago {
    ANTICIPO("Anticipo"),
    ABONO("Abono"),
    PAGO_FINAL("Pago final");

    private final String etiqueta;

    TipoPago(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    /** Texto que se guarda en el movimiento: "Abono" o "Abono: detalle". */
    public String describir(String detalle) {
        if (detalle == null || detalle.isBlank()) {
            return etiqueta;
        }
        return etiqueta + ": " + detalle.trim();
    }

    /** La lista desplegable del formulario muestra la etiqueta. */
    @Override
    public String toString() {
        return etiqueta;
    }
}

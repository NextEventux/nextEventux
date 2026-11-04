package views.solicitudes;

import java.math.BigDecimal;

import views.PrecioPropuestaParser;

/**
 * Datos de una propuesta ya revisados en pantalla (CU-12, E2). Revisar aquí
 * permite avisar al proveedor antes de enviar; el service vuelve a validar
 * por su cuenta, así que esta clase solo mejora el mensaje que se muestra.
 */
public record DatosPropuesta(
        BigDecimal precio,
        String descripcion,
        String condiciones,
        String materialApoyo) {

    public static final int MAX_DESCRIPCION = 255;
    public static final int MAX_CONDICIONES = 1000;

    /**
     * Valida lo escrito en el formulario. Lanza IllegalArgumentException con
     * un mensaje listo para mostrar si algo es inválido.
     */
    public static DatosPropuesta validar(
            String precio,
            String descripcion,
            String condiciones,
            String materialApoyo) {

        BigDecimal valor = leerPrecio(precio);

        String alcance = limpiar(descripcion);
        if (alcance.isEmpty()) {
            throw new IllegalArgumentException(
                    "Describe lo que incluye la propuesta.");
        }
        if (alcance.length() > MAX_DESCRIPCION) {
            throw new IllegalArgumentException(
                    "La descripción puede tener hasta "
                    + MAX_DESCRIPCION + " caracteres.");
        }

        String reglas = limpiar(condiciones);
        if (reglas.isEmpty()) {
            throw new IllegalArgumentException(
                    "Indica las condiciones de la propuesta.");
        }
        if (reglas.length() > MAX_CONDICIONES) {
            throw new IllegalArgumentException(
                    "Las condiciones pueden tener hasta "
                    + MAX_CONDICIONES + " caracteres.");
        }

        String material = limpiar(materialApoyo);
        return new DatosPropuesta(
                valor, alcance, reglas, material.isEmpty() ? null : material);
    }

    private static BigDecimal leerPrecio(String texto) {
        BigDecimal valor;

        try {
            valor = PrecioPropuestaParser.parsear(texto);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Escribe el precio solo con números, por ejemplo 1500000.");
        }

        if (valor.signum() <= 0) {
            throw new IllegalArgumentException(
                    "El precio debe ser mayor que cero.");
        }
        return valor;
    }

    private static String limpiar(String texto) {
        return texto == null ? "" : texto.trim();
    }
}

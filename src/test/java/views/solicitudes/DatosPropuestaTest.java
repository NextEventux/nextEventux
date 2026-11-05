package views.solicitudes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class DatosPropuestaTest {

    private static String mensajeDe(
            String precio, String descripcion, String condiciones) {
        return assertThrows(IllegalArgumentException.class,
                () -> DatosPropuesta.validar(
                        precio, descripcion, condiciones, null)).getMessage();
    }

    @Test
    void aceptaUnaPropuestaCompletaYLimpiaLosEspacios() {
        DatosPropuesta datos = DatosPropuesta.validar(
                " 1500000 ", " Decoración completa ", " 50% de anticipo ",
                "  https://ejemplo.com/catalogo ");

        assertEquals(new BigDecimal("1500000"), datos.precio());
        assertEquals("Decoración completa", datos.descripcion());
        assertEquals("50% de anticipo", datos.condiciones());
        assertEquals("https://ejemplo.com/catalogo", datos.materialApoyo());
    }

    @Test
    void elMaterialDeApoyoEsOpcional() {
        assertNull(DatosPropuesta.validar("100", "Alcance", "Condiciones", "  ")
                .materialApoyo());
        assertNull(DatosPropuesta.validar("100", "Alcance", "Condiciones", null)
                .materialApoyo());
    }

    @Test
    void rechazaUnPrecioQueNoEsNumero() {
        assertEquals(
                "Escribe el precio solo con números, por ejemplo 1500000.",
                mensajeDe("mil pesos", "Alcance", "Condiciones"));
        assertEquals(
                "Escribe el precio solo con números, por ejemplo 1500000.",
                mensajeDe("", "Alcance", "Condiciones"));
    }

    @Test
    void rechazaUnPrecioCeroONegativo() {
        assertEquals("El precio debe ser mayor que cero.",
                mensajeDe("0", "Alcance", "Condiciones"));
        assertEquals("El precio debe ser mayor que cero.",
                mensajeDe("-5", "Alcance", "Condiciones"));
    }

    @Test
    void exigeAlcanceYCondiciones() {
        assertEquals("Describe lo que incluye la propuesta.",
                mensajeDe("100", "   ", "Condiciones"));
        assertEquals("Indica las condiciones de la propuesta.",
                mensajeDe("100", "Alcance", ""));
    }

    @Test
    void respetaLosLimitesDeTexto() {
        assertEquals("La descripción puede tener hasta 255 caracteres.",
                mensajeDe("100", "a".repeat(256), "Condiciones"));
        assertEquals("Las condiciones pueden tener hasta 1000 caracteres.",
                mensajeDe("100", "Alcance", "a".repeat(1001)));
    }

    @Test
    void elMaterialDeApoyoDebeSerUnEnlaceHttps() {
        String mensaje = "El material de apoyo debe ser un enlace que empiece "
                + "por https://, por ejemplo https://ejemplo.com/catalogo.";

        for (String material : new String[] {
            "C:\\Users\\ana\\catalogo.pdf",
            "/Users/ana/catalogo.pdf",
            "http://ejemplo.com/catalogo",
            "https://",
            "https://ejemplo .com/catalogo"
        }) {
            assertEquals(mensaje, assertThrows(IllegalArgumentException.class,
                    () -> DatosPropuesta.validar(
                            "100", "Alcance", "Condiciones", material))
                    .getMessage());
        }
    }
}

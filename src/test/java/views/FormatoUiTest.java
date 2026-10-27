package views;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

class FormatoUiTest {

    @Test
    void monedaUsaPuntosYSinDecimales() {
        assertEquals("$1.500.000", FormatoUi.moneda(new BigDecimal("1500000")));
        assertEquals("$850", FormatoUi.moneda(new BigDecimal("850")));
        assertEquals("$0", FormatoUi.moneda(BigDecimal.ZERO));
    }

    @Test
    void monedaRedondeaLosDecimales() {
        assertEquals("$1.200.001", FormatoUi.moneda(new BigDecimal("1200000.50")));
        assertEquals("$1.200.000", FormatoUi.moneda(new BigDecimal("1200000.49")));
    }

    @Test
    void monedaNegativaLlevaSignoAntesDelSimbolo() {
        assertEquals("-$250.000", FormatoUi.moneda(new BigDecimal("-250000")));
    }

    @Test
    void valoresNulosSeMuestranConGuion() {
        assertEquals("—", FormatoUi.moneda(null));
        assertEquals("—", FormatoUi.fecha(null));
        assertEquals("—", FormatoUi.hora(null));
        assertEquals("—", FormatoUi.fechaHora(null));
    }

    @Test
    void fechaUsaMesAbreviadoEnEspanol() {
        assertEquals("20 nov. 2026", FormatoUi.fecha(LocalDate.of(2026, 11, 20)));
        assertEquals("5 ene. 2027", FormatoUi.fecha(LocalDate.of(2027, 1, 5)));
        assertEquals("31 dic. 2026", FormatoUi.fecha(LocalDate.of(2026, 12, 31)));
    }

    @Test
    void horaUsaVeinticuatroHoras() {
        assertEquals("09:05", FormatoUi.hora(LocalTime.of(9, 5)));
        assertEquals("22:00", FormatoUi.hora(LocalTime.of(22, 0)));
    }

    @Test
    void rangoDelMismoDiaMuestraLaFechaUnaVez() {
        LocalDateTime inicio = LocalDateTime.of(2026, 11, 20, 15, 0);
        LocalDateTime fin = LocalDateTime.of(2026, 11, 20, 22, 0);

        assertEquals("20 nov. 2026, 15:00 a 22:00", FormatoUi.rango(inicio, fin));
    }

    @Test
    void rangoDeVariosDiasMuestraAmbasFechas() {
        LocalDateTime inicio = LocalDateTime.of(2026, 11, 20, 15, 0);
        LocalDateTime fin = LocalDateTime.of(2026, 11, 21, 2, 0);

        assertEquals("20 nov. 2026, 15:00 a 21 nov. 2026, 02:00",
                FormatoUi.rango(inicio, fin));
    }
}

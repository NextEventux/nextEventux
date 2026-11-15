import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import entities.Campana;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CampanaTest {

    @Test
    void unaCampanaNuevaQuedaProgramadaYSinMetricas() {
        Campana campana = new Campana(1, 2, "Bodas", "Descuento", LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 31), new BigDecimal("500000"));

        assertNull(campana.getIdCampana());
        assertEquals(Campana.ESTADO_PROGRAMADA, campana.getEstado());
        assertEquals(0L, campana.getVisualizaciones());
        assertEquals(0L, campana.getConversiones());
        assertNull(campana.getNombreSegmento());
        assertEquals(1, campana.getIdUsuario());
        assertEquals(2, campana.getIdSegmento());
        assertEquals("Bodas", campana.getNombre());
        assertEquals("Descuento", campana.getTipoOferta());
        assertEquals(LocalDate.of(2026, 12, 1), campana.getFechaInicio());
        assertEquals(LocalDate.of(2026, 12, 31), campana.getFechaFin());
        assertEquals(new BigDecimal("500000"), campana.getPresupuestoMaximo());
    }

    @Test
    void unaCampanaExistenteConservaTodosSusDatos() {
        Campana campana = new Campana(7, 1, 2, "Bodas", "Descuento", LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 31), new BigDecimal("500000.00"), Campana.ESTADO_ACTIVA,
                48L, 5L, "Bogotá - Boda");

        assertEquals(7, campana.getIdCampana());
        assertEquals(Campana.ESTADO_ACTIVA, campana.getEstado());
        assertEquals(48L, campana.getVisualizaciones());
        assertEquals(5L, campana.getConversiones());
        assertEquals("Bogotá - Boda", campana.getNombreSegmento());
    }

    @Test
    void elIdentificadorSeAsignaDespuesDeGuardar() {
        Campana campana = DatosCampanaPrueba.campanaValida();
        campana.setIdCampana(3);
        assertEquals(3, campana.getIdCampana());
    }

    @Test
    void definePorLoMenosLosEstadosDelCicloDeVida() {
        assertEquals("PROGRAMADA", Campana.ESTADO_PROGRAMADA);
        assertEquals("ACTIVA", Campana.ESTADO_ACTIVA);
        assertEquals("PAUSADA", Campana.ESTADO_PAUSADA);
        assertEquals("FINALIZADA", Campana.ESTADO_FINALIZADA);
    }
}

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import entities.Campana;
import entities.Segmentacion;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repositories.RepositoryMarketing;

class RepositoryMarketingCampanaTest {

    private String url;
    private RepositoryMarketing repositorio;

    @BeforeEach
    void preparar() throws Exception {
        url = DatosCampanaPrueba.crearBase();
        repositorio = new RepositoryMarketing(url);
    }

    private Campana guardada(String presupuesto) {
        Campana campana = DatosCampanaPrueba.campana(DatosCampanaPrueba.SEGMENTO_ACTIVO, presupuesto,
                DatosCampanaPrueba.INICIO, DatosCampanaPrueba.FIN);
        assertTrue(repositorio.guardarCampanaComprometiendoPresupuesto(campana));
        return campana;
    }

    @Test
    void buscarSegmentoDevuelveElSegmentoConSusClientesOnullSiNoExiste() {
        Segmentacion segmento = repositorio.buscarSegmento(DatosCampanaPrueba.SEGMENTO_ACTIVO);

        assertEquals("Bogotá - Boda", segmento.getNombre());
        assertEquals(Segmentacion.ESTADO_ACTIVA, segmento.getEstado());
        assertEquals(List.of(2, 3, 4), segmento.getIdsClientes());
        assertEquals(3, segmento.getCantidadClientes());
        assertEquals(Segmentacion.ESTADO_INACTIVA,
                repositorio.buscarSegmento(DatosCampanaPrueba.SEGMENTO_INACTIVO).getEstado());
        assertNull(repositorio.buscarSegmento(9999));
    }

    @Test
    void elPresupuestoDisponibleSeLeeDeLaBase() {
        assertEquals(new BigDecimal(DatosCampanaPrueba.PRESUPUESTO_INICIAL),
                repositorio.obtenerPresupuestoDisponible());
    }

    @Test
    void guardarComprometeElPresupuestoYAsignaElIdentificador() {
        Campana campana = guardada("300000.00");

        assertNotNull(campana.getIdCampana());
        assertEquals(new BigDecimal("700000.00"), repositorio.obtenerPresupuestoDisponible());
    }

    @Test
    void sePuedeComprometerExactamenteTodoElDisponible() {
        guardada(DatosCampanaPrueba.PRESUPUESTO_INICIAL);
        assertEquals(0, repositorio.obtenerPresupuestoDisponible().signum());
    }

    @Test
    void conPresupuestoInsuficienteNoSeGuardaNiSeDescuenta() {
        Campana campana = DatosCampanaPrueba.campana(DatosCampanaPrueba.SEGMENTO_ACTIVO, "1000000.01",
                DatosCampanaPrueba.INICIO, DatosCampanaPrueba.FIN);

        assertFalse(repositorio.guardarCampanaComprometiendoPresupuesto(campana));
        assertNull(campana.getIdCampana());
        assertTrue(repositorio.listarCampanas().isEmpty());
        assertEquals(new BigDecimal(DatosCampanaPrueba.PRESUPUESTO_INICIAL),
                repositorio.obtenerPresupuestoDisponible());
    }

    @Test
    void laCampanaSeRecuperaConSusFechasEstadoYNombreDeSegmento() {
        Campana campana = guardada("300000.00");

        Campana leida = repositorio.buscarCampana(campana.getIdCampana());

        assertEquals("Bodas de fin de año", leida.getNombre());
        assertEquals("Descuento del 10 %", leida.getTipoOferta());
        assertEquals(DatosCampanaPrueba.ADMINISTRADOR, leida.getIdUsuario());
        assertEquals(DatosCampanaPrueba.SEGMENTO_ACTIVO, leida.getIdSegmento());
        assertEquals("Bogotá - Boda", leida.getNombreSegmento());
        assertEquals(LocalDate.of(2026, 12, 1), leida.getFechaInicio());
        assertEquals(LocalDate.of(2026, 12, 31), leida.getFechaFin());
        assertEquals(new BigDecimal("300000.00"), leida.getPresupuestoMaximo());
        assertEquals(Campana.ESTADO_PROGRAMADA, leida.getEstado());
        assertEquals(0L, leida.getVisualizaciones());
        assertEquals(0L, leida.getConversiones());
        assertNull(repositorio.buscarCampana(9999));
    }

    @Test
    void listarCampanasLasDevuelveEnOrdenDeCreacion() {
        guardada("100000.00");
        guardada("200000.00");

        List<Campana> campanas = repositorio.listarCampanas();

        assertEquals(2, campanas.size());
        assertEquals(new BigDecimal("100000.00"), campanas.get(0).getPresupuestoMaximo());
        assertEquals(new BigDecimal("200000.00"), campanas.get(1).getPresupuestoMaximo());
    }

    @Test
    void cambiarEstadoSoloSeAplicaSiElEstadoActualEsElEsperado() {
        int id = guardada("100000.00").getIdCampana();

        assertFalse(repositorio.cambiarEstadoCampana(id, Campana.ESTADO_ACTIVA, Campana.ESTADO_PAUSADA));
        assertEquals(Campana.ESTADO_PROGRAMADA, repositorio.buscarCampana(id).getEstado());

        assertTrue(repositorio.cambiarEstadoCampana(id, Campana.ESTADO_PROGRAMADA, Campana.ESTADO_ACTIVA));
        assertEquals(Campana.ESTADO_ACTIVA, repositorio.buscarCampana(id).getEstado());
    }

    @Test
    void activarRegistraUnEnvioSoloParaLosClientesQueAceptanMarketing() throws Exception {
        int id = guardada("100000.00").getIdCampana();

        assertTrue(repositorio.activarYDistribuirCampana(id));

        assertEquals(Campana.ESTADO_ACTIVA, repositorio.buscarCampana(id).getEstado());
        assertEquals(2, DatosCampanaPrueba.contarEnvios(url, id));
    }

    @Test
    void activarDosVecesNoDuplicaLosEnvios() throws Exception {
        int id = guardada("100000.00").getIdCampana();

        assertTrue(repositorio.activarYDistribuirCampana(id));
        assertFalse(repositorio.activarYDistribuirCampana(id));

        assertEquals(2, DatosCampanaPrueba.contarEnvios(url, id));
    }

    @Test
    void lasMetricasSoloSeSumanConLaCampanaActiva() {
        int id = guardada("100000.00").getIdCampana();

        assertFalse(repositorio.incrementarVisualizaciones(id));
        assertFalse(repositorio.incrementarConversiones(id));

        repositorio.activarYDistribuirCampana(id);
        assertTrue(repositorio.incrementarVisualizaciones(id));
        assertTrue(repositorio.incrementarVisualizaciones(id));
        assertTrue(repositorio.incrementarConversiones(id));

        Campana leida = repositorio.buscarCampana(id);
        assertEquals(2L, leida.getVisualizaciones());
        assertEquals(1L, leida.getConversiones());

        repositorio.cambiarEstadoCampana(id, Campana.ESTADO_ACTIVA, Campana.ESTADO_PAUSADA);
        assertFalse(repositorio.incrementarVisualizaciones(id));
    }
}

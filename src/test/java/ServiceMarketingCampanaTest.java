import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import entities.Campana;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repositories.RepositoryMarketing;
import services.ServiceMarketing;

class ServiceMarketingCampanaTest {

    private static final LocalDate INICIO = DatosCampanaPrueba.INICIO;
    private static final LocalDate FIN = DatosCampanaPrueba.FIN;

    private String url;
    private DatosCampanaPrueba.RelojManual reloj;
    private ServiceMarketing servicio;

    @BeforeEach
    void preparar() throws Exception {
        url = DatosCampanaPrueba.crearBase();
        reloj = new DatosCampanaPrueba.RelojManual(LocalDate.of(2026, 11, 14));
        servicio = new ServiceMarketing(new RepositoryMarketing(url), reloj);
    }

    @AfterEach
    void limpiar() {
        servicio.detenerPlanificador();
    }

    private int crearYObtenerId(String presupuesto) {
        return servicio.crearCampana(DatosCampanaPrueba.campana(
                DatosCampanaPrueba.SEGMENTO_ACTIVO, presupuesto, INICIO, FIN)).getIdCampana();
    }

    private int crearYActivar() {
        int id = crearYObtenerId("100000");
        servicio.lanzarCampana(id);
        return id;
    }

    // ----- Paso 2: validación y excepciones E1 y E2 -----

    @Test
    void unaConfiguracionValidaNoLanzaExcepcion() {
        servicio.validarCampana(DatosCampanaPrueba.campanaValida());
    }

    @Test
    void e2LaFechaDeFinIgualALaDeInicioBloqueaLaCreacion() {
        Campana igual = DatosCampanaPrueba.campana(1, "100000", INICIO, INICIO);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> servicio.crearCampana(igual));

        assertTrue(error.getMessage().contains("posterior a la fecha de inicio"));
        assertTrue(servicio.listarCampanas().isEmpty());
    }

    @Test
    void e2LaFechaDeFinAnteriorALaDeInicioBloqueaLaCreacion() {
        assertThrows(IllegalArgumentException.class, () -> servicio.crearCampana(
                DatosCampanaPrueba.campana(1, "100000", FIN, INICIO)));
        assertTrue(servicio.listarCampanas().isEmpty());
    }

    @Test
    void e1UnPresupuestoMayorAlDisponibleBloqueaYAvisaElMontoMaximoRestante() {
        Campana excede = DatosCampanaPrueba.campana(1, "1000000.01", INICIO, FIN);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> servicio.crearCampana(excede));

        assertTrue(error.getMessage().contains("Monto máximo restante: 1000000.00"));
        assertTrue(servicio.listarCampanas().isEmpty());
        assertEquals(new BigDecimal("1000000.00"), servicio.obtenerPresupuestoDisponible());
    }

    @Test
    void sePuedeUsarExactamenteTodoElPresupuestoDisponible() {
        crearYObtenerId("1000000");
        assertEquals(0, servicio.obtenerPresupuestoDisponible().signum());
    }

    @Test
    void variasCampanasNoPuedenSuperarElPresupuestoTotal() {
        crearYObtenerId("600000");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> crearYObtenerId("600000"));

        assertTrue(error.getMessage().contains("Monto máximo restante: 400000.00"));
    }

    @Test
    void losDatosIncompletosSeRechazan() {
        List<Campana> incompletas = List.of(
                new Campana(1, 1, " ", "Oferta", INICIO, FIN, BigDecimal.TEN),
                new Campana(1, 1, "N", "", INICIO, FIN, BigDecimal.TEN),
                new Campana(1, 0, "N", "Oferta", INICIO, FIN, BigDecimal.TEN),
                new Campana(1, 1, "N", "Oferta", null, FIN, BigDecimal.TEN),
                new Campana(1, 1, "N", "Oferta", INICIO, null, BigDecimal.TEN),
                new Campana(1, 1, "N", "Oferta", INICIO, FIN, null),
                new Campana(1, 1, "N", "Oferta", INICIO, FIN, BigDecimal.ZERO));
        for (Campana campana : incompletas) {
            assertThrows(IllegalArgumentException.class, () -> servicio.validarCampana(campana));
        }
    }

    @Test
    void soloUnAdministradorActivoPuedeCrearCampanas() {
        Campana deCliente = new Campana(DatosCampanaPrueba.CLIENTE_CON_CONSENTIMIENTO, 1, "N", "Oferta",
                INICIO, FIN, BigDecimal.TEN);
        Campana deNadie = new Campana(9999, 1, "N", "Oferta", INICIO, FIN, BigDecimal.TEN);

        assertThrows(IllegalArgumentException.class, () -> servicio.crearCampana(deCliente));
        assertThrows(IllegalArgumentException.class, () -> servicio.crearCampana(deNadie));
        assertThrows(IllegalArgumentException.class, () -> servicio.crearCampana(null));
    }

    @Test
    void elSegmentoDebeExistirYEstarActivo() {
        assertThrows(IllegalArgumentException.class, () -> servicio.crearCampana(
                DatosCampanaPrueba.campana(DatosCampanaPrueba.SEGMENTO_INACTIVO, "100", INICIO, FIN)));
        assertThrows(IllegalArgumentException.class, () -> servicio.crearCampana(
                DatosCampanaPrueba.campana(9999, "100", INICIO, FIN)));
    }

    // ----- Paso 3: programación y activación -----

    @Test
    void crearCampanaLaProgramaConSegmentoYComprometeElPresupuesto() {
        Campana creada = servicio.crearCampana(DatosCampanaPrueba.campanaValida());

        assertNotNull(creada.getIdCampana());
        Campana guardada = servicio.obtenerCampana(creada.getIdCampana());
        assertEquals(Campana.ESTADO_PROGRAMADA, guardada.getEstado());
        assertEquals("Bogotá - Boda", guardada.getNombreSegmento());
        assertEquals(new BigDecimal("300000.00"), guardada.getPresupuestoMaximo());
        assertEquals(new BigDecimal("700000.00"), servicio.obtenerPresupuestoDisponible());
    }

    @Test
    void antesDeLaFechaDeInicioLaCampanaSigueProgramada() throws Exception {
        int id = crearYObtenerId("100000");

        servicio.verificarCampanas();

        assertEquals(Campana.ESTADO_PROGRAMADA, servicio.obtenerCampana(id).getEstado());
        assertEquals(0, DatosCampanaPrueba.contarEnvios(url, id));
    }

    @Test
    void enLaFechaDeInicioSeActivaYSeDistribuyeAlSegmento() throws Exception {
        int id = crearYObtenerId("100000");
        reloj.setFecha(INICIO);

        servicio.verificarCampanas();
        servicio.verificarCampanas();

        assertEquals(Campana.ESTADO_ACTIVA, servicio.obtenerCampana(id).getEstado());
        assertEquals(2, DatosCampanaPrueba.contarEnvios(url, id));
    }

    @Test
    void lanzarUnaCampanaQueNoEstaProgramadaFalla() {
        int id = crearYActivar();
        assertThrows(IllegalArgumentException.class, () -> servicio.lanzarCampana(id));
        assertThrows(IllegalArgumentException.class, () -> servicio.lanzarCampana(9999));
    }

    // ----- Paso 4: métricas -----

    @Test
    void lasMetricasSeRegistranMientrasLaCampanaEstaVigente() {
        int idProgramada = crearYObtenerId("100000");
        assertFalse(servicio.registrarVisualizacion(idProgramada));

        int id = crearYActivar();
        assertTrue(servicio.registrarVisualizacion(id));
        assertTrue(servicio.registrarVisualizacion(id));
        assertTrue(servicio.registrarConversion(id));

        Campana actual = servicio.obtenerCampana(id);
        assertEquals(2L, actual.getVisualizaciones());
        assertEquals(1L, actual.getConversiones());
    }

    // ----- Paso 5: cierre automático y resumen -----

    @Test
    void alLlegarLaFechaDeFinSeDesactivaYEntregaElResumen() {
        int id = crearYActivar();
        servicio.registrarVisualizacion(id);
        servicio.registrarConversion(id);
        List<Campana> resumenes = new ArrayList<>();
        servicio.setAlFinalizarCampana(resumenes::add);

        reloj.setFecha(FIN.minusDays(1));
        servicio.verificarCampanas();
        assertEquals(Campana.ESTADO_ACTIVA, servicio.obtenerCampana(id).getEstado());
        assertTrue(resumenes.isEmpty());

        reloj.setFecha(FIN);
        servicio.verificarCampanas();

        assertEquals(Campana.ESTADO_FINALIZADA, servicio.obtenerCampana(id).getEstado());
        assertEquals(1, resumenes.size());
        assertEquals(1L, resumenes.get(0).getVisualizaciones());
        assertEquals(1L, resumenes.get(0).getConversiones());
        assertEquals("Bogotá - Boda", resumenes.get(0).getNombreSegmento());
        assertFalse(servicio.registrarVisualizacion(id));
    }

    @Test
    void unObservadorQueFallaNoImpideFinalizarLaCampana() {
        int id = crearYActivar();
        servicio.setAlFinalizarCampana(resumen -> {
            throw new IllegalStateException("falla al mostrar el resumen");
        });

        servicio.finalizarCampana(id);

        assertEquals(Campana.ESTADO_FINALIZADA, servicio.obtenerCampana(id).getEstado());
    }

    @Test
    void unaCampanaProgramadaNoSePuedeFinalizar() {
        int id = crearYObtenerId("100000");
        assertThrows(IllegalArgumentException.class, () -> servicio.finalizarCampana(id));
    }

    // ----- A1: pausa manual -----

    @Test
    void pausarSuspendeLaDistribucionConservaMetricasYPermiteReactivar() {
        int id = crearYActivar();
        servicio.registrarVisualizacion(id);

        servicio.pausarCampana(DatosCampanaPrueba.ADMINISTRADOR, id);
        assertEquals(Campana.ESTADO_PAUSADA, servicio.obtenerCampana(id).getEstado());
        assertFalse(servicio.registrarVisualizacion(id));
        assertEquals(1L, servicio.obtenerCampana(id).getVisualizaciones());

        servicio.reanudarCampana(DatosCampanaPrueba.ADMINISTRADOR, id);
        assertEquals(Campana.ESTADO_ACTIVA, servicio.obtenerCampana(id).getEstado());
        assertTrue(servicio.registrarVisualizacion(id));
        assertEquals(2L, servicio.obtenerCampana(id).getVisualizaciones());
    }

    @Test
    void soloSePuedePausarUnaCampanaActivaYReactivarUnaPausada() {
        int id = crearYObtenerId("100000");
        assertThrows(IllegalArgumentException.class,
                () -> servicio.pausarCampana(DatosCampanaPrueba.ADMINISTRADOR, id));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.reanudarCampana(DatosCampanaPrueba.ADMINISTRADOR, id));
    }

    @Test
    void soloUnAdministradorPuedePausarOReactivar() {
        int id = crearYActivar();
        assertThrows(IllegalArgumentException.class, () -> servicio.pausarCampana(
                DatosCampanaPrueba.CLIENTE_CON_CONSENTIMIENTO, id));
        assertThrows(IllegalArgumentException.class, () -> servicio.reanudarCampana(9999, id));
        assertEquals(Campana.ESTADO_ACTIVA, servicio.obtenerCampana(id).getEstado());
    }

    @Test
    void unaCampanaPausadaTambienSeDesactivaEnLaFechaDeFin() {
        int id = crearYActivar();
        servicio.pausarCampana(DatosCampanaPrueba.ADMINISTRADOR, id);

        reloj.setFecha(FIN);
        servicio.verificarCampanas();

        assertEquals(Campana.ESTADO_FINALIZADA, servicio.obtenerCampana(id).getEstado());
    }

    @Test
    void obtenerUnaCampanaInexistenteFalla() {
        assertThrows(IllegalArgumentException.class, () -> servicio.obtenerCampana(9999));
    }

    // ----- Planificador -----

    @Test
    void elPlanificadorSeIniciaUnaSolaVezYSeDetiene() {
        assertFalse(servicio.planificadorActivo());
        servicio.iniciarPlanificador(60);
        servicio.iniciarPlanificador(60);
        assertTrue(servicio.planificadorActivo());
        servicio.detenerPlanificador();
        assertFalse(servicio.planificadorActivo());
    }

    @Test
    void elPlanificadorActivaLasCampanasPorSiSolo() throws Exception {
        int id = crearYObtenerId("100000");
        reloj.setFecha(INICIO);

        servicio.iniciarPlanificador(1);

        long limite = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < limite
                && !Campana.ESTADO_ACTIVA.equals(servicio.obtenerCampana(id).getEstado())) {
            Thread.sleep(100);
        }
        assertEquals(Campana.ESTADO_ACTIVA, servicio.obtenerCampana(id).getEstado());
    }
}

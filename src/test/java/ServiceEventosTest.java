import controllers.ControllerEventos;
import entities.Evento;
import org.junit.jupiter.api.Test;
import repositories.RepositoryEvento;
import services.ServiceEventos;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class ServiceEventosTest {
    private final RepositoryEvento repository = new RepositoryEvento();
    private final ServiceEventos service = new ServiceEventos(repository);
    private final LocalDateTime inicio = LocalDateTime.of(2026, 10, 20, 9, 0);

    @Test
    void controladorGuardaLosDatosDelFormulario() {
        Evento evento = evento("Festival", "Corporativo", "Bogotá", new BigDecimal("15000000.25"), 100);
        new ControllerEventos(service).crearEvento(evento);
        assertEquals(1, repository.listarEventos().size());
        assertSame(evento, repository.listarEventos().get(0));
        assertEquals("PLANEADO", evento.getEstado());
        assertNotNull(evento.getFechaCreacion());
        assertNull(evento.getIdEvento());
    }

    @Test
    void permiteCeroEnPresupuestoEInvitados() {
        service.crearEvento(evento("Evento", "Social", "Bogotá", BigDecimal.ZERO, 0));
        assertEquals(1, repository.listarEventos().size());
    }

    @Test
    void rechazaCamposObligatoriosSinGuardar() {
        rechazar(null, "Completa los datos");
        rechazar(evento(" ", "Social", "Bogotá", BigDecimal.ZERO, 0), "nombre");
        rechazar(evento("Evento", null, "Bogotá", BigDecimal.ZERO, 0), "tipo");
        rechazar(evento("Evento", "Social", "", BigDecimal.ZERO, 0), "lugar");
        rechazar(evento("Evento", "Social", "Bogotá", null, 0), "presupuesto");
        rechazar(new Evento(0, "Evento", "Social", null, inicio, inicio, "Bogotá", BigDecimal.ZERO, 0), "organizador");
    }

    @Test
    void rechazaFechasAusentesOInvertidas() {
        rechazar(new Evento(1, "Evento", "Social", null, null, inicio, "Bogotá", BigDecimal.ZERO, 0), "fechas");
        rechazar(new Evento(1, "Evento", "Social", null, inicio, null, "Bogotá", BigDecimal.ZERO, 0), "fechas");
        rechazar(new Evento(1, "Evento", "Social", null, inicio, inicio.minusHours(1), "Bogotá", BigDecimal.ZERO, 0), "anterior");
    }

    @Test
    void rechazaNumerosNegativosFueraDeRangoOConDemasiadosDecimales() {
        rechazar(evento("Evento", "Social", "Bogotá", new BigDecimal("-1"), 0), "presupuesto");
        rechazar(evento("Evento", "Social", "Bogotá", new BigDecimal("10000000000"), 0), "superar");
        rechazar(evento("Evento", "Social", "Bogotá", new BigDecimal("0.001"), 0), "decimales");
        rechazar(evento("Evento", "Social", "Bogotá", BigDecimal.ZERO, -1), "invitados");
    }

    @Test
    void validaLongitudesDelEsquema() {
        rechazar(evento("x".repeat(151), "Social", "Bogotá", BigDecimal.ZERO, 0), "nombre");
        rechazar(evento("Evento", "x".repeat(101), "Bogotá", BigDecimal.ZERO, 0), "tipo");
        rechazar(evento("Evento", "Social", "x".repeat(256), BigDecimal.ZERO, 0), "lugar");
        rechazar(new Evento(1, "Evento", "Social", "x".repeat(256), inicio, inicio, "Bogotá", BigDecimal.ZERO, 0), "descripción");
    }

    @Test
    void aceptaLimitesDelEsquemaSinRedondeo() {
        service.crearEvento(new Evento(1, "x".repeat(150), "x".repeat(100), "x".repeat(255),
                inicio, inicio, "x".repeat(255), new BigDecimal("9999999999.99"), Integer.MAX_VALUE));
        assertEquals(new BigDecimal("9999999999.99"), repository.listarEventos().get(0).getPresupuesto());
    }

    private Evento evento(String nombre, String tipo, String lugar, BigDecimal presupuesto, int invitados) {
        return new Evento(1, nombre, tipo, null, inicio, inicio.plusHours(2), lugar, presupuesto, invitados);
    }

    private void rechazar(Evento evento, String motivo) {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> service.crearEvento(evento));
        assertTrue(error.getMessage().contains(motivo), error.getMessage());
        assertTrue(repository.listarEventos().isEmpty());
    }
}

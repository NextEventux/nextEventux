import config.DatabaseConnection;
import entities.Evento;
import repositories.RepositoryEvento;
import services.ServiceEventos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.sql.*;
import static org.junit.jupiter.api.Assertions.*;

class RepositoryEventoTest {
    @TempDir Path directorio;

    @Test
    void recuperaTodosLosDatosDespuesDeCerrarYReabrirLaBase() throws Exception {
        String url = "jdbc:h2:" + directorio.resolve("eventos");
        BaseDatosPrueba.preparar(url);
        Evento evento = evento(1);
        new ServiceEventos(new RepositoryEvento(url)).crearEvento(evento);
        Evento recuperado = new RepositoryEvento(url).listarEventos().get(0);
        assertEquals(evento.getIdEvento(), recuperado.getIdEvento());
        assertEquals(evento.getIdOrganizador(), recuperado.getIdOrganizador());
        assertEquals(evento.getNombre(), recuperado.getNombre());
        assertEquals(evento.getTipo(), recuperado.getTipo());
        assertEquals(evento.getDescripcion(), recuperado.getDescripcion());
        assertEquals(evento.getFechaInicio(), recuperado.getFechaInicio());
        assertEquals(evento.getFechaFin(), recuperado.getFechaFin());
        assertEquals(evento.getUbicacion(), recuperado.getUbicacion());
        assertEquals(0, evento.getPresupuesto().compareTo(recuperado.getPresupuesto()));
        assertEquals(evento.getInvitadosIniciales(), recuperado.getInvitadosIniciales());
        assertEquals(evento.getEstado(), recuperado.getEstado());
        // H2 TIMESTAMP usa precisión de microsegundos, con redondeo.
        assertTrue(Math.abs(ChronoUnit.NANOS.between(evento.getFechaCreacion(), recuperado.getFechaCreacion())) <= 1000);
    }

    @Test
    void rechazaOrganizadorInexistenteSinGuardar() throws Exception {
        String url = "jdbc:h2:" + directorio.resolve("eventos");
        BaseDatosPrueba.preparar(url);
        RepositoryEvento repository = new RepositoryEvento(url);
        Evento evento = evento(999);
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> new ServiceEventos(repository).crearEvento(evento));
        assertTrue(error.getMessage().contains("organizador no existe"));
        assertNull(evento.getIdEvento());
        assertTrue(repository.listarEventos().isEmpty());
    }

    @Test
    void conservaCamposNulosYEstadoDeEventosHistoricos() throws Exception {
        String url = "jdbc:h2:" + directorio.resolve("eventos");
        BaseDatosPrueba.preparar(url);
        try (Connection connection = DatabaseConnection.getConnection(url);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO Evento (id_organizador,nombre,fecha_inicio,fecha_fin,estado,fecha_creacion) "
                    + "VALUES (1,'Histórico',TIMESTAMP '2026-10-20 09:00:00',TIMESTAMP '2026-10-20 18:00:00',"
                    + "'FINALIZADO',TIMESTAMP '2026-10-01 10:00:00')");
        }
        Evento evento = new RepositoryEvento(url).listarEventos().get(0);
        assertNull(evento.getTipo());
        assertNull(evento.getPresupuesto());
        assertNull(evento.getInvitadosIniciales());
        assertEquals("FINALIZADO", evento.getEstado());
        assertEquals(LocalDateTime.of(2026,10,1,10,0), evento.getFechaCreacion());
    }

    @Test
    void cargaEjemplosSinSobrescribirUnaBaseExistente() throws Exception {
        String url = "jdbc:h2:" + directorio.resolve("ejemplos");
        try (Connection connection = DatabaseConnection.getConnection(url)) {
            config.CargarDatosEjemplo.cargar(connection);
            assertThrows(SQLException.class, () -> config.CargarDatosEjemplo.cargar(connection));
        }
        RepositoryEvento repository = new RepositoryEvento(url);
        assertEquals(2, repository.listarEventos().size());
        new ServiceEventos(repository).crearEvento(evento(1));
        assertEquals(3, repository.listarEventos().size());
    }

    @Test
    void comunicaFalloDeConexionSinAsignarIdentificador() {
        Evento evento = evento(1);
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new RepositoryEvento("jdbc:desconocido:prueba").guardarEvento(evento));
        assertTrue(error.getMessage().contains("No se pudo guardar"));
        assertNotNull(error.getCause());
        assertNull(evento.getIdEvento());
    }

    private Evento evento(int organizador) {
        return new Evento(organizador, "Festival", "Corporativo", "Descripción",
                LocalDateTime.of(2026,10,20,9,0), LocalDateTime.of(2026,10,20,18,0),
                "Bogotá", new BigDecimal("15000000.25"), 100);
    }
}

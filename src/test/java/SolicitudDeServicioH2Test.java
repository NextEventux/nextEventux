import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import persistencia.DatabaseConnection;
import entities.EventoSolicitable;
import entities.NecesidadAbierta;
import services.ServiceSolicitudes;

/** CU-12: el organizador elige su evento y su necesidad por nombre. */
class SolicitudDeServicioH2Test {

    private static final String URL
            = "jdbc:h2:mem:cu12solicitar;DB_CLOSE_DELAY=-1";

    private ServiceSolicitudes organizador;

    @BeforeEach
    void preparar() throws Exception {
        DatosCU12Prueba.preparar(URL);
        organizador = DatosCU12Prueba.servicioOrganizador();
    }

    @AfterEach
    void limpiar() throws Exception {
        DatosCU12Prueba.limpiar(URL);
    }

    private void ejecutar(String sql) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection();
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    @Test
    void listaLosEventosDelOrganizadorConSuNombre() {
        List<EventoSolicitable> eventos = organizador
                .listarEventosSolicitables(DatosCU12Prueba.ORGANIZADOR);

        assertEquals(1, eventos.size());
        assertEquals("Boda de Valentina", eventos.get(0).nombre());
        assertEquals(DatosCU12Prueba.SOLICITUD, eventos.get(0).idEvento());
    }

    @Test
    void noOfreceEventosCanceladosNiFinalizados() throws Exception {
        ejecutar("UPDATE Evento SET estado = 'CANCELADO' WHERE id_evento = 9301");

        assertTrue(organizador
                .listarEventosSolicitables(DatosCU12Prueba.ORGANIZADOR)
                .isEmpty());
    }

    @Test
    void listaLasNecesidadesAbiertasConSuDescripcion() {
        List<NecesidadAbierta> necesidades = organizador
                .listarNecesidadesOrganizador(DatosCU12Prueba.ORGANIZADOR);

        assertEquals(1, necesidades.size());
        assertEquals("Decoración del salón", necesidades.get(0).descripcion());
        assertEquals(9301, necesidades.get(0).idEvento());
    }

    @Test
    void unaNecesidadConPropuestaSeleccionadaYaNoSeOfrece() throws Exception {
        ejecutar("""
                INSERT INTO Propuesta
                (id_propuesta, id_solicitud, numero_version, valor, descripcion,
                 condiciones, estado, fecha_envio, vigente)
                VALUES (9301, 9301, 1, 1000, 'x', 'y', 'ENVIADA',
                        CURRENT_TIMESTAMP, TRUE)
                """);
        ejecutar("""
                UPDATE Necesidad_Servicio_Evento
                SET id_propuesta_seleccionada = 9301 WHERE id_necesidad_servicio = 9301
                """);

        assertTrue(organizador
                .listarNecesidadesOrganizador(DatosCU12Prueba.ORGANIZADOR)
                .isEmpty());
    }

    @Test
    void unOrganizadorNoConsultaLosEventosDeOtro() {
        assertThrows(RuntimeException.class,
                () -> organizador.listarEventosSolicitables(9999));
    }
}

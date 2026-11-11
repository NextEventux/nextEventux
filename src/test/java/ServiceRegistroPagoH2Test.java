import entities.SesionUsuario;
import entities.finanzas.SaldoContratacion;
import inicializacion.CargarDatosEjemplo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import persistencia.DatabaseConnection;
import repositories.RepositoryMovimientoFinanciero;
import repositories.eventos.RepositoryEvento;
import services.ServiceRegistroPago;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CU-10: registrar pago de una contratación.
 * Usa una base H2 en memoria con seed.sql. En los datos de ejemplo, el evento 1
 * ("Boda Laura y Daniel") es de laura.gomez y tiene dos contrataciones ACTIVA:
 * 1 decoración (1.800.000, anticipo de 900.000 REGISTRADO y pago final #2 de 900.000 PENDIENTE) y
 * 2 catering (2.200.000, anticipo de 1.100.000 REGISTRADO y pago final #4 de 1.100.000 PENDIENTE).
 */
public class ServiceRegistroPagoH2Test {

    private ServiceRegistroPago service;

    @BeforeEach
    void prepararBaseDatos() throws Exception {
        System.setProperty("nexteventux.db.url", "jdbc:h2:mem:cu10_test;DB_CLOSE_DELAY=-1");
        try (Connection connection = DatabaseConnection.getConnection()) {
            CargarDatosEjemplo.cargar(connection);
        }
        service = new ServiceRegistroPago(new RepositoryMovimientoFinanciero(), new RepositoryEvento());
    }

    @AfterEach
    void limpiar() throws Exception {
        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DROP ALL OBJECTS");
        }
        System.clearProperty("nexteventux.db.url");
    }

    // ---------- Pasos 3 y 4: listar contrataciones por pagar ----------

    @Test
    void listaContratacionesConPagadoSaldoYPagoProgramado() {
        List<SaldoContratacion> saldos = service.listarContratacionesPorPagar(1, sesionLaura());

        assertEquals(2, saldos.size());
        SaldoContratacion decoracion = saldos.get(0);
        assertEquals(1, decoracion.getIdContratacion());
        assertEquals("Decoración floral", decoracion.getServicio());
        assertNotNull(decoracion.getProveedor());
        assertDinero("1800000", decoracion.getValorAcordado());
        assertDinero("900000", decoracion.getPagado());
        assertDinero("900000", decoracion.getSaldo());
        assertEquals(2, decoracion.getIdPagoProgramado());
        assertDinero("900000", decoracion.getValorPagoProgramado());
        assertTrue(decoracion.admitePagos());
        assertNull(decoracion.motivoSinPagos());
    }

    @Test
    void contratacionCanceladaNoSeLista() throws Exception {
        ejecutar("UPDATE Contratacion SET estado = 'CANCELADA' WHERE id_contratacion = 2");

        List<SaldoContratacion> saldos = service.listarContratacionesPorPagar(1, sesionLaura());

        assertEquals(1, saldos.size());
        assertEquals(1, saldos.get(0).getIdContratacion());
    }

    @Test
    void contratacionPagadaNoAdmitePagos() throws Exception {
        ejecutar("UPDATE Movimiento_financiero SET estado = 'REGISTRADO' WHERE id_movimiento = 2");

        SaldoContratacion decoracion = service.listarContratacionesPorPagar(1, sesionLaura()).get(0);

        assertDinero("0", decoracion.getSaldo());
        assertFalse(decoracion.admitePagos());
        assertEquals("Pagada", decoracion.motivoSinPagos());
        assertNull(decoracion.getIdPagoProgramado());
    }

    @Test
    void contratacionSinValorNoAdmitePagos() throws Exception {
        ejecutar("UPDATE Contratacion SET valor_acordado = NULL WHERE id_contratacion = 1");
        ejecutar("UPDATE Propuesta SET valor = NULL WHERE id_propuesta = 1");

        SaldoContratacion decoracion = service.listarContratacionesPorPagar(1, sesionLaura()).get(0);

        assertNull(decoracion.getSaldo());
        assertFalse(decoracion.admitePagos());
        assertEquals("Sin valor acordado", decoracion.motivoSinPagos());
    }

    @Test
    void eventoCanceladoNoPermitePagos() throws Exception {
        ejecutar("UPDATE Evento SET estado = 'CANCELADO' WHERE id_evento = 1");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.listarContratacionesPorPagar(1, sesionLaura()));
        assertEquals("No se pueden registrar pagos de un evento cancelado.", error.getMessage());
    }

    @Test
    void soloElOrganizadorDelEventoPuedeVerSusContrataciones() {
        // El evento 2 pertenece al organizador 2 (carlos.rodriguez).
        assertThrows(IllegalArgumentException.class,
                () -> service.listarContratacionesPorPagar(2, sesionLaura()));
        assertThrows(IllegalArgumentException.class,
                () -> service.listarContratacionesPorPagar(0, sesionLaura()));
    }

    @Test
    void requiereSesionDeOrganizadorActivo() {
        assertThrows(IllegalStateException.class,
                () -> service.listarContratacionesPorPagar(1, null));
        assertThrows(IllegalStateException.class,
                () -> service.listarContratacionesPorPagar(1, sesion(3, 1, "PROVEEDOR")));
    }

    // ---------- Utilidades ----------

    private static SesionUsuario sesionLaura() {
        return sesion(1, 1, "ORGANIZADOR");
    }

    private static SesionUsuario sesion(int usuario, int actor, String tipo) {
        SesionUsuario sesion = new SesionUsuario();
        sesion.setIdUsuario(usuario);
        sesion.setIdActor(actor);
        sesion.setTipoUsuario(tipo);
        sesion.setEstadoUsuario("ACTIVO");
        sesion.setEstadoActor("ACTIVO");
        return sesion;
    }

    /** Compara importes sin depender de la escala (900000 = 900000.00). */
    private static void assertDinero(String esperado, BigDecimal actual) {
        assertNotNull(actual);
        assertEquals(0, new BigDecimal(esperado).compareTo(actual),
                "Se esperaba " + esperado + " y se obtuvo " + actual);
    }

    private static void ejecutar(String sql) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }
}

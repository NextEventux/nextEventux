package services.finanzas;

import entities.SesionUsuario;
import entities.finanzas.SaldoContratacion;
import entities.finanzas.SolicitudPago;
import entities.finanzas.TipoPago;
import inicializacion.CargarDatosEjemplo;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import persistencia.DatabaseConnection;
import persistencia.PropiedadSistemaTemporal;
import repositories.RepositoryMovimientoFinanciero;
import repositories.eventos.RepositoryEvento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// evitamos que otras pruebas cambien al mismo tiempo la propiedad global de la base
@ResourceLock(Resources.SYSTEM_PROPERTIES)
class ServiceRegistroPagoH2Test {

    private static final int EVENTO = 1;
    private static final int CONTRATACION = 1;
    // los pagos de prueba deben ocurrir después de la fecha de contratación
    private static final LocalDate FECHA_CONTRATACION = LocalDate.now().minusDays(10);

    private String url;
    private ServiceRegistroPago service;
    private PropiedadSistemaTemporal propiedadUrlBaseDatos;

    @BeforeEach
    void preparar() throws Exception {
        // la base vive entre conexiones del servicio y tiene un nombre único en memoria
        url = "jdbc:h2:mem:registro_pago_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        // el repositorio de movimientos lee esta propiedad global
        propiedadUrlBaseDatos = new PropiedadSistemaTemporal("nexteventux.db.url", url);

        // cargamos el seed solo en esta h2 temporal vacía
        try (Connection connection = DatabaseConnection.getConnection(url)) {
            CargarDatosEjemplo.cargar(connection);
        }
        ejecutar("UPDATE Contratacion SET fecha_contratacion = TIMESTAMP '"
                + FECHA_CONTRATACION + " 12:00:00' WHERE id_contratacion IN (1, 2)");

        service = new ServiceRegistroPago(
                new RepositoryMovimientoFinanciero(),
                new RepositoryEvento(url));
    }

    @AfterEach
    void limpiar() throws Exception {
        try {
            if (url != null) {
                // usamos jdbc directo para apagar esta base temporal sin volver a inicializarla
                try (Connection connection = DriverManager.getConnection(url, "sa", "");
                        Statement statement = connection.createStatement()) {
                    statement.execute("SHUTDOWN");
                }
            }
        } finally {
            if (propiedadUrlBaseDatos != null) {
                // devolvemos la propiedad a su valor anterior
                propiedadUrlBaseDatos.restaurar();
            }
        }
    }

    @Test
    void listaContratacionesConPagosYSaldoPendiente() {
        List<SaldoContratacion> saldos = service.listarContratacionesPorPagar(
                EVENTO, sesionOrganizador());

        assertEquals(2, saldos.size());
        SaldoContratacion saldo = saldos.get(0);
        assertEquals(CONTRATACION, saldo.getIdContratacion());
        assertEquals(0, new BigDecimal("1800000.00").compareTo(saldo.getValorAcordado()));
        assertEquals(0, new BigDecimal("900000.00").compareTo(saldo.getPagado()));
        assertEquals(0, new BigDecimal("900000.00").compareTo(saldo.getSaldo()));
        assertEquals(0, new BigDecimal("900000.00").compareTo(saldo.getTotalProgramado()));
        assertTrue(saldo.tienePagoProgramado());
    }

    @Test
    void envuelveErroresAlConsultarLasContrataciones() throws Exception {
        ejecutar("ALTER TABLE Movimiento_financiero RENAME COLUMN valor TO valor_prueba");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.listarContratacionesPorPagar(EVENTO, sesionOrganizador()));

        assertEquals("No se pudieron consultar las contrataciones. Vuelve a intentarlo.", error.getMessage());
        assertTrue(error.getCause() instanceof RuntimeException);
    }

    @Test
    void listaContratacionesRequiereOrganizadorActivoYEventoVigente() throws Exception {
        assertThrows(IllegalStateException.class,
                () -> service.listarContratacionesPorPagar(EVENTO, null));
        assertThrows(IllegalStateException.class,
                () -> service.listarContratacionesPorPagar(EVENTO, sesion(1, 1, "PROVEEDOR")));
        assertThrows(IllegalStateException.class,
                () -> service.listarContratacionesPorPagar(EVENTO, sesion(999, 999, "ORGANIZADOR")));
        assertThrows(IllegalArgumentException.class,
                () -> service.listarContratacionesPorPagar(0, sesionOrganizador()));
        assertThrows(IllegalArgumentException.class,
                () -> service.listarContratacionesPorPagar(999, sesionOrganizador()));

        ejecutar("UPDATE Evento SET estado = 'CANCELADO' WHERE id_evento = " + EVENTO);
        assertThrows(IllegalStateException.class,
                () -> service.listarContratacionesPorPagar(EVENTO, sesionOrganizador()));
    }

    @Test
    void validaConLaFechaActualYRecortaElDetalle() throws Exception {
        ejecutar("DELETE FROM Movimiento_financiero WHERE id_movimiento = 2");

        SolicitudPago pago = service.validarPago(
                EVENTO, CONTRATACION, TipoPago.ABONO, new BigDecimal("400000"),
                LocalDate.now(), "  Segunda cuota  ", false, sesionOrganizador());

        assertEquals("Segunda cuota", pago.getDetalle());
        assertEquals("Abono: Segunda cuota", pago.getDescripcion());
        assertEquals(0, new BigDecimal("500000.00").compareTo(pago.getSaldoRestante()));
        assertFalse(pago.esPagoProgramado());
        assertNull(pago.getIdPagoProgramado());
    }

    @Test
    void confirmaUnPagoNuevoYCompruebaLaPersistencia() throws Exception {
        ejecutar("DELETE FROM Movimiento_financiero WHERE id_movimiento = 2");
        SolicitudPago pago = validar(
                CONTRATACION, TipoPago.ABONO, new BigDecimal("400000"),
                LocalDate.now().minusDays(1), "Segunda cuota", false);

        SaldoContratacion saldo = service.confirmarPago(pago, sesionOrganizador());

        assertEquals(0, new BigDecimal("1300000.00").compareTo(saldo.getPagado()));
        assertEquals(0, new BigDecimal("500000.00").compareTo(saldo.getSaldo()));
        assertEquals("1", valor("SELECT COUNT(*) FROM Movimiento_financiero "
                + "WHERE id_contratacion = 1 AND estado = 'REGISTRADO' "
                + "AND valor = 400000.00 AND descripcion = 'Abono: Segunda cuota'"));
        assertEquals(4, Integer.parseInt(valor("SELECT COUNT(*) FROM Movimiento_financiero")));
    }

    @Test
    void confirmaUnPagoProgramadoActualizandoElMovimientoExistente() throws Exception {
        SolicitudPago pago = validar(
                CONTRATACION, TipoPago.PAGO_FINAL, new BigDecimal("900000"),
                LocalDate.now(), null, true);

        SaldoContratacion saldo = service.confirmarPago(pago, sesionOrganizador());

        assertEquals(0, saldo.getSaldo().signum());
        assertEquals(0, saldo.getTotalProgramado().signum());
        assertEquals("REGISTRADO", valor(
                "SELECT estado FROM Movimiento_financiero WHERE id_movimiento = 2"));
        assertEquals("Pago final", valor(
                "SELECT descripcion FROM Movimiento_financiero WHERE id_movimiento = 2"));
        assertEquals(4, Integer.parseInt(valor("SELECT COUNT(*) FROM Movimiento_financiero")));
    }

    @Test
    void noConfirmaDosVecesUnResumenConSaldoDesactualizado() throws Exception {
        ejecutar("DELETE FROM Movimiento_financiero WHERE id_movimiento = 2");
        SolicitudPago pago = validar(
                CONTRATACION, TipoPago.ABONO, new BigDecimal("300000"),
                LocalDate.now(), "Abono", false);

        service.confirmarPago(pago, sesionOrganizador());

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.confirmarPago(pago, sesionOrganizador()));
        assertTrue(error.getMessage().contains("El saldo de la contratación cambió"));
    }

    @Test
    void noConfirmaUnaSolicitudNulaONoValida() {
        assertThrows(IllegalArgumentException.class,
                () -> service.confirmarPago(null, sesionOrganizador()));

        SolicitudPago sinContratacion = new SolicitudPago(
                EVENTO, null, TipoPago.ABONO, new BigDecimal("1000"),
                LocalDate.now(), "Abono", null);
        assertThrows(IllegalArgumentException.class,
                () -> service.confirmarPago(sinContratacion, sesionOrganizador()));
    }

    @Test
    void noConfirmaUnResumenSinSaldoMostrado() throws Exception {
        liberarSaldoReservado();
        SaldoContratacion sinSaldoMostrado = new SaldoContratacion(
                CONTRATACION, 1, "Servicio", "Proveedor", null, null,
                BigDecimal.ZERO, null, null, BigDecimal.ZERO);
        SolicitudPago pago = new SolicitudPago(
                EVENTO, sinSaldoMostrado, TipoPago.ABONO, new BigDecimal("1000"),
                LocalDate.now(), null, null);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.confirmarPago(pago, sesionOrganizador()));

        assertTrue(error.getMessage().contains("El saldo de la contratación cambió"));
        assertEquals("1", valor("SELECT COUNT(*) FROM Movimiento_financiero "
                + "WHERE id_contratacion = 1 AND estado = 'REGISTRADO'"));
    }

    @Test
    void rechazaTipoYValoresDePagoInvalidos() {
        assertThrows(IllegalArgumentException.class,
                () -> validar(CONTRATACION, null, new BigDecimal("1000"),
                        LocalDate.now(), null, false));
        assertThrows(IllegalArgumentException.class,
                () -> validar(CONTRATACION, TipoPago.ABONO, null,
                        LocalDate.now(), null, false));
        assertThrows(IllegalArgumentException.class,
                () -> validar(CONTRATACION, TipoPago.ABONO, BigDecimal.ZERO,
                        LocalDate.now(), null, false));
        assertThrows(IllegalArgumentException.class,
                () -> validar(CONTRATACION, TipoPago.ABONO, new BigDecimal("-1"),
                        LocalDate.now(), null, false));
        assertThrows(IllegalArgumentException.class,
                () -> validar(CONTRATACION, TipoPago.ABONO, new BigDecimal("1.001"),
                        LocalDate.now(), null, false));
        assertThrows(IllegalArgumentException.class,
                () -> validar(CONTRATACION, TipoPago.ABONO, new BigDecimal("900000.01"),
                        LocalDate.now(), null, false));
    }

    @Test
    void noPermiteUnAnticipoCuandoYaHayPagosRegistrados() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> validar(CONTRATACION, TipoPago.ANTICIPO, new BigDecimal("1000"),
                        LocalDate.now(), null, false));

        assertTrue(error.getMessage().contains("Ya hay pagos registrados"));
    }

    @Test
    void noPermiteUsarElSaldoQueEstaReservadoParaUnPagoProgramado() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> validar(CONTRATACION, TipoPago.ABONO, new BigDecimal("1"),
                        LocalDate.now(), null, false));

        assertTrue(error.getMessage().contains("supera el saldo libre"));
    }

    @Test
    void requiereRegistrarElPagoProgramadoAntesDeUnPagoFinalNuevo() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> validar(CONTRATACION, TipoPago.PAGO_FINAL, new BigDecimal("900000"),
                        LocalDate.now(), null, false));

        assertTrue(error.getMessage().contains("Hay $ 900.000 en pagos programados"));
    }

    @Test
    void exigeQueElPagoFinalSeaIgualAlSaldoPendiente() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> validar(CONTRATACION, TipoPago.PAGO_FINAL, new BigDecimal("400000"),
                        LocalDate.now(), null, true));

        assertTrue(error.getMessage().contains("El pago final debe ser igual al saldo pendiente"));
    }

    @Test
    void pagoFinalDebeSerIgualAlSaldoPendiente() throws Exception {
        ejecutar("DELETE FROM Movimiento_financiero WHERE id_movimiento = 2");
        SolicitudPago pago = validar(
                CONTRATACION, TipoPago.PAGO_FINAL, new BigDecimal("900000"),
                LocalDate.now(), "Saldo final", false);

        SaldoContratacion saldo = service.confirmarPago(pago, sesionOrganizador());

        assertEquals(0, saldo.getSaldo().signum());
        assertEquals("2", valor("SELECT COUNT(*) FROM Movimiento_financiero "
                + "WHERE id_contratacion = 1 AND estado = 'REGISTRADO'"));
    }

    @Test
    void pagoProgramadoDebeCoincidirConElValorPendiente() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> validar(CONTRATACION, TipoPago.ABONO, new BigDecimal("800000"),
                        LocalDate.now(), null, true));
        assertTrue(error.getMessage().contains("El pago programado es de"));
    }

    @Test
    void rechazaSolicitarUnPagoProgramadoSiNoExisteNinguno() throws Exception {
        ejecutar("DELETE FROM Movimiento_financiero WHERE id_movimiento = 2");
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> validar(CONTRATACION, TipoPago.ABONO, new BigDecimal("1000"),
                        LocalDate.now(), null, true));
        assertEquals("Esta contratación no tiene pagos programados.", error.getMessage());
    }

    @Test
    void rechazaContratacionSinValorAcordado() throws Exception {
        ejecutar("UPDATE Contratacion SET valor_acordado = NULL WHERE id_contratacion = 1");
        ejecutar("UPDATE Propuesta SET valor = NULL WHERE id_propuesta = 1");
        IllegalArgumentException sinValor = assertThrows(IllegalArgumentException.class,
                () -> validar(CONTRATACION, TipoPago.ABONO, new BigDecimal("1000"),
                        LocalDate.now(), null, false));
        assertTrue(sinValor.getMessage().contains("no tiene valor acordado"));
    }

    @Test
    void rechazaContratacionQueYaNoTieneSaldoPendiente() throws Exception {
        ejecutar("UPDATE Contratacion SET valor_acordado = 900000.00 WHERE id_contratacion = 1");
        IllegalArgumentException pagada = assertThrows(IllegalArgumentException.class,
                () -> validar(CONTRATACION, TipoPago.ABONO, new BigDecimal("1000"),
                        LocalDate.now(), null, false));
        assertTrue(pagada.getMessage().contains("ya está pagada"));
    }

    @Test
    void rechazaDescripcionSuperiorAlLimite() throws Exception {
        liberarSaldoReservado();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> validar(CONTRATACION, TipoPago.ABONO, new BigDecimal("1000"),
                        LocalDate.now(), "x".repeat(ServiceRegistroPago.MAXIMO_DETALLE + 1), false));

        assertEquals("La descripción puede tener máximo 200 caracteres.", error.getMessage());
    }

    @Test
    void rechazaFechaNula() throws Exception {
        liberarSaldoReservado();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> validar(CONTRATACION, TipoPago.ABONO, new BigDecimal("1000"),
                        null, null, false));

        assertEquals("Ingresa la fecha del pago.", error.getMessage());
    }

    @Test
    void rechazaFechaFutura() throws Exception {
        liberarSaldoReservado();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> validar(CONTRATACION, TipoPago.ABONO, new BigDecimal("1000"),
                        LocalDate.now().plusDays(1), null, false));

        assertEquals("La fecha del pago no puede ser futura.", error.getMessage());
    }

    @Test
    void rechazaFechaAnteriorALaContratacion() throws Exception {
        liberarSaldoReservado();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> validar(CONTRATACION, TipoPago.ABONO, new BigDecimal("1000"),
                        FECHA_CONTRATACION.minusDays(1), null, false));

        assertTrue(error.getMessage().contains("La fecha del pago no puede ser anterior a la contratación"));
    }

    @Test
    void validaUnaSegundaContratacionDelEvento() {
        SolicitudPago pago = validar(2, TipoPago.PAGO_FINAL, new BigDecimal("1100000"),
                LocalDate.now(), null, true);

        assertEquals(2, pago.getContratacion().getIdContratacion());
        assertEquals(0, new BigDecimal("1100000").compareTo(pago.getValor()));
        assertTrue(pago.esPagoProgramado());
    }

    @Test
    void rechazaUnaContratacionInexistente() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> validar(999, TipoPago.ABONO, new BigDecimal("1000"),
                        LocalDate.now(), null, false));

        assertEquals("Selecciona una contratación vigente de este evento.", error.getMessage());
    }

    @Test
    void permiteUnAnticipoCuandoAunNoHayPagosRegistrados() throws Exception {
        ejecutar("DELETE FROM Movimiento_financiero WHERE id_contratacion = " + CONTRATACION);

        SolicitudPago pago = validar(CONTRATACION, TipoPago.ANTICIPO, new BigDecimal("300000"),
                LocalDate.now(), null, false);

        assertEquals(TipoPago.ANTICIPO, pago.getTipo());
        assertEquals(0, new BigDecimal("1500000").compareTo(pago.getSaldoRestante()));
    }

    @Test
    void confirmaUnPagoQueCabeEnElSaldoNoReservado() throws Exception {
        ejecutar("UPDATE Movimiento_financiero SET valor = 400000.00 WHERE id_movimiento = 2");
        SolicitudPago pago = validar(CONTRATACION, TipoPago.ABONO, new BigDecimal("400000"),
                LocalDate.now(), "Abono dentro del saldo libre", false);

        SaldoContratacion saldo = service.confirmarPago(pago, sesionOrganizador());

        assertEquals(0, new BigDecimal("500000").compareTo(saldo.getSaldo()));
        assertEquals(0, new BigDecimal("400000").compareTo(saldo.getTotalProgramado()));
        assertEquals("2", valor("SELECT COUNT(*) FROM Movimiento_financiero "
                + "WHERE id_contratacion = 1 AND estado = 'REGISTRADO'"));
    }

    @Test
    void conservaElSaldoYEnvuelveUnErrorDePersistencia() throws Exception {
        liberarSaldoReservado();
        ejecutar("ALTER TABLE Movimiento_financiero ADD CONSTRAINT CK_PAGO_PRUEBA "
                + "CHECK (valor <> 12345.00)");
        SolicitudPago pago = validar(CONTRATACION, TipoPago.ABONO, new BigDecimal("12345"),
                LocalDate.now(), "Pago rechazado por la base", false);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.confirmarPago(pago, sesionOrganizador()));

        assertEquals("No se pudo registrar el pago. Vuelve a las contrataciones "
                + "y revisa el saldo antes de intentarlo de nuevo.", error.getMessage());
        assertTrue(error.getCause() instanceof RuntimeException);
        assertEquals("1", valor("SELECT COUNT(*) FROM Movimiento_financiero "
                + "WHERE id_contratacion = 1 AND estado = 'REGISTRADO'"));
    }

    @Test
    void rechazaConfirmacionSiCambioElImporteProgramado() throws Exception {
        ejecutar("UPDATE Movimiento_financiero SET valor = 400000.00 WHERE id_movimiento = 2");
        SolicitudPago pago = validar(CONTRATACION, TipoPago.ABONO, new BigDecimal("100000"),
                LocalDate.now(), "Abono", false);
        ejecutar("UPDATE Movimiento_financiero SET valor = 300000.00 WHERE id_movimiento = 2");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.confirmarPago(pago, sesionOrganizador()));

        assertTrue(error.getMessage().contains("El saldo de la contratación cambió"));
        assertEquals("300000.00", valor(
                "SELECT valor FROM Movimiento_financiero WHERE id_movimiento = 2"));
        assertEquals("1", valor("SELECT COUNT(*) FROM Movimiento_financiero "
                + "WHERE id_contratacion = 1 AND estado = 'REGISTRADO'"));
    }

    @Test
    void muestraLosCentavosEnElMensajeDeSaldoPendiente() throws Exception {
        ejecutar("UPDATE Contratacion SET valor_acordado = 1800000.50 "
                + "WHERE id_contratacion = " + CONTRATACION);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> validar(CONTRATACION, TipoPago.ABONO, new BigDecimal("900000.51"),
                        LocalDate.now(), null, false));

        assertEquals("El valor supera el saldo pendiente de $ 900.000,50.", error.getMessage());
    }

    @Test
    void rechazaUnResumenProgramadoSiElPagoFueReemplazado() throws Exception {
        SolicitudPago resumen = validar(
                CONTRATACION, TipoPago.PAGO_FINAL, new BigDecimal("900000"),
                LocalDate.now(), null, true);

        ejecutar("DELETE FROM Movimiento_financiero WHERE id_movimiento = 2");
        ejecutar("INSERT INTO Movimiento_financiero "
                + "(id_propuesta, id_evento, id_contratacion, valor, fecha, descripcion, tipo, estado) "
                + "VALUES (1, 1, 1, 900000.00, CURRENT_TIMESTAMP, 'Nuevo pago programado', "
                + "'EGRESO', 'PENDIENTE')");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.confirmarPago(resumen, sesionOrganizador()));
        assertTrue(error.getMessage().contains("El saldo de la contratación cambió"));
        assertEquals("1", valor("SELECT COUNT(*) FROM Movimiento_financiero "
                + "WHERE id_contratacion = 1 AND estado = 'PENDIENTE'"));
    }

    private SolicitudPago validar(
            int idContratacion,
            TipoPago tipo,
            BigDecimal valor,
            LocalDate fecha,
            String detalle,
            boolean pagoProgramado) {
        return service.validarPago(
                EVENTO, idContratacion, tipo, valor, fecha, detalle, pagoProgramado,
                sesionOrganizador(), LocalDate.now());
    }

    private SesionUsuario sesionOrganizador() {
        return sesion(1, 1, "ORGANIZADOR");
    }

    private SesionUsuario sesion(int usuario, int organizador, String tipo) {
        SesionUsuario sesion = new SesionUsuario();
        sesion.setIdUsuario(usuario);
        sesion.setIdActor(organizador);
        sesion.setTipoUsuario(tipo);
        return sesion;
    }

    private void ejecutar(String sql) throws Exception {
        // cambiamos movimientos del seed para preparar pagos conocidos
        try (Connection connection = DatabaseConnection.getConnection(url);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private void liberarSaldoReservado() throws Exception {
        // quitamos el pago pendiente para que el saldo quede disponible en esta prueba
        ejecutar("DELETE FROM Movimiento_financiero "
                + "WHERE id_contratacion = " + CONTRATACION + " AND estado = 'PENDIENTE'");
    }

    private String valor(String sql) throws Exception {
        // leemos h2 para confirmar los cambios que dejó el servicio
        try (Connection connection = DatabaseConnection.getConnection(url);
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getString(1);
        }
    }
}

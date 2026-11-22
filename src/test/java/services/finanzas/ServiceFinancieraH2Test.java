package services.finanzas;

import entities.SesionUsuario;
import entities.eventos.Planificacion;
import entities.finanzas.CompromisoFinanciero;
import inicializacion.CargarDatosEjemplo;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
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
import repositories.eventos.RepositoryPlanificacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// estos repositorios usan nexteventux.db.url cuando no reciben una url
@ResourceLock(Resources.SYSTEM_PROPERTIES)
class ServiceFinancieraH2Test {

    private static final int EVENTO = 1;
    private String url;
    private ServiceFinanciera service;
    private PropiedadSistemaTemporal propiedadUrlBaseDatos;

    @BeforeEach
    void preparar() throws Exception {
        // la base vive entre conexiones del servicio y tiene un nombre único en memoria
        url = "jdbc:h2:mem:finanzas_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        // los repositorios financieros leen esta propiedad global
        propiedadUrlBaseDatos = new PropiedadSistemaTemporal("nexteventux.db.url", url);

        // cargamos el seed solo en esta h2 temporal vacía
        try (Connection connection = DatabaseConnection.getConnection(url)) {
            CargarDatosEjemplo.cargar(connection);
        }

        service = new ServiceFinanciera(
                new RepositoryMovimientoFinanciero(),
                new RepositoryPlanificacion(),
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
    void consultaElEstadoFinancieroCompletoYLaPlanificacionActiva() throws Exception {
        ejecutar("INSERT INTO Planificacion (id_evento, version, descripcion, estado) "
                + "VALUES (1, 3, 'Borrador más reciente', 'BORRADOR')");

        Map<String, Object> estado = consultarEstado();

        assertMonto("2000000.00", estado, "Pagado");
        assertMonto("2000000.00", estado, "Pagos_Programados");
        assertMonto("2000000.00", estado, "Por_Pagar");
        assertEquals(Boolean.FALSE, estado.get("Pagado_De_Mas"));
        assertEquals(Boolean.TRUE, estado.get("Presupuesto_Registrado"));
        assertMonto("15000000.00", estado, "Presupuesto");
        assertMonto("4000000.00", estado, "Comprometido");
        assertMonto("11000000.00", estado, "Disponible");
        assertEquals(0.2667, (Double) estado.get("Porcentaje_Usado"), 0.0001);
        assertEquals(Boolean.FALSE, estado.get("Excede_Presupuesto"));
        assertEquals(0, estado.get("Contrataciones_Sin_Valor"));

        assertEquals("Segunda propuesta con ajustes de decoración",
                estado.get("Planificacion_Descripcion"));
        assertEquals(2, estado.get("Planificacion_Version"));
        assertEquals("ACTIVA", estado.get("Planificacion_Estado"));
        assertEquals(4, ((List<?>) estado.get("Movimientos")).size());

        List<?> compromisos = (List<?>) estado.get("Compromisos");
        assertEquals(2, compromisos.size());
        CompromisoFinanciero primero = (CompromisoFinanciero) compromisos.get(0);
        assertEquals(1, primero.getIdContratacion());
        assertEquals(new BigDecimal("1800000.00"), primero.getValor());
    }

    @Test
    void conservaLaPrecisionDeLosImportesMonetarios() throws Exception {
        ejecutar("UPDATE Movimiento_financiero SET valor = 0.10 WHERE id_movimiento = 1");
        ejecutar("UPDATE Movimiento_financiero SET valor = 0.20 WHERE id_movimiento = 3");

        Map<String, Object> estado = consultarEstado();

        assertMonto("0.30", estado, "Pagado");
        assertTrue(estado.get("Pagado") instanceof BigDecimal);
    }

    @Test
    void normalizaMovimientosYExcluyeIngresosYDatosIncompletos() throws Exception {
        ejecutar("UPDATE Movimiento_financiero SET tipo = ' egreso ', estado = ' registrado ' "
                + "WHERE id_movimiento = 1");
        ejecutar("UPDATE Movimiento_financiero SET tipo = 'INGRESO', estado = 'REGISTRADO' "
                + "WHERE id_movimiento = 2");
        ejecutar("UPDATE Movimiento_financiero SET estado = 'ANULADO' WHERE id_movimiento = 3");
        ejecutar("UPDATE Movimiento_financiero SET valor = NULL WHERE id_movimiento = 4");
        ejecutar("INSERT INTO Movimiento_financiero "
                + "(id_propuesta, id_evento, id_contratacion, valor, fecha, descripcion, tipo, estado) "
                + "VALUES (1, 1, 1, 1000, CURRENT_TIMESTAMP, 'Tipo ausente', NULL, 'REGISTRADO')");
        ejecutar("INSERT INTO Movimiento_financiero "
                + "(id_propuesta, id_evento, id_contratacion, valor, fecha, descripcion, tipo, estado) "
                + "VALUES (1, 1, 1, NULL, CURRENT_TIMESTAMP, 'Valor ausente', 'EGRESO', 'PENDIENTE')");
        ejecutar("INSERT INTO Movimiento_financiero "
                + "(id_propuesta, id_evento, id_contratacion, valor, fecha, descripcion, tipo, estado) "
                + "VALUES (1, 1, 1, 1000, CURRENT_TIMESTAMP, 'Estado ausente', 'EGRESO', NULL)");
        ejecutar("INSERT INTO Movimiento_financiero "
                + "(id_propuesta, id_evento, id_contratacion, valor, fecha, descripcion, tipo, estado) "
                + "VALUES (1, 1, 1, 75000, CURRENT_TIMESTAMP, 'Pendiente normalizado', "
                + "' egreso ', ' pendiente ')");

        Map<String, Object> estado = consultarEstado();

        assertMonto("900000.00", estado, "Pagado");
        assertMonto("75000.00", estado, "Pagos_Programados");
    }

    @Test
    void excluyeLasContratacionesCanceladasYDetectaPagosDeMas() throws Exception {
        ejecutar("UPDATE Contratacion SET estado = 'CANCELADA' WHERE id_contratacion = 2");

        Map<String, Object> estado = consultarEstado();

        assertEquals(1, ((List<?>) estado.get("Compromisos")).size());
        assertMonto("1800000.00", estado, "Comprometido");
        assertMonto("13200000.00", estado, "Disponible");
        assertMonto("-200000.00", estado, "Por_Pagar");
        assertEquals(Boolean.TRUE, estado.get("Pagado_De_Mas"));
    }

    @Test
    void incluyeLasContratacionesFinalizadasEnElPresupuestoComprometido() throws Exception {
        ejecutar("UPDATE Contratacion SET estado = 'FINALIZADA' WHERE id_contratacion = 2");
        Map<String, Object> estado = consultarEstado();

        assertEquals(2, ((List<?>) estado.get("Compromisos")).size());
        assertMonto("4000000.00", estado, "Comprometido");
        assertEquals(Boolean.FALSE, estado.get("Pagado_De_Mas"));
    }

    @Test
    void usaElValorDeLaPropuestaYCuentaContratacionesSinValor() throws Exception {
        ejecutar("UPDATE Contratacion SET valor_acordado = NULL WHERE id_contratacion = 1");

        Map<String, Object> estado = consultarEstado();
        List<?> compromisos = (List<?>) estado.get("Compromisos");
        CompromisoFinanciero primero = (CompromisoFinanciero) compromisos.get(0);

        assertEquals(new BigDecimal("1800000.00"), primero.getValor());
        assertMonto("4000000.00", estado, "Comprometido");
        assertEquals(0, estado.get("Contrataciones_Sin_Valor"));

        ejecutar("UPDATE Propuesta SET valor = NULL WHERE id_propuesta = 1");
        estado = consultarEstado();

        assertMonto("2200000.00", estado, "Comprometido");
        assertMonto("12800000.00", estado, "Disponible");
        assertEquals(1, estado.get("Contrataciones_Sin_Valor"));
    }

    @Test
    void informaCuandoLosCompromisosSuperanElPresupuesto() throws Exception {
        ejecutar("UPDATE Evento SET presupuesto = 3000000 WHERE id_evento = 1");

        Map<String, Object> estado = consultarEstado();

        assertEquals(Boolean.TRUE, estado.get("Excede_Presupuesto"));
        assertMonto("-1000000.00", estado, "Disponible");
        assertEquals(1.3333, (Double) estado.get("Porcentaje_Usado"), 0.0001);
    }

    @Test
    void informaCuandoElPresupuestoEsCero() throws Exception {
        ejecutar("UPDATE Evento SET presupuesto = 0 WHERE id_evento = 1");
        Map<String, Object> estado = consultarEstado();

        assertEquals(Boolean.TRUE, estado.get("Presupuesto_Registrado"));
        assertEquals(Boolean.TRUE, estado.get("Excede_Presupuesto"));
        assertEquals(0.0, (Double) estado.get("Porcentaje_Usado"), 0.0001);
    }

    @Test
    void informaCuandoElPresupuestoNoEstaRegistrado() throws Exception {
        ejecutar("UPDATE Evento SET presupuesto = NULL WHERE id_evento = 1");
        Map<String, Object> estado = consultarEstado();

        assertEquals(Boolean.FALSE, estado.get("Presupuesto_Registrado"));
        assertMonto("0.00", estado, "Presupuesto");
        assertMonto("-4000000.00", estado, "Disponible");
        assertEquals(Boolean.FALSE, estado.get("Excede_Presupuesto"));
        assertEquals(0.0, (Double) estado.get("Porcentaje_Usado"), 0.0001);
        assertNull(service.consultarPresupuestoDisponible(EVENTO, sesionOrganizador()));
    }

    @Test
    void muestraUnMensajeCuandoNoHayPlanificacion() throws Exception {
        ejecutar("DELETE FROM Planificacion WHERE id_evento = 1");

        Map<String, Object> estado = consultarEstado();

        assertEquals("No hay planificación registrada para este evento.",
                estado.get("Planificacion_Descripcion"));
        assertEquals("Desconocido", estado.get("Planificacion_Estado"));
        assertFalse(estado.containsKey("Planificacion_Version"));
    }

    @Test
    void consultaUnEventoSinContratacionesActivasNiMovimientos() throws Exception {
        ejecutar("UPDATE Contratacion SET estado = 'CANCELADA' WHERE id_evento = 1");
        ejecutar("DELETE FROM Movimiento_financiero WHERE id_evento = 1");

        Map<String, Object> estado = consultarEstado();

        assertMonto("0.00", estado, "Pagado");
        assertMonto("0.00", estado, "Pagos_Programados");
        assertMonto("0.00", estado, "Por_Pagar");
        assertMonto("15000000.00", estado, "Disponible");
        assertEquals(Boolean.FALSE, estado.get("Pagado_De_Mas"));
        assertEquals(0, ((List<?>) estado.get("Movimientos")).size());
        assertEquals(0, ((List<?>) estado.get("Compromisos")).size());
    }

    @Test
    void envuelveErroresAlConsultarElEstadoFinanciero() throws Exception {
        ejecutar("ALTER TABLE Movimiento_financiero RENAME COLUMN valor TO valor_prueba");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.consultarEstadoFinanciero(EVENTO, sesionOrganizador()));

        assertEquals("No se pudo consultar el estado financiero. Vuelve a intentarlo.", error.getMessage());
        assertTrue(error.getCause() instanceof RuntimeException);
    }

    @Test
    void envuelveErroresAlConsultarElPresupuestoDisponible() throws Exception {
        ejecutar("ALTER TABLE Contratacion RENAME COLUMN valor_acordado TO valor_acordado_prueba");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.consultarPresupuestoDisponible(EVENTO, sesionOrganizador()));

        assertEquals("No se pudo consultar el presupuesto disponible. Vuelve a intentarlo.", error.getMessage());
        assertTrue(error.getCause() instanceof RuntimeException);
    }

    @Test
    void consultaPresupuestoDisponibleConCompromisosReales() throws Exception {
        assertMonto("11000000.00",
                service.consultarPresupuestoDisponible(EVENTO, sesionOrganizador()));

        ejecutar("UPDATE Contratacion SET estado = 'CANCELADA' WHERE id_contratacion = 2");

        assertMonto("13200000.00",
                service.consultarPresupuestoDisponible(EVENTO, sesionOrganizador()));
    }

    @Test
    void noSumaCompromisosRelacionadosConOtroEvento() throws Exception {
        ejecutar("INSERT INTO Contratacion "
                + "(id_propuesta, id_evento, fecha_contratacion, estado, condiciones, valor_acordado) "
                + "VALUES (3, 1, CURRENT_TIMESTAMP, 'ACTIVA', 'Relación de prueba', 900000)");

        Map<String, Object> estado = consultarEstado();

        assertEquals(2, ((List<?>) estado.get("Compromisos")).size());
        assertMonto("4000000.00", estado, "Comprometido");
    }

    @Test
    void rechazaSesionesYEventosQueNoPertenecenAlOrganizador() {
        assertThrows(IllegalStateException.class,
                () -> service.consultarEstadoFinanciero(EVENTO, null));
        assertThrows(IllegalStateException.class,
                () -> service.consultarEstadoFinanciero(
                        EVENTO, sesion(1, 1, "PROVEEDOR")));
        assertThrows(IllegalStateException.class,
                () -> service.consultarEstadoFinanciero(
                        EVENTO, sesion(999, 1, "ORGANIZADOR")));

        assertThrows(IllegalArgumentException.class,
                () -> service.consultarEstadoFinanciero(0, sesionOrganizador()));
        assertThrows(IllegalArgumentException.class,
                () -> service.consultarEstadoFinanciero(999, sesionOrganizador()));
        assertThrows(IllegalArgumentException.class,
                () -> service.consultarEstadoFinanciero(1, sesion(2, 2, "ORGANIZADOR")));
        assertThrows(IllegalArgumentException.class,
                () -> service.consultarPresupuestoDisponible(0, sesionOrganizador()));
    }

    private Map<String, Object> consultarEstado() {
        return service.consultarEstadoFinanciero(EVENTO, sesionOrganizador());
    }

    private SesionUsuario sesionOrganizador() {
        return sesion(1, 1, "ORGANIZADOR");
    }

    private SesionUsuario sesion(int idUsuario, int idOrganizador, String tipoUsuario) {
        SesionUsuario sesion = new SesionUsuario();
        sesion.setIdUsuario(idUsuario);
        sesion.setIdActor(idOrganizador);
        sesion.setTipoUsuario(tipoUsuario);
        return sesion;
    }

    private void ejecutar(String sql) throws Exception {
        // ajustamos datos del seed para comprobar estados financieros específicos
        try (Connection connection = DatabaseConnection.getConnection(url);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private void assertMonto(String esperado, Map<String, Object> estado, String clave) {
        assertMonto(esperado, (BigDecimal) estado.get(clave));
    }

    private void assertMonto(String esperado, BigDecimal actual) {
        assertEquals(0, new BigDecimal(esperado).compareTo(actual));
    }
}

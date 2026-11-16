package services.proveedores;

import entities.SesionUsuario;
import inicializacion.CargarDatosEjemplo;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import persistencia.DatabaseConnection;
import repositories.proveedores.RepositorySancionesProveedor;
import repositories.proveedores.RepositorySancionesProveedor.ContratoSancionable;
import repositories.proveedores.RepositorySancionesProveedor.EventoProveedor;
import repositories.proveedores.RepositorySancionesProveedor.ResumenProveedor;
import services.proveedores.ServiceSancionesProveedor.ResultadoSancion;
import services.proveedores.ServiceSancionesProveedor.SolicitudSancion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceSancionesProveedorH2Test {

    private String url;
    private ServiceSancionesProveedor service;
    private SesionUsuario administrador;

    @BeforeEach
    void preparar() throws Exception {
        // la base vive entre conexiones del servicio y tiene un nombre único en memoria
        url = "jdbc:h2:mem:sanciones_" + UUID.randomUUID().toString().replace("-", "")
                + ";DB_CLOSE_DELAY=-1";
        // el seed se carga únicamente en esta base temporal vacía
        try (Connection connection = DatabaseConnection.getConnection(url)) {
            CargarDatosEjemplo.cargar(connection);
        }
        service = new ServiceSancionesProveedor(new RepositorySancionesProveedor(url));
        administrador = sesion("ADMINISTRADOR", "ACTIVO");
    }

    @AfterEach
    void cerrarBase() throws Exception {
        if (url != null) {
            // usamos jdbc directo para apagar esta base temporal sin volver a inicializarla
            try (Connection connection = DriverManager.getConnection(url, "sa", "");
                    Statement statement = connection.createStatement()) {
                statement.execute("SHUTDOWN");
            }
        }
    }

    @Test
    void buscaPorNombreEIdYConsultaEventosYContratosReales() {
        assertTrue(contieneProveedor(
                service.buscarProveedores(administrador, "  mariana  "), 1));
        assertTrue(contieneProveedor(
                service.buscarProveedores(administrador, " 1 "), 1));

        List<EventoProveedor> eventos = service.eventosDelProveedor(administrador, 1);
        assertTrue(contieneEvento(eventos, 1));

        List<ContratoSancionable> contratos = service.contratosActivos(administrador, 1, 1);
        ContratoSancionable contrato = null;
        for (ContratoSancionable item : contratos) {
            if (item.idContratacion() == 1) {
                contrato = item;
                break;
            }
        }
        assertNotNull(contrato);
        assertEquals(new BigDecimal("1800000.00"), contrato.valorAcordado());
    }

    @Test
    void acumulaMultasYPersisteSancionesYNotificaciones() throws Exception {
        ResultadoSancion primera = service.aplicarSancion(administrador, multa("10"));
        ResultadoSancion segunda = service.aplicarSancion(administrador, multa("20"));

        assertDinero("180000.00", primera.valorDescontado());
        assertDinero("1260000.00", segunda.saldoPendiente());
        assertEquals("APROBADO", segunda.estadoProveedor());
        assertEquals(2, contar("SELECT COUNT(*) FROM Sancion "
                + "WHERE motivo LIKE 'CU09|MULTA|PROV=1|CONTRATO=1|%'"));
        assertEquals(2, contar("SELECT COUNT(*) FROM Notificacion_interna "
                + "WHERE id_usuario = 3 AND id_evento = 1 AND mensaje LIKE 'Se aplicó una multa%'"));
    }

    @Test
    void multaQueSuperaElSaldoNoAgregaUnaSancionNiUnaNotificacion() throws Exception {
        service.aplicarSancion(administrador, multa("100"));

        assertThrows(IllegalArgumentException.class,
                () -> service.aplicarSancion(administrador, multa("1")));

        assertEquals(1, contar("SELECT COUNT(*) FROM Sancion "
                + "WHERE motivo LIKE 'CU09|MULTA|PROV=1|CONTRATO=1|%'"));
        assertEquals(1, contar("SELECT COUNT(*) FROM Notificacion_interna "
                + "WHERE id_usuario = 3 AND id_evento = 1 AND mensaje LIKE 'Se aplicó una multa%'"));
    }

    @Test
    void suspensionActualizaProveedorYNotificaYNoPuedeRepetirse() throws Exception {
        ResultadoSancion resultado = service.aplicarSancion(administrador, suspension());

        assertEquals("SUSPENDIDO", resultado.estadoProveedor());
        assertEquals(1, contar("SELECT COUNT(*) FROM Proveedor "
                + "WHERE id_proveedor = 1 AND estado = 'SUSPENDIDO'"));
        assertEquals(1, contar("SELECT COUNT(*) FROM Sancion "
                + "WHERE motivo LIKE 'CU09|SUSPENSION|PROV=1|%'"));
        assertEquals(1, contar("SELECT COUNT(*) FROM Notificacion_interna "
                + "WHERE id_usuario = 3 AND id_evento = 1 AND mensaje LIKE 'Tu cuenta de proveedor fue suspendida%'"));

        assertThrows(IllegalArgumentException.class,
                () -> service.aplicarSancion(administrador, suspension()));
        assertEquals(1, contar("SELECT COUNT(*) FROM Sancion "
                + "WHERE motivo LIKE 'CU09|SUSPENSION|PROV=1|%'"));
    }

    @Test
    void rechazaSesionesSinPermisoYEntradasDeSancionInvalidas() {
        SesionUsuario proveedor = sesion("PROVEEDOR", "ACTIVO");
        assertThrows(SecurityException.class,
                () -> service.buscarProveedores(proveedor, "Mariana"));
        assertThrows(SecurityException.class,
                () -> service.eventosDelProveedor(sesion("ADMINISTRADOR", "INACTIVO"), 1));

        assertThrows(IllegalArgumentException.class,
                () -> service.aplicarSancion(administrador, null));
        assertThrows(IllegalArgumentException.class,
                () -> service.aplicarSancion(administrador,
                        new SolicitudSancion(1, 1, "FALTA_INVALIDA", "SUSPENSION", 0, null,
                                "Justificación suficientemente larga")));
        assertThrows(IllegalArgumentException.class,
                () -> service.aplicarSancion(administrador,
                        new SolicitudSancion(1, 1, "RETRASO", "MULTA", 1, "0",
                                "Justificación suficientemente larga")));
        assertThrows(IllegalArgumentException.class,
                () -> service.buscarProveedores(administrador, "x".repeat(101)));
    }

    @Test
    void noRegistraUnaMultaSiElContratoNoPerteneceAlProveedor() throws Exception {
        int sancionesAntes = contar("SELECT COUNT(*) FROM Sancion");
        SolicitudSancion solicitud = new SolicitudSancion(
                1, 1, "RETRASO", "MULTA", 2, "10", "Retraso durante el montaje");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.aplicarSancion(administrador, solicitud));
        assertTrue(error.getMessage().contains("no pertenece"));
        assertEquals(sancionesAntes, contar("SELECT COUNT(*) FROM Sancion"));
        assertEquals(0, contar("SELECT COUNT(*) FROM Notificacion_interna "
                + "WHERE id_usuario = 3 AND id_evento = 1 AND mensaje LIKE 'Se aplicó una multa%'"));
    }

    @Test
    void rechazaPorcentajesMalFormadosFueraDeRangoOSinPrecisionPermitida()
            throws Exception {
        int sancionesAntes = contar("SELECT COUNT(*) FROM Sancion");
        int notificacionesAntes = contar("SELECT COUNT(*) FROM Notificacion_interna");
        String estadoProveedorAntes = valor(
                "SELECT estado FROM Proveedor WHERE id_proveedor = 1");
        assertEquals("APROBADO", estadoProveedorAntes);

        IllegalArgumentException porcentajeMalFormado = assertThrows(
                IllegalArgumentException.class,
                () -> service.aplicarSancion(administrador, multa("doce")));
        assertTrue(porcentajeMalFormado.getMessage().contains("debe ser un número"));
        IllegalArgumentException porcentajeFueraDeRango = assertThrows(
                IllegalArgumentException.class,
                () -> service.aplicarSancion(administrador, multa("100.01")));
        assertTrue(porcentajeFueraDeRango.getMessage().contains("no superar 100"));
        IllegalArgumentException precisionInvalida = assertThrows(
                IllegalArgumentException.class,
                () -> service.aplicarSancion(administrador, multa("10.001")));
        assertTrue(precisionInvalida.getMessage().contains("máximo dos decimales"));

        assertEquals(sancionesAntes, contar("SELECT COUNT(*) FROM Sancion"));
        assertEquals(notificacionesAntes,
                contar("SELECT COUNT(*) FROM Notificacion_interna"));
        assertEquals(estadoProveedorAntes,
                valor("SELECT estado FROM Proveedor WHERE id_proveedor = 1"));
    }

    @Test
    void conviertePorcentajeConComaYPersisteMultaYNotificacion() throws Exception {
        ResultadoSancion resultado
                = service.aplicarSancion(administrador, multa("10,5"));

        assertDinero("189000.00", resultado.valorDescontado());
        assertDinero("1611000.00", resultado.saldoPendiente());
        assertEquals(1, contar("SELECT COUNT(*) FROM Sancion "
                + "WHERE motivo LIKE 'CU09|MULTA|PROV=1|CONTRATO=1|%'"));
        assertEquals(1, contar("SELECT COUNT(*) FROM Notificacion_interna "
                + "WHERE id_usuario = 3 AND id_evento = 1 "
                + "AND mensaje LIKE 'Se aplicó una multa%'"));
    }

    @Test
    void rechazaIdentificadoresYRelacionEventoProveedorInvalidosSinEscrituras()
            throws Exception {
        int sancionesAntes = contar("SELECT COUNT(*) FROM Sancion");
        int notificacionesAntes = contar("SELECT COUNT(*) FROM Notificacion_interna");
        String estadoProveedorAntes = valor(
                "SELECT estado FROM Proveedor WHERE id_proveedor = 1");
        assertEquals("APROBADO", estadoProveedorAntes);

        IllegalArgumentException proveedorInexistente = assertThrows(
                IllegalArgumentException.class,
                () -> service.aplicarSancion(administrador,
                        new SolicitudSancion(
                                9999, 1, "RETRASO", "MULTA", 1, "10",
                                "Retraso durante el montaje")));
        assertTrue(proveedorInexistente.getMessage().contains("proveedor no existe"));
        IllegalArgumentException eventoNoRelacionado = assertThrows(
                IllegalArgumentException.class,
                () -> service.aplicarSancion(administrador,
                        new SolicitudSancion(
                                1, 9999, "RETRASO", "MULTA", 1, "10",
                                "Retraso durante el montaje")));
        assertTrue(eventoNoRelacionado.getMessage().contains("no tiene servicios"));
        IllegalArgumentException idEventoInvalido = assertThrows(
                IllegalArgumentException.class,
                () -> service.aplicarSancion(administrador,
                        new SolicitudSancion(
                                1, 0, "RETRASO", "MULTA", 1, "10",
                                "Retraso durante el montaje")));
        assertTrue(idEventoInvalido.getMessage().contains("evento válido"));

        assertEquals(sancionesAntes, contar("SELECT COUNT(*) FROM Sancion"));
        assertEquals(notificacionesAntes,
                contar("SELECT COUNT(*) FROM Notificacion_interna"));
        assertEquals(estadoProveedorAntes,
                valor("SELECT estado FROM Proveedor WHERE id_proveedor = 1"));
    }

    private SolicitudSancion multa(String porcentaje) {
        return new SolicitudSancion(
                1, 1, "RETRASO", "MULTA", 1, porcentaje, "Retraso durante el montaje");
    }

    private SolicitudSancion suspension() {
        return new SolicitudSancion(
                1, 1, "INCUMPLIMIENTO_CONDICIONES", "SUSPENSION", 0, null,
                "No cumplió las condiciones acordadas");
    }

    private SesionUsuario sesion(String tipo, String estado) {
        SesionUsuario sesion = new SesionUsuario();
        sesion.setIdUsuario(14);
        sesion.setTipoUsuario(tipo);
        sesion.setEstadoUsuario(estado);
        return sesion;
    }

    private int contar(String sql) throws Exception {
        // contamos los registros persistidos porque el servicio no ofrece esta consulta
        try (Connection connection = DatabaseConnection.getConnection(url);
                Statement statement = connection.createStatement();
                ResultSet resultado = statement.executeQuery(sql)) {
            resultado.next();
            return resultado.getInt(1);
        }
    }

    private String valor(String sql) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection(url);
                Statement statement = connection.createStatement();
                ResultSet resultado = statement.executeQuery(sql)) {
            resultado.next();
            return resultado.getString(1);
        }
    }

    private boolean contieneProveedor(List<ResumenProveedor> proveedores, int idProveedor) {
        for (ResumenProveedor proveedor : proveedores) {
            if (proveedor.idProveedor() == idProveedor) {
                return true;
            }
        }
        return false;
    }

    private boolean contieneEvento(List<EventoProveedor> eventos, int idEvento) {
        for (EventoProveedor evento : eventos) {
            if (evento.idEvento() == idEvento) {
                return true;
            }
        }
        return false;
    }

    private void assertDinero(String esperado, BigDecimal actual) {
        assertEquals(0, new BigDecimal(esperado).compareTo(actual));
    }
}

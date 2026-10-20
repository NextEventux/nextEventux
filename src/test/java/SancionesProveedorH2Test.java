import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import config.CargarDatosEjemplo;
import config.DatabaseConnection;
import entities.SesionUsuario;
import repositories.RepositorySancionesProveedor;
import services.ServiceSancionesProveedor;
import services.ServiceSancionesProveedor.ResultadoSancion;
import services.ServiceSancionesProveedor.SolicitudSancion;

/**
 * CU-09 (aplicar sanciones) sobre H2 en memoria, con los datos del seed:
 * proveedor 1 (Mariana Lopez, usuario 3) tiene el contrato 1 (evento 1, ACTIVA, 1.800.000).
 */
class SancionesProveedorH2Test {

    private String url;
    private ServiceSancionesProveedor servicio;
    private SesionUsuario admin;

    @BeforeEach
    void preparar() throws Exception {
        url = "jdbc:h2:mem:sanciones_" + UUID.randomUUID().toString().replace("-", "")
                + ";DB_CLOSE_DELAY=-1";
        try (Connection c = DatabaseConnection.getConnection(url)) {
            CargarDatosEjemplo.cargar(c);
        }
        servicio = new ServiceSancionesProveedor(new RepositorySancionesProveedor(url));

        admin = new SesionUsuario();
        admin.setIdUsuario(7);
        admin.setTipoUsuario("ADMINISTRADOR");
        admin.setEstadoUsuario("ACTIVO");
    }

    private SolicitudSancion multa(String porcentaje, String justificacion) {
        return new SolicitudSancion(1, 1, "RETRASO", "MULTA", 1, porcentaje, justificacion);
    }

    private SolicitudSancion suspension(String justificacion) {
        return new SolicitudSancion(1, 1, "INCUMPLIMIENTO_CONDICIONES", "SUSPENSION", 0, null, justificacion);
    }

    private static void igualDinero(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real),
                "esperado " + esperado + " pero fue " + real);
    }

    private int contar(String sql, Object... parametros) throws Exception {
        try (Connection c = DatabaseConnection.getConnection(url);
             var s = c.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                s.setObject(i + 1, parametros[i]);
            }
            try (ResultSet r = s.executeQuery()) {
                r.next();
                return r.getInt(1);
            }
        }
    }

    @Test
    void multaDescuentaElPorcentajeYActualizaElSaldo() {
        ResultadoSancion r = servicio.aplicarSancion(admin, multa("10", "Llegó tarde al montaje"));

        igualDinero("180000.00", r.valorDescontado());
        igualDinero("1620000.00", r.saldoPendiente());
        assertEquals("APROBADO", r.estadoProveedor());
    }

    @Test
    void lasMultasSeAcumulan() {
        servicio.aplicarSancion(admin, multa("10", "Primera falta del montaje"));
        ResultadoSancion segunda = servicio.aplicarSancion(admin, multa("20", "Segunda falta del montaje"));

        igualDinero("1260000.00", segunda.saldoPendiente());
    }

    @Test
    void lasMultasNoSuperanElValorDelContrato() {
        servicio.aplicarSancion(admin, multa("100", "Incumplimiento total del contrato"));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> servicio.aplicarSancion(admin, multa("1", "Una multa adicional de prueba")));
        assertTrue(error.getMessage().contains("supera el saldo pendiente"));
    }

    @Test
    void laComaDecimalSeAcepta() {
        ResultadoSancion r = servicio.aplicarSancion(admin, multa("12,5", "Retraso en la entrega del servicio"));

        igualDinero("225000.00", r.valorDescontado());
    }

    @Test
    void porcentajesInvalidosSeRechazan() {
        for (String invalido : new String[] {"0", "100,5", "-5", "abc", "1.234", ""}) {
            assertThrows(IllegalArgumentException.class,
                    () -> servicio.aplicarSancion(admin, multa(invalido, "Justificación válida de prueba")),
                    "debió rechazar el porcentaje: '" + invalido + "'");
        }
    }

    @Test
    void suspensionCambiaElEstadoDelProveedorYNoSePuedeRepetir() throws Exception {
        ResultadoSancion r = servicio.aplicarSancion(admin, suspension("Se presentó sin el equipo acordado"));

        assertEquals("SUSPENDIDO", r.estadoProveedor());
        assertEquals(1, contar("SELECT COUNT(*) FROM Proveedor WHERE id_proveedor = 1 AND estado = 'SUSPENDIDO'"));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> servicio.aplicarSancion(admin, suspension("Otra falta de prueba registrada")));
        assertTrue(error.getMessage().contains("ya se encuentra suspendido"));
    }

    @Test
    void sancionYNotificacionQuedanRegistradasEnLaMismaTransaccion() throws Exception {
        servicio.aplicarSancion(admin, multa("10", "Llegó tarde al montaje"));

        assertEquals(1, contar("SELECT COUNT(*) FROM Sancion WHERE motivo LIKE ?",
                "CU09|MULTA|PROV=1|CONTRATO=1|%"));
        assertTrue(contar("SELECT COUNT(*) FROM Notificacion_interna WHERE id_usuario = 3 AND id_evento = 1 "
                + "AND mensaje LIKE 'Se aplicó una multa%'") >= 1);
    }

    @Test
    void proveedorSinServiciosEnEventoSeRechaza() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.aplicarSancion(admin,
                        new SolicitudSancion(1, 999, "RETRASO", "SUSPENSION", 0, null,
                                "Justificación válida de prueba")));
    }

    @Test
    void justificacionCortaOExcesivaSeRechaza() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.aplicarSancion(admin, multa("10", "corta")));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.aplicarSancion(admin, multa("10", "x".repeat(151))));
    }

    @Test
    void soloUnAdministradorPuedeAplicarSanciones() {
        SesionUsuario cliente = new SesionUsuario();
        cliente.setIdUsuario(5);
        cliente.setTipoUsuario("CLIENTE");
        cliente.setEstadoUsuario("ACTIVO");

        assertThrows(SecurityException.class,
                () -> servicio.aplicarSancion(cliente, multa("10", "Justificación válida de prueba")));
    }

    @Test
    void proveedorInexistenteSeRechaza() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.aplicarSancion(admin,
                        new SolicitudSancion(999, 1, "RETRASO", "SUSPENSION", 0, null,
                                "Justificación válida de prueba")));
    }

    @Test
    void buscarProveedorPorNombreOId() {
        assertTrue(servicio.buscarProveedores(admin, "mariana").stream().anyMatch(p -> p.idProveedor() == 1));
        assertTrue(servicio.buscarProveedores(admin, "1").stream().anyMatch(p -> p.idProveedor() == 1));
    }
}

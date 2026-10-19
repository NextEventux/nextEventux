import config.DatabaseConnection;
import controllers.ControllerInvitados;
import entities.Invitacion;
import entities.Invitado;
import entities.Regalo;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repositories.RepositoryEvento;
import repositories.RepositoryInvitacion;
import repositories.RepositoryInvitado;
import repositories.RepositoryLugar;
import repositories.RepositoryRegalo;
import repositories.RepositoryReservaRegalo;
import services.ServiceInvitados;
import static org.junit.jupiter.api.Assertions.*;

/** CU-20: el organizador arma la mesa de regalos y los invitados reservan sin duplicados (R43). */
class MesaRegalosTest {
    private static final int EVENTO = DatosInvitacionesPrueba.EVENTO;
    private static final int GRANDE = DatosInvitacionesPrueba.LUGAR_GRANDE;

    @TempDir Path directorio;
    private String url;
    private ServiceInvitados service;
    private ControllerInvitados controller;
    private int invitacion;
    private int ana;
    private int luis;

    @BeforeEach
    void prepararBase() throws Exception {
        url = "jdbc:h2:" + directorio.resolve("regalos");
        DatosInvitacionesPrueba.preparar(url);
        service = new ServiceInvitados(new RepositoryInvitacion(url), new RepositoryInvitado(url),
                new RepositoryLugar(url), new RepositoryEvento(url), new RepositoryRegalo(url),
                new RepositoryReservaRegalo(url));
        controller = new ControllerInvitados(service);
        invitacion = crearInvitacion(EVENTO);
        ana = agregarInvitado(invitacion, "Ana", "ana@correo.com");
        luis = agregarInvitado(invitacion, "Luis", "luis@correo.com");
    }

    // ---------------------------------------------------------------- organizador
    @Test
    void elOrganizadorAgregaRegalosDisponibles() {
        int cobija = agregarRegalo(invitacion, "  Cobija  ", "Cobija suave", "60000");
        List<Regalo> mesa = controller.listarRegalos(invitacion);
        assertEquals(1, mesa.size());
        Regalo regalo = mesa.get(0);
        assertEquals(cobija, regalo.getIdRegalo());
        assertEquals("Cobija", regalo.getNombre());
        assertEquals(Regalo.ESTADO_DISPONIBLE, regalo.getEstado());
        assertEquals(0, new BigDecimal("60000").compareTo(regalo.getPrecioEstimado()));
        assertFalse(regalo.estaReservado());
    }

    @Test
    void validaLosDatosDelRegalo() {
        assertMensaje(() -> agregarRegalo(invitacion, "  ", null, "1000"), "nombre");
        assertMensaje(() -> agregarRegalo(invitacion, "Cobija", null, "0"), "mayor que cero");
        assertMensaje(() -> service.agregarRegalo(invitacion, new Regalo("Cobija", null, null)), "mayor que cero");
        assertMensaje(() -> agregarRegalo(invitacion, "Cobija", "x".repeat(256), "1000"), "255");
        assertMensaje(() -> agregarRegalo(invitacion, "Cobija", null, "100000000000"), "demasiado alto");
        assertTrue(controller.listarRegalos(invitacion).isEmpty());
    }

    @Test
    void noSeModificaLaMesaDeUnaInvitacionRevocada() {
        service.revocarInvitacion(invitacion);
        assertMensaje(() -> agregarRegalo(invitacion, "Cobija", null, "1000"), "revocada");
    }

    @Test
    void permiteVariasUnidadesComoRegalosSeparados() {
        agregarRegalo(invitacion, "Pañitos", null, "25000");
        agregarRegalo(invitacion, "Pañitos", null, "25000");
        assertEquals(2, controller.listarRegalos(invitacion).size());
    }

    @Test
    void noSePuedeQuitarUnRegaloReservado() {
        int cobija = agregarRegalo(invitacion, "Cobija", null, "60000");
        controller.reservarRegalo(ana, cobija);
        assertMensaje(() -> controller.quitarRegalo(cobija), "ya fue reservado");

        controller.cancelarReservaRegalo(ana, cobija);
        controller.quitarRegalo(cobija); // con la reserva cancelada sí se puede quitar
        assertTrue(controller.listarRegalos(invitacion).isEmpty());
    }

    // ---------------------------------------------------------------- invitado
    @Test
    void elInvitadoReservaUnRegaloYOtroNoPuedeReservarlo() throws Exception {
        int cobija = agregarRegalo(invitacion, "Cobija", null, "60000");
        assertTrue(controller.validarDisponibilidadRegalo(cobija));

        controller.reservarRegalo(ana, cobija);

        assertFalse(controller.validarDisponibilidadRegalo(cobija));
        Regalo paraAna = controller.consultarRegalosDisponibles(ana).get(0);
        assertTrue(paraAna.estaReservadoPor(ana));
        Regalo paraLuis = controller.consultarRegalosDisponibles(luis).get(0);
        assertTrue(paraLuis.estaReservado());
        assertFalse(paraLuis.estaReservadoPor(luis));
        assertEquals("Ana", paraLuis.getReservadoPor());
        assertEquals(Regalo.ESTADO_RESERVADO, estadoEnTabla(cobija));

        assertMensaje(() -> controller.reservarRegalo(luis, cobija), "otro invitado");
        assertMensaje(() -> controller.reservarRegalo(ana, cobija), "Ya tienes reservado");
        assertEquals(1, contarReservasActivas(cobija));
    }

    @Test
    void cancelarLiberaElRegaloParaOtroInvitado() throws Exception {
        int cobija = agregarRegalo(invitacion, "Cobija", null, "60000");
        controller.reservarRegalo(ana, cobija);
        assertMensaje(() -> controller.cancelarReservaRegalo(luis, cobija), "No tienes una reserva");

        controller.cancelarReservaRegalo(ana, cobija);
        assertTrue(controller.validarDisponibilidadRegalo(cobija));
        assertEquals(Regalo.ESTADO_DISPONIBLE, estadoEnTabla(cobija));

        controller.reservarRegalo(luis, cobija);
        assertTrue(controller.consultarRegalosDisponibles(luis).get(0).estaReservadoPor(luis));
    }

    @Test
    void soloVeYReservaLosRegalosDeSuInvitacion() {
        int otraInvitacion = crearInvitacion(DatosInvitacionesPrueba.OTRO_EVENTO);
        int ajena = agregarRegalo(otraInvitacion, "Libro", null, "40000");
        agregarRegalo(invitacion, "Cobija", null, "60000");

        List<Regalo> mesa = controller.consultarRegalosDisponibles(ana);
        assertEquals(1, mesa.size());
        assertEquals("Cobija", mesa.get(0).getNombre());
        assertMensaje(() -> controller.reservarRegalo(ana, ajena), "no pertenece");
    }

    @Test
    void noSeReservaEnUnEventoQueYaComenzo() throws Exception {
        int pasada = DatosInvitacionesPrueba.insertarInvitacion(url, DatosInvitacionesPrueba.EVENTO_PASADO, GRANDE);
        int pedro = insertarInvitado(pasada, "pedro@correo.com");
        int regalo = insertarRegalo(pasada, "Reloj");
        assertMensaje(() -> controller.reservarRegalo(pedro, regalo), "ya comenzó");
    }

    @Test
    void conReservasDuplicadasAntiguasCadaInvitadoVeLaSuya() throws Exception {
        // Como en seed.sql: el mismo regalo quedó reservado por dos invitados.
        int panitos = agregarRegalo(invitacion, "Pañitos", null, "25000");
        try (Connection c = DatabaseConnection.getConnection(url); Statement s = c.createStatement()) {
            s.executeUpdate("INSERT INTO Reserva_regalo (id_evento, id_invitado, id_regalo, fecha_reserva, estado) VALUES "
                    + "(" + EVENTO + ", " + ana + ", " + panitos + ", CURRENT_TIMESTAMP, 'RESERVADO'), "
                    + "(" + EVENTO + ", " + luis + ", " + panitos + ", CURRENT_TIMESTAMP, 'RESERVADO')");
        }
        assertTrue(controller.consultarRegalosDisponibles(ana).get(0).estaReservadoPor(ana));
        assertTrue(controller.consultarRegalosDisponibles(luis).get(0).estaReservadoPor(luis));

        controller.cancelarReservaRegalo(ana, panitos);
        assertFalse(controller.validarDisponibilidadRegalo(panitos)); // Luis todavía lo tiene
        controller.cancelarReservaRegalo(luis, panitos);
        assertTrue(controller.validarDisponibilidadRegalo(panitos));
    }

    @Test
    void validaQueElInvitadoYElRegaloExistan() {
        int cobija = agregarRegalo(invitacion, "Cobija", null, "60000");
        assertMensaje(() -> controller.reservarRegalo(999, cobija), "invitado no existe");
        assertMensaje(() -> controller.reservarRegalo(ana, 999), "regalo no existe");
        assertMensaje(() -> controller.consultarRegalosDisponibles(999), "invitado no existe");
    }

    // ---------------------------------------------------------------- utilidades
    private int crearInvitacion(int evento) {
        Invitacion datos = new Invitacion();
        datos.setIdLugar(GRANDE);
        datos.setMensaje("Te invitamos");
        service.crearInvitacion(evento, datos);
        return datos.getIdInvitacion();
    }

    private int agregarInvitado(int idInvitacion, String nombre, String correo) {
        Invitado invitado = new Invitado(nombre, correo, null, null);
        service.agregarInvitado(idInvitacion, invitado);
        return invitado.getIdInvitado();
    }

    private int agregarRegalo(int idInvitacion, String nombre, String descripcion, String precio) {
        Regalo regalo = new Regalo(nombre, descripcion, new BigDecimal(precio));
        controller.agregarRegalo(idInvitacion, regalo);
        return regalo.getIdRegalo();
    }

    private int insertarInvitado(int idInvitacion, String correo) throws Exception {
        return insertar("INSERT INTO Invitado (id_invitacion, nombre, estado, correo) VALUES ("
                + idInvitacion + ", 'Pedro', 'PENDIENTE', '" + correo + "')");
    }

    private int insertarRegalo(int idInvitacion, String nombre) throws Exception {
        return insertar("INSERT INTO Regalo (id_invitacion, nombre, estado, precio_estimado) VALUES ("
                + idInvitacion + ", '" + nombre + "', 'DISPONIBLE', 10000)");
    }

    private int insertar(String sql) throws Exception {
        try (Connection c = DatabaseConnection.getConnection(url); Statement s = c.createStatement()) {
            s.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
            try (ResultSet keys = s.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private String estadoEnTabla(int idRegalo) throws Exception {
        try (Connection c = DatabaseConnection.getConnection(url); Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT estado FROM Regalo WHERE id_regalo = " + idRegalo)) {
            r.next();
            return r.getString(1);
        }
    }

    private int contarReservasActivas(int idRegalo) throws Exception {
        try (Connection c = DatabaseConnection.getConnection(url); Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT COUNT(*) FROM Reserva_regalo WHERE estado = 'RESERVADO' AND id_regalo = " + idRegalo)) {
            r.next();
            return r.getInt(1);
        }
    }

    private void assertMensaje(org.junit.jupiter.api.function.Executable accion, String motivo) {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, accion);
        assertTrue(error.getMessage().contains(motivo), error.getMessage());
    }
}

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import config.DatabaseConnection;
import entities.ConfirmacionContratacion;
import entities.DisponibilidadContratacion;
import entities.PresupuestoContratacion;
import entities.ResumenContratacionServicio;
import entities.SesionUsuario;
import repositories.RepositoryContratacionServicio;
import repositories.RepositoryMovimientoFinanciero;
import repositories.RepositoryPropuestaServicio;
import repositories.RepositoryServicio;
import repositories.RepositorySolicitudEjecucion;
import repositories.RepositorySolicitudServicio;
import services.ServiceContratacionServicio;
import services.ServiceSolicitudes;

/**
 * CU-13 (contratar servicio) sobre H2 en memoria, recorriendo el flujo real:
 * solicitud (CU-12) -> propuesta -> seleccion -> contratacion.
 */
public class ServiceContratacionServicioH2Test {

    private static final String URL
            = "jdbc:h2:mem:cu13contratacion;DB_CLOSE_DELAY=-1";

    private static final int ORG = 9301;
    private static final int USUARIO_ORG = 9301;
    private static final int ORG_AJENO = 9302;
    private static final int USUARIO_ORG_AJENO = 9302;
    private static final int PROV_A = 9311;
    private static final int USUARIO_PROV_A = 9311;
    private static final int PROV_B = 9312;
    private static final int USUARIO_PROV_B = 9312;
    private static final int EVENTO = 9321;
    private static final int SERVICIO_A = 9331;
    private static final int SERVICIO_B = 9332;

    private final LocalDate dia = LocalDate.now().plusDays(30);

    private ServiceSolicitudes solicitudesOrg;
    private ServiceSolicitudes solicitudesProvA;
    private ServiceSolicitudes solicitudesProvB;
    private ServiceContratacionServicio contratacion;

    @BeforeEach
    void preparar() throws Exception {

        System.setProperty("nexteventux.db.url", URL);

        try (Connection c = DatabaseConnection.getConnection();
                Statement s = c.createStatement()) {

            s.executeUpdate("INSERT INTO Usuario (id_usuario, nombre, usuario, contraseña, estado, "
                    + "fecha_registro, acepta_marketing, tipo_usuario) VALUES "
                    + usuario(USUARIO_ORG, "Organizadora", "ORGANIZADOR") + ","
                    + usuario(USUARIO_ORG_AJENO, "Otro organizador", "ORGANIZADOR") + ","
                    + usuario(USUARIO_PROV_A, "Proveedor A", "PROVEEDOR") + ","
                    + usuario(USUARIO_PROV_B, "Proveedor B", "PROVEEDOR"));

            s.executeUpdate("INSERT INTO Organizador (id_organizador, id_usuario, empresa, estado) VALUES "
                    + "(" + ORG + "," + USUARIO_ORG + ",'Eventos','ACTIVO'),"
                    + "(" + ORG_AJENO + "," + USUARIO_ORG_AJENO + ",'Otros','ACTIVO')");

            s.executeUpdate("INSERT INTO Proveedor (id_proveedor, id_usuario, nombre, estado) VALUES "
                    + "(" + PROV_A + "," + USUARIO_PROV_A + ",'Proveedor A','APROBADO'),"
                    + "(" + PROV_B + "," + USUARIO_PROV_B + ",'Proveedor B','APROBADO')");

            s.executeUpdate("INSERT INTO Evento (id_evento, id_organizador, nombre, fecha_inicio, "
                    + "fecha_fin, estado, fecha_creacion, presupuesto) VALUES ("
                    + EVENTO + "," + ORG + ",'Boda de prueba',"
                    + "TIMESTAMP '" + dia + " 10:00:00', TIMESTAMP '" + dia + " 14:00:00',"
                    + "'PLANEADO', CURRENT_TIMESTAMP, 10000000)");

            s.executeUpdate("INSERT INTO Servicio (id_servicio, id_proveedor, nombre, precio, categoria, estado) VALUES "
                    + "(" + SERVICIO_A + "," + PROV_A + ",'Fotografia',1500000,'Fotografia','DISPONIBLE'),"
                    + "(" + SERVICIO_B + "," + PROV_B + ",'Fotografia B',1800000,'Fotografia','DISPONIBLE')");

            // Cada proveedor publica un horario que cubre el evento completo
            s.executeUpdate("INSERT INTO Disponibilidad_Servicio (id_servicio, fecha, hora_inicio, hora_fin, estado) VALUES "
                    + "(" + SERVICIO_A + ", DATE '" + dia + "', TIME '09:00:00', TIME '18:00:00', 'DISPONIBLE'),"
                    + "(" + SERVICIO_B + ", DATE '" + dia + "', TIME '08:00:00', TIME '20:00:00', 'DISPONIBLE')");
        }

        solicitudesOrg = solicitudes(sesion("ORGANIZADOR", ORG, USUARIO_ORG));
        solicitudesProvA = solicitudes(sesion("PROVEEDOR", PROV_A, USUARIO_PROV_A));
        solicitudesProvB = solicitudes(sesion("PROVEEDOR", PROV_B, USUARIO_PROV_B));
        contratacion = contratacion(sesion("ORGANIZADOR", ORG, USUARIO_ORG));
    }

    @AfterEach
    void limpiar() throws Exception {
        try {
            try (Connection c = DriverManager.getConnection(URL, "sa", "");
                    Statement s = c.createStatement()) {
                s.execute("DROP ALL OBJECTS");
            }
        } finally {
            System.clearProperty("nexteventux.db.url");
        }
    }

    // ------------------------------------------------------------------
    // Camino feliz
    // ------------------------------------------------------------------

    @Test
    void flujoBasicoCreaContratacionReservaHorarioYComprometePresupuesto() throws Exception {

        int propuesta = propuestaSeleccionada(solicitudesProvA, SERVICIO_A,
                new BigDecimal("2000000.00"), "Cobertura completa", "Pago 50% anticipado");

        ResumenContratacionServicio resumen = contratacion.consultarResumen(ORG, propuesta);
        assertEquals(propuesta, resumen.idPropuesta());
        assertEquals(1, resumen.numeroVersion());
        assertEquals(0, new BigDecimal("2000000").compareTo(resumen.valor()));
        assertEquals("Fotografia", resumen.nombreServicio());
        assertEquals("Proveedor A", resumen.nombreProveedor());
        assertEquals("Pago 50% anticipado", resumen.condiciones());

        // Verificar no reserva ni compromete nada (A3: abandonar la confirmación)
        DisponibilidadContratacion disponibilidad = contratacion.verificarDisponibilidad(ORG, propuesta);
        assertTrue(disponibilidad.disponible());
        PresupuestoContratacion presupuesto = contratacion.verificarPresupuesto(ORG, propuesta);
        assertTrue(presupuesto.suficiente());
        assertEquals(0, new BigDecimal("8000000").compareTo(presupuesto.disponibleDespues()));
        assertEquals(0, contar("SELECT COUNT(*) FROM Contratacion"));
        assertEquals("DISPONIBLE", texto("SELECT estado FROM Disponibilidad_Servicio WHERE id_servicio=" + SERVICIO_A));
        assertEquals("SELECCIONADA", estadoVisible(propuesta));

        ConfirmacionContratacion confirmacion = contratacion.contratar(ORG, propuesta);

        assertTrue(confirmacion.idContratacion() > 0);
        assertEquals(0, new BigDecimal("2000000").compareTo(confirmacion.valor()));
        assertEquals(dia, confirmacion.fechaServicio());

        // Contratación con la copia exacta de la propuesta
        assertEquals(1, contar("SELECT COUNT(*) FROM Contratacion"));
        assertEquals("ACTIVA", texto("SELECT estado FROM Contratacion"));
        assertEquals("Pago 50% anticipado", texto("SELECT condiciones FROM Contratacion"));
        assertEquals("Cobertura completa", texto("SELECT alcance_propuesta FROM Contratacion"));
        assertEquals(1, contar("SELECT numero_version_propuesta FROM Contratacion"));
        assertEquals(propuesta, contar("SELECT id_propuesta FROM Contratacion"));
        assertEquals(EVENTO, contar("SELECT id_evento FROM Contratacion"));
        assertEquals(0, new BigDecimal("2000000").compareTo(
                new BigDecimal(texto("SELECT valor_acordado FROM Contratacion"))));
        assertEquals("09:00:00", texto("SELECT hora_inicio FROM Contratacion"));
        assertEquals("18:00:00", texto("SELECT hora_fin FROM Contratacion"));

        // Reserva, estados y notificación
        assertEquals("RESERVADA", texto("SELECT estado FROM Disponibilidad_Servicio WHERE id_servicio=" + SERVICIO_A));
        assertEquals(contar("SELECT id_disponibilidad FROM Disponibilidad_Servicio WHERE id_servicio=" + SERVICIO_A),
                contar("SELECT id_disponibilidad FROM Contratacion"));
        assertEquals("CONTRATADA", texto("SELECT estado FROM Propuesta WHERE id_propuesta=" + propuesta));
        assertEquals("CONTRATADA", texto("SELECT estado FROM Solicitud_Ejecucion WHERE id_proveedor=" + PROV_A));
        assertEquals("CONTRATADA", texto("SELECT estado FROM Necesidad_Servicio_Evento"));
        assertEquals("CONTRATADA", estadoVisible(propuesta));
        assertEquals(1, contar("SELECT COUNT(*) FROM Notificacion_interna WHERE id_usuario=" + USUARIO_PROV_A
                + " AND mensaje LIKE '%fue contratada%'"));

        // El valor contratado reemplaza al costo estimado: se cuenta una sola vez
        BigDecimal comprometido = new RepositorySolicitudServicio().presupuesto(EVENTO, ORG).comprometido();
        assertEquals(0, new BigDecimal("2000000").compareTo(comprometido));
        var compromisos = new RepositoryMovimientoFinanciero().listarCompromisosPorEvento(EVENTO);
        assertEquals(1, compromisos.size());
        assertEquals(0, new BigDecimal("2000000").compareTo(compromisos.get(0).getValor()));
    }

    @Test
    void lasDemasPropuestasDeLaNecesidadQuedanNoSeleccionadasYConservanHistorial() throws Exception {

        int solicitudA = solicitudesOrg.crearSolicitud(ORG, EVENTO, SERVICIO_A, "Fotografia del evento");
        int necesidad = contar("SELECT id_necesidad_servicio FROM Solicitud_Ejecucion WHERE id_solicitud=" + solicitudA);
        int solicitudB = solicitudesOrg.crearSolicitudParaNecesidad(ORG, EVENTO, SERVICIO_B, necesidad);

        int propuestaA = solicitudesProvA.enviarPropuesta(PROV_A, solicitudA, new BigDecimal("2000000"), "Alcance A", "Condiciones A");
        int propuestaB = solicitudesProvB.enviarPropuesta(PROV_B, solicitudB, new BigDecimal("2500000"), "Alcance B", "Condiciones B");
        solicitudesOrg.seleccionarPropuesta(ORG, propuestaA);

        contratacion.contratar(ORG, propuestaA);

        assertEquals("NO_SELECCIONADA", texto("SELECT estado FROM Propuesta WHERE id_propuesta=" + propuestaB));
        assertEquals("NO_SELECCIONADA", texto("SELECT estado FROM Solicitud_Ejecucion WHERE id_solicitud=" + solicitudB));
        // No se borra nada: el historial sigue completo
        assertEquals(2, contar("SELECT COUNT(*) FROM Propuesta"));
        assertEquals(1, contar("SELECT COUNT(*) FROM Notificacion_interna WHERE id_usuario=" + USUARIO_PROV_B
                + " AND mensaje LIKE '%no seleccionada%'"));
        // El proveedor B sigue viendo su propuesta con el nuevo estado
        assertEquals("NO_SELECCIONADA",
                solicitudesProvB.listarHistorialPropuestas(PROV_B, solicitudB).get(0).getEstado());
        // Y el organizador también
        assertTrue(solicitudesOrg.listarPropuestasOrganizador(ORG).stream()
                .anyMatch(p -> p.getIdPropuesta() == propuestaB && "NO_SELECCIONADA".equals(p.getEstado())));
        // Las no seleccionadas ya no comprometen presupuesto
        assertEquals(0, new BigDecimal("2000000").compareTo(
                new RepositorySolicitudServicio().presupuesto(EVENTO, ORG).comprometido()));
        // Ya no se puede negociar ni contratar con la propuesta no seleccionada
        assertThrows(IllegalStateException.class,
                () -> solicitudesProvB.enviarPropuesta(PROV_B, solicitudB, new BigDecimal("1"), "d", "c"));
        assertThrows(IllegalStateException.class, () -> contratacion.contratar(ORG, propuestaB));
    }

    @Test
    void elPrecioFinalReemplazaElCostoEstimadoDeLaSolicitudAlComprobarPresupuesto() throws Exception {

        // Presupuesto 3.000.000: el costo estimado (1.500.000) más el precio final
        // (2.800.000) no cabría si se sumaran ambos, pero la contratación los sustituye.
        ejecutar("UPDATE Evento SET presupuesto=3000000 WHERE id_evento=" + EVENTO);

        int propuesta = propuestaSeleccionada(solicitudesProvA, SERVICIO_A,
                new BigDecimal("2800000"), "Alcance", "Condiciones");

        PresupuestoContratacion presupuesto = contratacion.verificarPresupuesto(ORG, propuesta);
        assertTrue(presupuesto.suficiente());
        assertEquals(0, new BigDecimal("200000").compareTo(presupuesto.disponibleDespues()));

        contratacion.contratar(ORG, propuesta);

        assertEquals(1, contar("SELECT COUNT(*) FROM Contratacion"));
    }

    // ------------------------------------------------------------------
    // A1: presupuesto insuficiente
    // ------------------------------------------------------------------

    @Test
    void presupuestoInsuficienteNoFormalizaYConservaLaPropuestaSeleccionada() throws Exception {

        int propuesta = propuestaSeleccionada(solicitudesProvA, SERVICIO_A,
                new BigDecimal("2000000"), "Alcance", "Condiciones");
        // El organizador baja el presupuesto después de seleccionar
        ejecutar("UPDATE Evento SET presupuesto=1700000 WHERE id_evento=" + EVENTO);

        PresupuestoContratacion presupuesto = contratacion.verificarPresupuesto(ORG, propuesta);
        assertFalse(presupuesto.suficiente());
        assertEquals(0, new BigDecimal("300000").compareTo(presupuesto.faltante()));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> contratacion.contratar(ORG, propuesta));
        assertTrue(error.getMessage().contains("Presupuesto insuficiente"), error.getMessage());
        assertTrue(error.getMessage().contains("$ 300.000 COP"), error.getMessage());

        assertEquals(0, contar("SELECT COUNT(*) FROM Contratacion"));
        assertEquals("DISPONIBLE", texto("SELECT estado FROM Disponibilidad_Servicio WHERE id_servicio=" + SERVICIO_A));
        assertEquals("SELECCIONADA", estadoVisible(propuesta));
        assertEquals("EN_NEGOCIACION", texto("SELECT estado FROM Solicitud_Ejecucion WHERE id_proveedor=" + PROV_A));

        // El organizador ajusta el presupuesto y puede contratar la misma propuesta
        ejecutar("UPDATE Evento SET presupuesto=2000000 WHERE id_evento=" + EVENTO);
        contratacion.contratar(ORG, propuesta);
        assertEquals(1, contar("SELECT COUNT(*) FROM Contratacion"));
    }

    @Test
    void otrasNecesidadesYContratacionesComprometenElPresupuestoDisponible() throws Exception {

        // Otra necesidad ya contratada por 8.500.000 deja 1.500.000 libres
        int primera = propuestaSeleccionada(solicitudesProvB, SERVICIO_B,
                new BigDecimal("8500000"), "Alcance B", "Condiciones B");
        contratacion.contratar(ORG, primera);

        int segunda = propuestaSeleccionada(solicitudesProvA, SERVICIO_A,
                new BigDecimal("2000000"), "Alcance A", "Condiciones A");

        PresupuestoContratacion presupuesto = contratacion.verificarPresupuesto(ORG, segunda);
        assertFalse(presupuesto.suficiente());
        assertEquals(0, new BigDecimal("500000").compareTo(presupuesto.faltante()));
        assertThrows(IllegalArgumentException.class, () -> contratacion.contratar(ORG, segunda));
        assertEquals(1, contar("SELECT COUNT(*) FROM Contratacion"));
    }

    @Test
    void eventoSinPresupuestoNoPermiteContratar() throws Exception {

        int propuesta = propuestaSeleccionada(solicitudesProvA, SERVICIO_A,
                new BigDecimal("2000000"), "Alcance", "Condiciones");
        ejecutar("UPDATE Evento SET presupuesto=NULL WHERE id_evento=" + EVENTO);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> contratacion.contratar(ORG, propuesta));
        assertTrue(error.getMessage().contains("presupuesto"), error.getMessage());
        assertEquals(0, contar("SELECT COUNT(*) FROM Contratacion"));
    }

    // ------------------------------------------------------------------
    // A2: el proveedor ya no está disponible
    // ------------------------------------------------------------------

    @Test
    void proveedorOcupadoBloqueaLaFormalizacionYLaPropuestaNoSeBorra() throws Exception {

        int propuesta = propuestaSeleccionada(solicitudesProvA, SERVICIO_A,
                new BigDecimal("2000000"), "Alcance", "Condiciones");
        // Otro evento ocupa el horario del proveedor antes de contratar
        ejecutar("UPDATE Disponibilidad_Servicio SET estado='RESERVADA' WHERE id_servicio=" + SERVICIO_A);

        DisponibilidadContratacion disponibilidad = contratacion.verificarDisponibilidad(ORG, propuesta);
        assertFalse(disponibilidad.disponible());

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> contratacion.contratar(ORG, propuesta));
        assertTrue(error.getMessage().contains("disponible"), error.getMessage());

        assertEquals(0, contar("SELECT COUNT(*) FROM Contratacion"));
        assertEquals("SELECCIONADA", estadoVisible(propuesta));
        assertEquals(1, contar("SELECT COUNT(*) FROM Propuesta"));
    }

    @Test
    void contratacionConfirmadaQueSeCruzaImpideContratar() throws Exception {

        int propuesta = propuestaSeleccionada(solicitudesProvA, SERVICIO_A,
                new BigDecimal("2000000"), "Alcance", "Condiciones");
        crearContratacionExistente(SERVICIO_A, "11:00:00", "12:00:00");

        DisponibilidadContratacion disponibilidad = contratacion.verificarDisponibilidad(ORG, propuesta);
        assertFalse(disponibilidad.disponible());
        assertThrows(IllegalStateException.class, () -> contratacion.contratar(ORG, propuesta));
        assertEquals(1, contar("SELECT COUNT(*) FROM Contratacion"));
    }

    @Test
    void horarioQueNoCubreElEventoCompletoNoCuenta() throws Exception {

        ejecutar("UPDATE Disponibilidad_Servicio SET hora_fin=TIME '12:00:00' WHERE id_servicio=" + SERVICIO_A);
        int propuesta = propuestaSeleccionada(solicitudesProvA, SERVICIO_A,
                new BigDecimal("2000000"), "Alcance", "Condiciones");

        assertFalse(contratacion.verificarDisponibilidad(ORG, propuesta).disponible());
        assertThrows(IllegalStateException.class, () -> contratacion.contratar(ORG, propuesta));
    }

    @Test
    void eventoDeVariosDiasNoSePuedeReservarConUnaDisponibilidadDiaria() throws Exception {

        ejecutar("UPDATE Evento SET fecha_fin=TIMESTAMP '" + dia.plusDays(1) + " 14:00:00' WHERE id_evento=" + EVENTO);
        int propuesta = propuestaSeleccionada(solicitudesProvA, SERVICIO_A,
                new BigDecimal("2000000"), "Alcance", "Condiciones");

        DisponibilidadContratacion disponibilidad = contratacion.verificarDisponibilidad(ORG, propuesta);
        assertFalse(disponibilidad.disponible());
        assertTrue(disponibilidad.mensaje().contains("más de un día"), disponibilidad.mensaje());
    }

    // ------------------------------------------------------------------
    // E1 y E2
    // ------------------------------------------------------------------

    @Test
    void necesidadYaCubiertaImpideUnaSegundaContratacion() throws Exception {

        int propuesta = propuestaSeleccionada(solicitudesProvA, SERVICIO_A,
                new BigDecimal("2000000"), "Alcance", "Condiciones");
        contratacion.contratar(ORG, propuesta);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> contratacion.contratar(ORG, propuesta));
        assertTrue(error.getMessage().contains("ya está cubierta"), error.getMessage());
        assertEquals(1, contar("SELECT COUNT(*) FROM Contratacion"));
        assertEquals("RESERVADA", texto("SELECT estado FROM Disponibilidad_Servicio WHERE id_servicio=" + SERVICIO_A));
    }

    @Test
    void propuestaSinSeleccionarNoSePuedeFormalizar() throws Exception {

        int solicitud = solicitudesOrg.crearSolicitud(ORG, EVENTO, SERVICIO_A, "Fotografia");
        int propuesta = solicitudesProvA.enviarPropuesta(PROV_A, solicitud,
                new BigDecimal("2000000"), "Alcance", "Condiciones");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> contratacion.contratar(ORG, propuesta));
        assertTrue(error.getMessage().contains("seleccionada"), error.getMessage());
        assertThrows(IllegalStateException.class, () -> contratacion.consultarResumen(ORG, propuesta));
        assertEquals(0, contar("SELECT COUNT(*) FROM Contratacion"));
    }

    @Test
    void soloSeContrataLaVersionVigenteSeleccionada() throws Exception {

        int solicitud = solicitudesOrg.crearSolicitud(ORG, EVENTO, SERVICIO_A, "Fotografia");
        int version1 = solicitudesProvA.enviarPropuesta(PROV_A, solicitud,
                new BigDecimal("2000000"), "Alcance v1", "Condiciones v1");
        int version2 = solicitudesProvA.enviarPropuesta(PROV_A, solicitud,
                new BigDecimal("1800000"), "Alcance v2", "Condiciones v2");
        solicitudesOrg.seleccionarPropuesta(ORG, version2);

        // La versión 1 ya no es la vigente ni la seleccionada
        assertThrows(IllegalStateException.class, () -> contratacion.contratar(ORG, version1));

        ConfirmacionContratacion confirmacion = contratacion.contratar(ORG, version2);
        assertEquals(2, confirmacion.numeroVersion());
        assertEquals("Alcance v2", texto("SELECT alcance_propuesta FROM Contratacion"));
        assertEquals("Condiciones v2", texto("SELECT condiciones FROM Contratacion"));
        // La versión anterior se conserva intacta
        assertEquals("ENVIADA", texto("SELECT estado FROM Propuesta WHERE id_propuesta=" + version1));
    }

    // ------------------------------------------------------------------
    // Precondiciones y seguridad
    // ------------------------------------------------------------------

    @Test
    void soloElOrganizadorDuenoDelEventoPuedeContratar() throws Exception {

        int propuesta = propuestaSeleccionada(solicitudesProvA, SERVICIO_A,
                new BigDecimal("2000000"), "Alcance", "Condiciones");

        // Otro organizador con sesión propia
        ServiceContratacionServicio ajeno = contratacion(sesion("ORGANIZADOR", ORG_AJENO, USUARIO_ORG_AJENO));
        assertThrows(IllegalStateException.class, () -> ajeno.contratar(ORG_AJENO, propuesta));
        assertThrows(IllegalStateException.class, () -> ajeno.consultarResumen(ORG_AJENO, propuesta));

        // Suplantar al organizador legítimo desde otra sesión
        assertThrows(IllegalStateException.class, () -> ajeno.contratar(ORG, propuesta));

        // Un proveedor no puede contratar
        ServiceContratacionServicio comoProveedor = contratacion(sesion("PROVEEDOR", PROV_A, USUARIO_PROV_A));
        assertThrows(IllegalStateException.class, () -> comoProveedor.contratar(PROV_A, propuesta));

        assertEquals(0, contar("SELECT COUNT(*) FROM Contratacion"));
    }

    @Test
    void identificadoresYSesionInvalidosSeRechazan() {

        assertThrows(IllegalArgumentException.class, () -> contratacion.contratar(0, 1));
        assertThrows(IllegalArgumentException.class, () -> contratacion.contratar(ORG, 0));
        assertThrows(IllegalArgumentException.class, () -> contratacion.contratar(-1, -1));
        assertThrows(IllegalStateException.class, () -> contratacion.contratar(ORG, 987654));
        assertThrows(IllegalArgumentException.class,
                () -> new ServiceContratacionServicio(new RepositoryContratacionServicio(), null));

        SesionUsuario inactiva = sesion("ORGANIZADOR", ORG, USUARIO_ORG);
        inactiva.setEstadoActor("SUSPENDIDO");
        assertThrows(IllegalStateException.class, () -> contratacion(inactiva).contratar(ORG, 1));

        SesionUsuario cerrada = sesion("ORGANIZADOR", ORG, USUARIO_ORG);
        cerrada.cerrar();
        assertThrows(IllegalStateException.class, () -> contratacion(cerrada).contratar(ORG, 1));
    }

    @Test
    void eventoCanceladoOServicioRetiradoOProveedorNoVerificadoBloquean() throws Exception {

        int propuesta = propuestaSeleccionada(solicitudesProvA, SERVICIO_A,
                new BigDecimal("2000000"), "Alcance", "Condiciones");

        ejecutar("UPDATE Servicio SET estado='RETIRADO' WHERE id_servicio=" + SERVICIO_A);
        assertThrows(IllegalStateException.class, () -> contratacion.contratar(ORG, propuesta));
        ejecutar("UPDATE Servicio SET estado='DISPONIBLE' WHERE id_servicio=" + SERVICIO_A);

        ejecutar("UPDATE Proveedor SET estado='SUSPENDIDO' WHERE id_proveedor=" + PROV_A);
        assertThrows(IllegalStateException.class, () -> contratacion.contratar(ORG, propuesta));
        ejecutar("UPDATE Proveedor SET estado='APROBADO' WHERE id_proveedor=" + PROV_A);

        ejecutar("UPDATE Evento SET estado='CANCELADO' WHERE id_evento=" + EVENTO);
        assertThrows(IllegalStateException.class, () -> contratacion.contratar(ORG, propuesta));
        ejecutar("UPDATE Evento SET estado='PLANEADO' WHERE id_evento=" + EVENTO);

        assertEquals(0, contar("SELECT COUNT(*) FROM Contratacion"));
        // Con todo restablecido sí se puede contratar
        contratacion.contratar(ORG, propuesta);
        assertEquals(1, contar("SELECT COUNT(*) FROM Contratacion"));
    }

    // ------------------------------------------------------------------
    // Integridad de la base de datos
    // ------------------------------------------------------------------

    @Test
    void laBaseDeDatosImpideDosContratacionesParaLaMismaPropuestaONecesidad() throws Exception {

        int propuesta = propuestaSeleccionada(solicitudesProvA, SERVICIO_A,
                new BigDecimal("2000000"), "Alcance", "Condiciones");
        contratacion.contratar(ORG, propuesta);
        int necesidad = contar("SELECT id_necesidad_servicio FROM Contratacion");

        assertThrows(SQLException.class, () -> ejecutar(
                "INSERT INTO Contratacion (id_propuesta, id_evento, fecha_contratacion, estado) "
                + "VALUES (" + propuesta + "," + EVENTO + ", CURRENT_TIMESTAMP, 'ACTIVA')"));

        assertThrows(SQLException.class, () -> ejecutar(
                "INSERT INTO Contratacion (id_propuesta, id_evento, fecha_contratacion, estado, id_necesidad_servicio) "
                + "VALUES (" + (propuesta + 1000) + "," + EVENTO + ", CURRENT_TIMESTAMP, 'ACTIVA', " + necesidad + ")"));
    }

    @Test
    void lasCondicionesDeMilCaracteresSeCopianCompletas() throws Exception {

        String condiciones = "c".repeat(1000);
        int propuesta = propuestaSeleccionada(solicitudesProvA, SERVICIO_A,
                new BigDecimal("2000000"), "a".repeat(255), condiciones);

        contratacion.contratar(ORG, propuesta);

        assertEquals(condiciones, texto("SELECT condiciones FROM Contratacion"));
        assertEquals("a".repeat(255), texto("SELECT alcance_propuesta FROM Contratacion"));
    }

    @Test
    void noSePuedeRetirarUnServicioConContratacionVigente() throws Exception {

        int propuesta = propuestaSeleccionada(solicitudesProvA, SERVICIO_A,
                new BigDecimal("2000000"), "Alcance", "Condiciones");
        contratacion.contratar(ORG, propuesta);

        services.ServiceProveedores proveedores = new services.ServiceProveedores(
                new RepositoryServicio(), new repositories.RepositoryDisponibilidadServicio());

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> proveedores.retirarServicio(PROV_A, SERVICIO_A));
        assertTrue(error.getMessage().contains("contrataciones"), error.getMessage());
        assertEquals("DISPONIBLE", texto("SELECT estado FROM Servicio WHERE id_servicio=" + SERVICIO_A));
    }

    @Test
    void cancelarElEventoLiberaElHorarioYCancelaLaContratacion() throws Exception {

        int propuesta = propuestaSeleccionada(solicitudesProvA, SERVICIO_A,
                new BigDecimal("2000000"), "Alcance", "Condiciones");
        contratacion.contratar(ORG, propuesta);

        new repositories.RepositoryCancelacion().cancelar(EVENTO, ORG);

        assertEquals("CANCELADA", texto("SELECT estado FROM Contratacion"));
        assertEquals("DISPONIBLE", texto("SELECT estado FROM Disponibilidad_Servicio WHERE id_servicio=" + SERVICIO_A));
    }

    // ------------------------------------------------------------------
    // Concurrencia
    // ------------------------------------------------------------------

    @Test
    void dosConfirmacionesSimultaneasSoloFormalizanUnaContratacion() throws Exception {

        int propuesta = propuestaSeleccionada(solicitudesProvA, SERVICIO_A,
                new BigDecimal("2000000"), "Alcance", "Condiciones");

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> tarea = () -> {
                try {
                    contratacion.contratar(ORG, propuesta);
                    return true;
                } catch (RuntimeException e) {
                    return false;
                }
            };

            Future<Boolean> primera = pool.submit(tarea);
            Future<Boolean> segunda = pool.submit(tarea);

            int exitos = (primera.get() ? 1 : 0) + (segunda.get() ? 1 : 0);
            assertEquals(1, exitos);
        } finally {
            pool.shutdownNow();
        }

        assertEquals(1, contar("SELECT COUNT(*) FROM Contratacion"));
        assertEquals("RESERVADA", texto("SELECT estado FROM Disponibilidad_Servicio WHERE id_servicio=" + SERVICIO_A));
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private int propuestaSeleccionada(
            ServiceSolicitudes proveedor,
            int servicio,
            BigDecimal valor,
            String alcance,
            String condiciones) {

        int idProveedor = servicio == SERVICIO_A ? PROV_A : PROV_B;
        int solicitud = solicitudesOrg.crearSolicitud(ORG, EVENTO, servicio, "Necesidad de prueba");
        int propuesta = proveedor.enviarPropuesta(idProveedor, solicitud, valor, alcance, condiciones);
        solicitudesOrg.seleccionarPropuesta(ORG, propuesta);
        return propuesta;
    }

    private String estadoVisible(int propuesta) {
        List<entities.PropuestaServicio> propuestas = solicitudesOrg.listarPropuestasOrganizador(ORG);
        return propuestas.stream()
                .filter(p -> p.getIdPropuesta() == propuesta)
                .findFirst()
                .map(entities.PropuestaServicio::getEstado)
                .orElse(null);
    }

    private void crearContratacionExistente(int servicio, String inicio, String fin) throws Exception {

        // Solicitud histórica de otro evento del mismo proveedor con contratación confirmada
        ejecutar("INSERT INTO Evento (id_evento, id_organizador, nombre, fecha_inicio, fecha_fin, estado, "
                + "fecha_creacion, presupuesto) VALUES (9399," + ORG_AJENO + ",'Otro evento',"
                + "TIMESTAMP '" + dia + " 11:00:00', TIMESTAMP '" + dia + " 12:00:00',"
                + "'PLANEADO', CURRENT_TIMESTAMP, 5000000)");
        ejecutar("INSERT INTO Solicitud_Ejecucion (id_solicitud, id_proveedor, id_evento, id_servicio, "
                + "fecha_solicitud, estado) VALUES (9398," + PROV_A + ",9399," + servicio
                + ", CURRENT_TIMESTAMP, 'CONTRATADA')");
        ejecutar("INSERT INTO Propuesta (id_propuesta, id_solicitud, valor, descripcion, estado) "
                + "VALUES (9397, 9398, 500000, 'Historica', 'CONTRATADA')");
        ejecutar("INSERT INTO Contratacion (id_propuesta, id_evento, fecha_contratacion, estado, "
                + "valor_acordado, fecha_servicio, hora_inicio, hora_fin) VALUES (9397, 9399, "
                + "CURRENT_TIMESTAMP, 'CONFIRMADA', 500000, DATE '" + dia + "', TIME '" + inicio
                + "', TIME '" + fin + "')");
    }

    private ServiceSolicitudes solicitudes(SesionUsuario sesion) {
        return new ServiceSolicitudes(
                new RepositorySolicitudEjecucion(),
                new RepositoryPropuestaServicio(),
                sesion);
    }

    private ServiceContratacionServicio contratacion(SesionUsuario sesion) {
        return new ServiceContratacionServicio(
                new RepositoryContratacionServicio(), sesion);
    }

    private SesionUsuario sesion(String rol, int actor, int usuario) {
        SesionUsuario sesion = new SesionUsuario();
        sesion.setTipoUsuario(rol);
        sesion.setIdActor(actor);
        sesion.setIdUsuario(usuario);
        sesion.setEstadoUsuario("ACTIVO");
        sesion.setEstadoActor("PROVEEDOR".equals(rol) ? "APROBADO" : "ACTIVO");
        return sesion;
    }

    private String usuario(int id, String nombre, String tipo) {
        return "(" + id + ",'" + nombre + "','u" + id + "','x','ACTIVO',CURRENT_TIMESTAMP,FALSE,'" + tipo + "')";
    }

    private void ejecutar(String sql) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
                Statement s = c.createStatement()) {
            s.execute(sql);
        }
    }

    private String texto(String sql) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
                Statement s = c.createStatement();
                ResultSet r = s.executeQuery(sql)) {
            return r.next() ? r.getString(1) : null;
        }
    }

    private int contar(String sql) throws SQLException {
        String valor = texto(sql);
        return valor == null ? 0 : Integer.parseInt(valor);
    }
}

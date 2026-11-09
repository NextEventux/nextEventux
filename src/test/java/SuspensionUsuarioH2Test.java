import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.h2.tools.RunScript;
import org.junit.jupiter.api.Test;

import inicializacion.CargarDatosEjemplo;
import persistencia.DatabaseConnection;
import entities.SesionUsuario;
import repositories.RepositoryAdministracion;
import repositories.RepositoryAdministracion.UsuarioAdministrable;
import repositories.RepositoryAutenticacion;
import repositories.RepositoryContratacion;
import repositories.RepositoryOrganizador;
import repositories.proveedores.RepositorySancionesProveedor;
import services.ServiceAdministracion;
import services.ServiceAutenticacion;
import services.proveedores.ServiceSancionesProveedor;
import services.proveedores.ServiceSancionesProveedor.SolicitudSancion;

/**
 * CU-029 (suspender usuario) y su ampliacion (reactivar usuario) sobre H2 en
 * memoria con los datos del seed:
 * administradores 14 (camila), 15 (mateo) y 16 (shirly); cliente 5
 * (ana.martinez); proveedor 1 = usuario 3; organizador 1 = usuario 1.
 * Todas las cuentas del seed usan la clave 1234.
 */
class SuspensionUsuarioH2Test {

    private static final int ADMIN_CAMILA = 14;
    private static final int ADMIN_MATEO = 15;
    private static final int ADMIN_SHIRLY = 16;
    private static final int CLIENTE_ANA = 5;
    private static final int USUARIO_PROVEEDOR = 3;
    private static final int USUARIO_ORGANIZADOR = 1;

    private String url;
    private ServiceAdministracion servicio;

    @BeforeEach
    void preparar() throws Exception {
        url = "jdbc:h2:mem:cu29_" + UUID.randomUUID().toString().replace("-", "")
                + ";DB_CLOSE_DELAY=-1";
        try (Connection c = DatabaseConnection.getConnection(url)) {
            CargarDatosEjemplo.cargar(c);
        }
        servicio = new ServiceAdministracion(
                new RepositoryContratacion(url),
                new RepositoryOrganizador(url),
                new RepositoryAdministracion(url));
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    static SesionUsuario sesion(int idUsuario, String tipo, String estado) {
        SesionUsuario sesion = new SesionUsuario();
        sesion.setIdUsuario(idUsuario);
        sesion.setTipoUsuario(tipo);
        sesion.setEstadoUsuario(estado);
        return sesion;
    }

    static SesionUsuario admin(int idUsuario) {
        return sesion(idUsuario, "ADMINISTRADOR", "ACTIVO");
    }

    private void sql(String sentencia) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection(url);
             Statement s = c.createStatement()) {
            s.execute(sentencia);
        }
    }

    private int contar(String consulta) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection(url);
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery(consulta)) {
            r.next();
            return r.getInt(1);
        }
    }

    private String texto(String consulta) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection(url);
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery(consulta)) {
            r.next();
            return r.getString(1);
        }
    }

    private String estado(int idUsuario) throws SQLException {
        return texto("SELECT estado FROM Usuario WHERE id_usuario = " + idUsuario);
    }

    private int suspensiones(int idUsuario) throws SQLException {
        return contar("SELECT COUNT(*) FROM Historial_suspension_usuario WHERE id_usuario = " + idUsuario);
    }

    private int vigentes(int idUsuario) throws SQLException {
        return contar("SELECT COUNT(*) FROM Historial_suspension_usuario WHERE id_usuario = "
                + idUsuario + " AND fecha_reactivacion IS NULL");
    }

    private int notificaciones(int idUsuario) throws SQLException {
        return contar("SELECT COUNT(*) FROM Notificacion_interna WHERE id_usuario = " + idUsuario);
    }

    private void suspenderAna() {
        servicio.suspenderUsuario(admin(ADMIN_CAMILA), CLIENTE_ANA, "Incumplió las normas de uso");
    }

    // ------------------------------------------------------------------
    // E0: esquema
    // ------------------------------------------------------------------

    @Test
    void e0EsquemaPermiteUnaSolaSuspensionVigentePorUsuario() throws Exception {
        sql("INSERT INTO Historial_suspension_usuario (id_usuario, id_administrador, motivo) VALUES (5, 14, 'Primera')");

        SQLException duplicada = assertThrows(SQLException.class,
                () -> sql("INSERT INTO Historial_suspension_usuario (id_usuario, id_administrador, motivo) "
                        + "VALUES (5, 14, 'Segunda')"));
        assertEquals(23505, duplicada.getErrorCode());

        sql("UPDATE Historial_suspension_usuario SET id_administrador_reactivacion = 14, "
                + "fecha_reactivacion = CURRENT_TIMESTAMP WHERE id_usuario = 5");
        sql("INSERT INTO Historial_suspension_usuario (id_usuario, id_administrador, motivo, "
                + "id_administrador_reactivacion, fecha_reactivacion) "
                + "VALUES (5, 14, 'Cerrada', 14, CURRENT_TIMESTAMP)");
        sql("INSERT INTO Historial_suspension_usuario (id_usuario, id_administrador, motivo) VALUES (5, 14, 'Nueva')");

        assertEquals(3, suspensiones(CLIENTE_ANA));
        assertEquals(1, vigentes(CLIENTE_ANA));
        assertThrows(SQLException.class,
                () -> sql("INSERT INTO Historial_suspension_usuario (id_usuario, id_administrador, motivo) "
                        + "VALUES (6, 14, '   ')"));
        assertThrows(SQLException.class,
                () -> sql("INSERT INTO Historial_suspension_usuario (id_usuario, id_administrador, motivo, "
                        + "fecha_reactivacion) VALUES (6, 14, 'Incompleta', CURRENT_TIMESTAMP)"));
    }

    // ------------------------------------------------------------------
    // S: suspender (flujo basico, A2, E1, E2)
    // ------------------------------------------------------------------

    @Test
    void s1SuspenderCambiaEstadoRegistraMotivoYNotifica() throws Exception {
        int avisosAntes = notificaciones(CLIENTE_ANA);

        assertTrue(servicio.validarSuspension(admin(ADMIN_CAMILA), CLIENTE_ANA, null));
        assertTrue(servicio.validarSuspension(admin(ADMIN_CAMILA), CLIENTE_ANA, "Motivo válido"));
        servicio.suspenderUsuario(admin(ADMIN_CAMILA), CLIENTE_ANA, "  Incumplió   las normas de uso ");

        assertEquals("SUSPENDIDO", estado(CLIENTE_ANA));
        assertEquals(1, vigentes(CLIENTE_ANA));
        assertEquals("Incumplió las normas de uso",
                texto("SELECT motivo FROM Historial_suspension_usuario WHERE id_usuario = 5"));
        assertEquals(ADMIN_CAMILA,
                contar("SELECT id_administrador FROM Historial_suspension_usuario WHERE id_usuario = 5"));
        assertEquals(avisosAntes + 1, notificaciones(CLIENTE_ANA));
        assertTrue(texto("SELECT mensaje FROM Notificacion_interna WHERE id_usuario = 5 "
                + "ORDER BY id_notificacion DESC FETCH FIRST 1 ROWS ONLY")
                .contains("Incumplió las normas de uso"));
    }

    @Test
    void s2CuentaYaSuspendidaNoCambia() throws Exception {
        suspenderAna();
        int avisos = notificaciones(CLIENTE_ANA);

        IllegalStateException previa = assertThrows(IllegalStateException.class,
                () -> servicio.validarSuspension(admin(ADMIN_MATEO), CLIENTE_ANA, null));
        assertEquals(ServiceAdministracion.YA_SUSPENDIDA, previa.getMessage());

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> servicio.suspenderUsuario(admin(ADMIN_MATEO), CLIENTE_ANA, "Otro motivo"));
        assertEquals(ServiceAdministracion.YA_SUSPENDIDA, error.getMessage());
        assertEquals(1, suspensiones(CLIENTE_ANA));
        assertEquals(avisos, notificaciones(CLIENTE_ANA));
    }

    @Test
    void s3MotivoVacioOLargoSeRechazaSinCambios() throws Exception {
        for (String motivo : new String[] {null, "", "   ", "\n\t"}) {
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> servicio.suspenderUsuario(admin(ADMIN_CAMILA), CLIENTE_ANA, motivo));
            assertEquals(ServiceAdministracion.MOTIVO_OBLIGATORIO, error.getMessage());
        }
        assertThrows(IllegalArgumentException.class,
                () -> servicio.validarSuspension(admin(ADMIN_CAMILA), CLIENTE_ANA, "  "));

        String largo = "x".repeat(ServiceAdministracion.MAX_MOTIVO + 1);
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> servicio.suspenderUsuario(admin(ADMIN_CAMILA), CLIENTE_ANA, largo));
        assertEquals(ServiceAdministracion.MOTIVO_LARGO, error.getMessage());

        assertEquals("ACTIVO", estado(CLIENTE_ANA));
        assertEquals(0, suspensiones(CLIENTE_ANA));

        servicio.suspenderUsuario(admin(ADMIN_CAMILA), CLIENTE_ANA,
                "y".repeat(ServiceAdministracion.MAX_MOTIVO));
        assertEquals("SUSPENDIDO", estado(CLIENTE_ANA));
    }

    @Test
    void s4ErrorAlNotificarRevierteTodoYPermiteReintentar() throws Exception {
        int avisos = notificaciones(CLIENTE_ANA);
        sql("ALTER TABLE Notificacion_interna ADD CONSTRAINT fallo_cu29 CHECK (id_usuario IS NULL OR id_usuario <> 5)");
        try {
            IllegalStateException error = assertThrows(IllegalStateException.class,
                    this::suspenderAna);
            assertEquals(ServiceAdministracion.ERROR_SUSPENSION, error.getMessage());
            assertEquals("ACTIVO", estado(CLIENTE_ANA));
            assertEquals(0, suspensiones(CLIENTE_ANA));
            assertEquals(avisos, notificaciones(CLIENTE_ANA));
        } finally {
            sql("ALTER TABLE Notificacion_interna DROP CONSTRAINT fallo_cu29");
        }

        suspenderAna();
        assertEquals("SUSPENDIDO", estado(CLIENTE_ANA));
        assertEquals(1, vigentes(CLIENTE_ANA));
    }

    @Test
    void s5ErrorAlRegistrarHistorialRevierteEstadoYNotificacion() throws Exception {
        int avisos = notificaciones(CLIENTE_ANA);
        sql("ALTER TABLE Historial_suspension_usuario ADD CONSTRAINT fallo_historial CHECK (id_usuario <> 5)");
        try {
            IllegalStateException error = assertThrows(IllegalStateException.class,
                    this::suspenderAna);
            assertEquals(ServiceAdministracion.ERROR_SUSPENSION, error.getMessage());
        } finally {
            sql("ALTER TABLE Historial_suspension_usuario DROP CONSTRAINT fallo_historial");
        }
        assertEquals("ACTIVO", estado(CLIENTE_ANA));
        assertEquals(avisos, notificaciones(CLIENTE_ANA));
    }

    @Test
    void s6UsuarioInexistenteOIdInvalido() {
        IllegalArgumentException inexistente = assertThrows(IllegalArgumentException.class,
                () -> servicio.suspenderUsuario(admin(ADMIN_CAMILA), 999999, "Motivo válido"));
        assertEquals(ServiceAdministracion.USUARIO_INEXISTENTE, inexistente.getMessage());

        for (int id : new int[] {0, -3}) {
            IllegalArgumentException invalido = assertThrows(IllegalArgumentException.class,
                    () -> servicio.suspenderUsuario(admin(ADMIN_CAMILA), id, "Motivo válido"));
            assertEquals(ServiceAdministracion.USUARIO_INVALIDO, invalido.getMessage());
        }
        assertThrows(IllegalArgumentException.class,
                () -> servicio.consultarUsuario(admin(ADMIN_CAMILA), 999999));
    }

    @Test
    void s7CuentaInactivaNoSePuedeSuspender() throws Exception {
        sql("UPDATE Usuario SET estado = 'INACTIVO' WHERE id_usuario = 5");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                this::suspenderAna);
        assertEquals(ServiceAdministracion.NO_ACTIVA, error.getMessage());
        assertEquals("INACTIVO", estado(CLIENTE_ANA));
        assertEquals(0, suspensiones(CLIENTE_ANA));
    }

    // ------------------------------------------------------------------
    // P: permisos y sesion administrativa desactualizada
    // ------------------------------------------------------------------

    @Test
    void p1SoloUnAdministradorPuedeOperar() throws Exception {
        SesionUsuario[] noAutorizadas = {
            null,
            sesion(USUARIO_ORGANIZADOR, "ORGANIZADOR", "ACTIVO"),
            sesion(CLIENTE_ANA, "CLIENTE", "ACTIVO"),
            sesion(ADMIN_CAMILA, "ADMINISTRADOR", "SUSPENDIDO")
        };
        for (SesionUsuario s : noAutorizadas) {
            assertThrows(SecurityException.class,
                    () -> servicio.suspenderUsuario(s, 6, "Motivo válido"));
            assertThrows(SecurityException.class, () -> servicio.reactivarUsuario(s, 6));
            assertThrows(SecurityException.class, () -> servicio.consultarUsuarios(s, ""));
        }
        assertEquals("ACTIVO", estado(6));
    }

    @Test
    void p2AdministradorSuspendidoEnLaBaseNoPuedeOperar() throws Exception {
        sql("UPDATE Usuario SET estado = 'SUSPENDIDO' WHERE id_usuario = 14");

        SecurityException error = assertThrows(SecurityException.class, this::suspenderAna);
        assertEquals(ServiceAdministracion.SESION_ADMIN_INVALIDA, error.getMessage());
        assertEquals("ACTIVO", estado(CLIENTE_ANA));
        assertEquals(0, suspensiones(CLIENTE_ANA));
    }

    @Test
    void p3OperadorQueYaNoEsAdministradorEnLaBaseNoPuedeOperar() throws Exception {
        sql("UPDATE Usuario SET tipo_usuario = 'CLIENTE' WHERE id_usuario = 14");

        assertThrows(SecurityException.class, this::suspenderAna);
        assertEquals("ACTIVO", estado(CLIENTE_ANA));
    }

    @Test
    void p4OperadorInexistenteNoPuedeOperar() throws Exception {
        assertThrows(SecurityException.class,
                () -> servicio.suspenderUsuario(admin(987654), CLIENTE_ANA, "Motivo válido"));
        assertEquals("ACTIVO", estado(CLIENTE_ANA));
    }

    @Test
    void p5OperadorDesactualizadoNoPuedeConsultar() throws Exception {
        sql("UPDATE Usuario SET estado = 'SUSPENDIDO' WHERE id_usuario = 14");

        assertThrows(SecurityException.class,
                () -> servicio.consultarUsuarios(admin(ADMIN_CAMILA), ""));
        assertThrows(SecurityException.class,
                () -> servicio.consultarUsuario(admin(ADMIN_CAMILA), CLIENTE_ANA));
        assertThrows(SecurityException.class,
                () -> servicio.validarSuspension(admin(ADMIN_CAMILA), CLIENTE_ANA, null));
        assertThrows(SecurityException.class,
                () -> servicio.validarReactivacion(admin(ADMIN_CAMILA), CLIENTE_ANA));
    }

    // ------------------------------------------------------------------
    // AD: autosuspension y ultimo administrador
    // ------------------------------------------------------------------

    @Test
    void ad1NoSePuedeSuspenderLaPropiaCuenta() throws Exception {
        IllegalStateException previa = assertThrows(IllegalStateException.class,
                () -> servicio.validarSuspension(admin(ADMIN_CAMILA), ADMIN_CAMILA, null));
        assertEquals(ServiceAdministracion.AUTOSUSPENSION, previa.getMessage());

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> servicio.suspenderUsuario(admin(ADMIN_CAMILA), ADMIN_CAMILA, "Prueba"));
        assertEquals(ServiceAdministracion.AUTOSUSPENSION, error.getMessage());
        assertEquals("ACTIVO", estado(ADMIN_CAMILA));
    }

    @Test
    void ad2ConDosAdministradoresActivosUnoPuedeSuspenderAlOtro() throws Exception {
        sql("UPDATE Usuario SET estado = 'INACTIVO' WHERE id_usuario = 16");

        servicio.suspenderUsuario(admin(ADMIN_CAMILA), ADMIN_MATEO, "Acceso indebido");

        assertEquals("SUSPENDIDO", estado(ADMIN_MATEO));
        assertEquals(1, contar("SELECT COUNT(*) FROM Usuario WHERE tipo_usuario = 'ADMINISTRADOR' "
                + "AND estado = 'ACTIVO'"));
    }

    @Test
    void ad3ConTresAdministradoresSePuedeSuspenderAUno() throws Exception {
        servicio.suspenderUsuario(admin(ADMIN_CAMILA), ADMIN_SHIRLY, "Cuenta comprometida");

        assertEquals("SUSPENDIDO", estado(ADMIN_SHIRLY));
        assertEquals(2, contar("SELECT COUNT(*) FROM Usuario WHERE tipo_usuario = 'ADMINISTRADOR' "
                + "AND estado = 'ACTIVO'"));
    }

    @Test
    void ad4NuncaQuedanCeroAdministradoresActivos() throws Exception {
        sql("UPDATE Usuario SET estado = 'INACTIVO' WHERE id_usuario = 16");
        servicio.suspenderUsuario(admin(ADMIN_CAMILA), ADMIN_MATEO, "Acceso indebido");

        // Mateo conserva una sesion abierta con datos anteriores a su suspension.
        SecurityException error = assertThrows(SecurityException.class,
                () -> servicio.suspenderUsuario(admin(ADMIN_MATEO), ADMIN_CAMILA, "Represalia"));
        assertEquals(ServiceAdministracion.SESION_ADMIN_INVALIDA, error.getMessage());
        assertEquals("ACTIVO", estado(ADMIN_CAMILA));
        assertEquals(1, contar("SELECT COUNT(*) FROM Usuario WHERE tipo_usuario = 'ADMINISTRADOR' "
                + "AND estado = 'ACTIVO'"));

        // El unico administrador activo tampoco puede suspenderse a si mismo.
        assertThrows(IllegalStateException.class,
                () -> servicio.suspenderUsuario(admin(ADMIN_CAMILA), ADMIN_CAMILA, "Prueba"));
        assertEquals("ACTIVO", estado(ADMIN_CAMILA));
    }

    // ------------------------------------------------------------------
    // H: historial inconsistente
    // ------------------------------------------------------------------

    @Test
    void h1CuentaActivaConSuspensionVigenteNoSeSuspende() throws Exception {
        sql("INSERT INTO Historial_suspension_usuario (id_usuario, id_administrador, motivo) "
                + "VALUES (5, 15, 'Fila huérfana')");
        int avisos = notificaciones(CLIENTE_ANA);

        IllegalStateException error = assertThrows(IllegalStateException.class, this::suspenderAna);
        assertEquals(ServiceAdministracion.HISTORIAL_INCONSISTENTE, error.getMessage());
        assertEquals("ACTIVO", estado(CLIENTE_ANA));
        assertEquals(1, suspensiones(CLIENTE_ANA));
        assertEquals(avisos, notificaciones(CLIENTE_ANA));
    }

    @Test
    void h2CuentaSuspendidaSinHistorialNoSeReactivaNiSeRepara() throws Exception {
        sql("UPDATE Usuario SET estado = 'SUSPENDIDO' WHERE id_usuario = 5");
        int avisos = notificaciones(CLIENTE_ANA);

        IllegalStateException previa = assertThrows(IllegalStateException.class,
                () -> servicio.validarReactivacion(admin(ADMIN_CAMILA), CLIENTE_ANA));
        assertEquals(ServiceAdministracion.HISTORIAL_INCONSISTENTE, previa.getMessage());

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> servicio.reactivarUsuario(admin(ADMIN_CAMILA), CLIENTE_ANA));
        assertEquals(ServiceAdministracion.HISTORIAL_INCONSISTENTE, error.getMessage());

        assertEquals("SUSPENDIDO", estado(CLIENTE_ANA));
        assertEquals(0, suspensiones(CLIENTE_ANA));
        assertEquals(avisos, notificaciones(CLIENTE_ANA));
    }

    @Test
    void h3VariosCiclosConservanTodoElHistorial() throws Exception {
        suspenderAna();
        servicio.reactivarUsuario(admin(ADMIN_MATEO), CLIENTE_ANA);
        servicio.suspenderUsuario(admin(ADMIN_MATEO), CLIENTE_ANA, "Reincidencia");
        servicio.reactivarUsuario(admin(ADMIN_CAMILA), CLIENTE_ANA);

        assertEquals("ACTIVO", estado(CLIENTE_ANA));
        assertEquals(2, suspensiones(CLIENTE_ANA));
        assertEquals(0, vigentes(CLIENTE_ANA));
        assertEquals(2, contar("SELECT COUNT(*) FROM Historial_suspension_usuario WHERE id_usuario = 5 "
                + "AND id_administrador_reactivacion IS NOT NULL AND fecha_reactivacion IS NOT NULL"));
    }

    // ------------------------------------------------------------------
    // R: reactivacion (ampliacion)
    // ------------------------------------------------------------------

    @Test
    void r1ReactivarCierraLaSuspensionYNotifica() throws Exception {
        suspenderAna();
        int avisos = notificaciones(CLIENTE_ANA);

        assertEquals("Incumplió las normas de uso",
                servicio.validarReactivacion(admin(ADMIN_MATEO), CLIENTE_ANA));
        servicio.reactivarUsuario(admin(ADMIN_MATEO), CLIENTE_ANA);

        assertEquals("ACTIVO", estado(CLIENTE_ANA));
        assertEquals(1, suspensiones(CLIENTE_ANA));
        assertEquals(0, vigentes(CLIENTE_ANA));
        assertEquals(ADMIN_MATEO, contar("SELECT id_administrador_reactivacion FROM Historial_suspension_usuario "
                + "WHERE id_usuario = 5"));
        assertNotNull(texto("SELECT fecha_reactivacion FROM Historial_suspension_usuario WHERE id_usuario = 5"));
        assertEquals(avisos + 1, notificaciones(CLIENTE_ANA));
    }

    @Test
    void r2NoSeReactivaUnaCuentaActiva() throws Exception {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> servicio.reactivarUsuario(admin(ADMIN_CAMILA), CLIENTE_ANA));
        assertEquals(ServiceAdministracion.YA_ACTIVA, error.getMessage());

        sql("UPDATE Usuario SET estado = 'INACTIVO' WHERE id_usuario = 6");
        IllegalStateException inactiva = assertThrows(IllegalStateException.class,
                () -> servicio.reactivarUsuario(admin(ADMIN_CAMILA), 6));
        assertEquals(ServiceAdministracion.NO_SUSPENDIDA, inactiva.getMessage());
        assertEquals("INACTIVO", estado(6));
    }

    @Test
    void r3ErrorAlReactivarConservaLaSuspension() throws Exception {
        suspenderAna();
        sql("ALTER TABLE Notificacion_interna ADD CONSTRAINT fallo_reactivar CHECK (mensaje NOT LIKE 'Tu cuenta fue reactivada%')");
        try {
            IllegalStateException error = assertThrows(IllegalStateException.class,
                    () -> servicio.reactivarUsuario(admin(ADMIN_CAMILA), CLIENTE_ANA));
            assertEquals(ServiceAdministracion.ERROR_REACTIVACION, error.getMessage());
        } finally {
            sql("ALTER TABLE Notificacion_interna DROP CONSTRAINT fallo_reactivar");
        }
        assertEquals("SUSPENDIDO", estado(CLIENTE_ANA));
        assertEquals(1, vigentes(CLIENTE_ANA));
    }

    @Test
    void r4ReactivarNoCambiaRolesNiEstadoDelActor() throws Exception {
        String consultaRoles = "SELECT COUNT(*) FROM Usuario_rol";
        int roles = contar(consultaRoles);
        String proveedor = texto("SELECT estado FROM Proveedor WHERE id_usuario = 3");
        String organizador = texto("SELECT estado FROM Organizador WHERE id_usuario = 1");

        for (int usuario : new int[] {USUARIO_PROVEEDOR, USUARIO_ORGANIZADOR}) {
            servicio.suspenderUsuario(admin(ADMIN_CAMILA), usuario, "Revisión de la cuenta");
            servicio.reactivarUsuario(admin(ADMIN_CAMILA), usuario);
            assertEquals("ACTIVO", estado(usuario));
        }

        assertEquals(roles, contar(consultaRoles));
        assertEquals(proveedor, texto("SELECT estado FROM Proveedor WHERE id_usuario = 3"));
        assertEquals(organizador, texto("SELECT estado FROM Organizador WHERE id_usuario = 1"));
    }

    @Test
    void r5ReactivarNoLevantaLaSancionDeCu09() throws Exception {
        ServiceSancionesProveedor sanciones =
                new ServiceSancionesProveedor(new RepositorySancionesProveedor(url));
        sanciones.aplicarSancion(admin(ADMIN_CAMILA), new SolicitudSancion(
                1, 1, "INCUMPLIMIENTO_CONDICIONES", "SUSPENSION", 0, null,
                "Se presentó sin el equipo acordado"));

        servicio.suspenderUsuario(admin(ADMIN_CAMILA), USUARIO_PROVEEDOR, "Revisión de la cuenta");
        servicio.reactivarUsuario(admin(ADMIN_CAMILA), USUARIO_PROVEEDOR);

        assertEquals("ACTIVO", estado(USUARIO_PROVEEDOR));
        assertEquals("SUSPENDIDO", texto("SELECT estado FROM Proveedor WHERE id_proveedor = 1"));

        IllegalStateException login = assertThrows(IllegalStateException.class,
                () -> new ServiceAutenticacion(new RepositoryAutenticacion(url))
                        .iniciarSesion("decoraciones.arcoiris", "1234"));
        assertEquals("El proveedor aun no esta aprobado.", login.getMessage());
    }

    // ------------------------------------------------------------------
    // L: acceso despues de suspender y recuperacion despues de reactivar
    // ------------------------------------------------------------------

    @Test
    void l1SuspendidoNoPuedeIniciarSesionYL3RecuperaElAccesoAlReactivar() throws Exception {
        ServiceAutenticacion autenticacion = new ServiceAutenticacion(new RepositoryAutenticacion(url));
        SesionUsuario antes = autenticacion.iniciarSesion("ana.martinez", "1234");

        suspenderAna();

        IllegalStateException bloqueado = assertThrows(IllegalStateException.class,
                () -> autenticacion.iniciarSesion("ana.martinez", "1234"));
        assertEquals(ServiceAutenticacion.MENSAJE_CUENTA_SUSPENDIDA, bloqueado.getMessage());
        assertEquals(ServiceAutenticacion.MENSAJE_CUENTA_SUSPENDIDA,
                autenticacion.verificarSesionVigente(antes).orElseThrow());

        servicio.reactivarUsuario(admin(ADMIN_MATEO), CLIENTE_ANA);

        SesionUsuario despues = autenticacion.iniciarSesion("ana.martinez", "1234");
        assertEquals(antes.getTipoUsuario(), despues.getTipoUsuario());
        assertEquals(antes.getIdActor(), despues.getIdActor());
        assertTrue(autenticacion.verificarSesionVigente(despues).isEmpty());
    }

    @Test
    void consultarUsuariosFiltraYMuestraElEstado() throws Exception {
        suspenderAna();

        var todos = servicio.consultarUsuarios(admin(ADMIN_CAMILA), "");
        assertTrue(todos.size() >= 12);

        var filtrados = servicio.consultarUsuarios(admin(ADMIN_CAMILA), "ana.mart");
        assertEquals(1, filtrados.size());
        UsuarioAdministrable ana = filtrados.get(0);
        assertEquals(CLIENTE_ANA, ana.idUsuario());
        assertEquals("SUSPENDIDO", ana.estado());
        assertEquals("CLIENTE", ana.tipoUsuario());

        assertTrue(servicio.consultarUsuarios(admin(ADMIN_CAMILA), "%").size() >= 12);
        assertThrows(IllegalArgumentException.class,
                () -> servicio.consultarUsuarios(admin(ADMIN_CAMILA), "z".repeat(101)));
    }

    // ------------------------------------------------------------------
    // Regresion: bases locales con las tablas de la rama anterior
    // (CU29-suspender-usuario), que tenian otro esquema y los mismos
    // nombres de constraint que la primera version de la migracion 014.
    // ------------------------------------------------------------------

    /** DDL exacta de las migraciones de la rama anterior (013 y 014). */
    private static final String DDL_TABLAS_ANTIGUAS = """
            CREATE TABLE IF NOT EXISTS Suspension_usuario (
                id_suspension INT PRIMARY KEY AUTO_INCREMENT,
                id_usuario INT NOT NULL,
                id_administrador INT NOT NULL,
                motivo VARCHAR(500) NOT NULL,
                fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                CONSTRAINT fk_suspension_usuario FOREIGN KEY (id_usuario) REFERENCES Usuario(id_usuario),
                CONSTRAINT fk_suspension_administrador FOREIGN KEY (id_administrador) REFERENCES Usuario(id_usuario),
                CONSTRAINT chk_motivo_suspension CHECK (TRIM(motivo) <> '')
            );
            CREATE TABLE IF NOT EXISTS Reactivacion_usuario (
                id_reactivacion INT PRIMARY KEY AUTO_INCREMENT,
                id_usuario INT NOT NULL,
                id_administrador INT NOT NULL,
                motivo VARCHAR(500),
                fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                CONSTRAINT fk_reactivacion_usuario FOREIGN KEY (id_usuario) REFERENCES Usuario(id_usuario),
                CONSTRAINT fk_reactivacion_administrador FOREIGN KEY (id_administrador) REFERENCES Usuario(id_usuario),
                CONSTRAINT chk_motivo_reactivacion CHECK (motivo IS NULL OR TRIM(motivo) <> '')
            );
            """;

    private static String volcar(Connection c, String consulta) throws SQLException {
        StringBuilder texto = new StringBuilder();
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery(consulta)) {
            int columnas = r.getMetaData().getColumnCount();
            while (r.next()) {
                for (int i = 1; i <= columnas; i++) {
                    texto.append(r.getString(i)).append('|');
                }
                texto.append('\n');
            }
        }
        return texto.toString();
    }

    @Test
    void baseConTablasDeLaRamaAnteriorFuncionaSinModificarlas() throws Exception {
        String urlAntigua = "jdbc:h2:mem:cu29_antigua_" + UUID.randomUUID().toString().replace("-", "")
                + ";DB_CLOSE_DELAY=-1";

        // 1. Base con el esquema base y las tablas antiguas, creadas antes de la migracion 014.
        try (Connection c = DriverManager.getConnection(urlAntigua, "sa", "");
             InputStreamReader schema = new InputStreamReader(
                     SuspensionUsuarioH2Test.class.getResourceAsStream("/schema.sql"),
                     StandardCharsets.UTF_8);
             Statement s = c.createStatement()) {
            RunScript.execute(c, schema);
            s.execute(DDL_TABLAS_ANTIGUAS);
        }

        // 2. Primer arranque con el codigo actual: no debe fallar por constraints repetidos.
        try (Connection c = DatabaseConnection.getConnection(urlAntigua);
             Statement s = c.createStatement()) {
            CargarDatosEjemplo.cargar(c);
            s.executeUpdate("INSERT INTO Suspension_usuario (id_usuario, id_administrador, motivo, fecha) "
                    + "VALUES (5, 14, 'Registro de la rama anterior', TIMESTAMP '2026-11-02 16:58:20')");
            s.executeUpdate("INSERT INTO Reactivacion_usuario (id_usuario, id_administrador, fecha) "
                    + "VALUES (5, 14, TIMESTAMP '2026-11-02 17:00:01')");
            s.executeUpdate("INSERT INTO Esquema_version (nombre) VALUES "
                    + "('013_cu29_suspender_usuario.sql'), ('014_cu29_reactivar_usuario.sql')");
        }

        String suspensionesAntiguas;
        String reactivacionesAntiguas;
        String columnasAntiguas;
        String versiones;
        int usuarios;
        try (Connection c = DatabaseConnection.getConnection(urlAntigua)) {
            suspensionesAntiguas = volcar(c, "SELECT * FROM Suspension_usuario ORDER BY 1");
            reactivacionesAntiguas = volcar(c, "SELECT * FROM Reactivacion_usuario ORDER BY 1");
            columnasAntiguas = volcar(c, "SELECT TABLE_NAME, COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS "
                    + "WHERE TABLE_NAME IN ('SUSPENSION_USUARIO', 'REACTIVACION_USUARIO') ORDER BY 1, ORDINAL_POSITION");
            versiones = volcar(c, "SELECT nombre FROM Esquema_version ORDER BY nombre");
            usuarios = contarEn(c, "SELECT COUNT(*) FROM Usuario");
        }

        // 3. Un nuevo arranque vuelve a ejecutar las migraciones sin errores.
        DatabaseConnection.reiniciarInicializacion();
        try (Connection c = DatabaseConnection.getConnection(urlAntigua)) {
            assertEquals(0, contarEn(c, "SELECT COUNT(*) FROM Historial_suspension_usuario"));
        }

        // 4. CU-029 completo sobre la tabla nueva.
        ServiceAdministracion servicioAntiguo = new ServiceAdministracion(
                new RepositoryContratacion(urlAntigua),
                new RepositoryOrganizador(urlAntigua),
                new RepositoryAdministracion(urlAntigua));
        ServiceAutenticacion autenticacion =
                new ServiceAutenticacion(new RepositoryAutenticacion(urlAntigua));

        assertTrue(servicioAntiguo.validarSuspension(admin(ADMIN_CAMILA), CLIENTE_ANA, null));
        servicioAntiguo.suspenderUsuario(admin(ADMIN_CAMILA), CLIENTE_ANA, "Motivo con la tabla nueva");
        IllegalStateException bloqueado = assertThrows(IllegalStateException.class,
                () -> autenticacion.iniciarSesion("ana.martinez", "1234"));
        assertEquals(ServiceAutenticacion.MENSAJE_CUENTA_SUSPENDIDA, bloqueado.getMessage());

        assertEquals("Motivo con la tabla nueva",
                servicioAntiguo.validarReactivacion(admin(ADMIN_MATEO), CLIENTE_ANA));
        servicioAntiguo.reactivarUsuario(admin(ADMIN_MATEO), CLIENTE_ANA);
        assertEquals("CLIENTE", autenticacion.iniciarSesion("ana.martinez", "1234").getTipoUsuario());

        // 5. Las tablas antiguas, sus filas, el registro de versiones y los usuarios no cambian.
        try (Connection c = DatabaseConnection.getConnection(urlAntigua)) {
            assertEquals(suspensionesAntiguas, volcar(c, "SELECT * FROM Suspension_usuario ORDER BY 1"));
            assertEquals(reactivacionesAntiguas, volcar(c, "SELECT * FROM Reactivacion_usuario ORDER BY 1"));
            assertEquals(columnasAntiguas, volcar(c, "SELECT TABLE_NAME, COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS "
                    + "WHERE TABLE_NAME IN ('SUSPENSION_USUARIO', 'REACTIVACION_USUARIO') ORDER BY 1, ORDINAL_POSITION"));
            assertEquals(versiones, volcar(c, "SELECT nombre FROM Esquema_version ORDER BY nombre"));
            assertEquals(usuarios, contarEn(c, "SELECT COUNT(*) FROM Usuario"));
            assertEquals(1, contarEn(c, "SELECT COUNT(*) FROM Historial_suspension_usuario "
                    + "WHERE id_usuario = 5 AND fecha_reactivacion IS NOT NULL"));
        }
    }

    private static int contarEn(Connection c, String consulta) throws SQLException {
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery(consulta)) {
            r.next();
            return r.getInt(1);
        }
    }
}

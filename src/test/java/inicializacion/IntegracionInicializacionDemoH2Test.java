package inicializacion;

import entities.SesionUsuario;
import entities.eventos.Evento;
import entities.finanzas.CompromisoFinanciero;
import entities.servicios.Servicio;
import entities.solicitudes.PropuestaServicio;
import entities.solicitudes.SolicitudEjecucion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import persistencia.DatabaseConnection;
import repositories.RepositoryAutenticacion;
import repositories.RepositoryMovimientoFinanciero;
import repositories.solicitudes.RepositoryPropuestaServicio;
import repositories.solicitudes.RepositorySolicitudEjecucion;
import repositories.eventos.RepositoryEvento;
import repositories.servicios.RepositoryServicio;
import services.ServiceAutenticacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntegracionInicializacionDemoH2Test {

    private static final String CLAVE = "1234";

    private String url;
    private String urlAnterior;

    @BeforeEach
    void prepararBaseEnMemoria() {
        urlAnterior = System.getProperty("nexteventux.db.url");
        url = "jdbc:h2:mem:integracion_demo_" + UUID.randomUUID()
                + ";DB_CLOSE_DELAY=-1";
        System.setProperty("nexteventux.db.url", url);
    }

    @AfterEach
    void restaurarConfiguracion() {
        if (urlAnterior == null) {
            System.clearProperty("nexteventux.db.url");
        } else {
            System.setProperty("nexteventux.db.url", urlAnterior);
        }
    }

    @Test
    void inicializaBaseNuevaSinDuplicarYConsultaLaHistoriaDesdeRepositorios()
            throws Exception {
        assertTrue(inicializarDemo());
        assertEquals(12, contar("SELECT COUNT(*) FROM Usuario"));
        assertEquals(4, contar("SELECT COUNT(*) FROM Evento"));
        assertEquals(15, contar("SELECT COUNT(*) FROM Servicio"));
        comprobarConteosOficiales();
        comprobarAutenticacionDeCuentasOficiales();
        comprobarRepositoriosConIdsReales();

        assertFalse(inicializarDemo());
        assertEquals(12, contar("SELECT COUNT(*) FROM Usuario"));
        assertEquals(4, contar("SELECT COUNT(*) FROM Evento"));
        assertEquals(15, contar("SELECT COUNT(*) FROM Servicio"));
        comprobarConteosOficiales();
    }

    @Test
    void completaBaseExistenteConservandoCuentasYRegistrosPersonales()
            throws Exception {
        try (Connection conexion = DatabaseConnection.getConnection(url)) {
            insertarDatosPersonales(conexion);
        }

        assertFalse(inicializarDemo());
        assertEquals(14, contar("SELECT COUNT(*) FROM Usuario"));
        assertEquals(5, contar("SELECT COUNT(*) FROM Evento"));
        assertEquals(16, contar("SELECT COUNT(*) FROM Servicio"));
        comprobarConteosOficiales();

        try (Connection conexion = DatabaseConnection.getConnection(url);
                Statement sentencia = conexion.createStatement()) {
            sentencia.executeUpdate("""
                    UPDATE Usuario SET contraseña = 'clave-elegida'
                    WHERE usuario = 'laura.gomez'
                    """);
        }

        assertFalse(inicializarDemo());
        assertEquals(14, contar("SELECT COUNT(*) FROM Usuario"));
        assertEquals(5, contar("SELECT COUNT(*) FROM Evento"));
        assertEquals(16, contar("SELECT COUNT(*) FROM Servicio"));
        assertEquals("Mi evento personal",
                texto("""
                        SELECT nombre FROM Evento
                        WHERE nombre = 'Mi evento personal'
                        """));
        assertEquals("Mi servicio personal",
                texto("""
                        SELECT nombre FROM Servicio
                        WHERE nombre = 'Mi servicio personal'
                        """));
        assertEquals("4321.00", texto("""
                SELECT CAST(precio AS VARCHAR) FROM Servicio
                WHERE nombre = 'Mi servicio personal'
                """));
        assertEquals("clave-personal", texto("""
                SELECT contraseña FROM Usuario WHERE usuario = 'cuenta.personal'
                """));

        ServiceAutenticacion autenticacion = new ServiceAutenticacion(
                new RepositoryAutenticacion(url));
        assertThrows(IllegalArgumentException.class,
                () -> autenticacion.iniciarSesion("laura.gomez", CLAVE));
    }

    @Test
    void informaElConflictoDeIdentidadConUnMensajeClaro() throws Exception {
        inicializarDemo();
        int babyShower = idRegistro("EVENTO", "baby-shower-valentina");
        int idLaura = idPerfil("Organizador", "id_organizador", "laura.gomez");
        int eventosAntes = contar("SELECT COUNT(*) FROM Evento");
        int serviciosAntes = contar("SELECT COUNT(*) FROM Servicio");
        int asociacionesAntes = contar("SELECT COUNT(*) FROM Registro_demo_catalogo");

        try (Connection conexion = DatabaseConnection.getConnection(url);
                PreparedStatement actualizar = conexion.prepareStatement(
                        "UPDATE Evento SET id_organizador = ? WHERE id_evento = ?")) {
            actualizar.setInt(1, idLaura);
            actualizar.setInt(2, babyShower);
            assertEquals(1, actualizar.executeUpdate());
        }

        SQLException error = assertThrows(SQLException.class, this::inicializarDemo);
        assertTrue(error.getMessage().contains(
                "Conflicto de identidad para organizador de la asociación de evento"));
        assertEquals(eventosAntes, contar("SELECT COUNT(*) FROM Evento"));
        assertEquals(serviciosAntes, contar("SELECT COUNT(*) FROM Servicio"));
        assertEquals(asociacionesAntes,
                contar("SELECT COUNT(*) FROM Registro_demo_catalogo"));
    }

    private boolean inicializarDemo() throws Exception {
        try (Connection conexion
                = DatabaseConnection.getConnectionParaSincronizarDemo(url)) {
            boolean seedCargado = CargarDatosEjemplo.cargarSiEstaVacia(conexion);
            SincronizarCuentasDemo.sincronizar(conexion);
            SincronizarCatalogoDemo.sincronizar(conexion);
            SincronizarHistoriaDemo.sincronizar(conexion);
            return seedCargado;
        }
    }

    private void comprobarAutenticacionDeCuentasOficiales() {
        ServiceAutenticacion autenticacion = new ServiceAutenticacion(
                new RepositoryAutenticacion(url));
        comprobarIngreso(autenticacion, "laura.gomez", "ORGANIZADOR");
        comprobarIngreso(autenticacion, "carlos.rodriguez", "ORGANIZADOR");
        comprobarIngreso(autenticacion, "valentina", "ORGANIZADOR");
        comprobarIngreso(autenticacion, "decoraciones.arcoiris", "PROVEEDOR");
        comprobarIngreso(autenticacion, "sabores.eventos", "PROVEEDOR");
        comprobarIngreso(autenticacion, "flash", "PROVEEDOR");
        comprobarIngreso(autenticacion, "ana.martinez", "CLIENTE");
        comprobarIngreso(autenticacion, "diego.perez", "CLIENTE");
        comprobarIngreso(autenticacion, "paula.rios", "CLIENTE");
        comprobarIngreso(autenticacion, "camila", "ADMINISTRADOR");
        comprobarIngreso(autenticacion, "mateo", "ADMINISTRADOR");
        comprobarIngreso(autenticacion, "shirly", "ADMINISTRADOR");
    }

    private void comprobarIngreso(
            ServiceAutenticacion autenticacion,
            String usuario,
            String tipo) {
        SesionUsuario sesion = autenticacion.iniciarSesion(usuario, CLAVE);
        assertNotNull(sesion, usuario);
        assertEquals(tipo, sesion.getTipoUsuario(), usuario);
    }

    private void comprobarRepositoriosConIdsReales() throws Exception {
        int idBodaLaura = idRegistro("EVENTO", "boda-laura");
        int idLaura = idPerfil("Organizador", "id_organizador", "laura.gomez");
        List<Evento> eventos = new RepositoryEvento(url)
                .listarEventosPorOrganizador(idLaura);
        assertTrue(eventos.stream().anyMatch(
                evento -> evento.getIdEvento() == idBodaLaura));

        int idFlash = idPerfil("Proveedor", "id_proveedor", "flash");
        int idServicioFlash = idRegistro("SERVICIO", "cobertura-fotografica");
        List<Servicio> servicios = new RepositoryServicio()
                .listarServiciosProveedor(idFlash);
        assertTrue(servicios.stream().anyMatch(
                servicio -> servicio.getIdServicio() == idServicioFlash));

        int idDecoraciones = idPerfil(
                "Proveedor", "id_proveedor", "decoraciones.arcoiris");
        List<SolicitudEjecucion> solicitudes = new RepositorySolicitudEjecucion()
                .listarPorProveedor(idDecoraciones);
        assertFalse(solicitudes.isEmpty());

        int idSolicitudSeis = idRegistro("SOLICITUD", "solicitud-6");
        int idProveedorSolicitud = entero("""
                SELECT id_proveedor FROM Solicitud_Ejecucion WHERE id_solicitud = %d
                """.formatted(idSolicitudSeis));
        int idPropuestaSeis = idRegistro("PROPUESTA", "propuesta-6");
        List<PropuestaServicio> propuestas = new RepositoryPropuestaServicio()
                .listarHistorial(idProveedorSolicitud, idSolicitudSeis);
        assertTrue(propuestas.stream().anyMatch(
                propuesta -> propuesta.getIdPropuesta() == idPropuestaSeis));

        List<CompromisoFinanciero> contrataciones = new RepositoryMovimientoFinanciero()
                .listarCompromisosPorEvento(idBodaLaura);
        assertEquals(2, contrataciones.size());
        int contratacionUno = idRegistro("CONTRATACION", "contratacion-1");
        int contratacionDos = idRegistro("CONTRATACION", "contratacion-2");
        assertTrue(contrataciones.stream().anyMatch(
                contrato -> contrato.getIdContratacion() == contratacionUno));
        assertTrue(contrataciones.stream().anyMatch(
                contrato -> contrato.getIdContratacion() == contratacionDos));
    }

    private void comprobarConteosOficiales() throws Exception {
        assertEquals(4, contar("""
                SELECT COUNT(*) FROM Registro_demo_catalogo WHERE tipo = 'EVENTO'
                """));
        assertEquals(15, contar("""
                SELECT COUNT(*) FROM Registro_demo_catalogo WHERE tipo = 'SERVICIO'
                """));
        assertEquals(8, contar("SELECT COUNT(*) FROM Necesidad_Servicio_Evento"));
        assertEquals(15, contar("SELECT COUNT(*) FROM Solicitud_Ejecucion"));
        assertEquals(10, contar("SELECT COUNT(*) FROM Propuesta"));
        assertEquals(2, contar("SELECT COUNT(*) FROM Contratacion"));
    }

    private void insertarDatosPersonales(Connection conexion) throws SQLException {
        try (Statement sentencia = conexion.createStatement()) {
            sentencia.executeUpdate("""
                    INSERT INTO Usuario
                    (nombre, usuario, contraseña, estado, fecha_registro,
                     acepta_marketing, tipo_usuario)
                    VALUES ('Cuenta personal', 'cuenta.personal', 'clave-personal',
                            'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'ORGANIZADOR')
                    """);
            sentencia.executeUpdate("""
                    INSERT INTO Usuario
                    (nombre, usuario, contraseña, estado, fecha_registro,
                     acepta_marketing, tipo_usuario)
                    VALUES ('Proveedor personal', 'proveedor.personal', 'clave-proveedor',
                            'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'PROVEEDOR')
                    """);
            sentencia.executeUpdate("""
                    INSERT INTO Organizador (id_usuario, estado)
                    SELECT id_usuario, 'ACTIVO' FROM Usuario
                    WHERE usuario = 'cuenta.personal'
                    """);
            sentencia.executeUpdate("""
                    INSERT INTO Proveedor (id_usuario, nombre, estado)
                    SELECT id_usuario, 'Proveedor personal', 'APROBADO'
                    FROM Usuario WHERE usuario = 'proveedor.personal'
                    """);
            sentencia.executeUpdate("""
                    INSERT INTO Evento
                    (id_organizador, nombre, fecha_inicio, fecha_fin,
                     ubicacion, estado, fecha_creacion)
                    SELECT o.id_organizador, 'Mi evento personal',
                           CURRENT_TIMESTAMP, DATEADD('HOUR', 2, CURRENT_TIMESTAMP),
                           'Cali', 'PLANEADO', CURRENT_TIMESTAMP
                    FROM Organizador o
                    JOIN Usuario u ON u.id_usuario = o.id_usuario
                    WHERE u.usuario = 'cuenta.personal'
                    """);
            sentencia.executeUpdate("""
                    INSERT INTO Servicio
                    (id_proveedor, nombre, descripcion, precio, categoria, estado)
                    SELECT p.id_proveedor, 'Mi servicio personal', 'No modificar',
                           4321.00, 'Personal', 'DISPONIBLE'
                    FROM Proveedor p
                    JOIN Usuario u ON u.id_usuario = p.id_usuario
                    WHERE u.usuario = 'proveedor.personal'
                    """);
        }
    }

    private int idRegistro(String tipo, String clave) throws Exception {
        try (Connection conexion = DatabaseConnection.getConnection(url);
                PreparedStatement consulta = conexion.prepareStatement("""
                        SELECT id_registro FROM Registro_demo_catalogo
                        WHERE tipo = ? AND clave = ?
                        """)) {
            consulta.setString(1, tipo);
            consulta.setString(2, clave);
            try (ResultSet resultado = consulta.executeQuery()) {
                assertTrue(resultado.next(), tipo + " " + clave);
                int id = resultado.getInt(1);
                assertFalse(resultado.next(), tipo + " " + clave);
                return id;
            }
        }
    }

    private int idPerfil(
            String tabla,
            String columnaId,
            String usuario) throws Exception {
        String sql = "SELECT p." + columnaId + " FROM " + tabla + " p "
                + "JOIN Usuario u ON u.id_usuario = p.id_usuario "
                + "WHERE u.usuario = ?";
        try (Connection conexion = DatabaseConnection.getConnection(url);
                PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setString(1, usuario);
            try (ResultSet resultado = consulta.executeQuery()) {
                assertTrue(resultado.next(), usuario);
                int id = resultado.getInt(1);
                assertFalse(resultado.next(), usuario);
                return id;
            }
        }
    }

    private int contar(String sql) throws Exception {
        return entero(sql);
    }

    private int entero(String sql) throws Exception {
        try (Connection conexion = DatabaseConnection.getConnection(url);
                Statement sentencia = conexion.createStatement();
                ResultSet resultado = sentencia.executeQuery(sql)) {
            assertTrue(resultado.next());
            return resultado.getInt(1);
        }
    }

    private String texto(String sql) throws Exception {
        try (Connection conexion = DatabaseConnection.getConnection(url);
                Statement sentencia = conexion.createStatement();
                ResultSet resultado = sentencia.executeQuery(sql)) {
            assertTrue(resultado.next());
            return resultado.getString(1);
        }
    }
}

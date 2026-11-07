import config.DatabaseConnection;
import controllers.ControllerAdministracion;
import entities.Contratacion;
import entities.Organizador;
import entities.ResenaOrganizador;
import entities.SesionUsuario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repositories.RepositoryAutenticacion;
import repositories.RepositoryContratacion;
import repositories.RepositoryEvento;
import repositories.RepositoryOrganizador;
import repositories.RepositoryPublicacion;
import services.OrganizadorNoDisponibleException;
import services.ServiceAdministracion;
import services.ServiceAutenticacion;
import services.ServicePublicacion;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CU-026 Contratar organizador: flujo básico, A1, A2, E1, E2, sesión,
 * aislamiento y regresión de las consultas existentes, sobre H2 en memoria.
 *
 * Los datos son propios de cada prueba (base aislada), no del seed.
 */
public class CU026ContratarOrganizadorTest {

    private static final int CLIENTE = 9001;
    private static final int OTRO_CLIENTE = 9003;
    private static final int USUARIO_ORGANIZADOR = 9002;
    private static final int ORGANIZADOR = 9001;
    private static final int USUARIO_OTRO_ORGANIZADOR = 9004;
    private static final int OTRO_ORGANIZADOR = 9002;

    private String url;
    private RepositoryContratacion repositoryContratacion;
    private RepositoryOrganizador repositoryOrganizador;
    private ServiceAdministracion servicio;
    private ControllerAdministracion controlador;

    private final LocalDate fecha = LocalDate.now().plusDays(40);

    @BeforeEach
    void preparar() throws Exception {

        url = "jdbc:h2:mem:cu26_" + UUID.randomUUID()
                + ";DB_CLOSE_DELAY=-1";

        try (Connection connection = DatabaseConnection.getConnection(url);
             Statement statement = connection.createStatement()) {

            statement.executeUpdate("""
                    INSERT INTO Usuario
                    (id_usuario, nombre, usuario, contraseña, estado,
                     fecha_registro, acepta_marketing, tipo_usuario)
                    VALUES
                    (9001, 'Cliente CU26', 'cliente_cu26', 'clave',
                     'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'CLIENTE'),
                    (9002, 'Organizador CU26', 'organizador_cu26', 'clave',
                     'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'ORGANIZADOR'),
                    (9003, 'Otro cliente CU26', 'otro_cliente_cu26', 'clave',
                     'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'CLIENTE'),
                    (9004, 'Beatriz Corporativa', 'organizador2_cu26', 'clave',
                     'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'ORGANIZADOR')
                    """);

            statement.executeUpdate("""
                    INSERT INTO Organizador
                    (id_organizador, id_usuario, empresa, telefono, estado,
                     ciudad, especialidad, portafolio, tarifa_base,
                     estado_verificacion)
                    VALUES
                    (9001, 9002, 'Eventos CU26', '3000000000', 'ACTIVO',
                     'Bogota', 'Bodas y eventos sociales',
                     'Organizacion de bodas y celebraciones',
                     1500000, 'VERIFICADO'),
                    (9002, 9004, 'Corporativos CU26', '3000000001', 'ACTIVO',
                     'Bogota', 'Bodas y eventos empresariales',
                     'Lanzamientos 100% corporativos',
                     1000000, 'VERIFICADO')
                    """);
        }

        repositoryContratacion = new RepositoryContratacion(url);
        repositoryOrganizador = new RepositoryOrganizador(url);

        servicio = new ServiceAdministracion(
                repositoryContratacion,
                repositoryOrganizador);

        controlador = new ControllerAdministracion(
                servicio, sesion(CLIENTE, "CLIENTE", "ACTIVO"));
    }

    @AfterEach
    void limpiar() throws Exception {

        try (Connection connection =
                     DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {

            statement.execute("DROP ALL OBJECTS");
        }
    }

    // ------------------------------------------------------------------
    // Búsqueda (pasos 1 y 2, A1)
    // ------------------------------------------------------------------

    @Test
    void e01_faltaTipoDeEvento() {
        Map<String, Object> filtros = filtrosValidos();
        filtros.remove("tipoEvento");
        assertThrows(IllegalArgumentException.class,
                () -> controlador.consultarOrganizadores(filtros));
    }

    @Test
    void e02_faltaCiudad() {
        Map<String, Object> filtros = filtrosValidos();
        filtros.put("ciudad", "  ");
        assertThrows(IllegalArgumentException.class,
                () -> controlador.consultarOrganizadores(filtros));
    }

    @Test
    void e03_faltaFecha() {
        Map<String, Object> filtros = filtrosValidos();
        filtros.remove("fechaAproximada");
        assertThrows(IllegalArgumentException.class,
                () -> controlador.consultarOrganizadores(filtros));
    }

    @Test
    void e04_faltaPresupuesto() {
        Map<String, Object> filtros = filtrosValidos();
        filtros.remove("presupuestoEstimado");
        assertThrows(IllegalArgumentException.class,
                () -> controlador.consultarOrganizadores(filtros));
    }

    @Test
    void e05_presupuestoNegativo() {
        Map<String, Object> filtros = filtrosValidos();
        filtros.put("presupuestoEstimado", new BigDecimal("-1"));
        assertThrows(IllegalArgumentException.class,
                () -> controlador.consultarOrganizadores(filtros));
    }

    @Test
    void e06_presupuestoNoNumerico() {
        Map<String, Object> filtros = filtrosValidos();
        filtros.put("presupuestoEstimado", "dos millones");
        assertThrows(IllegalArgumentException.class,
                () -> controlador.consultarOrganizadores(filtros));
    }

    @Test
    void e07_fechaPasadaSeRechazaEnBusquedaYRegistro() {
        Map<String, Object> filtros = filtrosValidos();
        filtros.put("fechaAproximada", LocalDate.now().minusDays(1));
        assertThrows(IllegalArgumentException.class,
                () -> controlador.consultarOrganizadores(filtros));

        Contratacion datos = solicitud(ORGANIZADOR, LocalDate.now().minusDays(1));
        assertThrows(IllegalArgumentException.class,
                () -> controlador.contratarOrganizador(ORGANIZADOR, datos));
        assertEquals(0, contar("Solicitud_Contratacion_Organizador"));
    }

    @Test
    void e08_excluyeOrganizadorNoVerificado() throws Exception {
        sql("UPDATE Organizador SET estado_verificacion = 'PENDIENTE' WHERE id_organizador = 9001");
        assertEquals(List.of(OTRO_ORGANIZADOR), ids(buscar()));
    }

    @Test
    void e09_excluyeOrganizadorInactivo() throws Exception {
        sql("UPDATE Organizador SET estado = 'INACTIVO' WHERE id_organizador = 9001");
        assertEquals(List.of(OTRO_ORGANIZADOR), ids(buscar()));
    }

    @Test
    void e10_excluyeOrganizadorConCuentaSuspendida() throws Exception {
        sql("UPDATE Usuario SET estado = 'SUSPENDIDO' WHERE id_usuario = 9002");
        assertEquals(List.of(OTRO_ORGANIZADOR), ids(buscar()));
    }

    @Test
    void e11_excluyeOtraCiudad() {
        Map<String, Object> filtros = filtrosValidos();
        filtros.put("ciudad", "Cali");
        assertTrue(controlador.consultarOrganizadores(filtros).isEmpty());
    }

    @Test
    void e12_ciudadSinDistinguirMayusculas() {
        Map<String, Object> filtros = filtrosValidos();
        filtros.put("ciudad", "BOGOTA");
        assertEquals(2, controlador.consultarOrganizadores(filtros).size());
    }

    @Test
    void e13_excluyeTarifaMayorAlPresupuesto() {
        Map<String, Object> filtros = filtrosValidos();
        filtros.put("presupuestoEstimado", new BigDecimal("1200000"));
        assertEquals(List.of(OTRO_ORGANIZADOR),
                ids(controlador.consultarOrganizadores(filtros)));
    }

    @Test
    void e14_excluyeOrganizadorConEventoEnLaFecha() throws Exception {
        evento(ORGANIZADOR, fecha, "PLANEADO");
        assertEquals(List.of(OTRO_ORGANIZADOR), ids(buscar()));
        assertFalse(controlador.verificarDisponibilidadOrganizador(ORGANIZADOR, fecha));
    }

    @Test
    void e15_incluyeSiElEventoDeEsaFechaEstaCancelado() throws Exception {
        evento(ORGANIZADOR, fecha, "CANCELADO");
        assertTrue(ids(buscar()).contains(ORGANIZADOR));
        assertTrue(controlador.verificarDisponibilidadOrganizador(ORGANIZADOR, fecha));
    }

    @Test
    void e16_criteriosAdicionalesFiltranPorEspecialidadPortafolioEmpresaONombre() {
        assertEquals(List.of(OTRO_ORGANIZADOR), ids(buscarConCriterios("empresariales")));
        assertEquals(List.of(OTRO_ORGANIZADOR), ids(buscarConCriterios("lanzamientos")));
        assertEquals(List.of(OTRO_ORGANIZADOR), ids(buscarConCriterios("corporativos cu26")));
        assertEquals(List.of(OTRO_ORGANIZADOR), ids(buscarConCriterios("Beatriz")));
        assertEquals(List.of(ORGANIZADOR), ids(buscarConCriterios("celebraciones")));
    }

    @Test
    void e17_criteriosVaciosNoFiltran() {
        assertEquals(2, buscarConCriterios("   ").size());
        assertEquals(2, buscarConCriterios("").size());
    }

    @Test
    void e18_sinCoincidenciasDevuelveListaVacia() {
        Map<String, Object> filtros = filtrosValidos();
        filtros.put("tipoEvento", "Graduacion");
        assertTrue(controlador.consultarOrganizadores(filtros).isEmpty());

        // A1: al modificar el criterio la búsqueda vuelve a encontrar.
        filtros.put("tipoEvento", "Boda");
        assertFalse(controlador.consultarOrganizadores(filtros).isEmpty());
    }

    // ------------------------------------------------------------------
    // Perfil (paso 3, A2)
    // ------------------------------------------------------------------

    @Test
    void e19_perfilMuestraResenasDeLaMasRecienteALaMasAntigua() throws Exception {
        resena(ORGANIZADOR, CLIENTE, 4, "Buena coordinacion", "2026-01-10 10:00:00");
        resena(ORGANIZADOR, OTRO_CLIENTE, 5, "Muy puntual", "2026-03-15 18:30:00");

        List<ResenaOrganizador> resenas = controlador.consultarResenas(ORGANIZADOR);

        assertEquals(2, resenas.size());
        assertEquals("Muy puntual", resenas.get(0).comentario());
        assertEquals(5, resenas.get(0).calificacion());
        assertEquals("Buena coordinacion", resenas.get(1).comentario());

        Organizador perfil = controlador.seleccionarOrganizador(ORGANIZADOR);
        assertEquals(2, perfil.totalResenas());
        assertEquals(0, new BigDecimal("4.5").compareTo(perfil.calificacionPromedio()));
    }

    @Test
    void e20_organizadorSinResenas() {
        assertTrue(controlador.consultarResenas(ORGANIZADOR).isEmpty());
        assertEquals(0, controlador.seleccionarOrganizador(ORGANIZADOR).totalResenas());
    }

    @Test
    void e21_seleccionarOtroOrganizadorDevuelveSuPerfil() {
        Organizador primero = controlador.seleccionarOrganizador(ORGANIZADOR);
        Organizador segundo = controlador.seleccionarOrganizador(OTRO_ORGANIZADOR);

        assertEquals("Organizador CU26", primero.nombre());
        assertEquals("Beatriz Corporativa", segundo.nombre());
        assertEquals("Lanzamientos 100% corporativos", segundo.portafolio());
        assertEquals("Bodas y eventos empresariales", segundo.especialidad());
        assertEquals(0, new BigDecimal("1000000").compareTo(segundo.tarifaBase()));
    }

    // ------------------------------------------------------------------
    // Registro (paso 5)
    // ------------------------------------------------------------------

    @Test
    void e22_registraLaSolicitudPendiente() {
        Contratacion datos = solicitud(ORGANIZADOR, fecha);
        controlador.contratarOrganizador(ORGANIZADOR, datos);

        assertTrue(datos.getIdContratacion() > 0);
        assertEquals("PENDIENTE", datos.getEstado());

        Contratacion guardada = repositoryContratacion.buscarContratacion(
                datos.getIdContratacion());

        assertEquals("PENDIENTE", guardada.getEstado());
        assertEquals(ORGANIZADOR, guardada.getIdOrganizador());
        assertEquals(fecha, guardada.getFechaAproximada());
        assertEquals("Boda", guardada.getTipoEvento());
        assertEquals("Bogota", guardada.getCiudad());
        assertEquals(0, new BigDecimal("2000000").compareTo(guardada.getPresupuestoEstimado()));
        assertEquals("Celebracion familiar", guardada.getCriteriosBusqueda());
    }

    @Test
    void e23_elClienteSeTomaDeLaSesionYNoDelFormulario() {
        Contratacion datos = solicitud(ORGANIZADOR, fecha);
        datos.setIdCliente(OTRO_CLIENTE);

        controlador.contratarOrganizador(ORGANIZADOR, datos);

        assertEquals(CLIENTE, repositoryContratacion
                .buscarContratacion(datos.getIdContratacion()).getIdCliente());
    }

    @Test
    void e24_cargaLaFechaDeSolicitud() {
        Contratacion datos = solicitud(ORGANIZADOR, fecha);
        controlador.contratarOrganizador(ORGANIZADOR, datos);

        assertNotNull(datos.getFechaSolicitud());
        assertNotNull(repositoryContratacion
                .buscarContratacion(datos.getIdContratacion()).getFechaSolicitud());
    }

    @Test
    void e25_notificaAlUsuarioDelOrganizador() throws Exception {
        controlador.contratarOrganizador(ORGANIZADOR, solicitud(ORGANIZADOR, fecha));

        assertEquals(1, contar("Notificacion_interna WHERE id_usuario = 9002 "
                + "AND id_usuario_destinatario = 9002 "
                + "AND mensaje LIKE 'Nueva solicitud de contratación de Cliente CU26%'"));
        assertEquals(1, contar("Notificacion_interna"));
    }

    @Test
    void e26_elOrganizadorVeLaNotificacionEnSusAvisos() {
        controlador.contratarOrganizador(ORGANIZADOR, solicitud(ORGANIZADOR, fecha));

        SesionUsuario organizador = new ServiceAutenticacion(
                new RepositoryAutenticacion(url))
                .iniciarSesion("organizador_cu26", "clave");

        List<String> avisos = new ServicePublicacion(
                new RepositoryPublicacion(url), new RepositoryEvento(url))
                .notificaciones(organizador);

        assertEquals(1, avisos.size());
        assertTrue(avisos.get(0).contains("Estado: Pendiente"));
    }

    // ------------------------------------------------------------------
    // E1: error al registrar
    // ------------------------------------------------------------------

    @Test
    void e27_falloAlNotificarNoDejaSolicitudIncompleta() throws Exception {
        sql("ALTER TABLE Notificacion_interna ADD CONSTRAINT fallo_aviso_cu26 "
                + "CHECK (id_usuario <> 9002)");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> controlador.contratarOrganizador(
                        ORGANIZADOR, solicitud(ORGANIZADOR, fecha)));

        assertEquals(RepositoryContratacion.ERROR_REGISTRO, error.getMessage());
        assertEquals(0, contar("Solicitud_Contratacion_Organizador"));
        assertEquals(0, contar("Notificacion_interna"));
    }

    @Test
    void e28_elClientePuedeReintentarDespuesDelError() throws Exception {
        sql("ALTER TABLE Notificacion_interna ADD CONSTRAINT fallo_aviso_cu26 "
                + "CHECK (id_usuario <> 9002)");
        assertThrows(IllegalStateException.class,
                () -> controlador.contratarOrganizador(
                        ORGANIZADOR, solicitud(ORGANIZADOR, fecha)));

        sql("ALTER TABLE Notificacion_interna DROP CONSTRAINT fallo_aviso_cu26");

        controlador.contratarOrganizador(ORGANIZADOR, solicitud(ORGANIZADOR, fecha));

        assertEquals(1, contar("Solicitud_Contratacion_Organizador"));
        assertEquals(1, contar("Notificacion_interna"));
    }

    // ------------------------------------------------------------------
    // E2: el organizador deja de estar disponible
    // ------------------------------------------------------------------

    @Test
    void e29_organizadorVuelveAPendienteDeVerificacion() throws Exception {
        controlador.seleccionarOrganizador(ORGANIZADOR);
        sql("UPDATE Organizador SET estado_verificacion = 'PENDIENTE' WHERE id_organizador = 9001");
        assertNoDisponibleSinRegistro(OrganizadorNoDisponibleException.NO_ACTIVO_VERIFICADO);
    }

    @Test
    void e30_organizadorInactivo() throws Exception {
        controlador.seleccionarOrganizador(ORGANIZADOR);
        sql("UPDATE Organizador SET estado = 'INACTIVO' WHERE id_organizador = 9001");
        assertNoDisponibleSinRegistro(OrganizadorNoDisponibleException.NO_ACTIVO_VERIFICADO);
    }

    @Test
    void e31_cuentaDelOrganizadorSuspendida() throws Exception {
        controlador.seleccionarOrganizador(ORGANIZADOR);
        sql("UPDATE Usuario SET estado = 'SUSPENDIDO' WHERE id_usuario = 9002");
        assertNoDisponibleSinRegistro(OrganizadorNoDisponibleException.NO_ACTIVO_VERIFICADO);
    }

    @Test
    void e31b_laTransaccionTambienDetectaE2() throws Exception {
        sql("UPDATE Usuario SET estado = 'SUSPENDIDO' WHERE id_usuario = 9002");

        Contratacion datos = solicitud(ORGANIZADOR, fecha);
        datos.setIdCliente(CLIENTE);

        OrganizadorNoDisponibleException error = assertThrows(
                OrganizadorNoDisponibleException.class,
                () -> repositoryContratacion.guardarContratacion(datos));

        assertEquals(OrganizadorNoDisponibleException.NO_ACTIVO_VERIFICADO, error.getMessage());
        assertEquals(0, contar("Solicitud_Contratacion_Organizador"));
        assertEquals(0, contar("Notificacion_interna"));
    }

    // ------------------------------------------------------------------
    // Disponibilidad por fecha verificada al registrar
    // ------------------------------------------------------------------

    @Test
    void e32_eventoNuevoEnLaFechaImpideElRegistro() throws Exception {
        assertTrue(ids(buscar()).contains(ORGANIZADOR));
        evento(ORGANIZADOR, fecha, "PLANEADO");

        assertNoDisponibleSinRegistro(OrganizadorNoDisponibleException.SIN_DISPONIBILIDAD_FECHA);
    }

    // ------------------------------------------------------------------
    // Duplicados
    // ------------------------------------------------------------------

    @Test
    void e33_rechazaLaMismaSolicitudPendiente() throws Exception {
        controlador.contratarOrganizador(ORGANIZADOR, solicitud(ORGANIZADOR, fecha));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> controlador.contratarOrganizador(
                        ORGANIZADOR, solicitud(ORGANIZADOR, fecha)));

        assertEquals(RepositoryContratacion.SOLICITUD_DUPLICADA, error.getMessage());
        assertEquals(1, contar("Solicitud_Contratacion_Organizador"));
    }

    @Test
    void e34_permiteOtraFecha() throws Exception {
        controlador.contratarOrganizador(ORGANIZADOR, solicitud(ORGANIZADOR, fecha));
        controlador.contratarOrganizador(ORGANIZADOR, solicitud(ORGANIZADOR, fecha.plusDays(1)));
        assertEquals(2, contar("Solicitud_Contratacion_Organizador"));
    }

    @Test
    void e35_permiteOtroOrganizador() throws Exception {
        controlador.contratarOrganizador(ORGANIZADOR, solicitud(ORGANIZADOR, fecha));
        controlador.contratarOrganizador(OTRO_ORGANIZADOR, solicitud(OTRO_ORGANIZADOR, fecha));
        assertEquals(2, contar("Solicitud_Contratacion_Organizador"));
    }

    @Test
    void e36_elDuplicadoNoGeneraOtraNotificacion() throws Exception {
        controlador.contratarOrganizador(ORGANIZADOR, solicitud(ORGANIZADOR, fecha));
        assertThrows(IllegalStateException.class,
                () -> controlador.contratarOrganizador(
                        ORGANIZADOR, solicitud(ORGANIZADOR, fecha)));
        assertEquals(1, contar("Notificacion_interna"));
    }

    // ------------------------------------------------------------------
    // Sesión y rol: el actor es el CLIENTE
    // ------------------------------------------------------------------

    @Test
    void e37_sinSesionNoSePuedeUsar() {
        ControllerAdministracion sinSesion = new ControllerAdministracion(servicio);
        assertRechazaTodo(sinSesion);
    }

    @Test
    void e38_administradorNoPuedeContratarOrganizador() {
        assertRechazaTodo(new ControllerAdministracion(
                servicio, sesion(9100, "ADMINISTRADOR", "ACTIVO")));
    }

    @Test
    void e38b_organizadorNoPuedeContratarOrganizador() {
        assertRechazaTodo(new ControllerAdministracion(
                servicio, sesion(USUARIO_ORGANIZADOR, "ORGANIZADOR", "ACTIVO")));
    }

    @Test
    void e38c_proveedorNoPuedeContratarOrganizador() {
        assertRechazaTodo(new ControllerAdministracion(
                servicio, sesion(9101, "PROVEEDOR", "ACTIVO")));
    }

    @Test
    void e39_clienteSuspendidoConSesionAbiertaNoRegistra() throws Exception {
        sql("UPDATE Usuario SET estado = 'SUSPENDIDO' WHERE id_usuario = 9001");

        assertThrows(IllegalArgumentException.class,
                () -> controlador.contratarOrganizador(
                        ORGANIZADOR, solicitud(ORGANIZADOR, fecha)));

        assertEquals(0, contar("Solicitud_Contratacion_Organizador"));
        assertEquals(0, contar("Notificacion_interna"));
    }

    @Test
    void e39b_lasResenasTambienExigenSesionDeCliente() {
        ControllerAdministracion administrador = new ControllerAdministracion(
                servicio, sesion(9100, "ADMINISTRADOR", "ACTIVO"));
        ControllerAdministracion clienteInactivo = new ControllerAdministracion(
                servicio, sesion(CLIENTE, "CLIENTE", "SUSPENDIDO"));

        assertThrows(IllegalStateException.class,
                () -> administrador.consultarResenas(ORGANIZADOR));
        assertThrows(IllegalStateException.class,
                () -> clienteInactivo.consultarResenas(ORGANIZADOR));
    }

    // ------------------------------------------------------------------
    // Aislamiento entre clientes
    // ------------------------------------------------------------------

    @Test
    void e40_unClienteNoVeLasSolicitudesDeOtro() {
        controlador.contratarOrganizador(ORGANIZADOR, solicitud(ORGANIZADOR, fecha));

        ControllerAdministracion otro = new ControllerAdministracion(
                servicio, sesion(OTRO_CLIENTE, "CLIENTE", "ACTIVO"));
        otro.contratarOrganizador(OTRO_ORGANIZADOR, solicitud(OTRO_ORGANIZADOR, fecha));

        Map<String, Object> intento = new HashMap<>();
        intento.put("idCliente", OTRO_CLIENTE);

        List<Contratacion> propias = controlador.consultarContrataciones(intento);

        assertEquals(1, propias.size());
        assertEquals(CLIENTE, propias.get(0).getIdCliente());
        assertEquals(1, otro.consultarContrataciones(null).size());
    }

    @Test
    void e41_misSolicitudesIncluyenElNombreDelOrganizador() {
        controlador.contratarOrganizador(ORGANIZADOR, solicitud(ORGANIZADOR, fecha));

        Contratacion solicitud = controlador.consultarContrataciones(null).get(0);

        assertEquals("Organizador CU26", solicitud.getNombreOrganizador());
        assertEquals("PENDIENTE", solicitud.getEstado());
    }

    // ------------------------------------------------------------------
    // Regresión de consultas existentes
    // ------------------------------------------------------------------

    @Test
    void r1_sinCriteriosSeConservaElConjuntoYElOrden() throws Exception {
        // Orden: calificación promedio, total de reseñas y nombre.
        assertEquals(List.of(OTRO_ORGANIZADOR, ORGANIZADOR), ids(buscar()));

        resena(ORGANIZADOR, CLIENTE, 5, "Excelente", "2026-01-10 10:00:00");
        assertEquals(List.of(ORGANIZADOR, OTRO_ORGANIZADOR), ids(buscar()));

        Map<String, Object> conClaveVacia = filtrosValidos();
        conClaveVacia.put("criterios", null);
        assertEquals(ids(buscar()), ids(controlador.consultarOrganizadores(conClaveVacia)));
    }

    @Test
    void r2_filtrosDeSolicitudesSiguenFuncionandoConElJoin() {
        controlador.contratarOrganizador(ORGANIZADOR, solicitud(ORGANIZADOR, fecha));
        Contratacion medellin = solicitud(OTRO_ORGANIZADOR, fecha);
        medellin.setCiudad("Medellin");
        medellin.setTipoEvento("Lanzamiento");
        controlador.contratarOrganizador(OTRO_ORGANIZADOR, medellin);

        assertEquals(1, repositoryContratacion.listarContrataciones(
                Map.of("ciudad", "medellin")).size());
        assertEquals(1, repositoryContratacion.listarContrataciones(
                Map.of("tipoEvento", "boda")).size());
        assertEquals(2, repositoryContratacion.listarContrataciones(
                Map.of("estado", "pendiente")).size());
        assertEquals(1, repositoryContratacion.listarContrataciones(
                Map.of("idOrganizador", OTRO_ORGANIZADOR)).size());
        assertEquals(2, repositoryContratacion.listarContrataciones(
                Map.of("idCliente", CLIENTE)).size());
        assertEquals(0, repositoryContratacion.listarContrataciones(
                Map.of("idCliente", OTRO_CLIENTE)).size());
    }

    @Test
    void r3_consultasPorClienteEIdentificadorConservanLosCampos() {
        Contratacion datos = solicitud(ORGANIZADOR, fecha);
        controlador.contratarOrganizador(ORGANIZADOR, datos);

        Contratacion porId = repositoryContratacion.buscarContratacion(datos.getIdContratacion());
        Contratacion porCliente = repositoryContratacion.listarContrataciones(CLIENTE).get(0);

        for (Contratacion c : List.of(porId, porCliente)) {
            assertEquals(datos.getIdContratacion(), c.getIdContratacion());
            assertEquals(CLIENTE, c.getIdCliente());
            assertEquals(ORGANIZADOR, c.getIdOrganizador());
            assertEquals(fecha, c.getFechaAproximada());
            assertEquals("Celebracion familiar", c.getCriteriosBusqueda());
            assertNotNull(c.getFechaSolicitud());
        }
        assertNull(repositoryContratacion.buscarContratacion(999999));
    }

    @Test
    void r4_buscarYListarPendientesNoCambian() throws Exception {
        Organizador perfil = repositoryOrganizador.buscarOrganizador(ORGANIZADOR);
        assertEquals("Eventos CU26", perfil.empresa());
        assertEquals(0, BigDecimal.ZERO.compareTo(perfil.calificacionPromedio()));
        assertEquals(0, perfil.totalResenas());
        assertNull(repositoryOrganizador.buscarOrganizador(999999));

        assertTrue(repositoryOrganizador.listarOrganizadoresPendientes().isEmpty());
        sql("UPDATE Organizador SET estado_verificacion = 'PENDIENTE' WHERE id_organizador = 9002");
        assertEquals(List.of(OTRO_ORGANIZADOR),
                ids(repositoryOrganizador.listarOrganizadoresPendientes()));
    }

    @Test
    void r5_constructorCompatibleConEventos() {
        Organizador simple = new Organizador(7, "Laura");
        assertEquals("Laura", simple.toString());
        assertEquals(0, simple.totalResenas());
        assertEquals(BigDecimal.ZERO, simple.tarifaBase());
    }

    @Test
    void r6_comodinesSeTratanComoTextoYSeLimitaLaLongitud() {
        assertEquals(List.of(OTRO_ORGANIZADOR), ids(buscarConCriterios("100%")));
        // Sin escape, "%" y "_" coincidirían con ambos organizadores.
        assertEquals(List.of(OTRO_ORGANIZADOR), ids(buscarConCriterios("%")));
        assertTrue(buscarConCriterios("_").isEmpty());

        assertThrows(IllegalArgumentException.class,
                () -> buscarConCriterios("x".repeat(501)));

        Contratacion larga = solicitud(ORGANIZADOR, fecha);
        larga.setCriteriosBusqueda("x".repeat(501));
        assertThrows(IllegalArgumentException.class,
                () -> controlador.contratarOrganizador(ORGANIZADOR, larga));
    }

    @Test
    void r7_unaSolicitudQueNoEstaPendienteNoBloquea() throws Exception {
        controlador.contratarOrganizador(ORGANIZADOR, solicitud(ORGANIZADOR, fecha));
        sql("UPDATE Solicitud_Contratacion_Organizador SET estado = 'RECHAZADA'");

        controlador.contratarOrganizador(ORGANIZADOR, solicitud(ORGANIZADOR, fecha));
        assertEquals(2, contar("Solicitud_Contratacion_Organizador"));
    }

    @Test
    void r8_eventoDeOtroOrganizadorNoBloquea() throws Exception {
        evento(OTRO_ORGANIZADOR, fecha, "PLANEADO");

        assertTrue(ids(buscar()).contains(ORGANIZADOR));
        controlador.contratarOrganizador(ORGANIZADOR, solicitud(ORGANIZADOR, fecha));
        assertEquals(1, contar("Solicitud_Contratacion_Organizador"));
    }

    // ------------------------------------------------------------------
    // Auxiliares
    // ------------------------------------------------------------------

    private void assertNoDisponibleSinRegistro(String mensaje) throws Exception {
        OrganizadorNoDisponibleException error = assertThrows(
                OrganizadorNoDisponibleException.class,
                () -> controlador.contratarOrganizador(
                        ORGANIZADOR, solicitud(ORGANIZADOR, fecha)));

        assertEquals(mensaje, error.getMessage());
        assertEquals(0, contar("Solicitud_Contratacion_Organizador"));
        assertEquals(0, contar("Notificacion_interna"));
    }

    private void assertRechazaTodo(ControllerAdministracion controller) {
        assertThrows(IllegalStateException.class,
                () -> controller.consultarOrganizadores(filtrosValidos()));
        assertThrows(IllegalStateException.class,
                () -> controller.seleccionarOrganizador(ORGANIZADOR));
        assertThrows(IllegalStateException.class,
                () -> controller.consultarResenas(ORGANIZADOR));
        assertThrows(IllegalStateException.class,
                () -> controller.verificarDisponibilidadOrganizador(ORGANIZADOR, fecha));
        assertThrows(IllegalStateException.class,
                () -> controller.contratarOrganizador(
                        ORGANIZADOR, solicitud(ORGANIZADOR, fecha)));
        assertThrows(IllegalStateException.class,
                () -> controller.consultarContrataciones(null));
        assertEquals(0, contar("Solicitud_Contratacion_Organizador"));
    }

    private SesionUsuario sesion(int idUsuario, String tipo, String estado) {
        SesionUsuario sesion = new SesionUsuario();
        sesion.setIdUsuario(idUsuario);
        sesion.setNombre("Sesion " + tipo);
        sesion.setTipoUsuario(tipo);
        sesion.setEstadoUsuario(estado);
        return sesion;
    }

    private Contratacion solicitud(int idOrganizador, LocalDate fechaEvento) {
        return new Contratacion(
                0,
                idOrganizador,
                "Boda",
                "Bogota",
                fechaEvento,
                new BigDecimal("2000000"),
                "Celebracion familiar");
    }

    private Map<String, Object> filtrosValidos() {
        Map<String, Object> filtros = new HashMap<>();
        filtros.put("ciudad", "Bogota");
        filtros.put("tipoEvento", "Boda");
        filtros.put("presupuestoEstimado", new BigDecimal("2000000"));
        filtros.put("fechaAproximada", fecha);
        return filtros;
    }

    private List<Organizador> buscar() {
        return controlador.consultarOrganizadores(filtrosValidos());
    }

    private List<Organizador> buscarConCriterios(String criterios) {
        Map<String, Object> filtros = filtrosValidos();
        filtros.put("criterios", criterios);
        return controlador.consultarOrganizadores(filtros);
    }

    private List<Integer> ids(List<Organizador> organizadores) {
        return organizadores.stream().map(Organizador::id).toList();
    }

    private void evento(int idOrganizador, LocalDate dia, String estado) throws Exception {
        sql("INSERT INTO Evento (id_organizador, nombre, fecha_inicio, fecha_fin, "
                + "estado, fecha_creacion) VALUES (" + idOrganizador + ", 'Evento CU26', "
                + "TIMESTAMP '" + dia + " 10:00:00', TIMESTAMP '" + dia + " 20:00:00', '"
                + estado + "', CURRENT_TIMESTAMP)");
    }

    private void resena(int idOrganizador, int idCliente, int calificacion,
                        String comentario, String fechaResena) throws Exception {
        sql("INSERT INTO Resena_Organizador (id_organizador, id_cliente, calificacion, "
                + "comentario, fecha) VALUES (" + idOrganizador + ", " + idCliente + ", "
                + calificacion + ", '" + comentario + "', TIMESTAMP '" + fechaResena + "')");
    }

    private void sql(String sentencia) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection(url);
             Statement statement = connection.createStatement()) {
            statement.execute(sentencia);
        }
    }

    private int contar(String tablaYCondicion) {
        try (Connection connection = DatabaseConnection.getConnection(url);
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT COUNT(*) FROM " + tablaYCondicion)) {
            result.next();
            return result.getInt(1);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}

package services;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import entities.Campana;
import entities.Segmentacion;
import entities.ClienteMarketing;
import entities.SegmentoCalculado;
import entities.SolicitudEvento;
import persistencia.DatabaseConnection;
import repositories.RepositoryMarketing;

class ServiceMarketingH2Test {

    private static final int ADMINISTRADOR = 9301;
    private static final int CLIENTE_CON_MARKETING = 9302;
    private static final int CLIENTE_SIN_MARKETING = 9303;
    private static final int SEGMENTO = 9401;
    private static final LocalDate HOY = LocalDate.of(2026, 11, 16);

    private String url;
    private ServiceMarketing service;

    @BeforeEach
    void preparar() throws Exception {
        // la base vive entre conexiones del servicio y tiene un nombre único en memoria
        url = "jdbc:h2:mem:marketing_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";

        // creamos el administrador, los clientes y el presupuesto de prueba
        insertarUsuario(ADMINISTRADOR, "Administradora", "ADMINISTRADOR", "ACTIVO", false);
        insertarUsuario(CLIENTE_CON_MARKETING, "Cliente uno", "CLIENTE", "ACTIVO", true);
        insertarUsuario(CLIENTE_SIN_MARKETING, "Cliente dos", "CLIENTE", "ACTIVO", false);
        ejecutar("INSERT INTO Presupuesto_marketing (id_presupuesto, disponible) "
                + "VALUES (1, 1000.00)");

        service = new ServiceMarketing(
                new RepositoryMarketing(url),
                // fijamos la fecha para que las campañas no dependan del día de ejecución
                Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneOffset.UTC));
    }

    @AfterEach
    void limpiar() throws Exception {
        if (url != null) {
            // usamos jdbc directo para apagar esta base temporal sin volver a inicializarla
            try (Connection connection = DriverManager.getConnection(url, "sa", "");
                    Statement statement = connection.createStatement()) {
                statement.execute("SHUTDOWN");
            }
        }
    }

    @Test
    void segmentaClientesPorPreferenciaYConservaElResultadoEnH2() throws Exception {
        ejecutar("INSERT INTO Busqueda_evento (id_usuario, tipo, fecha_busqueda) "
                + "VALUES (9302, ' Bodas ', CURRENT_TIMESTAMP)");
        ejecutar("INSERT INTO Busqueda_evento (id_usuario, tipo, fecha_busqueda) "
                + "VALUES (9302, 'Bodas', CURRENT_TIMESTAMP)");
        ejecutar("INSERT INTO Busqueda_evento (id_usuario, tipo, fecha_busqueda) "
                + "VALUES (9303, 'Bodas', CURRENT_TIMESTAMP)");

        List<Segmentacion> creados = service.segmentarClientes(ADMINISTRADOR);

        assertEquals(1, creados.size());
        Segmentacion segmento = creados.get(0);
        assertEquals("Bogota - Bodas", segmento.getNombre());
        assertEquals(List.of(CLIENTE_CON_MARKETING, CLIENTE_SIN_MARKETING), segmento.getIdsClientes());
        assertTrue(segmento.getIdSegmento() > 0);

        List<Segmentacion> persistidos = service.listarSegmentosActivos();
        assertEquals(1, persistidos.size());
        assertEquals(segmento.getIdSegmento(), persistidos.get(0).getIdSegmento());
        assertEquals(segmento.getIdsClientes(), persistidos.get(0).getIdsClientes());
        assertEquals(Segmentacion.ESTADO_ACTIVA, persistidos.get(0).getEstado());
        assertThrows(IllegalArgumentException.class, () -> service.segmentarClientes(9999));
    }

    @Test
    void segmentaPorPaisCiudadYTipoNormalizandoYConservandoGruposDistintos()
            throws Exception {
        insertarUsuario(9304, "Cliente Chile", "CLIENTE", "ACTIVO", true);
        ejecutar("UPDATE Usuario SET pais = 'Chile' WHERE id_usuario = 9304");
        ejecutar("UPDATE Usuario SET ciudad = 'Medellin' WHERE id_usuario = 9303");
        ejecutar("INSERT INTO Busqueda_evento (id_usuario, tipo, fecha_busqueda) "
                + "VALUES (9302, ' Bodas ', CURRENT_TIMESTAMP)");
        ejecutar("INSERT INTO Busqueda_evento (id_usuario, tipo, fecha_busqueda) "
                + "VALUES (9302, 'Bodas', CURRENT_TIMESTAMP)");
        ejecutar("INSERT INTO Busqueda_evento (id_usuario, tipo, fecha_busqueda) "
                + "VALUES (9302, 'Quince años', CURRENT_TIMESTAMP)");
        ejecutar("INSERT INTO Busqueda_evento (id_usuario, tipo, fecha_busqueda) "
                + "VALUES (9303, 'Bodas', CURRENT_TIMESTAMP)");
        ejecutar("INSERT INTO Busqueda_evento (id_usuario, tipo, fecha_busqueda) "
                + "VALUES (9304, 'Bodas', CURRENT_TIMESTAMP)");

        List<Segmentacion> segmentos = service.segmentarClientes(ADMINISTRADOR);

        assertEquals(4, segmentos.size());
        assertEquals("AUTO|pais=Chile|ciudad=Bogota|tipo=Bodas",
                segmentos.get(0).getCriterios());
        assertEquals(List.of(9304), segmentos.get(0).getIdsClientes());
        assertEquals("AUTO|pais=Colombia|ciudad=Bogota|tipo=Bodas",
                segmentos.get(1).getCriterios());
        assertEquals(List.of(9302), segmentos.get(1).getIdsClientes());
        assertEquals("AUTO|pais=Colombia|ciudad=Bogota|tipo=Quince años",
                segmentos.get(2).getCriterios());
        assertEquals(List.of(9302), segmentos.get(2).getIdsClientes());
        assertEquals("AUTO|pais=Colombia|ciudad=Medellin|tipo=Bodas",
                segmentos.get(3).getCriterios());
        assertEquals(List.of(9303), segmentos.get(3).getIdsClientes());
        assertEquals(4, service.listarSegmentosActivos().size());
    }

    @Test
    void calcularSegmentosIgnoraClientesYPreferenciasNulosOVacios() {
        assertThrows(IllegalArgumentException.class,
                () -> service.calcularSegmentos(null, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> service.calcularSegmentos(List.of(), null));

        List<ClienteMarketing> clientes = new ArrayList<>();
        clientes.add(null);
        clientes.add(new ClienteMarketing(9304, "Sin ciudad", "Chile", null, true));
        clientes.add(new ClienteMarketing(9305, "Ciudad vacía", "Chile", "  ", true));
        clientes.add(new ClienteMarketing(9306, "País desconocido", null, " Bogota ", false));

        List<SolicitudEvento> solicitudes = new ArrayList<>();
        solicitudes.add(null);
        solicitudes.add(new SolicitudEvento(9999, "Bodas", 1));
        solicitudes.add(new SolicitudEvento(9306, null, 1));
        solicitudes.add(new SolicitudEvento(9306, "  ", 1));
        solicitudes.add(new SolicitudEvento(9306, "Bodas", 0));
        solicitudes.add(new SolicitudEvento(9306, " Bodas ", 1));

        List<SegmentoCalculado> segmentos
                = service.calcularSegmentos(clientes, solicitudes);

        assertEquals(1, segmentos.size());
        SegmentoCalculado segmento = segmentos.get(0);
        assertEquals("No especificado", segmento.getPais());
        assertEquals("Bogota", segmento.getCiudad());
        assertEquals("Bodas", segmento.getTipoEvento());
        assertEquals(List.of(9306), segmento.getIdsClientes());
    }

    @Test
    void segmentaClienteSinPaisYPersisteLaUbicacionPredeterminada() throws Exception {
        ejecutar("INSERT INTO Usuario "
                + "(id_usuario, nombre, usuario, contraseña, estado, fecha_registro, "
                + "acepta_marketing, tipo_usuario, pais, ciudad) "
                + "VALUES (9304, 'Cliente sin país', 'usuario9304', 'clave', "
                + "'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'CLIENTE', NULL, 'Bogota')");
        ejecutar("INSERT INTO Busqueda_evento (id_usuario, tipo, fecha_busqueda) "
                + "VALUES (9304, 'Quince años', CURRENT_TIMESTAMP)");

        List<Segmentacion> creados = service.segmentarClientes(ADMINISTRADOR);

        assertEquals(1, creados.size());
        Segmentacion segmento = creados.get(0);
        assertEquals("Bogota - Quince años", segmento.getNombre());
        assertEquals(List.of(9304), segmento.getIdsClientes());
        assertEquals("AUTO|pais=No especificado|ciudad=Bogota|tipo=Quince años",
                valor("SELECT criterios FROM Segmentacion "
                        + "WHERE id_segmento = " + segmento.getIdSegmento()));
        assertEquals("1", valor("SELECT COUNT(*) FROM Segmento_cliente "
                + "WHERE id_segmento = " + segmento.getIdSegmento()
                + " AND id_usuario = 9304"));
    }

    @Test
    void creaCampanaYComprometePresupuestoRedondeado() throws Exception {
        insertarSegmentoActivo();
        Campana solicitada = nuevaCampana(
                ADMINISTRADOR, SEGMENTO, "  Oferta bodas  ", "  15%  ",
                HOY.plusDays(2), HOY.plusDays(10), new BigDecimal("125.005"));

        Campana creada = service.crearCampana(solicitada);

        assertNotNull(creada.getIdCampana());
        assertEquals("Oferta bodas", creada.getNombre());
        assertEquals("15%", creada.getTipoOferta());
        assertEquals(Campana.ESTADO_PROGRAMADA, creada.getEstado());
        assertEquals(new BigDecimal("125.01"), creada.getPresupuestoMaximo());
        assertEquals(0, service.obtenerPresupuestoDisponible().compareTo(new BigDecimal("874.99")));

        Campana leida = service.obtenerCampana(creada.getIdCampana());
        assertEquals(SEGMENTO, leida.getIdSegmento());
        assertEquals("Segmento bodas", leida.getNombreSegmento());
        assertEquals(Campana.ESTADO_PROGRAMADA, service.listarCampanas().get(0).getEstado());
        assertEquals(0, service.obtenerPresupuestoDisponible()
                .compareTo(new BigDecimal("874.99")));
    }

    @Test
    void validaAdministradorFechasSegmentoYPresupuesto() throws Exception {
        insertarSegmentoActivo();

        assertThrows(IllegalArgumentException.class, () -> service.validarCampana(null));
        assertThrows(IllegalArgumentException.class,
                () -> service.validarCampana(nuevaCampana(
                        9999, SEGMENTO, "Oferta", "15%",
                        HOY, HOY.plusDays(1), new BigDecimal("10"))));
        assertThrows(IllegalArgumentException.class,
                () -> service.validarCampana(nuevaCampana(
                        ADMINISTRADOR, SEGMENTO, "Oferta", "15%",
                        HOY, HOY, new BigDecimal("10"))));
        assertThrows(IllegalArgumentException.class,
                () -> service.validarCampana(nuevaCampana(
                        ADMINISTRADOR, 9998, "Oferta", "15%",
                        HOY, HOY.plusDays(1), new BigDecimal("10"))));
        assertThrows(IllegalArgumentException.class,
                () -> service.validarCampana(nuevaCampana(
                        ADMINISTRADOR, 0, "Oferta", "15%",
                        HOY, HOY.plusDays(1), new BigDecimal("10"))));
        assertThrows(IllegalArgumentException.class,
                () -> service.validarCampana(nuevaCampana(
                        ADMINISTRADOR, SEGMENTO, "Oferta", "15%",
                        HOY, HOY.plusDays(1), new BigDecimal("1000.01"))));
    }

    @Test
    void rechazaCamposYFechasInvalidosSinComprometerPresupuesto() throws Exception {
        insertarSegmentoActivo();
        ejecutar("INSERT INTO Segmentacion "
                + "(id_segmento, id_usuario, nombre, criterios, descripcion, estados) "
                + "VALUES (9402, 9301, 'Segmento inactivo', 'manual', "
                + "'No disponible', 'INACTIVA')");

        assertThrows(IllegalArgumentException.class,
                () -> service.crearCampana(nuevaCampana(
                        ADMINISTRADOR, SEGMENTO, " ", "15%",
                        HOY.plusDays(1), HOY.plusDays(2), new BigDecimal("10"))));
        assertThrows(IllegalArgumentException.class,
                () -> service.crearCampana(nuevaCampana(
                        ADMINISTRADOR, SEGMENTO, null, "15%",
                        HOY.plusDays(1), HOY.plusDays(2), new BigDecimal("10"))));
        assertThrows(IllegalArgumentException.class,
                () -> service.crearCampana(nuevaCampana(
                        ADMINISTRADOR, SEGMENTO, "Oferta", " ",
                        HOY.plusDays(1), HOY.plusDays(2), new BigDecimal("10"))));
        assertThrows(IllegalArgumentException.class,
                () -> service.crearCampana(nuevaCampana(
                        ADMINISTRADOR, SEGMENTO, "Oferta", null,
                        HOY.plusDays(1), HOY.plusDays(2), new BigDecimal("10"))));
        assertThrows(IllegalArgumentException.class,
                () -> service.crearCampana(nuevaCampana(
                        ADMINISTRADOR, SEGMENTO, "Oferta", "15%",
                        null, HOY.plusDays(2), new BigDecimal("10"))));
        assertThrows(IllegalArgumentException.class,
                () -> service.crearCampana(nuevaCampana(
                        ADMINISTRADOR, SEGMENTO, "Oferta", "15%",
                        HOY.plusDays(1), null, new BigDecimal("10"))));
        assertThrows(IllegalArgumentException.class,
                () -> service.crearCampana(nuevaCampana(
                        ADMINISTRADOR, SEGMENTO, "Oferta", "15%",
                        HOY.plusDays(2), HOY.plusDays(1), new BigDecimal("10"))));
        assertThrows(IllegalArgumentException.class,
                () -> service.crearCampana(nuevaCampana(
                        ADMINISTRADOR, SEGMENTO, "Oferta", "15%",
                        HOY.plusDays(1), HOY.plusDays(2), BigDecimal.ZERO)));
        assertThrows(IllegalArgumentException.class,
                () -> service.crearCampana(nuevaCampana(
                        ADMINISTRADOR, SEGMENTO, "Oferta", "15%",
                        HOY.plusDays(1), HOY.plusDays(2), new BigDecimal("-1"))));
        assertThrows(IllegalArgumentException.class,
                () -> service.crearCampana(nuevaCampana(
                        ADMINISTRADOR, SEGMENTO, "Oferta", "15%",
                        HOY.plusDays(1), HOY.plusDays(2), null)));
        assertThrows(IllegalArgumentException.class,
                () -> service.crearCampana(nuevaCampana(
                        ADMINISTRADOR, 9402, "Oferta", "15%",
                        HOY.plusDays(1), HOY.plusDays(2), new BigDecimal("10"))));

        assertEquals(0, consultarEntero("SELECT COUNT(*) FROM Campana"));
        assertEquals(0, service.obtenerPresupuestoDisponible()
                .compareTo(new BigDecimal("1000.00")));
    }

    @Test
    void distribuyeMidePausaReanudaYFinalizaCampanaConEstadoPersistido() throws Exception {
        insertarSegmentoActivo();
        Campana creada = service.crearCampana(nuevaCampana(
                ADMINISTRADOR, SEGMENTO, "Oferta", "15%",
                HOY.plusDays(1), HOY.plusDays(10), new BigDecimal("100")));
        int id = creada.getIdCampana();

        service.lanzarCampana(id);
        assertEquals(Campana.ESTADO_ACTIVA, service.obtenerCampana(id).getEstado());
        assertEquals(1, consultarEntero(
                "SELECT COUNT(*) FROM Envio_Campana WHERE id_campana = " + id));
        assertTrue(service.registrarVisualizacion(id));
        assertTrue(service.registrarConversion(id));
        assertEquals(1, service.obtenerCampana(id).getVisualizaciones());
        assertEquals(1, service.obtenerCampana(id).getConversiones());

        service.pausarCampana(ADMINISTRADOR, id);
        assertEquals(Campana.ESTADO_PAUSADA, service.obtenerCampana(id).getEstado());
        assertFalse(service.registrarVisualizacion(id));
        service.reanudarCampana(ADMINISTRADOR, id);
        assertEquals(Campana.ESTADO_ACTIVA, service.obtenerCampana(id).getEstado());

        List<Campana> resumen = new ArrayList<>();
        // guardamos el resumen para comprobar que el servicio avisa al finalizar
        service.setAlFinalizarCampana(campana -> resumen.add(campana));
        service.finalizarCampana(id);

        assertEquals(Campana.ESTADO_FINALIZADA, service.obtenerCampana(id).getEstado());
        assertEquals(1, resumen.size());
        assertEquals(id, resumen.get(0).getIdCampana());
        assertEquals(Campana.ESTADO_FINALIZADA, resumen.get(0).getEstado());
        assertThrows(IllegalArgumentException.class,
                () -> service.pausarCampana(9999, id));
    }

    @Test
    void rechazaTransicionesInvalidasSinCambiarElEstadoDeLaCampana() throws Exception {
        insertarSegmentoActivo();
        Campana creada = service.crearCampana(nuevaCampana(
                ADMINISTRADOR, SEGMENTO, "Oferta", "15%",
                HOY.plusDays(2), HOY.plusDays(10), new BigDecimal("50")));
        int id = creada.getIdCampana();

        IllegalArgumentException pausaProgramada = assertThrows(
                IllegalArgumentException.class,
                () -> service.pausarCampana(ADMINISTRADOR, id));
        assertTrue(pausaProgramada.getMessage().contains("no puede pausarse"));
        assertEquals(Campana.ESTADO_PROGRAMADA, service.obtenerCampana(id).getEstado());

        assertThrows(IllegalArgumentException.class,
                () -> service.reanudarCampana(ADMINISTRADOR, id));
        assertEquals(Campana.ESTADO_PROGRAMADA,
                service.obtenerCampana(id).getEstado());
        assertThrows(IllegalArgumentException.class,
                () -> service.finalizarCampana(id));
        assertEquals(Campana.ESTADO_PROGRAMADA,
                service.obtenerCampana(id).getEstado());
        assertThrows(IllegalArgumentException.class,
                () -> service.obtenerCampana(9999));

        service.lanzarCampana(id);
        IllegalArgumentException activacionDuplicada = assertThrows(
                IllegalArgumentException.class,
                () -> service.lanzarCampana(id));
        assertTrue(activacionDuplicada.getMessage().contains("no puede activarse"));
        assertEquals(Campana.ESTADO_ACTIVA, service.obtenerCampana(id).getEstado());

        service.pausarCampana(ADMINISTRADOR, id);
        IllegalArgumentException pausaDuplicada = assertThrows(
                IllegalArgumentException.class,
                () -> service.pausarCampana(ADMINISTRADOR, id));
        assertTrue(pausaDuplicada.getMessage().contains("no puede pausarse"));
        assertEquals(Campana.ESTADO_PAUSADA, service.obtenerCampana(id).getEstado());
        assertEquals(0, service.obtenerPresupuestoDisponible()
                .compareTo(new BigDecimal("950.00")));
    }

    @Test
    void finalizaCampanasConObservadorNuloOConFalloSinRevertirEstado()
            throws Exception {
        insertarSegmentoActivo();
        Campana observadorNulo = service.crearCampana(nuevaCampana(
                ADMINISTRADOR, SEGMENTO, "Sin observador", "10%",
                HOY, HOY.plusDays(5), new BigDecimal("25")));
        service.lanzarCampana(observadorNulo.getIdCampana());
        service.setAlFinalizarCampana(null);
        service.finalizarCampana(observadorNulo.getIdCampana());
        assertEquals(Campana.ESTADO_FINALIZADA,
                service.obtenerCampana(observadorNulo.getIdCampana()).getEstado());

        Campana observadorConFallo = service.crearCampana(nuevaCampana(
                ADMINISTRADOR, SEGMENTO, "Observador con fallo", "10%",
                HOY, HOY.plusDays(5), new BigDecimal("25")));
        service.lanzarCampana(observadorConFallo.getIdCampana());
        service.setAlFinalizarCampana(campana -> {
            throw new IllegalStateException("fallo controlado del observador");
        });
        service.finalizarCampana(observadorConFallo.getIdCampana());

        assertEquals(Campana.ESTADO_FINALIZADA,
                service.obtenerCampana(observadorConFallo.getIdCampana()).getEstado());
    }

    @Test
    void verificaCampanasProgramadasYVencidasSegunElRelojControlado() throws Exception {
        insertarSegmentoActivo();
        Campana iniciaHoy = service.crearCampana(nuevaCampana(
                ADMINISTRADOR, SEGMENTO, "Inicia hoy", "15%",
                HOY, HOY.plusDays(5), new BigDecimal("50")));
        Campana futura = service.crearCampana(nuevaCampana(
                ADMINISTRADOR, SEGMENTO, "Inicia después", "12%",
                HOY.plusDays(1), HOY.plusDays(5), new BigDecimal("50")));
        Campana vencida = service.crearCampana(nuevaCampana(
                ADMINISTRADOR, SEGMENTO, "Vencida", "10%",
                HOY.minusDays(3), HOY, new BigDecimal("50")));

        service.verificarCampanas();

        assertEquals(Campana.ESTADO_ACTIVA,
                service.obtenerCampana(iniciaHoy.getIdCampana()).getEstado());
        assertEquals(Campana.ESTADO_FINALIZADA,
                service.obtenerCampana(vencida.getIdCampana()).getEstado());
        assertEquals(Campana.ESTADO_PROGRAMADA,
                service.obtenerCampana(futura.getIdCampana()).getEstado());
    }

    @Test
    void finalizaCampanasActivasYEnPausaCuandoLlegaSuFechaFin() throws Exception {
        insertarSegmentoActivo();
        Campana activa = service.crearCampana(nuevaCampana(
                ADMINISTRADOR, SEGMENTO, "Activa vencida", "15%",
                HOY.minusDays(2), HOY, new BigDecimal("100")));
        Campana pausada = service.crearCampana(nuevaCampana(
                ADMINISTRADOR, SEGMENTO, "Pausada vencida", "10%",
                HOY.minusDays(2), HOY, new BigDecimal("100")));
        service.lanzarCampana(activa.getIdCampana());
        service.lanzarCampana(pausada.getIdCampana());
        service.pausarCampana(ADMINISTRADOR, pausada.getIdCampana());

        service.verificarCampanas();

        assertEquals(Campana.ESTADO_FINALIZADA,
                service.obtenerCampana(activa.getIdCampana()).getEstado());
        assertEquals(Campana.ESTADO_FINALIZADA,
                service.obtenerCampana(pausada.getIdCampana()).getEstado());
        assertEquals(2, consultarEntero("SELECT COUNT(*) FROM Campana "
                + "WHERE estado = 'FINALIZADA'"));
    }

    @Test
    void noReanudaCampanaPausadaQueYaVencio() throws Exception {
        insertarSegmentoActivo();
        Campana vencida = service.crearCampana(nuevaCampana(
                ADMINISTRADOR, SEGMENTO, "Campaña vencida", "10%",
                HOY.minusDays(2), HOY, new BigDecimal("50")));
        int idCampana = vencida.getIdCampana();

        service.lanzarCampana(idCampana);
        service.pausarCampana(ADMINISTRADOR, idCampana);

        assertThrows(IllegalArgumentException.class,
                () -> service.reanudarCampana(ADMINISTRADOR, idCampana));

        assertEquals(Campana.ESTADO_PAUSADA, service.obtenerCampana(idCampana).getEstado());
        assertEquals(0, service.obtenerPresupuestoDisponible()
                .compareTo(new BigDecimal("950.00")));
    }

    @Test
    void constructorSimpleYPlanificadorInactivoNoCambianLasCampanas()
            throws Exception {
        ServiceMarketing sinRelojFijo = new ServiceMarketing(
                new RepositoryMarketing(url));

        assertFalse(sinRelojFijo.planificadorActivo());
        sinRelojFijo.detenerPlanificador();
        assertFalse(sinRelojFijo.planificadorActivo());
        assertTrue(sinRelojFijo.listarCampanas().isEmpty());
    }

    @Test
    void iniciarPlanificadorEsIdempotenteYDetenerloEsSeguro() {
        assertFalse(service.planificadorActivo());

        service.detenerPlanificador();
        assertFalse(service.planificadorActivo());

        service.iniciarPlanificador(60);
        try {
            assertTrue(service.planificadorActivo());
            service.iniciarPlanificador(60);
            assertTrue(service.planificadorActivo());
        } finally {
            service.detenerPlanificador();
        }

        assertFalse(service.planificadorActivo());
        service.detenerPlanificador();
        assertFalse(service.planificadorActivo());
    }

    @Test
    void tareaProgramadaAbsorbeFallasDeConexionYSePuedeDetenerDeterministicamente() {
        PlanificadorControlado planificador = new PlanificadorControlado();
        String baseNoDisponible = "jdbc:h2:mem:marketing_ausente_"
                + UUID.randomUUID().toString().replace("-", "") + ";AUTO_SERVER=TRUE";
        assertThrows(SQLException.class,
                () -> DatabaseConnection.getConnection(baseNoDisponible));
        ServiceMarketing servicioConFalla = new ServiceMarketing(
                new RepositoryMarketing(baseNoDisponible),
                Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneOffset.UTC),
                () -> planificador);

        servicioConFalla.iniciarPlanificador(17);
        servicioConFalla.iniciarPlanificador(30);

        assertEquals(17, planificador.periodoSegundos);
        assertEquals(1, planificador.programaciones);
        assertNotNull(planificador.tarea);
        assertDoesNotThrow(planificador.tarea::run);
        assertTrue(servicioConFalla.planificadorActivo());

        servicioConFalla.detenerPlanificador();
        servicioConFalla.detenerPlanificador();

        assertEquals(1, planificador.detenciones);
        assertFalse(servicioConFalla.planificadorActivo());
    }

    @Test
    void desactivaSegmentosAutomaticosSiNoQuedanGruposElegibles() throws Exception {
        insertarSegmentoActivo();
        ejecutar("INSERT INTO Segmentacion "
                + "(id_segmento, id_usuario, nombre, criterios, descripcion, estados) "
                + "VALUES (9402, 9301, 'Segmento automático anterior', '"
                + Segmentacion.PREFIJO_CRITERIOS_AUTOMATICOS
                + "pais=Colombia|ciudad=Bogota|tipo=Bodas', 'Segmento previo', 'ACTIVA')");
        ejecutar("UPDATE Usuario SET ciudad = NULL "
                + "WHERE id_usuario IN (9302, 9303)");

        List<Segmentacion> creados = service.segmentarClientes(ADMINISTRADOR);

        assertTrue(creados.isEmpty());
        assertEquals("INACTIVA", valor("SELECT estados FROM Segmentacion "
                + "WHERE id_segmento = 9402"));
        List<Segmentacion> activos = service.listarSegmentosActivos();
        assertEquals(1, activos.size());
        assertEquals(SEGMENTO, activos.get(0).getIdSegmento());
    }

    private static final class PlanificadorControlado implements ServiceMarketing.Planificador {
        private Runnable tarea;
        private long periodoSegundos;
        private int programaciones;
        private int detenciones;

        @Override
        public void scheduleWithFixedDelay(Runnable tarea, long periodoSegundos) {
            this.tarea = tarea;
            this.periodoSegundos = periodoSegundos;
            programaciones++;
        }

        @Override
        public void shutdownNow() {
            detenciones++;
        }
    }

    private void insertarSegmentoActivo() throws Exception {
        ejecutar("INSERT INTO Segmentacion "
                + "(id_segmento, id_usuario, nombre, criterios, descripcion, estados) "
                + "VALUES (9401, 9301, 'Segmento bodas', 'manual', 'Clientes de bodas', 'ACTIVA')");
        ejecutar("INSERT INTO Segmento_cliente (id_segmento, id_usuario) VALUES (9401, 9302)");
        ejecutar("INSERT INTO Segmento_cliente (id_segmento, id_usuario) VALUES (9401, 9303)");
    }

    private Campana nuevaCampana(
            int idAdministrador,
            int idSegmento,
            String nombre,
            String oferta,
            LocalDate inicio,
            LocalDate fin,
            BigDecimal presupuesto) {
        return new Campana(
                idAdministrador, idSegmento, nombre, oferta, inicio, fin, presupuesto);
    }

    private void insertarUsuario(
            int id, String nombre, String tipo, String estado, boolean aceptaMarketing) throws Exception {
        ejecutar("INSERT INTO Usuario "
                + "(id_usuario, nombre, usuario, contraseña, estado, fecha_registro, "
                + "acepta_marketing, tipo_usuario, pais, ciudad) VALUES ("
                + id + ", '" + nombre + "', 'usuario" + id + "', 'clave', '"
                + estado + "', CURRENT_TIMESTAMP, " + aceptaMarketing + ", '"
                + tipo + "', 'Colombia', 'Bogota')");
    }

    private void ejecutar(String sql) throws Exception {
        // preparamos en h2 los datos que necesita cada escenario
        try (Connection connection = DatabaseConnection.getConnection(url);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private int consultarEntero(String sql) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection(url);
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getInt(1);
        }
    }

    private String valor(String sql) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection(url);
                Statement statement = connection.createStatement();
                ResultSet resultado = statement.executeQuery(sql)) {
            assertTrue(resultado.next());
            return resultado.getString(1);
        }
    }
}

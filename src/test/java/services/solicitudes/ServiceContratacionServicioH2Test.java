package services.solicitudes;

import entities.SesionUsuario;
import entities.contrataciones.ConfirmacionContratacion;
import entities.contrataciones.DisponibilidadContratacion;
import entities.contrataciones.PresupuestoContratacion;
import entities.contrataciones.ResumenContratacionServicio;
import inicializacion.CargarDatosEjemplo;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import persistencia.DatabaseConnection;
import persistencia.PropiedadSistemaTemporal;
import repositories.solicitudes.RepositoryContratacionServicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// estos repositorios usan nexteventux.db.url cuando no reciben una url
@ResourceLock(Resources.SYSTEM_PROPERTIES)
class ServiceContratacionServicioH2Test {

    private static final int ORGANIZADOR = 3;
    private static final int USUARIO_ORGANIZADOR = 8;
    private static final int EVENTO = 3;
    private static final int PROPUESTA = 6;

    private String url;
    private ServiceContratacionServicio service;
    private PropiedadSistemaTemporal propiedadUrlBaseDatos;

    @BeforeEach
    void preparar() throws Exception {
        // la base vive entre conexiones del servicio y tiene un nombre único en memoria
        url = "jdbc:h2:mem:contratacion_servicio_" + UUID.randomUUID()
                + ";DB_CLOSE_DELAY=-1";
        // los repositorios de contratación leen esta propiedad global
        propiedadUrlBaseDatos
                = new PropiedadSistemaTemporal("nexteventux.db.url", url);

        // cargamos el seed solo en esta h2 temporal vacía
        try (Connection connection = DatabaseConnection.getConnection(url)) {
            CargarDatosEjemplo.cargar(connection);
        }

        // movemos el evento y su disponibilidad a una fecha futura para no depender del seed
        LocalDate fechaEvento = LocalDate.now().plusDays(30);
        ejecutar("UPDATE Evento SET fecha_inicio = TIMESTAMP '" + fechaEvento
                + " 17:00:00', fecha_fin = TIMESTAMP '" + fechaEvento
                + " 23:00:00' WHERE id_evento = " + EVENTO);
        ejecutar("UPDATE Disponibilidad_Servicio SET fecha = DATE '" + fechaEvento
                + "', hora_inicio = TIME '16:00:00', hora_fin = TIME '23:30:00' "
                + "WHERE id_disponibilidad = 101");

        service = crearServicio(sesionOrganizador());
    }

    @AfterEach
    void limpiar() throws Exception {
        try {
            if (url != null) {
                // usamos jdbc directo para apagar esta base temporal sin volver a inicializarla
                try (Connection connection
                        = DriverManager.getConnection(url, "sa", "");
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
    void revisaResumenDisponibilidadYPresupuestoSinModificarLaSolicitud() throws Exception {
        ResumenContratacionServicio resumen
                = service.consultarResumen(ORGANIZADOR, PROPUESTA);

        assertEquals(PROPUESTA, resumen.idPropuesta());
        assertEquals(2, resumen.numeroVersion());
        assertEquals("Flash Studio Fotografía", resumen.nombreProveedor());
        assertEquals("Cobertura fotográfica de 8 horas con galería digital "
                + "y 300 fotos editadas", resumen.alcance());

        DisponibilidadContratacion disponibilidad
                = service.verificarDisponibilidad(ORGANIZADOR, PROPUESTA);
        assertTrue(disponibilidad.disponible(), disponibilidad.mensaje());

        PresupuestoContratacion presupuesto
                = service.verificarPresupuesto(ORGANIZADOR, PROPUESTA);
        assertTrue(presupuesto.suficiente(), presupuesto.mensaje());
        assertEquals(0, presupuesto.total().compareTo(new BigDecimal("12000000.00")));
        assertEquals(0, presupuesto.valor().compareTo(new BigDecimal("1350000.00")));
        assertTrue(presupuesto.disponibleDespues().signum() >= 0);

        assertEquals("0", valor("SELECT COUNT(*) FROM Contratacion WHERE id_evento = "
                + EVENTO));
        assertEquals("DISPONIBLE", valor("SELECT estado FROM Disponibilidad_Servicio "
                + "WHERE id_disponibilidad = 101"));
        assertEquals("SELECCIONADA", valor("SELECT estado FROM Necesidad_Servicio_Evento "
                + "WHERE id_necesidad_servicio = 3"));
    }

    @Test
    void formalizaLaPropuestaYPersisteContratacionReservaYEstados() throws Exception {
        ConfirmacionContratacion confirmacion
                = service.contratar(ORGANIZADOR, PROPUESTA);

        assertTrue(confirmacion.idContratacion() > 0);
        assertEquals(PROPUESTA, confirmacion.idPropuesta());
        assertEquals(2, confirmacion.numeroVersion());
        assertEquals("Flash Studio Fotografía", confirmacion.nombreProveedor());
        assertNotNull(confirmacion.fechaContratacion());

        assertEquals("1", valor("SELECT COUNT(*) FROM Contratacion "
                + "WHERE id_contratacion = " + confirmacion.idContratacion()
                + " AND id_propuesta = " + PROPUESTA
                + " AND id_evento = " + EVENTO
                + " AND estado = 'ACTIVA'"));
        assertEquals("1350000.00", valor("SELECT valor_acordado FROM Contratacion "
                + "WHERE id_contratacion = " + confirmacion.idContratacion()));
        assertEquals("RESERVADA", valor("SELECT estado FROM Disponibilidad_Servicio "
                + "WHERE id_disponibilidad = 101"));
        assertEquals("CONTRATADA", valor("SELECT estado FROM Propuesta "
                + "WHERE id_propuesta = " + PROPUESTA));
        assertEquals("CONTRATADA", valor("SELECT estado FROM Solicitud_Ejecucion "
                + "WHERE id_solicitud = 6"));
        assertEquals("CONTRATADA", valor("SELECT estado FROM Necesidad_Servicio_Evento "
                + "WHERE id_necesidad_servicio = 3"));
        assertEquals("NO_SELECCIONADA", valor("SELECT estado FROM Propuesta "
                + "WHERE id_propuesta = 9"));
        assertEquals("1", valor("SELECT COUNT(*) FROM Notificacion_interna "
                + "WHERE id_usuario = 13 AND id_evento = " + EVENTO
                + " AND mensaje LIKE '%fue contratada%'"));
    }

    @Test
    void noPermiteContratarSinDisponibilidadDelProveedor() throws Exception {
        ejecutar("UPDATE Disponibilidad_Servicio SET estado = 'RESERVADA' "
                + "WHERE id_disponibilidad = 101");

        DisponibilidadContratacion disponibilidad
                = service.verificarDisponibilidad(ORGANIZADOR, PROPUESTA);
        assertFalse(disponibilidad.disponible());
        assertTrue(disponibilidad.mensaje().contains("disponible"));

        assertThrows(IllegalStateException.class,
                () -> service.contratar(ORGANIZADOR, PROPUESTA));
        assertEquals("0", valor("SELECT COUNT(*) FROM Contratacion "
                + "WHERE id_evento = " + EVENTO));
        assertEquals("SELECCIONADA", valor("SELECT estado FROM Necesidad_Servicio_Evento "
                + "WHERE id_necesidad_servicio = 3"));
    }

    @Test
    void noPermiteContratarCuandoElPresupuestoEsInsuficiente() throws Exception {
        ejecutar("UPDATE Evento SET presupuesto = 0 WHERE id_evento = " + EVENTO);

        PresupuestoContratacion presupuesto
                = service.verificarPresupuesto(ORGANIZADOR, PROPUESTA);
        assertFalse(presupuesto.suficiente());
        assertTrue(presupuesto.faltante().signum() > 0);

        assertThrows(IllegalArgumentException.class,
                () -> service.contratar(ORGANIZADOR, PROPUESTA));

        assertEquals("0", valor("SELECT COUNT(*) FROM Contratacion "
                + "WHERE id_propuesta = " + PROPUESTA));
        assertEquals("DISPONIBLE", valor("SELECT estado FROM Disponibilidad_Servicio "
                + "WHERE id_disponibilidad = 101"));
        assertEquals("SELECCIONADA", valor("SELECT estado FROM Propuesta "
                + "WHERE id_propuesta = " + PROPUESTA));
        assertEquals("SELECCIONADA", valor("SELECT estado FROM Necesidad_Servicio_Evento "
                + "WHERE id_necesidad_servicio = 3"));
    }

    @Test
    void exigeOrganizadorPropietarioYSesionActiva() {
        ServiceContratacionServicio otroOrganizador
                = crearServicio(sesion("ORGANIZADOR", 1, 1, "ACTIVO", "ACTIVO"));
        ServiceContratacionServicio proveedor
                = crearServicio(sesion("PROVEEDOR", 5, 13, "ACTIVO", "APROBADO"));
        ServiceContratacionServicio usuarioInactivo
                = crearServicio(sesion("ORGANIZADOR", ORGANIZADOR,
                        USUARIO_ORGANIZADOR, "INACTIVO", "ACTIVO"));

        assertThrows(IllegalStateException.class,
                () -> otroOrganizador.consultarResumen(ORGANIZADOR, PROPUESTA));
        assertThrows(IllegalStateException.class,
                () -> proveedor.contratar(5, PROPUESTA));
        assertThrows(IllegalStateException.class,
                () -> usuarioInactivo.verificarDisponibilidad(ORGANIZADOR, PROPUESTA));
    }

    @Test
    void exigeRepositorioSesionYActorOrganizadorActivo() {
        assertThrows(IllegalArgumentException.class,
                () -> new ServiceContratacionServicio(null, sesionOrganizador()));
        assertThrows(IllegalArgumentException.class,
                () -> new ServiceContratacionServicio(
                        new RepositoryContratacionServicio(), null));

        ServiceContratacionServicio usuarioSinId
                = crearServicio(sesion("ORGANIZADOR", ORGANIZADOR,
                        0, "ACTIVO", "ACTIVO"));
        ServiceContratacionServicio actorSinId
                = crearServicio(sesion("ORGANIZADOR", 0,
                        USUARIO_ORGANIZADOR, "ACTIVO", "ACTIVO"));
        ServiceContratacionServicio actorInactivo
                = crearServicio(sesion("ORGANIZADOR", ORGANIZADOR,
                        USUARIO_ORGANIZADOR, "ACTIVO", "INACTIVO"));

        assertThrows(IllegalStateException.class,
                () -> usuarioSinId.consultarResumen(ORGANIZADOR, PROPUESTA));
        assertThrows(IllegalStateException.class,
                () -> actorSinId.verificarDisponibilidad(ORGANIZADOR, PROPUESTA));
        assertThrows(IllegalStateException.class,
                () -> actorInactivo.verificarPresupuesto(ORGANIZADOR, PROPUESTA));
    }

    @Test
    void rechazaIdentificadoresInvalidosAntesDeConsultarLaBase() throws Exception {
        assertThrows(IllegalArgumentException.class,
                () -> service.consultarResumen(0, PROPUESTA));
        assertThrows(IllegalArgumentException.class,
                () -> service.verificarPresupuesto(ORGANIZADOR, -1));
        assertEquals("0", valor("SELECT COUNT(*) FROM Contratacion "
                + "WHERE id_evento = " + EVENTO));
    }

    private ServiceContratacionServicio crearServicio(SesionUsuario sesion) {
        return new ServiceContratacionServicio(
                new RepositoryContratacionServicio(),
                sesion);
    }

    private SesionUsuario sesionOrganizador() {
        return sesion("ORGANIZADOR", ORGANIZADOR, USUARIO_ORGANIZADOR,
                "ACTIVO", "ACTIVO");
    }

    private SesionUsuario sesion(
            String tipo,
            int idActor,
            int idUsuario,
            String estadoUsuario,
            String estadoActor) {
        SesionUsuario sesion = new SesionUsuario();
        sesion.setTipoUsuario(tipo);
        sesion.setIdActor(idActor);
        sesion.setIdUsuario(idUsuario);
        sesion.setEstadoUsuario(estadoUsuario);
        sesion.setEstadoActor(estadoActor);
        return sesion;
    }

    private void ejecutar(String sql) throws Exception {
        // cambiamos un estado del seed para comprobar el resultado del servicio
        try (Connection connection = DatabaseConnection.getConnection();
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private String valor(String sql) throws Exception {
        // consultamos h2 para comprobar los cambios que dejó el servicio
        try (Connection connection = DatabaseConnection.getConnection();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(sql)) {
            assertTrue(result.next(), "La consulta debe devolver un valor.");
            return result.getString(1);
        }
    }
}

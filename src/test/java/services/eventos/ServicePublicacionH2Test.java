package services.eventos;

import entities.SesionUsuario;
import entities.SolicitudPublicacion;
import entities.eventos.Evento;
import entities.eventos.PlantillaEvento;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import persistencia.DatabaseConnection;
import repositories.RepositoryPublicacion;
import repositories.eventos.RepositoryEvento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServicePublicacionH2Test {

    private String url;
    private RepositoryEvento repositoryEvento;
    private RepositoryPublicacion repositoryPublicacion;
    private ServicePublicacion service;
    private Evento eventoFinalizado;
    private Evento eventoAjeno;
    private Evento eventoPlaneado;
    private SesionUsuario organizador;
    private SesionUsuario otroOrganizador;
    private SesionUsuario administrador;

    @BeforeEach
    void preparar() throws Exception {
        // la base vive entre conexiones del servicio y tiene un nombre único en memoria
        url = "jdbc:h2:mem:publicacion_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        // creamos en esta h2 los organizadores y el administrador del escenario
        try (Connection connection = DatabaseConnection.getConnection(url);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO Usuario "
                    + "(id_usuario, nombre, usuario, contraseña, estado, fecha_registro, acepta_marketing, tipo_usuario) "
                    + "VALUES (10, 'Laura Gómez', 'laura', 'solo-prueba', 'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'ORGANIZADOR'), "
                    + "(20, 'Carlos Ruiz', 'carlos', 'solo-prueba', 'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'ORGANIZADOR'), "
                    + "(30, 'Camila Díaz', 'camila', 'solo-prueba', 'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'ADMINISTRADOR')");
            statement.executeUpdate("INSERT INTO Organizador (id_organizador, id_usuario, estado) "
                    + "VALUES (10, 10, 'ACTIVO'), (20, 20, 'ACTIVO')");
            statement.executeUpdate("INSERT INTO Rol (id_rol, nombre, estado) "
                    + "VALUES (30, 'ADMINISTRADOR', 'ACTIVO')");
            statement.executeUpdate("INSERT INTO Usuario_rol (id_usuario_rol, id_usuario, id_rol) "
                    + "VALUES (30, 30, 30)");
        }

        repositoryEvento = new RepositoryEvento(url);
        repositoryPublicacion = new RepositoryPublicacion(url);
        eventoFinalizado = guardarEvento(10, "Boda de Laura", "FINALIZADO");
        eventoAjeno = guardarEvento(20, "Evento de Carlos", "FINALIZADO");
        eventoPlaneado = guardarEvento(10, "Próximo evento", "PLANEADO");
        service = new ServicePublicacion(repositoryPublicacion, repositoryEvento);
        organizador = sesion(10, 10, "Laura Gómez", "ORGANIZADOR");
        otroOrganizador = sesion(20, 20, "Carlos Ruiz", "ORGANIZADOR");
        administrador = sesion(30, 0, "Camila Díaz", "ADMINISTRADOR");
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
    void organizadorSolicitaYElAdministradorRecibeNotificacionPersistida() {
        assertEquals("Laura Gómez", service.nombreUsuario(organizador));
        assertFalse(service.esAdministrador(organizador));
        assertTrue(service.esAdministrador(administrador));
        List<Evento> eventosPropios = service.listarEventos(organizador);
        assertEquals(2, eventosPropios.size());
        boolean eventoFinalizadoEncontrado = false;
        boolean eventoPlaneadoEncontrado = false;
        for (Evento evento : eventosPropios) {
            if (evento.getIdEvento() == eventoFinalizado.getIdEvento()) {
                eventoFinalizadoEncontrado = true;
            }
            if (evento.getIdEvento() == eventoPlaneado.getIdEvento()) {
                eventoPlaneadoEncontrado = true;
            }
        }
        assertTrue(eventoFinalizadoEncontrado);
        assertTrue(eventoPlaneadoEncontrado);

        service.solicitar(organizador, eventoFinalizado.getIdEvento());

        SolicitudPublicacion solicitud = service.listarSolicitudes(organizador).get(0);
        assertEquals(eventoFinalizado.getIdEvento(), solicitud.idEvento());
        assertEquals("PENDIENTE", solicitud.estado());
        assertEquals(1, service.listarSolicitudes(administrador).size());
        assertTrue(service.notificaciones(administrador).get(0).contains("Boda de Laura"));
        assertTrue(service.notificaciones(organizador).isEmpty());
    }

    @Test
    void aprobarPublicaLaPlantillaYConservaLaRevisionAlVolverAConsultar() {
        service.solicitar(organizador, eventoFinalizado.getIdEvento());
        int idSolicitud = service.listarSolicitudes(organizador).get(0).id();

        service.revisar(administrador, idSolicitud, true, null);

        ServicePublicacion nuevaInstancia = new ServicePublicacion(
                new RepositoryPublicacion(url), new RepositoryEvento(url));
        PlantillaEvento plantilla = nuevaInstancia.listarPlantillas(organizador).get(0);
        assertEquals("Plantilla de evento Social", plantilla.nombre());
        assertEquals("Social", plantilla.tipo());
        assertEquals(180, plantilla.duracionMinutos());
        assertEquals("APROBADA", nuevaInstancia.listarSolicitudes(organizador).get(0).estado());
        assertTrue(nuevaInstancia.listarSolicitudes(administrador).isEmpty());
        assertTrue(nuevaInstancia.notificaciones(organizador).get(0).contains("se publicó"));
    }

    @Test
    void rechazoGuardaElMotivoYPermiteEnviarUnaNuevaSolicitud() {
        service.solicitar(organizador, eventoFinalizado.getIdEvento());
        int idSolicitud = service.listarSolicitudes(organizador).get(0).id();

        service.revisar(administrador, idSolicitud, false, "  Ajusta el contenido  ");

        SolicitudPublicacion rechazada = service.listarSolicitudes(organizador).get(0);
        assertEquals("RECHAZADA", rechazada.estado());
        assertEquals("Ajusta el contenido", rechazada.motivo());
        assertTrue(service.listarPlantillas(organizador).isEmpty());
        assertTrue(service.notificaciones(organizador).get(0).contains("Ajusta el contenido"));

        service.solicitar(organizador, eventoFinalizado.getIdEvento());

        List<SolicitudPublicacion> solicitudes = service.listarSolicitudes(organizador);
        assertEquals(2, solicitudes.size());
        assertEquals("PENDIENTE", solicitudes.get(0).estado());
        assertEquals("RECHAZADA", solicitudes.get(1).estado());
    }

    @Test
    void soloPermiteSolicitarEventosFinalizadosDelPropioOrganizadorUnaVez() {
        assertThrows(IllegalArgumentException.class,
                () -> service.solicitar(organizador, 0));
        assertThrows(IllegalArgumentException.class,
                () -> service.solicitar(organizador, eventoAjeno.getIdEvento()));
        assertThrows(IllegalArgumentException.class,
                () -> service.solicitar(otroOrganizador, eventoFinalizado.getIdEvento()));
        assertThrows(IllegalArgumentException.class,
                () -> service.solicitar(organizador, eventoPlaneado.getIdEvento()));
        assertTrue(service.listarSolicitudes(organizador).isEmpty());

        service.solicitar(organizador, eventoFinalizado.getIdEvento());

        assertThrows(IllegalArgumentException.class,
                () -> service.solicitar(organizador, eventoFinalizado.getIdEvento()));
        assertEquals(1, service.listarSolicitudes(organizador).size());
        assertEquals(1, service.notificaciones(administrador).size());
    }

    @Test
    void exigeElRolCorrectoYUnAdministradorActivo() {
        service.solicitar(organizador, eventoFinalizado.getIdEvento());
        int idSolicitud = service.listarSolicitudes(organizador).get(0).id();

        assertThrows(IllegalStateException.class,
                () -> service.solicitar(administrador, eventoFinalizado.getIdEvento()));
        assertThrows(IllegalStateException.class,
                () -> service.revisar(organizador, idSolicitud, true, null));
        SesionUsuario administradorInactivo = sesion(30, 0, "Camila Díaz", "ADMINISTRADOR");
        administradorInactivo.setEstadoUsuario("INACTIVO");
        assertThrows(IllegalStateException.class,
                () -> service.revisar(administradorInactivo, idSolicitud, true, null));
        assertEquals("PENDIENTE", service.listarSolicitudes(organizador).get(0).estado());
    }

    @Test
    void exigeIdentificadorYMotivoValidosParaRechazarLaSolicitud() {
        service.solicitar(organizador, eventoFinalizado.getIdEvento());
        int idSolicitud = service.listarSolicitudes(organizador).get(0).id();

        assertThrows(IllegalArgumentException.class,
                () -> service.revisar(administrador, 0, true, null));
        for (String motivo : new String[]{null, "   ", "x".repeat(251)}) {
            assertThrows(IllegalArgumentException.class,
                    () -> service.revisar(administrador, idSolicitud, false, motivo));
        }

        assertEquals("PENDIENTE", service.listarSolicitudes(organizador).get(0).estado());
        assertTrue(service.listarPlantillas(organizador).isEmpty());
    }

    private Evento guardarEvento(int idOrganizador, String nombre, String estado) {
        LocalDateTime inicio = LocalDateTime.now().plusDays(30).withSecond(0).withNano(0);
        Evento evento = new Evento(idOrganizador, nombre, "Social", "Descripción de prueba",
                inicio, inicio.plusHours(3), "Salón de prueba", new BigDecimal("1000.00"), 10);
        evento.setEstado(estado);
        repositoryEvento.guardarEvento(evento);
        return evento;
    }

    private SesionUsuario sesion(int usuario, int idOrganizador, String nombre, String tipo) {
        SesionUsuario sesion = new SesionUsuario();
        sesion.setIdUsuario(usuario);
        sesion.setIdActor(idOrganizador);
        sesion.setNombre(nombre);
        sesion.setTipoUsuario(tipo);
        sesion.setEstadoUsuario("ACTIVO");
        sesion.setEstadoActor("ACTIVO");
        return sesion;
    }
}

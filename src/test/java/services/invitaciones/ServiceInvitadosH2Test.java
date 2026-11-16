package services.invitaciones;

import entities.SesionUsuario;
import entities.eventos.Evento;
import entities.eventos.Fotografia;
import entities.invitaciones.Invitacion;
import entities.invitaciones.InvitacionRecibida;
import entities.invitaciones.Invitado;
import entities.invitaciones.Regalo;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import persistencia.DatabaseConnection;
import repositories.RepositoryLugar;
import repositories.eventos.RepositoryEvento;
import repositories.invitaciones.RepositoryInvitado;
import repositories.invitaciones.RepositoryInvitacion;
import repositories.invitaciones.RepositoryRegalo;
import repositories.invitaciones.RepositoryReservaRegalo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceInvitadosH2Test {

    private static final int ORGANIZADOR = 10;
    private static final int OTRO_ORGANIZADOR = 20;
    private static final int LUGAR_PEQUENO = 1;
    private static final int LUGAR_GRANDE = 2;

    private String url;
    private int evento;
    private int otroEvento;
    private RepositoryEvento repositoryEvento;
    private RepositoryInvitacion repositoryInvitacion;
    private RepositoryInvitado repositoryInvitado;
    private RepositoryRegalo repositoryRegalo;
    private ServiceInvitados service;

    @BeforeEach
    void preparar() throws Exception {
        // la base vive entre conexiones del servicio y tiene un nombre único en memoria
        url = "jdbc:h2:mem:invitados_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        // creamos las cuentas, perfiles y lugares que necesita la prueba
        try (Connection connection = DatabaseConnection.getConnection(url);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO Usuario "
                    + "(id_usuario, nombre, usuario, contraseña, estado, fecha_registro, acepta_marketing, tipo_usuario) "
                    + "VALUES (10, 'Laura Gómez', 'laura', 'solo-prueba', 'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'ORGANIZADOR'), "
                    + "(20, 'Carlos Ruiz', 'carlos', 'solo-prueba', 'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'ORGANIZADOR')");
            statement.executeUpdate("INSERT INTO Organizador (id_organizador, id_usuario, estado) "
                    + "VALUES (10, 10, 'ACTIVO'), (20, 20, 'ACTIVO')");
            statement.executeUpdate("INSERT INTO Lugar "
                    + "(id_lugar, nombre, direccion, capacidad, descripcion, estado) VALUES "
                    + "(1, 'Sala pequeña', 'Calle 1', 2, 'Aforo reducido', 'DISPONIBLE'), "
                    + "(2, 'Salón grande', 'Calle 2', 20, 'Aforo amplio', 'DISPONIBLE')");
        }

        repositoryEvento = new RepositoryEvento(url);
        evento = guardarEvento(ORGANIZADOR, "Boda de Laura").getIdEvento();
        otroEvento = guardarEvento(OTRO_ORGANIZADOR, "Evento de Carlos").getIdEvento();
        repositoryInvitacion = new RepositoryInvitacion(url);
        repositoryInvitado = new RepositoryInvitado(url);
        repositoryRegalo = new RepositoryRegalo(url);
        service = new ServiceInvitados(repositoryInvitacion, repositoryInvitado,
                new RepositoryLugar(url), repositoryEvento, repositoryRegalo,
                new RepositoryReservaRegalo(url));
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
    void creaInvitacionAgregaInvitadoYPermiteConfirmarConIdentidadVerificada() {
        Invitacion creada = crearInvitacion(evento, LUGAR_GRANDE, "  Te esperamos  ");

        assertTrue(creada.getIdInvitacion() > 0);
        assertEquals(evento, creada.getIdEvento());
        assertEquals("Te esperamos", creada.getMensaje());
        assertEquals(Invitacion.ESTADO_ENVIADA, creada.getEstadoRespuesta());
        assertNotNull(creada.getFechaEnvio());
        assertEquals(Invitacion.ESTADO_ENVIADA,
                repositoryInvitacion.buscarInvitacion(creada.getIdInvitacion()).getEstadoRespuesta());

        Invitado datos = new Invitado("  Ana Pérez ", " Ana@Correo.com ",
                "300-555-1212", "  ");
        service.agregarInvitado(creada.getIdInvitacion(), datos);
        Invitado guardado = repositoryInvitado.buscarInvitado(datos.getIdInvitado());
        assertTrue(guardado.getIdInvitado() > 0);
        assertEquals("Ana Pérez", guardado.getNombre());
        assertEquals("ana@correo.com", guardado.getCorreo());
        assertEquals("300-555-1212", guardado.getTelefono());
        assertEquals(Invitado.ESTADO_PENDIENTE, guardado.getEstado());

        List<InvitacionRecibida> recibidas =
                service.consultarInvitacionesPorCorreo(" ana@correo.com ", "300 555 1212");
        assertEquals(1, recibidas.size());
        assertEquals(1, recibidas.get(0).getOcupados());
        assertTrue(service.consultarInvitacionesPorCorreo("ana@correo.com", "3000000000").isEmpty());
        assertThrows(IllegalArgumentException.class,
                () -> service.gestionarRespuestaInvitacion(datos.getIdInvitado(),
                        "otra@correo.com", "300-555-1212", Invitado.ESTADO_CONFIRMADO));
        assertEquals(Invitado.ESTADO_PENDIENTE,
                repositoryInvitado.buscarInvitado(datos.getIdInvitado()).getEstado());

        service.gestionarRespuestaInvitacion(datos.getIdInvitado(),
                " ANA@correo.com ", "(300) 555-1212", " confirmado ");

        assertEquals(Invitado.ESTADO_CONFIRMADO,
                repositoryInvitado.buscarInvitado(datos.getIdInvitado()).getEstado());
        assertEquals(Invitacion.ESTADO_ACEPTADA,
                repositoryInvitacion.buscarInvitacion(creada.getIdInvitacion()).getEstadoRespuesta());
        assertEquals(1, service.contarAsistentes(evento, LUGAR_GRANDE));
    }

    @Test
    void actualizaInvitacionYPersisteElNuevoLugarYMensaje() {
        Invitacion invitacion = crearInvitacion(evento, LUGAR_GRANDE, "Mensaje inicial");

        service.actualizarInvitacion(invitacion.getIdInvitacion(),
                invitacion(LUGAR_PEQUENO, "  Mensaje actualizado  "));

        Invitacion actualizada
                = repositoryInvitacion.buscarInvitacion(invitacion.getIdInvitacion());
        assertEquals(LUGAR_PEQUENO, actualizada.getIdLugar());
        assertEquals("Mensaje actualizado", actualizada.getMensaje());
        assertEquals(Invitacion.ESTADO_ENVIADA, actualizada.getEstadoRespuesta());
    }

    @Test
    void consultaLugaresInvitacionesYEventosRelacionados() {
        Invitacion invitacion = crearInvitacion(evento, LUGAR_GRANDE, "Consulta");
        Invitado ana = agregarInvitado(
                invitacion.getIdInvitacion(), "Ana", "ana@correo.com", "3001112222");
        Regalo regalo = new Regalo("Jarrón", "Cerámica", new BigDecimal("45000.00"));
        service.agregarRegalo(ORGANIZADOR, invitacion.getIdInvitacion(), regalo);

        assertEquals(2, service.listarLugares().size());
        List<Invitacion> invitaciones = service.listarInvitaciones(evento);
        assertEquals(1, invitaciones.size());
        assertEquals(invitacion.getIdInvitacion(), invitaciones.get(0).getIdInvitacion());
        assertEquals(evento, service.eventoDeInvitado(ana.getIdInvitado()));
        assertEquals(evento, service.eventoDeRegalo(regalo.getIdRegalo()));
        assertEquals(1, service.listarRegalos(
                ORGANIZADOR, invitacion.getIdInvitacion()).size());
    }

    @Test
    void permiteQuitarInvitadoSinRelacionesYRechazaSiTieneAcompanante() throws Exception {
        Invitacion invitacion = crearInvitacion(evento, LUGAR_GRANDE, "Invitación");
        Invitado sinRelaciones
                = agregarInvitado(invitacion.getIdInvitacion(), "Ana", "ana@correo.com", null);
        service.quitarInvitado(sinRelaciones.getIdInvitado());
        assertNull(repositoryInvitado.buscarInvitado(sinRelaciones.getIdInvitado()));

        Invitado conAcompanante
                = agregarInvitado(invitacion.getIdInvitacion(), "Luis", "luis@correo.com", null);
        ejecutar("INSERT INTO Acompanante (id_invitado, nombre, tipo) VALUES ("
                + conAcompanante.getIdInvitado() + ", 'Acompañante de Luis', 'ADULTO')");

        assertThrows(IllegalArgumentException.class,
                () -> service.quitarInvitado(conAcompanante.getIdInvitado()));
        assertNotNull(repositoryInvitado.buscarInvitado(conAcompanante.getIdInvitado()));
        assertEquals(1, repositoryInvitado.listarInvitados(invitacion.getIdInvitacion()).size());
    }

    @Test
    void consultaGaleriaSoloParaElCorreoYTelefonoEntregados() throws Exception {
        Invitacion invitacion = crearInvitacion(evento, LUGAR_GRANDE, "Galería");
        Invitado ana
                = agregarInvitado(invitacion.getIdInvitacion(), "Ana", "ana@correo.com", "3001112222");
        ejecutar("INSERT INTO Finalizacion_evento (id_evento, enviada) VALUES ("
                + evento + ", TRUE)");
        insertarFotografia(evento, "Foto de la celebración", new byte[]{1, 2, 3});
        ejecutar("INSERT INTO Entrega_galeria (id_evento, id_invitado) VALUES ("
                + evento + ", " + ana.getIdInvitado() + ")");

        List<Fotografia> galeria
                = service.consultarGaleria(" ANA@correo.com ", "(300) 111-2222");

        assertEquals(1, galeria.size());
        assertEquals("Boda de Laura · Foto de la celebración", galeria.get(0).nombre());
        assertArrayEquals(new byte[]{1, 2, 3}, galeria.get(0).contenido());
        assertTrue(service.consultarGaleria("otra@correo.com", "3001112222").isEmpty());
        assertTrue(service.consultarGaleria("ana@correo.com", "3009999999").isEmpty());
        assertThrows(IllegalArgumentException.class,
                () -> service.consultarGaleria("correo-inválido", "3001112222"));
    }

    @Test
    void consultaAvisosAsociadosAlCorreoDelInvitado() throws Exception {
        Invitacion invitacion = crearInvitacion(evento, LUGAR_GRANDE, "Avisos");
        Invitado ana
                = agregarInvitado(invitacion.getIdInvitacion(), "Ana", "ana@correo.com", "3001112222");
        ejecutar("INSERT INTO Notificacion_interna "
                + "(id_usuario, mensaje, id_invitado, id_evento) VALUES (NULL, "
                + "'El evento tiene una actualización.', " + ana.getIdInvitado()
                + ", " + evento + ")");

        assertEquals(List.of("El evento tiene una actualización."),
                service.consultarAvisosPorCorreo(" ANA@correo.com "));
        assertTrue(service.consultarAvisosPorCorreo("otra@correo.com").isEmpty());
        assertThrows(IllegalArgumentException.class,
                () -> service.consultarAvisosPorCorreo("correo-inválido"));
    }

    @Test
    void actualizaLaRespuestaYElEstadoDeLaInvitacionEnCadaTransicion() {
        Invitacion invitacion = crearInvitacion(evento, LUGAR_GRANDE, "Responde");
        Invitado ana
                = agregarInvitado(invitacion.getIdInvitacion(), "Ana", "ana@correo.com", "3001112222");
        Invitado luis
                = agregarInvitado(invitacion.getIdInvitacion(), "Luis", "luis@correo.com", "3001113333");

        service.gestionarRespuestaInvitacion(ana.getIdInvitado(),
                ana.getCorreo(), ana.getTelefono(), Invitado.ESTADO_RECHAZADO);
        assertEquals(Invitacion.ESTADO_ENVIADA,
                repositoryInvitacion.buscarInvitacion(invitacion.getIdInvitacion()).getEstadoRespuesta());

        service.gestionarRespuestaInvitacion(luis.getIdInvitado(),
                luis.getCorreo(), luis.getTelefono(), Invitado.ESTADO_RECHAZADO);
        assertEquals(Invitacion.ESTADO_RECHAZADA,
                repositoryInvitacion.buscarInvitacion(invitacion.getIdInvitacion()).getEstadoRespuesta());

        service.gestionarRespuestaInvitacion(ana.getIdInvitado(),
                ana.getCorreo(), ana.getTelefono(), Invitado.ESTADO_CONFIRMADO);
        assertEquals(Invitado.ESTADO_CONFIRMADO,
                repositoryInvitado.buscarInvitado(ana.getIdInvitado()).getEstado());
        assertEquals(Invitacion.ESTADO_ACEPTADA,
                repositoryInvitacion.buscarInvitacion(invitacion.getIdInvitacion()).getEstadoRespuesta());
    }

    @Test
    void validaAforoYElRechazoLiberaElCupoParaOtroInvitado() {
        Invitacion invitacion = crearInvitacion(evento, LUGAR_PEQUENO, "Bienvenidos");
        Invitado ana = agregarInvitado(invitacion.getIdInvitacion(), "Ana", "ana@correo.com", "3001112222");
        Invitado luis = agregarInvitado(invitacion.getIdInvitacion(), "Luis", "luis@correo.com", "3001113333");

        assertEquals(2, service.contarAsistentes(evento, LUGAR_PEQUENO));
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.agregarInvitado(invitacion.getIdInvitacion(),
                        new Invitado("Eva", "eva@correo.com", null, null)));
        assertTrue(error.getMessage().contains("aforo"));

        service.gestionarRespuestaInvitacion(ana.getIdInvitado(), ana.getCorreo(),
                ana.getTelefono(), Invitado.ESTADO_RECHAZADO);
        assertEquals(1, service.contarAsistentes(evento, LUGAR_PEQUENO));
        Invitado eva = agregarInvitado(invitacion.getIdInvitacion(), "Eva", "eva@correo.com", null);

        assertEquals(2, service.contarAsistentes(evento, LUGAR_PEQUENO));
        assertEquals(2, service.contarAsistentesPorInvitacion(evento).get(invitacion.getIdInvitacion()));
        assertEquals(Invitado.ESTADO_RECHAZADO,
                repositoryInvitado.buscarInvitado(ana.getIdInvitado()).getEstado());
        assertEquals(Invitado.ESTADO_PENDIENTE,
                repositoryInvitado.buscarInvitado(eva.getIdInvitado()).getEstado());
        assertEquals(Invitacion.ESTADO_ENVIADA,
                repositoryInvitacion.buscarInvitacion(invitacion.getIdInvitacion()).getEstadoRespuesta());
    }

    @Test
    void correoDuplicadoSoloSePermiteEnOtroEventoONuevaInvitacionTrasRevocar() {
        Invitacion primera = crearInvitacion(evento, LUGAR_GRANDE, "Primera");
        Invitado ana = agregarInvitado(primera.getIdInvitacion(), "Ana", "ana@correo.com", "3001112222");
        Invitacion segunda = crearInvitacion(evento, LUGAR_GRANDE, "Segunda");
        assertThrows(IllegalArgumentException.class,
                () -> service.agregarInvitado(segunda.getIdInvitacion(),
                        new Invitado("Ana P.", " ANA@correo.com ", null, null)));

        Invitacion deOtroEvento = crearInvitacion(otroEvento, LUGAR_GRANDE, "Otro evento");
        service.agregarInvitado(deOtroEvento.getIdInvitacion(),
                new Invitado("Ana", "ana@correo.com", null, null));
        assertEquals(1, service.listarInvitados(deOtroEvento.getIdInvitacion()).size());

        service.revocarInvitacion(primera.getIdInvitacion());
        assertEquals(Invitacion.ESTADO_REVOCADA,
                repositoryInvitacion.buscarInvitacion(primera.getIdInvitacion()).getEstadoRespuesta());
        assertThrows(IllegalArgumentException.class,
                () -> service.agregarInvitado(primera.getIdInvitacion(),
                        new Invitado("Eva", "eva@correo.com", null, null)));

        Invitacion nueva = crearInvitacion(evento, LUGAR_GRANDE, "Nueva");
        service.agregarInvitado(nueva.getIdInvitacion(),
                new Invitado("Ana", ana.getCorreo(), null, null));
        assertEquals(1, service.listarInvitados(nueva.getIdInvitacion()).size());
    }

    @Test
    void validaPropiedadDeEventosYSesionActiva() throws Exception {
        assertEquals(ORGANIZADOR, service.validarSesionOrganizador(sesionOrganizador()));
        assertEquals(1, service.listarEventos(ORGANIZADOR).size());
        assertThrows(IllegalArgumentException.class,
                () -> service.validarEventoDelOrganizador(ORGANIZADOR, otroEvento));

        SesionUsuario sesionForjada = sesionOrganizador();
        sesionForjada.setIdUsuario(20);
        assertThrows(IllegalStateException.class,
                () -> service.validarSesionOrganizador(sesionForjada));
        SesionUsuario sesionCerrada = sesionOrganizador();
        sesionCerrada.setEstadoUsuario("INACTIVO");
        assertThrows(IllegalStateException.class,
                () -> service.validarSesionOrganizador(sesionCerrada));
        SesionUsuario actorInactivo = sesionOrganizador();
        actorInactivo.setEstadoActor("PENDIENTE");
        assertThrows(IllegalStateException.class,
                () -> service.validarSesionOrganizador(actorInactivo));
        SesionUsuario rolIncorrecto = sesionOrganizador();
        rolIncorrecto.setTipoUsuario("PROVEEDOR");
        assertThrows(IllegalStateException.class,
                () -> service.validarSesionOrganizador(rolIncorrecto));
    }

    @Test
    void rechazaCrearORevocarInvitacionesDeUnEventoCerrado() throws Exception {
        Invitacion invitacion = crearInvitacion(evento, LUGAR_GRANDE, "Evento abierto");
        ejecutar("UPDATE Evento SET estado = 'CANCELADO' WHERE id_evento = " + evento);
        assertThrows(IllegalArgumentException.class,
                () -> service.crearInvitacion(evento, invitacion(LUGAR_GRANDE, "Otra")));
        assertThrows(IllegalArgumentException.class,
                () -> service.revocarInvitacion(invitacion.getIdInvitacion()));
        assertEquals(Invitacion.ESTADO_ENVIADA,
                repositoryInvitacion.buscarInvitacion(invitacion.getIdInvitacion()).getEstadoRespuesta());
    }

    @Test
    void reservaYLiberaUnRegaloSoloParaElInvitadoIdentificado() {
        Invitacion invitacion = crearInvitacion(evento, LUGAR_GRANDE, "Mesa de regalos");
        Invitado ana = agregarInvitado(invitacion.getIdInvitacion(), "Ana", "ana@correo.com", "3001112222");
        Regalo regalo = new Regalo("Jarrón", "Cerámica", new BigDecimal("45000.00"));
        service.agregarRegalo(ORGANIZADOR, invitacion.getIdInvitacion(), regalo);

        assertTrue(regalo.getIdRegalo() > 0);
        assertTrue(service.validarDisponibilidadRegalo(regalo.getIdRegalo()));
        assertEquals(1, service.listarRegalos(ORGANIZADOR, invitacion.getIdInvitacion()).size());
        assertThrows(IllegalArgumentException.class,
                () -> service.reservarRegalo(ana.getIdInvitado(), ana.getCorreo(),
                        "3009999999", regalo.getIdRegalo()));
        assertTrue(service.validarDisponibilidadRegalo(regalo.getIdRegalo()));

        service.reservarRegalo(ana.getIdInvitado(), ana.getCorreo(),
                ana.getTelefono(), regalo.getIdRegalo());

        Regalo reservado = service.consultarRegalosDisponibles(ana.getIdInvitado(),
                ana.getCorreo(), ana.getTelefono()).get(0);
        assertFalse(service.validarDisponibilidadRegalo(regalo.getIdRegalo()));
        assertEquals(ana.getIdInvitado(), reservado.getIdInvitadoReserva());
        assertEquals(1, new RepositoryReservaRegalo(url).contarReservasActivas(regalo.getIdRegalo()));
        assertThrows(IllegalArgumentException.class,
                () -> service.quitarRegalo(ORGANIZADOR, regalo.getIdRegalo()));
        assertNotNull(repositoryRegalo.buscarRegalo(regalo.getIdRegalo()));

        service.cancelarReservaRegalo(ana.getIdInvitado(), ana.getCorreo(),
                ana.getTelefono(), regalo.getIdRegalo());

        assertTrue(service.validarDisponibilidadRegalo(regalo.getIdRegalo()));
        assertEquals(Regalo.ESTADO_DISPONIBLE,
                repositoryRegalo.buscarRegalo(regalo.getIdRegalo()).getEstado());
        service.quitarRegalo(ORGANIZADOR, regalo.getIdRegalo());
        assertNull(repositoryRegalo.buscarRegalo(regalo.getIdRegalo()));
    }

    private Evento guardarEvento(int idOrganizador, String nombre) {
        LocalDateTime inicio = LocalDateTime.now().plusDays(30).withSecond(0).withNano(0);
        Evento evento = new Evento(idOrganizador, nombre, "Social", "Evento de integración",
                inicio, inicio.plusHours(4), "Salón de prueba", BigDecimal.ZERO, 10);
        repositoryEvento.guardarEvento(evento);
        return evento;
    }

    private Invitacion crearInvitacion(int idEvento, int idLugar, String mensaje) {
        Invitacion invitacion = invitacion(idLugar, mensaje);
        service.crearInvitacion(idEvento, invitacion);
        return invitacion;
    }

    private Invitacion invitacion(int idLugar, String mensaje) {
        Invitacion invitacion = new Invitacion();
        invitacion.setIdLugar(idLugar);
        invitacion.setMensaje(mensaje);
        return invitacion;
    }

    private Invitado agregarInvitado(int idInvitacion, String nombre, String correo, String telefono) {
        Invitado invitado = new Invitado(nombre, correo, telefono, null);
        service.agregarInvitado(idInvitacion, invitado);
        return invitado;
    }

    private SesionUsuario sesionOrganizador() {
        SesionUsuario sesion = new SesionUsuario();
        sesion.setIdUsuario(ORGANIZADOR);
        sesion.setIdActor(ORGANIZADOR);
        sesion.setTipoUsuario("ORGANIZADOR");
        sesion.setEstadoUsuario("ACTIVO");
        sesion.setEstadoActor("ACTIVO");
        return sesion;
    }

    private void ejecutar(String sql) throws Exception {
        // cambiamos datos de prueba que no tienen una operación pública en el servicio
        try (Connection connection = DatabaseConnection.getConnection(url);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private void insertarFotografia(int idEvento, String nombre, byte[] contenido) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection(url);
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO Fotografia_evento (id_evento, nombre, contenido) VALUES (?, ?, ?)")) {
            statement.setInt(1, idEvento);
            statement.setString(2, nombre);
            statement.setBytes(3, contenido);
            statement.executeUpdate();
        }
    }
}

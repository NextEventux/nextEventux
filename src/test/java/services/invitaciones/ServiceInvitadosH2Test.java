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
import persistencia.DatabaseTestUtils;
import persistencia.PropiedadSistemaTemporal;
import repositories.RepositoryLugar;
import repositories.eventos.RepositoryEvento;
import repositories.invitaciones.RepositoryInvitado;
import repositories.invitaciones.RepositoryInvitacion;
import repositories.invitaciones.RepositoryRegalo;
import repositories.invitaciones.RepositoryReservaRegalo;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ResourceLock(Resources.SYSTEM_PROPERTIES)
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
            DatabaseTestUtils.crearEsquemaVacio(connection);
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
        service.actualizarInvitacion(invitacion.getIdInvitacion(),
                invitacion(LUGAR_PEQUENO, "Mismo lugar, nuevo mensaje"));

        Invitacion actualizada
                = repositoryInvitacion.buscarInvitacion(invitacion.getIdInvitacion());
        assertEquals(LUGAR_PEQUENO, actualizada.getIdLugar());
        assertEquals("Mismo lugar, nuevo mensaje", actualizada.getMensaje());
        assertEquals(Invitacion.ESTADO_ENVIADA, actualizada.getEstadoRespuesta());
    }

    @Test
    void consultaLugaresInvitacionesYEventosRelacionados() {
        Invitacion invitacion = crearInvitacion(evento, LUGAR_GRANDE, "Consulta");
        Invitado ana = new Invitado("Ana", "ana@correo.com", "3001112222", "Calle 5");
        service.agregarInvitado(invitacion.getIdInvitacion(), ana);
        Regalo regalo = new Regalo("Jarrón", "Cerámica", new BigDecimal("45000.00"));
        service.agregarRegalo(ORGANIZADOR, invitacion.getIdInvitacion(), regalo);

        Invitacion invitacionOtroEvento = crearInvitacion(otroEvento, LUGAR_GRANDE, "Otro evento");
        Regalo regaloOtroEvento = new Regalo("Planta", "Interior", new BigDecimal("18000.00"));
        service.agregarRegalo(OTRO_ORGANIZADOR, invitacionOtroEvento.getIdInvitacion(), regaloOtroEvento);

        assertEquals(2, service.listarLugares().size());
        List<Invitacion> invitaciones = service.listarInvitaciones(evento);
        assertEquals(1, invitaciones.size());
        assertEquals(invitacion.getIdInvitacion(), invitaciones.get(0).getIdInvitacion());
        assertEquals(evento, service.eventoDeInvitado(ana.getIdInvitado()));
        assertEquals(evento, service.eventoDeRegalo(regalo.getIdRegalo()));
        assertEquals("Calle 5",
                repositoryInvitado.buscarInvitado(ana.getIdInvitado()).getDireccion());
        assertEquals(1, service.listarRegalos(
                ORGANIZADOR, invitacion.getIdInvitacion()).size());
        assertNotNull(repositoryRegalo.buscarRegalo(regaloOtroEvento.getIdRegalo()));
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
    void quitarInvitadoToleraQueSuInvitacionSeElimineEntreConsultas() {
        Invitacion invitacion = crearInvitacion(evento, LUGAR_GRANDE, "Eliminación concurrente");
        Invitado ana = agregarInvitado(
                invitacion.getIdInvitacion(), "Ana", "ana@correo.com", "3001112222");
        ServiceInvitados coordinado = servicioCoordinado(punto -> {
            if (punto == ServiceInvitados.PuntoCoordinacion.ANTES_DE_BUSCAR_INVITACION_AL_QUITAR) {
                repositoryInvitado.eliminarInvitado(ana.getIdInvitado());
                repositoryInvitacion.eliminarInvitacion(invitacion.getIdInvitacion());
            }
        });

        coordinado.quitarInvitado(ana.getIdInvitado());

        assertNull(repositoryInvitado.buscarInvitado(ana.getIdInvitado()));
        assertNull(repositoryInvitacion.buscarInvitacion(invitacion.getIdInvitacion()));
    }

    @Test
    void omiteInvitadosEliminadosDespuesDeListarSusInvitaciones() {
        Invitacion invitacion = crearInvitacion(evento, LUGAR_GRANDE, "Consulta concurrente");
        Invitado ana = agregarInvitado(
                invitacion.getIdInvitacion(), "Ana", "ana@correo.com", "3001112222");
        ServiceInvitados coordinado = servicioCoordinado(punto -> {
            if (punto == ServiceInvitados.PuntoCoordinacion.DESPUES_DE_LISTAR_INVITACIONES_POR_CORREO) {
                repositoryInvitado.eliminarInvitado(ana.getIdInvitado());
            }
        });

        assertTrue(coordinado.consultarInvitacionesPorCorreo(
                ana.getCorreo(), ana.getTelefono()).isEmpty());
        assertNull(repositoryInvitado.buscarInvitado(ana.getIdInvitado()));
    }

    @Test
    void conservaLaInvitacionEnviadaSiElInvitadoDesapareceAlActualizarSuRespuesta() {
        Invitacion invitacion = crearInvitacion(evento, LUGAR_GRANDE, "Respuesta concurrente");
        Invitado ana = agregarInvitado(
                invitacion.getIdInvitacion(), "Ana", "ana@correo.com", "3001112222");
        ServiceInvitados coordinado = servicioCoordinado(punto -> {
            if (punto == ServiceInvitados.PuntoCoordinacion.DESPUES_DE_ACTUALIZAR_ESTADO_INVITADO) {
                repositoryInvitado.eliminarInvitado(ana.getIdInvitado());
            }
        });

        coordinado.gestionarRespuestaInvitacion(
                ana.getIdInvitado(), ana.getCorreo(), ana.getTelefono(), Invitado.ESTADO_CONFIRMADO);

        assertNull(repositoryInvitado.buscarInvitado(ana.getIdInvitado()));
        assertEquals(Invitacion.ESTADO_ENVIADA,
                repositoryInvitacion.buscarInvitacion(invitacion.getIdInvitacion()).getEstadoRespuesta());
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
        IllegalArgumentException rechazoRepetido = assertThrows(IllegalArgumentException.class,
                () -> service.gestionarRespuestaInvitacion(luis.getIdInvitado(),
                        luis.getCorreo(), luis.getTelefono(), Invitado.ESTADO_RECHAZADO));
        assertTrue(rechazoRepetido.getMessage().contains("Ya habías indicado que no asistirás."));

        service.gestionarRespuestaInvitacion(ana.getIdInvitado(),
                ana.getCorreo(), ana.getTelefono(), Invitado.ESTADO_CONFIRMADO);
        assertEquals(Invitado.ESTADO_CONFIRMADO,
                repositoryInvitado.buscarInvitado(ana.getIdInvitado()).getEstado());
        assertEquals(Invitacion.ESTADO_ACEPTADA,
                repositoryInvitacion.buscarInvitacion(invitacion.getIdInvitacion()).getEstadoRespuesta());
        assertThrows(IllegalArgumentException.class,
                () -> service.gestionarRespuestaInvitacion(ana.getIdInvitado(),
                        ana.getCorreo(), ana.getTelefono(), Invitado.ESTADO_CONFIRMADO));

        service.gestionarRespuestaInvitacion(luis.getIdInvitado(),
                luis.getCorreo(), luis.getTelefono(), Invitado.ESTADO_CONFIRMADO);
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
    void rechazarLiberaCupoDeAcompanantePeroImpideReconfirmarSinEspacio() throws Exception {
        Invitacion invitacion = crearInvitacion(evento, LUGAR_PEQUENO, "Confirmación con acompañante");
        Invitado ana = agregarInvitado(
                invitacion.getIdInvitacion(), "Ana", "ana@correo.com", "3001112222");
        ejecutar("INSERT INTO Acompanante (id_invitado, nombre, tipo) VALUES ("
                + ana.getIdInvitado() + ", 'Acompañante de Ana', 'ADULTO')");

        assertEquals(2, service.contarAsistentes(evento, LUGAR_PEQUENO));
        service.gestionarRespuestaInvitacion(
                ana.getIdInvitado(), ana.getCorreo(), ana.getTelefono(), Invitado.ESTADO_RECHAZADO);
        assertEquals(0, service.contarAsistentes(evento, LUGAR_PEQUENO));

        agregarInvitado(invitacion.getIdInvitacion(), "Eva", "eva@correo.com", null);
        assertEquals(1, service.contarAsistentes(evento, LUGAR_PEQUENO));
        assertThrows(IllegalArgumentException.class,
                () -> service.gestionarRespuestaInvitacion(
                        ana.getIdInvitado(), ana.getCorreo(), ana.getTelefono(),
                        Invitado.ESTADO_CONFIRMADO));

        assertEquals(Invitado.ESTADO_RECHAZADO,
                repositoryInvitado.buscarInvitado(ana.getIdInvitado()).getEstado());
        assertEquals(1, service.contarAsistentes(evento, LUGAR_PEQUENO));
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
        assertThrows(IllegalStateException.class, () -> service.validarSesionOrganizador(null));

        SesionUsuario sesionForjada = sesionOrganizador();
        sesionForjada.setIdUsuario(20);
        assertThrows(IllegalStateException.class,
                () -> service.validarSesionOrganizador(sesionForjada));
        SesionUsuario usuarioSinIdentificador = sesionOrganizador();
        usuarioSinIdentificador.setIdUsuario(0);
        assertThrows(IllegalStateException.class,
                () -> service.validarSesionOrganizador(usuarioSinIdentificador));
        SesionUsuario actorSinIdentificador = sesionOrganizador();
        actorSinIdentificador.setIdActor(0);
        assertThrows(IllegalStateException.class,
                () -> service.validarSesionOrganizador(actorSinIdentificador));
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

        ejecutar("UPDATE Evento SET estado = 'FINALIZADO' WHERE id_evento = " + evento);
        assertThrows(IllegalArgumentException.class,
                () -> service.crearInvitacion(evento, invitacion(LUGAR_GRANDE, "Evento finalizado")));
        assertThrows(IllegalArgumentException.class,
                () -> service.actualizarInvitacion(invitacion.getIdInvitacion(),
                        invitacion(LUGAR_GRANDE, "Evento finalizado")));
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

    @Test
    void detectaUnaReservaCreadaAntesDeQuitarUnRegalo() {
        Invitacion invitacion = crearInvitacion(evento, LUGAR_GRANDE, "Reserva antes de quitar");
        Invitado ana = agregarInvitado(
                invitacion.getIdInvitacion(), "Ana", "ana@correo.com", "3001112222");
        Regalo regalo = new Regalo("Jarrón", "Cerámica", new BigDecimal("45000.00"));
        service.agregarRegalo(ORGANIZADOR, invitacion.getIdInvitacion(), regalo);
        ServiceInvitados coordinado = servicioCoordinado(punto -> {
            if (punto == ServiceInvitados.PuntoCoordinacion.ANTES_DE_QUITAR_REGALO) {
                service.reservarRegalo(
                        ana.getIdInvitado(), ana.getCorreo(), ana.getTelefono(), regalo.getIdRegalo());
            }
        });

        assertThrows(IllegalArgumentException.class,
                () -> coordinado.quitarRegalo(ORGANIZADOR, regalo.getIdRegalo()));

        assertEquals(1, new RepositoryReservaRegalo(url).contarReservasActivas(regalo.getIdRegalo()));
        assertNotNull(repositoryRegalo.buscarRegalo(regalo.getIdRegalo()));
    }

    @Test
    void detectaUnaReservaCreadaEntreLaLecturaYLaConsultaDeDisponibilidad() {
        Invitacion invitacion = crearInvitacion(evento, LUGAR_GRANDE, "Disponibilidad concurrente");
        Invitado ana = agregarInvitado(
                invitacion.getIdInvitacion(), "Ana", "ana@correo.com", "3001112222");
        Regalo regalo = new Regalo("Libro", "Edición ilustrada", new BigDecimal("25000.00"));
        service.agregarRegalo(ORGANIZADOR, invitacion.getIdInvitacion(), regalo);
        ServiceInvitados coordinado = servicioCoordinado(punto -> {
            if (punto == ServiceInvitados.PuntoCoordinacion.ANTES_DE_VALIDAR_DISPONIBILIDAD_REGALO) {
                service.reservarRegalo(
                        ana.getIdInvitado(), ana.getCorreo(), ana.getTelefono(), regalo.getIdRegalo());
            }
        });

        assertFalse(coordinado.validarDisponibilidadRegalo(regalo.getIdRegalo()));
        assertEquals(1, new RepositoryReservaRegalo(url).contarReservasActivas(regalo.getIdRegalo()));
    }

    @Test
    void noDuplicaUnaReservaSiOtroInvitadoLaGanaTrasLaValidacion() {
        Invitacion invitacion = crearInvitacion(evento, LUGAR_GRANDE, "Dos invitados compiten");
        Invitado ana = agregarInvitado(
                invitacion.getIdInvitacion(), "Ana", "ana@correo.com", "3001112222");
        Invitado luis = agregarInvitado(
                invitacion.getIdInvitacion(), "Luis", "luis@correo.com", "3001113333");
        Regalo regalo = new Regalo("Tazas", "Cerámica", new BigDecimal("12000.00"));
        service.agregarRegalo(ORGANIZADOR, invitacion.getIdInvitacion(), regalo);
        ServiceInvitados coordinado = servicioCoordinado(punto -> {
            if (punto == ServiceInvitados.PuntoCoordinacion.ANTES_DE_REGISTRAR_RESERVA) {
                service.reservarRegalo(
                        luis.getIdInvitado(), luis.getCorreo(), luis.getTelefono(), regalo.getIdRegalo());
            }
        });

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> coordinado.reservarRegalo(
                        ana.getIdInvitado(), ana.getCorreo(), ana.getTelefono(), regalo.getIdRegalo()));

        assertTrue(error.getMessage().contains("ya fue reservado por otro invitado"));
        assertEquals(1, new RepositoryReservaRegalo(url).contarReservasActivas(regalo.getIdRegalo()));
        assertEquals(luis.getIdInvitado(),
                new RepositoryReservaRegalo(url)
                        .buscarReservaActiva(regalo.getIdRegalo(), luis.getIdInvitado())
                        .getIdInvitado());
    }

    @Test
    void construyeServiciosPredeterminadosContraLaH2Aislada() {
        PropiedadSistemaTemporal propiedadUrl =
                new PropiedadSistemaTemporal("nexteventux.db.url", url);
        try {
            ServiceInvitados sinArgumentos = new ServiceInvitados();
            assertEquals(2, sinArgumentos.listarLugares().size());
            assertEquals(2, sinArgumentos.listarEventos().size());

            Invitacion invitacion = invitacion(LUGAR_GRANDE, "Constructor sin argumentos");
            sinArgumentos.crearInvitacion(evento, invitacion);
            Invitado ana = new Invitado("Ana", "ana@correo.com", "3001112222", null);
            sinArgumentos.agregarInvitado(invitacion.getIdInvitacion(), ana);
            assertNotNull(repositoryInvitacion.buscarInvitacion(invitacion.getIdInvitacion()));
            assertNotNull(repositoryInvitado.buscarInvitado(ana.getIdInvitado()));

            ServiceInvitados cuatroRepositorios = new ServiceInvitados(
                    repositoryInvitacion, repositoryInvitado, new RepositoryLugar(url), repositoryEvento);
            Regalo regalo = new Regalo("Tazas", "Cerámica", new BigDecimal("12000.00"));
            cuatroRepositorios.agregarRegalo(ORGANIZADOR, invitacion.getIdInvitacion(), regalo);
            cuatroRepositorios.reservarRegalo(
                    ana.getIdInvitado(), ana.getCorreo(), ana.getTelefono(), regalo.getIdRegalo());

            assertTrue(regalo.getIdRegalo() > 0);
            assertNotNull(repositoryRegalo.buscarRegalo(regalo.getIdRegalo()));
            assertEquals(1, new RepositoryReservaRegalo(url).contarReservasActivas(regalo.getIdRegalo()));
            assertFalse(cuatroRepositorios.validarDisponibilidadRegalo(regalo.getIdRegalo()));
        } finally {
            propiedadUrl.restaurar();
        }
    }

    @Test
    void rechazaIdentificadoresInexistentesSinCambiarLaBase() {
        int invitacionesAntes = repositoryInvitacion.listarInvitaciones(evento).size();

        assertThrows(IllegalArgumentException.class,
                () -> service.crearInvitacion(999999, invitacion(LUGAR_GRANDE, "Evento inexistente")));
        assertThrows(IllegalArgumentException.class,
                () -> service.validarEventoDelOrganizador(ORGANIZADOR, 999999));
        assertThrows(IllegalArgumentException.class, () -> service.eventoDeInvitacion(999999));
        assertThrows(IllegalArgumentException.class, () -> service.eventoDeInvitado(999999));
        assertThrows(IllegalArgumentException.class, () -> service.eventoDeRegalo(999999));
        assertThrows(IllegalArgumentException.class,
                () -> service.actualizarInvitacion(999999, invitacion(LUGAR_GRANDE, "Invitación inexistente")));
        assertThrows(IllegalArgumentException.class,
                () -> service.agregarRegalo(ORGANIZADOR, 999999,
                        new Regalo("Libro", "Edición ilustrada", new BigDecimal("25000.00"))));
        assertThrows(IllegalArgumentException.class, () -> service.quitarInvitado(999999));
        assertThrows(IllegalArgumentException.class, () -> service.quitarRegalo(ORGANIZADOR, 999999));
        assertThrows(IllegalArgumentException.class, () -> service.validarDisponibilidadRegalo(999999));
        assertThrows(IllegalArgumentException.class,
                () -> service.consultarRegalosDisponibles(999999, "nadie@correo.com", "3001112222"));

        assertEquals(invitacionesAntes, repositoryInvitacion.listarInvitaciones(evento).size());
        assertTrue(service.listarInvitaciones(evento).isEmpty());
    }

    @Test
    void rechazaDatosInvalidosAlCrearInvitacionesSinPersistir() {
        int invitacionesAntes = repositoryInvitacion.listarInvitaciones(evento).size();

        assertThrows(IllegalArgumentException.class, () -> service.crearInvitacion(evento, null));
        assertThrows(IllegalArgumentException.class,
                () -> service.crearInvitacion(evento, invitacion(LUGAR_GRANDE, null)));
        assertThrows(IllegalArgumentException.class,
                () -> service.crearInvitacion(evento, invitacion(LUGAR_GRANDE, "   ")));
        assertThrows(IllegalArgumentException.class,
                () -> service.crearInvitacion(evento, invitacion(LUGAR_GRANDE, "m".repeat(501))));
        assertThrows(IllegalArgumentException.class,
                () -> service.crearInvitacion(evento, invitacion(999999, "Lugar inexistente")));

        assertEquals(invitacionesAntes, repositoryInvitacion.listarInvitaciones(evento).size());
    }

    @Test
    void rechazaActualizacionesInvalidasYConservaLaInvitacion() {
        Invitacion original = crearInvitacion(evento, LUGAR_GRANDE, "Mensaje que se conserva");
        int id = original.getIdInvitacion();

        assertThrows(IllegalArgumentException.class, () -> service.actualizarInvitacion(id, null));
        assertThrows(IllegalArgumentException.class,
                () -> service.actualizarInvitacion(id, invitacion(LUGAR_GRANDE, null)));
        assertThrows(IllegalArgumentException.class,
                () -> service.actualizarInvitacion(id, invitacion(LUGAR_GRANDE, "m".repeat(501))));
        assertThrows(IllegalArgumentException.class,
                () -> service.actualizarInvitacion(id, invitacion(999999, "Lugar inexistente")));

        Invitacion guardada = repositoryInvitacion.buscarInvitacion(id);
        assertEquals(LUGAR_GRANDE, guardada.getIdLugar());
        assertEquals("Mensaje que se conserva", guardada.getMensaje());
    }

    @Test
    void rechazaCambiarDeLugarCuandoSeSuperariaElAforo() throws Exception {
        Invitacion primera = crearInvitacion(evento, LUGAR_PEQUENO, "Lugar casi lleno");
        Invitado ana = agregarInvitado(
                primera.getIdInvitacion(), "Ana", "ana@correo.com", "3001112222");
        ejecutar("INSERT INTO Acompanante (id_invitado, nombre, tipo) VALUES ("
                + ana.getIdInvitado() + ", 'Acompañante de Ana', 'ADULTO')");

        Invitacion segunda = crearInvitacion(evento, LUGAR_GRANDE, "Debe conservar su lugar");
        agregarInvitado(segunda.getIdInvitacion(), "Luis", "luis@correo.com", null);

        assertThrows(IllegalArgumentException.class,
                () -> service.actualizarInvitacion(segunda.getIdInvitacion(),
                        invitacion(LUGAR_PEQUENO, "Cambio que excede el aforo")));

        Invitacion guardada = repositoryInvitacion.buscarInvitacion(segunda.getIdInvitacion());
        assertEquals(LUGAR_GRANDE, guardada.getIdLugar());
        assertEquals("Debe conservar su lugar", guardada.getMensaje());
        assertEquals(2, service.contarAsistentes(evento, LUGAR_PEQUENO));
        assertEquals(1, service.contarAsistentes(evento, LUGAR_GRANDE));
    }

    @Test
    void rechazaDatosObligatoriosYLimitesDeInvitadosSinPersistir() {
        Invitacion invitacion = crearInvitacion(evento, LUGAR_GRANDE, "Datos del invitado");
        String correoLargo = "a".repeat(140) + "@correo.com";
        int invitadosAntes = service.listarInvitados(invitacion.getIdInvitacion()).size();

        assertThrows(IllegalArgumentException.class,
                () -> service.agregarInvitado(invitacion.getIdInvitacion(), null));
        assertThrows(IllegalArgumentException.class,
                () -> service.agregarInvitado(invitacion.getIdInvitacion(),
                        new Invitado("   ", "ana@correo.com", null, null)));
        assertThrows(IllegalArgumentException.class,
                () -> service.agregarInvitado(invitacion.getIdInvitacion(),
                        new Invitado("a".repeat(101), "ana@correo.com", null, null)));
        assertThrows(IllegalArgumentException.class,
                () -> service.agregarInvitado(invitacion.getIdInvitacion(),
                        new Invitado("Ana", null, null, null)));
        assertThrows(IllegalArgumentException.class,
                () -> service.agregarInvitado(invitacion.getIdInvitacion(),
                        new Invitado("Ana", correoLargo, null, null)));
        assertThrows(IllegalArgumentException.class,
                () -> service.agregarInvitado(invitacion.getIdInvitacion(),
                        new Invitado("Ana", "correo-inválido", null, null)));
        assertThrows(IllegalArgumentException.class,
                () -> service.agregarInvitado(invitacion.getIdInvitacion(),
                        new Invitado("Ana", "ana@correo.com", "300-ABC", null)));
        assertThrows(IllegalArgumentException.class,
                () -> service.agregarInvitado(invitacion.getIdInvitacion(),
                        new Invitado("Ana", "ana@correo.com", "3".repeat(31), null)));
        assertThrows(IllegalArgumentException.class,
                () -> service.agregarInvitado(invitacion.getIdInvitacion(),
                        new Invitado("Ana", "ana@correo.com", null, "d".repeat(256))));

        assertEquals(invitadosAntes, service.listarInvitados(invitacion.getIdInvitacion()).size());
    }

    @Test
    void validaCorreosYTelefonosNulosOFueraDeLimiteEnConsultas() {
        String correoLargo = "a".repeat(140) + "@correo.com";

        assertThrows(IllegalArgumentException.class, () -> service.consultarGaleria(null, "3001112222"));
        assertThrows(IllegalArgumentException.class,
                () -> service.consultarGaleria(correoLargo, "3001112222"));
        assertThrows(IllegalArgumentException.class,
                () -> service.consultarGaleria("ana@correo.com", null));
        assertThrows(IllegalArgumentException.class,
                () -> service.consultarGaleria("ana@correo.com", "---"));
        assertThrows(IllegalArgumentException.class,
                () -> service.consultarInvitacionesPorCorreo(null, "3001112222"));
        assertThrows(IllegalArgumentException.class,
                () -> service.consultarInvitacionesPorCorreo(correoLargo, "3001112222"));
        assertThrows(IllegalArgumentException.class,
                () -> service.consultarInvitacionesPorCorreo("correo-inválido", "3001112222"));
        assertThrows(IllegalArgumentException.class,
                () -> service.consultarInvitacionesPorCorreo("ana@correo.com", null));
        assertThrows(IllegalArgumentException.class, () -> service.consultarAvisosPorCorreo(null));
        assertThrows(IllegalArgumentException.class, () -> service.consultarAvisosPorCorreo(correoLargo));

        Invitacion invitacion = crearInvitacion(evento, LUGAR_GRANDE, "Invitado sin teléfono");
        agregarInvitado(invitacion.getIdInvitacion(), "Eva", "eva@correo.com", null);
        assertTrue(service.consultarInvitacionesPorCorreo("eva@correo.com", "3001112222").isEmpty());
    }

    @Test
    void rechazaRespuestasInvalidasODeUnEventoIniciadoSinCambiarDatos() throws Exception {
        Invitacion invitacion = crearInvitacion(evento, LUGAR_GRANDE, "Respuesta pendiente");
        Invitado ana = agregarInvitado(
                invitacion.getIdInvitacion(), "Ana", "ana@correo.com", "3001112222");

        assertThrows(IllegalArgumentException.class,
                () -> service.gestionarRespuestaInvitacion(
                        ana.getIdInvitado(), ana.getCorreo(), ana.getTelefono(), null));
        assertThrows(IllegalArgumentException.class,
                () -> service.gestionarRespuestaInvitacion(
                        ana.getIdInvitado(), ana.getCorreo(), ana.getTelefono(), "QUIZAS"));
        assertThrows(IllegalArgumentException.class,
                () -> service.gestionarRespuestaInvitacion(
                        999999, ana.getCorreo(), ana.getTelefono(), Invitado.ESTADO_CONFIRMADO));
        assertThrows(IllegalArgumentException.class,
                () -> service.gestionarRespuestaInvitacion(
                        ana.getIdInvitado(), null, ana.getTelefono(), Invitado.ESTADO_CONFIRMADO));

        ejecutar("UPDATE Invitado SET correo = NULL WHERE id_invitado = " + ana.getIdInvitado());
        assertThrows(IllegalArgumentException.class,
                () -> service.gestionarRespuestaInvitacion(
                        ana.getIdInvitado(), ana.getCorreo(), ana.getTelefono(), Invitado.ESTADO_CONFIRMADO));

        ejecutar("UPDATE Invitado SET correo = 'ana@correo.com' WHERE id_invitado = " + ana.getIdInvitado());
        ejecutar("UPDATE Evento SET estado = 'CANCELADO' WHERE id_evento = " + evento);
        assertThrows(IllegalArgumentException.class,
                () -> service.gestionarRespuestaInvitacion(
                        ana.getIdInvitado(), ana.getCorreo(), ana.getTelefono(), Invitado.ESTADO_CONFIRMADO));
        ejecutar("UPDATE Evento SET estado = 'ACTIVO' WHERE id_evento = " + evento);
        ejecutar("UPDATE Evento SET fecha_inicio = TIMESTAMP '2000-01-01 10:00:00' WHERE id_evento = " + evento);
        assertThrows(IllegalArgumentException.class,
                () -> service.gestionarRespuestaInvitacion(
                        ana.getIdInvitado(), ana.getCorreo(), ana.getTelefono(), Invitado.ESTADO_CONFIRMADO));

        assertEquals(Invitado.ESTADO_PENDIENTE,
                repositoryInvitado.buscarInvitado(ana.getIdInvitado()).getEstado());
        assertEquals(Invitacion.ESTADO_ENVIADA,
                repositoryInvitacion.buscarInvitacion(invitacion.getIdInvitacion()).getEstadoRespuesta());
    }

    @Test
    void tomaElEstadoNuloDelInvitadoComoPendiente() throws Exception {
        Invitacion invitacion = crearInvitacion(evento, LUGAR_GRANDE, "Estado nulo legado");
        Invitado ana = agregarInvitado(
                invitacion.getIdInvitacion(), "Ana", "ana@correo.com", "3001112222");
        ejecutar("UPDATE Invitado SET estado = NULL WHERE id_invitado = " + ana.getIdInvitado());

        service.gestionarRespuestaInvitacion(
                ana.getIdInvitado(), ana.getCorreo(), ana.getTelefono(), Invitado.ESTADO_CONFIRMADO);

        assertEquals(Invitado.ESTADO_CONFIRMADO,
                repositoryInvitado.buscarInvitado(ana.getIdInvitado()).getEstado());
        assertEquals(Invitacion.ESTADO_ACEPTADA,
                repositoryInvitacion.buscarInvitacion(invitacion.getIdInvitacion()).getEstadoRespuesta());
    }

    @Test
    void rechazaDatosInvalidosDeRegalosSinPersistir() {
        Invitacion invitacion = crearInvitacion(evento, LUGAR_GRANDE, "Validación de regalos");
        String nombreLargo = "r".repeat(151);
        String descripcionLarga = "d".repeat(256);
        int regalosAntes = repositoryRegalo.listarRegalos(invitacion.getIdInvitacion()).size();

        assertThrows(IllegalArgumentException.class,
                () -> service.agregarRegalo(ORGANIZADOR, invitacion.getIdInvitacion(), null));
        assertThrows(IllegalArgumentException.class,
                () -> service.agregarRegalo(ORGANIZADOR, invitacion.getIdInvitacion(),
                        new Regalo(null, "Descripción", new BigDecimal("100.00"))));
        assertThrows(IllegalArgumentException.class,
                () -> service.agregarRegalo(ORGANIZADOR, invitacion.getIdInvitacion(),
                        new Regalo(nombreLargo, "Descripción", new BigDecimal("100.00"))));
        assertThrows(IllegalArgumentException.class,
                () -> service.agregarRegalo(ORGANIZADOR, invitacion.getIdInvitacion(),
                        new Regalo("Libro", descripcionLarga, new BigDecimal("100.00"))));
        assertThrows(IllegalArgumentException.class,
                () -> service.agregarRegalo(ORGANIZADOR, invitacion.getIdInvitacion(),
                        new Regalo("Libro", null, null)));
        assertThrows(IllegalArgumentException.class,
                () -> service.agregarRegalo(ORGANIZADOR, invitacion.getIdInvitacion(),
                        new Regalo("Libro", null, BigDecimal.ZERO)));
        assertThrows(IllegalArgumentException.class,
                () -> service.agregarRegalo(ORGANIZADOR, invitacion.getIdInvitacion(),
                        new Regalo("Libro", null, new BigDecimal("-1.00"))));
        assertThrows(IllegalArgumentException.class,
                () -> service.agregarRegalo(ORGANIZADOR, invitacion.getIdInvitacion(),
                        new Regalo("Libro", null, new BigDecimal("100.001"))));
        assertThrows(IllegalArgumentException.class,
                () -> service.agregarRegalo(ORGANIZADOR, invitacion.getIdInvitacion(),
                        new Regalo("Libro", null, new BigDecimal("10000000000.00"))));

        assertEquals(regalosAntes, repositoryRegalo.listarRegalos(invitacion.getIdInvitacion()).size());
    }

    @Test
    void bloqueaLaMesaDeRegalosEnEventosCerradosOIniciados() throws Exception {
        Invitacion invitacion = crearInvitacion(evento, LUGAR_GRANDE, "Evento con mesa");
        Invitado ana = agregarInvitado(
                invitacion.getIdInvitacion(), "Ana", "ana@correo.com", "3001112222");
        Regalo regalo = new Regalo("Jarrón", "Cerámica", new BigDecimal("45000.00"));
        service.agregarRegalo(ORGANIZADOR, invitacion.getIdInvitacion(), regalo);

        ejecutar("UPDATE Evento SET estado = 'CANCELADO' WHERE id_evento = " + evento);
        assertThrows(IllegalArgumentException.class,
                () -> service.agregarRegalo(ORGANIZADOR, invitacion.getIdInvitacion(),
                        new Regalo("Libro", null, new BigDecimal("25000.00"))));
        ejecutar("UPDATE Evento SET estado = 'FINALIZADO' WHERE id_evento = " + evento);
        assertThrows(IllegalArgumentException.class,
                () -> service.agregarRegalo(ORGANIZADOR, invitacion.getIdInvitacion(),
                        new Regalo("Planta", null, new BigDecimal("18000.00"))));
        assertEquals(1, repositoryRegalo.listarRegalos(invitacion.getIdInvitacion()).size());

        ejecutar("UPDATE Evento SET estado = 'ACTIVO', fecha_inicio = "
                + "TIMESTAMP '2000-01-01 10:00:00' WHERE id_evento = " + evento);
        assertThrows(IllegalArgumentException.class,
                () -> service.reservarRegalo(ana.getIdInvitado(), ana.getCorreo(),
                        ana.getTelefono(), regalo.getIdRegalo()));
        assertEquals(0, new RepositoryReservaRegalo(url).contarReservasActivas(regalo.getIdRegalo()));
        assertTrue(service.validarDisponibilidadRegalo(regalo.getIdRegalo()));
    }

    @Test
    void rechazaReservasDeOtraMesaRepetidasYCancelacionesSinReservaActiva() {
        Invitacion mesaAna = crearInvitacion(evento, LUGAR_GRANDE, "Mesa de Ana");
        Invitacion mesaLuis = crearInvitacion(evento, LUGAR_GRANDE, "Mesa de Luis");
        Invitado ana = agregarInvitado(mesaAna.getIdInvitacion(), "Ana", "ana@correo.com", "3001112222");
        Invitado luis = agregarInvitado(mesaAna.getIdInvitacion(), "Luis", "luis@correo.com", "3001113333");
        Regalo regaloAna = new Regalo("Jarrón", "Cerámica", new BigDecimal("45000.00"));
        Regalo regaloLuis = new Regalo("Libro", "Edición ilustrada", new BigDecimal("25000.00"));
        service.agregarRegalo(ORGANIZADOR, mesaAna.getIdInvitacion(), regaloAna);
        service.agregarRegalo(ORGANIZADOR, mesaLuis.getIdInvitacion(), regaloLuis);
        RepositoryReservaRegalo reservas = new RepositoryReservaRegalo(url);

        assertThrows(IllegalArgumentException.class,
                () -> service.cancelarReservaRegalo(
                        ana.getIdInvitado(), ana.getCorreo(), ana.getTelefono(), regaloAna.getIdRegalo()));
        assertThrows(IllegalArgumentException.class,
                () -> service.reservarRegalo(
                        ana.getIdInvitado(), ana.getCorreo(), ana.getTelefono(), regaloLuis.getIdRegalo()));
        assertEquals(0, reservas.contarReservasActivas(regaloAna.getIdRegalo()));
        assertEquals(0, reservas.contarReservasActivas(regaloLuis.getIdRegalo()));

        service.reservarRegalo(ana.getIdInvitado(), ana.getCorreo(), ana.getTelefono(), regaloAna.getIdRegalo());
        assertEquals(1, reservas.contarReservasActivas(regaloAna.getIdRegalo()));
        assertThrows(IllegalArgumentException.class,
                () -> service.reservarRegalo(
                        ana.getIdInvitado(), ana.getCorreo(), ana.getTelefono(), regaloAna.getIdRegalo()));
        assertThrows(IllegalArgumentException.class,
                () -> service.reservarRegalo(
                        luis.getIdInvitado(), luis.getCorreo(), luis.getTelefono(), regaloAna.getIdRegalo()));
        assertThrows(IllegalArgumentException.class,
                () -> service.cancelarReservaRegalo(
                        luis.getIdInvitado(), luis.getCorreo(), luis.getTelefono(), regaloAna.getIdRegalo()));

        assertEquals(1, reservas.contarReservasActivas(regaloAna.getIdRegalo()));
        assertFalse(service.validarDisponibilidadRegalo(regaloAna.getIdRegalo()));
        service.cancelarReservaRegalo(
                ana.getIdInvitado(), ana.getCorreo(), ana.getTelefono(), regaloAna.getIdRegalo());
        assertEquals(0, reservas.contarReservasActivas(regaloAna.getIdRegalo()));
        assertTrue(service.validarDisponibilidadRegalo(regaloAna.getIdRegalo()));
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

    private ServiceInvitados servicioCoordinado(ServiceInvitados.Coordinador coordinador) {
        return new ServiceInvitados(repositoryInvitacion, repositoryInvitado,
                new RepositoryLugar(url), repositoryEvento, repositoryRegalo,
                new RepositoryReservaRegalo(url), coordinador);
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

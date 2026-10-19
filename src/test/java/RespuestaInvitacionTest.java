import controllers.ControllerInvitados;
import entities.Invitacion;
import entities.InvitacionRecibida;
import entities.Invitado;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repositories.RepositoryEvento;
import repositories.RepositoryInvitacion;
import repositories.RepositoryInvitado;
import repositories.RepositoryLugar;
import services.ServiceInvitados;
import static org.junit.jupiter.api.Assertions.*;

/** CU-16: el invitado consulta sus invitaciones y responde si asistirá. */
class RespuestaInvitacionTest {
    private static final int EVENTO = DatosInvitacionesPrueba.EVENTO;
    private static final int GRANDE = DatosInvitacionesPrueba.LUGAR_GRANDE;
    private static final int PEQUENO = DatosInvitacionesPrueba.LUGAR_PEQUENO;

    @TempDir Path directorio;
    private String url;
    private ServiceInvitados service;
    private RepositoryInvitacion invitaciones;
    private RepositoryInvitado invitados;

    @BeforeEach
    void prepararBase() throws Exception {
        url = "jdbc:h2:" + directorio.resolve("respuestas");
        DatosInvitacionesPrueba.preparar(url);
        invitaciones = new RepositoryInvitacion(url);
        invitados = new RepositoryInvitado(url);
        service = new ServiceInvitados(invitaciones, invitados, new RepositoryLugar(url), new RepositoryEvento(url));
    }

    // ---------------------------------------------------------------- R37 consultar
    @Test
    void consultaLasInvitacionesActivasDelCorreoSinImportarMayusculas() {
        int primera = crear(EVENTO, GRANDE);
        int segunda = crear(DatosInvitacionesPrueba.OTRO_EVENTO, GRANDE);
        agregar(primera, "Ana", "ana@correo.com");
        agregar(segunda, "Ana", "ana@correo.com");
        agregar(primera, "Luis", "luis@correo.com");

        List<InvitacionRecibida> encontradas = new ControllerInvitados(service).consultarInvitacionesPorCorreo("  ANA@correo.com ");
        assertEquals(2, encontradas.size());
        InvitacionRecibida boda = encontradas.get(0);
        assertEquals("Boda", boda.getEvento());
        assertEquals("Salón Grande", boda.getLugar());
        assertEquals(100, boda.getCapacidad());
        assertEquals(2, boda.getOcupados());
        assertEquals(Invitado.ESTADO_PENDIENTE, boda.getEstadoInvitado());
    }

    @Test
    void noMuestraInvitacionesRevocadasYValidaElCorreo() {
        int id = crear(EVENTO, GRANDE);
        agregar(id, "Ana", "ana@correo.com");
        service.revocarInvitacion(id);
        assertTrue(service.consultarInvitacionesPorCorreo("ana@correo.com").isEmpty());
        assertMensaje(() -> service.consultarInvitacionesPorCorreo("sin-arroba"), "correo válido");
        assertMensaje(() -> service.consultarInvitacionesPorCorreo(" "), "correo válido");
    }

    // ---------------------------------------------------------------- R38 responder
    @Test
    void confirmarMarcaAlInvitadoYDejaLaInvitacionAceptada() {
        int id = crear(EVENTO, GRANDE);
        int ana = agregar(id, "Ana", "ana@correo.com");
        agregar(id, "Luis", "luis@correo.com");
        new ControllerInvitados(service).gestionarRespuestaInvitacion(ana, "confirmado");
        assertEquals(Invitado.ESTADO_CONFIRMADO, invitados.buscarInvitado(ana).getEstado());
        assertEquals(Invitacion.ESTADO_ACEPTADA, invitaciones.buscarInvitacion(id).getEstadoRespuesta());
    }

    @Test
    void laInvitacionQuedaRechazadaSoloCuandoTodosRechazan() {
        int id = crear(EVENTO, GRANDE);
        int ana = agregar(id, "Ana", "ana@correo.com");
        int luis = agregar(id, "Luis", "luis@correo.com");
        service.gestionarRespuestaInvitacion(ana, Invitado.ESTADO_RECHAZADO);
        assertEquals(Invitacion.ESTADO_ENVIADA, invitaciones.buscarInvitacion(id).getEstadoRespuesta());
        service.gestionarRespuestaInvitacion(luis, Invitado.ESTADO_RECHAZADO);
        assertEquals(Invitacion.ESTADO_RECHAZADA, invitaciones.buscarInvitacion(id).getEstadoRespuesta());
    }

    @Test
    void validaLaRespuestaYNoRepiteLaMisma() {
        int id = crear(EVENTO, GRANDE);
        int ana = agregar(id, "Ana", "ana@correo.com");
        assertMensaje(() -> service.gestionarRespuestaInvitacion(ana, "QUIZAS"), "Elige");
        assertMensaje(() -> service.gestionarRespuestaInvitacion(ana, null), "Elige");
        assertMensaje(() -> service.gestionarRespuestaInvitacion(999, Invitado.ESTADO_CONFIRMADO), "no existe");
        service.gestionarRespuestaInvitacion(ana, Invitado.ESTADO_CONFIRMADO);
        assertMensaje(() -> service.gestionarRespuestaInvitacion(ana, Invitado.ESTADO_CONFIRMADO), "Ya habías confirmado");
    }

    @Test
    void noPermiteResponderInvitacionesRevocadasNiEventosPasados() throws Exception {
        int id = crear(EVENTO, GRANDE);
        int ana = agregar(id, "Ana", "ana@correo.com");
        service.revocarInvitacion(id);
        assertMensaje(() -> service.gestionarRespuestaInvitacion(ana, Invitado.ESTADO_CONFIRMADO), "revocada");

        int pasada = DatosInvitacionesPrueba.insertarInvitacion(url, DatosInvitacionesPrueba.EVENTO_PASADO, GRANDE);
        Invitado luis = new Invitado(0, pasada, "Luis", null, null, Invitado.ESTADO_PENDIENTE, "luis@correo.com");
        invitados.guardarInvitado(luis);
        assertMensaje(() -> service.gestionarRespuestaInvitacion(luis.getIdInvitado(), Invitado.ESTADO_CONFIRMADO), "ya comenzó");
        assertEquals(Invitado.ESTADO_PENDIENTE, invitados.buscarInvitado(luis.getIdInvitado()).getEstado());
    }

    // ---------------------------------------------------------------- R39 cupos
    @Test
    void rechazarLiberaElCupoDelInvitadoYSusAcompanantes() throws Exception {
        int id = crear(EVENTO, PEQUENO);
        int ana = agregar(id, "Ana", "ana@correo.com");
        DatosInvitacionesPrueba.agregarAcompanante(url, ana);
        assertEquals(2, service.contarAsistentes(EVENTO, PEQUENO));
        assertMensaje(() -> agregar(id, "Luis", "luis@correo.com"), "aforo");

        service.gestionarRespuestaInvitacion(ana, Invitado.ESTADO_RECHAZADO);
        assertEquals(0, service.contarAsistentes(EVENTO, PEQUENO));
        agregar(id, "Luis", "luis@correo.com");
        assertEquals(1, service.contarAsistentes(EVENTO, PEQUENO));
    }

    @Test
    void volverAConfirmarDespuesDeRechazarExigeCupo() throws Exception {
        int id = crear(EVENTO, PEQUENO);
        int ana = agregar(id, "Ana", "ana@correo.com");
        DatosInvitacionesPrueba.agregarAcompanante(url, ana);
        service.gestionarRespuestaInvitacion(ana, Invitado.ESTADO_RECHAZADO);
        agregar(id, "Luis", "luis@correo.com");
        // Quedan 1 de 2 cupos: Ana necesita 2 (ella y su acompañante).
        assertMensaje(() -> service.gestionarRespuestaInvitacion(ana, Invitado.ESTADO_CONFIRMADO), "cupo");
        assertEquals(Invitado.ESTADO_RECHAZADO, invitados.buscarInvitado(ana).getEstado());

        int luis = invitados.listarInvitados(id).get(1).getIdInvitado();
        service.gestionarRespuestaInvitacion(luis, Invitado.ESTADO_RECHAZADO);
        service.gestionarRespuestaInvitacion(ana, Invitado.ESTADO_CONFIRMADO);
        assertEquals(Invitado.ESTADO_CONFIRMADO, invitados.buscarInvitado(ana).getEstado());
    }

    // ---------------------------------------------------------------- utilidades
    private int crear(int evento, int lugar) {
        Invitacion invitacion = new Invitacion();
        invitacion.setIdLugar(lugar);
        invitacion.setMensaje("Te invitamos");
        service.crearInvitacion(evento, invitacion);
        return invitacion.getIdInvitacion();
    }

    private int agregar(int idInvitacion, String nombre, String correo) {
        Invitado invitado = new Invitado(nombre, correo, null, null);
        service.agregarInvitado(idInvitacion, invitado);
        return invitado.getIdInvitado();
    }

    private void assertMensaje(org.junit.jupiter.api.function.Executable accion, String motivo) {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, accion);
        assertTrue(error.getMessage().contains(motivo), error.getMessage());
    }
}

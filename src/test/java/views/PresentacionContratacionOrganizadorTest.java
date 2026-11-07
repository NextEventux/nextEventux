package views;

import entities.Contratacion;
import entities.Organizador;
import entities.ResenaOrganizador;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PresentacionContratacionOrganizadorTest {

    private final Organizador organizador = new Organizador(
            1, "Laura Gómez", "Eventos Laura", "Bogota",
            "Bodas y eventos sociales", "Organizacion de bodas",
            new BigDecimal("1500000.00"), new BigDecimal("4.50"), 2);

    @Test
    void calificacionConYSinResenas() {
        assertEquals("4,5 / 5 · 2 reseñas",
                PresentacionContratacionOrganizador.calificacion(organizador));

        Organizador una = new Organizador(2, "Carlos", "", "", "", "",
                BigDecimal.ONE, new BigDecimal("5"), 1);
        assertEquals("5,0 / 5 · 1 reseña",
                PresentacionContratacionOrganizador.calificacion(una));

        assertEquals("Sin reseñas",
                PresentacionContratacionOrganizador.calificacion(new Organizador(3, "Nuevo")));
    }

    @Test
    void tarjetaMuestraLosDatosParaComparar() {
        String tarjeta = PresentacionContratacionOrganizador.tarjeta(organizador);

        assertTrue(tarjeta.startsWith("Laura Gómez\nEventos Laura · Bogota"));
        assertTrue(tarjeta.contains("Especialidad: Bodas y eventos sociales"));
        assertTrue(tarjeta.contains("Tarifa base: $1.500.000"));
        assertTrue(tarjeta.contains("Calificación: 4,5 / 5 · 2 reseñas"));

        String incompleta = PresentacionContratacionOrganizador.tarjeta(new Organizador(3, "Nuevo"));
        assertTrue(incompleta.contains("Empresa no registrada · Ciudad no registrada"));
        assertTrue(incompleta.contains("Especialidad: No registrada"));
    }

    @Test
    void resenasVaciasYLlenas() {
        assertEquals(PresentacionContratacionOrganizador.SIN_RESENAS,
                PresentacionContratacionOrganizador.resenas(List.of()));
        assertEquals(PresentacionContratacionOrganizador.SIN_RESENAS,
                PresentacionContratacionOrganizador.resenas(null));

        String texto = PresentacionContratacionOrganizador.resenas(List.of(
                new ResenaOrganizador(5, "Muy puntual", LocalDateTime.of(2026, 3, 15, 18, 30)),
                new ResenaOrganizador(4, null, null)));

        assertEquals("★ 5 / 5 · 15 mar. 2026\nMuy puntual\n\n★ 4 / 5\nSin comentario.", texto);
    }

    @Test
    void resumenIncluyeOrganizadorDatosDelEventoYCriterios() {
        String resumen = PresentacionContratacionOrganizador.resumen(
                organizador, "Boda", "Bogota", LocalDate.of(2027, 6, 1),
                new BigDecimal("2000000"), "jardin");

        assertTrue(resumen.contains("Organizador: Laura Gómez"));
        assertTrue(resumen.contains("Empresa: Eventos Laura"));
        assertTrue(resumen.contains("Tarifa base: $1.500.000"));
        assertTrue(resumen.contains("Tipo de evento: Boda"));
        assertTrue(resumen.contains("Ciudad: Bogota"));
        assertTrue(resumen.contains("Fecha aproximada: 1 jun. 2027"));
        assertTrue(resumen.contains("Presupuesto estimado: $2.000.000"));
        assertTrue(resumen.contains("Criterios adicionales: jardin"));
        assertTrue(resumen.contains("estado Pendiente"));

        assertTrue(PresentacionContratacionOrganizador.resumen(
                organizador, "Boda", "Bogota", LocalDate.of(2027, 6, 1),
                BigDecimal.TEN, "  ").contains("Criterios adicionales: Ninguno"));
    }

    @Test
    void rolDeLaSesionBajoElNombre() {
        entities.SesionUsuario sesion = new entities.SesionUsuario();
        sesion.setTipoUsuario("CLIENTE");
        assertEquals("Cliente", PresentacionContratacionOrganizador.rol(sesion));

        sesion.setTipoUsuario("ORGANIZADOR");
        assertEquals("Organizador", PresentacionContratacionOrganizador.rol(sesion));

        sesion.setTipoUsuario(" ");
        assertEquals("Usuario", PresentacionContratacionOrganizador.rol(sesion));
        assertEquals("Usuario", PresentacionContratacionOrganizador.rol(null));
    }

    @Test
    void estadoLegible() {
        assertEquals("Pendiente", PresentacionContratacionOrganizador.estado("PENDIENTE"));
        assertEquals("En revision", PresentacionContratacionOrganizador.estado("EN_REVISION"));
        assertEquals(FormatoUi.SIN_DATO, PresentacionContratacionOrganizador.estado(null));
    }

    @Test
    void vistaDelOrganizador() {
        Contratacion pendiente = new Contratacion(5, 1, "Boda", "Bogota",
                LocalDate.of(2027, 6, 1), new BigDecimal("2000000"), null);
        pendiente.setIdContratacion(7);
        pendiente.setNombreCliente("Ana Martínez");

        Contratacion aceptada = new Contratacion();
        aceptada.setEstado("ACEPTADA");

        assertTrue(PresentacionContratacionOrganizador.puedeResponder(pendiente));
        assertFalse(PresentacionContratacionOrganizador.puedeResponder(aceptada));
        assertFalse(PresentacionContratacionOrganizador.puedeResponder(null));

        String p = PresentacionContratacionOrganizador.FILTRO_PENDIENTES;
        String r = PresentacionContratacionOrganizador.FILTRO_RESPONDIDAS;
        String t = PresentacionContratacionOrganizador.FILTRO_TODAS;
        assertTrue(PresentacionContratacionOrganizador.coincideFiltro(pendiente, p));
        assertFalse(PresentacionContratacionOrganizador.coincideFiltro(pendiente, r));
        assertTrue(PresentacionContratacionOrganizador.coincideFiltro(aceptada, r));
        assertFalse(PresentacionContratacionOrganizador.coincideFiltro(aceptada, p));
        assertTrue(PresentacionContratacionOrganizador.coincideFiltro(aceptada, t));

        assertEquals("chip-aviso", PresentacionContratacionOrganizador.estiloEstado("PENDIENTE"));
        assertEquals("chip-ok", PresentacionContratacionOrganizador.estiloEstado("ACEPTADA"));
        assertEquals("chip-error", PresentacionContratacionOrganizador.estiloEstado("RECHAZADA"));
        assertEquals("chip-neutro", PresentacionContratacionOrganizador.estiloEstado(null));

        assertEquals("Solicitud #7 · Pendiente\nCliente: Ana Martínez\nBoda · Bogota · 1 jun. 2027",
                PresentacionContratacionOrganizador.solicitudRecibida(pendiente));
        assertTrue(PresentacionContratacionOrganizador.preguntaRespuesta(pendiente, false)
                .startsWith("¿Rechazar la solicitud #7 de Ana Martínez?"));

        pendiente.setEstado("RECHAZADA");
        assertEquals("Solicitud #7 rechazada. Se notificó a Ana Martínez.",
                PresentacionContratacionOrganizador.confirmacionRespuesta(pendiente));
    }

    @Test
    void lineaDeMisSolicitudesYConfirmacion() {
        Contratacion solicitud = new Contratacion(5, 1, "Boda", "Bogota",
                LocalDate.of(2027, 6, 1), new BigDecimal("2000000"), null);
        solicitud.setIdContratacion(12);
        solicitud.setNombreOrganizador("Laura Gómez");
        solicitud.setFechaSolicitud(LocalDateTime.of(2026, 11, 6, 9, 5));

        String linea = PresentacionContratacionOrganizador.solicitud(solicitud);

        assertTrue(linea.startsWith("Solicitud #12 · Pendiente"));
        assertTrue(linea.contains("Organizador: Laura Gómez"));
        assertTrue(linea.contains("Fecha aproximada: 1 jun. 2027 · Presupuesto: $2.000.000"));
        assertTrue(linea.contains("Enviada: 6 nov. 2026, 09:05"));

        solicitud.setNombreOrganizador(null);
        assertTrue(PresentacionContratacionOrganizador.solicitud(solicitud)
                .contains("Organizador: Organizador #1"));

        assertEquals("Solicitud #12 enviada a Laura Gómez con estado Pendiente. "
                + "Se notificó al organizador y puedes consultar su estado en «Mis solicitudes».",
                PresentacionContratacionOrganizador.confirmacion(solicitud, "Laura Gómez"));
    }
}

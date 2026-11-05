package views.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import views.servicios.PresentacionServicio.Revision;

class PresentacionServicioTest {

    @Test
    void sinDisponibilidadNoSePuedePublicar() {
        Revision revision = PresentacionServicio.revisar(0, 3);

        assertFalse(revision.puedePublicar());
        assertEquals(
                "Agrega al menos un horario de disponibilidad para publicar.",
                revision.bloqueo());
    }

    @Test
    void sinImagenesSePuedePublicarPeroSeAvisa() {
        Revision revision = PresentacionServicio.revisar(2, 0);

        assertTrue(revision.puedePublicar());
        assertNull(revision.bloqueo());
        assertEquals(
                "Sin imágenes: los organizadores verán tu servicio sin fotos.",
                revision.aviso());
    }

    @Test
    void conTodoCompletoNoHayBloqueoNiAviso() {
        Revision revision = PresentacionServicio.revisar(1, 1);

        assertTrue(revision.puedePublicar());
        assertNull(revision.aviso());
    }

    @Test
    void laCapacidadVaciaSeMuestraSinLimite() {
        assertEquals("Sin límite", PresentacionServicio.capacidad(null));
        assertEquals("1 persona", PresentacionServicio.capacidad(1));
        assertEquals("150 personas", PresentacionServicio.capacidad(150));
    }

    @Test
    void laAnticipacionYLosConteosUsanSingularOPlural() {
        assertEquals("1 día", PresentacionServicio.anticipacion(1));
        assertEquals("0 días", PresentacionServicio.anticipacion(0));
        assertEquals("1 horario",
                PresentacionServicio.cantidad(1, "horario", "horarios"));
        assertEquals("3 horarios",
                PresentacionServicio.cantidad(3, "horario", "horarios"));
    }
}

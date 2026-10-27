package views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import entities.DisponibilidadContratacion;
import entities.PresupuestoContratacion;

class PresentacionContratacionTest {

    private static DisponibilidadContratacion disponible() {
        return new DisponibilidadContratacion(
                true, LocalDate.of(2026, 11, 20),
                LocalTime.of(15, 0), LocalTime.of(22, 0),
                LocalTime.of(8, 0), LocalTime.of(23, 0), "ok");
    }

    private static DisponibilidadContratacion noDisponible(String mensaje) {
        return new DisponibilidadContratacion(
                false, LocalDate.of(2026, 11, 20),
                LocalTime.of(15, 0), LocalTime.of(22, 0),
                null, null, mensaje);
    }

    private static PresupuestoContratacion presupuestoSuficiente() {
        return new PresupuestoContratacion(
                new BigDecimal("5000000"), new BigDecimal("1000000"),
                new BigDecimal("1200000"), new BigDecimal("2800000"),
                true, BigDecimal.ZERO, "ok");
    }

    private static PresupuestoContratacion presupuestoInsuficiente(BigDecimal faltante) {
        return new PresupuestoContratacion(
                new BigDecimal("5000000"), new BigDecimal("4500000"),
                new BigDecimal("1200000"), new BigDecimal("-700000"),
                false, faltante, "no alcanza");
    }

    @Test
    void puedeConfirmarSoloConDisponibilidadYPresupuestoSuficiente() {
        assertTrue(PresentacionContratacion.puedeConfirmar(
                disponible(), presupuestoSuficiente()));
    }

    @Test
    void noPuedeConfirmarSinDisponibilidad() {
        assertFalse(PresentacionContratacion.puedeConfirmar(
                noDisponible("Ocupado"), presupuestoSuficiente()));
    }

    @Test
    void noPuedeConfirmarConPresupuestoInsuficiente() {
        assertFalse(PresentacionContratacion.puedeConfirmar(
                disponible(), presupuestoInsuficiente(new BigDecimal("700000"))));
    }

    @Test
    void noPuedeConfirmarSiAlgunaVerificacionFalto() {
        assertFalse(PresentacionContratacion.puedeConfirmar(null, presupuestoSuficiente()));
        assertFalse(PresentacionContratacion.puedeConfirmar(disponible(), null));
    }

    @Test
    void textoDeDisponibilidadConfirmadaEsElDeLaPantalla() {
        assertEquals("Disponibilidad verificada para el evento",
                PresentacionContratacion.textoDisponibilidad(disponible()));
    }

    @Test
    void textoDeNoDisponibilidadUsaElMensajeDelService() {
        assertEquals("El proveedor ya tiene otra contratación ese día.",
                PresentacionContratacion.textoDisponibilidad(
                        noDisponible("El proveedor ya tiene otra contratación ese día.")));
    }

    @Test
    void textoDeNoDisponibilidadSinMensajeUsaUnoGenerico() {
        assertEquals(
                "El proveedor no está disponible para la fecha y el horario del evento.",
                PresentacionContratacion.textoDisponibilidad(noDisponible(" ")));
    }

    @Test
    void textoDePresupuestoInsuficienteIndicaElFaltante() {
        assertEquals("Al presupuesto del evento le faltan $700.000 para esta contratación.",
                PresentacionContratacion.textoPresupuesto(
                        presupuestoInsuficiente(new BigDecimal("700000"))));
    }

    @Test
    void textoDePresupuestoInsuficienteSinFaltanteEsGenerico() {
        assertEquals("El presupuesto del evento no alcanza para esta contratación.",
                PresentacionContratacion.textoPresupuesto(
                        presupuestoInsuficiente(BigDecimal.ZERO)));
    }

    @Test
    void motivoDeBloqueoEsNuloCuandoSePuedeConfirmar() {
        assertNull(PresentacionContratacion.motivoBloqueo(
                disponible(), presupuestoSuficiente()));
    }

    @Test
    void motivoDeBloqueoPriorizaLaDisponibilidad() {
        assertEquals("Ocupado", PresentacionContratacion.motivoBloqueo(
                noDisponible("Ocupado"),
                presupuestoInsuficiente(new BigDecimal("700000"))));
    }

    @Test
    void motivoDeBloqueoPorPresupuesto() {
        assertEquals("Al presupuesto del evento le faltan $700.000 para esta contratación.",
                PresentacionContratacion.motivoBloqueo(
                        disponible(), presupuestoInsuficiente(new BigDecimal("700000"))));
    }

    @Test
    void presupuestoDisponibleAntesDescuentaLoComprometido() {
        assertEquals(new BigDecimal("4000000"),
                PresentacionContratacion.presupuestoDisponibleAntes(presupuestoSuficiente()));
        assertNull(PresentacionContratacion.presupuestoDisponibleAntes(null));
    }

    @Test
    void presupuestoDisponibleDespuesVieneDelService() {
        assertEquals(new BigDecimal("2800000"),
                PresentacionContratacion.presupuestoDisponibleDespues(presupuestoSuficiente()));
    }
}

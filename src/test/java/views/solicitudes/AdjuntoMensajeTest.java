package views.solicitudes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import views.solicitudes.AdjuntoMensaje.Tipo;

class AdjuntoMensajeTest {

    @Test
    void sinTextoNoHayAdjunto() {
        assertNull(AdjuntoMensaje.de(null));
        assertNull(AdjuntoMensaje.de("   "));
    }

    @Test
    void unaDireccionWebEsUnEnlace() {
        AdjuntoMensaje adjunto
                = AdjuntoMensaje.de(" HTTPS://ejemplo.com/catalogo ");

        assertEquals(Tipo.ENLACE, adjunto.tipo());
        assertEquals("HTTPS://ejemplo.com/catalogo", adjunto.ubicacion());
        assertEquals("Enlace: HTTPS://ejemplo.com/catalogo", adjunto.etiqueta());
    }

    @Test
    void unaRutaConExtensionDeImagenEsUnaImagen() {
        AdjuntoMensaje adjunto
                = AdjuntoMensaje.de("/Users/ana/fotos/mesa.PNG");

        assertEquals(Tipo.IMAGEN, adjunto.tipo());
        assertEquals("mesa.PNG", adjunto.nombre());
        assertEquals("Imagen: mesa.PNG", adjunto.etiqueta());
    }

    @Test
    void otraRutaEsUnArchivoYSeLeeConBarrasDeWindows() {
        AdjuntoMensaje adjunto
                = AdjuntoMensaje.de("C:\\Users\\ana\\Documents\\cotizacion.pdf");

        assertEquals(Tipo.ARCHIVO, adjunto.tipo());
        assertEquals("cotizacion.pdf", adjunto.nombre());
    }

    @Test
    void unArchivoSinExtensionEsUnArchivo() {
        assertEquals(Tipo.ARCHIVO, AdjuntoMensaje.de("notas").tipo());
    }
}

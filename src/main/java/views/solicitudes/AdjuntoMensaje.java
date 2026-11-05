package views.solicitudes;

import java.util.Locale;
import java.util.Set;

/**
 * Adjunto opcional de un mensaje (CU-12): una imagen, un archivo o un enlace.
 * En la base se guarda un solo texto, la ruta del archivo o la dirección web,
 * y este tipo solo interpreta ese texto para mostrarlo.
 */
public record AdjuntoMensaje(Tipo tipo, String ubicacion, String nombre) {

    public enum Tipo {
        IMAGEN("Imagen"),
        ARCHIVO("Archivo"),
        ENLACE("Enlace");

        private final String texto;

        Tipo(String texto) {
            this.texto = texto;
        }

        public String texto() {
            return texto;
        }
    }

    private static final Set<String> EXTENSIONES_IMAGEN
            = Set.of("png", "jpg", "jpeg", "gif", "bmp");

    /** Interpreta el texto guardado; devuelve null si no hay adjunto. */
    public static AdjuntoMensaje de(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        String ubicacion = valor.trim();
        String minusculas = ubicacion.toLowerCase(Locale.ROOT);

        if (minusculas.startsWith("http://")
                || minusculas.startsWith("https://")) {
            return new AdjuntoMensaje(Tipo.ENLACE, ubicacion, ubicacion);
        }

        String nombre = nombreDeArchivo(ubicacion);
        Tipo tipo = EXTENSIONES_IMAGEN.contains(extension(nombre))
                ? Tipo.IMAGEN
                : Tipo.ARCHIVO;

        return new AdjuntoMensaje(tipo, ubicacion, nombre);
    }

    /** Texto breve para la conversación, por ejemplo "Imagen: mesa.png". */
    public String etiqueta() {
        return tipo.texto() + ": " + nombre;
    }

    private static String nombreDeArchivo(String ruta) {
        int corte = Math.max(ruta.lastIndexOf('/'), ruta.lastIndexOf('\\'));
        String nombre = ruta.substring(corte + 1);
        return nombre.isEmpty() ? ruta : nombre;
    }

    private static String extension(String nombre) {
        int punto = nombre.lastIndexOf('.');
        return punto < 0
                ? ""
                : nombre.substring(punto + 1).toLowerCase(Locale.ROOT);
    }
}

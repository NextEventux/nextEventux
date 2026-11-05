package services;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Valida el material de apoyo de una propuesta (CU-12). Solo se aceptan
 * enlaces https: una ruta de un archivo local no sirve en otro equipo, así
 * que el organizador nunca podría abrirla.
 */
public final class EnlaceMaterialApoyo {

    public static final int MAX_LONGITUD = 1000;

    private EnlaceMaterialApoyo() {
    }

    /**
     * Devuelve el enlace sin espacios en los extremos, o null si no se
     * escribió nada (el material es opcional). Lanza
     * IllegalArgumentException si el texto no es un enlace https válido.
     */
    public static String validar(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }

        String enlace = texto.trim();

        if (enlace.length() > MAX_LONGITUD) {
            throw new IllegalArgumentException(
                    "El material de apoyo no puede superar los "
                    + MAX_LONGITUD + " caracteres.");
        }

        if (!esEnlaceHttps(enlace)) {
            throw new IllegalArgumentException(
                    "El material de apoyo debe ser un enlace que empiece "
                    + "por https://, por ejemplo https://ejemplo.com/catalogo.");
        }

        return enlace;
    }

    private static boolean esEnlaceHttps(String enlace) {
        try {
            URI uri = new URI(enlace);
            return "https".equalsIgnoreCase(uri.getScheme())
                    && uri.getHost() != null;
        } catch (URISyntaxException e) {
            return false;
        }
    }
}

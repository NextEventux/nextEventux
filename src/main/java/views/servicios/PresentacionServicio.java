package views.servicios;

/**
 * Textos y reglas de presentación de un servicio del proveedor (CU-11).
 * No usa JavaFX, así que se prueba sin abrir una ventana. Las reglas de
 * negocio de publicación siguen en {@code ServiceProveedores}.
 */
public final class PresentacionServicio {

    /**
     * Resultado de revisar un servicio antes de publicarlo.
     *
     * @param bloqueo motivo por el que no se puede publicar, o null
     * @param aviso recomendación que no impide publicar, o null
     */
    public record Revision(String bloqueo, String aviso) {

        public boolean puedePublicar() {
            return bloqueo == null;
        }
    }

    private PresentacionServicio() {
    }

    /** Sin disponibilidad no se publica; sin imágenes solo se recomienda. */
    public static Revision revisar(int disponibilidades, int imagenes) {
        String bloqueo = disponibilidades == 0
                ? "Agrega al menos un horario de disponibilidad para publicar."
                : null;

        String aviso = imagenes == 0
                ? "Sin imágenes: los organizadores verán tu servicio sin fotos."
                : null;

        return new Revision(bloqueo, aviso);
    }

    public static String capacidad(Integer personas) {
        if (personas == null) {
            return "Sin límite";
        }
        return personas == 1 ? "1 persona" : personas + " personas";
    }

    public static String anticipacion(int dias) {
        return dias == 1 ? "1 día" : dias + " días";
    }

    public static String cantidad(int cantidad, String singular, String plural) {
        return cantidad == 1 ? "1 " + singular : cantidad + " " + plural;
    }
}

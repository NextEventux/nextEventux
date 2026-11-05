package views.solicitudes;

import entities.ContextoSolicitud;
import entities.MensajeSolicitud;
import views.FormatoUi;

/**
 * Textos y reglas de presentación de las solicitudes y propuestas (CU-12).
 * No usa JavaFX, así que se prueba sin abrir una ventana. Las reglas de
 * negocio siguen en el service.
 */
public final class PresentacionSolicitud {

    private PresentacionSolicitud() {
    }

    public static String textoEstado(String estado) {
        if (estado == null) {
            return FormatoUi.SIN_DATO;
        }

        return switch (estado.toUpperCase()) {
            case "PENDIENTE" -> "Pendiente";
            case "EN_NEGOCIACION" -> "En negociación";
            case "RECHAZADA" -> "Rechazada";
            case "PROPUESTA_RECHAZADA" -> "Propuesta rechazada";
            case "ENVIADA" -> "Enviada";
            case "SELECCIONADA" -> "Seleccionada para contratación";
            case "CONTRATADA" -> "Contratada";
            case "NO_SELECCIONADA" -> "No seleccionada";
            default -> estado.replace('_', ' ');
        };
    }

    /** Estilo de chip (chip-ok, chip-aviso, ...) que corresponde al estado. */
    public static String estiloEstado(String estado) {
        if (estado == null) {
            return "chip-neutro";
        }

        return switch (estado.toUpperCase()) {
            case "PENDIENTE" -> "chip-aviso";
            case "EN_NEGOCIACION", "ENVIADA" -> "chip-info";
            case "SELECCIONADA", "CONTRATADA" -> "chip-ok";
            case "RECHAZADA", "PROPUESTA_RECHAZADA" -> "chip-error";
            default -> "chip-neutro";
        };
    }

    /** Solo se rechaza una solicitud que el proveedor aún no respondió. */
    public static boolean puedeRechazar(String estadoSolicitud) {
        return "PENDIENTE".equalsIgnoreCase(estadoSolicitud);
    }

    /** Se puede enviar una propuesta mientras la negociación siga abierta. */
    public static boolean puedeEnviarPropuesta(String estadoSolicitud) {
        return "PENDIENTE".equalsIgnoreCase(estadoSolicitud)
                || "EN_NEGOCIACION".equalsIgnoreCase(estadoSolicitud);
    }

    public static String textoBotonPropuesta(String estadoSolicitud) {
        return "EN_NEGOCIACION".equalsIgnoreCase(estadoSolicitud)
                ? "Enviar nueva versión"
                : "Enviar propuesta";
    }

    /** Una propuesta contratada o descartada ya no admite cambios. */
    public static boolean propuestaCerrada(String estadoPropuesta) {
        return "CONTRATADA".equalsIgnoreCase(estadoPropuesta)
                || "NO_SELECCIONADA".equalsIgnoreCase(estadoPropuesta);
    }

    /** Solo una propuesta recién enviada puede elegirse para contratar. */
    public static boolean puedeSeleccionar(String estadoPropuesta) {
        return "ENVIADA".equalsIgnoreCase(estadoPropuesta);
    }

    /** Se contrata únicamente la propuesta que el organizador ya seleccionó. */
    public static boolean puedeContratar(String estadoPropuesta) {
        return "SELECCIONADA".equalsIgnoreCase(estadoPropuesta);
    }

    /** El ajuste se pide sobre una versión no seleccionada (CU-12, A2). */
    public static boolean puedeAjustar(String estadoPropuesta) {
        return "ENVIADA".equalsIgnoreCase(estadoPropuesta);
    }

    public static boolean puedeRechazarPropuesta(String estadoPropuesta) {
        return "ENVIADA".equalsIgnoreCase(estadoPropuesta)
                || "SELECCIONADA".equalsIgnoreCase(estadoPropuesta);
    }

    /** Explica por qué una propuesta cerrada ya no admite cambios. */
    public static String mensajeCerrada(String estadoPropuesta) {
        return "CONTRATADA".equalsIgnoreCase(estadoPropuesta)
                ? "Esta propuesta ya fue contratada y no admite cambios."
                : "Otra propuesta cubrió esta necesidad; esta quedó como "
                + "no seleccionada y no admite cambios.";
    }

    public static String nombreEvento(ContextoSolicitud contexto, int idEvento) {
        return vacio(contexto.nombreEvento())
                ? "Evento #" + idEvento
                : contexto.nombreEvento();
    }

    /** Nombre del evento o "—" cuando la propuesta no trae el dato. */
    public static String nombreEvento(ContextoSolicitud contexto) {
        return vacio(contexto.nombreEvento())
                ? FormatoUi.SIN_DATO
                : contexto.nombreEvento();
    }

    public static String nombreServicio(ContextoSolicitud contexto) {
        return vacio(contexto.nombreServicio())
                ? FormatoUi.SIN_DATO
                : contexto.nombreServicio();
    }

    public static String nombreProveedor(ContextoSolicitud contexto) {
        return vacio(contexto.nombreProveedor())
                ? FormatoUi.SIN_DATO
                : contexto.nombreProveedor();
    }

    public static String nombreServicio(ContextoSolicitud contexto, int idServicio) {
        return vacio(contexto.nombreServicio())
                ? "Servicio #" + idServicio
                : contexto.nombreServicio();
    }

    public static String fechaEvento(ContextoSolicitud contexto) {
        return FormatoUi.rango(contexto.inicioEvento(), contexto.finEvento());
    }

    public static String personas(ContextoSolicitud contexto) {
        Integer personas = contexto.personas();

        if (personas == null) {
            return FormatoUi.SIN_DATO;
        }

        return personas == 1 ? "1 persona" : personas + " personas";
    }

    /** "Tú" para los mensajes propios; para los demás, el nombre del otro. */
    public static String autorMensaje(
            MensajeSolicitud mensaje, int idUsuarioActual, String interlocutor) {

        if (mensaje.getIdUsuarioRemitente() == idUsuarioActual) {
            return "Tú";
        }
        return vacio(interlocutor) ? "Otro participante" : interlocutor;
    }

    /** Aviso para los mensajes que piden un ajuste de una versión concreta. */
    public static String textoAjuste(MensajeSolicitud mensaje) {
        if (!mensaje.esSolicitudDeAjuste()) {
            return null;
        }

        Integer version = mensaje.getVersionPropuesta();
        return version == null
                ? "Ajuste solicitado"
                : "Ajuste solicitado sobre la versión " + version;
    }

    private static boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }
}

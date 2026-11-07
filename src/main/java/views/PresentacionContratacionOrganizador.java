package views;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import entities.Contratacion;
import entities.Organizador;
import entities.ResenaOrganizador;

/**
 * Textos de la pantalla Contratar organizador (CU-026).
 *
 * No usa JavaFX para que pueda probarse sin abrir una ventana. Las reglas
 * de negocio siguen en el service; aquí solo se arma lo que ve el cliente.
 */
public final class PresentacionContratacionOrganizador {

    public static final String SIN_RESENAS = "Sin reseñas disponibles.";
    public static final String SIN_RESULTADOS =
            "No encontramos organizadores para esos criterios. "
            + "Prueba con otra ciudad, fecha, presupuesto o criterio adicional.";

    private PresentacionContratacionOrganizador() {
    }

    /** Valor del texto o el predeterminado cuando está vacío. */
    public static String texto(String valor, String predeterminado) {
        return valor == null || valor.isBlank() ? predeterminado : valor.trim();
    }

    /** «4,5 / 5 · 2 reseñas», o «Sin reseñas» si no tiene ninguna. */
    public static String calificacion(Organizador organizador) {

        if (organizador == null || organizador.totalResenas() <= 0) {
            return "Sin reseñas";
        }

        BigDecimal promedio = organizador.calificacionPromedio() == null
                ? BigDecimal.ZERO
                : organizador.calificacionPromedio();

        String valor = promedio.setScale(1, RoundingMode.HALF_UP)
                .toPlainString().replace('.', ',');

        return valor + " / 5 · " + organizador.totalResenas()
                + (organizador.totalResenas() == 1 ? " reseña" : " reseñas");
    }

    /** Texto de cada organizador en la lista de resultados. */
    public static String tarjeta(Organizador organizador) {

        return organizador.nombre()
                + "\n" + texto(organizador.empresa(), "Empresa no registrada")
                + " · " + texto(organizador.ciudad(), "Ciudad no registrada")
                + "\nEspecialidad: " + texto(organizador.especialidad(), "No registrada")
                + "\nTarifa base: " + FormatoUi.moneda(organizador.tarifaBase())
                + "\nCalificación: " + calificacion(organizador);
    }

    /** Una reseña del perfil: calificación, fecha y comentario. */
    public static String resena(ResenaOrganizador resena) {

        String encabezado = "★ " + resena.calificacion() + " / 5";

        if (resena.fecha() != null) {
            encabezado += " · " + FormatoUi.fecha(resena.fecha().toLocalDate());
        }

        return encabezado + "\n" + texto(resena.comentario(), "Sin comentario.");
    }

    /** Todas las reseñas del perfil, separadas por una línea en blanco. */
    public static String resenas(List<ResenaOrganizador> resenas) {

        if (resenas == null || resenas.isEmpty()) {
            return SIN_RESENAS;
        }

        StringBuilder texto = new StringBuilder();

        for (ResenaOrganizador resena : resenas) {
            if (texto.length() > 0) {
                texto.append("\n\n");
            }
            texto.append(resena(resena));
        }

        return texto.toString();
    }

    /** Resumen que el cliente revisa antes de confirmar (paso 4). */
    public static String resumen(
            Organizador organizador,
            String tipoEvento,
            String ciudad,
            LocalDate fecha,
            BigDecimal presupuesto,
            String criterios) {

        return "Organizador: " + organizador.nombre()
                + "\nEmpresa: " + texto(organizador.empresa(), "No registrada")
                + "\nTarifa base: " + FormatoUi.moneda(organizador.tarifaBase())
                + "\n\nTipo de evento: " + texto(tipoEvento, FormatoUi.SIN_DATO)
                + "\nCiudad: " + texto(ciudad, FormatoUi.SIN_DATO)
                + "\nFecha aproximada: " + FormatoUi.fecha(fecha)
                + "\nPresupuesto estimado: " + FormatoUi.moneda(presupuesto)
                + "\nCriterios adicionales: " + texto(criterios, "Ninguno")
                + "\n\nLa solicitud quedará en estado Pendiente y el organizador "
                + "recibirá una notificación.";
    }

    /** Mensaje que confirma el registro (paso 5). */
    public static String confirmacion(Contratacion solicitud, String nombreOrganizador) {

        return "Solicitud #" + solicitud.getIdContratacion()
                + " enviada a " + texto(nombreOrganizador, "el organizador")
                + " con estado " + estado(solicitud.getEstado())
                + ". Se notificó al organizador y puedes consultar su estado "
                + "en «Mis solicitudes».";
    }

    /**
     * Rol de la sesión bajo el nombre del menú lateral («CLIENTE» se
     * muestra «Cliente»), con la misma regla que MenuCompartido.
     */
    public static String rol(entities.SesionUsuario sesion) {

        if (sesion == null || sesion.getTipoUsuario() == null
                || sesion.getTipoUsuario().isBlank()) {
            return "Usuario";
        }

        String rol = sesion.getTipoUsuario().trim().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(rol.charAt(0)) + rol.substring(1);
    }

    /** «PENDIENTE» se muestra como «Pendiente». */
    public static String estado(String estado) {

        if (estado == null || estado.isBlank()) {
            return FormatoUi.SIN_DATO;
        }

        String limpio = estado.trim().replace('_', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(limpio.charAt(0)) + limpio.substring(1);
    }

    // ------------------------------------------------------------------
    // Ampliación: vista del organizador (solicitudes recibidas)
    // ------------------------------------------------------------------

    /** Filtros de la lista de solicitudes recibidas. */
    public static final String FILTRO_PENDIENTES = "Pendientes";
    public static final String FILTRO_RESPONDIDAS = "Respondidas";
    public static final String FILTRO_TODAS = "Todas";

    /** Solo una solicitud pendiente puede aceptarse o rechazarse. */
    public static boolean puedeResponder(Contratacion solicitud) {
        return solicitud != null
                && "PENDIENTE".equalsIgnoreCase(solicitud.getEstado());
    }

    /** Indica si la solicitud entra en el filtro elegido. */
    public static boolean coincideFiltro(Contratacion solicitud, String filtro) {

        if (FILTRO_PENDIENTES.equals(filtro)) {
            return puedeResponder(solicitud);
        }

        if (FILTRO_RESPONDIDAS.equals(filtro)) {
            return !puedeResponder(solicitud);
        }

        return true;
    }

    /** Estilo del chip según el estado (chip-aviso, chip-ok, chip-error). */
    public static String estiloEstado(String estado) {

        if (estado == null) {
            return "chip-neutro";
        }

        return switch (estado.trim().toUpperCase(Locale.ROOT)) {
            case "PENDIENTE" -> "chip-aviso";
            case "ACEPTADA" -> "chip-ok";
            case "RECHAZADA" -> "chip-error";
            default -> "chip-neutro";
        };
    }

    /** Texto de cada solicitud en la lista del organizador. */
    public static String solicitudRecibida(Contratacion solicitud) {

        return "Solicitud #" + solicitud.getIdContratacion()
                + " · " + estado(solicitud.getEstado())
                + "\nCliente: " + texto(solicitud.getNombreCliente(), FormatoUi.SIN_DATO)
                + "\n" + texto(solicitud.getTipoEvento(), FormatoUi.SIN_DATO)
                + " · " + texto(solicitud.getCiudad(), FormatoUi.SIN_DATO)
                + " · " + FormatoUi.fecha(solicitud.getFechaAproximada());
    }

    /** Pregunta antes de aceptar o rechazar. */
    public static String preguntaRespuesta(Contratacion solicitud, boolean aceptar) {

        return (aceptar ? "¿Aceptar" : "¿Rechazar")
                + " la solicitud #" + solicitud.getIdContratacion()
                + " de " + texto(solicitud.getNombreCliente(), "este cliente")
                + "? La decisión no se puede cambiar y el cliente recibirá una notificación.";
    }

    /** Mensaje después de registrar la decisión. */
    public static String confirmacionRespuesta(Contratacion solicitud) {

        return "Solicitud #" + solicitud.getIdContratacion()
                + " " + estado(solicitud.getEstado()).toLowerCase(Locale.ROOT)
                + ". Se notificó a " + texto(solicitud.getNombreCliente(), "el cliente") + ".";
    }

    /** Texto de cada solicitud en «Mis solicitudes». */
    public static String solicitud(Contratacion solicitud) {

        return "Solicitud #" + solicitud.getIdContratacion()
                + " · " + estado(solicitud.getEstado())
                + "\nOrganizador: " + texto(
                        solicitud.getNombreOrganizador(),
                        "Organizador #" + solicitud.getIdOrganizador())
                + "\nEvento: " + texto(solicitud.getTipoEvento(), FormatoUi.SIN_DATO)
                + " · Ciudad: " + texto(solicitud.getCiudad(), FormatoUi.SIN_DATO)
                + "\nFecha aproximada: " + FormatoUi.fecha(solicitud.getFechaAproximada())
                + " · Presupuesto: " + FormatoUi.moneda(solicitud.getPresupuestoEstimado())
                + (solicitud.getFechaSolicitud() == null
                        ? ""
                        : "\nEnviada: " + FormatoUi.fechaHora(solicitud.getFechaSolicitud()));
    }
}

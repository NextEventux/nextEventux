package entities.solicitudes;

import java.time.LocalDateTime;

public class MensajeSolicitud {

    private int idMensaje;
    private int idSolicitud;
    private int idUsuarioRemitente;
    private String contenido;
    private String adjuntoUrl;
    private LocalDateTime fechaHora;
    private Integer idPropuesta;
    private Integer versionPropuesta;

    /** Propuesta sobre la que se pide un ajuste; null si es un mensaje normal. */
    public Integer getIdPropuesta() {
        return idPropuesta;
    }

    public void setIdPropuesta(Integer idPropuesta) {
        this.idPropuesta = idPropuesta;
    }

    public Integer getVersionPropuesta() {
        return versionPropuesta;
    }

    public void setVersionPropuesta(Integer versionPropuesta) {
        this.versionPropuesta = versionPropuesta;
    }

    public boolean esSolicitudDeAjuste() {
        return idPropuesta != null;
    }

    public int getIdMensaje() {
        return idMensaje;
    }

    public void setIdMensaje(int idMensaje) {
        this.idMensaje = idMensaje;
    }

    public int getIdSolicitud() {
        return idSolicitud;
    }

    public void setIdSolicitud(int idSolicitud) {
        this.idSolicitud = idSolicitud;
    }

    public int getIdUsuarioRemitente() {
        return idUsuarioRemitente;
    }

    public void setIdUsuarioRemitente(int idUsuarioRemitente) {
        this.idUsuarioRemitente = idUsuarioRemitente;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public String getAdjuntoUrl() {
        return adjuntoUrl;
    }

    public void setAdjuntoUrl(String adjuntoUrl) {
        this.adjuntoUrl = adjuntoUrl;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }
}
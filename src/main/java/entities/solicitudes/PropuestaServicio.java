package entities.solicitudes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PropuestaServicio {

    private int idPropuesta;
    private int idSolicitud;
    private int numeroVersion;
    private BigDecimal valor;
    private String descripcion;
    private String condiciones;
    private String materialApoyo;
    private String estado;
    private boolean vigente;
    private LocalDateTime fechaEnvio;
    private ContextoSolicitud contexto = ContextoSolicitud.vacio();

    public String getMaterialApoyo() {
        return materialApoyo;
    }

    public void setMaterialApoyo(String materialApoyo) {
        this.materialApoyo = materialApoyo;
    }

    public ContextoSolicitud getContexto() {
        return contexto;
    }

    public void setContexto(ContextoSolicitud contexto) {
        this.contexto = contexto == null
                ? ContextoSolicitud.vacio()
                : contexto;
    }

    public int getIdPropuesta() {
        return idPropuesta;
    }

    public void setIdPropuesta(int idPropuesta) {
        this.idPropuesta = idPropuesta;
    }

    public int getIdSolicitud() {
        return idSolicitud;
    }

    public void setIdSolicitud(int idSolicitud) {
        this.idSolicitud = idSolicitud;
    }

    public int getNumeroVersion() {
        return numeroVersion;
    }

    public void setNumeroVersion(int numeroVersion) {
        this.numeroVersion = numeroVersion;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getCondiciones() {
        return condiciones;
    }

    public void setCondiciones(String condiciones) {
        this.condiciones = condiciones;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public boolean isVigente() {
        return vigente;
    }

    public void setVigente(boolean vigente) {
        this.vigente = vigente;
    }

    public LocalDateTime getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(LocalDateTime fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }
}
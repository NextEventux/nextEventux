package entities;

import java.math.BigDecimal;
import java.time.LocalDate;

//Campaña de marketing dirigida a un segmento de clientes (cu-23).
public class Campana {
    public static final String ESTADO_PROGRAMADA = "PROGRAMADA";
    public static final String ESTADO_ACTIVA = "ACTIVA";
    public static final String ESTADO_PAUSADA = "PAUSADA";
    public static final String ESTADO_FINALIZADA = "FINALIZADA";

    private Integer idCampana;
    private final int idUsuario;
    private final int idSegmento;
    private final String nombre;
    private final String tipoOferta;
    private final LocalDate fechaInicio;
    private final LocalDate fechaFin;
    private final BigDecimal presupuestoMaximo;
    private final String estado;
    private final long visualizaciones;
    private final long conversiones;
    private final String nombreSegmento;

    //Campaña nueva: queda programada y sin métricas.
    public Campana(int idUsuario, int idSegmento, String nombre, String tipoOferta,
                   LocalDate fechaInicio, LocalDate fechaFin, BigDecimal presupuestoMaximo) {
        this(null, idUsuario, idSegmento, nombre, tipoOferta, fechaInicio, fechaFin,
                presupuestoMaximo, ESTADO_PROGRAMADA, 0L, 0L, null);
    }

    //Campaña que ya existe en la BD.
    public Campana(Integer idCampana, int idUsuario, int idSegmento, String nombre, String tipoOferta,
                   LocalDate fechaInicio, LocalDate fechaFin, BigDecimal presupuestoMaximo,
                   String estado, long visualizaciones, long conversiones, String nombreSegmento) {
        this.idCampana = idCampana;
        this.idUsuario = idUsuario;
        this.idSegmento = idSegmento;
        this.nombre = nombre;
        this.tipoOferta = tipoOferta;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.presupuestoMaximo = presupuestoMaximo;
        this.estado = estado;
        this.visualizaciones = visualizaciones;
        this.conversiones = conversiones;
        this.nombreSegmento = nombreSegmento;
    }

    public Integer getIdCampana() { return idCampana; }

    public void setIdCampana(Integer idCampana) { this.idCampana = idCampana; }

    public int getIdUsuario() { return idUsuario; }

    public int getIdSegmento() { return idSegmento; }

    public String getNombre() { return nombre; }

    public String getTipoOferta() { return tipoOferta; }

    public LocalDate getFechaInicio() { return fechaInicio; }

    public LocalDate getFechaFin() { return fechaFin; }

    public BigDecimal getPresupuestoMaximo() { return presupuestoMaximo; }

    public String getEstado() { return estado; }

    //Alcance: veces que se visualizó la oferta.
    public long getVisualizaciones() { return visualizaciones; }

    //Conversión: servicios contratados a partir de la oferta.
    public long getConversiones() { return conversiones; }

    //Nombre del segmento; solo viene lleno cuando la campaña se lee de la BD.
    public String getNombreSegmento() { return nombreSegmento; }
}

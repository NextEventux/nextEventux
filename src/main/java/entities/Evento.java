package entities;

public class Evento {

    private String nombre;
    private String tipo;
    private String fecha;
    private String lugar;
    private double presupuesto;
    private int invitadosIniciales;
    private String estado;

    public Evento(
            String nombre,
            String tipo,
            String fecha,
            String lugar,
            double presupuesto,
            int invitadosIniciales) {

        this.nombre = nombre;
        this.tipo = tipo;
        this.fecha = fecha;
        this.lugar = lugar;
        this.presupuesto = presupuesto;
        this.invitadosIniciales = invitadosIniciales;
        this.estado = "En planeación";
    }

    public String getNombre() {
        return nombre;
    }

    public String getTipo() {
        return tipo;
    }

    public String getFecha() {
        return fecha;
    }

    public String getLugar() {
        return lugar;
    }

    public double getPresupuesto() {
        return presupuesto;
    }

    public int getInvitadosIniciales() {
        return invitadosIniciales;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    @Override
    public String toString() {
        return "Evento{" +
                "nombre='" + nombre + '\'' +
                ", tipo='" + tipo + '\'' +
                ", fecha='" + fecha + '\'' +
                ", lugar='" + lugar + '\'' +
                ", presupuesto=" + presupuesto +
                ", invitadosIniciales=" + invitadosIniciales +
                ", estado='" + estado + '\'' +
                '}';
    }
}
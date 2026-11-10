package entities;

//Datos del cliente para segmentar
public class ClienteMarketing {
    private final int idUsuario;
    private final String nombre;
    private final String pais;
    private final String ciudad;
    private final boolean aceptaMarketing;

    public ClienteMarketing(
            int idUsuario, String nombre, String pais, String ciudad, boolean aceptaMarketing) {
        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.pais = pais;
        this.ciudad = ciudad;
        this.aceptaMarketing = aceptaMarketing;
    }

    public int getIdUsuario() { return idUsuario; }
    public String getNombre() { return nombre; }
    public String getPais() { return pais; }
    public String getCiudad() { return ciudad; }
    public boolean isAceptaMarketing() { return aceptaMarketing; }
}
package entities;

public class Servicio {
    
    // Atributos (coinciden con las columnas de tu base de datos)
    private int idServicio;
    private int idProveedor;
    private String nombre;
    private String descripcion;
    private double precio;
    private String categoria;
    private String estado;

    // Constructor vacío (siempre es buena práctica tener uno por defecto)
    public Servicio() {
    }

    // Constructor con todos los atributos
    public Servicio(int idServicio, int idProveedor, String nombre, String descripcion, double precio, String categoria, String estado) {
        this.idServicio = idServicio;
        this.idProveedor = idProveedor;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.categoria = categoria;
        this.estado = estado;
    }

    // --- Getters y Setters ---

    public int getIdServicio() {
        return idServicio;
    }

    public void setIdServicio(int idServicio) {
        this.idServicio = idServicio;
    }

    public int getIdProveedor() {
        return idProveedor;
    }

    public void setIdProveedor(int idProveedor) {
        this.idProveedor = idProveedor;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
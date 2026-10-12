package entities;

// Representa uno de los servicios que un proveedor puede ofrecer en la plataforma
public class Servicio {

    // Identificadores para saber qué servicio es y a qué proveedor pertenece
    private int idServicio;
    private int idProveedor;

    // Información principal que se muestra del servicio
    private String nombre;
    private String descripcion;
    private double precio;
    private String categoria;

    // Sirve para saber si el servicio está activo, retirado, etc.
    private String estado;

    // Permite consultar el identificador del servicio
    public int getIdServicio() {
        return idServicio;
    }

    // Permite asignar el identificador del servicio
    public void setIdServicio(int idServicio) {
        this.idServicio = idServicio;
    }

    // Permite saber a qué proveedor pertenece el servicio
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
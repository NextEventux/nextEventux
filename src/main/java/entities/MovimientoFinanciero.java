package main.java.entities;

import java.time.LocalDate;

public class MovimientoFinanciero {
    
    // Atributos basados en la tabla Movimiento_financiero del diagrama
    private int idMovimiento;
    private int idEvento;
    private int idContratacion;
    private double valor;
    private LocalDate fecha;
    private String descripcion;
    private String tipo;
    private String estado;

    // Constructor vacío
    public MovimientoFinanciero() {
    }

    // Constructor con todos los atributos
    public MovimientoFinanciero(int idMovimiento, int idEvento, int idContratacion, double valor, 
                                LocalDate fecha, String descripcion, String tipo, String estado) {
        this.idMovimiento = idMovimiento;
        this.idEvento = idEvento;
        this.idContratacion = idContratacion;
        this.valor = valor;
        this.fecha = fecha;
        this.descripcion = descripcion;
        this.tipo = tipo;
        this.estado = estado;
    }

    // Getters y Setters
    public int getIdMovimiento() {
        return idMovimiento;
    }

    public void setIdMovimiento(int idMovimiento) {
        this.idMovimiento = idMovimiento;
    }

    public int getIdEvento() {
        return idEvento;
    }

    public void setIdEvento(int idEvento) {
        this.idEvento = idEvento;
    }

    public int getIdContratacion() {
        return idContratacion;
    }

    public void setIdContratacion(int idContratacion) {
        this.idContratacion = idContratacion;
    }

    public double getValor() {
        return valor;
    }

    public void setValor(double valor) {
        this.valor = valor;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
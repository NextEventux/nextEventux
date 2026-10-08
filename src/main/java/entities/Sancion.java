package main.java.entities;

import java.time.LocalDate;

public class Sancion {
    
    private int idSancion;
    private int idEvento;
    private String motivo;
    private double valor;
    private String estado;
    private LocalDate fecha;

    // Constructor vacío 
    public Sancion() {
    }

    // Constructor con todos los atributos
    public Sancion(int idSancion, int idEvento, String motivo, double valor, String estado, LocalDate fecha) {
        this.idSancion = idSancion;
        this.idEvento = idEvento;
        this.motivo = motivo;
        this.valor = valor;
        this.estado = estado;
        this.fecha = fecha;
    }

    // Getters y Setters
    public int getIdSancion() {
        return idSancion;
    }

    public void setIdSancion(int idSancion) {
        this.idSancion = idSancion;
    }

    public int getIdEvento() {
        return idEvento;
    }

    public void setIdEvento(int idEvento) {
        this.idEvento = idEvento;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public double getValor() {
        return valor;
    }

    public void setValor(double valor) {
        this.valor = valor;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }
}
package main.java.views;

import java.util.Map;
import main.java.controllers.ControllerFinanciera;

public class ViewFinanciera {
    
    private ControllerFinanciera controller;

    public ViewFinanciera(ControllerFinanciera controller) {
        this.controller = controller;
    }

    public void mostrarEstadoFinanciero(int idEvento) {
        System.out.println("=============================================");
        System.out.println("   ESTADO FINANCIERO DEL EVENTO ID: " + idEvento);
        System.out.println("=============================================");
        
        Map<String, Object> estado = controller.consultarEstadoFinanciero(idEvento);
        
        // 1. Imprimir detalles de la Planificación
        System.out.println("--- DATOS DE PLANIFICACIÓN ---");
        System.out.println("Descripción : " + estado.get("Planificacion_Descripcion"));
        
        if (estado.containsKey("Planificacion_Version")) {
            System.out.println("Versión     : " + estado.get("Planificacion_Version"));
            System.out.println("Estado      : " + estado.get("Planificacion_Estado"));
        }
        
        // 2. Imprimir el balance económico
        System.out.println("\n--- RESUMEN DE MOVIMIENTOS ---");
        System.out.println("Total Ingresos : $" + estado.get("Ingresos"));
        System.out.println("Total Egresos  : $" + estado.get("Egresos"));
        System.out.println("---------------------------------------------");
        System.out.println("BALANCE FINAL  : $" + estado.get("Balance"));
        System.out.println("=============================================");
    }
}
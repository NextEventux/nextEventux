package main.java.repositories;

import java.util.ArrayList;
import java.util.List;
import main.java.entities.MovimientoFinanciero;

public class RepositoryMovimientoFinanciero {
    
    // Lista simulando la base de datos
    private List<MovimientoFinanciero> baseDeDatosSimulada = new ArrayList<>();

    public List<MovimientoFinanciero> listarMovimientosPorEvento(int idEvento) {
        List<MovimientoFinanciero> filtrados = new ArrayList<>();
        for (MovimientoFinanciero mov : baseDeDatosSimulada) {
            if (mov.getIdEvento() == idEvento) {
                filtrados.add(mov);
            }
        }
        return filtrados;
    }

    public MovimientoFinanciero buscarMovimiento(int idMovimiento) {
        return null; // A implementar luego
    }
}
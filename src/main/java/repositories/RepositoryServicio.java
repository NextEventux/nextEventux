package repositories;

import entities.Servicio;
import java.util.List;
import java.util.ArrayList;

public class RepositoryServicio {
    
    public void guardarServicio(Servicio servicio) {
    }

    public Servicio buscarServicio(int idServicio) {
        return null;
    }

    public void actualizarServicio(Servicio servicio) {
    }

    public void eliminarServicio(int idServicio) {
    }

    // --- ¡ESTE ES EL MÉTODO QUE FALTABA! ---
    public List<Servicio> listarServiciosPorEvento(int idEvento) {
        // Por ahora devuelve una lista vacía para que no dé errores, 
        // luego aquí irá tu consulta SQL a la base de datos.
        return new ArrayList<>();
    }
}
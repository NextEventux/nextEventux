package repositories;

import java.util.ArrayList;
import java.util.List;

import entities.Servicio;

// Se encarga del acceso a los servicios.
// Por ahora usamos una lista para poder probar el flujo antes de conectar H2.
public class RepositoryServicio {

    // Guarda temporalmente los servicios mientras validamos la primera implementación
    // Es una colección donde podemos tener varios objetos Servicio
    private List<Servicio> servicios;

    public RepositoryServicio() {
        //crea esa lista cuando se crea el repositorio
        servicios = new ArrayList<>();
    }

    // Guarda un servicio para que después pueda ser consultado
    public void guardarServicio(Servicio servicio) {
        servicios.add(servicio);
    }

    // Busca un servicio usando su identificador
    public Servicio buscarServicio(int idServicio) {

        //for de niño grande, Java va recorriendo uno por uno los servicios que hay guardados
        for (Servicio servicio : servicios) {
            if (servicio.getIdServicio() == idServicio) {
                return servicio;
            }
        }

        // Si no existe un servicio con ese id, no tenemos nada que devolver
        return null;
    }
}
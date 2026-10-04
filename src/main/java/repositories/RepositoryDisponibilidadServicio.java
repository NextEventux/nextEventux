package repositories;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import entities.DisponibilidadServicio;

// Guarda temporalmente las disponibilidades mientras validamos el diseño
public class RepositoryDisponibilidadServicio {

    private List<DisponibilidadServicio> disponibilidades;

    public RepositoryDisponibilidadServicio() {
        disponibilidades = new ArrayList<>();
    }

    // Guarda una disponibilidad asociada a un servicio
    public void guardarDisponibilidad(DisponibilidadServicio disponibilidad) {
        disponibilidades.add(disponibilidad);
    }

    // Busca una disponibilidad usando el servicio, la fecha y el horario
    public DisponibilidadServicio buscarDisponibilidad(
            int idServicio,
            Date fecha,
            String horario) {

        for (DisponibilidadServicio disponibilidad : disponibilidades) {

            if (disponibilidad.getIdServicio() == idServicio
                    && disponibilidad.getFecha().equals(fecha)
                    && disponibilidad.getHorario().equals(horario)) {

                return disponibilidad;
            }
        }

        return null;
    }

    // Devuelve las disponibilidades registradas para un servicio
    public List<DisponibilidadServicio> listarDisponibilidadesServicio(int idServicio) {

        List<DisponibilidadServicio> resultado = new ArrayList<>();

        for (DisponibilidadServicio disponibilidad : disponibilidades) {

            if (disponibilidad.getIdServicio() == idServicio) {
                resultado.add(disponibilidad);
            }
        }

        return resultado;
    }
}
package repositories;

import entities.Evento;
import java.util.ArrayList;
import java.util.List;

public class RepositoryEvento {

    private final List<Evento> eventos = new ArrayList<>();

    public void guardarEvento(Evento evento) {
        eventos.add(evento);
    }

    public List<Evento> listarEventos() {
        return new ArrayList<>(eventos);
    }
}
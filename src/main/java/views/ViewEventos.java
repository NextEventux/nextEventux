package views;

import controllers.ControllerEventos;
import entities.Evento;

public class ViewEventos {

    private final ControllerEventos controller;

    public ViewEventos(ControllerEventos controller) {
        this.controller = controller;
    }

    public void mostrarFormularioEvento(Evento datos) {
        controller.crearEvento(datos);
        mostrarMensaje("Evento creado correctamente.");
    }

    public void mostrarMensaje(String mensaje) {
        System.out.println(mensaje);
    }
}
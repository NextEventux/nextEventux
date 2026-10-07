import controllers.ControllerEventos;
import entities.Evento;
import repositories.RepositoryEvento;
import services.ServiceEventos;
import views.ViewEventos;

public class PruebaCU01 {

    public static void main(String[] args) {

        RepositoryEvento repositoryEvento = new RepositoryEvento();

        ServiceEventos serviceEventos =
                new ServiceEventos(repositoryEvento);

        ControllerEventos controllerEventos =
                new ControllerEventos(serviceEventos);

        ViewEventos viewEventos =
                new ViewEventos(controllerEventos);

        Evento evento = new Evento(
                "Festival NextEventux",
                "Corporativo",
                "2026-10-20",
                "Bogotá",
                15000000,
                100
        );

        viewEventos.mostrarFormularioEvento(evento);

        System.out.println(evento);
        System.out.println(
                "Eventos guardados: "
                + repositoryEvento.listarEventos().size()
        );
    }
}
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
                1, // Organizador de ejemplo; JDBC deberá comprobar que exista.
                "Festival NextEventux",
                "Corporativo",
                "Festival corporativo",
                LocalDateTime.of(2026, 10, 20, 9, 0),
                LocalDateTime.of(2026, 10, 20, 18, 0),
                "Bogotá",
                new BigDecimal("15000000.00"),
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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import controllers.ControllerEventos;
import entities.Evento;
import repositories.RepositoryEvento;
import services.ServiceEventos;

public class PruebaCU01 {

    public static void main(String[] args) throws Exception {

        String url = "jdbc:h2:" + java.nio.file.Files.createTempDirectory("cu01-ejemplo-").resolve("eventos");
        BaseDatosPrueba.preparar(url);
        RepositoryEvento repositoryEvento = new RepositoryEvento(url);

        ServiceEventos serviceEventos =
                new ServiceEventos(repositoryEvento);

        ControllerEventos controllerEventos =
                new ControllerEventos(serviceEventos);

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

        controllerEventos.crearEvento(evento);
        if (repositoryEvento.listarEventos().size() != 1) {
            throw new AssertionError("No se guardó el evento.");
        }

        System.out.println(evento);
        System.out.println(
                "Eventos guardados: "
                + repositoryEvento.listarEventos().size()
        );
    }
}
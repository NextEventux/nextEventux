import entities.Evento;
import repositories.RepositoryEvento;
import services.ServiceEventos;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PruebaValidacionesEvento {
    public static void main(String[] args) {
        RepositoryEvento repository = new RepositoryEvento();
        ServiceEventos service = new ServiceEventos(repository);
        LocalDateTime inicio = LocalDateTime.of(2026, 10, 20, 9, 0);
        LocalDateTime fin = inicio.plusHours(2);
        Evento valido = evento(1, inicio, fin, BigDecimal.ZERO, 0);
        service.crearEvento(valido);
        if (repository.listarEventos().size() != 1
                || !Evento.ESTADO_INICIAL.equals(valido.getEstado())
                || valido.getIdEvento() != null || valido.getFechaCreacion() == null) {
            throw new AssertionError("Datos iniciales incorrectos.");
        }
        rechazar(service, null);
        rechazar(service, evento(0, inicio, fin, BigDecimal.ZERO, 0));
        rechazar(service, evento(1, null, fin, BigDecimal.ZERO, 0));
        rechazar(service, evento(1, inicio, null, BigDecimal.ZERO, 0));
        rechazar(service, evento(1, inicio, inicio.minusHours(1), BigDecimal.ZERO, 0));
        rechazar(service, evento(1, inicio, fin, null, 0));
        rechazar(service, evento(1, inicio, fin, new BigDecimal("-1"), 0));
        rechazar(service, evento(1, inicio, fin, new BigDecimal("0.001"), 0));
        rechazar(service, evento(1, inicio, fin, new BigDecimal("10000000000"), 0));
        rechazar(service, evento(1, inicio, fin, BigDecimal.ZERO, -1));
        if (repository.listarEventos().size() != 1) {
            throw new AssertionError("Se guardaron eventos inválidos.");
        }
        System.out.println("Validaciones de CU01 correctas.");
    }

    private static Evento evento(int organizador, LocalDateTime inicio, LocalDateTime fin,
                                 BigDecimal presupuesto, int invitados) {
        return new Evento(organizador, "Evento", "Corporativo", null, inicio, fin,
                "Bogotá", presupuesto, invitados);
    }

    private static void rechazar(ServiceEventos service, Evento evento) {
        try {
            service.crearEvento(evento);
        } catch (IllegalArgumentException esperado) {
            return;
        }
        throw new AssertionError("Se aceptó un evento inválido.");
    }
}

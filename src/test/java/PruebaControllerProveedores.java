import java.util.Date;
import controllers.ControllerProveedores;
import entities.DisponibilidadServicio;
import entities.Servicio;
import repositories.RepositoryServicio;
import services.ServiceProveedores;
import repositories.RepositoryDisponibilidadServicio;

// Comprueba el flujo desde el controller hasta el repositorio
public class PruebaControllerProveedores {

    public static void main(String[] args) {

        // Creamos las capas que necesita el flujo
        RepositoryServicio repository = new RepositoryServicio();
        RepositoryDisponibilidadServicio repositoryDisponibilidad = new RepositoryDisponibilidadServicio();
        ServiceProveedores service = new ServiceProveedores(repository, repositoryDisponibilidad);
        ControllerProveedores controller = new ControllerProveedores(service);

        Servicio servicio = new Servicio();
        servicio.setIdServicio(3);
        servicio.setIdProveedor(10);
        servicio.setNombre("Catering para eventos");
        servicio.setDescripcion("Servicio de alimentacion para el evento");
        servicio.setPrecio(1200000);
        servicio.setCategoria("Alimentacion");
        servicio.setEstado("Activo");
        servicio.setTipo("Catering");
        servicio.setCantidadMaximaPersonas(150);
        servicio.setCiudadZona("Bogota");
        servicio.setAnticipacionMinima(7);

        // El servicio entra por el controller y sigue por las demas capas
        controller.crearServicio(servicio);

        // Revisamos al final que haya llegado al repositorio
        Servicio servicioEncontrado = repository.buscarServicio(3);

        if (servicioEncontrado != null) {
            System.out.println("El controller completo el flujo correctamente");
            System.out.println("Nombre: " + servicioEncontrado.getNombre());
        } else {
            System.out.println("El servicio no completo el flujo");
        }

        Date fecha = new Date();

        // Registramos una disponibilidad entrando por el controller
        controller.registrarDisponibilidadServicio( 3, fecha,"14:00 - 18:00");

        // Comprobamos que haya llegado al repositorio
        DisponibilidadServicio disponibilidadEncontrada = repositoryDisponibilidad.buscarDisponibilidad(3, fecha, "14:00 - 18:00");

        if (disponibilidadEncontrada != null) {
            System.out.println("El controller registro la disponibilidad correctamente");
            System.out.println("Horario: " + disponibilidadEncontrada.getHorario());
        } else {
            System.out.println("La disponibilidad no completo el flujo");
        }
    }
}
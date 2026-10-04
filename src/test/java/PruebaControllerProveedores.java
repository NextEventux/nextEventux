import controllers.ControllerProveedores;
import entities.Servicio;
import repositories.RepositoryServicio;
import services.ServiceProveedores;

// Comprueba el flujo desde el controller hasta el repositorio
public class PruebaControllerProveedores {

    public static void main(String[] args) {

        // Creamos las capas que necesita el flujo
        RepositoryServicio repository = new RepositoryServicio();
        ServiceProveedores service = new ServiceProveedores(repository);
        ControllerProveedores controller = new ControllerProveedores(service);

        Servicio servicio = new Servicio();
        servicio.setIdServicio(3);
        servicio.setIdProveedor(10);
        servicio.setNombre("Catering para eventos");
        servicio.setDescripcion("Servicio de alimentacion para el evento");
        servicio.setPrecio(1200000);
        servicio.setCategoria("Alimentacion");
        servicio.setEstado("Activo");

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
    }
}
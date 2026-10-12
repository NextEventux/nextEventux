import entities.Servicio;
import repositories.RepositoryServicio;
import services.ServiceProveedores;
import repositories.RepositoryDisponibilidadServicio;

// Comprueba que el service pueda enviar un servicio al repositorio
public class PruebaServiceProveedores {

    public static void main(String[] args) {

        // Primero creamos el repositorio que va a guardar los servicios
        RepositoryServicio repository = new RepositoryServicio();
        RepositoryDisponibilidadServicio repositoryDisponibilidad =
        new RepositoryDisponibilidadServicio();

        ServiceProveedores service = new ServiceProveedores(repository, repositoryDisponibilidad);

        Servicio servicio = new Servicio();
        servicio.setIdServicio(2);
        servicio.setIdProveedor(10);
        servicio.setNombre("Decoracion para eventos");
        servicio.setDescripcion("Decoracion del lugar del evento");
        servicio.setPrecio(800000);
        servicio.setCategoria("Decoracion");
        servicio.setEstado("Activo");

        // En vez de guardar directamente desde la prueba, pasamos por el service
        service.crearServicio(servicio);

        // Buscamos en el repositorio para comprobar que el service si lo envio
        Servicio servicioEncontrado = repository.buscarServicio(2);

        if (servicioEncontrado != null) {
            System.out.println("El service creo el servicio correctamente");
            System.out.println("Nombre: " + servicioEncontrado.getNombre());
        } else {
            System.out.println("El servicio no fue guardado");
        }
    }
}
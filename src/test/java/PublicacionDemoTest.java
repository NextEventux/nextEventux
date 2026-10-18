import config.CargarDemoPublicacion;
import config.DatabaseConnection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repositories.RepositoryEvento;
import repositories.RepositoryPublicacion;
import services.ServicePublicacion;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import static org.junit.jupiter.api.Assertions.*;

class PublicacionDemoTest {
    @TempDir Path directorio;
    @Test void preparaElFlujoCompletoSoloEnBaseVacia() throws Exception {
        String url="jdbc:h2:"+directorio.resolve("demo");
        try(Connection connection=DatabaseConnection.getConnection(url)) {
            CargarDemoPublicacion.cargar(connection);
            assertThrows(SQLException.class,()->CargarDemoPublicacion.cargar(connection));
        }
        var service=new ServicePublicacion(new RepositoryPublicacion(url),new RepositoryEvento(url));
        var finalizado=service.listarEventos(1).stream().filter(e->"FINALIZADO".equals(e.getEstado())).findFirst().orElseThrow();
        service.solicitar(1,finalizado.getIdEvento());
        var solicitud=service.listarSolicitudes(1).get(0);
        service.revisar(solicitud.id(),service.listarAdministradores().get(0).id(),true,null);
        assertEquals(1,service.listarPlantillas().size());
        assertEquals(1,service.notificacionesOrganizador(1).size());
    }
}

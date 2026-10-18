import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import config.CargarDatosEjemplo;
import config.DatabaseConnection;
import entities.SesionUsuario;
import repositories.RepositoryAutenticacion;
import services.ServiceAutenticacion;

public class DatosDemostracionH2Test {

    private static final String TEST_URL =
            "jdbc:h2:mem:datos_demostracion;DB_CLOSE_DELAY=-1";

    @Test
    void usuariosDeDemostracionDebenPoderIngresar() throws Exception {

        System.setProperty("nexteventux.db.url", TEST_URL);

        try {
            try (Connection connection =
                         DatabaseConnection.getConnection(TEST_URL)) {

                assertTrue(
                        connection.getMetaData()
                                .getURL()
                                .startsWith("jdbc:h2:mem:datos_demostracion"));

                CargarDatosEjemplo.cargar(connection);
            }

            ServiceAutenticacion service =
                    new ServiceAutenticacion(
                            new RepositoryAutenticacion());

            comprobarUsuario(service, "valentina", "ORGANIZADOR");
            comprobarUsuario(service, "santiago", "ORGANIZADOR");
            comprobarUsuario(service, "juliana", "ORGANIZADOR");

            comprobarUsuario(service, "luna", "PROVEEDOR");
            comprobarUsuario(service, "sabor", "PROVEEDOR");
            comprobarUsuario(service, "flash", "PROVEEDOR");

            comprobarUsuario(service, "camila", "ADMINISTRADOR");
            comprobarUsuario(service, "mateo", "ADMINISTRADOR");
            comprobarUsuario(service, "shirly", "ADMINISTRADOR");

            try (Connection connection =
                         DatabaseConnection.getConnection(TEST_URL);
                 Statement statement =
                         connection.createStatement();
                 ResultSet result =
                         statement.executeQuery("""
                                 SELECT COUNT(*)
                                 FROM Invitado
                                 WHERE id_invitado IN (5, 6, 7)
                                 """)) {

                assertTrue(result.next());
                assertEquals(3, result.getInt(1));
            }

        } finally {
            System.clearProperty("nexteventux.db.url");
        }
    }

    private void comprobarUsuario(
            ServiceAutenticacion service,
            String usuario,
            String tipoEsperado) {

        SesionUsuario sesion =
                service.iniciarSesion(usuario, "1234");

        assertNotNull(sesion);
        assertEquals(tipoEsperado, sesion.getTipoUsuario());
    }
}
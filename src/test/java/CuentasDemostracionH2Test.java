
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import config.CargarDatosEjemplo;
import config.DatabaseConnection;

/** Datos de demostración: tres cuentas por actor y una sola clave sencilla. */
public class CuentasDemostracionH2Test {

    private String url;

    @BeforeEach
    void cargarDatos() throws Exception {
        url = "jdbc:h2:mem:cuentas_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        System.setProperty("nexteventux.db.url", url);

        try (Connection connection = DatabaseConnection.getConnection(url)) {
            CargarDatosEjemplo.cargar(connection);
        }
    }

    @AfterEach
    void limpiar() throws Exception {
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
                Statement statement = connection.createStatement()) {
            statement.execute("SHUTDOWN");
        } finally {
            System.clearProperty("nexteventux.db.url");
        }
    }

    @Test
    void cadaActorTieneTresCuentas() throws Exception {
        for (String rol : List.of(
                "ORGANIZADOR", "PROVEEDOR", "CLIENTE", "ADMINISTRADOR")) {

            assertEquals(3, contar("SELECT COUNT(*) FROM Usuario "
                    + "WHERE tipo_usuario = '" + rol + "'"), rol);
        }
    }

    @Test
    void todasLasCuentasUsanLaClave1234() throws Exception {
        assertEquals(0, contar("SELECT COUNT(*) FROM Usuario "
                + "WHERE contraseña <> '1234'"));
    }

    private int contar(String sql) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection(url);
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(sql)) {

            assertTrue(result.next());
            return result.getInt(1);
        }
    }
}

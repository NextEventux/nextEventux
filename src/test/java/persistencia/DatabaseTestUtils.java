package persistencia;

import inicializacion.CargarDatosEjemplo;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class DatabaseTestUtils {

    private DatabaseTestUtils() {
    }

    public static void crearEsquemaVacio(Connection connection) throws Exception {
        CargarDatosEjemplo.cargar(connection);

        List<String> tablas = new ArrayList<>();
        try (ResultSet result = connection.getMetaData().getTables(
                null, "PUBLIC", "%", new String[]{"TABLE"})) {
            while (result.next()) {
                tablas.add(result.getString("TABLE_NAME"));
            }
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute("SET REFERENTIAL_INTEGRITY FALSE");
            try {
                for (String tabla : tablas) {
                    statement.execute("TRUNCATE TABLE PUBLIC.\"" + tabla.replace("\"", "\"\"")
                            + "\" RESTART IDENTITY");
                }
            } finally {
                statement.execute("SET REFERENTIAL_INTEGRITY TRUE");
            }
        }
    }
}

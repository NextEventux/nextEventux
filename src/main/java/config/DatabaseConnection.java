package config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.h2.tools.RunScript;

public class DatabaseConnection
{
    private static final String URL = "jdbc:h2:./data/nexteventux";
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    public static Connection getConnection() throws SQLException
    {
        Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
        try {
            inicializarEsquema(connection);
            return connection;
        } catch (SQLException | RuntimeException e) {
            try {
                connection.close();
            } catch (SQLException cierre) {
                e.addSuppressed(cierre);
            }
            throw e;
        }
    }

    public static void inicializarEsquema(Connection connection) throws SQLException {
        InputStream schema = DatabaseConnection.class.getResourceAsStream("/schema.sql");
        if (schema == null) {
            throw new SQLException("No se encontró schema.sql en los recursos del proyecto.");
        }
        try (InputStreamReader reader = new InputStreamReader(schema, StandardCharsets.UTF_8)) {
            RunScript.execute(connection, reader);
        } catch (java.io.IOException e) {
            throw new SQLException("No se pudo leer el esquema de la base de datos.", e);
        }
    }
}

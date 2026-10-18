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
        return getConnection(System.getProperty("nexteventux.db.url", URL));
    }

    public static Connection getConnection(String url) throws SQLException {
        Connection connection = DriverManager.getConnection(url, USER, PASSWORD);
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
        ejecutarScript(connection, "/schema.sql");
        ejecutarScript(connection, "/migrations/001_evento_cu01.sql");
        ejecutarScript(connection, "/migrations/002_proveedores_cu11.sql");
        ejecutarScript(connection, "/migrations/003_proveedores_cu11_publicacion.sql");
        actualizarContadorEventos(connection);
    }

    // Los ejemplos antiguos usaban IDs explícitos sin avanzar la secuencia H2.
    private static void actualizarContadorEventos(Connection connection) throws SQLException {
        try (java.sql.Statement statement = connection.createStatement();
             java.sql.ResultSet result = statement.executeQuery(
                     "SELECT IDENTITY_BASE, (SELECT COALESCE(MAX(id_evento), 0) FROM Evento) "
                     + "FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = 'PUBLIC' "
                     + "AND TABLE_NAME = 'EVENTO' AND COLUMN_NAME = 'ID_EVENTO'")) {
            if (result.next()) {
                long siguiente = result.getLong(1);
                long maximo = result.getLong(2);
                if (siguiente <= maximo) {
                    try (java.sql.Statement alter = connection.createStatement()) {
                        alter.execute("ALTER TABLE Evento ALTER COLUMN id_evento RESTART WITH " + (maximo + 1));
                    }
                }
            }
        }
    }

    private static void ejecutarScript(Connection connection, String recurso) throws SQLException {
        InputStream schema = DatabaseConnection.class.getResourceAsStream(recurso);
        if (schema == null) {
            throw new SQLException("No se encontró el recurso " + recurso);
        }
        try (InputStreamReader reader = new InputStreamReader(schema, StandardCharsets.UTF_8)) {
            RunScript.execute(connection, reader);
        } catch (java.io.IOException e) {
            throw new SQLException("No se pudo leer el esquema de la base de datos.", e);
        }
    }
}

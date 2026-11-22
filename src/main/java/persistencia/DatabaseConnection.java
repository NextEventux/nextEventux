package persistencia;

import entities.eventos.Evento;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.h2.tools.RunScript;

public class DatabaseConnection {

    private static final String USER = "sa";
    private static final String PASSWORD = "";

    /**
     * Ruta de la base cuando no se indica nexteventux.db.url. Es absoluta para que
     * no dependa del directorio desde el que se lance la aplicación: una ruta
     * relativa creaba otra base vacía al ejecutar desde otra carpeta.
     * La carpeta se puede cambiar con -Dnexteventux.home=...
     */
    static String urlPorDefecto() {
        String carpeta = System.getProperty("nexteventux.home");
        Path base = carpeta == null || carpeta.isBlank()
                ? Path.of(System.getProperty("user.home"), ".nexteventux")
                : Path.of(carpeta);
        String archivo = base.toAbsolutePath().resolve("nexteventux").toString().replace('\\', '/');
        // La base permanece abierta durante la ejecución para evitar reabrir el archivo.
        // AUTO_SERVER permite que otra herramienta (consola H2, DBeaver) consulte la
        // misma base mientras la aplicación está abierta.
        return "jdbc:h2:" + archivo + ";DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE";
    }

    public static Connection getConnection() throws SQLException {
        return getConnection(
                System.getProperty("nexteventux.db.url", urlPorDefecto()), true);
    }

    public static Connection getConnection(String url) throws SQLException {
        return getConnection(url, true);
    }

    public static Connection getConnectionParaSincronizarDemo() throws SQLException {
        return getConnection(
                System.getProperty("nexteventux.db.url", urlPorDefecto()), false);
    }

    public static Connection getConnectionParaSincronizarDemo(String url) throws SQLException {
        return getConnection(url, false);
    }

    private static Connection getConnection(String url, boolean repararContadorEventos)
            throws SQLException {
        Connection connection = DriverManager.getConnection(url, USER, PASSWORD);
        try {
            inicializarSiHaceFalta(url, connection, repararContadorEventos);
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

    // Se evita repetir el esquema en cada consulta a una base ya inicializada.
    private static final java.util.Set<String> INICIALIZADAS =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    private static synchronized void inicializarSiHaceFalta(
            String url,
            Connection connection,
            boolean repararContadorEventos) throws SQLException {
        // La tabla centinela detecta bases recreadas con la misma URL (por ejemplo, en memoria).
        if (INICIALIZADAS.contains(url) && existeTabla(connection, "ESQUEMA_INICIALIZADO")) {
            if (repararContadorEventos) {
                actualizarContadorEventos(connection);
            }
            return;
        }
        inicializarEsquema(connection, repararContadorEventos);
        try (java.sql.Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS Esquema_inicializado (id INT PRIMARY KEY)");
        }
        INICIALIZADAS.add(url);
    }

    public static synchronized void inicializarEsquema(Connection connection) throws SQLException {
        inicializarEsquema(connection, true);
    }

    private static synchronized void inicializarEsquema(
            Connection connection,
            boolean repararContadorEventos) throws SQLException {
        ejecutarScript(connection, "/schema.sql");
        if (repararContadorEventos) {
            actualizarContadorEventos(connection);
        }
    }

    private static boolean existeTabla(Connection connection, String tabla) throws SQLException {
        try (java.sql.PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = ?")) {
            statement.setString(1, tabla);
            try (java.sql.ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    // Los ejemplos antiguos usaban IDs explícitos sin avanzar la secuencia H2.
    private static void actualizarContadorEventos(Connection connection) throws SQLException {
        try (java.sql.Statement statement = connection.createStatement(); java.sql.ResultSet result = statement.executeQuery(
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

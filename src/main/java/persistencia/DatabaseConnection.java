package persistencia;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

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
        return getConnection(System.getProperty("nexteventux.db.url", urlPorDefecto()));
    }

    public static Connection getConnection(String url) throws SQLException {
        Connection connection = DriverManager.getConnection(url, USER, PASSWORD);
        try {
            inicializarSiHaceFalta(url, connection);
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

    private static final Set<String> INICIALIZADAS = new HashSet<>();

    private static synchronized void inicializarSiHaceFalta(
            String url,
            Connection connection) throws SQLException {
        if (INICIALIZADAS.contains(url)) {
            return;
        }
        ejecutarScript(connection, "/schema.sql");
        INICIALIZADAS.add(url);
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

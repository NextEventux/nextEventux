package persistencia;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

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
        return DriverManager.getConnection(url, USER, PASSWORD);
    }
}

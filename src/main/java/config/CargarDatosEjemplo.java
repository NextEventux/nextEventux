package config;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.h2.tools.RunScript;

/** Carga explícita de ejemplos; nunca se ejecuta al abrir la aplicación. */
public class CargarDatosEjemplo {
    public static void main(String[] args) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection()) {
            cargar(connection);
        }
        System.out.println("Datos de ejemplo cargados. Organizadores disponibles: 1 y 2.");
    }

    /**
     * Carga los ejemplos solo si la base no tiene ningún registro de negocio.
     * Se usa al abrir la aplicación para que una base recién creada (por ejemplo
     * en un computador nuevo o tras cambiar de carpeta) tenga usuarios con los
     * que iniciar sesión. Una base con datos nunca se modifica.
     *
     * @return true si se cargaron los ejemplos
     */
    public static boolean cargarSiEstaVacia(Connection connection) throws Exception {
        if (!estaVacia(connection)) {
            return false;
        }
        cargar(connection);
        return true;
    }

    /** Indica si todas las tablas de negocio están sin registros. */
    public static boolean estaVacia(Connection connection) throws SQLException {
        try (Statement tablas = connection.createStatement();
             ResultSet result = tablas.executeQuery(
                     "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES "
                     + "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_TYPE = 'BASE TABLE' "
                     + "AND TABLE_NAME NOT IN ('ESQUEMA_VERSION','ESQUEMA_INICIALIZADO')")) {
            while (result.next()) {
                String tabla = result.getString(1).replace("\"", "\"\"");
                try (Statement statement = connection.createStatement();
                     ResultSet registros = statement.executeQuery("SELECT COUNT(*) FROM \"" + tabla + "\"")) {
                    registros.next();
                    if (registros.getLong(1) != 0) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static void cargar(Connection connection) throws Exception {
        if (!estaVacia(connection)) {
            throw new SQLException("Los ejemplos requieren una base vacía. No se modificaron los registros existentes.");
        }
        try (InputStreamReader reader = new InputStreamReader(
                CargarDatosEjemplo.class.getResourceAsStream("/seed.sql"), StandardCharsets.UTF_8)) {
            RunScript.execute(connection, reader);
        }
    }
}

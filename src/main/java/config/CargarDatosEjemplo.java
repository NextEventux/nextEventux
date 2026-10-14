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

    public static void cargar(Connection connection) throws Exception {
        try (Statement tablas = connection.createStatement();
             ResultSet result = tablas.executeQuery(
                     "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES "
                     + "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_TYPE = 'BASE TABLE'")) {
            while (result.next()) {
                String tabla = result.getString(1).replace("\"", "\"\"");
                try (Statement statement = connection.createStatement();
                     ResultSet registros = statement.executeQuery("SELECT COUNT(*) FROM \"" + tabla + "\"")) {
                    registros.next();
                    if (registros.getLong(1) != 0) {
                        throw new SQLException("Los ejemplos requieren una base vacía. No se modificaron los registros existentes.");
                    }
                }
            }
        }
        try (InputStreamReader reader = new InputStreamReader(
                CargarDatosEjemplo.class.getResourceAsStream("/seed.sql"), StandardCharsets.UTF_8)) {
            RunScript.execute(connection, reader);
        }
    }
}

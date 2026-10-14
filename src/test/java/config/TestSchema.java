package config;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.h2.tools.RunScript;

/** Validación ejecutable sin dependencias adicionales de pruebas. */
public class TestSchema {
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("nexteventux-schema-");
        String url = "jdbc:h2:" + directory.resolve("prueba");
        try {
            try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
                DatabaseConnection.inicializarEsquema(connection);
                comprobarEventos(connection, 0);
                try (InputStreamReader reader = new InputStreamReader(
                        TestSchema.class.getResourceAsStream("/seed.sql"), StandardCharsets.UTF_8)) {
                    RunScript.execute(connection, reader);
                }
                comprobarEventos(connection, 2);
                DatabaseConnection.inicializarEsquema(connection);
                comprobarEventos(connection, 2);
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("UPDATE Evento SET nombre = 'Evento modificado' WHERE id_evento = 1");
                }
            }
            try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
                DatabaseConnection.inicializarEsquema(connection);
                comprobarEventos(connection, 2);
                try (Statement statement = connection.createStatement();
                     ResultSet result = statement.executeQuery("SELECT nombre FROM Evento WHERE id_evento = 1")) {
                    if (!result.next() || !"Evento modificado".equals(result.getString(1))) {
                        throw new AssertionError("La reinicialización no conservó los datos.");
                    }
                }
            }
            System.out.println("Esquema creado correctamente. Reinicialización y persistencia verificadas.");
        } finally {
            Files.deleteIfExists(directory.resolve("prueba.mv.db"));
            Files.deleteIfExists(directory.resolve("prueba.trace.db"));
            Files.deleteIfExists(directory);
        }
    }

    private static void comprobarEventos(Connection connection, int esperado) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM Evento")) {
            result.next();
            if (result.getInt(1) != esperado) {
                throw new AssertionError("Cantidad inesperada de eventos.");
            }
        }
    }
}

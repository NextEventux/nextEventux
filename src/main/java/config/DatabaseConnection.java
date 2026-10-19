package config;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

import org.h2.tools.RunScript;

public class DatabaseConnection {

    private static final String URL = "jdbc:h2:./data/nexteventux";
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    // Cuenta de administrador exclusiva para las pruebas del equipo.
    private static final String ADMIN_NOMBRE = "Administrador CU-030";
    private static final String ADMIN_USUARIO = "admin.cu030";
    private static final String ADMIN_CLAVE = "AdminCU0302026";

    public static Connection getConnection() throws SQLException {
        return getConnection(
                System.getProperty("nexteventux.db.url", URL));
    }

    public static Connection getConnection(String url) throws SQLException {

        Connection connection = DriverManager.getConnection(
                url, USER, PASSWORD);

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

    public static void inicializarEsquema(
            Connection connection) throws SQLException {

        ejecutarScript(connection, "/schema.sql");
        ejecutarScript(connection, "/migrations/001_evento_cu01.sql");
        ejecutarScript(connection, "/migrations/002_proveedores_cu11.sql");
        ejecutarScript(connection, "/migrations/003_proveedores_cu11_publicacion.sql");
        ejecutarScript(connection, "/migrations/002_publicacion_eventos.sql");
        ejecutarScript(connection, "/migrations/003_cancelacion_eventos.sql");

        /*
         * Ejecuta CU-030 si el esquema todavía no está instalado.
         * Si sus tablas ya existen, evita volver a ejecutar toda
         * la migración en cada apertura de la conexión.
         */
        if (!esquemaCu30Instalado(connection)) {
            ejecutarScript(
                    connection,
                    "/migrations/004_cu30_verificar_categorizar_organizador.sql");
        }

        // Garantiza una cuenta de pruebas para el equipo.
        asegurarAdministradorPruebas(connection);

        actualizarContadorEventos(connection);
    }

    private static boolean esquemaCu30Instalado(
            Connection connection) throws SQLException {

        return existeTabla(
                    connection,
                    "CONFIGURACION_EVALUACION_ORGANIZADOR")
                && existeTabla(
                    connection,
                    "DOCUMENTO_ORGANIZADOR")
                && existeTabla(
                    connection,
                    "EVALUACION_ORGANIZADOR")
                && existeTabla(
                    connection,
                    "MENSAJE_ORGANIZADOR")
                && existeTabla(
                    connection,
                    "BITACORA_ORGANIZADOR")
                && existeColumna(
                    connection,
                    "ORGANIZADOR",
                    "ESTADO_VERIFICACION")
                && existeColumna(
                    connection,
                    "USUARIO",
                    "DOCUMENTO_IDENTIDAD");
    }

    private static boolean existeTabla(
            Connection connection,
            String tabla) throws SQLException {

        String sql =
                "SELECT 1 FROM INFORMATION_SCHEMA.TABLES "
                + "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = ?";

        try (PreparedStatement statement =
                    connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    tabla.toUpperCase(Locale.ROOT));

            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private static boolean existeColumna(
            Connection connection,
            String tabla,
            String columna) throws SQLException {

        String sql =
                "SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE TABLE_SCHEMA = 'PUBLIC' "
                + "AND TABLE_NAME = ? AND COLUMN_NAME = ?";

        try (PreparedStatement statement =
                    connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    tabla.toUpperCase(Locale.ROOT));

            statement.setString(
                    2,
                    columna.toUpperCase(Locale.ROOT));

            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private static void asegurarAdministradorPruebas(
            Connection connection) throws SQLException {

        String actualizar =
                "UPDATE Usuario "
                + "SET nombre = ?, "
                + "contraseña = ?, "
                + "estado = 'ACTIVO', "
                + "acepta_marketing = FALSE, "
                + "tipo_usuario = 'ADMINISTRADOR' "
                + "WHERE usuario = ?";

        try (PreparedStatement statement =
                    connection.prepareStatement(actualizar)) {

            statement.setString(1, ADMIN_NOMBRE);
            statement.setString(2, ADMIN_CLAVE);
            statement.setString(3, ADMIN_USUARIO);

            int filasActualizadas = statement.executeUpdate();

            if (filasActualizadas == 0) {

                String insertar =
                        "INSERT INTO Usuario "
                        + "(nombre, usuario, contraseña, estado, "
                        + "fecha_registro, acepta_marketing, tipo_usuario) "
                        + "VALUES (?, ?, ?, 'ACTIVO', CURRENT_TIMESTAMP, "
                        + "FALSE, 'ADMINISTRADOR')";

                try (PreparedStatement alta =
                            connection.prepareStatement(insertar)) {

                    alta.setString(1, ADMIN_NOMBRE);
                    alta.setString(2, ADMIN_USUARIO);
                    alta.setString(3, ADMIN_CLAVE);

                    alta.executeUpdate();
                }
            }
        }
    }

    private static void actualizarContadorEventos(
            Connection connection) throws SQLException {

        String sql =
                "SELECT IDENTITY_BASE, "
                + "(SELECT COALESCE(MAX(id_evento), 0) FROM Evento) "
                + "FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE TABLE_SCHEMA = 'PUBLIC' "
                + "AND TABLE_NAME = 'EVENTO' "
                + "AND COLUMN_NAME = 'ID_EVENTO'";

        try (java.sql.Statement statement =
                    connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {

            if (result.next()) {

                long siguiente = result.getLong(1);
                long maximo = result.getLong(2);

                if (siguiente <= maximo) {

                    try (java.sql.Statement alter =
                                connection.createStatement()) {

                        alter.execute(
                                "ALTER TABLE Evento ALTER COLUMN "
                                + "id_evento RESTART WITH "
                                + (maximo + 1));
                    }
                }
            }
        }
    }

    private static void ejecutarScript(
            Connection connection,
            String recurso) throws SQLException {

        InputStream schema =
                DatabaseConnection.class.getResourceAsStream(recurso);

        if (schema == null) {
            throw new SQLException(
                    "No se encontró el recurso " + recurso);
        }

        try (InputStreamReader reader =
                    new InputStreamReader(
                            schema,
                            StandardCharsets.UTF_8)) {

            RunScript.execute(connection, reader);

        } catch (java.io.IOException e) {

            throw new SQLException(
                    "No se pudo leer el esquema de la base de datos.",
                    e);
        }
    }
}
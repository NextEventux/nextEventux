package inicializacion;

import persistencia.DatabaseConnection;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import org.h2.tools.RunScript;

/** Carga data.sql al abrir una base nueva o al ejecutarse explícitamente. */
public class CargarDatosEjemplo {

    private static final Set<String> TABLAS_ESPERADAS = Set.of(
            "PERMISO", "ROL", "USUARIO", "ROL_PERMISO", "USUARIO_ROL", "LUGAR",
            "ORGANIZADOR", "PROVEEDOR", "EVENTO", "PLANIFICACION", "INVITACION",
            "INVITADO", "ACOMPANANTE", "NECESIDAD_ESPECIAL", "REGALO", "RESERVA_REGALO",
            "SERVICIO", "PAQUETE_SERVICIO", "SOLICITUD_EJECUCION", "PROPUESTA",
            "CONTRATACION", "MOVIMIENTO_FINANCIERO", "RESENA", "CONVERSACION", "MENSAJE",
            "RESPUESTA_RESENA", "SANCION", "SEGMENTACION", "CAMPANA", "ENVIO_CAMPANA",
            "PUNTOS_FIDELIDAD", "DISPONIBILIDAD_SERVICIO", "SOLICITUD_PUBLICACION",
            "PLANTILLA_EVENTO", "IMAGEN_SERVICIO", "PREGUNTA_SERVICIO",
            "NECESIDAD_SERVICIO_EVENTO", "NOTIFICACION_INTERNA", "FINALIZACION_EVENTO",
            "FOTOGRAFIA_EVENTO", "ENTREGA_GALERIA",
            "SOLICITUD_CONTRATACION_ORGANIZADOR", "RESENA_ORGANIZADOR",
            "BUSQUEDA_EVENTO", "SEGMENTO_CLIENTE", "PRESUPUESTO_MARKETING");

    public static void main(String[] args) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection()) {
            cargar(connection);
        }
        System.out.println("Datos de ejemplo cargados. Organizadores disponibles: 1 y 2.");
    }

    /**
     * Carga el esquema y los ejemplos solo si la base aún no tiene tablas.
     * Una base existente nunca se reconstruye durante el arranque normal.
     *
     * @return true si se cargaron los ejemplos
     */
    public static boolean cargarSiEstaVacia(Connection connection) throws Exception {
        Set<String> existentes = tablasPublicas(connection);
        if (existentes.isEmpty()) {
            ejecutarDataSql(connection);
            return true;
        }

        validarEsquemaCompleto(existentes);
        return false;
    }

    /** Indica si PUBLIC aún no tiene tablas base. */
    public static boolean estaVacia(Connection connection) throws SQLException {
        return tablasPublicas(connection).isEmpty();
    }

    private static Set<String> tablasPublicas(Connection connection) throws SQLException {
        Set<String> tablas = new HashSet<>();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES "
                     + "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_TYPE = 'BASE TABLE'")) {
            while (result.next()) {
                tablas.add(result.getString("TABLE_NAME").toUpperCase(Locale.ROOT));
            }
        }
        return tablas;
    }

    public static void cargar(Connection connection) throws Exception {
        Set<String> existentes = tablasPublicas(connection);
        if (!existentes.isEmpty()) {
            validarEsquemaCompleto(existentes);
            throw new SQLException("La carga inicial requiere una base sin tablas. "
                    + "El esquema existe; no se ejecutó data.sql ni se modificaron los datos. "
                    + "Para reconstruir una base de desarrollo, ejecute data.sql explícitamente.");
        }
        ejecutarDataSql(connection);
    }

    private static void validarEsquemaCompleto(Set<String> existentes) throws SQLException {
        Set<String> faltantes = new TreeSet<>(TABLAS_ESPERADAS);
        faltantes.removeAll(existentes);
        if (!faltantes.isEmpty()) {
            throw new SQLException("El esquema H2 está incompleto. Faltan tablas en PUBLIC: "
                    + String.join(", ", faltantes)
                    + ". No se ejecutó data.sql y no se modificaron los datos.");
        }
    }

    private static void ejecutarDataSql(Connection connection) throws SQLException {
        InputStream script = CargarDatosEjemplo.class.getResourceAsStream("/data.sql");
        if (script == null) {
            throw new SQLException("No se encontró el recurso /data.sql");
        }
        try (InputStreamReader reader = new InputStreamReader(script, StandardCharsets.UTF_8)) {
            RunScript.execute(connection, reader);
        } catch (IOException e) {
            throw new SQLException("No se pudo leer el recurso /data.sql", e);
        }
    }
}

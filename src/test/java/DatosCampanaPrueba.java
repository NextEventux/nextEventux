import entities.Campana;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import persistencia.DatabaseConnection;

// Datos de prueba del CU-22: administrador, clientes, segmentos y presupuesto de marketing.
final class DatosCampanaPrueba {
    static final int ADMINISTRADOR = 1;
    static final int CLIENTE_CON_CONSENTIMIENTO = 2;
    static final int OTRO_CLIENTE_CON_CONSENTIMIENTO = 3;
    static final int CLIENTE_SIN_CONSENTIMIENTO = 4;
    static final int SEGMENTO_ACTIVO = 1;
    static final int SEGMENTO_INACTIVO = 2;
    static final String PRESUPUESTO_INICIAL = "1000000.00";
    static final LocalDate INICIO = LocalDate.of(2026, 12, 1);
    static final LocalDate FIN = LocalDate.of(2026, 12, 31);

    private DatosCampanaPrueba() {
    }

    // Base en memoria con tres clientes en el segmento activo, un segmento inactivo y 1.000.000 disponibles.
    static String crearBase() throws Exception {
        String url = "jdbc:h2:mem:marketing_cu22_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        try (Connection connection = DatabaseConnection.getConnection(url)) {
            insertarUsuario(connection, ADMINISTRADOR, "Administradora", "ADMINISTRADOR", true);
            insertarUsuario(connection, CLIENTE_CON_CONSENTIMIENTO, "Cliente 2", "CLIENTE", true);
            insertarUsuario(connection, OTRO_CLIENTE_CON_CONSENTIMIENTO, "Cliente 3", "CLIENTE", true);
            insertarUsuario(connection, CLIENTE_SIN_CONSENTIMIENTO, "Cliente 4", "CLIENTE", false);
            insertarSegmento(connection, "Bogotá - Boda", estadoActivo());
            insertarSegmento(connection, "Cali - Conferencia", "INACTIVA");
            for (int cliente : new int[]{2, 3, 4}) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO Segmento_cliente (id_segmento, id_usuario) VALUES (?, ?)")) {
                    statement.setInt(1, SEGMENTO_ACTIVO);
                    statement.setInt(2, cliente);
                    statement.executeUpdate();
                }
            }
            fijarPresupuesto(connection, PRESUPUESTO_INICIAL);
        }
        return url;
    }

    private static String estadoActivo() {
        return entities.Segmentacion.ESTADO_ACTIVA;
    }

    static void fijarPresupuesto(String url, String monto) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection(url)) {
            fijarPresupuesto(connection, monto);
        }
    }

    private static void fijarPresupuesto(Connection connection, String monto) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "MERGE INTO Presupuesto_marketing (id_presupuesto, disponible) KEY (id_presupuesto) VALUES (1, ?)")) {
            statement.setBigDecimal(1, new BigDecimal(monto));
            statement.executeUpdate();
        }
    }

    static Campana campana(int idSegmento, String presupuesto, LocalDate inicio, LocalDate fin) {
        return new Campana(ADMINISTRADOR, idSegmento, "Bodas de fin de año", "Descuento del 10 %",
                inicio, fin, presupuesto == null ? null : new BigDecimal(presupuesto));
    }

    static Campana campanaValida() {
        return campana(SEGMENTO_ACTIVO, "300000", INICIO, FIN);
    }

    static int contarEnvios(String url, int idCampana) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection(url);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM Envio_Campana WHERE id_campana = ?")) {
            statement.setInt(1, idCampana);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    private static void insertarUsuario(Connection connection, int id, String nombre, String tipo,
            boolean aceptaMarketing) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO Usuario (id_usuario, nombre, usuario, contraseña, estado, "
                        + "fecha_registro, acepta_marketing, tipo_usuario) "
                        + "VALUES (?, ?, ?, 'x', 'ACTIVO', CURRENT_TIMESTAMP, ?, ?)")) {
            statement.setInt(1, id);
            statement.setString(2, nombre);
            statement.setString(3, "campana_" + id);
            statement.setBoolean(4, aceptaMarketing);
            statement.setString(5, tipo);
            statement.executeUpdate();
        }
    }

    private static void insertarSegmento(Connection connection, String nombre, String estado) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO Segmentacion (id_usuario, nombre, criterios, descripcion, estados) "
                        + "VALUES (?, ?, 'AUTO|pais=Colombia', 'Segmento de prueba', ?)")) {
            statement.setInt(1, ADMINISTRADOR);
            statement.setString(2, nombre);
            statement.setString(3, estado);
            statement.executeUpdate();
        }
    }

    // Reloj cuya fecha se cambia a mano para simular el paso de los días.
    static class RelojManual extends Clock {
        private LocalDate fecha;

        RelojManual(LocalDate fecha) {
            this.fecha = fecha;
        }

        void setFecha(LocalDate fecha) {
            this.fecha = fecha;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return fecha.atStartOfDay(ZoneOffset.UTC).toInstant();
        }
    }
}

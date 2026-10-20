import config.DatabaseConnection;
import entities.ClienteMarketing;
import entities.Segmentacion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import repositories.RepositoryMarketing;
import services.ServiceMarketing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketingIntegracionAppTest {

    @Test
    void segmentaClientesUsandoSolicitudesRealesDeContratacionYCiudadSolicitada() throws Exception {
        String url = "jdbc:h2:mem:marketing_app_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        try (Connection connection = DatabaseConnection.getConnection(url)) {
            insertarUsuario(connection, 1, "Administrador", "ADMINISTRADOR", null, null);
            for (int id = 2; id <= 6; id++) {
                insertarUsuario(connection, id, "Cliente " + id, "CLIENTE",
                        null, LocalDate.of(2000, 1, 1));
            }
            insertarUsuario(connection, 7, "Organizador", "ORGANIZADOR", null, null);

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(
                        "INSERT INTO Organizador(id_usuario, estado) VALUES (7, 'ACTIVO')");
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO Solicitud_Contratacion_Organizador "
                    + "(id_cliente, id_organizador, tipo_evento, ciudad, fecha_aproximada, "
                    + "presupuesto_estimado, estado) VALUES (?, 1, 'Boda', 'Bogota', "
                    + "DATE '2026-12-01', 1000000, 'PENDIENTE')")) {
                for (int id = 2; id <= 6; id++) {
                    statement.setInt(1, id);
                    statement.addBatch();
                }
                statement.executeBatch();
            }
        }

        RepositoryMarketing repository = new RepositoryMarketing(url);
        List<ClienteMarketing> clientes = repository.listarClientesActivos();
        assertEquals(5, clientes.size());
        assertTrue(clientes.stream().allMatch(cliente -> "Bogota".equals(cliente.getCiudad())));
        assertTrue(repository.contarSolicitudesPorTipo().stream()
                .allMatch(solicitud -> "Boda".equals(solicitud.getTipo())));

        List<Segmentacion> segmentos = new ServiceMarketing(repository)
                .segmentarClientes(1, LocalDate.of(2026, 10, 20));
        assertEquals(1, segmentos.size());
        assertEquals(5, segmentos.get(0).getCantidadClientes());
        assertTrue(segmentos.get(0).getNombre().contains("Bogota"));
    }

    private void insertarUsuario(
            Connection connection,
            int id,
            String nombre,
            String tipo,
            String ciudad,
            LocalDate fechaNacimiento) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO Usuario (id_usuario, nombre, usuario, contraseña, estado, "
                + "fecha_registro, acepta_marketing, tipo_usuario, pais, ciudad, fecha_nacimiento) "
                + "VALUES (?, ?, ?, 'x', 'ACTIVO', CURRENT_TIMESTAMP, TRUE, ?, NULL, ?, ?)")) {
            statement.setInt(1, id);
            statement.setString(2, nombre);
            statement.setString(3, "marketing_" + id);
            statement.setString(4, tipo);
            statement.setString(5, ciudad);
            if (fechaNacimiento == null) {
                statement.setNull(6, java.sql.Types.DATE);
            } else {
                statement.setObject(6, fechaNacimiento);
            }
            statement.executeUpdate();
        }
    }
}

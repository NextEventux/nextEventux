import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import config.DatabaseConnection;
import entities.ClienteMarketing;
import entities.Segmentacion;
import entities.SolicitudEvento;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import repositories.RepositoryMarketing;
import services.ServiceMarketing;

class MarketingCU21Test {
    private static final LocalDate HOY = LocalDate.of(2026, 10, 20);

    @Test
    void segmentaYGuardaClientesEnLaBase() throws Exception {
        String url = crearBaseConCincoClientes();
        agregarBusqueda(url, 2, "Boda");
        agregarBusqueda(url, 3, "Boda");
        agregarBusqueda(url, 4, "Boda");
        agregarBusqueda(url, 5, "Boda");
        agregarBusqueda(url, 6, "Boda");

        ServiceMarketing servicio = new ServiceMarketing(new RepositoryMarketing(url));
        List<Segmentacion> segmentos = servicio.segmentarClientes(1, HOY);

        assertEquals(1, segmentos.size());
        assertEquals("Bogotá 26-35 - Boda", segmentos.get(0).getNombre());
        assertEquals(5, contarMiembrosActivos(url));
        assertThrows(IllegalArgumentException.class, () -> servicio.segmentarClientes(9999, HOY));
    }

    @Test
    void repositorioListaClientesYAgrupaBusquedas() throws Exception {
        String url = crearBaseConCincoClientes();
        agregarBusqueda(url, 2, "Social");
        agregarBusqueda(url, 2, "Social");
        agregarBusqueda(url, 3, "Boda");

        RepositoryMarketing repositorio = new RepositoryMarketing(url);
        assertEquals(5, repositorio.listarClientesActivos().size());
        List<SolicitudEvento> solicitudes = repositorio.contarSolicitudesPorTipo();
        assertEquals(2, solicitudes.size());
        assertTrue(solicitudes.stream().anyMatch(
                solicitud -> solicitud.getIdCliente() == 2
                        && solicitud.getTipo().equals("Social")
                        && solicitud.getCantidad() == 2));
        assertTrue(solicitudes.stream().anyMatch(
                solicitud -> solicitud.getIdCliente() == 3
                        && solicitud.getTipo().equals("Boda")
                        && solicitud.getCantidad() == 1));
    }

    @Test
    void aplicaReglasDeAgrupacionYMinimoDeClientes() {
        List<ClienteMarketing> clientes = new ArrayList<>();
        List<SolicitudEvento> solicitudes = new ArrayList<>();
        for (int id = 2; id <= 6; id++) {
            String ciudad = id == 6 ? " Bogotá " : "Bogotá";
            clientes.add(new ClienteMarketing(
                    id, "Cliente " + id, "Colombia", ciudad, LocalDate.of(2000, 1, 1), true));
            solicitudes.add(new SolicitudEvento(id, "Social", 1));
        }
        for (int id = 7; id <= 10; id++) {
            clientes.add(new ClienteMarketing(
                    id, "Cliente " + id, "Colombia", "Cali", LocalDate.of(2000, 1, 1), true));
            solicitudes.add(new SolicitudEvento(id, "Boda", 1));
        }
        clientes.add(new ClienteMarketing(
                11, "Sin ciudad", "Colombia", null, LocalDate.of(2000, 1, 1), true));

        List<entities.SegmentoCalculado> segmentos = new ServiceMarketing(new RepositoryMarketing())
                .calcularSegmentos(clientes, solicitudes, HOY);

        assertEquals(1, segmentos.size());
        assertEquals("Bogotá", segmentos.get(0).getCiudad());
        assertEquals(5, segmentos.get(0).getCantidadClientes());
        assertEquals("Social", segmentos.get(0).getTipoEvento());
        assertThrows(IllegalArgumentException.class,
                () -> new ServiceMarketing(new RepositoryMarketing())
                        .calcularSegmentos(null, solicitudes, HOY));
    }

    private String crearBaseConCincoClientes() throws Exception {
        String url = "jdbc:h2:mem:marketing_cu21_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        try (Connection connection = DatabaseConnection.getConnection(url)) {
            insertarUsuario(connection, 1, "Administradora", "ADMINISTRADOR");
            for (int id = 2; id <= 6; id++) {
                insertarUsuario(connection, id, "Cliente " + id, "CLIENTE");
            }
        }
        return url;
    }

    private void insertarUsuario(Connection connection, int id, String nombre, String tipo)
            throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO Usuario (id_usuario, nombre, usuario, contraseña, estado, "
                + "fecha_registro, acepta_marketing, tipo_usuario, pais, ciudad, fecha_nacimiento) "
                + "VALUES (?, ?, ?, 'x', 'ACTIVO', CURRENT_TIMESTAMP, TRUE, ?, "
                + "'Colombia', 'Bogotá', DATE '2000-01-01')")) {
            statement.setInt(1, id);
            statement.setString(2, nombre);
            statement.setString(3, "marketing_" + id);
            statement.setString(4, tipo);
            statement.executeUpdate();
        }
    }

    private void agregarBusqueda(String url, int idCliente, String tipo) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection(url);
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO Busqueda_evento (id_usuario, tipo, fecha_busqueda) "
                                + "VALUES (?, ?, CURRENT_TIMESTAMP)")) {
            statement.setInt(1, idCliente);
            statement.setString(2, tipo);
            statement.executeUpdate();
        }
    }

    private int contarMiembrosActivos(String url) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection(url);
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(
                        "SELECT COUNT(*) FROM Segmento_cliente sc "
                                + "JOIN Segmentacion s ON s.id_segmento = sc.id_segmento "
                                + "WHERE s.estados = 'ACTIVA'")) {
            result.next();
            return result.getInt(1);
        }
    }
}

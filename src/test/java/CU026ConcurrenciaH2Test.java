import config.DatabaseConnection;
import entities.Contratacion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repositories.RepositoryContratacion;
import services.OrganizadorNoDisponibleException;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Concurrencia del registro de CU-026 frente a cambios simultáneos de
 * estado de cuenta, como los que hace CU-029 al suspender un usuario
 * (UPDATE de Usuario.estado dentro de una transacción).
 */
public class CU026ConcurrenciaH2Test {

    private static final long ESPERA_MS = 700;

    private String url;
    private RepositoryContratacion repository;
    private final LocalDate fecha = LocalDate.now().plusDays(30);

    @BeforeEach
    void preparar() throws Exception {
        url = "jdbc:h2:mem:cu26_concurrencia_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";

        try (Connection connection = DatabaseConnection.getConnection(url);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO Usuario
                    (id_usuario, nombre, usuario, contraseña, estado,
                     fecha_registro, acepta_marketing, tipo_usuario)
                    VALUES
                    (9001, 'Cliente CU26', 'cliente_cu26', 'clave',
                     'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'CLIENTE'),
                    (9002, 'Organizador CU26', 'organizador_cu26', 'clave',
                     'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'ORGANIZADOR')
                    """);
            statement.executeUpdate("""
                    INSERT INTO Organizador
                    (id_organizador, id_usuario, empresa, telefono, estado,
                     ciudad, especialidad, portafolio, tarifa_base,
                     estado_verificacion)
                    VALUES
                    (9001, 9002, 'Eventos CU26', '3000000000', 'ACTIVO',
                     'Bogota', 'Bodas', 'Bodas', 1500000, 'VERIFICADO')
                    """);
        }

        repository = new RepositoryContratacion(url);
    }

    @AfterEach
    void limpiar() throws Exception {
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("DROP ALL OBJECTS");
        }
    }

    @Test
    void dosEnviosSimultaneosDeLaMismaSolicitudRegistranSoloUna() throws Exception {
        CountDownLatch salida = new CountDownLatch(1);
        List<CompletableFuture<String>> envios = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            envios.add(CompletableFuture.supplyAsync(() -> {
                try {
                    salida.await();
                    repository.guardarContratacion(solicitud());
                    return "OK";
                } catch (IllegalStateException e) {
                    return e.getMessage();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return "INTERRUMPIDO";
                }
            }));
        }

        salida.countDown();

        List<String> resultados = new ArrayList<>();
        for (CompletableFuture<String> envio : envios) {
            resultados.add(envio.get(15, TimeUnit.SECONDS));
        }

        assertEquals(1, resultados.stream().filter("OK"::equals).count(), resultados.toString());
        assertTrue(resultados.contains(RepositoryContratacion.SOLICITUD_DUPLICADA), resultados.toString());
        assertEquals(1, contar("Solicitud_Contratacion_Organizador"));
        assertEquals(1, contar("Notificacion_interna"));
    }

    @Test
    void suspensionEnCursoDelOrganizadorBloqueaElRegistroYProduceE2() throws Exception {
        try (Connection suspension = DatabaseConnection.getConnection(url)) {
            suspension.setAutoCommit(false);
            ejecutar(suspension, "UPDATE Usuario SET estado = 'SUSPENDIDO' WHERE id_usuario = 9002");

            CompletableFuture<Void> registro = CompletableFuture.runAsync(
                    () -> repository.guardarContratacion(solicitud()));

            // Mientras la suspensión no termina, el registro espera el bloqueo.
            assertThrows(TimeoutException.class,
                    () -> registro.get(ESPERA_MS, TimeUnit.MILLISECONDS));

            suspension.commit();

            ExecutionException error = assertThrows(ExecutionException.class,
                    () -> registro.get(15, TimeUnit.SECONDS));
            assertInstanceOf(OrganizadorNoDisponibleException.class, error.getCause());
        }

        assertEquals(0, contar("Solicitud_Contratacion_Organizador"));
        assertEquals(0, contar("Notificacion_interna"));
    }

    @Test
    void suspensionRevertidaPermiteCompletarElRegistro() throws Exception {
        try (Connection suspension = DatabaseConnection.getConnection(url)) {
            suspension.setAutoCommit(false);
            ejecutar(suspension, "UPDATE Usuario SET estado = 'SUSPENDIDO' WHERE id_usuario = 9002");

            CompletableFuture<Void> registro = CompletableFuture.runAsync(
                    () -> repository.guardarContratacion(solicitud()));

            assertThrows(TimeoutException.class,
                    () -> registro.get(ESPERA_MS, TimeUnit.MILLISECONDS));

            suspension.rollback();
            registro.get(15, TimeUnit.SECONDS);
        }

        assertEquals(1, contar("Solicitud_Contratacion_Organizador"));
        assertEquals(1, contar("Notificacion_interna"));
    }

    @Test
    void registroEnCursoHaceEsperarLaSuspensionDelOrganizador() throws Exception {
        try (Connection registro = DatabaseConnection.getConnection(url)) {
            registro.setAutoCommit(false);
            // Misma sentencia de bloqueo que usa guardarContratacion (E2).
            ejecutar(registro, "SELECT o.id_organizador FROM Organizador o "
                    + "JOIN Usuario u ON u.id_usuario = o.id_usuario "
                    + "WHERE o.id_organizador = 9001 FOR UPDATE");

            CompletableFuture<Void> suspension = CompletableFuture.runAsync(() -> {
                try (Connection otra = DatabaseConnection.getConnection(url)) {
                    ejecutar(otra, "UPDATE Usuario SET estado = 'SUSPENDIDO' WHERE id_usuario = 9002");
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });

            assertThrows(TimeoutException.class,
                    () -> suspension.get(ESPERA_MS, TimeUnit.MILLISECONDS));

            registro.commit();
            suspension.get(15, TimeUnit.SECONDS);
        }

        assertEquals(1, contar("Usuario WHERE id_usuario = 9002 AND estado = 'SUSPENDIDO'"));
    }

    @Test
    void suspensionEnCursoDelClienteImpideRegistrar() throws Exception {
        try (Connection suspension = DatabaseConnection.getConnection(url)) {
            suspension.setAutoCommit(false);
            ejecutar(suspension, "UPDATE Usuario SET estado = 'SUSPENDIDO' WHERE id_usuario = 9001");

            CompletableFuture<Void> registro = CompletableFuture.runAsync(
                    () -> repository.guardarContratacion(solicitud()));

            assertThrows(TimeoutException.class,
                    () -> registro.get(ESPERA_MS, TimeUnit.MILLISECONDS));

            suspension.commit();

            ExecutionException error = assertThrows(ExecutionException.class,
                    () -> registro.get(15, TimeUnit.SECONDS));
            assertInstanceOf(IllegalArgumentException.class, error.getCause());
        }

        assertEquals(0, contar("Solicitud_Contratacion_Organizador"));
    }

    private Contratacion solicitud() {
        Contratacion datos = new Contratacion(
                9001, 9001, "Boda", "Bogota", fecha, new BigDecimal("2000000"), null);
        return datos;
    }

    private void ejecutar(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private int contar(String tablaYCondicion) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection(url);
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + tablaYCondicion)) {
            result.next();
            return result.getInt(1);
        }
    }
}

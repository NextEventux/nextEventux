import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import inicializacion.CargarDatosEjemplo;
import persistencia.DatabaseConnection;
import entities.SesionUsuario;
import repositories.RepositoryAdministracion;
import repositories.RepositoryContratacion;
import repositories.RepositoryOrganizador;
import services.ServiceAdministracion;

/**
 * CU-029: operaciones simultaneas de varios administradores.
 * Comprueba que el bloqueo ordenado de las cuentas de administrador
 * mantiene las invariantes: nunca quedan cero administradores activos y
 * nunca hay mas de una suspension vigente por usuario.
 */
class SuspensionUsuarioConcurrenciaH2Test {

    private static final int REPETICIONES = 20;

    private String url;
    private ServiceAdministracion servicio;

    private void nuevaBase() throws Exception {
        url = "jdbc:h2:mem:cu29c_" + UUID.randomUUID().toString().replace("-", "")
                + ";DB_CLOSE_DELAY=-1";
        try (Connection c = DatabaseConnection.getConnection(url)) {
            CargarDatosEjemplo.cargar(c);
        }
        servicio = new ServiceAdministracion(
                new RepositoryContratacion(url),
                new RepositoryOrganizador(url),
                new RepositoryAdministracion(url));
    }

    private static SesionUsuario admin(int id) {
        SesionUsuario sesion = new SesionUsuario();
        sesion.setIdUsuario(id);
        sesion.setTipoUsuario("ADMINISTRADOR");
        sesion.setEstadoUsuario("ACTIVO");
        return sesion;
    }

    private void sql(String sentencia) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection(url);
             Statement s = c.createStatement()) {
            s.execute(sentencia);
        }
    }

    private int contar(String consulta) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection(url);
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery(consulta)) {
            r.next();
            return r.getInt(1);
        }
    }

    private int administradoresActivos() throws SQLException {
        return contar("SELECT COUNT(*) FROM Usuario WHERE tipo_usuario = 'ADMINISTRADOR' "
                + "AND estado = 'ACTIVO'");
    }

    /** Ejecuta las tareas a la vez y devuelve cuantas terminaron sin error. */
    private int ejecutarALaVez(List<Runnable> tareas) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tareas.size());
        CountDownLatch inicio = new CountDownLatch(1);
        try {
            List<Future<Boolean>> resultados = new ArrayList<>();
            for (Runnable tarea : tareas) {
                Callable<Boolean> llamada = () -> {
                    inicio.await();
                    try {
                        tarea.run();
                        return true;
                    } catch (RuntimeException rechazo) {
                        // Rechazo de negocio, sesion invalida o espera de bloqueo agotada.
                        return false;
                    }
                };
                resultados.add(pool.submit(llamada));
            }
            inicio.countDown();
            int exitos = 0;
            for (Future<Boolean> resultado : resultados) {
                if (resultado.get(20, TimeUnit.SECONDS)) {
                    exitos++;
                }
            }
            return exitos;
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void c1SuspensionCruzadaDeLosDosUltimosAdministradores() throws Exception {
        for (int i = 0; i < REPETICIONES; i++) {
            nuevaBase();
            sql("UPDATE Usuario SET estado = 'INACTIVO' WHERE id_usuario = 16");

            int exitos = ejecutarALaVez(List.of(
                    () -> servicio.suspenderUsuario(admin(14), 15, "Suspensión cruzada A"),
                    () -> servicio.suspenderUsuario(admin(15), 14, "Suspensión cruzada B")));

            int suspendidos = contar("SELECT COUNT(*) FROM Usuario WHERE id_usuario IN (14, 15) "
                    + "AND estado = 'SUSPENDIDO'");
            assertTrue(exitos <= 1, "como maximo una suspension cruzada puede aplicarse");
            assertTrue(administradoresActivos() >= 1, "nunca pueden quedar cero administradores activos");
            assertEquals(2 - exitos, administradoresActivos());
            assertEquals(exitos, suspendidos);
            assertEquals(suspendidos, contar("SELECT COUNT(*) FROM Historial_suspension_usuario "
                    + "WHERE fecha_reactivacion IS NULL"));
        }
    }

    @Test
    void c2DosAdministradoresSuspendenALaMismaCuenta() throws Exception {
        for (int i = 0; i < REPETICIONES; i++) {
            nuevaBase();
            int avisos = contar("SELECT COUNT(*) FROM Notificacion_interna WHERE id_usuario = 5");

            int exitos = ejecutarALaVez(List.of(
                    () -> servicio.suspenderUsuario(admin(14), 5, "Motivo de Camila"),
                    () -> servicio.suspenderUsuario(admin(15), 5, "Motivo de Mateo")));

            assertEquals(1, exitos);
            assertEquals(1, contar("SELECT COUNT(*) FROM Historial_suspension_usuario WHERE id_usuario = 5"));
            assertEquals(1, contar("SELECT COUNT(*) FROM Historial_suspension_usuario WHERE id_usuario = 5 "
                    + "AND fecha_reactivacion IS NULL"));
            assertEquals(avisos + 1,
                    contar("SELECT COUNT(*) FROM Notificacion_interna WHERE id_usuario = 5"));
        }
    }

    @Test
    void c3SuspensionYReactivacionSimultaneasDejanEstadoCoherente() throws Exception {
        for (int i = 0; i < REPETICIONES; i++) {
            nuevaBase();
            servicio.suspenderUsuario(admin(14), 5, "Primera suspensión");

            ejecutarALaVez(List.of(
                    () -> servicio.reactivarUsuario(admin(15), 5),
                    () -> servicio.suspenderUsuario(admin(16), 5, "Segunda suspensión")));

            int suspendida = contar("SELECT COUNT(*) FROM Usuario WHERE id_usuario = 5 "
                    + "AND estado = 'SUSPENDIDO'");
            int vigentes = contar("SELECT COUNT(*) FROM Historial_suspension_usuario WHERE id_usuario = 5 "
                    + "AND fecha_reactivacion IS NULL");
            assertEquals(suspendida, vigentes, "el estado debe coincidir con el historial");
            assertTrue(vigentes <= 1);
        }
    }

    @Test
    void c4TresAdministradoresSeSuspendenEntreSi() throws Exception {
        for (int i = 0; i < REPETICIONES; i++) {
            nuevaBase();

            ejecutarALaVez(List.of(
                    () -> servicio.suspenderUsuario(admin(14), 15, "Cruzada 14-15"),
                    () -> servicio.suspenderUsuario(admin(14), 16, "Cruzada 14-16"),
                    () -> servicio.suspenderUsuario(admin(15), 14, "Cruzada 15-14"),
                    () -> servicio.suspenderUsuario(admin(15), 16, "Cruzada 15-16"),
                    () -> servicio.suspenderUsuario(admin(16), 14, "Cruzada 16-14"),
                    () -> servicio.suspenderUsuario(admin(16), 15, "Cruzada 16-15")));

            assertTrue(administradoresActivos() >= 1, "nunca pueden quedar cero administradores activos");
            assertEquals(contar("SELECT COUNT(*) FROM Usuario WHERE tipo_usuario = 'ADMINISTRADOR' "
                    + "AND estado = 'SUSPENDIDO'"),
                    contar("SELECT COUNT(*) FROM Historial_suspension_usuario WHERE fecha_reactivacion IS NULL"));
        }
    }
}

import entities.eventos.Evento;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import persistencia.DatabaseConnection;
import entities.SesionUsuario;
import repositories.RepositoryPropuestaServicio;
import repositories.RepositorySolicitudEjecucion;
import services.ServiceSolicitudes;

/**
 * Datos mínimos de CU-12 para las pruebas en H2 en memoria: un organizador con
 * un evento y una necesidad, un proveedor con un servicio y una solicitud
 * pendiente entre ambos.
 */
class DatosCU12Prueba {

    static final int USUARIO_ORGANIZADOR = 9301;
    static final int USUARIO_PROVEEDOR = 9302;
    static final int ORGANIZADOR = 9301;
    static final int PROVEEDOR = 9302;
    static final int SOLICITUD = 9301;

    private DatosCU12Prueba() {
    }

    static void preparar(String url) throws Exception {
        System.setProperty("nexteventux.db.url", url);

        try (Connection connection = DatabaseConnection.getConnection();
                Statement statement = connection.createStatement()) {

            statement.executeUpdate("""
                    INSERT INTO Usuario
                    (id_usuario, nombre, usuario, contraseña,
                     estado, fecha_registro, acepta_marketing, tipo_usuario)
                    VALUES
                    (9301, 'Valentina Flores', 'orgcu12', 'clave',
                     'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'ORGANIZADOR'),
                    (9302, 'Luna Decoraciones', 'provcu12', 'clave',
                     'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'PROVEEDOR')
                    """);
            statement.executeUpdate("""
                    INSERT INTO Organizador
                    (id_organizador, id_usuario, empresa, estado)
                    VALUES (9301, 9301, 'Eventos Flores', 'ACTIVO')
                    """);
            statement.executeUpdate("""
                    INSERT INTO Proveedor
                    (id_proveedor, id_usuario, nombre, estado)
                    VALUES (9302, 9302, 'Luna Decoraciones', 'APROBADO')
                    """);
            statement.executeUpdate("""
                    INSERT INTO Evento
                    (id_evento, id_organizador, nombre, presupuesto,
                     invitados_iniciales, fecha_inicio, fecha_fin,
                     ubicacion, estado, fecha_creacion)
                    VALUES
                    (9301, 9301, 'Boda de Valentina', 10000000, 80,
                     TIMESTAMP '2026-12-05 15:00:00',
                     TIMESTAMP '2026-12-05 23:00:00',
                     'Salón Primavera', 'PLANEADO', CURRENT_TIMESTAMP)
                    """);
            statement.executeUpdate("""
                    INSERT INTO Servicio
                    (id_servicio, id_proveedor, nombre, precio, estado)
                    VALUES (9301, 9302, 'Decoración floral', 1200000,
                            'DISPONIBLE')
                    """);
            statement.executeUpdate("""
                    INSERT INTO Necesidad_Servicio_Evento
                    (id_necesidad_servicio, id_evento, descripcion, estado)
                    VALUES (9301, 9301, 'Decoración del salón', 'ABIERTA')
                    """);
            statement.executeUpdate("""
                    INSERT INTO Solicitud_Ejecucion
                    (id_solicitud, id_proveedor, id_evento, id_servicio,
                     id_necesidad_servicio, fecha_solicitud, estado)
                    VALUES (9301, 9302, 9301, 9301, 9301,
                            CURRENT_TIMESTAMP, 'PENDIENTE')
                    """);
        }
    }

    static void limpiar(String url) throws Exception {
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
                Statement statement = connection.createStatement()) {
            statement.execute("DROP ALL OBJECTS");
        } finally {
            System.clearProperty("nexteventux.db.url");
        }
    }

    static ServiceSolicitudes servicioProveedor() {
        return crearServicio("PROVEEDOR", PROVEEDOR, USUARIO_PROVEEDOR);
    }

    static ServiceSolicitudes servicioOrganizador() {
        return crearServicio("ORGANIZADOR", ORGANIZADOR, USUARIO_ORGANIZADOR);
    }

    private static ServiceSolicitudes crearServicio(
            String rol, int idActor, int idUsuario) {

        SesionUsuario sesion = new SesionUsuario();
        sesion.setTipoUsuario(rol);
        sesion.setIdActor(idActor);
        sesion.setIdUsuario(idUsuario);
        sesion.setEstadoUsuario("ACTIVO");
        sesion.setEstadoActor("PROVEEDOR".equals(rol) ? "APROBADO" : "ACTIVO");

        return new ServiceSolicitudes(
                new RepositorySolicitudEjecucion(),
                new RepositoryPropuestaServicio(),
                sesion);
    }
}

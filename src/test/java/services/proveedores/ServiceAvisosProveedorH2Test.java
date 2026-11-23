package services.proveedores;

import entities.SesionUsuario;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import persistencia.DatabaseConnection;
import persistencia.DatabaseTestUtils;
import repositories.proveedores.RepositoryAvisosProveedor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServiceAvisosProveedorH2Test {

    private static final int USUARIO_PROVEEDOR = 8101;
    private static final int PROVEEDOR = 9101;
    private static final int OTRO_USUARIO = 8102;

    private String url;
    private ServiceAvisosProveedor service;

    @BeforeEach
    void preparar() throws Exception {
        // la base vive entre conexiones del servicio y tiene un nombre único en memoria
        url = "jdbc:h2:mem:avisos_" + UUID.randomUUID().toString().replace("-", "")
                + ";DB_CLOSE_DELAY=-1";

        // creamos las dos cuentas para comprobar que cada una vea sus propios avisos
        try (Connection connection = DatabaseConnection.getConnection(url);
                Statement statement = connection.createStatement()) {
            DatabaseTestUtils.crearEsquemaVacio(connection);
            statement.executeUpdate("INSERT INTO Usuario "
                    + "(id_usuario, nombre, usuario, contraseña, estado, fecha_registro, "
                    + "acepta_marketing, tipo_usuario) VALUES "
                    + "(" + USUARIO_PROVEEDOR + ", 'Proveedor de prueba', 'proveedor.test', "
                    + "'x', 'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'PROVEEDOR')");
            statement.executeUpdate("INSERT INTO Usuario "
                    + "(id_usuario, nombre, usuario, contraseña, estado, fecha_registro, "
                    + "acepta_marketing, tipo_usuario) VALUES "
                    + "(" + OTRO_USUARIO + ", 'Otro proveedor', 'otro.test', "
                    + "'x', 'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'PROVEEDOR')");
            statement.executeUpdate("INSERT INTO Proveedor "
                    + "(id_proveedor, id_usuario, nombre, estado) VALUES "
                    + "(" + PROVEEDOR + ", " + USUARIO_PROVEEDOR
                    + ", 'Proveedor de prueba', 'APROBADO')");
            statement.executeUpdate("INSERT INTO Proveedor "
                    + "(id_proveedor, id_usuario, nombre, estado) VALUES "
                    + "(9102, " + OTRO_USUARIO + ", 'Otro proveedor', 'APROBADO')");
        }

        service = new ServiceAvisosProveedor(new RepositoryAvisosProveedor(url));
    }

    @AfterEach
    void cerrarBase() throws Exception {
        if (url != null) {
            // usamos jdbc directo para apagar esta base temporal sin volver a inicializarla
            try (Connection connection = DriverManager.getConnection(url, "sa", "");
                    Statement statement = connection.createStatement()) {
                statement.execute("SHUTDOWN");
            }
        }
    }

    @Test
    void listaSoloLosAvisosDelProveedorEnOrdenDeMasReciente() throws Exception {
        ejecutar("INSERT INTO Notificacion_interna (id_notificacion, id_usuario, mensaje) "
                + "VALUES (1, " + USUARIO_PROVEEDOR + ", 'Aviso anterior')");
        ejecutar("INSERT INTO Notificacion_interna (id_notificacion, id_usuario, mensaje) "
                + "VALUES (2, " + USUARIO_PROVEEDOR + ", 'Aviso reciente')");
        ejecutar("INSERT INTO Notificacion_interna (id_notificacion, id_usuario, mensaje) "
                + "VALUES (3, " + OTRO_USUARIO + ", 'Aviso de otra cuenta')");

        assertEquals(
                java.util.List.of("Aviso reciente", "Aviso anterior"),
                service.listar(sesionProveedor()));
    }

    @Test
    void exigeSesionDeProveedorActivoYVerificado() {
        assertThrows(IllegalStateException.class, () -> service.listar(null));

        SesionUsuario sinUsuario = sesionProveedor();
        sinUsuario.setIdUsuario(0);
        assertThrows(IllegalStateException.class, () -> service.listar(sinUsuario));

        SesionUsuario sinProveedor = sesionProveedor();
        sinProveedor.setIdActor(0);
        assertThrows(IllegalStateException.class, () -> service.listar(sinProveedor));

        SesionUsuario organizador = sesionProveedor();
        organizador.setTipoUsuario("ORGANIZADOR");
        assertThrows(IllegalStateException.class, () -> service.listar(organizador));

        SesionUsuario usuarioInactivo = sesionProveedor();
        usuarioInactivo.setEstadoUsuario("INACTIVO");
        assertThrows(IllegalStateException.class, () -> service.listar(usuarioInactivo));

        SesionUsuario proveedorPendiente = sesionProveedor();
        proveedorPendiente.setEstadoActor("PENDIENTE");
        assertThrows(IllegalStateException.class, () -> service.listar(proveedorPendiente));
    }

    @Test
    void elRepositorioVuelveAVerificarQueLaCuentaSigaHabilitada() throws Exception {
        ejecutar("UPDATE Proveedor SET estado = 'PENDIENTE' WHERE id_proveedor = " + PROVEEDOR);

        assertThrows(IllegalStateException.class, () -> service.listar(sesionProveedor()));
    }

    @Test
    void permiteProveedorVerificadoSinAvisos() throws Exception {
        ejecutar("UPDATE Proveedor SET estado = 'VERIFICADO' WHERE id_proveedor = " + PROVEEDOR);
        SesionUsuario verificado = sesionProveedor();
        verificado.setEstadoActor("VERIFICADO");

        assertEquals(java.util.List.of(), service.listar(verificado));
    }

    private SesionUsuario sesionProveedor() {
        SesionUsuario sesion = new SesionUsuario();
        sesion.setIdUsuario(USUARIO_PROVEEDOR);
        sesion.setIdActor(PROVEEDOR);
        sesion.setTipoUsuario("PROVEEDOR");
        sesion.setEstadoUsuario("ACTIVO");
        sesion.setEstadoActor("APROBADO");
        return sesion;
    }

    private void ejecutar(String sql) throws Exception {
        // guardamos avisos de prueba sin pasar por otro servicio
        try (Connection connection = DatabaseConnection.getConnection(url);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }
}

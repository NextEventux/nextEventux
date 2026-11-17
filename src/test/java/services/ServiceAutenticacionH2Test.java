package services;

import entities.SesionUsuario;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import persistencia.DatabaseConnection;
import repositories.RepositoryAutenticacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceAutenticacionH2Test {

    private String url;
    private ServiceAutenticacion service;

    @BeforeEach
    void preparar() throws Exception {
        // la base vive entre conexiones del servicio y tiene un nombre único en memoria
        url = "jdbc:h2:mem:autenticacion_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";

        // creamos las cuentas y perfiles de prueba en esta base temporal
        insertarUsuario(8001, "Proveedor aprobado", "proveedor.aprobado", "PROVEEDOR", "ACTIVO");
        insertarUsuario(8002, "Organizadora activa", "organizadora.activa", "ORGANIZADOR", "ACTIVO");
        insertarUsuario(8003, "Proveedor pendiente", "proveedor.pendiente", "PROVEEDOR", "ACTIVO");
        insertarUsuario(8004, "Organizador inactivo", "organizador.inactivo", "ORGANIZADOR", "ACTIVO");
        insertarUsuario(8005, "Cliente", "cliente", "CLIENTE", "ACTIVO");
        insertarUsuario(8006, "Administrador", "administrador", "ADMINISTRADOR", "ACTIVO");
        insertarUsuario(8007, "Cuenta inactiva", "cuenta.inactiva", "ADMINISTRADOR", "INACTIVO");
        insertarUsuario(8008, "Proveedor sin perfil", "proveedor.sin.perfil", "PROVEEDOR", "ACTIVO");
        insertarUsuario(8009, "Rol no habilitado", "rol.no.habilitado", "INVITADO", "ACTIVO");
        insertarUsuario(8010, "Proveedor verificado", "proveedor.verificado", "PROVEEDOR", "ACTIVO");
        insertarUsuario(8011, "Organizador sin perfil", "organizador.sin.perfil", "ORGANIZADOR", "ACTIVO");

        ejecutar("INSERT INTO Proveedor "
                + "(id_proveedor, id_usuario, nombre, empresa, correo, telefono, direccion, estado) "
                + "VALUES (8101, 8001, 'Proveedor aprobado', 'Empresa', 'p@prueba.com', "
                + "'3000000001', 'Bogota', 'APROBADO')");
        ejecutar("INSERT INTO Proveedor "
                + "(id_proveedor, id_usuario, nombre, empresa, correo, telefono, direccion, estado) "
                + "VALUES (8102, 8003, 'Proveedor pendiente', 'Empresa', 'pp@prueba.com', "
                + "'3000000002', 'Bogota', 'PENDIENTE')");
        ejecutar("INSERT INTO Proveedor "
                + "(id_proveedor, id_usuario, nombre, empresa, correo, telefono, direccion, estado) "
                + "VALUES (8110, 8010, 'Proveedor verificado', 'Empresa', 'pv@prueba.com', "
                + "'3000000010', 'Bogota', 'VERIFICADO')");
        ejecutar("INSERT INTO Organizador "
                + "(id_organizador, id_usuario, empresa, telefono, estado) "
                + "VALUES (8201, 8002, 'Eventos', '3000000003', 'ACTIVO')");
        ejecutar("INSERT INTO Organizador "
                + "(id_organizador, id_usuario, empresa, telefono, estado) "
                + "VALUES (8202, 8004, 'Eventos', '3000000004', 'INACTIVO')");

        service = new ServiceAutenticacion(new RepositoryAutenticacion(url));
    }

    @AfterEach
    void limpiar() throws Exception {
        if (url != null) {
            // usamos jdbc directo para apagar esta base temporal sin volver a inicializarla
            try (Connection connection = DriverManager.getConnection(url, "sa", "");
                    Statement statement = connection.createStatement()) {
                statement.execute("SHUTDOWN");
            }
        }
    }

    @Test
    void permiteIngresarAProveedorAprobadoOrganizadorClienteYAdministrador() {
        SesionUsuario proveedor = service.iniciarSesion("proveedor.aprobado", "clave8001");
        assertEquals(8001, proveedor.getIdUsuario());
        assertEquals(8101, proveedor.getIdActor());
        assertEquals("PROVEEDOR", proveedor.getTipoUsuario());

        SesionUsuario organizador = service.iniciarSesion("organizadora.activa", "clave8002");
        assertEquals(8201, organizador.getIdActor());
        assertEquals("ORGANIZADOR", organizador.getTipoUsuario());

        SesionUsuario cliente = service.iniciarSesion("cliente", "clave8005");
        assertEquals(8005, cliente.getIdUsuario());
        assertEquals("CLIENTE", cliente.getTipoUsuario());

        SesionUsuario administrador = service.iniciarSesion("administrador", "clave8006");
        assertEquals(8006, administrador.getIdUsuario());
        assertEquals("ADMINISTRADOR", administrador.getTipoUsuario());
    }

    @Test
    void rechazaCredencialesVaciasOIncorrectas() {
        assertThrows(IllegalArgumentException.class,
                () -> service.iniciarSesion(null, "clave8001"));
        assertThrows(IllegalArgumentException.class,
                () -> service.iniciarSesion(" ", "clave8001"));
        assertThrows(IllegalArgumentException.class,
                () -> service.iniciarSesion("proveedor.aprobado", null));
        assertThrows(IllegalArgumentException.class,
                () -> service.iniciarSesion("proveedor.aprobado", " "));
        assertThrows(IllegalArgumentException.class,
                () -> service.iniciarSesion("proveedor.aprobado", "incorrecta"));
        assertThrows(IllegalArgumentException.class,
                () -> service.iniciarSesion("no-existe", "clave"));
    }

    @Test
    void rechazaProveedorSinPerfilYUsuarioConRolNoHabilitado() {
        assertThrows(IllegalStateException.class,
                () -> service.iniciarSesion("proveedor.sin.perfil", "clave8008"));
        assertThrows(IllegalStateException.class,
                () -> service.iniciarSesion("rol.no.habilitado", "clave8009"));
    }

    @Test
    void admiteProveedorVerificadoYRechazaOrganizadorSinPerfil() {
        SesionUsuario proveedor = service.iniciarSesion("proveedor.verificado", "clave8010");
        assertEquals(8110, proveedor.getIdActor());
        assertEquals("VERIFICADO", proveedor.getEstadoActor());

        assertThrows(IllegalStateException.class,
                () -> service.iniciarSesion("organizador.sin.perfil", "clave8011"));
    }

    @Test
    void rechazaCuentasInactivasYActoresNoHabilitados() {
        assertThrows(IllegalStateException.class,
                () -> service.iniciarSesion("proveedor.pendiente", "clave8003"));
        assertThrows(IllegalStateException.class,
                () -> service.iniciarSesion("organizador.inactivo", "clave8004"));
        assertThrows(IllegalStateException.class,
                () -> service.iniciarSesion("cuenta.inactiva", "clave8007"));
    }

    private void insertarUsuario(
            int id, String nombre, String usuario, String tipo, String estado) throws Exception {
        ejecutar("INSERT INTO Usuario "
                + "(id_usuario, nombre, usuario, contraseña, estado, fecha_registro, "
                + "acepta_marketing, tipo_usuario) VALUES ("
                + id + ", '" + nombre + "', '" + usuario + "', 'clave" + id + "', '"
                + estado + "', CURRENT_TIMESTAMP, FALSE, '" + tipo + "')");
    }

    private void ejecutar(String sql) throws Exception {
        // agregamos los datos de autenticación que necesita el servicio
        try (Connection connection = DatabaseConnection.getConnection(url);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }
}

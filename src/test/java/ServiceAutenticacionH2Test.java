import config.DatabaseConnection;
import entities.SesionUsuario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repositories.RepositoryAutenticacion;
import services.ServiceAutenticacion;

import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

public class ServiceAutenticacionH2Test {

    private ServiceAutenticacion service;

    @BeforeEach
    void preparar() throws Exception {

        System.setProperty(
                "nexteventux.db.url",
                "jdbc:h2:mem:auth;DB_CLOSE_DELAY=-1");

        try (Connection connection =
                     DatabaseConnection.getConnection();
             Statement statement =
                     connection.createStatement()) {

            statement.executeUpdate("""
                    INSERT INTO Usuario
                    (id_usuario, nombre, usuario, contraseña,
                     estado, fecha_registro,
                     acepta_marketing, tipo_usuario)
                    VALUES
                    (8001, 'Proveedor prueba',
                     'proveedor.prueba', 'clave1',
                     'ACTIVO', CURRENT_TIMESTAMP,
                     FALSE, 'PROVEEDOR')
                    """);

            statement.executeUpdate("""
                    INSERT INTO Proveedor
                    (id_proveedor, id_usuario, nombre,
                     empresa, correo, telefono,
                     direccion, estado)
                    VALUES
                    (8101, 8001, 'Proveedor prueba',
                     'Empresa prueba',
                     'proveedor@prueba.com',
                     '3000000000',
                     'Bogota',
                     'APROBADO')
                    """);

            statement.executeUpdate("""
                    INSERT INTO Usuario
                    (id_usuario, nombre, usuario, contraseña,
                     estado, fecha_registro,
                     acepta_marketing, tipo_usuario)
                    VALUES
                    (8002, 'Organizador prueba',
                     'organizador.prueba', 'clave2',
                     'ACTIVO', CURRENT_TIMESTAMP,
                     FALSE, 'ORGANIZADOR')
                    """);

            statement.executeUpdate("""
                    INSERT INTO Organizador
                    (id_organizador, id_usuario,
                     empresa, telefono, estado)
                    VALUES
                    (8201, 8002,
                     'Eventos prueba',
                     '3000000001',
                     'ACTIVO')
                    """);

            statement.executeUpdate("""
                    INSERT INTO Usuario
                    (id_usuario, nombre, usuario, contraseña,
                     estado, fecha_registro,
                     acepta_marketing, tipo_usuario)
                    VALUES
                    (8003, 'Proveedor pendiente',
                     'proveedor.pendiente', 'clave3',
                     'ACTIVO', CURRENT_TIMESTAMP,
                     FALSE, 'PROVEEDOR')
                    """);

            statement.executeUpdate("""
                    INSERT INTO Proveedor
                    (id_proveedor, id_usuario, nombre,
                     empresa, correo, telefono,
                     direccion, estado)
                    VALUES
                    (8102, 8003, 'Proveedor pendiente',
                     'Empresa pendiente',
                     'pendiente@prueba.com',
                     '3000000002',
                     'Bogota',
                     'PENDIENTE')
                    """);
        }

        service =
                new ServiceAutenticacion(
                        new RepositoryAutenticacion());
    }

    @AfterEach
    void limpiar() throws Exception {

        try (Connection connection =
                     DatabaseConnection.getConnection();
             Statement statement =
                     connection.createStatement()) {

            statement.execute(
                    "DROP ALL OBJECTS");
        }

        System.clearProperty(
                "nexteventux.db.url");
    }

    @Test
    void proveedorAprobadoPuedeIngresar() {

        SesionUsuario sesion =
                service.iniciarSesion(
                        "proveedor.prueba",
                        "clave1");

        assertEquals(
                "PROVEEDOR",
                sesion.getTipoUsuario());

        assertEquals(
                8101,
                sesion.getIdActor());
    }

    @Test
    void organizadorActivoPuedeIngresar() {

        SesionUsuario sesion =
                service.iniciarSesion(
                        "organizador.prueba",
                        "clave2");

        assertEquals(
                "ORGANIZADOR",
                sesion.getTipoUsuario());

        assertEquals(
                8201,
                sesion.getIdActor());
    }

    @Test
    void proveedorPendienteNoPuedeIngresar() {

        IllegalStateException error =
                assertThrows(
                        IllegalStateException.class,
                        () -> service.iniciarSesion(
                                "proveedor.pendiente",
                                "clave3"));

        assertEquals(
                "El proveedor aun no esta aprobado.",
                error.getMessage());
    }

    @Test
    void credencialesIncorrectasNoPermitenIngresar() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.iniciarSesion(
                        "proveedor.prueba",
                        "incorrecta"));
    }
}

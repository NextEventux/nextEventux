import config.DatabaseConnection;
import controllers.ControllerAdministracion;
import entities.Proveedor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repositories.RepositoryProveedor;
import services.ServiceAdministracion;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ServiceAdministracionTest {
    @TempDir Path directorio;
    private RepositoryProveedor repository;
    private ServiceAdministracion service;
    private ControllerAdministracion controller;

    @BeforeEach
    void prepararBase() throws Exception {
        String url = "jdbc:h2:" + directorio.resolve("administracion_" + UUID.randomUUID());
        try (Connection connection = DatabaseConnection.getConnection(url);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO Usuario (id_usuario, nombre, usuario, contraseña, estado, "
                    + "fecha_registro, acepta_marketing, tipo_usuario) VALUES "
                    + "(1, 'Ana Proveedor', 'ana_proveedor', 'solo-prueba', 'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'PROVEEDOR'), "
                    + "(2, 'Correo inválido', 'correo_invalido', 'solo-prueba', 'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'PROVEEDOR')");
            statement.executeUpdate("INSERT INTO Proveedor "
                    + "(id_proveedor, id_usuario, nombre, empresa, correo, telefono, direccion, estado) VALUES "
                    + "(1, 1, 'Ana Proveedor', 'Flores Ana', 'ana@flores.test', '3001234567', 'Bogotá', 'PENDIENTE_VERIFICACION'), "
                    + "(2, 2, 'Proveedor Correo Inválido', 'Empresa', 'no-es-correo', '3101234567', 'Bogotá', 'PENDIENTE_VERIFICACION')");
        }
        repository = new RepositoryProveedor(url);
        service = new ServiceAdministracion(repository);
        controller = new ControllerAdministracion(service);
    }

    @Test
    void flujoPrincipalValidaYApruebaProveedor() {
        assertTrue(controller.verificarInformacionProveedor(1));
        controller.aprobarProveedor(1);
        assertEquals(Proveedor.ESTADO_VERIFICADO, repository.buscarProveedor(1).getEstado());
        assertTrue(controller.consultarProveedoresPendientes().stream().noneMatch(p -> p.getIdProveedor() == 1));
    }

    @Test
    void noApruebaProveedorConCorreoMalFormado() {
        assertFalse(service.verificarInformacionProveedor(2));
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.aprobarProveedor(2));
        assertTrue(error.getMessage().contains("incompleta") || error.getMessage().contains("correo"));
        assertEquals(Proveedor.ESTADO_PENDIENTE_VERIFICACION, repository.buscarProveedor(2).getEstado());
    }

    @Test
    void rechazaProveedorSiElMotivoEstaVacio() {
        assertThrows(IllegalArgumentException.class, () -> service.rechazarProveedor(1, "  "));
        assertEquals(Proveedor.ESTADO_PENDIENTE_VERIFICACION, repository.buscarProveedor(1).getEstado());
    }

    @Test
    void rechazaProveedorYConservaElMotivo() {
        controller.rechazarProveedor(1, "Documentación no coincide con los datos registrados.");
        Proveedor proveedor = repository.buscarProveedor(1);
        assertEquals(Proveedor.ESTADO_RECHAZADO, proveedor.getEstado());
        assertEquals("Documentación no coincide con los datos registrados.", proveedor.getMotivoRechazo());
    }

    @Test
    void noPermiteAprobarUnIdentificadorInexistente() {
        assertThrows(IllegalArgumentException.class, () -> service.aprobarProveedor(999));
    }

    @Test
    void limitaLaLongitudDelMotivoDeRechazo() {
        assertThrows(IllegalArgumentException.class, () -> service.rechazarProveedor(1, "x".repeat(501)));
        assertEquals(Proveedor.ESTADO_PENDIENTE_VERIFICACION, repository.buscarProveedor(1).getEstado());
    }
}

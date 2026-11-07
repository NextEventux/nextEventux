package config;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ActualizacionCuentasDemoTest {

    private String nuevaBase() {
        return "jdbc:h2:mem:demo_" + UUID.randomUUID()
                + ";DB_CLOSE_DELAY=-1";
    }

    private String consultar(Connection conexion, String sql) throws Exception {
        try (Statement statement = conexion.createStatement();
             ResultSet resultado = statement.executeQuery(sql)) {
            assertTrue(resultado.next());
            return resultado.getString(1);
        }
    }

    @Test
    void actualizaCuentasConservandoDatosAnteriores() throws Exception {
        try (Connection conexion = DatabaseConnection.getConnection(nuevaBase())) {
            CargarDatosEjemplo.cargar(conexion);

            // Simulamos una base antigua con una contraseña diferente
            try (Statement statement = conexion.createStatement()) {
                statement.executeUpdate(
                        "UPDATE Usuario SET contraseña = 'clave_antigua' "
                        + "WHERE usuario = 'laura.gomez'");

                // En una base anterior, el ID 7 pertenecía a otra cuenta
                statement.executeUpdate(
                        "UPDATE Usuario SET usuario = 'admin.nexteventux', "
                        + "contraseña = 'clave_personal' WHERE id_usuario = 7");

                // Conservamos también un evento creado anteriormente
                statement.executeUpdate(
                        "UPDATE Evento SET nombre = 'Evento modificado' "
                        + "WHERE id_evento = 1");
            }

            CargarDatosEjemplo.actualizarCuentasDemostracion(conexion);

            assertEquals("1234", consultar(conexion,
                    "SELECT contraseña FROM Usuario "
                    + "WHERE usuario = 'laura.gomez'"));

            assertEquals("clave_personal", consultar(conexion,
                    "SELECT contraseña FROM Usuario "
                    + "WHERE usuario = 'admin.nexteventux'"));

            assertEquals("Evento modificado", consultar(conexion,
                    "SELECT nombre FROM Evento WHERE id_evento = 1"));

            assertEquals("1", consultar(conexion,
                    "SELECT COUNT(*) FROM Esquema_version "
                    + "WHERE nombre = '014_cuentas_demostracion.sql'"));

            // Si ya se aplicó, no debe volver a cambiar la contraseña
            try (Statement statement = conexion.createStatement()) {
                statement.executeUpdate(
                        "UPDATE Usuario SET contraseña = 'nueva_clave' "
                        + "WHERE usuario = 'laura.gomez'");
            }

            CargarDatosEjemplo.actualizarCuentasDemostracion(conexion);

            assertEquals("nueva_clave", consultar(conexion,
                    "SELECT contraseña FROM Usuario "
                    + "WHERE usuario = 'laura.gomez'"));

            assertEquals("1", consultar(conexion,
                    "SELECT COUNT(*) FROM Esquema_version "
                    + "WHERE nombre = '014_cuentas_demostracion.sql'"));
        }
    }


    @Test
    void normalizaLaClaveDeUnaCuentaOficialUnaSolaVez() throws Exception {
        try (Connection conexion = DatabaseConnection.getConnection(nuevaBase())) {
            CargarDatosEjemplo.cargar(conexion);

            // Esta cuenta está reservada para las demostraciones del proyecto
            try (Statement statement = conexion.createStatement()) {
                statement.executeUpdate(
                        "UPDATE Usuario SET contraseña = 'clave_anterior' "
                        + "WHERE usuario = 'laura.gomez'");
            }

            CargarDatosEjemplo.actualizarCuentasDemostracion(conexion);

            assertEquals("1234", consultar(conexion,
                    "SELECT contraseña FROM Usuario "
                    + "WHERE usuario = 'laura.gomez'"));

            // Una vez registrada la migración, no restablece la clave otra vez
            try (Statement statement = conexion.createStatement()) {
                statement.executeUpdate(
                        "UPDATE Usuario SET contraseña = 'clave_nueva' "
                        + "WHERE usuario = 'laura.gomez'");
            }

            CargarDatosEjemplo.actualizarCuentasDemostracion(conexion);

            assertEquals("clave_nueva", consultar(conexion,
                    "SELECT contraseña FROM Usuario "
                    + "WHERE usuario = 'laura.gomez'"));
        }
    }

    @Test
    void funcionaConDatosInicialesActuales() throws Exception {
        try (Connection conexion = DatabaseConnection.getConnection(nuevaBase())) {
            assertTrue(CargarDatosEjemplo.cargarSiEstaVacia(conexion));

            CargarDatosEjemplo.actualizarCuentasDemostracion(conexion);

            assertEquals("12", consultar(conexion,
                    "SELECT COUNT(*) FROM Usuario"));

            assertEquals("1234", consultar(conexion,
                    "SELECT contraseña FROM Usuario "
                    + "WHERE usuario = 'flash'"));

            assertEquals("1", consultar(conexion,
                    "SELECT COUNT(*) FROM Esquema_version "
                    + "WHERE nombre = '014_cuentas_demostracion.sql'"));
        }
    }
}

package config;

import java.sql.*;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ReparacionMainTest {
    @TempDir Path carpeta;

    @Test void rutaAbsolutaConfigurableSinTocarLaBaseReal() {
        String anterior=System.getProperty("nexteventux.home");
        try {
            System.setProperty("nexteventux.home",carpeta.toString());
            assertEquals("jdbc:h2:"+carpeta.resolve("nexteventux")+";DB_CLOSE_DELAY=-1",DatabaseConnection.urlPorDefecto());
        } finally {
            if(anterior==null)System.clearProperty("nexteventux.home");else System.setProperty("nexteventux.home",anterior);
        }
    }

    @Test void baseNuevaCargaEjemplosSinConfundirMetadatosConDatosDelUsuario() throws Exception {
        String url=url();
        try(Connection c=DatabaseConnection.getConnection(url)) {
            CargarDatosEjemplo.cargar(c);
            assertEquals(2,contar(c,"Evento"));
            assertEquals(1,contar(c,"Esquema_version"));
            assertThrows(SQLException.class,()->CargarDatosEjemplo.cargar(c));
        }
        DatabaseConnection.reiniciarInicializacion();
        try(Connection c=DatabaseConnection.getConnection(url)) {
            assertEquals(2,contar(c,"Evento"));
            assertEquals(1,contar(c,"Esquema_version"));
        }
    }

    @Test void reconstruyeUnaBaseBorradaConLaMismaUrl() throws Exception {
        String url=url();
        try(Connection c=DatabaseConnection.getConnection(url);Statement s=c.createStatement()) {s.execute("DROP ALL OBJECTS");}
        try(Connection c=DatabaseConnection.getConnection(url)) {
            assertEquals(0,contar(c,"Evento"));
            assertEquals(1,contar(c,"Esquema_version"));
        }
    }

    @Test void unicidadNoBorraOrganizadoresDuplicadosAlActualizar() throws Exception {
        String url=url();
        try(Connection c=DatabaseConnection.getConnection(url);Statement s=c.createStatement()) {
            CargarDatosEjemplo.cargar(c);
            s.execute("DROP INDEX uq_organizador_usuario");
            s.executeUpdate("DELETE FROM Esquema_version WHERE nombre='010_organizador_usuario_unico.sql'");
            s.executeUpdate("INSERT INTO Organizador(id_usuario,estado) VALUES(1,'ACTIVO')");
        }
        DatabaseConnection.reiniciarInicializacion();
        SQLException error=assertThrows(SQLException.class,()->DatabaseConnection.getConnection(url));
        assertTrue(error.getMessage().contains("repetidos"));
        try(Connection c=DriverManager.getConnection(url,"sa","");Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT COUNT(*) FROM Organizador WHERE id_usuario=1")) {
            assertTrue(r.next());assertEquals(2,r.getInt(1));
            assertEquals(0,contar(c,"Esquema_version"));
        }
    }

    private String url(){return "jdbc:h2:mem:reparacion_"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1";}
    private int contar(Connection c,String tabla) throws SQLException {
        try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT COUNT(*) FROM "+tabla)){r.next();return r.getInt(1);}
    }
}

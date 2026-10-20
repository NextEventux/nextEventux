package config;

import java.sql.*;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ReparacionMainTest {
    /** Migraciones que DatabaseConnection registra en Esquema_version mediante ejecutarUnaVez (010 y 011). */
    private static final int MIGRACIONES_REGISTRADAS = 2;

    @TempDir Path carpeta;

    @Test void rutaAbsolutaConfigurableSinTocarLaBaseReal() {
        String anterior=System.getProperty("nexteventux.home");
        try {
            System.setProperty("nexteventux.home",carpeta.toString());
            assertEquals("jdbc:h2:"+carpeta.resolve("nexteventux").toString().replace('\\','/')+";DB_CLOSE_DELAY=-1",DatabaseConnection.urlPorDefecto());
        } finally {
            if(anterior==null)System.clearProperty("nexteventux.home");else System.setProperty("nexteventux.home",anterior);
        }
    }

    @Test void baseNuevaCargaEjemplosSinConfundirMetadatosConDatosDelUsuario() throws Exception {
        String url=url();
        try(Connection c=DatabaseConnection.getConnection(url)) {
            CargarDatosEjemplo.cargar(c);
            assertEquals(2,contar(c,"Evento"));
            assertEquals(MIGRACIONES_REGISTRADAS,contar(c,"Esquema_version"));
            assertThrows(SQLException.class,()->CargarDatosEjemplo.cargar(c));
        }
        DatabaseConnection.reiniciarInicializacion();
        try(Connection c=DatabaseConnection.getConnection(url)) {
            assertEquals(2,contar(c,"Evento"));
            assertEquals(MIGRACIONES_REGISTRADAS,contar(c,"Esquema_version"));
        }
    }

    @Test void reconstruyeUnaBaseBorradaConLaMismaUrl() throws Exception {
        String url=url();
        try(Connection c=DatabaseConnection.getConnection(url);Statement s=c.createStatement()) {s.execute("DROP ALL OBJECTS");}
        try(Connection c=DatabaseConnection.getConnection(url)) {
            assertEquals(0,contar(c,"Evento"));
            assertEquals(MIGRACIONES_REGISTRADAS,contar(c,"Esquema_version"));
        }
    }

    @Test void unicidadNoBorraOrganizadoresDuplicadosAlActualizar() throws Exception {
        String url=url();
        try(Connection c=DatabaseConnection.getConnection(url);Statement s=c.createStatement()) {
            CargarDatosEjemplo.cargar(c);
            // En H2 el índice único respalda la clave foránea Organizador -> Usuario: se quita primero esa restricción.
            s.execute("ALTER TABLE Organizador DROP CONSTRAINT "+claveForaneaQueUsaIndice(c));
            s.execute("DROP INDEX uq_organizador_usuario");
            s.executeUpdate("DELETE FROM Esquema_version WHERE nombre='010_organizador_usuario_unico.sql'");
            s.executeUpdate("INSERT INTO Organizador(id_usuario,estado) VALUES(1,'ACTIVO')");
        }
        DatabaseConnection.reiniciarInicializacion();
        SQLException error=assertThrows(SQLException.class,()->DatabaseConnection.getConnection(url));
        assertTrue(error.getMessage().contains("repetidos"));
        try(Connection c=DriverManager.getConnection(url,"sa","");Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT COUNT(*) FROM Organizador WHERE id_usuario=1")) {
            assertTrue(r.next());assertEquals(2,r.getInt(1));
            // La migración 010 queda sin registrar; la posterior (011) se conserva.
            assertEquals(MIGRACIONES_REGISTRADAS-1,contar(c,"Esquema_version"));
        }
    }

    private String claveForaneaQueUsaIndice(Connection c) throws SQLException {
        String sql="SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS "
                +"WHERE TABLE_NAME='ORGANIZADOR' AND CONSTRAINT_TYPE='FOREIGN KEY' AND INDEX_NAME='UQ_ORGANIZADOR_USUARIO'";
        try(Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)) {
            assertTrue(r.next(),"No se encontró la clave foránea que usa uq_organizador_usuario");
            String nombre=r.getString(1);
            assertFalse(r.next(),"Se esperaba una sola clave foránea sobre uq_organizador_usuario");
            return "\""+nombre+"\"";
        }
    }

    private String url(){return "jdbc:h2:mem:reparacion_"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1";}
    private int contar(Connection c,String tabla) throws SQLException {
        try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT COUNT(*) FROM "+tabla)){r.next();return r.getInt(1);}
    }
}

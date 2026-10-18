import config.CargarDatosEjemplo;
import config.DatabaseConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repositories.RepositoryCancelacion;
import repositories.RepositoryEvento;
import services.ServiceEventos;
import java.nio.file.Path;
import java.sql.*;
import static org.junit.jupiter.api.Assertions.*;

class CancelacionTest {
    @TempDir Path directorio;
    private String url;
    private ServiceEventos service;

    @BeforeEach void preparar() throws Exception {
        url="jdbc:h2:"+directorio.resolve("cancelacion");
        try(Connection connection=DatabaseConnection.getConnection(url)) { CargarDatosEjemplo.cargar(connection); }
        sql("INSERT INTO Disponibilidad_Servicio VALUES (1,1,DATE '2026-11-15',TIME '15:00:00',TIME '23:00:00','RESERVADO')");
        sql("INSERT INTO Disponibilidad_Servicio VALUES (2,3,DATE '2026-11-15',TIME '15:00:00',TIME '23:00:00','RESERVADO')");
        sql("UPDATE Contratacion SET id_disponibilidad=id_contratacion");
        sql("UPDATE Evento SET id_lugar=id_evento");
        sql("UPDATE Lugar SET estado='RESERVADO' WHERE id_lugar IN (1,2)");
        service=new ServiceEventos(new RepositoryEvento(url),new RepositoryCancelacion(url));
    }

    @Test void cancelaLiberaRevocaYNotificaSinCambiarOtrosEventos() throws Exception {
        service.cancelarEvento(1,1,true);
        assertEquals("CANCELADO",valor("SELECT estado FROM Evento WHERE id_evento=1"));
        assertEquals("DISPONIBLE",valor("SELECT estado FROM Disponibilidad_Servicio WHERE id_disponibilidad=1"));
        assertEquals("DISPONIBLE",valor("SELECT estado FROM Lugar WHERE id_lugar=1"));
        assertEquals("CANCELADA",valor("SELECT estado FROM Contratacion WHERE id_contratacion=1"));
        assertEquals("REVOCADA",valor("SELECT estado_respuesta FROM Invitacion WHERE id_invitacion=1"));
        assertEquals(4,service.listarAvisosCancelacion(1,1).size());
        assertEquals("PLANEADO",valor("SELECT estado FROM Evento WHERE id_evento=2"));
        assertEquals("RESERVADO",valor("SELECT estado FROM Lugar WHERE id_lugar=2"));
        assertEquals("ENVIADA",valor("SELECT estado_respuesta FROM Invitacion WHERE id_invitacion=3"));
        assertEquals("APROBADO",valor("SELECT estado FROM Proveedor WHERE id_proveedor=1"));
        var nueva=new RepositoryEvento(url);
        assertEquals("CANCELADO",nueva.listarEventos().get(0).getEstado());
    }

    @Test void exigeConfirmacionYPropiedad() throws Exception {
        assertThrows(IllegalArgumentException.class,()->service.cancelarEvento(1,1,false));
        assertThrows(IllegalArgumentException.class,()->service.cancelarEvento(1,2,true));
        assertThrows(IllegalArgumentException.class,()->service.cancelarEvento(999,1,true));
        comprobarOriginal();
    }

    @Test void bloqueaCanceladosYFinalizados() throws Exception {
        for(String estado:new String[]{"CANCELADO","FINALIZADO"}) {
            sql("UPDATE Evento SET estado='"+estado+"' WHERE id_evento=1");
            assertThrows(IllegalArgumentException.class,()->service.cancelarEvento(1,1,true));
            assertEquals(estado,valor("SELECT estado FROM Evento WHERE id_evento=1"));
        }
        assertTrue(service.listarAvisosCancelacion(1,1).isEmpty());
    }

    @Test void revierteTodasLasLiberacionesSiFallaUnServicio() throws Exception {
        sql("ALTER TABLE Disponibilidad_Servicio ADD CONSTRAINT fallo CHECK (id_disponibilidad<>2 OR estado<>'DISPONIBLE')");
        assertThrows(IllegalStateException.class,()->service.cancelarEvento(1,1,true));
        comprobarOriginal();
        sql("ALTER TABLE Disponibilidad_Servicio DROP CONSTRAINT fallo");
        service.cancelarEvento(1,1,true);
        assertEquals("CANCELADO",valor("SELECT estado FROM Evento WHERE id_evento=1"));
    }

    @Test void revierteTodoSiFallaLaNotificacionAUnInvitado() throws Exception {
        sql("ALTER TABLE Notificacion_interna ADD CONSTRAINT fallo CHECK (id_invitado IS NULL OR id_invitado<>2)");
        assertThrows(IllegalStateException.class,()->service.cancelarEvento(1,1,true));
        comprobarOriginal();
    }

    @Test void noLiberaReservasDeOtroEvento() throws Exception {
        sql("UPDATE Evento SET id_lugar=1 WHERE id_evento=2");
        sql("INSERT INTO Contratacion (id_propuesta,id_evento,fecha_contratacion,estado,id_disponibilidad) VALUES (3,2,CURRENT_TIMESTAMP,'ACTIVA',1)");
        service.cancelarEvento(1,1,true);
        assertEquals("RESERVADO",valor("SELECT estado FROM Lugar WHERE id_lugar=1"));
        assertEquals("RESERVADO",valor("SELECT estado FROM Disponibilidad_Servicio WHERE id_disponibilidad=1"));
    }

    @Test void noAdivinaUnaReservaQueNoEstaVinculada() throws Exception {
        sql("UPDATE Contratacion SET id_disponibilidad=NULL WHERE id_contratacion=1");
        assertThrows(IllegalArgumentException.class,()->service.cancelarEvento(1,1,true));
        comprobarOriginal();
    }

    @Test void noDuplicaNotificacionesAlCancelarDeNuevo() {
        service.cancelarEvento(1,1,true);
        assertThrows(IllegalArgumentException.class,()->service.cancelarEvento(1,1,true));
        assertEquals(4,service.listarAvisosCancelacion(1,1).size());
        assertTrue(service.listarAvisosCancelacion(1,2).isEmpty());
    }

    private void comprobarOriginal() throws Exception {
        assertEquals("PLANEADO",valor("SELECT estado FROM Evento WHERE id_evento=1"));
        assertEquals("RESERVADO",valor("SELECT estado FROM Disponibilidad_Servicio WHERE id_disponibilidad=1"));
        assertEquals("RESERVADO",valor("SELECT estado FROM Lugar WHERE id_lugar=1"));
        assertEquals("ACTIVA",valor("SELECT estado FROM Contratacion WHERE id_contratacion=1"));
        assertEquals("ENVIADA",valor("SELECT estado_respuesta FROM Invitacion WHERE id_invitacion=1"));
        assertTrue(service.listarAvisosCancelacion(1,1).isEmpty());
    }
    private void sql(String sql) throws Exception {
        try(Connection c=DatabaseConnection.getConnection(url);Statement s=c.createStatement()) { s.execute(sql); }
    }
    private String valor(String sql) throws Exception {
        try(Connection c=DatabaseConnection.getConnection(url);Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)) { r.next();return r.getString(1); }
    }
}

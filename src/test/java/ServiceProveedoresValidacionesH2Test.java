import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import config.DatabaseConnection;
import entities.Servicio;
import repositories.RepositoryDisponibilidadServicio;
import repositories.RepositoryServicio;
import services.ServiceProveedores;

/**
 * CU-11: validaciones al guardar y editar un servicio.
 * Se exigen aquí porque CU-12 y CU-13 usan el precio del servicio
 * para comprometer el presupuesto de un evento.
 */
public class ServiceProveedoresValidacionesH2Test {

    private static final String URL
            = "jdbc:h2:mem:cu11validaciones;DB_CLOSE_DELAY=-1";
    private static final int PROVEEDOR = 9401;

    private ServiceProveedores service;

    @BeforeEach
    void preparar() throws Exception {

        System.setProperty("nexteventux.db.url", URL);

        try (Connection c = DatabaseConnection.getConnection();
                Statement s = c.createStatement()) {

            s.executeUpdate("INSERT INTO Usuario (id_usuario, nombre, usuario, contraseña, estado, "
                    + "fecha_registro, acepta_marketing, tipo_usuario) VALUES "
                    + "(9401,'Proveedor','prov9401','x','ACTIVO',CURRENT_TIMESTAMP,FALSE,'PROVEEDOR')");
            s.executeUpdate("INSERT INTO Proveedor (id_proveedor, id_usuario, nombre, estado) "
                    + "VALUES (9401, 9401, 'Proveedor', 'APROBADO')");
        }

        service = new ServiceProveedores(
                new RepositoryServicio(),
                new RepositoryDisponibilidadServicio());
    }

    @AfterEach
    void limpiar() throws Exception {
        try {
            try (Connection c = DriverManager.getConnection(URL, "sa", "");
                    Statement s = c.createStatement()) {
                s.execute("DROP ALL OBJECTS");
            }
        } finally {
            System.clearProperty("nexteventux.db.url");
        }
    }

    private Servicio valido() {
        Servicio s = new Servicio();
        s.setIdProveedor(PROVEEDOR);
        s.setNombre("Fotografia");
        s.setTipo("Fotografia");
        s.setCategoria("Foto");
        s.setPrecio(500000);
        s.setCiudadZona("Bogota");
        s.setAnticipacionMinima(3);
        s.setCantidadMaximaPersonas(100);
        s.setDescripcion("Cobertura completa");
        return s;
    }

    private void rechaza(Servicio servicio, String fragmento) {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.crearServicio(servicio));
        assertTrue(error.getMessage().contains(fragmento),
                "Mensaje inesperado: " + error.getMessage());
        assertEquals(0, service.listarServiciosProveedor(PROVEEDOR).size());
    }

    @Test
    void guardaUnServicioValidoComoBorrador() {
        Servicio s = valido();
        service.crearServicio(s);
        assertEquals("BORRADOR", service.buscarServicio(s.getIdServicio()).getEstado());
    }

    @Test
    void rechazaPreciosInvalidos() {
        for (double precio : new double[]{-5, 0, Double.NaN,
            Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            Servicio s = valido();
            s.setPrecio(precio);
            assertThrows(IllegalArgumentException.class, () -> service.crearServicio(s),
                    "Precio " + precio);
        }

        Servicio decimales = valido();
        decimales.setPrecio(500.999);
        rechaza(decimales, "dos decimales");

        Servicio enorme = valido();
        enorme.setPrecio(1e15);
        rechaza(enorme, "supera");

        Servicio microscopico = valido();
        microscopico.setPrecio(0.001);
        rechaza(microscopico, "dos decimales");
    }

    @Test
    void aceptaPreciosConDosDecimalesOMenos() {
        for (double precio : new double[]{0.01, 1, 1000.5, 50000.99, 9999999999.99}) {
            Servicio s = valido();
            s.setNombre("Servicio " + precio);
            s.setPrecio(precio);
            service.crearServicio(s);
        }
        assertEquals(5, service.listarServiciosProveedor(PROVEEDOR).size());
    }

    @Test
    void rechazaAnticipacionYCapacidadInvalidas() {
        Servicio anticipacion = valido();
        anticipacion.setAnticipacionMinima(-1);
        rechaza(anticipacion, "anticipacion");

        Servicio cero = valido();
        cero.setCantidadMaximaPersonas(0);
        rechaza(cero, "cantidad maxima");

        Servicio negativa = valido();
        negativa.setCantidadMaximaPersonas(-7);
        rechaza(negativa, "cantidad maxima");
    }

    @Test
    void rechazaTextosVaciosOMuyLargos() {
        Servicio sinNombre = valido();
        sinNombre.setNombre("   ");
        rechaza(sinNombre, "nombre");

        Servicio sinTipo = valido();
        sinTipo.setTipo(null);
        rechaza(sinTipo, "tipo");

        Servicio sinCategoria = valido();
        sinCategoria.setCategoria("");
        rechaza(sinCategoria, "categoria");

        Servicio sinDescripcion = valido();
        sinDescripcion.setDescripcion(" ");
        rechaza(sinDescripcion, "descripcion");

        Servicio sinCiudad = valido();
        sinCiudad.setCiudadZona(null);
        rechaza(sinCiudad, "ciudad");

        Servicio nombreLargo = valido();
        nombreLargo.setNombre("x".repeat(151));
        rechaza(nombreLargo, "150");

        Servicio descripcionLarga = valido();
        descripcionLarga.setDescripcion("d".repeat(256));
        rechaza(descripcionLarga, "255");

        Servicio sinProveedor = valido();
        sinProveedor.setIdProveedor(0);
        rechaza(sinProveedor, "proveedor");
    }

    @Test
    void unServicioPublicadoNoSePuedeEditarConDatosInvalidos() {
        Servicio s = valido();
        service.crearServicio(s);
        service.registrarDisponibilidadServicio(PROVEEDOR, s.getIdServicio(),
                java.sql.Date.valueOf("2027-02-01"), "10:00 - 12:00");
        service.publicarServicio(PROVEEDOR, s.getIdServicio());

        Servicio negativo = valido();
        negativo.setPrecio(-5);
        assertThrows(IllegalArgumentException.class,
                () -> service.modificarServicio(PROVEEDOR, s.getIdServicio(), negativo));

        Servicio anticipacion = valido();
        anticipacion.setAnticipacionMinima(-9);
        assertThrows(IllegalArgumentException.class,
                () -> service.modificarServicio(PROVEEDOR, s.getIdServicio(), anticipacion));

        Servicio sinDescripcion = valido();
        sinDescripcion.setDescripcion("");
        assertThrows(IllegalArgumentException.class,
                () -> service.modificarServicio(PROVEEDOR, s.getIdServicio(), sinDescripcion));

        // Nada cambió y sigue publicado
        Servicio guardado = service.buscarServicio(s.getIdServicio());
        assertEquals(500000, guardado.getPrecio());
        assertEquals("DISPONIBLE", guardado.getEstado());

        // Una edición válida sí se guarda
        Servicio nuevo = valido();
        nuevo.setPrecio(650000.50);
        service.modificarServicio(PROVEEDOR, s.getIdServicio(), nuevo);
        assertEquals(650000.50, service.buscarServicio(s.getIdServicio()).getPrecio());
    }
}

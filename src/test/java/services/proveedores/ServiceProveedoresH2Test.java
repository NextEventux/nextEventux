package services.proveedores;

import entities.servicios.Servicio;
import entities.servicios.ImagenServicio;
import entities.servicios.PreguntaServicio;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.Date;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import persistencia.DatabaseConnection;
import persistencia.PropiedadSistemaTemporal;
import repositories.servicios.RepositoryDisponibilidadServicio;
import repositories.servicios.RepositoryServicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// estos repositorios usan nexteventux.db.url cuando no reciben una url
@ResourceLock(Resources.SYSTEM_PROPERTIES)
class ServiceProveedoresH2Test {

    private static final int PROVEEDOR = 101;
    private static final int ORGANIZADOR = 201;
    private static final int EVENTO = 301;

    private String url;
    private ServiceProveedores service;
    private PropiedadSistemaTemporal propiedadUrlBaseDatos;

    @BeforeEach
    void preparar() throws Exception {
        // la base vive entre conexiones del servicio y tiene un nombre único en memoria
        url = "jdbc:h2:mem:proveedores_" + UUID.randomUUID().toString().replace("-", "")
                + ";DB_CLOSE_DELAY=-1";
        // dejamos la propiedad apuntando a la h2 temporal para los repositorios sin url
        propiedadUrlBaseDatos = new PropiedadSistemaTemporal("nexteventux.db.url", url);

        // creamos el proveedor, el organizador y el evento que usan los servicios
        try (Connection connection = DatabaseConnection.getConnection(url);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO Usuario "
                    + "(id_usuario, nombre, usuario, contraseña, estado, fecha_registro, "
                    + "acepta_marketing, tipo_usuario) VALUES "
                    + "(101, 'Proveedor de prueba', 'proveedor.test', 'x', 'ACTIVO', "
                    + "CURRENT_TIMESTAMP, FALSE, 'PROVEEDOR')");
            statement.executeUpdate("INSERT INTO Usuario "
                    + "(id_usuario, nombre, usuario, contraseña, estado, fecha_registro, "
                    + "acepta_marketing, tipo_usuario) VALUES "
                    + "(201, 'Organizador de prueba', 'organizador.test', 'x', 'ACTIVO', "
                    + "CURRENT_TIMESTAMP, FALSE, 'ORGANIZADOR')");
            statement.executeUpdate("INSERT INTO Proveedor "
                    + "(id_proveedor, id_usuario, nombre, estado) VALUES "
                    + "(" + PROVEEDOR + ", 101, 'Proveedor de prueba', 'APROBADO')");
            statement.executeUpdate("INSERT INTO Organizador "
                    + "(id_organizador, id_usuario, empresa, estado) VALUES "
                    + "(" + ORGANIZADOR + ", 201, 'Organizador de prueba', 'ACTIVO')");
            statement.executeUpdate("INSERT INTO Evento "
                    + "(id_evento, id_organizador, nombre, fecha_inicio, fecha_fin, estado, fecha_creacion) "
                    + "VALUES (" + EVENTO + ", " + ORGANIZADOR + ", 'Evento de prueba', "
                    + "TIMESTAMP '2027-01-15 10:00:00', TIMESTAMP '2027-01-15 18:00:00', "
                    + "'PLANEADO', CURRENT_TIMESTAMP)");
        }

        service = new ServiceProveedores(
                new RepositoryServicio(),
                new RepositoryDisponibilidadServicio());
    }

    @AfterEach
    void cerrarBase() throws Exception {
        try {
            if (url != null) {
                // usamos jdbc directo para apagar esta base temporal sin volver a inicializarla
                try (Connection connection = DriverManager.getConnection(url, "sa", "");
                        Statement statement = connection.createStatement()) {
                    statement.execute("SHUTDOWN");
                }
            }
        } finally {
            if (propiedadUrlBaseDatos != null) {
                // restauramos la propiedad para que otras pruebas no hereden esta url
                propiedadUrlBaseDatos.restaurar();
            }
        }
    }

    @Test
    void creaYModificaServicioPersistiendoEstadoYProveedorOriginal() {
        Servicio datos = servicioValido();
        datos.setEstado(null);

        service.crearServicio(datos);

        assertTrue(datos.getIdServicio() > 0);
        Servicio guardado = service.buscarServicio(datos.getIdServicio());
        assertNotNull(guardado);
        assertEquals("BORRADOR", guardado.getEstado());

        Servicio estadoEnBlanco = servicioValido();
        estadoEnBlanco.setEstado("  ");
        service.crearServicio(estadoEnBlanco);
        assertEquals("BORRADOR",
                service.buscarServicio(estadoEnBlanco.getIdServicio()).getEstado());

        Servicio estadoExplicito = servicioValido();
        estadoExplicito.setEstado("BORRADOR");
        service.crearServicio(estadoExplicito);
        assertEquals("BORRADOR",
                service.buscarServicio(estadoExplicito.getIdServicio()).getEstado());

        Servicio edicion = servicioValido();
        edicion.setIdProveedor(999);
        edicion.setNombre("Fotografía editada");
        edicion.setEstado(null);
        service.modificarServicio(PROVEEDOR, datos.getIdServicio(), edicion);

        Servicio actualizado = service.buscarServicio(datos.getIdServicio());
        assertEquals("Fotografía editada", actualizado.getNombre());
        assertEquals(PROVEEDOR, actualizado.getIdProveedor());
        assertEquals("BORRADOR", actualizado.getEstado());

        assertThrows(IllegalArgumentException.class,
                () -> service.modificarServicio(
                        PROVEEDOR, datos.getIdServicio(), null));
        assertEquals("Fotografía editada",
                service.buscarServicio(datos.getIdServicio()).getNombre());

        Servicio estadoEdicionEnBlanco = servicioValido();
        estadoEdicionEnBlanco.setNombre("Fotografía sin cambio de estado");
        estadoEdicionEnBlanco.setEstado("  ");
        service.modificarServicio(
                PROVEEDOR, datos.getIdServicio(), estadoEdicionEnBlanco);
        assertEquals("BORRADOR",
                service.buscarServicio(datos.getIdServicio()).getEstado());

        Servicio estadoEdicionExplicito = servicioValido();
        estadoEdicionExplicito.setEstado("BORRADOR");
        service.modificarServicio(
                PROVEEDOR, datos.getIdServicio(), estadoEdicionExplicito);
        assertEquals("BORRADOR",
                service.buscarServicio(datos.getIdServicio()).getEstado());
    }

    @Test
    void rechazaDatosInvalidosSinGuardarElServicio() {
        assertThrows(IllegalArgumentException.class, () -> service.crearServicio(null));

        Servicio proveedorInvalido = servicioValido();
        proveedorInvalido.setIdProveedor(0);
        assertThrows(IllegalArgumentException.class,
                () -> service.crearServicio(proveedorInvalido));

        Servicio precioInvalido = servicioValido();
        precioInvalido.setPrecio(0);
        assertThrows(IllegalArgumentException.class, () -> service.crearServicio(precioInvalido));

        Servicio precioNoNumerico = servicioValido();
        precioNoNumerico.setPrecio(Double.NaN);
        assertThrows(IllegalArgumentException.class,
                () -> service.crearServicio(precioNoNumerico));

        Servicio precioInfinito = servicioValido();
        precioInfinito.setPrecio(Double.POSITIVE_INFINITY);
        assertThrows(IllegalArgumentException.class,
                () -> service.crearServicio(precioInfinito));

        Servicio precioSobreElMaximo = servicioValido();
        precioSobreElMaximo.setPrecio(10_000_000_000d);
        assertThrows(IllegalArgumentException.class,
                () -> service.crearServicio(precioSobreElMaximo));

        Servicio capacidadInvalida = servicioValido();
        capacidadInvalida.setCantidadMaximaPersonas(0);
        assertThrows(IllegalArgumentException.class, () -> service.crearServicio(capacidadInvalida));

        Servicio nombreInvalido = servicioValido();
        nombreInvalido.setNombre(" ");
        assertThrows(IllegalArgumentException.class, () -> service.crearServicio(nombreInvalido));

        Servicio nombreNulo = servicioValido();
        nombreNulo.setNombre(null);
        assertThrows(IllegalArgumentException.class,
                () -> service.crearServicio(nombreNulo));

        Servicio categoriaVacia = servicioValido();
        categoriaVacia.setCategoria(" ");
        IllegalArgumentException errorCategoria = assertThrows(
                IllegalArgumentException.class,
                () -> service.crearServicio(categoriaVacia));
        assertTrue(errorCategoria.getMessage().contains("La categoria es obligatoria"));

        assertTrue(service.listarServiciosProveedor(PROVEEDOR).isEmpty());
    }

    @Test
    void rechazaLimitesCompuestosDelServicioSinGuardarDatosInvalidos() throws Exception {
        int serviciosAntes = contar(
                "SELECT COUNT(*) FROM Servicio WHERE id_proveedor = " + PROVEEDOR);

        Servicio precioConDecimalesExtra = servicioValido();
        precioConDecimalesExtra.setPrecio(500000.125);
        IllegalArgumentException errorPrecio = assertThrows(
                IllegalArgumentException.class,
                () -> service.crearServicio(precioConDecimalesExtra));
        assertTrue(errorPrecio.getMessage().contains("mas de dos decimales"));

        Servicio tipoExcesivo = servicioValido();
        tipoExcesivo.setTipo("T".repeat(101));
        IllegalArgumentException errorTipo = assertThrows(
                IllegalArgumentException.class,
                () -> service.crearServicio(tipoExcesivo));
        assertTrue(errorTipo.getMessage().contains("no puede superar los 100 caracteres"));

        Servicio descripcionExcesiva = servicioValido();
        descripcionExcesiva.setDescripcion("D".repeat(256));
        IllegalArgumentException errorDescripcion = assertThrows(
                IllegalArgumentException.class,
                () -> service.crearServicio(descripcionExcesiva));
        assertTrue(errorDescripcion.getMessage().contains("no puede superar los 255 caracteres"));

        Servicio anticipacionNegativa = servicioValido();
        anticipacionNegativa.setAnticipacionMinima(-1);
        IllegalArgumentException errorAnticipacion = assertThrows(
                IllegalArgumentException.class,
                () -> service.crearServicio(anticipacionNegativa));
        assertTrue(errorAnticipacion.getMessage().contains("no puede ser negativa"));

        assertEquals(serviciosAntes, contar(
                "SELECT COUNT(*) FROM Servicio WHERE id_proveedor = " + PROVEEDOR));
    }

    @Test
    void rechazaRegistrarDisponibilidadSinFecha() {
        Servicio servicio = crearServicio();

        assertThrows(IllegalArgumentException.class,
                () -> service.registrarDisponibilidadServicio(PROVEEDOR, servicio.getIdServicio(), null,
                        "09:00 - 12:00"));
        assertTrue(service.listarDisponibilidadesServicio(servicio.getIdServicio()).isEmpty());
    }

    @Test
    void rechazaDisponibilidadQueSeCruzaConUnaContratacionActiva() throws Exception {
        Servicio servicio = crearServicio();
        Date fechaEvento = Date.valueOf(LocalDate.of(2027, 1, 15));
        ejecutar("INSERT INTO Solicitud_Ejecucion "
                + "(id_solicitud, id_proveedor, id_evento, fecha_solicitud, estado, id_servicio) "
                + "VALUES (401, " + PROVEEDOR + ", " + EVENTO
                + ", CURRENT_TIMESTAMP, 'CONTRATADA', " + servicio.getIdServicio() + ")");
        ejecutar("INSERT INTO Propuesta "
                + "(id_propuesta, id_solicitud, valor, descripcion, estado) "
                + "VALUES (501, 401, 500.00, 'Propuesta aceptada', 'ACEPTADA')");
        ejecutar("INSERT INTO Contratacion "
                + "(id_contratacion, id_propuesta, id_evento, fecha_contratacion, estado, "
                + "condiciones, valor_acordado, fecha_servicio, hora_inicio, hora_fin) "
                + "VALUES (601, 501, " + EVENTO
                + ", CURRENT_TIMESTAMP, 'ACTIVA', 'Servicio reservado', 500.00, "
                + "DATE '2027-01-15', TIME '10:00:00', TIME '12:00:00')");

        assertEquals(1, contar("SELECT COUNT(*) FROM Contratacion c "
                + "JOIN Propuesta p ON p.id_propuesta = c.id_propuesta "
                + "JOIN Solicitud_Ejecucion s ON s.id_solicitud = p.id_solicitud "
                + "WHERE s.id_servicio = " + servicio.getIdServicio()
                + " AND c.estado = 'ACTIVA' AND c.fecha_servicio = DATE '2027-01-15' "
                + "AND c.hora_inicio = TIME '10:00:00' AND c.hora_fin = TIME '12:00:00'"));

        assertThrows(IllegalArgumentException.class,
                () -> service.registrarDisponibilidadServicio(
                        PROVEEDOR, servicio.getIdServicio(), fechaEvento, "11:00 - 13:00"));

        assertTrue(service.listarDisponibilidadesServicio(servicio.getIdServicio()).isEmpty());
        assertEquals(1, contar("SELECT COUNT(*) FROM Contratacion "
                + "WHERE id_contratacion = 601 AND estado = 'ACTIVA'"));
    }

    @Test
    void registraDisponibilidadYRechazaCrucesDeHorario() {
        Servicio servicio = crearServicio();
        Date fecha = Date.valueOf(LocalDate.now().plusDays(10));
        service.registrarDisponibilidadServicio(
                PROVEEDOR, servicio.getIdServicio(), fecha, "09:00 - 12:00");
        assertThrows(IllegalArgumentException.class,
                () -> service.registrarDisponibilidadServicio(
                        PROVEEDOR, servicio.getIdServicio(), fecha, "11:00 - 13:00"));

        assertEquals(1, service.listarDisponibilidadesServicio(servicio.getIdServicio()).size());
        service.eliminarDisponibilidad(PROVEEDOR, servicio.getIdServicio(),
                service.listarDisponibilidadesServicio(servicio.getIdServicio()).get(0).getIdDisponibilidad());
        assertTrue(service.listarDisponibilidadesServicio(servicio.getIdServicio()).isEmpty());
    }

    @Test
    void publicaServicioConCapacidadMaximaOpcionalNula() {
        Servicio servicio = servicioValido();
        servicio.setCantidadMaximaPersonas(null);
        service.crearServicio(servicio);

        assertNull(service.buscarServicio(servicio.getIdServicio())
                .getCantidadMaximaPersonas());

        service.registrarDisponibilidadServicio(
                PROVEEDOR,
                servicio.getIdServicio(),
                Date.valueOf(LocalDate.now().plusDays(20)),
                "09:00 - 10:00");
        service.publicarServicio(PROVEEDOR, servicio.getIdServicio());

        Servicio publicado = service.buscarServicio(servicio.getIdServicio());
        assertEquals("DISPONIBLE", publicado.getEstado());
        assertNull(publicado.getCantidadMaximaPersonas());
    }

    @Test
    void publicaSoloTrasRegistrarDisponibilidad() throws Exception {
        Servicio servicio = crearServicio();

        assertThrows(IllegalArgumentException.class,
                () -> service.publicarServicio(PROVEEDOR, servicio.getIdServicio()));
        assertEquals("BORRADOR", service.buscarServicio(servicio.getIdServicio()).getEstado());

        service.registrarDisponibilidadServicio(
                PROVEEDOR,
                servicio.getIdServicio(),
                Date.valueOf(LocalDate.now().plusDays(12)),
                "09:00 - 13:00");
        // simulamos un dato vacío ya persistido para comprobar la validación al publicar
        ejecutar("UPDATE Servicio SET descripcion = ' ' WHERE id_servicio = "
                + servicio.getIdServicio());
        assertThrows(IllegalArgumentException.class,
                () -> service.publicarServicio(PROVEEDOR, servicio.getIdServicio()));
        assertEquals("BORRADOR", service.buscarServicio(servicio.getIdServicio()).getEstado());
        ejecutar("UPDATE Servicio SET descripcion = 'Cobertura fotográfica de eventos' "
                + "WHERE id_servicio = " + servicio.getIdServicio());
        service.publicarServicio(PROVEEDOR, servicio.getIdServicio());

        assertEquals("DISPONIBLE", service.buscarServicio(servicio.getIdServicio()).getEstado());
        boolean servicioPublicado = false;
        for (Servicio disponible : service.listarServiciosDisponibles()) {
            if (disponible.getIdServicio() == servicio.getIdServicio()) {
                servicioPublicado = true;
                break;
            }
        }
        assertTrue(servicioPublicado);
    }

    @Test
    void publicarServicioRechazaDatosInvalidosPersistidos() throws Exception {
        assertPublicacionRechazada("nombre = ' '", "El nombre es obligatorio", PROVEEDOR);
        assertPublicacionRechazada("tipo = NULL", "El tipo es obligatorio", PROVEEDOR);
        assertPublicacionRechazada("tipo = ' '", "El tipo es obligatorio", PROVEEDOR);
        assertPublicacionRechazada("descripcion = NULL", "La descripcion es obligatoria", PROVEEDOR);
        assertPublicacionRechazada("descripcion = ' '", "La descripcion es obligatoria", PROVEEDOR);
        assertPublicacionRechazada("precio = 0", "El precio debe ser mayor que cero", PROVEEDOR);
        assertPublicacionRechazada("categoria = NULL", "La categoria es obligatoria", PROVEEDOR);
        assertPublicacionRechazada("categoria = ' '", "La categoria es obligatoria", PROVEEDOR);
        assertPublicacionRechazada("ciudad_zona = NULL", "La ciudad o zona es obligatoria", PROVEEDOR);
        assertPublicacionRechazada("ciudad_zona = ' '", "La ciudad o zona es obligatoria", PROVEEDOR);
        assertPublicacionRechazada("anticipacion_minima = -1",
                "La anticipacion minima no puede ser negativa", PROVEEDOR);
        assertPublicacionRechazada("cantidad_maxima_personas = 0",
                "La cantidad maxima debe ser mayor que cero", PROVEEDOR);

        ejecutar("INSERT INTO Usuario "
                + "(id_usuario, nombre, usuario, contraseña, estado, fecha_registro, "
                + "acepta_marketing, tipo_usuario) VALUES "
                + "(0, 'Proveedor cero', 'proveedor.cero', 'x', 'ACTIVO', "
                + "CURRENT_TIMESTAMP, FALSE, 'PROVEEDOR')");
        ejecutar("INSERT INTO Proveedor "
                + "(id_proveedor, id_usuario, nombre, estado) VALUES "
                + "(0, 0, 'Proveedor cero', 'APROBADO')");
        assertPublicacionRechazada(
                "id_proveedor = 0", "El proveedor es obligatorio", 0);
    }

    @Test
    void protegeServiciosAjenoAlModificarYAgregarDisponibilidad() {
        Servicio servicio = crearServicio();

        assertThrows(IllegalStateException.class,
                () -> service.modificarServicio(999, servicio.getIdServicio(), servicioValido()));
        assertThrows(IllegalStateException.class,
                () -> service.registrarDisponibilidadServicio(
                        999, servicio.getIdServicio(), Date.valueOf(LocalDate.now().plusDays(10)),
                        "09:00 - 12:00"));
        assertEquals("Fotografía", service.buscarServicio(servicio.getIdServicio()).getNombre());
        assertTrue(service.listarDisponibilidadesServicio(servicio.getIdServicio()).isEmpty());
    }

    @Test
    void rechazaModificarUnServicioInexistenteSinCambiarLaPersistencia() throws Exception {
        int serviciosAntes = contar("SELECT COUNT(*) FROM Servicio");

        assertThrows(IllegalArgumentException.class,
                () -> service.modificarServicio(PROVEEDOR, 99999, servicioValido()));

        assertEquals(serviciosAntes, contar("SELECT COUNT(*) FROM Servicio"));
        assertTrue(service.listarServiciosProveedor(PROVEEDOR).isEmpty());
    }

    @Test
    void asignaOrdenAImagenesYValidaAlProveedorAntesDeEliminar() {
        Servicio servicio = crearServicio();

        assertThrows(IllegalArgumentException.class,
                () -> service.agregarImagenServicio(
                        PROVEEDOR, servicio.getIdServicio(), null));
        assertThrows(IllegalArgumentException.class,
                () -> service.agregarImagenServicio(PROVEEDOR, servicio.getIdServicio(), " "));
        assertTrue(service.listarImagenesServicio(servicio.getIdServicio()).isEmpty());
        service.agregarImagenServicio(PROVEEDOR, servicio.getIdServicio(), "https://ejemplo.test/uno.jpg");
        service.agregarImagenServicio(PROVEEDOR, servicio.getIdServicio(), "https://ejemplo.test/dos.jpg");

        List<ImagenServicio> imagenes = service.listarImagenesServicio(servicio.getIdServicio());
        assertEquals(2, imagenes.size());
        assertEquals(1, imagenes.get(0).getOrdenImagen());
        assertEquals(2, imagenes.get(1).getOrdenImagen());
        assertThrows(IllegalArgumentException.class,
                () -> service.eliminarImagenServicio(PROVEEDOR, 99999));
        assertThrows(IllegalStateException.class,
                () -> service.eliminarImagenServicio(999, imagenes.get(0).getIdImagen()));

        service.eliminarImagenServicio(PROVEEDOR, imagenes.get(0).getIdImagen());
        assertEquals(1, service.listarImagenesServicio(servicio.getIdServicio()).size());
    }

    @Test
    void preguntaPublicaSeRespondeYPersisteElCambioDeEstado() {
        Servicio servicio = crearServicio();

        assertThrows(IllegalArgumentException.class,
                () -> service.formularPreguntaPublica(
                        99999, ORGANIZADOR, "¿Está disponible?"));
        assertThrows(IllegalStateException.class,
                () -> service.formularPreguntaPublica(servicio.getIdServicio(), ORGANIZADOR, "¿Está disponible?"));
        service.registrarDisponibilidadServicio(
                PROVEEDOR,
                servicio.getIdServicio(),
                Date.valueOf(LocalDate.now().plusDays(14)),
                "09:00 - 12:00");
        service.publicarServicio(PROVEEDOR, servicio.getIdServicio());

        assertThrows(IllegalArgumentException.class,
                () -> service.formularPreguntaPublica(
                        servicio.getIdServicio(), ORGANIZADOR, null));
        assertThrows(IllegalArgumentException.class,
                () -> service.formularPreguntaPublica(
                        servicio.getIdServicio(), ORGANIZADOR, "  "));
        assertTrue(service.listarPreguntasPublicas(servicio.getIdServicio()).isEmpty());

        service.formularPreguntaPublica(servicio.getIdServicio(), ORGANIZADOR, "  ¿Incluye montaje?  ");
        PreguntaServicio pregunta = service.listarPreguntasPublicas(servicio.getIdServicio()).get(0);
        assertEquals("¿Incluye montaje?", pregunta.getPregunta());
        assertEquals("PENDIENTE", pregunta.getEstado());

        assertThrows(IllegalArgumentException.class,
                () -> service.responderPreguntaPublica(
                        PROVEEDOR, 99999, "Respuesta válida"));
        assertThrows(IllegalArgumentException.class,
                () -> service.responderPreguntaPublica(
                        PROVEEDOR, pregunta.getIdPregunta(), null));
        assertThrows(IllegalArgumentException.class,
                () -> service.responderPreguntaPublica(PROVEEDOR, pregunta.getIdPregunta(), " "));
        PreguntaServicio pendiente = service.listarPreguntasPublicas(
                servicio.getIdServicio()).get(0);
        assertEquals("PENDIENTE", pendiente.getEstado());
        assertNull(pendiente.getRespuesta());
        assertThrows(IllegalStateException.class,
                () -> service.responderPreguntaPublica(999, pregunta.getIdPregunta(), "Sí"));
        service.responderPreguntaPublica(PROVEEDOR, pregunta.getIdPregunta(), "  Sí, está incluido.  ");

        PreguntaServicio respondida = service.listarPreguntasPublicas(servicio.getIdServicio()).get(0);
        assertEquals("Sí, está incluido.", respondida.getRespuesta());
        assertEquals("RESPONDIDA", respondida.getEstado());
        assertNotNull(respondida.getFechaRespuesta());
        assertThrows(IllegalStateException.class,
                () -> service.responderPreguntaPublica(PROVEEDOR, pregunta.getIdPregunta(), "Otra respuesta"));
    }

    @Test
    void noRetiraServicioConSolicitudPendienteNiContratoActivo() throws Exception {
        Servicio servicio = crearServicio();
        ejecutar("INSERT INTO Solicitud_Ejecucion "
                + "(id_solicitud, id_proveedor, id_evento, fecha_solicitud, estado, id_servicio) "
                + "VALUES (401, " + PROVEEDOR + ", " + EVENTO
                + ", CURRENT_TIMESTAMP, 'PENDIENTE', " + servicio.getIdServicio() + ")");

        assertThrows(IllegalStateException.class,
                () -> service.retirarServicio(PROVEEDOR, servicio.getIdServicio()));

        ejecutar("UPDATE Solicitud_Ejecucion SET estado = 'ACEPTADA' WHERE id_solicitud = 401");
        ejecutar("INSERT INTO Propuesta (id_propuesta, id_solicitud, valor, descripcion, estado) "
                + "VALUES (501, 401, 500.00, 'Propuesta aceptada', 'ACEPTADA')");
        ejecutar("INSERT INTO Contratacion "
                + "(id_contratacion, id_propuesta, id_evento, fecha_contratacion, estado, condiciones, valor_acordado) "
                + "VALUES (601, 501, " + EVENTO
                + ", CURRENT_TIMESTAMP, 'ACTIVA', 'Servicio reservado', 500.00)");

        assertThrows(IllegalStateException.class,
                () -> service.retirarServicio(PROVEEDOR, servicio.getIdServicio()));

        ejecutar("UPDATE Contratacion SET estado = 'FINALIZADA' WHERE id_contratacion = 601");
        service.retirarServicio(PROVEEDOR, servicio.getIdServicio());
        assertEquals("RETIRADO", service.buscarServicio(servicio.getIdServicio()).getEstado());
    }

    private Servicio crearServicio() {
        Servicio servicio = servicioValido();
        service.crearServicio(servicio);
        return servicio;
    }

    private void assertPublicacionRechazada(
            String actualizacion,
            String mensajeEsperado,
            int idProveedor) throws Exception {
        Servicio servicio = crearServicio();
        ejecutar("UPDATE Servicio SET " + actualizacion
                + " WHERE id_servicio = " + servicio.getIdServicio());

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.publicarServicio(idProveedor, servicio.getIdServicio()));

        assertTrue(error.getMessage().contains(mensajeEsperado));
        assertEquals("BORRADOR",
                service.buscarServicio(servicio.getIdServicio()).getEstado());
    }

    private Servicio servicioValido() {
        Servicio servicio = new Servicio();
        servicio.setIdProveedor(PROVEEDOR);
        servicio.setNombre("Fotografía");
        servicio.setTipo("Cobertura");
        servicio.setCategoria("FOTOGRAFIA");
        servicio.setDescripcion("Cobertura fotográfica de eventos");
        servicio.setPrecio(500000.50);
        servicio.setCiudadZona("Bogotá");
        servicio.setAnticipacionMinima(3);
        servicio.setCantidadMaximaPersonas(100);
        return servicio;
    }

    private void ejecutar(String sql) throws Exception {
        // preparamos solicitudes y contratos para probar el retiro del servicio
        try (Connection connection = DatabaseConnection.getConnection(url);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private int contar(String sql) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection(url);
                Statement statement = connection.createStatement();
                ResultSet resultado = statement.executeQuery(sql)) {
            assertTrue(resultado.next());
            return resultado.getInt(1);
        }
    }
}

package services;

import java.util.Date;
import java.util.List;

import entities.DisponibilidadServicio;
import entities.ImagenServicio;
import entities.PreguntaServicio;
import entities.Servicio;
import repositories.RepositoryDisponibilidadServicio;
import repositories.RepositoryImagenServicio;
import repositories.RepositoryPreguntaServicio;
import repositories.RepositoryServicio;

// Aqui va la logica relacionada con las acciones del modulo de proveedores
public class ServiceProveedores {

    private RepositoryServicio repositoryServicio;
    private RepositoryDisponibilidadServicio repositoryDisponibilidadServicio;
    private RepositoryImagenServicio repositoryImagenServicio;
    private RepositoryPreguntaServicio repositoryPreguntaServicio;

    public ServiceProveedores(
            RepositoryServicio repositoryServicio,
            RepositoryDisponibilidadServicio repositoryDisponibilidadServicio) {

        this(
                repositoryServicio,
                repositoryDisponibilidadServicio,
                new RepositoryImagenServicio(),
                new RepositoryPreguntaServicio());
    }

    public ServiceProveedores(
            RepositoryServicio repositoryServicio,
            RepositoryDisponibilidadServicio repositoryDisponibilidadServicio,
            RepositoryImagenServicio repositoryImagenServicio,
            RepositoryPreguntaServicio repositoryPreguntaServicio) {

        this.repositoryServicio = repositoryServicio;
        this.repositoryDisponibilidadServicio
                = repositoryDisponibilidadServicio;
        this.repositoryImagenServicio
                = repositoryImagenServicio;
        this.repositoryPreguntaServicio
                = repositoryPreguntaServicio;
    }

    public void crearServicio(Servicio datos) {

        if (datos == null) {
            throw new IllegalArgumentException(
                    "Los datos del servicio son obligatorios.");
        }

        // El precio y las condiciones se reutilizan al contratar (CU-13),
        // por eso se exigen validos desde el primer guardado.
        validarDatosServicio(datos);

        if (datos.getEstado() == null
                || datos.getEstado().isBlank()) {
            datos.setEstado("BORRADOR");
        }

        repositoryServicio.guardarServicio(datos);
    }

    public void modificarServicio(
            int idProveedor,
            int idServicio,
            Servicio datos) {

        validarServicioDelProveedor(
                idServicio,
                idProveedor);

        if (datos == null) {
            throw new IllegalArgumentException(
                    "Los datos del servicio son obligatorios.");
        }

        // Un servicio ya publicado tambien debe conservar datos validos
        validarDatosServicio(datos);

        Servicio existente
                = repositoryServicio.buscarServicio(idServicio);

        datos.setIdServicio(idServicio);

        // Conservamos el proveedor original del servicio
        datos.setIdProveedor(existente.getIdProveedor());

        if (datos.getEstado() == null
                || datos.getEstado().isBlank()) {
            datos.setEstado(existente.getEstado());
        }

        repositoryServicio.actualizarServicio(datos);
    }

    public void registrarDisponibilidadServicio(
            int idProveedor,
            int idServicio,
            Date fecha,
            String horario) {

        validarServicioDelProveedor(
                idServicio,
                idProveedor);

        if (fecha == null) {
            throw new IllegalArgumentException(
                    "La fecha es obligatoria.");
        }

        if (repositoryDisponibilidadServicio
                .existeCruceContratacion(
                        idServicio,
                        fecha,
                        horario)) {

            throw new IllegalArgumentException(
                    "Ese horario ya esta ocupado por una contratacion.");
        }

        if (repositoryDisponibilidadServicio
                .existeCruceDisponibilidad(
                        idServicio,
                        fecha,
                        horario)) {

            throw new IllegalArgumentException(
                    "Ese horario se cruza con otra disponibilidad.");
        }

        DisponibilidadServicio disponibilidad
                = new DisponibilidadServicio();

        disponibilidad.setIdServicio(idServicio);
        disponibilidad.setFecha(fecha);
        disponibilidad.setHorario(horario);

        repositoryDisponibilidadServicio
                .guardarDisponibilidad(disponibilidad);
    }

    public void publicarServicio(
            int idProveedor,
            int idServicio) {

        validarServicioDelProveedor(
                idServicio,
                idProveedor);

        Servicio servicio
                = repositoryServicio.buscarServicio(idServicio);

        validarServicioParaPublicar(servicio);

        if (repositoryDisponibilidadServicio
                .contarDisponibilidades(idServicio) == 0) {

            throw new IllegalArgumentException(
                    "Debe registrar al menos una disponibilidad.");
        }

        repositoryServicio.actualizarEstado(
                idServicio,
                "DISPONIBLE");
    }

    public void retirarServicio(
            int idProveedor,
            int idServicio) {

        validarServicioDelProveedor(
                idServicio,
                idProveedor);

        if (repositoryServicio
                .tieneSolicitudesPendientes(idServicio)) {

            throw new IllegalStateException(
                    "No se puede retirar el servicio porque tiene solicitudes pendientes.");
        }

        // CU-13 reserva el horario del proveedor: un servicio con
        // contrataciones vigentes no puede desaparecer del catalogo.
        if (repositoryServicio
                .tieneContratacionesActivas(idServicio)) {

            throw new IllegalStateException(
                    "No se puede retirar el servicio porque tiene contrataciones vigentes.");
        }

        repositoryServicio.actualizarEstado(
                idServicio,
                "RETIRADO");
    }

    public Servicio buscarServicio(int idServicio) {
        return repositoryServicio.buscarServicio(idServicio);
    }

    public List<Servicio> listarServiciosDisponibles() {

        return repositoryServicio
                .listarServiciosDisponibles();
    }

    public List<Servicio> listarServiciosProveedor(
            int idProveedor) {

        return repositoryServicio
                .listarServiciosProveedor(idProveedor);
    }

    public List<DisponibilidadServicio>
            listarDisponibilidadesServicio(
                    int idServicio) {

        return repositoryDisponibilidadServicio
                .listarDisponibilidadesServicio(idServicio);
    }

    public void eliminarDisponibilidad(
            int idProveedor,
            int idServicio,
            int idDisponibilidad) {

        validarServicioDelProveedor(
                idServicio,
                idProveedor);

        repositoryDisponibilidadServicio
                .eliminarDisponibilidad(
                        idServicio,
                        idDisponibilidad);
    }

    public void agregarImagenServicio(
            int idProveedor,
            int idServicio,
            String ruta) {

        validarServicioDelProveedor(
                idServicio,
                idProveedor);

        if (ruta == null || ruta.isBlank()) {
            throw new IllegalArgumentException(
                    "La ruta de la imagen es obligatoria.");
        }

        ImagenServicio imagen
                = new ImagenServicio();

        imagen.setIdServicio(idServicio);
        imagen.setRuta(ruta);

        int orden
                = repositoryImagenServicio
                        .listarImagenesServicio(idServicio)
                        .size() + 1;

        imagen.setOrdenImagen(orden);

        repositoryImagenServicio
                .guardarImagen(imagen);
    }

    public void eliminarImagenServicio(
            int idProveedor,
            int idImagen) {

        ImagenServicio imagen
                = repositoryImagenServicio
                        .buscarImagen(idImagen);

        if (imagen == null) {
            throw new IllegalArgumentException(
                    "La imagen no existe.");
        }

        validarServicioDelProveedor(
                imagen.getIdServicio(),
                idProveedor);

        repositoryImagenServicio
                .eliminarImagen(idImagen);
    }

    public List<ImagenServicio> listarImagenesServicio(
            int idServicio) {

        return repositoryImagenServicio
                .listarImagenesServicio(idServicio);
    }

    public void formularPreguntaPublica(
            int idServicio,
            int idOrganizador,
            String texto) {

        Servicio servicio
                = repositoryServicio
                        .buscarServicio(idServicio);

        if (servicio == null) {
            throw new IllegalArgumentException(
                    "El servicio no existe.");
        }

        if (!"DISPONIBLE".equalsIgnoreCase(
                servicio.getEstado())) {

            throw new IllegalStateException(
                    "Solo se pueden hacer preguntas sobre servicios publicados.");
        }

        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException(
                    "La pregunta es obligatoria.");
        }

        PreguntaServicio pregunta
                = new PreguntaServicio();

        pregunta.setIdServicio(idServicio);
        pregunta.setIdOrganizador(idOrganizador);
        pregunta.setPregunta(texto.trim());
        pregunta.setFechaPregunta(
                java.time.LocalDateTime.now());
        pregunta.setEstado("PENDIENTE");

        repositoryPreguntaServicio
                .guardarPregunta(pregunta);
    }

    public void responderPreguntaPublica(
            int idProveedor,
            int idPregunta,
            String respuesta) {

        PreguntaServicio pregunta
                = repositoryPreguntaServicio
                        .buscarPregunta(idPregunta);

        if (pregunta == null) {
            throw new IllegalArgumentException(
                    "La pregunta no existe.");
        }

        validarServicioDelProveedor(
                pregunta.getIdServicio(),
                idProveedor);

        if (respuesta == null
                || respuesta.isBlank()) {

            throw new IllegalArgumentException(
                    "La respuesta es obligatoria.");
        }

        repositoryPreguntaServicio
                .responderPregunta(
                        idPregunta,
                        respuesta.trim());
    }

    public List<PreguntaServicio> listarPreguntasPublicas(
            int idServicio) {

        return repositoryPreguntaServicio
                .listarPreguntasServicio(idServicio);
    }

    private void validarServicioDelProveedor(
            int idServicio,
            int idProveedor) {

        Servicio servicio
                = repositoryServicio
                        .buscarServicio(idServicio);

        if (servicio == null) {
            throw new IllegalArgumentException(
                    "El servicio no existe.");
        }

        if (servicio.getIdProveedor()
                != idProveedor) {

            throw new IllegalStateException(
                    "El servicio no pertenece al proveedor.");
        }
    }

    private static final java.math.BigDecimal PRECIO_MAXIMO
            = new java.math.BigDecimal("9999999999.99");

    // Reglas que se aplican siempre que se guarda un servicio,
    // sea borrador o este publicado
    private void validarDatosServicio(Servicio servicio) {

        if (servicio.getIdProveedor() <= 0) {
            throw new IllegalArgumentException(
                    "El proveedor es obligatorio.");
        }

        validarTexto(servicio.getNombre(),
                "El nombre", false, 150);
        validarTexto(servicio.getTipo(),
                "El tipo", false, 100);
        validarTexto(servicio.getCategoria(),
                "La categoria", true, 100);
        validarTexto(servicio.getDescripcion(),
                "La descripcion", true, 255);
        validarTexto(servicio.getCiudadZona(),
                "La ciudad o zona", true, 150);

        double precio = servicio.getPrecio();

        if (Double.isNaN(precio) || Double.isInfinite(precio)) {
            throw new IllegalArgumentException(
                    "El precio debe ser un numero valido.");
        }

        java.math.BigDecimal valor
                = java.math.BigDecimal.valueOf(precio);

        if (valor.signum() <= 0) {
            throw new IllegalArgumentException(
                    "El precio debe ser mayor que cero.");
        }

        if (valor.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException(
                    "El precio no puede tener mas de dos decimales.");
        }

        if (valor.compareTo(PRECIO_MAXIMO) > 0) {
            throw new IllegalArgumentException(
                    "El precio supera el valor permitido.");
        }

        if (servicio.getAnticipacionMinima() < 0) {
            throw new IllegalArgumentException(
                    "La anticipacion minima no puede ser negativa.");
        }

        if (servicio.getCantidadMaximaPersonas() != null
                && servicio.getCantidadMaximaPersonas() <= 0) {

            throw new IllegalArgumentException(
                    "La cantidad maxima debe ser mayor que cero.");
        }
    }

    private void validarTexto(
            String valor,
            String campo,
            boolean femenino,
            int maximo) {

        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(
                    campo + (femenino
                            ? " es obligatoria."
                            : " es obligatorio."));
        }

        if (valor.trim().length() > maximo) {
            throw new IllegalArgumentException(
                    campo + " no puede superar los "
                    + maximo + " caracteres.");
        }
    }

    private void validarServicioParaPublicar(
            Servicio servicio) {

        if (servicio.getIdProveedor() <= 0) {
            throw new IllegalArgumentException(
                    "El proveedor es obligatorio.");
        }

        if (servicio.getNombre() == null
                || servicio.getNombre().isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre es obligatorio.");
        }

        if (servicio.getTipo() == null
                || servicio.getTipo().isBlank()) {
            throw new IllegalArgumentException(
                    "El tipo es obligatorio.");
        }

        if (servicio.getDescripcion() == null
                || servicio.getDescripcion().isBlank()) {
            throw new IllegalArgumentException(
                    "La descripcion es obligatoria.");
        }

        if (servicio.getPrecio() <= 0) {
            throw new IllegalArgumentException(
                    "El precio debe ser mayor que cero.");
        }

        if (servicio.getCategoria() == null
                || servicio.getCategoria().isBlank()) {
            throw new IllegalArgumentException(
                    "La categoria es obligatoria.");
        }

        if (servicio.getCiudadZona() == null
                || servicio.getCiudadZona().isBlank()) {
            throw new IllegalArgumentException(
                    "La ciudad o zona es obligatoria.");
        }

        if (servicio.getAnticipacionMinima() < 0) {
            throw new IllegalArgumentException(
                    "La anticipacion minima no puede ser negativa.");
        }

        if (servicio.getCantidadMaximaPersonas() != null
                && servicio.getCantidadMaximaPersonas() <= 0) {

            throw new IllegalArgumentException(
                    "La cantidad maxima debe ser mayor que cero.");
        }
    }
}

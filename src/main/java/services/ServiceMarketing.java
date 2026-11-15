
package services;

import entities.Campana;
import entities.ClienteMarketing;
import entities.SegmentoCalculado;
import entities.SolicitudEvento;
import entities.Segmentacion;
import repositories.RepositoryMarketing;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServiceMarketing {

    private final RepositoryMarketing repositorio;
    private final Clock reloj;

    public ServiceMarketing(RepositoryMarketing repositorio) {
        this(repositorio, Clock.systemDefaultZone());
    }

    // Permite controlar la fecha actual en las pruebas.
    public ServiceMarketing(RepositoryMarketing repositorio, Clock reloj) {
        this.repositorio = repositorio;
        this.reloj = reloj;
    }

    // Cada grupo reúne clientes de una misma ubicación que comparten un tipo de evento
    private record GrupoClientes(String pais, String ciudad, String tipoEvento,
                                 List<Integer> idsClientes) {

        boolean coincideCon(String otroPais, String otraCiudad, String otroTipo) {
            return pais.equals(otroPais)
                    && ciudad.equals(otraCiudad)
                    && tipoEvento.equals(otroTipo);
        }
    }

    public List<Segmentacion> listarSegmentosActivos() {
        return repositorio.listarSegmentosActivos();
    }

    public List<Segmentacion> segmentarClientes(int idAdministrador) {
        if (!repositorio.existeAdministradorActivo(idAdministrador)) {
            throw new IllegalArgumentException("Solo un administrador activo puede segmentar clientes.");
        }

        List<SegmentoCalculado> calculados = calcularSegmentos(
                repositorio.listarClientesActivos(),
                repositorio.contarSolicitudesPorTipo());

        List<Segmentacion> segmentos = new ArrayList<>();
        for (SegmentoCalculado calculado : calculados) {
            segmentos.add(convertirEnSegmentacion(idAdministrador, calculado));
        }

        // Incluso si no quedan grupos válidos, se desactivan los automáticos anteriores
        return repositorio.reemplazarSegmentosAutomaticos(segmentos);
    }

    private Segmentacion convertirEnSegmentacion(int idAdministrador,
                                                 SegmentoCalculado calculado) {
        String nombre = calculado.getCiudad() + " - " + calculado.getTipoEvento();

        String criterios = Segmentacion.PREFIJO_CRITERIOS_AUTOMATICOS
                + "pais=" + calculado.getPais()
                + "|ciudad=" + calculado.getCiudad()
                + "|tipo=" + calculado.getTipoEvento();

        String descripcion = "Clientes de " + calculado.getCiudad()
                + " (" + calculado.getPais() + ") interesados en eventos de tipo "
                + calculado.getTipoEvento();

        return new Segmentacion(
                idAdministrador,
                nombre,
                criterios,
                descripcion,
                Segmentacion.ESTADO_ACTIVA,
                calculado.getIdsClientes());
    }

    public List<SegmentoCalculado> calcularSegmentos(
            List<ClienteMarketing> clientes,
            List<SolicitudEvento> solicitudes) {

        if (clientes == null || solicitudes == null) {
            throw new IllegalArgumentException(
                    "Se requieren los clientes y las preferencias de eventos.");
        }

        List<GrupoClientes> grupos = new ArrayList<>();

        for (ClienteMarketing cliente : clientes) {
            if (cliente == null || !hayTexto(cliente.getCiudad())) {
                continue;
            }

            String pais = hayTexto(cliente.getPais())
                    ? cliente.getPais().trim()
                    : "No especificado";

            String ciudad = cliente.getCiudad().trim();

            // Revisamos las preferencias de cada cliente, no las de todo el grupo
            for (SolicitudEvento solicitud : solicitudes) {
                if (solicitud == null
                        || solicitud.getIdCliente() != cliente.getIdUsuario()
                        || !hayTexto(solicitud.getTipo())
                        || solicitud.getCantidad() <= 0) {
                    continue;
                }

                String tipo = solicitud.getTipo().trim();
                GrupoClientes grupo = buscarGrupo(grupos, pais, ciudad, tipo);

                if (grupo == null) {
                    grupo = new GrupoClientes(
                            pais, ciudad, tipo, new ArrayList<>());
                    grupos.add(grupo);
                }

                // Una persona puede tener varias preferencias, pero no se repite en un grupo
                if (!grupo.idsClientes().contains(cliente.getIdUsuario())) {
                    grupo.idsClientes().add(cliente.getIdUsuario());
                }
            }
        }

        List<SegmentoCalculado> segmentos = new ArrayList<>();

        for (GrupoClientes grupo : grupos) {
            segmentos.add(new SegmentoCalculado(
                    grupo.pais(),
                    grupo.ciudad(),
                    grupo.tipoEvento(),
                    grupo.idsClientes()));
        }

        segmentos.sort(Comparator.comparing(SegmentoCalculado::getPais)
                .thenComparing(SegmentoCalculado::getCiudad)
                .thenComparing(SegmentoCalculado::getTipoEvento));

        return segmentos;
    }

    // Busca un grupo existente con la misma ubicación y preferencia
    private GrupoClientes buscarGrupo(List<GrupoClientes> grupos, String pais,
                                      String ciudad, String tipo) {
        for (GrupoClientes grupo : grupos) {
            if (grupo.coincideCon(pais, ciudad, tipo)) {
                return grupo;
            }
        }
        return null;
    }

    private static boolean hayTexto(String texto) {
        return texto != null && !texto.isBlank();
    }

    // ---------- cu-23: crear campaña de marketing ----------

    private static final Logger LOG = Logger.getLogger(ServiceMarketing.class.getName());

    private volatile Consumer<Campana> alFinalizarCampana = campana -> { };
    private ScheduledExecutorService planificador;

    // Presupuesto de marketing que todavía no está comprometido en campañas.
    public BigDecimal obtenerPresupuestoDisponible() {
        return repositorio.obtenerPresupuestoDisponible();
    }

    /*
     * Valida la campaña antes de programarla: administrador activo, datos completos,
     * coherencia de fechas (E2), segmento activo y presupuesto frente al disponible (E1).
     */
    public void validarCampana(Campana campana) {
        if (campana == null || !repositorio.existeAdministradorActivo(campana.getIdUsuario())) {
            throw new IllegalArgumentException("Solo un administrador activo puede crear campañas.");
        }
        if (!hayTexto(campana.getNombre()) || !hayTexto(campana.getTipoOferta())
                || campana.getIdSegmento() <= 0
                || campana.getFechaInicio() == null || campana.getFechaFin() == null
                || campana.getPresupuestoMaximo() == null
                || campana.getPresupuestoMaximo().signum() <= 0) {
            throw new IllegalArgumentException(
                    "Completa el segmento, el nombre, el tipo de oferta, las fechas y un presupuesto mayor que cero.");
        }
        if (!campana.getFechaFin().isAfter(campana.getFechaInicio())) {
            throw new IllegalArgumentException(
                    "La fecha de finalización debe ser posterior a la fecha de inicio.");
        }
        Segmentacion segmento = repositorio.buscarSegmento(campana.getIdSegmento());
        if (segmento == null || !Segmentacion.ESTADO_ACTIVA.equals(segmento.getEstado())) {
            throw new IllegalArgumentException("El segmento seleccionado no existe o no está activo.");
        }
        BigDecimal disponible = repositorio.obtenerPresupuestoDisponible();
        if (campana.getPresupuestoMaximo().compareTo(disponible) > 0) {
            throw presupuestoInsuficiente(disponible);
        }
    }

    /*
     * Valida y programa la campaña (estado PROGRAMADA) y compromete su presupuesto.
     * Se activa después, cuando llega su fecha de inicio.
     */
    public Campana crearCampana(Campana campana) {
        validarCampana(campana);
        Campana programada = new Campana(
                campana.getIdUsuario(),
                campana.getIdSegmento(),
                campana.getNombre().trim(),
                campana.getTipoOferta().trim(),
                campana.getFechaInicio(),
                campana.getFechaFin(),
                campana.getPresupuestoMaximo().setScale(2, RoundingMode.HALF_UP));
        if (!repositorio.guardarCampanaComprometiendoPresupuesto(programada)) {
            throw presupuestoInsuficiente(repositorio.obtenerPresupuestoDisponible());
        }
        return programada;
    }

    // Activa la campaña y la distribuye al segmento (PROGRAMADA -> ACTIVA).
    public void lanzarCampana(int idCampana) {
        exigirCampana(idCampana);
        if (!repositorio.activarYDistribuirCampana(idCampana)) {
            throw transicionInvalida(idCampana, "activarse");
        }
    }

    // Pausa manual (A1): suspende la distribución y conserva las métricas (ACTIVA -> PAUSADA).
    public void pausarCampana(int idAdministrador, int idCampana) {
        exigirAdministrador(idAdministrador);
        exigirCampana(idCampana);
        if (!repositorio.cambiarEstadoCampana(idCampana, Campana.ESTADO_ACTIVA, Campana.ESTADO_PAUSADA)) {
            throw transicionInvalida(idCampana, "pausarse");
        }
    }

    // Reactiva una campaña pausada (PAUSADA -> ACTIVA).
    public void reanudarCampana(int idAdministrador, int idCampana) {
        exigirAdministrador(idAdministrador);
        Campana campana = exigirCampana(idCampana);
        if (Campana.ESTADO_PAUSADA.equals(campana.getEstado())
                && !campana.getFechaFin().isAfter(LocalDate.now(reloj))) {
            throw new IllegalArgumentException("La campaña ya finalizó y no se puede reanudar.");
        }
        if (!repositorio.cambiarEstadoCampana(idCampana, Campana.ESTADO_PAUSADA, Campana.ESTADO_ACTIVA)) {
            throw transicionInvalida(idCampana, "reactivarse");
        }
    }

    // Registra una visualización de la oferta; devuelve false si la campaña no está ACTIVA.
    public boolean registrarVisualizacion(int idCampana) {
        return repositorio.incrementarVisualizaciones(idCampana);
    }

    // Registra una contratación a partir de la oferta; devuelve false si la campaña no está ACTIVA.
    public boolean registrarConversion(int idCampana) {
        return repositorio.incrementarConversiones(idCampana);
    }

    // Desactiva la campaña (ACTIVA o PAUSADA -> FINALIZADA) y entrega su resumen al observador.
    public void finalizarCampana(int idCampana) {
        exigirCampana(idCampana);
        boolean finalizada = repositorio.cambiarEstadoCampana(
                        idCampana, Campana.ESTADO_ACTIVA, Campana.ESTADO_FINALIZADA)
                || repositorio.cambiarEstadoCampana(
                        idCampana, Campana.ESTADO_PAUSADA, Campana.ESTADO_FINALIZADA);
        if (!finalizada) {
            throw transicionInvalida(idCampana, "finalizarse");
        }
        try {
            alFinalizarCampana.accept(exigirCampana(idCampana));
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "No se pudo entregar el resumen de la campaña " + idCampana, e);
        }
    }

    // Campaña con su estado, segmento y métricas actuales.
    public Campana obtenerCampana(int idCampana) {
        return exigirCampana(idCampana);
    }

    public List<Campana> listarCampanas() {
        return repositorio.listarCampanas();
    }

    // Quién recibe el resumen cuando una campaña se desactiva automáticamente.
    public void setAlFinalizarCampana(Consumer<Campana> observador) {
        this.alFinalizarCampana = observador == null ? campana -> { } : observador;
    }

    /*
     * Activa solo las campañas programadas dentro de su vigencia y finaliza las vencidas
     * sin distribuirlas. También finaliza las activas o pausadas cuya fecha de fin llegó.
     */
    public void verificarCampanas() {
        LocalDate hoy = LocalDate.now(reloj);
        for (Campana campana : repositorio.listarCampanas()) {
            if (Campana.ESTADO_PROGRAMADA.equals(campana.getEstado())) {
                if (!campana.getFechaFin().isAfter(hoy)) {
                    intentar(() -> finalizarCampanaProgramada(campana.getIdCampana()));
                } else if (!campana.getFechaInicio().isAfter(hoy)) {
                    intentar(() -> lanzarCampana(campana.getIdCampana()));
                }
            }
        }
        for (Campana campana : repositorio.listarCampanas()) {
            boolean vigente = Campana.ESTADO_ACTIVA.equals(campana.getEstado())
                    || Campana.ESTADO_PAUSADA.equals(campana.getEstado());
            if (vigente && !campana.getFechaFin().isAfter(hoy)) {
                intentar(() -> finalizarCampana(campana.getIdCampana()));
            }
        }
    }

    private void finalizarCampanaProgramada(int idCampana) {
        if (!repositorio.cambiarEstadoCampana(
                idCampana, Campana.ESTADO_PROGRAMADA, Campana.ESTADO_FINALIZADA)) {
            throw transicionInvalida(idCampana, "finalizarse");
        }
    }

    // Revisa las campañas cada cierto tiempo en un hilo de fondo; si ya está iniciado, no hace nada.
    public synchronized void iniciarPlanificador(long periodoSegundos) {
        if (planificador != null) {
            return;
        }
        planificador = Executors.newSingleThreadScheduledExecutor(tarea -> {
            Thread hilo = new Thread(tarea, "planificador-campanas");
            hilo.setDaemon(true);
            return hilo;
        });
        planificador.scheduleWithFixedDelay(() -> {
            try {
                verificarCampanas();
            } catch (RuntimeException e) {
                LOG.log(Level.WARNING, "No se pudieron revisar las campañas", e);
            }
        }, 0, periodoSegundos, TimeUnit.SECONDS);
    }

    public synchronized void detenerPlanificador() {
        if (planificador != null) {
            planificador.shutdownNow();
            planificador = null;
        }
    }

    public synchronized boolean planificadorActivo() {
        return planificador != null;
    }

    // Otro proceso pudo cambiar la campaña entre la lectura y el cambio: se omite sin detener la revisión.
    private void intentar(Runnable accion) {
        try {
            accion.run();
        } catch (IllegalArgumentException e) {
            LOG.log(Level.FINE, "Cambio de estado omitido: " + e.getMessage());
        }
    }

    private void exigirAdministrador(int idAdministrador) {
        if (!repositorio.existeAdministradorActivo(idAdministrador)) {
            throw new IllegalArgumentException("Solo un administrador activo puede gestionar campañas.");
        }
    }

    private Campana exigirCampana(int idCampana) {
        Campana campana = repositorio.buscarCampana(idCampana);
        if (campana == null) {
            throw new IllegalArgumentException("La campaña seleccionada no existe.");
        }
        return campana;
    }

    private IllegalArgumentException presupuestoInsuficiente(BigDecimal disponible) {
        return new IllegalArgumentException(
                "El presupuesto solicitado excede el disponible de marketing. Monto máximo restante: "
                        + disponible.toPlainString());
    }

    private IllegalArgumentException transicionInvalida(int idCampana, String accion) {
        return new IllegalArgumentException(
                "La campaña " + idCampana + " no puede " + accion + " desde su estado actual.");
    }
}

package app;

import controllers.ControllerAutenticacion;
import controllers.ControllerAdministracion;
import controllers.ControllerEventos;
import controllers.ControllerFinanciera;
import controllers.ControllerPublicacion;
import controllers.ControllerInvitados;
import controllers.ControllerProveedores;
import controllers.ControllerContratacionServicio;
import controllers.ControllerSolicitudes;
import entities.Evento;
import entities.Servicio;
import entities.SesionUsuario;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import repositories.RepositoryAutenticacion;
import repositories.RepositoryDisponibilidadServicio;
import repositories.RepositoryEvento;
import repositories.RepositoryContratacion;
import repositories.RepositoryContratacionServicio;
import repositories.RepositoryMovimientoFinanciero;
import repositories.RepositoryPlanificacion;
import repositories.RepositoryPublicacion;
import repositories.RepositoryPropuestaServicio;
import repositories.RepositoryServicio;
import repositories.RepositorySolicitudEjecucion;
import repositories.RepositoryOrganizador;
import services.ServiceAdministracion;
import services.ServiceAutenticacion;
import services.ServiceContratacionServicio;
import services.ServiceEventos;
import services.ServiceFinanciera;
import services.ServicePublicacion;
import services.ServiceInvitados;
import services.ServiceProveedores;
import services.ServiceSolicitudes;
import views.ViewCatalogoServicios;
import views.ViewDetalleServicio;
import views.ViewDetalleServicioProveedor;
import views.ViewEventos;
import views.ViewFinanciera;
import views.ViewPublicacion;
import views.ViewEdicion;
import views.ViewInvitados;
import views.ViewListaServiciosProveedor;
import views.ViewLogin;
import views.ViewPropuestasOrganizador;
import views.ViewSolicitudesProveedor;
import views.ViewMisEventos;
import views.ViewCancelacion;
import views.ViewFinalizacion;
import views.ViewAvisosProveedor;
import views.ViewAdministracion;
import services.ServiceAvisosProveedor;
import repositories.RepositoryAvisosProveedor;

public class NextEventuxApp extends Application {

    private BorderPane contenedor;
    private ControllerEventos controllerEventos;
    private ControllerAutenticacion controllerAutenticacion;
    private ControllerProveedores controllerProveedores;
    private ControllerSolicitudes controllerSolicitudes;
    private ControllerContratacionServicio controllerContratacion;

    private SesionUsuario sesionActual;
    private Navegacion navegacion;
    private Servicio servicioSeleccionado;
    private Integer servicioProveedorSeleccionado;
    private Integer eventoSeleccionado;
    private Stage stage;
    private ControllerPublicacion publicaciones;
    private ControllerFinanciera controllerFinanciera;

    @Override
    public void start(Stage stage) {
        this.stage = stage;

        controllerAutenticacion = new ControllerAutenticacion(
                new ServiceAutenticacion(new RepositoryAutenticacion()));

        controllerProveedores = new ControllerProveedores(
                new ServiceProveedores(
                        new RepositoryServicio(),
                        new RepositoryDisponibilidadServicio()));

        contenedor = new BorderPane();

        Rectangle2D pantalla = Screen.getPrimary().getVisualBounds();

        double ancho = Math.min(1440, pantalla.getWidth());
        double alto = Math.min(900, pantalla.getHeight());

        Scene scene = new Scene(contenedor, ancho, alto);

        scene.getStylesheets().add(
                getClass().getResource("/styles/eventos.css").toExternalForm());

        scene.getStylesheets().add(
                getClass().getResource("/styles/proveedores.css").toExternalForm());

        scene.getStylesheets().add(
                getClass().getResource("/styles/login.css").toExternalForm());

        mostrarLogin();

        stage.setTitle("NextEventux");
        stage.setMinWidth(Math.min(700, pantalla.getWidth()));
        stage.setMinHeight(Math.min(500, pantalla.getHeight()));
        stage.setResizable(true);
        stage.setScene(scene);
        stage.show();
        stage.centerOnScreen();
    }

    private void mostrarLogin() {

        if (sesionActual != null) sesionActual.cerrar();
        sesionActual = null;
        navegacion = null;
        servicioSeleccionado = null;
        servicioProveedorSeleccionado = null;
        eventoSeleccionado = null;
        controllerEventos = null;
        controllerSolicitudes = null;
        publicaciones = null;
        contenedor.setTop(null);
        stage.setTitle("NextEventux | Iniciar sesión");

        contenedor.setCenter(
                new ViewLogin(
                        controllerAutenticacion,
                        this::procesarSesion,
                        this::mostrarInvitado)
                        .crearPantalla());
    }

    // ------------------------------------------------------------------
    // Acceso del invitado (CU-16 y CU-20), sin cuenta: se identifica con
    // su correo y su teléfono en cada pantalla.
    // ------------------------------------------------------------------
    private void mostrarInvitado() {

        sesionActual = null;
        contenedor.setTop(null);
        navegacion = null;
        new AccesoInvitado(new ControllerInvitados(new ServiceInvitados()), this::mostrarLogin)
                .mostrarEn(contenedor, Navegacion.Pantalla.MIS_INVITACIONES);
    }

    private void procesarSesion(SesionUsuario sesion) {

        System.out.printf(
                "[SESION] recibida rol=%s actorId=%d%n",
                sesion.getTipoUsuario(),
                sesion.getIdActor());
        sesionActual = sesion;
        publicaciones = new ControllerPublicacion(new ServicePublicacion(
                new RepositoryPublicacion(), new RepositoryEvento()), sesion);
        controllerEventos = new ControllerEventos(new ServiceEventos(new RepositoryEvento()), sesion);
        controllerFinanciera = new ControllerFinanciera(new ServiceFinanciera(new RepositoryMovimientoFinanciero(),
            new RepositoryPlanificacion(), new RepositoryEvento()), sesion);

        String tipo = sesion.getTipoUsuario();

        if (!"ORGANIZADOR".equalsIgnoreCase(tipo)
                && !"PROVEEDOR".equalsIgnoreCase(tipo)
                && !"ADMINISTRADOR".equalsIgnoreCase(tipo)
                && !"CLIENTE".equalsIgnoreCase(tipo)) {

            System.err.printf(
                    "[SESION] rol no soportado rol=%s actorId=%d%n",
                    tipo,
                    sesion.getIdActor());
            mostrarLogin();
            return;
        }

        controllerSolicitudes = new ControllerSolicitudes(
                new ServiceSolicitudes(
                        new RepositorySolicitudEjecucion(),
                        new RepositoryPropuestaServicio(),
                        sesionActual));

        // CU-13: contratar el servicio de la propuesta seleccionada
        controllerContratacion = new ControllerContratacionServicio(
                new ServiceContratacionServicio(
                        new RepositoryContratacionServicio(),
                        sesionActual));

        contenedor.setTop(null);
        navegacion = new Navegacion();

        if ("ORGANIZADOR".equalsIgnoreCase(tipo) || "ADMINISTRADOR".equalsIgnoreCase(tipo)) {
            registrarRutasOrganizador();
            if ("ORGANIZADOR".equalsIgnoreCase(tipo)) {
                mostrarEventos();
            } else {
                mostrarPublicacion();
            }
        } else if ("PROVEEDOR".equalsIgnoreCase(tipo)) {
            System.out.printf(
                    "[SESION] rama PROVEEDOR actorId=%d%n",
                    sesion.getIdActor());
            registrarRutasProveedor();
            System.out.println(
                    "[SESION] rutas de proveedor registradas");
            mostrarProveedores();
        } else if ("CLIENTE".equalsIgnoreCase(tipo)) {
            mostrarContratarOrganizador();
        }
    }

    private boolean esOrganizador() {

        return sesionActual != null
                && "ORGANIZADOR".equalsIgnoreCase(
                        sesionActual.getTipoUsuario());
    }

    private void mostrarEventos() {

        if (!esOrganizador()) {
            mostrarLogin();
            return;
        }

        navegacion.mostrarEn(
                contenedor,
                Navegacion.Pantalla.EVENTOS);
    }

    private void mostrarInvitados() {

        if (!esOrganizador()) {
            mostrarLogin();
            return;
        }

        navegacion.ir(Navegacion.Pantalla.INVITADOS);
    }

    private void mostrarEstadoFinanciero(Evento evento) {
        if (!esOrganizador()) {
            mostrarLogin();
            return;
        }
        eventoSeleccionado = evento.getIdEvento();
        navegacion.ir(Navegacion.Pantalla.ESTADO_FINANCIERO);
     }

    private void registrarRutasOrganizador() {
        navegacion
                .registrar(
                        Navegacion.Pantalla.EVENTOS,
                        () -> new ViewEventos(
                                controllerEventos,
                                this::mostrarEventos,
                                this::mostrarCatalogoServicios,
                                this::mostrarInvitados,
                                this::mostrarLogin,
                                sesionActual,
                                this::mostrarMisEventos,
                                this::mostrarPropuestasOrganizador).conSolicitudes(this::mostrarSolicitudesEvento).crearPantalla())
                .registrar(
                        Navegacion.Pantalla.INVITADOS,
                        () -> new ViewInvitados(
                                new ControllerInvitados(
                                        new ServiceInvitados(), sesionActual),
                                this::mostrarEventos,
                                this::mostrarInvitados,
                                this::mostrarCatalogoServicios,
                                this::mostrarLogin,
                                sesionActual,
                                this::mostrarMisEventos).crearPantalla())
                .registrar(
                        Navegacion.Pantalla.CATALOGO_SERVICIOS,
                        () -> new ViewCatalogoServicios(
                                controllerProveedores,
                                sesionActual,
                                this::mostrarEventos,
                                this::mostrarInvitados,
                                this::mostrarCatalogoServicios,
                                this::mostrarLogin,
                                this::mostrarDetalleServicio,
                                this::mostrarMisEventos).crearPantalla())
                .registrar(
                        Navegacion.Pantalla.DETALLE_SERVICIO,
                        this::crearVistaDetalleServicio)
                .registrarDinamica(
                        Navegacion.Pantalla.PROPUESTAS_ORGANIZADOR,
                        this::crearVistaPropuestasOrganizador)
                .registrarDinamica(
                        Navegacion.Pantalla.MIS_EVENTOS,
                        () -> new ViewMisEventos(
                                controllerEventos,
                                sesionActual,
                                this::mostrarEventos,
                                this::mostrarMisEventos,
                                this::mostrarCatalogoServicios,
                                this::mostrarInvitados,
                                this::mostrarLogin,
                                this::mostrarEdicion,
                                this::mostrarPublicacion,
                                this::mostrarCancelacion,
                                this::mostrarFinalizacion)
                                .conSolicitudes(this::mostrarSolicitudesEvento)
                                .conFinanzas(this::mostrarEstadoFinanciero)
                                .crearPantalla())
                .registrarDinamica(
                        Navegacion.Pantalla.EDICION_EVENTO,
                        () -> new ViewEdicion(
                                controllerEventos,
                                this::mostrarMisEventos,
                                eventoSeleccionado,
                                sesionActual,
                                this::mostrarEventos,
                                this::mostrarMisEventos,
                                this::mostrarCatalogoServicios,
                                this::mostrarInvitados,
                                this::mostrarLogin).crearPantalla())
                .registrarDinamica(
                        Navegacion.Pantalla.CANCELACION_EVENTO,
                        () -> new ViewCancelacion(
                                controllerEventos,
                                this::mostrarMisEventos,
                                eventoSeleccionado,
                                sesionActual,
                                this::mostrarEventos,
                                this::mostrarMisEventos,
                                this::mostrarCatalogoServicios,
                                this::mostrarInvitados,
                                this::mostrarLogin).crearPantalla())
                .registrarDinamica(
                        Navegacion.Pantalla.FINALIZACION_EVENTO,
                        () -> new ViewFinalizacion(controllerEventos, this::mostrarMisEventos,
                                null, eventoSeleccionado, sesionActual, this::mostrarEventos,
                                this::mostrarMisEventos, this::mostrarCatalogoServicios,
                                this::mostrarInvitados, this::mostrarLogin).crearPantalla())
                .registrarDinamica(
                        Navegacion.Pantalla.PUBLICACION_EVENTOS,
                        () -> new ViewPublicacion(
                                publicaciones,
                                this::volverDesdePublicacion,
                                eventoSeleccionado,
                                sesionActual,
                                this::mostrarEventos,
                                this::mostrarMisEventos,
                                this::mostrarCatalogoServicios,
                                this::mostrarInvitados,
                                this::mostrarLogin).crearPantalla())
                .registrarDinamica(
                        Navegacion.Pantalla.ESTADO_FINANCIERO,
                        () -> new ViewFinanciera(
                                controllerFinanciera,
                                sesionActual,
                                this::mostrarEventos,
                                this::mostrarMisEventos,
                                this::mostrarCatalogoServicios,
                                this::mostrarInvitados,
                                this::mostrarLogin)
                                .crearPantalla(eventoSeleccionado != null ? eventoSeleccionado : 0));
    }

    private void mostrarFinalizacion(Evento evento) {
        if(!esOrganizador()) { mostrarLogin(); return; }
        eventoSeleccionado=evento.getIdEvento();
        navegacion.ir(Navegacion.Pantalla.FINALIZACION_EVENTO);
    }

    private void mostrarSolicitudesEvento(Evento evento) {
        if (!esOrganizador()) { mostrarLogin(); return; }
        new views.ViewSolicitudesEvento(controllerEventos, evento).mostrar(stage);
    }

    private void mostrarMisEventos() {
        if (!esOrganizador()) {
            mostrarLogin();
            return;
        }
        navegacion.ir(Navegacion.Pantalla.MIS_EVENTOS);
    }

    private void mostrarEdicion(Evento evento) {
        if (!esOrganizador()) {
            mostrarLogin();
            return;
        }
        eventoSeleccionado = evento.getIdEvento();
        navegacion.ir(Navegacion.Pantalla.EDICION_EVENTO);
    }

    private void mostrarPublicacion() {
        if (!esOrganizador()
                && (sesionActual == null
                || !"ADMINISTRADOR".equalsIgnoreCase(sesionActual.getTipoUsuario()))) {
            mostrarLogin();
            return;
        }
        eventoSeleccionado = null;
        navegacion.mostrarEn(contenedor, Navegacion.Pantalla.PUBLICACION_EVENTOS);
    }

    private void mostrarPublicacion(Evento evento) {
        if (!esOrganizador()) {
            mostrarLogin();
            return;
        }
        eventoSeleccionado = evento.getIdEvento();
        navegacion.ir(Navegacion.Pantalla.PUBLICACION_EVENTOS);
    }

    private void mostrarCancelacion(Evento evento) {
        if (!esOrganizador()) {
            mostrarLogin();
            return;
        }
        eventoSeleccionado = evento.getIdEvento();
        navegacion.ir(Navegacion.Pantalla.CANCELACION_EVENTO);
    }

    private void volverDesdePublicacion() {
        if (eventoSeleccionado != null && esOrganizador()) {
            mostrarMisEventos();
        } else if (esOrganizador()) {
            mostrarEventos();
        } else {
            contenedor.setCenter(new Label(
                    "Selecciona Publicaciones para revisar las solicitudes."));
        }
    }

    private void mostrarCatalogoServicios() {

        if (!esOrganizador()) {
            mostrarLogin();
            return;
        }

        navegacion.ir(Navegacion.Pantalla.CATALOGO_SERVICIOS);
    }

    private void mostrarDetalleServicio(Servicio servicio) {

        if (!esOrganizador()) {
            mostrarLogin();
            return;
        }

        if (servicio == null) {
            mostrarCatalogoServicios();
            return;
        }

        if (servicioSeleccionado == null
                || servicioSeleccionado.getIdServicio()
                        != servicio.getIdServicio()) {
            navegacion.invalidar(Navegacion.Pantalla.DETALLE_SERVICIO);
        }
        servicioSeleccionado = servicio;
        navegacion.ir(Navegacion.Pantalla.DETALLE_SERVICIO);
    }

    private javafx.scene.Parent crearVistaDetalleServicio() {
        if (servicioSeleccionado == null) {
            throw new IllegalStateException(
                    "No hay un servicio seleccionado para mostrar");
        }
        return new ViewDetalleServicio(
                controllerProveedores,
                controllerSolicitudes,
                servicioSeleccionado,
                sesionActual.getIdActor(),
                sesionActual,
                this::mostrarCatalogoServicios,
                this::mostrarEventos,
                this::mostrarInvitados,
                this::mostrarLogin,
                this::mostrarMisEventos)
                .crearPantalla();
    }

    private void mostrarPropuestasOrganizador() {

        if (!esOrganizador()) {
            mostrarLogin();
            return;
        }

        navegacion.ir(Navegacion.Pantalla.PROPUESTAS_ORGANIZADOR);
    }

    private javafx.scene.Parent crearVistaPropuestasOrganizador() {
        return new ViewPropuestasOrganizador(
                controllerSolicitudes,
                sesionActual.getIdActor(),
                sesionActual.getIdUsuario(),
                this::mostrarEventos,
                sesionActual,
                this::mostrarEventos,
                this::mostrarInvitados,
                this::mostrarCatalogoServicios,
                this::mostrarMisEventos,
                this::mostrarPropuestasOrganizador,
                this::mostrarLogin)
                .conContratacion(controllerContratacion)
                .crearPantalla();
    }

    private void mostrarProveedores() {
        System.out.printf(
                "[NAVEGACION] acceso a MIS_SERVICIOS rol=%s actorId=%d%n",
                sesionActual == null ? null : sesionActual.getTipoUsuario(),
                sesionActual == null ? 0 : sesionActual.getIdActor());

        if (sesionActual == null
                || !"PROVEEDOR".equalsIgnoreCase(
                        sesionActual.getTipoUsuario())) {

            mostrarLogin();
            return;
        }

        navegacion.invalidar(Navegacion.Pantalla.MIS_SERVICIOS);
        navegacion.mostrarEn(
                contenedor,
                Navegacion.Pantalla.MIS_SERVICIOS);
    }

    private void registrarRutasProveedor() {
        System.out.println(
                "[NAVEGACION] registrando rutas MIS_SERVICIOS, "
                + "DETALLE_SERVICIO_PROVEEDOR y SOLICITUDES_PROVEEDOR");
        navegacion
                .registrar(
                        Navegacion.Pantalla.MIS_SERVICIOS,
                        () -> new ViewListaServiciosProveedor(
                                controllerProveedores,
                                sesionActual.getIdActor(),
                                sesionActual,
                                this::mostrarLogin,
                                this::mostrarDetalleServicioProveedor,
                                () -> mostrarDetalleServicioProveedor(null),
                                this::mostrarProveedores,
                                this::mostrarSolicitudesProveedor,
                                this::mostrarAvisosProveedor)
                                .crearPantalla())
                .registrarDinamica(
                        Navegacion.Pantalla.AVISOS_PROVEEDOR,
                        () -> new ViewAvisosProveedor(new ServiceAvisosProveedor(new RepositoryAvisosProveedor()),
                                sesionActual,this::mostrarProveedores,this::mostrarLogin).crearPantalla())
                .registrar(
                        Navegacion.Pantalla.DETALLE_SERVICIO_PROVEEDOR,
                        this::crearVistaDetalleServicioProveedor)
                .registrarDinamica(
                        Navegacion.Pantalla.SOLICITUDES_PROVEEDOR,
                        this::crearVistaSolicitudesProveedor);
    }

    private void mostrarDetalleServicioProveedor(
            Integer idServicio) {

        if (sesionActual == null
                || !"PROVEEDOR".equalsIgnoreCase(
                        sesionActual.getTipoUsuario())) {

            mostrarLogin();
            return;
        }

        if (!java.util.Objects.equals(
                servicioProveedorSeleccionado,
                idServicio)) {
            navegacion.invalidar(
                    Navegacion.Pantalla.DETALLE_SERVICIO_PROVEEDOR);
        }
        servicioProveedorSeleccionado = idServicio;
        navegacion.ir(
                Navegacion.Pantalla.DETALLE_SERVICIO_PROVEEDOR);
    }

    private void mostrarAvisosProveedor() {
        if(sesionActual==null || !"PROVEEDOR".equalsIgnoreCase(sesionActual.getTipoUsuario())) {
            mostrarLogin(); return;
        }
        navegacion.ir(Navegacion.Pantalla.AVISOS_PROVEEDOR);
    }

    private javafx.scene.Parent crearVistaDetalleServicioProveedor() {
        System.out.printf(
                "[NAVEGACION] construyendo DETALLE_SERVICIO_PROVEEDOR actorId=%d%n",
                sesionActual.getIdActor());
        return new ViewDetalleServicioProveedor(
                controllerProveedores,
                sesionActual.getIdActor(),
                sesionActual,
                this::mostrarLogin,
                servicioProveedorSeleccionado,
                this::mostrarProveedores)
                .crearPantalla();
    }

    /** CU-026: un cliente solicita contratar un organizador desde su propia sesión. */
    private void mostrarContratarOrganizador() {
        if (sesionActual == null || !"CLIENTE".equalsIgnoreCase(sesionActual.getTipoUsuario())) {
            mostrarLogin();
            return;
        }
        ControllerAdministracion controlador = new ControllerAdministracion(
                new ServiceAdministracion(new RepositoryContratacion(), new RepositoryOrganizador()),
                sesionActual);
        contenedor.setCenter(new ViewAdministracion(controlador, sesionActual).crearPantalla());
        stage.setTitle("NextEventux | Contratar organizador");
    }

    private void mostrarSolicitudesProveedor() {

        if (sesionActual == null
                || !"PROVEEDOR".equalsIgnoreCase(
                        sesionActual.getTipoUsuario())) {

            mostrarLogin();
            return;
        }

        navegacion.ir(Navegacion.Pantalla.SOLICITUDES_PROVEEDOR);
    }

    private javafx.scene.Parent crearVistaSolicitudesProveedor() {
        System.out.printf(
                "[NAVEGACION] construyendo SOLICITUDES_PROVEEDOR actorId=%d%n",
                sesionActual.getIdActor());
        return new ViewSolicitudesProveedor(
                controllerSolicitudes,
                sesionActual.getIdActor(),
                sesionActual.getIdUsuario(),
                this::mostrarProveedores,
                sesionActual,
                this::mostrarProveedores,
                this::mostrarSolicitudesProveedor,
                this::mostrarLogin)
                .crearPantalla();
    }

}

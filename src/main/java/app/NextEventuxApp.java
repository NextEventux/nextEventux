package app;

import controllers.ControllerAutenticacion;
import controllers.ControllerEventos;
import controllers.ControllerPublicacion;
import controllers.ControllerInvitados;
import controllers.ControllerProveedores;
import controllers.ControllerAdministracion;
import entities.Evento;
import entities.Servicio;
import entities.SesionUsuario;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import repositories.RepositoryProveedor;
import repositories.RepositoryAutenticacion;
import repositories.RepositoryDisponibilidadServicio;
import repositories.RepositoryEvento;
import repositories.RepositoryPublicacion;
import repositories.RepositoryServicio;
import services.ServiceAdministracion;
import services.ServiceAutenticacion;
import services.ServiceEventos;
import services.ServicePublicacion;
import services.ServiceInvitados;
import services.ServiceProveedores;
import views.ViewAdministracion;
import views.ViewCatalogoServicios;
import views.ViewDetalleServicio;
import views.ViewDetalleServicioProveedor;
import views.ViewEventos;
import views.ViewPublicacion;
import views.ViewEdicion;
import views.ViewInvitados;
import views.ViewListaServiciosProveedor;
import views.ViewLogin;
import views.ViewMisEventos;
import views.ViewCancelacion;

public class NextEventuxApp extends Application {

    private BorderPane contenedor;
    private ControllerEventos controllerEventos;
    private ControllerAutenticacion controllerAutenticacion;
    private ControllerProveedores controllerProveedores;

    private SesionUsuario sesionActual;
    private Navegacion navegacion;
    private Servicio servicioSeleccionado;
    private Integer servicioProveedorSeleccionado;
    private Integer eventoSeleccionado;
    private Stage stage;
    private Stage ventanaAdministracion;
    private ControllerPublicacion publicaciones;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        stage.setOnHidden(event -> cerrarAdministracion());

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

    private void cerrarAdministracion() {
        if(ventanaAdministracion!=null) {
            ventanaAdministracion.close();
            ventanaAdministracion=null;
        }
    }

    private void mostrarLogin() {
        cerrarAdministracion();

        if (sesionActual != null) sesionActual.cerrar();
        sesionActual = null;
        navegacion = null;
        servicioSeleccionado = null;
        servicioProveedorSeleccionado = null;
        eventoSeleccionado = null;
        controllerEventos = null;
        publicaciones = null;
        contenedor.setTop(null);
        stage.setTitle("NextEventux | Iniciar sesión");

        contenedor.setCenter(
                new ViewLogin(
                        controllerAutenticacion,
                        this::procesarSesion)
                        .crearPantalla());
    }

    private void procesarSesion(SesionUsuario sesion) {

    sesionActual = sesion;

    publicaciones = new ControllerPublicacion(
            new ServicePublicacion(
                    new RepositoryPublicacion(),
                    new RepositoryEvento()),
            sesion);

    controllerEventos = new ControllerEventos(
            new ServiceEventos(new RepositoryEvento()),
            sesion);

    String tipo = sesion.getTipoUsuario();

    if (!"ORGANIZADOR".equalsIgnoreCase(tipo)
            && !"PROVEEDOR".equalsIgnoreCase(tipo)
            && !"ADMINISTRADOR".equalsIgnoreCase(tipo)) {
        mostrarLogin();
        return;
    }

    contenedor.setTop(null);
    navegacion = new Navegacion();

    if ("ORGANIZADOR".equalsIgnoreCase(tipo)
            || "ADMINISTRADOR".equalsIgnoreCase(tipo)) {

        registrarRutasOrganizador();

        if ("ORGANIZADOR".equalsIgnoreCase(tipo)) {
            mostrarEventos();
        } else {
            mostrarPublicacion();
        }

    } else if ("PROVEEDOR".equalsIgnoreCase(tipo)) {

        registrarRutasProveedor();
        mostrarProveedores();
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
                                this::mostrarMisEventos).crearPantalla())
                .registrar(
                        Navegacion.Pantalla.INVITADOS,
                        () -> new ViewInvitados(
                                new ControllerInvitados(
                                        new ServiceInvitados()),
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
                                this::mostrarCancelacion)
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
                                sesionActual.getIdActor(),
                                eventoSeleccionado,
                                sesionActual,
                                this::mostrarEventos,
                                this::mostrarMisEventos,
                                this::mostrarCatalogoServicios,
                                this::mostrarInvitados,
                                this::mostrarLogin).crearPantalla())
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
                                this::mostrarLogin,
                                this::mostrarAdministracion).crearPantalla());
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
        navegacion.mostrarEn(contenedor,Navegacion.Pantalla.PUBLICACION_EVENTOS);
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

    private void mostrarProveedores() {

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
                                this::mostrarProveedores)
                                .crearPantalla())
                .registrar(
                        Navegacion.Pantalla.DETALLE_SERVICIO_PROVEEDOR,
                        this::crearVistaDetalleServicioProveedor);
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

    private javafx.scene.Parent crearVistaDetalleServicioProveedor() {
        return new ViewDetalleServicioProveedor(
                controllerProveedores,
                sesionActual.getIdActor(),
                sesionActual,
                this::mostrarLogin,
                servicioProveedorSeleccionado,
                this::mostrarProveedores)
                .crearPantalla();
    }
    private void mostrarAdministracion() {

    if (sesionActual == null
            || !"ADMINISTRADOR".equalsIgnoreCase(
                    sesionActual.getTipoUsuario())) {
        mostrarLogin();
        return;
    }

    ControllerAdministracion controller =
            new ControllerAdministracion(
                    new ServiceAdministracion(
                            new RepositoryProveedor(),sesionActual));

    if(ventanaAdministracion!=null && ventanaAdministracion.isShowing()) {
        ventanaAdministracion.toFront(); return;
    }
    ventanaAdministracion = new Stage();
    ventanaAdministracion.initOwner(stage);

    Rectangle2D pantalla = Screen.getPrimary().getVisualBounds();
    double ancho = Math.min(1440, pantalla.getWidth());
    double alto = Math.min(900, pantalla.getHeight());

    Scene escena = new Scene(
            new ViewAdministracion(controller,sesionActual,this::mostrarLogin).crearPantalla(),
            ancho,
            alto);

    escena.getStylesheets().add(getClass().getResource("/styles/eventos.css").toExternalForm());
    escena.getStylesheets().add(
            getClass().getResource(
                    "/styles/administracion.css").toExternalForm());

    ventanaAdministracion.setTitle(
            "NextEventux | Aprobar proveedores");
    ventanaAdministracion.setMinWidth(Math.min(1000, pantalla.getWidth()));
    ventanaAdministracion.setMinHeight(Math.min(650, pantalla.getHeight()));
    ventanaAdministracion.setScene(escena);
    ventanaAdministracion.show();
    ventanaAdministracion.centerOnScreen();
}
}

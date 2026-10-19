package app;

import controllers.ControllerAutenticacion;
import controllers.ControllerEventos;
import controllers.ControllerPublicacion;
import controllers.ControllerInvitados;
import controllers.ControllerProveedores;
import controllers.ControllerAdministracion;
import entities.Servicio;
import entities.SesionUsuario;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
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

public class NextEventuxApp extends Application {

    private BorderPane contenedor;
    private ControllerEventos controllerEventos;
    private ControllerAutenticacion controllerAutenticacion;
    private ControllerProveedores controllerProveedores;

    private SesionUsuario sesionActual;
    private Stage stage;
    private ControllerPublicacion publicaciones;

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
        publicaciones = new ControllerPublicacion(new ServicePublicacion(
                new RepositoryPublicacion(), new RepositoryEvento()), sesion);
        controllerEventos = new ControllerEventos(new ServiceEventos(new RepositoryEvento()), sesion);

        String tipo = sesion.getTipoUsuario();

        if (!"ORGANIZADOR".equalsIgnoreCase(tipo)
                && !"PROVEEDOR".equalsIgnoreCase(tipo)
                && !"ADMINISTRADOR".equalsIgnoreCase(tipo)) {

            mostrarLogin();
            return;
        }

        Label usuario = new Label(
                sesion.getNombre() + " - " + tipo);

        Button cerrarSesion = new Button("Cerrar sesión");
        cerrarSesion.setId("cerrarSesion");

        cerrarSesion.setOnAction(
                event -> mostrarLogin());

        FlowPane barra = new FlowPane(15, 8, usuario, cerrarSesion);

        if ("ORGANIZADOR".equalsIgnoreCase(tipo) || "ADMINISTRADOR".equalsIgnoreCase(tipo)) {
            Button publicar = new Button("Publicaciones"); publicar.setId("abrirPublicacionSesion");
            publicar.setOnAction(event -> mostrarPublicacion()); barra.getChildren().add(0, publicar);
        }

        if ("ADMINISTRADOR".equalsIgnoreCase(tipo)) {
    Button aprobarProveedores = new Button("Aprobar proveedores");
    aprobarProveedores.setId("abrirAdministracionSesion");
    aprobarProveedores.setOnAction(
            event -> mostrarAdministracion());
    barra.getChildren().add(1, aprobarProveedores);
}
        if ("ORGANIZADOR".equalsIgnoreCase(tipo)) {

            Button crear = new Button("Crear evento");
            crear.setId("abrirCrearSesion"); crear.setOnAction(event -> mostrarEventos());
            Button editar = new Button("Editar evento");
            editar.setId("abrirEdicionSesion"); editar.setOnAction(event -> mostrarEdicion());
            Button invitados = new Button("Invitados");
            invitados.setId("abrirInvitadosSesion"); invitados.setOnAction(event -> mostrarInvitados());
            barra.getChildren().addAll(0, java.util.List.of(crear, editar, invitados));
            Button catalogo = new Button("Catálogo de proveedores");
            catalogo.setId("abrirCatalogoSesion");

            catalogo.setOnAction(
                    event -> mostrarCatalogoServicios());

            barra.getChildren().add(1, catalogo);
        }

        barra.setAlignment(Pos.CENTER_RIGHT);
        barra.setPadding(new Insets(12));

        contenedor.setTop(barra);

        if ("ORGANIZADOR".equalsIgnoreCase(tipo)) {

            mostrarEventos();

        } else if ("PROVEEDOR".equalsIgnoreCase(tipo)) {

            mostrarProveedores();

        } else {
            mostrarPublicacion();
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

        // El menú lateral permite ir de Eventos a Invitados (CU-17) y volver.
        new Navegacion()
                .registrar(Navegacion.Pantalla.EVENTOS,
                        () -> new ViewEventos(controllerEventos, sesionActual, this::mostrarEdicion, this::mostrarPublicacion).crearPantalla())
                .registrar(Navegacion.Pantalla.INVITADOS,
                        () -> new ViewInvitados(new ControllerInvitados(new ServiceInvitados())).crearPantalla())
                .mostrarEn(contenedor, Navegacion.Pantalla.EVENTOS);
    }

    private void mostrarEdicion() {
        if (!esOrganizador()) { mostrarLogin(); return; }
        stage.setTitle("NextEventux | Editar evento");
        contenedor.setCenter(new ViewEdicion(controllerEventos, this::mostrarEventos).crearPantalla());
    }

    private void mostrarInvitados() {
        if (!esOrganizador()) { mostrarLogin(); return; }
        stage.setTitle("NextEventux | Invitaciones");
        new Navegacion()
                .registrar(Navegacion.Pantalla.EVENTOS,
                        () -> new ViewEventos(controllerEventos, sesionActual, this::mostrarEdicion, this::mostrarPublicacion).crearPantalla())
                .registrar(Navegacion.Pantalla.INVITADOS,
                        () -> new ViewInvitados(new ControllerInvitados(new ServiceInvitados())).crearPantalla())
                .mostrarEn(contenedor, Navegacion.Pantalla.INVITADOS);
    }

    private void mostrarPublicacion() {
        stage.setTitle("NextEventux | Publicación de eventos");
        contenedor.setCenter(new ViewPublicacion(publicaciones, () -> {
            if (esOrganizador()) mostrarEventos();
            else contenedor.setCenter(new Label("Selecciona Publicaciones para revisar las solicitudes."));
        }).crearPantalla());
    }

    private void mostrarCatalogoServicios() {

        if (!esOrganizador()) {
            mostrarLogin();
            return;
        }

        contenedor.setCenter(
                new ViewCatalogoServicios(
                        controllerProveedores,
                        sesionActual,
                        this::mostrarEventos,
                        this::mostrarLogin,
                        this::mostrarDetalleServicio)
                        .crearPantalla());
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

        contenedor.setCenter(
                new ViewDetalleServicio(
                        controllerProveedores,
                        servicio,
                        sesionActual.getIdActor(),
                        sesionActual,
                        this::mostrarCatalogoServicios,
                        this::mostrarEventos,
                        this::mostrarLogin)
                        .crearPantalla());
    }

    private void mostrarProveedores() {

        if (sesionActual == null
                || !"PROVEEDOR".equalsIgnoreCase(
                        sesionActual.getTipoUsuario())) {

            mostrarLogin();
            return;
        }

        contenedor.setTop(null);

        contenedor.setCenter(
                new ViewListaServiciosProveedor(
                        controllerProveedores,
                        sesionActual.getIdActor(),
                        sesionActual,
                        this::mostrarLogin,
                        this::mostrarDetalleServicioProveedor,
                        () -> mostrarDetalleServicioProveedor(null))
                        .crearPantalla());
    }

    private void mostrarDetalleServicioProveedor(
            Integer idServicio) {

        if (sesionActual == null
                || !"PROVEEDOR".equalsIgnoreCase(
                        sesionActual.getTipoUsuario())) {

            mostrarLogin();
            return;
        }

        contenedor.setTop(null);

        contenedor.setCenter(
                new ViewDetalleServicioProveedor(
                        controllerProveedores,
                        sesionActual.getIdActor(),
                        sesionActual,
                        this::mostrarLogin,
                        idServicio,
                        this::mostrarProveedores)
                        .crearPantalla());
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
                            new RepositoryProveedor()));

    Stage ventanaAdministracion = new Stage();

    Rectangle2D pantalla = Screen.getPrimary().getVisualBounds();
    double ancho = Math.min(1440, pantalla.getWidth());
    double alto = Math.min(900, pantalla.getHeight());

    Scene escena = new Scene(
            new ViewAdministracion(controller).crearPantalla(),
            ancho,
            alto);

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

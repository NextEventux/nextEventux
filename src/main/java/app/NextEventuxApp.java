package app;

import controllers.ControllerAutenticacion;
import controllers.ControllerEventos;
import controllers.ControllerInvitados;
import controllers.ControllerProveedores;
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
import repositories.RepositoryAutenticacion;
import repositories.RepositoryDisponibilidadServicio;
import repositories.RepositoryEvento;
import repositories.RepositoryServicio;
import services.ServiceAutenticacion;
import services.ServiceEventos;
import services.ServiceInvitados;
import services.ServiceProveedores;
import views.ViewCatalogoServicios;
import views.ViewDetalleServicio;
import views.ViewDetalleServicioProveedor;
import views.ViewEventos;
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

            Label pendiente = new Label(
                    "Sesión iniciada correctamente como administrador.\n"
                    + "El módulo de administración (CU03) "
                    + "está pendiente de integración.");

            pendiente.setStyle("-fx-font-size: 18px;");
            pendiente.setWrapText(true);

            contenedor.setCenter(pendiente);
            BorderPane.setAlignment(pendiente, Pos.CENTER);
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
                        () -> new ViewEventos(controllerEventos, sesionActual, this::mostrarEdicion).crearPantalla())
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
                        () -> new ViewEventos(controllerEventos, sesionActual, this::mostrarEdicion).crearPantalla())
                .registrar(Navegacion.Pantalla.INVITADOS,
                        () -> new ViewInvitados(new ControllerInvitados(new ServiceInvitados())).crearPantalla())
                .mostrarEn(contenedor, Navegacion.Pantalla.INVITADOS);
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
}

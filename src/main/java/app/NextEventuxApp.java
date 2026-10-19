package app;

import controllers.ControllerAutenticacion;
import controllers.ControllerEventos;
import controllers.ControllerInvitados;
import controllers.ControllerProveedores;
import controllers.ControllerSolicitudes;
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
import javafx.scene.layout.HBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import repositories.RepositoryAutenticacion;
import repositories.RepositoryDisponibilidadServicio;
import repositories.RepositoryEvento;
import repositories.RepositoryServicio;
import repositories.RepositorySolicitudEjecucion;
import services.ServiceAutenticacion;
import services.ServiceEventos;
import services.ServiceInvitados;
import services.ServiceProveedores;
import services.ServiceSolicitudes;
import views.ViewCatalogoServicios;
import views.ViewDetalleServicio;
import views.ViewDetalleServicioProveedor;
import views.ViewEventos;
import views.ViewInvitados;
import views.ViewListaServiciosProveedor;
import views.ViewSolicitudesProveedor;
import views.ViewLogin;

public class NextEventuxApp extends Application {

    private BorderPane contenedor;
    private ControllerEventos controllerEventos;
    private ControllerAutenticacion controllerAutenticacion;
    private ControllerProveedores controllerProveedores;
    private ControllerSolicitudes controllerSolicitudes;

    private SesionUsuario sesionActual;

    @Override
    public void start(Stage stage) {

        controllerEventos = new ControllerEventos(
                new ServiceEventos(new RepositoryEvento()));

        controllerAutenticacion = new ControllerAutenticacion(
                new ServiceAutenticacion(new RepositoryAutenticacion()));

        controllerProveedores = new ControllerProveedores(
                new ServiceProveedores(
                        new RepositoryServicio(),
                        new RepositoryDisponibilidadServicio()));

        controllerSolicitudes = new ControllerSolicitudes(
                new ServiceSolicitudes(
                        new RepositorySolicitudEjecucion()));

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

        sesionActual = null;
        contenedor.setTop(null);

        contenedor.setCenter(
                new ViewLogin(
                        controllerAutenticacion,
                        this::procesarSesion)
                        .crearPantalla());
    }

    private void procesarSesion(SesionUsuario sesion) {

        sesionActual = sesion;

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

        cerrarSesion.setOnAction(
                event -> mostrarLogin());

        HBox barra = new HBox(20, usuario, cerrarSesion);

        if ("ORGANIZADOR".equalsIgnoreCase(tipo)) {

            Button catalogo = new Button("Catálogo de proveedores");

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
                        () -> new ViewEventos(controllerEventos).crearPantalla())
                .registrar(Navegacion.Pantalla.INVITADOS,
                        () -> new ViewInvitados(new ControllerInvitados(new ServiceInvitados())).crearPantalla())
                .mostrarEn(contenedor, Navegacion.Pantalla.EVENTOS);
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
                        () -> mostrarDetalleServicioProveedor(null),
                        this::mostrarSolicitudesProveedor)
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

    private void mostrarSolicitudesProveedor() {

        if (sesionActual == null
                || !"PROVEEDOR".equalsIgnoreCase(
                        sesionActual.getTipoUsuario())) {

            mostrarLogin();
            return;
        }

        contenedor.setTop(null);

        contenedor.setCenter(
                new ViewSolicitudesProveedor(
                        controllerSolicitudes,
                        sesionActual.getIdActor(),
                        this::mostrarProveedores)
                        .crearPantalla());
    }

}

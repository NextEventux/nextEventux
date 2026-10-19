package app;

import controllers.ControllerAutenticacion;
import controllers.ControllerEventos;
import controllers.ControllerPublicacion;
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
import repositories.RepositoryEvento;
import repositories.RepositoryPublicacion;
import services.ServiceAutenticacion;
import services.ServiceEventos;
import services.ServicePublicacion;
import views.ViewEdicion;
import views.ViewEventos;
import views.ViewLogin;
import views.ViewPublicacion;
import views.ViewCancelacion;
import views.ViewAvisosProveedor;
import views.ViewRespuestaInvitacion;
import controllers.ControllerInvitados;
import services.ServiceInvitados;
import views.ViewListaServiciosProveedor;
import views.ViewDetalleServicioProveedor;
import controllers.ControllerProveedores;
import repositories.RepositoryServicio;
import repositories.RepositoryDisponibilidadServicio;
import repositories.RepositoryAvisosProveedor;
import services.ServiceProveedores;
import services.ServiceAvisosProveedor;

public class NextEventuxApp extends Application {
    private final ServiceEventos eventos = new ServiceEventos(new RepositoryEvento());
    private final ControllerAutenticacion autenticacion = new ControllerAutenticacion(
            new ServiceAutenticacion(new RepositoryAutenticacion()));
    private final BorderPane contenedor = new BorderPane();
    private ControllerEventos controller;
    private ControllerPublicacion publicaciones;
    private SesionUsuario sesion;
    private Stage stage;
    private final ControllerProveedores proveedores = new ControllerProveedores(new ServiceProveedores(
            new RepositoryServicio(), new RepositoryDisponibilidadServicio()));

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        Rectangle2D pantalla = Screen.getPrimary().getVisualBounds();
        Scene scene = new Scene(contenedor, Math.min(1440, pantalla.getWidth()), Math.min(900, pantalla.getHeight()));
        scene.getStylesheets().add(getClass().getResource("/styles/eventos.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/styles/login.css").toExternalForm());
        mostrarLogin();
        stage.setMinWidth(Math.min(800, pantalla.getWidth()));
        stage.setMinHeight(Math.min(650, pantalla.getHeight()));
        stage.setScene(scene);
        stage.show();
        stage.centerOnScreen();
    }

    private void mostrarLogin() {
        if (sesion != null) sesion.cerrar();
        sesion = null;
        controller = null;
        publicaciones = null;
        contenedor.setTop(null);
        contenedor.setCenter(new ViewLogin(autenticacion, this::procesarSesion, this::mostrarConsultaInvitado).crearPantalla());
        stage.setTitle("NextEventux | Iniciar sesión");
    }

    private void procesarSesion(SesionUsuario usuario) {
        sesion = usuario;
        controller = new ControllerEventos(eventos, sesion);
        publicaciones = new ControllerPublicacion(new ServicePublicacion(
                new RepositoryPublicacion(), new RepositoryEvento()), sesion);
        Button salir = new Button("Cerrar sesión");
        salir.setId("cerrarSesion");
        salir.setOnAction(event -> mostrarLogin());
        FlowPane barra = new FlowPane(15, 8, new Label(sesion.getNombre()), salir);
        barra.setAlignment(Pos.CENTER_RIGHT);
        barra.setPadding(new Insets(10));
        contenedor.setTop(barra);
        if ("ORGANIZADOR".equalsIgnoreCase(sesion.getTipoUsuario())
                || "ADMINISTRADOR".equalsIgnoreCase(sesion.getTipoUsuario())) {
            Button publicar = new Button("Publicaciones");
            publicar.setId("abrirPublicacionSesion");
            publicar.setOnAction(event -> mostrarPublicacion());
            barra.getChildren().add(0, publicar);
        }
        if ("ORGANIZADOR".equalsIgnoreCase(sesion.getTipoUsuario())) {
            Button crear = new Button("Crear evento");
            crear.setOnAction(event -> mostrarCrear());
            Button editar = new Button("Editar evento");
            editar.setId("abrirEdicionSesion");
            editar.setOnAction(event -> mostrarEdicion());
            Button cancelar = new Button("Cancelar evento");
            cancelar.setId("abrirCancelacionSesion");
            cancelar.setOnAction(event -> mostrarCancelacion());
            barra.getChildren().addAll(0, java.util.List.of(crear, editar, cancelar));
            mostrarCrear();
        } else if ("ADMINISTRADOR".equalsIgnoreCase(sesion.getTipoUsuario())) {
            mostrarPublicacion();
        } else if ("PROVEEDOR".equalsIgnoreCase(sesion.getTipoUsuario())) {
            Button servicios = new Button("Mis servicios"); servicios.setOnAction(e -> mostrarProveedores());
            Button avisos = new Button("Mis avisos"); avisos.setId("abrirAvisosProveedor");
            avisos.setOnAction(e -> contenedor.setCenter(new ViewAvisosProveedor(
                    new ServiceAvisosProveedor(new RepositoryAvisosProveedor()), sesion).crearPantalla()));
            barra.getChildren().addAll(0, java.util.List.of(servicios, avisos));
            mostrarProveedores();
        } else {
            contenedor.setCenter(new Label("Sesión iniciada. El módulo de "
                    + sesion.getTipoUsuario().toLowerCase() + " está pendiente de integración."));
        }
    }

    private void mostrarCrear() {
        stage.setTitle("NextEventux | Crear evento");
        contenedor.setCenter(new ViewEventos(controller, sesion, this::mostrarEdicion, this::mostrarPublicacion, this::mostrarCancelacion).crearPantalla());
    }

    private void mostrarEdicion() {
        stage.setTitle("NextEventux | Editar evento");
        contenedor.setCenter(new ViewEdicion(controller, this::mostrarCrear).crearPantalla());
    }

    private void mostrarCancelacion() {
        stage.setTitle("NextEventux | Cancelar evento");
        contenedor.setCenter(new ViewCancelacion(controller, this::mostrarCrear).crearPantalla());
    }

    private void mostrarPublicacion() {
        stage.setTitle("NextEventux | Publicación de eventos");
        contenedor.setCenter(new ViewPublicacion(publicaciones, () -> {
            if ("ORGANIZADOR".equalsIgnoreCase(sesion.getTipoUsuario())) mostrarCrear();
            else contenedor.setCenter(new Label("Selecciona Publicaciones para revisar las solicitudes."));
        }).crearPantalla());
    }

    private void mostrarConsultaInvitado() {
        Button volver = new Button("← Iniciar sesión"); volver.setOnAction(e -> mostrarLogin());
        contenedor.setTop(volver);
        contenedor.setCenter(new ViewRespuestaInvitacion(new ControllerInvitados(new ServiceInvitados())).crearPantalla());
        stage.setTitle("NextEventux | Mis invitaciones y avisos");
    }

    private void mostrarProveedores() {
        contenedor.setCenter(new ViewListaServiciosProveedor(proveedores, sesion.getIdActor(), sesion,
                this::mostrarLogin, this::mostrarDetalleProveedor, () -> mostrarDetalleProveedor(null)).crearPantalla());
    }

    private void mostrarDetalleProveedor(Integer servicio) {
        contenedor.setCenter(new ViewDetalleServicioProveedor(proveedores, sesion.getIdActor(), sesion,
                this::mostrarLogin, servicio, this::mostrarProveedores).crearPantalla());
    }

    public static void main(String[] args) { launch(args); }
}

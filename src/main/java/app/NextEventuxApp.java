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
import javafx.scene.layout.HBox;
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

public class NextEventuxApp extends Application {
    private final ServiceEventos eventos = new ServiceEventos(new RepositoryEvento());
    private final ControllerAutenticacion autenticacion = new ControllerAutenticacion(
            new ServiceAutenticacion(new RepositoryAutenticacion()));
    private final BorderPane contenedor = new BorderPane();
    private ControllerEventos controller;
    private ControllerPublicacion publicaciones;
    private SesionUsuario sesion;
    private Stage stage;

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
        contenedor.setCenter(new ViewLogin(autenticacion, this::procesarSesion).crearPantalla());
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
        HBox barra = new HBox(15, new Label(sesion.getNombre()), salir);
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
            barra.getChildren().addAll(0, java.util.List.of(crear, editar));
            mostrarCrear();
        } else if ("ADMINISTRADOR".equalsIgnoreCase(sesion.getTipoUsuario())) {
            mostrarPublicacion();
        } else {
            contenedor.setCenter(new Label("Sesión iniciada. El módulo de "
                    + sesion.getTipoUsuario().toLowerCase() + " está pendiente de integración."));
        }
    }

    private void mostrarCrear() {
        stage.setTitle("NextEventux | Crear evento");
        contenedor.setCenter(new ViewEventos(controller, sesion, this::mostrarEdicion, this::mostrarPublicacion).crearPantalla());
    }

    private void mostrarEdicion() {
        stage.setTitle("NextEventux | Editar evento");
        contenedor.setCenter(new ViewEdicion(controller, this::mostrarCrear).crearPantalla());
    }

    private void mostrarPublicacion() {
        stage.setTitle("NextEventux | Publicación de eventos");
        contenedor.setCenter(new ViewPublicacion(publicaciones, () -> {
            if ("ORGANIZADOR".equalsIgnoreCase(sesion.getTipoUsuario())) mostrarCrear();
            else contenedor.setCenter(new Label("Selecciona Publicaciones para revisar las solicitudes."));
        }).crearPantalla());
    }

    public static void main(String[] args) { launch(args); }
}

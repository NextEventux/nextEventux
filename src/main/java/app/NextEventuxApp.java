package app;

import controllers.ControllerAutenticacion;
import controllers.ControllerEventos;
import controllers.ControllerPublicacion;
import controllers.ControllerInvitados;
import controllers.ControllerProveedores;
import controllers.ControllerAdministracion;
import config.CargarOrganizadoresPruebaCU30;
import config.DatabaseConnection;
import java.sql.Connection;

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

import repositories.RepositoryOrganizador;
import repositories.RepositoryAutenticacion;
import repositories.RepositoryDisponibilidadServicio;
import repositories.RepositoryEvento;
import repositories.RepositoryPublicacion;
import repositories.RepositoryServicio;

import services.ServiceAutenticacion;
import services.ServiceEventos;
import services.ServicePublicacion;
import services.ServiceInvitados;
import services.ServiceProveedores;
import services.ServiceAdministracion;

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
        // Preparar los datos de demostración del CU-030 al iniciar.
    prepararDatosPruebaCU30();

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

        if (sesionActual != null) {
            sesionActual.cerrar();
        }

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

        Label usuario = new Label(
                sesion.getNombre() + " - " + tipo);

        Button cerrarSesion = new Button("Cerrar sesión");
        cerrarSesion.setId("cerrarSesion");
        cerrarSesion.setOnAction(event -> mostrarLogin());

        FlowPane barra = new FlowPane(
                15, 8, usuario, cerrarSesion);

        // Acceso a publicaciones para organizadores y administradores.
        if ("ORGANIZADOR".equalsIgnoreCase(tipo)
                || "ADMINISTRADOR".equalsIgnoreCase(tipo)) {

            Button publicar = new Button("Publicaciones");
            publicar.setId("abrirPublicacionSesion");
            publicar.setOnAction(event -> mostrarPublicacion());

            barra.getChildren().add(0, publicar);
        }

        // Acceso al CU-030 únicamente para administradores.
        if ("ADMINISTRADOR".equalsIgnoreCase(tipo)) {

            Button verificar = new Button("Verificar organizadores");
            verificar.setId("abrirVerificarOrganizadores");
            verificar.setOnAction(event -> mostrarAdministracion());

            barra.getChildren().add(0, verificar);
        }

        // Opciones disponibles para el organizador.
        if ("ORGANIZADOR".equalsIgnoreCase(tipo)) {

            Button crear = new Button("Crear evento");
            crear.setId("abrirCrearSesion");
            crear.setOnAction(event -> mostrarEventos());

            Button editar = new Button("Editar evento");
            editar.setId("abrirEdicionSesion");
            editar.setOnAction(event -> mostrarEdicion());

            Button invitados = new Button("Invitados");
            invitados.setId("abrirInvitadosSesion");
            invitados.setOnAction(event -> mostrarInvitados());

            barra.getChildren().addAll(
                    0,
                    java.util.List.of(crear, editar, invitados));

            Button catalogo = new Button("Catálogo de proveedores");
            catalogo.setId("abrirCatalogoSesion");

            catalogo.setOnAction(
                    event -> mostrarCatalogoServicios());

            barra.getChildren().add(1, catalogo);
        }

        barra.setAlignment(Pos.CENTER_RIGHT);
        barra.setPadding(new Insets(12));

        contenedor.setTop(barra);

        // Pantalla inicial según el tipo de usuario.
        if ("ORGANIZADOR".equalsIgnoreCase(tipo)) {

            mostrarEventos();

        } else if ("PROVEEDOR".equalsIgnoreCase(tipo)) {

            mostrarProveedores();

        } else if ("ADMINISTRADOR".equalsIgnoreCase(tipo)) {

            mostrarAdministracion();

        } else {

            mostrarLogin();
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

        // El menú lateral permite navegar entre Eventos e Invitados.
        new Navegacion()
                .registrar(
                        Navegacion.Pantalla.EVENTOS,
                        () -> new ViewEventos(
                                controllerEventos,
                                sesionActual,
                                this::mostrarEdicion,
                                this::mostrarPublicacion)
                                .crearPantalla())
                .registrar(
                        Navegacion.Pantalla.INVITADOS,
                        () -> new ViewInvitados(
                                new ControllerInvitados(
                                        new ServiceInvitados()))
                                .crearPantalla())
                .mostrarEn(
                        contenedor,
                        Navegacion.Pantalla.EVENTOS);
    }

    private void mostrarEdicion() {

        if (!esOrganizador()) {
            mostrarLogin();
            return;
        }

        stage.setTitle("NextEventux | Editar evento");

        contenedor.setCenter(
                new ViewEdicion(
                        controllerEventos,
                        this::mostrarEventos)
                        .crearPantalla());
    }

    private void mostrarInvitados() {

        if (!esOrganizador()) {
            mostrarLogin();
            return;
        }

        stage.setTitle("NextEventux | Invitaciones");

        new Navegacion()
                .registrar(
                        Navegacion.Pantalla.EVENTOS,
                        () -> new ViewEventos(
                                controllerEventos,
                                sesionActual,
                                this::mostrarEdicion,
                                this::mostrarPublicacion)
                                .crearPantalla())
                .registrar(
                        Navegacion.Pantalla.INVITADOS,
                        () -> new ViewInvitados(
                                new ControllerInvitados(
                                        new ServiceInvitados()))
                                .crearPantalla())
                .mostrarEn(
                        contenedor,
                        Navegacion.Pantalla.INVITADOS);
    }

    private void mostrarPublicacion() {

        stage.setTitle("NextEventux | Publicación de eventos");

        contenedor.setCenter(
                new ViewPublicacion(
                        publicaciones,
                        () -> {
                            if (esOrganizador()) {
                                mostrarEventos();
                            } else {
                                contenedor.setCenter(
                                        new Label(
                                                "Selecciona Publicaciones para revisar las solicitudes."));
                            }
                        })
                        .crearPantalla());
    }

    /**
     * Abre el módulo CU-030:
     * verificar, categorizar y aprobar organizadores.
     */
    private void mostrarAdministracion() {

        if (sesionActual == null
                || !"ADMINISTRADOR".equalsIgnoreCase(
                        sesionActual.getTipoUsuario())
                || !"ACTIVO".equalsIgnoreCase(
                        sesionActual.getEstadoUsuario())) {

            mostrarLogin();
            return;
        }

        RepositoryOrganizador repositorio =
                new RepositoryOrganizador();

        ServiceAdministracion servicio =
                new ServiceAdministracion(
                        repositorio,
                        sesionActual.getIdUsuario());

        ControllerAdministracion controlador =
                new ControllerAdministracion(
                        servicio,
                        sesionActual);

        stage.setTitle(
                "NextEventux | Verificar y categorizar organizadores");

        contenedor.setCenter(
                new ViewAdministracion(
                        controlador,
                        sesionActual)
                        .crearPantalla());
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
    private void prepararDatosPruebaCU30() {
    try (Connection connection = DatabaseConnection.getConnection()) {

        CargarOrganizadoresPruebaCU30.cargar(connection);

        System.out.println(
                "Datos de prueba del CU-030 preparados correctamente.");

    } catch (Exception e) {

        System.err.println(
                "No fue posible preparar los datos de prueba del CU-030.");

        e.printStackTrace();
    }
}
}
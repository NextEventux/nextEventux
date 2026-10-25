package views;

import entities.SesionUsuario;
import services.ServiceAvisosProveedor;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import views.components.MenuCompartido;
import java.util.List;

public class ViewAvisosProveedor {
    private final ServiceAvisosProveedor service;
    private final SesionUsuario sesion;
    private final VBox avisos = new VBox(10);
    private final Label mensaje = new Label();
    private final Runnable abrirServicios, abrirSolicitudes, abrirAvisos, cerrarSesion;

    public ViewAvisosProveedor(ServiceAvisosProveedor service, SesionUsuario sesion) {
        this(service, sesion, null, null, null);
    }

    public ViewAvisosProveedor(ServiceAvisosProveedor service, SesionUsuario sesion, Runnable volver, Runnable cerrarSesion) {
        this(service, sesion, volver, null, cerrarSesion);
    }

    public ViewAvisosProveedor(
            ServiceAvisosProveedor service,
            SesionUsuario sesion,
            Runnable abrirServicios,
            Runnable abrirSolicitudes,
            Runnable cerrarSesion) {
        this(service, sesion, abrirServicios, abrirSolicitudes, null, cerrarSesion);
    }

    public ViewAvisosProveedor(
            ServiceAvisosProveedor service,
            SesionUsuario sesion,
            Runnable abrirServicios,
            Runnable abrirSolicitudes,
            Runnable abrirAvisos,
            Runnable cerrarSesion) {
        this.service = service;
        this.sesion = sesion;
        this.abrirServicios = abrirServicios;
        this.abrirSolicitudes = abrirSolicitudes;
        this.abrirAvisos = abrirAvisos;
        this.cerrarSesion = cerrarSesion;
    }

    public Parent crearPantalla() {
        avisos.setId("avisosProveedor");
        avisos.getStyleClass().add("provider-notices");
        Label titulo = new Label("Mis avisos"); titulo.getStyleClass().add("page-title");
        mensaje.setWrapText(true);
        mensaje.getStyleClass().add("provider-notices-error");
        mensaje.setManaged(false);
        mensaje.setVisible(false);

        ScrollPane desplazamiento = new ScrollPane(avisos);
        desplazamiento.setFitToWidth(true);
        desplazamiento.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        desplazamiento.getStyleClass().add("provider-notices-scroll");

        VBox contenido = new VBox(18, titulo, mensaje, desplazamiento);
        contenido.getStyleClass().addAll("app", "content", "provider-notices-content");
        VBox.setVgrow(desplazamiento, Priority.ALWAYS);

        BorderPane principal=new BorderPane(contenido);
        principal.setTop(MenuCompartido.crearBarraSuperior(sesion, "Mis avisos"));
        BorderPane raiz=new BorderPane(principal);
        raiz.setLeft(MenuCompartido.crear(
                sesion,
                MenuCompartido.Pagina.AVISOS_PROVEEDOR,
                null,
                null,
                null,
                abrirServicios,
                cerrarSesion,
                null,
                abrirSolicitudes,
                null,
                null,
                abrirAvisos));
        raiz.getStyleClass().add("app");
        cargarAvisos();
        return raiz;
    }

    private void cargarAvisos() {
        avisos.getChildren().clear();
        mensaje.setText("");
        try {
            List<String> elementos = service.listar(sesion);
            if (elementos.isEmpty()) {
                Label vacio = new Label("No tienes avisos");
                vacio.getStyleClass().add("provider-notices-empty");
                avisos.getChildren().add(vacio);
                return;
            }

            for (String texto : elementos) {
                Label contenido = new Label(texto);
                contenido.setWrapText(true);
                contenido.getStyleClass().add("provider-notice-text");

                VBox tarjeta = new VBox(contenido);
                tarjeta.getStyleClass().add("provider-notice");
                avisos.getChildren().add(tarjeta);
            }
        } catch (IllegalStateException e) {
            mensaje.setText(e.getMessage());
            mensaje.setManaged(true);
            mensaje.setVisible(true);
        }
    }
}

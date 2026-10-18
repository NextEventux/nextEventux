package views;

import controllers.ControllerAutenticacion;
import entities.SesionUsuario;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class ViewLogin {

    private final ControllerAutenticacion controller;
    private final Consumer<SesionUsuario> alIngresar;

    private final TextField usuario =
            new TextField();

    private final PasswordField contrasena =
            new PasswordField();

    private final Label mensaje =
            new Label();

    public ViewLogin(
            ControllerAutenticacion controller,
            Consumer<SesionUsuario> alIngresar) {

        this.controller = controller;
        this.alIngresar = alIngresar;

        usuario.setPromptText("Usuario");
        contrasena.setPromptText("Contraseña");

        usuario.setMaxWidth(Double.MAX_VALUE);
        contrasena.setMaxWidth(Double.MAX_VALUE);

        mensaje.getStyleClass().add(
                "login-error");

        mensaje.setWrapText(true);
        mensaje.setVisible(false);
        mensaje.setManaged(false);
    }

    public Parent crearPantalla() {

        Label icono =
                new Label("🎪");

        icono.getStyleClass().add(
                "login-icon");

        Label marca =
                new Label("nextEventux");

        marca.getStyleClass().add(
                "login-brand");

        Label subtitulo =
                new Label("GESTIÓN DE EVENTOS");

        subtitulo.getStyleClass().add(
                "login-caption");

        VBox cabecera =
                new VBox(
                        5,
                        icono,
                        marca,
                        subtitulo);

        cabecera.setAlignment(
                Pos.CENTER);

        Label titulo =
                new Label("Iniciar sesión");

        titulo.getStyleClass().add(
                "login-title");

        Label ayuda =
                new Label(
                        "Ingresa con tu cuenta para continuar.");

        ayuda.getStyleClass().add(
                "login-muted");

        Button ingresar =
                new Button("Ingresar");

        ingresar.getStyleClass().add(
                "login-button");

        ingresar.setMaxWidth(
                Double.MAX_VALUE);

        ingresar.setOnAction(
                event -> iniciarSesion());

        contrasena.setOnAction(
                event -> iniciarSesion());

        VBox formulario =
                new VBox(
                        14,
                        titulo,
                        ayuda,
                        espacio(),
                        etiqueta("Usuario"),
                        usuario,
                        etiqueta("Contraseña"),
                        contrasena,
                        mensaje,
                        ingresar);

        formulario.getStyleClass().add(
                "login-card");

        VBox contenido =
                new VBox(
                        30,
                        cabecera,
                        formulario);

        contenido.setAlignment(
                Pos.CENTER);

        VBox raiz =
                new VBox(contenido);

        raiz.setAlignment(
                Pos.CENTER);

        raiz.getStyleClass().add(
                "login-root");

        return raiz;
    }

    private void iniciarSesion() {

        try {

            SesionUsuario sesion =
                    controller.iniciarSesion(
                            usuario.getText(),
                            contrasena.getText());

            mensaje.setVisible(false);
            mensaje.setManaged(false);

            alIngresar.accept(sesion);

        } catch (Exception e) {

            mensaje.setText(
                    e.getMessage() == null
                            ? "No se pudo iniciar sesión."
                            : e.getMessage());

            mensaje.setVisible(true);
            mensaje.setManaged(true);
        }
    }

    private Label etiqueta(
            String texto) {

        Label label =
                new Label(texto);

        label.getStyleClass().add(
                "login-label");

        return label;
    }

    private Region espacio() {

        Region region =
                new Region();

        region.setPrefHeight(3);

        return region;
    }
}

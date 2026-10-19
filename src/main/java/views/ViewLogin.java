package views;

import controllers.ControllerAutenticacion;
import entities.SesionUsuario;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class ViewLogin {

    private final ControllerAutenticacion controller;
    private final Consumer<SesionUsuario> alIngresar;

    private final TextField usuario = new TextField();
    private final PasswordField contrasena = new PasswordField();
    private final TextField contrasenaVisible = new TextField();
    private final Label mensaje = new Label();

    private boolean mostrarContrasena;

    public ViewLogin(
            ControllerAutenticacion controller,
            Consumer<SesionUsuario> alIngresar) {

        this.controller = controller;
        this.alIngresar = alIngresar;

        usuario.setId("usuarioLogin");
        usuario.setPromptText("Usuario");

        contrasena.setId("contrasenaLogin");
        contrasena.setPromptText("Contraseña");
        contrasenaVisible.setPromptText("Contraseña");

        usuario.setMaxWidth(Double.MAX_VALUE);
        contrasena.setMaxWidth(Double.MAX_VALUE);
        contrasenaVisible.setMaxWidth(Double.MAX_VALUE);

        contrasenaVisible.textProperty()
                .bindBidirectional(contrasena.textProperty());

        contrasenaVisible.setVisible(false);
        contrasenaVisible.setManaged(false);

        contrasena.getStyleClass().add("login-password-field");
        contrasenaVisible.getStyleClass().add("login-password-field");

        mensaje.getStyleClass().add("login-error");
        mensaje.setId("mensajeLogin");
        mensaje.setWrapText(true);
        mensaje.setVisible(false);
        mensaje.setManaged(false);
    }

    public Parent crearPantalla() {

        Label icono = new Label("🎪");
        icono.getStyleClass().add("login-icon");

        Label marca = new Label("nextEventux");
        marca.getStyleClass().add("login-brand");

        Label subtitulo = new Label("GESTIÓN DE EVENTOS");
        subtitulo.getStyleClass().add("login-caption");

        VBox cabecera = new VBox(5, icono, marca, subtitulo);
        cabecera.setAlignment(Pos.CENTER);

        Label titulo = new Label("Iniciar sesión");
        titulo.getStyleClass().add("login-title");

        Label ayuda = new Label(
                "Ingresa con tu cuenta para continuar.");

        ayuda.getStyleClass().add("login-muted");

        Button ingresar = new Button("Ingresar");
        ingresar.setId("ingresarLogin");
        ingresar.getStyleClass().add("login-button");
        ingresar.setMaxWidth(Double.MAX_VALUE);

        ingresar.setOnAction(event -> iniciarSesion());

        contrasena.setOnAction(event -> iniciarSesion());
        contrasenaVisible.setOnAction(event -> iniciarSesion());

        VBox formulario = new VBox(
                14,
                titulo,
                ayuda,
                espacio(),
                etiqueta("Usuario"),
                usuario,
                etiqueta("Contraseña"),
                crearCampoContrasena(),
                mensaje,
                ingresar);

        formulario.getStyleClass().add("login-card");

        VBox contenido = new VBox(30, cabecera, formulario);
        contenido.setAlignment(Pos.CENTER);

        VBox raiz = new VBox(contenido);
        raiz.setAlignment(Pos.CENTER);
        raiz.getStyleClass().add("login-root");

        return raiz;
    }

    private StackPane crearCampoContrasena() {

        Button ver = new Button("👁");
        ver.getStyleClass().add("login-eye-button");
        ver.setFocusTraversable(false);

        StackPane.setAlignment(ver, Pos.CENTER_RIGHT);
        StackPane.setMargin(
                ver,
                new Insets(0, 8, 0, 0));

        ver.setOnAction(event -> alternarContrasena());

        StackPane campo = new StackPane(
                contrasena,
                contrasenaVisible,
                ver);

        campo.getStyleClass().add("login-password-box");

        return campo;
    }

    private void alternarContrasena() {

        mostrarContrasena = !mostrarContrasena;

        contrasenaVisible.setVisible(mostrarContrasena);
        contrasenaVisible.setManaged(mostrarContrasena);

        contrasena.setVisible(!mostrarContrasena);
        contrasena.setManaged(!mostrarContrasena);

        if (mostrarContrasena) {

            contrasenaVisible.requestFocus();

            contrasenaVisible.positionCaret(
                    contrasenaVisible.getText().length());

        } else {

            contrasena.requestFocus();

            contrasena.positionCaret(
                    contrasena.getText().length());
        }
    }

    private void iniciarSesion() {

        final SesionUsuario sesion;

        // Primera etapa: autenticar las credenciales.
        try {

            sesion = controller.iniciarSesion(
                    usuario.getText(),
                    contrasena.getText());

            if (sesion == null) {
                mostrarMensaje("No se recibió una sesión válida.");
                return;
            }

        } catch (Exception e) {

            System.err.println(
                    "\n========== ERROR DURANTE LA AUTENTICACIÓN ==========");

            e.printStackTrace();

            System.err.println(
                    "====================================================\n");

            mostrarMensaje(
                    e.getMessage() == null
                            ? "No se pudo iniciar sesión."
                            : e.getMessage());

            return;
        }

        mensaje.setVisible(false);
        mensaje.setManaged(false);

        // Segunda etapa: abrir la pantalla del usuario autenticado.
        try {

            alIngresar.accept(sesion);

        } catch (Exception e) {

            System.err.println(
                    "\n========== ERROR AL ABRIR EL MÓDULO ==========");

            e.printStackTrace();

            System.err.println(
                    "==============================================\n");

            mostrarMensaje(
                    "Autenticación correcta, pero no se pudo abrir el módulo.");
        }
    }

    private void mostrarMensaje(String texto) {

        mensaje.setText(texto);
        mensaje.setVisible(true);
        mensaje.setManaged(true);
    }

    private Label etiqueta(String texto) {

        Label label = new Label(texto);
        label.getStyleClass().add("login-label");

        return label;
    }

    private Region espacio() {

        Region region = new Region();
        region.setPrefHeight(3);

        return region;
    }
}
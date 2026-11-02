
package views;

import controllers.ControllerAdministracion;
import entities.SesionUsuario;
import repositories.RepositoryAdministracion.UsuarioResumen;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * CU-029: Gestion de usuarios.
 * Permite consultar, suspender y reactivar cuentas.
 */
public class ViewSuspenderUsuario {

    private final ControllerAdministracion controller;
    private final SesionUsuario sesion;

    private final ObservableList<UsuarioResumen> usuarios =
            FXCollections.observableArrayList();

    private TableView<UsuarioResumen> tabla;
    private TextField campoBusqueda;
    private TextArea campoMotivo;

    private Label mensajeEstado;
    private Label usuarioSeleccionado;
    private Label tituloPanel;
    private Label etiquetaMotivo;

    private Button botonAccion;

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public ViewSuspenderUsuario(
            ControllerAdministracion controller,
            SesionUsuario sesion) {

        if (controller == null || sesion == null) {
            throw new IllegalArgumentException(
                    "El controlador y la sesion son obligatorios.");
        }

        this.controller = controller;
        this.sesion = sesion;
    }

    public Parent crearPantalla() {

        BorderPane raiz = new BorderPane();
        raiz.getStyleClass().add("app");

        VBox contenido = new VBox(22);
        contenido.getStyleClass().add("content");
        contenido.setPadding(new Insets(30));
        contenido.setMaxWidth(1250);

        Label titulo = new Label("Gestión de usuarios");
        titulo.getStyleClass().add("page-title");

        Label subtitulo = new Label(
                "Consulta las cuentas registradas y gestiona "
                        + "las suspensiones y reactivaciones de usuarios.");

        subtitulo.getStyleClass().add("muted");
        subtitulo.setWrapText(true);

        VBox encabezado = new VBox(7, titulo, subtitulo);

        contenido.getChildren().addAll(
                encabezado,
                crearPanelBusqueda(),
                crearTabla(),
                crearPanelAcciones()
        );

        VBox.setVgrow(tabla, Priority.ALWAYS);

        StackPane centrado = new StackPane(contenido);
        centrado.setAlignment(Pos.TOP_CENTER);

        ScrollPane scroll = new ScrollPane(centrado);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("content-scroll");

        raiz.setCenter(scroll);

        cargarUsuarios("");

        return raiz;
    }

    // ==========================================================
    // BUSQUEDA
    // ==========================================================

    private VBox crearPanelBusqueda() {

        Label titulo = new Label("Buscar usuarios");
        titulo.getStyleClass().add("section-title");

        campoBusqueda = new TextField();
        campoBusqueda.setPromptText(
                "Nombre, usuario o identificador...");

        campoBusqueda.setMaxWidth(Double.MAX_VALUE);

        Button buscar = new Button("Buscar");
        buscar.getStyleClass().add("primary-button");

        buscar.setOnAction(e ->
                cargarUsuarios(campoBusqueda.getText()));

        Button limpiar = new Button("Limpiar");

        limpiar.setOnAction(e -> {
            campoBusqueda.clear();
            cargarUsuarios("");
        });

        HBox acciones = new HBox(
                10, campoBusqueda, buscar, limpiar);

        acciones.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(campoBusqueda, Priority.ALWAYS);

        VBox panel = new VBox(12, titulo, acciones);
        panel.getStyleClass().add("publicacion");
        panel.setPadding(new Insets(18));

        return panel;
    }

    // ==========================================================
    // TABLA DE USUARIOS
    // ==========================================================

    private TableView<UsuarioResumen> crearTabla() {

        tabla = new TableView<>();
        tabla.setItems(usuarios);

        tabla.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<UsuarioResumen, Number> columnaId =
                new TableColumn<>("ID");

        columnaId.setCellValueFactory(data ->
                new javafx.beans.property.SimpleIntegerProperty(
                        data.getValue().idUsuario()));

        TableColumn<UsuarioResumen, String> columnaNombre =
                new TableColumn<>("Nombre");

        columnaNombre.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().nombre()));

        TableColumn<UsuarioResumen, String> columnaUsuario =
                new TableColumn<>("Usuario");

        columnaUsuario.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().usuario()));

        TableColumn<UsuarioResumen, String> columnaTipo =
                new TableColumn<>("Tipo");

        columnaTipo.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().tipoUsuario()));

        TableColumn<UsuarioResumen, String> columnaEstado =
                new TableColumn<>("Estado");

        columnaEstado.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().estado()));

        TableColumn<UsuarioResumen, String> columnaFecha =
                new TableColumn<>("Registro");

        columnaFecha.setCellValueFactory(data -> {

            Timestamp fecha = data.getValue().fechaRegistro();

            String texto = fecha == null
                    ? "No disponible"
                    : fecha.toLocalDateTime()
                    .format(FORMATO_FECHA);

            return new javafx.beans.property.SimpleStringProperty(
                    texto);
        });

        tabla.getColumns().addAll(
                columnaId,
                columnaNombre,
                columnaUsuario,
                columnaTipo,
                columnaEstado,
                columnaFecha
        );

        tabla.setPlaceholder(
                new Label("No se encontraron usuarios."));

        tabla.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, anterior, actual) ->
                        actualizarSeleccion(actual));

        return tabla;
    }

    // ==========================================================
    // PANEL DINAMICO DE ACCIONES
    // ==========================================================

    private VBox crearPanelAcciones() {

        tituloPanel = new Label("Gestionar cuenta");
        tituloPanel.getStyleClass().add("section-title");

        usuarioSeleccionado = new Label(
                "Selecciona un usuario de la tabla.");

        usuarioSeleccionado.getStyleClass().add("muted");

        etiquetaMotivo = new Label("Motivo de la operación");

        campoMotivo = new TextArea();

        campoMotivo.setPromptText(
                "Describe el motivo de la suspensión "
                        + "o la justificación de la reactivación...");

        campoMotivo.setWrapText(true);
        campoMotivo.setPrefRowCount(3);
        campoMotivo.setMaxWidth(Double.MAX_VALUE);

        Label contador = new Label("0 / 500 caracteres");

        campoMotivo.textProperty().addListener(
                (obs, anterior, actual) -> {

                    contador.setText(
                            actual.length() + " / 500 caracteres");

                    if (actual.length() > 500) {
                        contador.setTextFill(Color.FIREBRICK);
                    } else {
                        contador.setTextFill(Color.GRAY);
                    }
                });

        mensajeEstado = new Label();
        mensajeEstado.setWrapText(true);

        botonAccion = new Button("Selecciona un usuario");
        botonAccion.getStyleClass().add("primary-button");
        botonAccion.setDisable(true);

        botonAccion.setOnAction(e -> ejecutarAccion());

        VBox panel = new VBox(
                12,
                tituloPanel,
                usuarioSeleccionado,
                etiquetaMotivo,
                campoMotivo,
                contador,
                botonAccion,
                mensajeEstado
        );

        panel.getStyleClass().add("publicacion");
        panel.setPadding(new Insets(20));

        return panel;
    }

    // ==========================================================
    // CARGA Y SELECCION
    // ==========================================================

    private void cargarUsuarios(String filtro) {

        try {

            List<UsuarioResumen> resultado =
                    controller.consultarUsuarios(filtro);

            usuarios.setAll(resultado);

            actualizarSeleccion(null);

            mostrarMensaje(
                    resultado.size() + " usuario(s) encontrado(s).",
                    false);

        } catch (RuntimeException e) {

            mostrarMensaje(
                    e.getMessage() == null
                            ? "No fue posible consultar los usuarios."
                            : e.getMessage(),
                    true);
        }
    }

    private void actualizarSeleccion(UsuarioResumen usuario) {

        campoMotivo.clear();

        if (usuario == null) {

            usuarioSeleccionado.setText(
                    "Selecciona un usuario de la tabla.");

            tituloPanel.setText("Gestionar cuenta");
            etiquetaMotivo.setText("Motivo de la operación");
            campoMotivo.setPromptText(
                    "Selecciona una cuenta para continuar.");

            botonAccion.setText("Selecciona un usuario");
            botonAccion.setDisable(true);

            return;
        }

        usuarioSeleccionado.setText(
                "Usuario seleccionado: "
                        + usuario.nombre()
                        + " | ID: " + usuario.idUsuario()
                        + " | Tipo: " + usuario.tipoUsuario()
                        + " | Estado: " + usuario.estado());

        botonAccion.setDisable(false);

        // Cuenta activa.
        if ("ACTIVO".equalsIgnoreCase(usuario.estado())) {

            tituloPanel.setText("Suspender cuenta");

            etiquetaMotivo.setText(
                    "Motivo de la suspensión (obligatorio)");

            campoMotivo.setPromptText(
                    "Describe el motivo de la suspensión...");

            botonAccion.setText("Suspender usuario");

            botonAccion.setStyle("");

            boolean esAdministrador =
                    "ADMINISTRADOR".equalsIgnoreCase(
                            usuario.tipoUsuario());

            boolean esPropiaCuenta =
                    usuario.idUsuario() == sesion.getIdUsuario();

            if (esAdministrador) {

                botonAccion.setDisable(true);

                mostrarMensaje(
                        "Las cuentas administrativas están protegidas "
                                + "y no pueden suspenderse.",
                        true);

            } else if (esPropiaCuenta) {

                botonAccion.setDisable(true);

                mostrarMensaje(
                        "No puedes suspender tu propia cuenta.",
                        true);

            } else {

                mostrarMensaje(
                        "Usuario listo para validar la suspensión.",
                        false);
            }

            // Cuenta suspendida.
        } else if ("SUSPENDIDO".equalsIgnoreCase(usuario.estado())) {

            tituloPanel.setText("Reactivar cuenta");

            etiquetaMotivo.setText(
                    "Justificación de reactivación (opcional)");

            campoMotivo.setPromptText(
                    "Puedes agregar una observación sobre la reactivación...");

            botonAccion.setText("Reactivar usuario");

            botonAccion.setStyle(
                    "-fx-background-color: #238B57; "
                            + "-fx-text-fill: white;");

            mostrarMensaje(
                    "Esta cuenta está suspendida y puede reactivarse.",
                    false);

        } else {

            tituloPanel.setText("Cuenta no disponible");

            botonAccion.setText("Acción no disponible");
            botonAccion.setDisable(true);

            mostrarMensaje(
                    "El estado actual de esta cuenta no permite "
                            + "suspenderla ni reactivarla.",
                    true);
        }
    }

    // ==========================================================
    // EJECUCION DE ACCIONES
    // ==========================================================

    private void ejecutarAccion() {

        UsuarioResumen seleccionado =
                tabla.getSelectionModel().getSelectedItem();

        if (seleccionado == null) {

            mostrarMensaje(
                    "Selecciona primero un usuario.",
                    true);

            return;
        }

        if ("ACTIVO".equalsIgnoreCase(seleccionado.estado())) {

            confirmarSuspension(seleccionado);

        } else if ("SUSPENDIDO".equalsIgnoreCase(
                seleccionado.estado())) {

            confirmarReactivacion(seleccionado);

        } else {

            mostrarMensaje(
                    "No se puede realizar esta operación.",
                    true);
        }
    }

    // ==========================================================
    // SUSPENSION
    // ==========================================================

    private void confirmarSuspension(UsuarioResumen seleccionado) {

        String motivo = campoMotivo.getText();

        if (motivo == null || motivo.isBlank()) {

            mostrarMensaje(
                    "Debes ingresar el motivo de la suspensión.",
                    true);

            return;
        }

        if (motivo.trim().length() > 500) {

            mostrarMensaje(
                    "El motivo no puede superar los 500 caracteres.",
                    true);

            return;
        }

        try {

            controller.validarSuspension(
                    seleccionado.idUsuario(), motivo);

        } catch (RuntimeException e) {

            mostrarMensaje(e.getMessage(), true);
            return;
        }

        Alert confirmacion =
                new Alert(Alert.AlertType.CONFIRMATION);

        confirmacion.setTitle("Confirmar suspensión");

        confirmacion.setHeaderText(
                "¿Deseas suspender esta cuenta?");

        confirmacion.setContentText(
                "Usuario: " + seleccionado.nombre()
                        + "\nID: " + seleccionado.idUsuario()
                        + "\nMotivo: " + motivo.trim()
                        + "\n\nEsta operación impedirá el acceso "
                        + "a la cuenta.");

        Optional<ButtonType> respuesta =
                confirmacion.showAndWait();

        if (respuesta.isEmpty()
                || respuesta.get() != ButtonType.OK) {

            mostrarMensaje(
                    "La operación fue cancelada.",
                    false);

            return;
        }

        try {

            controller.suspenderUsuario(
                    seleccionado.idUsuario(),
                    motivo);

            mostrarMensaje(
                    "La cuenta fue suspendida correctamente.",
                    false);

            campoMotivo.clear();

            cargarUsuarios(campoBusqueda.getText());

        } catch (RuntimeException e) {

            mostrarMensaje(
                    e.getMessage() == null
                            ? "No fue posible suspender la cuenta."
                            : e.getMessage(),
                    true);
        }
    }

    // ==========================================================
    // REACTIVACION
    // ==========================================================

    private void confirmarReactivacion(
            UsuarioResumen seleccionado) {

        String motivo = campoMotivo.getText();

        if (motivo != null && motivo.trim().length() > 500) {

            mostrarMensaje(
                    "La justificación no puede superar los 500 caracteres.",
                    true);

            return;
        }

        Alert confirmacion =
                new Alert(Alert.AlertType.CONFIRMATION);

        confirmacion.setTitle("Confirmar reactivación");

        confirmacion.setHeaderText(
                "¿Deseas reactivar esta cuenta?");

        String justificacion =
                motivo == null || motivo.isBlank()
                        ? "Sin observaciones adicionales."
                        : motivo.trim();

        confirmacion.setContentText(
                "Usuario: " + seleccionado.nombre()
                        + "\nID: " + seleccionado.idUsuario()
                        + "\nJustificación: " + justificacion
                        + "\n\nEl usuario podrá volver a ingresar "
                        + "a NextEventux.");

        Optional<ButtonType> respuesta =
                confirmacion.showAndWait();

        if (respuesta.isEmpty()
                || respuesta.get() != ButtonType.OK) {

            mostrarMensaje(
                    "La reactivación fue cancelada.",
                    false);

            return;
        }

        try {

            controller.reactivarUsuario(
                    seleccionado.idUsuario(),
                    motivo);

            mostrarMensaje(
                    "La cuenta fue reactivada correctamente.",
                    false);

            campoMotivo.clear();

            cargarUsuarios(campoBusqueda.getText());

        } catch (RuntimeException e) {

            mostrarMensaje(
                    e.getMessage() == null
                            ? "No fue posible reactivar la cuenta."
                            : e.getMessage(),
                    true);
        }
    }

    // ==========================================================
    // MENSAJES
    // ==========================================================

    private void mostrarMensaje(String mensaje, boolean error) {

        mensajeEstado.setText(
                mensaje == null ? "" : mensaje);

        mensajeEstado.setTextFill(
                error
                        ? Color.FIREBRICK
                        : Color.FORESTGREEN);
    }
}

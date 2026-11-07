package views;

import java.util.Locale;
import java.util.Optional;

import controllers.ControllerAdministracion;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import repositories.RepositoryAdministracion.UsuarioAdministrable;
import services.ServiceAdministracion;

/**
 * Vista de administracion de cuentas de usuario.
 *
 * CU-029 Suspender usuario: consultar usuarios -> seleccionar -> Suspender
 * -> motivo obligatorio -> resumen y confirmacion -> resultado.
 *
 * Ampliacion (fuera del flujo oficial de CU-029): Reactivar usuario para
 * cuentas suspendidas, con confirmacion previa.
 *
 * La vista no decide permisos: el servicio valida al administrador en la
 * base de datos en cada operacion.
 */
public class ViewUsuariosAdministracion {

    private final ControllerAdministracion controller;

    private final TextField buscar = new TextField();
    private final ListView<UsuarioAdministrable> listaUsuarios = new ListView<>();

    private final Label detalleNombre = new Label("Selecciona un usuario");
    private final Label detalleUsuario = new Label(FormatoUi.SIN_DATO);
    private final Label detalleTipo = new Label(FormatoUi.SIN_DATO);
    private final Label detalleEstado = new Label(FormatoUi.SIN_DATO);
    private final Label detalleRegistro = new Label(FormatoUi.SIN_DATO);
    private final Label detalleMotivo = new Label(FormatoUi.SIN_DATO);
    private VBox campoMotivoVigente;

    private final TextArea motivo = new TextArea();
    private final Button suspender = new Button("Suspender usuario");
    private final Button reactivar = new Button("Reactivar usuario");
    private final Label mensaje = new Label();

    private UsuarioAdministrable seleccionado;
    private boolean refrescandoLista;

    public ViewUsuariosAdministracion(ControllerAdministracion controller) {
        if (controller == null) {
            throw new IllegalArgumentException(
                    "El controlador de administración es obligatorio.");
        }
        this.controller = controller;
    }

    public Parent crearPantalla() {

        listaUsuarios.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(UsuarioAdministrable u, boolean vacio) {
                super.updateItem(u, vacio);
                setText(vacio || u == null ? null
                        : u.nombre() + " · " + legible(u.estado()));
            }
        });
        listaUsuarios.setPlaceholder(new Label("No hay usuarios para mostrar."));
        listaUsuarios.getSelectionModel().selectedItemProperty()
                .addListener((obs, anterior, nuevo) -> {
                    if (!refrescandoLista) {
                        seleccionar(nuevo);
                    }
                });

        buscar.setPromptText("Nombre o usuario");
        buscar.setOnAction(e -> buscar());
        Button botonBuscar = new Button("Buscar");
        botonBuscar.getStyleClass().add("btn-secondary");
        botonBuscar.setOnAction(e -> buscar());

        motivo.setWrapText(true);
        motivo.setPrefRowCount(4);
        motivo.setPromptText("Obligatorio para suspender. Máximo "
                + ServiceAdministracion.MAX_MOTIVO + " caracteres.");

        suspender.getStyleClass().add("btn-primary");
        suspender.setOnAction(e -> confirmarYSuspender());
        reactivar.getStyleClass().add("btn-secondary");
        reactivar.setOnAction(e -> confirmarYReactivar());
        mensaje.setWrapText(true);
        mensaje.setVisible(false);
        mensaje.setManaged(false);

        Label titulo = new Label("Usuarios");
        titulo.getStyleClass().add("page-title");
        Label descripcion = new Label(
                "Consulta las cuentas registradas, suspende el acceso de un usuario "
                + "indicando el motivo o reactiva una cuenta suspendida.");
        descripcion.getStyleClass().add("muted");
        descripcion.setWrapText(true);
        VBox encabezado = new VBox(8, titulo, descripcion);

        Label tituloBusqueda = new Label("Buscar usuario");
        tituloBusqueda.getStyleClass().add("card-title");
        buscar.setMinWidth(0);
        buscar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(buscar, Priority.ALWAYS);
        botonBuscar.setMinWidth(Region.USE_PREF_SIZE);
        HBox filaBusqueda = new HBox(10, buscar, botonBuscar);
        listaUsuarios.getStyleClass().add("lista-proveedores-sanciones");
        listaUsuarios.setPrefHeight(360);

        VBox izquierda = new VBox(18, tituloBusqueda, filaBusqueda, listaUsuarios);
        izquierda.getStyleClass().add("card");
        izquierda.setPrefWidth(380);
        izquierda.setMinWidth(330);

        Label tituloDetalle = new Label("Detalle de la cuenta");
        tituloDetalle.getStyleClass().add("card-title");
        detalleNombre.setWrapText(true);
        detalleMotivo.setWrapText(true);

        GridPane datos = new GridPane();
        datos.setHgap(18);
        datos.setVgap(18);
        ColumnConstraints columnaIzquierda = new ColumnConstraints();
        columnaIzquierda.setPercentWidth(50);
        ColumnConstraints columnaDerecha = new ColumnConstraints();
        columnaDerecha.setPercentWidth(50);
        datos.getColumnConstraints().addAll(columnaIzquierda, columnaDerecha);
        datos.add(campo("Nombre", detalleNombre), 0, 0, 2, 1);
        datos.add(campo("Usuario", detalleUsuario), 0, 1);
        datos.add(campo("Tipo de cuenta", detalleTipo), 1, 1);
        datos.add(campo("Estado actual", detalleEstado), 0, 2);
        datos.add(campo("Fecha de registro", detalleRegistro), 1, 2);
        campoMotivoVigente = campo("Motivo de la suspensión vigente", detalleMotivo);
        datos.add(campoMotivoVigente, 0, 3, 2, 1);

        HBox acciones = new HBox(12, suspender, reactivar);
        acciones.setAlignment(Pos.CENTER_LEFT);

        VBox derecha = new VBox(20,
                tituloDetalle,
                datos,
                campo("Motivo de la suspensión", motivo),
                acciones,
                mensaje);
        derecha.getStyleClass().add("card");
        derecha.setMinWidth(0);
        HBox.setHgrow(derecha, Priority.ALWAYS);

        HBox cuerpo = new HBox(24, izquierda, derecha);
        cuerpo.setAlignment(Pos.TOP_LEFT);
        cuerpo.setFillHeight(false);

        VBox contenido = new VBox(24, encabezado, cuerpo);
        contenido.getStyleClass().add("content");
        contenido.setMaxWidth(1120);

        StackPane centrado = new StackPane(contenido);
        centrado.setAlignment(Pos.TOP_CENTER);

        ScrollPane scroll = new ScrollPane(centrado);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("content-scroll");

        mostrarDetalle(null, null);
        buscar();
        return scroll;
    }

    // ------------------------------------------------------------------
    // Consulta (paso 1 del flujo basico)
    // ------------------------------------------------------------------

    private void buscar() {
        cargarLista(null);
    }

    /** Recarga la lista con el filtro actual y vuelve a seleccionar la cuenta indicada. */
    private void cargarLista(Integer idSeleccionar) {
        try {
            refrescandoLista = true;
            listaUsuarios.getItems().setAll(controller.consultarUsuarios(buscar.getText()));
        } catch (RuntimeException ex) {
            mostrarError(ex);
            return;
        } finally {
            refrescandoLista = false;
        }

        UsuarioAdministrable aSeleccionar = idSeleccionar == null ? null
                : listaUsuarios.getItems().stream()
                        .filter(u -> u.idUsuario() == idSeleccionar)
                        .findFirst()
                        .orElse(null);
        if (aSeleccionar != null) {
            listaUsuarios.getSelectionModel().select(aSeleccionar);
        } else {
            listaUsuarios.getSelectionModel().clearSelection();
            seleccionar(null);
        }
    }

    private void seleccionar(UsuarioAdministrable elegido) {
        limpiarMensaje();
        if (elegido == null) {
            mostrarDetalle(null, null);
            return;
        }
        try {
            // Se consulta de nuevo para mostrar el estado actual de la cuenta.
            UsuarioAdministrable actual = controller.consultarUsuario(elegido.idUsuario());
            String motivoVigente = null;
            if (esSuspendida(actual)) {
                try {
                    motivoVigente = controller.validarReactivacion(actual.idUsuario());
                } catch (IllegalStateException sinHistorial) {
                    // Solo el historial inconsistente se muestra como dato del detalle;
                    // cualquier otro error (por ejemplo, de base de datos) se informa.
                    if (!ServiceAdministracion.HISTORIAL_INCONSISTENTE
                            .equals(sinHistorial.getMessage())) {
                        throw sinHistorial;
                    }
                    motivoVigente = "Sin suspensión vigente registrada.";
                }
            }
            mostrarDetalle(actual, motivoVigente);
        } catch (RuntimeException ex) {
            mostrarDetalle(null, null);
            mostrarError(ex);
        }
    }

    private void mostrarDetalle(UsuarioAdministrable u, String motivoVigente) {
        seleccionado = u;
        if (u == null) {
            detalleNombre.setText("Selecciona un usuario");
            detalleUsuario.setText(FormatoUi.SIN_DATO);
            detalleTipo.setText(FormatoUi.SIN_DATO);
            detalleEstado.setText(FormatoUi.SIN_DATO);
            detalleRegistro.setText(FormatoUi.SIN_DATO);
        } else {
            detalleNombre.setText(u.nombre());
            detalleUsuario.setText(u.usuario());
            detalleTipo.setText(legible(u.tipoUsuario()));
            detalleEstado.setText(legible(u.estado()));
            detalleRegistro.setText(FormatoUi.fechaHora(u.fechaRegistro()));
        }
        boolean suspendida = u != null && esSuspendida(u);
        detalleMotivo.setText(motivoVigente == null ? FormatoUi.SIN_DATO : motivoVigente);
        campoMotivoVigente.setVisible(suspendida);
        campoMotivoVigente.setManaged(suspendida);

        boolean activa = u != null
                && ServiceAdministracion.ESTADO_ACTIVO.equalsIgnoreCase(u.estado());
        suspender.setDisable(!activa);
        motivo.setDisable(!activa);
        reactivar.setDisable(!suspendida);
    }

    // ------------------------------------------------------------------
    // CU-029: suspender (pasos 2 a 5, A1, A2, E1 y E2)
    // ------------------------------------------------------------------

    private void confirmarYSuspender() {
        if (seleccionado == null) {
            mostrarMensajeError("Selecciona un usuario.");
            return;
        }
        UsuarioAdministrable usuario = seleccionado;
        try {
            // Paso 2 y A2: la cuenta debe seguir activa y poder suspenderse.
            controller.validarSuspension(usuario.idUsuario(), null);
            // Pasos 3 y E1: el motivo es obligatorio.
            if (motivo.getText() == null || motivo.getText().isBlank()) {
                mostrarMensajeError(ServiceAdministracion.MOTIVO_OBLIGATORIO);
                motivo.requestFocus();
                return;
            }
            controller.validarSuspension(usuario.idUsuario(), motivo.getText());
        } catch (RuntimeException ex) {
            seleccionar(usuario);
            mostrarError(ex);
            return;
        }

        String resumen = "Vas a suspender la siguiente cuenta:\n\n"
                + "Nombre: " + usuario.nombre() + "\n"
                + "Usuario: " + usuario.usuario() + "\n"
                + "Tipo de cuenta: " + legible(usuario.tipoUsuario()) + "\n"
                + "Motivo: " + motivo.getText().trim().replaceAll("\\s+", " ") + "\n\n"
                + "El usuario no podrá iniciar sesión hasta que la cuenta sea reactivada "
                + "y recibirá una notificación interna.";

        if (!confirmar("Confirmar suspensión", resumen, "Suspender")) {
            // A1: no se modifica la cuenta.
            mostrarMensaje("Suspensión cancelada. La cuenta no fue modificada.", false);
            return;
        }

        suspender.setDisable(true);
        try {
            controller.suspenderUsuario(usuario.idUsuario(), motivo.getText());
            motivo.clear();
            String exito = "La cuenta de " + usuario.nombre()
                    + " fue suspendida y el usuario fue notificado.";
            cargarLista(usuario.idUsuario());
            mostrarMensaje(exito, false);
            informar(exito);
        } catch (RuntimeException ex) {
            // E2: el servicio revirtio todo; se conserva el motivo para reintentar.
            seleccionar(usuario);
            mostrarError(ex);
        }
    }

    // ------------------------------------------------------------------
    // Ampliacion: reactivar usuario
    // ------------------------------------------------------------------

    private void confirmarYReactivar() {
        if (seleccionado == null) {
            mostrarMensajeError("Selecciona un usuario.");
            return;
        }
        UsuarioAdministrable usuario = seleccionado;
        String motivoVigente;
        try {
            motivoVigente = controller.validarReactivacion(usuario.idUsuario());
        } catch (RuntimeException ex) {
            // Incluye el historial inconsistente: no se ofrece la reactivacion.
            mostrarError(ex);
            return;
        }

        String resumen = "Vas a reactivar la siguiente cuenta:\n\n"
                + "Nombre: " + usuario.nombre() + "\n"
                + "Usuario: " + usuario.usuario() + "\n"
                + "Tipo de cuenta: " + legible(usuario.tipoUsuario()) + "\n"
                + "Motivo de la suspensión: " + motivoVigente + "\n\n"
                + "El usuario recuperará el acceso que le permiten sus roles y "
                + "recibirá una notificación interna. El historial se conserva.";

        if (!confirmar("Confirmar reactivación", resumen, "Reactivar")) {
            mostrarMensaje("Reactivación cancelada. La cuenta no fue modificada.", false);
            return;
        }

        reactivar.setDisable(true);
        try {
            controller.reactivarUsuario(usuario.idUsuario());
            String exito = "La cuenta de " + usuario.nombre()
                    + " fue reactivada y el usuario fue notificado.";
            cargarLista(usuario.idUsuario());
            mostrarMensaje(exito, false);
            informar(exito);
        } catch (RuntimeException ex) {
            seleccionar(usuario);
            mostrarError(ex);
        }
    }

    // ------------------------------------------------------------------
    // Utilidades de presentacion
    // ------------------------------------------------------------------

    private static boolean esSuspendida(UsuarioAdministrable u) {
        return ServiceAdministracion.ESTADO_SUSPENDIDO.equalsIgnoreCase(u.estado());
    }

    /** ACTIVO -> Activo, ADMINISTRADOR -> Administrador. */
    static String legible(String valor) {
        if (valor == null || valor.isBlank()) {
            return FormatoUi.SIN_DATO;
        }
        String minusculas = valor.trim().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(minusculas.charAt(0)) + minusculas.substring(1);
    }

    private VBox campo(String texto, Region control) {
        Label etiqueta = new Label(texto);
        etiqueta.getStyleClass().add("field-label");
        control.setMaxWidth(Double.MAX_VALUE);
        VBox campo = new VBox(8, etiqueta, control);
        campo.setMaxWidth(Double.MAX_VALUE);
        return campo;
    }

    private boolean confirmar(String titulo, String texto, String accion) {
        ButtonType aceptar = new ButtonType(accion, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION, texto, aceptar, cancelar);
        alerta.setTitle(titulo);
        alerta.setHeaderText(titulo);
        Optional<ButtonType> respuesta = alerta.showAndWait();
        return respuesta.isPresent() && respuesta.get() == aceptar;
    }

    private void informar(String texto) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION, texto);
        alerta.setHeaderText("Operación completada");
        alerta.showAndWait();
    }

    private void mostrarError(RuntimeException ex) {
        registrarCausaTecnica(ex);
        String texto = ex.getMessage() == null || ex.getMessage().isBlank()
                ? "Ocurrió un error inesperado. Vuelve a intentarlo."
                : ex.getMessage();
        mostrarMensajeError(texto);
        new Alert(Alert.AlertType.ERROR, texto).showAndWait();
    }

    /**
     * El mensaje para el administrador es claro y no técnico (RNF-02); la
     * excepción original se registra en la consola para poder diagnosticarla.
     */
    private static void registrarCausaTecnica(RuntimeException ex) {
        if (ex.getCause() == null) {
            return;
        }
        System.err.printf("[USUARIOS] %s: %s%n",
                ex.getClass().getSimpleName(), ex.getMessage());
        Throwable causa = ex.getCause();
        while (causa != null) {
            System.err.printf("[USUARIOS]   causa %s: %s%n",
                    causa.getClass().getName(), causa.getMessage());
            causa = causa.getCause();
        }
        ex.printStackTrace(System.err);
    }

        private void mostrarMensajeError(String texto) {
        mostrarMensaje(texto, true);
    }

    /** Mismo patrón de mensajes que ViewPublicacion (.message.success / .message.error). */
    private void mostrarMensaje(String texto, boolean error) {
        mensaje.setText(texto);
        mensaje.getStyleClass().setAll("label", "message", error ? "error" : "success");
        mensaje.setVisible(true);
        mensaje.setManaged(true);
    }

    private void limpiarMensaje() {
        mensaje.setText("");
        mensaje.setVisible(false);
        mensaje.setManaged(false);
    }
}

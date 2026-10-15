package views;

import controllers.ControllerAdministracion;
import entities.Proveedor;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.List;

/** Interfaz JavaFX de la primera iteración del CU-028: Aprobar proveedores. */
public class ViewAdministracion {
    private final ControllerAdministracion controller;
    private final TableView<Proveedor> tabla = new TableView<>();
    private final Label nombre = new Label("Selecciona un proveedor");
    private final Label empresa = new Label("—");
    private final Label correo = new Label("—");
    private final Label telefono = new Label("—");
    private final Label direccion = new Label("—");
    private final Label estado = new Label("Sin selección");
    private final Label mensaje = new Label();
    private final Label cantidadPendientes = new Label("0");
    private final Button validar = new Button("✓  Verificar información");
    private final Button aprobar = new Button("Aprobar proveedor");
    private final Button rechazar = new Button("Rechazar proveedor");
    private boolean informacionValidada;

    public ViewAdministracion(ControllerAdministracion controller) {
        if (controller == null) {
            throw new IllegalArgumentException("El controlador de administración es obligatorio.");
        }
        this.controller = controller;
        configurarTabla();
        configurarAcciones();
    }

    public Parent crearPantalla() {
        BorderPane raiz = new BorderPane();
        raiz.getStyleClass().add("app");
        raiz.setLeft(crearMenu());

        BorderPane principal = new BorderPane();
        principal.setTop(crearBarraSuperior());

        VBox contenido = new VBox(24);
        contenido.getStyleClass().add("content");
        contenido.setMaxWidth(1380);
        contenido.getChildren().addAll(crearEncabezado(), crearResumen(), crearZonaTrabajo(), crearNotaDiseno());

        StackPane centrado = new StackPane(contenido);
        centrado.setAlignment(Pos.TOP_CENTER);
        ScrollPane scroll = new ScrollPane(centrado);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("content-scroll");
        principal.setCenter(scroll);
        raiz.setCenter(principal);

        cargarProveedores();
        return raiz;
    }

    private VBox crearMenu() {
        VBox menu = new VBox(7);
        menu.getStyleClass().add("sidebar");
        menu.setPrefWidth(250);
        menu.setMinWidth(250);

        VBox marca = new VBox(5, etiqueta("🎪", "brand-icon"), etiqueta("nextEventux", "brand"),
                etiqueta("GESTIÓN DE EVENTOS", "brand-caption"));
        marca.setAlignment(Pos.CENTER);
        VBox.setMargin(marca, new Insets(0, 0, 38, 0));
        menu.getChildren().addAll(marca, etiqueta("MENÚ PRINCIPAL", "menu-title"));

        menu.getChildren().add(itemMenu("⌂  Inicio", false));
        menu.getChildren().add(itemMenu("▦  Mis eventos", false));
        menu.getChildren().add(itemMenu("♧  Proveedores", true));
        menu.getChildren().add(itemMenu("✓  Aprobar proveedores", true));
        menu.getChildren().add(itemMenu("♙  Invitados", false));

        Region espacio = new Region();
        VBox.setVgrow(espacio, Priority.ALWAYS);
        Button volver = new Button("←  Volver a crear evento");
        volver.getStyleClass().addAll("menu-item", "menu-action");
        volver.setMaxWidth(Double.MAX_VALUE);
        volver.setOnAction(event -> ((Stage) volver.getScene().getWindow()).close());
        VBox perfil = new VBox(4, etiqueta("Administración", "profile-name"), etiqueta("Revisión de proveedores", "hint"));
        HBox cuenta = new HBox(12, etiqueta("AD", "avatar"), perfil);
        cuenta.setAlignment(Pos.CENTER_LEFT);
        cuenta.getStyleClass().add("sidebar-bottom");
        menu.getChildren().addAll(espacio, volver, cuenta);
        return menu;
    }

    private Label itemMenu(String texto, boolean activo) {
        Label item = etiqueta(texto, "menu-item");
        item.setMaxWidth(Double.MAX_VALUE);
        if (activo && texto.contains("Aprobar")) item.getStyleClass().add("active");
        return item;
    }

    private HBox crearBarraSuperior() {
        HBox barra = new HBox(18, etiqueta("Inicio / Administración /", "breadcrumb"),
                etiqueta("Aprobar proveedores", "breadcrumb-current"), espacioHorizontal(),
                etiqueta("♧", "notification"), etiqueta("Administrador", "user-badge"));
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.getStyleClass().add("topbar");
        return barra;
    }

    private HBox crearEncabezado() {
        VBox textos = new VBox(8,
                etiqueta("Aprobación de proveedores", "page-title"),
                etiqueta("Revisa la información registrada y habilita a los proveedores que cumplen los criterios.", "muted"));
        HBox.setHgrow(textos, Priority.ALWAYS);
        Label badge = etiqueta("● Revisión pendiente", "status");
        HBox encabezado = new HBox(20, textos, badge);
        encabezado.setAlignment(Pos.CENTER_LEFT);
        return encabezado;
    }

    private HBox crearResumen() {
        VBox pendientes = new VBox(8, etiqueta("Proveedores pendientes", "hint"), cantidadPendientes,
                etiqueta("Registros que requieren revisión", "muted"));
        pendientes.getStyleClass().add("admin-stat-card");
        cantidadPendientes.getStyleClass().add("admin-stat-value");

        VBox proceso = new VBox(8, etiqueta("Proceso de revisión", "hint"),
                etiqueta("Consultar → Verificar → Decidir", "admin-stat-value-small"),
                etiqueta("Cada decisión se guarda en la base de datos.", "muted"));
        proceso.getStyleClass().add("admin-stat-card");

        HBox resumen = new HBox(16, pendientes, proceso);
        HBox.setHgrow(pendientes, Priority.ALWAYS);
        HBox.setHgrow(proceso, Priority.ALWAYS);
        pendientes.setMaxWidth(Double.MAX_VALUE);
        proceso.setMaxWidth(Double.MAX_VALUE);
        return resumen;
    }

    private GridPane crearZonaTrabajo() {
        VBox tarjetaLista = new VBox(16);
        tarjetaLista.getStyleClass().add("card");
        HBox tituloLista = new HBox(12, etiqueta("▤", "heading-icon"),
                etiqueta("Solicitudes pendientes", "card-title"), espacioHorizontal());
        Button actualizar = new Button("↻ Actualizar");
        actualizar.getStyleClass().add("btn-secondary");
        actualizar.setOnAction(event -> cargarProveedores());
        tituloLista.getChildren().add(actualizar);
        Label ayudaLista = etiqueta("Selecciona un proveedor para consultar sus datos.", "muted");
        tabla.setPrefHeight(430);
        VBox.setVgrow(tabla, Priority.ALWAYS);
        tarjetaLista.getChildren().addAll(tituloLista, ayudaLista, tabla);

        VBox tarjetaDetalle = crearDetalle();
        GridPane zona = new GridPane();
        zona.setHgap(22);
        ColumnConstraints izquierda = new ColumnConstraints();
        izquierda.setPercentWidth(57);
        izquierda.setHgrow(Priority.ALWAYS);
        ColumnConstraints derecha = new ColumnConstraints();
        derecha.setPercentWidth(43);
        derecha.setHgrow(Priority.ALWAYS);
        zona.getColumnConstraints().addAll(izquierda, derecha);
        zona.add(tarjetaLista, 0, 0);
        zona.add(tarjetaDetalle, 1, 0);
        GridPane.setHgrow(tarjetaLista, Priority.ALWAYS);
        GridPane.setHgrow(tarjetaDetalle, Priority.ALWAYS);
        tarjetaLista.setMinWidth(0);
        tarjetaDetalle.setMinWidth(0);
        return zona;
    }

    private VBox crearDetalle() {
        VBox tarjeta = new VBox(17);
        tarjeta.getStyleClass().add("card");
        HBox titulo = new HBox(12, etiqueta("♧", "heading-icon"), etiqueta("Información del proveedor", "card-title"));
        titulo.setAlignment(Pos.CENTER_LEFT);
        nombre.getStyleClass().add("admin-provider-name");
        nombre.setWrapText(true);
        estado.getStyleClass().add("admin-state");

        GridPane campos = new GridPane();
        campos.setHgap(15);
        campos.setVgap(14);
        ColumnConstraints etiquetaColumna = new ColumnConstraints();
        etiquetaColumna.setMinWidth(88);
        ColumnConstraints valorColumna = new ColumnConstraints();
        valorColumna.setHgrow(Priority.ALWAYS);
        campos.getColumnConstraints().addAll(etiquetaColumna, valorColumna);
        agregarCampo(campos, 0, "Empresa", empresa);
        agregarCampo(campos, 1, "Correo", correo);
        agregarCampo(campos, 2, "Teléfono", telefono);
        agregarCampo(campos, 3, "Dirección", direccion);
        agregarCampo(campos, 4, "Estado", estado);

        validar.getStyleClass().add("btn-secondary");
        aprobar.getStyleClass().add("btn-primary");
        rechazar.getStyleClass().add("btn-danger");
        validar.setMaxWidth(Double.MAX_VALUE);
        aprobar.setMaxWidth(Double.MAX_VALUE);
        rechazar.setMaxWidth(Double.MAX_VALUE);
        validar.setDisable(true);
        aprobar.setDisable(true);
        rechazar.setDisable(true);
        VBox acciones = new VBox(10, validar, aprobar, rechazar);
        mensaje.setWrapText(true);
        mensaje.setVisible(false);
        mensaje.setManaged(false);
        mensaje.setMaxWidth(Double.MAX_VALUE);
        tarjeta.getChildren().addAll(titulo, nombre, new Separator(), campos, new Separator(), acciones, mensaje);
        return tarjeta;
    }

    private VBox crearNotaDiseno() {
        VBox nota = new VBox(9,
                etiqueta("ⓘ Alcance de esta primera iteración", "card-title"),
                etiqueta("Esta versión verifica los datos de contacto disponibles y permite registrar la aprobación o el rechazo. "
                        + "El esquema actual todavía no contiene una entidad o tabla para documentos del proveedor; por eso no puede validar "
                        + "archivos adjuntos ni sus fechas de vencimiento. Ese hallazgo debe coordinarse con el equipo antes de ampliar la persistencia.", "muted"));
        nota.getStyleClass().addAll("info-card", "highlight");
        nota.getChildren().get(1).setStyle("-fx-wrap-text: true;");
        return nota;
    }

    private void configurarTabla() {
        tabla.setId("tablaProveedoresPendientes");
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setPlaceholder(etiqueta("No hay proveedores pendientes de verificación.", "muted"));

        TableColumn<Proveedor, Number> id = new TableColumn<>("ID");
        id.setCellValueFactory(datos -> new javafx.beans.property.ReadOnlyIntegerWrapper(datos.getValue().getIdProveedor()));
        id.setPrefWidth(48);

        TableColumn<Proveedor, String> proveedor = new TableColumn<>("Proveedor");
        proveedor.setCellValueFactory(datos -> new SimpleStringProperty(datos.getValue().getNombre()));
        proveedor.setPrefWidth(150);

        TableColumn<Proveedor, String> empresaColumna = new TableColumn<>("Empresa");
        empresaColumna.setCellValueFactory(datos -> new SimpleStringProperty(valor(datos.getValue().getEmpresa())));
        empresaColumna.setPrefWidth(150);

        TableColumn<Proveedor, String> estadoColumna = new TableColumn<>("Estado");
        estadoColumna.setCellValueFactory(datos -> new SimpleStringProperty(datos.getValue().getEstado()));
        estadoColumna.setPrefWidth(110);

        tabla.getColumns().addAll(id, proveedor, empresaColumna, estadoColumna);
        tabla.getSelectionModel().selectedItemProperty().addListener((observable, anterior, seleccionado) -> mostrarDetalle(seleccionado));
    }

    private void configurarAcciones() {
        validar.setOnAction(event -> validarSeleccionado());
        aprobar.setOnAction(event -> aprobarSeleccionado());
        rechazar.setOnAction(event -> rechazarSeleccionado());
    }

    private void cargarProveedores() {
        try {
            List<Proveedor> proveedores = controller.consultarProveedoresPendientes();
            tabla.getItems().setAll(proveedores);
            cantidadPendientes.setText(Integer.toString(proveedores.size()));
            mostrarMensaje(proveedores.isEmpty()
                    ? "No hay proveedores pendientes. Para demostrar el flujo, prepara un registro con estado PENDIENTE_VERIFICACION."
                    : "Lista actualizada. Selecciona un proveedor para comenzar la revisión.", true);
            if (proveedores.isEmpty()) {
                mostrarDetalle(null);
            } else {
                tabla.getSelectionModel().selectFirst();
            }
        } catch (IllegalStateException e) {
            cantidadPendientes.setText("—");
            mostrarDetalle(null);
            mostrarMensaje(e.getMessage(), false);
        }
    }

    private void mostrarDetalle(Proveedor proveedor) {
        informacionValidada = false;
        boolean seleccionado = proveedor != null;
        validar.setDisable(!seleccionado);
        aprobar.setDisable(true);
        rechazar.setDisable(!seleccionado);

        if (!seleccionado) {
            nombre.setText("Selecciona un proveedor");
            empresa.setText("—");
            correo.setText("—");
            telefono.setText("—");
            direccion.setText("—");
            estado.setText("Sin selección");
            return;
        }
        nombre.setText(proveedor.getNombre());
        empresa.setText(valor(proveedor.getEmpresa()));
        correo.setText(valor(proveedor.getCorreo()));
        telefono.setText(valor(proveedor.getTelefono()));
        direccion.setText(valor(proveedor.getDireccion()));
        estado.setText(proveedor.getEstado());
        mostrarMensaje("Revisa los datos y pulsa «Verificar información» antes de aprobar.", true);
    }

    private void validarSeleccionado() {
        Proveedor proveedor = tabla.getSelectionModel().getSelectedItem();
        if (proveedor == null) return;
        try {
            informacionValidada = controller.verificarInformacionProveedor(proveedor.getIdProveedor());
            aprobar.setDisable(!informacionValidada);
            if (informacionValidada) {
                mostrarMensaje("Los datos disponibles pasan las validaciones básicas. La revisión de documentos aún no está modelada.", true);
            } else {
                mostrarMensaje("La información disponible está incompleta o el correo no tiene un formato válido. No se puede aprobar.", false);
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            informacionValidada = false;
            aprobar.setDisable(true);
            mostrarMensaje(e.getMessage(), false);
        }
    }

    private void aprobarSeleccionado() {
        Proveedor proveedor = tabla.getSelectionModel().getSelectedItem();
        if (proveedor == null || !informacionValidada) {
            mostrarMensaje("Selecciona y verifica un proveedor antes de aprobarlo.", false);
            return;
        }
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Confirmas la aprobación de " + proveedor.getNombre() + "? El estado cambiará a VERIFICADO.",
                ButtonType.CANCEL, ButtonType.OK);
        confirmacion.setTitle("Confirmar aprobación");
        confirmacion.setHeaderText("Aprobar proveedor");
        if (confirmacion.showAndWait().filter(ButtonType.OK::equals).isEmpty()) return;
        try {
            controller.aprobarProveedor(proveedor.getIdProveedor());
            cargarProveedores();
            mostrarMensaje("Proveedor aprobado. Su estado quedó como VERIFICADO.", true);
        } catch (IllegalArgumentException | IllegalStateException e) {
            mostrarMensaje(e.getMessage(), false);
            cargarProveedores();
        }
    }

    private void rechazarSeleccionado() {
        Proveedor proveedor = tabla.getSelectionModel().getSelectedItem();
        if (proveedor == null) return;
        TextInputDialog dialogo = new TextInputDialog();
        dialogo.setTitle("Rechazar proveedor");
        dialogo.setHeaderText("Indica el motivo del rechazo");
        dialogo.setContentText("Motivo (máximo 500 caracteres):");
        dialogo.showAndWait().ifPresent(motivo -> {
            try {
                controller.rechazarProveedor(proveedor.getIdProveedor(), motivo);
                cargarProveedores();
                mostrarMensaje("Proveedor rechazado. El motivo quedó guardado en el registro.", true);
            } catch (IllegalArgumentException | IllegalStateException e) {
                mostrarMensaje(e.getMessage(), false);
            }
        });
    }

    private void agregarCampo(GridPane grilla, int fila, String titulo, Label valor) {
        valor.setWrapText(true);
        grilla.add(etiqueta(titulo, "hint"), 0, fila);
        grilla.add(valor, 1, fila);
    }

    private void mostrarMensaje(String texto, boolean exito) {
        mensaje.setText(texto);
        mensaje.getStyleClass().setAll("message", exito ? "success" : "error");
        mensaje.setVisible(true);
        mensaje.setManaged(true);
    }

    private String valor(String texto) {
        return texto == null || texto.isBlank() ? "No registrado" : texto;
    }

    private Label etiqueta(String texto, String estilo) {
        Label label = new Label(texto);
        label.getStyleClass().add(estilo);
        return label;
    }

    private Region espacioHorizontal() {
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        return espacio;
    }
}

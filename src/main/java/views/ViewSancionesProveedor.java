package views;

import java.util.Optional;

import controllers.ControllerSancionesProveedor;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import repositories.RepositorySancionesProveedor.ContratoSancionable;
import repositories.RepositorySancionesProveedor.EventoProveedor;
import repositories.RepositorySancionesProveedor.ResumenProveedor;
import services.ServiceSancionesProveedor;
import services.ServiceSancionesProveedor.ResultadoSancion;
import services.ServiceSancionesProveedor.SolicitudSancion;

/**
 * Vista de CU-09: aplicar sanciones a un proveedor.
 * Flujo: buscar proveedor -> elegir evento -> tipo de falta y de sanción
 * -> (multa) contrato y porcentaje -> justificación -> confirmar.
 */
public class ViewSancionesProveedor {

    private final ControllerSancionesProveedor controller;
    private final Runnable volver;

    private final TextField buscar = new TextField();
    private final ListView<ResumenProveedor> listaProveedores = new ListView<>();
    private final Label proveedorElegido = new Label("Selecciona un proveedor");
    private final ComboBox<EventoProveedor> evento = new ComboBox<>();
    private final ComboBox<String> tipoFalta = new ComboBox<>();
    private final ComboBox<String> tipoSancion = new ComboBox<>();
    private final ComboBox<ContratoSancionable> contrato = new ComboBox<>();
    private final TextField porcentaje = new TextField();
    private final TextArea justificacion = new TextArea();
    private final Button aplicar = new Button("Aplicar sanción");
    private final Label mensaje = new Label();

    private ResumenProveedor proveedor;
    private boolean refrescandoLista;

    public ViewSancionesProveedor(ControllerSancionesProveedor controller, Runnable volver) {
        this.controller = controller;
        this.volver = volver;
    }

    public Parent crearPantalla() {
        tipoFalta.getItems().setAll(
                ServiceSancionesProveedor.TIPOS_FALTA.stream().sorted().toList());
        tipoSancion.getItems().setAll(ServiceSancionesProveedor.SUSPENSION, ServiceSancionesProveedor.MULTA);

        evento.setConverter(new StringConverter<>() {
            @Override public String toString(EventoProveedor e) {
                return e == null ? "" : e.nombre();
            }
            @Override public EventoProveedor fromString(String s) { return null; }
        });
        contrato.setConverter(new StringConverter<>() {
            @Override public String toString(ContratoSancionable c) {
                if (c == null) {
                    return "";
                }
                String valor = c.valorAcordado() == null ? "sin valor" : "$" + c.valorAcordado().toPlainString();
                return "Contrato #" + c.idContratacion() + " · " + valor + " · " + c.estado();
            }
            @Override public ContratoSancionable fromString(String s) { return null; }
        });
        listaProveedores.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(ResumenProveedor p, boolean vacio) {
                super.updateItem(p, vacio);
                setText(vacio || p == null ? null
                        : p.nombre() + " (#" + p.idProveedor() + ") · " + p.estado());
            }
        });

        buscar.setPromptText("Nombre o ID del proveedor");
        buscar.setOnAction(e -> buscar());
        Button botonBuscar = new Button("Buscar");
        botonBuscar.setOnAction(e -> buscar());
        listaProveedores.getSelectionModel().selectedItemProperty()
                .addListener((obs, anterior, nuevo) -> {
                    if (!refrescandoLista) {
                        seleccionarProveedor(nuevo);
                    }
                });

        evento.valueProperty().addListener((obs, anterior, nuevo) -> cargarContratos());
        tipoSancion.valueProperty().addListener((obs, anterior, nuevo) -> actualizarCamposMulta());

        porcentaje.setPromptText("Ej. 10 o 12,5");
        porcentaje.setDisable(true);
        contrato.setDisable(true);
        justificacion.setWrapText(true);
        justificacion.setPrefRowCount(4);
        justificacion.setPromptText("Mínimo 10 caracteres, máximo 150");

        aplicar.setOnAction(e -> confirmarYAplicar());
        mensaje.setWrapText(true);

        Button botonVolver = new Button("← Volver");
        botonVolver.setOnAction(e -> volver.run());
        Label titulo = new Label("Aplicar sanciones a proveedores");
        titulo.getStyleClass().add("titulo-seccion");

        HBox fila = new HBox(8, buscar, botonBuscar);
        VBox izquierda = new VBox(10, fila, listaProveedores);
        VBox.setVgrow(listaProveedores, javafx.scene.layout.Priority.ALWAYS);
        izquierda.setPrefWidth(340);

        GridPane formulario = new GridPane();
        formulario.setHgap(12);
        formulario.setVgap(10);
        int siguiente = 0;
        formulario.addRow(siguiente++, new Label("Proveedor"), proveedorElegido);
        formulario.addRow(siguiente++, new Label("Evento"), evento);
        formulario.addRow(siguiente++, new Label("Tipo de falta"), tipoFalta);
        formulario.addRow(siguiente++, new Label("Tipo de sanción"), tipoSancion);
        formulario.addRow(siguiente++, new Label("Contrato (multa)"), contrato);
        formulario.addRow(siguiente++, new Label("Porcentaje (multa)"), porcentaje);
        formulario.addRow(siguiente++, new Label("Justificación"), justificacion);
        formulario.addRow(siguiente++, new Label(""), aplicar);
        formulario.addRow(siguiente, new Label(""), mensaje);

        HBox cuerpo = new HBox(24, izquierda, formulario);
        cuerpo.setPadding(new Insets(16));
        VBox raiz = new VBox(12, new HBox(12, botonVolver, titulo), cuerpo);
        raiz.setPadding(new Insets(16));
        return raiz;
    }

    private void buscar() {
        try {
            listaProveedores.getItems().setAll(controller.buscarProveedores(buscar.getText()));
            mensaje.setText("");
        } catch (RuntimeException ex) {
            mostrarError(ex);
        }
    }

    private void seleccionarProveedor(ResumenProveedor elegido) {
        proveedor = elegido;
        evento.getSelectionModel().clearSelection();
        evento.getItems().clear();
        contrato.getItems().clear();
        if (elegido == null) {
            proveedorElegido.setText("Selecciona un proveedor");
            return;
        }
        proveedorElegido.setText(elegido.nombre() + " (#" + elegido.idProveedor() + ") · " + elegido.estado());
        try {
            evento.getItems().setAll(controller.eventosDelProveedor(elegido.idProveedor()));
        } catch (RuntimeException ex) {
            mostrarError(ex);
        }
    }

    private void cargarContratos() {
        contrato.getSelectionModel().clearSelection();
        contrato.getItems().clear();
        if (proveedor == null || evento.getValue() == null) {
            return;
        }
        try {
            contrato.getItems().setAll(
                    controller.contratosActivos(proveedor.idProveedor(), evento.getValue().idEvento()));
        } catch (RuntimeException ex) {
            mostrarError(ex);
        }
    }

    private void actualizarCamposMulta() {
        boolean esMulta = ServiceSancionesProveedor.MULTA.equals(tipoSancion.getValue());
        porcentaje.setDisable(!esMulta);
        contrato.setDisable(!esMulta);
        if (!esMulta) {
            porcentaje.clear();
            contrato.getSelectionModel().clearSelection();
        }
    }

    private void confirmarYAplicar() {
        if (proveedor == null || evento.getValue() == null
                || tipoFalta.getValue() == null || tipoSancion.getValue() == null) {
            mostrarMensajeError("Completa proveedor, evento, tipo de falta y tipo de sanción.");
            return;
        }
        boolean esMulta = ServiceSancionesProveedor.MULTA.equals(tipoSancion.getValue());
        if (esMulta && contrato.getValue() == null) {
            mostrarMensajeError("Selecciona el contrato al que se aplica la multa.");
            return;
        }

        Alert confirmar = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Está seguro de aplicar esta sanción?", ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> respuesta = confirmar.showAndWait();
        if (respuesta.isEmpty() || respuesta.get() != ButtonType.OK) {
            return;
        }

        SolicitudSancion solicitud = new SolicitudSancion(
                proveedor.idProveedor(),
                evento.getValue().idEvento(),
                tipoFalta.getValue(),
                tipoSancion.getValue(),
                esMulta ? contrato.getValue().idContratacion() : 0,
                porcentaje.getText(),
                justificacion.getText());

        aplicar.setDisable(true);
        try {
            ResultadoSancion resultado = controller.aplicarSancion(solicitud);
            mensaje.setStyle("-fx-text-fill: #1b7a3a;");
            mensaje.setText(resultado.mensaje());
            new Alert(Alert.AlertType.INFORMATION, resultado.mensaje()).showAndWait();
            justificacion.clear();
            porcentaje.clear();
            actualizarProveedorTrasSancion(resultado.estadoProveedor());
            cargarContratos();
        } catch (RuntimeException ex) {
            mostrarError(ex);
        } finally {
            aplicar.setDisable(false);
        }
    }

    private void actualizarProveedorTrasSancion(String nuevoEstado) {
        if (proveedor == null || nuevoEstado == null) {
            return;
        }
        proveedor = new ResumenProveedor(
                proveedor.idProveedor(), proveedor.nombre(), proveedor.empresa(), nuevoEstado);
        proveedorElegido.setText(proveedor.nombre() + " (#" + proveedor.idProveedor() + ") · " + nuevoEstado);

        int indice = listaProveedores.getSelectionModel().getSelectedIndex();
        if (indice >= 0) {
            refrescandoLista = true;
            try {
                listaProveedores.getItems().set(indice, proveedor);
            } finally {
                refrescandoLista = false;
            }
        }
    }

    private void mostrarError(RuntimeException ex) {
        String texto = ex.getMessage() == null ? "Ocurrió un error inesperado." : ex.getMessage();
        mostrarMensajeError(texto);
        new Alert(Alert.AlertType.ERROR, texto).showAndWait();
    }

    private void mostrarMensajeError(String texto) {
        mensaje.setStyle("-fx-text-fill: #b00020;");
        mensaje.setText(texto);
    }
}

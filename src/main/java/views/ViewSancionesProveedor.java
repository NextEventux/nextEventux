package views;

import java.util.Optional;

import controllers.ControllerSancionesProveedor;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
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
import javafx.util.StringConverter;
import repositories.RepositorySancionesProveedor.ContratoSancionable;
import repositories.RepositorySancionesProveedor.EventoProveedor;
import repositories.RepositorySancionesProveedor.ResumenProveedor;
import services.ServiceSancionesProveedor;
import services.ServiceSancionesProveedor.ResultadoSancion;
import services.ServiceSancionesProveedor.SolicitudSancion;

/**
 * Vista de CU-09: aplicar sanciones a un proveedor. Flujo: buscar proveedor ->
 * elegir evento -> tipo de falta y de sanción -> (multa) contrato y porcentaje
 * -> justificación -> confirmar.
 */
public class ViewSancionesProveedor {

    private final ControllerSancionesProveedor controller;

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

    public ViewSancionesProveedor(ControllerSancionesProveedor controller) {
        this.controller = controller;
    }

    public Parent crearPantalla() {
        tipoFalta.getItems().setAll(
                ServiceSancionesProveedor.TIPOS_FALTA.stream().sorted().toList());
        tipoSancion.getItems().setAll(ServiceSancionesProveedor.SUSPENSION, ServiceSancionesProveedor.MULTA);

        StringConverter<String> nombresLegibles = new StringConverter<>() {
            @Override
            public String toString(String valor) {
                if (valor == null) {
                    return "";
                }

                return switch (valor) {
                    case "CANCELACION_TARDIA" ->
                        "Cancelación tardía";
                    case "INASISTENCIA" ->
                        "Inasistencia";
                    case "INCUMPLIMIENTO_CONDICIONES" ->
                        "Incumplimiento de condiciones";
                    case "RETRASO" ->
                        "Retraso";
                    case "SUSPENSION" ->
                        "Suspensión";
                    case "MULTA" ->
                        "Multa";
                    default ->
                        valor;
                };
            }

            @Override
            public String fromString(String texto) {
                return texto;
            }
        };

        tipoFalta.setConverter(nombresLegibles);
        tipoSancion.setConverter(nombresLegibles);

        evento.setPrefWidth(280);
        tipoFalta.setPrefWidth(280);
        tipoSancion.setPrefWidth(280);
        contrato.setPrefWidth(280);

        evento.setConverter(new StringConverter<>() {
            @Override
            public String toString(EventoProveedor e) {
                return e == null ? "" : e.nombre();
            }

            @Override
            public EventoProveedor fromString(String s) {
                return null;
            }
        });
        contrato.setConverter(new StringConverter<>() {
            @Override
            public String toString(ContratoSancionable c) {
                if (c == null) {
                    return "";
                }
                String valor = c.valorAcordado() == null ? "sin valor" : "$" + c.valorAcordado().toPlainString();
                return "Contrato #" + c.idContratacion() + " · " + valor + " · " + c.estado();
            }

            @Override
            public ContratoSancionable fromString(String s) {
                return null;
            }
        });
        listaProveedores.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ResumenProveedor p, boolean vacio) {
                super.updateItem(p, vacio);
                setText(vacio || p == null ? null
                        : p.nombre() + " (#" + p.idProveedor() + ") · " + p.estado());
            }
        });

        buscar.setPromptText("Nombre o ID del proveedor");
        buscar.setOnAction(e -> buscar());
        Button botonBuscar = new Button("Buscar");
        botonBuscar.getStyleClass().add("btn-secondary");
        botonBuscar.setOnAction(e -> buscar());
        listaProveedores.getSelectionModel().selectedItemProperty()
                .addListener((obs, anterior, nuevo) -> {
                    if (!refrescandoLista) {
                        seleccionarProveedor(nuevo);
                    }
                });

        evento.valueProperty().addListener((obs, anterior, nuevo) -> cargarContratos());

        porcentaje.setPromptText("Ej. 10 o 12,5");
        porcentaje.setDisable(true);
        contrato.setDisable(true);
        justificacion.setWrapText(true);
        justificacion.setPrefRowCount(4);
        justificacion.setPromptText("Mínimo 10 caracteres, máximo 150");

        aplicar.setOnAction(e -> confirmarYAplicar());
        aplicar.getStyleClass().add("btn-primary");
        mensaje.setWrapText(true);

        Label titulo = new Label("Sanciones a proveedores");
        titulo.getStyleClass().add("page-title");

        Label descripcion = new Label(
                "Busca un proveedor y registra una sanción asociada a un evento.");
        descripcion.getStyleClass().add("muted");
        descripcion.setWrapText(true);

        VBox encabezado = new VBox(8, titulo, descripcion);

        Label tituloBusqueda = new Label("Buscar proveedor");
        tituloBusqueda.getStyleClass().add("card-title");

        buscar.setMinWidth(0);
        buscar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(buscar, Priority.ALWAYS);

        botonBuscar.setMinWidth(Region.USE_PREF_SIZE);

        HBox filaBusqueda = new HBox(10, buscar, botonBuscar);

        listaProveedores.getStyleClass().add("lista-proveedores-sanciones");
        listaProveedores.setPrefHeight(240);

        VBox izquierda = new VBox(
                18,
                tituloBusqueda,
                filaBusqueda,
                listaProveedores);

        izquierda.getStyleClass().add("card");
        izquierda.setPrefWidth(365);
        izquierda.setMinWidth(330);

        Label tituloFormulario = new Label("Datos de la sanción");
        tituloFormulario.getStyleClass().add("card-title");

        GridPane formulario = new GridPane();
        formulario.setHgap(18);
        formulario.setVgap(18);

        ColumnConstraints columnaIzquierda = new ColumnConstraints();
        columnaIzquierda.setPercentWidth(50);

        ColumnConstraints columnaDerecha = new ColumnConstraints();
        columnaDerecha.setPercentWidth(50);

        formulario.getColumnConstraints().addAll(
                columnaIzquierda, columnaDerecha);

        formulario.add(campoFormulario("Proveedor", proveedorElegido), 0, 0, 2, 1);

        formulario.add(campoFormulario("Evento", evento), 0, 1);
        formulario.add(campoFormulario("Tipo de falta", tipoFalta), 1, 1);

        formulario.add(campoFormulario("Tipo de sanción", tipoSancion), 0, 2, 2, 1);

        VBox campoContrato = campoFormulario("Contrato (multa)", contrato);
        VBox campoPorcentaje = campoFormulario("Porcentaje (multa)", porcentaje);

        formulario.add(campoContrato, 0, 3);
        formulario.add(campoPorcentaje, 1, 3);

        formulario.add(campoFormulario("Justificación", justificacion), 0, 4, 2, 1);

        formulario.add(aplicar, 0, 5);
        formulario.add(mensaje, 0, 6, 2, 1);

        Runnable actualizarVisibilidadMulta = () -> {
            boolean esMulta = ServiceSancionesProveedor.MULTA.equals(tipoSancion.getValue());

            campoContrato.setVisible(esMulta);
            campoContrato.setManaged(esMulta);

            campoPorcentaje.setVisible(esMulta);
            campoPorcentaje.setManaged(esMulta);
        };

        tipoSancion.valueProperty().addListener((obs, anterior, nuevo) -> {
            actualizarCamposMulta();
            actualizarVisibilidadMulta.run();
        });

        actualizarCamposMulta();
        actualizarVisibilidadMulta.run();

        VBox derecha = new VBox(20, tituloFormulario, formulario);
        derecha.getStyleClass().add("card");
        derecha.setMinWidth(0);
        HBox.setHgrow(derecha, Priority.ALWAYS);

        HBox cuerpo = new HBox(24, izquierda, derecha);
        cuerpo.setAlignment(Pos.TOP_LEFT);
        cuerpo.setFillHeight(false);

        VBox contenido = new VBox(
                24,
                encabezado,
                cuerpo);

        contenido.getStyleClass().add("content");
        contenido.setMaxWidth(1120);

        StackPane centrado = new StackPane(contenido);
        centrado.setAlignment(Pos.TOP_CENTER);

        ScrollPane scroll = new ScrollPane(centrado);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("content-scroll");

        return scroll;
    }

    private VBox campoFormulario(String texto, javafx.scene.layout.Region control) {
        Label etiqueta = new Label(texto);
        etiqueta.getStyleClass().add("field-label");

        control.setMaxWidth(Double.MAX_VALUE);

        VBox campo = new VBox(8, etiqueta, control);
        campo.setMaxWidth(Double.MAX_VALUE);

        return campo;
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

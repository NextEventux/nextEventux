package views;

import controllers.ControllerMarketing;
import entities.Segmentacion;
import java.util.List;
import java.util.function.Function;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import services.ServiceMarketing;

public class ViewMarketing {
    private final ControllerMarketing controller;
    private final int idAdministrador;
    private final TableView<Segmentacion> tabla = new TableView<>();
    private final Label resumenSegmentos = new Label("0");
    private final Label resumenClientes = new Label("0");
    private final Label mensaje = new Label();
    
    public ViewMarketing(ControllerMarketing controller, int idAdministrador) {
    this.controller = controller;
    this.idAdministrador = idAdministrador;
    mensaje.setId("mensaje");
    mensaje.setWrapText(true);
    mensaje.setVisible(false);
    mensaje.setManaged(false);
    resumenSegmentos.setId("resumenSegmentos");
    resumenClientes.setId("resumenClientes");
    configurarTabla();
    }

    public Parent crearPantalla() {
        BorderPane raiz = new BorderPane();
        raiz.getStyleClass().add("app");
        raiz.setLeft(crearMenu());

        BorderPane principal = new BorderPane();
        principal.setTop(crearBarraSuperior());

        VBox contenido = new VBox(30);
        contenido.getStyleClass().add("content");
        contenido.setMaxWidth(1320);
        VBox titulo = new VBox(8, etiqueta("Segmentos de clientes", "page-title"),
                etiqueta("Agrupa a los clientes por ciudad, rango de edad y tipo de evento que más solicitan.", "muted"));
        HBox encabezado = new HBox(20, titulo, espacioHorizontal(), etiqueta("● Segmentación automática", "status"));
        encabezado.setAlignment(Pos.CENTER_LEFT);

        VBox tarjeta = crearTarjetaSegmentos();
        VBox panel = crearPanelDerecho();
        GridPane columnas = new GridPane();
        columnas.setHgap(25);
        columnas.setVgap(25);
        ColumnConstraints izquierda = new ColumnConstraints();
        izquierda.setHgrow(Priority.ALWAYS);
        ColumnConstraints derecha = new ColumnConstraints(290);
        columnas.getColumnConstraints().addAll(izquierda, derecha);
        columnas.add(tarjeta, 0, 0);
        columnas.add(panel, 1, 0);
        GridPane.setHgrow(tarjeta, Priority.ALWAYS);
        tarjeta.setMinWidth(0);
        contenido.getChildren().addAll(encabezado, columnas);

        StackPane centrado = new StackPane(contenido);
        centrado.setAlignment(Pos.TOP_CENTER);
        ScrollPane scroll = new ScrollPane(centrado);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("content-scroll");
        principal.setCenter(scroll);
        raiz.setCenter(principal);

        try {
            cargarSegmentos();
        } catch (IllegalStateException e) {
            mostrarMensaje(e.getMessage(), "error");
        }
    return raiz;
    }

    private void configurarTabla() {
        tabla.setId("tablaSegmentos");
        tabla.setPlaceholder(new Label("Aún no hay segmentos activos. Pulsa «Generar segmentos»."));
        tabla.setPrefHeight(350);
        tabla.getColumns().add(columnaTexto("Segmento", 0.30, false, Segmentacion::getNombre));
        tabla.getColumns().add(columnaTexto("Descripción", 0.405, false, Segmentacion::getDescripcion));
        tabla.getColumns().add(columnaTexto("Clientes", 0.14, true, segmento -> String.valueOf(segmento.getCantidadClientes())));
        tabla.getColumns().add(columnaTexto("Estado", 0.14, true, Segmentacion::getEstado));
    }

    private TableColumn<Segmentacion, String> columnaTexto(String titulo, double proporcion, boolean centrada, Function<Segmentacion, String> valor) {
        TableColumn<Segmentacion, String> columna = new TableColumn<>(titulo);
        columna.setCellValueFactory(datos -> new ReadOnlyStringWrapper(valor.apply(datos.getValue())));
        columna.prefWidthProperty().bind(tabla.widthProperty().multiply(proporcion));
        columna.setSortable(false);
        if (centrada) {
            columna.getStyleClass().add("centrado");
        }
        return columna;
    }
    
    private VBox crearTarjetaSegmentos() {
        VBox tarjeta = new VBox(22);
        tarjeta.getStyleClass().add("card");
        HBox titulo = new HBox(13, etiqueta("♙", "heading-icon"), etiqueta("Segmentos activos", "card-title"));
        titulo.setAlignment(Pos.CENTER_LEFT);
        Label ayuda = etiqueta("Cada segmento reúne a los clientes de una misma ciudad y rango de edad " + "que más buscan o contratan un tipo de evento.", "muted");
        ayuda.setWrapText(true);

        Button generar = new Button("Generar segmentos");
        generar.setId("generar");
        generar.getStyleClass().add("btn-primary");
        generar.setOnAction(event -> generarSegmentos());
        HBox acciones = new HBox(12, generar);
        acciones.setAlignment(Pos.CENTER_RIGHT);

        tarjeta.getChildren().addAll(titulo, ayuda, tabla, mensaje, acciones);
        return tarjeta;
    }

    private VBox crearPanelDerecho() {
        VBox panel = new VBox(20);
        panel.setPrefWidth(290);
        panel.setMinWidth(260);
        VBox inicio = informacion("✦ Segmentación automática",
                "El sistema analiza a los clientes y los agrupa sin que tengas que configurar nada.");
        inicio.getStyleClass().add("highlight");
        VBox resumen = new VBox(14, etiqueta("Resumen", "card-title"),
                filaResumen("Segmentos activos", resumenSegmentos), new Separator(),
                filaResumen("Clientes segmentados", resumenClientes), new Separator(),
                filaResumen("Mínimo por segmento", new Label(String.valueOf(ServiceMarketing.MINIMO_CLIENTES_POR_SEGMENTO))));
        resumen.getStyleClass().add("info-card");
        panel.getChildren().addAll(inicio, resumen, informacion("ⓘ Información importante",
        "Al generar de nuevo, los segmentos automáticos anteriores pasan a inactivos. " + "Los clientes sin ciudad o fecha de nacimiento no se incluyen."));
    return panel;
    }

    private void generarSegmentos() {
        try {
            List<Segmentacion> generados = controller.segmentarClientes(idAdministrador);
            if (generados.isEmpty()) {
                mostrarMensaje("No hay datos suficientes para formar segmentos. Se necesitan al menos "
                        + ServiceMarketing.MINIMO_CLIENTES_POR_SEGMENTO + " clientes con ciudad y fecha de "
                        + "nacimiento en un mismo grupo, con solicitudes de eventos. Los segmentos actuales no cambiaron.",
                        "warning");
                return;
            }
            cargarSegmentos();
            mostrarMensaje(generados.size() == 1 ? "Se generó 1 segmento."
                    : "Se generaron " + generados.size() + " segmentos.", "success");
        } catch (IllegalArgumentException | IllegalStateException e) {
            mostrarMensaje(e.getMessage(), "error");
        }
    }

    private void cargarSegmentos() {
        List<Segmentacion> activos = controller.listarSegmentosActivos();
        tabla.setItems(FXCollections.observableArrayList(activos));
        int clientes = 0;
        for (Segmentacion segmento : activos) {
            clientes += segmento.getCantidadClientes();
        }
        resumenSegmentos.setText(String.valueOf(activos.size()));
        resumenClientes.setText(String.valueOf(clientes));
    }

    private void mostrarMensaje(String texto, String estilo) {
        mensaje.setText(texto);
        mensaje.getStyleClass().setAll("message", estilo);
        mensaje.setVisible(true);
        mensaje.setManaged(true);
    }

    

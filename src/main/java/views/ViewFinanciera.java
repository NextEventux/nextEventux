package views;

import controllers.ControllerFinanciera;
import entities.MovimientoFinanciero;
import entities.Servicio;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pantalla de Gestión Financiera.
 * Sigue la misma estructura que ViewEventos y usa las clases del CSS de la carpeta styles
 * (sidebar, topbar, card, card-title, heading-icon, field-label, text-field, btn-primary, etc.).
 */
public class ViewFinanciera {

    // Ruta del CSS dentro de resources. Cámbiala si tu archivo se llama distinto.
    private static final String RUTA_CSS = "/styles/styles.css";

    private final ControllerFinanciera controller;

    public ViewFinanciera(ControllerFinanciera controller) {
        this.controller = controller;
    }

    // ==========================================
    // Abre la pantalla en una ventana propia
    // ==========================================
    public void mostrarEstadoFinanciero(int idEvento) {
        Parent raiz = crearPantalla(idEvento);
        Scene scene = new Scene(raiz, 1280, 800);
        URL css = getClass().getResource(RUTA_CSS);
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        } else {
            System.out.println("No se encontró el CSS en " + RUTA_CSS + ". Revisa la constante RUTA_CSS.");
        }
        Stage stage = new Stage();
        stage.setTitle("NextEventux");
        stage.setScene(scene);
        stage.show();
    }

    // ==========================================
    // Construye la pantalla (como ViewEventos.crearPantalla)
    // ==========================================
    @SuppressWarnings("unchecked")
    public Parent crearPantalla(int idEvento) {
        Map<String, Object> estado = controller.consultarEstadoFinanciero(idEvento);

        String descripcion = estado.containsKey("Planificacion_Descripcion")
                ? estado.get("Planificacion_Descripcion").toString()
                : "No hay planificación registrada para este evento.";
        String estadoPlan = estado.containsKey("Planificacion_Estado")
                ? estado.get("Planificacion_Estado").toString()
                : "Desconocido";
        String ingresos = estado.containsKey("Ingresos") ? estado.get("Ingresos").toString() : "0.0";
        String egresos = estado.containsKey("Egresos") ? estado.get("Egresos").toString() : "0.0";
        String balance = estado.containsKey("Balance") ? estado.get("Balance").toString() : "0.0";

        List<Servicio> servicios = estado.containsKey("Servicios")
                ? (List<Servicio>) estado.get("Servicios")
                : new ArrayList<>();
        List<MovimientoFinanciero> movimientos = estado.containsKey("Movimientos")
                ? (List<MovimientoFinanciero>) estado.get("Movimientos")
                : new ArrayList<>();

        BorderPane raiz = new BorderPane();
        raiz.getStyleClass().add("app");

        VBox sidebar = crearMenu();
        raiz.setLeft(sidebar);

        BorderPane principal = new BorderPane();
        principal.setTop(crearBarraSuperior());

        VBox contenido = new VBox(30);
        contenido.getStyleClass().add("content");
        contenido.setMaxWidth(1320);

        // ---------- Encabezado ----------
        Label subtitulo = etiqueta(descripcion, "muted");
        subtitulo.setWrapText(true);
        VBox titulo = new VBox(8, etiqueta("Finanzas", "page-title"), subtitulo);
        HBox encabezado = new HBox(20, titulo, espacioHorizontal(), etiqueta("● " + estadoPlan, "status"));
        encabezado.setAlignment(Pos.CENTER_LEFT);

        // ---------- Tarjeta principal ----------
        VBox tarjeta = new VBox(22);
        tarjeta.getStyleClass().add("card");

        HBox tituloTarjeta = new HBox(13, etiqueta("$", "heading-icon"), etiqueta("Resumen financiero", "card-title"));
        tituloTarjeta.setAlignment(Pos.CENTER_LEFT);

        Label ayuda = etiqueta("Consulta los ingresos, egresos y el balance actualizado de tu evento.", "muted");
        ayuda.setWrapText(true);

        GridPane datos = grilla();
        datos.add(grupo("Total ingresos", valor("ingresos", "$" + ingresos)), 0, 0);
        datos.add(grupo("Total egresos", valor("egresos", "$" + egresos)), 1, 0);
        datos.add(grupo("Balance final", valor("balance", "$" + balance)), 0, 1);
        datos.add(grupo("Estado del plan", valor("estadoPlan", estadoPlan)), 1, 1);

        tarjeta.getChildren().addAll(tituloTarjeta, ayuda, datos, new Separator(),
                etiqueta("Presupuesto por categoría", "section-title"));

        // ---------- Categorías ----------
        if (servicios.isEmpty()) {
            tarjeta.getChildren().add(etiqueta("No hay servicios asignados a este evento.", "hint"));
        } else {
            Map<String, Double> costosPorCategoria = new LinkedHashMap<>();
            double totalPresupuesto = 0.0;
            for (Servicio s : servicios) {
                String cat = s.getCategoria() != null ? s.getCategoria() : "Otros";
                costosPorCategoria.put(cat, costosPorCategoria.getOrDefault(cat, 0.0) + s.getPrecio());
                totalPresupuesto += s.getPrecio();
            }
            String[] colores = {"#D71920", "#2563EB", "#059669", "#D97706", "#7C3AED"};
            int idx = 0;
            VBox barras = new VBox(16);
            for (Map.Entry<String, Double> entry : costosPorCategoria.entrySet()) {
                double fraccion = totalPresupuesto > 0 ? entry.getValue() / totalPresupuesto : 0;
                barras.getChildren().add(crearBarra(entry.getKey(), Math.round(fraccion * 100) + "%",
                        fraccion, colores[idx % colores.length]));
                idx++;
            }
            tarjeta.getChildren().add(barras);
        }

        // ---------- Movimientos ----------
        tarjeta.getChildren().addAll(new Separator(), etiqueta("Últimos movimientos", "section-title"));

        if (movimientos.isEmpty()) {
            tarjeta.getChildren().add(etiqueta("No hay movimientos registrados.", "hint"));
        } else {
            VBox lista = new VBox(10);
            for (MovimientoFinanciero mov : movimientos) {
                String tituloMov = mov.getDescripcion() != null ? mov.getDescripcion() : "Movimiento";
                String fecha = mov.getFecha() != null ? mov.getFecha().toString() : "--";
                String monto = "$" + mov.getValor();
                String estadoMov = mov.getEstado() != null ? mov.getEstado() : "Pendiente";
                lista.getChildren().add(crearFilaMovimiento(tituloMov, fecha, monto, estadoMov));
            }
            tarjeta.getChildren().add(lista);
        }

        // ---------- Botones ----------
        Button cerrar = new Button("Cerrar");
        cerrar.setId("cerrar");
        cerrar.getStyleClass().add("btn-secondary");
        cerrar.setOnAction(e -> {
            Window w = cerrar.getScene() != null ? cerrar.getScene().getWindow() : null;
            if (w instanceof Stage) ((Stage) w).close();
        });

        Button exportar = new Button("Exportar reporte");
        exportar.setId("exportar");
        exportar.getStyleClass().add("btn-primary");
        exportar.setOnAction(e -> exportarCSV(
                exportar.getScene() != null ? exportar.getScene().getWindow() : null, estado));

        HBox acciones = new HBox(12, cerrar, exportar);
        acciones.setAlignment(Pos.CENTER_RIGHT);
        tarjeta.getChildren().add(acciones);

        contenido.getChildren().addAll(encabezado, tarjeta);

        StackPane centrado = new StackPane(contenido);
        centrado.setAlignment(Pos.TOP_CENTER);
        ScrollPane scroll = new ScrollPane(centrado);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("content-scroll");
        principal.setCenter(scroll);
        raiz.setCenter(principal);

        // En ventanas pequeñas el menú deja espacio al contenido.
        raiz.widthProperty().addListener((obs, anterior, actual) -> {
            sidebar.setVisible(actual.doubleValue() >= 1000);
            sidebar.setManaged(sidebar.isVisible());
        });
        return raiz;
    }

    // ==========================================
    // MENÚ LATERAL
    // ==========================================
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

        String[] opciones = {"⌂  Inicio", "▦  Mis eventos", "＋  Crear evento", "♧  Proveedores",
                "♙  Invitados", "$  Gestión financiera", "⚙  Configuración"};
        for (String opcion : opciones) {
            Label item = etiqueta(opcion, "menu-item");
            item.setMaxWidth(Double.MAX_VALUE);
            if (opcion.contains("Gestión financiera")) item.getStyleClass().add("active");
            menu.getChildren().add(item);
        }

        Region espacio = new Region();
        VBox.setVgrow(espacio, Priority.ALWAYS);
        VBox perfil = new VBox(4, etiqueta("Organizador", "profile-name"), etiqueta("Mi cuenta", "hint"));
        HBox cuenta = new HBox(12, etiqueta("SC", "avatar"), perfil);
        cuenta.setAlignment(Pos.CENTER_LEFT);
        cuenta.getStyleClass().add("sidebar-bottom");
        menu.getChildren().addAll(espacio, cuenta);
        return menu;
    }

    private HBox crearBarraSuperior() {
        HBox barra = new HBox(18, etiqueta("Inicio / Mis eventos /", "breadcrumb"),
                etiqueta("Gestión financiera", "breadcrumb-current"), espacioHorizontal(),
                etiqueta("♧", "notification"), etiqueta("Organizador", "user-badge"));
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.getStyleClass().add("topbar");
        return barra;
    }

    // ==========================================
    // COMPONENTES
    // ==========================================
    /** Campo de solo lectura que usa el mismo estilo .text-field de los formularios. */
    private TextField valor(String id, String texto) {
        TextField campo = new TextField(texto);
        campo.setId(id);
        campo.setEditable(false);
        campo.setFocusTraversable(false);
        return campo;
    }

    private VBox grupo(String titulo, Node control) {
        return new VBox(9, etiqueta(titulo, "field-label"), control);
    }

    private GridPane grilla() {
        GridPane grid = new GridPane();
        grid.setHgap(23);
        grid.setVgap(23);
        for (int i = 0; i < 2; i++) {
            ColumnConstraints columna = new ColumnConstraints();
            columna.setPercentWidth(50);
            columna.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(columna);
        }
        return grid;
    }

    private VBox crearBarra(String nombre, String porcentaje, double progreso, String color) {
        Circle punto = new Circle(5, javafx.scene.paint.Color.web(color));
        HBox textos = new HBox(10, punto, etiqueta(nombre, "field-label"), espacioHorizontal(),
                etiqueta(porcentaje, "summary-value"));
        textos.setAlignment(Pos.CENTER_LEFT);

        ProgressBar pb = new ProgressBar(progreso);
        pb.setMaxWidth(Double.MAX_VALUE);
        pb.setPrefHeight(10);
        pb.setStyle("-fx-accent: " + color + "; -fx-control-inner-background: #F1F2F6; " +
                "-fx-background-color: transparent; -fx-background-radius: 6;");
        return new VBox(7, textos, pb);
    }

    private HBox crearFilaMovimiento(String titulo, String fecha, String monto, String estado) {
        String bg = "#EAF8EF";
        String fg = "#247443";
        if (estado.equalsIgnoreCase("Pendiente")) {
            bg = "#FFF3DC";
            fg = "#A96A00";
        } else if (estado.equalsIgnoreCase("Sanción") || estado.equalsIgnoreCase("Cancelado")) {
            bg = "#FFF0F0";
            fg = "#BA2424";
        }

        VBox textos = new VBox(3, etiqueta(titulo, "field-label"), etiqueta(fecha, "hint"));

        Label lblEstado = etiqueta(estado, "hint");
        lblEstado.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg + "; " +
                "-fx-background-radius: 20; -fx-padding: 4 12; -fx-font-weight: bold;");
        Label lblMonto = etiqueta(monto, "field-label");
        VBox valores = new VBox(5, lblMonto, lblEstado);
        valores.setAlignment(Pos.CENTER_RIGHT);

        HBox fila = new HBox(10, textos, espacioHorizontal(), valores);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.setPadding(new Insets(12, 16, 12, 16));
        fila.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                "-fx-border-color: #E1E3EA; -fx-border-radius: 10;");
        return fila;
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

    // ==========================================
    // EXPORTACIÓN A CSV
    // ==========================================
    @SuppressWarnings("unchecked")
    private void exportarCSV(Window owner, Map<String, Object> estado) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Reporte Financiero");
        fileChooser.setInitialFileName("Reporte_Finanzas_NextEventux.csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivo CSV", "*.csv"));

        File file = fileChooser.showSaveDialog(owner);

        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file, "UTF-8")) {
                writer.write('\ufeff');

                writer.println("REPORTE FINANCIERO;" + estado.getOrDefault("Planificacion_Descripcion", ""));
                writer.println("Estado del evento;" + estado.getOrDefault("Planificacion_Estado", ""));
                writer.println("Ingresos Totales;$" + estado.getOrDefault("Ingresos", "0.0"));
                writer.println("Egresos Totales;$" + estado.getOrDefault("Egresos", "0.0"));
                writer.println("Balance Final;$" + estado.getOrDefault("Balance", "0.0"));
                writer.println();

                writer.println("--- DETALLE DE MOVIMIENTOS ---");
                writer.println("Fecha;Descripcion;Monto;Estado");

                List<MovimientoFinanciero> movimientos = (List<MovimientoFinanciero>) estado.get("Movimientos");
                if (movimientos != null) {
                    for (MovimientoFinanciero mov : movimientos) {
                        writer.println(mov.getFecha() + ";" + mov.getDescripcion() + ";$" + mov.getValor() + ";" + mov.getEstado());
                    }
                }

                System.out.println("Archivo CSV exportado exitosamente en: " + file.getAbsolutePath());

            } catch (Exception ex) {
                System.out.println("Error al exportar el archivo CSV: " + ex.getMessage());
            }
        }
    }
}
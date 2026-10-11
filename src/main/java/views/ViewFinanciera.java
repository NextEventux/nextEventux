package views;

import controllers.ControllerFinanciera;
import entities.Servicio;
import entities.MovimientoFinanciero;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import javafx.stage.FileChooser;
import java.io.File;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ViewFinanciera {

    private ControllerFinanciera controller;

    public ViewFinanciera(ControllerFinanciera controller) {
        this.controller = controller;
    }

    @SuppressWarnings("unchecked")
    public void mostrarEstadoFinanciero(int idEvento) {
        // 1. OBTENER DATOS DEL BACKEND
        Map<String, Object> estado = controller.consultarEstadoFinanciero(idEvento);

        // Extraer variables generales
        String descripcion = estado.containsKey("Planificacion_Descripcion") ? estado.get("Planificacion_Descripcion").toString() : "Evento sin planificación";
        String estadoPlan = estado.containsKey("Planificacion_Estado") ? estado.get("Planificacion_Estado").toString() : "Desconocido";
        String ingresos = estado.containsKey("Ingresos") ? estado.get("Ingresos").toString() : "0.0";
        String egresos = estado.containsKey("Egresos") ? estado.get("Egresos").toString() : "0.0";
        String balance = estado.containsKey("Balance") ? estado.get("Balance").toString() : "0.0";

        // Extraer listas de objetos (Si no existen aún en el backend, se crean listas vacías para no romper la vista)
        List<Servicio> servicios = estado.containsKey("Servicios") ? (List<Servicio>) estado.get("Servicios") : new ArrayList<>();
        List<MovimientoFinanciero> movimientos = estado.containsKey("Movimientos") ? (List<MovimientoFinanciero>) estado.get("Movimientos") : new ArrayList<>();

        // Contenedor principal
        VBox root = new VBox(25);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: #1A1A1A; -fx-font-family: 'Segoe UI', sans-serif;");

        // --- HEADER ---
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        
        VBox titleBox = new VBox(5);
        Label lblTitle = new Label("Finanzas");
        lblTitle.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");
        Label lblSubtitle = new Label(descripcion);
        lblSubtitle.setStyle("-fx-text-fill: #A0A0A0; -fx-font-size: 14px;");
        titleBox.getChildren().addAll(lblTitle, lblSubtitle);
        
        Region spacerHeader = new Region();
        HBox.setHgrow(spacerHeader, Priority.ALWAYS);
        
        Button btnExportar = new Button("📥 Exportar");
        btnExportar.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-border-color: #444; -fx-border-radius: 8; -fx-padding: 8 15; -fx-cursor: hand;");
        btnExportar.setOnAction(e -> exportarCSV(null, estado));
        header.getChildren().addAll(titleBox, spacerHeader, btnExportar);

        // --- TARJETAS DE RESUMEN ---
        HBox cardsBox = new HBox(15);
        cardsBox.getChildren().addAll(
            crearTarjeta("Total Ingresos", "$" + ingresos, "#F3EEFF", "#5B44A9"),
            crearTarjeta("Total Egresos", "$" + egresos, "#E8F5EE", "#1E8B5E"),
            crearTarjeta("Balance Final", "$" + balance, "#FFF6E5", "#A67C00"),
            crearTarjeta("Estado", estadoPlan, "#FFECEC", "#A23A3A")
        );

        HBox mainContent = new HBox(40);
        
        // ==========================================
        // SECCIÓN ROJA: PRESUPUESTO POR CATEGORÍA
        // ==========================================
        VBox leftColumn = new VBox(15);
        leftColumn.setPrefWidth(350);
        Label lblCategorias = new Label("Presupuesto por categoría");
        lblCategorias.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        leftColumn.getChildren().add(lblCategorias);

        if (servicios.isEmpty()) {
            Label emptySrv = new Label("No hay servicios asignados a este evento.");
            emptySrv.setStyle("-fx-text-fill: #A0A0A0;");
            leftColumn.getChildren().add(emptySrv);
        } else {
            // Agrupar precios por categoría y sumar el total
            Map<String, Double> costosPorCategoria = new HashMap<>();
            double totalPresupuesto = 0.0;
            for (Servicio s : servicios) {
                String cat = s.getCategoria() != null ? s.getCategoria() : "Otros";
                costosPorCategoria.put(cat, costosPorCategoria.getOrDefault(cat, 0.0) + s.getPrecio());
                totalPresupuesto += s.getPrecio();
            }

            // Dibujar una barra por cada categoría
            String[] colores = {"#8250DF", "#27AE60", "#E67E22", "#E84393", "#3498DB"};
            int colorIdx = 0;
            for (Map.Entry<String, Double> entry : costosPorCategoria.entrySet()) {
                double porcentajeVal = totalPresupuesto > 0 ? (entry.getValue() / totalPresupuesto) : 0;
                String porcentajeStr = Math.round(porcentajeVal * 100) + "%";
                String color = colores[colorIdx % colores.length];
                
                leftColumn.getChildren().add(crearBarraProgreso(entry.getKey(), porcentajeStr, porcentajeVal, color));
                colorIdx++;
            }
        }

        // =========================
        // ÚLTIMOS MOVIMIENTOS
        // =========================
        VBox rightColumn = new VBox(15);
        rightColumn.setPrefWidth(350);
        Label lblMovimientos = new Label("Últimos movimientos");
        lblMovimientos.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        rightColumn.getChildren().add(lblMovimientos);

        if (movimientos.isEmpty()) {
            Label emptyMov = new Label("No hay movimientos registrados.");
            emptyMov.setStyle("-fx-text-fill: #A0A0A0;");
            rightColumn.getChildren().add(emptyMov);
        } else {
            // Dibujar cada movimiento. Asumimos que el más reciente viene primero desde la Base de Datos.
            for (MovimientoFinanciero mov : movimientos) {
                String titulo = mov.getDescripcion() != null ? mov.getDescripcion() : "Movimiento";
                String fecha = mov.getFecha() != null ? mov.getFecha().toString() : "--";
                String monto = "$" + mov.getValor();
                String estadoMov = mov.getEstado() != null ? mov.getEstado() : "Pendiente";

                // Asignar colores dinámicamente según el estado
                String bgBadge = "#E8F5EE"; 
                String textBadge = "#1E8B5E"; // Por defecto: Verde (Pagado)
                
                if (estadoMov.equalsIgnoreCase("Pendiente")) {
                    bgBadge = "#FFF6E5"; textBadge = "#A67C00"; // Amarillo
                } else if (estadoMov.equalsIgnoreCase("Sanción") || estadoMov.equalsIgnoreCase("Cancelado")) {
                    bgBadge = "#FFECEC"; textBadge = "#A23A3A"; // Rojo
                }

                rightColumn.getChildren().add(crearItemMovimiento(titulo, fecha, monto, estadoMov, bgBadge, textBadge));
            }
        }

        mainContent.getChildren().addAll(leftColumn, rightColumn);
        root.getChildren().addAll(header, cardsBox, mainContent);

        Scene scene = new Scene(root, 800, 500);
        Stage stage = new Stage();
        stage.setTitle("NextEventux - Finanzas");
        stage.setScene(scene);
        stage.show();
    }

    private VBox crearTarjeta(String titulo, String valor, String bgColor, String textColor) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setPrefWidth(180);
        card.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 12;");

        Label lblTitulo = new Label(titulo);
        lblTitulo.setStyle("-fx-text-fill: " + textColor + "; -fx-font-size: 14px;");
        
        Label lblValor = new Label(valor);
        lblValor.setStyle("-fx-text-fill: " + textColor + "; -fx-font-size: 24px; -fx-font-weight: bold;");

        card.getChildren().addAll(lblTitulo, lblValor);
        return card;
    }

    private VBox crearBarraProgreso(String nombre, String porcentaje, double progreso, String colorBarra) {
        VBox box = new VBox(5);
        HBox textos = new HBox();
        Label lblNombre = new Label(nombre);
        lblNombre.setStyle("-fx-text-fill: white;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label lblPorcentaje = new Label(porcentaje);
        lblPorcentaje.setStyle("-fx-text-fill: white;");
        textos.getChildren().addAll(lblNombre, spacer, lblPorcentaje);

        ProgressBar pb = new ProgressBar(progreso);
        pb.setMaxWidth(Double.MAX_VALUE);
        pb.setStyle("-fx-accent: " + colorBarra + "; -fx-control-inner-background: #333333; -fx-background-color: transparent;");
        
        box.getChildren().addAll(textos, pb);
        return box;
    }

    private HBox crearItemMovimiento(String titulo, String fecha, String monto, String estado, String bgBadge, String textBadge) {
        HBox item = new HBox();
        item.setAlignment(Pos.CENTER_LEFT);

        VBox textos = new VBox(2);
        Label lblTitulo = new Label(titulo);
        lblTitulo.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        Label lblFecha = new Label(fecha);
        lblFecha.setStyle("-fx-text-fill: #A0A0A0; -fx-font-size: 12px;");
        textos.getChildren().addAll(lblTitulo, lblFecha);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox valores = new VBox(5);
        valores.setAlignment(Pos.CENTER_RIGHT);
        Label lblMonto = new Label(monto);
        lblMonto.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        Label lblEstado = new Label(estado);
        lblEstado.setStyle("-fx-background-color: " + bgBadge + "; -fx-text-fill: " + textBadge + "; -fx-padding: 2 10; -fx-background-radius: 10; -fx-font-size: 11px;");
        valores.getChildren().addAll(lblMonto, lblEstado);

        item.getChildren().addAll(textos, spacer, valores);
        return item;
    }

    private void exportarCSV(Stage stage, Map<String, Object> estado) {
        // 1. Configurar la ventana de "Guardar como..."
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Reporte Financiero");
        fileChooser.setInitialFileName("Reporte_Finanzas_NextEventux.csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivo CSV", "*.csv"));

        // 2. Mostrar la ventana y obtener la ruta que eligió el usuario
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            // 3. Escribir el archivo
            try (PrintWriter writer = new PrintWriter(file, "UTF-8")) {
                // Escribir un carácter especial (BOM) para que Excel reconozca las tildes perfectamente
                writer.write('\ufeff'); 

                // --- Escribir Resumen General ---
                writer.println("REPORTE FINANCIERO;" + estado.getOrDefault("Planificacion_Descripcion", ""));
                writer.println("Estado del evento;" + estado.getOrDefault("Planificacion_Estado", ""));
                writer.println("Ingresos Totales;$" + estado.getOrDefault("Ingresos", "0.0"));
                writer.println("Egresos Totales;$" + estado.getOrDefault("Egresos", "0.0"));
                writer.println("Balance Final;$" + estado.getOrDefault("Balance", "0.0"));
                writer.println(); // Línea en blanco

                // --- Escribir Lista de Movimientos ---
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
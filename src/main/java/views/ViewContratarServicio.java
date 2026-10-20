package views;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import controllers.ControllerContratacionServicio;
import entities.ConfirmacionContratacion;
import entities.DisponibilidadContratacion;
import entities.PresupuestoContratacion;
import entities.PropuestaServicio;
import entities.ResumenContratacionServicio;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * CU-13: contratar el servicio de la propuesta seleccionada.
 *
 * El organizador revisa la versión exacta de la propuesta, comprueba la
 * disponibilidad del proveedor y el presupuesto del evento, revisa el resumen
 * final y confirma. Cerrar la ventana antes de confirmar no reserva nada (A3).
 */
public class ViewContratarServicio {

    private static final NumberFormat MONEDA = NumberFormat.getCurrencyInstance(
            Locale.forLanguageTag("es-CO"));

    private static final DateTimeFormatter FECHA_HORA
            = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final DateTimeFormatter FECHA
            = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ControllerContratacionServicio controller;
    private final int idOrganizador;
    private final PropuestaServicio propuesta;
    private final Runnable alFinalizar;

    private Stage ventana;
    private ResumenContratacionServicio resumen;

    private boolean disponibilidadVerificada;
    private boolean presupuestoVerificado;

    private final VBox contenido = new VBox(16);

    private final Label resultadoDisponibilidad = new Label();
    private final Label resultadoPresupuesto = new Label();
    private final VBox resumenFinal = new VBox(8);

    private final Button verificarDisponibilidad
            = new Button("Verificar disponibilidad");
    private final Button verificarPresupuesto
            = new Button("Comprobar presupuesto");
    private final Button confirmar
            = new Button("Confirmar contratación");
    private final Button cancelar = new Button("No contratar todavía");

    public ViewContratarServicio(
            ControllerContratacionServicio controller,
            int idOrganizador,
            PropuestaServicio propuesta,
            Runnable alFinalizar) {

        this.controller = controller;
        this.idOrganizador = idOrganizador;
        this.propuesta = propuesta;
        this.alFinalizar = alFinalizar;
    }

    public void mostrar(Window propietario) {

        // Pasos 1 y 2: recuperar la versión exacta antes de abrir la ventana
        try {
            resumen = controller.consultarResumen(
                    idOrganizador, propuesta.getIdPropuesta());

        } catch (RuntimeException e) {
            mostrarError("No se puede contratar esta propuesta",
                    e.getMessage());
            return;
        }

        ventana = new Stage();
        ventana.setTitle("Contratar servicio");
        ventana.initModality(Modality.APPLICATION_MODAL);

        if (propietario != null) {
            ventana.initOwner(propietario);
        }

        contenido.getStyleClass().addAll("content", "cu12-content", "cu13-dialog");
        contenido.setPadding(new Insets(24));
        construirFormulario();

        ScrollPane scroll = new ScrollPane(contenido);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: #F8F9FC;");

        Scene escena = new Scene(scroll, 720, 760);

        if (propietario != null && propietario.getScene() != null) {
            escena.getStylesheets().addAll(
                    propietario.getScene().getStylesheets());
        }

        ventana.setScene(escena);
        ventana.showAndWait();
    }

    // ------------------------------------------------------------------
    // Pantalla de revisión (pasos 1 a 7)
    // ------------------------------------------------------------------

    private void construirFormulario() {

        contenido.getChildren().clear();

        Label titulo = new Label("Contratar servicio");
        titulo.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        Label ayuda = new Label(
                "Revisa la propuesta seleccionada, verifica la disponibilidad "
                + "del proveedor y el presupuesto del evento. El horario solo "
                + "se reserva cuando confirmas la contratación.");
        ayuda.setWrapText(true);
        ayuda.getStyleClass().add("cu12-card-detail");

        verificarDisponibilidad.setOnAction(e -> comprobarDisponibilidad());

        verificarPresupuesto.setOnAction(e -> comprobarPresupuesto());
        verificarPresupuesto.setDisable(true);

        confirmar.getStyleClass().add("btn-primary");
        confirmar.setDisable(true);
        confirmar.setOnAction(e -> confirmarContratacion());

        cancelar.setOnAction(e -> ventana.close());

        resultadoDisponibilidad.setWrapText(true);
        resultadoPresupuesto.setWrapText(true);

        HBox acciones = new HBox(12, cancelar, confirmar);
        acciones.setAlignment(Pos.CENTER_RIGHT);

        contenido.getChildren().addAll(
                titulo,
                ayuda,
                tarjeta("1. Propuesta seleccionada", crearDetallePropuesta()),
                tarjeta("2. Disponibilidad del proveedor",
                        verificarDisponibilidad, resultadoDisponibilidad),
                tarjeta("3. Presupuesto del evento",
                        verificarPresupuesto, resultadoPresupuesto),
                tarjeta("4. Resumen final", resumenFinal),
                acciones);

        resumenFinal.getChildren().setAll(
                nota("Completa la verificación de disponibilidad y de "
                        + "presupuesto para ver el resumen final."));
    }

    private VBox crearDetallePropuesta() {

        VBox detalle = new VBox(6);

        Label valor = new Label(MONEDA.format(resumen.valor()));
        valor.getStyleClass().add("cu12-price");

        detalle.getChildren().addAll(
                valor,
                nota("Servicio: " + resumen.nombreServicio()
                        + "  •  Proveedor: " + resumen.nombreProveedor()),
                nota("Evento: " + resumen.nombreEvento() + " ("
                        + resumen.fechaInicioEvento().format(FECHA_HORA)
                        + " a "
                        + resumen.fechaFinEvento().format(FECHA_HORA) + ")"),
                nota("Propuesta #" + resumen.idPropuesta()
                        + "  •  Versión " + resumen.numeroVersion()),
                nota("Necesidad: " + resumen.necesidad()),
                nota("Alcance: " + resumen.alcance()),
                nota("Condiciones acordadas: " + resumen.condiciones()));

        return detalle;
    }

    private void comprobarDisponibilidad() {

        try {
            DisponibilidadContratacion resultado
                    = controller.verificarDisponibilidad(
                            idOrganizador, resumen.idPropuesta());

            disponibilidadVerificada = resultado.disponible();
            marcar(resultadoDisponibilidad,
                    resultado.mensaje(), resultado.disponible());

            verificarPresupuesto.setDisable(!disponibilidadVerificada);

            if (!disponibilidadVerificada) {
                presupuestoVerificado = false;
            }

            actualizarResumenFinal(null);

        } catch (RuntimeException e) {
            disponibilidadVerificada = false;
            marcar(resultadoDisponibilidad, e.getMessage(), false);
            verificarPresupuesto.setDisable(true);
            actualizarResumenFinal(null);
        }
    }

    private PresupuestoContratacion ultimoPresupuesto;

    private void comprobarPresupuesto() {

        try {
            ultimoPresupuesto
                    = controller.verificarPresupuesto(
                            idOrganizador, resumen.idPropuesta());

            presupuestoVerificado = ultimoPresupuesto.suficiente();

            marcar(resultadoPresupuesto,
                    "Presupuesto total: "
                    + MONEDA.format(ultimoPresupuesto.total())
                    + "\nYa comprometido en otras necesidades: "
                    + MONEDA.format(ultimoPresupuesto.comprometido())
                    + "\nPrecio final acordado: "
                    + MONEDA.format(ultimoPresupuesto.valor())
                    + "\n" + ultimoPresupuesto.mensaje(),
                    ultimoPresupuesto.suficiente());

            actualizarResumenFinal(ultimoPresupuesto);

        } catch (RuntimeException e) {
            presupuestoVerificado = false;
            marcar(resultadoPresupuesto, e.getMessage(), false);
            actualizarResumenFinal(null);
        }
    }

    private void actualizarResumenFinal(PresupuestoContratacion presupuesto) {

        boolean listo = disponibilidadVerificada && presupuestoVerificado;

        confirmar.setDisable(!listo);

        if (!listo || presupuesto == null) {
            resumenFinal.getChildren().setAll(
                    nota("Completa la verificación de disponibilidad y de "
                            + "presupuesto para ver el resumen final."));
            return;
        }

        resumenFinal.getChildren().setAll(
                nota("Se asociará a la contratación la propuesta #"
                        + resumen.idPropuesta() + ", versión "
                        + resumen.numeroVersion() + ", con las condiciones "
                        + "indicadas arriba. No podrán modificarse después."),
                nota("Se reservará el horario del proveedor y se "
                        + "comprometerán " + MONEDA.format(resumen.valor())
                        + " del presupuesto."),
                nota("Presupuesto disponible después de contratar: "
                        + MONEDA.format(presupuesto.disponibleDespues())),
                nota("Las demás propuestas de esta necesidad quedarán como "
                        + "\"No seleccionada\" y se avisará a sus proveedores."));
    }

    // ------------------------------------------------------------------
    // Confirmación (pasos 7 y 8)
    // ------------------------------------------------------------------

    private void confirmarContratacion() {

        Alert pregunta = new Alert(
                Alert.AlertType.CONFIRMATION,
                "¿Confirmas la contratación de \"" + resumen.nombreServicio()
                + "\" por " + MONEDA.format(resumen.valor()) + "?",
                ButtonType.YES,
                ButtonType.NO);

        pregunta.setHeaderText("Confirmar contratación");
        pregunta.initOwner(ventana);

        if (pregunta.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
            return;
        }

        try {
            ConfirmacionContratacion confirmacion
                    = controller.contratar(
                            idOrganizador, resumen.idPropuesta());

            mostrarConfirmacion(confirmacion);

        } catch (RuntimeException e) {

            // A1 y A2: no se formalizó nada y la propuesta sigue seleccionada
            mostrarError("No se pudo contratar el servicio",
                    e.getMessage());

            disponibilidadVerificada = false;
            presupuestoVerificado = false;
            confirmar.setDisable(true);
            verificarPresupuesto.setDisable(true);
            resultadoDisponibilidad.setText("");
            resultadoPresupuesto.setText("");
            actualizarResumenFinal(null);
        }
    }

    private void mostrarConfirmacion(ConfirmacionContratacion c) {

        contenido.getChildren().clear();

        Label titulo = new Label("Contratación confirmada");
        titulo.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        Label valor = new Label(MONEDA.format(c.valor()));
        valor.getStyleClass().add("cu12-price");

        VBox detalle = new VBox(6,
                valor,
                nota("Contratación #" + c.idContratacion()),
                nota("Servicio: " + c.nombreServicio()
                        + "  •  Proveedor: " + c.nombreProveedor()),
                nota("Evento: " + c.nombreEvento()),
                nota("Propuesta #" + c.idPropuesta()
                        + "  •  Versión " + c.numeroVersion()),
                nota("Horario reservado: " + c.fechaServicio().format(FECHA)
                        + " de " + c.horaInicio() + " a " + c.horaFin()),
                nota("Alcance: " + c.alcance()),
                nota("Condiciones acordadas: " + c.condiciones()),
                nota("Presupuesto disponible del evento: "
                        + MONEDA.format(c.presupuestoDisponible())),
                nota("El proveedor fue notificado."));

        Button cerrar = new Button("Cerrar");
        cerrar.getStyleClass().add("btn-primary");
        cerrar.setOnAction(e -> ventana.close());

        HBox acciones = new HBox(cerrar);
        acciones.setAlignment(Pos.CENTER_RIGHT);

        contenido.getChildren().addAll(
                titulo,
                tarjeta("Resumen de la contratación", detalle),
                acciones);

        ventana.setOnHidden(e -> {
            if (alFinalizar != null) {
                alFinalizar.run();
            }
        });
    }

    // ------------------------------------------------------------------
    // Utilidades de presentación
    // ------------------------------------------------------------------

    private VBox tarjeta(String titulo, javafx.scene.Node... nodos) {

        Label etiqueta = new Label(titulo);
        etiqueta.getStyleClass().add("cu12-card-title");

        VBox tarjeta = new VBox(10);
        tarjeta.getChildren().add(etiqueta);
        tarjeta.getChildren().addAll(nodos);
        tarjeta.setPadding(new Insets(18));
        tarjeta.setStyle(
                "-fx-background-color: white; -fx-background-radius: 12; "
                + "-fx-border-color: #E8E9EF; -fx-border-radius: 12;");

        return tarjeta;
    }

    private Label nota(String texto) {

        Label etiqueta = new Label(texto);
        etiqueta.setWrapText(true);
        etiqueta.getStyleClass().add("cu12-card-detail");
        return etiqueta;
    }

    private void marcar(Label etiqueta, String mensaje, boolean correcto) {

        etiqueta.setText(mensaje);
        etiqueta.setStyle(correcto
                ? "-fx-text-fill: #237344; -fx-font-weight: bold;"
                : "-fx-text-fill: #B9151B; -fx-font-weight: bold;");
    }

    private void mostrarError(String encabezado, String mensaje) {

        Alert alerta = new Alert(
                Alert.AlertType.ERROR,
                mensaje == null ? "Ocurrió un error." : mensaje,
                ButtonType.OK);

        alerta.setHeaderText(encabezado);

        if (ventana != null) {
            alerta.initOwner(ventana);
        }

        alerta.showAndWait();
    }
}

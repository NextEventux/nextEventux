
package views;

import controllers.ControllerAdministracion;
import entities.Contratacion;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Vista CU-026:
 * Permite al organizador consultar y responder las solicitudes
 * de contratación enviadas por los clientes.
 */
public class ViewSolicitudesContratacionOrganizador {

    private final ControllerAdministracion controller;

    private final ListView<Contratacion> listaSolicitudes =
            new ListView<>();

    private final Label totalSolicitudes =
            new Label("0 solicitudes recibidas");

    private final NumberFormat formatoMoneda =
            NumberFormat.getCurrencyInstance(
                    Locale.forLanguageTag("es-CO"));

    public ViewSolicitudesContratacionOrganizador(
            ControllerAdministracion controller) {

        if (controller == null) {
            throw new IllegalArgumentException(
                    "El controlador de administracion es obligatorio.");
        }

        this.controller = controller;

        configurarLista();
    }

    /**
     * Construye la pantalla principal.
     */
    public Parent crearPantalla() {

        BorderPane raiz = new BorderPane();
        raiz.getStyleClass().addAll("app", "publicacion");

        VBox contenido = new VBox(22);
        contenido.getStyleClass().add("content");
        contenido.setPadding(new Insets(30));

        Label titulo = etiqueta(
                "Solicitudes de contratación recibidas",
                "page-title");

        Label descripcion = etiqueta(
                "Consulta las solicitudes que los clientes "
                        + "han enviado para contratar tus servicios.",
                "muted");

        descripcion.setWrapText(true);

        HBox encabezado = new HBox(15);
        encabezado.setAlignment(Pos.CENTER_LEFT);

        VBox textos = new VBox(8, titulo, descripcion);
        HBox.setHgrow(textos, Priority.ALWAYS);

        Button actualizar = new Button("↻  Actualizar");
        actualizar.getStyleClass().add("btn-primary");
        actualizar.setOnAction(
                event -> cargarSolicitudes());

        encabezado.getChildren().addAll(textos, actualizar);

        VBox tarjeta = new VBox(18);
        tarjeta.getStyleClass().add("card");
        tarjeta.setPadding(new Insets(24));

        HBox resumen = new HBox(
                totalSolicitudes,
                espacioHorizontal(),
                etiqueta(
                        "Ordenadas de más recientes a más antiguas",
                        "muted"));

        resumen.setAlignment(Pos.CENTER_LEFT);

        listaSolicitudes.setId("listaSolicitudesOrganizador");

        listaSolicitudes.setPlaceholder(
                etiqueta(
                        "Todavía no has recibido solicitudes de contratación.",
                        "muted"));

        listaSolicitudes.setPrefHeight(500);
        listaSolicitudes.setMinHeight(300);

        VBox.setVgrow(listaSolicitudes, Priority.ALWAYS);

        tarjeta.getChildren().addAll(
                resumen,
                new Separator(),
                listaSolicitudes);

        contenido.getChildren().addAll(
                encabezado,
                tarjeta);

        ScrollPane scroll = new ScrollPane(contenido);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("content-scroll");

        raiz.setCenter(scroll);

        cargarSolicitudes();

        return raiz;
    }

    /**
     * Configura la representación de cada solicitud.
     */
    private void configurarLista() {

        listaSolicitudes.setCellFactory(
                lista -> new ListCell<>() {

                    @Override
                    protected void updateItem(
                            Contratacion solicitud,
                            boolean vacia) {

                        super.updateItem(solicitud, vacia);

                        if (vacia || solicitud == null) {
                            setText(null);
                            setGraphic(null);
                            return;
                        }

                        setText(null);

                        String estadoActual =
                                solicitud.getEstado() == null
                                        ? "PENDIENTE"
                                        : solicitud.getEstado();

                        Label numero = etiqueta(
                                "Solicitud #"
                                        + solicitud.getIdContratacion(),
                                "card-title");

                        Label estado = etiqueta(
                                estadoActual,
                                "status-badge");

                        HBox encabezado = new HBox(
                                12,
                                numero,
                                espacioHorizontal(),
                                estado);

                        encabezado.setAlignment(Pos.CENTER_LEFT);

                        // Nombre real del cliente e identificador.
                        String nombreCliente =
                                solicitud.getNombreCliente();

                        if (nombreCliente == null
                                || nombreCliente.isBlank()) {
                            nombreCliente = "Cliente";
                        }

                        Label cliente = etiqueta(
                                "Cliente: " + nombreCliente
                                        + " (#" + solicitud.getIdCliente() + ")",
                                "profile-name");

                        // Nombre real del organizador e identificador.
                        String nombreOrganizador =
                                solicitud.getNombreOrganizador();

                        if (nombreOrganizador == null
                                || nombreOrganizador.isBlank()) {
                            nombreOrganizador = "Organizador";
                        }

                        Label organizador = etiqueta(
                                "Organizador: " + nombreOrganizador
                                        + " (#" + solicitud.getIdOrganizador() + ")",
                                "body-text");

                        Label evento = etiqueta(
                                "Tipo de evento: "
                                        + solicitud.getTipoEvento(),
                                "body-text");

                        Label ciudad = etiqueta(
                                "Ciudad: " + solicitud.getCiudad(),
                                "body-text");

                        Label fecha = etiqueta(
                                "Fecha aproximada: "
                                        + solicitud.getFechaAproximada(),
                                "body-text");

                        BigDecimal presupuesto =
                                solicitud.getPresupuestoEstimado() == null
                                        ? BigDecimal.ZERO
                                        : solicitud.getPresupuestoEstimado();

                        Label valor = etiqueta(
                                "Presupuesto máximo: "
                                        + formatoMoneda.format(presupuesto),
                                "body-text");

                        Label criterios = etiqueta(
                                "Criterios adicionales: "
                                        + (solicitud.getCriteriosBusqueda() == null
                                        || solicitud.getCriteriosBusqueda().isBlank()
                                        ? "Sin criterios adicionales"
                                        : solicitud.getCriteriosBusqueda()),
                                "muted");

                        criterios.setWrapText(true);

                        VBox informacion = new VBox(
                                9,
                                encabezado,
                                cliente,
                                organizador,
                                evento,
                                ciudad,
                                fecha,
                                valor,
                                criterios);

                        informacion.setPadding(new Insets(15));
                        informacion.getStyleClass().add("card");

                        // Acciones únicamente para solicitudes pendientes.
                        if ("PENDIENTE".equalsIgnoreCase(estadoActual)) {

                            Button aceptar =
                                    new Button("✓ Aceptar solicitud");

                            aceptar.getStyleClass().add("btn-primary");

                            Button rechazar =
                                    new Button("✕ Rechazar solicitud");

                            rechazar.getStyleClass().add("btn-secondary");

                            aceptar.setOnAction(event ->
                                    responderSolicitud(
                                            solicitud,
                                            "ACEPTADA"));

                            rechazar.setOnAction(event ->
                                    responderSolicitud(
                                            solicitud,
                                            "RECHAZADA"));

                            HBox acciones = new HBox(
                                    12,
                                    aceptar,
                                    rechazar);

                            acciones.setAlignment(Pos.CENTER_RIGHT);

                            informacion.getChildren().add(acciones);
                        }

                        setGraphic(informacion);
                    }
                });
    }

    /**
     * Consulta exclusivamente las solicitudes del organizador
     * autenticado.
     */
    public void cargarSolicitudes() {

        try {

            List<Contratacion> solicitudes =
                    controller.consultarSolicitudesRecibidas();

            listaSolicitudes.getItems().setAll(solicitudes);

            int cantidad = solicitudes.size();

            totalSolicitudes.setText(
                    cantidad == 1
                            ? "1 solicitud recibida"
                            : cantidad + " solicitudes recibidas");

        } catch (RuntimeException e) {

            mostrarError(
                    e.getMessage() == null
                            ? "No fue posible consultar las solicitudes."
                            : e.getMessage());
        }
    }

    /**
     * Confirma y procesa la respuesta del organizador.
     */
    private void responderSolicitud(
            Contratacion solicitud,
            String nuevoEstado) {

        String accion = "ACEPTADA".equals(nuevoEstado)
                ? "aceptar"
                : "rechazar";

        Alert confirmacion = new Alert(
                Alert.AlertType.CONFIRMATION);

        confirmacion.setTitle("Responder solicitud");

        confirmacion.setHeaderText(
                "¿Deseas " + accion + " la solicitud #"
                        + solicitud.getIdContratacion() + "?");

        confirmacion.setContentText(
                "El cliente recibirá una notificación "
                        + "con el resultado de tu decisión.");

        confirmacion.getButtonTypes().setAll(
                ButtonType.OK,
                ButtonType.CANCEL);

        confirmacion.showAndWait().ifPresent(respuesta -> {

            if (respuesta != ButtonType.OK) {
                return;
            }

            try {

                controller.responderSolicitud(
                        solicitud.getIdContratacion(),
                        nuevoEstado);

                Alert informacion = new Alert(
                        Alert.AlertType.INFORMATION);

                informacion.setTitle("Solicitud actualizada");

                informacion.setHeaderText(
                        "Respuesta registrada correctamente");

                informacion.setContentText(
                        "La solicitud #"
                                + solicitud.getIdContratacion()
                                + " ahora está " + nuevoEstado + ".\n"
                                + "Se notificó al cliente.");

                informacion.showAndWait();

                cargarSolicitudes();

            } catch (RuntimeException e) {

                mostrarError(
                        e.getMessage() == null
                                ? "No fue posible responder la solicitud."
                                : e.getMessage());
            }
        });
    }

    private Label etiqueta(
            String texto,
            String estilo) {

        Label label = new Label(texto);
        label.getStyleClass().add(estilo);

        return label;
    }

    private Region espacioHorizontal() {

        Region espacio = new Region();

        HBox.setHgrow(
                espacio,
                Priority.ALWAYS);

        return espacio;
    }

    private void mostrarError(String mensaje) {

        Alert alerta = new Alert(
                Alert.AlertType.ERROR);

        alerta.setTitle("Solicitudes recibidas");

        alerta.setHeaderText(
                "No fue posible cargar las solicitudes.");

        alerta.setContentText(mensaje);

        alerta.showAndWait();
    }
}

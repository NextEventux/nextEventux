package views;

import controllers.ControllerPublicacion;
import entities.*;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import java.util.function.Function;

public class ViewPublicacion {
    private final ControllerPublicacion controller;
    private final Runnable volver;
    private boolean esAdministrador;
    private final Label usuario = new Label();
    private final ComboBox<Evento> evento = new ComboBox<>();
    private final ListView<SolicitudPublicacion> misSolicitudes = new ListView<>();
    private final ListView<SolicitudPublicacion> pendientes = new ListView<>();
    private final ListView<String> avisosOrganizador = new ListView<>();
    private final ListView<String> avisosAdministrador = new ListView<>();
    private final ListView<PlantillaEvento> plantillas = new ListView<>();
    private final TextArea motivo = new TextArea();
    private final Label mensaje = new Label();

    public ViewPublicacion(ControllerPublicacion controller, Runnable volver) {
        this.controller = controller;
        this.volver = volver;
        usuario.setId("usuarioPublicacion");
        configurarCombo(evento, "eventoPublicacion", "Seleccionar evento", e -> e.getNombre() + " — " + estado(e.getEstado()));
        pendientes.setId("solicitudesPendientes");
        misSolicitudes.setId("misSolicitudes");
        plantillas.setId("plantillasPublicadas");
        avisosOrganizador.setId("notificacionesOrganizador");
        avisosAdministrador.setId("notificacionesAdministrador");
        motivo.setId("motivoRechazo");
        motivo.setPromptText("Escribe el motivo del rechazo (hasta 250 caracteres)");
        motivo.setPrefRowCount(2);
        motivo.setWrapText(true);
        mensaje.setId("mensajePublicacion");
        mensaje.setWrapText(true);
        mensaje.setVisible(false);
        mensaje.setManaged(false);
        for (ListView<?> lista : new ListView<?>[]{misSolicitudes,pendientes,avisosOrganizador,avisosAdministrador,plantillas}) {
            lista.setPrefHeight(170);
            lista.setPlaceholder(new Label("No hay elementos para mostrar."));
        }
        configurarAvisos(avisosOrganizador);
        configurarAvisos(avisosAdministrador);
    }

    public Parent crearPantalla() {
        BorderPane raiz = new BorderPane();
        raiz.getStyleClass().addAll("app", "publicacion");
        ImageView logo = new ImageView(new Image(getClass().getResource("/images/nexteventux-logo.png").toExternalForm()));
        logo.setViewport(new Rectangle2D(75,225,1110,725));
        logo.setFitWidth(210); logo.setFitHeight(138); logo.setPreserveRatio(true);
        VBox menu = new VBox(24,logo,etiqueta("GESTIÓN DE EVENTOS", "brand-caption"),
                etiqueta("↑ Subir al repositorio", "breadcrumb-current"));
        menu.setPrefWidth(250); menu.setMinWidth(250); menu.getStyleClass().add("sidebar");
        menu.setAlignment(Pos.TOP_CENTER);
        raiz.setLeft(menu);
        Button actualizar = boton("Actualizar", "actualizarPublicacion", this::actualizar, false);
        HBox barra = new HBox(20,etiqueta("Mis eventos / Subir al repositorio", "breadcrumb"),espacio(),actualizar);
        barra.setAlignment(Pos.CENTER_LEFT); barra.getStyleClass().add("topbar");
        VBox contenido = new VBox(22);
        contenido.getStyleClass().add("content");
        Label explicacion = etiqueta("Comparte un evento finalizado como plantilla. La publicación requiere aprobación del administrador.","muted");
        explicacion.setWrapText(true);
        try {
            esAdministrador = controller.esAdministrador();
            usuario.setText(controller.nombreUsuario());
        } catch (IllegalStateException e) {
            mostrar(e.getMessage(), false);
            return new VBox(20, mensaje);
        }
        Tab organizacion = new Tab("Solicitar publicación", panelOrganizador());
        organizacion.setId("tabOrganizador");
        Tab revision = new Tab("Revisión del administrador", panelAdministrador());
        revision.setId("tabAdministrador");
        Tab catalogo = new Tab("Plantillas publicadas", panelPlantillas());
        catalogo.setId("tabPlantillas");
        TabPane tabs = new TabPane(esAdministrador ? revision : organizacion, catalogo);
        tabs.setId("tabsPublicacion");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        contenido.getChildren().addAll(etiqueta("Subir evento al repositorio","page-title"),explicacion,usuario,mensaje,tabs);
        ScrollPane scroll = new ScrollPane(contenido); scroll.setFitToWidth(true); scroll.getStyleClass().add("content-scroll");
        BorderPane principal = new BorderPane(scroll,barra,null,null,null);
        raiz.setCenter(principal);
        raiz.widthProperty().addListener((obs,antes,ancho)-> {
            menu.setVisible(ancho.doubleValue()>=1100); menu.setManaged(menu.isVisible());
        });
        // La navegación de regreso permanece disponible incluso si se oculta el menú.
        Button regresar = boton(esAdministrador ? "← Inicio" : "← Crear evento","volverCrearCompacto",volver,false);
        barra.getChildren().add(0,regresar);
        actualizar();
        return raiz;
    }

    private VBox panelOrganizador() {
        Button solicitar = boton("↑ Solicitar publicación","solicitarPublicacion",()-> {
            if (evento.getValue()==null) {
                mostrar("Selecciona un evento de tu cuenta.",false); return;
            }
            ejecutar(()->controller.solicitar(evento.getValue().getIdEvento()),
                    "Solicitud enviada al administrador. El evento todavía no está publicado.");
        },true);
        VBox panel = tarjeta(etiqueta("Selecciona el evento que quieres compartir","card-title"),
                campo("Evento",evento),
                etiqueta("Solo pueden publicarse eventos finalizados.","hint"),solicitar,
                etiqueta("Mis solicitudes","section-title"),misSolicitudes,
                etiqueta("Notificaciones internas","section-title"),avisosOrganizador);
        return panel;
    }

    private VBox panelAdministrador() {
        Button aprobar = boton("Aprobar y publicar","aprobarPublicacion",()->revisar(true),true);
        Button rechazar = boton("Rechazar","rechazarPublicacion",()->revisar(false),false);
        HBox acciones = new HBox(12,aprobar,rechazar);
        return tarjeta(etiqueta("Revisar solicitudes de publicación","card-title"),
                etiqueta("Solicitudes pendientes","section-title"),pendientes,campo("Motivo del rechazo",motivo),acciones,
                etiqueta("Notificaciones internas","section-title"),avisosAdministrador);
    }

    private VBox panelPlantillas() {
        Label privacidad = etiqueta("Las plantillas contienen tipo y duración. No incluyen nombres del evento original, invitados, presupuesto, ubicación ni descripciones.","muted");
        privacidad.setWrapText(true);
        plantillas.setPrefHeight(350);
        return tarjeta(etiqueta("Plantillas reutilizables","card-title"),privacidad,plantillas);
    }

    private void revisar(boolean aprobar) {
        if (pendientes.getSelectionModel().getSelectedItem()==null) {
            mostrar("Selecciona una solicitud pendiente.",false); return;
        }
        int solicitud=pendientes.getSelectionModel().getSelectedItem().id();
        ejecutar(()->controller.revisar(solicitud,aprobar,motivo.getText()),
                aprobar ? "Plantilla publicada. Se notificó al organizador." : "Solicitud rechazada. Se notificó el motivo al organizador.");
    }

    private void ejecutar(Runnable accion,String exito) {
        try {
            accion.run();
        } catch (IllegalArgumentException | IllegalStateException e) {
            mostrar(e.getMessage(),false); return;
        }
        motivo.clear();
        actualizar();
        // Una actualización de listas puede fallar después de una operación ya confirmada.
        if (!mensaje.getStyleClass().contains("error")) mostrar(exito,true);
    }

    private void actualizar() {
        mensaje.getStyleClass().remove("error");
        // Limpiar primero evita mostrar datos anteriores si la sesión dejó de ser válida.
        pendientes.getItems().clear(); misSolicitudes.getItems().clear();
        avisosOrganizador.getItems().clear(); avisosAdministrador.getItems().clear();
        plantillas.getItems().clear();
        Evento anterior = evento.getValue();
        evento.getItems().clear();
        try {
            if (esAdministrador) {
                pendientes.getItems().setAll(controller.listarSolicitudes());
                avisosAdministrador.getItems().setAll(controller.notificaciones());
            } else {
                evento.getItems().setAll(controller.listarEventos());
                if (anterior != null) evento.getItems().stream().filter(e -> e.getIdEvento().equals(anterior.getIdEvento()))
                        .findFirst().ifPresent(e -> evento.getSelectionModel().select(e));
                misSolicitudes.getItems().setAll(controller.listarSolicitudes());
                avisosOrganizador.getItems().setAll(controller.notificaciones());
            }
            plantillas.getItems().setAll(controller.listarPlantillas());
        } catch (IllegalStateException e) { mostrar(e.getMessage(),false); }
    }

    private void mostrar(String texto,boolean exito) {
        mensaje.setText(texto); mensaje.getStyleClass().setAll("message",exito?"success":"error");
        mensaje.setVisible(true); mensaje.setManaged(true);
    }
    private static String estado(String estado) {
        return switch (estado.toUpperCase(java.util.Locale.ROOT)) {
            case "PLANEADO" -> "En planeación";
            case "FINALIZADO" -> "Finalizado";
            case "CANCELADO" -> "Cancelado";
            case "CONFIRMADO" -> "Confirmado";
            default -> estado;
        };
    }
    private Label etiqueta(String texto,String estilo) { Label label=new Label(texto); label.getStyleClass().add(estilo); return label; }
    private Region espacio() { Region espacio=new Region(); HBox.setHgrow(espacio,Priority.ALWAYS); return espacio; }
    private VBox tarjeta(javafx.scene.Node... nodos) { VBox box=new VBox(18,nodos); box.getStyleClass().add("card"); return box; }
    private VBox campo(String texto,javafx.scene.Node control) { return new VBox(9,etiqueta(texto,"field-label"),control); }
    private Button boton(String texto,String id,Runnable accion,boolean principal) {
        Button button=new Button(texto); button.setId(id); button.getStyleClass().add(principal?"btn-primary":"btn-secondary");
        button.setOnAction(e->accion.run()); return button;
    }
    private <T> void configurarCombo(ComboBox<T> combo,String id,String prompt,Function<T,String> texto) {
        combo.setId(id); combo.setPromptText(prompt); combo.setMaxWidth(Double.MAX_VALUE);
        combo.setCellFactory(lista->celda(texto,prompt)); combo.setButtonCell(celda(texto,prompt));
    }
    private <T> ListCell<T> celda(Function<T,String> texto,String prompt) {
        return new ListCell<>() {
            @Override protected void updateItem(T item,boolean empty) {
                super.updateItem(item,empty); setText(item==null?prompt:texto.apply(item));
            }
        };
    }
    private void configurarAvisos(ListView<String> lista) {
        lista.setCellFactory(v->new ListCell<>() {
            @Override protected void updateItem(String item,boolean empty) {
                super.updateItem(item,empty); setText(null);
                if(empty || item==null) { setGraphic(null); return; }
                Label texto=new Label(item); texto.setWrapText(true);
                texto.maxWidthProperty().bind(lista.widthProperty().subtract(45)); setGraphic(texto);
            }
        });
    }
}

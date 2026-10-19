package views;

import controllers.ControllerEventos;
import entities.Evento;
import entities.Organizador;
import entities.SesionUsuario;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import views.components.MenuCompartido;

public class ViewCancelacion {
    private final ControllerEventos controller;
    private final Runnable volver;
    private final Integer organizadorInicial;
    private final Integer eventoInicial;
    private final SesionUsuario sesion;
    private final Runnable abrirEventos;
    private final Runnable abrirMisEventos;
    private final Runnable abrirCatalogo;
    private final Runnable abrirInvitados;
    private final Runnable cerrarSesion;
    private final ComboBox<Organizador> organizador = new ComboBox<>();
    private final ComboBox<Evento> evento = new ComboBox<>();
    private final CheckBox confirmar = new CheckBox("Confirmo que deseo cancelar el evento y revocar sus invitaciones.");
    private final Label mensaje = new Label();
    private final ListView<String> avisos = new ListView<>();

    public ViewCancelacion(ControllerEventos controller, Runnable volver) {
        this(controller,volver,null,null);
    }

    public ViewCancelacion(
            ControllerEventos controller,
            Runnable volver,
            Integer organizadorInicial,
            Integer eventoInicial) {

        this(controller,volver,organizadorInicial,eventoInicial,
                null,null,null,null,null,null);
    }

    public ViewCancelacion(
            ControllerEventos controller,
            Runnable volver,
            Integer organizadorInicial,
            Integer eventoInicial,
            SesionUsuario sesion,
            Runnable abrirEventos,
            Runnable abrirMisEventos,
            Runnable abrirCatalogo,
            Runnable abrirInvitados,
            Runnable cerrarSesion) {

        this.controller=controller;
        this.volver=volver;
        this.organizadorInicial=organizadorInicial;
        this.eventoInicial=eventoInicial;
        this.sesion=sesion;
        this.abrirEventos=abrirEventos;
        this.abrirMisEventos=abrirMisEventos;
        this.abrirCatalogo=abrirCatalogo;
        this.abrirInvitados=abrirInvitados;
        this.cerrarSesion=cerrarSesion;
    }

    public Parent crearPantalla() {
        organizador.setId("organizadorCancelacion"); organizador.setPromptText("Seleccionar organizador");
        evento.setId("eventoCancelacion"); evento.setPromptText("Seleccionar evento");
        organizador.setMaxWidth(Double.MAX_VALUE); evento.setMaxWidth(Double.MAX_VALUE);
        evento.setCellFactory(lista->celdaEvento()); evento.setButtonCell(celdaEvento());
        confirmar.setId("confirmarCancelacion"); confirmar.setWrapText(true);
        mensaje.setId("mensajeCancelacion"); mensaje.setWrapText(true); mensaje.setVisible(false); mensaje.setManaged(false);
        avisos.setId("avisosCancelacion"); avisos.setPrefHeight(150); avisos.setPlaceholder(new Label("No hay avisos de cancelación."));
        avisos.setCellFactory(lista->new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item,empty); setText(null);
                if(empty || item==null) { setGraphic(null); return; }
                Label texto=new Label(item); texto.setWrapText(true); texto.maxWidthProperty().bind(avisos.widthProperty().subtract(40));
                setGraphic(texto);
            }
        });
        organizador.setOnAction(e->cargarEventos());
        evento.setOnAction(e-> { confirmar.setSelected(false); cargarAvisos(); });
        Button cancelar=new Button("Cancelar evento"); cancelar.setId("cancelarEvento"); cancelar.getStyleClass().add("btn-primary");
        cancelar.disableProperty().bind(organizador.valueProperty().isNull().or(evento.valueProperty().isNull()).or(confirmar.selectedProperty().not()));
        cancelar.setOnAction(e->cancelar());
        Label titulo=new Label("Cancelar evento"); titulo.getStyleClass().add("page-title");
        Label ayuda=new Label("Se liberarán las reservas del evento y se revocarán las invitaciones enviadas. No se puede cancelar un evento finalizado.");
        ayuda.getStyleClass().add("muted"); ayuda.setWrapText(true);
        Label demo=new Label("Selección de organizador para demostración. Inicio de sesión pendiente de integración.");
        demo.getStyleClass().add("status"); demo.setWrapText(true);
        demo.setVisible(organizadorInicial==null); demo.setManaged(demo.isVisible());
        VBox tarjeta=new VBox(16,new Label("Organizador"),organizador,new Label("Evento"),evento,confirmar,cancelar,
                mensaje,new Separator(),new Label("Avisos internos registrados para los afectados"),avisos);
        tarjeta.getStyleClass().add("card");
        VBox contenido=new VBox(22,titulo,ayuda,demo,tarjeta);
        contenido.getStyleClass().add("content"); contenido.setMaxWidth(950);
        StackPane centro=new StackPane(contenido); centro.setAlignment(Pos.TOP_CENTER);
        ScrollPane scroll=new ScrollPane(centro); scroll.setFitToWidth(true); scroll.getStyleClass().add("content-scroll");
        BorderPane principal=new BorderPane(scroll);
        principal.setTop(MenuCompartido.crearBarraSuperior(sesion,"Cancelar evento","Mis eventos",volver));
        BorderPane raiz=new BorderPane();
        raiz.setLeft(MenuCompartido.crear(
                sesion,MenuCompartido.Pagina.MIS_EVENTOS,abrirEventos,
                abrirInvitados,abrirCatalogo,null,cerrarSesion,abrirMisEventos));
        raiz.setCenter(principal);
        raiz.getStyleClass().add("app");
        try {
            if(organizadorInicial==null) {
                organizador.getItems().setAll(controller.listarOrganizadores());
            } else {
                Organizador seleccionado=controller.listarOrganizadores().stream()
                        .filter(item->item.id()==organizadorInicial)
                        .findFirst()
                        .orElse(null);
                if(seleccionado==null) {
                    mostrar("No se encontró el organizador de la sesión.",false);
                } else {
                    organizador.getItems().setAll(seleccionado);
                    organizador.getSelectionModel().select(seleccionado);
                    organizador.setDisable(true);
                    cargarEventos();
                    if(eventoInicial!=null) {
                        Evento eventoSeleccionado=evento.getItems().stream()
                                .filter(item->eventoInicial.equals(item.getIdEvento()))
                                .findFirst()
                                .orElse(null);
                        if(eventoSeleccionado==null) {
                            mostrar("El evento seleccionado ya no está disponible para esta cuenta.",false);
                        } else {
                            evento.getSelectionModel().select(eventoSeleccionado);
                            evento.setDisable(true);
                            cargarAvisos();
                        }
                    }
                }
            }
        }
        catch(IllegalStateException e) { mostrar(e.getMessage(),false); }
        return raiz;
    }

    private void cancelar() {
        try {
            controller.cancelarEvento(evento.getValue().getIdEvento(),organizador.getValue().id(),confirmar.isSelected());
            mostrar("El evento fue cancelado. Se liberaron sus compromisos y se registraron los avisos internos.",true);
            cargarEventos();
        } catch(IllegalArgumentException | IllegalStateException e) { mostrar(e.getMessage(),false); }
    }

    private void cargarEventos() {
        Evento anterior=evento.getValue(); confirmar.setSelected(false); avisos.getItems().clear();
        if(organizador.getValue()==null) { evento.getItems().clear(); return; }
        try {
            evento.getItems().setAll(controller.listarEventos(organizador.getValue().id()));
            if(anterior!=null) evento.getItems().stream().filter(e->e.getIdEvento().equals(anterior.getIdEvento())).findFirst()
                    .ifPresent(e->evento.setValue(e));
        } catch(IllegalStateException e) { mostrar(e.getMessage(),false); }
    }

    private void cargarAvisos() {
        avisos.getItems().clear();
        if(evento.getValue()==null || organizador.getValue()==null) return;
        try { avisos.getItems().setAll(controller.listarAvisosCancelacion(evento.getValue().getIdEvento(),organizador.getValue().id())); }
        catch(IllegalStateException e) { mostrar(e.getMessage(),false); }
    }

    private void mostrar(String texto, boolean exito) {
        mensaje.setText(texto); mensaje.getStyleClass().setAll("message",exito?"success":"error");
        mensaje.setVisible(true); mensaje.setManaged(true);
    }

    private ListCell<Evento> celdaEvento() {
        return new ListCell<>() {
            @Override protected void updateItem(Evento item, boolean empty) {
                super.updateItem(item,empty);
                setText(item==null ? "Seleccionar evento" : item.getNombre()+" — "+item.getEstado());
            }
        };
    }
}

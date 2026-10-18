package views;

import controllers.ControllerEventos;
import entities.EdicionEvento;
import entities.Evento;
import entities.Organizador;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class ViewEdicion {
    private final ControllerEventos controller;
    private final Runnable volver;
    private final ComboBox<Organizador> organizador=new ComboBox<>();
    private final ComboBox<Evento> evento=new ComboBox<>();
    private final ComboBox<String> tipo=new ComboBox<>();
    private final TextField nombre=new TextField(), lugar=new TextField(), horaInicio=new TextField(), horaFin=new TextField();
    private final DatePicker inicio=new DatePicker(), fin=new DatePicker();
    private final VBox formulario=new VBox(18);
    private final Label mensaje=new Label();
    private Evento actual;

    public ViewEdicion(ControllerEventos controller,Runnable volver) { this.controller=controller; this.volver=volver; }

    public Parent crearPantalla() {
        organizador.setId("organizadorEdicion"); organizador.setPromptText("Seleccionar organizador");
        evento.setId("eventoEdicion"); evento.setPromptText("Seleccionar evento");
        evento.setCellFactory(lista->celdaEvento()); evento.setButtonCell(celdaEvento());
        nombre.setId("nombreEdicion"); tipo.setId("tipoEdicion"); lugar.setId("lugarEdicion");
        inicio.setId("inicioEdicion"); fin.setId("finEdicion"); inicio.setEditable(false); fin.setEditable(false);
        horaInicio.setId("horaInicioEdicion"); horaFin.setId("horaFinEdicion");
        horaInicio.setPromptText("HH:mm"); horaFin.setPromptText("HH:mm");
        tipo.getItems().addAll("Corporativo","Social","Cultural","Otro"); tipo.setPromptText("Seleccionar tipo");
        for(Control campo:new Control[]{organizador,evento,tipo,inicio,fin}) campo.setMaxWidth(Double.MAX_VALUE);
        mensaje.setId("mensajeEdicion"); mensaje.setWrapText(true); mensaje.setVisible(false); mensaje.setManaged(false);
        organizador.setOnAction(e->cargarEventos()); evento.setOnAction(e->precargar());
        GridPane fechas=new GridPane(); fechas.setHgap(20); fechas.setVgap(14);
        for(int i=0;i<2;i++) { ColumnConstraints c=new ColumnConstraints(); c.setPercentWidth(50); fechas.getColumnConstraints().add(c); }
        fechas.add(campo("Fecha de inicio",inicio),0,0); fechas.add(campo("Fecha de fin",fin),1,0);
        fechas.add(campo("Hora de inicio",horaInicio),0,1); fechas.add(campo("Hora de fin",horaFin),1,1);
        Button guardar=new Button("Guardar cambios"); guardar.setId("guardarEdicion"); guardar.getStyleClass().add("btn-primary"); guardar.setOnAction(e->guardar());
        Button restablecer=new Button("Restablecer"); restablecer.getStyleClass().add("btn-secondary"); restablecer.setOnAction(e->precargar());
        formulario.getChildren().addAll(campo("Nombre del evento",nombre),campo("Tipo de evento",tipo),fechas,campo("Lugar",lugar),new HBox(12,restablecer,guardar));
        formulario.getStyleClass().add("card"); formulario.setDisable(true);
        VBox seleccion=new VBox(14,campo("Organizador",organizador),campo("Evento",evento)); seleccion.getStyleClass().add("card");
        Button regresar=new Button("← Crear evento"); regresar.getStyleClass().add("btn-secondary"); regresar.setOnAction(e->volver.run());
        Label titulo=new Label("Editar evento"); titulo.getStyleClass().add("page-title");
        Label ayuda=new Label("Solo se pueden editar eventos en planeación. Presupuesto, invitados y organizador se conservarán."); ayuda.setWrapText(true); ayuda.getStyleClass().add("muted");
        Label demo=new Label("Selección de organizador para demostración. Inicio de sesión pendiente de integración."); demo.setWrapText(true); demo.getStyleClass().add("status");
        VBox contenido=new VBox(22,regresar,titulo,ayuda,demo,seleccion,mensaje,formulario); contenido.setMaxWidth(950); contenido.getStyleClass().add("content");
        StackPane centro=new StackPane(contenido); centro.setAlignment(Pos.TOP_CENTER);
        ScrollPane scroll=new ScrollPane(centro); scroll.setFitToWidth(true); scroll.getStyleClass().add("content-scroll");
        BorderPane raiz=new BorderPane(scroll); raiz.getStyleClass().add("app");
        try { organizador.getItems().setAll(controller.listarOrganizadores()); }
        catch(IllegalStateException e) { mostrar(e.getMessage(),false); }
        return raiz;
    }

    private void cargarEventos() {
        limpiar();
        if(organizador.getValue()==null) { evento.getItems().clear(); return; }
        try { evento.getItems().setAll(controller.listarEventos(organizador.getValue().id())); }
        catch(IllegalStateException e) { mostrar(e.getMessage(),false); }
    }

    private void precargar() {
        limpiar();
        if(evento.getValue()==null || organizador.getValue()==null) return;
        try {
            actual=controller.consultarEventoParaEditar(evento.getValue().getIdEvento(),organizador.getValue().id());
            nombre.setText(actual.getNombre()); tipo.setValue(actual.getTipo()); lugar.setText(actual.getUbicacion());
            inicio.setValue(actual.getFechaInicio().toLocalDate()); fin.setValue(actual.getFechaFin().toLocalDate());
            horaInicio.setText(actual.getFechaInicio().toLocalTime().toString()); horaFin.setText(actual.getFechaFin().toLocalTime().toString());
            formulario.setDisable(false); mensaje.setVisible(false); mensaje.setManaged(false);
        } catch(IllegalArgumentException | IllegalStateException e) { mostrar(e.getMessage(),false); }
    }

    private void guardar() {
        if(actual==null) return;
        int id=actual.getIdEvento();
        try {
            EdicionEvento datos=new EdicionEvento(nombre.getText().trim(),tipo.getValue(),fecha(inicio,horaInicio),fecha(fin,horaFin),lugar.getText().trim());
            controller.editarEvento(id,organizador.getValue().id(),datos);
        } catch(IllegalArgumentException | IllegalStateException e) { mostrar(e.getMessage(),false); return; }
        try {
            evento.getItems().setAll(controller.listarEventos(organizador.getValue().id()));
            evento.getItems().stream().filter(e->e.getIdEvento()==id).findFirst().ifPresent(e->evento.setValue(e));
            mostrar("Cambios guardados. El evento conserva su estado de planeación.",true);
        } catch(IllegalStateException e) { mostrar("Los cambios se guardaron, pero no se pudo actualizar la lista. Vuelve a abrir la pantalla.",false); }
    }

    private LocalDateTime fecha(DatePicker fecha,TextField hora) {
        if(fecha.getValue()==null) throw new IllegalArgumentException("Selecciona las fechas de inicio y fin.");
        try { return LocalDateTime.of(fecha.getValue(),LocalTime.parse(hora.getText().trim())); }
        catch(java.time.format.DateTimeParseException e) { throw new IllegalArgumentException("Ingresa una hora válida en formato HH:mm."); }
    }
    private void limpiar() {
        actual=null; formulario.setDisable(true); nombre.clear(); lugar.clear(); horaInicio.clear(); horaFin.clear();
        tipo.setValue(null); inicio.setValue(null); fin.setValue(null);
    }
    private void mostrar(String texto,boolean exito) {
        mensaje.setText(texto); mensaje.getStyleClass().setAll("message",exito?"success":"error"); mensaje.setVisible(true); mensaje.setManaged(true);
    }
    private VBox campo(String titulo,javafx.scene.Node control) {
        Label label=new Label(titulo); label.getStyleClass().add("field-label"); return new VBox(9,label,control);
    }
    private ListCell<Evento> celdaEvento() {
        return new ListCell<>() {
            @Override protected void updateItem(Evento item,boolean empty) {
                super.updateItem(item,empty); setText(item==null?"Seleccionar evento":item.getNombre()+" — "+item.getEstado());
            }
        };
    }
}

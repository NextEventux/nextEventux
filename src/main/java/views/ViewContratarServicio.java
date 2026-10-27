package views;

import controllers.ControllerContratacionServicio;
import entities.ConfirmacionContratacion;
import entities.DisponibilidadContratacion;
import entities.PresupuestoContratacion;
import entities.PropuestaServicio;
import entities.ResumenContratacionServicio;
import entities.SesionUsuario;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import views.components.MenuCompartido;

/**
 * CU-13: contratar el servicio de la propuesta seleccionada.
 *
 * Es una pantalla más de la aplicación, con el menú lateral y la barra
 * superior. Al abrirla consulta la versión exacta de la propuesta, la
 * disponibilidad del proveedor y el presupuesto del evento, y muestra el
 * resultado sin pedir pasos manuales. El botón "Confirmar contratación" solo
 * se habilita si todo es viable; contratar vuelve a validar dentro de su
 * transacción, así que cualquier cambio de último momento se informa aquí
 * mismo. Salir antes de confirmar no reserva nada (A3).
 */
public class ViewContratarServicio {

    private final ControllerContratacionServicio controller;
    private final int idOrganizador;
    private final PropuestaServicio propuesta;
    private final Runnable volverPropuestas;

    private SesionUsuario sesion;
    private Runnable abrirEventos;
    private Runnable abrirInvitados;
    private Runnable abrirCatalogo;
    private Runnable abrirMisEventos;
    private Runnable cerrarSesion;

    private final VBox contenido = new VBox(18);

    // Barra inferior fija: las acciones quedan visibles sin desplazarse
    private final VBox barraAcciones = new VBox(10);

    private ResumenContratacionServicio resumen;
    private DisponibilidadContratacion disponibilidad;
    private PresupuestoContratacion presupuesto;
    private String errorDisponibilidad;
    private String errorPresupuesto;

    public ViewContratarServicio(
            ControllerContratacionServicio controller,
            int idOrganizador,
            PropuestaServicio propuesta,
            Runnable volverPropuestas) {

        this.controller = controller;
        this.idOrganizador = idOrganizador;
        this.propuesta = propuesta;
        this.volverPropuestas = volverPropuestas;
    }

    public ViewContratarServicio conMenu(
            SesionUsuario sesion,
            Runnable abrirEventos,
            Runnable abrirInvitados,
            Runnable abrirCatalogo,
            Runnable abrirMisEventos,
            Runnable cerrarSesion) {

        this.sesion = sesion;
        this.abrirEventos = abrirEventos;
        this.abrirInvitados = abrirInvitados;
        this.abrirCatalogo = abrirCatalogo;
        this.abrirMisEventos = abrirMisEventos;
        this.cerrarSesion = cerrarSesion;
        return this;
    }

    public Parent crearPantalla() {

        BorderPane raiz = new BorderPane();
        raiz.getStyleClass().add("app");

        VBox menu = MenuCompartido.crear(
                sesion,
                MenuCompartido.Pagina.PROPUESTAS_ORGANIZADOR,
                abrirEventos,
                abrirInvitados,
                abrirCatalogo,
                null,
                cerrarSesion,
                abrirMisEventos,
                null,
                volverPropuestas);

        raiz.setLeft(menu);

        raiz.widthProperty().addListener((observable, anterior, actual) -> {
            boolean mostrarMenu = actual.doubleValue() >= 1000;
            menu.setVisible(mostrarMenu);
            menu.setManaged(mostrarMenu);
        });

        contenido.getStyleClass().addAll("content", "cu12-content");
        contenido.setMaxWidth(900);

        ScrollPane scroll = new ScrollPane(contenido);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("content-scroll");

        barraAcciones.getStyleClass().add("barra-acciones");

        BorderPane principal = new BorderPane(scroll);
        principal.setBottom(barraAcciones);
        principal.setTop(MenuCompartido.crearBarraSuperior(
                sesion,
                "Contratar servicio",
                "Propuestas recibidas",
                volverPropuestas));

        raiz.setCenter(principal);

        cargar();

        return raiz;
    }

    // ------------------------------------------------------------------
    // Carga automática: versión de la propuesta, disponibilidad y presupuesto
    // ------------------------------------------------------------------

    private void cargar() {

        try {
            resumen = controller.consultarResumen(
                    idOrganizador, propuesta.getIdPropuesta());

        } catch (RuntimeException e) {
            mostrarNoContratable(e.getMessage());
            return;
        }

        verificar();
        mostrarRevision(null);
    }

    private void verificar() {

        disponibilidad = null;
        presupuesto = null;
        errorDisponibilidad = null;
        errorPresupuesto = null;

        try {
            disponibilidad = controller.verificarDisponibilidad(
                    idOrganizador, resumen.idPropuesta());

        } catch (RuntimeException e) {
            errorDisponibilidad = e.getMessage();
        }

        try {
            presupuesto = controller.verificarPresupuesto(
                    idOrganizador, resumen.idPropuesta());

        } catch (RuntimeException e) {
            errorPresupuesto = e.getMessage();
        }
    }

    // ------------------------------------------------------------------
    // Pantalla de revisión
    // ------------------------------------------------------------------

    private void mostrarNoContratable(String mensaje) {

        contenido.getChildren().setAll(
                titulo("Contratar servicio"),
                banner(mensaje == null
                        ? "No se puede contratar esta propuesta."
                        : mensaje, "banner-error"));

        barraAcciones.getChildren().setAll(
                botonVolver("Volver a propuestas", "btn-secondary"));
    }

    private void mostrarRevision(String errorConfirmacion) {

        boolean puedeConfirmar = PresentacionContratacion.puedeConfirmar(
                disponibilidad, presupuesto);

        Button confirmar = new Button("Confirmar contratación");
        confirmar.setId("confirmarContratacion");
        confirmar.getStyleClass().add("btn-primary");
        confirmar.setDisable(!puedeConfirmar);
        confirmar.setOnAction(e -> confirmar(confirmar));

        Button volver = new Button("Volver a propuestas");
        volver.getStyleClass().add("btn-ghost");
        volver.setOnAction(e -> volverPropuestas.run());

        HBox acciones = new HBox(12, confirmar, volver);
        acciones.setAlignment(Pos.CENTER_LEFT);

        contenido.getChildren().setAll(
                titulo("Contratar servicio"),
                texto("Revisa el resumen. El horario del proveedor solo se "
                        + "reserva cuando confirmas la contratación."));

        contenido.getChildren().addAll(
                crearTarjetaPropuesta(),
                crearTarjetaVerificacion());

        barraAcciones.getChildren().clear();

        if (errorConfirmacion != null) {
            Node error = banner(errorConfirmacion, "banner-error");
            error.setId("errorContratacion");
            barraAcciones.getChildren().add(error);
        }

        String bloqueo = mensajeBloqueo();

        if (bloqueo != null) {
            Node aviso = banner(bloqueo, "banner-aviso");
            aviso.setId("motivoBloqueo");
            barraAcciones.getChildren().add(aviso);
        }

        barraAcciones.getChildren().add(acciones);
    }

    private String mensajeBloqueo() {

        if (errorDisponibilidad != null) {
            return errorDisponibilidad;
        }

        if (errorPresupuesto != null) {
            return errorPresupuesto;
        }

        return PresentacionContratacion.motivoBloqueo(
                disponibilidad, presupuesto);
    }

    private VBox crearTarjetaPropuesta() {

        Label servicio = new Label(resumen.nombreServicio());
        servicio.getStyleClass().add("titulo-seccion");

        Label precio = new Label(FormatoUi.moneda(resumen.valor()));
        precio.getStyleClass().add("dato-destacado");

        GridPane datos = new GridPane();
        datos.setHgap(32);
        datos.setVgap(12);

        datos.add(dato("Proveedor", resumen.nombreProveedor()), 0, 0);
        datos.add(dato("Evento", resumen.nombreEvento()), 1, 0);
        datos.add(dato("Fecha y hora", FormatoUi.rango(
                resumen.fechaInicioEvento(), resumen.fechaFinEvento())), 0, 1);
        datos.add(dato("Necesidad", resumen.necesidad()), 1, 1);
        datos.add(dato("Propuesta", "#" + resumen.idPropuesta()
                + " · versión " + resumen.numeroVersion()), 0, 2);

        VBox tarjeta = tarjeta(
                servicio,
                precio,
                datos,
                dato("Alcance", resumen.alcance()),
                dato("Condiciones acordadas", resumen.condiciones()));

        tarjeta.setId("tarjetaPropuesta");

        return tarjeta;
    }

    private VBox crearTarjetaVerificacion() {

        Label encabezado = new Label("Verificaciones");
        encabezado.getStyleClass().add("titulo-seccion");

        VBox tarjeta = tarjeta(encabezado, filaDisponibilidad());

        if (presupuesto != null) {
            GridPane cifras = new GridPane();
            cifras.setHgap(32);
            cifras.setVgap(12);

            cifras.add(dato("Precio acordado",
                    FormatoUi.moneda(presupuesto.valor())), 0, 0);
            cifras.add(dato("Presupuesto disponible del evento",
                    FormatoUi.moneda(PresentacionContratacion
                            .presupuestoDisponibleAntes(presupuesto))), 1, 0);
            cifras.add(dato("Presupuesto después de contratar",
                    FormatoUi.moneda(PresentacionContratacion
                            .presupuestoDisponibleDespues(presupuesto))), 2, 0);

            tarjeta.getChildren().add(cifras);
        }

        tarjeta.getChildren().add(filaPresupuesto());
        tarjeta.setId("tarjetaVerificacion");

        return tarjeta;
    }

    private Node filaDisponibilidad() {

        if (errorDisponibilidad != null) {
            return fila("Disponibilidad del proveedor",
                    errorDisponibilidad, "chip-error", "No verificada");
        }

        boolean disponible = disponibilidad != null
                && disponibilidad.disponible();

        return fila("Disponibilidad del proveedor",
                PresentacionContratacion.textoDisponibilidad(disponibilidad),
                disponible ? "chip-ok" : "chip-error",
                disponible ? "Disponible" : "No disponible");
    }

    private Node filaPresupuesto() {

        if (errorPresupuesto != null) {
            return fila("Presupuesto del evento",
                    errorPresupuesto, "chip-error", "No verificado");
        }

        boolean suficiente = presupuesto != null && presupuesto.suficiente();

        return fila("Presupuesto del evento",
                PresentacionContratacion.textoPresupuesto(presupuesto),
                suficiente ? "chip-ok" : "chip-error",
                suficiente ? "Suficiente" : "Insuficiente");
    }

    // ------------------------------------------------------------------
    // Confirmación
    // ------------------------------------------------------------------

    private void confirmar(Button boton) {

        boton.setDisable(true);

        try {
            ConfirmacionContratacion confirmacion = controller.contratar(
                    idOrganizador, resumen.idPropuesta());

            mostrarConfirmacion(confirmacion);

        } catch (RuntimeException e) {

            // A1 y A2: no se formalizó nada y la propuesta sigue seleccionada
            String motivo = e.getMessage() == null
                    ? "No se pudo contratar el servicio."
                    : e.getMessage();

            verificar();
            mostrarRevision(motivo);
        }
    }

    private void mostrarConfirmacion(ConfirmacionContratacion c) {

        Label servicio = new Label(c.nombreServicio());
        servicio.getStyleClass().add("titulo-seccion");

        Label precio = new Label(FormatoUi.moneda(c.valor()));
        precio.getStyleClass().add("dato-destacado");

        GridPane datos = new GridPane();
        datos.setHgap(32);
        datos.setVgap(12);

        datos.add(dato("Proveedor", c.nombreProveedor()), 0, 0);
        datos.add(dato("Evento", c.nombreEvento()), 1, 0);
        datos.add(dato("Horario reservado",
                FormatoUi.fecha(c.fechaServicio()) + ", "
                + FormatoUi.hora(c.horaInicio()) + " a "
                + FormatoUi.hora(c.horaFin())), 0, 1);
        datos.add(dato("Contratación",
                "#" + c.idContratacion() + " · propuesta #"
                + c.idPropuesta() + " versión " + c.numeroVersion()), 1, 1);
        datos.add(dato("Presupuesto disponible del evento",
                FormatoUi.moneda(c.presupuestoDisponible())), 0, 2);

        VBox resultado = tarjeta(
                servicio,
                precio,
                datos,
                dato("Alcance", c.alcance()),
                dato("Condiciones acordadas", c.condiciones()));

        resultado.setId("resultadoContratacion");

        Button ver = new Button("Ver propuestas");
        ver.setId("verPropuestas");
        ver.getStyleClass().add("btn-primary");
        ver.setOnAction(e -> volverPropuestas.run());

        HBox acciones = new HBox(ver);
        acciones.setAlignment(Pos.CENTER_LEFT);

        Node exito = banner("Contratación confirmada. El horario quedó "
                + "reservado y el proveedor fue notificado.", "banner-ok");
        exito.setId("bannerContratacion");

        contenido.getChildren().setAll(
                titulo("Contratación confirmada"),
                exito,
                resultado);

        barraAcciones.getChildren().setAll(acciones);
    }

    // ------------------------------------------------------------------
    // Componentes de presentación
    // ------------------------------------------------------------------

    private Label titulo(String texto) {

        Label etiqueta = new Label(texto);
        etiqueta.getStyleClass().add("titulo-pantalla");
        return etiqueta;
    }

    private Label texto(String texto) {

        Label etiqueta = new Label(texto);
        etiqueta.setWrapText(true);
        etiqueta.getStyleClass().add("texto-apoyo");
        return etiqueta;
    }

    private Label banner(String mensaje, String estilo) {

        Label etiqueta = new Label(mensaje);
        etiqueta.setWrapText(true);
        etiqueta.setMaxWidth(Double.MAX_VALUE);
        etiqueta.getStyleClass().addAll("banner", estilo);
        return etiqueta;
    }

    private VBox tarjeta(Node... nodos) {

        VBox tarjeta = new VBox(14, nodos);
        tarjeta.getStyleClass().add("tarjeta");
        tarjeta.setPadding(new Insets(20));
        return tarjeta;
    }

    private VBox dato(String etiqueta, String valor) {

        Label titulo = new Label(etiqueta);
        titulo.getStyleClass().add("dato-etiqueta");

        Label contenidoDato = new Label(
                valor == null || valor.isBlank() ? FormatoUi.SIN_DATO : valor);
        contenidoDato.setWrapText(true);
        contenidoDato.getStyleClass().add("dato-valor");

        return new VBox(2, titulo, contenidoDato);
    }

    private HBox fila(
            String etiqueta,
            String detalle,
            String estiloChip,
            String textoChip) {

        Label chip = new Label(textoChip);
        chip.getStyleClass().addAll("chip", estiloChip);

        VBox texto = dato(etiqueta, detalle);
        HBox.setHgrow(texto, Priority.ALWAYS);

        HBox fila = new HBox(14, texto, chip);
        fila.setAlignment(Pos.CENTER_LEFT);
        return fila;
    }

    private Button botonVolver(String texto, String estilo) {

        Button boton = new Button(texto);
        boton.getStyleClass().add(estilo);
        boton.setOnAction(e -> volverPropuestas.run());
        return boton;
    }
}

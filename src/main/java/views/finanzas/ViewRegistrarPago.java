package views.finanzas;

import controllers.ControllerRegistroPago;
import entities.SesionUsuario;
import entities.finanzas.SaldoContratacion;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import views.components.MenuCompartido;

/**
 * CU-10: registrar pago de una contratación.
 * La pantalla tiene una sola tarjeta que cambia según el paso: contrataciones (pasos 4 y 5),
 * formulario (6 a 8), resumen (9 y 10) y pago registrado (12).
 * Usa las clases del CSS compartido (card, info-card, field-label, btn-primary, message, etc.).
 */
public class ViewRegistrarPago {

    private final ControllerRegistroPago controller;
    private final SesionUsuario sesion;
    private final Runnable volverAFinanzas;
    private final Runnable abrirEventos;
    private final Runnable abrirMisEventos;
    private final Runnable abrirCatalogo;
    private final Runnable abrirInvitados;
    private final Runnable cerrarSesion;
    private Runnable abrirPropuestas;

    private final VBox tarjeta = new VBox(22);
    private int idEvento;

    public ViewRegistrarPago(
            ControllerRegistroPago controller,
            SesionUsuario sesion,
            Runnable volverAFinanzas,
            Runnable abrirEventos,
            Runnable abrirMisEventos,
            Runnable abrirCatalogo,
            Runnable abrirInvitados,
            Runnable cerrarSesion) {
        this.controller = controller;
        this.sesion = sesion;
        this.volverAFinanzas = volverAFinanzas;
        this.abrirEventos = abrirEventos;
        this.abrirMisEventos = abrirMisEventos;
        this.abrirCatalogo = abrirCatalogo;
        this.abrirInvitados = abrirInvitados;
        this.cerrarSesion = cerrarSesion;
    }

    public ViewRegistrarPago conPropuestas(Runnable accion) {
        this.abrirPropuestas = accion;
        return this;
    }

    public Parent crearPantalla(int idEvento) {
        this.idEvento = idEvento;

        BorderPane raiz = new BorderPane();
        raiz.getStyleClass().add("app");
        VBox sidebar = MenuCompartido.crear(sesion, MenuCompartido.Pagina.MIS_EVENTOS,
                abrirEventos, abrirInvitados, abrirCatalogo, null, cerrarSesion, abrirMisEventos,
                null, abrirPropuestas);
        raiz.setLeft(sidebar);

        BorderPane principal = new BorderPane();
        principal.setTop(MenuCompartido.crearBarraSuperior(
                sesion, "Registrar pago", "Estado financiero", volverAFinanzas));

        Label subtitulo = etiqueta(
                "Registra cada pago que haces a un proveedor, sin superar el valor acordado.", "muted");
        subtitulo.setWrapText(true);
        VBox encabezado = new VBox(8, etiqueta("Registrar pago", "page-title"), subtitulo);

        tarjeta.getStyleClass().add("card");
        VBox contenido = new VBox(30, encabezado, tarjeta);
        contenido.getStyleClass().add("content");
        contenido.setMaxWidth(1100);

        StackPane centrado = new StackPane(contenido);
        centrado.setAlignment(Pos.TOP_CENTER);
        ScrollPane scroll = new ScrollPane(centrado);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("content-scroll");
        principal.setCenter(scroll);
        raiz.setCenter(principal);

        // En ventanas pequeñas el menú deja espacio al contenido, igual que en CU06.
        raiz.widthProperty().addListener((obs, anterior, actual) -> {
            sidebar.setVisible(actual.doubleValue() >= 1000);
            sidebar.setManaged(sidebar.isVisible());
        });

        mostrarContrataciones();
        return raiz;
    }

    // ==========================================
    // Pasos 4 y 5: contrataciones del evento
    // ==========================================
    private void mostrarContrataciones() {
        tarjeta.getChildren().setAll(
                tituloTarjeta("Contrataciones del evento"),
                ayuda("Elige la contratación que vas a pagar. Las contrataciones canceladas no aparecen."));

        List<SaldoContratacion> contrataciones;
        try {
            contrataciones = controller.listarContratacionesPorPagar(idEvento);
        } catch (IllegalArgumentException | IllegalStateException e) {
            tarjeta.getChildren().addAll(mensaje(e.getMessage(), "error"), acciones(botonVolver()));
            return;
        }

        if (contrataciones.isEmpty()) {
            tarjeta.getChildren().add(etiqueta("Este evento no tiene contrataciones vigentes.", "hint"));
        }
        VBox lista = new VBox(14);
        for (SaldoContratacion contratacion : contrataciones) {
            lista.getChildren().add(crearFilaContratacion(contratacion));
        }
        tarjeta.getChildren().addAll(lista, acciones(botonVolver()));
    }

    private Node crearFilaContratacion(SaldoContratacion contratacion) {
        VBox nombre = new VBox(4,
                etiqueta(contratacion.getServicio(), "card-title"),
                etiqueta(proveedor(contratacion), "muted"));
        nombre.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(nombre, Priority.ALWAYS);

        String acordado = contratacion.tieneValor() ? dinero(contratacion.getValorAcordado()) : "—";
        String saldo = contratacion.tieneValor() ? dinero(contratacion.getSaldo()) : "—";
        HBox datos = new HBox(28,
                dato("Acordado", acordado),
                dato("Pagado", dinero(contratacion.getPagado())),
                dato("Saldo", saldo));
        datos.setAlignment(Pos.CENTER_LEFT);

        Node accion;
        if (contratacion.admitePagos()) {
            Button pagar = new Button("Pagar");
            pagar.setId("pagar" + contratacion.getIdContratacion());
            pagar.getStyleClass().add("btn-primary");
            pagar.setOnAction(e -> mostrarFormulario(contratacion));
            accion = pagar;
        } else {
            accion = etiqueta(contratacion.motivoSinPagos(), "event-row-status");
        }

        HBox fila = new HBox(24, nombre, datos, accion);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.getStyleClass().add("info-card");
        return fila;
    }

    // ==========================================
    // Paso 6: formulario de pago
    // ==========================================
    private void mostrarFormulario(SaldoContratacion contratacion) {
        tarjeta.getChildren().setAll(
                tituloTarjeta("Pago a " + contratacion.getServicio()),
                ayuda(proveedor(contratacion) + " · Saldo pendiente: " + dinero(contratacion.getSaldo())),
                acciones(botonCancelar()));
    }

    // ==========================================
    // Componentes
    // ==========================================
    private Button botonVolver() {
        Button volver = new Button("Volver al estado financiero");
        volver.setId("volverFinanzas");
        volver.getStyleClass().add("btn-secondary");
        volver.setOnAction(e -> {
            if (volverAFinanzas != null) {
                volverAFinanzas.run();
            }
        });
        return volver;
    }

    private Button botonCancelar() {
        Button cancelar = new Button("Cancelar");
        cancelar.setId("cancelarPago");
        cancelar.getStyleClass().add("btn-secondary");
        cancelar.setOnAction(e -> mostrarContrataciones());
        return cancelar;
    }

    private HBox tituloTarjeta(String texto) {
        HBox titulo = new HBox(13, etiqueta("$", "heading-icon"), etiqueta(texto, "card-title"));
        titulo.setAlignment(Pos.CENTER_LEFT);
        return titulo;
    }

    private Label ayuda(String texto) {
        Label ayuda = etiqueta(texto, "muted");
        ayuda.setWrapText(true);
        return ayuda;
    }

    private Label mensaje(String texto, String estilo) {
        Label mensaje = new Label(texto);
        mensaje.setId("mensajePago");
        mensaje.setWrapText(true);
        mensaje.getStyleClass().addAll("message", estilo);
        return mensaje;
    }

    private HBox acciones(Node... botones) {
        HBox acciones = new HBox(12, botones);
        acciones.setAlignment(Pos.CENTER_RIGHT);
        return acciones;
    }

    private VBox dato(String titulo, String valor) {
        return new VBox(4, etiqueta(titulo, "hint"), etiqueta(valor, "summary-value"));
    }

    private Label etiqueta(String texto, String estilo) {
        Label label = new Label(texto);
        label.getStyleClass().add(estilo);
        return label;
    }

    private static String proveedor(SaldoContratacion contratacion) {
        String nombre = contratacion.getProveedor();
        return nombre == null || nombre.isBlank() ? "Proveedor sin nombre" : nombre;
    }

    /** Formato de pesos colombianos: $ 1.800.000 (con centavos solo si los hay). */
    private static String dinero(BigDecimal valor) {
        NumberFormat formato = NumberFormat.getNumberInstance(new Locale("es", "CO"));
        formato.setMaximumFractionDigits(2);
        return "$ " + formato.format(valor == null ? BigDecimal.ZERO : valor);
    }
}

package views.finanzas;

import controllers.ControllerRegistroPago;
import entities.SesionUsuario;
import entities.finanzas.SaldoContratacion;
import entities.finanzas.SolicitudPago;
import entities.finanzas.TipoPago;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import services.ServiceRegistroPago;
import views.components.MenuCompartido;

/**
 * CU-10: registrar pago de una contratación.
 * La pantalla tiene una sola tarjeta que cambia según el paso: contrataciones (pasos 4 y 5),
 * formulario (6 a 8), resumen (9 y 10) y pago registrado (12).
 * Usa las clases del CSS compartido (card, info-card, field-label, btn-primary, message, etc.).
 */
public class ViewRegistrarPago {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    // Valores como 400000, 400.000, 1.250.000,50 o 400000.50.
    private static final Pattern VALOR_CON_MILES = Pattern.compile("\\d{1,3}(\\.\\d{3})+(,\\d{1,2})?");
    private static final Pattern VALOR_CON_COMA = Pattern.compile("\\d+(,\\d{1,2})?");
    private static final Pattern VALOR_CON_PUNTO = Pattern.compile("\\d+\\.\\d{1,2}");

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
        if (contratacion.tieneValor() && contratacion.getTotalProgramado().signum() > 0) {
            datos.getChildren().addAll(
                    dato("Programado", dinero(contratacion.getTotalProgramado())),
                    dato("Libre", dinero(contratacion.getSaldoLibre())));
        }
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
    // Pasos 6 a 8: formulario de pago
    // ==========================================
    private void mostrarFormulario(SaldoContratacion contratacion) {
        mostrarFormulario(contratacion, null);
    }

    /** previo trae los datos ya escritos cuando el organizador vuelve desde el resumen. */
    private void mostrarFormulario(SaldoContratacion contratacion, SolicitudPago previo) {
        ComboBox<TipoPago> tipo = new ComboBox<>();
        tipo.setId("tipoPago");
        tipo.getItems().setAll(TipoPago.values());
        tipo.setMaxWidth(Double.MAX_VALUE);
        // Sin pagos previos se sugiere Anticipo; con pagos, Abono.
        tipo.setValue(previo != null ? previo.getTipo()
                : contratacion.getPagado().signum() == 0 ? TipoPago.ANTICIPO : TipoPago.ABONO);

        TextField valor = new TextField(previo != null ? previo.getValor().toPlainString() : "");
        valor.setId("valorPago");
        valor.setPromptText("Ej: 400.000");

        DatePicker fecha = new DatePicker(previo != null ? previo.getFecha() : LocalDate.now());
        fecha.setId("fechaPago");
        fecha.setMaxWidth(Double.MAX_VALUE);

        TextField detalle = new TextField(previo != null ? previo.getDetalle() : "");
        detalle.setId("detallePago");
        detalle.setPromptText("Opcional, por ejemplo: segunda cuota");
        detalle.setTextFormatter(new TextFormatter<String>(cambio ->
                cambio.getControlNewText().length() <= ServiceRegistroPago.MAXIMO_DETALLE ? cambio : null));

        GridPane campos = new GridPane();
        campos.setHgap(24);
        campos.setVgap(18);
        campos.add(campo("Tipo de pago", tipo), 0, 0);
        campos.add(campo("Valor", valor), 1, 0);
        campos.add(campo("Fecha del pago", fecha), 0, 1);
        campos.add(campo("Descripción", detalle), 1, 1);
        dosColumnas(campos);

        // Si hay un pago programado que cabe en el saldo, se ofrece registrarlo en vez de crear otro.
        CheckBox programado = new CheckBox();
        programado.setId("usarPagoProgramado");
        boolean ofrecerProgramado = contratacion.tienePagoProgramado()
                && contratacion.getValorPagoProgramado().compareTo(contratacion.getSaldo()) <= 0;
        if (ofrecerProgramado) {
            programado.setText("Registrar el pago programado de "
                    + dinero(contratacion.getValorPagoProgramado()) + " (pendiente)");
            programado.selectedProperty().addListener((obs, antes, marcado) -> {
                valor.setDisable(marcado);
                if (marcado) {
                    valor.setText(contratacion.getValorPagoProgramado().toPlainString());
                }
            });
            boolean sinSaldoLibre = contratacion.getSaldoLibre().signum() == 0;
            programado.setSelected(previo != null ? previo.esPagoProgramado() : sinSaldoLibre);
        }

        Label error = mensaje("", "error");
        error.setVisible(false);
        error.setManaged(false);

        Button continuar = new Button("Continuar");
        continuar.setId("continuarPago");
        continuar.getStyleClass().add("btn-primary");
        continuar.setOnAction(e -> {
            try {
                SolicitudPago pago = controller.validarPago(idEvento, contratacion.getIdContratacion(),
                        tipo.getValue(), leerValor(valor.getText()), fecha.getValue(), detalle.getText(),
                        ofrecerProgramado && programado.isSelected());
                mostrarResumen(pago);
            } catch (IllegalArgumentException | IllegalStateException ex) {
                error.setText(ex.getMessage());
                error.setVisible(true);
                error.setManaged(true);
            }
        });

        tarjeta.getChildren().setAll(
                tituloTarjeta("Pago a " + contratacion.getServicio()),
                ayuda(resumenSaldo(contratacion)),
                campos);
        if (ofrecerProgramado) {
            tarjeta.getChildren().add(programado);
        }
        tarjeta.getChildren().addAll(error, acciones(botonCancelar(), continuar));
    }

    // ==========================================
    // Paso 9: resumen para confirmar
    // ==========================================
    private void mostrarResumen(SolicitudPago pago) {
        SaldoContratacion contratacion = pago.getContratacion();
        GridPane datos = new GridPane();
        datos.setHgap(40);
        datos.setVgap(16);
        datos.add(dato("Contratación", contratacion.getServicio()), 0, 0);
        datos.add(dato("Proveedor", proveedor(contratacion)), 1, 0);
        datos.add(dato("Tipo de pago", pago.getTipo().getEtiqueta()), 0, 1);
        datos.add(dato("Fecha del pago", pago.getFecha().format(FORMATO_FECHA)), 1, 1);
        datos.add(dato("Valor", dinero(pago.getValor())), 0, 2);
        datos.add(dato("Descripción", pago.getDescripcion()), 1, 2);
        datos.add(dato("Saldo actual", dinero(contratacion.getSaldo())), 0, 3);
        datos.add(dato("Saldo que quedará", dinero(pago.getSaldoRestante())), 1, 3);
        dosColumnas(datos);

        Button corregir = new Button("Corregir datos");
        corregir.setId("corregirPago");
        corregir.getStyleClass().add("btn-secondary");
        corregir.setOnAction(e -> mostrarFormulario(contratacion, pago));

        tarjeta.getChildren().setAll(
                tituloTarjeta("Revisa el pago"),
                ayuda("Confirma que los datos son correctos. El pago se guardará como registrado."),
                datos);
        if (pago.esPagoProgramado()) {
            tarjeta.getChildren().add(etiqueta(
                    "Se registrará el pago programado que estaba pendiente; no se crea un pago adicional.", "hint"));
        }
        Label error = mensaje("", "error");
        error.setVisible(false);
        error.setManaged(false);

        Button confirmar = new Button("Confirmar pago");
        confirmar.setId("confirmarPago");
        confirmar.getStyleClass().add("btn-primary");
        confirmar.setOnAction(e -> {
            // Evita un segundo clic mientras se guarda.
            confirmar.setDisable(true);
            try {
                SaldoContratacion actualizada = controller.confirmarPago(pago);
                mostrarPagoRegistrado(pago, actualizada);
            } catch (IllegalArgumentException | IllegalStateException ex) {
                // El resumen ya no es válido (por ejemplo, el saldo cambió): no se deja reintentar
                // con los mismos datos; se vuelve a la lista con los saldos actuales.
                error.setText(ex.getMessage());
                error.setVisible(true);
                error.setManaged(true);
                Button volverLista = new Button("Volver a las contrataciones");
                volverLista.setId("volverContrataciones");
                volverLista.getStyleClass().add("btn-primary");
                volverLista.setOnAction(ev -> mostrarContrataciones());
                ((HBox) confirmar.getParent()).getChildren().setAll(volverLista);
            }
        });

        tarjeta.getChildren().addAll(error, acciones(corregir, confirmar));
    }

    // ==========================================
    // Paso 12: pago registrado con el nuevo saldo
    // ==========================================
    private void mostrarPagoRegistrado(SolicitudPago pago, SaldoContratacion actualizada) {
        GridPane datos = new GridPane();
        datos.setHgap(40);
        datos.setVgap(16);
        datos.add(dato("Contratación", actualizada.getServicio()), 0, 0);
        datos.add(dato("Valor pagado", dinero(pago.getValor())), 1, 0);
        datos.add(dato("Total pagado", dinero(actualizada.getPagado())), 0, 1);
        datos.add(dato("Nuevo saldo pendiente", dinero(actualizada.getSaldo())), 1, 1);
        dosColumnas(datos);

        Button otro = new Button("Registrar otro pago");
        otro.setId("otroPago");
        otro.getStyleClass().add("btn-secondary");
        otro.setOnAction(e -> mostrarContrataciones());

        Button volver = botonVolver();
        volver.getStyleClass().setAll("button", "btn-primary");

        tarjeta.getChildren().setAll(
                tituloTarjeta("Pago registrado"),
                mensaje("Pago registrado. El estado financiero del evento ya refleja este pago.", "success"),
                datos,
                acciones(otro, volver));
    }

    /** Convierte el texto del valor a BigDecimal; null si está vacío (el servicio pide el valor). */
    private static BigDecimal leerValor(String texto) {
        String limpio = texto == null ? "" : texto.replace("$", "").replace(" ", "").trim();
        if (limpio.isEmpty()) {
            return null;
        }
        if (VALOR_CON_MILES.matcher(limpio).matches() || VALOR_CON_COMA.matcher(limpio).matches()) {
            return new BigDecimal(limpio.replace(".", "").replace(",", "."));
        }
        if (VALOR_CON_PUNTO.matcher(limpio).matches()) {
            return new BigDecimal(limpio);
        }
        throw new IllegalArgumentException("Escribe el valor solo con números, por ejemplo 400.000 o 400000.");
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

    /** Reparte la grilla en dos columnas del mismo ancho. */
    private static void dosColumnas(GridPane grilla) {
        for (int i = 0; i < 2; i++) {
            ColumnConstraints columna = new ColumnConstraints();
            columna.setPercentWidth(50);
            grilla.getColumnConstraints().add(columna);
        }
    }

    private VBox campo(String titulo, Node control) {
        VBox campo = new VBox(8, etiqueta(titulo, "field-label"), control);
        campo.setMaxWidth(Double.MAX_VALUE);
        return campo;
    }

    private VBox dato(String titulo, String valor) {
        return new VBox(4, etiqueta(titulo, "hint"), etiqueta(valor, "summary-value"));
    }

    private Label etiqueta(String texto, String estilo) {
        Label label = new Label(texto);
        label.getStyleClass().add(estilo);
        return label;
    }

    /** "Proveedor · Saldo pendiente: $ X", con lo programado y lo libre si hay pagos programados. */
    private static String resumenSaldo(SaldoContratacion contratacion) {
        String texto = proveedor(contratacion) + " · Saldo pendiente: " + dinero(contratacion.getSaldo());
        if (contratacion.getTotalProgramado().signum() > 0) {
            texto += " · Programado: " + dinero(contratacion.getTotalProgramado())
                    + " · Libre para pagos nuevos: " + dinero(contratacion.getSaldoLibre());
        }
        return texto;
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

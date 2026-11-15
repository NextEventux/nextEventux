import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import controllers.ControllerMarketing;
import entities.Campana;
import entities.SesionUsuario;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import repositories.RepositoryMarketing;
import services.ServiceMarketing;
import views.FormatoUi;
import views.ViewCampana;

class ViewCampanaTest {

    private static boolean toolkitDisponible;

    @BeforeAll
    static void iniciarToolkit() {
        try {
            Platform.startup(() -> { });
            toolkitDisponible = true;
        } catch (IllegalStateException yaIniciado) {
            toolkitDisponible = true;
        } catch (Throwable sinPantalla) {
            toolkitDisponible = false;
        }
    }

    private static <T> T enHiloFx(Callable<T> tarea) throws Exception {
        CompletableFuture<T> resultado = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                resultado.complete(tarea.call());
            } catch (Throwable t) {
                resultado.completeExceptionally(t);
            }
        });
        return resultado.get(15, TimeUnit.SECONDS);
    }

    // ---------- Sin pantalla ----------

    @Test
    void elResumenIncluyeConfiguracionAlcanceYConversion() {
        Campana campana = new Campana(7, 1, 2, "Bodas 2027", "Descuento del 10 %",
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 31), new BigDecimal("300000.00"),
                Campana.ESTADO_FINALIZADA, 480L, 12L, "Bogotá - Boda");

        String texto = ViewCampana.construirTextoResumen(campana);

        assertTrue(texto.contains("Campaña: Bodas 2027"));
        assertTrue(texto.contains("Segmento: Bogotá - Boda"));
        assertTrue(texto.contains("Oferta: Descuento del 10 %"));
        assertTrue(texto.contains("Vigencia: " + FormatoUi.fecha(LocalDate.of(2026, 12, 1))
                + " a " + FormatoUi.fecha(LocalDate.of(2026, 12, 31))));
        assertTrue(texto.contains("Presupuesto asignado: " + FormatoUi.moneda(new BigDecimal("300000.00"))));
        assertTrue(texto.contains("Alcance (visualizaciones): 480"));
        assertTrue(texto.contains("Conversión (contrataciones): 12"));
    }

    @Test
    void elResumenToleraUnSegmentoSinNombre() {
        Campana campana = new Campana(7, 1, 2, "N", "O", LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 2), BigDecimal.ONE, Campana.ESTADO_FINALIZADA, 0L, 0L, null);
        assertTrue(ViewCampana.construirTextoResumen(campana).contains("Segmento: " + FormatoUi.SIN_DATO));
    }

    // ---------- Con el toolkit de JavaFX (se omiten si no hay pantalla) ----------

    private Parent crearPantalla(String url) throws Exception {
        SesionUsuario sesion = new SesionUsuario();
        sesion.setIdUsuario(DatosCampanaPrueba.ADMINISTRADOR);
        sesion.setNombre("Administradora");
        sesion.setTipoUsuario("ADMINISTRADOR");
        ControllerMarketing controlador = new ControllerMarketing(
                new ServiceMarketing(new RepositoryMarketing(url)));
        return enHiloFx(() -> {
            Parent raiz = new ViewCampana(controlador, sesion, null, null, null, null).crearPantalla();
            // Sin escena, el ScrollPane no construye su contenido y los campos no se pueden consultar.
            new Scene(raiz, 1200, 800);
            raiz.applyCss();
            raiz.layout();
            return raiz;
        });
    }

    private void llenarFormulario(Parent raiz, LocalDate inicio, LocalDate fin, String monto) throws Exception {
        enHiloFx(() -> {
            @SuppressWarnings("unchecked")
            ComboBox<Object> segmento = (ComboBox<Object>) raiz.lookup("#segmento");
            segmento.getSelectionModel().select(0);
            ((TextField) raiz.lookup("#nombre")).setText("Bodas de fin de año");
            ((TextField) raiz.lookup("#tipoOferta")).setText("Descuento del 10 %");
            ((DatePicker) raiz.lookup("#fechaInicio")).setValue(inicio);
            ((DatePicker) raiz.lookup("#fechaFin")).setValue(fin);
            ((TextField) raiz.lookup("#presupuesto")).setText(monto);
            return null;
        });
    }

    @Test
    void laPantallaCargaLosSegmentosActivosConSuCantidadDeUsuarios() throws Exception {
        Assumptions.assumeTrue(toolkitDisponible, "Sin entorno gráfico: se omite");
        Parent raiz = crearPantalla(DatosCampanaPrueba.crearBase());

        assertEquals(1, ((ComboBox<?>) raiz.lookup("#segmento")).getItems().size());
        enHiloFx(() -> {
            ((ComboBox<?>) raiz.lookup("#segmento")).getSelectionModel().select(0);
            return null;
        });
        assertEquals("Usuarios estimados: 3", enHiloFx(() -> ((Label) raiz.lookup("#usuariosEstimados")).getText()));
        assertNotNull(raiz.lookup("#tablaCampanas"));
    }

    @Test
    void crearUnaCampanaValidaLaProgramaYLaMuestraEnLaTabla() throws Exception {
        Assumptions.assumeTrue(toolkitDisponible, "Sin entorno gráfico: se omite");
        Parent raiz = crearPantalla(DatosCampanaPrueba.crearBase());
        llenarFormulario(raiz, DatosCampanaPrueba.INICIO, DatosCampanaPrueba.FIN, "300000");

        enHiloFx(() -> { ((Button) raiz.lookup("#crear")).fire(); return null; });

        String mensaje = enHiloFx(() -> ((Label) raiz.lookup("#mensaje")).getText());
        assertTrue(mensaje.contains("programada"), mensaje);
        assertEquals(1, enHiloFx(() -> ((TableView<?>) raiz.lookup("#tablaCampanas")).getItems().size()));
        assertEquals("", enHiloFx(() -> ((TextField) raiz.lookup("#nombre")).getText()));
    }

    @Test
    void lasExcepcionesE1YE2SeMuestranEnElFormularioSinPerderLoEscrito() throws Exception {
        Assumptions.assumeTrue(toolkitDisponible, "Sin entorno gráfico: se omite");
        Parent raiz = crearPantalla(DatosCampanaPrueba.crearBase());

        llenarFormulario(raiz, DatosCampanaPrueba.INICIO, DatosCampanaPrueba.INICIO, "300000");
        enHiloFx(() -> { ((Button) raiz.lookup("#crear")).fire(); return null; });
        assertTrue(enHiloFx(() -> ((Label) raiz.lookup("#mensaje")).getText()).contains("posterior"));

        llenarFormulario(raiz, DatosCampanaPrueba.INICIO, DatosCampanaPrueba.FIN, "5000000");
        enHiloFx(() -> { ((Button) raiz.lookup("#crear")).fire(); return null; });
        assertTrue(enHiloFx(() -> ((Label) raiz.lookup("#mensaje")).getText())
                .contains("Monto máximo restante: 1000000.00"));

        llenarFormulario(raiz, DatosCampanaPrueba.INICIO, DatosCampanaPrueba.FIN, "mucho");
        enHiloFx(() -> { ((Button) raiz.lookup("#crear")).fire(); return null; });
        assertTrue(enHiloFx(() -> ((Label) raiz.lookup("#mensaje")).getText()).contains("numérico"));

        assertEquals("Bodas de fin de año", enHiloFx(() -> ((TextField) raiz.lookup("#nombre")).getText()));
        assertEquals(0, enHiloFx(() -> ((TableView<?>) raiz.lookup("#tablaCampanas")).getItems().size()));
    }
}

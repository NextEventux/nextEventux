import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import controllers.ControllerMarketing;
import entities.Campana;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repositories.RepositoryMarketing;
import services.ServiceMarketing;

class ControllerMarketingCampanaTest {

    private ServiceMarketing servicio;
    private ControllerMarketing controlador;

    @BeforeEach
    void preparar() throws Exception {
        String url = DatosCampanaPrueba.crearBase();
        servicio = new ServiceMarketing(new RepositoryMarketing(url));
        controlador = new ControllerMarketing(servicio);
    }

    @Test
    void crearCampanaLaProgramaYDevuelveLaCampanaGuardada() {
        Campana creada = controlador.crearCampana(DatosCampanaPrueba.campanaValida());

        List<Campana> campanas = controlador.listarCampanas();
        assertEquals(1, campanas.size());
        assertEquals(creada.getIdCampana(), campanas.get(0).getIdCampana());
        assertEquals(Campana.ESTADO_PROGRAMADA, campanas.get(0).getEstado());
    }

    @Test
    void crearCampanaPropagaLosErroresDeValidacion() {
        Campana excede = DatosCampanaPrueba.campana(1, "9999999", DatosCampanaPrueba.INICIO,
                DatosCampanaPrueba.FIN);
        assertThrows(IllegalArgumentException.class, () -> controlador.crearCampana(excede));
    }

    @Test
    void pausarYReanudarDelegaEnElServicio() {
        int id = controlador.crearCampana(DatosCampanaPrueba.campanaValida()).getIdCampana();
        servicio.lanzarCampana(id);

        controlador.pausarCampana(DatosCampanaPrueba.ADMINISTRADOR, id);
        assertEquals(Campana.ESTADO_PAUSADA, controlador.listarCampanas().get(0).getEstado());

        controlador.reanudarCampana(DatosCampanaPrueba.ADMINISTRADOR, id);
        assertEquals(Campana.ESTADO_ACTIVA, controlador.listarCampanas().get(0).getEstado());
    }
}

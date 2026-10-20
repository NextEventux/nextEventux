package services;

import entities.ConfirmacionContratacion;
import entities.DisponibilidadContratacion;
import entities.PresupuestoContratacion;
import entities.ResumenContratacionServicio;
import entities.SesionUsuario;
import repositories.RepositoryContratacionServicio;

/**
 * CU-13: contratar un servicio a partir de la propuesta seleccionada en CU-12.
 * Solo el organizador dueño del evento puede revisar y formalizar la contratación.
 */
public class ServiceContratacionServicio {

    private final RepositoryContratacionServicio repository;
    private final SesionUsuario sesion;

    public ServiceContratacionServicio(
            RepositoryContratacionServicio repository,
            SesionUsuario sesion) {

        if (repository == null) {
            throw new IllegalArgumentException(
                    "Se requiere el repositorio de contrataciones.");
        }

        if (sesion == null) {
            throw new IllegalArgumentException(
                    "Se requiere una sesion para habilitar CU13.");
        }

        this.repository = repository;
        this.sesion = sesion;
    }

    public ResumenContratacionServicio consultarResumen(
            int idOrganizador,
            int idPropuesta) {

        validar(idOrganizador, idPropuesta);
        return repository.consultarResumen(idOrganizador, idPropuesta);
    }

    public DisponibilidadContratacion verificarDisponibilidad(
            int idOrganizador,
            int idPropuesta) {

        validar(idOrganizador, idPropuesta);
        return repository.verificarDisponibilidad(idOrganizador, idPropuesta);
    }

    public PresupuestoContratacion verificarPresupuesto(
            int idOrganizador,
            int idPropuesta) {

        validar(idOrganizador, idPropuesta);
        return repository.verificarPresupuesto(idOrganizador, idPropuesta);
    }

    public ConfirmacionContratacion contratar(
            int idOrganizador,
            int idPropuesta) {

        validar(idOrganizador, idPropuesta);
        return repository.contratar(idOrganizador, idPropuesta);
    }

    private void validar(int idOrganizador, int idPropuesta) {

        if (idOrganizador <= 0 || idPropuesta <= 0) {
            throw new IllegalArgumentException(
                    "Los identificadores no son validos.");
        }

        if (sesion.getIdUsuario() <= 0
                || sesion.getIdActor() <= 0
                || !"ACTIVO".equalsIgnoreCase(sesion.getEstadoUsuario())) {

            throw new IllegalStateException(
                    "Se requiere una sesion activa para esta operacion.");
        }

        if (!"ORGANIZADOR".equalsIgnoreCase(sesion.getTipoUsuario())) {
            throw new IllegalStateException(
                    "Tu rol no permite realizar esta operacion.");
        }

        if (!"ACTIVO".equalsIgnoreCase(sesion.getEstadoActor())) {
            throw new IllegalStateException(
                    "La cuenta no tiene permisos activos para esta operacion.");
        }

        if (sesion.getIdActor() != idOrganizador) {
            throw new IllegalStateException(
                    "La operacion no pertenece a tu cuenta.");
        }
    }
}

package services;

import java.util.Optional;

import entities.SesionUsuario;
import repositories.RepositoryAutenticacion;

public class ServiceAutenticacion {

    /** CU-029: mensaje para una cuenta suspendida por un administrador. */
    public static final String MENSAJE_CUENTA_SUSPENDIDA =
            "Tu cuenta se encuentra suspendida. Comunícate con el administrador "
            + "para obtener más información.";

    /** Mensaje para una sesion cuya cuenta ya no existe o no esta activa. */
    public static final String MENSAJE_SESION_INVALIDA =
            "Tu sesión ya no es válida. Vuelve a iniciar sesión.";

    private final RepositoryAutenticacion repository;

    public ServiceAutenticacion(
            RepositoryAutenticacion repository) {

        this.repository = repository;
    }

    public SesionUsuario iniciarSesion(
            String usuario,
            String contrasena) {

        if (usuario == null
                || usuario.isBlank()
                || contrasena == null
                || contrasena.isBlank()) {

            throw new IllegalArgumentException(
                    "Ingresa usuario y contrasena.");
        }

        SesionUsuario sesion
                = repository.autenticar(
                        usuario.trim(),
                        contrasena);

        if (sesion == null) {
            throw new IllegalArgumentException(
                    "Usuario o contrasena incorrectos.");
        }

        // CU-029: la cuenta suspendida se informa solo despues de validar
        // las credenciales, para no revelar el estado a quien no las conoce.
        if ("SUSPENDIDO".equalsIgnoreCase(
                sesion.getEstadoUsuario())) {

            throw new IllegalStateException(
                    MENSAJE_CUENTA_SUSPENDIDA);
        }

        if (!"ACTIVO".equalsIgnoreCase(
                sesion.getEstadoUsuario())) {

            throw new IllegalStateException(
                    "El usuario no esta activo.");
        }

        if ("PROVEEDOR".equalsIgnoreCase(
                sesion.getTipoUsuario())) {

            if (sesion.getIdActor() == 0
                    || !("APROBADO".equalsIgnoreCase(sesion.getEstadoActor())
                         || "VERIFICADO".equalsIgnoreCase(sesion.getEstadoActor()))) {

                throw new IllegalStateException(
                        "El proveedor aun no esta aprobado.");
            }

            return sesion;
        }

        if ("ORGANIZADOR".equalsIgnoreCase(
                sesion.getTipoUsuario())) {

            if (sesion.getIdActor() == 0
                    || !"ACTIVO".equalsIgnoreCase(
                            sesion.getEstadoActor())) {

                throw new IllegalStateException(
                        "El organizador no esta activo.");
            }

            return sesion;
        }
        if ("CLIENTE".equalsIgnoreCase(
        sesion.getTipoUsuario())) {

        return sesion;
        }
        
        if ("ADMINISTRADOR".equalsIgnoreCase(
                sesion.getTipoUsuario())) {

            return sesion;
        }

        throw new IllegalStateException(
                "Este usuario no tiene acceso a estas funciones.");
    }

    /**
     * CU-029: comprueba en la base de datos que la cuenta de una sesion
     * abierta sigue activa.
     *
     * @return vacio si la sesion sigue vigente, o el mensaje que debe verse
     *         al cerrarla
     * @throws IllegalStateException si no se pudo consultar la base de datos;
     *         un fallo tecnico no debe confundirse con un rechazo
     */
    public Optional<String> verificarSesionVigente(SesionUsuario sesion) {

        if (sesion == null || sesion.getIdUsuario() <= 0) {
            return Optional.of(MENSAJE_SESION_INVALIDA);
        }

        Optional<String> estado =
                repository.consultarEstadoCuenta(sesion.getIdUsuario());

        if (estado.isEmpty()) {
            return Optional.of(MENSAJE_SESION_INVALIDA);
        }
        if ("SUSPENDIDO".equalsIgnoreCase(estado.get().trim())) {
            return Optional.of(MENSAJE_CUENTA_SUSPENDIDA);
        }
        if (!"ACTIVO".equalsIgnoreCase(estado.get().trim())) {
            return Optional.of(MENSAJE_SESION_INVALIDA);
        }
        return Optional.empty();
    }
}

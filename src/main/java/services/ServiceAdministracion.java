package services;

import entities.Proveedor;
import repositories.RepositoryProveedor;

import java.util.List;
import java.util.regex.Pattern;

/** Lógica de negocio implementada para la primera iteración del CU-028. */
public class ServiceAdministracion {
    private static final Pattern CORREO = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private final RepositoryProveedor repositoryProveedor;

    public ServiceAdministracion(RepositoryProveedor repositoryProveedor) {
        if (repositoryProveedor == null) {
            throw new IllegalArgumentException("El repositorio de proveedores es obligatorio.");
        }
        this.repositoryProveedor = repositoryProveedor;
    }

    public List<Proveedor> consultarProveedoresPendientes() {
        return repositoryProveedor.listarProveedoresPendientes();
    }

    /**
     * Verifica los datos disponibles en la tabla Proveedor.
     * La revisión de documentos adjuntos queda pendiente porque el esquema actual no modela esos documentos.
     */
    public boolean verificarInformacionProveedor(int idProveedor) {
        Proveedor proveedor = obtenerProveedorPendiente(idProveedor);
        if (proveedor == null) {
            return false;
        }
        if (proveedor.getNombre() == null || proveedor.getNombre().isBlank()) {
            return false;
        }
        String correo = proveedor.getCorreo();
        return correo != null && !correo.isBlank() && CORREO.matcher(correo.trim()).matches();  
    }

    public void aprobarProveedor(int idProveedor) {
        Proveedor proveedor = obtenerProveedorPendiente(idProveedor);
        if (proveedor == null) {
            throw new IllegalArgumentException("Selecciona un proveedor pendiente de verificación.");
        }
        if (!verificarInformacionProveedor(idProveedor)) {
            throw new IllegalArgumentException(
                    "La información disponible del proveedor está incompleta o tiene un correo inválido. No se aprobó.");
        }
        repositoryProveedor.actualizarEstadoProveedor(idProveedor, Proveedor.ESTADO_VERIFICADO);
    }

    public void rechazarProveedor(int idProveedor, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("Escribe el motivo del rechazo.");
        }
        if (motivo.trim().length() > 500) {
            throw new IllegalArgumentException("El motivo del rechazo debe tener máximo 500 caracteres.");
        }
        obtenerProveedorPendiente(idProveedor);
        repositoryProveedor.rechazarProveedor(idProveedor, motivo.trim());
    }

    private Proveedor obtenerProveedorPendiente(int idProveedor) {
        if (idProveedor <= 0) {
            throw new IllegalArgumentException("Selecciona un proveedor válido.");
        }
        Proveedor proveedor = repositoryProveedor.buscarProveedor(idProveedor);
        if (proveedor == null || !esEstadoPendiente(proveedor.getEstado())) {
            return null;
        }
        return proveedor;
    }

    private boolean esEstadoPendiente(String estado) {
        if (estado == null) return false;
        String normalizado = estado.trim().toUpperCase().replace(' ', '_').replace('-', '_')
                .replace('Á', 'A').replace('É', 'E').replace('Í', 'I').replace('Ó', 'O').replace('Ú', 'U');
        return normalizado.equals("PENDIENTE")
                || normalizado.equals("PENDIENTE_VERIFICACION")
                || normalizado.equals("PENDIENTE_DE_VERIFICACION")
                || normalizado.equals("PENDIENTE_REVISION")
                || normalizado.equals("PENDIENTE_DE_REVISION");
    }
}


package services;

import entities.ClienteMarketing;
import entities.SegmentoCalculado;
import entities.SolicitudEvento;
import entities.Segmentacion;
import repositories.RepositoryMarketing;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ServiceMarketing {

    private final RepositoryMarketing repositorio;

    public ServiceMarketing(RepositoryMarketing repositorio) {
        this.repositorio = repositorio;
    }

    // Cada grupo reúne clientes de una misma ubicación que comparten un tipo de evento
    private record GrupoClientes(String pais, String ciudad, String tipoEvento,
                                 List<Integer> idsClientes) {

        boolean coincideCon(String otroPais, String otraCiudad, String otroTipo) {
            return pais.equals(otroPais)
                    && ciudad.equals(otraCiudad)
                    && tipoEvento.equals(otroTipo);
        }
    }

    public List<Segmentacion> listarSegmentosActivos() {
        return repositorio.listarSegmentosActivos();
    }

    public List<Segmentacion> segmentarClientes(int idAdministrador) {
        if (!repositorio.existeAdministradorActivo(idAdministrador)) {
            throw new IllegalArgumentException("Solo un administrador activo puede segmentar clientes.");
        }

        List<SegmentoCalculado> calculados = calcularSegmentos(
                repositorio.listarClientesActivos(),
                repositorio.contarSolicitudesPorTipo());

        List<Segmentacion> segmentos = new ArrayList<>();
        for (SegmentoCalculado calculado : calculados) {
            segmentos.add(convertirEnSegmentacion(idAdministrador, calculado));
        }

        // Incluso si no quedan grupos válidos, se desactivan los automáticos anteriores
        return repositorio.reemplazarSegmentosAutomaticos(segmentos);
    }

    private Segmentacion convertirEnSegmentacion(int idAdministrador,
                                                 SegmentoCalculado calculado) {
        String nombre = calculado.getCiudad() + " - " + calculado.getTipoEvento();

        String criterios = Segmentacion.PREFIJO_CRITERIOS_AUTOMATICOS
                + "pais=" + calculado.getPais()
                + "|ciudad=" + calculado.getCiudad()
                + "|tipo=" + calculado.getTipoEvento();

        String descripcion = "Clientes de " + calculado.getCiudad()
                + " (" + calculado.getPais() + ") interesados en eventos de tipo "
                + calculado.getTipoEvento();

        return new Segmentacion(
                idAdministrador,
                nombre,
                criterios,
                descripcion,
                Segmentacion.ESTADO_ACTIVA,
                calculado.getIdsClientes());
    }

    public List<SegmentoCalculado> calcularSegmentos(
            List<ClienteMarketing> clientes,
            List<SolicitudEvento> solicitudes) {

        if (clientes == null || solicitudes == null) {
            throw new IllegalArgumentException(
                    "Se requieren los clientes y las preferencias de eventos.");
        }

        List<GrupoClientes> grupos = new ArrayList<>();

        for (ClienteMarketing cliente : clientes) {
            if (cliente == null || !hayTexto(cliente.getCiudad())) {
                continue;
            }

            String pais = hayTexto(cliente.getPais())
                    ? cliente.getPais().trim()
                    : "No especificado";

            String ciudad = cliente.getCiudad().trim();

            // Revisamos las preferencias de cada cliente, no las de todo el grupo
            for (SolicitudEvento solicitud : solicitudes) {
                if (solicitud == null
                        || solicitud.getIdCliente() != cliente.getIdUsuario()
                        || !hayTexto(solicitud.getTipo())
                        || solicitud.getCantidad() <= 0) {
                    continue;
                }

                String tipo = solicitud.getTipo().trim();
                GrupoClientes grupo = buscarGrupo(grupos, pais, ciudad, tipo);

                if (grupo == null) {
                    grupo = new GrupoClientes(
                            pais, ciudad, tipo, new ArrayList<>());
                    grupos.add(grupo);
                }

                // Una persona puede tener varias preferencias, pero no se repite en un grupo
                if (!grupo.idsClientes().contains(cliente.getIdUsuario())) {
                    grupo.idsClientes().add(cliente.getIdUsuario());
                }
            }
        }

        List<SegmentoCalculado> segmentos = new ArrayList<>();

        for (GrupoClientes grupo : grupos) {
            segmentos.add(new SegmentoCalculado(
                    grupo.pais(),
                    grupo.ciudad(),
                    grupo.tipoEvento(),
                    grupo.idsClientes()));
        }

        segmentos.sort(Comparator.comparing(SegmentoCalculado::getPais)
                .thenComparing(SegmentoCalculado::getCiudad)
                .thenComparing(SegmentoCalculado::getTipoEvento));

        return segmentos;
    }

    // Busca un grupo existente con la misma ubicación y preferencia
    private GrupoClientes buscarGrupo(List<GrupoClientes> grupos, String pais,
                                      String ciudad, String tipo) {
        for (GrupoClientes grupo : grupos) {
            if (grupo.coincideCon(pais, ciudad, tipo)) {
                return grupo;
            }
        }
        return null;
    }

    private static boolean hayTexto(String texto) {
        return texto != null && !texto.isBlank();
    }
}

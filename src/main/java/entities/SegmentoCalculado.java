
package entities;

import java.util.List;

// Resultado de la segmentación, clientes que comparten ubicación y preferencia
public class SegmentoCalculado {
    private final String pais;
    private final String ciudad;
    private final String tipoEvento;
    private final List<Integer> idsClientes;

    public SegmentoCalculado(String pais, String ciudad,
                             String tipoEvento, List<Integer> idsClientes) {
        this.pais = pais;
        this.ciudad = ciudad;
        this.tipoEvento = tipoEvento;
        this.idsClientes = List.copyOf(idsClientes);
    }

    public String getPais() {
        return pais;
    }

    public String getCiudad() {
        return ciudad;
    }

    public String getTipoEvento() {
        return tipoEvento;
    }

    public List<Integer> getIdsClientes() {
        return idsClientes;
    }

    public int getCantidadClientes() {
        return idsClientes.size();
    }
}

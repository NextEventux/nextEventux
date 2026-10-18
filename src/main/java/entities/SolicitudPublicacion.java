package entities;

public record SolicitudPublicacion(int id, int idEvento, String nombreEvento, String estado, String motivo) {
    @Override public String toString() { return nombreEvento + " — " + estado; }
}

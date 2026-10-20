package entities;

/** Opción del formulario: el identificador se utiliza internamente. */
public record Organizador(int id, String nombre) {
    @Override
    public String toString() {
        return nombre;
    }
}

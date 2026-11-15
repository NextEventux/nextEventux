package persistencia;

public final class PropiedadSistemaTemporal {

    private final String nombrePropiedad;
    private final String valorAnterior;
    private boolean restaurada;

    public PropiedadSistemaTemporal(String nombrePropiedad, String valorTemporal) {
        this.nombrePropiedad = nombrePropiedad;
        this.valorAnterior = System.getProperty(nombrePropiedad);
        System.setProperty(nombrePropiedad, valorTemporal);
    }

    public void restaurar() {
        if (restaurada) {
            return;
        }
        if (valorAnterior == null) {
            System.clearProperty(nombrePropiedad);
        } else {
            System.setProperty(nombrePropiedad, valorAnterior);
        }
        restaurada = true;
    }
}

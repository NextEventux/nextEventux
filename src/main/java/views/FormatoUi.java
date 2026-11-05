package views;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Formatos de moneda y fecha que se muestran al usuario.
 *
 * Los valores se presentan en pesos colombianos sin decimales ($1.500.000) y
 * las fechas con el mes abreviado en español (20 nov. 2026). El formato no
 * depende de la configuración regional del equipo para que la interfaz se
 * vea igual en todos los computadores.
 */
public final class FormatoUi {

    public static final String SIN_DATO = "—";

    private static final String[] MESES = {
        "ene.", "feb.", "mar.", "abr.", "may.", "jun.",
        "jul.", "ago.", "sep.", "oct.", "nov.", "dic."
    };

    private static final DateTimeFormatter HORA
            = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);

    private FormatoUi() {
    }

    public static String moneda(BigDecimal valor) {

        if (valor == null) {
            return SIN_DATO;
        }

        DecimalFormatSymbols simbolos = new DecimalFormatSymbols(Locale.ROOT);
        simbolos.setGroupingSeparator('.');
        simbolos.setDecimalSeparator(',');

        DecimalFormat formato = new DecimalFormat("#,##0", simbolos);
        formato.setRoundingMode(RoundingMode.HALF_UP);

        String texto = formato.format(valor.abs());

        return (valor.signum() < 0 ? "-$" : "$") + texto;
    }

    public static String fecha(LocalDate fecha) {

        if (fecha == null) {
            return SIN_DATO;
        }

        return fecha.getDayOfMonth() + " "
                + MESES[fecha.getMonthValue() - 1] + " "
                + fecha.getYear();
    }

    public static String hora(LocalTime hora) {
        return hora == null ? SIN_DATO : hora.format(HORA);
    }

    public static String fechaHora(LocalDateTime fechaHora) {

        if (fechaHora == null) {
            return SIN_DATO;
        }

        return fecha(fechaHora.toLocalDate())
                + ", " + hora(fechaHora.toLocalTime());
    }

    /**
     * Rango de un evento: si empieza y termina el mismo día se muestra la
     * fecha una sola vez (20 nov. 2026, 15:00 a 22:00).
     */
    public static String rango(LocalDateTime inicio, LocalDateTime fin) {

        if (inicio == null || fin == null) {
            return fechaHora(inicio != null ? inicio : fin);
        }

        if (inicio.toLocalDate().equals(fin.toLocalDate())) {
            return fechaHora(inicio) + " a " + hora(fin.toLocalTime());
        }

        return fechaHora(inicio) + " a " + fechaHora(fin);
    }
}

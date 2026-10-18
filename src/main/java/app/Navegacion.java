package app;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

/**
 * Navegación entre pantallas mediante el menú lateral.
 *
 * No modifica las vistas: busca los elementos con la clase CSS "menu-item"
 * de cada pantalla y les asigna el clic según su texto. Cada pantalla se crea
 * una sola vez, así conserva lo que el usuario haya escrito al volver a ella.
 */
public class Navegacion {

    public enum Pantalla {
        EVENTOS("NextEventux | Crear evento"),
        INVITADOS("NextEventux | Invitaciones");

        private final String titulo;

        Pantalla(String titulo) {
            this.titulo = titulo;
        }
    }

    private final Map<Pantalla, Supplier<Parent>> fabricas = new EnumMap<>(Pantalla.class);
    private final Map<Pantalla, Parent> creadas = new EnumMap<>(Pantalla.class);
    private Scene scene;

    public Navegacion registrar(Pantalla pantalla, Supplier<Parent> fabrica) {
        fabricas.put(pantalla, fabrica);
        return this;
    }

    public Scene crearEscena(Pantalla inicial, double ancho, double alto) {
        scene = new Scene(obtener(inicial), ancho, alto);
        return scene;
    }

    public void ir(Pantalla pantalla) {
        if (scene == null || !fabricas.containsKey(pantalla)) return;
        scene.setRoot(obtener(pantalla));
        if (scene.getWindow() instanceof Stage stage) stage.setTitle(pantalla.titulo);
    }

    private Parent obtener(Pantalla pantalla) {
        return creadas.computeIfAbsent(pantalla, p -> {
            Parent raiz = fabricas.get(p).get();
            conectarMenu(raiz);
            return raiz;
        });
    }

    private void conectarMenu(Parent raiz) {
        for (Node nodo : raiz.lookupAll(".menu-item")) {
            if (!(nodo instanceof Label item)) continue;
            Pantalla destino = destinoDe(item.getText());
            if (destino == null || !fabricas.containsKey(destino)) continue;
            item.setCursor(Cursor.HAND);
            item.setOnMouseClicked(evento -> ir(destino));
        }
    }

    /** Opciones del menú que ya tienen pantalla. Las demás se conectarán cuando existan. */
    private static Pantalla destinoDe(String texto) {
        if (texto == null) return null;
        if (texto.contains("Crear evento") || texto.contains("Mis eventos")) return Pantalla.EVENTOS;
        if (texto.contains("Invitados")) return Pantalla.INVITADOS;
        return null;
    }
}

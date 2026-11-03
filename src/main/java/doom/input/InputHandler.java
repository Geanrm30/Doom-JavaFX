package doom.input;

import javafx.scene.input.KeyCode;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class InputHandler {

    private final Set<KeyCode> keys = new HashSet<>();

    /** Giro acumulado del raton (radianes) desde el ultimo fotograma. */
    private double mouseTurn;
    public boolean mouseFire;
    public boolean mouseLook = true;

    public void keyPressed(KeyCode code) { keys.add(code); }
    public void keyReleased(KeyCode code) { keys.remove(code); }
    public Set<KeyCode> getKeys() { return Collections.unmodifiableSet(keys); }

    public void addMouseTurn(double radians) { mouseTurn += radians; }

    public double takeMouseTurn() {
        double t = mouseTurn;
        mouseTurn = 0;
        return t;
    }

    /** Al perder el foco se sueltan todas las teclas para que no queden "pegadas". */
    public void releaseAll() {
        keys.clear();
        mouseFire = false;
        mouseTurn = 0;
    }
}

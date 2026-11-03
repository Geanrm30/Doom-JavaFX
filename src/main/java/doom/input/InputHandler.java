package doom.input;

import javafx.scene.input.KeyCode;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class InputHandler {

    private final Set<KeyCode> keys = new HashSet<>();

    public void keyPressed(KeyCode code) { keys.add(code); }
    public void keyReleased(KeyCode code) { keys.remove(code); }
    public Set<KeyCode> getKeys() { return Collections.unmodifiableSet(keys); }
}

package doom.world;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/**
 * Progreso y ajustes del jugador, guardados en ~/.doom-javafx/progreso.properties.
 * Todo lo que se lee del archivo se valida y se limita a rangos razonables.
 */
public final class Progress {

    public static final int MAX_LEVELS = 99;

    public int unlocked = 1;
    public int difficulty = 1;
    public int sensitivity = 5;
    public boolean mouseLook = true;
    public boolean muted = false;
    public int volume = 8;
    public boolean fullscreen = true;
    public final double[] bestTime = new double[MAX_LEVELS + 1];

    /** Inventario con el que se empieza el nivel desbloqueado mas alto. */
    public boolean hasInventory;
    public int health = 100, armor = 0, bullets = 50, shells = 0, rockets = 0, cells = 0, weapon = 0;
    public final boolean[] has = {true, false, false, false, false};

    private final Path file;

    private Progress(Path file) {
        this.file = file;
    }

    public static Path defaultPath() {
        String custom = System.getProperty("doom.save");
        if (custom != null && !custom.isBlank()) return Paths.get(custom);
        return Paths.get(System.getProperty("user.home", "."), ".doom-javafx", "progreso.properties");
    }

    public static Progress load() {
        Progress p = new Progress(defaultPath());
        Properties props = new Properties();
        if (Files.isRegularFile(p.file)) {
            try (InputStream in = Files.newInputStream(p.file)) {
                props.load(in);
            } catch (IOException | IllegalArgumentException e) {
                return p; // archivo ilegible: se empieza de cero
            }
        }
        p.unlocked = num(props, "unlocked", 1, 1, MAX_LEVELS);
        p.difficulty = num(props, "difficulty", 1, 0, 2);
        p.sensitivity = num(props, "sensitivity", 5, 1, 10);
        p.mouseLook = bool(props, "mouseLook", true);
        p.muted = bool(props, "muted", false);
        p.volume = num(props, "volume", 8, 0, 10);
        p.fullscreen = bool(props, "fullscreen", true);
        for (int i = 1; i <= MAX_LEVELS; i++) {
            p.bestTime[i] = dbl(props, "best." + i, 0, 0, 86_400);
        }
        p.hasInventory = bool(props, "inv.present", false);
        p.health = num(props, "inv.health", 100, 1, 200);
        p.armor = num(props, "inv.armor", 0, 0, 200);
        p.bullets = num(props, "inv.bullets", 50, 0, Player.MAX_AMMO[0]);
        p.shells = num(props, "inv.shells", 0, 0, Player.MAX_AMMO[1]);
        p.has[1] = bool(props, "inv.shotgun", false);
        p.has[2] = bool(props, "inv.chaingun", false);
        p.has[3] = bool(props, "inv.rocket", false);
        p.has[4] = bool(props, "inv.plasma", false);
        p.rockets = num(props, "inv.rockets", 0, 0, Player.MAX_AMMO[2]);
        p.cells = num(props, "inv.cells", 0, 0, Player.MAX_AMMO[3]);
        p.weapon = num(props, "inv.weapon", 0, 0, 4);
        if (!p.has[p.weapon]) p.weapon = 0;
        return p;
    }

    public void save() {
        Properties props = new Properties();
        props.setProperty("unlocked", Integer.toString(unlocked));
        props.setProperty("difficulty", Integer.toString(difficulty));
        props.setProperty("sensitivity", Integer.toString(sensitivity));
        props.setProperty("mouseLook", Boolean.toString(mouseLook));
        props.setProperty("muted", Boolean.toString(muted));
        props.setProperty("volume", Integer.toString(volume));
        props.setProperty("fullscreen", Boolean.toString(fullscreen));
        for (int i = 1; i <= MAX_LEVELS; i++) {
            if (bestTime[i] > 0) props.setProperty("best." + i, Double.toString(bestTime[i]));
        }
        props.setProperty("inv.present", Boolean.toString(hasInventory));
        props.setProperty("inv.health", Integer.toString(health));
        props.setProperty("inv.armor", Integer.toString(armor));
        props.setProperty("inv.bullets", Integer.toString(bullets));
        props.setProperty("inv.shells", Integer.toString(shells));
        props.setProperty("inv.shotgun", Boolean.toString(has[1]));
        props.setProperty("inv.chaingun", Boolean.toString(has[2]));
        props.setProperty("inv.rocket", Boolean.toString(has[3]));
        props.setProperty("inv.plasma", Boolean.toString(has[4]));
        props.setProperty("inv.rockets", Integer.toString(rockets));
        props.setProperty("inv.cells", Integer.toString(cells));
        props.setProperty("inv.weapon", Integer.toString(weapon));
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            try (OutputStream out = Files.newOutputStream(tmp)) {
                props.store(out, "DOOM JavaFX - progreso (se puede borrar para empezar de cero)");
            }
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | RuntimeException e) {
            // guardar es "best effort": el juego sigue aunque no haya permisos de escritura
        }
    }

    /** Borra el avance (no los ajustes). */
    public void resetProgress() {
        unlocked = 1;
        java.util.Arrays.fill(bestTime, 0);
        hasInventory = false;
        health = 100;
        armor = 0;
        bullets = 50;
        shells = 0;
        rockets = cells = 0;
        weapon = 0;
        has[1] = has[2] = has[3] = has[4] = false;
    }

    public void storeInventory(Player p) {
        hasInventory = true;
        health = Math.max(1, Math.min(200, p.health));
        armor = p.armor;
        bullets = p.ammo[0];
        shells = p.ammo[1];
        rockets = p.ammo[2];
        cells = p.ammo[3];
        System.arraycopy(p.has, 1, has, 1, 4);
        weapon = p.weapon.ordinal();
    }

    public void applyInventory(Player p) {
        p.resetInventory();
        if (!hasInventory) return;
        p.health = Math.max(health, 50);
        p.armor = armor;
        p.ammo[0] = Math.max(bullets, 20);
        p.ammo[1] = shells;
        p.ammo[2] = rockets;
        p.ammo[3] = cells;
        System.arraycopy(has, 1, p.has, 1, 4);
        p.weapon = Weapon.values()[weapon];
    }

    private static int num(Properties p, String k, int def, int min, int max) {
        try {
            int v = Integer.parseInt(p.getProperty(k, Integer.toString(def)).trim());
            return Math.max(min, Math.min(max, v));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static double dbl(Properties p, String k, double def, double min, double max) {
        try {
            double v = Double.parseDouble(p.getProperty(k, Double.toString(def)).trim());
            if (Double.isNaN(v)) return def;
            return Math.max(min, Math.min(max, v));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static boolean bool(Properties p, String k, boolean def) {
        String v = p.getProperty(k);
        return v == null ? def : Boolean.parseBoolean(v.trim());
    }
}

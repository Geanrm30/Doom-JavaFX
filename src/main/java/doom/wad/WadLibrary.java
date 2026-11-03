package doom.wad;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Busca archivos WAD del usuario y da los nombres oficiales de los mapas. */
public final class WadLibrary {

    private static final String[][] EPISODES = {
        {"Hangar", "Nuclear Plant", "Toxin Refinery", "Command Control", "Phobos Lab", "Central Processing",
         "Computer Station", "Phobos Anomaly", "Military Base"},
        {"Deimos Anomaly", "Containment Area", "Refinery", "Deimos Lab", "Command Center", "Halls of the Damned",
         "Spawning Vats", "Tower of Babel", "Fortress of Mystery"},
        {"Hell Keep", "Slough of Despair", "Pandemonium", "House of Pain", "Unholy Cathedral", "Mt. Erebus",
         "Limbo", "Dis", "Warrens"},
        {"Hell Beneath", "Perfect Hatred", "Sever the Wicked", "Unruly Evil", "They Will Repent",
         "Against Thee Wickedly", "And Hell Followed", "Unto the Cruel", "Fear"}};

    private static final String[] DOOM2 = {
        "Entryway", "Underhalls", "The Gantlet", "The Focus", "The Waste Tunnels", "The Crusher", "Dead Simple",
        "Tricks and Traps", "The Pit", "Refueling Base", "'O' of Destruction!", "The Factory", "Downtown",
        "The Inmost Dens", "Industrial Zone", "Suburbs", "Tenements", "The Courtyard", "The Citadel", "Gotcha!",
        "Nirvana", "The Catacombs", "Barrels o' Fun", "The Chasm", "Bloodfalls", "The Abandoned Mines",
        "Monster Condo", "The Spirit World", "The Living End", "Icon of Sin", "Wolfenstein", "Grosse"};

    private WadLibrary() { }

    /** Carpetas donde se buscan WAD: ./wads, la carpeta actual y ~/.doom-javafx (mas la indicada en doom.wads). */
    public static List<Path> folders() {
        List<Path> out = new ArrayList<>();
        String extra = System.getProperty("doom.wads");
        if (extra != null && !extra.isBlank()) out.add(Paths.get(extra));
        out.add(Paths.get("wads"));
        out.add(Paths.get("."));
        String home = System.getProperty("user.home");
        if (home != null) {
            out.add(Paths.get(home, ".doom-javafx", "wads"));
            out.add(Paths.get(home, ".doom-javafx"));
        }
        return out;
    }

    /** Todos los WAD validos que se encuentran (sin repetir el mismo archivo). */
    public static List<Wad> discover() {
        List<Wad> out = new ArrayList<>();
        Set<Path> seen = new HashSet<>();
        for (Path dir : folders()) {
            if (!Files.isDirectory(dir)) continue;
            List<Path> files = new ArrayList<>();
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
                for (Path p : ds) {
                    if (p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".wad") && Files.isRegularFile(p)) files.add(p);
                }
            } catch (IOException e) {
                continue;
            }
            files.sort((a, b) -> a.getFileName().toString().compareToIgnoreCase(b.getFileName().toString()));
            for (Path f : files) {
                Path real;
                try {
                    real = f.toRealPath();
                } catch (IOException e) {
                    continue;
                }
                if (!seen.add(real)) continue;
                Wad w = Wad.open(real);
                if (w != null && out.size() < 24) out.add(w);
            }
        }
        return out;
    }

    /** Nombre oficial del mapa (E1M1 es "Hangar", MAP01 es "Entryway"); vacio si no se conoce. */
    public static String mapTitle(String map) {
        String m = map.toUpperCase(Locale.ROOT);
        try {
            if (m.startsWith("MAP")) {
                int n = Integer.parseInt(m.substring(3));
                return n >= 1 && n <= DOOM2.length ? DOOM2[n - 1] : "";
            }
            int e = m.charAt(1) - '1', n = m.charAt(3) - '1';
            return e >= 0 && e < 4 && n >= 0 && n < 9 ? EPISODES[e][n] : "";
        } catch (RuntimeException ex) {
            return "";
        }
    }
}

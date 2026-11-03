package doom.world;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Un nivel cargado desde /levels/levelN.txt (ver tools/generate_levels.py para el formato). */
public final class Level {

    public record Thing(char type, double x, double y) {}

    private static final String THINGS = "zidcBYJhsakegmruHvwnlq";

    public final String name;
    public final char[][] cells;
    public final int width, height;
    public final int[] ceiling, floor;
    public final double fog;
    public double startX = 1.5, startY = 1.5, startAngle;
    public final List<Thing> things = new ArrayList<>();
    public final boolean[][] seen;
    /** Apertura de las puertas (0 cerrada, 1 abierta) y su fase: 0 reposo, 1 abriendo, 2 abierta, 3 cerrando. */
    private final float[][] doorOpen;
    private final byte[][] doorPhase;
    private final float[][] doorTimer;
    /** Textura que imita la puerta secreta (copia la pared de al lado). */
    private final char[][] secretLook;
    public int secretCount;
    /** Distancia en celdas hasta el jugador (BFS); los enemigos la siguen para rodear paredes. */
    private int[][] flow;
    private int flowCx = Integer.MIN_VALUE, flowCy;
    public static final int UNREACHABLE = 1 << 20;
    private static final double DOOR_SPEED = 1.6, DOOR_HOLD = 4.5;

    private Level(String name, char[][] cells, int[] ceiling, int[] floor, double fog) {
        this.name = name;
        this.cells = cells;
        this.height = cells.length;
        this.width = cells[0].length;
        this.ceiling = ceiling;
        this.floor = floor;
        this.fog = fog;
        this.seen = new boolean[height][width];
        this.doorOpen = new float[height][width];
        this.doorPhase = new byte[height][width];
        this.doorTimer = new float[height][width];
        this.secretLook = new char[height][width];
    }

    /** Cuenta cuantos archivos levelN.txt existen consecutivamente desde 1. */
    public static int count() {
        int n = 0;
        while (Level.class.getResource("/levels/level" + (n + 1) + ".txt") != null) n++;
        return n;
    }

    /** Lee solo el nombre del nivel (cabecera), sin cargar el mapa. */
    public static String nameOf(int index) {
        try (InputStream in = Level.class.getResourceAsStream("/levels/level" + index + ".txt")) {
            if (in == null) return "Nivel " + index;
            BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = br.readLine()) != null && !line.trim().equals("---")) {
                if (line.startsWith("name=")) return line.substring(5).trim();
            }
        } catch (IOException e) {
            // se usa el nombre por defecto
        }
        return "Nivel " + index;
    }

    public static Level load(int index) {
        try (InputStream in = Level.class.getResourceAsStream("/levels/level" + index + ".txt")) {
            if (in == null) return null;
            BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String name = "Nivel " + index;
            int[] ceil = {30, 30, 40}, flr = {60, 55, 50};
            double fog = 0.22, angleDeg = 0;
            String line;
            while ((line = br.readLine()) != null && !line.trim().equals("---")) {
                int eq = line.indexOf('=');
                if (eq < 0) continue;
                String k = line.substring(0, eq).trim(), v = line.substring(eq + 1).trim();
                switch (k) {
                    case "name" -> name = v;
                    case "ceiling" -> ceil = rgb(v);
                    case "floor" -> flr = rgb(v);
                    case "fog" -> fog = Double.parseDouble(v);
                    case "angle" -> angleDeg = Double.parseDouble(v);
                    default -> { }
                }
            }
            List<String> rows = new ArrayList<>();
            int w = 0;
            while ((line = br.readLine()) != null) {
                if (line.isEmpty()) continue;
                rows.add(line);
                w = Math.max(w, line.length());
            }
            char[][] cells = new char[rows.size()][w];
            Level lv = new Level(name, cells, ceil, flr, fog);
            lv.startAngle = Math.toRadians(angleDeg);
            for (int y = 0; y < rows.size(); y++) {
                String r = rows.get(y);
                for (int x = 0; x < w; x++) {
                    char c = x < r.length() ? r.charAt(x) : '1';
                    if (c == ' ') c = '.';
                    if (c == 'P') {
                        lv.startX = x + 0.5;
                        lv.startY = y + 0.5;
                        c = '.';
                    } else if (THINGS.indexOf(c) >= 0) {
                        lv.things.add(new Thing(c, x + 0.5, y + 0.5));
                        c = '.';
                    }
                    cells[y][x] = c;
                }
            }
            lv.prepareSecrets();
            return lv;
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el nivel " + index, e);
        }
    }

    private static int[] rgb(String v) {
        String[] p = v.split(",");
        return new int[]{Integer.parseInt(p[0].trim()), Integer.parseInt(p[1].trim()), Integer.parseInt(p[2].trim())};
    }

    public char cell(int cx, int cy) {
        if (cx < 0 || cy < 0 || cx >= width || cy >= height) return '1';
        return cells[cy][cx];
    }

    private void prepareSecrets() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (cells[y][x] != 'S') continue;
                secretCount++;
                char look = '1';
                int[][] nb = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
                for (int[] d : nb) {
                    char n = cell(x + d[0], y + d[1]);
                    if (n >= '1' && n <= '6') { look = n; break; }
                }
                secretLook[y][x] = look;
            }
        }
    }

    /** Caracter con el que se dibuja la celda (las puertas secretas parecen pared). */
    public char look(int cx, int cy) {
        char c = cell(cx, cy);
        return c == 'S' ? secretLook[cy][cx] : c;
    }

    public static boolean isDoor(char c) {
        return c == 'D' || c == 'R' || c == 'U' || c == 'S';
    }

    /** Apertura de la puerta en la celda (0 si no es puerta). */
    public double openness(int cx, int cy) {
        if (cx < 0 || cy < 0 || cx >= width || cy >= height) return 0;
        return isDoor(cells[cy][cx]) ? doorOpen[cy][cx] : 0;
    }

    public boolean isWall(int cx, int cy) {
        char c = cell(cx, cy);
        if (c == '.') return false;
        if (isDoor(c)) return doorOpen[cy][cx] < 0.8f;
        return true;
    }

    /** Los enemigos atraviesan celdas libres y puertas normales (las de llave no). */
    public boolean passable(int cx, int cy) {
        return !isWall(cx, cy) || cell(cx, cy) == 'D';
    }

    public void updateFlow(double px, double py) {
        int cx = (int) px, cy = (int) py;
        if (cx == flowCx && cy == flowCy) return;
        flowCx = cx;
        flowCy = cy;
        if (flow == null) flow = new int[height][width];
        for (int[] row : flow) java.util.Arrays.fill(row, UNREACHABLE);
        if (cx < 0 || cy < 0 || cx >= width || cy >= height) return;
        int[] queue = new int[width * height];
        int head = 0, tail = 0;
        flow[cy][cx] = 0;
        queue[tail++] = cy * width + cx;
        while (head < tail) {
            int cur = queue[head++];
            int x = cur % width, y = cur / width;
            for (int d = 0; d < 4; d++) {
                int nx = x + (d == 0 ? 1 : d == 1 ? -1 : 0), ny = y + (d == 2 ? 1 : d == 3 ? -1 : 0);
                if (nx < 0 || ny < 0 || nx >= width || ny >= height || flow[ny][nx] != UNREACHABLE) continue;
                if (!passable(nx, ny)) continue;
                flow[ny][nx] = flow[y][x] + 1;
                queue[tail++] = ny * width + nx;
            }
        }
    }

    public int flowAt(int cx, int cy) {
        if (flow == null || cx < 0 || cy < 0 || cx >= width || cy >= height) return UNREACHABLE;
        return flow[cy][cx];
    }

    /** Siguiente celda (centro) para acercarse al jugador desde (cx, cy), o null si no hay camino. */
    public int[] nextStep(int cx, int cy) {
        int here = flowAt(cx, cy);
        if (here == UNREACHABLE || here == 0) return null;
        int best = here, bx = 0, by = 0;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) continue;
                int nx = cx + dx, ny = cy + dy;
                int f = flowAt(nx, ny);
                if (f >= best) continue;
                if (dx != 0 && dy != 0 && (!passable(cx + dx, cy) || !passable(cx, cy + dy))) continue;
                best = f;
                bx = nx;
                by = ny;
            }
        }
        return best < here ? new int[]{bx, by} : null;
    }

    /** Empieza a abrir la puerta; si ya esta abierta, reinicia la espera. */
    public void open(int cx, int cy) {
        if (cx < 0 || cy < 0 || cx >= width || cy >= height) return;
        if (!isDoor(cells[cy][cx])) {
            cells[cy][cx] = '.';
            return;
        }
        if (doorPhase[cy][cx] == 2) {
            doorTimer[cy][cx] = (float) DOOR_HOLD;
        } else if (doorPhase[cy][cx] != 1) {
            doorPhase[cy][cx] = 1;
        }
    }

    /** True si la puerta empezo a cerrarse en este paso (para el sonido). */
    public interface DoorListener {
        boolean blocked(int cx, int cy);
        void closing(int cx, int cy);
    }

    public void updateDoors(double dt, DoorListener l) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                byte ph = doorPhase[y][x];
                if (ph == 0) continue;
                if (ph == 1) {
                    doorOpen[y][x] += (float) (DOOR_SPEED * dt);
                    if (doorOpen[y][x] >= 1f) {
                        doorOpen[y][x] = 1f;
                        doorPhase[y][x] = 2;
                        doorTimer[y][x] = (float) DOOR_HOLD;
                    }
                } else if (ph == 2) {
                    if (cells[y][x] == 'S') continue; // las puertas secretas se quedan abiertas
                    doorTimer[y][x] -= (float) dt;
                    if (doorTimer[y][x] <= 0 && !l.blocked(x, y)) {
                        doorPhase[y][x] = 3;
                        l.closing(x, y);
                    }
                } else {
                    if (l.blocked(x, y)) {
                        doorPhase[y][x] = 1; // algo se interpone: vuelve a abrirse
                    } else {
                        doorOpen[y][x] -= (float) (DOOR_SPEED * 0.7 * dt);
                        if (doorOpen[y][x] <= 0f) {
                            doorOpen[y][x] = 0f;
                            doorPhase[y][x] = 0;
                        }
                    }
                }
            }
        }
    }

    public boolean lineOfSight(double x0, double y0, double x1, double y1) {
        double dx = x1 - x0, dy = y1 - y0;
        double dist = Math.hypot(dx, dy);
        int steps = (int) (dist / 0.1) + 1;
        for (int i = 1; i < steps; i++) {
            double t = (double) i / steps;
            if (isWall((int) (x0 + dx * t), (int) (y0 + dy * t))) return false;
        }
        return true;
    }

    /** Marca como vistas las celdas cercanas con linea de vision (para el minimapa). */
    public void reveal(double px, double py) {
        int cx = (int) px, cy = (int) py;
        for (int y = cy - 4; y <= cy + 4; y++) {
            for (int x = cx - 4; x <= cx + 4; x++) {
                if (x < 0 || y < 0 || x >= width || y >= height || seen[y][x]) continue;
                if (lineOfSight(px, py, x + 0.5, y + 0.5) || (Math.abs(x - cx) <= 1 && Math.abs(y - cy) <= 1)
                        || neighborVisible(px, py, x, y)) {
                    seen[y][x] = true;
                }
            }
        }
    }

    // una pared es visible si la celda adyacente (hacia el jugador) tiene linea de vision
    private boolean neighborVisible(double px, double py, int x, int y) {
        if (!isWall(x, y)) return false;
        int sx = Integer.signum((int) px - x), sy = Integer.signum((int) py - y);
        return (sx != 0 && !isWall(x + sx, y) && lineOfSight(px, py, x + sx + 0.5, y + 0.5))
                || (sy != 0 && !isWall(x, y + sy) && lineOfSight(px, py, x + 0.5, y + sy + 0.5));
    }
}

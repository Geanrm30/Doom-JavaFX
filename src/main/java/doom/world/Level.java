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

    private static final String THINGS = "zidcBYJhsakegmruHvo";

    public final String name;
    public final char[][] cells;
    public final int width, height;
    public final int[] ceiling, floor;
    public final double fog;
    public double startX = 1.5, startY = 1.5, startAngle;
    public final List<Thing> things = new ArrayList<>();
    public final boolean[][] seen;

    private Level(String name, char[][] cells, int[] ceiling, int[] floor, double fog) {
        this.name = name;
        this.cells = cells;
        this.height = cells.length;
        this.width = cells[0].length;
        this.ceiling = ceiling;
        this.floor = floor;
        this.fog = fog;
        this.seen = new boolean[height][width];
    }

    /** Cuenta cuantos archivos levelN.txt existen consecutivamente desde 1. */
    public static int count() {
        int n = 0;
        while (Level.class.getResource("/levels/level" + (n + 1) + ".txt") != null) n++;
        return n;
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

    public boolean isWall(int cx, int cy) {
        return cell(cx, cy) != '.';
    }

    public void open(int cx, int cy) {
        if (cx >= 0 && cy >= 0 && cx < width && cy < height) cells[cy][cx] = '.';
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

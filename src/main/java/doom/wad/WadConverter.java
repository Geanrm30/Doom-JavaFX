package doom.wad;

import doom.world.Level;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Convierte un mapa de Doom (lineas, lados y sectores) a la cuadricula del motor.
 *
 * El motor es de celdas, asi que cada celda cubre 32x32 unidades de Doom. Para cada centro de celda se
 * averigua a que sector pertenece lanzando un rayo hacia la derecha hasta la primera linea (el lado de
 * esa linea indica el sector, o el vacio). Despues se recorre el mapa desde el inicio del jugador con las
 * reglas de Doom (escalones de hasta 24 unidades, altura minima de 56, puertas y ascensores) y todo lo que
 * no se alcanza se convierte en pared. Las cosas (enemigos, objetos, llaves) se traducen a los del motor.
 */
public final class WadConverter {

    private record Line(int v1, int v2, int flags, int special, int tag, int right, int left) {}

    private record Side(String upper, String lower, String mid, int sector) {}

    private record Sector(int floor, int ceil, int tag) {}

    // llaves de las puertas: 1 roja, 2 azul, 3 amarilla
    private static int keyOf(int special) {
        return switch (special) {
            case 28, 33, 134, 135 -> 1;
            case 26, 32, 99, 133 -> 2;
            case 27, 34, 136, 137 -> 3;
            default -> 0;
        };
    }

    private static boolean isLift(int special) {
        return switch (special) {
            case 10, 21, 62, 88, 120, 121, 122, 123 -> true;
            default -> false;
        };
    }

    /** Acciones que abren o mueven un sector cerrado (puertas y rincones de monstruos). */
    private static boolean isDoorAction(int special) {
        return switch (special) {
            case 1, 2, 3, 4, 16, 26, 27, 28, 29, 31, 32, 33, 34, 42, 46, 50, 61, 63, 75, 76, 86, 90, 99, 103,
                 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 133, 134, 135, 136, 137,
                 175, 196 -> true;
            default -> false;
        };
    }

    private static boolean isExit(int special) {
        return special == 11 || special == 52 || special == 197 || special == 198;
    }

    /** Resultado de la conversion. */
    public static final class Converted {
        public final Level level;
        public final int mode;
        public final int openCells;
        public final boolean exitReachable;

        Converted(Level level, int mode, int openCells, boolean exitReachable) {
            this.level = level;
            this.mode = mode;
            this.openCells = openCells;
            this.exitReachable = exitReachable;
        }
    }

    private WadConverter() { }

    public static Converted convert(Wad.MapData m, String title, int skill) {
        // ------------------------------------------------------------ lectura
        ByteBuffer vb = le(m.vertexes), lb = le(m.linedefs), sb = le(m.sidedefs), cb = le(m.sectors), tb = le(m.things);
        int nv = m.vertexes.length / 4;
        int[] vx = new int[nv], vy = new int[nv];
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (int i = 0; i < nv; i++) {
            vx[i] = vb.getShort(i * 4);
            vy[i] = vb.getShort(i * 4 + 2);
            minX = Math.min(minX, vx[i]);
            maxX = Math.max(maxX, vx[i]);
            minY = Math.min(minY, vy[i]);
            maxY = Math.max(maxY, vy[i]);
        }
        if (nv == 0) throw new IllegalStateException("mapa sin vertices");
        int nl = m.linedefs.length / 14;
        Line[] lines = new Line[nl];
        for (int i = 0; i < nl; i++) {
            int o = i * 14;
            lines[i] = new Line(lb.getShort(o) & 0xFFFF, lb.getShort(o + 2) & 0xFFFF, lb.getShort(o + 4) & 0xFFFF,
                    lb.getShort(o + 6) & 0xFFFF, lb.getShort(o + 8) & 0xFFFF, lb.getShort(o + 10) & 0xFFFF,
                    lb.getShort(o + 12) & 0xFFFF);
            if (lines[i].v1 >= nv || lines[i].v2 >= nv) throw new IllegalStateException("linea con vertice invalido");
        }
        int ns = m.sidedefs.length / 30;
        Side[] sides = new Side[ns];
        for (int i = 0; i < ns; i++) {
            int o = i * 30;
            sides[i] = new Side(name8(m.sidedefs, o + 4), name8(m.sidedefs, o + 12), name8(m.sidedefs, o + 20),
                    sb.getShort(o + 28) & 0xFFFF);
        }
        int nsec = m.sectors.length / 26;
        Sector[] secs = new Sector[nsec];
        for (int i = 0; i < nsec; i++) {
            int o = i * 26;
            secs[i] = new Sector(cb.getShort(o), cb.getShort(o + 2), cb.getShort(o + 24));
        }

        // ------------------------------------------------------------ cuadricula
        double cell = 32;
        while ((maxX - minX) / cell > 330 || (maxY - minY) / cell > 330) cell += 8;
        int W = (int) Math.ceil((maxX - minX) / cell) + 4, H = (int) Math.ceil((maxY - minY) / cell) + 4;
        double originX = minX - 2 * cell, topY = maxY + 2 * cell;

        int[][] sec = new int[H][W];
        sweepSectors(lines, sides, vx, vy, nsec, sec, W, H, originX, topY, cell);

        // sectores especiales
        boolean[] door = new boolean[nsec], lift = new boolean[nsec];
        int[] key = new int[nsec];
        for (Line l : lines) {
            if (!isDoorAction(l.special)) continue;
            int back = l.left == 0xFFFF || l.left >= ns ? -1 : sides[l.left].sector;
            if (back >= 0 && back < nsec && l.tag == 0) door[back] = true;
            if (l.tag != 0) for (int s = 0; s < nsec; s++) if (secs[s].tag == l.tag) door[s] = true;
        }
        for (int i = 0; i < nsec; i++) if (secs[i].ceil - secs[i].floor >= 56) door[i] = false;
        for (Line l : lines) {
            int k = keyOf(l.special);
            if (k != 0) {
                int back = l.left == 0xFFFF || l.left >= ns ? -1 : sides[l.left].sector;
                if (back >= 0 && back < nsec && door[back] && (l.special == 26 || l.special == 27 || l.special == 28
                        || l.special == 32 || l.special == 33 || l.special == 34)) key[back] = k;
                for (int s = 0; s < nsec; s++) if (l.tag != 0 && secs[s].tag == l.tag && door[s]) key[s] = k;
            }
            if (isLift(l.special) && l.tag != 0) {
                for (int s = 0; s < nsec; s++) if (secs[s].tag == l.tag) lift[s] = true;
            }
        }

        boolean[][] open = new boolean[H][W];
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int s = sec[y][x];
                open[y][x] = s >= 0 && s < nsec && (door[s] || secs[s].ceil - secs[s].floor >= 56);
            }
        }

        // paredes finas: lineas de un solo lado (o infranqueables) que pasan cerca del centro de una celda
        boolean[][] thin = new boolean[H][W];
        for (Line l : lines) {
            boolean oneSided = l.left == 0xFFFF || l.right == 0xFFFF;
            if (!oneSided && (l.flags & 1) == 0) continue;
            markNear(thin, vx[l.v1], vy[l.v1], vx[l.v2], vy[l.v2], 8.0, W, H, originX, topY, cell);
        }

        // ------------------------------------------------------------ cosas
        int mask = skill <= 0 ? 1 : skill == 1 ? 2 : 4;
        double startDoomX = 0, startDoomY = 0, startAngle = 0;
        boolean haveStart = false;
        List<int[]> things = new ArrayList<>(); // x, y, tipo
        int nt = m.things.length / 10;
        for (int i = 0; i < nt; i++) {
            int o = i * 10;
            int x = tb.getShort(o), y = tb.getShort(o + 2), ang = tb.getShort(o + 4) & 0xFFFF;
            int type = tb.getShort(o + 6) & 0xFFFF, flags = tb.getShort(o + 8) & 0xFFFF;
            if (type == 1) {
                startDoomX = x;
                startDoomY = y;
                startAngle = ang;
                haveStart = true;
                continue;
            }
            if ((flags & 0x10) != 0 || (flags & mask) == 0) continue;
            things.add(new int[]{x, y, type});
        }
        if (!haveStart) throw new IllegalStateException("el mapa no tiene inicio del jugador");

        String upper = m.name.toUpperCase();
        boolean bossEnd = upper.equals("E1M8") || upper.equals("E2M8") || upper.equals("E3M8") || upper.equals("E4M6")
                || upper.equals("E4M8") || upper.equals("MAP30");
        boolean e1m8 = upper.equals("E1M8");

        // ------------------------------------------------------------ salidas
        List<double[]> exitPoints = new ArrayList<>(); // x, y, normal x, normal y (hacia el lado vacio)
        for (Line l : lines) {
            if (!isExit(l.special)) continue;
            double x1 = vx[l.v1], y1 = vy[l.v1], x2 = vx[l.v2], y2 = vy[l.v2];
            double len = Math.hypot(x2 - x1, y2 - y1);
            int n = Math.max(1, (int) (len / 12));
            for (int i = 0; i <= n; i++) {
                double t = (double) i / n;
                double nx = -(y2 - y1) / Math.max(1e-6, len), ny = (x2 - x1) / Math.max(1e-6, len); // izquierda
                boolean oneSided = l.left == 0xFFFF;
                exitPoints.add(new double[]{x1 + (x2 - x1) * t, y1 + (y2 - y1) * t, oneSided ? nx : 0, oneSided ? ny : 0});
            }
        }

        int sx = (int) ((startDoomX - originX) / cell), sy = (int) ((topY - startDoomY) / cell);
        sx = Math.max(0, Math.min(W - 1, sx));
        sy = Math.max(0, Math.min(H - 1, sy));

        // ------------------------------------------------------------ alcanzabilidad (de lo estricto a lo laxo)
        boolean[][] reach = null;
        int usedMode = 0;
        boolean exitOk = false;
        for (int mode = 0; mode < 4; mode++) {
            boolean[][] r = flood(mode, sx, sy, W, H, open, thin, sec, secs, door, lift);
            reach = r;
            usedMode = mode;
            boolean ok = exitPoints.isEmpty() && !bossEnd;
            if (!exitPoints.isEmpty()) ok = exitReached(r, exitPoints, W, H, originX, topY, cell);
            if (bossEnd) {
                ok = false;
                for (int[] t : things) {
                    if (bossType(t[2], upper) == 0) continue;
                    int cx = (int) ((t[0] - originX) / cell), cy = (int) ((topY - t[1]) / cell);
                    if (cx >= 0 && cy >= 0 && cx < W && cy < H && r[cy][cx]) ok = true;
                }
            }
            exitOk = ok;
            if (ok) break;
        }

        // ------------------------------------------------------------ celdas finales
        char[][] cells = new char[H][W];
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                if (!reach[y][x]) {
                    cells[y][x] = '1';
                    continue;
                }
                int s = sec[y][x];
                if (s >= 0 && s < nsec && door[s]) {
                    cells[y][x] = key[s] == 2 ? 'U' : key[s] != 0 ? 'R' : 'D';
                } else {
                    cells[y][x] = '.';
                }
            }
        }
        int openCount = 0;
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) if (cells[y][x] != '1') openCount++;
        // textura de las paredes que ven las celdas libres
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                if (cells[y][x] != '1') continue;
                for (int[] d : dirs) {
                    int ax = x - d[0], ay = y - d[1]; // vecino libre desde el que se mira hacia (d)
                    if (ax < 0 || ay < 0 || ax >= W || ay >= H || cells[ay][ax] == '1') continue;
                    double px = originX + (ax + 0.5) * cell, py = topY - (ay + 0.5) * cell;
                    String tex = wallTexture(px, py, d[0], -d[1], lines, sides, vx, vy, ns);
                    cells[y][x] = textureClass(tex);
                    break;
                }
            }
        }
        // salidas: la pared de la linea de salida (o la celda que cruza si es un umbral)
        for (double[] p : exitPoints) {
            boolean oneSided = p[2] != 0 || p[3] != 0;
            int cx = (int) ((p[0] - originX) / cell), cy = (int) ((topY - p[1]) / cell);
            if (oneSided && inside(cx, cy, W, H) && cells[cy][cx] == '.') {
                cx = (int) ((p[0] + p[2] * 12 - originX) / cell);
                cy = (int) ((topY - (p[1] + p[3] * 12)) / cell);
            }
            if (!inside(cx, cy, W, H)) continue;
            if (oneSided && cells[cy][cx] == '.') continue;
            cells[cy][cx] = 'X';
        }

        // ------------------------------------------------------------ cosas del motor
        List<Level.Thing> out = new ArrayList<>();
        for (int[] t : things) {
            char ch = mapThing(t[2], e1m8, upper);
            if (ch == 0) continue;
            double gx = (t[0] - originX) / cell, gy = (topY - t[1]) / cell;
            double[] pos = nearestOpen(cells, gx, gy, W, H);
            if (pos == null) continue;
            out.add(new Level.Thing(ch, pos[0], pos[1]));
        }
        double[] sp = nearestOpen(cells, (startDoomX - originX) / cell, (topY - startDoomY) / cell, W, H);
        if (sp == null) sp = new double[]{sx + 0.5, sy + 0.5};

        int[] ceil, flr;
        if (upper.startsWith("MAP")) {
            ceil = new int[]{34, 34, 40};
            flr = new int[]{70, 62, 58};
        } else {
            switch (upper.charAt(1)) {
                case '2' -> { ceil = new int[]{36, 40, 46}; flr = new int[]{72, 66, 58}; }
                case '3', '4' -> { ceil = new int[]{44, 24, 22}; flr = new int[]{104, 42, 30}; }
                default -> { ceil = new int[]{30, 34, 44}; flr = new int[]{66, 64, 58}; }
            }
        }
        Level lv = Level.fromGrid(title, cells, ceil, flr, 0.16, sp[0], sp[1], -Math.toRadians(startAngle), out, bossEnd,
                32.0 / cell > 0.99 ? 1.6 : 1.6 * 32.0 / cell);
        return new Converted(lv, usedMode, openCount, exitOk);
    }

    // ---------------------------------------------------------------- recorrido

    private static boolean[][] flood(int mode, int sx, int sy, int W, int H, boolean[][] open, boolean[][] thin,
                                     int[][] sec, Sector[] secs, boolean[] door, boolean[] lift) {
        boolean[][] reach = new boolean[H][W];
        if (mode == 3) {
            for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) reach[y][x] = open[y][x];
            return reach;
        }
        // si el inicio cae en una celda cerrada se busca la mas cercana
        int[] st = {sx, sy};
        if (!usable(open, thin, mode, sx, sy)) {
            outer:
            for (int r = 1; r < 6; r++) {
                for (int dy = -r; dy <= r; dy++) {
                    for (int dx = -r; dx <= r; dx++) {
                        int x = sx + dx, y = sy + dy;
                        if (x >= 0 && y >= 0 && x < W && y < H && usable(open, thin, mode, x, y)) {
                            st = new int[]{x, y};
                            break outer;
                        }
                    }
                }
            }
        }
        ArrayDeque<int[]> q = new ArrayDeque<>();
        reach[st[1]][st[0]] = true;
        q.add(st);
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!q.isEmpty()) {
            int[] c = q.poll();
            for (int[] d : dirs) {
                int nx = c[0] + d[0], ny = c[1] + d[1];
                if (nx < 0 || ny < 0 || nx >= W || ny >= H || reach[ny][nx]) continue;
                if (!usable(open, thin, mode, nx, ny)) continue;
                if (!canStep(mode, sec[c[1]][c[0]], sec[ny][nx], secs, door, lift)) continue;
                reach[ny][nx] = true;
                q.add(new int[]{nx, ny});
            }
        }
        return reach;
    }

    private static boolean usable(boolean[][] open, boolean[][] thin, int mode, int x, int y) {
        if (!open[y][x]) return false;
        return mode >= 2 || !thin[y][x];
    }

    private static boolean canStep(int mode, int a, int b, Sector[] secs, boolean[] door, boolean[] lift) {
        if (a == b || mode >= 2) return true;
        if (door[a] || door[b] || lift[a] || lift[b]) return true;
        Sector sa = secs[a], sb = secs[b];
        int headroom = Math.min(sa.ceil, sb.ceil) - Math.max(sa.floor, sb.floor);
        if (mode == 1) return headroom >= 24;
        return Math.abs(sa.floor - sb.floor) <= 24 && headroom >= 56;
    }

    private static boolean exitReached(boolean[][] reach, List<double[]> pts, int W, int H, double ox, double oy, double cell) {
        for (double[] p : pts) {
            int cx = (int) ((p[0] - ox) / cell), cy = (int) ((oy - p[1]) / cell);
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    int x = cx + dx, y = cy + dy;
                    if (x >= 0 && y >= 0 && x < W && y < H && reach[y][x]) return true;
                }
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- geometria

    private static void sweepSectors(Line[] lines, Side[] sides, int[] vx, int[] vy, int nsec, int[][] sec,
                                     int W, int H, double ox, double oy, double cell) {
        long[] keys = new long[lines.length];
        for (int cy = 0; cy < H; cy++) {
            double yy = oy - (cy + 0.5) * cell + 0.37;
            int n = 0;
            for (int i = 0; i < lines.length; i++) {
                double y1 = vy[lines[i].v1], y2 = vy[lines[i].v2];
                if (y1 == y2) continue;
                double lo = Math.min(y1, y2), hi = Math.max(y1, y2);
                if (yy < lo || yy >= hi) continue;
                double x1 = vx[lines[i].v1], x2 = vx[lines[i].v2];
                double x = x1 + (yy - y1) * (x2 - x1) / (y2 - y1);
                keys[n++] = ((long) Math.round((x + 200000.0) * 64.0) << 20) | i;
            }
            Arrays.sort(keys, 0, n);
            for (int cx = 0; cx < W; cx++) {
                double px = ox + (cx + 0.5) * cell;
                long probe = ((long) Math.round((px + 200000.0) * 64.0) + 1) << 20;
                int lo = 0, hi = n;
                while (lo < hi) {
                    int mid = (lo + hi) >>> 1;
                    if (keys[mid] < probe) lo = mid + 1;
                    else hi = mid;
                }
                if (lo >= n) {
                    sec[cy][cx] = -1;
                    continue;
                }
                Line l = lines[(int) (keys[lo] & 0xFFFFF)];
                boolean up = vy[l.v2] > vy[l.v1];
                int sd = up ? l.left : l.right;
                sec[cy][cx] = (sd == 0xFFFF || sd >= sides.length) ? -1 : (sides[sd].sector < nsec ? sides[sd].sector : -1);
            }
        }
    }

    private static void markNear(boolean[][] grid, double x1, double y1, double x2, double y2, double r,
                                 int W, int H, double ox, double oy, double cell) {
        int cx0 = (int) ((Math.min(x1, x2) - r - ox) / cell), cx1 = (int) ((Math.max(x1, x2) + r - ox) / cell);
        int cy0 = (int) ((oy - Math.max(y1, y2) - r) / cell), cy1 = (int) ((oy - Math.min(y1, y2) + r) / cell);
        double dx = x2 - x1, dy = y2 - y1, len2 = Math.max(1e-9, dx * dx + dy * dy);
        for (int cy = Math.max(0, cy0); cy <= Math.min(H - 1, cy1); cy++) {
            for (int cx = Math.max(0, cx0); cx <= Math.min(W - 1, cx1); cx++) {
                double px = ox + (cx + 0.5) * cell, py = oy - (cy + 0.5) * cell;
                double t = Math.max(0, Math.min(1, ((px - x1) * dx + (py - y1) * dy) / len2));
                if (Math.hypot(x1 + dx * t - px, y1 + dy * t - py) <= r) grid[cy][cx] = true;
            }
        }
    }

    /** Textura de la primera linea que corta el rayo desde (px, py) en la direccion (dx, dy) (en coordenadas de Doom). */
    private static String wallTexture(double px, double py, int dx, int dy, Line[] lines, Side[] sides,
                                      int[] vx, int[] vy, int ns) {
        double best = Double.MAX_VALUE;
        String tex = "-";
        for (Line l : lines) {
            double x1 = vx[l.v1], y1 = vy[l.v1], x2 = vx[l.v2], y2 = vy[l.v2];
            double ex = x2 - x1, ey = y2 - y1;
            double den = dx * ey - dy * ex;
            if (Math.abs(den) < 1e-9) continue;
            double t = ((x1 - px) * ey - (y1 - py) * ex) / den; // distancia a lo largo del rayo
            double u = ((x1 - px) * dy - (y1 - py) * dx) / den; // posicion en la linea
            if (t <= 0 || t >= best || u < 0 || u > 1) continue;
            // el rayo viene del lado derecho si el producto cruzado es negativo
            double cross = ex * (py - y1) - ey * (px - x1);
            int sd = cross < 0 ? l.right : l.left;
            if (sd == 0xFFFF || sd >= ns) {
                sd = cross < 0 ? l.left : l.right;
                if (sd == 0xFFFF || sd >= ns) continue;
            }
            Side s = sides[sd];
            String n = !s.mid.equals("-") ? s.mid : !s.upper.equals("-") ? s.upper : s.lower;
            best = t;
            tex = n;
        }
        return tex;
    }

    /** Clase de textura del motor ('1' a '6') segun el nombre de la textura de Doom. */
    static char textureClass(String name) {
        String n = name.toUpperCase();
        if (has(n, "SKIN", "FLESH", "SKULL", "SKSNAKE", "SKSPINE", "FIRE", "LAVA", "ROCKRED", "BLOOD", "DEMON", "SP_FACE", "SP_HOT", "ASH")) return '6';
        if (has(n, "MARB", "GSTON", "GSTVINE", "SLIME", "VINE")) return '5';
        if (has(n, "BRICK", "BRIK", "BRN", "BROWN", "WOOD", "TAN")) return '1';
        if (has(n, "STONE", "ROCK", "CEMENT", "STON")) return '3';
        if (has(n, "METAL", "STEEL", "PIPE", "SHAWN", "SUPPORT", "RUST", "PLAT")) return '4';
        if (has(n, "STAR", "TEK", "COMP", "TECH", "SW1", "SW2", "LITE", "PANEL", "SILVER", "SLAD", "GRAY", "DOOR")) return '2';
        int h = 0;
        for (int i = 0; i < n.length(); i++) h = h * 31 + n.charAt(i);
        return (char) ('1' + Math.floorMod(h, 4));
    }

    private static boolean has(String n, String... parts) {
        for (String p : parts) if (n.contains(p)) return true;
        return false;
    }

    private static double[] nearestOpen(char[][] cells, double gx, double gy, int W, int H) {
        int cx = (int) gx, cy = (int) gy;
        if (cx >= 0 && cy >= 0 && cx < W && cy < H && cells[cy][cx] == '.') return new double[]{gx, gy};
        for (int r = 1; r <= 3; r++) {
            double bestD = Double.MAX_VALUE;
            double[] best = null;
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    int x = cx + dx, y = cy + dy;
                    if (x < 0 || y < 0 || x >= W || y >= H || cells[y][x] != '.') continue;
                    double d = Math.hypot(x + 0.5 - gx, y + 0.5 - gy);
                    if (d < bestD) {
                        bestD = d;
                        best = new double[]{x + 0.5, y + 0.5};
                    }
                }
            }
            if (best != null) return best;
        }
        return null;
    }

    // ---------------------------------------------------------------- cosas

    /** 0 si no es jefe en este mapa. */
    private static int bossType(int type, String map) {
        return switch (type) {
            case 16, 7 -> 1;
            case 3003 -> map.equals("E1M8") ? 1 : 0;
            case 88 -> 1;
            default -> 0;
        };
    }

    private static char mapThing(int type, boolean e1m8, String map) {
        return switch (type) {
            case 3004 -> 'z';
            case 9 -> 'p';
            case 65 -> 'x';
            case 84 -> 'W';
            case 3001 -> 'i';
            case 3002 -> 'd';
            case 58 -> 't';
            case 3006 -> 'f';
            case 3005 -> 'c';
            case 71 -> 'b';
            case 69 -> 'K';
            case 3003 -> e1m8 ? 'J' : 'B';
            case 66 -> 'V';
            case 67, 68 -> 'M'; // la Arachnotron se sustituye (sin aracnidos)
            case 64 -> 'A';
            case 16, 7 -> 'Y';  // Ciberdemonio y Spider Mastermind
            case 88 -> 'I';
            case 2001, 82 -> 'g';
            case 2002 -> 'm';
            case 2003 -> 'l';
            case 2004, 2006 -> 'q';
            case 2007, 2048, 8 -> 'k';
            case 2008, 2049 -> 'e';
            case 2010, 2046 -> 'w';
            case 2047, 17 -> 'n';
            case 2011 -> 's';
            case 2012 -> 'h';
            case 2013, 83 -> 'H';
            case 2018, 2019 -> 'a';
            case 2022 -> 'v';
            case 5, 40 -> 'u';
            case 6, 39, 13, 38 -> 'r';
            case 2035 -> 'o';
            default -> 0;
        };
    }

    // ---------------------------------------------------------------- utilidades

    private static boolean inside(int x, int y, int W, int H) {
        return x >= 0 && y >= 0 && x < W && y < H;
    }

    private static ByteBuffer le(byte[] b) {
        return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN);
    }

    private static String name8(byte[] b, int off) {
        int end = off;
        while (end < off + 8 && b[end] != 0) end++;
        return new String(b, off, end - off, StandardCharsets.US_ASCII).toUpperCase();
    }

    /** Conjunto de tipos de cosas reconocidos (para pruebas). */
    static Set<Integer> knownThings() {
        Set<Integer> s = new HashSet<>();
        for (int t = 0; t < 4000; t++) if (mapThing(t, false, "") != 0) s.add(t);
        return s;
    }
}

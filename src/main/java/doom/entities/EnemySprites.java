package doom.entities;

/**
 * Dibujo procedural de los enemigos. Cada tipo se pinta una sola vez en varios fotogramas
 * (parado, dos de caminata, ataque, dos de muerte y cadaver) con sombreado y contorno.
 */
public final class EnemySprites {

    public static final int STAND = 0, WALK1 = 1, WALK2 = 2, ATTACK = 3, DIE1 = 4, DIE2 = 5, CORPSE = 6;
    public static final int FRAMES = 7;
    /** Vistas segun el angulo desde el que se mira: 0 de frente, 1 y 3 en tres cuartos, 2 de lado, 4 de espalda. */
    public static final int VIEWS = 5;
    private static final double[] VIEW_WIDTH = {1.0, 0.84, 0.62, 0.84, 1.0};
    private static boolean back;
    static final int N = 96;

    private static final int[][][][] SHEET = new int[EnemyType.values().length][FRAMES][VIEWS][];

    static {
        for (EnemyType t : EnemyType.values()) {
            for (int f = 0; f < FRAMES; f++) {
                int views = f <= ATTACK ? VIEWS : 1;
                for (int v = 0; v < VIEWS; v++) {
                    SHEET[t.ordinal()][f][v] = v < views ? build(t, f, v) : SHEET[t.ordinal()][f][0];
                }
            }
        }
    }

    private EnemySprites() { }

    /** Color ARGB del fotograma en (u, v) de 0 a 1; 0 si es transparente. */
    public static int pixel(EnemyType t, int frame, int view, double u, double v) {
        int x = (int) (u * N), y = (int) (v * N);
        if (x < 0 || y < 0 || x >= N || y >= N) return 0;
        return SHEET[t.ordinal()][frame][view][y * N + x];
    }

    /** Hoja con todos los fotogramas de todos los tipos (para revisar el aspecto). */
    public static int[] sheet() {
        int rows = EnemyType.values().length;
        int[] out = new int[N * FRAMES * N * rows];
        for (int r = 0; r < rows; r++) {
            for (int f = 0; f < FRAMES; f++) {
                int[] src = SHEET[r][f][0];
                for (int y = 0; y < N; y++) {
                    for (int x = 0; x < N; x++) {
                        int c = src[y * N + x];
                        out[(r * N + y) * N * FRAMES + f * N + x] = c == 0 ? 0xFF303848 : c;
                    }
                }
            }
        }
        return out;
    }

    public static int sheetWidth() { return N * FRAMES; }

    public static int sheetHeight() { return N * EnemyType.values().length; }

    // ------------------------------------------------------------------ lienzo

    private static final class Cv {
        final int[] px = new int[N * N];

        static int rgb(int r, int g, int b) {
            return 0xFF000000 | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
        }

        static int clamp(int v) { return v < 0 ? 0 : Math.min(v, 255); }

        static int shade(int c, double f) {
            return rgb((int) (((c >> 16) & 255) * f), (int) (((c >> 8) & 255) * f), (int) ((c & 255) * f));
        }

        void put(int x, int y, int c) {
            if (x >= 0 && y >= 0 && x < N && y < N) px[y * N + x] = c;
        }

        /** Elipse con luz desde arriba a la izquierda. */
        void ell(double cx, double cy, double rx, double ry, int c) {
            for (int y = (int) ((cy - ry) * N) - 1; y <= (int) ((cy + ry) * N) + 1; y++) {
                for (int x = (int) ((cx - rx) * N) - 1; x <= (int) ((cx + rx) * N) + 1; x++) {
                    double nx = ((x + 0.5) / N - cx) / rx, ny = ((y + 0.5) / N - cy) / ry;
                    double d2 = nx * nx + ny * ny;
                    if (d2 <= 1) put(x, y, shade(c, 1.12 - 0.42 * d2 - 0.14 * (nx + ny)));
                }
            }
        }

        void rect(double x0, double y0, double x1, double y1, int c) {
            for (int y = (int) (y0 * N); y < (int) (y1 * N); y++) {
                for (int x = (int) (x0 * N); x < (int) (x1 * N); x++) {
                    double k = (x + 0.5) / N;
                    put(x, y, shade(c, 1.08 - 0.28 * (k - x0) / Math.max(1e-6, x1 - x0)));
                }
            }
        }

        /** Capsula de grosor w entre dos puntos. */
        void limb(double x0, double y0, double x1, double y1, double w, int c) {
            double minx = Math.min(x0, x1) - w, maxx = Math.max(x0, x1) + w;
            double miny = Math.min(y0, y1) - w, maxy = Math.max(y0, y1) + w;
            double dx = x1 - x0, dy = y1 - y0, len2 = Math.max(1e-9, dx * dx + dy * dy);
            for (int y = (int) (miny * N); y <= (int) (maxy * N); y++) {
                for (int x = (int) (minx * N); x <= (int) (maxx * N); x++) {
                    double px = (x + 0.5) / N, py = (y + 0.5) / N;
                    double t = Math.max(0, Math.min(1, ((px - x0) * dx + (py - y0) * dy) / len2));
                    double qx = x0 + dx * t - px, qy = y0 + dy * t - py;
                    double d = Math.sqrt(qx * qx + qy * qy);
                    if (d <= w / 2) {
                        double side = (qx * -dy + qy * dx) / Math.sqrt(len2) / (w / 2);
                        put(x, y, shade(c, 1.05 - 0.3 * side));
                    }
                }
            }
        }

        /** Poligono relleno (puas, cuernos, dientes). */
        void poly(double[] pts, int c) {
            int n = pts.length / 2;
            double minx = 9, maxx = -9, miny = 9, maxy = -9;
            for (int i = 0; i < n; i++) {
                minx = Math.min(minx, pts[2 * i]); maxx = Math.max(maxx, pts[2 * i]);
                miny = Math.min(miny, pts[2 * i + 1]); maxy = Math.max(maxy, pts[2 * i + 1]);
            }
            for (int y = (int) (miny * N); y <= (int) (maxy * N); y++) {
                for (int x = (int) (minx * N); x <= (int) (maxx * N); x++) {
                    double px = (x + 0.5) / N, py = (y + 0.5) / N;
                    boolean in = false;
                    for (int i = 0, j = n - 1; i < n; j = i++) {
                        double xi = pts[2 * i], yi = pts[2 * i + 1], xj = pts[2 * j], yj = pts[2 * j + 1];
                        if ((yi > py) != (yj > py) && px < (xj - xi) * (py - yi) / (yj - yi) + xi) in = !in;
                    }
                    if (in) put(x, y, shade(c, 1.1 - 0.35 * (py - miny) / Math.max(1e-6, maxy - miny)));
                }
            }
        }

        void tri(double ax, double ay, double bx, double by, double cx, double cy, int c) {
            poly(new double[]{ax, ay, bx, by, cx, cy}, c);
        }

        /** Granulado fino y contorno oscuro alrededor de la silueta. */
        void finish() {
            for (int y = 0; y < N; y++) {
                for (int x = 0; x < N; x++) {
                    int c = px[y * N + x];
                    if (c == 0) continue;
                    int h = (x * 73856093) ^ (y * 19349663);
                    h = (h ^ (h >>> 13)) * 1274126177;
                    int n = ((h >>> 16) & 15) - 7;
                    px[y * N + x] = rgb(((c >> 16) & 255) + n, ((c >> 8) & 255) + n, (c & 255) + n);
                }
            }
            int[] out = px.clone();
            for (int y = 0; y < N; y++) {
                for (int x = 0; x < N; x++) {
                    if (px[y * N + x] != 0) continue;
                    boolean edge = (x > 0 && px[y * N + x - 1] != 0) || (x < N - 1 && px[y * N + x + 1] != 0)
                            || (y > 0 && px[(y - 1) * N + x] != 0) || (y < N - 1 && px[(y + 1) * N + x] != 0);
                    if (edge) out[y * N + x] = rgb(18, 8, 8);
                }
            }
            System.arraycopy(out, 0, px, 0, px.length);
        }
    }

    // ------------------------------------------------------------------ fotogramas

    private static int[] build(EnemyType t, int frame, int view) {
        Cv cv = new Cv();
        if (frame == CORPSE) {
            corpse(cv, t);
        } else if (frame == DIE1 || frame == DIE2) {
            Cv live = new Cv();
            alive(live, t, STAND);
            squash(live, cv, frame == DIE1 ? 0.72 : 0.42, frame == DIE1 ? 1.04 : 1.22, frame == DIE1 ? 0.03 : 0.0);
            if (frame == DIE2) blood(cv, 0.5, 0.95, 0.30, 0.04, t);
        } else {
            back = view >= 3;
            alive(cv, t, frame);
            back = false;
            if (view > 0 && view < 4) narrow(cv, VIEW_WIDTH[view]);
        }
        cv.finish();
        return cv.px;
    }

    private static void alive(Cv cv, EnemyType t, int frame) {
        double swing = frame == WALK1 ? 1 : frame == WALK2 ? -1 : 0;
        boolean atk = frame == ATTACK;
        switch (t) {
            case ZOMBIE -> zombie(cv, swing, atk);
            case SHOTGUNNER -> soldier(cv, swing, atk, 1);
            case CHAINGUNNER -> soldier(cv, swing, atk, 2);
            case SS -> soldier(cv, swing, atk, 3);
            case SPECTRE -> demon(cv, swing, atk);
            case LOST_SOUL -> lostSoul(cv, swing, atk);
            case PAIN -> pain(cv, swing, atk);
            case HELL_KNIGHT -> baronLike(cv, swing, atk, 2);
            case REVENANT -> revenant(cv, swing, atk);
            case MANCUBUS -> mancubus(cv, swing, atk);
            case ARCHVILE -> archvile(cv, swing, atk);
            case ICON -> icon(cv, atk);
            case IMP -> imp(cv, swing, atk);
            case DEMON -> demon(cv, swing, atk);
            case CACO -> caco(cv, swing, atk);
            case CYBER -> cyber(cv, swing, atk);
            case BARON -> baronLike(cv, swing, atk, 0);
            case BOSS_BARON -> baronLike(cv, swing, atk, 1);
        }
    }

    /** Estrecha la figura (visto de lado) manteniendo el centro. */
    private static void narrow(Cv cv, double w) {
        int[] src = cv.px.clone();
        java.util.Arrays.fill(cv.px, 0);
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                double u = (x + 0.5) / N;
                int sx = (int) ((0.5 + (u - 0.5) / w) * N);
                if (sx >= 0 && sx < N) cv.px[y * N + x] = src[y * N + sx];
            }
        }
    }

    /** Aplasta la figura contra el suelo (caida) y la ensancha. */
    private static void squash(Cv src, Cv dst, double sy, double sx, double lean) {
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                double v = (double) y / N, u = (double) x / N;
                double sv = 1 - (1 - v) / sy;
                double su = 0.5 + (u - 0.5) / sx - lean * (1 - (1 - v) / sy);
                if (sv < 0 || sv >= 1 || su < 0 || su >= 1) continue;
                int c = src.px[(int) (sv * N) * N + (int) (su * N)];
                if (c != 0) dst.px[y * N + x] = Cv.shade(c, 0.9);
            }
        }
    }

    private static void blood(Cv cv, double cx, double cy, double rx, double ry, EnemyType t) {
        boolean green = t == EnemyType.BARON || t == EnemyType.BOSS_BARON || t == EnemyType.HELL_KNIGHT;
        int c = green ? Cv.rgb(30, 110, 30) : Cv.rgb(120, 8, 8);
        for (int y = (int) ((cy - ry) * N); y <= (int) ((cy + ry) * N); y++) {
            for (int x = (int) ((cx - rx) * N); x <= (int) ((cx + rx) * N); x++) {
                double nx = ((x + 0.5) / N - cx) / rx, ny = ((y + 0.5) / N - cy) / ry;
                if (nx * nx + ny * ny <= 1 && cv.px[y * N + x] == 0) cv.put(x, y, Cv.shade(c, 0.9 + 0.2 * ny));
            }
        }
    }

    private static void corpse(Cv cv, EnemyType t) {
        double w = 0.34 + 0.06 * t.bulk;
        blood(cv, 0.5, 0.95, w + 0.08, 0.045, t);
        if (t == EnemyType.CACO || t == EnemyType.PAIN) {
            cv.ell(0.5, 0.90, 0.36, 0.08, Cv.shade(t.body, 0.7));
            cv.ell(0.5, 0.88, 0.14, 0.04, Cv.rgb(200, 190, 170));
            return;
        }
        if (t == EnemyType.LOST_SOUL) {
            cv.ell(0.5, 0.90, 0.16, 0.06, Cv.rgb(190, 185, 165));
            cv.ell(0.45, 0.89, 0.03, 0.02, Cv.rgb(20, 14, 14));
            cv.ell(0.56, 0.89, 0.03, 0.02, Cv.rgb(20, 14, 14));
            return;
        }
        if (t == EnemyType.ICON) {
            for (int i = 0; i < 9; i++) {
                double cx = 0.12 + i * 0.095, h = 0.05 + 0.04 * ((i * 7) % 4);
                cv.ell(cx, 0.93 - h * 0.4, 0.08, h, Cv.shade(t.body, 0.55 + 0.08 * (i % 3)));
            }
            return;
        }
        int body = Cv.shade(t.body, 0.8);
        cv.limb(0.5 - w, 0.93, 0.5 + w * 0.2, 0.91, 0.10 * t.bulk, body);
        cv.limb(0.5 + w * 0.2, 0.92, 0.5 + w, 0.935, 0.07, Cv.shade(t.body, 0.6));
        cv.ell(0.5 - w - 0.02, 0.915, 0.06, 0.05, t.head);
        cv.ell(0.5 - w - 0.04, 0.905, 0.012, 0.012, Cv.rgb(20, 20, 20));
    }

    // ------------------------------------------------------------------ tipos

    private static void legs(Cv cv, double swing, int color, int boot, double hipY, double footY, double w, double spread) {
        for (int s = -1; s <= 1; s += 2) {
            double lift = Math.max(0, swing * s) * 0.04;
            double hx = 0.5 + s * 0.07 * spread;
            double fx = 0.5 + s * 0.09 * spread + swing * s * 0.045;
            cv.limb(hx, hipY, fx, footY - lift, w, color);
            cv.ell(fx + s * 0.01, footY + 0.005 - lift, w * 0.75, 0.032, boot);
        }
    }

    private static void eyes(Cv cv, double cx, double cy, double gap, double r, int color) {
        if (back) return;
        for (int s = -1; s <= 1; s += 2) {
            cv.ell(cx + s * gap, cy, r, r * 0.7, color);
        }
    }

    private static void zombie(Cv cv, double swing, boolean atk) {
        int uni = Cv.rgb(100, 118, 66), dark = Cv.rgb(62, 76, 42), skin = Cv.rgb(205, 165, 135);
        legs(cv, swing, dark, Cv.rgb(40, 34, 30), 0.60, 0.95, 0.11, 1.0);
        cv.ell(0.5, 0.47, 0.17, 0.20, uni);
        cv.rect(0.34, 0.57, 0.66, 0.62, Cv.rgb(80, 56, 34));
        cv.rect(0.47, 0.57, 0.53, 0.62, Cv.rgb(200, 180, 80));
        cv.rect(0.38, 0.40, 0.46, 0.50, Cv.shade(uni, 0.8)); // bolsillo
        // brazo izquierdo caido y derecho sosteniendo el fusil
        cv.limb(0.33, 0.35, 0.31, 0.57, 0.08, uni);
        cv.ell(0.31, 0.59, 0.04, 0.04, skin);
        double gy = atk ? 0.47 : 0.54;
        cv.limb(0.62, 0.36, 0.60, gy + 0.03, 0.08, uni);
        cv.limb(0.38, gy + 0.05, 0.78, gy - (atk ? 0.04 : 0.0), 0.045, Cv.rgb(55, 55, 62)); // canon
        cv.rect(0.30, gy + 0.02, 0.42, gy + 0.10, Cv.rgb(100, 66, 40)); // culata
        cv.ell(0.58, gy + 0.02, 0.04, 0.04, skin);
        if (atk) {
            cv.ell(0.83, gy - 0.05, 0.07, 0.06, Cv.rgb(255, 200, 60));
            cv.ell(0.83, gy - 0.05, 0.035, 0.03, Cv.rgb(255, 255, 220));
        }
        // cabeza y casco
        cv.ell(0.5, 0.215, 0.095, 0.105, skin);
        cv.ell(0.5, 0.17, 0.115, 0.085, Cv.rgb(80, 92, 70));
        cv.rect(0.38, 0.19, 0.62, 0.215, Cv.rgb(60, 70, 52));
        eyes(cv, 0.5, 0.235, 0.04, 0.016, Cv.rgb(25, 25, 25));
        if (!back) cv.rect(0.46, 0.275, 0.54, 0.285, Cv.rgb(110, 40, 40));
    }

    private static void imp(Cv cv, double swing, boolean atk) {
        int sk = Cv.rgb(150, 74, 48), dk = Cv.rgb(105, 48, 32), bone = Cv.rgb(230, 220, 190);
        legs(cv, swing, dk, Cv.shade(dk, 0.7), 0.60, 0.95, 0.11, 1.0);
        cv.ell(0.5, 0.47, 0.17, 0.21, sk);
        if (!back) for (int i = 0; i < 4; i++) cv.rect(0.40, 0.38 + i * 0.05, 0.60, 0.395 + i * 0.05, Cv.shade(sk, 0.78)); // costillas
        // puas en los hombros
        for (int s = -1; s <= 1; s += 2) {
            cv.tri(0.5 + s * 0.17, 0.33, 0.5 + s * 0.30, 0.20, 0.5 + s * 0.26, 0.37, bone);
            cv.tri(0.5 + s * 0.20, 0.36, 0.5 + s * 0.34, 0.31, 0.5 + s * 0.24, 0.42, bone);
        }
        cv.limb(0.33, 0.36, 0.26, 0.58, 0.08, sk);
        claws(cv, 0.25, 0.60, bone);
        if (atk) {
            cv.limb(0.67, 0.36, 0.78, 0.22, 0.08, sk);
            cv.ell(0.80, 0.16, 0.09, 0.09, Cv.rgb(255, 120, 20));
            cv.ell(0.80, 0.16, 0.05, 0.05, Cv.rgb(255, 240, 150));
            claws(cv, 0.79, 0.27, bone);
        } else {
            cv.limb(0.67, 0.36, 0.74, 0.58, 0.08, sk);
            claws(cv, 0.75, 0.60, bone);
        }
        cv.ell(0.5, 0.215, 0.10, 0.11, sk);
        cv.tri(0.40, 0.14, 0.36, 0.04, 0.45, 0.12, bone);
        cv.tri(0.60, 0.14, 0.64, 0.04, 0.55, 0.12, bone);
        if (!back) cv.rect(0.40, 0.18, 0.60, 0.20, dk); // ceja
        eyes(cv, 0.5, 0.21, 0.04, 0.02, Cv.rgb(255, 235, 40));
        if (!back) cv.ell(0.5, 0.27, 0.05, atk ? 0.035 : 0.02, Cv.rgb(60, 8, 8));
        if (!back) if (atk) for (int i = -1; i <= 1; i++) cv.tri(0.5 + i * 0.03 - 0.012, 0.245, 0.5 + i * 0.03 + 0.012, 0.245, 0.5 + i * 0.03, 0.275, bone);
    }

    private static void claws(Cv cv, double x, double y, int c) {
        for (int i = -1; i <= 1; i++) cv.tri(x + i * 0.025 - 0.01, y, x + i * 0.025 + 0.01, y, x + i * 0.025, y + 0.05, c);
    }

    private static void demon(Cv cv, double swing, boolean atk) {
        int sk = Cv.rgb(214, 112, 134), dk = Cv.rgb(160, 70, 92), bone = Cv.rgb(238, 228, 200);
        legs(cv, swing, dk, Cv.shade(dk, 0.6), 0.62, 0.95, 0.15, 1.1);
        cv.ell(0.5, 0.52, 0.25, 0.22, sk);
        if (!back) cv.ell(0.5, 0.60, 0.17, 0.12, Cv.shade(sk, 1.1)); // panza
        // brazos cortos hacia delante
        double ex = atk ? 0.20 : 0.24;
        cv.limb(0.30, 0.42, ex, atk ? 0.40 : 0.62, 0.10, sk);
        cv.limb(0.70, 0.42, 1 - ex, atk ? 0.40 : 0.62, 0.10, sk);
        // cabeza grande, baja
        cv.ell(0.5, 0.28, 0.17, 0.15, sk);
        cv.tri(0.36, 0.20, 0.28, 0.04, 0.44, 0.16, bone);
        cv.tri(0.64, 0.20, 0.72, 0.04, 0.56, 0.16, bone);
        if (!back) cv.ell(0.5, 0.35, 0.12, atk ? 0.075 : 0.045, Cv.rgb(80, 8, 14)); // boca
        if (!back) for (int i = -3; i <= 3; i++) {
            cv.tri(0.5 + i * 0.03 - 0.011, atk ? 0.30 : 0.325, 0.5 + i * 0.03 + 0.011, atk ? 0.30 : 0.325, 0.5 + i * 0.03, atk ? 0.34 : 0.350, bone);
        }
        eyes(cv, 0.5, 0.245, 0.07, 0.022, Cv.rgb(255, 210, 30));
        if (!back) cv.rect(0.38, 0.215, 0.62, 0.228, Cv.shade(dk, 0.7));
    }

    private static void caco(Cv cv, double swing, boolean atk) {
        int bd = Cv.rgb(196, 38, 38), bone = Cv.rgb(238, 228, 200);
        double bob = swing * 0.02;
        for (int i = 0; i < 6; i++) { // pequenas puas
            double a = Math.PI * (1.15 + i * 0.14);
            cv.tri(0.5 + Math.cos(a) * 0.38, 0.52 + bob + Math.sin(a) * 0.38, 0.5 + Math.cos(a) * 0.50, 0.52 + bob + Math.sin(a) * 0.50,
                    0.5 + Math.cos(a + 0.18) * 0.38, 0.52 + bob + Math.sin(a + 0.18) * 0.38, bone);
        }
        cv.ell(0.5, 0.52 + bob, 0.43, 0.41, bd);
        cv.ell(0.5, 0.72 + bob, 0.34, 0.17, Cv.shade(bd, 0.75));
        // ojo
        if (!back) {
        cv.ell(0.5, 0.36 + bob, 0.17, 0.15, Cv.rgb(240, 236, 220));
        cv.ell(0.5, 0.37 + bob, 0.09, 0.09, atk ? Cv.rgb(220, 40, 20) : Cv.rgb(60, 190, 70));
        cv.ell(0.5, 0.37 + bob, atk ? 0.05 : 0.03, atk ? 0.05 : 0.04, Cv.rgb(8, 8, 8));
        cv.ell(0.47, 0.34 + bob, 0.015, 0.015, Cv.rgb(255, 255, 255));
        // boca
        double open = atk ? 0.15 : 0.07;
        cv.ell(0.5, 0.68 + bob, 0.25, open, Cv.rgb(60, 4, 6));
        if (atk) cv.ell(0.5, 0.69 + bob, 0.12, 0.07, Cv.rgb(255, 150, 30));
        for (int i = -4; i <= 4; i++) {
            double x = 0.5 + i * 0.05;
            cv.tri(x - 0.018, 0.68 + bob - open * 0.75, x + 0.018, 0.68 + bob - open * 0.75, x, 0.68 + bob - open * 0.75 + 0.06, bone);
            cv.tri(x - 0.018, 0.68 + bob + open * 0.75, x + 0.018, 0.68 + bob + open * 0.75, x, 0.68 + bob + open * 0.75 - 0.05, bone);
        }
        }
        cv.tri(0.32, 0.22 + bob, 0.30, 0.06 + bob, 0.40, 0.17 + bob, bone);
        cv.tri(0.68, 0.22 + bob, 0.70, 0.06 + bob, 0.60, 0.17 + bob, bone);
    }

    private static void cyber(Cv cv, double swing, boolean atk) {
        int sk = Cv.rgb(128, 98, 82), dk = Cv.rgb(86, 64, 54), metal = Cv.rgb(120, 126, 136), bone = Cv.rgb(230, 220, 190);
        // patas de cabra con pezunas
        for (int s = -1; s <= 1; s += 2) {
            double lift = Math.max(0, swing * s) * 0.04;
            double fx = 0.5 + s * 0.12 + swing * s * 0.05;
            cv.limb(0.5 + s * 0.09, 0.62, 0.5 + s * 0.11, 0.80, 0.17, dk);
            cv.limb(0.5 + s * 0.11, 0.80, fx, 0.93 - lift, 0.10, dk);
            cv.tri(fx - 0.06, 0.93 - lift, fx + 0.06, 0.93 - lift, fx + s * 0.02, 0.985 - lift, Cv.rgb(30, 24, 22));
        }
        cv.ell(0.5, 0.45, 0.26, 0.24, sk);
        cv.rect(0.34, 0.34, 0.66, 0.40, metal);
        cv.rect(0.38, 0.48, 0.62, 0.52, metal);
        cv.rect(0.44, 0.50, 0.56, 0.62, Cv.shade(metal, 0.75));
        // brazo izquierdo
        cv.limb(0.26, 0.34, 0.20, 0.58, 0.11, sk);
        cv.ell(0.19, 0.61, 0.05, 0.05, dk);
        // brazo derecho: lanzacohetes
        cv.limb(0.74, 0.34, 0.72, 0.52, 0.10, sk);
        cv.rect(0.60, 0.46, 0.94, 0.58, metal);
        cv.rect(0.86, 0.44, 0.97, 0.60, Cv.shade(metal, 0.6));
        cv.rect(0.60, 0.50, 0.94, 0.52, Cv.shade(metal, 0.5));
        if (atk) {
            cv.ell(0.97, 0.52, 0.06, 0.09, Cv.rgb(255, 140, 20));
            cv.ell(0.96, 0.52, 0.03, 0.05, Cv.rgb(255, 245, 170));
        }
        // cabeza y cuernos
        cv.ell(0.5, 0.20, 0.13, 0.13, sk);
        cv.tri(0.38, 0.16, 0.22, 0.02, 0.37, 0.07, bone);
        cv.tri(0.62, 0.16, 0.78, 0.02, 0.63, 0.07, bone);
        if (!back) cv.rect(0.38, 0.17, 0.62, 0.195, dk);
        eyes(cv, 0.5, 0.215, 0.05, 0.024, Cv.rgb(255, 60, 30));
        if (!back) cv.rect(0.43, 0.26, 0.57, 0.285, Cv.rgb(50, 8, 8));
    }

    private static void baronLike(Cv cv, double swing, boolean atk, int variant) {
        boolean boss = variant == 1, knight = variant == 2;
        int sk = boss ? Cv.rgb(86, 132, 92) : knight ? Cv.rgb(190, 168, 134) : Cv.rgb(206, 140, 140);
        int leg = boss ? Cv.rgb(52, 84, 58) : knight ? Cv.rgb(104, 92, 76) : Cv.rgb(120, 80, 62);
        int bone = Cv.rgb(232, 222, 192);
        int ball = boss ? Cv.rgb(255, 70, 30) : Cv.rgb(80, 255, 70);
        int eye = boss ? Cv.rgb(255, 40, 30) : Cv.rgb(255, 230, 40);
        for (int s = -1; s <= 1; s += 2) {
            double lift = Math.max(0, swing * s) * 0.04;
            double fx = 0.5 + s * 0.12 + swing * s * 0.05;
            cv.limb(0.5 + s * 0.09, 0.62, 0.5 + s * 0.09, 0.78, 0.15, leg);
            cv.limb(0.5 + s * 0.09, 0.78, fx, 0.92 - lift, 0.10, leg);
            cv.tri(fx - 0.06, 0.92 - lift, fx + 0.06, 0.92 - lift, fx + s * 0.02, 0.985 - lift, Cv.rgb(30, 24, 22));
        }
        cv.ell(0.5, 0.45, 0.24, 0.23, sk);
        cv.ell(0.5, 0.50, 0.14, 0.16, Cv.shade(sk, 1.12));
        if (!back) for (int i = 0; i < 3; i++) cv.rect(0.43, 0.41 + i * 0.06, 0.57, 0.425 + i * 0.06, Cv.shade(sk, 0.72));
        if (boss) {
            for (int s = -1; s <= 1; s += 2) {
                for (int i = 0; i < 3; i++) {
                    cv.tri(0.5 + s * 0.20, 0.32 + i * 0.07, 0.5 + s * 0.33, 0.30 + i * 0.07, 0.5 + s * 0.22, 0.38 + i * 0.07, bone);
                }
            }
        }
        // brazos
        if (atk) {
            cv.limb(0.28, 0.34, 0.20, 0.20, 0.10, sk);
            cv.limb(0.72, 0.34, 0.80, 0.20, 0.10, sk);
            cv.ell(0.17, 0.14, 0.09, 0.09, ball);
            cv.ell(0.17, 0.14, 0.05, 0.05, Cv.rgb(240, 255, 220));
            cv.ell(0.83, 0.14, 0.09, 0.09, ball);
            cv.ell(0.83, 0.14, 0.05, 0.05, Cv.rgb(240, 255, 220));
        } else {
            cv.limb(0.28, 0.34, 0.20, 0.58, 0.10, sk);
            cv.limb(0.72, 0.34, 0.80, 0.58, 0.10, sk);
            claws(cv, 0.19, 0.60, bone);
            claws(cv, 0.81, 0.60, bone);
        }
        // cabeza y cuernos curvos
        cv.ell(0.5, 0.20, 0.12, 0.13, sk);
        for (int s = -1; s <= 1; s += 2) {
            cv.limb(0.5 + s * 0.09, 0.12, 0.5 + s * 0.22, 0.10, 0.05, bone);
            cv.limb(0.5 + s * 0.22, 0.10, 0.5 + s * 0.28, 0.01, 0.04, bone);
        }
        if (!back) cv.rect(0.39, 0.17, 0.61, 0.19, Cv.shade(sk, 0.65));
        eyes(cv, 0.5, 0.205, 0.045, 0.02, eye);
        if (!back) cv.ell(0.5, 0.265, 0.05, atk ? 0.04 : 0.02, Cv.rgb(50, 6, 8));
        if (!back) if (atk) for (int i = -1; i <= 1; i++) cv.tri(0.5 + i * 0.03 - 0.01, 0.235, 0.5 + i * 0.03 + 0.01, 0.235, 0.5 + i * 0.03, 0.27, bone);
    }

    // ------------------------------------------------------------------ enemigos nuevos

    /** Soldados humanos: 1 escopetero, 2 ametrallador, 3 SS. */
    private static void soldier(Cv cv, double swing, boolean atk, int kind) {
        int uni = kind == 1 ? Cv.rgb(128, 76, 52) : kind == 2 ? Cv.rgb(66, 88, 124) : Cv.rgb(86, 94, 112);
        int dark = Cv.shade(uni, 0.62), skin = Cv.rgb(205, 165, 135);
        int boot = kind == 3 ? Cv.rgb(18, 16, 18) : Cv.rgb(40, 34, 30);
        double w = kind == 2 ? 1.2 : 1.0;
        legs(cv, swing, dark, boot, 0.60, 0.95, 0.11 * w, 1.0);
        cv.ell(0.5, 0.47, 0.17 * w, 0.20, uni);
        cv.rect(0.34, 0.57, 0.66, 0.62, Cv.rgb(60, 44, 30));
        cv.rect(0.47, 0.57, 0.53, 0.62, Cv.rgb(200, 180, 80));
        if (kind == 2) { // cinturon de municion cruzado
            cv.limb(0.36, 0.30, 0.62, 0.58, 0.04, Cv.rgb(190, 160, 60));
        }
        if (kind == 3) { // brazalete y botones
            cv.rect(0.30, 0.40, 0.36, 0.46, Cv.rgb(190, 30, 30));
            for (int i = 0; i < 3; i++) cv.ell(0.5, 0.36 + i * 0.07, 0.012, 0.012, Cv.rgb(220, 200, 90));
        }
        cv.limb(0.33, 0.35, 0.31, 0.57, 0.08, uni);
        cv.ell(0.31, 0.59, 0.04, 0.04, skin);
        double gy = atk ? 0.47 : 0.54;
        cv.limb(0.62, 0.36, 0.60, gy + 0.03, 0.08, uni);
        if (kind == 1) { // escopeta de dos canones cortos
            cv.limb(0.40, gy + 0.04, 0.80, gy - (atk ? 0.04 : 0.0), 0.06, Cv.rgb(70, 66, 70));
            cv.rect(0.30, gy + 0.02, 0.44, gy + 0.10, Cv.rgb(120, 80, 46));
            cv.rect(0.48, gy + 0.045, 0.60, gy + 0.075, Cv.rgb(100, 70, 40));
        } else if (kind == 2) { // ametralladora pesada
            cv.limb(0.36, gy + 0.05, 0.82, gy - (atk ? 0.03 : 0.0), 0.075, Cv.rgb(50, 52, 60));
            cv.rect(0.52, gy + 0.06, 0.62, gy + 0.18, Cv.rgb(80, 82, 90));
            cv.rect(0.28, gy + 0.02, 0.40, gy + 0.10, Cv.rgb(60, 46, 34));
        } else {
            cv.limb(0.38, gy + 0.05, 0.76, gy - (atk ? 0.04 : 0.0), 0.04, Cv.rgb(55, 55, 62));
            cv.rect(0.30, gy + 0.02, 0.42, gy + 0.10, Cv.rgb(100, 66, 40));
        }
        cv.ell(0.58, gy + 0.02, 0.04, 0.04, skin);
        if (atk) {
            cv.ell(0.86, gy - 0.05, 0.08, 0.07, Cv.rgb(255, 200, 60));
            cv.ell(0.86, gy - 0.05, 0.04, 0.035, Cv.rgb(255, 255, 220));
        }
        cv.ell(0.5, 0.215, 0.095, 0.105, skin);
        if (kind == 3) { // gorra de plato
            cv.ell(0.5, 0.15, 0.12, 0.06, Cv.rgb(70, 76, 90));
            cv.rect(0.38, 0.165, 0.62, 0.19, Cv.rgb(20, 20, 24));
            cv.ell(0.5, 0.15, 0.02, 0.02, Cv.rgb(210, 190, 80));
        } else if (kind == 1) { // pelo y sin casco
            cv.ell(0.5, 0.15, 0.10, 0.06, Cv.rgb(90, 56, 30));
        } else {
            cv.ell(0.5, 0.17, 0.115, 0.085, Cv.rgb(60, 78, 104));
            cv.rect(0.38, 0.19, 0.62, 0.215, Cv.rgb(46, 60, 82));
        }
        eyes(cv, 0.5, 0.235, 0.04, 0.016, Cv.rgb(25, 25, 25));
        if (!back) cv.rect(0.46, 0.275, 0.54, 0.285, Cv.rgb(110, 40, 40));
    }

    private static void lostSoul(Cv cv, double swing, boolean atk) {
        int bone = Cv.rgb(228, 222, 200);
        double bob = swing * 0.025;
        // llamas
        for (int i = 0; i < 7; i++) {
            double a = Math.PI * (1.0 + i / 6.0), len = 0.30 + 0.07 * ((i + (swing > 0 ? 1 : 0)) % 3);
            double bx = 0.5 + Math.cos(a) * 0.30, by = 0.55 + bob + Math.sin(a) * 0.30;
            cv.tri(bx - 0.06, by + 0.06, bx + 0.06, by + 0.06, 0.5 + Math.cos(a) * (0.30 + len), 0.55 + bob + Math.sin(a) * (0.30 + len), Cv.rgb(255, 130, 20));
            cv.tri(bx - 0.03, by + 0.05, bx + 0.03, by + 0.05, 0.5 + Math.cos(a) * (0.30 + len * 0.6), 0.55 + bob + Math.sin(a) * (0.30 + len * 0.6), Cv.rgb(255, 220, 90));
        }
        cv.ell(0.5, 0.56 + bob, 0.30, 0.30, bone);
        if (!back) {
            cv.ell(0.38, 0.50 + bob, 0.075, 0.09, Cv.rgb(20, 8, 6));
            cv.ell(0.62, 0.50 + bob, 0.075, 0.09, Cv.rgb(20, 8, 6));
            cv.ell(0.38, 0.51 + bob, 0.03, 0.04, Cv.rgb(255, 150, 20));
            cv.ell(0.62, 0.51 + bob, 0.03, 0.04, Cv.rgb(255, 150, 20));
            cv.tri(0.47, 0.60 + bob, 0.53, 0.60 + bob, 0.50, 0.55 + bob, Cv.rgb(40, 20, 16));
            double open = atk ? 0.09 : 0.04;
            cv.ell(0.5, 0.74 + bob, 0.17, open, Cv.rgb(50, 6, 6));
            for (int i = -3; i <= 3; i++) {
                cv.tri(0.5 + i * 0.045 - 0.014, 0.74 + bob - open * 0.8, 0.5 + i * 0.045 + 0.014, 0.74 + bob - open * 0.8, 0.5 + i * 0.045, 0.74 + bob + 0.01, Cv.rgb(240, 236, 215));
            }
        }
    }

    private static void pain(Cv cv, double swing, boolean atk) {
        int bd = Cv.rgb(148, 98, 70), dk = Cv.shade(bd, 0.7), bone = Cv.rgb(232, 222, 192);
        double bob = swing * 0.02;
        for (int s = -1; s <= 1; s += 2) { // cuernos largos
            cv.limb(0.5 + s * 0.22, 0.22 + bob, 0.5 + s * 0.36, 0.08 + bob, 0.07, bone);
            cv.limb(0.5 + s * 0.36, 0.08 + bob, 0.5 + s * 0.30, 0.00 + bob, 0.045, bone);
        }
        cv.ell(0.5, 0.52 + bob, 0.43, 0.40, bd);
        if (!back) {
            cv.ell(0.5, 0.60 + bob, 0.30, atk ? 0.25 : 0.19, Cv.rgb(54, 6, 8));
            if (atk) cv.ell(0.5, 0.60 + bob, 0.14, 0.12, Cv.rgb(255, 150, 30));
            for (int i = -4; i <= 4; i++) {
                double x = 0.5 + i * 0.058, top = 0.60 + bob - (atk ? 0.22 : 0.16);
                cv.tri(x - 0.02, top, x + 0.02, top, x, top + 0.07, bone);
            }
            for (int s = -1; s <= 1; s += 2) {
                cv.ell(0.5 + s * 0.17, 0.32 + bob, 0.07, 0.065, Cv.rgb(240, 236, 220));
                cv.ell(0.5 + s * 0.17, 0.33 + bob, 0.035, 0.04, Cv.rgb(220, 40, 20));
                cv.ell(0.5 + s * 0.17, 0.33 + bob, 0.017, 0.025, Cv.rgb(8, 8, 8));
            }
        }
        cv.limb(0.12, 0.62 + bob, 0.04, 0.78 + bob, 0.08, dk);
        cv.limb(0.88, 0.62 + bob, 0.96, 0.78 + bob, 0.08, dk);
    }

    private static void revenant(Cv cv, double swing, boolean atk) {
        int bone = Cv.rgb(226, 216, 186), dk = Cv.shade(bone, 0.65), arm = Cv.rgb(150, 70, 40);
        for (int s = -1; s <= 1; s += 2) { // piernas de hueso
            double lift = Math.max(0, swing * s) * 0.04;
            double fx = 0.5 + s * 0.09 + swing * s * 0.045;
            cv.limb(0.5 + s * 0.06, 0.60, 0.5 + s * 0.08, 0.78, 0.06, bone);
            cv.limb(0.5 + s * 0.08, 0.78, fx, 0.94 - lift, 0.055, bone);
            cv.ell(0.5 + s * 0.08, 0.78, 0.04, 0.035, dk);
            cv.ell(fx + s * 0.02, 0.955 - lift, 0.06, 0.028, dk);
        }
        cv.ell(0.5, 0.52, 0.09, 0.08, dk); // pelvis
        cv.limb(0.5, 0.52, 0.5, 0.32, 0.035, bone); // columna
        cv.ell(0.5, 0.40, 0.17, 0.17, Cv.shade(arm, 0.9)); // peto
        if (!back) for (int i = 0; i < 4; i++) cv.rect(0.38, 0.32 + i * 0.05, 0.62, 0.338 + i * 0.05, bone);
        // lanzamisiles en los hombros
        for (int s = -1; s <= 1; s += 2) {
            cv.rect(0.5 + s * 0.22 - 0.06, 0.18, 0.5 + s * 0.22 + 0.06, 0.40, Cv.rgb(110, 118, 126));
            cv.ell(0.5 + s * 0.22, 0.17, 0.06, 0.03, Cv.rgb(60, 64, 70));
            if (atk) cv.ell(0.5 + s * 0.22, 0.12, 0.05, 0.06, Cv.rgb(255, 170, 50));
        }
        double hy = atk ? 0.30 : 0.60;
        cv.limb(0.30, 0.36, 0.25, hy, 0.05, bone);
        cv.limb(0.70, 0.36, 0.75, atk ? 0.30 : 0.62, 0.05, bone);
        cv.ell(0.25, hy + 0.02, 0.04, 0.04, bone);
        cv.ell(0.75, (atk ? 0.30 : 0.62) + 0.02, 0.04, 0.04, bone);
        // craneo
        cv.ell(0.5, 0.20, 0.10, 0.10, bone);
        cv.rect(0.44, 0.27, 0.56, 0.31, bone);
        if (!back) {
            cv.ell(0.45, 0.20, 0.025, 0.03, Cv.rgb(10, 4, 4));
            cv.ell(0.55, 0.20, 0.025, 0.03, Cv.rgb(10, 4, 4));
            cv.ell(0.45, 0.205, 0.01, 0.012, Cv.rgb(255, 40, 30));
            cv.ell(0.55, 0.205, 0.01, 0.012, Cv.rgb(255, 40, 30));
            for (int i = -2; i <= 2; i++) cv.rect(0.5 + i * 0.022 - 0.006, 0.28, 0.5 + i * 0.022 + 0.006, 0.30, dk);
        }
    }

    private static void mancubus(Cv cv, double swing, boolean atk) {
        int sk = Cv.rgb(196, 146, 104), dk = Cv.shade(sk, 0.68), metal = Cv.rgb(100, 108, 118);
        for (int s = -1; s <= 1; s += 2) {
            double fx = 0.5 + s * 0.16 + swing * s * 0.03;
            cv.limb(0.5 + s * 0.12, 0.72, fx, 0.92, 0.16, dk);
            cv.ell(fx, 0.95, 0.10, 0.04, Cv.rgb(40, 30, 26));
        }
        cv.ell(0.5, 0.56, 0.36, 0.28, sk);
        cv.ell(0.5, 0.62, 0.26, 0.20, Cv.shade(sk, 1.1));
        if (!back) for (int i = 0; i < 5; i++) cv.rect(0.34, 0.42 + i * 0.07, 0.66, 0.432 + i * 0.07, Cv.shade(sk, 0.78));
        // brazos-canon
        for (int s = -1; s <= 1; s += 2) {
            cv.limb(0.5 + s * 0.32, 0.44, 0.5 + s * 0.42, 0.56, 0.12, sk);
            cv.rect(0.5 + s * 0.42 - 0.07, 0.50, 0.5 + s * 0.42 + 0.07, 0.72, metal);
            cv.ell(0.5 + s * 0.42, 0.72, 0.07, 0.03, Cv.rgb(30, 32, 36));
            if (atk) cv.ell(0.5 + s * 0.42, 0.76, 0.07, 0.07, Cv.rgb(255, 160, 40));
        }
        cv.ell(0.5, 0.22, 0.11, 0.11, sk);
        if (!back) {
            cv.rect(0.42, 0.18, 0.58, 0.20, dk);
            eyes(cv, 0.5, 0.215, 0.04, 0.018, Cv.rgb(255, 230, 50));
            cv.ell(0.5, 0.28, 0.05, atk ? 0.035 : 0.02, Cv.rgb(50, 6, 8));
        }
        cv.tri(0.41, 0.15, 0.36, 0.05, 0.46, 0.12, Cv.rgb(232, 222, 192));
        cv.tri(0.59, 0.15, 0.64, 0.05, 0.54, 0.12, Cv.rgb(232, 222, 192));
    }

    private static void archvile(Cv cv, double swing, boolean atk) {
        int sk = Cv.rgb(226, 172, 62), dk = Cv.rgb(150, 100, 30), robe = Cv.rgb(120, 60, 34), bone = Cv.rgb(232, 222, 192);
        legs(cv, swing, dk, Cv.shade(dk, 0.7), 0.62, 0.95, 0.09, 0.9);
        // tunica rasgada
        cv.poly(new double[]{0.33, 0.30, 0.67, 0.30, 0.72, 0.72, 0.62, 0.78, 0.52, 0.70, 0.42, 0.80, 0.30, 0.72}, robe);
        cv.ell(0.5, 0.40, 0.14, 0.15, sk);
        if (!back) for (int i = 0; i < 3; i++) cv.rect(0.43, 0.34 + i * 0.05, 0.57, 0.345 + i * 0.05, Cv.shade(sk, 0.72));
        if (atk) { // brazos en alto con llamas
            cv.limb(0.36, 0.34, 0.20, 0.12, 0.07, sk);
            cv.limb(0.64, 0.34, 0.80, 0.12, 0.07, sk);
            for (int s = -1; s <= 1; s += 2) {
                cv.tri(0.5 + s * 0.30 - 0.07, 0.12, 0.5 + s * 0.30 + 0.07, 0.12, 0.5 + s * 0.30, -0.12, Cv.rgb(255, 120, 20));
                cv.tri(0.5 + s * 0.30 - 0.035, 0.11, 0.5 + s * 0.30 + 0.035, 0.11, 0.5 + s * 0.30, -0.04, Cv.rgb(255, 230, 120));
            }
        } else {
            cv.limb(0.36, 0.34, 0.28, 0.58, 0.07, sk);
            cv.limb(0.64, 0.34, 0.72, 0.58, 0.07, sk);
            claws(cv, 0.27, 0.60, bone);
            claws(cv, 0.73, 0.60, bone);
        }
        cv.ell(0.5, 0.20, 0.09, 0.10, sk);
        cv.tri(0.43, 0.17, 0.30, 0.10, 0.42, 0.24, sk); // orejas puntiagudas
        cv.tri(0.57, 0.17, 0.70, 0.10, 0.58, 0.24, sk);
        eyes(cv, 0.5, 0.20, 0.035, 0.018, Cv.rgb(255, 255, 120));
        if (!back) cv.ell(0.5, 0.255, 0.04, atk ? 0.03 : 0.016, Cv.rgb(70, 10, 8));
    }

    private static void icon(Cv cv, boolean atk) {
        int sk = Cv.rgb(122, 74, 62), dk = Cv.shade(sk, 0.6), bone = Cv.rgb(232, 222, 192);
        cv.rect(0.10, 0.50, 0.90, 0.99, dk); // base de piedra
        cv.ell(0.5, 0.52, 0.42, 0.40, sk);
        cv.ell(0.5, 0.17, 0.30, 0.15, Cv.rgb(214, 120, 140)); // cerebro expuesto
        for (int i = 0; i < 5; i++) cv.limb(0.28 + i * 0.11, 0.12, 0.31 + i * 0.11, 0.22, 0.025, Cv.rgb(170, 80, 100));
        for (int s = -1; s <= 1; s += 2) { // cuernos
            cv.tri(0.5 + s * 0.30, 0.34, 0.5 + s * 0.46, 0.00, 0.5 + s * 0.40, 0.42, bone);
            cv.tri(0.5 + s * 0.38, 0.52, 0.5 + s * 0.50, 0.30, 0.5 + s * 0.46, 0.58, bone);
        }
        if (!back) {
            for (int s = -1; s <= 1; s += 2) {
                cv.ell(0.5 + s * 0.17, 0.45, 0.11, 0.07, Cv.rgb(255, 230, 60));
                cv.ell(0.5 + s * 0.17, 0.45, 0.05, 0.05, Cv.rgb(200, 20, 10));
            }
            cv.rect(0.46, 0.50, 0.54, 0.62, dk);
            double open = atk ? 0.15 : 0.08;
            cv.ell(0.5, 0.76, 0.27, open, Cv.rgb(50, 4, 6));
            for (int i = -5; i <= 5; i++) {
                double x = 0.5 + i * 0.046;
                cv.tri(x - 0.017, 0.76 - open * 0.8, x + 0.017, 0.76 - open * 0.8, x, 0.76 - open * 0.8 + 0.07, bone);
                cv.tri(x - 0.017, 0.76 + open * 0.8, x + 0.017, 0.76 + open * 0.8, x, 0.76 + open * 0.8 - 0.06, bone);
            }
            if (atk) cv.ell(0.5, 0.76, 0.14, 0.09, Cv.rgb(255, 150, 30));
        }
    }
}

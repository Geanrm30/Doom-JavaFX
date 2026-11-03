package doom.entities;

import doom.engine.ColorUtils;

/** Efecto visual de corta duracion: explosion (0), sangre (1) o chispa de impacto (2). */
public class Effect extends Sprite {

    public static final double LIFE = 0.5;
    public final int kind;
    public double age;
    private final double life;

    public Effect(double x, double y) {
        this(x, y, 0);
    }

    public Effect(double x, double y, int kind) {
        this.x = x;
        this.y = y;
        this.kind = kind;
        this.life = kind == 0 ? LIFE : kind == 1 ? 0.28 : 0.16;
        this.lift = kind == 0 ? 0.1 : 0.32;
        this.scale = kind == 0 ? 0.7 : kind == 1 ? 0.2 : 0.12;
    }

    public boolean alive() {
        return age < life;
    }

    public void update(double dt) {
        age += dt;
        double k = age / life;
        scale = switch (kind) {
            case 0 -> 0.7 + 1.1 * k;
            case 1 -> 0.2 + 0.18 * k;
            default -> 0.12 + 0.06 * k;
        };
    }

    @Override
    public int colorAt(double u, double v) {
        double dx = u - 0.5, dy = v - 0.5;
        double d = Math.sqrt(dx * dx + dy * dy) * 2;
        double fade = 1 - age / life;
        if (kind == 1) {
            // salpicadura: gotas sueltas alrededor del centro
            int h = ((int) (u * 9) * 73856093) ^ ((int) (v * 9) * 19349663);
            boolean drop = d < 0.55 || (d < 1.0 && ((h >>> 5) & 3) == 0);
            return drop ? ColorUtils.rgb((int) (190 * fade + 40), 10, 10) : 0;
        }
        if (kind == 2) {
            if (d > 1) return 0;
            double core = 1 - d;
            return ColorUtils.rgb(255, (int) (190 + 65 * core), (int) (80 + 170 * core * core));
        }
        double wob = 1 + 0.18 * Math.sin(u * 17 + v * 13 + age * 30);
        if (d * wob > 1) return 0;
        double core = 1 - d * wob;
        int r = 255;
        int g = (int) (40 + 215 * core * fade);
        int b = (int) (180 * core * core * fade);
        return ColorUtils.rgb(r, g, b);
    }
}

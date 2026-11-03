package doom.entities;

import doom.engine.ColorUtils;

/** Explosion visual de corta duracion. */
public class Effect extends Sprite {

    public static final double LIFE = 0.5;
    public double age;

    public Effect(double x, double y) {
        this.x = x;
        this.y = y;
        this.lift = 0.1;
        this.scale = 0.7;
    }

    public boolean alive() {
        return age < LIFE;
    }

    public void update(double dt) {
        age += dt;
        scale = 0.7 + 1.1 * (age / LIFE);
    }

    @Override
    public int colorAt(double u, double v) {
        double dx = u - 0.5, dy = v - 0.5;
        double d = Math.sqrt(dx * dx + dy * dy) * 2;
        double fade = 1 - age / LIFE;
        double wob = 1 + 0.18 * Math.sin(u * 17 + v * 13 + age * 30);
        if (d * wob > 1) return 0;
        double core = 1 - d * wob;
        int r = 255;
        int g = (int) (40 + 215 * core * fade);
        int b = (int) (180 * core * core * fade);
        return ColorUtils.rgb(r, g, b);
    }
}

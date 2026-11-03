package doom.entities;

import doom.engine.ColorUtils;

/** Barril explosivo: no bloquea el paso, explota al recibir un disparo. */
public class Barrel extends Sprite {

    public boolean alive = true;
    public int hp = 20;

    public Barrel(double x, double y) {
        this.x = x;
        this.y = y;
        this.scale = 0.55;
    }

    @Override
    public int colorAt(double u, double v) {
        double cx = u - 0.5;
        if (Math.abs(cx) > 0.3 || v < 0.15) return 0;
        double bulge = 0.3 - 0.03 * Math.abs(v - 0.55) * 4;
        if (Math.abs(cx) > bulge) return 0;
        boolean ring = (v > 0.28 && v < 0.33) || (v > 0.72 && v < 0.77) || v > 0.97;
        if (ring) return ColorUtils.rgb(40, 60, 35);
        boolean hazard = v > 0.45 && v < 0.6;
        if (hazard) return ((int) ((u + v) * 22)) % 2 == 0 ? ColorUtils.rgb(230, 200, 30) : ColorUtils.rgb(30, 30, 30);
        int shade = (int) (30 * Math.abs(cx) / 0.3);
        return ColorUtils.rgb(80 - shade, 140 - shade, 60 - shade);
    }
}

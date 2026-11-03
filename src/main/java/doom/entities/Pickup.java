package doom.entities;

import doom.engine.ColorUtils;

public class Pickup extends Sprite {

    public static final int HEALTH = 0;
    public static final int AMMO = 1;

    public final int type;

    public Pickup(double x, double y, int type) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.scale = 0.35;
    }

    @Override
    public int colorAt(double u, double v) {
        double cx = u - 0.5;
        if (v < 0.35 || Math.abs(cx) > 0.45) return 0;
        if (type == HEALTH) {
            boolean cross = (Math.abs(cx) < 0.09 && v > 0.45 && v < 0.9)
                    || (v > 0.58 && v < 0.74 && Math.abs(cx) < 0.28);
            return cross ? ColorUtils.rgb(220, 30, 30) : ColorUtils.rgb(235, 235, 235);
        }
        boolean band = v > 0.55 && v < 0.65;
        return band ? ColorUtils.rgb(40, 40, 40) : ColorUtils.rgb(200, 170, 40);
    }
}

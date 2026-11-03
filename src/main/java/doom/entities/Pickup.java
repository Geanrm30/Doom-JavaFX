package doom.entities;

import doom.engine.ColorUtils;

public class Pickup extends Sprite {

    public static final int HEALTH = 0, STIM = 1, ARMOR = 2, CLIP = 3, SHELLS = 4,
            SHOTGUN = 5, CHAINGUN = 6, RED_KEY = 7, BLUE_KEY = 8, MEGA = 9, INVULN = 10;

    public final int type;

    public Pickup(double x, double y, int type) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.scale = switch (type) {
            case STIM, CLIP -> 0.28;
            case SHOTGUN, CHAINGUN -> 0.45;
            case RED_KEY, BLUE_KEY -> 0.35;
            default -> 0.35;
        };
        if (type == RED_KEY || type == BLUE_KEY) this.lift = 0.12;
        if (type == MEGA || type == INVULN) {
            this.scale = 0.38;
            this.lift = 0.1;
        }
    }

    public static int fromChar(char c) {
        return switch (c) {
            case 'h' -> HEALTH;
            case 's' -> STIM;
            case 'a' -> ARMOR;
            case 'k' -> CLIP;
            case 'e' -> SHELLS;
            case 'g' -> SHOTGUN;
            case 'm' -> CHAINGUN;
            case 'r' -> RED_KEY;
            case 'u' -> BLUE_KEY;
            case 'H' -> MEGA;
            case 'v' -> INVULN;
            default -> -1;
        };
    }

    @Override
    public int colorAt(double u, double v) {
        double cx = u - 0.5;
        if (v < 0.35 || Math.abs(cx) > 0.45) return 0;
        switch (type) {
            case HEALTH -> {
                boolean cross = (Math.abs(cx) < 0.09 && v > 0.45 && v < 0.9)
                        || (v > 0.58 && v < 0.74 && Math.abs(cx) < 0.28);
                return cross ? ColorUtils.rgb(220, 30, 30) : ColorUtils.rgb(235, 235, 235);
            }
            case STIM -> {
                if (Math.abs(cx) > 0.2 || v < 0.45) return 0;
                if (v < 0.55) return ColorUtils.rgb(200, 200, 210);
                return v > 0.62 && v < 0.8 ? ColorUtils.rgb(60, 140, 255) : ColorUtils.rgb(170, 200, 255);
            }
            case ARMOR -> {
                double w = 0.42 - Math.max(0, v - 0.65) * 1.0;
                if (Math.abs(cx) > w) return 0;
                boolean edge = Math.abs(cx) > w - 0.05 || v < 0.4;
                return edge ? ColorUtils.rgb(30, 110, 50) : ColorUtils.rgb(60, 190, 90);
            }
            case CLIP -> {
                boolean band = v > 0.55 && v < 0.65;
                return band ? ColorUtils.rgb(40, 40, 40) : ColorUtils.rgb(200, 170, 40);
            }
            case SHELLS -> {
                boolean gap = ((int) ((cx + 0.5) * 12)) % 3 == 2;
                if (gap) return 0;
                return v > 0.75 ? ColorUtils.rgb(190, 150, 60) : ColorUtils.rgb(200, 40, 40);
            }
            case SHOTGUN, CHAINGUN -> {
                if (v < 0.5 || v > 0.78) return 0;
                if (type == SHOTGUN) {
                    return u > 0.7 ? ColorUtils.rgb(120, 75, 40)
                            : (v < 0.58 ? ColorUtils.rgb(90, 90, 95) : ColorUtils.rgb(60, 60, 65));
                }
                return ((int) (u * 14)) % 2 == 0 ? ColorUtils.rgb(70, 70, 78) : ColorUtils.rgb(40, 40, 46);
            }
            case MEGA, INVULN -> {
                double dx = u - 0.5, dy = v - 0.62;
                double d = Math.sqrt(dx * dx + dy * dy);
                if (d > 0.3) return 0;
                double glow = 1 - d / 0.3;
                if (type == MEGA) {
                    return ColorUtils.rgb((int) (40 + 60 * glow), (int) (90 + 160 * glow), (int) (255 * (0.6 + 0.4 * glow)));
                }
                return ColorUtils.rgb((int) (90 + 160 * glow), (int) (255 * (0.5 + 0.5 * glow)), (int) (90 + 80 * glow));
            }
            default -> {
                // tarjetas llave
                if (Math.abs(cx) > 0.22 || v < 0.45 || v > 0.9) return 0;
                if (v > 0.55 && v < 0.62) return ColorUtils.rgb(240, 240, 240);
                return type == RED_KEY ? ColorUtils.rgb(220, 30, 30) : ColorUtils.rgb(40, 80, 230);
            }
        }
    }
}

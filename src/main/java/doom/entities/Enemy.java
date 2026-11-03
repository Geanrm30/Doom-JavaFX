package doom.entities;

import doom.engine.ColorUtils;
import doom.world.GameMap;
import doom.world.Player;

import java.util.Random;

public class Enemy extends Sprite {

    private static final double SPEED = 1.3;
    private static final Random RND = new Random();

    public int hp = 60;
    public boolean alive = true;
    public boolean awake = false;
    public double hurt, cooldown;

    public Enemy(double x, double y) {
        this.x = x;
        this.y = y;
        this.scale = 0.9;
    }

    public void hit(int damage) {
        hp -= damage;
        hurt = 0.15;
        awake = true;
        if (hp <= 0) alive = false;
    }

    public void update(double dt, Player player) {
        if (!alive) return;
        if (hurt > 0) hurt -= dt;
        if (cooldown > 0) cooldown -= dt;

        double dx = player.x - x, dy = player.y - y;
        double dist = Math.hypot(dx, dy);
        boolean los = dist < 14 && GameMap.lineOfSight(x, y, player.x, player.y);
        if (los) awake = true;
        if (!awake) return;

        if (dist > 1.1) {
            double sp = SPEED * dt;
            double nx = x + dx / dist * sp;
            double ny = y + dy / dist * sp;
            if (!GameMap.isWall((int) nx, (int) y)) x = nx;
            if (!GameMap.isWall((int) x, (int) ny)) y = ny;
        }

        if (cooldown <= 0) {
            if (dist < 1.5) {
                player.damage(10);
                cooldown = 1.0;
            } else if (los && dist < 8) {
                if (RND.nextDouble() < 0.5) player.damage(6);
                cooldown = 1.6;
            }
        }
    }

    @Override
    public int colorAt(double u, double v) {
        if (!alive) {
            if (v > 0.86 && Math.abs(u - 0.5) < 0.38) return ColorUtils.rgb(110, 10, 10);
            return 0;
        }
        boolean flash = hurt > 0;
        int skinR = flash ? 255 : 160, skinG = flash ? 220 : 80, skinB = flash ? 220 : 55;
        double cx = u - 0.5;

        if (v > 0.03 && v < 0.11 && Math.abs(cx) > 0.08 && Math.abs(cx) < 0.15)
            return ColorUtils.rgb(225, 215, 190);
        double hx = cx, hy = v - 0.2;
        if (hx * hx + hy * hy < 0.13 * 0.13) {
            double ex = Math.abs(cx) - 0.05, ey = v - 0.18;
            if (ex * ex + ey * ey < 0.028 * 0.028) return ColorUtils.rgb(255, 230, 40);
            if (v > 0.245 && v < 0.275 && Math.abs(cx) < 0.06) return ColorUtils.rgb(40, 5, 5);
            return ColorUtils.rgb(skinR, skinG, skinB);
        }
        if (v > 0.33 && v < 0.65 && Math.abs(cx) < 0.16) {
            boolean rib = ((int) (v * 60)) % 4 == 0;
            return rib ? ColorUtils.rgb(skinR - 50, skinG - 30, skinB - 20) : ColorUtils.rgb(skinR, skinG, skinB);
        }
        if (v > 0.34 && v < 0.6 && Math.abs(cx) >= 0.16 && Math.abs(cx) < 0.24)
            return ColorUtils.rgb(skinR - 35, skinG - 20, skinB - 10);
        if (v >= 0.6 && v < 0.67 && Math.abs(cx) >= 0.17 && Math.abs(cx) < 0.23)
            return ColorUtils.rgb(235, 230, 210);
        if (v >= 0.65 && v < 0.97 && Math.abs(cx) > 0.03 && Math.abs(cx) < 0.15)
            return ColorUtils.rgb(skinR - 70, skinG - 40, skinB - 25);
        return 0;
    }
}

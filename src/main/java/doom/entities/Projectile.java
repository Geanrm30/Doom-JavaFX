package doom.entities;

import doom.engine.ColorUtils;
import doom.world.World;

/** Bola de fuego lanzada por un enemigo. */
public class Projectile extends Sprite {

    public final double vx, vy;
    public final int damage;
    public boolean alive = true;
    private double age;

    public Projectile(double x, double y, double dirX, double dirY, double speed, int damage) {
        this.x = x;
        this.y = y;
        this.vx = dirX * speed;
        this.vy = dirY * speed;
        this.damage = damage;
        this.scale = 0.28;
        this.lift = 0.32;
    }

    public void update(double dt, World w) {
        age += dt;
        double nx = x + vx * dt, ny = y + vy * dt;
        if (w.level.isWall((int) nx, (int) ny) || age > 8) {
            alive = false;
            return;
        }
        x = nx;
        y = ny;
        if (Math.hypot(w.player.x - x, w.player.y - y) < 0.4) {
            alive = false;
            w.hurtPlayer(damage);
        }
    }

    @Override
    public int colorAt(double u, double v) {
        double dx = u - 0.5, dy = v - 0.5;
        double d = Math.sqrt(dx * dx + dy * dy);
        if (d > 0.5) return 0;
        double t = 1 - d / 0.5;
        double flick = 0.85 + 0.15 * Math.sin(age * 30);
        return ColorUtils.rgb((int) (255 * flick), (int) (60 + 190 * t * t), (int) (30 * t));
    }
}

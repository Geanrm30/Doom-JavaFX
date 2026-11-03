package doom.entities;

import doom.engine.ColorUtils;
import doom.world.World;

/** Proyectil: bola de fuego enemiga (kind 0) o disparo del jugador (1 cohete, 2 plasma). */
public class Projectile extends Sprite {

    public double vx, vy;
    public final int damage;
    public final int kind;
    public boolean alive = true;
    private double age;

    public Projectile(double x, double y, double dirX, double dirY, double speed, int damage) {
        this(x, y, dirX, dirY, speed, damage, 0);
    }

    public Projectile(double x, double y, double dirX, double dirY, double speed, int damage, int kind) {
        this.x = x;
        this.y = y;
        this.vx = dirX * speed;
        this.vy = dirY * speed;
        this.damage = damage;
        this.kind = kind;
        this.scale = kind == 2 ? 0.2 : kind == 1 ? 0.26 : kind == 3 ? 0.3 : 0.28;
        this.lift = kind == 0 || kind == 3 ? 0.32 : 0.28;
    }

    public void update(double dt, World w) {
        age += dt;
        if (kind == 0 || kind == 3) {
            updateEnemyShot(dt, w);
        } else {
            updatePlayerShot(dt, w);
        }
    }

    private void updateEnemyShot(double dt, World w) {
        if (kind == 3) {
            // misil teledirigido: gira poco a poco hacia el jugador
            double sp = Math.hypot(vx, vy), a = Math.atan2(vy, vx);
            double want = Math.atan2(w.player.y - y, w.player.x - x);
            double diff = want - a;
            while (diff > Math.PI) diff -= 2 * Math.PI;
            while (diff < -Math.PI) diff += 2 * Math.PI;
            a += Math.max(-1.4 * dt, Math.min(1.4 * dt, diff));
            vx = Math.cos(a) * sp;
            vy = Math.sin(a) * sp;
        }
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

    private void updatePlayerShot(double dt, World w) {
        int steps = 3;
        for (int i = 0; i < steps && alive; i++) {
            double nx = x + vx * dt / steps, ny = y + vy * dt / steps;
            if (w.level.isWall((int) nx, (int) ny) || age > 6) {
                impact(w, null);
                return;
            }
            x = nx;
            y = ny;
            for (Enemy e : w.enemies) {
                if (e.alive && Math.hypot(e.x - x, e.y - y) < e.type.radius + 0.12) {
                    impact(w, e);
                    return;
                }
            }
            for (Barrel b : w.barrels) {
                if (b.alive && Math.hypot(b.x - x, b.y - y) < 0.35) {
                    impact(w, null);
                    w.damageBarrel(b, damage);
                    return;
                }
            }
        }
    }

    private void impact(World w, Enemy e) {
        alive = false;
        if (kind == 1) {
            w.blast(x - vx * 0.02, y - vy * 0.02, 2.6, damage);
        } else if (e != null) {
            w.damageEnemy(e, damage);
        }
    }

    @Override
    public int colorAt(double u, double v) {
        double dx = u - 0.5, dy = v - 0.5;
        double d = Math.sqrt(dx * dx + dy * dy);
        if (d > 0.5) return 0;
        double t = 1 - d / 0.5;
        double flick = 0.85 + 0.15 * Math.sin(age * 30);
        if (kind == 2) {
            return ColorUtils.rgb((int) (30 + 200 * t * t * t), (int) (120 + 130 * t * t), (int) (255 * flick));
        }
        if (kind == 1) {
            return ColorUtils.rgb((int) (255 * flick), (int) (120 + 135 * t * t), (int) (40 + 190 * t * t * t));
        }
        if (kind == 3) {
            return ColorUtils.rgb((int) (255 * flick), (int) (110 + 120 * t * t), (int) (60 + 150 * t * t * t));
        }
        return ColorUtils.rgb((int) (255 * flick), (int) (60 + 190 * t * t), (int) (30 * t));
    }
}

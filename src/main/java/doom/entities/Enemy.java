package doom.entities;

import doom.engine.ColorUtils;
import doom.engine.SoundFx;
import doom.world.Player;
import doom.world.World;

import java.util.Random;

public class Enemy extends Sprite {

    private static final Random RND = new Random();

    public final EnemyType type;
    public int hp;
    public boolean alive = true;
    public boolean awake = false;
    public double hurt, cooldown;

    public Enemy(EnemyType type, double x, double y) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.hp = type.hp;
        this.scale = type.scale;
        this.lift = type.lift;
        this.cooldown = 0.5 + RND.nextDouble();
    }

    /** @return true si este golpe lo mato */
    public boolean hit(int damage) {
        if (!alive) return false;
        hp -= damage;
        hurt = 0.15;
        awake = true;
        if (hp <= 0) {
            alive = false;
            return true;
        }
        return false;
    }

    public void update(double dt, World w) {
        if (!alive) return;
        Player p = w.player;
        if (hurt > 0) hurt -= dt;
        if (cooldown > 0) cooldown -= dt;

        double dx = p.x - x, dy = p.y - y;
        double dist = Math.hypot(dx, dy);
        boolean los = dist < 16 && w.level.lineOfSight(x, y, p.x, p.y);
        if (los) awake = true;
        if (!awake) return;

        double stop = switch (type) {
            case ZOMBIE -> 3.5;
            case CACO -> 2.4;
            case CYBER -> 3.0;
            default -> 1.1;
        };
        if (dist > stop) {
            double sp = type.speed * dt;
            double nx = x + dx / dist * sp;
            double ny = y + dy / dist * sp;
            if (canStand(w, nx, y)) x = nx;
            if (canStand(w, x, ny)) y = ny;
        }

        if (cooldown > 0) return;
        if (type.meleeDmg > 0 && dist < 1.3) {
            w.hurtPlayer(type.meleeDmg);
            cooldown = type.meleeCd;
        } else if (type.ranged != EnemyType.Ranged.NONE && los && dist < 13) {
            if (type.ranged == EnemyType.Ranged.HITSCAN) {
                w.sound.play(SoundFx.ZOMBIE_SHOT);
                double chance = Math.max(0.2, 0.8 - dist * 0.045);
                if (RND.nextDouble() < chance) w.hurtPlayer(type.rangedDmg + RND.nextInt(4));
            } else {
                double speed = type.isBoss() ? 6.0 : type == EnemyType.BARON ? 5.5 : 4.2;
                w.sound.play(SoundFx.FIREBALL);
                double[] spread = type == EnemyType.CYBER ? new double[]{-0.2, 0, 0.2} : new double[]{0};
                for (double s : spread) {
                    double c = Math.cos(s), sn = Math.sin(s);
                    double ux = dx / dist, uy = dy / dist;
                    w.projectiles.add(new Projectile(x, y, ux * c - uy * sn, ux * sn + uy * c, speed,
                            type.rangedDmg + RND.nextInt(5)));
                }
            }
            cooldown = type.rangedCd * (0.8 + RND.nextDouble() * 0.5);
        }
    }

    private boolean canStand(World w, double nx, double ny) {
        double r = 0.25;
        if (w.level.isWall((int) (nx + r), (int) ny) || w.level.isWall((int) (nx - r), (int) ny)
                || w.level.isWall((int) nx, (int) (ny + r)) || w.level.isWall((int) nx, (int) (ny - r))) {
            return false;
        }
        for (Enemy o : w.enemies) {
            if (o != this && o.alive && Math.abs(o.x - nx) < 0.55 && Math.abs(o.y - ny) < 0.55) return false;
        }
        return true;
    }

    @Override
    public int colorAt(double u, double v) {
        if (!alive) {
            if (v > 0.86 && Math.abs(u - 0.5) < 0.38) {
                return type == EnemyType.BARON || type == EnemyType.BOSS_BARON ? ColorUtils.rgb(20, 90, 20) : ColorUtils.rgb(110, 10, 10);
            }
            return 0;
        }
        return type == EnemyType.CACO ? cacodemon(u, v) : humanoid(u, v);
    }

    private int tint(int base, int sub) {
        boolean flash = hurt > 0;
        int r = (base >> 16) & 0xFF, g = (base >> 8) & 0xFF, b = base & 0xFF;
        if (flash) return ColorUtils.rgb(255, 220, 220);
        return ColorUtils.rgb(r - sub, g - sub, b - sub);
    }

    private int cacodemon(double u, double v) {
        double cx = u - 0.5, cy = v - 0.5;
        double d = Math.sqrt(cx * cx + cy * cy);
        if (d > 0.46) {
            // pequenos cuernos
            if (v < 0.12 && Math.abs(Math.abs(cx) - 0.2) < 0.04) return tint(type.body, 90);
            return 0;
        }
        double ex = cx, ey = cy + 0.02;
        double ed = Math.sqrt(ex * ex + ey * ey);
        if (ed < 0.16) {
            if (ed < 0.04) return ColorUtils.rgb(10, 10, 10);
            if (ed < 0.09) return hurt > 0 ? ColorUtils.rgb(255, 255, 255) : ColorUtils.rgb(70, 200, 70);
            return ColorUtils.rgb(240, 235, 220);
        }
        if (cy > 0.12 && Math.abs(cx) < 0.28 && cy < 0.12 + 0.2 * (1 - Math.abs(cx) / 0.28)) {
            boolean teeth = ((int) ((cx + 0.5) * 40)) % 4 == 0;
            return teeth ? ColorUtils.rgb(235, 230, 210) : ColorUtils.rgb(50, 5, 5);
        }
        int sub = (int) (60 * (d / 0.46) * (d / 0.46)) + (cy > 0.2 ? 30 : 0);
        return tint(type.body, sub);
    }

    private int humanoid(double u, double v) {
        double bulk = type.bulk;
        int skin = type.body, head = type.head;
        double cx = u - 0.5;
        double ax = Math.abs(cx);

        if (type.horns && v > 0.03 && v < 0.11 && ax > 0.08 && ax < 0.15) return ColorUtils.rgb(225, 215, 190);
        double hx = cx, hy = v - 0.2;
        if (hx * hx + hy * hy < 0.13 * 0.13) {
            double ex = ax - 0.05, ey = v - 0.18;
            if (ex * ex + ey * ey < 0.028 * 0.028) {
                return type == EnemyType.ZOMBIE ? ColorUtils.rgb(30, 30, 30) : ColorUtils.rgb(255, 230, 40);
            }
            if (v > 0.245 && v < 0.275 && ax < 0.06) return ColorUtils.rgb(40, 5, 5);
            if (type == EnemyType.ZOMBIE && v < 0.12) return ColorUtils.rgb(80, 80, 70); // casco
            return tint(head, 0);
        }
        if (v > 0.33 && v < 0.65 && ax < 0.16 * bulk) {
            boolean rib = ((int) (v * 60)) % 4 == 0;
            return tint(skin, rib ? 50 : 0);
        }
        if (v > 0.34 && v < 0.6 && ax >= 0.16 * bulk && ax < 0.24 * bulk) return tint(skin, 35);
        if (v >= 0.6 && v < 0.67 && ax >= 0.17 * bulk && ax < 0.23 * bulk) {
            if (type == EnemyType.ZOMBIE) {
                // fusil
                return ColorUtils.rgb(60, 60, 65);
            }
            return ColorUtils.rgb(235, 230, 210);
        }
        if (v >= 0.65 && v < 0.97 && ax > 0.03 && ax < 0.15 * Math.min(bulk, 1.2)) return tint(skin, 70);
        return 0;
    }
}

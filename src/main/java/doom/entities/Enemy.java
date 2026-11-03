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
    /** Animacion: fase de caminata, tiempo de ataque restante y tiempo desde la muerte. */
    private double walkPhase, attackAnim, deathTime, painGap;
    private boolean sighted, painPending;

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
        if (hp > 0 && RND.nextDouble() < 0.45) painPending = true;
        if (hp <= 0) {
            alive = false;
            lift = 0;
            return true;
        }
        return false;
    }

    public void update(double dt, World w) {
        if (!alive) {
            deathTime += dt;
            return;
        }
        Player p = w.player;
        if (hurt > 0) hurt -= dt;
        if (cooldown > 0) cooldown -= dt;
        if (attackAnim > 0) attackAnim -= dt;
        if (painGap > 0) painGap -= dt;
        if (painPending && painGap <= 0) {
            w.sound.play(painSound(), x, y);
            painGap = 0.5;
        }
        painPending = false;

        double dx = p.x - x, dy = p.y - y;
        double dist = Math.hypot(dx, dy);
        boolean los = dist < 16 && w.level.lineOfSight(x, y, p.x, p.y);
        if (los) awake = true;
        if (!awake) return;
        if (!sighted) {
            sighted = true;
            w.sound.play(sightSound(), x, y);
        }

        double stop = switch (type) {
            case ZOMBIE -> 3.5;
            case CACO -> 2.4;
            case CYBER -> 3.0;
            default -> 1.1;
        };
        boolean direct = dist < 1.6 || (los && w.level.flowAt((int) x, (int) y) <= dist * 1.5 + 2);
        double tx = p.x, ty = p.y;
        if (!direct) {
            int[] next = w.level.nextStep((int) x, (int) y);
            if (next != null) {
                tx = next[0] + 0.5;
                ty = next[1] + 0.5;
                if (w.level.isWall(next[0], next[1]) && w.level.cell(next[0], next[1]) == 'D') {
                    if (w.level.openness(next[0], next[1]) == 0) w.sound.play(SoundFx.DOOR, next[0] + 0.5, next[1] + 0.5);
                    w.level.open(next[0], next[1]);
                }
            }
        }
        double mdx = tx - x, mdy = ty - y, md = Math.hypot(mdx, mdy);
        if (md > 1e-6 && (!direct || dist > stop)) {
            double sp = type.speed * dt;
            double nx = x + mdx / md * sp;
            double ny = y + mdy / md * sp;
            double ox = x, oy = y;
            if (canStand(w, nx, y)) x = nx;
            if (canStand(w, x, ny)) y = ny;
            if (Math.hypot(x - ox, y - oy) < sp * 0.3) {
                // atascado (otro enemigo o una esquina): despierta a los vecinos y prueba a rodear por los lados
                for (Enemy o : w.enemies) if (o != this && o.alive && Math.hypot(o.x - x, o.y - y) < 1.2) o.awake = true;
                double base = Math.atan2(mdy, mdx);
                for (double turn : new double[]{0.8, -0.8, 1.57, -1.57, 2.4, -2.4}) {
                    double ax = x + Math.cos(base + turn) * sp, ay = y + Math.sin(base + turn) * sp;
                    if (canStand(w, ax, ay)) { x = ax; y = ay; break; }
                }
            }
            walkPhase += Math.hypot(x - ox, y - oy) * 2.2;
        }

        if (cooldown > 0) return;
        if (type.meleeDmg > 0 && dist < 1.3) {
            w.sound.play(type == EnemyType.DEMON ? SoundFx.BITE : SoundFx.CLAW, x, y);
            w.hurtPlayer(type.meleeDmg);
            cooldown = type.meleeCd;
            attackAnim = 0.35;
        } else if (type.ranged != EnemyType.Ranged.NONE && los && dist < 13) {
            if (type.ranged == EnemyType.Ranged.HITSCAN) {
                w.sound.play(SoundFx.ZOMBIE_SHOT, x, y);
                double chance = Math.max(0.2, 0.8 - dist * 0.045);
                if (RND.nextDouble() < chance) w.hurtPlayer(type.rangedDmg + RND.nextInt(4));
            } else {
                double speed = type.isBoss() ? 6.0 : type == EnemyType.BARON ? 5.5 : 4.2;
                w.sound.play(SoundFx.FIREBALL, x, y);
                double[] spread = type == EnemyType.CYBER ? new double[]{-0.2, 0, 0.2} : new double[]{0};
                for (double s : spread) {
                    double c = Math.cos(s), sn = Math.sin(s);
                    double ux = dx / dist, uy = dy / dist;
                    w.projectiles.add(new Projectile(x, y, ux * c - uy * sn, ux * sn + uy * c, speed,
                            type.rangedDmg + RND.nextInt(5)));
                }
            }
            cooldown = type.rangedCd * (0.8 + RND.nextDouble() * 0.5);
            attackAnim = 0.4;
        }
    }

    private boolean canStand(World w, double nx, double ny) {
        double r = 0.25;
        if (w.level.isWall((int) (nx + r), (int) ny) || w.level.isWall((int) (nx - r), (int) ny)
                || w.level.isWall((int) nx, (int) (ny + r)) || w.level.isWall((int) nx, (int) (ny - r))) {
            return false;
        }
        for (Enemy o : w.enemies) {
            if (o != this && o.alive && Math.abs(o.x - nx) < 0.55 && Math.abs(o.y - ny) < 0.55
                    && Math.hypot(o.x - nx, o.y - ny) < Math.hypot(o.x - x, o.y - y)) return false; // solo impide acercarse
        }
        return true;
    }

    private int sightSound() {
        return switch (type) {
            case ZOMBIE -> SoundFx.SIGHT_HUMAN;
            case IMP -> SoundFx.SIGHT_IMP;
            case DEMON -> SoundFx.SIGHT_DEMON;
            case CACO -> SoundFx.SIGHT_CACO;
            case BARON -> SoundFx.SIGHT_DEMON;
            default -> SoundFx.SIGHT_BOSS;
        };
    }

    private int painSound() {
        return switch (type) {
            case ZOMBIE -> SoundFx.PAIN_HUMAN;
            case IMP -> SoundFx.PAIN_IMP;
            case DEMON -> SoundFx.PAIN_DEMON;
            case CACO -> SoundFx.PAIN_CACO;
            case BARON -> SoundFx.PAIN_DEMON;
            default -> SoundFx.PAIN_BOSS;
        };
    }

    private int frame() {
        if (!alive) {
            return deathTime < 0.18 ? EnemySprites.DIE1 : deathTime < 0.38 ? EnemySprites.DIE2 : EnemySprites.CORPSE;
        }
        if (attackAnim > 0) return EnemySprites.ATTACK;
        if (!awake) return EnemySprites.STAND;
        int step = (int) walkPhase % 2;
        return walkPhase < 0.01 ? EnemySprites.STAND : step == 0 ? EnemySprites.WALK1 : EnemySprites.WALK2;
    }

    @Override
    public int colorAt(double u, double v) {
        int c = EnemySprites.pixel(type, frame(), u, v);
        if (c != 0 && hurt > 0 && alive) {
            int r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, b = c & 0xFF;
            return ColorUtils.rgb(Math.min(255, r + 110), Math.min(255, g + 70), Math.min(255, b + 70));
        }
        return c;
    }
}

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
    /** Invocado por un Elemental del dolor o el Icono: no cuenta para el total de enemigos. */
    public boolean summoned;
    private double chargeT, chargeDx, chargeDy, flameT, burstT;
    private boolean chargeHit;
    private int burst;
    /** Hacia donde mira el enemigo (radianes) y la vista elegida para la camara. */
    private double facing;
    private int view;
    private boolean mirror;

    public Enemy(EnemyType type, double x, double y) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.hp = type.hp;
        this.scale = type.scale;
        this.lift = type.lift;
        this.cooldown = 0.5 + RND.nextDouble();
        this.facing = RND.nextDouble() * Math.PI * 2;
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
        double ss = w.level.speedScale;
        boolean los = dist < 16 * ss && w.level.lineOfSight(x, y, p.x, p.y);
        if (los) awake = true;
        if (!awake) return;
        if (!sighted) {
            sighted = true;
            w.sound.play(sightSound(), x, y);
        }

        if (chargeT > 0) {
            charge(dt, w, p);
            return;
        }
        double stop = switch (type) {
            case ZOMBIE, SS -> 3.5;
            case SHOTGUNNER -> 3.0;
            case CHAINGUNNER -> 4.0;
            case CACO -> 2.4;
            case CYBER, MANCUBUS -> 3.0;
            case PAIN -> 3.5;
            case ARCHVILE -> 5.0;
            default -> 1.1;
        };
        if (los) facing = Math.atan2(dy, dx);
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
        if (!los && md > 1e-6) facing = Math.atan2(mdy, mdx);
        if (type.speed > 0 && md > 1e-6 && (!direct || dist > stop)) {
            double sp = type.speed * ss * dt;
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

        if (burst > 0) {
            burstT -= dt;
            if (burstT <= 0) {
                burst--;
                burstT = 0.11;
                attackAnim = 0.16;
                w.sound.play(SoundFx.CHAINGUN, x, y);
                if (los && RND.nextDouble() < Math.max(0.15, 0.7 - dist * 0.04)) w.hurtPlayer(type.rangedDmg + RND.nextInt(3));
            }
        }
        if (flameT > 0) {
            flameT -= dt;
            if (flameT <= 0 && los && dist < 16) {
                w.sound.play(SoundFx.EXPLODE, p.x, p.y);
                w.effects.add(new Effect(p.x - Math.cos(facing) * 0.4, p.y - Math.sin(facing) * 0.4, 0));
                w.hurtPlayer(type.rangedDmg + RND.nextInt(6));
            }
        }
        if (cooldown > 0) return;
        if (type.meleeDmg > 0 && dist < 1.3) {
            w.sound.play(type == EnemyType.DEMON || type == EnemyType.SPECTRE ? SoundFx.BITE : SoundFx.CLAW, x, y);
            w.hurtPlayer(type.meleeDmg);
            cooldown = type.meleeCd;
            attackAnim = 0.35;
        } else if (type.ranged != EnemyType.Ranged.NONE && (type.ranged == EnemyType.Ranged.SPAWN || los)
                && dist < ss * (type.ranged == EnemyType.Ranged.LUNGE ? 8 : type.ranged == EnemyType.Ranged.FLAME ? 14 : 13)) {
            double ux = dx / dist, uy = dy / dist;
            switch (type.ranged) {
                case HITSCAN -> {
                    w.sound.play(SoundFx.ZOMBIE_SHOT, x, y);
                    double chance = Math.max(0.2, 0.8 - dist * 0.045);
                    if (RND.nextDouble() < chance) w.hurtPlayer(type.rangedDmg + RND.nextInt(4));
                }
                case SHOTGUN -> {
                    w.sound.play(SoundFx.SHOTGUN, x, y);
                    double chance = Math.max(0.15, 0.7 - dist * 0.05);
                    for (int i = 0; i < 3; i++) if (RND.nextDouble() < chance) w.hurtPlayer(type.rangedDmg + RND.nextInt(4));
                }
                case BURST -> {
                    burst = 4;
                    burstT = 0;
                }
                case FIREBALL, SPREAD, MISSILE -> {
                    boolean knight = type == EnemyType.HELL_KNIGHT;
                    double speed = type.isBoss() ? 6.0 : (type == EnemyType.BARON || knight) ? 5.5
                            : type == EnemyType.REVENANT ? 3.6 : type == EnemyType.MANCUBUS ? 4.6 : 4.2;
                    w.sound.play(type == EnemyType.REVENANT ? SoundFx.ROCKET : SoundFx.FIREBALL, x, y);
                    double[] spread = type == EnemyType.CYBER || type == EnemyType.MANCUBUS ? new double[]{-0.22, 0, 0.22}
                            : new double[]{0};
                    for (double sp : spread) {
                        double c = Math.cos(sp), sn = Math.sin(sp);
                        w.projectiles.add(new Projectile(x, y, ux * c - uy * sn, ux * sn + uy * c, speed * ss,
                                type.rangedDmg + RND.nextInt(5), type == EnemyType.REVENANT ? 3 : 0));
                    }
                }
                case LUNGE -> {
                    w.sound.play(SoundFx.SIGHT_CACO, x, y);
                    chargeT = 0.9;
                    chargeDx = ux;
                    chargeDy = uy;
                    chargeHit = false;
                }
                case FLAME -> {
                    w.sound.play(SoundFx.SIGHT_IMP, x, y);
                    flameT = 0.9;
                    attackAnim = 0.9;
                }
                case SPAWN -> spawn(w, ux, uy);
                default -> { }
            }
            cooldown = type.rangedCd * (0.8 + RND.nextDouble() * 0.5);
            if (flameT <= 0) attackAnim = 0.4;
        }
    }

    /** Alma perdida: embestida en linea recta; hace dano al tocar al jugador. */
    private void charge(double dt, World w, Player p) {
        chargeT -= dt;
        attackAnim = 0.1;
        double nx = x + chargeDx * 9.0 * w.level.speedScale * dt, ny = y + chargeDy * 9.0 * w.level.speedScale * dt;
        if (w.level.isWall((int) nx, (int) ny)) {
            chargeT = 0;
            return;
        }
        x = nx;
        y = ny;
        if (!chargeHit && Math.hypot(p.x - x, p.y - y) < 0.65) {
            chargeHit = true;
            chargeT = Math.min(chargeT, 0.15);
            w.sound.play(SoundFx.BITE, x, y);
            w.hurtPlayer(type.rangedDmg + RND.nextInt(5));
        }
    }

    /** Elemental del dolor: escupe almas perdidas. Icono del pecado: invoca demonios. */
    private void spawn(World w, double ux, double uy) {
        int alive = 0;
        for (Enemy o : w.enemies) if (o.alive && o.summoned) alive++;
        int cap = type == EnemyType.ICON ? 9 : 6;
        if (alive >= cap) return;
        EnemyType kind = EnemyType.LOST_SOUL;
        double sx = x + ux * 0.9, sy = y + uy * 0.9;
        if (type == EnemyType.ICON) {
            EnemyType[] pool = {EnemyType.IMP, EnemyType.IMP, EnemyType.DEMON, EnemyType.SHOTGUNNER,
                    EnemyType.CACO, EnemyType.REVENANT, EnemyType.LOST_SOUL, EnemyType.HELL_KNIGHT};
            kind = pool[RND.nextInt(pool.length)];
            boolean placed = false;
            for (int i = 0; i < 12 && !placed; i++) {
                double a = RND.nextDouble() * Math.PI * 2, r = 2.0 + RND.nextDouble() * 3.5;
                sx = x + Math.cos(a) * r;
                sy = y + Math.sin(a) * r;
                placed = !w.level.isWall((int) sx, (int) sy) && w.level.lineOfSight(x, y, sx, sy);
            }
            if (!placed) return;
        } else if (w.level.isWall((int) sx, (int) sy)) {
            sx = x;
            sy = y;
        }
        Enemy e = new Enemy(kind, sx, sy);
        e.awake = true;
        e.summoned = true;
        w.spawned.add(e);
        w.sound.play(SoundFx.SIGHT_BOSS, sx, sy);
        w.effects.add(new Effect(sx, sy, 0));
    }

    @Override
    public boolean translucent() {
        return type == EnemyType.SPECTRE && alive;
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
            case ZOMBIE, SHOTGUNNER, CHAINGUNNER, SS -> SoundFx.SIGHT_HUMAN;
            case IMP, ARCHVILE, REVENANT -> SoundFx.SIGHT_IMP;
            case DEMON, SPECTRE, MANCUBUS -> SoundFx.SIGHT_DEMON;
            case CACO, LOST_SOUL, PAIN -> SoundFx.SIGHT_CACO;
            case BARON, HELL_KNIGHT -> SoundFx.SIGHT_DEMON;
            default -> SoundFx.SIGHT_BOSS;
        };
    }

    private int painSound() {
        return switch (type) {
            case ZOMBIE, SHOTGUNNER, CHAINGUNNER, SS -> SoundFx.PAIN_HUMAN;
            case IMP, ARCHVILE, REVENANT -> SoundFx.PAIN_IMP;
            case DEMON, SPECTRE, MANCUBUS -> SoundFx.PAIN_DEMON;
            case CACO, LOST_SOUL, PAIN -> SoundFx.PAIN_CACO;
            case BARON, HELL_KNIGHT -> SoundFx.PAIN_DEMON;
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
    public void prepare(double viewerX, double viewerY) {
        double rel = Math.atan2(viewerY - y, viewerX - x) - facing;
        while (rel > Math.PI) rel -= 2 * Math.PI;
        while (rel < -Math.PI) rel += 2 * Math.PI;
        double a = Math.abs(rel);
        view = a < Math.PI / 8 ? 0 : a < 3 * Math.PI / 8 ? 1 : a < 5 * Math.PI / 8 ? 2 : a < 7 * Math.PI / 8 ? 3 : 4;
        mirror = rel < 0;
    }

    @Override
    public int colorAt(double u, double v) {
        int fr = frame();
        boolean turned = alive && fr <= EnemySprites.ATTACK;
        int c = EnemySprites.pixel(type, fr, turned ? view : 0, turned && mirror ? 1 - u : u, v);
        if (c != 0 && hurt > 0 && alive) {
            int r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, b = c & 0xFF;
            return ColorUtils.rgb(Math.min(255, r + 110), Math.min(255, g + 70), Math.min(255, b + 70));
        }
        return c;
    }
}

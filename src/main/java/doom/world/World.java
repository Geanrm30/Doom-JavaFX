package doom.world;

import doom.engine.GameState;
import doom.engine.SoundFx;
import doom.entities.Barrel;
import doom.entities.Effect;
import doom.entities.Enemy;
import doom.entities.EnemyType;
import doom.entities.Pickup;
import doom.entities.Projectile;

import javafx.scene.input.KeyCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class World {

    public final Player player = new Player();
    public final List<Enemy> enemies = new ArrayList<>();
    public final List<Pickup> pickups = new ArrayList<>();
    public final List<Projectile> projectiles = new ArrayList<>();
    public final List<Barrel> barrels = new ArrayList<>();
    public final List<Effect> effects = new ArrayList<>();
    public final SoundFx sound = new SoundFx();

    public Level level;
    public GameState state = GameState.TITLE;
    public final int levelCount = Level.count();
    public int levelIndex = 1, titleLevel = 1;
    public int kills, totalEnemies, items, totalItems;
    public double time;
    public String message = "";
    public double messageTimer;
    public boolean showMap = true;
    public boolean paused;

    private final Player levelStart = new Player();
    private final Random rnd = new Random();

    // ---------------------------------------------------------------- flujo de niveles

    public void startGame(int index) {
        player.resetInventory();
        loadLevel(index);
    }

    public void loadLevel(int index) {
        level = Level.load(index);
        if (level == null) throw new IllegalStateException("Falta el nivel " + index);
        levelIndex = index;
        player.placeAt(level);
        levelStart.copyInventoryFrom(player);

        enemies.clear();
        pickups.clear();
        projectiles.clear();
        barrels.clear();
        effects.clear();
        paused = false;
        for (Level.Thing t : level.things) {
            EnemyType et = EnemyType.fromChar(t.type());
            if (et != null) {
                enemies.add(new Enemy(et, t.x(), t.y()));
                continue;
            }
            if (t.type() == 'o') {
                barrels.add(new Barrel(t.x(), t.y()));
                continue;
            }
            int pt = Pickup.fromChar(t.type());
            if (pt >= 0) pickups.add(new Pickup(t.x(), t.y(), pt));
        }
        kills = 0;
        items = 0;
        time = 0;
        totalEnemies = enemies.size();
        totalItems = (int) pickups.stream()
                .filter(p -> p.type != Pickup.RED_KEY && p.type != Pickup.BLUE_KEY).count();
        level.reveal(player.x, player.y);
        say("Nivel " + index + ": " + level.name, 3.5);
        state = GameState.PLAYING;
    }

    public void restartLevel() {
        player.copyInventoryFrom(levelStart);
        player.health = Math.max(player.health, 100);
        player.ammo[0] = Math.max(player.ammo[0], 50);
        loadLevel(levelIndex);
    }

    private void finishLevel() {
        sound.play(SoundFx.EXIT);
        state = levelIndex >= levelCount ? GameState.WON : GameState.LEVEL_DONE;
    }

    /** Mensaje corto en pantalla (lo usa tambien la ventana). */
    public void notice(String msg) {
        say(msg, 2.0);
    }

    /** ESC: pausa durante el juego, vuelve al titulo desde las pantallas finales. @return true si hay que cerrar el juego */
    public boolean escape() {
        switch (state) {
            case TITLE -> {
                return true;
            }
            case PLAYING -> paused = !paused;
            default -> {
                paused = false;
                state = GameState.TITLE;
            }
        }
        return false;
    }

    private void say(String msg, double secs) {
        message = msg;
        messageTimer = secs;
    }

    // ---------------------------------------------------------------- entrada discreta

    public void keyPressed(KeyCode k) {
        if (k == KeyCode.M) {
            sound.muted = !sound.muted;
            return;
        }
        switch (state) {
            case TITLE -> {
                if (k == KeyCode.ENTER || k == KeyCode.SPACE) startGame(titleLevel);
                else if (k == KeyCode.RIGHT || k == KeyCode.D) titleLevel = Math.min(levelCount, titleLevel + 1);
                else if (k == KeyCode.LEFT || k == KeyCode.A) titleLevel = Math.max(1, titleLevel - 1);
            }
            case PLAYING -> {
                if (paused) {
                    if (k == KeyCode.P || k == KeyCode.ENTER) paused = false;
                    else if (k == KeyCode.T) {
                        paused = false;
                        state = GameState.TITLE;
                    }
                    return;
                }
                switch (k) {
                    case F, ENTER -> use();
                    case TAB -> showMap = !showMap;
                    case P -> paused = !paused;
                    case DIGIT1, NUMPAD1 -> selectWeapon(0);
                    case DIGIT2, NUMPAD2 -> selectWeapon(1);
                    case DIGIT3, NUMPAD3 -> selectWeapon(2);
                    default -> { }
                }
            }
            case DEAD -> {
                if (k == KeyCode.ENTER || k == KeyCode.SPACE) restartLevel();
            }
            case LEVEL_DONE -> {
                if (k == KeyCode.ENTER || k == KeyCode.SPACE) loadLevel(levelIndex + 1);
            }
            case WON -> {
                if (k == KeyCode.ENTER) {
                    titleLevel = 1;
                    state = GameState.TITLE;
                }
            }
        }
    }

    /** Rueda del raton: siguiente (+1) o anterior (-1) arma disponible. */
    public void cycleWeapon(int dir) {
        if (state != GameState.PLAYING || paused) return;
        int n = Weapon.values().length;
        int i = player.weapon.ordinal();
        for (int step = 0; step < n; step++) {
            i = (i + dir + n) % n;
            if (player.has[i]) {
                player.weapon = Weapon.values()[i];
                return;
            }
        }
    }

    private void selectWeapon(int i) {
        if (!player.has[i]) {
            say("No tienes esa arma", 1.5);
            return;
        }
        player.weapon = Weapon.values()[i];
    }

    private void bestWeapon() {
        Weapon[] order = {Weapon.CHAINGUN, Weapon.SHOTGUN, Weapon.PISTOL};
        for (Weapon w : order) {
            if (player.has[w.ordinal()] && player.ammoFor(w) > 0) {
                player.weapon = w;
                return;
            }
        }
    }

    // ---------------------------------------------------------------- actualizacion

    public void update(double dt, Set<KeyCode> keys) {
        update(dt, keys, 0, false);
    }

    public void update(double dt, Set<KeyCode> keys, double mouseTurn, boolean mouseFire) {
        Player p = player;

        if (messageTimer > 0) messageTimer -= dt;
        if (p.fireCooldown > 0) p.fireCooldown -= dt;
        if (p.recoil > 0) p.recoil = Math.max(0, p.recoil - dt * 4);
        if (p.muzzle > 0) p.muzzle -= dt;
        if (p.dmgFlash > 0) p.dmgFlash -= dt;
        if (p.pickFlash > 0) p.pickFlash -= dt;

        if (state != GameState.PLAYING || paused) return;
        time += dt;
        if (p.invuln > 0) p.invuln -= dt;

        p.angle += mouseTurn;
        boolean run = keys.contains(KeyCode.SHIFT);
        double rot = (run ? 3.0 : 2.2) * dt;
        if (keys.contains(KeyCode.LEFT) || keys.contains(KeyCode.Q)) p.angle -= rot;
        if (keys.contains(KeyCode.RIGHT) || keys.contains(KeyCode.E)) p.angle += rot;

        double dirX = Math.cos(p.angle), dirY = Math.sin(p.angle);
        double mx = 0, my = 0;
        if (keys.contains(KeyCode.W) || keys.contains(KeyCode.UP)) { mx += dirX; my += dirY; }
        if (keys.contains(KeyCode.S) || keys.contains(KeyCode.DOWN)) { mx -= dirX; my -= dirY; }
        if (keys.contains(KeyCode.D)) { mx -= dirY; my += dirX; }
        if (keys.contains(KeyCode.A)) { mx += dirY; my -= dirX; }
        double len = Math.hypot(mx, my);
        if (len > 0) {
            double sp = (run ? 4.8 : 3.0) * dt / len;
            p.tryMove(level, mx * sp, my * sp);
            level.reveal(p.x, p.y);
        }

        if ((mouseFire || keys.contains(KeyCode.SPACE) || keys.contains(KeyCode.CONTROL)) && p.fireCooldown <= 0) {
            tryFire();
        }

        for (Enemy e : enemies) e.update(dt, this);
        for (Projectile pr : projectiles) pr.update(dt, this);
        projectiles.removeIf(pr -> !pr.alive);
        for (Effect ef : effects) ef.update(dt);
        effects.removeIf(ef -> !ef.alive());

        pickups.removeIf(this::collect);
    }

    public void hurtPlayer(int amount) {
        if (state != GameState.PLAYING || player.invuln > 0) return;
        if (player.damage(amount)) {
            state = GameState.DEAD;
            sound.play(SoundFx.DEATH);
        } else {
            sound.play(SoundFx.HURT);
        }
    }

    private boolean collect(Pickup pk) {
        Player p = player;
        if (Math.hypot(pk.x - p.x, pk.y - p.y) >= 0.6) return false;
        String msg;
        switch (pk.type) {
            case Pickup.HEALTH -> {
                if (p.health >= 100) return false;
                p.health = Math.min(100, p.health + 25);
                msg = "Botiquin: +25 de salud";
            }
            case Pickup.STIM -> {
                if (p.health >= 100) return false;
                p.health = Math.min(100, p.health + 10);
                msg = "Estimulante: +10 de salud";
            }
            case Pickup.ARMOR -> {
                if (p.armor >= 100) return false;
                p.armor = Math.min(100, p.armor + 50);
                msg = "Armadura: +50";
            }
            case Pickup.CLIP -> {
                if (p.ammo[0] >= Player.MAX_AMMO[0]) return false;
                p.addAmmo(Weapon.BULLETS, 10);
                msg = "Cargador: +10 balas";
            }
            case Pickup.SHELLS -> {
                if (p.ammo[1] >= Player.MAX_AMMO[1]) return false;
                p.addAmmo(Weapon.SHELLS, 8);
                msg = "Cartuchos: +8";
            }
            case Pickup.SHOTGUN -> {
                p.has[1] = true;
                p.addAmmo(Weapon.SHELLS, 8);
                p.weapon = Weapon.SHOTGUN;
                msg = "Tienes la ESCOPETA (tecla 2)";
            }
            case Pickup.CHAINGUN -> {
                p.has[2] = true;
                p.addAmmo(Weapon.BULLETS, 20);
                p.weapon = Weapon.CHAINGUN;
                msg = "Tienes la AMETRALLADORA (tecla 3)";
            }
            case Pickup.MEGA -> {
                if (p.health >= 200) return false;
                p.health = Math.min(200, p.health + 100);
                p.armor = Math.max(p.armor, 100);
                msg = "MEGASALUD: salud al maximo y armadura";
                sound.play(SoundFx.POWERUP);
            }
            case Pickup.INVULN -> {
                p.invuln = 12;
                msg = "INVULNERABLE durante 12 segundos";
                sound.play(SoundFx.POWERUP);
            }
            case Pickup.RED_KEY -> {
                p.redKey = true;
                msg = "Llave ROJA";
            }
            case Pickup.BLUE_KEY -> {
                p.blueKey = true;
                msg = "Llave AZUL";
            }
            default -> {
                return false;
            }
        }
        if (pk.type != Pickup.RED_KEY && pk.type != Pickup.BLUE_KEY) items++;
        p.pickFlash = 0.2;
        sound.play(SoundFx.PICKUP);
        say(msg, 2.0);
        return true;
    }

    /** Usar: abre puertas y activa la salida que tengas enfrente. */
    private void use() {
        Player p = player;
        double dx = Math.cos(p.angle), dy = Math.sin(p.angle);
        for (double d = 0.2; d <= 1.6; d += 0.1) {
            int cx = (int) (p.x + dx * d), cy = (int) (p.y + dy * d);
            char c = level.cell(cx, cy);
            if (c == '.') continue;
            switch (c) {
                case 'D' -> { level.open(cx, cy); sound.play(SoundFx.DOOR); }
                case 'R' -> {
                    if (p.redKey) { level.open(cx, cy); sound.play(SoundFx.DOOR); }
                    else { say("Necesitas la llave ROJA", 2.0); sound.play(SoundFx.DENIED); }
                }
                case 'U' -> {
                    if (p.blueKey) { level.open(cx, cy); sound.play(SoundFx.DOOR); }
                    else { say("Necesitas la llave AZUL", 2.0); sound.play(SoundFx.DENIED); }
                }
                case 'X' -> {
                    if (bossAlive()) { say("Derrota al JEFE para abrir la salida", 2.5); sound.play(SoundFx.DENIED); }
                    else finishLevel();
                }
                default -> { }
            }
            return;
        }
    }

    public boolean bossAlive() {
        return enemies.stream().anyMatch(e -> e.alive && e.type.isBoss());
    }

    // ---------------------------------------------------------------- disparo

    private void tryFire() {
        Player p = player;
        if (p.ammoFor(p.weapon) <= 0) {
            bestWeapon();
            if (p.ammoFor(p.weapon) <= 0) {
                p.fireCooldown = 0.3;
                say("Sin municion", 1.0);
                sound.play(SoundFx.DENIED);
                return;
            }
        }
        shoot(p.weapon);
    }

    private void shoot(Weapon w) {
        Player p = player;
        p.ammo[w.ammoType]--;
        p.fireCooldown = w.cooldown;
        p.recoil = w == Weapon.CHAINGUN ? 0.35 : 1.0;
        p.muzzle = 0.1;
        sound.play(w == Weapon.PISTOL ? SoundFx.PISTOL : w == Weapon.SHOTGUN ? SoundFx.SHOTGUN : SoundFx.CHAINGUN);

        for (Enemy e : enemies) {
            if (!e.alive || e.awake) continue;
            double d = Math.hypot(e.x - p.x, e.y - p.y);
            if (d < 4 || (d < 10 && level.lineOfSight(p.x, p.y, e.x, e.y))) e.awake = true;
        }

        for (int i = 0; i < w.pellets; i++) {
            double a = p.angle + (rnd.nextDouble() * 2 - 1) * w.spread;
            double rx = Math.cos(a), ry = Math.sin(a);
            Enemy best = null;
            double bestAlong = Double.MAX_VALUE;
            for (Enemy e : enemies) {
                if (!e.alive) continue;
                double dx = e.x - p.x, dy = e.y - p.y;
                double along = dx * rx + dy * ry;
                if (along < 0.15 || along >= bestAlong) continue;
                if (Math.abs(dx * ry - dy * rx) > e.type.radius) continue;
                if (!level.lineOfSight(p.x, p.y, e.x, e.y)) continue;
                best = e;
                bestAlong = along;
            }
            Barrel bb = null;
            double bbAlong = Double.MAX_VALUE;
            for (Barrel b : barrels) {
                if (!b.alive) continue;
                double dx = b.x - p.x, dy = b.y - p.y;
                double along = dx * rx + dy * ry;
                if (along < 0.15 || along >= bbAlong) continue;
                if (Math.abs(dx * ry - dy * rx) > 0.28) continue;
                if (!level.lineOfSight(p.x, p.y, b.x, b.y)) continue;
                bb = b;
                bbAlong = along;
            }
            int dmg = w.dmgMin + rnd.nextInt(w.dmgMax - w.dmgMin + 1);
            if (bb != null && bbAlong < bestAlong) {
                bb.hp -= dmg;
                if (bb.hp <= 0) explode(bb);
            } else if (best != null) {
                if (best.hit(dmg)) onKill(best);
            }
        }
    }

    private void explode(Barrel b) {
        if (!b.alive) return;
        b.alive = false;
        effects.add(new Effect(b.x, b.y));
        sound.play(SoundFx.EXPLODE);
        double radius = 3.0;
        for (Barrel o : barrels) {
            if (o.alive && Math.hypot(o.x - b.x, o.y - b.y) < radius) explode(o);
        }
        for (Enemy e : enemies) {
            if (!e.alive) continue;
            double d = Math.hypot(e.x - b.x, e.y - b.y);
            if (d < radius && level.lineOfSight(b.x, b.y, e.x, e.y)) {
                if (e.hit((int) (140 * (1 - d / radius)) + 20)) onKill(e);
            }
        }
        double pd = Math.hypot(player.x - b.x, player.y - b.y);
        if (pd < radius && level.lineOfSight(b.x, b.y, player.x, player.y)) {
            hurtPlayer((int) (60 * (1 - pd / radius)) + 5);
        }
    }

    private void onKill(Enemy e) {
        kills++;
        if (e.type.isBoss() && !bossAlive()) {
            say("JEFE DERROTADO: la salida esta abierta", 4.0);
            sound.play(SoundFx.POWERUP);
        }
        sound.play(SoundFx.ENEMY_DIE);
        if (e.type == EnemyType.ZOMBIE && rnd.nextDouble() < 0.8) {
            pickups.add(new Pickup(e.x, e.y, Pickup.CLIP));
        }
    }
}

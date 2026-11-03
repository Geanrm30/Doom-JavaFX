package doom.world;

import doom.engine.Constants;
import doom.engine.GameState;
import doom.entities.Enemy;
import doom.entities.Pickup;

import javafx.scene.input.KeyCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class World {

    public final Player player = new Player();
    public final List<Enemy> enemies = new ArrayList<>();
    public final List<Pickup> pickups = new ArrayList<>();

    private final Random rnd = new Random();

    public World() {
        reset();
    }

    public void reset() {
        player.reset();

        enemies.clear();
        double[][] spawns = {
            {9.5, 3.5}, {15.5, 3.5}, {3.5, 3.5}, {5.5, 8.5}, {16.5, 8.5},
            {10.5, 12.5}, {3.5, 14.5}, {15.5, 14.5}, {10.5, 17.5}
        };
        for (double[] s : spawns) enemies.add(new Enemy(s[0], s[1]));

        pickups.clear();
        pickups.add(new Pickup(1.5, 1.5, Pickup.HEALTH));
        pickups.add(new Pickup(18.5, 10.5, Pickup.HEALTH));
        pickups.add(new Pickup(10.5, 6.5, Pickup.AMMO));
        pickups.add(new Pickup(1.5, 12.5, Pickup.AMMO));
        pickups.add(new Pickup(18.5, 17.5, Pickup.AMMO));
    }

    public void update(double dt, Set<KeyCode> keys, double[] zBuf) {
        Player p = player;

        if (p.fireCooldown > 0) p.fireCooldown -= dt;
        if (p.recoil > 0) p.recoil = Math.max(0, p.recoil - dt * 4);
        if (p.muzzle > 0) p.muzzle -= dt;
        if (p.dmgFlash > 0) p.dmgFlash -= dt;
        if (p.pickFlash > 0) p.pickFlash -= dt;

        if (p.state != GameState.PLAYING) return;

        double rot = 2.2 * dt;
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
            double sp = 3.0 * dt / len;
            p.tryMove(mx * sp, my * sp);
        }

        if ((keys.contains(KeyCode.SPACE) || keys.contains(KeyCode.CONTROL))
                && p.fireCooldown <= 0 && p.ammo > 0) {
            shoot(p, zBuf);
        }

        for (Enemy e : enemies) e.update(dt, p);

        pickups.removeIf(pickup -> {
            if (Math.hypot(pickup.x - p.x, pickup.y - p.y) < 0.6) {
                if (pickup.type == Pickup.HEALTH && p.health < 100) {
                    p.health = Math.min(100, p.health + 25);
                    p.pickFlash = 0.2;
                    return true;
                } else if (pickup.type == Pickup.AMMO) {
                    p.ammo += 12;
                    p.pickFlash = 0.2;
                    return true;
                }
            }
            return false;
        });

        if (enemies.stream().noneMatch(e -> e.alive)) p.state = GameState.WON;
    }

    private void shoot(Player p, double[] zBuf) {
        p.ammo--;
        p.fireCooldown = 0.45;
        p.recoil = 1.0;
        p.muzzle = 0.1;

        int W = Constants.SCREEN_W, H = Constants.SCREEN_H;
        double dirX = Math.cos(p.angle), dirY = Math.sin(p.angle);
        double planeX = -dirY * 0.66, planeY = dirX * 0.66;
        double invDet = 1.0 / (planeX * dirY - dirX * planeY);

        Enemy best = null;
        double bestDist = Double.MAX_VALUE;
        for (Enemy e : enemies) {
            if (!e.alive) continue;
            double dx = e.x - p.x, dy = e.y - p.y;
            double tx = invDet * (dirY * dx - dirX * dy);
            double ty = invDet * (-planeY * dx + planeX * dy);
            if (ty < 0.1) continue;
            double screenX = W / 2.0 * (1 + tx / ty);
            double halfWidth = H / ty * 0.12;
            if (Math.abs(screenX - W / 2.0) < halfWidth
                    && ty < zBuf[W / 2]
                    && ty < bestDist) {
                best = e;
                bestDist = ty;
            }
        }
        if (best != null) best.hit(25 + rnd.nextInt(16));
    }
}

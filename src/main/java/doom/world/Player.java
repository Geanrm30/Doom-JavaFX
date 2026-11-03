package doom.world;

import doom.engine.GameState;

public class Player {

    public double x = 1.5, y = 18.5, angle = -Math.PI / 4;
    public int health = 100, ammo = 40;
    public double fireCooldown, recoil, muzzle, dmgFlash, pickFlash;
    public GameState state = GameState.PLAYING;

    public void reset() {
        x = 1.5;
        y = 18.5;
        angle = -Math.PI / 4;
        health = 100;
        ammo = 40;
        fireCooldown = recoil = muzzle = dmgFlash = pickFlash = 0;
        state = GameState.PLAYING;
    }

    public void damage(int amount) {
        if (state != GameState.PLAYING) return;
        health -= amount;
        dmgFlash = 0.25;
        if (health <= 0) {
            health = 0;
            state = GameState.DEAD;
        }
    }

    public void tryMove(double dx, double dy) {
        double r = 0.22;
        double nx = x + dx, ny = y + dy;
        double sx = dx > 0 ? r : -r, sy = dy > 0 ? r : -r;
        if (!GameMap.isWall((int) (nx + sx), (int) y)) x = nx;
        if (!GameMap.isWall((int) x, (int) (ny + sy))) y = ny;
    }
}

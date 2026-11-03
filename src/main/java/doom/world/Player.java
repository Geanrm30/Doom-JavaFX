package doom.world;

public class Player {

    public static final int[] MAX_AMMO = {200, 50};

    public double x = 1.5, y = 1.5, angle;
    public int health = 100, armor = 0;
    public final int[] ammo = {50, 0};
    public final boolean[] has = {true, false, false};
    public Weapon weapon = Weapon.PISTOL;
    public boolean redKey, blueKey;
    public double invuln, bobPhase, bobAmp;
    public double fireCooldown, recoil, muzzle, dmgFlash, pickFlash;

    /** Inventario inicial de una partida nueva. */
    public void resetInventory() {
        health = 100;
        armor = 0;
        ammo[0] = 50;
        ammo[1] = 0;
        has[0] = true;
        has[1] = has[2] = false;
        weapon = Weapon.PISTOL;
    }

    public void copyInventoryFrom(Player o) {
        health = o.health;
        armor = o.armor;
        ammo[0] = o.ammo[0];
        ammo[1] = o.ammo[1];
        System.arraycopy(o.has, 0, has, 0, has.length);
        weapon = o.weapon;
    }

    public void placeAt(Level level) {
        x = level.startX;
        y = level.startY;
        angle = level.startAngle;
        redKey = blueKey = false;
        fireCooldown = recoil = muzzle = dmgFlash = pickFlash = invuln = 0;
    }

    /** @return true si el jugador murio con este golpe */
    public boolean damage(int amount) {
        if (health <= 0 || invuln > 0) return false;
        if (armor > 0) {
            int absorbed = Math.min(armor, amount / 2);
            armor -= absorbed;
            amount -= absorbed;
        }
        health -= amount;
        dmgFlash = 0.25;
        if (health <= 0) {
            health = 0;
            return true;
        }
        return false;
    }

    public int ammoFor(Weapon w) {
        return ammo[w.ammoType];
    }

    public void addAmmo(int type, int n) {
        ammo[type] = Math.min(MAX_AMMO[type], ammo[type] + n);
    }

    public void tryMove(Level level, double dx, double dy) {
        double r = 0.22;
        double nx = x + dx, ny = y + dy;
        double sx = dx > 0 ? r : -r, sy = dy > 0 ? r : -r;
        if (!level.isWall((int) (nx + sx), (int) y) && !level.isWall((int) (nx + sx), (int) (y + r))
                && !level.isWall((int) (nx + sx), (int) (y - r))) x = nx;
        if (!level.isWall((int) x, (int) (ny + sy)) && !level.isWall((int) (x + r), (int) (ny + sy))
                && !level.isWall((int) (x - r), (int) (ny + sy))) y = ny;
    }
}

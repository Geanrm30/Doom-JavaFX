package doom.world;

public class Player {

    public static final int[] MAX_AMMO = {200, 50, 30, 300};

    public double x = 1.5, y = 1.5, angle;
    public int health = 100, armor = 0;
    public final int[] ammo = {50, 0, 0, 0};
    public final boolean[] has = {true, false, false, false, false, true};
    /** Balas en el cargador de cada arma (indice = ordinal del arma). */
    public final int[] loaded = new int[Weapon.values().length];
    public Weapon weapon = Weapon.PISTOL;
    /** Recarga en curso: segundos que faltan, total, y estado de la animacion/sonido. */
    public double reloadT, reloadTotal;
    public boolean reloadByRound;
    public int reloadStage, reloadInserted;
    /** Animacion del tajo de katana (1 -> 0) y lado del tajo; ejecucion en curso. */
    public double slash, slashSide, execT, execTotal;
    public boolean redKey, blueKey;
    public double invuln, bobPhase, bobAmp;
    public double fireCooldown, recoil, muzzle, dmgFlash, pickFlash;

    /** Inventario inicial de una partida nueva. */
    public void resetInventory() {
        health = 100;
        armor = 0;
        ammo[0] = 50;
        ammo[1] = ammo[2] = ammo[3] = 0;
        has[0] = true;
        has[1] = has[2] = has[3] = has[4] = false;
        has[5] = true;
        java.util.Arrays.fill(loaded, 0);
        loaded[0] = Weapon.PISTOL.mag;
        weapon = Weapon.PISTOL;
        cancelReload();
    }

    /** Llena el cargador de un arma recien conseguida. */
    public void fillMag(Weapon w) {
        loaded[w.ordinal()] = w.mag;
    }

    public void cancelReload() {
        reloadT = 0;
        reloadStage = reloadInserted = 0;
    }

    /** Cambia de arma (cancela la recarga en curso). */
    public void equip(Weapon w) {
        if (w != weapon) {
            cancelReload();
            slash = 0;
            fireCooldown = Math.max(fireCooldown, 0.2);
        }
        weapon = w;
    }

    /** Balas disponibles del arma (cargador + reserva). */
    public int rounds(Weapon w) {
        return w.isMelee() ? 0 : loaded[w.ordinal()] + ammo[w.ammoType];
    }

    /** Arma preferida para convertir una caja de municion en cargadores. */
    public Weapon ammoWeapon(int type) {
        Weapon best = null;
        for (Weapon w : Weapon.values()) {
            if (w.ammoType != type) continue;
            if (has[w.ordinal()]) best = w;
            else if (best == null) best = w;
        }
        return best;
    }

    public void copyInventoryFrom(Player o) {
        health = o.health;
        armor = o.armor;
        System.arraycopy(o.ammo, 0, ammo, 0, ammo.length);
        System.arraycopy(o.has, 0, has, 0, has.length);
        System.arraycopy(o.loaded, 0, loaded, 0, loaded.length);
        weapon = o.weapon;
        cancelReload();
    }

    public void placeAt(Level level) {
        x = level.startX;
        y = level.startY;
        angle = level.startAngle;
        redKey = blueKey = false;
        fireCooldown = recoil = muzzle = dmgFlash = pickFlash = invuln = slash = execT = 0;
        cancelReload();
    }

    /** @return true si el jugador murio con este golpe */
    public boolean damage(int amount) {
        if (health <= 0 || invuln > 0 || execT > 0) return false;
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
        return w.isMelee() ? 0 : ammo[w.ammoType];
    }

    public void addAmmo(int type, int n) {
        ammo[type] = Math.min(MAX_AMMO[type], ammo[type] + n);
    }

    public boolean ghost;

    public void tryMove(Level level, double dx, double dy) {
        if (ghost) {
            double gx = x + dx, gy = y + dy;
            if (gx > 1.3 && gx < level.width - 1.3) x = gx;
            if (gy > 1.3 && gy < level.height - 1.3) y = gy;
            return;
        }
        double r = 0.22;
        double nx = x + dx, ny = y + dy;
        double sx = dx > 0 ? r : -r, sy = dy > 0 ? r : -r;
        if (!level.isWall((int) (nx + sx), (int) y) && !level.isWall((int) (nx + sx), (int) (y + r))
                && !level.isWall((int) (nx + sx), (int) (y - r))) x = nx;
        if (!level.isWall((int) x, (int) (ny + sy)) && !level.isWall((int) (x + r), (int) (ny + sy))
                && !level.isWall((int) (x - r), (int) (ny + sy))) y = ny;
    }
}

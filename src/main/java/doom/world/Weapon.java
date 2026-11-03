package doom.world;

public enum Weapon {
    //            nombre         municion cooldown pellets spread dmgMin dmgMax
    PISTOL("PISTOLA", 0, 0.40, 1, 0.012, 15, 25),
    SHOTGUN("ESCOPETA", 1, 0.95, 7, 0.085, 9, 17),
    CHAINGUN("AMETRALLADORA", 0, 0.09, 1, 0.045, 10, 18);

    public static final int BULLETS = 0, SHELLS = 1;

    public final String label;
    public final int ammoType;
    public final double cooldown;
    public final int pellets;
    public final double spread;
    public final int dmgMin, dmgMax;

    Weapon(String label, int ammoType, double cooldown, int pellets, double spread, int dmgMin, int dmgMax) {
        this.label = label;
        this.ammoType = ammoType;
        this.cooldown = cooldown;
        this.pellets = pellets;
        this.spread = spread;
        this.dmgMin = dmgMin;
        this.dmgMax = dmgMax;
    }
}

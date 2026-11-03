package doom.world;

public enum Weapon {
    //            nombre            municion cooldown pellets spread dmgMin dmgMax proyectil cargador recarga(s) cargadores por recogida
    PISTOL("PISTOLA", 0, 0.40, 1, 0.012, 15, 25, 0, 7, 1.15, 2),
    SHOTGUN("ESCOPETA", 1, 0.95, 7, 0.085, 9, 17, 0, 4, 2.0, 2),
    CHAINGUN("AMETRALLADORA", 0, 0.09, 1, 0.045, 10, 18, 0, 30, 2.5, 1),
    ROCKET("LANZACOHETES", 2, 0.80, 1, 0.0, 90, 130, 1, 1, 1.7, 4),
    PLASMA("RIFLE DE PLASMA", 3, 0.10, 1, 0.01, 14, 22, 2, 25, 2.1, 2),
    /** Cuerpo a cuerpo: sin municion. */
    KATANA("KATANA", -1, 0.42, 0, 0, 38, 56, 0, 0, 0, 0);

    public static final int BULLETS = 0, SHELLS = 1, ROCKETS = 2, CELLS = 3;

    public final String label;
    public final int ammoType;
    public final double cooldown;
    public final int pellets;
    public final double spread;
    public final int dmgMin, dmgMax;
    /** 0 impacto instantaneo, 1 cohete, 2 plasma. */
    public final int proj;
    /** Balas por cargador (0 si no usa municion). */
    public final int mag;
    /** Segundos que tarda en recargar un cargador completo. */
    public final double reload;
    /** Cuantos cargadores da una caja de municion de este arma. */
    public final int pickupMags;

    Weapon(String label, int ammoType, double cooldown, int pellets, double spread, int dmgMin, int dmgMax, int proj,
           int mag, double reload, int pickupMags) {
        this.label = label;
        this.ammoType = ammoType;
        this.cooldown = cooldown;
        this.pellets = pellets;
        this.spread = spread;
        this.dmgMin = dmgMin;
        this.dmgMax = dmgMax;
        this.proj = proj;
        this.mag = mag;
        this.reload = reload;
        this.pickupMags = pickupMags;
    }

    public boolean isMelee() { return ammoType < 0; }
}

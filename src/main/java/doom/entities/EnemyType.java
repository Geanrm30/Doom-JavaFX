package doom.entities;

public enum EnemyType {
    //      nombre         hp  vel  escala lift radio  ataque a distancia      dano  cd   melee  mcd  cuerpo          cabeza
    ZOMBIE("Zombi",        30, 0.8, 0.90, 0.00, 0.30, Ranged.HITSCAN,  7, 1.7,  5, 1.0, rgb(105, 120, 70),  rgb(210, 170, 140), false, 1.0),
    IMP("Imp",             60, 1.3, 0.90, 0.00, 0.30, Ranged.FIREBALL, 12, 2.0, 10, 1.0, rgb(150, 75, 50),  rgb(165, 85, 58),  true, 1.0),
    DEMON("Demonio",      110, 2.7, 0.95, 0.00, 0.38, Ranged.NONE,      0, 1.0, 18, 0.8, rgb(215, 115, 135), rgb(225, 125, 145), false, 1.35),
    CACO("Cacodemonio",   160, 1.0, 1.00, 0.22, 0.42, Ranged.FIREBALL, 18, 2.2, 15, 1.2, rgb(200, 40, 40),  rgb(200, 40, 40),  false, 1.0),
    CYBER("Ciberdemonio", 1000, 0.9, 1.55, 0.00, 0.60, Ranged.FIREBALL, 24, 1.3, 40, 1.0, rgb(125, 105, 95), rgb(160, 125, 105), true, 1.5),
    BOSS_BARON("Baron jefe", 700, 1.4, 1.45, 0.00, 0.55, Ranged.FIREBALL, 26, 1.1, 35, 0.9, rgb(70, 120, 80), rgb(90, 140, 95), true, 1.4),
    BARON("Baron",        450, 1.2, 1.30, 0.00, 0.50, Ranged.FIREBALL, 22, 1.5, 28, 1.0, rgb(120, 150, 115), rgb(135, 160, 125), true, 1.25),
    SHOTGUNNER("Escopetero",   40, 0.9, 0.92, 0.00, 0.30, Ranged.SHOTGUN, 5, 1.9, 5, 1.0, rgb(130, 78, 54),  rgb(205, 165, 135), false, 1.0),
    CHAINGUNNER("Ametrallador", 70, 0.8, 0.98, 0.00, 0.32, Ranged.BURST,   4, 2.0, 5, 1.0, rgb(70, 92, 128),  rgb(205, 165, 135), false, 1.1),
    SS("Soldado SS",           50, 1.0, 0.92, 0.00, 0.30, Ranged.HITSCAN, 8, 1.4, 5, 1.0, rgb(92, 100, 118), rgb(225, 190, 160), false, 1.0),
    SPECTRE("Espectro",       110, 2.7, 0.95, 0.00, 0.38, Ranged.NONE,    0, 1.0, 18, 0.8, rgb(150, 150, 175), rgb(160, 160, 185), false, 1.35),
    LOST_SOUL("Alma perdida",  40, 1.6, 0.45, 0.36, 0.22, Ranged.LUNGE,  12, 2.6, 0, 1.0, rgb(230, 225, 205), rgb(235, 230, 210), false, 0.6),
    PAIN("Elemental del dolor", 250, 0.8, 1.15, 0.18, 0.50, Ranged.SPAWN, 0, 3.0, 0, 1.0, rgb(150, 100, 72), rgb(150, 100, 72), true, 1.2),
    HELL_KNIGHT("Caballero infernal", 500, 1.3, 1.28, 0.00, 0.50, Ranged.FIREBALL, 20, 1.6, 26, 1.0, rgb(190, 165, 130), rgb(200, 175, 140), true, 1.25),
    REVENANT("Revenant",      300, 1.7, 1.00, 0.00, 0.32, Ranged.MISSILE, 22, 2.4, 24, 0.9, rgb(225, 215, 185), rgb(235, 228, 200), false, 0.8),
    MANCUBUS("Mancubus",      600, 0.8, 1.20, 0.00, 0.55, Ranged.SPREAD,  18, 2.0, 0, 1.0, rgb(190, 140, 100), rgb(200, 150, 110), false, 1.7),
    ARCHVILE("Arch-vile",     350, 1.9, 0.98, 0.00, 0.32, Ranged.FLAME,   30, 3.6, 10, 1.0, rgb(225, 170, 60), rgb(230, 180, 80), false, 0.9),
    ICON("Icono del pecado", 1200, 0.0, 2.00, 0.00, 0.75, Ranged.SPAWN,    0, 3.2, 0, 1.0, rgb(120, 70, 60), rgb(130, 80, 66), true, 2.0);

    public enum Ranged { NONE, HITSCAN, FIREBALL, SHOTGUN, BURST, LUNGE, SPAWN, MISSILE, SPREAD, FLAME }

    public final String label;
    public final int hp;
    public final double speed, scale, lift, radius;
    public final Ranged ranged;
    public final int rangedDmg, meleeDmg;
    public final double rangedCd, meleeCd;
    public final int body, head;
    public final boolean horns;
    public final double bulk;

    EnemyType(String label, int hp, double speed, double scale, double lift, double radius, Ranged ranged,
              int rangedDmg, double rangedCd, int meleeDmg, double meleeCd, int body, int head,
              boolean horns, double bulk) {
        this.label = label;
        this.hp = hp;
        this.speed = speed;
        this.scale = scale;
        this.lift = lift;
        this.radius = radius;
        this.ranged = ranged;
        this.rangedDmg = rangedDmg;
        this.rangedCd = rangedCd;
        this.meleeDmg = meleeDmg;
        this.meleeCd = meleeCd;
        this.body = body;
        this.head = head;
        this.horns = horns;
        this.bulk = bulk;
    }

    public boolean isBoss() {
        return this == CYBER || this == BOSS_BARON || this == ICON;
    }

    private static int rgb(int r, int g, int b) {
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public static EnemyType fromChar(char c) {
        return switch (c) {
            case 'z' -> ZOMBIE;
            case 'i' -> IMP;
            case 'd' -> DEMON;
            case 'c' -> CACO;
            case 'B' -> BARON;
            case 'Y' -> CYBER;
            case 'J' -> BOSS_BARON;
            case 'p' -> SHOTGUNNER;
            case 'x' -> CHAINGUNNER;
            case 'W' -> SS;
            case 't' -> SPECTRE;
            case 'f' -> LOST_SOUL;
            case 'b' -> PAIN;
            case 'K' -> HELL_KNIGHT;
            case 'V' -> REVENANT;
            case 'M' -> MANCUBUS;
            case 'A' -> ARCHVILE;
            case 'I' -> ICON;
            default -> null;
        };
    }
}

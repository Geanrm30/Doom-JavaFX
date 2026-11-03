package doom.entities;

public abstract class Sprite {
    public double x, y, scale;
    /** Elevacion sobre el suelo, en alturas de pared (para cacodemonios y bolas de fuego). */
    public double lift;

    public abstract int colorAt(double u, double v);
}

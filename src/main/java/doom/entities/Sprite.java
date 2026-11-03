package doom.entities;

public abstract class Sprite {
    public double x, y, scale;

    public abstract int colorAt(double u, double v);
}

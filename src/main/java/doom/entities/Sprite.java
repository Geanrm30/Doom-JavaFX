package doom.entities;

public abstract class Sprite {
    public double x, y, scale;
    /** Elevacion sobre el suelo, en alturas de pared (para cacodemonios y bolas de fuego). */
    public double lift;

    /** Se llama antes de dibujar, con la posicion de la camara (los enemigos eligen su vista). */
    public void prepare(double viewerX, double viewerY) { }

    public abstract int colorAt(double u, double v);
}

package doom.engine;

public final class ColorUtils {

    private ColorUtils() {}

    public static int rgb(int r, int g, int b) {
        return 0xFF000000 | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    public static int clamp(int v) {
        return v < 0 ? 0 : Math.min(v, 255);
    }

    public static int shade(int color, double factor) {
        int r = (int) (((color >> 16) & 0xFF) * factor);
        int g = (int) (((color >> 8) & 0xFF) * factor);
        int b = (int) ((color & 0xFF) * factor);
        return rgb(r, g, b);
    }
}

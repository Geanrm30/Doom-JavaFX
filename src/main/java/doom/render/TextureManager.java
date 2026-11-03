package doom.render;

import doom.engine.ColorUtils;
import doom.engine.Constants;

import java.util.Random;

public class TextureManager {

    // 0-5 paredes '1'..'6', 6 puerta, 7 puerta roja, 8 puerta azul, 9 salida
    public final int[][] data = new int[10][Constants.TEX_SIZE * Constants.TEX_SIZE];

    public TextureManager() {
        buildTextures();
    }

    public static int indexFor(char c) {
        if (c >= '1' && c <= '6') return c - '1';
        return switch (c) {
            case 'D' -> 6;
            case 'R' -> 7;
            case 'U' -> 8;
            case 'X' -> 9;
            default -> 0;
        };
    }

    private void buildTextures() {
        Random r = new Random(7);
        int tex = Constants.TEX_SIZE;
        for (int y = 0; y < tex; y++) {
            for (int x = 0; x < tex; x++) {
                int n = r.nextInt(31) - 15;
                int i = y * tex + x;

                // 1: ladrillo
                int row = y / 16;
                int bx = (x + (row % 2) * 16) % 32;
                boolean mortar = (y % 16 == 0) || bx == 0;
                data[0][i] = mortar
                        ? ColorUtils.rgb(70 + n / 2, 65 + n / 2, 60 + n / 2)
                        : ColorUtils.rgb(150 + n, 60 + n / 2, 45 + n / 2);

                // 2: panel tecnologico
                boolean line = (x % 32 == 0) || (y % 32 == 0);
                int base = line ? 30 : 62;
                int c = ColorUtils.rgb(base + n / 2, base + 12 + n / 2, base + 28 + n / 2);
                if (x >= 24 && x <= 40 && y >= 26 && y <= 31) c = ColorUtils.rgb(50, 220 + n, 110);
                if (x >= 10 && x <= 14 && y >= 44 && y <= 56) c = ColorUtils.rgb(220 + n, 60, 50);
                data[1][i] = c;

                // 3: piedra
                boolean edge = (x % 32 == 0) || (y % 16 == 0);
                int s = edge ? 55 : 110;
                data[2][i] = ColorUtils.rgb(s + n, s + n, s + 6 + n);

                // 4: metal oxidado
                int m = (y % 8 == 0) ? 90 : 140;
                int c3 = ColorUtils.rgb(m + n, (int) (m * 0.65) + n / 2, (int) (m * 0.3));
                int rx = x % 16, ry = y % 16;
                if ((rx - 4) * (rx - 4) + (ry - 4) * (ry - 4) < 5) c3 = ColorUtils.rgb(190, 170, 140);
                data[3][i] = c3;

                // 5: marmol verde
                double vein = Math.sin((x + y * 0.7) * 0.22) + Math.sin(y * 0.17 + x * 0.09 + 2);
                int g = 95 + (int) (35 * Math.sin(vein * 2.2)) + n / 2;
                boolean slab = (x % 32 == 0) || (y % 32 == 0);
                data[4][i] = slab ? ColorUtils.rgb(20, 45, 28)
                        : ColorUtils.rgb(g / 3, g, g / 2 + 8);

                // 6: carne infernal
                double wob = Math.sin(x * 0.28 + Math.sin(y * 0.19) * 3.0);
                int rr = 110 + (int) (50 * wob) + n;
                boolean pulse = ((int) (y + 6 * Math.sin(x * 0.2))) % 14 < 2;
                data[5][i] = pulse ? ColorUtils.rgb(40, 5, 5)
                        : ColorUtils.rgb(rr, 22 + n / 3, 18 + n / 3);

                // 7: puerta metalica con franja de peligro
                boolean frame = x < 3 || x > 60 || y < 3 || y > 60 || x == 31 || x == 32;
                int dm = frame ? 40 : 105 + n / 2;
                int dc = ColorUtils.rgb(dm, dm, dm + 8);
                if (y >= 26 && y <= 37 && !frame) {
                    dc = ((x + y) / 6) % 2 == 0 ? ColorUtils.rgb(225, 190, 30) : ColorUtils.rgb(25, 25, 25);
                }
                data[6][i] = dc;

                // 8 / 9: puertas con cerradura de color
                data[7][i] = lockedDoor(x, y, n, 200, 30, 30);
                data[8][i] = lockedDoor(x, y, n, 40, 80, 230);

                // 10: salida (flecha verde)
                int ex = ColorUtils.rgb(14 + n / 3, 60 + n / 2, 26 + n / 3);
                if (x < 3 || x > 60 || y < 3 || y > 60) ex = ColorUtils.rgb(120, 120, 125);
                for (int off = 0; off <= 24; off += 24) {
                    int t = x - 12 - off;
                    if (t >= 0 && t <= 22 && Math.abs(Math.abs(y - 32) - (22 - t)) <= 3) {
                        ex = ColorUtils.rgb(120, 255, 150);
                    }
                }
                data[9][i] = ex;
            }
        }
    }

    private static int lockedDoor(int x, int y, int n, int cr, int cg, int cb) {
        boolean frame = x < 3 || x > 60 || y < 3 || y > 60 || x == 31 || x == 32;
        if (frame) return ColorUtils.rgb(40, 40, 45);
        int dx = x - 32, dy = y - 32;
        if (dx * dx + dy * dy < 9 * 9) return ColorUtils.rgb(cr, cg, cb);
        if (dx * dx + dy * dy < 12 * 12) return ColorUtils.rgb(20, 20, 24);
        int m = 85 + n / 2;
        return ColorUtils.rgb(m * cr / 255 + 30, m * cg / 255 + 28, m * cb / 255 + 30);
    }
}

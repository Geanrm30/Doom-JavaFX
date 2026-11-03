package doom.render;

import doom.engine.ColorUtils;
import doom.engine.Constants;

import java.util.Random;

public class TextureManager {

    public final int[][] data = new int[4][Constants.TEX_SIZE * Constants.TEX_SIZE];

    public TextureManager() {
        buildTextures();
    }

    private void buildTextures() {
        Random r = new Random(7);
        int tex = Constants.TEX_SIZE;
        for (int y = 0; y < tex; y++) {
            for (int x = 0; x < tex; x++) {
                int n = r.nextInt(31) - 15;
                int i = y * tex + x;

                // brick wall
                int row = y / 16;
                int bx = (x + (row % 2) * 16) % 32;
                boolean mortar = (y % 16 == 0) || bx == 0;
                data[0][i] = mortar
                        ? ColorUtils.rgb(70 + n / 2, 65 + n / 2, 60 + n / 2)
                        : ColorUtils.rgb(150 + n, 60 + n / 2, 45 + n / 2);

                // tech panel
                boolean line = (x % 32 == 0) || (y % 32 == 0);
                int base = line ? 30 : 62;
                int c = ColorUtils.rgb(base + n / 2, base + 12 + n / 2, base + 28 + n / 2);
                if (x >= 24 && x <= 40 && y >= 26 && y <= 31) c = ColorUtils.rgb(50, 220 + n, 110);
                if (x >= 10 && x <= 14 && y >= 44 && y <= 56) c = ColorUtils.rgb(220 + n, 60, 50);
                data[1][i] = c;

                // stone
                boolean edge = (x % 32 == 0) || (y % 16 == 0);
                int s = edge ? 55 : 110;
                data[2][i] = ColorUtils.rgb(s + n, s + n, s + 6 + n);

                // rusted metal
                int m = (y % 8 == 0) ? 90 : 140;
                int c3 = ColorUtils.rgb(m + n, (int) (m * 0.65) + n / 2, (int) (m * 0.3));
                int rx = x % 16, ry = y % 16;
                if ((rx - 4) * (rx - 4) + (ry - 4) * (ry - 4) < 5) c3 = ColorUtils.rgb(190, 170, 140);
                data[3][i] = c3;
            }
        }
    }
}

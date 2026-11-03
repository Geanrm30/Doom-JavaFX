package doom.render;

import java.util.Random;

/**
 * Patrones de suelo y techo (64x64). Guardan solo luminosidad: el color lo pone cada nivel.
 * Un valor de 2 o mas indica una lampara (el techo la dibuja encendida).
 */
final class Flats {

    static final int N = 64, KINDS = 3;
    static final float LAMP = 2f;

    final float[][] floor = new float[KINDS][N * N];
    final float[][] ceiling = new float[KINDS][N * N];

    Flats() {
        Random r = new Random(21);
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                int i = y * N + x;
                float n = (r.nextFloat() - 0.5f) * 0.14f;

                // suelo 0: placas de metal con remaches
                boolean seam = x % 32 < 2 || y % 32 < 2;
                float f0 = seam ? 0.35f : 1.0f + n;
                int rx = x % 32, ry = y % 32;
                if (!seam && ((rx == 3 || rx == 29) && (ry == 3 || ry == 29))) f0 = 1.25f;
                if (!seam && (rx == 1 || ry == 1)) f0 += 0.12f;
                floor[0][i] = f0;

                // suelo 1: baldosas alternadas
                boolean tile = ((x / 16) + (y / 16)) % 2 == 0;
                boolean seam1 = x % 16 < 2 || y % 16 < 2;
                floor[1][i] = seam1 ? 0.5f : (tile ? 1.2f : 0.7f) + n;

                // suelo 2: rejilla
                boolean bar = y % 8 < 3;
                boolean cross = x % 32 < 2;
                floor[2][i] = cross ? 0.4f : bar ? 0.55f + n : 1.0f + n;

                // techo 0: paneles con una lampara central
                boolean cseam = x % 32 < 2 || y % 32 < 2;
                float c0 = cseam ? 0.35f : 1.0f + n;
                if (x >= 20 && x < 44 && y >= 27 && y < 37) c0 = LAMP;
                ceiling[0][i] = c0;

                // techo 1: losetas con luces pequenas
                boolean cs1 = x % 16 < 2 || y % 16 < 2;
                float c1 = cs1 ? 0.55f : 1.0f + n;
                if (x % 32 >= 12 && x % 32 < 20 && y % 32 >= 12 && y % 32 < 20) c1 = LAMP;
                ceiling[1][i] = c1;

                // techo 2: tuberias
                float c2 = 0.8f + n;
                int px = x % 32;
                if (px < 10) c2 = 0.5f + 0.5f * (float) Math.sin(px / 9.0 * Math.PI) + n;
                if (x % 32 == 10 || x % 32 == 0) c2 = 0.35f;
                if (y % 32 < 2 && px < 10) c2 = 1.1f;
                ceiling[2][i] = c2;
            }
        }
    }
}

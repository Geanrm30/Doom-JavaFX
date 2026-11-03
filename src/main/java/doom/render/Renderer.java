package doom.render;

import doom.engine.ColorUtils;
import doom.engine.Constants;
import doom.world.Level;
import doom.world.Player;
import doom.world.World;

import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;

public class Renderer {

    private final int[] pixels = new int[Constants.SCREEN_W * Constants.SCREEN_H];
    public final double[] zBuf = new double[Constants.SCREEN_W];
    public final WritableImage frame = new WritableImage(Constants.SCREEN_W, Constants.SCREEN_H);

    private final TextureManager textures;
    private final SpriteRenderer spriteRenderer = new SpriteRenderer();

    public Renderer(TextureManager textures) {
        this.textures = textures;
    }

    public void render(Player player, World world) {
        int W = Constants.SCREEN_W, H = Constants.SCREEN_H, TEX = Constants.TEX_SIZE;

        Level level = world.level;
        int[] cc = level.ceiling, fc = level.floor;
        double dirX = Math.cos(player.angle), dirY = Math.sin(player.angle);
        double planeX = -dirY * 0.66, planeY = dirX * 0.66;

        for (int x = 0; x < W; x++) {
            double camX = 2.0 * x / W - 1;
            double rayX = dirX + planeX * camX;
            double rayY = dirY + planeY * camX;

            int mapX = (int) player.x, mapY = (int) player.y;
            double deltaX = rayX == 0 ? 1e30 : Math.abs(1 / rayX);
            double deltaY = rayY == 0 ? 1e30 : Math.abs(1 / rayY);
            int stepX, stepY;
            double sideX, sideY;
            if (rayX < 0) { stepX = -1; sideX = (player.x - mapX) * deltaX; }
            else           { stepX =  1; sideX = (mapX + 1.0 - player.x) * deltaX; }
            if (rayY < 0) { stepY = -1; sideY = (player.y - mapY) * deltaY; }
            else           { stepY =  1; sideY = (mapY + 1.0 - player.y) * deltaY; }

            int side = 0;
            boolean hit = false;
            int guard = 0;
            // puerta a medio abrir: se recuerda y el rayo sigue hasta la pared del fondo
            boolean hasDoor = false;
            double dPerp = 0, dWallX = 0, dOpen = 0;
            int dSide = 0;
            char dCh = 'D';
            while (!hit && guard++ < 96) {
                if (sideX < sideY) { sideX += deltaX; mapX += stepX; side = 0; }
                else               { sideY += deltaY; mapY += stepY; side = 1; }
                char cc0 = level.cell(mapX, mapY);
                if (cc0 == '.') continue;
                if (Level.isDoor(cc0)) {
                    double o = level.openness(mapX, mapY);
                    if (o >= 0.999) continue;
                    if (o > 0.001) {
                        if (!hasDoor) {
                            hasDoor = true;
                            dOpen = o;
                            dSide = side;
                            dCh = cc0;
                            dPerp = Math.max(0.05, side == 0 ? sideX - deltaX : sideY - deltaY);
                            double wx = side == 0 ? player.y + dPerp * rayY : player.x + dPerp * rayX;
                            dWallX = wx - Math.floor(wx);
                        }
                        continue;
                    }
                }
                hit = true;
            }

            double perp = side == 0 ? sideX - deltaX : sideY - deltaY;
            if (perp < 0.05) perp = 0.05;
            zBuf[x] = perp;

            int lineH = (int) (H / perp);
            int start = Math.max(0, -lineH / 2 + H / 2);
            int end   = Math.min(H - 1, lineH / 2 + H / 2);

            for (int y = 0; y < H; y++) {
                if (y < start) {
                    double f = 0.45 + 1.0 * (1.0 - (double) y / (H / 2.0));
                    pixels[y * W + x] = ColorUtils.rgb((int) (cc[0] * f), (int) (cc[1] * f), (int) (cc[2] * f));
                } else if (y > end) {
                    double f = 0.35 + 1.0 * ((double) (y - H / 2) / (H / 2.0));
                    pixels[y * W + x] = ColorUtils.rgb((int) (fc[0] * f), (int) (fc[1] * f), (int) (fc[2] * f));
                }
            }

            int[] tex = textures.data[TextureManager.indexFor(level.cell(mapX, mapY))];
            double wallX = side == 0 ? player.y + perp * rayY : player.x + perp * rayX;
            wallX -= Math.floor(wallX);
            int texX = (int) (wallX * TEX);
            if ((side == 0 && rayX > 0) || (side == 1 && rayY < 0)) texX = TEX - 1 - texX;

            double step = (double) TEX / lineH;
            double texPos = (start - H / 2.0 + lineH / 2.0) * step;
            double light = Math.max(0.12, Math.min(1.0, 1.0 / (1.0 + perp * level.fog)));
            if (side == 1) light *= 0.72;
            for (int y = start; y <= end; y++) {
                int texY = (int) texPos & (TEX - 1);
                texPos += step;
                pixels[y * W + x] = ColorUtils.shade(tex[texY * TEX + texX], light);
            }

            if (hasDoor) {
                int dLine = (int) (H / dPerp);
                int dTop = -dLine / 2 + H / 2;
                int dBottom = dTop + (int) (dLine * (1 - dOpen));
                int[] dtex = textures.data[TextureManager.indexFor(dCh)];
                int dTexX = (int) (dWallX * TEX);
                if ((dSide == 0 && rayX > 0) || (dSide == 1 && rayY < 0)) dTexX = TEX - 1 - dTexX;
                double dStep = (double) TEX / dLine;
                double dLight = Math.max(0.12, Math.min(1.0, 1.0 / (1.0 + dPerp * level.fog)));
                if (dSide == 1) dLight *= 0.72;
                for (int y = Math.max(0, dTop); y <= Math.min(H - 1, dBottom - 1); y++) {
                    int dTexY = (int) ((y - dTop) * dStep + dOpen * TEX) & (TEX - 1);
                    pixels[y * W + x] = ColorUtils.shade(dtex[dTexY * TEX + dTexX], dLight);
                }
                if (dOpen < 0.7) zBuf[x] = Math.min(zBuf[x], dPerp);
            }
        }

        spriteRenderer.render(pixels, zBuf, player, world, dirX, dirY, planeX, planeY);
        frame.getPixelWriter().setPixels(0, 0, W, H, PixelFormat.getIntArgbInstance(), pixels, 0, W);
    }
}

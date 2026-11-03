package doom.render;

import doom.engine.ColorUtils;
import doom.engine.Constants;
import doom.entities.Sprite;
import doom.world.Player;
import doom.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SpriteRenderer {

    public void render(int[] pixels, double[] zBuf, Player player, World world,
                       double dirX, double dirY, double planeX, double planeY) {
        List<Sprite> all = new ArrayList<>(world.pickups);
        all.addAll(world.enemies);
        all.sort(Comparator.comparingDouble((Sprite s) ->
                -((s.x - player.x) * (s.x - player.x) + (s.y - player.y) * (s.y - player.y))));

        int W = Constants.SCREEN_W, H = Constants.SCREEN_H;
        double invDet = 1.0 / (planeX * dirY - dirX * planeY);

        for (Sprite s : all) {
            double dx = s.x - player.x, dy = s.y - player.y;
            double tx = invDet * (dirY * dx - dirX * dy);
            double ty = invDet * (-planeY * dx + planeX * dy);
            if (ty < 0.15) continue;

            int screenX = (int) (W / 2.0 * (1 + tx / ty));
            int fullH = (int) Math.abs(H / ty);
            int sprH = (int) (fullH * s.scale);
            int floorY = H / 2 + fullH / 2;
            int startY = floorY - sprH;
            int startX = screenX - sprH / 2;
            double light = Math.max(0.2, Math.min(1.0, 1.0 / (1.0 + ty * 0.2)));

            for (int sx = Math.max(0, startX); sx < Math.min(W, startX + sprH); sx++) {
                if (ty >= zBuf[sx]) continue;
                double u = (double) (sx - startX) / sprH;
                for (int sy = Math.max(0, startY); sy < Math.min(H, floorY); sy++) {
                    double v = (double) (sy - startY) / sprH;
                    int color = s.colorAt(u, v);
                    if (color != 0) pixels[sy * W + sx] = ColorUtils.shade(color, light);
                }
            }
        }
    }
}

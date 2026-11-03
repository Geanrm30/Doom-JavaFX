package doom.hud;

import doom.engine.Constants;
import doom.engine.GameState;
import doom.world.Player;
import doom.world.World;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

public class HudRenderer {

    public void draw(GraphicsContext gc, Player player, World world) {
        double cw = Constants.SCREEN_W * Constants.SCALE;
        double ch = Constants.SCREEN_H * Constants.SCALE;

        if (player.dmgFlash > 0) {
            gc.setFill(Color.rgb(200, 0, 0, Math.min(0.5, player.dmgFlash * 2)));
            gc.fillRect(0, 0, cw, ch);
        }
        if (player.pickFlash > 0) {
            gc.setFill(Color.rgb(255, 255, 120, Math.min(0.3, player.pickFlash * 1.5)));
            gc.fillRect(0, 0, cw, ch);
        }

        if (player.state == GameState.PLAYING || player.state == GameState.WON) {
            drawWeapon(gc, player, cw, ch);
        }

        drawCrosshair(gc, cw, ch);
        drawStatusBar(gc, player, world, cw, ch);

        if (player.state != GameState.PLAYING) {
            drawEndScreen(gc, player, cw, ch);
        }
    }

    private void drawWeapon(GraphicsContext gc, Player player, double cw, double ch) {
        double cx = cw / 2;
        double off = player.recoil * 28;
        double by = ch - 60;
        if (player.muzzle > 0) {
            gc.setFill(Color.rgb(255, 200, 60, 0.9));
            gc.fillPolygon(
                new double[]{cx - 40, cx, cx + 40, cx + 14, cx, cx - 14},
                new double[]{by - 170 + off, by - 230 + off, by - 170 + off,
                             by - 140 + off, by - 120 + off, by - 140 + off},
                6);
        }
        gc.setFill(Color.rgb(45, 45, 50));
        gc.fillRect(cx - 18, by - 150 + off, 36, 110);
        gc.setFill(Color.rgb(75, 75, 82));
        gc.fillRect(cx - 12, by - 150 + off, 10, 110);
        gc.setFill(Color.rgb(25, 25, 28));
        gc.fillRect(cx - 14, by - 158 + off, 28, 10);
        gc.setFill(Color.rgb(110, 70, 40));
        gc.fillPolygon(
            new double[]{cx - 50, cx + 50, cx + 34, cx - 34},
            new double[]{by + 60 + off, by + 60 + off, by - 40 + off, by - 40 + off},
            4);
    }

    private void drawCrosshair(GraphicsContext gc, double cw, double ch) {
        gc.setStroke(Color.rgb(240, 240, 240, 0.8));
        gc.setLineWidth(1.5);
        gc.strokeLine(cw / 2 - 8, ch / 2, cw / 2 - 3, ch / 2);
        gc.strokeLine(cw / 2 + 3, ch / 2, cw / 2 + 8, ch / 2);
        gc.strokeLine(cw / 2, ch / 2 - 8, cw / 2, ch / 2 - 3);
        gc.strokeLine(cw / 2, ch / 2 + 3, cw / 2, ch / 2 + 8);
    }

    private void drawStatusBar(GraphicsContext gc, Player player, World world, double cw, double ch) {
        gc.setFill(Color.rgb(30, 28, 26));
        gc.fillRect(0, ch - 56, cw, 56);
        gc.setStroke(Color.rgb(90, 80, 70));
        gc.setLineWidth(1);
        gc.strokeLine(0, ch - 56, cw, ch - 56);

        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 26));
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFill(player.health > 30 ? Color.rgb(230, 200, 90) : Color.rgb(230, 60, 50));
        gc.fillText("HEALTH " + player.health + "%", 24, ch - 18);
        gc.setFill(Color.rgb(230, 200, 90));
        gc.fillText("AMMO " + player.ammo, 330, ch - 18);
        long alive = world.enemies.stream().filter(e -> e.alive).count();
        gc.fillText("DEMONS " + alive, 640, ch - 18);
    }

    private void drawEndScreen(GraphicsContext gc, Player player, double cw, double ch) {
        gc.setFill(Color.rgb(0, 0, 0, 0.55));
        gc.fillRect(0, ch / 2 - 70, cw, 130);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 52));
        if (player.state == GameState.DEAD) {
            gc.setFill(Color.rgb(220, 40, 30));
            gc.fillText("YOU DIED", cw / 2, ch / 2 - 5);
        } else {
            gc.setFill(Color.rgb(120, 230, 120));
            gc.fillText("LEVEL COMPLETE", cw / 2, ch / 2 - 5);
        }
        gc.setFont(Font.font("Monospaced", FontWeight.NORMAL, 22));
        gc.setFill(Color.WHITE);
        gc.fillText("Press R to restart", cw / 2, ch / 2 + 40);
    }
}

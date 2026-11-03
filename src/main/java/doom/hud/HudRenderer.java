package doom.hud;

import doom.engine.Constants;
import doom.engine.GameState;
import doom.engine.I18n;
import doom.entities.Enemy;
import doom.world.Level;
import doom.world.Player;
import doom.world.Weapon;
import doom.world.World;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

public class HudRenderer {

    private static final double CW = Constants.SCREEN_W * Constants.SCALE;
    private static final double CH = Constants.SCREEN_H * Constants.SCALE;

    public void draw(GraphicsContext gc, World world) {
        Player player = world.player;
        GameState st = world.state;

        if (st == GameState.TITLE) {
            drawTitle(gc, world);
            drawMessage(gc, world);
            return;
        }

        if (player.dmgFlash > 0) {
            gc.setFill(Color.rgb(200, 0, 0, Math.min(0.5, player.dmgFlash * 2)));
            gc.fillRect(0, 0, CW, CH);
        }
        if (player.invuln > 0) {
            double a = player.invuln < 3 ? 0.12 + 0.1 * Math.sin(player.invuln * 12) : 0.22;
            gc.setFill(Color.rgb(120, 255, 140, Math.max(0.05, a)));
            gc.fillRect(0, 0, CW, CH);
        }
        if (player.pickFlash > 0) {
            gc.setFill(Color.rgb(255, 255, 120, Math.min(0.3, player.pickFlash * 1.5)));
            gc.fillRect(0, 0, CW, CH);
        }

        if (st == GameState.PLAYING) drawWeapon(gc, player);
        drawCrosshair(gc);
        if (st == GameState.PLAYING && player.execT > 0) drawExecution(gc, player);
        if (world.showMap) drawMiniMap(gc, world);
        drawStatusBar(gc, world);
        drawMessage(gc, world);
        drawBossBar(gc, world);
        if (world.paused && st == GameState.PLAYING) {
            drawPause(gc, world);
            drawMessage(gc, world);
        }

        if (world.wheelOpen && st == GameState.PLAYING) drawWheel(gc, world);
        if (world.consoleOpen) drawConsole(gc, world);

        switch (st) {
            case DEAD -> drawBanner(gc, "HAS MUERTO", Color.rgb(220, 40, 30), "ENTER: reintentar el nivel");
            case LEVEL_DONE -> drawIntermission(gc, world, false);
            case WON -> drawIntermission(gc, world, true);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ armas

    private void drawWeapon(GraphicsContext gc, Player p) {
        Weapon w = p.weapon;
        if (w == Weapon.KATANA) {
            drawKatana(gc, p);
            return;
        }
        double cx = CW / 2 + Math.sin(p.bobPhase) * p.bobAmp * 12;
        double off = p.recoil * 28 + Math.abs(Math.cos(p.bobPhase)) * p.bobAmp * 7;
        double by = CH - 60;
        double rp = p.reloadT > 0 ? 1 - p.reloadT / p.reloadTotal : -1;
        double elapsed = rp >= 0 ? rp * p.reloadTotal : 0;
        if (rp >= 0) {
            double tilt;
            if (p.reloadByRound) {
                off += 50 + 16 * Math.sin(elapsed / 0.45 * 2 * Math.PI);
                tilt = 12;
            } else {
                double d = Math.sin(Math.PI * rp);
                off += d * 125;
                tilt = d * 10;
            }
            gc.save();
            gc.translate(cx, CH);
            gc.rotate(tilt);
            gc.translate(-cx, -CH);
        }

        if (p.muzzle > 0) {
            gc.setFill(Color.rgb(255, 200, 60, 0.9));
            double s = w == Weapon.SHOTGUN ? 1.6 : 1.0;
            if (w == Weapon.PLASMA) gc.setFill(Color.rgb(90, 200, 255, 0.9));
            else if (w == Weapon.ROCKET) gc.setFill(Color.rgb(255, 150, 40, 0.9));
            gc.fillPolygon(
                new double[]{cx - 40 * s, cx, cx + 40 * s, cx + 14 * s, cx, cx - 14 * s},
                new double[]{by - 170 + off, by - 230 - 20 * s + off, by - 170 + off,
                             by - 140 + off, by - 120 + off, by - 140 + off},
                6);
        }
        switch (w) {
            case PISTOL -> {
                gc.setFill(Color.rgb(45, 45, 50));
                gc.fillRect(cx - 18, by - 150 + off, 36, 110);
                gc.setFill(Color.rgb(75, 75, 82));
                gc.fillRect(cx - 12, by - 150 + off, 10, 110);
                gc.setFill(Color.rgb(25, 25, 28));
                gc.fillRect(cx - 14, by - 158 + off, 28, 10);
            }
            case SHOTGUN -> {
                gc.setFill(Color.rgb(55, 55, 60));
                gc.fillRect(cx - 30, by - 150 + off, 26, 120);
                gc.fillRect(cx + 4, by - 150 + off, 26, 120);
                gc.setFill(Color.rgb(90, 90, 98));
                gc.fillRect(cx - 26, by - 150 + off, 7, 120);
                gc.fillRect(cx + 8, by - 150 + off, 7, 120);
                gc.setFill(Color.rgb(20, 20, 22));
                gc.fillRect(cx - 30, by - 156 + off, 60, 8);
                gc.setFill(Color.rgb(120, 78, 40));
                gc.fillRect(cx - 34, by - 70 + off, 68, 34);
            }
            case ROCKET -> {
                gc.setFill(Color.rgb(70, 82, 66));
                gc.fillRect(cx - 40, by - 130 + off, 80, 95);
                gc.setFill(Color.rgb(100, 116, 94));
                gc.fillRect(cx - 40, by - 130 + off, 22, 95);
                gc.setFill(Color.rgb(20, 22, 20));
                gc.fillOval(cx - 34, by - 160 + off, 68, 50);
                gc.setFill(Color.rgb(8, 8, 8));
                gc.fillOval(cx - 26, by - 154 + off, 52, 38);
                gc.setFill(Color.rgb(180, 150, 50));
                gc.fillRect(cx - 40, by - 62 + off, 80, 8);
            }
            case PLASMA -> {
                gc.setFill(Color.rgb(52, 56, 74));
                gc.fillRect(cx - 28, by - 140 + off, 56, 100);
                gc.setFill(Color.rgb(90, 96, 120));
                gc.fillRect(cx - 28, by - 140 + off, 12, 100);
                double glow = 0.6 + 0.4 * Math.sin(System.currentTimeMillis() / 140.0);
                gc.setFill(Color.rgb(60, (int) (170 + 60 * glow), 255));
                gc.fillRect(cx - 8, by - 132 + off, 16, 70);
                gc.setFill(Color.rgb(20, 24, 34));
                gc.fillRect(cx - 34, by - 150 + off, 68, 14);
                gc.setFill(Color.rgb(120, 220, 255));
                gc.fillRect(cx - 20, by - 156 + off, 40, 6);
            }
            case CHAINGUN -> {
                gc.setFill(Color.rgb(40, 40, 46));
                gc.fillRect(cx - 34, by - 140 + off, 68, 100);
                for (int i = 0; i < 4; i++) {
                    gc.setFill(i % 2 == 0 ? Color.rgb(85, 85, 92) : Color.rgb(25, 25, 30));
                    gc.fillRect(cx - 34 + i * 17, by - 160 + off, 15, 30);
                }
                gc.setFill(Color.rgb(160, 150, 60));
                gc.fillRect(cx - 40, by - 60 + off, 80, 8);
            }
        }
        gc.setFill(Color.rgb(110, 70, 40));
        gc.fillPolygon(
            new double[]{cx - 50, cx + 50, cx + 34, cx - 34},
            new double[]{by + 60 + off, by + 60 + off, by - 40 + off, by - 40 + off},
            4);
        if (rp >= 0) {
            drawReloadProps(gc, p, w, rp, elapsed, cx, by, off);
            gc.restore();
        }
    }

    /** Cargador, cartucho o cohete que entra y sale durante la recarga. */
    private void drawReloadProps(GraphicsContext gc, Player p, Weapon w, double rp, double elapsed, double cx, double by, double off) {
        double slotY = by - 50 + off;
        switch (w) {
            case PISTOL, CHAINGUN, PLASMA -> {
                double mw = w == Weapon.CHAINGUN ? 34 : w == Weapon.PLASMA ? 20 : 14;
                double mh = w == Weapon.CHAINGUN ? 40 : 36;
                double mx = cx + (w == Weapon.CHAINGUN ? 22 : 0) - mw / 2;
                Color body = w == Weapon.PLASMA ? Color.rgb(90, 210, 255) : w == Weapon.CHAINGUN ? Color.rgb(165, 150, 60) : Color.rgb(70, 70, 78);
                double y;
                if (rp < 0.35) y = slotY + rp / 0.35 * 230;
                else if (rp < 0.55) return;
                else y = slotY + (1 - Math.min(1, (rp - 0.55) / 0.3)) * 230;
                gc.setFill(Color.rgb(15, 15, 18));
                gc.fillRect(mx - 2, y - 2, mw + 4, mh + 4);
                gc.setFill(body);
                gc.fillRect(mx, y, mw, mh);
                gc.setFill(Color.rgb(255, 255, 255, 0.25));
                gc.fillRect(mx, y, mw * 0.3, mh);
            }
            case ROCKET -> {
                if (rp < 0.35 || rp > 0.85) return;
                double k = Math.min(1, (rp - 0.35) / 0.4);
                double y = slotY + (1 - k) * 240;
                gc.setFill(Color.rgb(150, 60, 50));
                gc.fillRect(cx - 12, y, 24, 70);
                gc.setFill(Color.rgb(205, 205, 210));
                gc.fillPolygon(new double[]{cx - 12, cx + 12, cx}, new double[]{y, y, y - 26}, 3);
                gc.setFill(Color.rgb(60, 60, 64));
                gc.fillRect(cx - 18, y + 60, 36, 10);
            }
            case SHOTGUN -> {
                double k = (elapsed / 0.45) % 1.0;
                double y = by + 140 - k * 190;
                gc.setFill(Color.rgb(190, 40, 35));
                gc.fillRect(cx - 7, y, 14, 24);
                gc.setFill(Color.rgb(205, 170, 80));
                gc.fillRect(cx - 7, y + 20, 14, 8);
            }
            default -> { }
        }
    }

    // ------------------------------------------------------------------ katana

    private void drawKatana(GraphicsContext gc, Player p) {
        double cx = CW / 2 + Math.sin(p.bobPhase) * p.bobAmp * 10;
        double bob = Math.abs(Math.cos(p.bobPhase)) * p.bobAmp * 6;
        double t = -1, side = p.slashSide > 0.5 ? -1 : 1;
        if (p.execT > 0) {
            double tt = 1 - p.execT / p.execTotal;
            if (tt < 0.5) { t = Math.max(0, Math.min(1, (tt - 0.08) / 0.32)); side = 1; }
            else { t = Math.max(0, Math.min(1, (tt - 0.5) / 0.32)); side = -1; }
            if (tt < 0.08) t = -1;
        } else if (p.slash > 0) {
            t = 1 - p.slash;
        }
        double pivotX = cx + side * 130, pivotY = CH - 20 + bob;
        double angDeg;
        if (t >= 0) {
            double e = 1 - Math.pow(1 - t, 3);
            angDeg = side * (38 + (-88 - 38) * e);
        } else {
            angDeg = side * -10;
        }
        double L = 330;
        if (t >= 0 && t < 0.95) {
            // estela del tajo
            double a0 = Math.toRadians(side * 38), a1 = Math.toRadians(angDeg);
            int n = 14;
            double[] xs = new double[n + 1], ys = new double[n + 1];
            xs[0] = pivotX; ys[0] = pivotY;
            for (int i = 0; i < n; i++) {
                double a = a0 + (a1 - a0) * i / (n - 1);
                xs[i + 1] = pivotX + Math.sin(a) * L;
                ys[i + 1] = pivotY - Math.cos(a) * L;
            }
            gc.setFill(Color.rgb(235, 240, 255, 0.28 * (1 - t)));
            gc.fillPolygon(xs, ys, n + 1);
        }
        gc.save();
        gc.translate(pivotX, pivotY);
        gc.rotate(angDeg);
        // empunadura
        gc.setFill(Color.rgb(28, 24, 30));
        gc.fillRect(-11, -90, 22, 120);
        gc.setFill(Color.rgb(150, 30, 35));
        for (int i = 0; i < 5; i++) gc.fillPolygon(new double[]{-9, 9, 0}, new double[]{-82 + i * 20, -82 + i * 20, -72 + i * 20}, 3);
        // guarda
        gc.setFill(Color.rgb(200, 165, 60));
        gc.fillOval(-26, -98, 52, 14);
        // hoja
        gc.setFill(Color.rgb(228, 233, 242));
        gc.fillPolygon(new double[]{-10, 0, 0, -10}, new double[]{-96, -96, -L, -L + 46}, 4);
        gc.setFill(Color.rgb(150, 156, 172));
        gc.fillPolygon(new double[]{0, 10, 0}, new double[]{-96, -96, -L}, 3);
        gc.setStroke(Color.rgb(255, 255, 255, 0.85));
        gc.setLineWidth(1.2);
        gc.strokeLine(-10, -96, -10, -L + 46);
        gc.restore();
    }

    /** Pantalla de ejecucion: barras de cine, dos cortes cruzados y destello rojo. */
    private void drawExecution(GraphicsContext gc, Player p) {
        double tt = 1 - p.execT / p.execTotal;
        double bars = 46 * Math.sin(Math.PI * Math.min(1, tt));
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, CW, bars);
        gc.fillRect(0, CH - bars, CW, bars);
        double k1 = Math.max(0, Math.min(1, (tt - 0.1) / 0.25));
        double k2 = Math.max(0, Math.min(1, (tt - 0.5) / 0.25));
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        for (int s = 0; s < 2; s++) {
            double k = s == 0 ? k1 : k2;
            if (k <= 0) continue;
            double fade = s == 0 ? 1 - Math.max(0, (tt - 0.45) / 0.4) : 1 - Math.max(0, (tt - 0.85) / 0.15);
            fade = Math.max(0, Math.min(1, fade));
            double x0 = s == 0 ? CW * 0.12 : CW * 0.88, y0 = CH * 0.12, x1 = s == 0 ? CW * 0.88 : CW * 0.12, y1 = CH * 0.88;
            double ex = x0 + (x1 - x0) * k, ey = y0 + (y1 - y0) * k;
            gc.setStroke(Color.rgb(220, 20, 20, 0.55 * fade));
            gc.setLineWidth(22);
            gc.strokeLine(x0, y0, ex, ey);
            gc.setStroke(Color.rgb(255, 255, 255, 0.95 * fade));
            gc.setLineWidth(6);
            gc.strokeLine(x0, y0, ex, ey);
        }
        if (tt > 0.5) {
            double a = Math.max(0, 0.45 - (tt - 0.5) * 0.9);
            gc.setFill(Color.rgb(190, 0, 0, a));
            gc.fillRect(0, 0, CW, CH);
        }
    }

    private void drawCrosshair(GraphicsContext gc) {
        gc.setStroke(Color.rgb(240, 240, 240, 0.8));
        gc.setLineWidth(1.5);
        gc.strokeLine(CW / 2 - 8, CH / 2, CW / 2 - 3, CH / 2);
        gc.strokeLine(CW / 2 + 3, CH / 2, CW / 2 + 8, CH / 2);
        gc.strokeLine(CW / 2, CH / 2 - 8, CW / 2, CH / 2 - 3);
        gc.strokeLine(CW / 2, CH / 2 + 3, CW / 2, CH / 2 + 8);
    }

    // ------------------------------------------------------------------ minimapa

    private void drawMiniMap(GraphicsContext gc, World world) {
        Level lv = world.level;
        Player p = world.player;
        double cell = 4;
        double ox = 10, oy = 10;
        gc.setFill(Color.rgb(0, 0, 0, 0.45));
        gc.fillRect(ox - 4, oy - 4, lv.width * cell + 8, lv.height * cell + 8);
        for (int y = 0; y < lv.height; y++) {
            for (int x = 0; x < lv.width; x++) {
                if (!lv.seen[y][x]) continue;
                char c = lv.cells[y][x];
                Color col = switch (c) {
                    case '.' -> Color.rgb(60, 60, 70);
                    case 'D' -> Color.rgb(210, 180, 40);
                    case 'R' -> Color.rgb(230, 50, 50);
                    case 'U' -> Color.rgb(60, 100, 255);
                    case 'X' -> Color.rgb(80, 255, 120);
                    default -> Color.rgb(170, 170, 180);
                };
                gc.setFill(col);
                gc.fillRect(ox + x * cell, oy + y * cell, cell, cell);
            }
        }
        double px = ox + p.x * cell, py = oy + p.y * cell;
        gc.setFill(Color.rgb(255, 255, 90));
        gc.fillOval(px - 2, py - 2, 4, 4);
        gc.setStroke(Color.rgb(255, 255, 90));
        gc.setLineWidth(1);
        gc.strokeLine(px, py, px + Math.cos(p.angle) * 8, py + Math.sin(p.angle) * 8);
    }

    // ------------------------------------------------------------------ barra de estado

    private void drawStatusBar(GraphicsContext gc, World world) {
        Player p = world.player;
        gc.setFill(Color.rgb(30, 28, 26));
        gc.fillRect(0, CH - 56, CW, 56);
        gc.setStroke(Color.rgb(90, 80, 70));
        gc.setLineWidth(1);
        gc.strokeLine(0, CH - 56, CW, CH - 56);

        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 22));
        gc.setFill(p.health > 100 ? Color.rgb(110, 190, 255) : p.health > 30 ? Color.rgb(230, 200, 90) : Color.rgb(230, 60, 50));
        gc.fillText(I18n.tr(String.valueOf("SALUD " + p.health + "%")), 16, CH - 30);
        gc.setFill(Color.rgb(110, 200, 130));
        gc.fillText(I18n.tr(String.valueOf("ARMAD " + p.armor + "%")), 16, CH - 8);

        gc.setFill(Color.rgb(230, 200, 90));
        gc.fillText(I18n.tr(String.valueOf(p.weapon.label)), 250, CH - 30);
        if (!p.weapon.isMelee()) {
            int ld = p.loaded[p.weapon.ordinal()];
            boolean rel = p.reloadT > 0;
            gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 16));
            gc.setFill(rel ? Color.rgb(255, 150, 60) : ld == 0 ? Color.rgb(230, 60, 50) : Color.rgb(255, 255, 255));
            gc.setTextAlign(TextAlignment.RIGHT);
            gc.fillText(I18n.tr(String.valueOf(rel ? "RECARGANDO" : ld + "/" + p.weapon.mag)), 548, CH - 30);
            gc.setTextAlign(TextAlignment.LEFT);
        }
        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 14));
        gc.setFill(Color.rgb(230, 200, 90));
        gc.fillText(I18n.tr(String.valueOf(I18n.pick("BAL ", "BUL ") + p.ammo[0] + I18n.pick(" CAR ", " SHL ") + p.ammo[1] + I18n.pick(" COH ", " RKT ") + p.ammo[2] + " CEL " + p.ammo[3])), 250, CH - 8);

        // llaves
        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 18));
        gc.setFill(Color.rgb(200, 200, 200));
        gc.fillText(I18n.tr(String.valueOf("LLAVES")), 560, CH - 30);
        gc.setFill(p.redKey ? Color.rgb(230, 40, 40) : Color.rgb(60, 40, 40));
        gc.fillRect(560, CH - 24, 22, 14);
        gc.setFill(p.blueKey ? Color.rgb(50, 90, 240) : Color.rgb(40, 44, 65));
        gc.fillRect(590, CH - 24, 22, 14);

        long alive = world.enemies.stream().filter(e -> e.alive).count();
        gc.setFill(Color.rgb(230, 200, 90));
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 20));
        gc.fillText(I18n.tr(String.valueOf("DEMONIOS " + alive)), CW - 14, CH - 30);
        gc.setFont(Font.font("Monospaced", FontWeight.NORMAL, 15));
        gc.setFill(Color.rgb(190, 180, 160));
        gc.fillText(I18n.tr(String.valueOf(world.levelIndex + "/" + world.levelCount + "  " + world.level.name)), CW - 14, CH - 8);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    private void drawBossBar(GraphicsContext gc, World world) {
        Enemy boss = null;
        for (Enemy e : world.enemies) {
            if (e.alive && e.type.isBoss() && e.awake) { boss = e; break; }
        }
        if (boss == null) return;
        double w = 360, x = (CW - w) / 2, y = 62;
        gc.setFill(Color.rgb(0, 0, 0, 0.7));
        gc.fillRect(x - 3, y - 3, w + 6, 20);
        gc.setFill(Color.rgb(200, 30, 30));
        gc.fillRect(x, y, w * Math.max(0, boss.hp) / boss.type.hp, 14);
        gc.setStroke(Color.rgb(230, 200, 90));
        gc.setLineWidth(1);
        gc.strokeRect(x - 3, y - 3, w + 6, 20);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 14));
        gc.setFill(Color.rgb(255, 235, 130));
        gc.fillText(I18n.tr(String.valueOf(boss.type.label.toUpperCase())), CW / 2, y + 36);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    private void drawMessage(GraphicsContext gc, World world) {
        if (world.messageTimer <= 0 || world.message.isEmpty()) return;
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 22));
        gc.setFill(Color.rgb(0, 0, 0, 0.6));
        gc.fillText(I18n.tr(String.valueOf(world.message)), CW / 2 + 2, 44 + 2);
        gc.setFill(Color.rgb(255, 235, 130, Math.min(1, world.messageTimer * 2)));
        gc.fillText(I18n.tr(String.valueOf(world.message)), CW / 2, 44);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    // ------------------------------------------------------------------ pantallas

    private void drawBanner(GraphicsContext gc, String title, Color color, String sub) {
        gc.setFill(Color.rgb(0, 0, 0, 0.55));
        gc.fillRect(0, CH / 2 - 70, CW, 130);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 52));
        gc.setFill(color);
        gc.fillText(I18n.tr(String.valueOf(title)), CW / 2, CH / 2 - 5);
        gc.setFont(Font.font("Monospaced", FontWeight.NORMAL, 22));
        gc.setFill(Color.WHITE);
        gc.fillText(I18n.tr(String.valueOf(sub)), CW / 2, CH / 2 + 40);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    private void drawIntermission(GraphicsContext gc, World world, boolean last) {
        gc.setFill(Color.rgb(0, 0, 0, 0.82));
        gc.fillRect(0, 0, CW, CH);
        gc.setFill(Color.BLACK);
        gc.fillRect(0, CH - 56, CW, 56);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 44));
        gc.setFill(Color.rgb(120, 230, 120));
        gc.fillText(I18n.tr(String.valueOf(last ? "HAS TERMINADO EL JUEGO" : "NIVEL COMPLETADO")), CW / 2, 120);
        gc.setFont(Font.font("Monospaced", FontWeight.NORMAL, 24));
        gc.setFill(Color.rgb(230, 200, 90));
        gc.fillText(I18n.tr(String.valueOf(world.level.name)), CW / 2, 160);

        int secs = (int) world.time;
        java.util.List<String> list = new java.util.ArrayList<>();
        list.add("Enemigos  " + world.kills + " / " + world.totalEnemies);
        list.add("Objetos   " + Math.min(world.items, world.totalItems) + " / " + world.totalItems);
        if (world.level.secretCount > 0) list.add("Secretos  " + world.secretsFound + " / " + world.level.secretCount);
        list.add(String.format("Tiempo    %d:%02d", secs / 60, secs % 60));
        String[] lines = list.toArray(new String[0]);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 28));
        for (int i = 0; i < lines.length; i++) gc.fillText(I18n.tr(String.valueOf(lines[i])), CW / 2, 250 + i * 46);

        gc.setFont(Font.font("Monospaced", FontWeight.NORMAL, 20));
        gc.setFill(Color.rgb(190, 190, 190));
        gc.fillText(I18n.tr(String.valueOf(last ? "ENTER: volver al titulo" : "ENTER: siguiente nivel (" + (world.levelIndex + 1) + ")")),
                CW / 2, CH - 90);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    // ------------------------------------------------------------------ menus

    private static final double MENU_W = 640;

    /** {top, filaAlto, primeraVisible, maxVisibles} segun la pantalla del menu. */
    private double[] layout(World world, int n) {
        double top, rowH;
        int visible = n;
        switch (world.screen) {
            case MAIN -> { top = 262; rowH = 42; }
            case LEVELS -> { top = 128; rowH = 40; visible = Math.min(n, 10); }
            default -> { top = 196; rowH = 48; }
        }
        int first = Math.max(0, Math.min(world.menuIndex - visible + 1, n - visible));
        if (world.menuIndex < first) first = world.menuIndex;
        return new double[]{top, rowH, first, visible};
    }

    /** Fila del menu bajo la posicion (en coordenadas virtuales 960x600), o -1. */
    public int rowAt(World world, double vx, double vy) {
        if (Math.abs(vx - CW / 2) > MENU_W / 2) return -1;
        double[] l = layout(world, world.menuRows().size());
        int i = (int) Math.floor((vy - l[0]) / l[1]);
        if (vy < l[0] || i < 0 || i >= (int) l[3]) return -1;
        return (int) l[2] + i;
    }

    /** -1 / +1 si el punto cae en la mitad izquierda / derecha de la zona del valor; 0 en otro caso. */
    public int sideAt(double vx) {
        double right = CW / 2 + MENU_W / 2;
        if (vx < right - 230 || vx > right) return 0;
        return vx < right - 115 ? -1 : 1;
    }

    private void drawMenu(GraphicsContext gc, World world) {
        java.util.List<World.Row> rows = world.menuRows();
        double[] l = layout(world, rows.size());
        double x0 = CW / 2 - MENU_W / 2;
        for (int i = 0; i < (int) l[3]; i++) {
            int idx = (int) l[2] + i;
            World.Row row = rows.get(idx);
            double y = l[0] + i * l[1];
            boolean sel = idx == world.menuIndex;
            if (sel) {
                gc.setFill(Color.rgb(120, 14, 10, 0.85));
                gc.fillRect(x0, y, MENU_W, l[1] - 6);
                gc.setStroke(Color.rgb(230, 200, 90));
                gc.setLineWidth(1);
                gc.strokeRect(x0 + 0.5, y + 0.5, MENU_W - 1, l[1] - 7);
            }
            Color text = !row.enabled() ? Color.rgb(110, 110, 110)
                    : sel ? Color.rgb(255, 235, 130) : Color.rgb(215, 215, 215);
            gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 22));
            gc.setTextAlign(TextAlignment.LEFT);
            gc.setFill(text);
            gc.fillText(I18n.tr(String.valueOf((sel ? "> " : "  ") + row.label())), x0 + 16, y + l[1] / 2 + 4);
            if (!row.value().isEmpty()) {
                gc.setTextAlign(TextAlignment.RIGHT);
                boolean adjustable = (world.screen == World.Screen.MAIN && idx == 2)
                        || (world.screen == World.Screen.OPTIONS && idx <= 4);
                String v = sel && adjustable ? "<  " + row.value() + "  >" : row.value();
                gc.setFill(row.enabled() ? (sel ? Color.rgb(255, 235, 130) : Color.rgb(230, 200, 90))
                        : Color.rgb(150, 70, 60));
                gc.fillText(I18n.tr(String.valueOf(v)), x0 + MENU_W - 16, y + l[1] / 2 + 4);
            }
        }
        if (rows.size() > (int) l[3]) {
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setFont(Font.font("Monospaced", FontWeight.NORMAL, 14));
            gc.setFill(Color.rgb(150, 150, 150));
            if (l[2] > 0) gc.fillText(I18n.tr(String.valueOf("(mas arriba)")), CW / 2, l[0] - 8);
            if (l[2] + l[3] < rows.size()) gc.fillText(I18n.tr(String.valueOf("(mas abajo)")), CW / 2, l[0] + l[3] * l[1] + 6);
        }
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Monospaced", FontWeight.NORMAL, 15));
        gc.setFill(Color.rgb(150, 150, 150));
        gc.fillText(I18n.tr(String.valueOf("ARRIBA/ABAJO mover    ENTER elegir    IZQ/DER cambiar    ESC volver")), CW / 2, CH - 16);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    private void drawTitle(GraphicsContext gc, World world) {
        gc.setFill(Color.rgb(10, 0, 0));
        gc.fillRect(0, 0, CW, CH);
        for (int i = 0; i < 12; i++) {
            gc.setFill(Color.rgb(90 - i * 6, 8, 5, 0.5));
            gc.fillRect(0, CH - 40 - i * 14, CW, 14);
        }
        gc.setTextAlign(TextAlignment.CENTER);
        if (world.screen == World.Screen.MAIN) {
            gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 84));
            gc.setFill(Color.rgb(20, 0, 0));
            gc.fillText(I18n.tr(String.valueOf("DOOM")), CW / 2 + 5, 135);
            gc.setFill(Color.rgb(210, 40, 20));
            gc.fillText(I18n.tr(String.valueOf("DOOM")), CW / 2, 130);
            gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 20));
            gc.setFill(Color.rgb(230, 200, 90));
            gc.fillText(I18n.tr(String.valueOf("JAVAFX RAYCASTER")), CW / 2, 168);
            int done = Math.min(world.progress.unlocked - 1, world.levelCount);
            gc.setFont(Font.font("Monospaced", FontWeight.NORMAL, 17));
            gc.setFill(Color.rgb(190, 190, 190));
            gc.fillText(I18n.tr(String.valueOf("Niveles completados: " + done + " / " + world.levelCount
                    + "    Desbloqueado: " + world.progress.unlocked)), CW / 2, 205);
            gc.setFont(Font.font("Monospaced", FontWeight.NORMAL, 15));
            gc.setFill(Color.rgb(140, 140, 140));
            String[] help = {
                "W A S D mover   raton o Q E girar   SHIFT correr   clic o ESPACIO disparar",
                "F usar   R recargar   1-6 o TAB armas   N minimapa   P o ESC pausa   F11 pantalla"
            };
            for (int i = 0; i < help.length; i++) gc.fillText(I18n.tr(String.valueOf(help[i])), CW / 2, 500 + i * 22);
        } else {
            gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 38));
            gc.setFill(Color.rgb(210, 40, 20));
            gc.fillText(I18n.tr(String.valueOf(world.menuHeader())), CW / 2, 82);
        }
        gc.setTextAlign(TextAlignment.LEFT);
        drawMenu(gc, world);
    }

    /** Rueda de armas (mantener TAB): un sector por arma, el cursor resalta el que se equipara al soltar. */
    private void drawWheel(GraphicsContext gc, World world) {
        Player p = world.player;
        Weapon[] ws = Weapon.values();
        int n = ws.length;
        double cx = CW / 2, cy = CH / 2 - 14, ro = 158, ri = 62;
        gc.setFill(Color.rgb(0, 0, 0, 0.5));
        gc.fillRect(0, 0, CW, CH);
        double seg = 2 * Math.PI / n, gap = 0.035;
        for (int i = 0; i < n; i++) {
            double mid = -Math.PI / 2 + i * seg;
            double a0 = mid - seg / 2 + gap, a1 = mid + seg / 2 - gap;
            int pts = 16;
            double[] xs = new double[pts * 2], ys = new double[pts * 2];
            for (int k = 0; k < pts; k++) {
                double a = a0 + (a1 - a0) * k / (pts - 1);
                xs[k] = cx + Math.cos(a) * ro;
                ys[k] = cy + Math.sin(a) * ro;
                xs[pts * 2 - 1 - k] = cx + Math.cos(a) * ri;
                ys[pts * 2 - 1 - k] = cy + Math.sin(a) * ri;
            }
            boolean sel = i == world.wheelSel, own = p.has[i];
            gc.setFill(sel ? Color.rgb(235, 190, 60, 0.88) : own ? Color.rgb(30, 30, 34, 0.82) : Color.rgb(20, 20, 22, 0.55));
            gc.fillPolygon(xs, ys, pts * 2);
            gc.setStroke(sel ? Color.rgb(255, 235, 150) : Color.rgb(110, 110, 118, own ? 0.9 : 0.4));
            gc.setLineWidth(sel ? 2.5 : 1);
            gc.strokePolygon(xs, ys, pts * 2);

            double lx = cx + Math.cos(mid) * (ri + ro) / 2, ly = cy + Math.sin(mid) * (ri + ro) / 2;
            Color ink = sel ? Color.rgb(25, 18, 5) : own ? Color.rgb(225, 225, 225) : Color.rgb(110, 110, 110);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setFill(ink);
            gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 13));
            gc.fillText(I18n.tr(String.valueOf(ws[i].label)), lx, ly - 2);
            gc.setFont(Font.font("Monospaced", FontWeight.NORMAL, 13));
            gc.fillText(I18n.tr(String.valueOf(!own ? "---" : ws[i].isMelee() ? "CUERPO" : p.loaded[i] + "/" + p.ammoFor(ws[i]))), lx, ly + 15);
            gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 12));
            gc.setFill(Color.rgb(200, 200, 200));
            gc.fillText(I18n.tr(String.valueOf(Integer.toString(i + 1))), cx + Math.cos(mid) * (ro + 14), cy + Math.sin(mid) * (ro + 14) + 4);
        }
        Weapon cur = ws[world.wheelSel];
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(p.has[world.wheelSel] ? Color.rgb(255, 235, 150) : Color.rgb(150, 150, 150));
        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 13));
        gc.fillText(I18n.tr(String.valueOf(p.has[world.wheelSel] ? "ELEGIR" : "SIN ARMA")), cx, cy - 2);
        gc.setFont(Font.font("Monospaced", FontWeight.NORMAL, 11));
        gc.setFill(Color.rgb(190, 190, 190));
        gc.fillText(I18n.tr(String.valueOf(cur.label)), cx, cy + 14);
        gc.setFont(Font.font("Monospaced", FontWeight.NORMAL, 15));
        gc.setFill(Color.rgb(210, 210, 210));
        gc.fillText(I18n.tr(String.valueOf("Mueve el raton y suelta TAB (o haz clic) para equipar")), cx, cy + ro + 34);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    private void drawConsole(GraphicsContext gc, World world) {
        gc.setFill(Color.rgb(0, 0, 0, 0.82));
        gc.fillRect(0, 0, CW, 78);
        gc.setStroke(Color.rgb(230, 200, 90));
        gc.setLineWidth(1);
        gc.strokeLine(0, 78.5, CW, 78.5);
        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 22));
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFill(Color.rgb(255, 235, 130));
        boolean caret = (System.currentTimeMillis() / 500) % 2 == 0;
        gc.fillText(I18n.tr(String.valueOf("> " + world.consoleLine + (caret ? "_" : ""))), 16, 32);
        gc.setFont(Font.font("Monospaced", FontWeight.NORMAL, 16));
        gc.setFill(Color.rgb(170, 170, 170));
        gc.fillText(I18n.tr(String.valueOf(world.consoleReply)), 16, 60);
    }

    private void drawPause(GraphicsContext gc, World world) {
        gc.setFill(Color.rgb(0, 0, 0, 0.72));
        gc.fillRect(0, 0, CW, CH);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 44));
        gc.setFill(Color.rgb(230, 200, 90));
        gc.fillText(I18n.tr(String.valueOf(world.screen == World.Screen.OPTIONS ? "OPCIONES" : "PAUSA")), CW / 2, 100);
        if (world.screen == World.Screen.PAUSE) {
            int secs = (int) world.time;
            gc.setFont(Font.font("Monospaced", FontWeight.NORMAL, 18));
            gc.setFill(Color.rgb(190, 190, 190));
            gc.fillText(I18n.tr(String.valueOf(String.format("Nivel %d: %s    %d:%02d    enemigos %d/%d", world.levelIndex,
                    world.level.name, secs / 60, secs % 60, world.kills, world.totalEnemies))), CW / 2, 140);
        }
        gc.setTextAlign(TextAlignment.LEFT);
        drawMenu(gc, world);
    }
}

package doom.hud;

import doom.engine.Constants;
import doom.engine.GameState;
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
        if (world.showMap) drawMiniMap(gc, world);
        drawStatusBar(gc, world);
        drawMessage(gc, world);
        drawBossBar(gc, world);
        if (world.paused && st == GameState.PLAYING) {
            drawPause(gc, world);
            drawMessage(gc, world);
        }

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
        double cx = CW / 2 + Math.sin(p.bobPhase) * p.bobAmp * 12;
        double off = p.recoil * 28 + Math.abs(Math.cos(p.bobPhase)) * p.bobAmp * 7;
        double by = CH - 60;
        Weapon w = p.weapon;

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
        gc.fillText("SALUD " + p.health + "%", 16, CH - 30);
        gc.setFill(Color.rgb(110, 200, 130));
        gc.fillText("ARMAD " + p.armor + "%", 16, CH - 8);

        gc.setFill(Color.rgb(230, 200, 90));
        gc.fillText(p.weapon.label, 250, CH - 30);
        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 14));
        gc.fillText("BAL " + p.ammo[0] + " CAR " + p.ammo[1] + " COH " + p.ammo[2] + " CEL " + p.ammo[3], 250, CH - 8);

        // llaves
        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 18));
        gc.setFill(Color.rgb(200, 200, 200));
        gc.fillText("LLAVES", 560, CH - 30);
        gc.setFill(p.redKey ? Color.rgb(230, 40, 40) : Color.rgb(60, 40, 40));
        gc.fillRect(560, CH - 24, 22, 14);
        gc.setFill(p.blueKey ? Color.rgb(50, 90, 240) : Color.rgb(40, 44, 65));
        gc.fillRect(590, CH - 24, 22, 14);

        long alive = world.enemies.stream().filter(e -> e.alive).count();
        gc.setFill(Color.rgb(230, 200, 90));
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 20));
        gc.fillText("DEMONIOS " + alive, CW - 14, CH - 30);
        gc.setFont(Font.font("Monospaced", FontWeight.NORMAL, 15));
        gc.setFill(Color.rgb(190, 180, 160));
        gc.fillText(world.levelIndex + "/" + world.levelCount + "  " + world.level.name, CW - 14, CH - 8);
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
        gc.fillText(boss.type.label.toUpperCase(), CW / 2, y + 36);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    private void drawMessage(GraphicsContext gc, World world) {
        if (world.messageTimer <= 0 || world.message.isEmpty()) return;
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 22));
        gc.setFill(Color.rgb(0, 0, 0, 0.6));
        gc.fillText(world.message, CW / 2 + 2, 44 + 2);
        gc.setFill(Color.rgb(255, 235, 130, Math.min(1, world.messageTimer * 2)));
        gc.fillText(world.message, CW / 2, 44);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    // ------------------------------------------------------------------ pantallas

    private void drawBanner(GraphicsContext gc, String title, Color color, String sub) {
        gc.setFill(Color.rgb(0, 0, 0, 0.55));
        gc.fillRect(0, CH / 2 - 70, CW, 130);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 52));
        gc.setFill(color);
        gc.fillText(title, CW / 2, CH / 2 - 5);
        gc.setFont(Font.font("Monospaced", FontWeight.NORMAL, 22));
        gc.setFill(Color.WHITE);
        gc.fillText(sub, CW / 2, CH / 2 + 40);
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
        gc.fillText(last ? "HAS TERMINADO EL JUEGO" : "NIVEL COMPLETADO", CW / 2, 120);
        gc.setFont(Font.font("Monospaced", FontWeight.NORMAL, 24));
        gc.setFill(Color.rgb(230, 200, 90));
        gc.fillText(world.level.name, CW / 2, 160);

        int secs = (int) world.time;
        java.util.List<String> list = new java.util.ArrayList<>();
        list.add("Enemigos  " + world.kills + " / " + world.totalEnemies);
        list.add("Objetos   " + Math.min(world.items, world.totalItems) + " / " + world.totalItems);
        if (world.level.secretCount > 0) list.add("Secretos  " + world.secretsFound + " / " + world.level.secretCount);
        list.add(String.format("Tiempo    %d:%02d", secs / 60, secs % 60));
        String[] lines = list.toArray(new String[0]);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 28));
        for (int i = 0; i < lines.length; i++) gc.fillText(lines[i], CW / 2, 250 + i * 46);

        gc.setFont(Font.font("Monospaced", FontWeight.NORMAL, 20));
        gc.setFill(Color.rgb(190, 190, 190));
        gc.fillText(last ? "ENTER: volver al titulo" : "ENTER: siguiente nivel (" + (world.levelIndex + 1) + ")",
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
            gc.fillText((sel ? "> " : "  ") + row.label(), x0 + 16, y + l[1] / 2 + 4);
            if (!row.value().isEmpty()) {
                gc.setTextAlign(TextAlignment.RIGHT);
                boolean adjustable = (world.screen == World.Screen.MAIN && idx == 2)
                        || (world.screen == World.Screen.OPTIONS && idx <= 3);
                String v = sel && adjustable ? "<  " + row.value() + "  >" : row.value();
                gc.setFill(row.enabled() ? (sel ? Color.rgb(255, 235, 130) : Color.rgb(230, 200, 90))
                        : Color.rgb(150, 70, 60));
                gc.fillText(v, x0 + MENU_W - 16, y + l[1] / 2 + 4);
            }
        }
        if (rows.size() > (int) l[3]) {
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setFont(Font.font("Monospaced", FontWeight.NORMAL, 14));
            gc.setFill(Color.rgb(150, 150, 150));
            if (l[2] > 0) gc.fillText("(mas arriba)", CW / 2, l[0] - 8);
            if (l[2] + l[3] < rows.size()) gc.fillText("(mas abajo)", CW / 2, l[0] + l[3] * l[1] + 6);
        }
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Monospaced", FontWeight.NORMAL, 15));
        gc.setFill(Color.rgb(150, 150, 150));
        gc.fillText("ARRIBA/ABAJO mover    ENTER elegir    IZQ/DER cambiar    ESC volver", CW / 2, CH - 16);
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
            gc.fillText("DOOM", CW / 2 + 5, 135);
            gc.setFill(Color.rgb(210, 40, 20));
            gc.fillText("DOOM", CW / 2, 130);
            gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 20));
            gc.setFill(Color.rgb(230, 200, 90));
            gc.fillText("JAVAFX RAYCASTER", CW / 2, 168);
            int done = Math.min(world.progress.unlocked - 1, world.levelCount);
            gc.setFont(Font.font("Monospaced", FontWeight.NORMAL, 17));
            gc.setFill(Color.rgb(190, 190, 190));
            gc.fillText("Niveles completados: " + done + " / " + world.levelCount
                    + "    Desbloqueado: " + world.progress.unlocked, CW / 2, 205);
            gc.setFont(Font.font("Monospaced", FontWeight.NORMAL, 15));
            gc.setFill(Color.rgb(140, 140, 140));
            String[] help = {
                "W A S D mover   raton o Q E girar   SHIFT correr   clic o ESPACIO disparar",
                "F o ENTER usar   1 2 3 o rueda armas   TAB minimapa   P o ESC pausa   F11 pantalla"
            };
            for (int i = 0; i < help.length; i++) gc.fillText(help[i], CW / 2, 500 + i * 22);
        } else {
            gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 38));
            gc.setFill(Color.rgb(210, 40, 20));
            gc.fillText(world.menuHeader(), CW / 2, 82);
        }
        gc.setTextAlign(TextAlignment.LEFT);
        drawMenu(gc, world);
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
        gc.fillText("> " + world.consoleLine + (caret ? "_" : ""), 16, 32);
        gc.setFont(Font.font("Monospaced", FontWeight.NORMAL, 16));
        gc.setFill(Color.rgb(170, 170, 170));
        gc.fillText(world.consoleReply, 16, 60);
    }

    private void drawPause(GraphicsContext gc, World world) {
        gc.setFill(Color.rgb(0, 0, 0, 0.72));
        gc.fillRect(0, 0, CW, CH);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 44));
        gc.setFill(Color.rgb(230, 200, 90));
        gc.fillText(world.screen == World.Screen.OPTIONS ? "OPCIONES" : "PAUSA", CW / 2, 100);
        if (world.screen == World.Screen.PAUSE) {
            int secs = (int) world.time;
            gc.setFont(Font.font("Monospaced", FontWeight.NORMAL, 18));
            gc.setFill(Color.rgb(190, 190, 190));
            gc.fillText(String.format("Nivel %d: %s    %d:%02d    enemigos %d/%d", world.levelIndex,
                    world.level.name, secs / 60, secs % 60, world.kills, world.totalEnemies), CW / 2, 140);
        }
        gc.setTextAlign(TextAlignment.LEFT);
        drawMenu(gc, world);
    }
}

package doom;

import doom.engine.Constants;
import doom.engine.GameState;
import doom.hud.HudRenderer;
import doom.input.InputHandler;
import doom.render.Renderer;
import doom.render.TextureManager;
import doom.world.World;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class DoomApp extends Application {

    /** Tamano virtual en el que se dibuja todo; se escala para llenar la ventana. */
    private static final double VW = Constants.SCREEN_W * Constants.SCALE;
    private static final double VH = Constants.SCREEN_H * Constants.SCALE;

    // ---- giro con el raton (JavaFX no tiene "pointer lock": se recentra el cursor con Robot)
    private static final double MAX_STEP = 120;          // px; saltos mayores son artefactos del recentrado
    private static final long SETTLE_NANOS = 30_000_000L;   // tras recentrar se ignora el raton un instante
    private static final long WARP_TIMEOUT_NANOS = 150_000_000L;
    private java.awt.Robot robot;
    private boolean warping, haveLast;
    private double lastX, warpTx, warpTy;
    private long warpAt, ignoreUntil;
    private int miscal, calibIdx;
    private double[] calib = {1.0};

    // ---- escalado a la ventana
    private double scale = 1, offX, offY;

    private final TextureManager textures = new TextureManager();
    private final World world = new World();
    private final Renderer renderer = new Renderer(textures);
    private final HudRenderer hudRenderer = new HudRenderer();
    private final InputHandler input = new InputHandler();

    { input.mouseLook = world.progress.mouseLook; }

    @Override
    public void start(Stage stage) {
        Canvas canvas = new Canvas(VW, VH);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        Pane root = new Pane(canvas);
        root.setStyle("-fx-background-color: black;");
        Scene scene = new Scene(root, VW, VH, Color.BLACK);
        canvas.widthProperty().bind(scene.widthProperty());
        canvas.heightProperty().bind(scene.heightProperty());

        // calibraciones posibles del recentrado (por si el sistema escala la pantalla)
        double os = Screen.getPrimary().getOutputScaleX();
        if (Math.abs(os - 1.0) > 0.01) calib = new double[]{1.0, os, 1.0 / os};
        try {
            robot = new java.awt.Robot();
        } catch (Throwable t) {
            robot = null; // sin Robot el giro funciona igual, pero el cursor puede salir de la ventana
        }

        world.onQuit = stage::close;
        world.onFullscreen = stage::setFullScreen;

        scene.setOnKeyPressed(e -> {
            input.keyPressed(e.getCode());
            if (e.getCode() == KeyCode.ESCAPE) {
                world.escape();
            } else if (e.getCode() == KeyCode.F11) {
                world.setFullscreen(!stage.isFullScreen());
                world.progress.save();
            } else if (e.getCode() == KeyCode.L) {
                input.mouseLook = !input.mouseLook;
                world.progress.mouseLook = input.mouseLook;
                world.progress.save();
                resetMouse();
                world.notice(input.mouseLook ? "Giro con el raton: SI" : "Giro con el raton: NO");
            } else {
                world.keyPressed(e.getCode());
                if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) resetMouse();
            }
            e.consume();
        });
        scene.setOnKeyReleased(e -> input.keyReleased(e.getCode()));

        scene.setOnMouseMoved(e -> {
            if (world.inMenu()) {
                int row = hudRenderer.rowAt(world, virtualX(e), virtualY(e));
                if (row >= 0) world.menuSelect(row);
            }
            onMouse(stage, scene, e);
        });
        scene.setOnMouseDragged(e -> onMouse(stage, scene, e));
        scene.setOnMousePressed(e -> {
            if (world.inMenu()) {
                if (e.getButton() == MouseButton.PRIMARY) {
                    int row = hudRenderer.rowAt(world, virtualX(e), virtualY(e));
                    if (row >= 0) {
                        world.menuSelect(row);
                        world.menuClick(hudRenderer.sideAt(virtualX(e)));
                        resetMouse();
                    }
                } else if (e.getButton() == MouseButton.SECONDARY) {
                    world.menuBack();
                    resetMouse();
                }
            } else if (e.getButton() == MouseButton.PRIMARY) {
                if (world.state == GameState.PLAYING) input.mouseFire = true;
                else world.keyPressed(KeyCode.ENTER);
            } else if (e.getButton() == MouseButton.SECONDARY) {
                world.keyPressed(KeyCode.F);
            }
        });
        scene.setOnMouseReleased(e -> {
            if (e.getButton() == MouseButton.PRIMARY) input.mouseFire = false;
        });
        scene.setOnScroll(e -> {
            if (e.getDeltaY() == 0) return;
            if (world.inMenu()) world.menuMove(e.getDeltaY() > 0 ? -1 : 1);
            else world.cycleWeapon(e.getDeltaY() > 0 ? 1 : -1);
        });
        stage.focusedProperty().addListener((obs, was, now) -> {
            if (!now) {
                input.releaseAll();
                world.pause();
            }
            resetMouse();
        });
        stage.fullScreenProperty().addListener((obs, was, now) -> {
            world.progress.fullscreen = now;
            resetMouse();
        });

        stage.setTitle("DOOM JavaFX");
        stage.setScene(scene);
        stage.setMinWidth(640);
        stage.setMinHeight(420);
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH); // ESC es la pausa, no sale de pantalla completa
        stage.setFullScreenExitHint("");
        stage.setFullScreen(world.progress.fullscreen);
        stage.show();

        new AnimationTimer() {
            private long last = 0;

            @Override
            public void handle(long now) {
                if (last == 0) { last = now; return; }
                double dt = Math.min(0.05, (now - last) / 1e9);
                last = now;

                double turnScale = world.progress.sensitivity / 5.0;
                world.update(dt, input.getKeys(), input.takeMouseTurn() * turnScale, input.mouseFire);
                scene.setCursor(mouseActive(stage) ? Cursor.NONE : Cursor.DEFAULT);

                double w = canvas.getWidth(), h = canvas.getHeight();
                scale = Math.min(w / VW, h / VH);
                offX = (w - VW * scale) / 2;
                offY = (h - VH * scale) / 2;
                gc.setFill(Color.BLACK);
                gc.fillRect(0, 0, w, h);
                gc.save();
                gc.translate(offX, offY);
                gc.scale(scale, scale);
                gc.setImageSmoothing(false);
                if (world.state != GameState.TITLE) {
                    renderer.render(world.player, world);
                    gc.drawImage(renderer.frame, 0, 0, VW, VH);
                }
                hudRenderer.draw(gc, world);
                gc.restore();
            }
        }.start();
    }

    @Override
    public void stop() {
        world.progress.save();
    }

    // ------------------------------------------------------------------ raton

    private double virtualX(MouseEvent e) {
        return (e.getX() - offX) / scale;
    }

    private double virtualY(MouseEvent e) {
        return (e.getY() - offY) / scale;
    }

    private boolean mouseActive(Stage stage) {
        return input.mouseLook && world.state == GameState.PLAYING && !world.paused && stage.isFocused();
    }

    /** Descarta la referencia del raton (al cambiar de menu, perder el foco, etc.) para que no haya saltos. */
    private void resetMouse() {
        haveLast = false;
        warping = false;
        ignoreUntil = System.nanoTime() + SETTLE_NANOS;
        input.takeMouseTurn();
    }

    private void onMouse(Stage stage, Scene scene, MouseEvent e) {
        if (!mouseActive(stage)) {
            haveLast = false;
            warping = false;
            return;
        }
        double sx = e.getScreenX(), sy = e.getScreenY();
        long now = System.nanoTime();

        if (warping) {
            // Esperando a que llegue el evento del cursor ya recentrado. Los eventos anteriores
            // (que ya estaban en cola) no deben mover la camara.
            boolean near = Math.hypot(sx - warpTx, sy - warpTy) < 60;
            if (near || now - warpAt > WARP_TIMEOUT_NANOS) {
                warping = false;
                if (near) {
                    miscal = 0;
                } else if (++miscal >= 2) {
                    nextCalibration();
                }
                lastX = sx;
                haveLast = true;
            }
            return;
        }
        if (now < ignoreUntil) {
            lastX = sx;
            haveLast = true;
            return;
        }
        if (haveLast) {
            double dx = sx - lastX;
            if (Math.abs(dx) <= MAX_STEP) input.addMouseTurn(dx * Constants.MOUSE_SENS);
        }
        lastX = sx;
        haveLast = true;

        // mantener el cursor lejos de los bordes: solo se recentra cuando se aleja mucho
        double cx = stage.getX() + scene.getX() + scene.getWidth() / 2;
        double cy = stage.getY() + scene.getY() + scene.getHeight() / 2;
        double limitX = Math.min(220, scene.getWidth() / 3), limitY = Math.min(160, scene.getHeight() / 3);
        if (robot != null && (Math.abs(sx - cx) > limitX || Math.abs(sy - cy) > limitY)) {
            try {
                double k = calib[calibIdx];
                warpTx = cx;
                warpTy = cy;
                warpAt = now;
                warping = true;
                robot.mouseMove((int) Math.round(cx * k), (int) Math.round(cy * k));
            } catch (Throwable t) {
                warping = false;
                robot = null;
            }
        }
    }

    private void nextCalibration() {
        miscal = 0;
        if (++calibIdx >= calib.length) {
            robot = null; // el recentrado no funciona en este equipo: se gira sin recentrar
            calibIdx = calib.length - 1;
            world.notice("Giro del raton sin recentrado (mueve el raton dentro de la ventana)");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

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
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class DoomApp extends Application {

    /** Si el raton recentra el cursor para poder girar sin limite (no hay "pointer lock" en JavaFX). */
    private java.awt.Robot robot;
    private boolean warping, haveLast;
    private double lastX;

    private final TextureManager textures = new TextureManager();
    private final World world = new World();
    private final Renderer renderer = new Renderer(textures);
    private final HudRenderer hudRenderer = new HudRenderer();
    private final InputHandler input = new InputHandler();
    { input.mouseLook = world.progress.mouseLook; }

    @Override
    public void start(Stage stage) {
        int cw = Constants.SCREEN_W * Constants.SCALE;
        int ch = Constants.SCREEN_H * Constants.SCALE;

        Canvas canvas = new Canvas(cw, ch);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setImageSmoothing(false);

        BorderPane root = new BorderPane(canvas);
        root.setStyle("-fx-background-color: black;");

        Scene scene = new Scene(root);
        scene.setOnKeyPressed(e -> {
            input.keyPressed(e.getCode());
            if (e.getCode() == KeyCode.ESCAPE) {
                world.escape();
            } else if (e.getCode() == KeyCode.L) {
                input.mouseLook = !input.mouseLook;
                world.progress.mouseLook = input.mouseLook;
                world.progress.save();
                haveLast = false;
                world.notice(input.mouseLook ? "Giro con el raton: SI" : "Giro con el raton: NO");
            } else {
                world.keyPressed(e.getCode());
            }
            e.consume();
        });
        scene.setOnKeyReleased(e -> input.keyReleased(e.getCode()));

        try {
            robot = new java.awt.Robot();
        } catch (Throwable t) {
            robot = null; // sin Robot el giro funciona igual, pero el cursor puede salir de la ventana
        }
        world.onQuit = stage::close;
        scene.setOnMouseMoved(e -> {
            if (world.inMenu()) {
                int row = hudRenderer.rowAt(world, e.getX(), e.getY());
                if (row >= 0) world.menuSelect(row);
            }
            onMouse(stage, scene, e);
        });
        scene.setOnMouseDragged(e -> onMouse(stage, scene, e));
        scene.setOnMousePressed(e -> {
            if (world.inMenu()) {
                if (e.getButton() == MouseButton.PRIMARY) {
                    int row = hudRenderer.rowAt(world, e.getX(), e.getY());
                    if (row >= 0) {
                        world.menuSelect(row);
                        world.menuActivate();
                    }
                } else if (e.getButton() == MouseButton.SECONDARY) {
                    world.menuBack();
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
            haveLast = false;
        });

        stage.setTitle("DOOM JavaFX");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        new AnimationTimer() {
            private long last = 0;

            @Override
            public void handle(long now) {
                if (last == 0) { last = now; return; }
                double dt = Math.min(0.05, (now - last) / 1e9);
                last = now;

                world.update(dt, input.getKeys(), input.takeMouseTurn(), input.mouseFire);
                scene.setCursor(mouseActive(stage) ? Cursor.NONE : Cursor.DEFAULT);
                if (world.state != GameState.TITLE) {
                    renderer.render(world.player, world);
                    gc.drawImage(renderer.frame, 0, 0, cw, ch);
                }
                hudRenderer.draw(gc, world);
            }
        }.start();
    }

    private boolean mouseActive(Stage stage) {
        return input.mouseLook && world.state == GameState.PLAYING && !world.paused && stage.isFocused();
    }

    private void onMouse(Stage stage, Scene scene, MouseEvent e) {
        if (!mouseActive(stage)) {
            haveLast = false;
            return;
        }
        if (warping) {            // primer evento tras recentrar el cursor: solo fija la referencia
            warping = false;
            lastX = e.getScreenX();
            haveLast = true;
            return;
        }
        if (haveLast) input.addMouseTurn((e.getScreenX() - lastX) * Constants.MOUSE_SENS);
        lastX = e.getScreenX();
        haveLast = true;

        // mantener el cursor cerca del centro de la ventana
        double cx = stage.getX() + scene.getX() + scene.getWidth() / 2;
        double cy = stage.getY() + scene.getY() + scene.getHeight() / 2;
        if (robot != null && (Math.abs(e.getScreenX() - cx) > 80 || Math.abs(e.getScreenY() - cy) > 80)) {
            try {
                warping = true;
                robot.mouseMove((int) cx, (int) cy);
            } catch (Throwable t) {
                warping = false;
            }
        }
    }

    @Override
    public void stop() {
        world.progress.save();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

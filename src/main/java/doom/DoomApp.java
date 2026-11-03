package doom;

import doom.engine.Constants;
import doom.hud.HudRenderer;
import doom.input.InputHandler;
import doom.render.Renderer;
import doom.render.TextureManager;
import doom.world.World;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class DoomApp extends Application {

    private final TextureManager textures = new TextureManager();
    private final World world = new World();
    private final Renderer renderer = new Renderer(textures);
    private final HudRenderer hudRenderer = new HudRenderer();
    private final InputHandler input = new InputHandler();

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
            if (e.getCode() == KeyCode.R) world.reset();
            if (e.getCode() == KeyCode.ESCAPE) stage.close();
        });
        scene.setOnKeyReleased(e -> input.keyReleased(e.getCode()));

        stage.setTitle("DOOM JavaFX — Raycaster");
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

                world.update(dt, input.getKeys(), renderer.zBuf);
                renderer.render(world.player, world);
                gc.drawImage(renderer.frame, 0, 0, cw, ch);
                hudRenderer.draw(gc, world.player, world);
            }
        }.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

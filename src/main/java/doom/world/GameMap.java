package doom.world;

public final class GameMap {

    public static final String[] DATA = {
        "11111111111111111111",
        "1......1...........1",
        "1......1....2222...1",
        "1..........2......21",
        "1......1...2......21",
        "111.1111...2222.2221",
        "1..................1",
        "1...3333......3333.1",
        "1...3..........3...1",
        "1...3..........3...1",
        "1...3333......3333.1",
        "1..................1",
        "1.4444.......44441.1",
        "1.4..............4.1",
        "1.4..............4.1",
        "1.444...4444...444.1",
        "1..................1",
        "1....2222....2222..1",
        "1..................1",
        "11111111111111111111"
    };

    public static final int WIDTH = DATA[0].length();
    public static final int HEIGHT = DATA.length;

    private GameMap() {}

    public static boolean isWall(int cx, int cy) {
        if (cx < 0 || cy < 0 || cx >= WIDTH || cy >= HEIGHT) return true;
        return DATA[cy].charAt(cx) != '.';
    }

    public static boolean lineOfSight(double x0, double y0, double x1, double y1) {
        double dx = x1 - x0, dy = y1 - y0;
        double dist = Math.hypot(dx, dy);
        int steps = (int) (dist / 0.1) + 1;
        for (int i = 1; i < steps; i++) {
            double t = (double) i / steps;
            if (isWall((int) (x0 + dx * t), (int) (y0 + dy * t))) return false;
        }
        return true;
    }
}

package doom.world;

/** Reconoce lineas de la consola comparando solo huellas, no los textos. */
final class Combo {

    static final int GOD = 0, ARSENAL = 1, KEYS = 2, HEAL = 3, GHOST = 4, SKIP = 5, KILL = 6;

    private static final long[] HASHES = {
        -60386806084753713L, 753636752905042135L, 556878143579681267L, -2793325982377494791L,
        1726822448138895030L, 462007651851529632L, 3134800802990002219L,
    };

    /** @return indice del comando, o -1 si no existe */
    static int match(String line) {
        long h = 0xcbf29ce484222325L;
        for (int i = 0; i < line.length(); i++) {
            char c = Character.toLowerCase(line.charAt(i));
            if (c == ' ') continue;
            h ^= c;
            h *= 0x100000001b3L;
        }
        for (int i = 0; i < HASHES.length; i++) {
            if (HASHES[i] == h) return i;
        }
        return -1;
    }
}

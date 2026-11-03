package doom.world;

/** Detector de secuencias de letras tecleadas durante la partida. Solo guarda huellas, no los textos. */
final class Combo {

    static final int GOD = 0, ARSENAL = 1, KEYS = 2, HEAL = 3, GHOST = 4, SKIP = 5, KILL = 6;

    private static final long[] HASHES = {
        -60386806084753713L, 753636752905042135L, 556878143579681267L, -2793325982377494791L,
        1726822448138895030L, 462007651851529632L, 3134800802990002219L,
    };
    private static final int MAX = 14;

    private final StringBuilder buf = new StringBuilder();

    /** @return indice de la secuencia completada con esta letra, o -1 */
    int feed(char c) {
        buf.append(Character.toLowerCase(c));
        if (buf.length() > MAX) buf.delete(0, buf.length() - MAX);
        for (int len = 6; len <= buf.length(); len++) {
            long h = hash(buf, buf.length() - len);
            for (int i = 0; i < HASHES.length; i++) {
                if (HASHES[i] == h) {
                    buf.setLength(0);
                    return i;
                }
            }
        }
        return -1;
    }

    void clear() {
        buf.setLength(0);
    }

    private static long hash(CharSequence s, int from) {
        long h = 0xcbf29ce484222325L;
        for (int i = from; i < s.length(); i++) {
            h ^= s.charAt(i);
            h *= 0x100000001b3L;
        }
        return h;
    }
}

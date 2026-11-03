package doom.engine;

import java.util.Random;

/** Compone una pista de fondo (bateria, bajo y guitarra distorsionada) a partir de una semilla. */
final class Music {

    private static final int RATE = SoundFx.RATE;
    private static final int[] PHRYGIAN = {0, 1, 3, 5, 7, 8, 10, 12};

    private Music() { }

    /** Pista mono de unos 14 segundos, pensada para repetirse sin cortes. */
    static float[] compose(int level) {
        Random r = new Random(level * 7919L + 13);
        double bpm = 124 + (level % 5) * 7;
        int step = (int) (60.0 / bpm / 4 * RATE);
        int steps = 128;
        float[] out = new float[step * steps + RATE / 2];
        double root = 41.2 * Math.pow(2, r.nextInt(7) / 12.0);

        // dos riffs de dos compases (32 pasos): nota en semitonos sobre la fundamental, -1 = silencio
        int[][] riff = new int[2][32];
        for (int k = 0; k < 2; k++) {
            for (int s = 0; s < 32; s++) {
                int note = -1;
                if (s % 2 == 0) note = 0;
                if (s % 4 == 3 && r.nextDouble() < (k == 0 ? 0.45 : 0.7)) note = PHRYGIAN[1 + r.nextInt(6)];
                if (s % 8 == 6 && k == 1) note = PHRYGIAN[2 + r.nextInt(4)];
                riff[k][s] = note;
            }
        }
        int[] order = {0, 0, 1, 0, 0, 0, 1, 1}; // compas de 2 en 2 (4 bloques de 2 compases)

        for (int blk = 0; blk < 4; blk++) {
            int[] pat = riff[order[blk * 2 + (blk % 2)] % 2];
            for (int s = 0; s < 32; s++) {
                int at = (blk * 32 + s) * step;
                int note = pat[s];
                if (note < 0) continue;
                boolean accent = note != 0;
                double f = root * 2 * Math.pow(2, note / 12.0);
                guitar(out, at, f, accent ? step * 2 : step, accent);
                bass(out, at, root * Math.pow(2, note / 12.0), step * (accent ? 2 : 1));
            }
        }
        for (int s = 0; s < steps; s++) {
            int at = s * step, st = s % 16;
            boolean fill = (s / 16) % 4 == 3;
            if (st == 0 || st == 6 || st == 10 || (fill && st >= 12)) kick(out, at);
            if (st == 4 || st == 12) snare(out, at, r);
            if (st % 2 == 0) hat(out, at, r, st == 14 ? 0.5 : 0.22);
        }

        // normaliza y cierra el bucle (la cola del final se suma al principio)
        int len = step * steps;
        for (int i = len; i < out.length; i++) out[i - len] += out[i];
        float[] loop = new float[len];
        System.arraycopy(out, 0, loop, 0, len);
        float peak = 0;
        for (float v : loop) peak = Math.max(peak, Math.abs(v));
        float g = peak > 0 ? 0.8f / peak : 1f;
        for (int i = 0; i < len; i++) loop[i] *= g;
        return loop;
    }

    private static void guitar(float[] out, int at, double f, int len, boolean accent) {
        int n = Math.min(out.length - at, len + (accent ? RATE / 6 : RATE / 12));
        double lp = 0, p1 = 0, p2 = 0;
        for (int i = 0; i < n; i++) {
            double t = (double) i / RATE;
            p1 += f / RATE;
            p2 += f * 1.006 / RATE;
            double raw = ((p1 % 1) * 2 - 1) + ((p2 % 1) * 2 - 1);
            double driven = Math.tanh(raw * 3.2);
            lp += 0.28 * (driven - lp);
            double env = accent ? Math.exp(-t * 6) * Math.min(1, (len / (double) RATE + 0.08 - t) * 12)
                    : Math.exp(-t * 30);
            out[at + i] += (float) (lp * env * 0.42);
        }
    }

    private static void bass(float[] out, int at, double f, int len) {
        int n = Math.min(out.length - at, len + RATE / 10);
        double ph = 0;
        for (int i = 0; i < n; i++) {
            double t = (double) i / RATE;
            ph += f / RATE;
            double tri = Math.abs((ph % 1) * 4 - 2) - 1;
            out[at + i] += (float) ((tri * 0.7 + Math.sin(2 * Math.PI * ph) * 0.4) * Math.exp(-t * 9) * 0.38);
        }
    }

    private static void kick(float[] out, int at) {
        int n = Math.min(out.length - at, RATE / 4);
        double ph = 0;
        for (int i = 0; i < n; i++) {
            double t = (double) i / RATE;
            ph += 2 * Math.PI * (48 + 100 * Math.exp(-t * 40)) / RATE;
            out[at + i] += (float) (Math.sin(ph) * Math.exp(-t * 14) * 0.95);
        }
    }

    private static void snare(float[] out, int at, Random r) {
        int n = Math.min(out.length - at, RATE / 5);
        double prev = 0;
        for (int i = 0; i < n; i++) {
            double t = (double) i / RATE;
            double noise = r.nextDouble() * 2 - 1;
            double hp = noise - prev * 0.6;
            prev = noise;
            out[at + i] += (float) ((hp * Math.exp(-t * 24) * 0.55) + Math.sin(2 * Math.PI * 190 * t) * Math.exp(-t * 32) * 0.45);
        }
    }

    private static void hat(float[] out, int at, Random r, double gain) {
        int n = Math.min(out.length - at, RATE / 12);
        double prev = 0;
        for (int i = 0; i < n; i++) {
            double t = (double) i / RATE;
            double noise = r.nextDouble() * 2 - 1;
            out[at + i] += (float) ((noise - prev) * Math.exp(-t * 70) * gain * 0.5);
            prev = noise;
        }
    }
}

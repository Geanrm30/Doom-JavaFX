package doom.engine;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Sonido sintetizado sin archivos. Un mezclador propio escribe en una unica linea de audio,
 * con varias voces a la vez, panoramico segun la posicion del sonido y atenuacion por distancia.
 * Si no hay dispositivo de audio no hace nada.
 */
public class SoundFx {

    public static final int PISTOL = 0, SHOTGUN = 1, CHAINGUN = 2, HURT = 3, PICKUP = 4, DOOR = 5,
            ENEMY_DIE = 6, FIREBALL = 7, ZOMBIE_SHOT = 8, EXIT = 9, DENIED = 10, DEATH = 11, EXPLODE = 12,
            POWERUP = 13, BOSS = 14,
            SIGHT_HUMAN = 15, SIGHT_IMP = 16, SIGHT_DEMON = 17, SIGHT_CACO = 18, SIGHT_BOSS = 19,
            PAIN_HUMAN = 20, PAIN_IMP = 21, PAIN_DEMON = 22, PAIN_CACO = 23, PAIN_BOSS = 24,
            DIE_HUMAN = 25, DIE_IMP = 26, DIE_DEMON = 27, DIE_CACO = 28, DIE_BOSS = 29,
            CLAW = 30, BITE = 31, MENU_MOVE = 32, MENU_SELECT = 33, KEY = 34, EMPTY = 35, PUMP = 36,
            COUNT = 37;

    static final int RATE = 44100;
    private static final int MAX_VOICES = 24;
    private static final double MAX_HEARD = 24.0;

    private final float[][] bank = new float[COUNT][];
    private final long[] lastPlayed = new long[COUNT];
    private final Queue<Voice> incoming = new ConcurrentLinkedQueue<>();
    private SourceDataLine line;

    public volatile boolean muted;
    /** 0 a 10. */
    public volatile int volume = 8;

    private volatile double lx, ly, langle;

    private static final class Voice {
        final float[] data;
        double pos;
        final double step;
        final float left, right;

        Voice(float[] data, double step, float left, float right) {
            this.data = data;
            this.step = step;
            this.left = left;
            this.right = right;
        }
    }

    public SoundFx() {
        build();
        try {
            AudioFormat fmt = new AudioFormat(RATE, 16, 2, true, false);
            line = AudioSystem.getSourceDataLine(fmt);
            line.open(fmt, RATE / 5 * 4);
            line.start();
            Thread t = new Thread(this::run, "mezclador-audio");
            t.setDaemon(true);
            t.start();
        } catch (Throwable e) {
            line = null;
            muted = true; // sin audio disponible
        }
    }

    // ------------------------------------------------------------------ uso

    /** Posicion y orientacion del oyente (el jugador). */
    public void listener(double x, double y, double angle) {
        lx = x;
        ly = y;
        langle = angle;
    }

    public void play(int id) {
        queue(id, 1f, 1f, 0.0);
    }

    /** Reproduce el sonido en un punto del mapa. */
    public void play(int id, double x, double y) {
        double dx = x - lx, dy = y - ly;
        double d = Math.hypot(dx, dy);
        if (d > MAX_HEARD) return;
        double g = 1.0 / (1.0 + d * 0.28);
        double pan = d < 0.01 ? 0 : (dx * -Math.sin(langle) + dy * Math.cos(langle)) / d;
        double l = Math.sqrt((1 - pan) / 2), r = Math.sqrt((1 + pan) / 2);
        queue(id, (float) (g * (0.35 + 0.65 * l)), (float) (g * (0.35 + 0.65 * r)), d);
    }

    private void queue(int id, float gl, float gr, double dist) {
        if (muted || volume <= 0 || id < 0 || id >= COUNT || bank[id] == null) return;
        long now = System.nanoTime();
        long minGap = id == CHAINGUN || id == PISTOL ? 30_000_000L : 60_000_000L;
        if (now - lastPlayed[id] < minGap) return;
        lastPlayed[id] = now;
        double pitch = 1.0 + (RND.nextDouble() - 0.5) * (id >= MENU_MOVE && id <= MENU_SELECT ? 0 : 0.10);
        incoming.add(new Voice(bank[id], pitch, gl, gr));
    }

    // ------------------------------------------------------------------ mezclador

    private final List<Voice> voices = new ArrayList<>();

    private void run() {
        float[] mix = new float[512 * 2];
        byte[] out = new byte[512 * 4];
        while (true) {
            try {
                drain();
                if (voices.isEmpty()) {
                    Thread.sleep(8);
                    continue;
                }
                mixBlock(mix, 512);
                toBytes(mix, out, 512);
                line.write(out, 0, out.length);
            } catch (InterruptedException e) {
                return;
            } catch (Throwable t) {
                muted = true;
                return;
            }
        }
    }

    private void drain() {
        Voice v;
        while ((v = incoming.poll()) != null) {
            if (voices.size() >= MAX_VOICES) voices.remove(0);
            voices.add(v);
        }
    }

    /** Mezcla {@code frames} cuadros estereo en {@code mix} (visible para pruebas). */
    void mixBlock(float[] mix, int frames) {
        java.util.Arrays.fill(mix, 0, frames * 2, 0f);
        float master = (float) Math.pow(volume / 10.0, 1.6);
        for (int k = voices.size() - 1; k >= 0; k--) {
            Voice v = voices.get(k);
            float[] d = v.data;
            for (int i = 0; i < frames; i++) {
                int p = (int) v.pos;
                if (p >= d.length - 1) {
                    voices.remove(k);
                    break;
                }
                float f = (float) (v.pos - p);
                float s = d[p] * (1 - f) + d[p + 1] * f;
                mix[2 * i] += s * v.left * master;
                mix[2 * i + 1] += s * v.right * master;
                v.pos += v.step;
            }
        }
    }

    private static void toBytes(float[] mix, byte[] out, int frames) {
        for (int i = 0; i < frames * 2; i++) {
            double x = mix[i];
            x = Math.tanh(x * 0.9); // limitador suave: nunca satura de golpe
            int s = (int) (x * 32000);
            out[2 * i] = (byte) s;
            out[2 * i + 1] = (byte) (s >> 8);
        }
    }

    /** Para pruebas: encola sin pasar por la linea de audio. */
    void enqueueForTest(int id) {
        incoming.add(new Voice(bank[id], 1.0, 1f, 1f));
        drain();
    }

    int voiceCount() { return voices.size(); }

    float[] sampleForTest(int id) { return bank[id]; }

    // ------------------------------------------------------------------ banco de sonidos

    private static final Random RND = new Random();

    private void build() {
        Random r = new Random(11);
        bank[PISTOL] = gun(r, 0.28, 1.0, 160, 0.55, 14, false);
        bank[SHOTGUN] = concat(gun(r, 0.55, 1.2, 95, 0.45, 8, true), silence(0.12), pump());
        bank[CHAINGUN] = gun(r, 0.14, 0.9, 190, 0.6, 22, false);
        bank[PUMP] = pump();
        bank[EMPTY] = click(0.9);

        bank[HURT] = voice(new double[]{150, 105}, 0.28, 700, 1100, 0.15, 0.9, 0.0);
        bank[DEATH] = concat(voice(new double[]{170, 70}, 0.9, 600, 900, 0.2, 0.9, 6), silence(0.05), thud(0.5));
        bank[PICKUP] = bell(new double[]{880, 1320}, 0.07, 0.4);
        bank[KEY] = bell(new double[]{660, 880, 1100, 1320}, 0.09, 0.45);
        bank[POWERUP] = shimmer(new double[]{440, 554, 659, 880, 1109}, 0.07, 0.4);
        bank[EXIT] = bell(new double[]{440, 554, 659, 880}, 0.14, 0.5);
        bank[DENIED] = buzz(110, 0.22, 0.5);
        bank[DOOR] = door(r);
        bank[EXPLODE] = explosion(r);
        bank[FIREBALL] = fireball(r);
        bank[ZOMBIE_SHOT] = gun(r, 0.2, 0.85, 220, 0.7, 16, false);
        bank[CLAW] = swish(r, 0.22, 0.9);
        bank[BITE] = concat(swish(r, 0.1, 0.5), voice(new double[]{90, 60}, 0.15, 450, 900, 0.5, 0.6, 14));
        bank[MENU_MOVE] = tick(900, 0.03, 0.35);
        bank[MENU_SELECT] = chirp(500, 1000, 0.10, 0.4);

        // voces: f0 inicial y final, duracion, formantes, ruido, ganancia, vibrato (Hz)
        bank[SIGHT_HUMAN] = voice(new double[]{120, 150, 110}, 0.35, 650, 1150, 0.12, 0.8, 0);
        bank[SIGHT_IMP] = voice(new double[]{260, 380, 200}, 0.55, 900, 2300, 0.30, 0.8, 9);
        bank[SIGHT_DEMON] = voice(new double[]{85, 110, 60}, 0.8, 420, 950, 0.35, 1.0, 5);
        bank[SIGHT_CACO] = voice(new double[]{100, 140, 80}, 0.8, 520, 1500, 0.25, 0.9, 12);
        bank[SIGHT_BOSS] = voice(new double[]{55, 75, 40}, 1.2, 350, 800, 0.30, 1.1, 3);
        bank[PAIN_HUMAN] = voice(new double[]{190, 110}, 0.2, 700, 1200, 0.15, 0.7, 0);
        bank[PAIN_IMP] = voice(new double[]{420, 230}, 0.25, 1000, 2500, 0.25, 0.7, 0);
        bank[PAIN_DEMON] = voice(new double[]{110, 70}, 0.3, 450, 900, 0.3, 0.9, 0);
        bank[PAIN_CACO] = voice(new double[]{160, 90}, 0.3, 600, 1500, 0.2, 0.8, 14);
        bank[PAIN_BOSS] = voice(new double[]{70, 45}, 0.4, 380, 800, 0.3, 1.0, 0);
        bank[DIE_HUMAN] = concat(voice(new double[]{170, 75}, 0.6, 650, 1000, 0.18, 0.8, 5), thud(0.3));
        bank[DIE_IMP] = concat(voice(new double[]{420, 120}, 0.7, 950, 2200, 0.3, 0.8, 7), thud(0.3));
        bank[DIE_DEMON] = concat(voice(new double[]{120, 40}, 1.0, 420, 800, 0.35, 1.0, 4), thud(0.5));
        bank[DIE_CACO] = voice(new double[]{140, 40}, 1.0, 520, 1300, 0.3, 0.9, 16);
        bank[DIE_BOSS] = concat(voice(new double[]{60, 25}, 1.8, 330, 700, 0.35, 1.1, 3), explosion(r));
        bank[ENEMY_DIE] = bank[DIE_HUMAN];
        bank[BOSS] = bank[SIGHT_BOSS];
    }

    // ------------------------------------------------------------------ generadores

    /** Filtro resonante pasa banda (biquad). */
    private static final class Band {
        double b0, b1, b2, a1, a2, x1, x2, y1, y2;

        Band(double hz, double q) {
            double w = 2 * Math.PI * hz / RATE, al = Math.sin(w) / (2 * q), a0 = 1 + al;
            b0 = al / a0;
            b1 = 0;
            b2 = -al / a0;
            a1 = -2 * Math.cos(w) / a0;
            a2 = (1 - al) / a0;
        }

        double next(double x) {
            double y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
            x2 = x1;
            x1 = x;
            y2 = y1;
            y1 = y;
            return y;
        }
    }

    private static float[] silence(double secs) { return new float[(int) (secs * RATE)]; }

    private static float[] concat(float[]... parts) {
        int n = 0;
        for (float[] p : parts) n += p.length;
        float[] out = new float[n];
        int o = 0;
        for (float[] p : parts) {
            System.arraycopy(p, 0, out, o, p.length);
            o += p.length;
        }
        return out;
    }

    private static void fade(float[] s) {
        int n = Math.min(s.length, 200);
        for (int i = 0; i < n; i++) s[s.length - 1 - i] *= (float) i / n;
    }

    /** Disparo: chasquido de ruido, golpe grave descendente y eco corto. */
    private static float[] gun(Random r, double secs, double gain, double thumpHz, double crack, double decay, boolean tail) {
        int n = (int) (secs * RATE);
        float[] s = new float[n];
        double lp = 0, ph = 0;
        for (int i = 0; i < n; i++) {
            double t = (double) i / RATE;
            double env = Math.exp(-t * decay);
            double crackEnv = Math.exp(-t * decay * 3.5);
            lp += 0.45 * ((r.nextDouble() * 2 - 1) - lp);
            double hp = (r.nextDouble() * 2 - 1) - lp * 0.5;
            ph += 2 * Math.PI * (thumpHz * (0.5 + 0.5 * Math.exp(-t * 30))) / RATE;
            s[i] = (float) ((hp * crack * crackEnv + lp * 0.7 * env + Math.sin(ph) * 0.9 * env) * gain * 0.55);
        }
        int echo = (int) (0.055 * RATE);
        for (int i = n - 1; i >= echo; i--) s[i] += s[i - echo] * (tail ? 0.35f : 0.18f);
        fade(s);
        return s;
    }

    private static float[] pump() {
        float[] a = tick(300, 0.03, 0.7), b = tick(190, 0.04, 0.8);
        return concat(a, silence(0.11), b);
    }

    private static float[] click(double gain) { return tick(1500, 0.015, gain * 0.5); }

    private static float[] tick(double hz, double secs, double gain) {
        float[] s = new float[(int) (secs * RATE)];
        for (int i = 0; i < s.length; i++) {
            double t = (double) i / RATE;
            s[i] = (float) (Math.sin(2 * Math.PI * hz * t) * Math.exp(-t * 90) * gain);
        }
        return s;
    }

    private static float[] chirp(double f0, double f1, double secs, double gain) {
        float[] s = new float[(int) (secs * RATE)];
        double ph = 0;
        for (int i = 0; i < s.length; i++) {
            double k = (double) i / s.length;
            ph += 2 * Math.PI * (f0 + (f1 - f0) * k) / RATE;
            s[i] = (float) (Math.sin(ph) * (1 - k) * gain);
        }
        return s;
    }

    private static float[] buzz(double hz, double secs, double gain) {
        float[] s = new float[(int) (secs * RATE)];
        for (int i = 0; i < s.length; i++) {
            double t = (double) i / RATE;
            double saw = ((t * hz) % 1) * 2 - 1;
            double sq = saw > 0 ? 1 : -1;
            s[i] = (float) ((saw * 0.5 + sq * 0.4) * gain * Math.min(1, (secs - t) * 20));
        }
        return s;
    }

    private static float[] bell(double[] freqs, double noteSecs, double gain) {
        int n = (int) (noteSecs * RATE), tail = (int) (0.25 * RATE);
        float[] s = new float[n * (freqs.length - 1) + n + tail];
        for (int f = 0; f < freqs.length; f++) {
            for (int i = 0; i < n + tail; i++) {
                double t = (double) i / RATE;
                double v = Math.sin(2 * Math.PI * freqs[f] * t) + 0.35 * Math.sin(2 * Math.PI * freqs[f] * 2.76 * t);
                s[f * n + i] += (float) (v * Math.exp(-t * 14) * gain * 0.6);
            }
        }
        fade(s);
        return s;
    }

    private static float[] shimmer(double[] freqs, double noteSecs, double gain) {
        int n = (int) (noteSecs * RATE), tail = (int) (0.4 * RATE);
        float[] s = new float[n * (freqs.length - 1) + n + tail];
        for (int f = 0; f < freqs.length; f++) {
            for (int i = 0; i < n + tail; i++) {
                double t = (double) i / RATE;
                double trem = 0.7 + 0.3 * Math.sin(2 * Math.PI * 12 * t);
                double v = Math.sin(2 * Math.PI * freqs[f] * t) + 0.5 * Math.sin(2 * Math.PI * freqs[f] * 2 * t + 0.4);
                s[f * n + i] += (float) (v * trem * Math.exp(-t * 7) * gain * 0.5);
            }
        }
        fade(s);
        return s;
    }

    private static float[] thud(double gain) {
        float[] s = new float[(int) (0.3 * RATE)];
        double ph = 0;
        Random r = new Random(3);
        double lp = 0;
        for (int i = 0; i < s.length; i++) {
            double t = (double) i / RATE;
            ph += 2 * Math.PI * (70 - 40 * t) / RATE;
            lp += 0.1 * ((r.nextDouble() * 2 - 1) - lp);
            s[i] = (float) ((Math.sin(ph) + lp * 0.6) * Math.exp(-t * 16) * gain);
        }
        fade(s);
        return s;
    }

    private static float[] swish(Random r, double secs, double gain) {
        int n = (int) (secs * RATE);
        float[] s = new float[n];
        Band b = new Band(1800, 1.2);
        for (int i = 0; i < n; i++) {
            double k = (double) i / n;
            double env = Math.sin(Math.PI * k);
            s[i] = (float) (b.next(r.nextDouble() * 2 - 1) * env * gain * 1.6);
        }
        fade(s);
        return s;
    }

    private static float[] fireball(Random r) {
        int n = (int) (0.55 * RATE);
        float[] s = new float[n];
        double ph = 0;
        Band b = new Band(700, 2.5);
        for (int i = 0; i < n; i++) {
            double t = (double) i / RATE, k = (double) i / n;
            ph += 2 * Math.PI * (480 - 330 * k) / RATE;
            double noise = b.next(r.nextDouble() * 2 - 1) * 2.0;
            double saw = ((ph / (2 * Math.PI)) % 1) * 2 - 1;
            s[i] = (float) ((noise * 0.55 + saw * 0.25) * Math.min(1, t * 40) * (1 - k) * 0.8);
        }
        fade(s);
        return s;
    }

    private static float[] door(Random r) {
        int n = (int) (0.8 * RATE);
        float[] s = new float[n];
        double lp = 0;
        Band grind = new Band(160, 4);
        for (int i = 0; i < n; i++) {
            double t = (double) i / RATE, k = (double) i / n;
            lp += 0.06 * ((r.nextDouble() * 2 - 1) - lp);
            double saw = ((t * 52) % 1) * 2 - 1;
            double env = Math.sin(Math.PI * Math.min(1, k * 1.15));
            s[i] = (float) ((lp * 1.6 + saw * 0.22 + grind.next(r.nextDouble() * 2 - 1) * 0.5) * env * 0.7);
        }
        fade(s);
        return s;
    }

    private static float[] explosion(Random r) {
        int n = (int) (1.3 * RATE);
        float[] s = new float[n];
        double lp = 0, lp2 = 0, ph = 0;
        for (int i = 0; i < n; i++) {
            double t = (double) i / RATE;
            double cut = 0.02 + 0.5 * Math.exp(-t * 4);
            lp += cut * ((r.nextDouble() * 2 - 1) - lp);
            lp2 += 0.3 * (lp - lp2);
            ph += 2 * Math.PI * (75 * Math.exp(-t * 1.2)) / RATE;
            double env = Math.exp(-t * 3.0) * Math.min(1, t * 400);
            s[i] = (float) ((lp2 * 2.4 + Math.sin(ph) * 0.9) * env * 0.45);
        }
        fade(s);
        return s;
    }

    /**
     * Voz de criatura: tren de pulsos con tono variable pasado por dos formantes.
     * {@code pitch} da puntos del contorno de tono (se interpolan), {@code vib} es el vibrato en Hz.
     */
    private static float[] voice(double[] pitch, double secs, double f1, double f2, double noise, double gain, double vib) {
        int n = (int) (secs * RATE);
        float[] s = new float[n];
        Band b1 = new Band(f1, 5), b2 = new Band(f2, 6);
        Random r = new Random((long) (f1 * 7 + f2));
        double ph = 0;
        for (int i = 0; i < n; i++) {
            double k = (double) i / n;
            double pos = k * (pitch.length - 1);
            int a = Math.min(pitch.length - 2, (int) pos);
            double f0 = pitch[a] + (pitch[a + 1] - pitch[a]) * (pos - a);
            if (vib > 0) f0 *= 1 + 0.05 * Math.sin(2 * Math.PI * vib * i / RATE);
            ph += f0 / RATE;
            double saw = (ph % 1) * 2 - 1;
            double src = saw * (1 - noise) + (r.nextDouble() * 2 - 1) * noise;
            double v = b1.next(src) * 1.4 + b2.next(src) * 0.9;
            double env = Math.min(1, k * 25) * Math.pow(1 - k, 0.7);
            s[i] = (float) (v * env * gain * 1.5);
        }
        fade(s);
        return s;
    }
}

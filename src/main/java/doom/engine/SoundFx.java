package doom.engine;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.util.Random;

/** Efectos de sonido sinteticos (sin archivos). Si no hay dispositivo de audio, no hace nada. */
public class SoundFx {

    public static final int PISTOL = 0, SHOTGUN = 1, CHAINGUN = 2, HURT = 3, PICKUP = 4, DOOR = 5,
            ENEMY_DIE = 6, FIREBALL = 7, ZOMBIE_SHOT = 8, EXIT = 9, DENIED = 10, DEATH = 11, EXPLODE = 12, POWERUP = 13, BOSS = 14;
    private static final float RATE = 22050f;

    private final Clip[] clips = new Clip[15];
    public boolean muted;

    public SoundFx() {
        try {
            Random r = new Random(5);
            set(PISTOL, burst(r, 0.16, 0.9, 140, 7));
            set(SHOTGUN, burst(r, 0.42, 1.0, 90, 4));
            set(CHAINGUN, burst(r, 0.10, 0.8, 170, 12));
            set(HURT, sweep(260, 90, 0.22, 0.7, false));
            set(PICKUP, arpeggio(new double[]{660, 880}, 0.07, 0.5));
            set(DOOR, burst(r, 0.45, 0.45, 55, 2));
            set(ENEMY_DIE, sweep(220, 50, 0.45, 0.7, true));
            set(FIREBALL, sweep(500, 180, 0.3, 0.45, true));
            set(ZOMBIE_SHOT, burst(r, 0.14, 0.7, 200, 9));
            set(EXIT, arpeggio(new double[]{440, 554, 659, 880}, 0.11, 0.6));
            set(DENIED, sweep(180, 150, 0.2, 0.5, true));
            set(DEATH, sweep(300, 40, 0.9, 0.8, true));
            set(EXPLODE, burst(r, 0.7, 1.0, 45, 3));
            set(POWERUP, arpeggio(new double[]{440, 660, 880, 1320}, 0.06, 0.55));
            set(BOSS, sweep(110, 55, 0.8, 0.8, true));
        } catch (Throwable t) {
            muted = true; // sin audio disponible
        }
    }

    public void play(int id) {
        if (muted) return;
        try {
            Clip c = clips[id];
            if (c == null) return;
            if (c.isRunning()) c.stop();
            c.setFramePosition(0);
            c.start();
        } catch (Throwable ignored) {
            // el audio nunca debe romper el juego
        }
    }

    private void set(int id, byte[] pcm) throws Exception {
        Clip c = AudioSystem.getClip();
        c.open(new AudioFormat(RATE, 16, 1, true, false), pcm, 0, pcm.length);
        clips[id] = c;
    }

    private static byte[] pack(double[] s) {
        byte[] out = new byte[s.length * 2];
        for (int i = 0; i < s.length; i++) {
            int v = (int) (Math.max(-1, Math.min(1, s[i])) * 30000);
            out[i * 2] = (byte) v;
            out[i * 2 + 1] = (byte) (v >> 8);
        }
        return out;
    }

    private static byte[] burst(Random r, double secs, double gain, double thumpHz, double decay) {
        double[] s = new double[(int) (secs * RATE)];
        double lp = 0;
        for (int i = 0; i < s.length; i++) {
            double t = i / RATE;
            double env = Math.exp(-t * decay * 4 / Math.max(0.1, secs * 4));
            lp += 0.35 * ((r.nextDouble() * 2 - 1) - lp);
            s[i] = (lp * 0.8 + Math.sin(2 * Math.PI * thumpHz * t) * 0.5) * env * gain;
        }
        return pack(s);
    }

    private static byte[] sweep(double f0, double f1, double secs, double gain, boolean saw) {
        double[] s = new double[(int) (secs * RATE)];
        double ph = 0;
        for (int i = 0; i < s.length; i++) {
            double k = (double) i / s.length;
            ph += 2 * Math.PI * (f0 + (f1 - f0) * k) / RATE;
            double w = saw ? (((ph / Math.PI) % 2) - 1) : Math.sin(ph);
            s[i] = w * (1 - k) * gain;
        }
        return pack(s);
    }

    private static byte[] arpeggio(double[] freqs, double noteSecs, double gain) {
        int n = (int) (noteSecs * RATE);
        double[] s = new double[n * freqs.length];
        for (int f = 0; f < freqs.length; f++) {
            for (int i = 0; i < n; i++) {
                double env = 1 - (double) i / n;
                s[f * n + i] = Math.sin(2 * Math.PI * freqs[f] * i / RATE) * env * gain;
            }
        }
        return pack(s);
    }
}

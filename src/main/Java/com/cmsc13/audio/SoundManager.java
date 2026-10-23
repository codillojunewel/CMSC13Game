package com.cmsc13.audio;

import com.cmsc13.model.Settings;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;

/**
 * Plays the game's music and sound effects (WAV files under /audio) using the
 * JDK's built-in javax.sound API, so no extra dependency is required.
 * Every failure is swallowed: a PC without speakers can still play the game.
 */
public final class SoundManager {

    /** Every audio asset, mapped to its file (see "Sound & Background Music Asset Mapping"). */
    public enum Sound {
        MAIN_THEME("main_theme.wav", true),
        AUDIT_THEME("audit_theme.wav", true),
        CORRECT("correct_ping.wav", false),
        WRONG("wrong_buzz.wav", false),
        HOVER("hover.wav", false),
        PRESS("press.wav", false),
        DEFEAT("cinematicerror.wav", false),
        VICTORY("cinematicceleb.wav", false),
        TICK("tick.wav", false),
        TYPE("type.wav", false),
        LIFELINE("lifeline.wav", false),
        REBOOT("reboot.wav", false);

        private final String file;
        private final boolean music;

        Sound(String file, boolean music) {
            this.file = file;
            this.music = music;
        }

        public boolean isMusic() { return music; }
    }

    private static final SoundManager INSTANCE = new SoundManager();

    private final Map<Sound, Clip> clips = new EnumMap<>(Sound.class);
    private Sound currentMusic;
    private volatile boolean loaded;

    private SoundManager() {
    }

    public static SoundManager get() {
        return INSTANCE;
    }

    /** Loads every clip; safe to call from a background thread. */
    public synchronized void preload() {
        if (loaded) {
            return;
        }
        for (Sound s : Sound.values()) {
            try (InputStream raw = SoundManager.class.getResourceAsStream("/audio/" + s.file)) {
                if (raw == null) {
                    continue;
                }
                AudioInputStream in = AudioSystem.getAudioInputStream(new BufferedInputStream(raw));
                Clip clip = AudioSystem.getClip();
                clip.open(in);
                clips.put(s, clip);
            } catch (Exception | LinkageError e) {
                System.err.println("[audio] could not load " + s.file + ": " + e.getMessage());
            }
        }
        loaded = true;
    }

    public void play(Sound sound) {
        if (!loaded) {
            return;
        }
        Clip clip = clips.get(sound);
        if (clip == null) {
            return;
        }
        try {
            if (clip.isRunning()) {
                clip.stop();
            }
            clip.setFramePosition(0);
            applyVolume(clip, Settings.get().getSfxVolume());
            clip.start();
        } catch (Exception ignored) {
        }
    }

    public synchronized void playMusic(Sound track) {
        if (!loaded || track == currentMusic) {
            return;
        }
        stopMusic();
        Clip clip = clips.get(track);
        if (clip == null) {
            return;
        }
        try {
            clip.setFramePosition(0);
            applyVolume(clip, Settings.get().getMusicVolume());
            clip.loop(Clip.LOOP_CONTINUOUSLY);
            currentMusic = track;
        } catch (Exception ignored) {
        }
    }

    public synchronized void stopMusic() {
        if (currentMusic != null) {
            Clip clip = clips.get(currentMusic);
            if (clip != null) {
                try {
                    clip.stop();
                } catch (Exception ignored) {
                }
            }
            currentMusic = null;
        }
    }

    /** Re-applies the music volume from {@link Settings} (used by the settings sliders). */
    public synchronized void refreshMusicVolume() {
        if (currentMusic != null && clips.get(currentMusic) != null) {
            applyVolume(clips.get(currentMusic), Settings.get().getMusicVolume());
        }
    }

    public void shutdown() {
        stopMusic();
        clips.values().forEach(c -> {
            try {
                c.close();
            } catch (Exception ignored) {
            }
        });
    }

    private static void applyVolume(Clip clip, double volume) {
        try {
            if (clip.isControlSupported(BooleanControl.Type.MUTE)) {
                ((BooleanControl) clip.getControl(BooleanControl.Type.MUTE)).setValue(volume <= 0.001);
            }
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float db = volume <= 0.001 ? gain.getMinimum() : (float) (20.0 * Math.log10(volume));
                gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), db)));
            }
        } catch (Exception ignored) {
        }
    }
}

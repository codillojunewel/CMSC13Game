package com.cmsc13.model;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.prefs.Preferences;

/**
 * Player settings, persisted between launches with {@link Preferences}
 * (stored in the Windows registry under the current user).
 */
public final class Settings {

    /** Countdown speed presets. */
    public enum TimerMode {
        OFF("Off", 0),
        RELAXED("Relaxed", 1.5),
        STANDARD("Standard", 1.0),
        HARDCORE("Hardcore", 0.6);

        private final String label;
        private final double multiplier;

        TimerMode(String label, double multiplier) {
            this.label = label;
            this.multiplier = multiplier;
        }

        public String getLabel() { return label; }
        public double getMultiplier() { return multiplier; }

        @Override
        public String toString() { return label; }
    }

    private static final Settings INSTANCE = new Settings();
    private final Preferences prefs = Preferences.userRoot().node("com/cmsc13/juniordev/settings");

    private double musicVolume;
    private double sfxVolume;
    private TimerMode timerMode;
    private boolean webcamEnabled;
    private String lastPlayerName;

    private Settings() {
        musicVolume = prefs.getDouble("musicVolume", 0.45);
        sfxVolume = prefs.getDouble("sfxVolume", 0.8);
        webcamEnabled = prefs.getBoolean("webcamEnabled", true);
        lastPlayerName = prefs.get("lastPlayerName", "");
        try {
            timerMode = TimerMode.valueOf(prefs.get("timerMode", TimerMode.STANDARD.name()));
        } catch (IllegalArgumentException e) {
            timerMode = TimerMode.STANDARD;
        }
    }

    public static Settings get() {
        return INSTANCE;
    }

    public void save() {
        prefs.putDouble("musicVolume", musicVolume);
        prefs.putDouble("sfxVolume", sfxVolume);
        prefs.put("timerMode", timerMode.name());
        prefs.putBoolean("webcamEnabled", webcamEnabled);
        prefs.put("lastPlayerName", lastPlayerName == null ? "" : lastPlayerName);
        try {
            prefs.flush();
        } catch (Exception ignored) {
            // Preferences are best-effort; the game still works without them.
        }
    }

    /** Folder where confession videos are logged ("the corporate database"). */
    public static Path confessionsFolder() {
        return Paths.get(System.getProperty("user.home"), "JuniorDevAudit", "confessions");
    }

    public double getMusicVolume() { return musicVolume; }
    public void setMusicVolume(double v) { this.musicVolume = clamp(v); }

    public double getSfxVolume() { return sfxVolume; }
    public void setSfxVolume(double v) { this.sfxVolume = clamp(v); }

    public TimerMode getTimerMode() { return timerMode; }
    public void setTimerMode(TimerMode timerMode) { this.timerMode = timerMode; }

    public boolean isWebcamEnabled() { return webcamEnabled; }
    public void setWebcamEnabled(boolean webcamEnabled) { this.webcamEnabled = webcamEnabled; }

    public String getLastPlayerName() { return lastPlayerName; }
    public void setLastPlayerName(String name) { this.lastPlayerName = name; }

    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }
}

package com.cmsc13.model;

import java.util.prefs.Preferences;

/** Lifetime career record shown on the main menu, persisted with {@link Preferences}. */
public final class PlayerStats {

    private static final PlayerStats INSTANCE = new PlayerStats();
    private final Preferences prefs = Preferences.userRoot().node("com/cmsc13/juniordev/stats");

    /** When frozen (used by the developer screen tour), nothing is recorded. */
    private boolean frozen;

    private PlayerStats() {
    }

    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }

    public static PlayerStats get() {
        return INSTANCE;
    }

    public int getBestRung() { return prefs.getInt("bestRung", 0); }
    public int getAttempts() { return prefs.getInt("attempts", 0); }
    public int getWins() { return prefs.getInt("wins", 0); }
    public int getConfessions() { return prefs.getInt("confessions", 0); }
    /** Fastest full audit in milliseconds, or 0 if never passed. */
    public long getFastestWin() { return prefs.getLong("fastestWin", 0); }

    public void recordAttemptStarted() {
        if (frozen) {
            return;
        }
        prefs.putInt("attempts", getAttempts() + 1);
        flush();
    }

    public void recordProgress(int clearedRungs) {
        if (frozen) {
            return;
        }
        if (clearedRungs > getBestRung()) {
            prefs.putInt("bestRung", clearedRungs);
            flush();
        }
    }

    public void recordWin(long elapsedMillis) {
        if (frozen) {
            return;
        }
        prefs.putInt("wins", getWins() + 1);
        long best = getFastestWin();
        if (best == 0 || elapsedMillis < best) {
            prefs.putLong("fastestWin", elapsedMillis);
        }
        recordProgress(SurvivalLadder.SIZE);
        flush();
    }

    public int recordConfession() {
        if (frozen) {
            return getConfessions();
        }
        int n = getConfessions() + 1;
        prefs.putInt("confessions", n);
        flush();
        return n;
    }

    public void reset() {
        try {
            prefs.clear();
        } catch (Exception ignored) {
        }
        flush();
    }

    private void flush() {
        try {
            prefs.flush();
        } catch (Exception ignored) {
        }
    }

    public static String formatDuration(long millis) {
        long s = millis / 1000;
        return String.format("%02d:%02d", s / 60, s % 60);
    }
}

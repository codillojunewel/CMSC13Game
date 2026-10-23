package com.cmsc13.model.lifeline;

import com.cmsc13.model.GameSession;

/**
 * Base class for the "developer adaptation" of the classic game-show lifelines.
 * Each subclass overrides {@link #activate(GameSession)} (polymorphism), and
 * the game screen treats every lifeline through this common interface.
 */
public abstract class Lifeline {

    private final String name;
    private final String command;
    private final String description;
    private boolean used;

    protected Lifeline(String name, String command, String description) {
        this.name = name;
        this.command = command;
        this.description = description;
    }

    public String getName() { return name; }
    public String getCommand() { return command; }
    public String getDescription() { return description; }
    public boolean isUsed() { return used; }

    protected void markUsed() { this.used = true; }

    /** Passive lifelines (like SAVE) trigger automatically and cannot be clicked. */
    public boolean isManual() { return true; }

    public boolean canActivate(GameSession session) {
        return !used && isManual() && session.isAwaitingAnswer();
    }

    /** Applies the lifeline to the current question. */
    public abstract LifelineResult activate(GameSession session);

    /** Text shown on the lifeline button's status line. */
    public String statusText() {
        return used ? "USED" : (isManual() ? "READY" : "ARMED");
    }
}

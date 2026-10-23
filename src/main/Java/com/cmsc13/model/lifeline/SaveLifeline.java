package com.cmsc13.model.lifeline;

import com.cmsc13.model.GameSession;

/**
 * SAVE: a one-time try/catch around your career. If you commit a wrong answer
 * but the proctor's own answer was correct, the exception is caught and you survive.
 * It is passive: it arms itself and fires automatically.
 */
public class SaveLifeline extends Lifeline {

    public SaveLifeline() {
        super("SAVE", "try { } catch",
                "Passive. The first time you crash, if the Junior Dev got it right, the exception is caught and you survive.");
    }

    @Override
    public boolean isManual() {
        return false;
    }

    /** Returns true (and consumes the lifeline) when it rescues a wrong answer. */
    public boolean tryCatch(GameSession session) {
        if (isUsed() || !session.isProctorCorrect()) {
            return false;
        }
        markUsed();
        session.recordLifelineUse();
        return true;
    }

    @Override
    public LifelineResult activate(GameSession session) {
        return new LifelineResult("SAVE is passive: it fires automatically when you crash.", -1, false);
    }
}

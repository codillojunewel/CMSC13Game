package com.cmsc13.model.lifeline;

import com.cmsc13.model.GameSession;
import com.cmsc13.model.Question;

/** COPY: copy-paste the proctor's answer and commit it immediately, right or wrong. */
public class CopyLifeline extends Lifeline {

    public CopyLifeline() {
        super("COPY", "Ctrl+C Ctrl+V",
                "Blindly copy-paste the Junior Dev's answer. It is committed instantly, right or wrong.");
    }

    @Override
    public LifelineResult activate(GameSession session) {
        markUsed();
        session.recordLifelineUse();
        int idx = session.getProctorAnswer();
        return new LifelineResult("Copied from " + session.getProctor().getDeskName()
                + "'s terminal: " + Question.letter(idx) + ". Committing... no take-backs!", idx, true);
    }
}

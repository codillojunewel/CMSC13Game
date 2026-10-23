package com.cmsc13.model.lifeline;

import com.cmsc13.model.GameSession;
import com.cmsc13.model.Question;

/** PEEK: read the proctor's local memory terminal to see their answer. You still choose. */
public class PeekLifeline extends Lifeline {

    public PeekLifeline() {
        super("PEEK", "cat memory.log",
                "Read the Junior Dev's memory terminal to see what they answered. You still decide.");
    }

    @Override
    public LifelineResult activate(GameSession session) {
        markUsed();
        session.recordLifelineUse();
        int idx = session.getProctorAnswer();
        return new LifelineResult("[memory.log] " + session.getProctor().getDeskName()
                + " locked in " + Question.letter(idx) + ". Trust it... or don't.", idx, false);
    }
}

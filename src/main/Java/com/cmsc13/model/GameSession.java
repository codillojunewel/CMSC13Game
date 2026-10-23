package com.cmsc13.model;

import com.cmsc13.model.lifeline.CopyLifeline;
import com.cmsc13.model.lifeline.Lifeline;
import com.cmsc13.model.lifeline.PeekLifeline;
import com.cmsc13.model.lifeline.SaveLifeline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Holds the state of one audit run (the "model" in MVC). The controllers only
 * read from and send commands to this class; they never decide game rules.
 */
public class GameSession {

    /** Result of committing an answer. */
    public enum Outcome { CORRECT, SAVED, WRONG, TIMEOUT }

    /** How often a proctor's memory terminal holds the right answer. */
    private static final double PROCTOR_ACCURACY = 0.80;

    private final String playerName;
    private final int attemptNumber;
    private final QuestionBank bank;
    private final Random random = new Random();

    private final PeekLifeline peek = new PeekLifeline();
    private final CopyLifeline copy = new CopyLifeline();
    private final SaveLifeline save = new SaveLifeline();
    private final List<Lifeline> lifelines = List.of(peek, copy, save);

    private final Set<Integer> usedQuestionIds = new HashSet<>();
    /** Questions that crashed the player in earlier runs, keyed by rung index: they come back. */
    private final Map<Integer, Integer> haunted;

    private int rungIndex = -1;
    private Question current;
    private int proctorAnswer;
    private boolean awaitingAnswer;
    private int lifelinesUsed;
    private int lastSelected = -1;
    private boolean currentIsRepeat;
    private Outcome lastOutcome;
    private final long startNanos = System.nanoTime();
    private long endNanos;

    public GameSession(String playerName, int attemptNumber) {
        this(playerName, attemptNumber, new HashMap<>());
    }

    private GameSession(String playerName, int attemptNumber, Map<Integer, Integer> haunted) {
        this.playerName = playerName;
        this.attemptNumber = attemptNumber;
        this.haunted = haunted;
        this.bank = QuestionBank.getInstance();
    }

    /**
     * Forced reboot after a crash: progress is wiped, but the question that
     * caused the crash is remembered and will be asked again at the same rung.
     */
    public GameSession reboot() {
        Map<Integer, Integer> carry = new HashMap<>(haunted);
        if (current != null && rungIndex >= 0) {
            carry.put(rungIndex, current.getId());
        }
        return new GameSession(playerName, attemptNumber + 1, carry);
    }

    // --------------------------------------------------------------- flow

    /** Moves to the next rung and draws its question. */
    public Question nextQuestion() {
        rungIndex++;
        LadderRung rung = getRung();

        Integer hauntedId = haunted.remove(rungIndex);
        currentIsRepeat = hauntedId != null;
        current = (hauntedId != null)
                ? bank.findById(hauntedId).orElseGet(() -> bank.pick(rung.pool(), usedQuestionIds))
                : bank.pick(rung.pool(), usedQuestionIds);
        usedQuestionIds.add(current.getId());

        // The proctor's memory terminal: usually right, sometimes confidently wrong.
        if (random.nextDouble() < PROCTOR_ACCURACY) {
            proctorAnswer = current.getCorrectIndex();
        } else {
            int wrong;
            do {
                wrong = random.nextInt(4);
            } while (wrong == current.getCorrectIndex());
            proctorAnswer = wrong;
        }

        lastSelected = -1;
        awaitingAnswer = true;
        return current;
    }

    /** Commits an answer (or -1 for a timeout) and returns the outcome. */
    public Outcome commit(int choiceIndex) {
        if (!awaitingAnswer) {
            throw new IllegalStateException("No question is waiting for an answer");
        }
        awaitingAnswer = false;
        lastSelected = choiceIndex;

        if (choiceIndex == current.getCorrectIndex()) {
            lastOutcome = Outcome.CORRECT;
        } else if (save.tryCatch(this)) {
            lastOutcome = Outcome.SAVED;
        } else {
            lastOutcome = (choiceIndex < 0) ? Outcome.TIMEOUT : Outcome.WRONG;
        }

        if (lastOutcome == Outcome.CORRECT || lastOutcome == Outcome.SAVED) {
            if (isComplete()) {
                endNanos = System.nanoTime();
            }
        } else {
            endNanos = System.nanoTime();
        }
        return lastOutcome;
    }

    /** True once the 10th rung has been cleared. */
    public boolean isComplete() {
        return rungIndex == SurvivalLadder.SIZE - 1 && !awaitingAnswer
                && (lastOutcome == Outcome.CORRECT || lastOutcome == Outcome.SAVED);
    }

    public void recordLifelineUse() {
        lifelinesUsed++;
    }

    // ------------------------------------------------------------ getters

    public String getPlayerName() { return playerName; }
    public int getAttemptNumber() { return attemptNumber; }
    public int getRungIndex() { return rungIndex; }
    public LadderRung getRung() { return SurvivalLadder.get(Math.max(rungIndex, 0)); }
    public Persona getProctor() { return getRung().proctor(); }
    public Question getCurrent() { return current; }
    public int getProctorAnswer() { return proctorAnswer; }
    public boolean isProctorCorrect() { return current != null && proctorAnswer == current.getCorrectIndex(); }
    public boolean isAwaitingAnswer() { return awaitingAnswer; }
    public int getLastSelected() { return lastSelected; }
    public Outcome getLastOutcome() { return lastOutcome; }
    public int getLifelinesUsed() { return lifelinesUsed; }
    public List<Lifeline> getLifelines() { return new ArrayList<>(lifelines); }
    public PeekLifeline getPeek() { return peek; }
    public CopyLifeline getCopy() { return copy; }
    public SaveLifeline getSave() { return save; }
    /** True when the current question is the one that crashed the player in a previous run. */
    public boolean isRepeatQuestion() { return currentIsRepeat; }

    /** Number of rungs cleared so far (0-10). */
    public int getClearedCount() {
        if (rungIndex < 0) {
            return 0;
        }
        boolean currentCleared = !awaitingAnswer
                && (lastOutcome == Outcome.CORRECT || lastOutcome == Outcome.SAVED);
        return rungIndex + (currentCleared ? 1 : 0);
    }

    public long getElapsedMillis() {
        long end = endNanos == 0 ? System.nanoTime() : endNanos;
        return (end - startNanos) / 1_000_000L;
    }

    public boolean hasHauntedQuestions() {
        return !haunted.isEmpty();
    }
}

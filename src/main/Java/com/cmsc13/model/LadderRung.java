package com.cmsc13.model;

import java.util.function.Predicate;

/**
 * One step on the Survival Ladder.
 *
 * @param number      position on the ladder (1-10)
 * @param level       academic competence tier (Level 1-7 from the proposal)
 * @param tierName    e.g. "Procedural Thinking"
 * @param topic       the paradigm/lesson tested at this rung
 * @param proctor     the Junior Dev who proctors this rung
 * @param pool        which questions may be drawn for this rung
 * @param baseSeconds countdown length before the timer multiplier is applied
 */
public record LadderRung(int number,
                         int level,
                         String tierName,
                         String topic,
                         Persona proctor,
                         Predicate<Question> pool,
                         int baseSeconds) {

    /** Pool of theoretical questions from one lecture module. */
    static Predicate<Question> module(int moduleNo) {
        return q -> !q.isProgramming() && q.getModuleNo() != null && q.getModuleNo() == moduleNo;
    }

    /** Pool of programming questions written in one language. */
    static Predicate<Question> language(String language) {
        return q -> q.isProgramming() && language.equalsIgnoreCase(q.getLanguage());
    }
}

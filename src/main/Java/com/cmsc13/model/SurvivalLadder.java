package com.cmsc13.model;

import java.util.List;

import static com.cmsc13.model.LadderRung.language;
import static com.cmsc13.model.LadderRung.module;

/**
 * The 10-question competency audit. Rungs climb through the seven academic
 * tiers, proctored by the six Junior Devs at the contestant desks.
 */
public final class SurvivalLadder {

    public static final List<LadderRung> RUNGS = List.of(
            new LadderRung(1, 1, "Programming Foundations", "Introduction to Programming Paradigms",
                    Persona.ISAK, module(1), 45),
            new LadderRung(2, 2, "Procedural Thinking", "Procedural Programming",
                    Persona.HALIMA, module(2), 45),
            new LadderRung(3, 2, "Procedural Thinking", "Procedural code in C",
                    Persona.HALIMA, language("C"), 90),
            new LadderRung(4, 3, "Functional Thinking", "Functional Programming",
                    Persona.HALEY, module(3), 45),
            new LadderRung(5, 3, "Functional Thinking", "Python code review",
                    Persona.HALEY, language("Python"), 90),
            new LadderRung(6, 4, "Object-Oriented Design", "Object-Oriented Programming",
                    Persona.AYA, module(4), 45),
            new LadderRung(7, 4, "Object-Oriented Design", "Object-oriented code in Java",
                    Persona.AYA, language("Java"), 90),
            new LadderRung(8, 5, "Programming Models & Abstractions", "Imperative vs. Declarative Programming",
                    Persona.ISAK, module(5), 45),
            new LadderRung(9, 6, "Reactive & Event-Driven Systems", "Event-driven Programming",
                    Persona.CAMERON, module(6).or(language("JavaScript")), 75),
            new LadderRung(10, 7, "Paradigm Integration", "Component Mapping between Paradigms",
                    Persona.PRIYA, module(7), 50)
    );

    public static final int SIZE = RUNGS.size();

    private SurvivalLadder() {
    }

    public static LadderRung get(int index) {
        return RUNGS.get(index);
    }
}

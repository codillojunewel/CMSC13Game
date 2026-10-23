package com.cmsc13.model;

import java.util.List;
import java.util.Random;

/**
 * The AI "Junior Devs" seated at the contestant desks (as drawn in the GUI
 * mockups). Each one embodies a programming paradigm, proctors its own rungs
 * of the Survival Ladder and has its own lines of dialogue.
 */
public enum Persona {

    HALIMA("Halima", "Procedural Programming", "#7dff6a", "halima",
            "A clipboard-carrying intern obsessed with routine structure: subroutines, parameters and tidy call stacks.",
            List.of("Step one: read. Step two: think. Step three: commit. Very procedural.",
                    "Let's call this subroutine. Parameters ready?",
                    "I already wrote my answer in a reusable function. Did you?"),
            List.of("return SUCCESS; Nicely structured!",
                    "Clean call, clean return. Noted on my clipboard.",
                    "That answer is modular. I approve."),
            List.of("Stack overflow... in your career.",
                    "That subroutine returned garbage.",
                    "Even my clipboard knew that one.")),

    ISAK("Isak", "Imperative Programming", "#ffffff", "isak",
            "A strict micromanager who dictates every single step, loop and variable assignment.",
            List.of("Line 1: focus. Line 2: think. Line 3: commit. Execute in order.",
                    "I will be watching every state change you make.",
                    "while (!answered) { sweat++; }"),
            List.of("State updated: competence = true.",
                    "Correct. You followed my instructions exactly.",
                    "Acceptable. Proceed to the next instruction."),
            List.of("ERROR: unexpected state mutation in your brain.",
                    "You skipped a step. I told you not to skip steps.",
                    "goto termination;")),

    AYA("Aya", "Object-Oriented Programming", "#ffe14d", "aya",
            "A software architect holding blueprints. Encapsulation, inheritance, dynamic dispatch: everything is an object.",
            List.of("Let's see if you inherit any of your old competence.",
                    "This question encapsulates your whole future. No pressure.",
                    "I'll override your confidence if I have to."),
            List.of("@Override public boolean isSmart() { return true; }",
                    "Beautifully encapsulated answer.",
                    "Promoted to a subclass of 'Still Employed'."),
            List.of("NullPointerException: competence not found.",
                    "That method was abstract, just like your answer.",
                    "You've been dynamically dispatched... out of a job.")),

    HALEY("Haley", "Functional Programming", "#ffe14d", "haley",
            "A zen-like scholar who avoids side effects: pure functions, immutability, recursion, map / filter / reduce.",
            List.of("Breathe in. Map your thoughts. Filter the noise. Reduce to one answer.",
                    "Same input, same output. Be pure, like a function.",
                    "Mutation is suffering. Choose wisely."),
            List.of("A pure answer. No side effects.",
                    "x -> x.isCorrect(). Referentially transparent!",
                    "Your mind is immutable and correct."),
            List.of("A side effect occurred: your termination.",
                    "That answer was impure.",
                    "Recursion without a base case... like this attempt.")),

    CAMERON("Cameron", "Event-Driven Programming", "#c486ff", "cameron",
            "A hyper-reactive dev who lives inside the event loop: listeners, callbacks and handlers fire at every click.",
            List.of("onQuestionShown() -> panic(). Just kidding. Mostly.",
                    "I'm listening for your click... addEventListener('answer', you).",
                    "Events are queued. Your move triggers the next one."),
            List.of("Event handled! Callback says: you're good.",
                    "dispatchEvent(new Event('stillEmployed'))",
                    "Your answer bubbled all the way up. Nice."),
            List.of("Uncaught (in promise): your career.",
                    "That click fired the wrong handler.",
                    "Event loop blocked by... you.")),

    PRIYA("Priya", "Declarative Programming", "#ff7ac8", "priya",
            "An automated deductive query terminal. She declares WHAT she wants (SQL, logic rules, UI state), never HOW.",
            List.of("?- smarter_than_junior_dev(you). Evaluating...",
                    "SELECT answer FROM your_brain WHERE correct = TRUE;",
                    "I don't care how you think. Only what you answer."),
            List.of("true.",
                    "Query satisfied. 1 row returned.",
                    "Fact asserted: employee.competent = yes."),
            List.of("false.",
                    "Query returned 0 rows. Like your job prospects.",
                    "Unification failed. Goal: employment. Result: no."));

    private final String displayName;
    private final String paradigm;
    private final String color;
    private final String imageKey;
    private final String bio;
    private final List<String> introLines;
    private final List<String> correctLines;
    private final List<String> wrongLines;

    private static final Random RANDOM = new Random();

    Persona(String displayName, String paradigm, String color, String imageKey, String bio,
            List<String> introLines, List<String> correctLines, List<String> wrongLines) {
        this.displayName = displayName;
        this.paradigm = paradigm;
        this.color = color;
        this.imageKey = imageKey;
        this.bio = bio;
        this.introLines = introLines;
        this.correctLines = correctLines;
        this.wrongLines = wrongLines;
    }

    public String getDisplayName() { return displayName; }
    /** Name exactly as printed on the desk nameplate. */
    public String getDeskName() { return displayName.toUpperCase(); }
    public String getParadigm() { return paradigm; }
    public String getColor() { return color; }
    public String getBio() { return bio; }

    /** Classpath location of this Junior Dev's portrait. */
    public String getPortrait() { return "/images/portrait_" + imageKey + ".png"; }

    public String randomIntro() { return pick(introLines); }
    public String randomCorrect() { return pick(correctLines); }
    public String randomWrong() { return pick(wrongLines); }

    private static String pick(List<String> lines) {
        return lines.get(RANDOM.nextInt(lines.size()));
    }
}

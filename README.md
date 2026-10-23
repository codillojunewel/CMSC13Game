# Are You Smarter Than a Junior Dev?

CMSC 13: Survey of Programming Paradigms · Machine Problem · Group 7 (Diaz, Carl Jacob R. & Codillo, Junewel L.)

This is a JavaFX game show adapted from *Are You Smarter Than a 5th Grader?*. You play a Senior Systems Architect whose old certifications are under review by a ruthless AI HR system. To keep your job, you must pass a **10-question competency audit**. Six AI "Junior Devs" sit at the contestant desks (Halima, Isak, Aya, Haley, Cameron and Priya), and each one specialises in a programming paradigm.

## Running the game

You need **JDK 17+** and **Maven**.

```
mvn javafx:run
```

## Building a standalone file

| Command | Output |
|---|---|
| `mvn package` | `target/JuniorDev.jar`: one runnable "fat JAR". Start it with `java -jar target/JuniorDev.jar` |
| `mvn package -Pexe` | `target/JuniorDev.exe`: a Launch4j wrapper with an icon and `-Xms128m -Xmx512m` (needs Java 17+ installed) |

## How to play

1. On the **Check-in** screen, type your name. HR needs it for the confession.
2. Each level shows a question. Click an answer (or press **A–D** / **1–4**), then click **COMMIT** (or press **Enter**).
3. Every correct answer moves you up one rung of the **Survival Ladder**. Clear Level 10 to pass the audit.
4. A wrong answer (or running out of time) triggers a crash:
   - **Fatal Core Dump**: the Junior Dev dumps its memory and shows the correct answer and the rationale.
   - **Public Humiliation Protocol**: your webcam turns on and you must record *"My name is [Name], and I am NOT smarter than a Junior Dev!"*. The video is saved to `%USERPROFILE%\JuniorDevAudit\confessions`. With no webcam, an audio fallback asks you to say the line out loud instead.
   - **Forced reboot**: your progress is wiped and you start again from Level 1. The question that crashed you stays "cached" and will be asked again at the same level.

**F11** toggles full screen. Music, SFX, the timer mode and the webcam mode are all set in **Settings**.

### Lifelines (developer edition, one use each)

| Lifeline | Command | Effect |
|---|---|---|
| PEEK | `cat memory.log` | Shows the proctor's answer. You still choose. |
| COPY | `Ctrl+C Ctrl+V` | Commits the proctor's answer immediately, right or wrong. |
| SAVE | `try { } catch` | Passive. The first time you crash, if the proctor's answer was correct, you survive. |

Proctors are right about 80% of the time.

### Survival Ladder

| Level | Tier | Questions from | Proctor (desk) |
|---|---|---|---|
| 1 | L1 Programming Foundations | Module 1 | Isak (Imperative) |
| 2 | L2 Procedural Thinking | Module 2 | Halima (Procedural) |
| 3 | L2 Procedural Thinking | C code | Halima (Procedural) |
| 4 | L3 Functional Thinking | Module 3 | Haley (Functional) |
| 5 | L3 Functional Thinking | Python code | Haley (Functional) |
| 6 | L4 Object-Oriented Design | Module 4 | Aya (Object-Oriented) |
| 7 | L4 Object-Oriented Design | Java code | Aya (Object-Oriented) |
| 8 | L5 Programming Models & Abstractions | Module 5 | Isak (Imperative) |
| 9 | L6 Reactive & Event-Driven Systems | Module 6 + JavaScript code | Cameron (Event-Driven) |
| 10 | L7 Paradigm Integration | Module 7 | Priya (Declarative) |

The bank holds 145 questions (70 theoretical + 75 programming) in `src/main/resources/data/questions.json`. That file is generated from `Group7_Assignment2_MCQ_Questions.docx`, with code indentation preserved.

## Graphics: matching the proposal mockups

Every screen is built on the GUI mockups from the MP proposal (section 2). `tools/prep_assets.py` extracts them at 1280x720 and erases the parts that change during play: the loading bar, the question monitor, button labels and the error code. The game then draws the live elements at the exact same spots:

| Proposal mockup | Game screen |
|---|---|
| 2.1 Splash screen | Splash: the loading bar fills for real |
| 2.2 Main menu | Main menu: the drawn buttons are clickable |
| 2.2 Studio "Start" | Start screen, then the employee check-in |
| 2.2 Question screen | Game: the question goes on the big monitor, the proctor's desk lights up, ALGORITHM FLOW tracks the answer phase, and the empty right-hand monitor is the proctor cam + timer |
| 2.3 Level Complete | Shown after each cleared level; also the final "Audit Passed" screen |
| 2.3 Error screen | Defeat screen (glitch effect, live error code, OK button) |
| 2.3 Webcam confession | Public Humiliation Protocol with the live webcam inside the red HUD frame |

Titles and buttons use a hand-made 5x8 pixel font (`resources/fonts/pixel5x8.txt`, drawn by `PixelText`).

## Architecture (MVC)

```
src/main/Java/com/cmsc13/
├── Main.java / Launcher.java     entry points (Launcher is used by the fat JAR / exe)
├── model/                        MODEL: rules and data, no JavaFX
│   ├── Question, QuestionBank    question data + singleton loader (Jackson)
│   ├── LadderRung, SurvivalLadder  the 10 levels and their question pools
│   ├── Persona                   the 6 Junior Devs at the desks (enum)
│   ├── GameSession               state of one run: answers, timer-independent rules, reboot
│   ├── Settings, PlayerStats     saved with java.util.prefs
│   └── lifeline/                 Lifeline (abstract) → Peek / Copy / Save
├── audio/SoundManager            music + SFX with javax.sound (WAVs in resources/audio)
└── frontend/                     CONTROLLERS (one per screen) + SceneManager
    └── ui/                       custom widgets: PixelText, CodeView, ConfettiCanvas, NeonBackdrop
src/main/resources/
├── fxml/                         VIEWS (declarative layouts)
├── css/neon.css                  theme
├── audio/                        main_theme, audit_theme, correct_ping, wrong_buzz, hover, press,
│                                 cinematicerror, cinematicceleb, tick, type, lifeline, reboot
└── data/questions.json
```

### Where each paradigm shows up in the code

- **Object-oriented:** `Lifeline` is an abstract class and `PeekLifeline`, `CopyLifeline` and `SaveLifeline` override `activate()`. The game screen calls them polymorphically. Custom widgets extend JavaFX classes (`NeonBackdrop extends Pane`), and the data stays encapsulated in `GameSession`.
- **Functional:** Each ladder rung defines its question pool as a `Predicate<Question>`, and pools are combined with `.or(...)`. `QuestionBank` filters them with streams and lambdas.
- **Declarative:** Screens are laid out in FXML and styled with CSS. They describe *what* the UI looks like, not *how* to draw it.
- **Event-driven:** Mouse and keyboard handlers, `AnimationTimer` callbacks, `Timeline` key frames and the countdown all fire events that the controllers react to.
- **Imperative / procedural:** The `AnimationTimer` game loops use a `delta` time accumulator with `System.nanoTime()` and `lastTime`. The camera loop and the sound synthesis are written step by step.

## Sound assets

All audio was synthesised for this project (retro chiptune style), so there are no licensing issues. To replace a sound, drop in a WAV file (PCM 16-bit) with the same name under `src/main/resources/audio/`.

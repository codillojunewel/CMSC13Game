package com.cmsc13.frontend;

import com.cmsc13.audio.SoundManager;
import com.cmsc13.audio.SoundManager.Sound;
import com.cmsc13.frontend.SceneManager.Screen;
import com.cmsc13.frontend.ui.CodeView;
import com.cmsc13.frontend.ui.PixelText;
import com.cmsc13.model.GameSession;
import com.cmsc13.model.GameSession.Outcome;
import com.cmsc13.model.LadderRung;
import com.cmsc13.model.Persona;
import com.cmsc13.model.PlayerStats;
import com.cmsc13.model.Question;
import com.cmsc13.model.Settings;
import com.cmsc13.model.SurvivalLadder;
import com.cmsc13.model.lifeline.Lifeline;
import com.cmsc13.model.lifeline.LifelineResult;

import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The main game window, laid over the studio artwork from the GUI mockup.
 * <p>
 * The question and options appear on the big monitor, the active proctor's
 * desk lights up, the ALGORITHM FLOW panel tracks the answer phase
 * (START → INPUT → PROCESS → OUTPUT → END), and the empty right-hand monitor
 * becomes the proctor cam with the countdown.
 * <p>
 * This controller is event-driven: clicks, keys, timers and animation callbacks
 * are forwarded to the {@link GameSession} model, and the view is refreshed from it.
 */
public class GameController implements ScreenController {

    // ---- geometry of the studio artwork (1280 x 720) ----
    private static final double SCREEN_W = 463;
    private static final double SCREEN_H = 249;
    private static final double PILL_TEXT_W = SCREEN_W - 14 - 92; // left padding + commit badge reserve
    private static final double[] FLOW_Y = {137, 175, 215, 255, 292};
    private static final Map<Persona, double[]> DESKS = new EnumMap<>(Persona.class);

    static {
        // plate x, y, w, h, marker x, marker y
        DESKS.put(Persona.HALIMA, new double[]{478, 416, 84, 40, 535, 284});
        DESKS.put(Persona.ISAK, new double[]{648, 420, 72, 37, 690, 292});
        DESKS.put(Persona.AYA, new double[]{788, 428, 94, 44, 848, 294});
        DESKS.put(Persona.HALEY, new double[]{948, 493, 94, 44, 1012, 322});
        DESKS.put(Persona.CAMERON, new double[]{513, 573, 114, 44, 606, 402});
        DESKS.put(Persona.PRIYA, new double[]{710, 583, 100, 44, 760, 520});
    }

    private enum Flow { START, INPUT, PROCESS, OUTPUT, END }

    @FXML private StackPane root;
    @FXML private AnchorPane board;
    @FXML private Pane flowLayer;
    @FXML private Pane deskLayer;
    @FXML private VBox screenBox;
    @FXML private Label questionLabel;
    @FXML private ScrollPane optionsScroll;
    @FXML private VBox optionsBox;
    @FXML private VBox resultBox;
    @FXML private PixelText resultTitle;
    @FXML private Label resultAnswer;
    @FXML private Label resultWhy;
    @FXML private Button continueButton;
    @FXML private VBox codeWindow;
    @FXML private Label codeTitle;
    @FXML private ScrollPane codeScroll;
    @FXML private VBox expectedBox;
    @FXML private Label expectedLabel;
    @FXML private ImageView proctorFace;
    @FXML private Label proctorName;
    @FXML private Label proctorParadigm;
    @FXML private Label speechLabel;
    @FXML private HBox timerRow;
    @FXML private Label timerLabel;
    @FXML private ProgressBar timerBar;
    @FXML private VBox lifelineBox;
    @FXML private HBox ladderBox;
    @FXML private Label employeeLabel;
    @FXML private Region flash;
    @FXML private StackPane overlay;

    private GameSession session;
    private final List<Button> optionButtons = new ArrayList<>();
    private final List<Button> commitBadges = new ArrayList<>();
    private final List<Label> ladderChips = new ArrayList<>();
    private final List<Rectangle> flowLights = new ArrayList<>();
    private final Map<Lifeline, Button> lifelineButtons = new LinkedHashMap<>();
    private final EventHandler<KeyEvent> keyHandler = this::onKey;

    private int selected = -1;
    private boolean locked;
    private Outcome outcome;
    private Animation deskPulse;
    private Animation markerBounce;

    // ---- countdown (delta-time accumulator) ----
    private double timeLimit;
    private double remaining;
    private long lastTime;
    private boolean timerRunning;
    private int lastTickSecond = -1;

    private final AnimationTimer countdown = new AnimationTimer() {
        @Override
        public void handle(long now) {
            if (lastTime == 0) {
                lastTime = now;
                return;
            }
            double delta = (now - lastTime) / 1_000_000_000.0;
            lastTime = now;
            if (!timerRunning) {
                return;
            }
            remaining -= delta;
            renderTimer();
            if (remaining <= 0) {
                timerRunning = false;
                onTimeout();
            }
        }
    };

    // ================================================================ setup

    @FXML
    private void initialize() {
        flash.prefWidthProperty().bind(root.widthProperty());
        flash.prefHeightProperty().bind(root.heightProperty());
        buildFlowLights();
        buildLadder();
        codeScroll.setFitToWidth(false);
    }

    private void buildFlowLights() {
        for (double cy : FLOW_Y) {
            Rectangle r = new Rectangle(231, cy - 11, 50, 22);
            r.setArcWidth(12);
            r.setArcHeight(12);
            r.setFill(Color.rgb(90, 255, 255, 0.22));
            r.setStroke(Color.web("#7ffcff"));
            r.setStrokeWidth(1.6);
            r.setEffect(new DropShadow(14, Color.web("#3ff3ff")));
            r.setOpacity(0);
            flowLights.add(r);
            flowLayer.getChildren().add(r);
        }
    }

    private void setFlow(Flow step) {
        for (int i = 0; i < flowLights.size(); i++) {
            Rectangle r = flowLights.get(i);
            boolean on = i == step.ordinal();
            FadeTransition f = new FadeTransition(Duration.millis(180), r);
            f.setToValue(on ? 1 : (i < step.ordinal() ? 0.25 : 0));
            f.play();
        }
    }

    private void buildLadder() {
        PixelText label = new PixelText("LEVEL", 2, Color.web("#7fd8ff"));
        label.setFontSize(9);
        ladderBox.getChildren().add(label);
        for (LadderRung rung : SurvivalLadder.RUNGS) {
            Label chip = new Label(String.valueOf(rung.number()));
            chip.getStyleClass().add("ladder-chip");
            Tooltip.install(chip, new Tooltip("Level " + rung.number() + ": " + rung.tierName()
                    + " · Proctor: " + rung.proctor().getDisplayName()));
            ladderChips.add(chip);
            ladderBox.getChildren().add(chip);
        }
    }

    private void refreshLadder(boolean failed) {
        int cleared = session.getClearedCount();
        for (int i = 0; i < ladderChips.size(); i++) {
            Label chip = ladderChips.get(i);
            chip.getStyleClass().removeAll("cleared", "current", "failed");
            if (failed && i == session.getRungIndex()) {
                chip.getStyleClass().add("failed");
            } else if (i < cleared) {
                chip.getStyleClass().add("cleared");
            } else if (i == session.getRungIndex()) {
                chip.getStyleClass().add("current");
            }
        }
    }

    private void buildLifelines() {
        lifelineBox.getChildren().clear();
        lifelineButtons.clear();
        for (Lifeline l : session.getLifelines()) {
            PixelText name = new PixelText(l.getName(), 2, l.isManual() ? Color.web("#e2b8ff") : Color.web("#7dff9a"));
            name.setFontSize(10);
            Label cmd = new Label(l.getCommand());
            cmd.getStyleClass().add("lifeline-cmd");
            Label status = new Label();
            status.getStyleClass().add("lifeline-status");
            VBox content = new VBox(2, name, cmd, status);
            Button b = new Button();
            b.setGraphic(content);
            b.getStyleClass().add("lifeline-btn");
            if (!l.isManual()) {
                b.getStyleClass().add("passive");
            }
            b.setPrefWidth(118);
            b.setTooltip(new Tooltip(l.getDescription()));
            b.setUserData(status);
            b.setOnAction(e -> useLifeline(l));
            lifelineButtons.put(l, b);
            lifelineBox.getChildren().add(b);
        }
    }

    @Override
    public void onShow() {
        session = AppState.getSession();
        if (session == null) {
            session = new GameSession(Settings.get().getLastPlayerName(), 1);
            AppState.setSession(session);
        }
        employeeLabel.setText("EMPLOYEE: " + session.getPlayerName().toUpperCase() + "  ·  ATTEMPT #" + session.getAttemptNumber());
        buildLifelines();
        SoundManager.get().playMusic(Sound.AUDIT_THEME);
        root.getScene().addEventFilter(KeyEvent.KEY_PRESSED, keyHandler);
        countdown.start();
        loadNextQuestion();
    }

    @Override
    public void onHide() {
        if (root.getScene() != null) {
            root.getScene().removeEventFilter(KeyEvent.KEY_PRESSED, keyHandler);
        }
        countdown.stop();
        timerRunning = false;
        if (deskPulse != null) {
            deskPulse.stop();
        }
        if (markerBounce != null) {
            markerBounce.stop();
        }
    }

    // ================================================================ question flow

    private void loadNextQuestion() {
        Question q = session.nextQuestion();
        LadderRung rung = session.getRung();
        Persona proctor = rung.proctor();
        selected = -1;
        locked = false;
        outcome = null;

        resultBox.setVisible(false);
        screenBox.setVisible(true);

        boolean snippetMode = layoutScreen(q);
        layoutCodeWindow(q, snippetMode);

        // Proctor cam on the right monitor
        proctorFace.setImage(new Image(getClass().getResourceAsStream(proctor.getPortrait())));
        Rectangle clip = new Rectangle(50, 50);
        clip.setArcWidth(10);
        clip.setArcHeight(10);
        proctorFace.setClip(clip);
        proctorName.setText(proctor.getDeskName());
        proctorName.setStyle("-fx-text-fill: " + proctor.getColor() + ";");
        proctorParadigm.setText(proctor.getParadigm());
        say(session.isRepeatQuestion()
                ? "My cache says this exact question crashed you last time. Remember the memory dump?"
                : "Level " + rung.number() + ": " + rung.tierName() + ". " + proctor.randomIntro());

        highlightDesk(proctor);
        refreshLadder(false);
        refreshLifelines();
        setFlow(Flow.START);

        // power-on animation of the monitor
        screenBox.setOpacity(0);
        screenBox.setScaleY(0.92);
        Timeline in = new Timeline(new KeyFrame(Duration.millis(300),
                new KeyValue(screenBox.opacityProperty(), 1),
                new KeyValue(screenBox.scaleYProperty(), 1, Interpolator.EASE_OUT)));
        in.play();

        startTimer(rung);
    }

    /**
     * Lays the question and options out on the big monitor, shrinking fonts until
     * everything fits. Returns true for "snippet mode": code answers too long for
     * the monitor are listed in the code window instead.
     */
    private boolean layoutScreen(Question q) {
        // Question text: 19px bold down to 12px, at most ~4 lines.
        double qSize = 19;
        Font qFont = Font.font("Segoe UI", FontWeight.BOLD, qSize);
        while (qSize > 12 && textHeight(q.getPrompt(), qFont, SCREEN_W - 4) > 84) {
            qSize -= 1;
            qFont = Font.font("Segoe UI", FontWeight.BOLD, qSize);
        }
        questionLabel.setText(q.getPrompt() + (session.isRepeatQuestion() ? "  ⟲" : ""));
        questionLabel.setStyle(css(qFont));
        double available = SCREEN_H - textHeight(q.getPrompt(), qFont, SCREEN_W - 4) - 8;

        boolean code = q.choicesAreCode();
        String family = code ? "Consolas" : "Segoe UI";
        double oSize = 16;
        double minSize = code ? 11 : 10;
        boolean fits = false;
        while (oSize >= minSize) {
            Font f = Font.font(family, FontWeight.BOLD, oSize);
            double total = 3 * 6;
            for (int i = 0; i < 4; i++) {
                total += Math.max(30, textHeight(label(i, q), f, PILL_TEXT_W) + 12);
            }
            if (total <= available) {
                fits = true;
                break;
            }
            oSize -= 1;
        }
        boolean snippetMode = code && !fits;
        if (!fits) {
            oSize = snippetMode ? 15 : 10;
        }
        Font optionFont = Font.font(family, FontWeight.BOLD, oSize);

        optionsBox.getChildren().clear();
        optionButtons.clear();
        commitBadges.clear();
        for (int i = 0; i < 4; i++) {
            String text = snippetMode
                    ? Question.letter(i) + ")  Snippet " + Question.letter(i) + "   (see the code panel)"
                    : label(i, q);
            Button opt = new Button(text);
            opt.setStyle(css(optionFont));
            opt.setWrapText(true);
            opt.getStyleClass().add("opt-pill");
            opt.setMaxWidth(Double.MAX_VALUE);
            opt.setMinHeight(30);
            opt.setMaxHeight(Region.USE_PREF_SIZE);
            opt.setAlignment(Pos.CENTER_LEFT);
            final int index = i;
            opt.setOnAction(e -> {
                if (selected == index) {
                    commit();
                } else {
                    select(index);
                }
            });

            Button go = new Button("COMMIT ⏎");
            go.getStyleClass().add("commit-badge");
            go.setVisible(false);
            go.setOnAction(e -> commit());
            StackPane.setAlignment(go, Pos.CENTER_RIGHT);
            StackPane.setMargin(go, new Insets(0, 7, 0, 0));

            StackPane cell = new StackPane(opt, go);
            optionButtons.add(opt);
            commitBadges.add(go);
            optionsBox.getChildren().add(cell);
        }
        optionsScroll.setVvalue(0);
        return snippetMode;
    }

    /** Inline CSS for a font (author stylesheets would otherwise override setFont). */
    private static String css(Font f) {
        boolean bold = f.getStyle().toLowerCase().contains("bold");
        return String.format(java.util.Locale.ROOT, "-fx-font-family: '%s'; -fx-font-size: %.1fpx; -fx-font-weight: %s;",
                f.getFamily(), f.getSize(), bold ? "bold" : "normal");
    }

    private static String label(int i, Question q) {
        return Question.letter(i) + ") " + q.getChoices().get(i);
    }

    private static double textHeight(String s, Font f, double wrap) {
        Text t = new Text(s);
        t.setFont(f);
        t.setWrappingWidth(wrap);
        return t.getLayoutBounds().getHeight();
    }

    private void layoutCodeWindow(Question q, boolean snippetMode) {
        boolean show = q.hasCode() || q.hasExpectedOutput() || snippetMode;
        codeWindow.setVisible(show);
        if (!show) {
            return;
        }
        VBox content = new VBox(6);
        int lines = 0;
        if (q.hasCode()) {
            content.getChildren().add(new CodeView(q.getCode()));
            lines += q.getCode().split("\n").length;
        }
        if (snippetMode) {
            for (int i = 0; i < 4; i++) {
                Label head = new Label("── SNIPPET " + Question.letter(i) + " ──");
                head.getStyleClass().add("snippet-head");
                content.getChildren().addAll(head, new CodeView(q.getChoices().get(i)));
                lines += q.getChoices().get(i).split("\n").length + 2;
            }
        }
        boolean hasCodeArea = !content.getChildren().isEmpty();
        codeScroll.setVisible(hasCodeArea);
        codeScroll.setManaged(hasCodeArea);
        codeScroll.setContent(content);
        codeScroll.setPrefHeight(Math.min(360, lines * 16.2 + 18));
        codeScroll.setVvalue(0);
        codeScroll.setHvalue(0);

        expectedBox.setVisible(q.hasExpectedOutput());
        expectedBox.setManaged(q.hasExpectedOutput());
        expectedLabel.setText(q.hasExpectedOutput() ? q.getExpectedOutput() : "");

        String lang = q.getLanguage() == null ? "code" : q.getLanguage();
        codeTitle.setText("</> " + lang.toUpperCase() + "  ·  " + q.getType().toUpperCase());

        codeWindow.setOpacity(0);
        codeWindow.setTranslateX(-30);
        Timeline in = new Timeline(new KeyFrame(Duration.millis(320),
                new KeyValue(codeWindow.opacityProperty(), 1),
                new KeyValue(codeWindow.translateXProperty(), 0, Interpolator.EASE_OUT)));
        in.play();
    }

    private void highlightDesk(Persona p) {
        if (deskPulse != null) {
            deskPulse.stop();
        }
        if (markerBounce != null) {
            markerBounce.stop();
        }
        double[] d = DESKS.get(p);
        Color c = Color.web(p.getColor());
        Rectangle plate = new Rectangle(d[0] - 4, d[1] - 4, d[2] + 8, d[3] + 8);
        plate.setArcWidth(10);
        plate.setArcHeight(10);
        plate.setFill(c.deriveColor(0, 1, 1, 0.10));
        plate.setStroke(c);
        plate.setStrokeWidth(3);
        plate.setEffect(new DropShadow(22, c));

        // Marker above the proctor's head, or under the nameplate when the head sits below the big monitor.
        boolean hiddenByScreen = d[4] > 380 && d[4] < 940 && d[5] - 36 < 305;
        double cx = hiddenByScreen ? d[0] + d[2] / 2 : d[4];
        Polygon marker = hiddenByScreen
                ? new Polygon(0, 14, 22, 14, 11, 0)
                : new Polygon(0, 0, 22, 0, 11, 14);
        marker.setFill(c);
        marker.setEffect(new DropShadow(12, c));
        marker.setLayoutX(cx - 11);
        marker.setLayoutY(hiddenByScreen ? d[1] + d[3] + 8 : d[5] - 16);
        Label tag = new Label("PROCTOR");
        tag.getStyleClass().add("proctor-tag");
        tag.setStyle("-fx-text-fill: " + p.getColor() + ";");
        tag.setLayoutX(cx - 30);
        tag.setLayoutY(hiddenByScreen ? d[1] + d[3] + 24 : d[5] - 34);
        tag.setPrefWidth(60);
        tag.setAlignment(Pos.CENTER);

        deskLayer.getChildren().setAll(plate, marker, tag);

        FadeTransition pulse = new FadeTransition(Duration.millis(700), plate);
        pulse.setFromValue(1);
        pulse.setToValue(0.45);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.play();
        deskPulse = pulse;

        TranslateTransition bounce = new TranslateTransition(Duration.millis(450), marker);
        bounce.setFromY(0);
        bounce.setToY(6);
        bounce.setAutoReverse(true);
        bounce.setCycleCount(Animation.INDEFINITE);
        bounce.play();
        markerBounce = bounce;
    }

    private void select(int index) {
        if (locked) {
            return;
        }
        selected = index;
        for (int i = 0; i < optionButtons.size(); i++) {
            boolean on = i == index;
            optionButtons.get(i).getStyleClass().remove("selected");
            if (on) {
                optionButtons.get(i).getStyleClass().add("selected");
            }
            commitBadges.get(i).setVisible(on);
        }
        setFlow(Flow.INPUT);
    }

    private void commit() {
        if (locked || selected < 0 || !session.isAwaitingAnswer()) {
            return;
        }
        lockAndCompile(selected);
    }

    private void onTimeout() {
        if (locked || !session.isAwaitingAnswer()) {
            return;
        }
        say("TimeoutException: the audit clock ran out!");
        lockAndCompile(-1);
    }

    private void lockAndCompile(int choice) {
        locked = true;
        timerRunning = false;
        optionButtons.forEach(b -> b.setDisable(true));
        commitBadges.forEach(b -> b.setVisible(false));
        lifelineButtons.values().forEach(b -> b.setDisable(true));
        setFlow(Flow.PROCESS);
        if (choice >= 0) {
            say("Compiling answer " + Question.letter(choice) + "...");
        }
        PauseTransition suspense = new PauseTransition(Duration.millis(choice >= 0 ? 1100 : 500));
        suspense.setOnFinished(e -> resolve(session.commit(choice)));
        suspense.play();
    }

    private void resolve(Outcome result) {
        outcome = result;
        Question q = session.getCurrent();
        Persona proctor = session.getProctor();
        int correct = q.getCorrectIndex();
        setFlow(Flow.OUTPUT);

        optionButtons.get(correct).getStyleClass().add("correct");
        if (session.getLastSelected() >= 0 && session.getLastSelected() != correct) {
            optionButtons.get(session.getLastSelected()).getStyleClass().add("wrong");
        }
        PlayerStats.get().recordProgress(session.getClearedCount());

        switch (result) {
            case CORRECT -> {
                SoundManager.get().play(Sound.CORRECT);
                say(proctor.randomCorrect());
                refreshLadder(false);
                pulse(ladderChips.get(session.getRungIndex()));
            }
            case SAVED -> {
                SoundManager.get().play(Sound.LIFELINE);
                say("catch (Crash e) { /* I had the right answer. You owe me one. */ }");
                refreshLadder(false);
                refreshLifelines();
            }
            default -> {
                SoundManager.get().play(Sound.WRONG);
                say(proctor.randomWrong() + " Memory dump on the main screen.");
                refreshLadder(true);
                crashEffect();
            }
        }
        after(1300, () -> showResult(result));
    }

    /** Replaces the options on the big monitor with the result (and the memory dump on a crash). */
    private void showResult(Outcome result) {
        Question q = session.getCurrent();
        boolean passed = result == Outcome.CORRECT || result == Outcome.SAVED;
        switch (result) {
            case CORRECT -> {
                resultTitle.setText("CORRECT!");
                resultTitle.setColor(Color.web("#7dff9a"));
                resultTitle.setBottomColor(Color.web("#2fd36a"));
                resultTitle.setGlow(Color.web("#2fff7a"));
            }
            case SAVED -> {
                resultTitle.setText("EXCEPTION CAUGHT");
                resultTitle.setColor(Color.web("#ffe27a"));
                resultTitle.setBottomColor(Color.web("#ffb000"));
                resultTitle.setGlow(Color.web("#ffb000"));
            }
            case TIMEOUT -> {
                resultTitle.setText("TIMEOUT: CORE DUMP");
                resultTitle.setColor(Color.web("#ff8a96"));
                resultTitle.setBottomColor(Color.web("#ff2a3c"));
                resultTitle.setGlow(Color.web("#ff2a3c"));
            }
            default -> {
                resultTitle.setText("FATAL: CORE DUMP");
                resultTitle.setColor(Color.web("#ff8a96"));
                resultTitle.setBottomColor(Color.web("#ff2a3c"));
                resultTitle.setGlow(Color.web("#ff2a3c"));
            }
        }
        int sel = session.getLastSelected();
        StringBuilder answer = new StringBuilder();
        if (!passed || result == Outcome.SAVED) {
            answer.append(sel >= 0
                    ? "✗ Your answer: " + Question.letter(sel) + ") " + oneLine(q.getChoices().get(sel))
                    : "✗ Your answer: (none, time ran out)").append('\n');
        }
        answer.append("✓ Correct: ").append(Question.letter(q.getCorrectIndex())).append(") ")
                .append(oneLine(q.getCorrectChoice()));
        resultAnswer.setText(answer.toString());
        Font answerFont = Font.font("Segoe UI", FontWeight.BOLD, 14);
        resultAnswer.setStyle(css(answerFont));

        String why = q.getExplanation();
        double size = 14;
        double room = SCREEN_H - 34 - textHeight(answer.toString(), answerFont, SCREEN_W) - 54;
        while (size > 10 && textHeight(why, Font.font("Segoe UI", size), SCREEN_W) > room) {
            size -= 0.5;
        }
        resultWhy.setText(why);
        resultWhy.setStyle(css(Font.font("Segoe UI", size)));

        ((PixelText) continueButton.getGraphic()).setText(passed ? "CONTINUE >" : "FACE HR >");
        ((PixelText) continueButton.getGraphic()).setColor(Color.web(passed ? "#43ff6b" : "#ff5c6c"));
        continueButton.getStyleClass().removeAll("neon-btn-green", "neon-btn-red");
        continueButton.getStyleClass().add(passed ? "neon-btn-green" : "neon-btn-red");

        screenBox.setVisible(false);
        resultBox.setVisible(true);
        resultBox.setOpacity(0);
        FadeTransition f = new FadeTransition(Duration.millis(250), resultBox);
        f.setToValue(1);
        f.play();
        continueButton.requestFocus();
    }

    private static String oneLine(String s) {
        String flat = s.replace('\n', ' ').replaceAll("\\s+", " ").trim();
        return flat.length() > 120 ? flat.substring(0, 117) + "..." : flat;
    }

    @FXML
    private void continueAfterResult() {
        if (outcome == null) {
            return;
        }
        if (outcome == Outcome.CORRECT || outcome == Outcome.SAVED) {
            setFlow(Flow.END);
            SceneManager.show(session.isComplete() ? Screen.VICTORY : Screen.LEVEL_COMPLETE);
        } else {
            SceneManager.show(Screen.DEFEAT);
        }
    }

    // ================================================================ lifelines

    private void useLifeline(Lifeline lifeline) {
        if (locked || !lifeline.canActivate(session)) {
            if (!lifeline.isManual() && !lifeline.isUsed()) {
                say("SAVE is armed. It fires on its own if you crash and I had the right answer.");
            }
            return;
        }
        LifelineResult result = lifeline.activate(session);
        SoundManager.get().play(Sound.LIFELINE);
        say(result.message());
        refreshLifelines();
        if (result.revealedIndex() >= 0) {
            Button b = optionButtons.get(result.revealedIndex());
            b.getStyleClass().add("peeked");
            pulse(b);
            if (result.autoCommit()) {
                select(result.revealedIndex());
                after(700, () -> lockAndCompile(result.revealedIndex()));
            }
        }
    }

    private void refreshLifelines() {
        lifelineButtons.forEach((l, b) -> {
            Label status = (Label) b.getUserData();
            status.setText(l.statusText());
            b.setDisable(l.isUsed() || locked);
        });
    }

    // ================================================================ abort overlay

    @FXML
    private void abort() {
        if (locked && outcome == null) {
            return; // an answer is compiling
        }
        boolean wasRunning = timerRunning;
        timerRunning = false;

        PixelText title = new PixelText("ABORT AUDIT?", 3, Color.web("#ff8a96"));
        title.setFontSize(11);
        title.setGlow(Color.web("#ff2a3c"));
        Label msg = new Label("HR will log this as a voluntary resignation. Your progress on this run will be lost.");
        msg.setWrapText(true);
        msg.getStyleClass().add("memo-text");
        Button stay = neonButton("KEEP GOING", "green");
        stay.setOnAction(e -> {
            overlay.setVisible(false);
            if (wasRunning && !locked) {
                timerRunning = true;
            }
        });
        Button quit = neonButton("RESIGN", "red");
        quit.setOnAction(e -> {
            AppState.setSession(null);
            SceneManager.show(Screen.MAIN_MENU);
        });
        HBox buttons = new HBox(18, stay, quit);
        buttons.setAlignment(Pos.CENTER);
        VBox card = new VBox(18, title, msg, buttons);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("mock-panel");
        card.setPadding(new Insets(26, 34, 26, 34));
        card.setMaxSize(560, Region.USE_PREF_SIZE);
        overlay.getChildren().setAll(card);
        overlay.setVisible(true);
        stay.requestFocus();
    }

    static Button neonButton(String text, String color) {
        String hex = switch (color) {
            case "green" -> "#43ff6b";
            case "red" -> "#ff5c6c";
            default -> "#55b8ff";
        };
        PixelText label = new PixelText(text, 3, Color.web(hex));
        label.setBold(true);
        Button b = new Button();
        b.setGraphic(label);
        b.getStyleClass().addAll("neon-btn", "neon-btn-" + color);
        return b;
    }

    // ================================================================ timer

    private void startTimer(LadderRung rung) {
        double multiplier = Settings.get().getTimerMode().getMultiplier();
        if (multiplier <= 0) {
            timerRow.setVisible(false);
            timerBar.setVisible(false);
            timerRunning = false;
            return;
        }
        timerRow.setVisible(true);
        timerBar.setVisible(true);
        timeLimit = Math.round(rung.baseSeconds() * multiplier);
        remaining = timeLimit;
        lastTickSecond = -1;
        lastTime = 0;
        timerRunning = true;
        renderTimer();
    }

    private void renderTimer() {
        double r = Math.max(0, remaining);
        int secs = (int) Math.ceil(r);
        timerLabel.setText(String.format("%02d:%02d", secs / 60, secs % 60));
        timerBar.setProgress(timeLimit <= 0 ? 0 : r / timeLimit);
        boolean danger = r <= 10;
        toggleClass(timerLabel, "danger", danger);
        toggleClass(timerBar, "danger", danger);
        if (danger && secs != lastTickSecond && secs > 0 && timerRunning) {
            lastTickSecond = secs;
            SoundManager.get().play(Sound.TICK);
        }
    }

    // ================================================================ input

    private void onKey(KeyEvent e) {
        if (overlay.isVisible()) {
            return;
        }
        if (resultBox.isVisible()) {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) {
                continueAfterResult();
                e.consume();
            }
            return;
        }
        int idx = switch (e.getCode()) {
            case A, DIGIT1, NUMPAD1 -> 0;
            case B, DIGIT2, NUMPAD2 -> 1;
            case C, DIGIT3, NUMPAD3 -> 2;
            case D, DIGIT4, NUMPAD4 -> 3;
            default -> -1;
        };
        if (idx >= 0) {
            select(idx);
            e.consume();
        } else if (e.getCode() == KeyCode.ENTER) {
            commit();
            e.consume();
        }
    }

    // ================================================================ tour hooks (package-private)

    void tourSelect(int index) {
        select(index);
    }

    void tourCommit() {
        commit();
    }

    void tourLifeline(int index) {
        useLifeline(session.getLifelines().get(index));
    }

    void tourContinue() {
        continueAfterResult();
    }

    // ================================================================ helpers

    private void say(String text) {
        speechLabel.setText(text);
        speechLabel.setOpacity(0);
        FadeTransition f = new FadeTransition(Duration.millis(220), speechLabel);
        f.setToValue(1);
        f.play();
    }

    private void crashEffect() {
        Timeline shake = new Timeline();
        double[] offsets = {-14, 12, -10, 8, -6, 4, -2, 0};
        for (int i = 0; i < offsets.length; i++) {
            shake.getKeyFrames().add(new KeyFrame(Duration.millis(45 * (i + 1)),
                    new KeyValue(board.translateXProperty(), offsets[i])));
        }
        shake.play();
        flash.setOpacity(0.45);
        FadeTransition f = new FadeTransition(Duration.millis(700), flash);
        f.setToValue(0);
        f.play();
    }

    private static void pulse(Node node) {
        ScaleTransition s = new ScaleTransition(Duration.millis(160), node);
        s.setToX(1.08);
        s.setToY(1.08);
        s.setAutoReverse(true);
        s.setCycleCount(4);
        s.play();
    }

    private static void toggleClass(Node node, String cls, boolean on) {
        if (on && !node.getStyleClass().contains(cls)) {
            node.getStyleClass().add(cls);
        } else if (!on) {
            node.getStyleClass().remove(cls);
        }
    }

    private static void after(double millis, Runnable action) {
        PauseTransition p = new PauseTransition(Duration.millis(millis));
        p.setOnFinished(e -> action.run());
        p.play();
    }
}

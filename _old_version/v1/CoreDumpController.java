package com.cmsc13.frontend;

import com.cmsc13.audio.SoundManager;
import com.cmsc13.audio.SoundManager.Sound;
import com.cmsc13.frontend.SceneManager.Screen;
import com.cmsc13.frontend.ui.CodeView;
import com.cmsc13.frontend.ui.JuniorDevAvatar;
import com.cmsc13.frontend.ui.JuniorDevAvatar.Mood;
import com.cmsc13.model.GameSession;
import com.cmsc13.model.GameSession.Outcome;
import com.cmsc13.model.Persona;
import com.cmsc13.model.Question;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.Random;

/**
 * "Fatal Core Dump" screen shown after a wrong answer: the enemy Junior Dev
 * dumps its memory, revealing the correct answer and why, before the player is
 * sent to the Public Humiliation Protocol.
 */
public class CoreDumpController implements ScreenController {

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label sourceLabel;
    @FXML private VBox dumpBox;
    @FXML private StackPane avatarSlot;
    @FXML private Label proctorName;
    @FXML private Label speechLabel;
    @FXML private Label warningLabel;
    @FXML private Button proceedButton;

    private final Random random = new Random();
    private Timeline glitch;
    private Timeline blink;

    @Override
    public void onShow() {
        SoundManager.get().stopMusic();
        SoundManager.get().play(Sound.DEFEAT);

        GameSession s = AppState.getSession();
        Question q = s.getCurrent();
        Persona proctor = s.getProctor();
        boolean timeout = s.getLastOutcome() == Outcome.TIMEOUT;

        subtitleLabel.setText(String.format("Process senior_architect.exe terminated at Level %d  ·  %s 0x%08X",
                s.getRung().number(), timeout ? "TimeoutException" : "SegFault", 0xC0DE0000 + q.getId()));
        sourceLabel.setText(q.getSourceLabel());

        Label prompt = new Label(q.getPrompt());
        prompt.setWrapText(true);
        prompt.getStyleClass().add("prompt-label");
        dumpBox.getChildren().add(prompt);
        if (q.hasCode()) {
            javafx.scene.control.ScrollPane code = new javafx.scene.control.ScrollPane(new CodeView(q.getCode()));
            code.getStyleClass().add("code-scroll");
            code.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
            code.setMinHeight(q.getCode().split("\n").length * 18.5 + 24);
            dumpBox.getChildren().add(code);
        }
        if (q.hasExpectedOutput()) {
            Label exp = new Label("Expected output: " + q.getExpectedOutput());
            exp.setStyle("-fx-text-fill: #39ff88; -fx-font-size: 13px;");
            exp.setWrapText(true);
            dumpBox.getChildren().add(exp);
        }

        int sel = s.getLastSelected();
        Label yours = new Label(sel >= 0
                ? "✗ YOUR ANSWER   " + Question.letter(sel) + ". " + q.getChoices().get(sel)
                : "✗ YOUR ANSWER   (none: the audit clock ran out)");
        yours.setWrapText(true);
        yours.setStyle("-fx-text-fill: #ff8a9e; -fx-font-size: 15px; -fx-font-weight: bold;");
        Label right = new Label("✓ CORRECT       " + Question.letter(q.getCorrectIndex()) + ". " + q.getCorrectChoice());
        right.setWrapText(true);
        right.setStyle("-fx-text-fill: #39ff88; -fx-font-size: 15px; -fx-font-weight: bold;");
        Label why = new Label("// " + q.getExplanation());
        why.setWrapText(true);
        why.getStyleClass().add("body-text");
        why.setStyle("-fx-text-fill: #c8d6f0;");
        dumpBox.getChildren().addAll(yours, right, why);

        JuniorDevAvatar avatar = new JuniorDevAvatar(proctor, 150);
        avatar.setMood(Mood.SMUG);
        avatarSlot.getChildren().setAll(avatar);
        proctorName.setText(proctor.getDisplayName());
        proctorName.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: " + proctor.getColor() + ";");
        speechLabel.setText(proctor.randomWrong() + "\n\nThe correct answer is now in your memory. "
                + "Use it, because this question will come back.");

        // Glitching title
        glitch = new Timeline(new KeyFrame(Duration.millis(90), e -> {
            boolean on = random.nextDouble() < 0.25;
            titleLabel.setTranslateX(on ? random.nextInt(14) - 7 : 0);
            titleLabel.setText(on ? scramble("FATAL CORE DUMP") : "FATAL CORE DUMP");
        }));
        glitch.setCycleCount(Animation.INDEFINITE);
        glitch.play();

        blink = new Timeline(new KeyFrame(Duration.millis(500), e -> warningLabel.setVisible(!warningLabel.isVisible())));
        blink.setCycleCount(Animation.INDEFINITE);
        blink.play();

        PauseTransition unlock = new PauseTransition(Duration.seconds(2.5));
        unlock.setOnFinished(e -> {
            proceedButton.setDisable(false);
            proceedButton.requestFocus();
        });
        unlock.play();
    }

    private String scramble(String text) {
        char[] glyphs = "#$%&@!?/\\|<>01".toCharArray();
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            sb.append(c != ' ' && random.nextDouble() < 0.2 ? glyphs[random.nextInt(glyphs.length)] : c);
        }
        return sb.toString();
    }

    @Override
    public void onHide() {
        if (glitch != null) {
            glitch.stop();
        }
        if (blink != null) {
            blink.stop();
        }
    }

    @FXML
    private void proceed() {
        SceneManager.show(Screen.CONFESSION);
    }
}

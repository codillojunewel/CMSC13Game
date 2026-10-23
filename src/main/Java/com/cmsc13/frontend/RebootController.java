package com.cmsc13.frontend;

import com.cmsc13.audio.SoundManager;
import com.cmsc13.audio.SoundManager.Sound;
import com.cmsc13.frontend.SceneManager.Screen;
import com.cmsc13.model.GameSession;
import com.cmsc13.model.PlayerStats;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.util.Duration;

import java.util.List;

/** Terminal-style forced reboot that wipes progress and restarts the audit from Level 1. */
public class RebootController implements ScreenController {

    @FXML private Label terminal;
    @FXML private Label progressLabel;
    @FXML private ProgressBar progressBar;
    @FXML private Button continueButton;
    @FXML private Button menuButton;

    private Timeline typing;
    private Timeline bar;

    @Override
    public void onShow() {
        GameSession s = AppState.getSession();
        int level = s != null ? s.getRung().number() : 1;
        int qid = s != null && s.getCurrent() != null ? s.getCurrent().getId() : 0;
        int attempt = s != null ? s.getAttemptNumber() + 1 : 1;

        List<String> lines = List.of(
                String.format("[HR]  Confession #%04d archived to the corporate database.", PlayerStats.get().getConfessions()),
                "[SYS] Disciplinary action complete. Initiating forced reboot...",
                "[SYS] $ sudo rm -rf /career/progress/*",
                String.format("[SYS] Progress wiped: clearance Level %d -> Level 1.", level),
                "[SYS] Lifelines restored: PEEK, COPY, SAVE.",
                String.format("[WARN] Memory leak detected: question #%d is still cached and WILL be asked again.", qid),
                "[TIP] Use the knowledge from the memory dump in your new survival loop.",
                String.format("[SYS] Rebooting competency audit (attempt #%d)...", attempt));

        String all = String.join("\n", lines);
        int[] i = {0};
        typing = new Timeline(new KeyFrame(Duration.millis(16), e -> {
            if (i[0] < all.length()) {
                i[0] = Math.min(all.length(), i[0] + 2);
                terminal.setText(all.substring(0, i[0]) + "█");
                if (i[0] % 10 == 0) {
                    SoundManager.get().play(Sound.TYPE);
                }
            }
        }));
        typing.setCycleCount(all.length() / 2 + 1);
        typing.setOnFinished(e -> {
            terminal.setText(all);
            runBar();
        });
        typing.play();
        terminal.setOnMouseClicked(e -> {
            typing.stop();
            terminal.setText(all);
            runBar();
        });
    }

    private void runBar() {
        if (bar != null) {
            return;
        }
        SoundManager.get().play(Sound.REBOOT);
        bar = new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(progressBar.progressProperty(), 0)),
                new KeyFrame(Duration.seconds(1.8), new KeyValue(progressBar.progressProperty(), 1)));
        bar.setOnFinished(e -> {
            progressLabel.setText("READY");
            continueButton.setVisible(true);
            menuButton.setVisible(true);
            continueButton.requestFocus();
        });
        bar.play();
    }

    @Override
    public void onHide() {
        if (typing != null) {
            typing.stop();
        }
        if (bar != null) {
            bar.stop();
        }
    }

    @FXML
    private void restart() {
        GameSession s = AppState.getSession();
        PlayerStats.get().recordAttemptStarted();
        AppState.setSession(s != null ? s.reboot() : new GameSession("Player", 1));
        SceneManager.show(Screen.GAME);
    }

    @FXML
    private void quit() {
        AppState.setSession(null);
        SceneManager.show(Screen.MAIN_MENU);
    }
}

package com.cmsc13.frontend;

import com.cmsc13.audio.SoundManager;
import com.cmsc13.audio.SoundManager.Sound;
import com.cmsc13.frontend.SceneManager.Screen;
import com.cmsc13.model.GameSession;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.util.Duration;

import java.util.Random;

/**
 * Defeat screen (mockup 2.3, error image). The artwork glitches, the SYSTEM
 * ERROR box shows the real error code, and the drawn OK button leads to the
 * Public Humiliation Protocol.
 */
public class DefeatController implements ScreenController {

    @FXML private ImageView art;
    @FXML private Pane glitchLayer;
    @FXML private Label codeLabel;
    @FXML private Region flicker;
    @FXML private Button okButton;
    @FXML private Label hintLabel;

    private final Random random = new Random();
    private Timeline glitch;
    private Timeline blink;

    @Override
    public void onShow() {
        SoundManager.get().stopMusic();
        SoundManager.get().play(Sound.DEFEAT);

        GameSession s = AppState.getSession();
        int qid = s != null && s.getCurrent() != null ? s.getCurrent().getId() : 1;
        int level = s != null ? s.getRung().number() : 1;
        codeLabel.setText(String.format("0x%08X", 0xC0DE0000 + level * 0x100 + qid));
        hintLabel.setText("Senior architect process crashed at Level " + level
                + ".   Press OK to face the HR Public Humiliation Protocol.");

        glitch = new Timeline(new KeyFrame(Duration.millis(70), e -> doGlitch()));
        glitch.setCycleCount(Animation.INDEFINITE);
        glitch.play();

        blink = new Timeline(new KeyFrame(Duration.millis(550), e -> hintLabel.setOpacity(hintLabel.getOpacity() > 0.5 ? 0.35 : 1)));
        blink.setCycleCount(Animation.INDEFINITE);
        blink.play();

        okButton.requestFocus();
        okButton.setDefaultButton(true);
    }

    /** Displaces a few horizontal strips of the artwork for a VHS-style glitch. */
    private void doGlitch() {
        glitchLayer.getChildren().clear();
        flicker.setOpacity(random.nextDouble() < 0.12 ? 0.08 + random.nextDouble() * 0.08 : 0);
        if (random.nextDouble() < 0.6) {
            return;
        }
        int strips = 1 + random.nextInt(4);
        double sx = art.getImage().getWidth() / 1280.0;
        double sy = art.getImage().getHeight() / 720.0;
        for (int i = 0; i < strips; i++) {
            double y = random.nextDouble() * 700;
            double h = 4 + random.nextDouble() * 26;
            ImageView slice = new ImageView(art.getImage());
            slice.setViewport(new Rectangle2D(0, y * sy, 1280 * sx, h * sy));
            slice.setFitWidth(1280);
            slice.setFitHeight(h);
            slice.setLayoutX(random.nextInt(40) - 20);
            slice.setLayoutY(y);
            glitchLayer.getChildren().add(slice);
        }
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

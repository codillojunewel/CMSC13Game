package com.cmsc13.frontend;

import com.cmsc13.Main;
import com.cmsc13.audio.SoundManager;
import com.cmsc13.audio.SoundManager.Sound;
import com.cmsc13.frontend.SceneManager.Screen;
import com.cmsc13.model.PlayerStats;
import com.cmsc13.model.Settings;
import com.cmsc13.model.Settings.TimerMode;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;

import java.nio.file.Files;

/** Settings: music/SFX volume, question timer, webcam mode, confessions folder, stats reset. */
public class SettingsController implements ScreenController {

    @FXML private Slider musicSlider;
    @FXML private Slider sfxSlider;
    @FXML private Label musicValue;
    @FXML private Label sfxValue;
    @FXML private HBox timerButtons;
    @FXML private Label timerHint;
    @FXML private ToggleButton webcamOn;
    @FXML private ToggleButton webcamOff;
    @FXML private Button resetButton;
    @FXML private com.cmsc13.frontend.ui.PixelText resetText;
    @FXML private Label statusLabel;

    private final Settings settings = Settings.get();
    private final ToggleGroup timerGroup = new ToggleGroup();
    private boolean resetArmed;

    @FXML
    private void initialize() {
        musicSlider.setValue(settings.getMusicVolume() * 100);
        sfxSlider.setValue(settings.getSfxVolume() * 100);
        musicValue.setText(pct(musicSlider.getValue()));
        sfxValue.setText(pct(sfxSlider.getValue()));

        // Live preview while dragging
        musicSlider.valueProperty().addListener((o, a, v) -> {
            settings.setMusicVolume(v.doubleValue() / 100);
            musicValue.setText(pct(v.doubleValue()));
            SoundManager.get().refreshMusicVolume();
        });
        sfxSlider.valueProperty().addListener((o, a, v) -> {
            settings.setSfxVolume(v.doubleValue() / 100);
            sfxValue.setText(pct(v.doubleValue()));
        });
        sfxSlider.setOnMouseReleased(e -> SoundManager.get().play(Sound.CORRECT));

        for (TimerMode mode : TimerMode.values()) {
            ToggleButton b = new ToggleButton(mode.getLabel());
            b.setToggleGroup(timerGroup);
            b.setUserData(mode);
            b.setSelected(mode == settings.getTimerMode());
            timerButtons.getChildren().add(b);
        }
        timerGroup.selectedToggleProperty().addListener((o, a, t) -> {
            if (t == null) {
                a.setSelected(true); // always keep one selected
                return;
            }
            settings.setTimerMode((TimerMode) t.getUserData());
            updateTimerHint();
        });
        updateTimerHint();

        ToggleGroup webcamGroup = new ToggleGroup();
        webcamOn.setToggleGroup(webcamGroup);
        webcamOff.setToggleGroup(webcamGroup);
        (settings.isWebcamEnabled() ? webcamOn : webcamOff).setSelected(true);
        webcamGroup.selectedToggleProperty().addListener((o, a, t) -> {
            if (t == null) {
                a.setSelected(true);
                return;
            }
            settings.setWebcamEnabled(t == webcamOn);
        });
    }

    private void updateTimerHint() {
        TimerMode m = settings.getTimerMode();
        timerHint.setText(m == TimerMode.OFF
                ? "No countdown. Take all the time you need."
                : String.format("Theory questions: %ds  ·  Code questions: %ds",
                Math.round(45 * m.getMultiplier()), Math.round(90 * m.getMultiplier())));
    }

    private static String pct(double v) {
        return Math.round(v) + "%";
    }

    @FXML
    private void openFolder() {
        try {
            Files.createDirectories(Settings.confessionsFolder());
            Main.hostServices().showDocument(Settings.confessionsFolder().toUri().toString());
            statusLabel.setText("Opened " + Settings.confessionsFolder());
        } catch (Exception e) {
            statusLabel.setText("Folder: " + Settings.confessionsFolder());
        }
    }

    @FXML
    private void resetStats() {
        if (!resetArmed) {
            resetArmed = true;
            resetText.setText("CLICK TO CONFIRM");
            return;
        }
        PlayerStats.get().reset();
        resetArmed = false;
        resetText.setText("RESET STATS");
        statusLabel.setText("Career stats wiped. Confession videos were kept in the folder.");
    }

    @FXML
    private void save() {
        settings.save();
        statusLabel.setText("Settings saved.");
    }

    @FXML
    private void back() {
        settings.save();
        SceneManager.show(Screen.MAIN_MENU);
    }
}

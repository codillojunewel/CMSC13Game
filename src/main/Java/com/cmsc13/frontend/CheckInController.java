package com.cmsc13.frontend;

import com.cmsc13.audio.SoundManager;
import com.cmsc13.audio.SoundManager.Sound;
import com.cmsc13.frontend.SceneManager.Screen;
import com.cmsc13.model.GameSession;
import com.cmsc13.model.Persona;
import com.cmsc13.model.PlayerStats;
import com.cmsc13.model.Settings;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * Studio start screen (mockup 2.2, second image). Clicking the drawn "Start"
 * button opens the employee check-in: the narrative memo, today's proctors and
 * the player's name (needed for the confession line).
 */
public class CheckInController implements ScreenController {

    private static final String MEMO =
            "MEMO FROM: AI Human Resources  ·  TO: Senior Systems Architect\n\n"
                    + "Your programming certifications are over a decade old. Pass a 10-question COMPETENCY AUDIT "
                    + "proctored by our AI Junior Devs to keep your job. There is no grand prize. The only reward is survival.\n"
                    + "A fatal wrong answer activates the Public Humiliation Protocol (webcam), and your progress is wiped.";

    @FXML private Button startButton;
    @FXML private StackPane overlay;
    @FXML private VBox dialog;
    @FXML private Label memoLabel;
    @FXML private HBox proctorRow;
    @FXML private TextField nameField;
    @FXML private Label errorLabel;

    private Timeline typing;

    @FXML
    private void initialize() {
        nameField.setText(Settings.get().getLastPlayerName());
        nameField.setTextFormatter(new TextFormatter<String>(c ->
                c.getControlNewText().length() <= 16 ? c : null));

        for (Persona p : Persona.values()) {
            ImageView face = new ImageView(new Image(getClass().getResourceAsStream(p.getPortrait())));
            face.setFitWidth(96);
            face.setFitHeight(96);
            Rectangle clip = new Rectangle(96, 96);
            clip.setArcWidth(16);
            clip.setArcHeight(16);
            face.setClip(clip);
            Label name = new Label(p.getDeskName());
            name.setStyle("-fx-text-fill: " + p.getColor() + ";");
            name.getStyleClass().add("plate-name");
            Label paradigm = new Label(p.getParadigm().replace(" Programming", ""));
            paradigm.getStyleClass().add("plate-sub");
            VBox card = new VBox(3, face, name, paradigm);
            card.setAlignment(Pos.TOP_CENTER);
            card.getStyleClass().add("plate-card");
            proctorRow.getChildren().add(card);
        }
    }

    @Override
    public void onShow() {
        SoundManager.get().playMusic(Sound.MAIN_THEME);
        startButton.requestFocus();
    }

    @Override
    public void onHide() {
        if (typing != null) {
            typing.stop();
        }
    }

    @FXML
    private void openCheckIn() {
        overlay.setVisible(true);
        overlay.setOpacity(0);
        FadeTransition f = new FadeTransition(Duration.millis(220), overlay);
        f.setToValue(1);
        f.play();
        dialog.setScaleX(0.85);
        dialog.setScaleY(0.85);
        ScaleTransition s = new ScaleTransition(Duration.millis(260), dialog);
        s.setToX(1);
        s.setToY(1);
        s.play();

        int[] i = {0};
        typing = new Timeline(new KeyFrame(Duration.millis(12), e -> {
            if (i[0] < MEMO.length()) {
                i[0] = Math.min(MEMO.length(), i[0] + 3);
                memoLabel.setText(MEMO.substring(0, i[0]) + (i[0] < MEMO.length() ? "█" : ""));
                if (i[0] % 15 == 0) {
                    SoundManager.get().play(Sound.TYPE);
                }
            }
        }));
        typing.setCycleCount(Animation.INDEFINITE);
        typing.play();
        memoLabel.setOnMouseClicked(e -> i[0] = MEMO.length() - 1);
        nameField.requestFocus();
        nameField.positionCaret(nameField.getText().length());
    }

    @FXML
    private void closeCheckIn() {
        if (typing != null) {
            typing.stop();
        }
        overlay.setVisible(false);
        startButton.requestFocus();
    }

    @FXML
    private void begin() {
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        if (name.isEmpty()) {
            errorLabel.setText("HR requires a name for the record.");
            nameField.requestFocus();
            return;
        }
        Settings.get().setLastPlayerName(name);
        Settings.get().save();
        PlayerStats.get().recordAttemptStarted();
        AppState.setSession(new GameSession(name, 1));
        SceneManager.show(Screen.GAME);
    }

    void tourOpen() {
        openCheckIn();
    }

    @FXML
    private void back() {
        SceneManager.show(Screen.MAIN_MENU);
    }
}

package com.cmsc13.frontend;

import com.cmsc13.audio.SoundManager;
import com.cmsc13.audio.SoundManager.Sound;
import com.cmsc13.frontend.SceneManager.Screen;
import com.cmsc13.frontend.ui.PixelText;
import com.cmsc13.model.GameSession;
import com.cmsc13.model.LadderRung;
import com.cmsc13.model.SurvivalLadder;

import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/** "LEVEL COMPLETE! You Did It!" (mockup 2.3) shown after every cleared level. */
public class LevelCompleteController implements ScreenController {

    @FXML private ImageView art;
    @FXML private Label caption;
    @FXML private Button nextButton;
    @FXML private StackPane overlay;

    @Override
    public void onShow() {
        SoundManager.get().stopMusic();
        SoundManager.get().play(Sound.VICTORY);

        GameSession s = AppState.getSession();
        LadderRung done = s.getRung();
        LadderRung next = SurvivalLadder.get(Math.min(SurvivalLadder.SIZE - 1, s.getRungIndex() + 1));
        caption.setText(String.format("LEVEL %d OF %d CLEARED   ▸   NEXT: LEVEL %d %s  ·  PROCTOR %s",
                done.number(), SurvivalLadder.SIZE, next.number(), next.tierName().toUpperCase(),
                next.proctor().getDeskName()));

        art.setScaleX(1.04);
        art.setScaleY(1.04);
        ScaleTransition zoom = new ScaleTransition(Duration.millis(600), art);
        zoom.setToX(1);
        zoom.setToY(1);
        zoom.play();
        nextButton.requestFocus();
    }

    @FXML
    private void next() {
        SceneManager.show(Screen.GAME);
    }

    @FXML
    private void back() {
        PixelText title = new PixelText("QUIT THE AUDIT?", 3, Color.web("#ffffff"));
        title.setFontSize(11);
        title.setGlow(Color.web("#2f8cff"));
        Label msg = new Label("Going back resigns from this audit. Your clearance on this run will be lost.");
        msg.setWrapText(true);
        msg.getStyleClass().add("memo-text");
        Button stay = GameController.neonButton("STAY", "green");
        stay.setOnAction(e -> overlay.setVisible(false));
        Button quit = GameController.neonButton("MAIN MENU", "blue");
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
}

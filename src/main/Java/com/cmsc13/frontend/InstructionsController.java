package com.cmsc13.frontend;

import com.cmsc13.frontend.SceneManager.Screen;
import com.cmsc13.frontend.ui.PixelText;
import com.cmsc13.model.LadderRung;
import com.cmsc13.model.Persona;
import com.cmsc13.model.Question;
import com.cmsc13.model.QuestionBank;
import com.cmsc13.model.SurvivalLadder;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/** How to Play: story, controls, ladder, lifelines and the failure protocol. */
public class InstructionsController implements ScreenController {

    @FXML private VBox content;
    @FXML private Button backButton;

    @FXML
    private void initialize() {
        QuestionBank bank = QuestionBank.getInstance();

        section("THE STORY",
                "You are a seasoned Senior Systems Architect. A ruthless AI Human Resources system has flagged your "
                        + "decade-old certifications for termination. To keep your job you must pass a 10-question competency "
                        + "audit proctored by the AI \"Junior Devs\" at the contestant desks. There is no grand prize: the only "
                        + "reward is survival.");

        section("CONTROLS (POINT AND CLICK)",
                "1. Read the question on the big monitor. Code and expected output appear in the code panel on the left.\n"
                        + "2. Click an answer (A-D) to highlight it. Keyboard: A-D or 1-4.\n"
                        + "3. Click COMMIT (or the highlighted answer again, or press Enter) to submit your code.\n"
                        + "4. Watch the countdown on the right-hand monitor. Running out of time is a crash.\n"
                        + "The ALGORITHM FLOW panel lights up as you go: START > INPUT > PROCESS > OUTPUT > END.\n"
                        + "F11 toggles full screen.");

        content.getChildren().add(title("THE SURVIVAL LADDER"));
        content.getChildren().add(body("Each correct answer raises your clearance by one level. Clear Level 10 to pass the "
                + "audit. Questions are drawn at random from a bank of " + bank.size() + " ("
                + bank.count(q -> !q.isProgramming()) + " theoretical, "
                + bank.count(Question::isProgramming) + " programming)."));
        GridPane grid = new GridPane();
        grid.setHgap(22);
        grid.setVgap(5);
        String[] headers = {"LVL", "TIER", "TOPIC", "PROCTOR"};
        for (int c = 0; c < headers.length; c++) {
            grid.add(cell(headers[c], "#8aa0c8", true), c, 0);
        }
        int r = 1;
        for (LadderRung rung : SurvivalLadder.RUNGS) {
            grid.add(cell(String.valueOf(rung.number()), "#ffd23f", true), 0, r);
            grid.add(cell("L" + rung.level() + " " + rung.tierName(), "#ffffff", false), 1, r);
            grid.add(cell(rung.topic(), "#a9c4ee", false), 2, r);
            grid.add(cell(rung.proctor().getDeskName(), rung.proctor().getColor(), true), 3, r);
            r++;
        }
        content.getChildren().add(grid);

        content.getChildren().add(title("LIFELINES (DEVELOPER EDITION)"));
        content.getChildren().addAll(
                lifeline("PEEK", "cat /junior/memory.log",
                        "Read the proctor's memory terminal to see the answer they locked in. You still choose."),
                lifeline("COPY", "Ctrl+C > Ctrl+V",
                        "Copy the proctor's answer and commit it instantly, right or wrong."),
                lifeline("SAVE", "try { } catch (Crash e)",
                        "Passive. The first time you crash, if the proctor had the right answer, you survive."),
                body("Each lifeline works once per run. The Junior Devs are right about 80% of the time."));

        section("CRASHING: THE PUBLIC HUMILIATION PROTOCOL",
                "Commit a wrong answer and the system crashes. The Junior Dev dumps its memory on the big monitor, "
                        + "revealing the correct answer and why. HR then turns on your webcam and refuses to continue until "
                        + "you record the line:\n\n   \"My name is [Your Name], and I am NOT smarter than a Junior Dev!\"\n\n"
                        + "The video is logged in the corporate database (JuniorDevAudit\\confessions in your user folder). "
                        + "The system then force-reboots and wipes your progress back to Level 1. The question that crashed "
                        + "you stays cached and will be asked again, so remember the memory dump!");

        content.getChildren().add(title("MEET THE JUNIOR DEVS"));
        HBox devs = new HBox(14);
        devs.setAlignment(Pos.CENTER_LEFT);
        for (Persona p : Persona.values()) {
            ImageView face = new ImageView(new Image(getClass().getResourceAsStream(p.getPortrait())));
            face.setFitWidth(112);
            face.setFitHeight(112);
            Rectangle clip = new Rectangle(112, 112);
            clip.setArcWidth(18);
            clip.setArcHeight(18);
            face.setClip(clip);
            Label name = new Label(p.getDeskName());
            name.getStyleClass().add("plate-name");
            name.setStyle("-fx-text-fill: " + p.getColor() + ";");
            Label para = new Label(p.getParadigm());
            para.getStyleClass().add("plate-sub");
            para.setWrapText(true);
            para.setMaxWidth(118);
            VBox card = new VBox(4, face, name, para);
            card.setAlignment(Pos.TOP_CENTER);
            card.getStyleClass().add("plate-card");
            card.setPrefWidth(126);
            devs.getChildren().add(card);
        }
        content.getChildren().add(devs);
    }

    @Override
    public void onShow() {
        backButton.requestFocus();
    }

    private void section(String heading, String text) {
        content.getChildren().addAll(title(heading), body(text));
    }

    private static PixelText title(String text) {
        PixelText t = new PixelText(text, 2, Color.web("#5fd0ff"));
        t.setFontSize(10);
        t.setGlow(Color.web("#1e7fff"));
        return t;
    }

    private static Label body(String text) {
        Label l = new Label(text);
        l.setWrapText(true);
        l.getStyleClass().add("frame-body");
        return l;
    }

    private static Label cell(String text, String color, boolean bold) {
        Label l = new Label(text);
        l.getStyleClass().add("frame-cell");
        l.setStyle("-fx-text-fill: " + color + ";" + (bold ? " -fx-font-weight: bold;" : ""));
        return l;
    }

    private static HBox lifeline(String name, String cmd, String desc) {
        PixelText n = new PixelText(name, 2, Color.web("#e2b8ff"));
        n.setFontSize(10);
        HBox nameBox = new HBox(n);
        nameBox.setMinWidth(70);
        Label c = new Label(cmd);
        c.getStyleClass().add("frame-cmd");
        c.setMinWidth(200);
        Label d = body(desc);
        HBox row = new HBox(14, nameBox, c, d);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    @FXML
    private void back() {
        SceneManager.show(Screen.MAIN_MENU);
    }
}

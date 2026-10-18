
package com.cmsc13.frontend;

import java.io.IOException;

import com.cmsc13.model.Question;
import com.cmsc13.model.QuestionLoader;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.Scene;
import javafx.scene.Node;

import java.util.List;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

public class QuizController {

    private Stage stage;
    private Scene scene;
    private Parent root;
    
    private static Scene returnScene;

    public static Scene getReturnScene() {
        return returnScene;
    }

    public static void setReturnScene(Scene scene) {
        returnScene = scene;
    }
    public void switchtoMainMenu(ActionEvent event)throws IOException {
        root = FXMLLoader.load(getClass().getResource("/fxml/MainMenu.fxml"));
        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();

    }
    private void switchToCamera() {
        try {
            Parent root = FXMLLoader.load(
                getClass().getResource("/fxml/Camera.fxml")
            );

            Stage stage = (Stage) questionLabel.getScene()
                    .getWindow();

            QuizController.setReturnScene(stage.getScene());

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML
    private Label questionLabel;

    @FXML
    private Label feedbackLabel;

    @FXML
    private Label scoreLabel;

    @FXML
    private Label livesLabel;

    @FXML
    private Button choice1;

    @FXML
    private Button choice2;

    @FXML
    private Button choice3;

    @FXML
    private Button choice4;

    private List<Question> questions;
    private int currentIndex = 0;
    private int score = 0;
    private int lives = 3;
    private boolean answered = false;

    @FXML
    public void initialize() {
        try {
            questions = QuestionLoader.loadQuestions();

            if (questions == null || questions.isEmpty()) {
                questionLabel.setText("No questions found.");
                setChoicesDisabled(true);
                return;
            }

            showQuestion();

        } catch (Exception e) {
            questionLabel.setText("Could not load questions.");
            setChoicesDisabled(true);
            e.printStackTrace();
        }
    }


    private void showQuestion() {
        if (currentIndex >= questions.size()) {
            questionLabel.setText("Quiz completed!");
            feedbackLabel.setText("Final score: " + score);
            setChoicesDisabled(true);
            return;
        }

        Question q = questions.get(currentIndex);

        questionLabel.setText(
            (currentIndex + 1) + ". " + q.getQuestion()
        );

        choice1.setText("A. " + q.getChoices().get(0));
        choice2.setText("B. " + q.getChoices().get(1));
        choice3.setText("C. " + q.getChoices().get(2));
        choice4.setText("D. " + q.getChoices().get(3));

        feedbackLabel.setText("");
        answered = false;

        setChoicesDisabled(false);
        updateStatus();
    }

    @FXML
    private void answer1() {
        checkAnswer(0);
    }

    @FXML
    private void answer2() {
        checkAnswer(1);
    }

    @FXML
    private void answer3() {
        checkAnswer(2);
    }

    @FXML
    private void answer4() {
        checkAnswer(3);
    }

    private void checkAnswer(int selectedIndex) {
        if (answered || currentIndex >= questions.size()) {
            return;
        }

        Question q = questions.get(currentIndex);
        answered = true;
        setChoicesDisabled(true);

        Button selectedButton = switch (selectedIndex) {
                case 0 -> choice1;
                case 1 -> choice2;
                case 2 -> choice3;
                case 3 -> choice4;
                default -> null;
            };

        if (selectedIndex == q.getCorrectIndex()) {
            score++;
            selectedButton.setStyle(
                    "-fx-border-color: green; " +
                    "-fx-border-width: 3px; " +
                    "-fx-border-radius: 5px;"
                );
            feedbackLabel.setText("Correct! +1 point.");

        }else {
            lives--;

            selectedButton.setStyle(
                "-fx-border-color: red; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 5px;"
            );

            PauseTransition delay = new PauseTransition(
                Duration.seconds(0.5)
            );

            delay.setOnFinished(event -> switchToCamera());
            delay.play();
        }

        updateStatus();
    }

    @FXML
    private void nextQuestion() {
        if (!answered || lives <= 0) {
            return;
        }

        choice1.setStyle(
                    "-fx-border-color: black; " +
                    "-fx-border-width: 1px; " +
                    "-fx-border-radius: 5px;"
                );
        choice2.setStyle(
                    "-fx-border-color: black; " +
                    "-fx-border-width: 1px; " +
                    "-fx-border-radius: 5px;"
                );
        choice3.setStyle(
                    "-fx-border-color: black; " +
                    "-fx-border-width: 1px; " +
                    "-fx-border-radius: 5px;"
                );
        choice4.setStyle(
                    "-fx-border-color: black; " +
                    "-fx-border-width: 1px; " +
                    "-fx-border-radius: 5px;"
                );

        currentIndex++;
        showQuestion();
    }

    private void updateStatus() {
        if (scoreLabel != null) {
            scoreLabel.setText("Score: " + score);
        }

        if (livesLabel != null) {
            livesLabel.setText("Lives: " + lives);
        }
    }

    private void setChoicesDisabled(boolean disabled) {
        choice1.setDisable(disabled);
        choice2.setDisable(disabled);
        choice3.setDisable(disabled);
        choice4.setDisable(disabled);
    }
}

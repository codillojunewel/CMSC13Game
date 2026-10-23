package com.cmsc13.frontend;

import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class Game {

    private Stage stage;
    private Scene scene;
    private Parent root;

    
    public void switchtoMainMenu(ActionEvent event)throws IOException {
        root = FXMLLoader.load(getClass().getResource("/fxml/MainMenu.fxml"));
        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();

    }

    @FXML
    private Label questionLabel;

    @FXML
    private Label feedbackLabel;

    @FXML
    private Button choiceA;

    @FXML
    private Button choiceB;

    @FXML
    private Button choiceC;

    @FXML
    private Button choiceD;

    private final String[] questions = {
        "What is a programming paradigm?",
        "Which of these is a programming language?"
    };

    private final String[][] choices = {
        {
            "A binary machine code instruction format",
            "A physical hardware component inside a CPU",
            "A fundamental style of structuring programs",
            "A specific brand of compiler"
        },
        {
            "Java",
            "HTML only",
            "A keyboard",
            "A monitor"
        }
    };

    // The correct answer's index: A = 0, B = 1, C = 2, D = 3.
    private final int[] correctAnswers = {2, 0};

    private int currentQuestion = 0;
    private int score = 0;
    private int lives = 3;
    private boolean answered = false;

    @FXML
    public void initialize() {
        showQuestion();
    }

    private void showQuestion() {
        if (currentQuestion >= questions.length) {
            questionLabel.setText("Quiz finished!");
            feedbackLabel.setText(
                "Final score: " + score + "/" + questions.length
                + " | Lives remaining: " + lives
            );

            setChoicesDisabled(true);
            return;
        }

        questionLabel.setText(questions[currentQuestion]);

        choiceA.setText("A. " + choices[currentQuestion][0]);
        choiceB.setText("B. " + choices[currentQuestion][1]);
        choiceC.setText("C. " + choices[currentQuestion][2]);
        choiceD.setText("D. " + choices[currentQuestion][3]);

        feedbackLabel.setText("");
        answered = false;
        setChoicesDisabled(false);
    }

    @FXML
    private void answerA() {
        checkAnswer(0);
    }

    @FXML
    private void answerB() {
        checkAnswer(1);
    }

    @FXML
    private void answerC() {
        checkAnswer(2);
    }

    @FXML
    private void answerD() {
        checkAnswer(3);
    }

    private void checkAnswer(int selectedAnswer) {
        // Prevent the player from answering the same question repeatedly.
        if (answered || currentQuestion >= questions.length) {
            return;
        }

        answered = true;
        setChoicesDisabled(true);

        if (selectedAnswer == correctAnswers[currentQuestion]) {
            score++;
            feedbackLabel.setText("Correct! Score: " + score);
        } else {
            lives--;
            feedbackLabel.setText(
                "Wrong answer! Punishment: -1 life. Lives left: " + lives
            );

            applyPunishment();
        }
    }

    private void applyPunishment() {
        // Example game penalty: losing one life.
        // You can add an animation or sound effect here later.
        if (lives <= 0) {
            feedbackLabel.setText("Game over! Final score: " + score);
        }
    }

    private void setChoicesDisabled(boolean disabled) {
        choiceA.setDisable(disabled);
        choiceB.setDisable(disabled);
        choiceC.setDisable(disabled);
        choiceD.setDisable(disabled);
    }

    @FXML
    private void nextQuestion() {
        if (!answered || lives <= 0) {
            return;
        }

        currentQuestion++;
        showQuestion();
    }




    
}

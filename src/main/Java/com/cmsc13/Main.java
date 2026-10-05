package com.cmsc13;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {

        Button button = new Button("Hello JavaFX!");

        StackPane root = new StackPane(button);

        Scene scene = new Scene(root, 600, 400);

        stage.setTitle("CMSC13 Game");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
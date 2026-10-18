package com.cmsc13;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import javafx.scene.Parent;

public class Main extends Application {

    
    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(
            getClass().getResource("/fxml/MainMenu.fxml")
        );

        Scene scene = new Scene(root);

        stage.setTitle("Are You Smarter Than a Junior Dev?");
        stage.setScene(scene);

        stage.setResizable(false);

        stage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
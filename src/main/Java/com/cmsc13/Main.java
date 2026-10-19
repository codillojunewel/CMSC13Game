
package com.cmsc13;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        // Display the splash screen first
        Parent splash = FXMLLoader.load(
            getClass().getResource("/fxml/SplashScreen.fxml")
        );

        stage.setTitle("Are You Smarter Than a Junior Dev?");
        stage.setScene(new Scene(splash));
        stage.setResizable(false);
        stage.show();

        

        // Load the main menu in the background
        Task<FXMLLoader> loadingTask = new Task<>() {
            @Override
            protected FXMLLoader call() throws Exception {
                Thread.sleep(5000);
                FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/MainMenu.fxml")
                );
                
                loader.load();
                return loader;
            }
        };

        // Switch to the main menu after loading finishes
        loadingTask.setOnSucceeded(event -> {
            Parent mainMenu = loadingTask.getValue().getRoot();

            stage.setScene(new Scene(mainMenu));
            stage.sizeToScene();
            stage.centerOnScreen();
        });

        loadingTask.setOnFailed(event -> {
            loadingTask.getException().printStackTrace();
        });

        Thread thread = new Thread(loadingTask);
        thread.setDaemon(true);
        thread.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

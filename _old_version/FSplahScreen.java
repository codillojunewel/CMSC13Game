package com.cmsc13.frontend;

import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.KeyCombination;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class FSplahScreen{


    public void show(Stage stage) {

        Group root = new Group();
        Text text = new Text();
        text.setText("Let's GOOO");
        text.setX(10);
        text.setY(300);
        root.getChildren().add(text);

        Scene scene = new Scene(root, 600, 400);

        stage.setTitle("Hiya la!");
        stage.setScene(scene);
        stage.setFullScreen(true);
        stage.setFullScreenExitHint("Press K to exit");
        stage.setFullScreenExitKeyCombination(KeyCombination.valueOf("k"));
        stage.show();
    }

   
}

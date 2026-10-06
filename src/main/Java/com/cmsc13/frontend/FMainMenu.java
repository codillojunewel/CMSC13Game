package com.cmsc13.frontend;

import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;

public class FMainMenu {
    
    private Stage stage;
    private Scene scene;
    private Parent root;


    public void switchtoStart(ActionEvent event) throws IOException{
        root = FXMLLoader.load(getClass().getResource("/fxml/StartScreen.fxml"));
        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
}

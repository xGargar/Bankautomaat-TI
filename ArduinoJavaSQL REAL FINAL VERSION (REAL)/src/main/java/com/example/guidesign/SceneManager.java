package com.example.guidesign;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

import javafx.stage.Stage;

import java.io.IOException;

import static javafx.application.Application.launch;
public class SceneManager{
    private static SceneManager instance;
    private Stage stage;
    private Scene scene1;
    private Scene scene2;
    private Scene scene3;
    private Scene scene4;
    private Scene scene5;
    private Scene scene6;
    private Scene scene7;
    private Scene scene8;
    private Scene scene9;
    private Scene scene10;
    private Scene scene11;



    public SceneManager(Stage stage) {
        this.stage = new Stage();
    }
    /*
    public static SceneManager getInstance() {
        if (instance == null) {
            instance = new SceneManager();
        }
        return instance;
    } */

    public void loadScenes() {
        try {
            // Load scene 1
            Parent scene1Root = FXMLLoader.load(getClass().getResource("page1.fxml"));
            Scene scene1 = new Scene(scene1Root);

            // Load scene 2
            Parent scene2Root = FXMLLoader.load(getClass().getResource("page2.fxml"));
            scene2 = new Scene(scene2Root);

            // Load scene 3
            Parent scene3Root = FXMLLoader.load(getClass().getResource("page3.fxml"));
            scene3 = new Scene(scene3Root);

            // Load scene 4
            Parent scene4Root = FXMLLoader.load(getClass().getResource("page4.fxml"));
            scene4 = new Scene(scene4Root);

            // Load scene 5
            Parent scene5Root = FXMLLoader.load(getClass().getResource("page5.fxml"));
            scene5 = new Scene(scene5Root);

            // Load scene 6
            Parent scene6Root = FXMLLoader.load(getClass().getResource("page6.fxml"));
            scene6 = new Scene(scene6Root);

            // Load scene 7
            Parent scene7Root = FXMLLoader.load(getClass().getResource("page7.fxml"));
            scene7 = new Scene(scene7Root);

            // Load scene 8
            Parent scene8Root = FXMLLoader.load(getClass().getResource("page8.fxml"));
            scene8 = new Scene(scene8Root);

            // Load scene 9
            Parent scene9Root = FXMLLoader.load(getClass().getResource("page9.fxml"));
            scene9 = new Scene(scene9Root);

            // Load scene 10
            Parent scene10Root = FXMLLoader.load(getClass().getResource("page10.fxml"));
            scene10 = new Scene(scene10Root);

            // Load scene 11
            Parent scene11Root = FXMLLoader.load(getClass().getResource("page11.fxml"));
            scene11 = new Scene(scene11Root);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void setPrimaryStage(Stage stage) {
        this.stage = stage;
    }

    public void changeScene(Scene newScene) {
        stage.setScene(newScene);
    }

//    public void goToPage2 () throws IOException {
//        Parent scene2Root = FXMLLoader.load(getClass().getResource("page2.fxml"));
//        Scene scene2 = new Scene(scene2Root);
//    }
    public void goToPage2 () {
        try {
            stage.setScene(scene2);
        } catch (Exception e) {
            System.out.println(e);
        }

    }
    public void goToPage3 () throws IOException {
        Parent scene3Root = FXMLLoader.load(getClass().getResource("page3.fxml"));
        Scene scene3 = new Scene(scene3Root);
    }



}

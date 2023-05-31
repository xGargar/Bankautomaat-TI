package com.example.guidesign;



import javafx.application.Application;


import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;







public class App extends Application {
    private Scene scene;
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
    private Scene scene12;
    public static void main(String[] args) {
        launch(args);
    }


    @Override
    public void start (Stage stage){
        try {
            // Load the font:
            Font.loadFont(App.class.getResourceAsStream("/fonts/REZB____ttf"), 0);

            SceneManager manager = new SceneManager(stage);
            manager.loadScenes();

            // Load page 1
            Parent root = FXMLLoader.load(getClass().getResource("page1.fxml"));
            scene = new Scene(root);

            Label errorLabelp1 = (Label) root.lookup("#errorLabelp1");

            stage.setScene(scene);
            stage.show();

            // Load scene 2
            Parent scene2Root = FXMLLoader.load(getClass().getResource("page2.fxml"));
            scene2 = new Scene(scene2Root);

            PasswordField pinCodeField = (PasswordField) scene2Root.lookup("PasswordField");
            pinCodeField.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-text-fill: white;");
            Label errorLabelp2 = (Label) scene2Root.lookup("#errorLabelp2");


            // Load scene 3
            Parent scene3Root = FXMLLoader.load(getClass().getResource("page3.fxml"));
            scene3 = new Scene(scene3Root);

            Label errorLabelp3 = (Label) scene3Root.lookup("#errorLabelp3");

            // Load scene 4
            Parent scene4Root = FXMLLoader.load(getClass().getResource("page4.fxml"));
            scene4 = new Scene(scene4Root);

            Text balanceField = (Text) scene4Root.lookup("Text");

            // Load scene 5
            Parent scene5Root = FXMLLoader.load(getClass().getResource("page5.fxml"));
            scene5 = new Scene(scene5Root);

            // Get labels from scene 5
            Label customAmountLabel = (Label) scene5Root.lookup("#customPinAmount");
            Label errorLabelp5 = (Label) scene5Root.lookup("#errorLabelp5");

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

            // Load scene 12
            Parent scene12Root = FXMLLoader.load(getClass().getResource("page12.fxml"));
            scene12 = new Scene(scene12Root);

            Label errorLabelp12 = (Label) scene12Root.lookup("#errorLabelp12");

            App mainInstance = this;
            ATM atm = new ATM(stage, mainInstance, pinCodeField, balanceField, customAmountLabel, errorLabelp1, errorLabelp2, errorLabelp3, errorLabelp5, errorLabelp12);
            atm.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void returnScene() {

    }
    public void goToPage1 (Stage stage) {
        stage.setScene(scene);
    }
    public void goToPage2 (Stage stage) {
        stage.setScene(scene2);
    }

    public void goToPage3 (Stage stage) {
        stage.setScene(scene3);
    }

    public void goToPage4 (Stage stage) {
        stage.setScene(scene4);
    }
    public void goToPage5 (Stage stage) {
        stage.setScene(scene5);
    }
    public void goToPage6 (Stage stage) {
        stage.setScene(scene6);
    }
    public void goToPage7 (Stage stage) {
        stage.setScene(scene7);
    }
    public void goToPage8 (Stage stage) {
        stage.setScene(scene8);
    }
    public void goToPage9 (Stage stage) {
        stage.setScene(scene9);
    }
    public void goToPage10 (Stage stage) {
        stage.setScene(scene10);
    }
    public void goToPage11 (Stage stage) {
        stage.setScene(scene11);
    }
    public void goToPage12 (Stage stage) {
        stage.setScene(scene12);
    }

}



    
    

       
        
   












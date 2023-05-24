package com.example.guidesign;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class Controller {

    private Stage stage;
    private Scene scene;
    private Parent root;

    @FXML

    public void handleBtn1(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("page2.fxml"));
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();

    }

    public void ok1(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("page3.fxml"));
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();

    }


    public void saldoOpvragen(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("page4.fxml"));
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void geldOpnemen(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("page5.fxml"));
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void terug(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("page3.fxml"));
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void ok2(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("page6.fxml"));
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();

    }

    public void wacht(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("page7.fxml"));
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();

    }

    public void neem(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("page8.fxml"));
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();

    }

    public void zeventig(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("page6.fxml"));
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();

    }
    public void ja(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("page10.fxml"));
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();

    }
    public void nee(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("page9.fxml"));
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();

    }

    public void bon(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("page9.fxml"));
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();

    }
}
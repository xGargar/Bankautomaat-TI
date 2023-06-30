package com.example.guidesign;

import com.fazecast.jSerialComm.*;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Scanner;


public class ATM extends Thread {
    public static Connection connection;

    public static OutputStream output;
    public static InputStream inputStream;
    private static Parent root;
    private static Label customAmountLabel;

    // Declare chars for buttons to send
    public static char topLeftButton = 'Q';
    public static char topRightButton = 'W';
    public static char middleLeftButton = 'E';
    public static char middleRightButton = 'R';
    public static char bottomRightButton = 'T';
    public static char bottomLeftButton = 'X';

    // PIN MACHINE ID
    public static String machineID = "420";
    public static Integer transactionID = 0;
    public static String receivedString = "";
    public static Integer userBalance;

    private static Stage stage;
    private static App app;
    private static String pinCode;
    private static String klantID;
    private Parent scene2Root;
    private PasswordField pinCodeField;
    private static Text balanceField;
    private static Label errorLabelp1;
    private static Label errorLabelp2;
    private static Label errorLabelp3;
    private static Label errorLabelp5;
    private static Label errorLabelp12;
    public static ServerController serverController;


    public ATM(Stage stage, App app, PasswordField pinCodeField, Text balanceField, Label customAmountLabel, Label errorLabelp1, Label errorLabelp2, Label errorLabelp3,  Label errorLabelp5, Label errorLabelp12) {
        this.stage = stage;
        this.app = app;
        this.pinCodeField = pinCodeField;
        this.balanceField = balanceField;
        this.customAmountLabel = customAmountLabel;
        this.errorLabelp1 = errorLabelp1;
        this.errorLabelp2 = errorLabelp2;
        this.errorLabelp3 = errorLabelp3;
        this.errorLabelp5 = errorLabelp5;
        this.errorLabelp12 = errorLabelp12;
    }

    public void run() {
        String portName = "COM9"; // Replace with the name of your serial port
        int baudRate = 9600; // Replace with the baud rate used in your Arduino sketch

        // Get the serial port object for the given port name
        SerialPort serialPort = SerialPort.getCommPort(portName);

        // serialPort.openPort();
        serialPort.setBaudRate(baudRate);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 60000000, 0);




        // // Open the serial port
        if (!serialPort.openPort()) {
            System.err.println("Error: Could not open serial port");
            return;
        }

        // Wait for a response from the Arduino
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            System.err.println("Error: Thread interrupted");
            e.printStackTrace();
            return;
        }



        // Connect to my local SQL database
        String url = "jdbc:mysql://127.0.0.1:3306/bank";
        String username = "root";
        String password = "xb25082004";
        try {
            //Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(url, username, password);
        } catch (Exception e) {
            System.err.println("Error: Could not connect to database");
            e.printStackTrace();
            return;
        }
        klantID = "";
        pinCode = "";
        String correctPinCode = null;
        boolean machineIsOn = true;
        boolean userKnowsPincode = false;

        App mainInstance = new App();

        while (machineIsOn) {
            // While user has not been verified yet
            while (!userKnowsPincode) {
                // Reset user balance to null
                userBalance = 6;
                //while (correctPinCode == null) {
                while (klantID == "") {
                    // Call method to read RFID card


                    System.out.println("Please scan card");
                    Platform.runLater(() -> {
                        app.goToPage1(stage);
                    });
                    klantID = receiveArduinoStrings(serialPort);
                    if (klantID != null) {
                        char outChar = '1'; // Replace '1' with the character you want to send
                        byte[] sendData = new byte[] { (byte) outChar };
                        serialPort.writeBytes(sendData, sendData.length);
                    }
                    else {
                        char outChar = '0'; // Replace '1' with the character you want to send
                        byte[] sendData = new byte[] { (byte) outChar };
                        serialPort.writeBytes(sendData, sendData.length);
                    }
                    System.out.println(klantID);
                }
                // Go to pin validation screen in GUI
                try {
                    Thread.sleep(1000);
                    Platform.runLater(() -> {
                        app.goToPage2(stage);
                    });

                } catch (Exception e) {
                    System.out.println(e);
                }
                // Input Pin code
                try {
                    userKnowsPincode =  inputVerificationPin(serialPort, pinCodeField, klantID);
                } catch (Exception e) {
                    System.out.println(e);
                }
                // Sets userKnowsPincode to true if entered pin is correct. Otherwise, returns the user to the start screen
                if (userBalance != null) {
                    userKnowsPincode = true;
                }
                // Returns to start screen if user entered the incorrect pinCode 3 times in a row
                else if (!userKnowsPincode) {
                    klantID = "";
                }

            }
            // Go to Menu
            try {
                Platform.runLater(() -> {
                    app.goToPage3(stage);
                });

            } catch (Exception e) {
                System.out.println(e);
            }

            //getBalanceOnAccount(serialPort, connection, klantID);
            System.out.println("\nGet balance               fast pin 70 euro");
            System.out.println("\nPin money");
            System.out.println("\nCancel\n");
            // Run code for menu

            Boolean isPinningSessionDone = atmMenu(serialPort, klantID);

            if (isPinningSessionDone) {
                // Rest each used variable used when machine is on
                klantID = "";
                pinCode = "";
                transactionID++;
                correctPinCode = null;
                userKnowsPincode = false;
            }

        }
        // Close the serial port and database connection
        serialPort.closePort();
        try {
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean atmMenu(SerialPort serialPort, String klantID) {
        inputStream = serialPort.getInputStream();
        byte[] buffer = new byte[1024];
        char pressedButton;
        String amountToPin;
        char outChar;
        while(true) {
            try {
                int byteCount = inputStream.read(buffer);
                if (byteCount > -1) {
                    for (int i = 0; i < byteCount; i++) {

                        // Set pressed key to data read from Serial Port
                        pressedButton = (char) buffer[i];

                        // If top left button is pressed
                        if (pressedButton == topLeftButton) {
                            // Run code for checking balance
                            System.out.println("Checking balance...");
                            getBalanceOnAccount(serialPort, connection, klantID);
                        }
                        // If top right button is pressed
                        else if (pressedButton == topRightButton) {
                            // Run code for fast pin of 70
                            System.out.println("pinning 70 euro");
                            amountToPin = "70";
                            System.out.println(pinCode);
                            boolean youCanPin70 = serverController.withdrawFunction(klantID, pinCode, amountToPin);
                            if (youCanPin70) {
                                outChar = '1'; // Replace '1' with the character you want to send
                                byte[] sendData = new byte[] { (byte) outChar };
                                serialPort.writeBytes(sendData, sendData.length);
                                convertToBills(amountToPin, serialPort);
                                // Get and send data to arduino for receipt printer
                                chooseToPrintReceipt(serialPort, klantID, amountToPin);
                                // Restart arduino
                                Platform.runLater(() -> {
                                    app.goToPage9(stage);
                                });
                                // Run code for cancelling menu
                                serialPort.closePort();
                                try {
                                    Thread.sleep(1000);
                                } catch (Exception e) {
                                    System.out.println(e);
                                }
                                serialPort.openPort();
                                Platform.runLater(() -> {
                                    app.goToPage1(stage);
                                });
                                return true;
                            }
                            else {
                                outChar = '0'; // Replace '1' with the character you want to send
                                byte[] sendData = new byte[] { (byte) outChar };
                                serialPort.writeBytes(sendData, sendData.length);
                            }

                        }
                        // If middle left button is pressed
                        else if (pressedButton == middleLeftButton) {
                            System.out.println("50 euro               20 euro\n");
                            System.out.println("10 euro               Custom amount\n");
                            System.out.println("Cancel\n");

                            // Run code for pinning money

                            boolean pinnedSuccessful = pinMoneyMenu(serialPort, klantID);
                            if (pinnedSuccessful) {
                                return true;
                            }
                            else if (!pinnedSuccessful) {
                                System.out.println("back to main menu");
                            }

                        }
                        // If middle right button is pressed
                        else if (pressedButton == middleRightButton) {
                            // Run code for nothing for now

                        }
                        // If bottom left button is pressed
                        else if (pressedButton == bottomRightButton) {
                            // Run code for nothing rn

                        }
                        // If cancel button is pressed
                        else if (pressedButton == bottomLeftButton) {
                            System.out.println("Cancelling...");
                            Platform.runLater(() -> {
                                app.goToPage9(stage);
                            });
                            // Run code for cancelling menu
                            serialPort.closePort();
                            try {
                                Thread.sleep(500);
                            } catch (Exception e) {
                                // TODO: handle exception
                            }
                            serialPort.openPort();
                            Platform.runLater(() -> {
                                app.goToPage1(stage);
                            });
                            return true;
                        }

                    }
                }
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    private static boolean pinMoneyMenu(SerialPort serialPort, String klantID) {
        inputStream = serialPort.getInputStream();
        byte[] menuBuffer = new byte[1024];
        char pressedButton;
        String amountToPin;
        boolean pinningSuccessfull;
        char outChar;


        // Load page 12
        Platform.runLater(() -> {
            app.goToPage12(stage);
            errorLabelp3.setText("");
            errorLabelp5.setText("");
            errorLabelp12.setText("");
        });

        while (true) {
            try {
                int byteCount = inputStream.read(menuBuffer);
                if (byteCount > -1) {
                    for (int i = 0; i < byteCount; i++) {

                        // Set pressed key to data read from Serial Port
                        pressedButton = (char) menuBuffer[i];

                        // If top left button is pressed
                        if (pressedButton == topLeftButton) {
                            amountToPin = "50";
                            System.out.println("Selected 50 euro");
                            pinningSuccessfull = serverController.withdrawFunction(klantID, pinCode, amountToPin);
                            if (pinningSuccessfull) {
                                outChar = '1'; // Replace '1' with the character you want to send
                                byte[] sendData = new byte[] { (byte) outChar };
                                serialPort.writeBytes(sendData, sendData.length);
                                // Go to loading page
                                Platform.runLater(() -> {
                                    app.goToPage6(stage);
                                });

                                convertToBills(amountToPin, serialPort);
                                // Get and send data to arduino for receipt printer
                                chooseToPrintReceipt(serialPort, klantID, amountToPin);
                                // Restart arduino
                                Platform.runLater(() -> {
                                    app.goToPage9(stage);
                                });
                                // Run code for cancelling menu
                                serialPort.closePort();
                                try {
                                    Thread.sleep(1000);
                                } catch (Exception e) {
                                    // TODO: handle exception
                                }
                                serialPort.openPort();
                                Platform.runLater(() -> {
                                    app.goToPage1(stage);
                                });
                                return true;
                            }
                            else if (!pinningSuccessfull) {
                                outChar = '0'; // Replace '1' with the character you want to send
                                byte[] sendData = new byte[] { (byte) outChar };
                                serialPort.writeBytes(sendData, sendData.length);
                            }

                        }
                        // If top right button is pressed
                        else if (pressedButton == topRightButton) {
                            amountToPin = "20";
                            System.out.println("Selected 20 euro");
                            pinningSuccessfull = serverController.withdrawFunction(klantID, pinCode, amountToPin);
                            if (pinningSuccessfull) {
                                outChar = '1'; // Replace '1' with the character you want to send
                                byte[] sendData = new byte[] { (byte) outChar };
                                serialPort.writeBytes(sendData, sendData.length);
                                // Go to loading page
                                Platform.runLater(() -> {
                                    app.goToPage6(stage);
                                });
                                convertToBills(amountToPin, serialPort);
                                // Get and send data to arduino for receipt printer
                                chooseToPrintReceipt(serialPort, klantID, amountToPin);
                                // Restart arduino
                                Platform.runLater(() -> {
                                    app.goToPage9(stage);
                                });
                                // Run code for cancelling menu
                                serialPort.closePort();
                                try {
                                    Thread.sleep(1000);
                                } catch (Exception e) {
                                    // TODO: handle exception
                                }
                                serialPort.openPort();
                                Platform.runLater(() -> {
                                    app.goToPage1(stage);
                                });
                                return true;
                            }
                            else if (!pinningSuccessfull) {
                                outChar = '0'; // Replace '1' with the character you want to send
                                byte[] sendData = new byte[] { (byte) outChar };
                                serialPort.writeBytes(sendData, sendData.length);

                            }
                        }
                        // If middle left button is pressed
                        else if (pressedButton == middleLeftButton) {
                            amountToPin = "10";
                            System.out.println("Selected 10 euro");
                            pinningSuccessfull = serverController.withdrawFunction(klantID, pinCode, amountToPin);
                            if (pinningSuccessfull) {
                                outChar = '1'; // Replace '1' with the character you want to send
                                byte[] sendData = new byte[] { (byte) outChar };
                                serialPort.writeBytes(sendData, sendData.length);
                                // Go to loading page
                                Platform.runLater(() -> {
                                    app.goToPage6(stage);
                                });
                                convertToBills(amountToPin, serialPort);
                                // Get and send data to arduino for receipt printer
                                chooseToPrintReceipt(serialPort, klantID, amountToPin);
                                // Restart arduino
                                Platform.runLater(() -> {
                                    app.goToPage9(stage);
                                });
                                // Run code for cancelling menu
                                serialPort.closePort();
                                try {
                                    Thread.sleep(1000);
                                } catch (Exception e) {
                                    // TODO: handle exception
                                }
                                serialPort.openPort();
                                Platform.runLater(() -> {
                                    app.goToPage1(stage);
                                });
                                return true;
                            }
                            else if (!pinningSuccessfull) {
                                outChar = '0'; // Replace '1' with the character you want to send
                                byte[] sendData = new byte[] { (byte) outChar };
                                serialPort.writeBytes(sendData, sendData.length);
                            }
                        }

                        else if (pressedButton == middleRightButton) {
                            System.out.println("You want to enter a custom amount");
                            amountToPin = String.valueOf(getCustomPinAmount(serialPort, klantID));
                            if (amountToPin != null) {

                                convertToBills(amountToPin, serialPort);
                                // Get and send data to arduino for receipt printer
                                chooseToPrintReceipt(serialPort, klantID, amountToPin);
                                // Restart arduino
                                Platform.runLater(() -> {
                                    app.goToPage9(stage);
                                });
                                // Run code for cancelling menu
                                serialPort.closePort();
                                try {
                                    Thread.sleep(1000);
                                } catch (Exception e) {
                                    // TODO: handle exception
                                }
                                serialPort.openPort();
                                Platform.runLater(() -> {
                                    app.goToPage1(stage);
                                });
                                return true;
                            }
                            else if (amountToPin == null) {
                                System.out.println("exited pinning");
                            }

                        }

                        // If cancel button is pressed
                        else if (pressedButton == bottomLeftButton) {
                            // Run code for cancelling menu
                            System.out.println("Cancelling...");
                            // Load page 12
                            Platform.runLater(() -> {
                                app.goToPage3(stage);
                                app.goToPage12(stage);
                                errorLabelp3.setText("");
                                errorLabelp5.setText("");
                                errorLabelp12.setText("");
                            });
                            return false;
                        }

                    }
                }
            } catch (Exception e) {
                System.out.println(e);
            }

        }
    }

    private static void chooseToPrintReceipt(SerialPort serialPort, String klantID, String pinAmount) {
        inputStream = serialPort.getInputStream();
        byte[] receiptBuffer = new byte[1024];
        char pressedButton;

        while(receivedString != "good") {
            receivedString = receiveArduinoStrings(serialPort);
            if (receivedString.equals("good")) {
                receivedString = "";
                break;
            }
        }
        try {
            Platform.runLater(() -> {
                app.goToPage7(stage);
            });
            Thread.sleep(2500);
        } catch (Exception e){
            System.out.println(e);
        }
        // Wait for dispensing to finish

        System.out.println("Do you want a receipt?");

        try {
            Platform.runLater(() -> {
                app.goToPage8(stage);
            });
        } catch (Exception e) {
            System.out.println(e);
        }



        // Go to receipt page
        try {
            Platform.runLater(() -> {
                app.goToPage8(stage);
            });
        } catch (Exception e){
            System.out.println(e);
        }
        while (true) try {
            int byteCount = inputStream.read(receiptBuffer);
            if (byteCount > -1) {
                for (int i = 0; i < byteCount; i++) {
                    // Set pressed key to data read from Serial Port
                    pressedButton = (char) receiptBuffer[i];

                    // If top left button is pressed. aka you don't want to print a receipt
                    if (pressedButton == bottomRightButton) {
                        System.out.println("You chose to not print a receipt");

                        return;
                    }

                    // If top right button is pressed. aka you want to print a receipt
                    else if (pressedButton == bottomLeftButton) {
                        System.out.println("you chose to print a receipt");
                        try {
                            Platform.runLater(() -> {
                                app.goToPage6(stage);
                            });
                        } catch (Exception e){
                            System.out.println(e);
                        }
                        getAndSendDataForBill(serialPort, connection, klantID, pinAmount);
                        while(receivedString != "done") {
                            receivedString = receiveArduinoStrings(serialPort);
                            if (receivedString.equals("done")) {
                                receivedString = "";
                                return;
                            }
                        }
                    }
                }
            }
        }catch (Exception e) {
            System.out.println(e);
        }

    }

    private static Boolean inputVerificationPin(SerialPort serialPort, PasswordField passwordField, String klantID) {
        inputStream = serialPort.getInputStream();
        byte[] pinBuffer = new byte[1024];
        char pressedKey;
        int pinTries = 0;
        Boolean pinCorrect = false;
        char outChar;

        System.out.println("Enter your pin code");

        while (pinTries <= 2 && !pinCorrect) {
            try {
                int byteCount = inputStream.read(pinBuffer);
                if (byteCount > -1) {
                    for (int i = 0; i < byteCount; i++) {

                        // Set pressed key to data read from Serial Port
                        pressedKey = (char) pinBuffer[i];
                        System.out.println(pressedKey);
                        // If clear key is pressed, clear entered pin code
                        if (pressedKey == '*') {
                            pinCode = "";
                            passwordField.setText(pinCode);
                        }

                        else if (pressedKey == '#') {
                            System.out.println("test");
                            try {
                                userBalance = serverController.getBalance(klantID, pinCode);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            System.out.println(userBalance);
                            if (userBalance != -1) {
                                System.out.println("You entered the correct pin code!");
                                System.out.println("Balance: " + userBalance);
                                try {
                                    Thread.sleep(500);
                                } catch (Exception e) {
                                    // TODO: handle exception
                                }
                                // Send char 1 (true) as byte to arduino
                                outChar = '1'; // Replace '1' with the character you want to send
                                byte[] sendData = new byte[] { (byte) outChar };
                                serialPort.writeBytes(sendData, sendData.length);
                                passwordField.setText("");
                                Platform.runLater(() -> {
                                    errorLabelp2.setText("");
                                });
                                return true;
                            }
                            else {
                                System.out.println("You entered the incorrect pin code");

                                pinTries++;
                                int finalPinTries = pinTries;
                                Platform.runLater(() -> {
                                    errorLabelp2.setText("Foute pincode\nU heeft " + finalPinTries + " poging(en) verricht");
                                });
                                pinCode = "";
                                passwordField.setText(pinCode);
                            }


                        }

                        else if (pressedKey != '#' && pressedKey != '*' && pinCode.length() == 4) {
                            System.out.println("Press the '#' key, please");
                        }

                        else {
                            pinCode += pressedKey ;
                            //System.out.println(pinCode);
                            passwordField.setText(pinCode);
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println(e);
            }
        }
        // Do something with the fact that you exceeded the 3 tries

        // Call future method to update user's card status to blocked here


        outChar = '2';
        byte[] sendData = new byte[] { (byte) outChar};
        serialPort.writeBytes(sendData, sendData.length);
        serialPort.closePort();
        try {
            Thread.sleep(500);
        } catch (Exception e) {
            // TODO: handle exception
        }
        serialPort.openPort();
        return false;
    }

    public static void sendBillAmountToArduino(HashMap<String, Integer> eachBillAmount, SerialPort serialPort) {
        try {
            // Send amount of 50 euro bills to arduino
            System.out.println(Integer.toString(eachBillAmount.get("billAmount50")));
            String billAmount50 = Integer.toString(eachBillAmount.get("billAmount50"));
            sendString(serialPort, billAmount50);

            while(receivedString != "good") {
                receivedString = receiveArduinoStrings(serialPort);
                if (receivedString.equals("good")) {
                    receivedString = "";
                    break;
                }
            }
            // Wait 250 ms
            Thread.sleep(250);

            // Send amount of 20 euro bills to arduino
            String billAmount20 = Integer.toString(eachBillAmount.get("billAmount20"));
            sendString(serialPort, billAmount20);

            while(receivedString != "good") {
                receivedString = receiveArduinoStrings(serialPort);
                if (receivedString.equals("good")) {
                    receivedString = "";
                    break;
                }
            }

            // Wait 250 ms
            Thread.sleep(250);
            // Send amount of 10 euro bills to arduino
            String billAmount10 = Integer.toString(eachBillAmount.get("billAmount10"));
            sendString(serialPort, billAmount10);
            while(receivedString != "good") {
                receivedString = receiveArduinoStrings(serialPort);
                if (receivedString.equals("good")) {
                    receivedString = "";
                    break;
                }
            }

            // Wait 250 ms
            Thread.sleep(250);
            // Send amount of 5 euro bills to arduino
            String billAmount5 = Integer.toString(eachBillAmount.get("billAmount5"));
            sendString(serialPort, billAmount5);
            while(receivedString != "good") {
                receivedString = receiveArduinoStrings(serialPort);
                if (receivedString.equals("good")) {
                    receivedString = "";
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }

    }

    public static void getBalanceOnAccount(SerialPort serialPort, Connection connection, String klantID) {
        inputStream = serialPort.getInputStream();
        byte[] balanceBuffer = new byte[1024];
        char pressedButton;
        //char outChar;

        while(true) {
            try {

                Platform.runLater(() -> {
                    app.goToPage4(stage);
                });

                // Sets text on GUI to current user's balance
                balanceField.toFront();
                balanceField.setText(String.valueOf(userBalance) + " Euro");
                System.out.println("Your current balance: " + userBalance + " euro");

                // Wait for cancel button to be pressed
                while (true) {
                    int byteCount = inputStream.read(balanceBuffer);
                    if (byteCount > -1) {
                        for (int i = 0; i < byteCount; i++) {

                            // Set pressed button to data read from serial port
                            pressedButton = (char) balanceBuffer[i];

                            // If cancel button is pressed. exit method. in this code the input is 'X'
                            if (pressedButton == bottomLeftButton) {
                                System.out.println("Cancelling...");

                                // wait for a sec
                                try {
                                    Platform.runLater(() -> {
                                        app.goToPage3(stage);
                                    });
                                    Thread.sleep(1000);
                                } catch (Exception e) {
                                    // TODO: handle exception
                                }
                                return;
                            }
                        }
                    }
                }

            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    private static void getAndSendDataForBill(SerialPort serialPort, Connection connection, String klantID, String pinAmount) throws InterruptedException {
        System.out.println("Sending data for receipt");
        try {

            // Wait 250 ms
            Thread.sleep(250);

            // Send rekeningID as string to arduino
            String iban = klantID;
            sendString(serialPort, iban);

            while(receivedString != "good") {
                receivedString = receiveArduinoStrings(serialPort);
                if (receivedString.equals("good")) {
                    receivedString = "";
                    break;
                }
            }
            // Wait 250 ms
            Thread.sleep(250);

            // Send pin amount as string to arduino
            sendString(serialPort, pinAmount);
            while(receivedString != "good") {
                receivedString = receiveArduinoStrings(serialPort);
                if (receivedString.equals("good")) {
                    receivedString = "";
                    break;
                }
            }

            // Wait 250 ms
            Thread.sleep(250);
            // Send machine id to arduino
            sendString(serialPort, machineID);
            while(receivedString != "good") {
                receivedString = receiveArduinoStrings(serialPort);
                if (receivedString.equals("good")) {
                    receivedString = "";
                    break;
                }
            }

            // Wait 250 ms
            Thread.sleep(250);
            // Get and send current date time in format yy-mm-dd hh:mm:ss
            SimpleDateFormat dateFormat = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
            Date currentDate = new Date();
            String formattedDateTime = dateFormat.format(currentDate);
            sendString(serialPort, formattedDateTime);
            while(receivedString != "good") {
                receivedString = receiveArduinoStrings(serialPort);
                if (receivedString.equals("good")) {
                    receivedString = "";
                    break;
                }
            }
            // Wait 250 ms
            Thread.sleep(250);
            // Statement statement2 = connection.createStatement();

            sendString(serialPort, String.valueOf(transactionID));
            while(receivedString != "good") {
                receivedString = receiveArduinoStrings(serialPort);
                if (receivedString.equals("good")) {
                    receivedString = "";
                    break;
                }
            }
            System.out.println("Sent data for receipt!");
            return;

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return;
    }

    private static String receiveArduinoStrings(SerialPort serialPort) {
        // Read the response from the serial port

        InputStream input = serialPort.getInputStream();
        Scanner scanner = new Scanner(input);
        String response = "";
        while (scanner.hasNextLine()) {
            response = scanner.nextLine();
            scanner.close();
            return response;
        }
        scanner.close();

        return "";
    }

    private static void sendString(SerialPort serialPort, String stringToSend) {

        String message = stringToSend;
        byte[] bytes = message.getBytes();
        serialPort.writeBytes(bytes, bytes.length);
        System.out.println("Sent: " + message);

    }

    private static Integer getCustomPinAmount(SerialPort serialPort, String klantID) {
        inputStream = serialPort.getInputStream();
        byte[] buffer = new byte[1024];
        char pressedKey;
        boolean pinnedMoney = false;
        String customAmountToPin = "";
        char outChar;
        System.out.println("Please enter custom pin amount");
        try {
            // Go to loading page
            Platform.runLater(() -> {
                app.goToPage5(stage);
                errorLabelp5.setText("");
                errorLabelp12.setText("");
            });
        } catch (Exception e) {
            System.out.println(e);
        }
        customAmountLabel.setText("€");
        try {
            while (!pinnedMoney) {
                int byteCount = inputStream.read(buffer);
                if (byteCount > -1) {
                    for (int i = 0; i < byteCount; i++) {

                        // Set pressed key to data read from Serial Port
                        pressedKey = (char) buffer[i];
                        // If clear key is pressed, clear entered pin code
                        if (pressedKey == '*') {
                            customAmountToPin = "";
                            String amountToPinGUI = customAmountToPin;
                            Platform.runLater(() -> {
                                customAmountLabel.setText("€" + amountToPinGUI);
                            });
                        }

                        else if (pressedKey == '#') {
                            // Convert amount to pin to Integer
                            Integer amountToPin = Integer.valueOf(customAmountToPin);
                            if (amountToPin % 5 == 0 && amountToPin >= 10) {
                                String amountToPinString = Integer.toString(amountToPin);
                                boolean pinSuccessful = serverController.withdrawFunction(klantID, pinCode, amountToPinString);
                                if (pinSuccessful) {
                                    return amountToPin;
                                }
                                else {
                                    String amountToPinGUI = customAmountToPin;
                                    Platform.runLater(() -> {
                                        customAmountLabel.setText("€" + amountToPinGUI);
                                    });
                                }

                            }
                            else if (amountToPin % 5 != 0) {
                                System.out.println("Please enter an amount that is in intervals of 5");
                                customAmountToPin = "";
                                String amountToPinGUI = customAmountToPin;
                                Platform.runLater(() -> {
                                    customAmountLabel.setText("€" + amountToPinGUI);
                                    errorLabelp5.setText("Voer een bedrag in dat in de tafel van 5 zit");
                                });
                            }
                            else if (amountToPin < 10) {
                                System.out.println("Please enter an amount higher than 10 euro");
                                customAmountToPin = "";
                                String amountToPinGUI = customAmountToPin;
                                Platform.runLater(() -> {
                                    customAmountLabel.setText("€" + amountToPinGUI);
                                    errorLabelp5.setText("Voer een bedrag boven de 10 euro in");
                                });
                            }


                        }
                        else if (pressedKey == bottomLeftButton){
                            System.out.println("Cancelling entering custom pin");
                            Platform.runLater(() -> {
                                app.goToPage12(stage);
                            });
                            return null;
                        }
                        else {
                            customAmountToPin += pressedKey;
                            String amountToPinGUI = customAmountToPin;
                            Platform.runLater(() -> {
                                customAmountLabel.setText("€" + amountToPinGUI);
                            });

                            System.out.println(customAmountToPin);
                        }
                    }
                }
            }


        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }




    private static void convertToBills(String amountToDispenseString, SerialPort serialPort) {
        Integer amountToDispense = Integer.parseInt(amountToDispenseString);
        HashMap<String, Integer> eachBillAmount = new HashMap<>();
        Integer billAmount50;
        Integer billAmount20;
        Integer billAmount10;
        Integer billAmount5;
        try {
            Platform.runLater(() -> {
                app.goToPage6(stage);
            });
        } catch (Exception e){
            System.out.println(e);
        }

        // Sets the remainder to the full amount left to pin, since no calculations have been made yet
        Integer amountLeftToCalc = amountToDispense;
        System.out.println("Total amount of money: " + amountToDispense);

        // Calculate how many bills of 50 are needed and grabs the remainder from the divison
        billAmount50 = amountLeftToCalc / 50;
        amountLeftToCalc %= 50;
        eachBillAmount.put("billAmount50", billAmount50);
        System.out.println("Amount of 50 Dollar bills: " +billAmount50);

        // Calculate how many bills of 20 are needed and grabs the remainder from the divison
        billAmount20 = amountLeftToCalc / 20;
        amountLeftToCalc %= 20;
        eachBillAmount.put("billAmount20", billAmount20);
        System.out.println("Amount of 20 Dollar bills: " +billAmount20);

        // Calculate how many bills of 10 are needed and grabs the remainder from the divison
        billAmount10 = amountLeftToCalc / 10;
        amountLeftToCalc %= 10;
        eachBillAmount.put("billAmount10", billAmount10);
        System.out.println("Amount of 10 Dollar bills: " + billAmount10);

        // Calculate how many bills of 5 are needed and grabs the remainder from the divison
        billAmount5 = amountLeftToCalc / 5;
        amountLeftToCalc %= 5;
        eachBillAmount.put("billAmount5", billAmount5);
        System.out.println("Amount of 5 Dollar bills: " + billAmount5);

        sendBillAmountToArduino(eachBillAmount, serialPort);
        // Go to page to take money and wait 2.5s

        return;
    }
    private static void editPinCode(SerialPort serialPort, Connection connection, String klantID) {
        inputStream = serialPort.getInputStream();
        byte[] buffer = new byte[1024];
        char pressedKey;
        String pinCode = "";
        boolean editedPinCode = false;
        char outChar;

        System.out.println("enter your new pin code");

        try {
            while (!editedPinCode) {
                int byteCount = inputStream.read(buffer);
                if (byteCount > -1) {
                    for (int i = 0; i < byteCount; i++) {

                        // Set pressed key to data read from Serial Port
                        pressedKey = (char) buffer[i];
                        // If clear key is pressed, clear entered pin code
                        if (pressedKey == '*') {
                            pinCode = "";
                        }

                        else if (pressedKey == '#') {
                            if (pinCode.length() == 4) {
                                // Create SQL query for editing Pin Code
                                String sql = "Update klant SET klantPin = ? WHERE klantID = ? ;";
                                PreparedStatement statement = connection.prepareStatement(sql);
                                statement.setString(1, pinCode);
                                statement.setString(2, klantID);
                                statement.executeUpdate();
                                System.out.println("Edited your Pin to: " + pinCode);
                                editedPinCode = true;
                                outChar = '1'; // Replace '1' with the character you want to send
                                byte[] sendData = new byte[] { (byte) outChar };
                                serialPort.writeBytes(sendData, sendData.length);

                                // Close port and reopen to be able to send data again

                            }
                            // else if (pinCode.length() > 5 );
                            // System.out.println("Invalid pin. Please re-enter pin");
                            // pinCode = "";

                        }



                        else if (pressedKey != '*' && pressedKey != '#'){
                            pinCode += pressedKey;
                            System.out.println(pinCode);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }



}


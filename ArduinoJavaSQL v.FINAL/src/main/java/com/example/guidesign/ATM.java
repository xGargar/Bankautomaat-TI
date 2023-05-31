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

    public static String receivedString = "";

    private static Stage stage;
    private static App app;
    private Parent scene2Root;
    private PasswordField pinCodeField;
    private static Text balanceField;
    private static Label errorLabelp3;
    private static Label errorLabelp5;
    private static Label errorLabelp12;
    public ATM(Stage stage, App app, PasswordField pinCodeField, Text balanceField, Label customAmountLabel,Label errorLabelp3,  Label errorLabelp5, Label errorLabelp12) {
        this.stage = stage;
        this.app = app;
        //  this.scene2Root = scene2Root;
        this.pinCodeField = pinCodeField;
        this.balanceField = balanceField;
        this.customAmountLabel = customAmountLabel;
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

        String klantID = "";
        String correctPinCode = null;
        boolean machineIsOn = true;
        boolean userKnowsPincode = false;

        App mainInstance = new App();

        while (machineIsOn) {
            // While user has not been verified yet
            while (!userKnowsPincode) {
                while (correctPinCode == null) {
                    // Call method to read RFID card


                    System.out.println("Please scan card");
                    Platform.runLater(() -> {
                        app.goToPage1(stage);
                    });
                    klantID = ReadFID(serialPort);
                    System.out.println(klantID);


                    // Call readRFIDCard method to get the correct pin code
                    correctPinCode = retrieveCorrectPin(serialPort, connection, klantID);
                }
                // Go to pin validation screen in GUI
                try {
                    Platform.runLater(() -> {
                        app.goToPage2(stage);
                    });

                } catch (Exception e) {
                    System.out.println(e);
                }




                // Input Pin code
                userKnowsPincode = inputVerificationPin(serialPort, correctPinCode, pinCodeField);

                // Sets userKnowsPincode to true if entered pin is correct. Otherwise, returns the user to the start screen
                // Returns to start screen if user entered the incorrect pinCode 3 times in a row
                if (!userKnowsPincode) {
                    correctPinCode = null;
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
        Integer amountToPin;
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
                            amountToPin = 70;
                            boolean youCanPin70 = checkPinAmountWithDatabase(serialPort, connection, klantID, amountToPin);
                            if (youCanPin70) {
                                outChar = '1'; // Replace '1' with the character you want to send
                                byte[] sendData = new byte[] { (byte) outChar };
                                serialPort.writeBytes(sendData, sendData.length);
                                convertToBills(amountToPin, serialPort);
                                // Create new transaction entry
                                createNewTransactionEntry(serialPort, connection, klantID, amountToPin);
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
        Integer amountToPin;
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
                            amountToPin = 50;
                            System.out.println("Selected 50 euro");
                            pinningSuccessfull = checkPinAmountWithDatabase(serialPort, connection, klantID, amountToPin);
                            if (pinningSuccessfull) {
                                outChar = '1'; // Replace '1' with the character you want to send
                                byte[] sendData = new byte[] { (byte) outChar };
                                serialPort.writeBytes(sendData, sendData.length);
                                // Go to loading page
                                Platform.runLater(() -> {
                                    app.goToPage6(stage);
                                });

                                convertToBills(amountToPin, serialPort);
                                // Create new transaction entry
                                createNewTransactionEntry(serialPort, connection, klantID, amountToPin);
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
                            amountToPin = 20;
                            System.out.println("Selected 20 euro");
                            pinningSuccessfull = checkPinAmountWithDatabase(serialPort, connection, klantID, amountToPin);
                            if (pinningSuccessfull) {
                                outChar = '1'; // Replace '1' with the character you want to send
                                byte[] sendData = new byte[] { (byte) outChar };
                                serialPort.writeBytes(sendData, sendData.length);
                                // Go to loading page
                                Platform.runLater(() -> {
                                    app.goToPage6(stage);
                                });
                                convertToBills(amountToPin, serialPort);
                                // Create new transaction entry
                                createNewTransactionEntry(serialPort, connection, klantID, amountToPin);
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
                            amountToPin = 10;
                            System.out.println("Selected 10 euro");
                            pinningSuccessfull = checkPinAmountWithDatabase(serialPort, connection, klantID, amountToPin);
                            if (pinningSuccessfull) {
                                outChar = '1'; // Replace '1' with the character you want to send
                                byte[] sendData = new byte[] { (byte) outChar };
                                serialPort.writeBytes(sendData, sendData.length);
                                // Go to loading page
                                Platform.runLater(() -> {
                                    app.goToPage6(stage);
                                });
                                convertToBills(amountToPin, serialPort);
                                // Create new transaction entry
                                createNewTransactionEntry(serialPort, connection, klantID, amountToPin);
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
                            amountToPin = getCustomPinAmount(serialPort, klantID);
                            if (amountToPin != null) {

                                convertToBills(amountToPin, serialPort);
                                // Create new transaction entry
                                createNewTransactionEntry(serialPort, connection, klantID, amountToPin);
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


    private static String retrieveCorrectPin(SerialPort serialPort, Connection connection, String klantID) {
        String correctPinCode = "";
        inputStream = serialPort.getInputStream();
        char outChar;


        try {
            // Retrieve the value y from the database where x is equal to the data string
            String sql = "SELECT klantPin FROM klant WHERE klantID = ?;";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, klantID);
            ResultSet resultSet = statement.executeQuery();


            if (resultSet.next()) {
                correctPinCode = resultSet.getString("klantPin");
                System.out.println("Pincode: " + correctPinCode);

                outChar = '1'; // Replace '1' with the character you want to send
                byte[] sendData = new byte[] { (byte) outChar };
                serialPort.writeBytes(sendData, sendData.length);

                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    // TODO: handle exception
                }



                return correctPinCode;

            } else if (!resultSet.next()) {
                // Send 0 to arduino (false);
                outChar = '0'; // Replace '1' with the character you want to send
                byte[] sendData = new byte[] { (byte) outChar };
                serialPort.writeBytes(sendData, sendData.length);

                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    // TODO: handle exception
                }
                System.out.println("No data found for card: " + klantID);
            }



        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }

    private static void chooseToPrintReceipt(SerialPort serialPort, String klantID, Integer pinAmount) {
        inputStream = serialPort.getInputStream();
        byte[] receiptBuffer = new byte[1024];
        char pressedButton;
        System.out.println("Do you want a receipt?");

        try {
            Platform.runLater(() -> {
                app.goToPage7(stage);
            });
        } catch (Exception e) {
            System.out.println(e);
        }

        // Wait for dispensing to finish
        while(receivedString != "good") {
            receivedString = receiveArduinoStrings(serialPort);
            if (receivedString.equals("good")) {
                receivedString = "";
                break;
            }
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

    private static Boolean inputVerificationPin(SerialPort serialPort, String correctPinCode, PasswordField passwordField) {
        inputStream = serialPort.getInputStream();
        byte[] pinBuffer = new byte[1024];
        char pressedKey;
        int pinTries = 0;
        Boolean pinCorrect = false;
        String pinCode = "";
        char outChar;

        System.out.println("Enter your pin code");

        while (pinTries <= 2 && !pinCorrect) {
            try {
                int byteCount = inputStream.read(pinBuffer);
                if (byteCount > -1) {
                    for (int i = 0; i < byteCount; i++) {

                        // Set pressed key to data read from Serial Port
                        pressedKey = (char) pinBuffer[i];
                        // If clear key is pressed, clear entered pin code
                        if (pressedKey == '*') {
                            pinCode = "";
                            passwordField.setText(pinCode);
                        }

                        else if (pressedKey == '#') {
                            if (correctPinCode.equals(pinCode)) {
                                System.out.println("You entered the correct pin code!");


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
                                return true;
                            }
                            else {
                                System.out.println("You entered the wrong pin code :(");
                                // outChar = '0'; // Replace '1' with the character you want to send
                                // byte[] sendData = new byte[] { (byte) outChar };
                                // serialPort.writeBytes(sendData, sendData.length);

                                // Close port and reopen to be able to send data again


                                pinTries++;
                                System.out.println("You've tried " + pinTries + " times");
                                pinCode = "";
                                passwordField.setText(pinCode);
                            }


                        }

                        else if (pinCode.length() == 4) {
                            System.out.println("Press the '#' key, please");
                        }

                        else {
                            pinCode += pressedKey ;
                            System.out.println(pinCode);
                            passwordField.setText(pinCode);
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println(e);
            }
        }
        // Do something with the fact that you exceeded the 3 tries
        // Send int as byte 2 (user can't enter correct pin) to arduin



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
                String sql = "SELECT rekeningTotaal FROM rekening WHERE klant_klantID = ?;";
                PreparedStatement statement = connection.prepareStatement(sql);
                statement.setString(1, klantID);
                ResultSet resultSet = statement.executeQuery();



                Platform.runLater(() -> {
                    app.goToPage4(stage);
                });


                if (resultSet.next()) {
                    balanceField.toFront();
                    balanceField.setText(String.valueOf(resultSet.getInt("rekeningTotaal")) + " Euro");
                    System.out.println("Your current balance: " + resultSet.getInt("rekeningTotaal") + " euro");
                }

                else {
                    System.out.println("You do not currently have a banking account. Please make one on our website");
                }
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

    private static void getAndSendDataForBill(SerialPort serialPort, Connection connection, String klantID, Integer pinAmount) throws InterruptedException {
        System.out.println("Sending data for receipt");
        try {
            String sql = "SELECT rekeningID FROM rekening WHERE klant_klantID = ?;";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, klantID);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                // Wait 250 ms
                Thread.sleep(250);

                // Send rekeningID as string to arduino
                String rekeningID = Integer.toString(resultSet.getInt("rekeningID"));
                sendString(serialPort, rekeningID);

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
                String pinAmountString = Integer.toString(pinAmount);
                sendString(serialPort, pinAmountString);
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
                String sql2 = "SELECT transactieID FROM transactie ORDER BY transactieID DESC LIMIT 1;";
                PreparedStatement statement2  = connection.prepareStatement(sql2);
                ResultSet resultSet2 = statement2.executeQuery(sql2);

                if (resultSet2.next()) {
                    String transactieID = Integer.toString(resultSet2.getInt("transactieID"));
                    sendString(serialPort, transactieID);
                    while(receivedString != "good") {
                        receivedString = receiveArduinoStrings(serialPort);
                        if (receivedString.equals("good")) {
                            receivedString = "";
                            break;
                        }
                    }
                }

                System.out.println("Sent data for receipt!");
                return;
            }
        } catch (SQLException e) {
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

    private static void receiveArduinoStringsVoid(SerialPort serialPort) {
        String dummy = receiveArduinoStrings(serialPort);
    }

    private static void sendString(SerialPort serialPort, String stringToSend) {

        String message = stringToSend;
        byte[] bytes = message.getBytes();
        serialPort.writeBytes(bytes, bytes.length);
        System.out.println("Sent: " + message);

    }

    private static boolean checkPinAmountWithDatabase(SerialPort serialPort, Connection connection, String klantID, Integer amountToPin) throws SQLException{
        char outChar;
        // Get the amount the user pinned that day and the bank account total money
        String sql = "SELECT dagGepind, rekeningTotaal FROM rekening WHERE klant_klantID = ?;";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, klantID);
        ResultSet resultSet = statement.executeQuery();

        Platform.runLater(() -> {
            errorLabelp3.setText("");
            errorLabelp5.setText("");
            errorLabelp12.setText("");
        });
        // If user has a banking account
        try {
            if (resultSet.next()) {
                Integer dagGepind = resultSet.getInt("dagGepind");
                Integer rekeningTotaal = resultSet.getInt("rekeningTotaal");

                // If your selected pin amount plus the amount you already pinned that day exceeds 10.000 (The daily limit for Geldmaat)
                if (dagGepind + amountToPin > 10000) {
                    System.out.println("Please select a lower pin amount, since you are exceeding your allotted pin amount for the day");
                    Platform.runLater(() -> {
                        errorLabelp3.setText("You are exceeding your allotted pin amount for the day");
                        errorLabelp5.setText("You are exceeding your allotted pin amount for the day");
                        errorLabelp12.setText("You are exceeding your allotted pin amount for the day");
                    });
                    return false;
                }
                else if (amountToPin < 10) {
                    System.out.println("Please select a higher pin amount, " + amountToPin + " is too low of an amount to pin");
                    Platform.runLater(() -> {
                        errorLabelp5.setText("Please enter an amount that is higher than 10");
                    });
                    return false;
                }
                else if (rekeningTotaal < amountToPin) {
                    Platform.runLater(() -> {
                        errorLabelp3.setText("Please select a lower amount to pin, since your balance is too low");
                        errorLabelp5.setText("Please select a lower amount to pin, since your balance is too low");
                        errorLabelp12.setText("Please select a lower amount to pin, since your balance is too low");
                    });
                    System.out.println("Please select a lower amount to pin, since your balance is too low");

                }

                // If your selected pin amount plus the amount you already pinned that day DOES NOT exceed 10.000(The daily limit for Geldmaat)
                else {
                    // Update dagGepind in Java to then push said updated value to the database
                    Integer updatedDagGepind = dagGepind + amountToPin;
                    Integer updatedRekeningTotaal = rekeningTotaal - amountToPin;
                    String sql2 = "Update rekening SET dagGepind = ? WHERE klant_klantID = ? ;";
                    PreparedStatement statement2 = connection.prepareStatement(sql2);
                    statement2.setInt(1, updatedDagGepind);
                    statement2.setString(2, klantID);
                    statement2.executeUpdate();

                    // Update rekeningTotaal in database
                    String sql3 = "Update rekening SET rekeningTotaal = ? WHERE klant_klantID = ? ;";
                    PreparedStatement statement3 = connection.prepareStatement(sql3);
                    statement3.setInt(1, updatedRekeningTotaal);
                    statement3.setString(2, klantID);
                    statement3.executeUpdate();


                    outChar = '1'; // Replace '1' with the character you want to send
                    byte[] sendData = new byte[] { (byte) outChar };
                    serialPort.writeBytes(sendData, sendData.length);

                    // Return true, indicating that the method was successful
                    Platform.runLater(() -> {
                        errorLabelp3.setText("");
                        errorLabelp5.setText("");
                        errorLabelp12.setText("");
                    });
                    return true;

                }

            }

            // If user does not have a banking account
            else {
                System.out.println("Error. user does not have a banking account. Please make a banking account on our website");
                return false;
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return false;

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
                                boolean pinSuccessful = checkPinAmountWithDatabase(serialPort, connection, klantID, amountToPin);
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
                                    errorLabelp5.setText("Please enter an amount that is in intervals of 5");
                                });
                            }
                            else if (amountToPin < 10) {
                                System.out.println("Please enter an amount higher than 10 euro");
                                customAmountToPin = "";
                                String amountToPinGUI = customAmountToPin;
                                Platform.runLater(() -> {
                                    customAmountLabel.setText("€" + amountToPinGUI);
                                    errorLabelp5.setText("Please enter an amount higher than 10 euro");
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




    private static void convertToBills(Integer amountToDispense, SerialPort serialPort) {
        HashMap<String, Integer> eachBillAmount = new HashMap<>();
        Integer billAmount50;
        Integer billAmount20;
        Integer billAmount10;
        Integer billAmount5;

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
        try {
            Platform.runLater(() -> {
                app.goToPage7(stage);
            });
            Thread.sleep(2500);
        } catch (Exception e){
            System.out.println(e);
        }
        return;
    }






    public static String ReadFID(SerialPort serialPort) {
        StringBuilder receivedData = new StringBuilder();
        final String[] combinedString = {""};
        boolean[] isCombinedStringReady = {false};

        // Add data listener to the serial port
        serialPort.addDataListener(new SerialPortDataListener() {
            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
            }

            @Override
            public void serialEvent(SerialPortEvent event) {
                // Check if the event type is data available
                if (event.getEventType() == SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
                    // Read the available data from the serial port
                    byte[] newData = new byte[serialPort.bytesAvailable()];
                    serialPort.readBytes(newData, newData.length);

                    // Convert the received bytes to a string
                    String receivedString = new String(newData);

                    // Append the received string to the data buffer
                    receivedData.append(receivedString);

                    // Check if there are no more characters to combine
                    if (!receivedString.isEmpty() && receivedString.contains("\n")) {
                        // Remove whitespace characters and combine the received data
                        combinedString[0] = receivedData.toString().replaceAll("\\s", "");

                        // Reset the data buffer for the next combined string
                        receivedData.setLength(0);

                        // Set the flag to indicate that the combined string is ready
                        isCombinedStringReady[0] = true;
                        System.out.println("UID: " + combinedString[0]);

                        serialPort.removeDataListener();
                    }
                }
            }
        });

        // Keep the program running to receive serial data continuously
        while (true) {
            if (isCombinedStringReady[0]) {
                String result = combinedString[0];
                combinedString[0] = "";
                isCombinedStringReady[0] = false;
                return result;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static void createNewTransactionEntry(SerialPort serialPort, Connection connection, String klantID, Integer amountPinned) {
        while(true) {
            try {
                // Get rekening ID from klant
                String rekeningIDSQL = "SELECT rekeningID FROM rekening WHERE klant_klantID = ?;";
                PreparedStatement rekeningStatement = connection.prepareStatement(rekeningIDSQL);
                rekeningStatement.setString(1, klantID);
                ResultSet resultSet = rekeningStatement.executeQuery();

                if (resultSet.next()) {
                    System.out.println("Creating transaction entry in database... ");
                    Integer rekeningID = resultSet.getInt("rekeningID");
                    String transactionSQL = "INSERT INTO transactie (rekening_rekeningID, transactieHoeveelheid, transactieDate) VALUES (?, ?, ?)";
                    PreparedStatement insertStatement = connection.prepareStatement(transactionSQL);
                    insertStatement.setInt(1, rekeningID);
                    insertStatement.setInt(2, amountPinned);

                    // Create a Timestamp object for the current timestamp with milliseconds
                    Timestamp timestamp = new Timestamp(System.currentTimeMillis());


                    // The JDBC driver knows what to do with a java.sql type:
                    //insertStatement.setString(3, formattedDateTime);
                    insertStatement.setTimestamp(3, timestamp);
                    insertStatement.executeUpdate();

                    // Retrieve the generated auto-increment ID, if applicable
                    // if (affectedRows > 0) {
                    //     ResultSet generatedKeys = insertStatement.getGeneratedKeys();
                    //     if (generatedKeys.next()) {
                    //         int generatedId = generatedKeys.getInt(1);
                    //         System.out.println("New entry created with ID: " + generatedId);
                    //     }
                    // }
                    System.out.println("Transaction entry made!");

                    return;
                }
            } catch (Exception e) {
                // TODO: handle exception
                System.out.println(e);
            }
            return;
        }
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


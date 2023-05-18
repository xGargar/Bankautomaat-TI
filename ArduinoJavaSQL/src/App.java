import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import com.fazecast.jSerialComm.SerialPort;

public class App {
    static OutputStream output;
    static InputStream inputStream;
    static Connection connection;




    public static void main(String[] args) throws IOException, InterruptedException, SQLException {

        String portName = "COM13"; // Replace with the name of your serial port
        int baudRate = 9600; // Replace with the baud rate used in your Arduino sketch

        // Get the serial port object for the given port name
        SerialPort serialPort = SerialPort.getCommPort(portName);

        //serialPort.openPort();
        serialPort.setBaudRate(baudRate);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 60000, 0);


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

        

        // Connect to the SQL database
        String url = "jdbc:mysql://127.0.0.1:3306/bank";
        String username = "root";
        String password = "xb25082004";

        try {
            connection = DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            System.err.println("Error: Could not connect to database");
            e.printStackTrace();
            return;
        }


        String klantID = "";
        String correctPinCode = null;
        Boolean enteredCorrectPin = false;
        boolean done = false;
        while (!done) {
            // While user has not been verified yet
            while (!enteredCorrectPin) {
                while (correctPinCode == null) {
                    // Call method to read RFID card

                    // THIS TEMPORARILY DOESNT WORK DUE TO NOT HAVING A WORKING RFID
                    // klantID = readRFIDCard(serialPort, connection);
                    
                    klantID = "F9299DD4";

                    // Call readRFIDCard method to get the correct pin code
                    correctPinCode = retrieveCorrectPin(serialPort, connection, klantID);
                    // Output the correct pin code
                    System.out.println("Correct Pin Code: " + correctPinCode);
                }
                // Input Pin code
                enteredCorrectPin = inputPinCode(serialPort, correctPinCode);
            }

            Integer amountToPin = getCustomPinAmount(serialPort, connection, klantID);

            // Get HashMap of all bills needed for the pin amount
            HashMap<String, Integer> eachBillAmount = convertToBills(amountToPin);
        }
        
        

        // editPinCode(serialPort, connection, klantID);


        // Close the serial port and database connection
        serialPort.closePort();
        connection.close();
    }

    private static String readRFIDCard(SerialPort serialPort, Connection connection) {
        inputStream = serialPort.getInputStream();
        byte[] buffer = new byte[1024];
        String data = "";

        try {
            int cardByte = inputStream.read(buffer);

            if (cardByte > -1) {
                data += new String(buffer, 0, cardByte);
                if (data.contains("\n")) {
                    System.out.println("Data: " + data.trim());
                    String klantID = data.trim();
                    data = "";

                    // Set cardScanned boolean in Arduino to true;
                    boolean cardScanned = true;
                    serialPort.writeBytes(new byte[] {(byte) (cardScanned ? '1' : '0')}, 1);

                    return klantID;
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }

    private static String retrieveCorrectPin(SerialPort serialPort, Connection connection, String klantID) {

        boolean cardValid = false;
        String correctPinCode = "";
        inputStream = serialPort.getInputStream();
        

        try {
            // Retrieve the value y from the database where x is equal to the data string
            String sql = "SELECT klantPin FROM klant WHERE klantID = ?;";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, klantID);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                correctPinCode = resultSet.getString("klantPin");
                // System.out.println("Pincode: " + correctPinCode);

                // Set cardCorrect to true and set same boolean in Arduino to true
                cardValid = true;
                serialPort.writeBytes(new byte[] {(byte) (cardValid ? '1' : '0')}, 1);
                return correctPinCode;

            } else {
                System.out.println("No data found for card: " + klantID);
            }



        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }

    private static Boolean inputPinCode(SerialPort serialPort, String correctPinCode) {
        inputStream = serialPort.getInputStream();
        byte[] buffer = new byte[1024];
        char pressedKey;
        int pinTries = 0;
        Boolean pinCorrect = false;
        String pinCode = "";

        System.out.println("Enter your pin code");

        while (pinTries <= 2 && !pinCorrect) {
            try {
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
                            if (correctPinCode.equals(pinCode)) {
                                System.out.println("You entered the correct pin code!");
                                pinCorrect = true;
                                serialPort.writeBytes(new byte[] {(byte) (pinCorrect ? '1' : '0')}, 1);
                                return true;
                            }
                            else {
                                System.out.println("You entered the wrong pin code :(");
                                serialPort.writeBytes(new byte[] {(byte) (pinCorrect ? '1' : '0')}, 0);

                                pinTries++;
                                System.out.println("You've tried " + pinTries + " times");
                                pinCode = "";
                            }


                        }

                        else if (pinCode.length() == 4) {
                            System.out.println("Press the '#' key, please");
                        }

                        else {
                            pinCode += pressedKey ;
                            System.out.println(pinCode);
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println(e);
            }
        }
        // Do something with the fact that you exceeded the 3 tries
        return false;
    }

    private static void editPinCode(SerialPort serialPort, Connection connection, String klantID) {
        inputStream = serialPort.getInputStream();
        byte[] buffer = new byte[1024];
        char pressedKey;
        String pinCode = "";
        boolean editedPinCode = false;

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
                                serialPort.writeBytes(new byte[] {(byte) (editedPinCode ? '1' : '0')}, 1);
                            }
                            // else if (pinCode.length() > 5 );
                            // System.out.println("Invalid pin. Please re-enter pin");
                            // pinCode = "";

                        }



                        else if (pressedKey != '*' && pressedKey != '#'){
                            pinCode += pressedKey ;
                            System.out.println(pinCode);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    
    }

    private static Integer getCustomPinAmount(SerialPort serialPort, Connection connection, String klantID) {
        inputStream = serialPort.getInputStream();
        byte[] buffer = new byte[1024];
        char pressedKey;
        boolean pinnedMoney = false;
        String customAmountToPin = "";
        Integer dagGepind = 0;
        Integer rekeningTotaal = 0;
        System.out.println("Please enter custom pin amount");
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
                        }
                        
                        else if (pressedKey == '#') {
                            // Convert amount to pin to Integer
                            Integer amountToPin = Integer.valueOf(customAmountToPin);

                            if (amountToPin % 5 == 0) {
                                // Get the amount the user pinned that day and the bank account total money
                                String sql = "SELECT dagGepind, rekeningTotaal FROM rekening WHERE klant_klantID = ?;";
                                PreparedStatement statement = connection.prepareStatement(sql);
                                statement.setString(1, klantID);
                                ResultSet resultSet = statement.executeQuery();

                                // If user has a banking account
                                if (resultSet.next()) {
                                    dagGepind= resultSet.getInt("dagGepind");
                                    rekeningTotaal = resultSet.getInt("rekeningTotaal");

                                    // If your selected pin amount plus the amount you already pinned that day exceeds 10.000 (The daily limit for Geldmaat)
                                    if (dagGepind + amountToPin > 10000) {
                                        System.out.println("Please select a lower pin amount, since you are exceeding your allotted pin amount for the day");
                                        customAmountToPin = "";
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

                                        // Set boolean to true in java and arduino (This is temporary just like the rest of the instances of this code snippet)
                                        pinnedMoney = true; 
                                        serialPort.writeBytes(new byte[] {(byte) (pinnedMoney ? '1' : '0')}, 1);
                                        
                                        return amountToPin; 

                                    }

                                }

                                // If user does not have a banking account
                                else {
                                    System.out.println("Error. user does not have a banking account. Please make a banking account on our website");
                                    
                                }
                            }
                            else {
                                System.out.println("Please enter an amount that is in intervals of 5");
                                customAmountToPin = "";
                            }
                            

                        }
                        else {
                            customAmountToPin += pressedKey;
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

    private static HashMap<String, Integer> convertToBills(Integer amountToPin) {
        HashMap<String, Integer> eachBillAmount = new HashMap<>();
        Integer billAmount50; 
        Integer billAmount20;
        Integer billAmount10;
        Integer billAmount5;

        // Sets the remainder to the full amount left to pin, since no calculations have been made yet
        Integer amountLeftToCalc = amountToPin;
        System.out.println("Total amount of money: " + amountToPin);

        // Calculate how many bills of 50 are needed and grabs the remainder from the divison
        billAmount50 = amountLeftToCalc / 50;
        amountLeftToCalc %= 50;
        eachBillAmount.put("billAmount50", billAmount50);
        System.out.println("Amount of 50 Dollar bills: " +billAmount50);

        // Calculate how many bills of 20 are needed and grabs the remainder from the divison
        billAmount20 = amountLeftToCalc / 20;
        amountLeftToCalc %= 50;
        eachBillAmount.put("billAmount20", billAmount20);
        System.out.println("Amount of 20 Dollar bills: " +billAmount20);
        
        // Calculate how many bills of 10 are needed and grabs the remainder from the divison
        billAmount10 = amountLeftToCalc / 10;
        amountLeftToCalc %= 50;
        eachBillAmount.put("billAmount10", billAmount10);
        System.out.println("Amount of 10 Dollar bills: " + billAmount10);

        // Calculate how many bills of 5 are needed and grabs the remainder from the divison
        billAmount5 = amountLeftToCalc / 5;
        amountLeftToCalc %= 50;
        eachBillAmount.put("billAmount5", billAmount5);
        System.out.println("Amount of 5 Dollar bills: " + billAmount5);


        return eachBillAmount;
    }

}

       
        
   












import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

public class App {
    static OutputStream output;
    static InputStream inputStream;
    static Connection connection;
    




    public static void main(String[] args) throws IOException, InterruptedException, SQLException {

        String portName = "COM9"; // Replace with the name of your serial port
        int baudRate = 9600; // Replace with the baud rate used in your Arduino sketch

        // Get the serial port object for the given port name
        SerialPort serialPort = SerialPort.getCommPort(portName);

        // serialPort.openPort();
        serialPort.setBaudRate(baudRate);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 60000, 0);

        PrintWriter outputToArduino = new PrintWriter(serialPort.getOutputStream());


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
        boolean machineIsOn = true;
        boolean userKnowsPincode = false;



        while (machineIsOn) {
            // While user has not been verified yet
            while (!userKnowsPincode) {
                while (correctPinCode == null) {
                    // Call method to read RFID card

                    
                    System.out.println("Please scan card");
                    klantID = TestRFID(serialPort);
                    System.out.println(klantID);
                    
                    
                    // Call readRFIDCard method to get the correct pin code
                    correctPinCode = retrieveCorrectPin(serialPort, connection, klantID, outputToArduino);
                }
                // Input Pin code
                userKnowsPincode = inputVerificationPin(serialPort, correctPinCode);

                // Sets userKnowsPincode to true if entered pin is correct. Otherwise returns the user to the start screen
                // Returns to start screen if user entered the incorrect pinCode 3 times in a row
                if (!userKnowsPincode) {
                    break;
                }
                
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

                    //  Set cardScanned boolean in Arduino to true;
                    // boolean cardScanned = true;
                    // serialPort.writeBytes(new byte[] {(byte) (cardScanned ? '1' : '0')}, 1);

                    return klantID;
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }
    

     
    private static String retrieveCorrectPin(SerialPort serialPort, Connection connection, String klantID, PrintWriter outputToArduino) {
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
    
     
    private static Boolean inputVerificationPin(SerialPort serialPort, String correctPinCode) {
        inputStream = serialPort.getInputStream();
        byte[] buffer = new byte[1024];
        char pressedKey;
        int pinTries = 0;
        Boolean pinCorrect = false;
        String pinCode = "";
        char outChar;

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
                                
                                
                                try {
                                    Thread.sleep(500);
                                } catch (Exception e) {
                                    // TODO: handle exception
                                }
                                // Send char 1 (true) as byte to arduino
                                outChar = '1'; // Replace '1' with the character you want to send
                                byte[] sendData = new byte[] { (byte) outChar };
                                serialPort.writeBytes(sendData, sendData.length);

                                // Close port and reopen to be able to send data again


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
        try {
            Thread.sleep(500);
        } catch (Exception e) {
            // TODO: handle exception
        }
        // Send int as byte 2 (user can't enter correct pin) to arduin
        outChar = '2';
        byte[] sendData = new byte[] { (byte) outChar};
        serialPort.writeBytes(sendData, sendData.length);
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

                                // Close port and reopen to be able to send data again
                               
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

                                        // Close port and reopen to be able to send data again
                                  
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



    


    public static String TestRFID(SerialPort serialPort) {
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
}
    
    

       
        
   












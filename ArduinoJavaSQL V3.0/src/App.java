
import java.io.IOException;
import java.io.InputStream;

import java.io.OutputStream;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Scanner;
import java.sql.Timestamp;
import java.util.Date;
import java.text.SimpleDateFormat;


import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

public class App {
    static OutputStream output;
    static InputStream inputStream;

    static Connection connection;
    // Declare chars for buttons to send
    static char topLeftButton = 'Q';
    static char topRightButton = 'W';
    static char middleLeftButton = 'E';
    static char middleRightButton = 'R';
    static char bottomRightButton = 'T';
    static char cancelButton = 'X';
    
    // PIN MACHINE ID
    static String machineID = "420";

    static String receivedString = "";

    public static void main(String[] args) throws IOException, InterruptedException, SQLException {

        String portName = "COM9"; // Replace with the name of your serial port
        int baudRate = 9600; // Replace with the baud rate used in your Arduino sketch

        // Get the serial port object for the given port name
        SerialPort serialPort = SerialPort.getCommPort(portName);

        // serialPort.openPort();
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
        boolean machineIsOn = true;
        boolean userKnowsPincode = false;



        while (machineIsOn) {
            // While user has not been verified yet
            while (!userKnowsPincode) {
                while (correctPinCode == null) {
                    // Call method to read RFID card

                    
                    System.out.println("Please scan card");
                    klantID = ReadFID(serialPort);
                    System.out.println(klantID);
                    
                    
                    // Call readRFIDCard method to get the correct pin code
                    correctPinCode = retrieveCorrectPin(serialPort, connection, klantID);
                }
                // Input Pin code
                userKnowsPincode = inputVerificationPin(serialPort, correctPinCode);

                // Sets userKnowsPincode to true if entered pin is correct. Otherwise returns the user to the start screen
                // Returns to start screen if user entered the incorrect pinCode 3 times in a row
                if (!userKnowsPincode) {
                    correctPinCode = null;
                }
            }
            //editPinCode(serialPort, connection, klantID);

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
        connection.close();
    }

    private static boolean atmMenu(SerialPort serialPort, String klantID) {
        inputStream = serialPort.getInputStream();
        byte[] buffer = new byte[1024];
        char pressedButton;
        Integer amountToPin;
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
                                HashMap<String, Integer> eachBillAmount = convertToBills(amountToPin);
                                // Create new transaction entry
                                createNewTransactionEntry(serialPort, connection, klantID, amountToPin);
                                // Get and send data to arduino for receipt printer
                                getAndSendDataForBill(serialPort, connection, klantID, amountToPin);
                                // Restart arduino
                                serialPort.closePort();
                                try {
                                    Thread.sleep(1000);
                                } catch (Exception e) {
                                    // TODO: handle exception
                                }
                                serialPort.openPort();
                                return true;
                            }
                            
                        }
                        // If middle left button is pressed
                        else if (pressedButton == middleLeftButton) {
                            System.out.println("50 euro               20 euro\n");
                            System.out.println("10 euro               Custom amount\n");
                            System.out.println("Cancel\n");
                            // Run code for pinning money 
                            boolean pinnedSuccessful = pinMoneyMenu(serialPort, klantID);
                            if (pinnedSuccessful){
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
                        else if (pressedButton == cancelButton) {
                            System.out.println("Cancelling...");
                            // Run code for cancelling menu
                            serialPort.closePort();
                            try {
                                Thread.sleep(1000);
                            } catch (Exception e) {
                                // TODO: handle exception
                            }
                            serialPort.openPort();
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
        while (true) try {
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
                            // Send 1 (true) to arduino
                            
                            HashMap<String, Integer> eachBillAmount = convertToBills(amountToPin);
                            // Create new transaction entry
                            createNewTransactionEntry(serialPort, connection, klantID, amountToPin);
                            // Get and send data to arduino for receipt printer
                            getAndSendDataForBill(serialPort, connection, klantID, amountToPin);
                            // Restart arduino
                            serialPort.closePort();
                            try {
                                Thread.sleep(1000);
                            } catch (Exception e) {
                                // TODO: handle exception
                            }
                            serialPort.openPort();
                            return true;
                        }
                        
                    }
                    // If top right button is pressed
                    else if (pressedButton == topRightButton) {
                        amountToPin = 20;
                        System.out.println("Selected 20 euro");
                        pinningSuccessfull = checkPinAmountWithDatabase(serialPort, connection, klantID, amountToPin);
                        if (pinningSuccessfull) {
                            HashMap<String, Integer> eachBillAmount = convertToBills(amountToPin);
                            // Create new transaction entry
                            createNewTransactionEntry(serialPort, connection, klantID, amountToPin);
                            // Get and send data to arduino for receipt printer
                            getAndSendDataForBill(serialPort, connection, klantID, amountToPin);
                            // Restart arduino
                            serialPort.closePort();
                            try {
                                Thread.sleep(1000);
                            } catch (Exception e) {
                                // TODO: handle exception
                            }
                            serialPort.openPort();
                            return true;
                        }
                    }
                    // If middle left button is pressed
                    else if (pressedButton == middleLeftButton) {
                        amountToPin = 10;
                        System.out.println("Selected 10 euro");
                        pinningSuccessfull = checkPinAmountWithDatabase(serialPort, connection, klantID, amountToPin);
                        if (pinningSuccessfull) {
                            HashMap<String, Integer> eachBillAmount = convertToBills(amountToPin);
                            // Create new transaction entry
                            createNewTransactionEntry(serialPort, connection, klantID, amountToPin);
                            // Get and send data to arduino for receipt printer
                            getAndSendDataForBill(serialPort, connection, klantID, amountToPin);
                            // Restart arduino
                            serialPort.closePort();
                            try {
                                Thread.sleep(1000);
                            } catch (Exception e) {
                                // TODO: handle exception
                            }
                            serialPort.openPort();
                            return true;
                        }
                    }

                    else if (pressedButton == middleRightButton) {
                        System.out.println("You want to enter a custom amount");
                        amountToPin = getCustomPinAmount(serialPort, klantID);
                        if (amountToPin != null) {
                            HashMap<String, Integer> eachBillAmount = convertToBills(amountToPin);
                            // Create new transaction entry
                            createNewTransactionEntry(serialPort, connection, klantID, amountToPin);
                            // Get and send data to arduino for receipt printer
                            getAndSendDataForBill(serialPort, connection, klantID, amountToPin);
                            // Restart arduino
                            serialPort.closePort();
                            try {
                                Thread.sleep(1000);
                            } catch (Exception e) {
                                // TODO: handle exception
                            }
                            serialPort.openPort();
                            return true;
                        }
                        else if (amountToPin == null) {
                            System.out.println("exited custom pinning");
                        }
                        
                    }
                    
                    // If cancel button is pressed
                    else if (pressedButton == cancelButton) {
                        // Run code for cancelling menu
                        System.out.println("Cancelling...");
                        return false;
                    }

                }
            }
        } catch (Exception e) {
            System.out.println(e);
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

    private static Boolean inputVerificationPin(SerialPort serialPort, String correctPinCode) {
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

                if (resultSet.next()) {
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
                            if (pressedButton == cancelButton) {
                                System.out.println("Cancelling...");

                                // wait for a sec
                                try {
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


        // If user has a banking account
        try {
            if (resultSet.next()) {
                Integer dagGepind = resultSet.getInt("dagGepind");
                Integer rekeningTotaal = resultSet.getInt("rekeningTotaal");
    
                // If your selected pin amount plus the amount you already pinned that day exceeds 10.000 (The daily limit for Geldmaat)
                if (dagGepind + amountToPin > 10000) {
                    System.out.println("Please select a lower pin amount, since you are exceeding your allotted pin amount for the day");
                    return false;
                }
                else if (amountToPin < 10) {
                    System.out.println("Please select a higher pin amount, " + amountToPin + " is too low of an amount to pin");
                    return false;
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
                            boolean pinSuccessful = checkPinAmountWithDatabase(serialPort, connection, klantID, amountToPin);
                            if (pinSuccessful) {
                                return amountToPin;
                            }
                               
                            }
                            else {
                                System.out.println("Please enter an amount that is in intervals of 5");
                                customAmountToPin = "";
                            }
                            

                        }
                        else if (pressedKey == cancelButton){
                            System.out.println("Cancelling entering custom pin");
                            return null;
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
     
    
    
    
    private static HashMap<String, Integer> convertToBills(Integer amountToDispense) {
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


        return eachBillAmount;
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
    
    

       
        
   












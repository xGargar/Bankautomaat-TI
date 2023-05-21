import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

public class Test {
    
    static InputStream inputStream;
    static Connection connection;

    public static void main(String[] args) throws IOException {
        String portName = "COM13"; // Replace with the name of your serial port
        int baudRate = 9600; // Match the baud rate with Arduino's baud rate

        // Get the serial port object for the given port name
        SerialPort serialPort = SerialPort.getCommPort(portName);
        serialPort.setBaudRate(baudRate);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 60000, 0);

        PrintWriter outputToArduino = new PrintWriter(serialPort.getOutputStream());

        OutputStream output = serialPort.getOutputStream();

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

        

        serialPort.openPort();
        // Open the serial port
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
        
        }


        boolean machineIsOn = true;

        while (machineIsOn) {
            //inputPinTest(serialPort);
            // String testString = readRFIDCardTest(serialPort, stringifyRFIDHex);
            // System.out.println("Teststring: " + testString);
            String testString = TestRFID(serialPort);
            // try {
            //     Thread.sleep(1000);
            // } catch (InterruptedException e) {
            //     e.printStackTrace();
            // }

            // if (testString != null);{
            //     char ch = '9'; // Replace '1' with the character you want to send
            //     byte[] sendData = new byte[] { (byte) ch };
            //     serialPort.writeBytes(sendData, sendData.length);
            //     System.out.println("Sent character: " + ch);
            // }

            String correctPin = retrieveCorrectPin(serialPort, connection, testString, output);
        }
               
    }
        public static void inputPinTest(SerialPort serialPort) {
        // Read data from the serial port
        inputStream = serialPort.getInputStream();
        byte[] buffer = new byte[1];
        int len;
        try {
            while (true) {
                
                len = inputStream.read(buffer);
                
                // Create a new String object using the received data
                // Only use the portion of the buffer that contains the received data (0 to len-1 indices)
                String data = new String(buffer, 0, len);

                char inputKey = data.charAt(0);
                
                // Process the received data from Arduino
                System.out.println("Received data: " + inputKey);

                
                
                if (data.trim().equals("*")) {
                    // Stop the loop if the received data equals "*"
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Error: Could not read from serial port");
            e.printStackTrace();
            
        } finally {
            // Close the serial port
            serialPort.closePort();
        }
        
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
    private static String retrieveCorrectPin(SerialPort serialPort, Connection connection, String klantID, OutputStream output) {
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

                // Send 1 to arduino (true)
                
                // Send char 1 (true) as byte to arduino
                outChar = '1'; // Replace '1' with the character you want to send
                byte[] sendData = new byte[] { (byte) outChar };
                serialPort.writeBytes(sendData, sendData.length);
                
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    // TODO: handle exception
                }

                System.out.println("sent char " + outChar);
                
                
                // Close port and reopen to be able to send data again
                // serialPort.closePort();
                // serialPort.openPort();

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
                System.out.println("Sent char: " + outChar);
                // serialPort.closePort();
                // serialPort.openPort();
                System.out.println("No data found for card: " + klantID);
            }



        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }
}

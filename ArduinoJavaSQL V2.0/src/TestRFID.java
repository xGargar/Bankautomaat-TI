import com.fazecast.jSerialComm.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serial;
import java.sql.Connection;
import java.util.concurrent.CountDownLatch;

public class TestRFID { 
    static OutputStream output;
    static InputStream inputStream;
    static Connection connection;
    
    
    public static void main(String[] args) {
        String portName = "COM13"; // Replace with the name of your serial port
        int baudRate = 9600; // Replace with the baud rate used in your Arduino sketch

        // Get the serial port object for the given port name
        SerialPort serialPort = SerialPort.getCommPort(portName);

        // serialPort.openPort();
        serialPort.setBaudRate(baudRate);
        serialPort.setComPortParameters(baudRate, 8, 1, SerialPort.NO_PARITY);
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

        

        // Call the method to setup data listener and process received data
        while(true) {
            String result = setupDataListenerAndProcessData(serialPort);
            System.out.println("Combined String: " + result);
        }
        
    }

    public static String setupDataListenerAndProcessData(SerialPort serialPort) {
        
            StringBuilder receivedData = new StringBuilder();
            StringBuilder combinedString = new StringBuilder();

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
                            combinedString.append(receivedData.toString().replaceAll("\\s", ""));

                            // Reset the data buffer for the next combined string
                            receivedData.setLength(0);
                        }
                    }
                }
            });

            // Wait for a short duration to allow some data to be received
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Retrieve the combined string and close the serial port
            String result = combinedString.toString();
            serialPort.closePort();
            serialPort.openPort();
            return result;
    }
}

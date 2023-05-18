import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import com.fazecast.jSerialComm.SerialPort;

public class Test {
    static OutputStream output;
    static InputStream input;

    public static void main(String[] args) throws IOException {
        String portName = "COM13"; // Replace with the name of your serial port
        int baudRate = 9600; // Match the baud rate with Arduino's baud rate

        // Get the serial port object for the given port name
        SerialPort serialPort = SerialPort.getCommPort(portName);
        serialPort.setBaudRate(baudRate);

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
            return;
        }

        // Read data from the serial port
        input = serialPort.getInputStream();
        byte[] buffer = new byte[1024];
        int len;
        try {
            while (true) {
                if (input.available() > 0) {
                    // Read the data from the input stream into the buffer
                    len = input.read(buffer);
                    
                    // Create a new String object using the received data
                    // Only use the portion of the buffer that contains the received data (0 to len-1 indices)
                    String data = new String(buffer, 0, len);
                    
                    // Process the received data from Arduino
                    System.out.println("Received data: " + data.trim());
                }
            }
        } catch (IOException e) {
            System.err.println("Error: Could not read from serial port");
            e.printStackTrace();
            return;
        }
    }
}


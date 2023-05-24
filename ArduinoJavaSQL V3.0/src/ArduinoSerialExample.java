import com.fazecast.jSerialComm.*;

public class ArduinoSerialExample {
    public static void main(String[] args) {
        SerialPort serialPort = SerialPort.getCommPort("COM13"); // Replace "COM3" with your Arduino's port
        serialPort.openPort();
        serialPort.setComPortParameters(9600, 8, 1, SerialPort.NO_PARITY);
        
        try {
            // Wait for Arduino to reset
            Thread.sleep(2000);
            
            // Send character to Arduino
            char ch = '9'; // Replace '1' with the character you want to send
            byte[] sendData = new byte[] { (byte) ch };
            serialPort.writeBytes(sendData, sendData.length);
            System.out.println("Sent character: " + ch);
            
            // Read ASCII value from Arduino
            StringBuilder receivedData = new StringBuilder();
            while (true) {
                if (serialPort.bytesAvailable() > 0) {
                    byte[] readBuffer = new byte[serialPort.bytesAvailable()];
                    int numBytes = serialPort.readBytes(readBuffer, readBuffer.length);
                    
                    // Convert the received bytes to a string
                    String receivedValue = new String(readBuffer);
                    
                    // Append received data to the string builder
                    receivedData.append(receivedValue);
                    
                    // Break the loop if all expected data has been received
                    if (receivedData.length() >= sendData.length) {
                        break;
                    }
                }
            }
            
            // Remove any non-digit characters from the received data
            String cleanData = receivedData.toString().replaceAll("[^0-9]", "");
            
            // Parse the integer value
            int receivedValue = Integer.parseInt(cleanData);
            System.out.println("Received integer value: " + receivedValue);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            serialPort.closePort();
        }
    }
}

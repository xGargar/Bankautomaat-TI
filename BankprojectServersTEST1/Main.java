import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Main {
    public static void main(String[] args) {
        try {
            // Specify the URL of the Node.js server
            URL url = new URL("http://localhost:8080/api/getBalance");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Request method is POST
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);

            // Set the request headers
            connection.setRequestProperty("Content-Type", "application/json");

            // Prepare the JSON data to send
            String jsonData = "{\"accountName\": \"Xander\", \"balance\": 25000}";

            // Send the data
            DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
            outputStream.writeBytes(jsonData);
            outputStream.flush();
            outputStream.close();

            // Get the response from the server bankserver
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

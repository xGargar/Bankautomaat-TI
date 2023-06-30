package com.example.guidesign;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServerController {

    //If false then withdrawal denied, true withdraw accepted
    public static boolean withdrawFunction(String acctNo, String pin, String amountToPin){
        try {
            // Create connection
            int amount = (int) Integer.parseInt(amountToPin);
            String bankServerURL = "http://145.24.222.194:8888/api/withdraw";
            URL url = new URL(bankServerURL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            System.out.println(acctNo + pin + amount);

            // Create JSON request body
            String jsonRequestBody = String.format("{\"acctNo\":\"%s\",\"pin\":\"%s\",\"amount\":%s}", acctNo, pin, amount);

            // Send request
            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(jsonRequestBody.getBytes());
            outputStream.flush();

            // Get response
            int responseCode = connection.getResponseCode();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // Check response
            if (responseCode == 200) {
                // Withdrawal successful
                System.out.println("Withdrawal successful");
                System.out.println("Response Body: " + response.toString());
                Pattern pattern = Pattern.compile("\"success\":(true|false)");
                Matcher matcher = pattern.matcher(response.toString());

                if (matcher.find()) {
                    String success = matcher.group(1);
                    boolean isSuccess = Boolean.parseBoolean(success);
                    System.out.println("Success: " + isSuccess);
                    return true;
                } else {
                    System.out.println("No success value found in the input string.");
                    return false;
                }
            } else if (responseCode == 400) {
                // Insufficient balance or other error
                System.out.println("Withdrawal failed: " + response.toString());
            } else {
                // Other error occurred
                System.out.println("An error occurred");
            }

            // Close connection
            connection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    // -1
    public static int getBalance(String acctNo, String pin){
        try {
            // Create connection
            String bankServerURL = "http://145.24.222.194:8888/api/balance";
            URL url = new URL(bankServerURL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // Create JSON request body
            String jsonRequestBody = String.format("{\"acctNo\":\"%s\",\"pin\":\"%s\"}", acctNo, pin);

            // Send request
            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(jsonRequestBody.getBytes());
            outputStream.flush();

            // Get response
            int responseCode = connection.getResponseCode();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // Check response
            if (responseCode == 200) {
                // Account exists and PIN is correct
                System.out.println("Account Number: " + acctNo);
                System.out.println("Response Body: " + response.toString());
                Pattern pattern = Pattern.compile("\"balance\":(\\d+)");
                Matcher matcher = pattern.matcher(response.toString());
                if (matcher.find()) {
                    String balance = matcher.group(1);
                    int numericBalance = Integer.parseInt(balance);
                    if (numericBalance >= 0){
                        System.out.println("Balance: " + numericBalance);
                        return numericBalance;
                    }
                    else {
                        System.out.println("No balance found in the input string.");
                        return -1;
                    }
                }

            } else if (responseCode == 404) {
                // Account not found or PIN is incorrect
                System.out.println("Account not found or PIN is incorrect");
            } else {
                // Other error occurred
                System.out.println("An error occurred");
            }

            // Close connection
            connection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }
}


#include <SPI.h>
#include <MFRC522.h>
#include <Keypad.h>
#include <stdlib.h>
#include "Adafruit_Thermal.h"

#define SS_PIN 53
#define RST_PIN 5

#define TX_PIN 14 // Arduino transmit  YELLOW WIRE  labeled RX on printer
#define RX_PIN 15 // Arduino receive   GREEN WIRE   labeled TX on printer

#include "SoftwareSerial.h"
SoftwareSerial mySerial(RX_PIN, TX_PIN); // Declare SoftwareSerial obj first
Adafruit_Thermal printer(&mySerial);     // Pass addr to printer constructor


// Define static variables to save Data into to print on receipt

static String moneyPinned = "";
static String dateTime = "";
static String iban = "";
static String transactionID = "";
static String machineID = "420";



#define BUTTON_TOPLEFT 8
#define BUTTON_TOPRIGHT 9
#define BUTTON_MIDDLELEFT 10
#define BUTTON_MIDDLERIGHT 11
#define BUTTON_BOTTOMLEFT 12
#define BUTTON_BOTTOMRIGHT 13

MFRC522 rfid(SS_PIN, RST_PIN);

const int ROW_NUM = 4; //four rows
const int COLUMN_NUM = 3; //three columns

// Format is (Enable pin1, Enable pin2, motor1Pin1, motor1Pin2, motor2Pin1, motor2Pin2)
static int pinsBill50[6] = {30, 31, 33, 35, 32, 34};
static int pinsBill20[6] = {40, 41, 43, 45, 42, 44};
static int pinsBill10[6] = {16, 17, 18, 19, 20, 21};
static int pinsBill5[6] = {6, 7, 46, 47, 48, 49};


bool cardValid = false;
bool cardScanned = false;
int pinCorrect;
bool editedPin = false;
int validCustomPinAmount = false;
bool done = false;
bool userVerified = false;

// Declare chars for buttons to send
char topLeftButton = 'Q';
char topRightButton = 'W';
char middleLeftButton = 'E';
char middleRightButton = 'R';
char bottomRightButton = 'T';
char bottomLeftButton = 'X';

char keys[ROW_NUM][COLUMN_NUM] = {
  {'1','2','3'},
  {'4','5','6'},
  {'7','8','9'},
  {'*','0','#'}
};

byte pin_rows[ROW_NUM] = {22, 23, 24, 25}; //connect to the row pinouts of the keypad
byte pin_column[COLUMN_NUM] = {26, 27, 28}; //connect to the column pinouts of the keypad

Keypad keypad = Keypad( makeKeymap(keys), pin_rows, pin_column, ROW_NUM, COLUMN_NUM );

void setup() {

  // This line is for compatibility with the Adafruit IotP project pack,
  // which uses pin 7 as a spare grounding point.  You only need this if
  // wired up the same way (w/3-pin header into pins 5/6/7):
  pinMode(7, OUTPUT); digitalWrite(7, LOW);
  
  // start up serial port, spi and rfid shit
  Serial.begin(9600);
  SPI.begin();
  rfid.PCD_Init();

  // Declare the menu buttons and led 
  pinMode(4, OUTPUT);
  
  pinMode(BUTTON_TOPLEFT, INPUT);
  pinMode(BUTTON_TOPRIGHT, INPUT);
  pinMode(BUTTON_MIDDLELEFT, INPUT);
  pinMode(BUTTON_MIDDLERIGHT, INPUT);
  pinMode(BUTTON_BOTTOMRIGHT, INPUT);
  pinMode(BUTTON_BOTTOMLEFT, INPUT);

  

  // Declare motor pins for dispenser as output
  
  // pinMode(dispenser5, OUTPUT);
  // pinMode(dispenser10, OUTPUT);
  // pinMode(dispenser20, OUTPUT);
  // pinMode(dispenser50, OUTPUT);

}

// This code is for receiving the data from the arduino

int readData() {
  //blin(6);
  int intValue = -1;
  if (Serial.available()) {
    char receivedChar = Serial.read();
    intValue = atoi(&receivedChar); // Convert character to integer

  }
  //Serial.println(intValue);
  return intValue;
}

void blink(int blinks) {
  for (int i = 0; i < blinks; i++) {
    digitalWrite(4, HIGH);
    delay(500);
    digitalWrite(4, LOW);
    delay(500);
  }
}

void rfidScan() {
  cardValid = false;
  cardScanned = false;
  while (!cardValid){
    scanLoop: 
    while (!cardScanned) {
      if (rfid.PICC_IsNewCardPresent()) { // new tag is available
        if (rfid.PICC_ReadCardSerial()) { // NUID has been readed
          MFRC522::PICC_Type piccType = rfid.PICC_GetType(rfid.uid.sak);
          //Serial.print("RFID/NFC Tag Type: ");
          //Serial.println(rfid.PICC_GetTypeName(piccType));
          // Create a variable to store the HEX string
          String hexString = "";
          char hexChar = "";

          // Build the HEX string
          for (int i = 0; i < rfid.uid.size; i++) {
            hexString += (rfid.uid.uidByte[i] < 0x10 ? "0" : "");
            hexString += String(rfid.uid.uidByte[i], HEX);
          }
      
          hexString.toUpperCase();

          Serial.print(hexString);
          Serial.println();
          rfid.PICC_HaltA(); // halt PICC
          rfid.PCD_StopCrypto1(); // stop encryption on PCD
          cardScanned = true;
          break;
        }
      }
    }
  
    while (true) {
      int returnedInt = readData();

      if (returnedInt == 0) {
        cardScanned = false;
        goto scanLoop;
      }
      else if (returnedInt == 1) {
        cardValid = true;
        goto cardIsGood;
        
      }
      else {
        //blin(returnedInt);
      }
        
    
    }
    
  }
  // Point to jump to once user is confirmed to exist in database

  cardIsGood:
  return;
}

int keypadVerificationSerialPort() {
  int verificationKeypadStatus = -1;
  // 0 is in this case another way of saying "false"
  while(verificationKeypadStatus == -1) {
    sendKeypadInput();
    int verificationKeypadStatus = readData();
    // If the int return indicates a non-false statement
    if (verificationKeypadStatus != -1) {
      return verificationKeypadStatus;
    }
  }
}

int  cancelableKeypadSerialPort() {
  int keypadStatus = 0;
  // Run until either cancel button is pressed or until corresponding java method has succeeded
  while(true) {
    sendKeypadInput();
    keypadStatus = readData();
    // If the int return indicates a non-false statement
    if (keypadStatus == 1) {
      goto endOfKeypadMiscSerialPort;
    }
    else if (digitalRead(BUTTON_BOTTOMLEFT)) {
      Serial.write(bottomLeftButton);
      delay(1000);
      goto endOfKeypadMiscSerialPort;
    }
  }
  endOfKeypadMiscSerialPort:
  return keypadStatus;
}


void sendKeypadInput() {
  char inputKey = keypad.getKey();
  if (inputKey) {
    Serial.write(inputKey);
  }
}

/*
void readCardData() {
  cardValid = false;
  cardScanned = false;
  while (!cardValid){
    scanLoop: 
    while (!cardScanned) {
      if (rfid.PICC_IsNewCardPresent() && rfid.PICC_ReadCardSerial()) {
        MFRC522::MIFARE_Key key;
        for (byte i = 0; i < 6; i++) key.keyByte[i] = 0xFF;

        //some variables we need
        String data = "";
        byte block;
        byte len;
        MFRC522::StatusCode status;

        byte buffer1[18];

        block = 4;
        len = 18;

        //------------------------------------------- GET FIRST NAME
        status = rfid.PCD_Authenticate(MFRC522::PICC_CMD_MF_AUTH_KEY_A, 4, &key, &(rfid.uid)); //line 834 of MFRC522.cpp file

        for (uint8_t i = 0; i < 16; i++)
        {
          if (buffer1[i] != 32)
          {
            data += (char) buffer1[i];
            Serial.println(data);
          }
        }
      //return false;  // Return false if cardValid is never set to true
      Serial.println("\n" + data);
      cardScanned = true;
      }
    }
    while (true) {
      int returnedInt = readData();

      if (returnedInt == 0) {
        cardScanned = false;
        goto scanLoop;
      }
      else if (returnedInt == 1) {
        cardValid = true;
        goto cardIsGood;
        
      }    
    }
  }
  cardIsGood:
  return;
}
*/

void readCardData() {
  scanLoop:
  while(true) {
    if (rfid.PICC_IsNewCardPresent() && rfid.PICC_ReadCardSerial()) {

    String data = "";
    MFRC522::MIFARE_Key key;
    for (byte i = 0; i < 6; i++) key.keyByte[i] = 0xFF;

    //some variables we need
    byte block;
    byte len;
    MFRC522::StatusCode status;

    byte buffer1[18];

    block = 4;
    len = 18;

    //------------------------------------------- GET FIRST NAME
    status = rfid.PCD_Authenticate(MFRC522::PICC_CMD_MF_AUTH_KEY_A, 4, &key, &(rfid.uid)); //line 834 of MFRC522.cpp file
    if (status != MFRC522::STATUS_OK) {
      Serial.print(F("Authentication failed: "));
      Serial.println(rfid.GetStatusCodeName(status));
      return;
    }

    status = rfid.MIFARE_Read(block, buffer1, &len);
    if (status != MFRC522::STATUS_OK) {
      Serial.print(F("Reading failed: "));
      Serial.println(rfid.GetStatusCodeName(status));
      return;
    }

    for (uint8_t i = 0; i < 16; i++)
    {
      if (buffer1[i] != 32)
      {
        data += (char) buffer1[i];
      }
    }
    //return false;  // Return false if cardValid is never set to true
    Serial.println(data);
    goto endOfScanCard;
    }
  }
  endOfScanCard:
  while (true) {
    int returnedInt = readData();

    if (returnedInt == 0) {
      cardScanned = false;
      goto scanLoop;
    }
    else if (returnedInt == 1) {
      cardValid = true;
      goto cardIsGood;
      
    }    
  }
  
  cardIsGood:
  return;
}


void atmMenu(){
  while(true) {
    beginOfAtmMenu:
    // If top left button is pressed
    if (digitalRead(BUTTON_TOPLEFT)) {
      // CODE FOR CHECKING ACCOUNT BALANCE
      Serial.write(topLeftButton);
      delay(1000);
      waitForReturnButton();
    }
    // If top right button is pressed
    if(digitalRead(BUTTON_TOPRIGHT)) {
   
      // CODE FOR FAST PIN OPTION OF 70 EURO
      Serial.write(topRightButton);
      
      delay(1000);
    
      // Wait to see what char java returns
      while(true) {
        int javaReturnState = readData();

        if (javaReturnState == 1) {
          getAndDispenseMoney();
          chooseToPrintReceipt();
          // Wait, cuz arduino is gonna reset cuz end of pin thingy
          while(true) {}
        }
        else if (javaReturnState == 0) {
          goto beginOfAtmMenu;
        }

      }
    }
      
    // If middle left button is pressed 
    else if (digitalRead(BUTTON_MIDDLELEFT)) {

      // CODE FOR PINNING MONEY 
      Serial.write(middleLeftButton);
      delay(1500);
      bool pinningCanceled = pinMoneyMenu();
      if (pinningCanceled) {
        goto beginOfAtmMenu;
      }
    }
    
    // If cancel button is pressed
    else if(digitalRead(BUTTON_BOTTOMLEFT)) {
      // CODE FOR GOING BACK TO START
      Serial.write(bottomLeftButton);
      delay(1000);
      // Wait cuz shit is gonna be cancelled
      while(true) {}
        
    }
  }
}
  
bool pinMoneyMenu() {
  backToLoopPinMoneyMenu:
  int actionValid; 
  while(true) {
    //If you want 50 euro
    if (digitalRead(BUTTON_TOPLEFT)) {
      while(true) {
        if (digitalRead(BUTTON_TOPLEFT) == LOW) {
          Serial.write(topLeftButton);
          delay(1000);
          actionValid = readData();
          if (actionValid == 1) {
            getAndDispenseMoney();
            chooseToPrintReceipt();
            // getDataForReceipt();
            // Wait, cuz arduino boutta reset
            while(true) {}
          }
          else if (actionValid == 0) {
            goto backToLoopPinMoneyMenu;
          }
        }
      }
      
    }
    //If you want 20 Euro
    else if (digitalRead(BUTTON_TOPRIGHT) == HIGH) {
      while(true) {
        if (digitalRead(BUTTON_TOPRIGHT) == LOW) {
          Serial.write(topRightButton);
          delay(1000);
          actionValid = readData();
          if (actionValid == 1) {
            getAndDispenseMoney();
            chooseToPrintReceipt();
            // getDataForReceipt();
            // Wait, cuz arduino boutta reset
            while(true) {}
          }
          else if (actionValid == 0) {
            goto backToLoopPinMoneyMenu;
          }
        }
      }
      
    }
    //If you want 10 euro
    else if(digitalRead(BUTTON_MIDDLELEFT) == HIGH) {
      Serial.write(middleLeftButton);
      delay(1000);
      // blin(5);
      actionValid = readData();
      if (actionValid == 1) {
        getAndDispenseMoney();
        chooseToPrintReceipt();
        // getDataForReceipt();
        // Wait, cuz arduino boutta reset
        while(true) {}
      }
      else if (actionValid == 0) {
        goto backToLoopPinMoneyMenu;
      }
      
    }
    // if you want custom amount euro
    else if(digitalRead(BUTTON_MIDDLERIGHT) == HIGH) {
      Serial.write(middleRightButton);
      delay(1000);
      int stateOfKeypadMethod = cancelableKeypadSerialPort();
      
      if (stateOfKeypadMethod == 1) {
        getAndDispenseMoney();
        chooseToPrintReceipt();
        // getDataForReceipt();

        // Wait, cuz arduino is gonna reset cuz end of pin thingy
        while(true) {}
      }
      // If cancel button was pressed
      else {
        delay(1000);
        goto backToLoopPinMoneyMenu;
      }
    }
    else if (digitalRead(BUTTON_BOTTOMLEFT)) {
      goto endOfPinMoneyMenu;
    }
  }
  endOfPinMoneyMenu:
  return true;
}

void waitForReturnButton() {
  // blin(2);
  while(true) {
    // Cancel seeing balance if cancel button is pressed and go back to menu
    if (digitalRead(BUTTON_BOTTOMLEFT) == HIGH) {
      while(true) {
        Serial.write(bottomLeftButton);
        delay(1000);
        goto endOfWaitForReturnButton;
      }
    }
  }
  endOfWaitForReturnButton:
  return;
}

void loop() {
  cardScanned = false;
  pinCorrect = 0;
  editedPin = false;
  validCustomPinAmount = 0;
  done = false;
  userVerified = false;

  

  // Loop of whole process
  while(!done) {
    while (!userVerified) {
      //rfidScan();
      readCardData();

      
      // runs code for putting in pin and checking if users inputted pincode is valid
      pinCorrect = keypadVerificationSerialPort();

      
      // Loops back to card scanning if method returns 0 (user is not verified yet)
      if (pinCorrect == 0) {
        cardValid = false;
      }
      // Moves on to rest of the ATM methods if method returns 1 (user has been verified)
      else if (pinCorrect == 1) {
        userVerified = true;
      }
      // Resets all changed variables and return to beginning of verification
      else if (pinCorrect == 2) {
        pinCorrect = 0;
        cardValid = false;
      }
    }
    mainMenu:
    //while(true) { int dummy = keypadVerificationSerialPort();}
    
    // waitForReturnButtonToBePressed();
    // Call general menu method
    atmMenu();
  } 
}

void getDataForReceipt() {
  bool receivedAccountID = false;
  bool receivedMoneyPinned = false;
  bool receivedMachineID = false;
  bool receivedDateTime =  false;
  bool receivedTransactionID = false;
  
  while(true) {
    
    while(true) {
      if (Serial.available() > 0) {
        iban = Serial.readString(); // Read the incoming serial data
        receivedAccountID = true;
        Serial.println("good");
        goto getMoneyPinned;
      }
    }
    getMoneyPinned:
    while(true) {
      if (Serial.available() > 0) {
        moneyPinned = Serial.readString();
        receivedMoneyPinned = true;
        Serial.println("good");
        goto getMachineID;
      }
    }
    getMachineID:
    while(true) {
      if (Serial.available() > 0) {
        machineID = Serial.readString();
        receivedMachineID = true;
        Serial.println("good");
        goto getDateTime;
      }
    }
    getDateTime:
    while(true) {
        dateTime = Serial.readString();
        receivedDateTime = true;
        Serial.println("good");
        goto getTransactionID;
    }
    getTransactionID:
    while(true) {
      transactionID = Serial.readString();
      receivedTransactionID = true;
      Serial.println("good");
      goto reachEndOfMethod;
    }
    reachEndOfMethod:
    if (receivedAccountID == true && receivedMoneyPinned == true && receivedMachineID == true && receivedDateTime ==  true) {
      goto endOfReadString;
    }
  }
  endOfReadString:
  return;
}
// Code for obfuscating Account ID
String replaceFirstCharsWithAsterisk(String inputString) {
  int numCharsToReplace = iban.length() - 4;
  String outputString = "";

  for (size_t i = 0; i < inputString.length(); i++) {
    if (i < numCharsToReplace) {
      outputString += '*';
    } else {
      outputString += inputString.charAt(i);
    }
  }

  return outputString;
}

void chooseToPrintReceipt() {
  while(true) {
    // If you want to not print a receipt
    if (digitalRead(BUTTON_BOTTOMRIGHT)) {
      delay(1000);
      Serial.println(bottomRightButton);
      goto endOfChoosingToPrintReceipt;
    }

    // If you want to print a receipt
    else if (digitalRead(BUTTON_BOTTOMLEFT)) {
      delay(1000);
      Serial.println(bottomLeftButton);
      getDataForReceipt();
      delay(250);
      printReceipt();
      while(true) {}
    }
  }
  endOfChoosingToPrintReceipt:
  return;
}

void getAndDispenseMoney() {
  int billAmount50 = -1;
  int billAmount20 = -1;
  int billAmount10 = -1;
  int billAmount5 = -1;
  

  while(true) {
    
    while(true) {
      if (Serial.available() > 0) {
        billAmount50 = Serial.readString().toInt(); // Read the incoming serial data
        Serial.println("good");
        goto get20Bills;
      }
    }
    get20Bills:
    while(true) {
      if (Serial.available() > 0) {
        billAmount20 = Serial.readString().toInt();
        Serial.println("good");
        goto get10Bills;
      }
    }
    get10Bills:
    while(true) {
      if (Serial.available() > 0) {
        billAmount10 = Serial.readString().toInt();
        Serial.println("good");
        goto get5Bills;
      }
    }
    get5Bills:
    while(true) {
        billAmount5 = Serial.readString().toInt();
        Serial.println("good");
        goto finalCheck;
    }
    finalCheck:
    if (billAmount50 != -1 && billAmount20 != -1 && billAmount10 != -1 && billAmount5 != -1) {
      goto dispenseMoney;
    }
  }
  dispenseMoney:
  
  dispenseMoney(pinsBill50[6], billAmount50);
  dispenseMoney(pinsBill20[6], billAmount20);

  

  
  blink(4);
  Serial.println("good");
  
  return;
}

void dispenseMoney(int motorPins[], int billAmount) {
  for (int i = 0; i < billAmount; i++) {
    analogWrite(motorPins[0], 25);  // Adjust speed for motor 2
    analogWrite(motorPins[1], 25);  // Adjust speed for motor 2
    // Turn on motors
    digitalWrite(motorPins[0], HIGH);
    digitalWrite(motorPins[2], HIGH);
    digitalWrite(motorPins[3], LOW);
    digitalWrite(motorPins[1], HIGH);
    digitalWrite(motorPins[4], HIGH);
    digitalWrite(motorPins[5], LOW);
    
    delay(1000);  // Delay for 500 milliseconds
    
    // Turn off motors
    digitalWrite(motorPins[1], LOW);

    delay(2000);

    digitalWrite(motorPins[0], LOW);
    
    delay(400);  // Delay for 1 second before repeating
  }
}



void printReceipt() {
  
  String modifiedAccountID = replaceFirstCharsWithAsterisk(iban);

  // This line is for compatibility with the Adafruit IotP project pack,
  // which uses pin 7 as a spare grounding point.  You only need this if
  // wired up the same way (w/3-pin header into pins 5/6/7):
  pinMode(7, OUTPUT); digitalWrite(7, LOW);

  // NOTE: SOME PRINTERS NEED 9600 BAUD instead of 19200, check test page.
  mySerial.begin(9600);  // Initialize SoftwareSerial
  //Serial1.begin(19200); // Use this instead if using hardware serial
  printer.begin();        // Init printer (same regardless of serial type)

  // The following calls are in setup(), but don't *need* to be.  Use them
  // anywhere!  They're just here so they run one time and are not printed
  // over and over (which would happen if they were in loop() instead).
  // Some functions will feed a line when called, this is normal.

  // Test character double-height on & off
  printer.justify('C');
  printer.doubleHeightOn();
  printer.println(F("PinChilling"));
  printer.doubleHeightOff();
  //printer.println("--------------------------------");
  printer.println("================================");
  
  printer.println();
  printer.println();

  printer.println("Date and time: " + dateTime);
  printer.println();
  printer.println("Machine ID: " + machineID);
  printer.println();
  printer.println("Transcaction ID: " + transactionID);
  printer.println();
  printer.println("Amount pinned: " + moneyPinned + " Euro");
  printer.println();
  printer.println("Account ID: " + modifiedAccountID);
  printer.println();
  printer.println();
  printer.println();
  printer.println();
  printer.println();
  

  printer.sleep();      // Tell printer to sleep
  delay(3000L);         // Sleep for 3 seconds
  printer.wake();       // MUST wake() before printing again, even if reset
  printer.setDefault();

  Serial.println("done");

}
#include <SPI.h>
#include <MFRC522.h>
#include <Keypad.h>
#include <stdlib.h>

#define SS_PIN 53
#define RST_PIN 5


#define BUTTON_TOPLEFT 9
#define BUTTON_TOPRIGHT 10
#define BUTTON_MIDDLERIGHT 12
#define BUTTON_MIDDLELEFT 11

#define BUTTON_CANCEL 8

const int buttonAmount = 5;

MFRC522 rfid(SS_PIN, RST_PIN);

const int ROW_NUM = 4; //four rows
const int COLUMN_NUM = 3; //three columns



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
char cancelButton = 'X';

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
  
  // start up serial port, spi and rfid shit
  Serial.begin(9600);
  SPI.begin();
  rfid.PCD_Init();

  // Declare the menu buttons and test led 
  pinMode(4, OUTPUT);
  
  // for (int i = 0; i < buttonAmount; i++) {
  //   pinMode(buttons[i], INPUT);
  // } 
  pinMode(BUTTON_TOPLEFT, INPUT);
  pinMode(BUTTON_TOPRIGHT, INPUT);
  pinMode(BUTTON_MIDDLELEFT, INPUT);
  pinMode(BUTTON_MIDDLERIGHT, INPUT);
  pinMode(BUTTON_CANCEL, INPUT);

}

// This code is for receiving the data from the arduino

int readData() {
  //blinkTest(6);
  int intValue = -1;
  if (Serial.available()) {
    char receivedChar = Serial.read();
    intValue = atoi(&receivedChar); // Convert character to integer

  }
  //Serial.println(intValue);
  return intValue;
}

void blinkTest(int blinks) {
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
        //blinkTest(returnedInt);
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
  int keypadStatus;
  // Run until either cancel button is pressed or until corresponding java method has succeeded
  while(true) {
    sendKeypadInput();
    keypadStatus = readData();
    // If the int return indicates a non-false statement
    if (keypadStatus == 1) {
      goto endOfKeypadMiscSerialPort;
    }
    else if (digitalRead(BUTTON_CANCEL) == HIGH) {
      keypadStatus = 0;
      Serial.write(cancelButton);
      blinkTest(2);
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

void atmMenu(){
  while(true) {
    beginOfAtmMenu:
    // If top left button is pressed
    if (digitalRead(BUTTON_TOPLEFT) == HIGH) {
      // CODE FOR CHECKING ACCOUNT BALANCE
      blinkTest(1);
      Serial.write(topLeftButton);
      delay(1000);
      waitForReturnButton();
    }
    // If top right button is pressed
    else if(digitalRead(BUTTON_TOPRIGHT) == HIGH) {
      // CODE FOR FAST PIN OPTION OF 70 EURO
      blinkTest(1);
      Serial.write(topRightButton);
      delay(1000);
      boolean validAction =  false;

      // Wait to see what char java returns
      while(!validAction) {
        int javaReturnState = readData();

        if (javaReturnState == 1) {
          // Wait, cuz arduino is gonna reset cuz end of pin thingy
          while(true) {}
        }
      }
    }
      
    // If middle left button is pressed 
    else if (digitalRead(BUTTON_MIDDLELEFT) == HIGH) {
      // CODE FOR PINNING MONEY 
      blinkTest(1);
      Serial.write(middleLeftButton);
      bool pinningCanceled = pinMoneyMenu();
      if (pinningCanceled) {
        goto beginOfAtmMenu;
      }
    }
    // If middle right button is pressed
    else if (digitalRead(BUTTON_MIDDLERIGHT) == HIGH) {

    }

    // If cancel button is pressed
    else if(digitalRead(BUTTON_CANCEL) == HIGH) {
      // CODE FOR GOING BACK TO START
      blinkTest(1);
      Serial.write(cancelButton);
      delay(1000);
      // Wait cuz shit is gonna be cancelled
      while(true) {}
    }
  }
}
  
bool pinMoneyMenu() {
  while(true) {
    if (digitalRead(BUTTON_TOPLEFT)) {
      Serial.write(topLeftButton);
      delay(1000);
    }
    else if (digitalRead(BUTTON_TOPRIGHT)) {
      Serial.write(topRightButton);
      delay(1000);
    }
    else if(digitalRead(BUTTON_MIDDLELEFT)) {
      Serial.write(middleLeftButton);
      delay(1000);
    }
    else if(digitalRead(BUTTON_MIDDLERIGHT)) {
      Serial.write(middleRightButton);
      delay(1000);
      int stateOfKeypadMethod = cancelableKeypadSerialPort();
      if (stateOfKeypadMethod == 1) {
        // Wait, cuz arduino is gonna reset cuz end of pin thingy
        while(true) {}
      }
      // If cancel button was pressed
      else if (stateOfKeypadMethod == 0) {
      }
    }
    else if (digitalRead(BUTTON_CANCEL)) {
      Serial.write(cancelButton);
      delay(1000);
      return true;
    }
  }
  endOfPinMoneyMenu:
  return true;
}

void waitForReturnButton() {
  // blinkTest(2);
  while(true) {
    // Cancel seeing balance if cancel button is pressed and go back to menu
    if (digitalRead(BUTTON_CANCEL) == HIGH) {
      Serial.write(cancelButton);
      delay(1000);
      // blinkTest(3);
      goto endOfWaitForReturnButton;
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
      rfidScan();
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
    // waitForReturnButtonToBePressed();
    // Call general menu method
    atmMenu();
  } 
}
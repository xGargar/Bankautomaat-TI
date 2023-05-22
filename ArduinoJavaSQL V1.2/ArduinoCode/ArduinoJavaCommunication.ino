#include <SPI.h>
#include <MFRC522.h>
#include <Keypad.h>
#include <stdlib.h>

#define SS_PIN 53
#define RST_PIN 5

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
  

  Serial.begin(9600);
  SPI.begin();
  rfid.PCD_Init();
  pinMode(13, OUTPUT);

}

// bool verificationCheck() {
//   if (Serial.available()) {
//     char userValidConfirmation = Serial.read();
//     if (userValidConfirmation == '1') {
//       return true;    
//     }
//     else if (userValidConfirmation == '1{
//       return false;
//     }
//   }
//   else {
//     return false;
//   }
// }

// This basically checks if the user is verified. 0 means that the user has not been verified (yet), 1 means that the user has been verified,
// 2 means that the user failed entering their pin 3 times in a row.
int readData() {
  //blinkTest(6);
  int intValue = -1;
  if (Serial.available()) {
    char receivedChar = Serial.read();
    intValue = atoi(&receivedChar); // Convert character to integer
    
    // Send the integer value back to the Java program
    //blinkTest(intValue);
    // Serial.println("this gets run");
    // Serial.println(intValue);
    // Serial.println("\n");
    // blinkTest(10);
  }
  //Serial.println(intValue);
  return intValue;
}

void blinkTest(int blinks) {
  for (int i = 0; i < 10; i++) {
    digitalWrite(13, HIGH);
    delay(500);
    digitalWrite(13, LOW);
    delay(500);
  }
}


bool finishedKeypadInputCheck() {
  if (Serial.available()) {
    int finishedKeypadConfirmation = Serial.read();
    if(finishedKeypadConfirmation == 1) {
      return true;
    }
    else {
      return false;
    }
  }
  else{
    return false;
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
        digitalWrite(13, HIGH);
        delay(1000);
        digitalWrite(13, LOW);
        delay(1000);
        cardScanned = false;
        goto scanLoop;
      }
      else if (returnedInt == 1) {
        //delay(1000);
        cardValid = true;
        digitalWrite(13, HIGH);
        delay(500);
        digitalWrite(13, LOW);
        delay(500);
        goto cardIsGood;
        
      }
      else {
        //blinkTest(returnedInt);
      }
        
    
    }
    
  }
  cardIsGood:
  //blinkTest(2);

  return;
}


// bool scanCardTest() {
//   while (true) {
//     rfidScan();
//     while(true) {
//       cardValid = verificationCheck(); 
//       if (cardValid != 0) {
//         return true;
//       }
//     }
    
//   }
// }

/*bool scanCard() {
  
  //Serial.println("test");
  while (!cardValid) {
    while (!cardScanned) {
      if (rfid.PICC_IsNewCardPresent() && rfid.PICC_ReadCardSerial()) {
        // Read the UID from the RFID card
        String uid = "";
        for (byte i = 0; i < rfid.uid.size; i++) {
          uid.concat(String(rfid.uid.uidByte[i] < 0x10 ? "0" : ""));
          uid.concat(String(rfid.uid.uidByte[i], HEX));
        }
        uid.toUpperCase();

        // Send the UID to the computer over serial communication
        Serial.println(uid);

        rfid.PICC_HaltA();
        rfid.PCD_StopCrypto1();
        cardScanned = true;
        break;
      }
    }
    
    int cardvalidVerification = verificationCheck(); 
    if (cardValid == 1) {
      return true;
    }
    else {
      cardScanned = false;
    }
  }
  
} */

bool scanCard() {
  while (!cardValid) {
    if (!cardScanned) {

      if (rfid.PICC_IsNewCardPresent() && rfid.PICC_ReadCardSerial()) {
      // Read the UID from the RFID card
      MFRC522::PICC_Type piccType = rfid.PICC_GetType(rfid.uid.sak);
      String uid = "";
      for (byte i = 0; i < rfid.uid.size; i++) {
        Serial.print(rfid.uid.uidByte[i], HEX);
      }
      uid.toUpperCase();

      // Send the UID to the computer over serial communication
      rfid.PICC_HaltA();
      rfid.PCD_StopCrypto1();
      cardScanned = true;

        
      }
      // Perform verification check
      
    }
  }

  return false;  // Return false if cardValid is never set to true
}



int keypadSerialPort() {
  int finishedKeypadInput = 0;
  // 0 is in this case another way of saying "false"
  while(finishedKeypadInput == 0) {
    sendKeypadInput();
    int finishedKeypadInput = finishedKeypadInputCheck();
    // If the int return indicates a non-false statement
    if (finishedKeypadInput != 0) {
      return finishedKeypadInput;
    }
  }
}

void sendKeypadInput() {
  char inputKey = keypad.getKey();
  if (inputKey) {
    Serial.write(inputKey);
  }
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
      pinCorrect = keypadSerialPort();
      
      // Loops back to card scanning if method returns 0 (user is not verified yet)
      if (pinCorrect == 0) {
        Serial.println("test");
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
        break;
      }
    }
    // Enter in custom pin amount
    validCustomPinAmount = keypadSerialPort();
    
    
  } 

  
}


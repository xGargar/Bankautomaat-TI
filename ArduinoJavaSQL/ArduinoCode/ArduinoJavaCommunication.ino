#include <SPI.h>
#include <MFRC522.h>
#include <Keypad.h>

#define SS_PIN 53
#define RST_PIN 9

MFRC522 rfid(SS_PIN, RST_PIN);

const int ROW_NUM = 4; //four rows
const int COLUMN_NUM = 3; //three columns


bool finishedKeypadInput = false;
bool cardValid = false;
bool cardScanned = false;
bool pinCorrect = false;
bool editedPin = false;
bool pinnedTheMoney = false;
bool done = false;


char keys[ROW_NUM][COLUMN_NUM] = {
  {'1','2','3'},
  {'4','5','6'},
  {'7','8','9'},
  {'*','0','#'}
};

byte pin_rows[ROW_NUM] = {13, 12, 11, 10}; //connect to the row pinouts of the keypad
byte pin_column[COLUMN_NUM] = {9, 8, 7}; //connect to the column pinouts of the keypad

Keypad keypad = Keypad( makeKeymap(keys), pin_rows, pin_column, ROW_NUM, COLUMN_NUM );

void setup() {
  

  Serial.begin(9600);
  SPI.begin();
  rfid.PCD_Init();

}

bool cardValidCheck() {
  if (Serial.available()) {
    char cardValidConfirmation = Serial.read();
    if (cardValidConfirmation == '1') {
      cardValid = true;
      return cardValid;    
    }
    else {
      cardValid = false;
      return cardValid;
    }
  }
  else {
    return false;
  }
}

bool finishedKeypadInputCheck() {
  if (Serial.available()) {
    char finishedKeypadConfirmation = Serial.read();
    if(finishedKeypadConfirmation == '1') {
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
  }
}

bool scanCard() {
  while (!cardScanned) {
    
    rfidScan();
    cardScanned = true;
    cardValid = cardValidCheck(); 
    if (cardValid) {
      return true;
    }
    else {
      cardScanned = false;
    }
  }
}

bool keypadSerialPort() {
  while(!finishedKeypadInput) {
    sendKeypadInput();
    finishedKeypadInput = finishedKeypadInputCheck();
    if (finishedKeypadInput) {
      return true;
    } 
  }
}

void sendKeypadInput() {
  char inputKey = keypad.getKey();
  if (inputKey) {
    Serial.println(inputKey);
  }
}



void loop() {
  cardValid = false;
  cardScanned = false;
  pinCorrect = false;
  editedPin = false;
  pinnedTheMoney = false;
  done = false;
  /*while(!done) {
     
    
    if (!pinCorrect) {
      pinCorrect = keypadSerialPort();
    }
    else {
      pinnedTheMoney = keypadSerialPort();
    }
    
    //pinnedTheMoney= inputVerificationPin();
  }*/

  pinCorrect = keypadSerialPort();
}

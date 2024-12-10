#include <SPI.h>
#include <MFRC522.h>
#include <Arduino.h>
#include "time.h"

#include <WiFi.h>
#include <Firebase_ESP_Client.h>
#include "addons/TokenHelper.h"
#include "addons/RTDBHelper.h"

#define WIFI_SSID "Methlab"
#define WIFI_PASSWORD "lmaopisan"

#define API_KEY "AIzaSyAguTXdCJh2xtHuJ69J70eJ1Xy8oSiU2BY"
#define DATABASE_URL "https://iot-attendance-752c8-default-rtdb.asia-southeast1.firebasedatabase.app/" 

#define SS_PIN 5
#define RST_PIN 22

#define LED_RED_PIN 32
#define LED_GREEN_PIN 33
#define BUZZER_PIN 27

FirebaseAuth auth;
FirebaseConfig config;
FirebaseData fbdo;
FirebaseJson json;

MFRC522 rfid(SS_PIN, RST_PIN);
MFRC522::MIFARE_Key key;
byte nuidPICC[4];

String parentPath;
bool signupOK = false;

const char* ntpServer = "time.google.com";
const long gmtOffset_sec = 28800;
const int daylightOffset_sec = 3600;

void setup() { 
  Serial.begin(115200);

  // WiFi settings
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.println("Connecting to Wi-Fi");
  while (WiFi.status() != WL_CONNECTED){
    Serial.print(".");
    delay(300);
  }
  Serial.println();
  Serial.print("Connected with IP : ");
  Serial.println(WiFi.localIP());

  // Firebase rdtb settigns
  config.api_key = API_KEY;
  config.database_url = DATABASE_URL;

  if (Firebase.signUp(&config, &auth, "", "")){
    Serial.println("Connected to Firebase");
    signupOK = true;
  }
  else{
    Serial.printf("%s\n", config.signer.signupError.message.c_str());
  }

  config.token_status_callback = tokenStatusCallback;
  
  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);

  // RFID settings
  SPI.begin();
  rfid.PCD_Init();

  for (byte i = 0; i < 6; i++) {
    key.keyByte[i] = 0xFF;
  }

  // Get current time
  configTime(gmtOffset_sec, daylightOffset_sec, ntpServer);
  printCurrentTime();

  // LED and Buzzer settings
  pinMode(LED_RED_PIN, OUTPUT);
  pinMode(LED_GREEN_PIN, OUTPUT);
  pinMode(BUZZER_PIN, OUTPUT);
}
 
void loop() {

  // Reset the loop if no new card present on the sensor/reader. This saves the entire process when idle.
  if ( ! rfid.PICC_IsNewCardPresent())
    return;

  // Verify if the NUID has been readed
  if ( ! rfid.PICC_ReadCardSerial())
    return;

  MFRC522::PICC_Type piccType = rfid.PICC_GetType(rfid.uid.sak);

  // Check is the PICC of Classic MIFARE type
  if (piccType != MFRC522::PICC_TYPE_MIFARE_MINI &&  
    piccType != MFRC522::PICC_TYPE_MIFARE_1K &&
    piccType != MFRC522::PICC_TYPE_MIFARE_4K) {
    Serial.println(F("Your tag is not of type MIFARE Classic."));
    return;
  }

  if (Firebase.ready() && signupOK) {
    tone(BUZZER_PIN, 300);
    digitalWrite(LED_RED_PIN, HIGH);
    digitalWrite(LED_GREEN_PIN, HIGH);
    delay(300);

    noTone(BUZZER_PIN);  
    digitalWrite(LED_RED_PIN, LOW);
    digitalWrite(LED_GREEN_PIN, LOW);
    delay(300);  

    Serial.println();
    Serial.println(F("Card Detected"));

    // Store NUID into nuidPICC array
    for (byte i = 0; i < 4; i++) {
      nuidPICC[i] = rfid.uid.uidByte[i];
    }
  
    Serial.print(F("The NUID tag is : "));
    storeCard(rfid.uid.uidByte, rfid.uid.size);
    Serial.println();

  }

  // Halt PICC
  rfid.PICC_HaltA();

  // Stop encryption on PCD
  rfid.PCD_StopCrypto1();
}

void storeCard(byte *buffer, byte bufferSize) {
  String userId;

  // get card uid -> convert to hex -> store to usedId
  for (byte i = 0; i < bufferSize; i++) {
    userId += String(buffer[i], HEX);
  }

  parentPath = "/" + String(userId);

  struct tm timeinfo;
  char currentTime[50]; // Make sure the buffer is large enough

  if (getLocalTime(&timeinfo)) {
    strftime(currentTime, sizeof(currentTime), "%d/%b/%y-%H:%M:%S", &timeinfo);
  } else {
    Serial.println("Time not set. Failed to sync with NTP server.");
  }

  
  String activePath = parentPath + "/active";

  if (!checkIfUserExists(userId)) { 
    json.set("expired_at", currentTime);
    json.set("name", "Nama User");
    json.set("phone_number", "080808080808");
    json.set("active", "0");
    json.set("updated_at", currentTime);
    json.set("created_at", currentTime);

    Firebase.RTDB.setJSON(&fbdo, parentPath.c_str(), &json) ? "ok" : fbdo.errorReason().c_str();
  } else {
    if (Firebase.RTDB.getString(&fbdo, activePath)) {
      String active = fbdo.stringData();

      if (active == "1") {
        json.set("active", "0");
        json.set("updated_at", currentTime);

        Firebase.RTDB.updateNode(&fbdo, parentPath.c_str(), &json) ? "ok" : fbdo.errorReason().c_str();

        Serial.println(userId);
        Serial.println("Change active to 0");
      } else if (active == "0") {
        json.set("active", "1");
        json.set("updated_at", currentTime);

        Firebase.RTDB.updateNode(&fbdo, parentPath.c_str(), &json) ? "ok" : fbdo.errorReason().c_str();
        Serial.println(userId);
        Serial.println("Change active to 1");
      }
    }
  }
}

void printCurrentTime(){
  struct tm initialTime;
  if(!getLocalTime(&initialTime)){
    Serial.println("Failed to obtain time");
    return;
  }
  Serial.println("Connected to NTP server");
}

bool checkIfUserExists(const String id_user) {
  String userPath = "/" + id_user;

  if (Firebase.RTDB.get(&fbdo, userPath)) {
    return true;
  } else {
    return false;
  }
}
#include <MeOrion.h>
#include <math.h>

#include <Wire.h>
#include "Adafruit_VL53L0X.h"

Adafruit_VL53L0X lox = Adafruit_VL53L0X();

MeDCMotor motorLeft(M1);
MeDCMotor motorRight(M2);

// Encoder pins
const int ENCODER_RIGHT_A = 8;
const int ENCODER_RIGHT_B = 2;  // INT0
const int ENCODER_LEFT_A = 9;
const int ENCODER_LEFT_B = 3;  // INT1


int32_t targetEncoderLeft = 0;
int32_t targetEncoderRight = 0;
// Encoder counters
volatile int32_t encoderTicksLeft = 0;
volatile int32_t encoderTicksRight = 0;


int32_t encoderTicksLeftPrev = 0;
int32_t encoderTicksRightPrev = 0;

float xPos = 0.0;
float yPos = 0.0;
float angle = 0.0;

// Movement state flags
bool isMovingForward = false;
bool isMovingBackward = false;
bool isTurningLeft = false;
bool isTurningRight = false;


/**
* This method is called when there is a change in encoder ticks on the left motor.
* Increments or decrements `encoderTickLeft` depending on rotation.
*/
void handleLeftEncoder() {
  bool a = digitalRead(ENCODER_LEFT_A);
  bool b = digitalRead(ENCODER_LEFT_B);
  encoderTicksLeft += (a == b) ? -1 : 1;
}

/**
* This method is invoked when a change in encoder position is detected in the right motor.
* Increments or decrements `encoderTicksRight` depending on rotation.
*/
void handleRightEncoder() {
  bool a = digitalRead(ENCODER_RIGHT_A);
  bool b = digitalRead(ENCODER_RIGHT_B);
  encoderTicksRight += (a == b) ? -1 : 1;
}

void setup() {
  Serial.begin(115200);

  pinMode(ENCODER_LEFT_A, INPUT_PULLUP);
  pinMode(ENCODER_LEFT_B, INPUT_PULLUP);
  pinMode(ENCODER_RIGHT_A, INPUT_PULLUP);
  pinMode(ENCODER_RIGHT_B, INPUT_PULLUP);

  attachInterrupt(digitalPinToInterrupt(ENCODER_LEFT_B), handleLeftEncoder, CHANGE);
  attachInterrupt(digitalPinToInterrupt(ENCODER_RIGHT_B), handleRightEncoder, CHANGE);

  Wire.begin();
   if (!lox.begin()) {
    Serial.println(F("Failed to boot VL53L0X"));
    while(1);
  }
  lox.startRangeContinuous();

}

void resetEncoders() {
  encoderTicksLeft = 0;
  encoderTicksRight = 0;
}

#define WHEEL_CIRCUM ((float)(PI*7.0f))
#define WHEEL_DISTANCE (14.0f)

void odom(){

  float ldiff = (encoderTicksLeft-encoderTicksLeftPrev)*WHEEL_CIRCUM/506.0f;
  float rdiff = (encoderTicksRight-encoderTicksRightPrev)*WHEEL_CIRCUM/506.0f;
  encoderTicksLeftPrev = encoderTicksLeft;
  encoderTicksRightPrev = encoderTicksRight;

  float disp = (ldiff + rdiff)/2.0f;
  angle += (ldiff - rdiff)/WHEEL_DISTANCE;

  xPos += (float)cos(angle)*disp;
  yPos += (float)sin(angle)*disp;

  Serial.write(0xBB);
  float response[3] = { xPos, yPos, angle };
  Serial.write((uint8_t*)response, sizeof(response));
  // Serial.print("l: ");
  // Serial.print(encoderTicksLeft);
  // Serial.print(" pl: ");
  // Serial.print(encoderTicksLeftPrev);
  // Serial.print(" r: ");
  // Serial.print(encoderTicksRight);
  // Serial.print(" pr: ");
  // Serial.print(encoderTicksRightPrev);
  
  // Serial.print(" x: ");
  // Serial.print(xPos);
  // Serial.print(" y: ");
  // Serial.print(yPos);
  // Serial.print(" angle: ");
  // Serial.print(angle);
  // Serial.write(0);
}

void loop() {
  if (Serial.available()) {
    // char buff[256];
    // int read = Serial.readBytes(buff, sizeof(buff));
    // Serial.write(buff, read);

    int32_t input = Serial.read();

    switch (input) {
      case 0:
        targetEncoderLeft = encoderTicksLeft;
        targetEncoderRight = encoderTicksRight;
        break;
      case 1:
        targetEncoderLeft += 500;
        targetEncoderRight += 500;
        break;
      case 2:
        targetEncoderLeft -= 500;
        targetEncoderRight -= 500;
        break;
      case 3:
        targetEncoderLeft += 500;
        targetEncoderRight -= 500;
        break;
      case 4:
        targetEncoderLeft -= 500;
        targetEncoderRight += 500;
        break;
    }

    Serial.write(0xAA);
    Serial.write(input); 
  }

  odom();

   if (lox.isRangeComplete()) {
      Serial.write(0xCC);
      Serial.write(lox.readRange()<500);
  }
  // Serial.print(" ");
  // Serial.print(encoderTicksLeft);
  // Serial.print(" ");
  // Serial.print(targetEncoderLeft);
  // Serial.print(" ");
  // Serial.print(encoderTicksRight);
  // Serial.print(" ");
  // Serial.print(targetEncoderRight);

  int32_t errorLeft = (targetEncoderLeft - encoderTicksLeft) * 10;
  int32_t errorRight = (targetEncoderRight - encoderTicksRight) * 10;

  // Serial.print("[");
  // Serial.print(errorLeft);
  // Serial.print(" ");
  // Serial.print(errorRight);
  // Serial.print("]");


  motorLeft.run(constrain(errorLeft, -255, 255));
  motorRight.run(constrain(-errorRight, -255, 255));
}

/*
 * servo.h
 *
 *  Created on: Apr 8, 2025
 *      Author: ahogrady
 */

#ifndef SERVO_H_
#define SERVO_H_

#include <inc/tm4c123gh6pm.h>
#include <stdint.h>
#include "lcd.h"
#include "button.h"



int convert_degrees_to_pulse_width(int degrees);
void initialize_servo();
void servo_move_to(int pulse_width);
void callibrate_servo();
void servo_set_callibration(int min, int max);


#endif /* SERVO_H_ */

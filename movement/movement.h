/*
 * movement.h
 *
 *  Created on: Feb 4, 2025
 *      Author: ahogrady
 */

#ifndef MOVEMENT_H_
#define MOVEMENT_H_


#include "open_interface.h"
#include <stdint.h>
#include "Timer.h"
#include "sleep.h"
#include "uart-interrupt.h"
#include <string.h>

oi_t *sensor_data;
void movement_init();
void movement_free();
double move_forward(double distance_millimeters);   //millimeters
void turn_left(double degrees);
void turn_right(double degrees);
void turn_right_no_uart(double degrees);

#endif /* MOVEMENT_H_ */

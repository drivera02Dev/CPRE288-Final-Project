/*
 * Scan.h
 *
 *  Created on: Mar 11, 2025
 *      Author: andreyd
 */

#ifndef SCAN_H_
#define SCAN_H_


#include <stdio.h>
#include <math.h>
#include <stdlib.h>
#include "adc.h"
#include "ping.h"
#include "servo.h"
#include "uart-interrupt.h"
#include "sleep.h"
#include "lcd.h"

//for code compatibility sake:
typedef struct cyBOT_Scan_t {
    int sound_dist;
    double IR_raw_val;
} cyBOT_Scan_t;

typedef struct obj_t {

    int start_angle;
    int end_angle;
    double distance;
    double width;

} obj_t;

void scan_range(int min_angle, int max_angle, obj_t arr[], int *arr_size);
void cyBOT_Scan(int angle, cyBOT_Scan_t* scan_data);
void print_objects(obj_t arr[], int size);

void cyBOT_init_scan();



#endif /* SCAN_H_ */

/*
 * sleep.c
 *
 *  Created on: Feb 13, 2025
 *      Author: andreyd
 */

#include "sleep.h"


void sleep_millis(int ms) {
    double init_time = timer_getMillis();
    while(timer_getMillis() - init_time < ms) {}
}





/*
 * movement.c
 *
 *  Created on: Feb 4, 2025
 *      Author: ahogrady
 */


#include "movement.h"
#include "Timer.h"


//const double MOVE_FORWARD_SPEED = 350.0;
//const double MAXIMUM_ROTATIONAL_VELOCITY = 350.0;
#define MOVE_FORWARD_SPEED 200
#define MAXIMUM_ROTATIONAL_VELOCITY 30
#define MINIMUM_ROTATIONAL_VELOCITY 25
#define ANGLE_OFFSET 0
#define CLIFF_SENSOR_BORDER 2700
#define CLIFF_LOWER_BORDER 750

char buffer[20];
oi_t *sensor_data;

void movement_init() {

    sensor_data = oi_alloc();
    oi_init(sensor_data);

}

void movement_free() {
    oi_free(sensor_data);
}


double absoluteVal(double n) {
    return n < 0 ? n * -1 : n;
}



double move_forward(double distance_mm) { // dist in mm
    int dir = 1;

    if(distance_mm < 0) {
        dir = -1;
        distance_mm *= -1;
    }

    oi_update_minimal(sensor_data);
    double sum = 0;
    char buffer[100];
    oi_setWheels(MOVE_FORWARD_SPEED * dir, MOVE_FORWARD_SPEED * dir);


    while (sum < distance_mm) {
        oi_update_minimal(sensor_data);

        if(dir > 0 && (sensor_data->bumpRight == 1 || sensor_data->bumpLeft == 1)) {
            oi_setWheels(0, 0);
            sprintf(buffer, "moved,%d\n", (int) sum * dir);
            uart_sendStr(buffer);
            if(sensor_data->bumpLeft == 1) {
                uart_sendStr("error,bump,left\n");
            }
            else {
                uart_sendStr("error,bump,right\n");
            }

            move_forward(-50);
            return sum;

        } else if(dir > 0 && (sensor_data->cliffFrontLeftSignal > CLIFF_SENSOR_BORDER || sensor_data->cliffFrontLeftSignal < CLIFF_LOWER_BORDER)) {
            oi_setWheels(0, 0);
            sprintf(buffer, "moved,%d\n", (int) sum * dir);
            uart_sendStr(buffer);
            uart_sendStr("error,boundary,left\n");
            //lcd_printf("%d,%d", sensor_data->cliffFrontLeftSignal, sensor_data->cliffFrontRightSignal);
            move_forward(-50);
            return sum;
        } else if(dir > 0 && (sensor_data->cliffFrontRightSignal > CLIFF_SENSOR_BORDER || sensor_data->cliffFrontRightSignal < CLIFF_LOWER_BORDER)) {
            oi_setWheels(0, 0);
            sprintf(buffer, "moved,%d\n", (int) sum * dir);
            uart_sendStr(buffer);
            uart_sendStr("error,boundary,right\n");
            //lcd_printf("%d,%d", sensor_data->cliffFrontLeftSignal, sensor_data->cliffFrontRightSignal);
            move_forward(-50);
            return sum;
        }

        sum += dir * sensor_data->distance;
    }

    oi_setWheels(0,0); //stop
    oi_update(sensor_data);
    sprintf(buffer, "moved,%d\n", (int) sum * dir);
    uart_sendStr(buffer);


    return sum;
}


void turn_left(double degrees){
    lcd_printf("%.2f", degrees);
    oi_update(sensor_data);
    double sum = 0;
    char buffer[100];

    oi_setWheels(MAXIMUM_ROTATIONAL_VELOCITY, -MAXIMUM_ROTATIONAL_VELOCITY);
    while(sum < degrees-ANGLE_OFFSET) {
        oi_update_minimal(sensor_data);
        sum += absoluteVal(sensor_data -> angle);
    }

    oi_setWheels(0,0);
    oi_update(sensor_data);

}

void turn_right(double degrees){
    oi_update(sensor_data);
    double sum = 0;
    char buffer[100];

    oi_setWheels(-MAXIMUM_ROTATIONAL_VELOCITY, MAXIMUM_ROTATIONAL_VELOCITY);
    while(sum < degrees-ANGLE_OFFSET) {
        oi_update_minimal(sensor_data);
        sum += absoluteVal(sensor_data -> angle);
    }

    oi_setWheels(0,0);    oi_update(sensor_data);

}

void turn_right_no_uart(double degrees){
    oi_update(sensor_data);
    double sum = 0;

    oi_setWheels(-MAXIMUM_ROTATIONAL_VELOCITY, MAXIMUM_ROTATIONAL_VELOCITY);
    while(sum < degrees-ANGLE_OFFSET) {
        oi_update_minimal(sensor_data);
        sum += absoluteVal(sensor_data -> angle);
    }

    oi_setWheels(0,0);    oi_update(sensor_data);
}

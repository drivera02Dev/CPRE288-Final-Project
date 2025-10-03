/*
 * servo.c
 *
 *  Created on: Apr 8, 2025
 *      Author: ahogrady
 */
#include "servo.h"

int pulse_width_start = 0;
int pulse_width_end = 1000;
int isCallibrated = 0;

int convert_degrees_to_pulse_width(int degrees) {
    int converted_pulse;
    if(isCallibrated == 1) {
        //TODO what the fuck?
        converted_pulse = ( ((pulse_width_end/180.0 + 1)/1000)*16000000 - ((pulse_width_start/180.0 + 1)/1000)*16000000)
                *(degrees/180.0) + ((pulse_width_start/180.0 + 1)/1000)*16000000;
    }
    else {
        converted_pulse = (((degrees)/180.0 + 1)/1000)*16000000;
    }
    return converted_pulse;

}

void initialize_servo() {
    SYSCTL_RCGCTIMER_R |= 0b11;
    SYSCTL_RCGCGPIO_R |= 0x2A;
    TIMER1_CTL_R &= ~0b100000001;

    while ((SYSCTL_PRGPIO_R & 0x2) ==0) {}
    GPIO_PORTB_DIR_R |= 0x2A;
    GPIO_PORTB_DEN_R |= 0x2B;

    GPIO_PORTB_AFSEL_R |= 0x33;
    GPIO_PORTB_AMSEL_R |= 0x10;
    GPIO_PORTB_PCTL_R |= 0x0000707311;
    SYSCTL_RCGCTIMER_R |= 0x02;

    //TIMER1_CTL_R &= ~0x4000;

    SYSCTL_PRTIMER_R |= 0x2A;
    while ((SYSCTL_PRTIMER_R & 0x2A) == 0) {}

    TIMER1_CTL_R &= ~0x0100;
    TIMER1_CFG_R = 0x4;
    TIMER1_TBMR_R = 0xA;
    TIMER1_TBPR_R |= 0x04;
    TIMER1_TBILR_R |= 0xE200;
    TIMER1_TBPMR_R = 0x4;
    TIMER1_TBR_R = 0x810A;
    TIMER1_TBV_R = 0x45876;

    //TIMER1_CTL_R |= 100000000000000;




    ///TIMER1_TBMR_R |= 0x2;
    ///TIMER1_TBMR_R &= ~0x4;
    //TIMER1_TBMR_R |= 0x2;
    TIMER1_TBMATCHR_R = 320000;

    TIMER1_CTL_R |= 0x0100;
    //TIMER1_CTL_R |= 0b100000000;


}



void servo_move_to(int pulse_width) {
    int cycles = pulse_width + 320000;

    TIMER1_TBILR_R = (TIMER1_TBILR_R & ~0xFFFF) | ((cycles) & 0xFFFF);
    TIMER1_TBPR_R = (TIMER1_TBPR_R & ~0xFF) | ((cycles >> 16) & 0xFF);
}

void callibrate_servo() {

    button_init(); //does nothing if already called

    lcd_printf("Use buttons 1 and 2 to turn servo to zero degrees\nPress 4 when complete");
    int button_pressed = 0;
    int pulse_width = 90;
    while(button_pressed != 4) {

        button_pressed = button_getButton();
        switch(button_pressed) {
            case 0:
                break;
            case 1:
                pulse_width += 2;
                servo_move_to(convert_degrees_to_pulse_width(pulse_width));
                timer_waitMillis(100);
                break;
            case 2:
                pulse_width -= 2;
                servo_move_to(convert_degrees_to_pulse_width(pulse_width));
                timer_waitMillis(100);
                break;
            case 3:
                break;
            case 4:
                break;
        }

    }
    while(button_pressed==4){button_pressed=button_getButton(); timer_waitMillis(100);}
    pulse_width_start = pulse_width;
   // pulse_width = 90;
    lcd_printf("Use buttons 1 and 2 to turn servo to 90 degrees\nPress 4 when complete");
    while(button_pressed != 4) {

        button_pressed = button_getButton();
        switch(button_pressed) {
            case 0:
                break;
            case 1:
                pulse_width += 2;
                servo_move_to(convert_degrees_to_pulse_width(pulse_width));
                timer_waitMillis(100);
                break;
            case 2:
                pulse_width -= 5;
                servo_move_to(convert_degrees_to_pulse_width(pulse_width));
                timer_waitMillis(100);
                break;
            case 3:
                break;
            case 4:
                break;
        }

    }
    while(button_pressed==4){button_pressed=button_getButton();timer_waitMillis(100);}
    pulse_width_end = pulse_width;
    lcd_printf("min = %d, \nmax = %d \nPress 4 to continue",
               pulse_width_start, pulse_width_end);
    while(button_pressed != 4) {}



}

void servo_set_callibration(int min, int max) {
    isCallibrated = 1;
    pulse_width_end = max;
    pulse_width_start = min;

}


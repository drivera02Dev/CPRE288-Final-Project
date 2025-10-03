/**
 * Driver for ping sensor
 * @file ping.c
 * @author
 */

#include "ping.h"
#include "Timer.h"

#define CLOCK_FREQ 16000000.
#define SOUND_VEL 340

// Global shared variables
// Use extern declarations in the header file

volatile uint32_t g_start_time = 0;
volatile uint32_t g_end_time = 0;
volatile enum{LOW, HIGH, DONE} g_state = LOW; // State of ping echo pulse

void ping_init (void) {
    SYSCTL_RCGCGPIO_R |= 0x2;
    while ((SYSCTL_PRGPIO_R & 0x2) ==0) {}
    GPIO_PORTB_DIR_R |= 0x08;
    GPIO_PORTB_DEN_R |= 0x08;

    GPIO_PORTB_PCTL_R |= 0x00007000;
    SYSCTL_RCGCTIMER_R |= 0x08;
    while ((SYSCTL_PRTIMER_R & 0x8) ==0) {}

    TIMER3_CTL_R &= ~0x0100;
    TIMER3_CFG_R = 0x4;
    TIMER3_TBMR_R |= 0x0007;
    TIMER3_TBMR_R &= ~0x0010;
    TIMER3_CTL_R |= 0x0C00;
    TIMER3_TBPR_R |= 0xFF;
    TIMER3_TBILR_R = 0xFFFF;
    TIMER3_IMR_R |= 0x0400;
    TIMER3_CTL_R |= 0x0100;

    NVIC_PRI9_R = (NVIC_PRI9_R & 0xFFFFFF0F) | 0x00000002;
    NVIC_EN1_R |= 0b10000;

    IntRegister(INT_TIMER3B, TIMER3B_Handler);

    GPIO_PORTB_DATA_R &= ~(0b1000);
}

void ping_trigger (void) {
    g_state = LOW;
    TIMER3_CTL_R &= ~(0b100000000);
    TIMER3_IMR_R &= ~(0b1111 << 8);
    GPIO_PORTB_AFSEL_R &= ~(0b1000);
    GPIO_PORTB_DIR_R |= (0b1000);

    GPIO_PORTB_DATA_R |= 0b1000;
    timer_waitMicros(5);
    GPIO_PORTB_DATA_R &= ~(0b1000);

    GPIO_PORTB_DIR_R &= ~(0b1000);

    GPIO_PORTB_AFSEL_R |= (0b1000);
    TIMER3_TBV_R |= 0xFFFFFF;             // reset timer to reduce risk of overflow
    TIMER3_TBPR_R |= 0xFF;                // if code doesn't work it could be this line lol
    TIMER3_IMR_R |= (0b1111 << 8);
    TIMER3_CTL_R |= (0b100000000);
}

void TIMER3B_Handler(void){
    if(TIMER3_MIS_R & (1 << 10)) {
        TIMER3_ICR_R |= (1 << 10);

        if(g_state == LOW) {
            g_start_time = TIMER3_TBR_R & 0xFFFFFF;
            g_state = HIGH;

        } else if(g_state == HIGH) {
            g_end_time = TIMER3_TBR_R & 0xFFFFFF;
            g_state = DONE;

        } else {
            // should not happen
        }
    }
}

float ping_wait_response() {
//    static int overflows = 0;

    while(g_state != DONE) { }

//    char of = 0;
    int pulse = ((int) g_start_time) - g_end_time;



//    if(g_start_time < g_end_time) {
//        of = 1;
//    }

    return pulse / CLOCK_FREQ * SOUND_VEL * 50.;

//    lcd_printf("Cycles: %d %s\nMillis: %lf\nCm: %lf\nOverflows: %d", pulse, (of ? "(OF)" : ""), pulse / CLOCK_FREQ * 1000., pulse / CLOCK_FREQ * SOUND_VEL * 50., overflows);
//
//    overflows += of;
}

float ping_get_cm() {
    ping_trigger();

    float cm = ping_wait_response();

    return cm;
}

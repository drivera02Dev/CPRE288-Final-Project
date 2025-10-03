/*
*
*   uart-interrupt.c
*
*
*
*   @author
*   @date
*/

// The "???" placeholders should be the same as in your uart.c file.
// The "?????" placeholders are new in this file and must be replaced.

#include <inc/tm4c123gh6pm.h>
#include <stdint.h>
#include "uart-interrupt.h"
#include <string.h>
#include "lcd.h"
// These variables are declared as examples for your use in the interrupt handler.
volatile char command_byte = 's'; // byte value for special character used as a command
volatile int stop_scan = 0; // flag to tell the main program a special command was received











void uart_interrupt_init(){
    Interrupt_Result = 0;
    Interrupt_Ready = 0;
    copy = 0;
    //enable clock to GPIO port B
    SYSCTL_RCGCGPIO_R |= 0b10;

    //enable clock to UART1
    SYSCTL_RCGCUART_R |= 0b10;

    //wait for GPIOB and UART1 peripherals to be ready
    while ((SYSCTL_PRGPIO_R & 0b10) == 0) {};
    while ((SYSCTL_PRUART_R & 0b10) == 0) {};

    //enable digital functionality on port B pins
    GPIO_PORTB_DEN_R |= 0b11;

    GPIO_PORTB_DIR_R = (GPIO_PORTB_DIR_R & ~0x1) | 0b10;

    //enable alternate functions on port B pins
    GPIO_PORTB_AFSEL_R |= 0b11;

    //enable UART1 Rx and Tx on port B pins
    GPIO_PORTB_PCTL_R = (GPIO_PORTB_PCTL_R & ~0b11111111) | 0b10001;

    //calculate baud rate
    uint16_t iBRD = 8; //use equations
    uint16_t fBRD = 44; //use equations

    //turn off UART1 while setting it up
    UART1_CTL_R &= ~0b1;

    //set baud rate
    //note: to take effect, there must be a write to LCRH after these assignments
    UART1_IBRD_R = iBRD;
    UART1_FBRD_R = fBRD;

    //set frame, 8 data bits, 1 stop bit, no parity, no FIFO
    //note: this write to LCRH must be after the BRD assignments
    UART1_LCRH_R = 0b01100000;

    //use system clock as source
    //note from the datasheet UARTCCC register description:
    //field is 0 (system clock) by default on reset
    //Good to be explicit in your code
    UART1_CC_R = 0x0;

    //////Enable interrupts

    //first clear RX interrupt flag (clear by writing 1 to ICR)
    UART1_ICR_R |= 0b00010000;




    //enable RX raw interrupts in interrupt mask register
    UART1_IM_R |= 0b10000;

    //NVIC setup: set priority of UART1 interrupt to 1 in bits 21-23
    NVIC_PRI1_R = (NVIC_PRI1_R & 0xFF0FFFFF) | 0x00200000;

    //NVIC setup: enable interrupt for UART1, IRQ #6, set bit 6
    NVIC_EN0_R |= 0b1000000;

    //tell CPU to use ISR handler for UART1 (see interrupt.h file)
    //from system header file: #define INT_UART1 22
    IntRegister(INT_UART1, UART1_Handler);

    //globally allow CPU to service interrupts (see interrupt.h file)
    IntMasterEnable();

    //re-enable UART1 and also enable RX, TX (three bits)
    //note from the datasheet UARTCTL register description:
    //RX and TX are enabled by default on reset
   //Good to be explicit in your code
    //Be careful to not clear RX and TX enable bits
    //(either preserve if already set or set them)
    UART1_CTL_R |= 0b1100000001;

}

void uart_sendChar(char data){
    while((UART1_FR_R & 0b100000)) {}

    UART1_DR_R = data;
}

char uart_receive(void){
    while((UART1_FR_R & 0b10000)) {}

    return (char) UART1_DR_R & 0xFF;
}

void uart_sendStr(const char *data){
    int len = strlen(data);
    int i;

    for(i = 0; i < len; i++) {
        uart_sendChar(data[i]);
    }
}




void reset_Interrupt() {
    Interrupt_Ready = 0;
    Interrupt_Result = 0;
}

//stores full instruction in Interrupt_Result and sets Interrupt_Ready to 1, reset using reset_Interrupt();
void handleInstruction(char byte_received) {

    static int chars_recieved = 0;




    if(chars_recieved > 0) {
        if(chars_recieved > 1) {
           // lcd_printf("%d", chars_recieved);
            if(chars_recieved > 2) {

            }
            else {
                Interrupt_Result = (Interrupt_Result << 8) | byte_received;
                Interrupt_Ready = 1;
                chars_recieved = 0; //reset counter
            }
        }
        else {
            Interrupt_Result = (Interrupt_Result << 8) | byte_received;
            chars_recieved++;
            return;
        }


    } else {
        Interrupt_Result |= byte_received;
        chars_recieved++;
        return;
    }

    //lcd_printf("%d", chars_recieved);

}


// Interrupt handler for receive interrupts
void UART1_Handler(void)
{

    //static char[8] instruction = {0};

    char byte_received;
    //check if handler called due to RX event
    if (UART1_MIS_R & 0b10000)
    {
        //byte was received in the UART data register
        //clear the RX trigger flag (clear by writing 1 to ICR)
        UART1_ICR_R |= 0b00010000;


        //ignore the error bits in UART1_DR_R
        byte_received = (char) UART1_DR_R & 0xFF;
        handleInstruction(byte_received);
    }
}

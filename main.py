from machine import Pin, UART
import time
import random

# Initialize UART (you can change the pins if needed)
uart = UART(1, baudrate=9600, tx=Pin(4), rx=Pin(5))  # Change pins according to your setup

while True:
    # Generate a random number between 0 and 100
    value = random.randint(0, 100)
    
    # Send the value over UART
    uart.write(f"{value}")  # Sending the value followed by a newline for easy parsing
    
    # Print to the console (optional, for debugging)
    print(f"{value}")
    
    time.sleep(1)  # Wait for 1 second before sending the next value



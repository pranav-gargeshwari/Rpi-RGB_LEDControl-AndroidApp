///-----------------------------------------------------------------
///   Description:    <Description>
///   Author:         <Author>                    Date: <DateTime>
///   Notes:          <Notes>
///   Revision History:
///   Name:           Date:        Description:
///-----------------------------------------------------------------

#include <stdio.h>
#include <unistd.h>                     //Used for UART
#include <fcntl.h>                      //Used for UART
#include <termios.h>                    //Used for UART
#include <wiringPi.h>

struct termios rfcomm;
int rfcomm_fd = -1;


int openSerial(char *devName)
{
        int err;
        char *deviceName;

        deviceName = (devName == NULL) ? "/dev/ttyUSB0" : devName;

        rfcomm_fd = open(deviceName,O_RDWR | O_NONBLOCK);
        if(rfcomm_fd == -1)
        {
            printf("Failed to open KX3 device %s, %m\n",deviceName);
            return 0;
        }


        err = tcgetattr(rfcomm_fd,&rfcomm);
        if(err != 0)
        {
                printf("tcgetattr failed:");
                return 0;
        }

        cfsetospeed(&rfcomm,B9600);
        rfcomm.c_cc[VMIN] = 0;
        rfcomm.c_cc[VTIME] = 0;

        cfmakeraw(&rfcomm);

//        rfcomm.c_cflag &= ~CRTSCTS;
        tcsetattr(rfcomm_fd,TCSANOW,&rfcomm);

        tcflush(rfcomm_fd,TCIFLUSH);

        return 1;
}

int main(void)
{
        printf("\nUART test\n\n");
		
        openSerial("/dev/rfcomm0");

		char buf[] = "hello world";
 		int count = write(rfcomm_fd, buf,strlen(buf)+1);
		printf("%i bytes transmitted...\n\n");
		int led= 1;
		int red = 7;
		int green = 9;
		int blue = 8;
 		if (wiringPiSetup() == -1)
    	exit (1);
	
		pinMode(led, OUTPUT);
  		pinMode(red, OUTPUT);
		pinMode(green, OUTPUT);
		pinMode(blue, OUTPUT);
		
		digitalWrite(red, 1);
		digitalWrite(green, 1);
		digitalWrite(blue, 1);
	
        printf("listening...\n\n");
        while(1)
        {

        if (rfcomm_fd != -1)
        {
                // Read up to 255 characters from the port if they are there
                unsigned char rx_buffer[256];
                int rx_length = read(rfcomm_fd, (void*)rx_buffer, 255);//Filestream, buffer to store in, number of bytes to read (max)
                if (rx_length < 0)
                {
                        //An error occured (will occur if there are no bytes)
                }
                else if (rx_length == 0)
                {
                        //No data waiting
                }
                else
                {
                        //Bytes received
                        rx_buffer[rx_length] = '\0';
                        printf("%i bytes read : %s\n", rx_length, rx_buffer);
						if(rx_buffer[0] == 'R')
						{
							digitalWrite(red, 0);
							digitalWrite(green, 1);
							digitalWrite(blue, 1);
						}
						else if(rx_buffer[0] == 'G')
						{
							digitalWrite(red, 1);
							digitalWrite(green, 0);
							digitalWrite(blue, 1);
						}
						else if(rx_buffer[0] == 'B')
						{
							digitalWrite(red, 1);
							digitalWrite(green, 1);
							digitalWrite(blue, 0);
						}
						if(rx_buffer[0] == 'X')
						{
							digitalWrite(led, 1);
						}
						else if(rx_buffer[0] == 'O')
						{
							digitalWrite(led, 0);
						}
                }
        }

        }

}
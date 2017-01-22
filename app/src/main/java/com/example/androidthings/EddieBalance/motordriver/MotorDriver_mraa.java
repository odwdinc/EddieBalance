package com.example.androidthings.EddieBalance.motordriver;

import android.util.Log;

import mraa.Platform;
import mraa.mraa;
import mraa.Gpio;
import mraa.Pwm;
import mraa.Dir;
/**
 * Created by antho on 1/21/2017.
 */

public class MotorDriver_mraa {
    private static final String TAG = MotorDriver_mraa.class.getSimpleName();

    //Datasheet spec is 100kHz Maximum PWM switching frequency
    private static final int PWM_PERIOD = 1000000; //nano seconds for 1kHz
    private static final char FORWARD = 1;
    private static final char REVERSE = 2;

    char[] c_system_command = new char[128];

    Gpio gpio_stby,gpio_ain1,gpio_ain2,gpio_bin1,gpio_bin2;
    Pwm pwm0,pwm1;


    int MotorDriver(){
        System.loadLibrary("mraa");
        Platform platform = mraa.getPlatformType();
        Log.d(TAG,"Welcome to libmraa\n Version: "+mraa.getVersion()+"\n Running on "+platform.toString()+"\n");


        gpio_ain1 = new Gpio(48);
        gpio_ain2 = new Gpio(47);
        gpio_bin1 = new Gpio(15);
        gpio_bin2 = new Gpio(14);
        gpio_stby = new Gpio(49);

        gpio_ain1.dir(Dir.DIR_OUT);
        gpio_ain2.dir(Dir.DIR_OUT);
        gpio_bin1.dir(Dir.DIR_OUT);
        gpio_bin2.dir(Dir.DIR_OUT);
        gpio_stby.dir(Dir.DIR_OUT);

        pwm0 = new Pwm(12);
        pwm1 = new Pwm(13);
        if ( pwm0 == null )
        {
            Log.e(TAG,"PWM0 was NULL must be some MRAA bug cause I can't fix it :(");
            return -1;
        }
        if ( pwm1 == null )
        {
            Log.e(TAG,"PWM1 was NULL");
            return -1;
        }
        Log.d(TAG,"PWM Init OK");
        pwm0.period_us(200);
        pwm1.period_us(200);
        Log.d(TAG,"PWM Period OK");
        pwm0.enable(true);
        pwm1.enable(true);
        Log.d(TAG,"PWM Enabed\r\n");

        //Take H Bridge out of standby mode
        gpio_stby.write(1);
        return 1;
    }
    void motor_driver_disable()
    {
        //Put H Bridge into standby mode
        gpio_stby.write(0);

        //Cleaning up GPIO stuff
        gpio_ain1.delete();
        gpio_ain2.delete();

        gpio_bin1.delete();
        gpio_bin2.delete();
        gpio_stby.delete();

        if ( pwm0 != null ) pwm0.delete();
        if ( pwm1 != null ) pwm1.delete();

    }
    void motor_driver_standby( char p_option )
    {
        if ( p_option > 0 )
        {
            gpio_stby.write(0); //Enter Standby
        }
        else
        {
            gpio_stby.write(1); //Exit Standby
        }
    }
    void set_motor_direction_left ( char p_direction )
    {
        if ( p_direction == FORWARD ) //Left motor CCW
        {
            gpio_ain1.write(0); //AIN1 Low
            gpio_ain2.write(1); //AIN2 High
        }
        else if ( p_direction == REVERSE ) //Left motor CW
        {
            gpio_ain1.write(1); //AIN1 High
            gpio_ain2.write(0); //AIN2 Low
        }
    }
    void set_motor_direction_right( char p_direction )
    {
        if ( p_direction == FORWARD ) //Right motor CW
        {
            gpio_bin1.write(1); //BIN1 High
            gpio_bin2.write(0); //BIN2 Low
        }
        else if ( p_direction == REVERSE ) //Right motor CCW
        {
            gpio_bin1.write( 0); //BIN1 Low
            gpio_bin2.write( 1); //BIN2 High
        }
    }

    void set_motor_speed_left ( float p_speed )
    {
        if ( p_speed < 0.0f )
        {
            if ( p_speed < -100.0f ) p_speed = -100.0f;
            set_motor_direction_left( REVERSE );
        }
        else //if ( p_speed > 0.0f )
        {
            if ( p_speed > 100.0f ) p_speed = 100.0f;
            set_motor_direction_left( FORWARD );
        }

	/* TODO: Use MRAA to set PWM duty_cycle
	pwm0 = open("/sys/class/pwm/pwmchip0/pwm0/duty_cycle", O_RDWR);
	int length = sprintf(c_system_command, "%d", (int)(PWM_PERIOD * fabs(p_speed/100)));
	write(pwm0, c_system_command, length * sizeof(char));
	close(pwm0);
	*/
    }
    void set_motor_speed_right( float p_speed )
    {
        if ( p_speed < 0.0f )
        {
            if ( p_speed < -100.0f ) p_speed = -100.0f;
            set_motor_direction_right( REVERSE );
        }
        else //if ( p_speed > 0.0f )
        {
            if ( p_speed > 100.0f ) p_speed = 100.0f;
            set_motor_direction_right( FORWARD );
        }

	/* TODO: Use MRAA to set PWM duty_cycle
	pwm1 = open("/sys/class/pwm/pwmchip0/pwm1/duty_cycle", O_RDWR);
	int length = sprintf(c_system_command, "%d", (int)(PWM_PERIOD * fabs(p_speed/100)));
	write(pwm1, c_system_command, length * sizeof(char));
	close(pwm1);
	*/
    }
}


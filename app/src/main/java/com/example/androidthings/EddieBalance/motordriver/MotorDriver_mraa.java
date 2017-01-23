package com.example.androidthings.EddieBalance.motordriver;

import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;
import com.google.android.things.pio.Pwm;

import java.io.IOException;

import static java.lang.Thread.sleep;


/**
 * Created by antho on 1/21/2017.
 */

public class MotorDriver_mraa {
    private static final String TAG = MotorDriver_mraa.class.getSimpleName();

    //Datasheet spec is 100kHz Maximum PWM switching frequency
    private static final int PWM_PERIOD = 1000000; //nano seconds for 1kHz
    private static final char FORWARD = 1;
    private static final char REVERSE = 0;
    private static final char STOP=2;
    private final PeripheralManagerService manager;

    char[] c_system_command = new char[128];

    Gpio gpio_stby,gpio_ain1,gpio_ain2,gpio_bin1,gpio_bin2;
    Pwm pwm0,pwm1;
    private int a1;
    private int a2;
    private int b1;
    private int b2;
    private int stby;


    public MotorDriver_mraa(int a1,int a2,int b1,int b2,int stby){
        manager = new PeripheralManagerService();
        this.a1 = a1;
        this.a2 = a2;
        this.b1 = b1;
        this.b2 =b2;
        this.stby = stby;
    }

    public int motor_driver_enable() throws IOException {
        gpio_ain1 = manager.openGpio("GP"+a1);
        gpio_ain2 = manager.openGpio("GP"+a2);
        gpio_bin1 = manager.openGpio("GP"+b1);
        gpio_bin2 = manager.openGpio("GP"+b2);
        gpio_stby = manager.openGpio("GP"+stby);

        gpio_ain1.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        gpio_ain2.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        gpio_bin1.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        gpio_bin2.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        gpio_stby.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);

        pwm0 = manager.openPwm("GP12");
        pwm1 = manager.openPwm("GP13");

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
        pwm0.setPwmFrequencyHz(5000);
        pwm1.setPwmFrequencyHz(5000);
        Log.d(TAG,"PWM Period OK");
        pwm0.setEnabled(true);
        pwm1.setEnabled(true);
        Log.d(TAG,"PWM Enabed\r\n");

        //Take H Bridge out of standby mode
        gpio_stby.setValue(true);
        return 1;
    }
    public void motor_driver_disable()
    {
        //Put H Bridge into standby mode
        try {
            gpio_stby.setValue(false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Cleaning up GPIO stuff
        try {
            gpio_ain1.close();
            gpio_ain1 = null;
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            gpio_ain2.close();
            gpio_ain2 = null;
        } catch (IOException e) {
            e.printStackTrace();
        }


        try {
            gpio_bin1.close();
            gpio_bin1 = null;
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            gpio_bin2.close();
            gpio_bin2 = null;
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            gpio_stby.close();
            gpio_stby = null;
        } catch (IOException e) {
            e.printStackTrace();
        }

        if ( pwm0 != null ) try {
            pwm0.close();
            pwm0 = null;
        } catch (IOException e) {
            e.printStackTrace();
        }

        if ( pwm1 != null ) try {
            pwm1.close();
            pwm1 = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public boolean debugInfoing = false;

    public Thread debugInfo = new Thread(new Runnable() {
        @Override
        public void run() {
            debugInfoing =true;
            while (debugInfoing) {

                try {
                    sleep(1000);
                    set_motor_speed_left(10);
                    set_motor_speed_right(10);
                    Log.d(TAG, "set_motor_speed ON");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


                try {
                    sleep(1000);
                    set_motor_speed_left(0);
                    set_motor_speed_right(0);
                    Log.d(TAG, "set_motor_speed OFF");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    });


    public void motor_driver_standby( char p_option )
    {
        if ( p_option > 0 )
        {
            try {
                gpio_stby.setValue(false); //Enter Standby
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else
        {
            try {
                gpio_stby.setValue(true); //Exit Standby
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    void set_motor_direction_left ( char p_direction )
    {
        if ( p_direction == FORWARD ) //Left motor CCW
        {
            try {
                gpio_ain1.setValue(false); //AIN1 Low
                gpio_ain2.setValue(true); //AIN2 High
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        else if ( p_direction == REVERSE ) //Left motor CW
        {
            try {
                gpio_ain1.setValue(true); //AIN1 High
                gpio_ain2.setValue(false); //AIN2 Low
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
    void set_motor_direction_right( char p_direction )
    {
        if ( p_direction == FORWARD ) //Right motor CW
        {
            try {
                gpio_bin1.setValue(true); //BIN1 High
                gpio_bin2.setValue(false); //BIN2 Low
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        else if ( p_direction == REVERSE ) //Right motor CCW
        {
            try {
                gpio_bin1.setValue( false); //BIN1 Low
                gpio_bin2.setValue( true); //BIN2 High
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public void set_motor_speed_left (double p_speed )
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
    public void set_motor_speed_right(double p_speed )
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


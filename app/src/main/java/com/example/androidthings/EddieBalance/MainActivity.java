/*
 * Copyright 2016, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.androidthings.EddieBalance;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.example.androidthings.EddieBalance.imu.imu;
import com.example.androidthings.EddieBalance.motordriver.Kalman;
import com.example.androidthings.EddieBalance.motordriver.MotorDriver_mraa;
import com.example.androidthings.EddieBalance.motordriver.PID_t;
import com.example.androidthings.EddieBalance.motordriver.UDP_Interface;
import com.example.androidthings.EddieBalance.motordriver.encoder;
import com.example.androidthings.EddieBalance.motordriver.pid;
import static java.lang.StrictMath.abs;

/**
 /*
 /*
 Eddie the balance bot. Copyright (C) 2015 Renee L. Glinski. All rights reserved.
 This software may be distributed and modified under the terms of the GNU
 General Public License version 2 (GPL2) as published by the Free Software
 Foundation and appearing in the file LICENSE included in the packaging of
 this file. Please note that GPL2 Section 2[b] requires that all works based
 on this software must also be made publicly available under the terms of
 the GPL2 ("Copyleft").
 */


/* Incoming UDP_Interface Command Packet handler:
     *
     * DRIVE[value]	=	+ is Forwards, - is Reverse, 0.0 is IDLE
     * TURN[value]	=	+ is Right, - is Left, 0.0 is STRAIGHT
     *
     * SETPIDS = Changes all PIDs for speed and pitch controllers
     * GETPIDS = Returns all PIDs for speed and pitch controllers via UDP_Interface
     *
     * PIDP[P,I,D][value] = adjust pitch PIDs
     * SPID[P,I,D][value] = adjust speed PIDs
     *
     * KALQA[value] = adjust Kalman Q Angle
     * KALQB[value] = adjust Kalman Q Bias
     * KALR[value] = adjust Kalman R Measure
     *
     * STOPUDP	= Will stop Eddie from sending UDP_Interface to current recipient
     *
     * STREAM[0,1] = Enable/Disable Live Data Stream
     *
     */




public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final char CONSOLE = 0;
    private static final char UDP = 1;
    int outputto = CONSOLE; //Change to fit current need.

    static double last_gy_ms;
    static double last_PID_ms;


    boolean Running = true;
    boolean inFalloverState = false; //Used to flag when Eddie has fallen over and disables motors
    boolean inRunAwayState = false;
    int inSteadyState = 0; //Used to flag how long Eddie is being held upright in a steady state and will enable motors
    public int StreamData = 0;

    PID_t[] pitchPID = new PID_t[2]; //PID Controllers for pitch angle
    double[] pitchPIDoutput = new double[2];
    public double pidP_P_GAIN, pidP_I_GAIN, pidP_D_GAIN, pidP_I_LIMIT, pidP_EMA_SAMPLES;

    PID_t[] speedPID = new PID_t[2]; //PID Controllers for wheel speed
    double[] speedPIDoutput = new double[2];
    public double pidS_P_GAIN, pidS_I_GAIN, pidS_D_GAIN, pidS_I_LIMIT, pidS_EMA_SAMPLES;

    double filteredPitch;
    double filteredRoll;

    public double driveTrim = 0;
    public double turnTrim = 0;
    double smoothedDriveTrim = 0;
    private imu Eddyimu;
    private MotorDriver_mraa MotorDriver;
    public com.example.androidthings.EddieBalance.motordriver.Kalman Kalman;
    private pid EddyPid;
    private encoder EddyEncoder;
    private UDP_Interface EddyUDP;
    public String thisEddieName = "00010101";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        this.Eddyimu = new imu();
        this.MotorDriver = new MotorDriver_mraa(48,47,15,14,49);
        this.EddyEncoder = new encoder(183, 46, 45, 44);
        this.Kalman = new Kalman();
        this.EddyPid = new pid();
        this.EddyUDP = new UDP_Interface(this);

        EddyBotRunner.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            Running = false;
            EddyBotRunner.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "onDestroy");
    }

    double current_milliseconds()
    {
        long millis = System.currentTimeMillis() % 1000;
        return millis;
    }


    Thread EddyBotRunner = new Thread(new Runnable() {
        public void run() {
            main();
        }
    });


    Thread udplistenerThread= new Thread(new Runnable() {

        @Override
        public void run() {
            EddyUDP.udplistener_Thread();
        }
    });


    /* print() function used to handle data output
 * Current implementation will check output mode and direct data accordingly
 */
    public int print(String buffer, Object... b) {
        String result = String.format(buffer,b);

        switch( outputto )
        {
            case CONSOLE:
                Log.d(TAG, result);
                break;
            case UDP:
                EddyUDP.UDPBindSend(result);
                break;
        }

        return buffer.length();
    }

    // Define the exit signal handler
    void signal_callback_handler(int signum)
    {
        print("Exiting program; Caught signal %d\r\n", signum);
        Running = false;
    }

    int main()
    {


        //Init UDP_Interface with callbacks and pointer to run status

        //initUDP( &UDP_Command_Handler, &UDP_Control_Handler, &Running );

        print("Eddie starting...\r\n");

        //initIdentity();//Generate a unique ID to append to default name.

        double[] EncoderPos = new double[2];



        EddyEncoder.initEncoders( );
        print("Encoders activated.\r\n");


        print("IMU Started.\r\n");

        double kalmanAngle;

        Kalman.InitKalman();

        print( "Starting motor driver (and resetting wireless) please be patient..\r\n" );
        if ( MotorDriver.motor_driver_enable() < 1 )
        {
            print("Startup Failed; Error starting motor driver.\r\n");
            MotorDriver.motor_driver_disable();
            return -1;
        }
        print("Motor Driver Started.\r\n");

        //print("Eddie is starting the UDP_Interface network thread..\r\n");
        //pthread_create( udplistenerThread, null, udplistener_Thread, null );
        udplistenerThread.start();


        print( "Eddie is Starting PID controllers\r\n" );
	/*Set default PID values and init pitchPID controllers*/
        pidP_P_GAIN = pid.PIDP_P_GAIN;	pidP_I_GAIN = pid.PIDP_I_GAIN;	pidP_D_GAIN = pid.PIDP_D_GAIN;	pidP_I_LIMIT = pid.PIDP_I_LIMIT; pidP_EMA_SAMPLES = pid.PIDP_EMA_SAMPLES;
        EddyPid.PIDinit( pitchPID[0], pidP_P_GAIN, pidP_I_GAIN, pidP_D_GAIN, pidP_I_LIMIT, pidP_EMA_SAMPLES );
        EddyPid.PIDinit( pitchPID[1], pidP_P_GAIN, pidP_I_GAIN, pidP_D_GAIN, pidP_I_LIMIT, pidP_EMA_SAMPLES );

	/*Set default values and init speedPID controllers*/
        pidS_P_GAIN = pid.PIDS_P_GAIN;	pidS_I_GAIN = pid.PIDS_I_GAIN;	pidS_D_GAIN = pid.PIDS_D_GAIN;	pidS_I_LIMIT = pid.PIDS_I_LIMIT; pidS_EMA_SAMPLES = pid.PIDS_EMA_SAMPLES;
        EddyPid.PIDinit( speedPID[0], pidS_P_GAIN, pidS_I_GAIN, pidS_D_GAIN, pidS_I_LIMIT, pidS_EMA_SAMPLES );
        EddyPid.PIDinit( speedPID[1], pidS_P_GAIN, pidS_I_GAIN, pidS_D_GAIN, pidS_I_LIMIT, pidS_EMA_SAMPLES );

        //Get estimate of starting angle and specify complementary filter and kalman filter start angles
        Eddyimu.getOrientation();
        kalmanAngle = filteredPitch = Eddyimu.i2cPitch;
        Kalman.setkalmanangle( filteredPitch );
        filteredRoll = Eddyimu.i2cRoll;

        print( "Eddie startup complete. Hold me upright to begin\r\n" );

        double gy_scale = 0.01;
        last_PID_ms = last_gy_ms = current_milliseconds();

        while(Running)
        {
            EddyEncoder.GetEncoders( EncoderPos );

            if( abs(EddyEncoder.GetEncoder()) > 2000 && !inRunAwayState )
            {
                print( "Help! I'm running and not moving.\r\n");
                EddyEncoder.ResetEncoders();
                inRunAwayState = true;
            }

		/*Read IMU and calculate rough angle estimates*/
            Eddyimu.getOrientation();

		/*Calculate time since last IMU reading and determine gyro scale (dt)*/
            gy_scale = ( current_milliseconds() - last_gy_ms ) / 1000.0f;

            last_gy_ms = current_milliseconds();

		/*Complementary filters to smooth rough pitch and roll estimates*/
            filteredPitch = 0.995 * ( filteredPitch + ( Eddyimu.gy * gy_scale ) ) + ( 0.005 * Eddyimu.i2cPitch );
            filteredRoll = 0.98 * ( filteredRoll + ( Eddyimu.gx * gy_scale ) ) + ( 0.02 * Eddyimu.i2cRoll );

		/*Kalman filter for most accurate pitch estimates*/
            kalmanAngle = -Kalman.getkalmanangle(filteredPitch, Eddyimu.gy, gy_scale /*dt*/);

		/* Monitor angles to determine if Eddie has fallen too far... or if Eddie has been returned upright*/
            if ( ( inRunAwayState || ( abs( kalmanAngle ) > 50 || abs( filteredRoll ) > 45 ) ) && !inFalloverState )
            {

                MotorDriver.motor_driver_standby((char)1);
                inFalloverState = true;
                print( "Help! I've fallen over and I can't get up =)\r\n");
            }
            else if ( abs( kalmanAngle ) < 10 && inFalloverState && abs( filteredRoll ) < 20 )
            {
                if ( ++inSteadyState == 100 )
                {
                    inRunAwayState = false;
                    inSteadyState = 0;
                    MotorDriver.motor_driver_standby((char)0);
                        inFalloverState = false;
                    print( "Thank you!\r\n" );
                }
            }
            else
            {
                inSteadyState = 0;
            }

            if ( !inFalloverState )
            {
			/* Drive operations */
                smoothedDriveTrim = ( 0.99 * smoothedDriveTrim ) + ( 0.01 * driveTrim );
                if( smoothedDriveTrim != 0 )
                {
                    EddyEncoder.EncoderAddPos(smoothedDriveTrim); //Alter encoder position to generate movement
                }

			/* Turn operations */
                if( turnTrim != 0  )
                {
                    EddyEncoder.EncoderAddPos2( turnTrim, -turnTrim ); //Alter encoder positions to turn
                }

                double timenow = current_milliseconds();

                speedPIDoutput[0] = EddyPid.PIDUpdate( 0, EncoderPos[0], timenow - last_PID_ms, speedPID[0] );//Wheel Speed PIDs
                speedPIDoutput[1] = EddyPid.PIDUpdate( 0, EncoderPos[1], timenow - last_PID_ms, speedPID[1] );//Wheel Speed PIDs
                pitchPIDoutput[0] = EddyPid.PIDUpdate( speedPIDoutput[0], kalmanAngle, timenow - last_PID_ms, pitchPID[0] );//Pitch Angle PIDs
                pitchPIDoutput[1] = EddyPid.PIDUpdate( speedPIDoutput[1], kalmanAngle, timenow - last_PID_ms, pitchPID[1] );//Pitch Angle PIDs

                last_PID_ms = timenow;

                //Limit PID output to +/-100 to match 100% motor throttle
                if ( pitchPIDoutput[0] > 100.0 )  pitchPIDoutput[0] = 100.0;
                if ( pitchPIDoutput[1] > 100.0 )  pitchPIDoutput[1] = 100.0;
                if ( pitchPIDoutput[0] < -100.0 ) pitchPIDoutput[0] = -100.0;
                if ( pitchPIDoutput[1] < -100.0 ) pitchPIDoutput[1] = -100.0;

            }
            else //We are inFalloverState
            {
                EddyEncoder.ResetEncoders();
                pitchPID[0].accumulatedError = 0;
                pitchPID[1].accumulatedError = 0;
                speedPID[0].accumulatedError = 0;
                speedPID[1].accumulatedError = 0;
                driveTrim = 0;
                turnTrim = 0;
            }

            MotorDriver.set_motor_speed_right( pitchPIDoutput[0] );
            MotorDriver.set_motor_speed_left( pitchPIDoutput[1] );

            if ( (!inFalloverState || outputto == UDP) && StreamData == 1 )
            {
                print( "PIDout: %0.2f,%0.2f\tcompPitch: %6.2f kalPitch: %6.2f\tPe: %0.3f\tIe: %0.3f\tDe: %0.3f\tPe: %0.3f\tIe: %0.3f\tDe: %0.3f\r\n",
                        speedPIDoutput[0],
                        pitchPIDoutput[0],
                        filteredPitch,
                        kalmanAngle,
                        pitchPID[0].error,
                        pitchPID[0].accumulatedError,
                        pitchPID[0].differentialError,
                        speedPID[0].error,
                        speedPID[0].accumulatedError,
                        speedPID[0].differentialError
                );
            }

        } //--while(Running)

        print( "Eddie is cleaning up...\r\n" );

        EddyEncoder.CloseEncoder();

        //pthread_join(udplistenerThread, NULL);
        //print( "UDP_Interface Thread Joined..\r\n" );
        try {
            udplistenerThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        MotorDriver.motor_driver_disable();
        print( "Motor Driver Disabled..\r\n" );

        print( "Eddie cleanup complete. Good Bye!\r\n" );
        return 0;
    }

    public void setName(String substring) {
        thisEddieName  = substring;
    }
}
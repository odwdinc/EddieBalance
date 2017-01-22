package com.example.androidthings.EddieBalance.motordriver;

import com.example.androidthings.EddieBalance.MainActivity;

/**
 * Created by antho on 1/22/2017.
 */

public class UDP_Interface {
    private MainActivity activity;

    public UDP_Interface(MainActivity activity) {
        this.activity = activity;
    }

    /* Incoming UDP_Interface Control Packet handler */
    void UDP_Control_Handler( String p_udpin )
    {
        //DEBUG: printf( "UDP_Interface Control Packet Received: %s\r\n", p_udpin );

        String response = "DISCOVER: "+activity.thisEddieName;


        if ( p_udpin.contains("DISCOVER"))
        {
           response = "DISCOVER: "+ activity.thisEddieName;
        }
        else if ( p_udpin.contains("SETNAME"))
        {
            activity.setName( p_udpin.substring(7) );
            response = "SETNAME: "+activity.thisEddieName;
        }
        else if (p_udpin.contains("BIND"))
        {
            setCommandBindAddress();
            response = "BIND: OK";
        }

        UDPCtrlSend( response );
    }

    private void setCommandBindAddress() {
    }

    public void UDPCtrlSend(String response) {

    }

    public void UDPBindSend(String response) {

    }

    private void UDPCloseTX() {

    }



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
    public void UDP_Command_Handler( String p_udpin )
    {
	/* DRIVE commands */
        if(p_udpin.contains("DRIVE"))
        {
            activity.driveTrim = Integer.parseInt(p_udpin.substring(5));
        }
	/* TURN commands */
        else if( p_udpin.contains("TURN"))
        {
            activity.turnTrim = Integer.parseInt(p_udpin.substring(4));
        }
	/* Get/Set all PID quick commands*/
        else if ( p_udpin.contains("SETPIDS:"))
        {
            String[] strtok = p_udpin.split(",");
            activity.pidP_P_GAIN = Double.parseDouble(strtok[1]);

            activity.pidP_I_GAIN = Double.parseDouble(strtok[2]);

            activity.pidP_D_GAIN = Double.parseDouble(strtok[3]);;

            activity.pidS_P_GAIN = Double.parseDouble(strtok[4]);

            activity.pidS_I_GAIN = Double.parseDouble(strtok[5]);

            activity.pidS_D_GAIN =Double.parseDouble(strtok[6]);
        }
        else if ( p_udpin.contains( "GETPIDS"))
        {
            activity.print( "CURRENTPIDS:%0.3f,%0.3f,%0.3f,%0.3f,%0.3f,%0.3f\r\n", activity.pidP_P_GAIN, activity.pidP_I_GAIN, activity.pidP_D_GAIN, activity.pidS_P_GAIN, activity.pidS_I_GAIN, activity.pidS_D_GAIN );
        }
	/* Individual Pitch PID Controller commands */
        else if (  p_udpin.contains( "PPIDP"))
        {
            float newGain = 0;

            newGain =  Float.parseFloat(p_udpin.substring(5));
            activity.print( "New Pitch PID P Gain Received: Changing %0.3f to %0.3f\r\n", activity.pidP_P_GAIN, newGain );
            activity.pidP_P_GAIN = newGain;
        }
        else if ( p_udpin.contains( "PPIDI"))
        {
            float newGain = 0;
            newGain =   Float.parseFloat(p_udpin.substring(5));
            activity.print( "New Pitch PID I Gain Received: Changing %0.3f to %0.3f\r\n", activity.pidP_I_GAIN, newGain );
            activity.pidP_I_GAIN = newGain;
        }
        else if ( p_udpin.contains( "PPIDD"))
        {
            float newGain = 0;
            newGain =  Float.parseFloat(p_udpin.substring(5));
            activity.print( "New Pitch PID D Gain Received: Changing %0.3f to %0.3f\r\n", activity.pidP_D_GAIN, newGain );
            activity.pidP_D_GAIN = newGain;
        }
	/* Individual Speed PID Controller commands*/
        else if ( p_udpin.contains( "SPIDP"))
        {
            float newGain = 0;
            newGain =  Float.parseFloat(p_udpin.substring(5));
            activity.print( "New Speed PID P Gain Received: Changing %0.3f to %0.3f\r\n", activity.pidS_P_GAIN, newGain );
            activity.pidS_P_GAIN = newGain;
        }
        else if ( p_udpin.contains( "SPIDI"))
        {
            float newGain = 0;
            newGain = Float.parseFloat(p_udpin.substring(5));
            activity.print( "New Speed PID I Gain Received: Changing %0.3f to %0.3f\r\n", activity.pidS_I_GAIN, newGain );
            activity.pidS_I_GAIN = newGain;
        }
        else if ( p_udpin.contains( "SPIDD"))
        {
            float newGain = 0;
            newGain = Float.parseFloat(p_udpin.substring(5));
            activity.print( "New Speed PID D Gain Received: Changing %0.3f to %0.3f\r\n", activity.pidS_D_GAIN, newGain );
            activity.pidS_D_GAIN = newGain;
        }
	/* Pitch Kalman filter tuning commands */
        else if ( p_udpin.contains( "KALQA"))
        {
            float newGain = 0;
            newGain = Float.parseFloat(p_udpin.substring(5));
            activity.print( "Setting Kalman Q Angle to: %0.4f\r\n", newGain );
            activity.Kalman.setQkalmanangle( newGain );
        }
        else if ( p_udpin.contains( "KALQB"))
        {
            float newGain = 0;
            newGain =  Float.parseFloat(p_udpin.substring(5));
            activity.print( "Setting Kalman Q Bias to: %0.4f\r\n", newGain );
            activity.Kalman.setQbias( newGain );
        }
        else if ( p_udpin.contains( "KALR"))
        {
            float newGain = 0;
            newGain =   Float.parseFloat(p_udpin.substring(5));
            activity.print( "Setting Kalman R Measure to: %0.4f\r\n", newGain );
            activity.Kalman.setRmeasure( newGain );
        }
	/* UDP_Interface Hangup command */
        else if (  p_udpin.contains( "STOPUDP"))
        {
            UDPCloseTX();
        }
	/* Enable/Disable live data stream */
        else if (  p_udpin.contains( "STREAM1"))
        {
            activity.StreamData = 1;
        }
        else if (  p_udpin.contains( "STREAM0"))
        {
            activity.StreamData = 0;
        }
    }


}

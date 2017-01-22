package com.example.androidthings.EddieBalance.motordriver;

import com.example.androidthings.EddieBalance.MainActivity;

import java.util.Arrays;

import static java.lang.Thread.sleep;

/**
 * Created by antho on 1/22/2017.
 */

public class UDP_Interface {
    private MainActivity activity;
    private boolean isRunning;

    private static final char MAXMESSAGESIZE =64;

    private static final char UDP_COMMAND_PORT= 4242; //UDP Port for receiving command packets
    private static final char UDP_CONTROL_PORT = 4240; //UDP Port for receiving control packets
    private static final char  UDP_RESPOND_PORT =4243; //UDP Port for returning data to user


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

    void initUDP( int  p_running )
    {
        isRunning = p_running==1;
    }

    char[] lastRXAddress = new char[16]; //The last address a UDP message was received from
    char[] commandBindAddress = new char[16]; //This is the address we are bound to receive commands from
    int isBoundToClient = 0;

    void setCommandBindAddress()
    {
        //Set the bind address to the last address received from
        commandBindAddress = lastRXAddress;

        //Init the TX command socket with the new bind address
        initUDPCmdSend( commandBindAddress, UDP_RESPOND_PORT );

        isBoundToClient = 1;
    }

    void initUDPCtrlSend( char [] sendtoIP, short sendtoPort )
    {
        tx_control_socketfd = socket(PF_INET, SOCK_DGRAM, IPPROTO_UDP);	// Create socket for sending
        memset( &tx_ctrl_addrin, 0, sizeof( tx_ctrl_addrin ) );			// Zero out structure
        tx_ctrl_addrin.sin_family = AF_INET;							// Internet address family
        tx_ctrl_addrin.sin_addr.s_addr = inet_addr( sendtoIP );			// Destination IP address
        tx_ctrl_addrin.sin_port = htons(sendtoPort);					// Destination port
    }

    void initUDPCmdSend( char [] sendtoIP, char sendtoPort )
    {
        tx_command_socketfd = socket(PF_INET, SOCK_DGRAM, IPPROTO_UDP);	// Create socket for sending
        memset( &tx_cmd_addrin, 0, sizeof( tx_cmd_addrin ) );			// Zero out structure
        tx_cmd_addrin.sin_family = AF_INET;								// Internet address family
        tx_cmd_addrin.sin_addr.s_addr = inet_addr( sendtoIP );			// Destination IP address
        tx_cmd_addrin.sin_port = htons(sendtoPort);						// Destination port
    }

    void UDPBindSend( String data )
    {
        if ( tx_command_socketfd >= 0 && isBoundToClient )
        {
            sendto( tx_command_socketfd, data, len, 0, ( struct sockaddr * )&tx_cmd_addrin, sizeof( tx_cmd_addrin ) );
        }
    }

    void UDPCtrlSend( String data )
    {
        if(tx_control_socketfd >= 0)
        {
            sendto( tx_control_socketfd, data, strlen(data), 0, ( struct sockaddr * )&tx_ctrl_addrin, sizeof( tx_ctrl_addrin ) );
        }
    }

    void initListener(  short udpListenPort, int [] p_socket, char [] p_addr )
    {
        *p_socket = socket( AF_INET, SOCK_DGRAM, 0 );
        Arrays.fill(p_addr,(char)0);
        (*p_addr).sin_family = AF_INET;
        (*p_addr).sin_addr.s_addr = htonl( INADDR_ANY );
        (*p_addr).sin_port = htons( udpListenPort );
        bind( *p_socket, (struct sockaddr *)p_addr, sizeof( *p_addr ) );
    }

    int checkUDPReady( char [] udpBuffer, int [] p_socket )
    {
        int bytesAv = 0;

	/* If there is data to be read on the socket bring it in and capture the source IP */
        if( ioctl( *p_socket, FIONREAD, &bytesAv ) > 0 || bytesAv > 0 )
        {
            int udpMsgLen = 0;
            struct sockaddr_in rx_from_addr;
            socklen_t len = sizeof( rx_from_addr );
            //Receive UDP data
            udpMsgLen = recvfrom( *p_socket, udpBuffer, MAXMESSAGESIZE, 0, ( struct sockaddr * )&rx_from_addr, &len );
            udpBuffer[ udpMsgLen ] = 0; //Null terminate UDP RX string

            //Get address from this received packet
            char thisRXaddress[16] = {0};
            sprintf( thisRXaddress, "%s", inet_ntoa( rx_from_addr.sin_addr ) );

            //If this RX address does not match the last RX address && is a control packet...
            if ( p_socket == &rx_control_socketfd && memcmp( lastRXAddress, thisRXaddress, sizeof(lastRXAddress) ) != 0 )
            {
                UDPCloseCtrlTX(); //...close the control TX socket
                initUDPCtrlSend( thisRXaddress, UDP_RESPOND_PORT ); //and re-open with the address we need to respond to
            }

            //Store the last RX address
            strcpy( lastRXAddress, thisRXaddress );

            return 1;
        }

        return 0;
    }

    void UDPCloseTX()
    {
        close( tx_command_socketfd );
        tx_command_socketfd = -1;
    }
    void UDPCloseCtrlTX()
    {
        close( tx_control_socketfd );
        tx_control_socketfd = -1;
    }

    public void udplistener_Thread() throws InterruptedException {
        initListener( UDP_COMMAND_PORT, rx_command_socketfd, rx_cmd_addrin );
        initListener( UDP_CONTROL_PORT, rx_control_socketfd, rx_ctrl_addrin );

        char[] incomingUDP = new char[MAXMESSAGESIZE + 1 ];

        while (isRunning){
            sleep( 20000 ); //Give this thread a break between iterations to keep CPU usage down

		/* Check for UDP data on Control port */
            while( checkUDPReady( incomingUDP, rx_control_socketfd ) )
            {
                (controlFunctionPtr)(incomingUDP);
                Arrays.fill(incomingUDP,(char)0);
            }

		/* Check for UDP data on Command port */
            while( checkUDPReady( incomingUDP, rx_command_socketfd ) )
            {
                if ( isBoundToClient && memcmp( lastRXAddress, commandBindAddress, sizeof( lastRXAddress ) ) == 0 )
                {
                    (commandFunctionPtr)(incomingUDP);
                }
                Arrays.fill(incomingUDP,(char)0);
            }
        }

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

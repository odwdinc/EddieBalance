package com.example.androidthings.EddieBalance.motordriver;

import com.example.androidthings.EddieBalance.MainActivity;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;

import static android.system.OsConstants.AF_INET;
import static android.system.OsConstants.FIONREAD;
import static android.system.OsConstants.IPPROTO_UDP;
import static android.system.OsConstants.SOCK_DGRAM;
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

    public class sockaddr_in {
        public sockaddr_in(InetAddress address, int port){
            this.address = address;
            this.port = port;
            try {
                Socket = new DatagramSocket();
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
        byte[] message = new byte[MAXMESSAGESIZE];
        DatagramSocket Socket;
        DatagramPacket Packet;
        int port;
        InetAddress address;

        public sockaddr_in(int udpListenPort) {
            try {
                Socket = new DatagramSocket(udpListenPort);
            } catch (SocketException e) {
                e.printStackTrace();
            }
            Packet= new DatagramPacket(message, message.length);
        }

        public void send(String data) {
            Packet = new DatagramPacket(data.getBytes(), data.length(), address, port);
            try {
                Socket.send(Packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void close() {
            Socket.close();
        }

        public boolean hasData() {
            try {
                Socket.receive(Packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return (Packet.getLength() != 0);
        }

        public String receive() {
            String text = new String(message, 0, Packet.getLength());
            return text;
        }
    }


    sockaddr_in tx_command_socketfd; 	//Socket descriptor used to send data to bound client
    sockaddr_in tx_control_socketfd; 	//Socket descriptor used to response to control packets
    sockaddr_in rx_command_socketfd; 		//Socket descriptor used to receive commands from user
    sockaddr_in rx_control_socketfd; 		//Socket descriptor used to receive control data from user


    public UDP_Interface(MainActivity activity) {
        this.activity = activity;
    }



    void initUDP( int  p_running )
    {
        isRunning = p_running==1;
    }

    String lastRXAddress = ""; //The last address a UDP message was received from
    String commandBindAddress = ""; //This is the address we are bound to receive commands from
    boolean isBoundToClient = false;

    void setCommandBindAddress()
    {
        //Set the bind address to the last address received from
        commandBindAddress = lastRXAddress;

        //Init the TX command socket with the new bind address
        initUDPCmdSend( commandBindAddress, UDP_RESPOND_PORT );

        isBoundToClient = true;
    }

    void initUDPCtrlSend( String sendtoIP, char sendtoPort )
    {
        try {
            tx_control_socketfd = new sockaddr_in(InetAddress.getByName(sendtoIP),sendtoPort);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    void initUDPCmdSend( String sendtoIP, char sendtoPort )
    {
        try {
            tx_command_socketfd = new sockaddr_in(InetAddress.getByName(sendtoIP),sendtoPort);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public void UDPBindSend(String data)
    {
        if ( tx_command_socketfd !=null && isBoundToClient ) {
            tx_command_socketfd.send(data);
        }
    }

    void UDPCtrlSend( String data )
    {
        if(tx_control_socketfd!=null)
        {
            tx_control_socketfd.send(data);
        }
    }


    boolean checkUDPReady( sockaddr_in p_socket )
    {

	/* If there is data to be read on the socket bring it in and capture the source IP */
        if( p_socket.hasData() )
        {
            //Get address from this received packet
            String thisRXaddress = p_socket.Packet.getAddress().toString();



            //If this RX address does not match the last RX address && is a control packet...
            if ( thisRXaddress != lastRXAddress)
            {
                UDPCloseCtrlTX(); //...close the control TX socket
                initUDPCtrlSend( thisRXaddress, UDP_RESPOND_PORT ); //and re-open with the address we need to respond to
            }

            //Store the last RX address
            lastRXAddress = thisRXaddress;

            return true;
        }

        return false;
    }

    void UDPCloseTX()
    {
        tx_command_socketfd.close();
        tx_command_socketfd =null;
    }
    void UDPCloseCtrlTX()
    {
        tx_control_socketfd.close();
        tx_control_socketfd =null;
    }

    public void udplistener_Thread() throws InterruptedException {

        rx_command_socketfd = new sockaddr_in(UDP_COMMAND_PORT);
        rx_control_socketfd  = new sockaddr_in(UDP_CONTROL_PORT);

        while (isRunning){
            sleep( 2000 ); //Give this thread a break between iterations to keep CPU usage down

		/* Check for UDP data on Control port */
            while( checkUDPReady( rx_control_socketfd ) )
            {
                UDP_Control_Handler( rx_control_socketfd.receive() );
                Arrays.fill(rx_control_socketfd.message,(byte) 0);
            }

		/* Check for UDP data on Command port */
            while( checkUDPReady( rx_command_socketfd ) )
            {
                if ( isBoundToClient && ( lastRXAddress == commandBindAddress) )
                {
                    UDP_Command_Handler(rx_command_socketfd.receive());
                }
                Arrays.fill(rx_command_socketfd.message,(byte) 0);
            }
        }

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

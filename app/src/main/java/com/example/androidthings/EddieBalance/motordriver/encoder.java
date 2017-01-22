package com.example.androidthings.EddieBalance.motordriver;

import android.util.Log;

import mraa.Platform;
import mraa.mraa;
import mraa.Gpio;
import mraa.Dir;
import mraa.Edge;
/**
 * Created by antho on 1/22/2017.
 */

public class encoder {
    private static final String TAG = encoder.class.getSimpleName();
    Gpio[] encoderx = new Gpio[4];
    double[] position =new double[ 2 ];


    public void encoder(){
        System.loadLibrary("mraa");
        Platform platform = mraa.getPlatformType();
        Log.d(TAG,"Welcome ["+TAG+"] to libmraa\n Version: "+mraa.getVersion()+"\n Running on "+platform.toString()+"\n");
        mraa.init();
    }
    public  void initEncoders( int a, int b, int c, int d )
    {
        position[0] = position[1] = 0;
        encoderx[ 0 ] = new Gpio( a );
        encoderx[ 1 ] = new Gpio( b );
        encoderx[ 2 ] = new Gpio( c );
        encoderx[ 3 ] = new Gpio( d );

        encoderx[ 0 ].dir(Dir.DIR_IN);
        encoderx[ 0 ].isr(Edge.EDGE_BOTH,EncoderInterruptA);
        encoderx[ 1 ].dir(Dir.DIR_IN);
        encoderx[ 1 ].isr(Edge.EDGE_BOTH,EncoderInterruptA);

        encoderx[ 2 ].dir(Dir.DIR_IN);
        encoderx[ 2 ].isr(Edge.EDGE_BOTH,EncoderInterruptB);
        encoderx[ 3 ].dir(Dir.DIR_IN);
        encoderx[ 3 ].isr(Edge.EDGE_BOTH,EncoderInterruptB);

    }

    public void CloseEncoder()
    {
        encoderx[ 0 ].delete();
        encoderx[ 1 ].delete();
        encoderx[ 2 ].delete();
        encoderx[ 3 ].delete();
    }


    private Runnable EncoderInterruptA = new Runnable(){

        @Override
        public void run() {

        }
    };
    private Runnable EncoderInterruptB = new Runnable(){

        @Override
        public void run() {

        }
    };
}

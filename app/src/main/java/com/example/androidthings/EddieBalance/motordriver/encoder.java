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
    int[] lastpins = new int[ 4 ];

    public void encoder(){
        System.loadLibrary("mraa");
        Platform platform = mraa.getPlatformType();
        Log.d(TAG,"Welcome ["+TAG+"] to libmraa\n Version: "+mraa.getVersion()+"\n Running on "+platform.toString()+"\n");
        mraa.init();
    }

    public void ResetEncoders()
    {
        position[ 0 ] = position[ 1 ] = 0;
    }

    public double GetEncoder(  )
    {
        return (position[ 0 ] + position[ 1 ]) / 2;
    }

    public void GetEncoders( double temp[] )
    {
        temp[0] = position[ 0 ];
        temp[1] = position[ 1 ];
    }

    public void GetEncoderChange( double  temp[] )
    {
        temp[0] = position[ 0 ];
        temp[1] = position[ 1 ];
        position[ 0 ] = position[ 1 ] = 0;
    }

    public void EncoderAddPos2( double distance1, double distance2 )
    {
        position[0] += distance1;
        position[1] += distance2;
    }

    public void EncoderAddPos( double distance )
    {
        position[0] += distance;
        position[1] += distance;
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
            int[] currentpins = new int[ 2 ];

            int change = 0;
            currentpins[ 0 ] = encoderx[ 0 ].read();
            currentpins[ 1 ] =  encoderx[ 1 ].read();

            if( currentpins[ 0 ] != lastpins[ 0 ] )
            {
                if( currentpins[ 0 ] > lastpins[ 0 ] )
                {
                    if( currentpins[ 1 ] == 1  )
                    {
                        --change;
                    }
                    else
                    {
                        ++change;
                    }
                }
                else
                {
                    if( currentpins[ 1 ] == 1 )
                    {
                        ++change;
                    }
                    else
                    {
                        --change;
                    }
                }
            }
            else if( currentpins[ 1 ] != lastpins[ 1 ] )
            {
                if( currentpins[ 1 ] > lastpins[ 1 ] )
                {
                    if( currentpins[ 0 ] == 1 )
                    {
                        ++change;
                    }
                    else
                    {
                        --change;
                    }
                }
                else
                {
                    if( currentpins[ 0 ] == 1 )
                    {
                        --change;
                    }
                    else
                    {
                        ++change;
                    }
                }
            }

            position[ 0 ] += change;

            lastpins[ 0 ] = currentpins[ 0 ];
            lastpins[ 1 ] = currentpins[ 1 ];

        }
    };
    private Runnable EncoderInterruptB = new Runnable(){

        @Override
        public void run() {
            int[] currentpins =new int[ 2 ];

            int change = 0;
            currentpins[ 0 ] =  encoderx[ 2 ].read();
            currentpins[ 1 ] =  encoderx[ 3 ].read() ;

            if( currentpins[ 0 ] != lastpins[ 2 ] )
            {
                if( currentpins[ 0 ] > lastpins[ 2 ] )
                {
                    if( currentpins[ 1 ] ==1 )
                    {
                        ++change;
                    }
                    else
                    {
                        --change;
                    }
                }
                else
                {
                    if( currentpins[ 1 ] ==1)
                    {
                        --change;
                    }
                    else
                    {
                        ++change;
                    }
                }
            }
            else if( currentpins[ 1 ] != lastpins[ 3 ] )
            {
                if( currentpins[ 1 ] > lastpins[ 3 ] )
                {
                    if( currentpins[ 0 ] ==1)
                    {
                        --change;
                    }
                    else
                    {
                        ++change;
                    }
                }
                else
                {
                    if( currentpins[ 0 ] ==1)
                    {
                        ++change;
                    }
                    else
                    {
                        --change;
                    }
                }
            }

            position[ 1 ] += change;
            lastpins[ 2 ] = currentpins[ 0 ];
            lastpins[ 3 ] = currentpins[ 1 ];

        }
    };
}

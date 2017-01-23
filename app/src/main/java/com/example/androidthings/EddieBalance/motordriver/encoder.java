package com.example.androidthings.EddieBalance.motordriver;

import android.util.Log;

import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;
import java.util.List;;
import com.google.android.things.pio.Gpio;
/**
 * Created by antho on 1/22/2017.
 */

public class encoder {
    private static final String TAG = encoder.class.getSimpleName();
    private PeripheralManagerService manager;
    Gpio[] encoderx = new Gpio[4];
    double[] position =new double[ 2 ];
    boolean[] lastpins = new boolean[ 4 ];

    public encoder(){
        manager = new PeripheralManagerService();

        List<String> deviceList = manager.getGpioList();
        if (deviceList.isEmpty()) {
            Log.i(TAG, "No Gpios available on this device.");
        } else {
            Log.i(TAG, "List of available devices: " + deviceList);
        }

        //[GP109, GP110, GP111, GP114, GP115, GP12, GP128, GP129, GP13, GP130, GP131, GP134, GP135, GP14, GP15, GP165, GP182, GP183, GP19, GP20, GP27, GP28, GP40, GP41, GP42, GP43, GP44, GP45, GP46, GP47, GP48, GP49, GP77, GP78, GP79, GP80, GP81, GP82, GP83, GP84]

    }


    public boolean debug =false;

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

    public  void initEncoders() throws IOException {
        position[0] = position[1] = 0;



        encoderx[ 0 ] = manager.openGpio("GP183");
        encoderx[ 1 ] = manager.openGpio("GP44");
        encoderx[ 2 ] = manager.openGpio("GP46");
        encoderx[ 3 ] = manager.openGpio("GP45");

        encoderx[ 0 ].setDirection(Gpio.DIRECTION_IN);
        encoderx[ 0 ].setActiveType(Gpio.ACTIVE_HIGH);
        encoderx[ 0 ].setEdgeTriggerType(Gpio.EDGE_BOTH);
        encoderx[ 0 ].registerGpioCallback(EncoderInterruptA);

        encoderx[ 1 ].setDirection(Gpio.DIRECTION_IN);
        encoderx[ 1 ].setActiveType(Gpio.ACTIVE_HIGH);
        encoderx[ 1 ].setEdgeTriggerType(Gpio.EDGE_BOTH);
        encoderx[ 1 ].registerGpioCallback(EncoderInterruptA);

        encoderx[ 2 ].setDirection(Gpio.DIRECTION_IN);
        encoderx[ 2 ].setActiveType(Gpio.ACTIVE_HIGH);
        encoderx[ 2 ].setEdgeTriggerType(Gpio.EDGE_BOTH);
        encoderx[ 2 ].registerGpioCallback(EncoderInterruptB);

        encoderx[ 3 ].setDirection(Gpio.DIRECTION_IN);
        encoderx[ 3 ].setActiveType(Gpio.ACTIVE_HIGH);
        encoderx[ 3 ].setEdgeTriggerType(Gpio.EDGE_BOTH);
        encoderx[ 3 ].registerGpioCallback(EncoderInterruptB);
        Log.d(TAG,"encoders are setup");
    }

    public void CloseEncoder()
    {

        try {
            encoderx[ 0 ].unregisterGpioCallback(EncoderInterruptA);
            encoderx[ 0 ].close();
            encoderx[ 0 ]= null;
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            encoderx[ 1 ].unregisterGpioCallback(EncoderInterruptA);
            encoderx[ 1 ].close();
            encoderx[ 1 ]= null;
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            encoderx[ 2 ].unregisterGpioCallback(EncoderInterruptA);
            encoderx[ 2 ].close();
            encoderx[ 2 ]= null;
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            encoderx[ 3 ].unregisterGpioCallback(EncoderInterruptA);
            encoderx[ 3 ].close();
            encoderx[ 3 ]= null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private GpioCallback EncoderInterruptA = new GpioCallback() {

        @Override
        public boolean onGpioEdge(Gpio gpio) {
            boolean[] currentpins = new boolean[ 2 ];

            int change = 0;
            try {
                currentpins[ 0 ] =  encoderx[ 0 ].getValue();
                currentpins[ 1 ] =  encoderx[ 1 ].getValue();;
            } catch (IOException e) {
                e.printStackTrace();
            }


            if( currentpins[ 0 ] != lastpins[ 0 ] )
            {
                if( currentpins[ 0 ]  )
                {
                    if( currentpins[ 1 ] )
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
                    if( currentpins[ 1 ] )
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
                if( currentpins[ 1 ] )
                {
                    if( currentpins[ 0 ] )
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
                    if( currentpins[ 0 ] )
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

            if(debug){
                Log.d(TAG,"EncoderInterruptA: "+change+", position: "+position[ 0 ]);
            }

            lastpins[ 0 ] = currentpins[ 0 ];
            lastpins[ 1 ] = currentpins[ 1 ];
            return true;
        }
        @Override
        public void onGpioError(Gpio gpio, int error) {
            Log.w(TAG, gpio + ": EncoderInterruptA Error event " + error);
        }
    };
    private GpioCallback EncoderInterruptB = new GpioCallback() {

        @Override
        public boolean onGpioEdge(Gpio gpio) {
            boolean[] currentpins =new boolean[ 2 ];

            int change = 0;
            try {
                currentpins[ 0 ] =  encoderx[ 2 ].getValue();
                currentpins[ 1 ] =  encoderx[ 3 ].getValue() ;
            } catch (IOException e) {
                e.printStackTrace();
            }


            if( currentpins[ 0 ] != lastpins[ 2 ] )
            {
                if( currentpins[ 0 ]  )
                {
                    if( currentpins[ 1 ])
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
                    if( currentpins[ 1 ])
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
                if( currentpins[ 1 ]  )
                {
                    if( currentpins[ 0 ])
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
                    if( currentpins[ 0 ])
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
            if(debug){
                Log.d(TAG,"EncoderInterruptB: "+change+", position: "+position[ 1 ]);
            }

            lastpins[ 2 ] = currentpins[ 0 ];
            lastpins[ 3 ] = currentpins[ 1 ];
            return  true;
        }
        @Override
        public void onGpioError(Gpio gpio, int error) {
            Log.w(TAG, gpio + ": Error EncoderInterruptB event " + error);
        }
    };
}

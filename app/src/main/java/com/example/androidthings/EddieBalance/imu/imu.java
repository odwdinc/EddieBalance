package com.example.androidthings.EddieBalance.imu;

import android.util.Log;

import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;
import java.util.List;

import static java.lang.Math.*;
import static java.lang.Thread.sleep;

/**
 * Created by anthony on 1/21/2017.
 */

public class imu {





    ////////////////////////////
    // LSM9DS0 Gyro Registers //
    ////////////////////////////
    private static final int WHO_AM_I_G			=0x0F;
            private static final int CTRL_REG1_G			=0x20;
            private static final int CTRL_REG2_G			=0x21;
            private static final int CTRL_REG3_G			=0x22;
            private static final int CTRL_REG4_G			=0x23;
            private static final int CTRL_REG5_G			=0x24;
            private static final int REFERENCE_G			=0x25;
            private static final int STATUS_REG_G		=0x27;
            private static final int OUT_X_L_G			=0x28;
            private static final int OUT_X_H_G			=0x29;
            private static final int OUT_Y_L_G			=0x2A;
            private static final int OUT_Y_H_G			=0x2B;
            private static final int OUT_Z_L_G			=0x2C;
            private static final int OUT_Z_H_G			=0x2D;
            private static final int FIFO_CTRL_REG_G		=0x2E;
            private static final int FIFO_SRC_REG_G		=0x2F;
            private static final int INT1_CFG_G			=0x30;
            private static final int INT1_SRC_G			=0x31;
            private static final int INT1_THS_XH_G		=0x32;
            private static final int INT1_THS_XL_G		=0x33;
            private static final int INT1_THS_YH_G		=0x34;
            private static final int INT1_THS_YL_G		=0x35;
            private static final int INT1_THS_ZH_G		=0x36;
            private static final int INT1_THS_ZL_G		=0x37;
            private static final int INT1_DURATION_G		=0x38;

            //////////////////////////////////////////
            // LSM9DS0 Accel/Magneto (XM) Registers //
            //////////////////////////////////////////
            private static final int OUT_TEMP_L_XM		=0x05;
            private static final int OUT_TEMP_H_XM		=0x06;
            private static final int STATUS_REG_M		=0x07;
            private static final int OUT_X_L_M			=0x08;
            private static final int OUT_X_H_M			=0x09;
            private static final int OUT_Y_L_M			=0x0A;
            private static final int OUT_Y_H_M			=0x0B;
            private static final int OUT_Z_L_M			=0x0C;
            private static final int OUT_Z_H_M			=0x0D;
            private static final int WHO_AM_I_XM			=0x0F;
            private static final int INT_CTRL_REG_M		=0x12;
            private static final int INT_SRC_REG_M		=0x13;
            private static final int INT_THS_L_M			=0x14;
            private static final int INT_THS_H_M			=0x15;
            private static final int OFFSET_X_L_M		=0x16;
            private static final int OFFSET_X_H_M		=0x17;
            private static final int OFFSET_Y_L_M		=0x18;
            private static final int OFFSET_Y_H_M		=0x19;
            private static final int OFFSET_Z_L_M		=0x1A;
            private static final int OFFSET_Z_H_M		=0x1B;
            private static final int REFERENCE_X			=0x1C;
            private static final int REFERENCE_Y			=0x1D;
            private static final int REFERENCE_Z			=0x1E;
            private static final int CTRL_REG0_XM		=0x1F;
            private static final int CTRL_REG1_XM		=0x20;
            private static final int CTRL_REG2_XM		=0x21;
            private static final int CTRL_REG3_XM		=0x22;
            private static final int CTRL_REG4_XM		=0x23;
            private static final int CTRL_REG5_XM		=0x24;
            private static final int CTRL_REG6_XM		=0x25;
            private static final int CTRL_REG7_XM		=0x26;
            private static final int STATUS_REG_A		=0x27;
            private static final int OUT_X_L_A			=0x28;
            private static final int OUT_X_H_A			=0x29;
            private static final int OUT_Y_L_A			=0x2A;
            private static final int OUT_Y_H_A			=0x2B;
            private static final int OUT_Z_L_A			=0x2C;
            private static final int OUT_Z_H_A			=0x2D;
            private static final int FIFO_CTRL_REG		=0x2E;
            private static final int FIFO_SRC_REG		=0x2F;
            private static final int INT_GEN_1_REG		=0x30;
            private static final int INT_GEN_1_SRC		=0x31;
            private static final int INT_GEN_1_THS		=0x32;
            private static final int INT_GEN_1_DURATION	=0x33;
            private static final int INT_GEN_2_REG		=0x34;
            private static final int INT_GEN_2_SRC		=0x35;
            private static final int INT_GEN_2_THS		=0x36;
            private static final int INT_GEN_2_DURATION	=0x37;
            private static final int CLICK_CFG			=0x38;
            private static final int CLICK_SRC			=0x39;
            private static final int CLICK_THS			=0x3A;
            private static final int TIME_LIMIT			=0x3B;
            private static final int TIME_LATENCY		=0x3C;
            private static final int TIME_WINDOW	=		0x3D;
            private static final int ACT_THS	=			0x3E;
            private static final int ACT_DUR=				0x3F;

    public double temp;
    public double mx,my,mz;
    public double ax,ay,az;
    public double gx,gy,gz;
    public double i2cHeading,i2cPitch,i2cRoll;

    private static final String TAG = "imu";
    private static final int MAX_BUFFER_LENGTH = 512;
    private static final int GYRO_I2C_ADDR = 0x6B;
    private static final int XM_I2C_ADDR = 0x1D;
    private I2cDevice GYRO_Device;
    private I2cDevice XM_Device;

    byte[] rx_tx_buf= new byte[MAX_BUFFER_LENGTH]; ;

    public imu(String i2cPort) throws IOException {

        PeripheralManagerService manager = new PeripheralManagerService();
        List<String> deviceList = manager.getI2cBusList();
        if (deviceList.isEmpty()) {
            Log.i(TAG, "No I2C bus available on this device.");
        } else {
            Log.i(TAG, "List of available devices: " + deviceList);
        }

        GYRO_Device = manager.openI2cDevice(i2cPort, GYRO_I2C_ADDR);
        XM_Device = manager.openI2cDevice(i2cPort, XM_I2C_ADDR);

        sendi2c( GYRO_Device, FIFO_CTRL_REG_G, 0 );
        sendi2c( GYRO_Device, CTRL_REG1_G, 0x0F ); //Normal mode, enable all axes //0xFF ); //??unknown config??
        sendi2c( GYRO_Device, CTRL_REG2_G, 0x00); // Normal mode, high cutoff frequency
        sendi2c( GYRO_Device, CTRL_REG4_G, 0x10 ); // Set scale to 500 dps
        sendi2c( GYRO_Device, CTRL_REG5_G, 0x00 ); // FIFO Disabled, HPF Disabled

        sendi2c( XM_Device, FIFO_CTRL_REG, 0 );
        sendi2c( XM_Device, CTRL_REG1_XM, 0xFF );
        sendi2c( XM_Device, CTRL_REG2_XM, 0x00); //Set scale +/-2g
        sendi2c( XM_Device, CTRL_REG4_XM, 0x30 );

        sendi2c( XM_Device, CTRL_REG5_XM, 0x94);
        sendi2c( XM_Device, CTRL_REG6_XM, 0x00);
        sendi2c( XM_Device, CTRL_REG7_XM, 0x00);
	    /*
		return;
		*/
    }

    public Thread debugInfo = new Thread(new Runnable() {
        @Override
        public void run() {
            while (temp == 0.0) {
                readSensors();
                String result = String.format("gx:%6.2f gy:%6.2f gz:%6.2f  ax:%6.2f ay:%6.2f az:%6.2f  mx:%6.2f my:%6.2f mz:%6.2f  temp:%6.2f\n", gx, gy, gz, ax, ay, az, mx, my, mz, temp);
                Log.d(TAG, result);
                try {
                    sleep( 1000 );
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    });

    private void sendi2c(I2cDevice mDevice,int reg, int tosend)
    {
        try {
            mDevice.writeRegByte(reg,(byte)tosend);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private char readi2c(I2cDevice mDevice, int reg, int count)
    {

            try {
                mDevice.readRegBuffer(reg,rx_tx_buf,count);
            } catch (IOException e) {
                e.printStackTrace();
            }

        if(count == 1)return (char)rx_tx_buf[0];
        return 0;
    }
    public void readGyro()
    {
        readi2c(GYRO_Device, OUT_X_L_G, 6); // Read 6 bytes, beginning at OUT_X_L_G
        double tgx = (double)((short)(rx_tx_buf[1] << 8) | rx_tx_buf[0]) * (500.0 /*dps*/ / 32768.0);
        double tgy = (double)((short)(rx_tx_buf[3] << 8) | rx_tx_buf[2]) * (500.0 /*dps*/ / 32768.0);
        double tgz = (double)((short)(rx_tx_buf[5] << 8) | rx_tx_buf[4]) * (500.0 /*dps*/ / 32768.0);

        gx = tgz;
        gy = tgy;
        gz = tgx;
    }

    public void readAccel()
    {
        readi2c(XM_Device, OUT_X_L_A, 6); // Read 6 bytes, beginning at OUT_X_L_G
        double tax = (double)((short)(rx_tx_buf[1] << 8) | rx_tx_buf[0]) * 0.00006103515625; // Store x-axis values into gx
        double tay = (double)((short)(rx_tx_buf[3] << 8) | rx_tx_buf[2]) * 0.00006103515625; // Store y-axis values into gy
        double taz = (double)((short)(rx_tx_buf[5] << 8) | rx_tx_buf[4]) * 0.00006103515625; // Store z-axis values into gz

        ax = taz;
        ay = tay;
        az = tax;

        readi2c(XM_Device, OUT_TEMP_L_XM, 2);
        temp = (float)((short)(rx_tx_buf[1] << 8) | rx_tx_buf[0]);
    }

    public void readMag()
    {
        readi2c(XM_Device, OUT_X_L_M, 6); // Read 6 bytes, beginning at OUT_X_L_G
        double tmx = (double)((short)(rx_tx_buf[1] << 8) | rx_tx_buf[0]) * 0.00006103515625; // Store x-axis values into gx
        double tmy = (double)((short)(rx_tx_buf[3] << 8) | rx_tx_buf[2]) * 0.00006103515625; // Store y-axis values into gy
        double tmz = (double)((short)(rx_tx_buf[5] << 8) | rx_tx_buf[4]) * 0.00006103515625; // Store z-axis values into gz

        mx = tmz;
        my = tmy;
        mz = tmx;
    }

    public void readSensors()
    {
        readGyro();
        readAccel();
        readMag();
    }

    public void getOrientation()
    {
        readSensors();

        float PI_F = 3.14159265F;

        // i2cRoll: Rotation around the X-axis. -180 <= i2cRoll <= 180
        // a positive i2cRoll angle is defined to be a clockwise rotation about the positive X-axis
        //                    y
        //      i2cRoll = atan2(---)
        //                    z
        // where:  y, z are returned value from accelerometer sensor
        i2cRoll = (float)atan2(ay, az);

        // i2cPitch: Rotation around the Y-axis. -180 <= i2cRoll <= 180
        // a positive i2cPitch angle is defined to be a clockwise rotation about the positive Y-axis
        //                                 -x
        //      i2cPitch = atan(-------------------------------)
        //                    y * sin(i2cRoll) + z * cos(i2cRoll)
        // where:  x, y, z are returned value from accelerometer sensor
        if (ay * sin(i2cRoll) + az * cos(i2cRoll) == 0) i2cPitch = ax > 0 ? (PI_F / 2) : (-PI_F / 2);
        else i2cPitch = (float)atan(-ax / (ay * sin(i2cRoll) + az * cos(i2cRoll)));

        // i2cHeading: Rotation around the Z-axis. -180 <= i2cRoll <= 180
        // a positive i2cHeading angle is defined to be a clockwise rotation about the positive Z-axis
        //                                       z * sin(i2cRoll) - y * cos(i2cRoll)
        //   i2cHeading = atan2(--------------------------------------------------------------------------)
        //                    x * cos(i2cPitch) + y * sin(i2cPitch) * sin(i2cRoll) + z * sin(i2cPitch) * cos(i2cRoll))
        // where:  x, y, z are returned value from magnetometer sensor
//  i2cHeading = (float)atan2(mz * sin(i2cRoll) - my * cos(i2cRoll), mx * cos(i2cPitch) + my * sin(i2cPitch) * sin(i2cRoll) + mz * sin(i2cPitch) * cos(i2cRoll));

        // Convert angular data to degree
        i2cRoll 	 = - i2cRoll * 180.0 / PI_F;
        i2cPitch 	 =   i2cPitch * 180.0 / PI_F;
        i2cHeading = - i2cHeading * 180.0 / PI_F;

    }

}


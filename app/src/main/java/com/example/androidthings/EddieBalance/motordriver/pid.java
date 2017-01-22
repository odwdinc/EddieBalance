package com.example.androidthings.EddieBalance.motordriver;

import com.example.androidthings.EddieBalance.PID_t;

/*
  Created by antho on 1/22/2017.
 */

public class pid {
    public static final double PIDS_P_GAIN = 0.02f;
    public static final double PIDS_I_GAIN = 800.0f;
    public static final double PIDS_D_GAIN =  340.0f;
    public static final double PIDS_EMA_SAMPLES = 10.0f;
    public static final double PIDS_I_LIMIT  = 450.0; //Ilimit is before process gain

    //Pitch PID Configuration
    public static final double PIDP_P_GAIN  = 6.5f;
    public static final double  PIDP_I_GAIN = 600.0f;
    public static final double PIDP_D_GAIN  = 30.0f;
    public static final double PIDP_EMA_SAMPLES = 2.0f;
    public static final double  PIDP_I_LIMIT  = 10.0; //Ilimit is before process gain

    public void pid(){

    }

    public void PIDinit(PID_t pid, double pgain, double igain, double dgain, double ilimit, double numsamples)
    {
        pid.processGain = pgain;
        pid.integralTime = igain;
        pid.derivateTime = dgain;

        pid.error = 0;
        pid.accumulatedError = 0;
        pid.differentialError = 0;
        pid.lastFeedbackReading = 0;

        pid.iLimit = ilimit;
        pid.EMAnumberSamples = numsamples;
    }

    double
    exponentialMovingAverage( final double value, final double previousEMA, final double alpha )
    {
        double EMA = previousEMA;
        EMA += alpha * (value - previousEMA);
        return EMA;
    }

    void
    calculateP( final double setpoint, final double actual_position, PID_t  pPID )
    {
        pPID.error = setpoint - actual_position;
    }

    void
    calculateI( final double setpoint, final double dTmilliseconds, PID_t  pPID )
    {
        pPID.accumulatedError += pPID.error * dTmilliseconds / (pPID.integralTime);

        if(pPID.accumulatedError >  (pPID.iLimit))
        {
            pPID.accumulatedError = (pPID.iLimit);
        }
        else if(pPID.accumulatedError < -(pPID.iLimit))
        {
            pPID.accumulatedError = -(pPID.iLimit);
        }
    }

    void
    calculateD( final double actual_position, final double dTmilliseconds, PID_t pPID )
    {
        double currentDifferentialError = -1 * (pPID.derivateTime) * ((actual_position - pPID.lastFeedbackReading) / dTmilliseconds);
        pPID.lastFeedbackReading = actual_position;

        if( pPID.EMAnumberSamples > 0)
        {
            pPID.differentialError = exponentialMovingAverage( currentDifferentialError, pPID.differentialError, ( 1.0 / pPID.EMAnumberSamples ) );
        }
        else
        {
            pPID.differentialError = currentDifferentialError;
        }
    }

    public double
    PIDUpdate( double setpoint, double actual_position, double dTmilliseconds, PID_t pPID )
    {
        double controllerOutput = 0;

        calculateP(setpoint, actual_position, pPID);

        if ( pPID.integralTime == 0 ) pPID.accumulatedError = 0;
        else calculateI(setpoint, dTmilliseconds, pPID);

        if ( pPID.derivateTime == 0 ) pPID.differentialError = 0;
        else calculateD( actual_position, dTmilliseconds, pPID );

        controllerOutput = ( pPID.processGain )  * ( pPID.error + pPID.accumulatedError + pPID.differentialError );
        return controllerOutput;
    }
}

package com.example.androidthings.EddieBalance.motordriver;

/**
 * Created by antho on 1/22/2017.
 */

public class PID_t {
    public double processGain;
    public double integralTime;
    public double derivateTime;

    public double error;
    public double accumulatedError;
    public double differentialError;
    public double lastFeedbackReading;

    public double iLimit;

    public double EMAnumberSamples; //Determines the EMAalpha;
}

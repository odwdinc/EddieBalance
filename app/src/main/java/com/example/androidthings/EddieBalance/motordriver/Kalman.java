package com.example.androidthings.EddieBalance.motordriver;

/**
 * Created by antho on 1/22/2017.
 */

public class Kalman {
    /* Kalman filter variables */
    double Q_kalmanangle; // Process noise variance for the accelerometer
    double Q_bias; // Process noise variance for the gyro bias
    double R_measure; // Measurement noise variance - this is actually the variance of the measurement noise

    double kalmanangle; // The kalmanangle calculated by the Kalman filter - part of the 2x1 state vector
    double bias; // The gyro bias calculated by the Kalman filter - part of the 2x1 state vector
    double rate; // Unbiased rate calculated from the rate and the calculated bias - you have to call getkalmanangle to update the rate

    double[][]P = new double[2][2]; // Error covariance matrix - This is a 2x2 matrix
    double[] K = new double[2]; // Kalman gain - This is a 2x1 vector
    double y; // kalmanangle difference
    double S; // Estimate error

    public void Kalman(){

    }

    public void InitKalman()
    {
    /* We will set the variables like so, these can also be tuned by the user */
        Q_kalmanangle = 0.001;
        Q_bias = 0.003;
        R_measure = 0.03;

        kalmanangle = 0; // Reset the kalmanangle
        bias = 0; // Reset bias

        P[0][0] = 0; // Since we assume that the bias is 0 and we know the starting kalmanangle (use setkalmanangle), the error covariance matrix is set like so - see: http://en.wikipedia.org/wiki/Kalman_filter#Example_application.2C_technical
        P[0][1] = 0;
        P[1][0] = 0;
        P[1][1] = 0;
    }
    // The kalmanangle should be in degrees and the rate should be in degrees per second and the delta time in seconds
    public double getkalmanangle(double newkalmanangle, double newRate, double dt)
    {
        // KasBot V2  -  Kalman filter module - http://www.x-firm.com/?page_id=145
        // Modified by Kristian Lauszus
        // See my blog post for more information: http://blog.tkjelectronics.dk/2012/09/a-practical-approach-to-kalman-filter-and-how-to-implement-it

        // Discrete Kalman filter time update equations - Time Update ("Predict")
        // Update xhat - Project the state ahead
    /* Step 1 */
        rate = newRate - bias;
        kalmanangle += dt * rate;

        // Update estimation error covariance - Project the error covariance ahead
    /* Step 2 */
        P[0][0] += dt * (dt * P[1][1] - P[0][1] - P[1][0] + Q_kalmanangle);
        P[0][1] -= dt * P[1][1];
        P[1][0] -= dt * P[1][1];
        P[1][1] += Q_bias * dt;

        // Discrete Kalman filter measurement update equations - Measurement Update ("Correct")
        // Calculate Kalman gain - Compute the Kalman gain
    /* Step 4 */
        S = P[0][0] + R_measure;
    /* Step 5 */
        K[0] = P[0][0] / S;
        K[1] = P[1][0] / S;

        // Calculate kalmanangle and bias - Update estimate with measurement zk (newkalmanangle)
    /* Step 3 */
        y = newkalmanangle - kalmanangle;
    /* Step 6 */
        kalmanangle += K[0] * y;
        bias += K[1] * y;

        // Calculate estimation error covariance - Update the error covariance
    /* Step 7 */
        P[0][0] -= K[0] * P[0][0];
        P[0][1] -= K[0] * P[0][1];
        P[1][0] -= K[1] * P[0][0];
        P[1][1] -= K[1] * P[0][1];

        return kalmanangle;
    };
    public void setkalmanangle(double newkalmanangle) { kalmanangle = newkalmanangle; }; // Used to set kalmanangle, this should be set as the starting kalmanangle
    public double getRate() { return rate; }; // Return the unbiased rate

    /* These are used to tune the Kalman filter */
    public void setQkalmanangle(double newQ_kalmanangle) { Q_kalmanangle = newQ_kalmanangle; };
    public void setQbias(double newQ_bias) { Q_bias = newQ_bias; };
    public void setRmeasure(double newR_measure) { R_measure = newR_measure; };

    public double getQkalmanangle() { return Q_kalmanangle; };
    public double getQbias() { return Q_bias; };
    public double getRmeasure() { return R_measure; };
}

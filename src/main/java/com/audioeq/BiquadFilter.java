package com.audioeq;

/**
 * Lightweight biquad filter for real-time audio processing.
 * Uses Direct Form II for efficiency and numerical stability.
 */
public class BiquadFilter {
    private double b0;
    private double b1;
    private double b2;
    private double a1;
    private double a2;
    private double z1;
    private double z2;

    public void reset() {
        z1 = 0.0;
        z2 = 0.0;
    }

    public void setPeakingEq(double sampleRate, double frequency, double q, double gainDb) {
        double a = Math.pow(10.0, gainDb / 40.0);
        double w0 = 2.0 * Math.PI * frequency / sampleRate;
        double cos = Math.cos(w0);
        double sin = Math.sin(w0);
        double alpha = sin / (2.0 * q);

        double b0n = 1.0 + alpha * a;
        double b1n = -2.0 * cos;
        double b2n = 1.0 - alpha * a;
        double a0 = 1.0 + alpha / a;
        double a1n = -2.0 * cos;
        double a2n = 1.0 - alpha / a;

        b0 = b0n / a0;
        b1 = b1n / a0;
        b2 = b2n / a0;
        a1 = a1n / a0;
        a2 = a2n / a0;
    }

    public float process(float input) {
        double out = (input * b0) + z1;
        z1 = (input * b1) + z2 - (a1 * out);
        z2 = (input * b2) - (a2 * out);
        return (float) out;
    }
}

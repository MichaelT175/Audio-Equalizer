package com.audioeq;

/**
 * Transparent soft limiter to prevent clipping artifacts.
 */
public class LimiterDSP {
    private boolean enabled = true;
    private final double threshold = 0.92;
    private final double softClipDrive = 1.4;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public float process(float input) {
        if (!enabled) {
            return input;
        }

        double x = input;
        double abs = Math.abs(x);

        if (abs > threshold) {
            double over = abs - threshold;
            double compressed = threshold + (1.0 - Math.exp(-over * 3.0)) * (1.0 - threshold);
            x = Math.copySign(compressed, x);
        }

        x = Math.tanh(x * softClipDrive) / Math.tanh(softClipDrive);
        return (float) x;
    }
}

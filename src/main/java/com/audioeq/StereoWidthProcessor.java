package com.audioeq;

/**
 * Simple stereo width processor using mid/side scaling.
 */
public class StereoWidthProcessor {
    private float width = 1.0f;

    public void setWidth(float width) {
        this.width = Math.max(0.2f, Math.min(2.0f, width));
    }

    public void process(float[] stereoSamples) {
        float left = stereoSamples[0];
        float right = stereoSamples[1];

        float mid = (left + right) * 0.5f;
        float side = (left - right) * 0.5f;

        side *= width;

        stereoSamples[0] = mid + side;
        stereoSamples[1] = mid - side;
    }
}

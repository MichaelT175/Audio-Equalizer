package com.audioeq;

/**
 * Schroeder-style reverb optimized for real-time use.
 */
public class ReverbProcessor {
    private static final int[] COMB_TUNINGS = {1116, 1188, 1277, 1356};
    private static final int[] ALLPASS_TUNINGS = {556, 441};

    private final CombFilter[] combL;
    private final CombFilter[] combR;
    private final AllPassFilter[] allL;
    private final AllPassFilter[] allR;

    private double wet = 0.0;
    private double dry = 1.0;
    private ReverbPreset preset = ReverbPreset.STUDIO;

    public ReverbProcessor(double sampleRate) {
        double scale = sampleRate / 44100.0;

        combL = new CombFilter[COMB_TUNINGS.length];
        combR = new CombFilter[COMB_TUNINGS.length];
        for (int i = 0; i < COMB_TUNINGS.length; i++) {
            int size = (int) Math.max(200, Math.round(COMB_TUNINGS[i] * scale));
            combL[i] = new CombFilter(size);
            combR[i] = new CombFilter(size + 23);
        }

        allL = new AllPassFilter[ALLPASS_TUNINGS.length];
        allR = new AllPassFilter[ALLPASS_TUNINGS.length];
        for (int i = 0; i < ALLPASS_TUNINGS.length; i++) {
            int size = (int) Math.max(100, Math.round(ALLPASS_TUNINGS[i] * scale));
            allL[i] = new AllPassFilter(size);
            allR[i] = new AllPassFilter(size + 17);
        }

        applyPreset(ReverbPreset.STUDIO);
    }

    public void applyPreset(ReverbPreset preset) {
        this.preset = preset;
        for (CombFilter comb : combL) {
            comb.setFeedback(preset.roomSize);
            comb.setDamping(preset.damping);
        }
        for (CombFilter comb : combR) {
            comb.setFeedback(preset.roomSize);
            comb.setDamping(preset.damping);
        }
        for (AllPassFilter allPass : allL) {
            allPass.setFeedback(0.5);
        }
        for (AllPassFilter allPass : allR) {
            allPass.setFeedback(0.5);
        }
    }

    public void setIntensity(double intensity) {
        double mix = preset.baseMix * intensity;
        wet = Math.max(0.0, Math.min(0.65, mix));
        dry = 1.0 - wet;
    }

    public void process(float[] stereoSamples) {
        if (wet <= 0.0001) {
            return;
        }

        float input = (stereoSamples[0] + stereoSamples[1]) * 0.5f;
        float accL = 0f;
        float accR = 0f;

        for (CombFilter comb : combL) {
            accL += comb.process(input);
        }
        for (CombFilter comb : combR) {
            accR += comb.process(input);
        }

        for (AllPassFilter allPass : allL) {
            accL = allPass.process(accL);
        }
        for (AllPassFilter allPass : allR) {
            accR = allPass.process(accR);
        }

        stereoSamples[0] = (float) (stereoSamples[0] * dry + accL * wet);
        stereoSamples[1] = (float) (stereoSamples[1] * dry + accR * wet);
    }

    private static class CombFilter {
        private final float[] buffer;
        private int index;
        private float feedback = 0.5f;
        private float damping = 0.2f;
        private float filterStore = 0.0f;

        CombFilter(int size) {
            this.buffer = new float[size];
        }

        void setFeedback(double feedback) {
            this.feedback = (float) feedback;
        }

        void setDamping(double damping) {
            this.damping = (float) damping;
        }

        float process(float input) {
            float output = buffer[index];
            filterStore = (output * (1.0f - damping)) + (filterStore * damping);
            buffer[index] = input + (filterStore * feedback);
            index = (index + 1) % buffer.length;
            return output;
        }
    }

    private static class AllPassFilter {
        private final float[] buffer;
        private int index;
        private float feedback = 0.5f;

        AllPassFilter(int size) {
            this.buffer = new float[size];
        }

        void setFeedback(double feedback) {
            this.feedback = (float) feedback;
        }

        float process(float input) {
            float buffered = buffer[index];
            float output = -input + buffered;
            buffer[index] = input + (buffered * feedback);
            index = (index + 1) % buffer.length;
            return output;
        }
    }
}

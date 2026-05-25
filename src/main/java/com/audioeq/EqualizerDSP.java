package com.audioeq;

/**
 * Real-time EQ with smoothed gain changes and persistent filter state.
 */
public class EqualizerDSP {
    private static final double BASS_FREQ = 120.0;
    private static final double MID_FREQ = 1000.0;
    private static final double TREBLE_FREQ = 8000.0;
    private static final double BASS_Q = 0.8;
    private static final double MID_Q = 1.0;
    private static final double TREBLE_Q = 0.8;

    private final BiquadFilter[][] filters;
    private final double sampleRate;
    private final double smoothing;

    private double targetBassDb;
    private double targetMidDb;
    private double targetTrebleDb;

    private double bassDb;
    private double midDb;
    private double trebleDb;

    public EqualizerDSP(double sampleRate, int channels) {
        this.sampleRate = sampleRate;
        this.filters = new BiquadFilter[3][channels];
        this.smoothing = 0.12;

        for (int band = 0; band < filters.length; band++) {
            for (int ch = 0; ch < channels; ch++) {
                filters[band][ch] = new BiquadFilter();
            }
        }
    }

    public void setTargetGains(double bassDb, double midDb, double trebleDb) {
        this.targetBassDb = bassDb;
        this.targetMidDb = midDb;
        this.targetTrebleDb = trebleDb;
    }

    public void updateCoefficients() {
        bassDb += (targetBassDb - bassDb) * smoothing;
        midDb += (targetMidDb - midDb) * smoothing;
        trebleDb += (targetTrebleDb - trebleDb) * smoothing;

        for (int ch = 0; ch < filters[0].length; ch++) {
            filters[0][ch].setPeakingEq(sampleRate, BASS_FREQ, BASS_Q, bassDb);
            filters[1][ch].setPeakingEq(sampleRate, MID_FREQ, MID_Q, midDb);
            filters[2][ch].setPeakingEq(sampleRate, TREBLE_FREQ, TREBLE_Q, trebleDb);
        }
    }

    public float process(float sample, int channel) {
        float out = sample;
        out = filters[0][channel].process(out);
        out = filters[1][channel].process(out);
        out = filters[2][channel].process(out);
        return out;
    }

    public double getPreampDb() {
        double maxBoost = Math.max(0.0, Math.max(bassDb, Math.max(midDb, trebleDb)));
        double headroom = -2.0 - (maxBoost * 0.6);
        return Math.max(-12.0, headroom);
    }
}

package com.audioeq;


import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import com.github.psambit9791.jdsp.transform.FastFourier;
 
import javafx.application.Platform;

/**
 * The AudioProcessor class handles audio playback with equalization (EQ) effects.
 * Updated to work with JavaFX visualizers.
 */
public class AudioProcessor {

    private static boolean stopPlayback = false;
    private static boolean isPaused = false;
    private static final Object pauseLock = new Object();
    private static int playbackSession = 0;
    
    // Volume control
    private static float masterVolume = 1.0f;
    
    // Automatic sound leveling
    private static boolean autoLevelEnabled = true;
    private static float targetRMS = 0.1f;
    private static float currentTrackGain = 1.0f;
    
    // Echo cancellation
    private static boolean echoCancelEnabled = false;
    private static final int ECHO_DELAY_SAMPLES = 8000;
    private static final float ECHO_DECAY = 0.5f;
    private static double[] echoBuffer = null;
    private static int echoBufferIndex = 0;
    
    // dB Meter
    private static double currentdB = -60.0;
    
    // Smoothed limiter envelope (persists across buffers to avoid pumping)
    private static double limiterEnvelope = 0.0;
    
    // Persistent biquad filter states (avoids transients at buffer boundaries)
    private static double[] bassState = new double[4];   // x[n-1], x[n-2], y[n-1], y[n-2]
    private static double[] midState1 = new double[4];   // band-pass = LP then HP
    private static double[] midState2 = new double[4];
    private static double[] trebleState = new double[4];
    private static float lastSampleRate = 0;

    /**
     * Stops audio playback.
     */
    public static void stopAudioPlayback(boolean stop) {
        stopPlayback = stop;
    }

    /**
     * Pauses audio playback.
     */
    public static void pauseAudioPlayback() {
        isPaused = true;
    }

    /**
     * Unpauses audio playback.
     */
    public static void unpauseAudioPlayback() {
        synchronized (pauseLock) {
            isPaused = false;
            pauseLock.notifyAll();
        }
    }
    
    /**
     * Sets the master volume level.
     */
    public static void setMasterVolume(float volume) {
        masterVolume = Math.max(0.0f, Math.min(2.0f, volume));
    }
    
    /**
     * Gets the current master volume level.
     */
    public static float getMasterVolume() {
        return masterVolume;
    }
    
    /**
     * Enables or disables automatic sound leveling.
     */
    public static void setAutoLevelEnabled(boolean enabled) {
        autoLevelEnabled = enabled;
    }
    
    /**
     * Enables or disables echo cancellation.
     */
    public static void setEchoCancelEnabled(boolean enabled) {
        echoCancelEnabled = enabled;
    }
    
    /**
     * Gets the current dB level.
     */
    public static double getCurrentdB() {
        return currentdB;
    }

    /**
     * Plays audio with EQ and all effects (JavaFX version).
     */
    public static void playAudioWithEQ(String filePath, float initialBassGain, float initialMidGain, 
                                      float initialTrebleGain, VisualizerCanvasFX visualizer, 
                                      SpectrumCanvasFX spectrumCanvas, WaveformCanvasFX waveformCanvas, EqualizerApp eq) {
        playAudioWithEQ(filePath, initialBassGain, initialMidGain, initialTrebleGain, visualizer,
            spectrumCanvas, waveformCanvas, eq, null);
    }

    /**
     * Plays audio with EQ and all effects (JavaFX version) with optional completion callback.
     */
    public static void playAudioWithEQ(String filePath, float initialBassGain, float initialMidGain,
                                       float initialTrebleGain, VisualizerCanvasFX visualizer,
                                       SpectrumCanvasFX spectrumCanvas, WaveformCanvasFX waveformCanvas,
                                       EqualizerApp eq, Runnable onPlaybackFinished) {
        File audioFile = new File(filePath);

        if (!audioFile.exists()) {
            System.out.println("Error: File not found at " + filePath);
            return;
        }
        
        float[] gains = new float[] {initialBassGain, initialMidGain, initialTrebleGain};
        
        // Reset for new track
        echoBuffer = null;
        echoBufferIndex = 0;
        currentTrackGain = 1.0f;
        limiterEnvelope = 0.0;
        bassState = new double[4];
        midState1 = new double[4];
        midState2 = new double[4];
        trebleState = new double[4];
        lastSampleRate = 0;
        int sessionId;
        synchronized (AudioProcessor.class) {
            playbackSession++;
            sessionId = playbackSession;
            stopPlayback = false;
        }

        boolean completedNaturally = true;
        try (AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile)) {
            AudioFormat baseFormat = audioInputStream.getFormat();
            AudioFormat format = toPcmFormat(baseFormat);
            try (AudioInputStream decodedStream = AudioSystem.getAudioInputStream(format, audioInputStream)) {

            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            if (!AudioSystem.isLineSupported(info)) {
                throw new UnsupportedAudioFileException("Audio format not supported");
            }

            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            int numBars = 10;
            
            // Calculate track gain for auto-leveling
            if (autoLevelEnabled) {
                currentTrackGain = calculateTrackGain(audioFile, format);
            }

            while ((bytesRead = decodedStream.read(buffer, 0, buffer.length)) != -1) {
                if (stopPlayback || sessionId != playbackSession) {
                    line.stop();
                    line.close();
                    completedNaturally = false;
                    break;
                }

                synchronized (pauseLock) {
                    while (isPaused) {
                        try {
                            pauseLock.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }

                gains[0] = eq.getBassSliderValue();
                gains[1] = eq.getMidSliderValue();
                gains[2] = eq.getTrebleSliderValue();

                byte[] processingBuffer = Arrays.copyOf(buffer, bytesRead);

                // Apply EQ to the current buffer
                byte[] adjustedBuffer = applyEQ(processingBuffer, format, gains[0], gains[1], gains[2]);
                
                // Apply volume and auto-leveling
                adjustedBuffer = applyVolumeControl(adjustedBuffer, format);
                
                // Apply echo cancellation
                if (echoCancelEnabled) {
                    adjustedBuffer = applyEchoCancellation(adjustedBuffer, format);
                }
                
                // Calculate dB
                currentdB = calculatedB(adjustedBuffer, format);

                final byte[] finalBuffer = adjustedBuffer;
                
                // Update visualizers on JavaFX thread
                int[] barHeights = calculateBarHeights(adjustedBuffer, numBars, format);
                double[] spectrumData = notSorted(processingBuffer, numBars, format);
                
                Platform.runLater(() -> {
                    visualizer.updateVisualizer(barHeights);
                    spectrumCanvas.updateSpectrum(spectrumData);
                    waveformCanvas.updateWaveform(finalBuffer, format.getSampleSizeInBits() / 8); // ADD THIS LINE
                });

                line.write(adjustedBuffer, 0, adjustedBuffer.length);
            }

            line.drain();
            line.close();
            }

        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            completedNaturally = false;
            e.printStackTrace();
        }

        if (completedNaturally && onPlaybackFinished != null) {
            Platform.runLater(onPlaybackFinished);
        }
    }
    
    /**
     * Calculates track gain for normalization.
     */
    private static float calculateTrackGain(File audioFile, AudioFormat format) {
        AudioFormat targetFormat = toPcmFormat(format);
        try (AudioInputStream analysisStream = AudioSystem.getAudioInputStream(targetFormat,
            AudioSystem.getAudioInputStream(audioFile))) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            double sumSquares = 0;
            long sampleCount = 0;
            int samplesAnalyzed = 0;
            int maxSamplesToAnalyze = (int)(format.getSampleRate() * 10);
            
            while ((bytesRead = analysisStream.read(buffer, 0, buffer.length)) != -1 && 
                   samplesAnalyzed < maxSamplesToAnalyze) {
                double[] samples = byteToDouble(Arrays.copyOf(buffer, bytesRead), targetFormat);
                for (double sample : samples) {
                    sumSquares += sample * sample;
                    sampleCount++;
                    samplesAnalyzed++;
                    if (samplesAnalyzed >= maxSamplesToAnalyze) break;
                }
            }
            
            if (sampleCount == 0) return 1.0f;
            
            double rms = Math.sqrt(sumSquares / sampleCount);
            if (rms < 0.001) return 1.0f;

            System.out.println(rms);
            
            float gain = (float)(targetRMS / rms);
            
            System.out.println(gain);

            return Math.min(gain, 4.0f);
            
            
        } catch (Exception e) {
            e.printStackTrace();
            return 1.0f;
        }
    }
    
    /**
     * Applies volume control and auto-leveling with smooth limiting.
     */
    private static byte[] applyVolumeControl(byte[] buffer, AudioFormat format) {
        double[] audioData = byteToDouble(buffer, format);
        
        float totalGain = masterVolume;
        if (autoLevelEnabled) {
            totalGain *= currentTrackGain;
        }
        
        // Apply gain
        for (int i = 0; i < audioData.length; i++) {
            audioData[i] *= totalGain;
        }
        
        // Smoothed envelope limiter (avoids per-buffer pumping)
        double attackCoeff = 0.002;   // fast attack to catch peaks
        double releaseCoeff = 0.0001; // slow release for smooth recovery
        
        for (int i = 0; i < audioData.length; i++) {
            double absVal = Math.abs(audioData[i]);
            if (absVal > limiterEnvelope) {
                limiterEnvelope += attackCoeff * (absVal - limiterEnvelope);
            } else {
                limiterEnvelope += releaseCoeff * (absVal - limiterEnvelope);
            }
            
            if (limiterEnvelope > 0.98) {
                audioData[i] *= 0.98 / limiterEnvelope;
            }
        }
        
        return doubleToByte(audioData, format);
    }
    
    /**
     * Applies echo cancellation.
     */
    private static byte[] applyEchoCancellation(byte[] buffer, AudioFormat format) {
        double[] audioData = byteToDouble(buffer, format);
        
        if (echoBuffer == null) {
            echoBuffer = new double[ECHO_DELAY_SAMPLES];
        }
        
        for (int i = 0; i < audioData.length; i++) {
            double echo = echoBuffer[echoBufferIndex];
            double output = audioData[i] - (echo * ECHO_DECAY);
            echoBuffer[echoBufferIndex] = audioData[i];
            echoBufferIndex = (echoBufferIndex + 1) % ECHO_DELAY_SAMPLES;
            audioData[i] = Math.max(-1.0, Math.min(1.0, output));
        }
        
        return doubleToByte(audioData, format);
    }
    
    /**
     * Calculates current dB level.
     */
    private static double calculatedB(byte[] buffer, AudioFormat format) {
        double[] audioData = byteToDouble(buffer, format);
        
        double sumSquares = 0;
        for (double sample : audioData) {
            sumSquares += sample * sample;
        }
        
        double rms = Math.sqrt(sumSquares / audioData.length);
        
        if (rms < 0.00001) {
            return -60.0;
        }
        
        double db = 20 * Math.log10(rms);
        return Math.max(-60.0, Math.min(0.0, db));
    }

    /**
     * Calculates bar heights for visualizer using logarithmically spaced frequency bands.
     * Uses dB scaling and spectral tilt compensation for uniform visual distribution.
     */
    private static int[] calculateBarHeights(byte[] buffer, int numBars, AudioFormat format) {
        double[] newBuffer = byteToDouble(buffer, format);
        FastFourier fft = new FastFourier(newBuffer);
        fft.transform();
        double[] perFreqMagnitude = fft.getMagnitude(true);

        float sampleRate = format.getSampleRate();
        int usableBins = perFreqMagnitude.length / 2;

        // Normalize magnitudes by N/2 so a full-scale sine → magnitude ~1.0 (0 dB)
        double fftNorm = perFreqMagnitude.length / 2.0;

        double minFreq = 60.0;
        double maxFreq = Math.min(16000.0, sampleRate / 2.0 - 1);
        double freqPerBin = sampleRate / (double) perFreqMagnitude.length;

        // Per-band gain compensation for typical spectral slope (~4.5 dB/octave boost)
        double referenceFreq = minFreq;

        int[] heights = new int[numBars];
        for (int i = 0; i < numBars; i++) {
            double lowFreq = minFreq * Math.pow(maxFreq / minFreq, (double) i / numBars);
            double highFreq = minFreq * Math.pow(maxFreq / minFreq, (double) (i + 1) / numBars);

            int startBin = Math.max(1, (int) (lowFreq / freqPerBin));
            int endBin = Math.min(usableBins - 1, (int) (highFreq / freqPerBin));

            double sum = 0;
            int count = 0;
            for (int j = startBin; j <= endBin; j++) {
                sum += perFreqMagnitude[j] / fftNorm;
                count++;
            }

            double avg = (count > 0) ? sum / count : 0;

            // Convert to dB scale (compresses dynamic range)
            double dB = (avg > 1e-10) ? 20.0 * Math.log10(avg) : -100.0;

            // Apply spectral tilt compensation: boost higher bands ~4.5 dB per octave
            double centerFreq = Math.sqrt(lowFreq * highFreq);
            double octavesAboveRef = Math.log(centerFreq / referenceFreq) / Math.log(2.0);
            dB += octavesAboveRef * 4.5;

            // Map dB to a 0–100 visual range (floor at -60 dB, ceiling at 0 dB)
            double normalized = (dB + 60.0) / 60.0;
            normalized = Math.max(0.0, Math.min(1.0, normalized));

            heights[i] = (int) (normalized * 100);
        }
        return heights;
    }

    /**
     * Returns logarithmically-spaced spectrum data for the spectrum analyzer.
     * Produces numBins output points spaced from 60 Hz to 16 kHz on a log scale,
     * matching human frequency perception.
     */
    private static double[] notSorted(byte[] buffer, int numBars, AudioFormat format) {
        double[] newBuffer = byteToDouble(buffer, format);
        FastFourier fft = new FastFourier(newBuffer);
        fft.transform();
        double[] perFreqMagnitude = fft.getMagnitude(true);

        float sampleRate = format.getSampleRate();
        int usableBins = perFreqMagnitude.length / 2;
        double freqPerBin = sampleRate / (double) perFreqMagnitude.length;

        // Output bins for the spectrum display
        int outputBins = 128;
        double minFreq = 60.0;
        double maxFreq = Math.min(16000.0, sampleRate / 2.0 - 1);

        double[] result = new double[outputBins];
        for (int i = 0; i < outputBins; i++) {
            // Logarithmic band edges
            double lowFreq = minFreq * Math.pow(maxFreq / minFreq, (double) i / outputBins);
            double highFreq = minFreq * Math.pow(maxFreq / minFreq, (double) (i + 1) / outputBins);

            int startBin = Math.max(1, (int) (lowFreq / freqPerBin));
            int endBin = Math.min(usableBins - 1, (int) (highFreq / freqPerBin));

            double sum = 0;
            int count = 0;
            for (int j = startBin; j <= endBin; j++) {
                sum += perFreqMagnitude[j];
                count++;
            }

            result[i] = (count > 0) ? sum / count : 0;
        }
        return result;
    }

    private static AudioFormat toPcmFormat(AudioFormat sourceFormat) {
        if (sourceFormat.getEncoding() == AudioFormat.Encoding.PCM_SIGNED && sourceFormat.getSampleSizeInBits() == 16) {
            return sourceFormat;
        }
        return new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            sourceFormat.getSampleRate(),
            16,
            sourceFormat.getChannels(),
            sourceFormat.getChannels() * 2,
            sourceFormat.getSampleRate(),
            false
        );
    }

    /**
     * Converts byte buffer to double array.
     */
    private static double[] byteToDouble(byte[] buffer, AudioFormat format) {
        int sampleSizeInBytes = format.getSampleSizeInBits() / 8;
        boolean isBigEndian = format.isBigEndian();

        double[] audioData = new double[buffer.length / sampleSizeInBytes];
        for (int i = 0; i < audioData.length; i++) {
            int sampleIndex = i * sampleSizeInBytes;
            int sample = 0;

            if (sampleSizeInBytes == 2) {
                if (isBigEndian) {
                    sample = (buffer[sampleIndex] << 8) | (buffer[sampleIndex + 1] & 0xFF);
                } else {
                    sample = (buffer[sampleIndex + 1] << 8) | (buffer[sampleIndex] & 0xFF);
                }
            } else if (sampleSizeInBytes == 1) {
                sample = buffer[sampleIndex];
            }

            audioData[i] = sample / 32768.0;
        }
        return audioData;
    }

    /**
     * Converts double array back to byte buffer.
     */
    private static byte[] doubleToByte(double[] input, AudioFormat format) {
        int sampleSizeInBytes = format.getSampleSizeInBits() / 8;
        boolean isBigEndian = format.isBigEndian();

        byte[] outputBuffer = new byte[input.length * sampleSizeInBytes];
        for (int i = 0; i < input.length; i++) {
            int sample = (int) (input[i] * 32768);
            sample = Math.max(-32768, Math.min(32767, sample));

            int sampleIndex = i * sampleSizeInBytes;
            if (sampleSizeInBytes == 2) {
                if (isBigEndian) {
                    outputBuffer[sampleIndex] = (byte) (sample >> 8);
                    outputBuffer[sampleIndex + 1] = (byte) sample;
                } else {
                    outputBuffer[sampleIndex] = (byte) sample;
                    outputBuffer[sampleIndex + 1] = (byte) (sample >> 8);
                }
            } else if (sampleSizeInBytes == 1) {
                outputBuffer[sampleIndex] = (byte) sample;
            }
        }
        return outputBuffer;
    }

    /**
     * Computes biquad filter coefficients.
     * Returns [b0, b1, b2, a1, a2] (a0 is normalized to 1).
     */
    private static double[] biquadLowPass(double sampleRate, double cutoff) {
        double w0 = 2.0 * Math.PI * cutoff / sampleRate;
        double alpha = Math.sin(w0) / (2.0 * 0.707); // Q = 0.707 (Butterworth)
        double cosw0 = Math.cos(w0);
        double a0 = 1.0 + alpha;
        return new double[] {
            ((1.0 - cosw0) / 2.0) / a0,
            (1.0 - cosw0) / a0,
            ((1.0 - cosw0) / 2.0) / a0,
            (-2.0 * cosw0) / a0,
            (1.0 - alpha) / a0
        };
    }

    private static double[] biquadHighPass(double sampleRate, double cutoff) {
        double w0 = 2.0 * Math.PI * cutoff / sampleRate;
        double alpha = Math.sin(w0) / (2.0 * 0.707);
        double cosw0 = Math.cos(w0);
        double a0 = 1.0 + alpha;
        return new double[] {
            ((1.0 + cosw0) / 2.0) / a0,
            (-(1.0 + cosw0)) / a0,
            ((1.0 + cosw0) / 2.0) / a0,
            (-2.0 * cosw0) / a0,
            (1.0 - alpha) / a0
        };
    }

    /**
     * Applies a biquad filter in-place with persistent state.
     * state: [x[n-1], x[n-2], y[n-1], y[n-2]]
     * coeffs: [b0, b1, b2, a1, a2]
     */
    private static double[] applyBiquad(double[] input, double[] coeffs, double[] state) {
        double b0 = coeffs[0], b1 = coeffs[1], b2 = coeffs[2];
        double a1 = coeffs[3], a2 = coeffs[4];
        double x1 = state[0], x2 = state[1], y1 = state[2], y2 = state[3];

        double[] output = new double[input.length];
        for (int i = 0; i < input.length; i++) {
            double x0 = input[i];
            double y0 = b0 * x0 + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
            output[i] = y0;
            x2 = x1;
            x1 = x0;
            y2 = y1;
            y1 = y0;
        }

        state[0] = x1;
        state[1] = x2;
        state[2] = y1;
        state[3] = y2;
        return output;
    }

    /**
     * Applies EQ filters using persistent biquad IIR filters (stateful across buffers).
     */
    private static byte[] applyEQ(byte[] buffer, AudioFormat format, float bassGain, float midGain, float trebleGain) {
        float sampleRate = format.getSampleRate();
        double[] audioData = byteToDouble(buffer, format);

        // Compute coefficients (could cache if sample rate doesn't change)
        double[] bassCoeffs = biquadLowPass(sampleRate, 200.0);
        double[] midLpCoeffs = biquadLowPass(sampleRate, 2000.0);
        double[] midHpCoeffs = biquadHighPass(sampleRate, 200.0);
        double[] trebleCoeffs = biquadHighPass(sampleRate, 2000.0);

        // Apply filters with persistent state
        double[] bassFiltered = applyBiquad(audioData, bassCoeffs, bassState);
        double[] midFiltered = applyBiquad(audioData, midLpCoeffs, midState1);
        midFiltered = applyBiquad(midFiltered, midHpCoeffs, midState2);
        double[] trebleFiltered = applyBiquad(audioData, trebleCoeffs, trebleState);

        // Apply gains
        double bassScale = Math.pow(10, bassGain / 20.0);
        double midScale = Math.pow(10, midGain / 20.0);
        double trebleScale = Math.pow(10, trebleGain / 20.0);

        double[] combined = new double[audioData.length];
        for (int i = 0; i < combined.length; i++) {
            combined[i] = bassFiltered[i] * bassScale
                        + midFiltered[i] * midScale
                        + trebleFiltered[i] * trebleScale;
        }

        return doubleToByte(applyLimiter(combined), format);
    }

    private static double[] applyLimiter(double[] audioData) {
        double maxAbs = 0.0;
        for (double sample : audioData) {
            maxAbs = Math.max(maxAbs, Math.abs(sample));
        }
        if (maxAbs <= 1.0) {
            return audioData;
        }
        double scale = 0.98 / maxAbs;
        for (int i = 0; i < audioData.length; i++) {
            audioData[i] *= scale;
        }
        return audioData;
    }
}


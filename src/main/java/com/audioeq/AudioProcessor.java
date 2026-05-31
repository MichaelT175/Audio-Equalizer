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
    
    // Stateful biquad filters that persist between audio frames
    private static BiquadFilter[] bassFilters;
    private static BiquadFilter[] midHighPassFilters;
    private static BiquadFilter[] midLowPassFilters;
    private static BiquadFilter[] trebleFilters;
    private static float lastSampleRate = -1;
    private static int lastChannels = -1;
    
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

    /**
     * Biquad IIR filter (Direct Form II Transposed) that maintains state between frames.
     */
    private static class BiquadFilter {
        private double b0, b1, b2, a1, a2;
        private double z1 = 0, z2 = 0;

        public BiquadFilter(double b0, double b1, double b2, double a0, double a1, double a2) {
            this.b0 = b0 / a0;
            this.b1 = b1 / a0;
            this.b2 = b2 / a0;
            this.a1 = a1 / a0;
            this.a2 = a2 / a0;
        }

        public double process(double input) {
            double output = b0 * input + z1;
            z1 = b1 * input - a1 * output + z2;
            z2 = b2 * input - a2 * output;
            return output;
        }

        public static BiquadFilter lowPass(double sampleRate, double cutoff) {
            double w0 = 2.0 * Math.PI * cutoff / sampleRate;
            double alpha = Math.sin(w0) / (2.0 * Math.sqrt(2.0));
            double cosW0 = Math.cos(w0);
            double b0 = (1.0 - cosW0) / 2.0;
            double b1 = 1.0 - cosW0;
            double b2 = (1.0 - cosW0) / 2.0;
            double a0 = 1.0 + alpha;
            double a1 = -2.0 * cosW0;
            double a2 = 1.0 - alpha;
            return new BiquadFilter(b0, b1, b2, a0, a1, a2);
        }

        public static BiquadFilter highPass(double sampleRate, double cutoff) {
            double w0 = 2.0 * Math.PI * cutoff / sampleRate;
            double alpha = Math.sin(w0) / (2.0 * Math.sqrt(2.0));
            double cosW0 = Math.cos(w0);
            double b0 = (1.0 + cosW0) / 2.0;
            double b1 = -(1.0 + cosW0);
            double b2 = (1.0 + cosW0) / 2.0;
            double a0 = 1.0 + alpha;
            double a1 = -2.0 * cosW0;
            double a2 = 1.0 - alpha;
            return new BiquadFilter(b0, b1, b2, a0, a1, a2);
        }
    }

    /**
     * Initializes the stateful filters if the format has changed.
     */
    private static void initFilters(float sampleRate, int channels) {
        if (sampleRate != lastSampleRate || channels != lastChannels) {
            lastSampleRate = sampleRate;
            lastChannels = channels;
            bassFilters = new BiquadFilter[channels];
            midHighPassFilters = new BiquadFilter[channels];
            midLowPassFilters = new BiquadFilter[channels];
            trebleFilters = new BiquadFilter[channels];
            for (int ch = 0; ch < channels; ch++) {
                bassFilters[ch] = BiquadFilter.lowPass(sampleRate, 200.0);
                midHighPassFilters[ch] = BiquadFilter.highPass(sampleRate, 200.0);
                midLowPassFilters[ch] = BiquadFilter.lowPass(sampleRate, 2000.0);
                trebleFilters[ch] = BiquadFilter.highPass(sampleRate, 2000.0);
            }
        }
    }

    /**
     * Resets filter states for new playback.
     */
    private static void resetFilters() {
        lastSampleRate = -1;
        lastChannels = -1;
    }

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
        resetFilters();
        echoBuffer = null;
        echoBufferIndex = 0;
        currentTrackGain = 1.0f;
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

            return Math.min(gain, 20.0f);
            
            
        } catch (Exception e) {
            e.printStackTrace();
            return 1.0f;
        }
    }
    
    /**
     * Applies volume control and auto-leveling.
     */
    private static byte[] applyVolumeControl(byte[] buffer, AudioFormat format) {
        double[] audioData = byteToDouble(buffer, format);
        
        float totalGain = masterVolume;
        if (autoLevelEnabled) {
            totalGain *= currentTrackGain;
        }
        
        double maxAbs = 0.0;
        for (int i = 0; i < audioData.length; i++) {
            audioData[i] *= totalGain;
            maxAbs = Math.max(maxAbs, Math.abs(audioData[i]));
        }

        if (maxAbs > 1.0) {
            double scale = 0.98 / maxAbs;
            for (int i = 0; i < audioData.length; i++) {
                audioData[i] *= scale;
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
     * Calculates bar heights for visualizer.
     */
    private static int[] calculateBarHeights(byte[] buffer, int numBars, AudioFormat format) {
        double[] newBuffer = byteToDouble(buffer, format);
        FastFourier fft = new FastFourier(newBuffer);
        fft.transform();
        double[] perFreqMagnitude = fft.getMagnitude(true);
        int numBinsPerBar = perFreqMagnitude.length / numBars;

        int[] heights = new int[numBars];
        for (int i = 0; i < heights.length; i++) {
            double barWeight = 0;
            int start = i * numBinsPerBar + 1;
            int end = (i + 1) * numBinsPerBar;
            for (int j = start; j < end; j++) {
                barWeight += perFreqMagnitude[j];
            }
            heights[i] = (int)(barWeight);
        }
        return heights;
    }

    /**
     * Returns spectrum data for spectrum analyzer.
     */
    private static double[] notSorted(byte[] buffer, int numBars, AudioFormat format) {
        double[] newBuffer = byteToDouble(buffer, format);
        FastFourier fft = new FastFourier(newBuffer);
        fft.transform();
        double[] perFreqMagnitude = fft.getMagnitude(true);
        double[] arr = new double[perFreqMagnitude.length / 2];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = perFreqMagnitude[i];
        }
        return arr;
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
     * Applies EQ using stateful biquad filters that preserve state between frames.
     */
    private static byte[] applyEQ(byte[] buffer, AudioFormat format, float bassGain, float midGain, float trebleGain) {
        float sampleRate = format.getSampleRate();
        int channels = format.getChannels();

        // Initialize filters (only recreates if format changed)
        initFilters(sampleRate, channels);

        double[] audioData = byteToDouble(buffer, format);

        // Pre-compute linear gains from dB
        double bassLinear = Math.pow(10, bassGain / 20.0);
        double midLinear = Math.pow(10, midGain / 20.0);
        double trebleLinear = Math.pow(10, trebleGain / 20.0);

        // Process sample-by-sample with stateful filters
        double[] combined = new double[audioData.length];
        for (int i = 0; i < audioData.length; i++) {
            int ch = i % channels;
            double sample = audioData[i];

            // Bass: Low-pass filter at 200 Hz
            double bassSample = bassFilters[ch].process(sample) * bassLinear;

            // Mid: Band-pass (high-pass at 200 Hz cascaded with low-pass at 2000 Hz)
            double midSample = midLowPassFilters[ch].process(midHighPassFilters[ch].process(sample)) * midLinear;

            // Treble: High-pass filter at 2000 Hz
            double trebleSample = trebleFilters[ch].process(sample) * trebleLinear;

            combined[i] = bassSample + midSample + trebleSample;
            combined[i] = Math.max(-1.5, Math.min(1.5, combined[i]));
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


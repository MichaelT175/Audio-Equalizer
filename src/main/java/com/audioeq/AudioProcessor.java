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

import com.github.psambit9791.jdsp.filter.Butterworth;
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
     * Calculates bar heights for visualizer using logarithmically spaced frequency bands.
     * This matches human hearing perception where each octave gets equal visual weight.
     */
    private static int[] calculateBarHeights(byte[] buffer, int numBars, AudioFormat format) {
        double[] newBuffer = byteToDouble(buffer, format);
        FastFourier fft = new FastFourier(newBuffer);
        fft.transform();
        double[] perFreqMagnitude = fft.getMagnitude(true);

        float sampleRate = format.getSampleRate();
        // Only use the first half of the FFT (positive frequencies up to Nyquist)
        int usableBins = perFreqMagnitude.length / 2;

        // Logarithmically spaced band edges from ~60 Hz to ~16 kHz (or Nyquist if lower)
        double minFreq = 60.0;
        double maxFreq = Math.min(16000.0, sampleRate / 2.0 - 1);
        double freqPerBin = sampleRate / (double) perFreqMagnitude.length;

        int[] heights = new int[numBars];
        for (int i = 0; i < numBars; i++) {
            // Logarithmic band edges
            double lowFreq = minFreq * Math.pow(maxFreq / minFreq, (double) i / numBars);
            double highFreq = minFreq * Math.pow(maxFreq / minFreq, (double) (i + 1) / numBars);

            int startBin = Math.max(1, (int) (lowFreq / freqPerBin));
            int endBin = Math.min(usableBins - 1, (int) (highFreq / freqPerBin));

            double sum = 0;
            int count = 0;
            for (int j = startBin; j <= endBin; j++) {
                sum += perFreqMagnitude[j];
                count++;
            }

            // Average magnitude for this band (avoids higher bars just because more bins)
            double avg = (count > 0) ? sum / count : 0;
            heights[i] = (int) avg;
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
     * Applies EQ filters.
     */
    private static byte[] applyEQ(byte[] buffer, AudioFormat format, float bassGain, float midGain, float trebleGain) {
        float sampleRate = format.getSampleRate();
        double[] audioData = byteToDouble(buffer, format);
        
        Butterworth butterworth = new Butterworth(sampleRate);

        double[] bassFiltered = butterworth.lowPassFilter(audioData, 2, 200.0);
        for (int i = 0; i < bassFiltered.length; i++) {
            bassFiltered[i] *= Math.pow(10, bassGain / 20);
        }

        double[] midFiltered = butterworth.bandPassFilter(audioData, 2, 200.0, 2000.0);
        for (int i = 0; i < midFiltered.length; i++) {
            midFiltered[i] *= Math.pow(10, midGain / 20);
        }

        double[] trebleFiltered = butterworth.highPassFilter(audioData, 2, 2000.0);
        for (int i = 0; i < trebleFiltered.length; i++) {
            trebleFiltered[i] *= Math.pow(10, trebleGain / 20);
        }

        double[] combined = new double[audioData.length];
        for (int i = 0; i < combined.length; i++) {
            combined[i] = bassFiltered[i] + midFiltered[i] + trebleFiltered[i];
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


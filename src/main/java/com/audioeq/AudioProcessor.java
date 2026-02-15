package com.audioeq;


import javax.sound.sampled.*;
import javafx.application.Platform;
import com.github.psambit9791.jdsp.filter.Butterworth;
import com.github.psambit9791.jdsp.transform.FastFourier;
import java.io.*; 

/**
 * The AudioProcessor class handles audio playback with equalization (EQ) effects.
 * Updated to work with JavaFX visualizers.
 */
public class AudioProcessor {

    private static boolean stopPlayback = false;
    private static boolean isPaused = false;
    private static final Object pauseLock = new Object();
    
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
                                      SpectrumCanvasFX spectrumCanvas) {
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
        stopPlayback = false;

        try (AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile)) {
            AudioFormat format = audioInputStream.getFormat();

            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            if (!AudioSystem.isLineSupported(info)) {
                throw new UnsupportedAudioFileException("Audio format not supported");
            }

            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open();
            line.start();
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            int numBars = 10;
            
            // Calculate track gain for auto-leveling
            if (autoLevelEnabled) {
                currentTrackGain = calculateTrackGain(audioFile, format);
            }

            while ((bytesRead = audioInputStream.read(buffer, 0, buffer.length)) != -1) {
                if (stopPlayback) {
                    line.stop();
                    line.close();
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
                
                // Apply EQ
                byte[] adjustedBuffer = applyEQ(buffer, format, gains[0], gains[1], gains[2]);
                
                // Apply volume and auto-leveling
                adjustedBuffer = applyVolumeControl(adjustedBuffer, format);
                
                // Apply echo cancellation
                if (echoCancelEnabled) {
                    adjustedBuffer = applyEchoCancellation(adjustedBuffer, format);
                }
                
                // Calculate dB
                currentdB = calculatedB(adjustedBuffer, format);
                
                // Update visualizers on JavaFX thread
                int[] barHeights = calculateBarHeights(adjustedBuffer, numBars, format);
                double[] spectrumData = notSorted(buffer, numBars, format);
                
                Platform.runLater(() -> {
                    visualizer.updateVisualizer(barHeights);
                    spectrumCanvas.updateSpectrum(spectrumData);
                });
                
                line.write(adjustedBuffer, 0, bytesRead);
            }

            line.drain();
            line.close();

        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Calculates track gain for normalization.
     */
    private static float calculateTrackGain(File audioFile, AudioFormat format) {
        try (AudioInputStream analysisStream = AudioSystem.getAudioInputStream(audioFile)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            double sumSquares = 0;
            long sampleCount = 0;
            int samplesAnalyzed = 0;
            int maxSamplesToAnalyze = (int)(format.getSampleRate() * 10);
            
            while ((bytesRead = analysisStream.read(buffer, 0, buffer.length)) != -1 && 
                   samplesAnalyzed < maxSamplesToAnalyze) {
                double[] samples = byteToDouble(buffer, format);
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

            return Math.min(gain, 10.0f);
            
            
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
        
        for (int i = 0; i < audioData.length; i++) {
            audioData[i] *= totalGain;
            audioData[i] = Math.max(-1.0, Math.min(1.0, audioData[i]));
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
    private static byte[] applyEQ(byte[] buffer, AudioFormat format, float bassGain, 
                                 float midGain, float trebleGain) {
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
            combined[i] = Math.max(-1.0, Math.min(1.0, combined[i]));
        }

        return doubleToByte(combined, format);
    }
}

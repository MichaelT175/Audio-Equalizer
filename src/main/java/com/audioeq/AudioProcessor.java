package com.audioeq;

import java.io.File;
import java.io.IOException;

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
 * High-quality audio processor with real-time EQ, reverb, and limiter.
 */
public class AudioProcessor {

    private static volatile boolean stopPlayback = false;
    private static volatile boolean isPaused = false;
    private static final Object pauseLock = new Object();

    private static volatile float masterVolume = 1.0f;
    private static volatile boolean autoLevelEnabled = true;
    private static volatile float targetRMS = 0.11f;
    private static volatile float currentTrackGain = 1.0f;

    private static volatile boolean limiterEnabled = true;
    private static volatile ReverbPreset reverbPreset = ReverbPreset.STUDIO;
    private static volatile float reverbIntensity = 0.0f;
    private static volatile float stereoWidth = 1.0f;

    private static double currentdB = -60.0;

    public static void stopAudioPlayback(boolean stop) {
        stopPlayback = stop;
        if (stop) {
            unpauseAudioPlayback();
        }
    }

    public static void pauseAudioPlayback() {
        isPaused = true;
    }

    public static void unpauseAudioPlayback() {
        synchronized (pauseLock) {
            isPaused = false;
            pauseLock.notifyAll();
        }
    }

    public static void setMasterVolume(float volume) {
        masterVolume = Math.max(0.0f, Math.min(2.0f, volume));
    }

    public static float getMasterVolume() {
        return masterVolume;
    }

    public static void setAutoLevelEnabled(boolean enabled) {
        autoLevelEnabled = enabled;
    }

    public static void setLimiterEnabled(boolean enabled) {
        limiterEnabled = enabled;
    }

    public static void setReverbPreset(ReverbPreset preset) {
        if (preset != null) {
            reverbPreset = preset;
        }
    }

    public static void setReverbIntensity(float intensity) {
        reverbIntensity = Math.max(0.0f, Math.min(1.0f, intensity));
    }

    public static void setStereoWidth(float width) {
        stereoWidth = Math.max(0.2f, Math.min(2.0f, width));
    }

    public static double getCurrentdB() {
        return currentdB;
    }

    /**
     * Plays audio with high-quality EQ and effects.
     */
    public static void playAudioWithEQ(String filePath, float initialBassGain, float initialMidGain,
                                      float initialTrebleGain, VisualizerCanvasFX visualizer,
                                      SpectrumCanvasFX spectrumCanvas, WaveformCanvasFX waveformCanvas, EqualizerApp eq) {
        File audioFile = new File(filePath);
        if (!audioFile.exists()) {
            System.out.println("Error: File not found at " + filePath);
            return;
        }

        stopPlayback = false;
        isPaused = false;
        currentTrackGain = 1.0f;

        try (AudioInputStream inputStream = AudioSystem.getAudioInputStream(audioFile)) {
            AudioFormat baseFormat = inputStream.getFormat();
            AudioFormat targetFormat = createTargetFormat(baseFormat);

            if (!AudioSystem.isConversionSupported(targetFormat, baseFormat)) {
                throw new UnsupportedAudioFileException("Audio format not supported for PCM conversion");
            }

            try (AudioInputStream decodedStream = AudioSystem.getAudioInputStream(targetFormat, inputStream)) {
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, targetFormat);
                if (!AudioSystem.isLineSupported(info)) {
                    throw new UnsupportedAudioFileException("Audio format not supported");
                }

                int channels = targetFormat.getChannels();
                int frameSize = targetFormat.getFrameSize();
                int framesPerBuffer = 1024;
                int bufferSize = framesPerBuffer * frameSize;

                SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(targetFormat, bufferSize * 4);
                line.start();

                byte[] buffer = new byte[bufferSize];
                byte[] outputBuffer = new byte[bufferSize];
                float[] samples = new float[framesPerBuffer * channels];
                double[] fftInput = new double[framesPerBuffer];

                EqualizerDSP eqProcessor = new EqualizerDSP(targetFormat.getSampleRate(), channels);
                ReverbProcessor reverbProcessor = new ReverbProcessor(targetFormat.getSampleRate());
                StereoWidthProcessor widthProcessor = new StereoWidthProcessor();
                LimiterDSP limiter = new LimiterDSP();

                ReverbPreset lastPreset = reverbPreset;
                float lastReverb = reverbIntensity;
                float lastWidth = stereoWidth;
                boolean lastLimiter = limiterEnabled;

                if (autoLevelEnabled) {
                    currentTrackGain = calculateTrackGain(audioFile, targetFormat);
                }

                int bytesRead;
                int numBars = 12;

                while ((bytesRead = decodedStream.read(buffer, 0, buffer.length)) != -1) {
                    if (stopPlayback) {
                        line.flush();
                        line.stop();
                        line.close();
                        break;
                    }

                    synchronized (pauseLock) {
                        while (isPaused) {
                            try {
                                line.stop();
                                pauseLock.wait();
                                line.start();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }

                    int framesRead = bytesRead / frameSize;
                    int sampleCount = framesRead * channels;

                    decodePcm16(buffer, samples, sampleCount, targetFormat.isBigEndian());

                    eqProcessor.setTargetGains(eq.getBassSliderValue(), eq.getMidSliderValue(), eq.getTrebleSliderValue());
                    eqProcessor.updateCoefficients();

                    if (lastPreset != reverbPreset) {
                        reverbProcessor.applyPreset(reverbPreset);
                        lastPreset = reverbPreset;
                    }
                    if (lastReverb != reverbIntensity) {
                        reverbProcessor.setIntensity(reverbIntensity);
                        lastReverb = reverbIntensity;
                    }
                    if (lastWidth != stereoWidth) {
                        widthProcessor.setWidth(stereoWidth);
                        lastWidth = stereoWidth;
                    }
                    if (lastLimiter != limiterEnabled) {
                        limiter.setEnabled(limiterEnabled);
                        lastLimiter = limiterEnabled;
                    }

                    double preampGain = dbToLinear(eqProcessor.getPreampDb());
                    double trackGain = autoLevelEnabled ? currentTrackGain : 1.0;
                    double totalGain = masterVolume * trackGain * preampGain;

                    float[] stereoPair = new float[2];
                    for (int frame = 0; frame < framesRead; frame++) {
                        int base = frame * channels;

                        float left = eqProcessor.process(samples[base], 0) * (float) totalGain;
                        float right = left;
                        if (channels > 1) {
                            right = eqProcessor.process(samples[base + 1], 1) * (float) totalGain;
                        }

                        stereoPair[0] = left;
                        stereoPair[1] = right;

                        reverbProcessor.process(stereoPair);
                        widthProcessor.process(stereoPair);

                        stereoPair[0] = limiter.process(stereoPair[0]);
                        stereoPair[1] = limiter.process(stereoPair[1]);

                        samples[base] = stereoPair[0];
                        if (channels > 1) {
                            samples[base + 1] = stereoPair[1];
                        }
                    }

                    encodePcm16(samples, sampleCount, targetFormat.isBigEndian(), outputBuffer);
                    currentdB = calculateDb(samples, sampleCount);

                    for (int i = 0; i < framesRead; i++) {
                        double mono = samples[i * channels];
                        if (channels > 1) {
                            mono = (samples[i * channels] + samples[i * channels + 1]) * 0.5;
                        }
                        fftInput[i] = mono;
                    }
                    for (int i = framesRead; i < fftInput.length; i++) {
                        fftInput[i] = 0.0;
                    }

                    FastFourier fft = new FastFourier(fftInput);
                    fft.transform();
                    double[] magnitudes = fft.getMagnitude(true);
                    int[] barHeights = calculateBarHeights(magnitudes, numBars);
                    double[] spectrumData = extractSpectrum(magnitudes);

                    Platform.runLater(() -> {
                        visualizer.updateVisualizer(barHeights);
                        spectrumCanvas.updateSpectrum(spectrumData);
                        waveformCanvas.updateWaveform(outputBuffer, targetFormat.getSampleSizeInBits() / 8);
                    });

                    line.write(outputBuffer, 0, framesRead * frameSize);
                }

                line.drain();
                line.close();
            }
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    private static AudioFormat createTargetFormat(AudioFormat baseFormat) {
        return new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            baseFormat.getSampleRate(),
            16,
            baseFormat.getChannels(),
            baseFormat.getChannels() * 2,
            baseFormat.getSampleRate(),
            false
        );
    }

    private static float calculateTrackGain(File audioFile, AudioFormat targetFormat) {
        try (AudioInputStream analysisStream = AudioSystem.getAudioInputStream(audioFile)) {
            if (!AudioSystem.isConversionSupported(targetFormat, analysisStream.getFormat())) {
                return 1.0f;
            }

            try (AudioInputStream decodedStream = AudioSystem.getAudioInputStream(targetFormat, analysisStream)) {
                byte[] buffer = new byte[4096];
                float[] samples = new float[2048 * targetFormat.getChannels()];

                int bytesRead;
                double sumSquares = 0;
                long sampleCount = 0;
                long maxSamplesToAnalyze = (long) (targetFormat.getSampleRate() * 8);

                while ((bytesRead = decodedStream.read(buffer, 0, buffer.length)) != -1 &&
                       sampleCount < maxSamplesToAnalyze) {
                    int framesRead = bytesRead / targetFormat.getFrameSize();
                    int sampleLimit = framesRead * targetFormat.getChannels();

                    decodePcm16(buffer, samples, sampleLimit, targetFormat.isBigEndian());
                    for (int i = 0; i < sampleLimit && sampleCount < maxSamplesToAnalyze; i++) {
                        double sample = samples[i];
                        sumSquares += sample * sample;
                        sampleCount++;
                    }
                }

                if (sampleCount == 0) {
                    return 1.0f;
                }

                double rms = Math.sqrt(sumSquares / sampleCount);
                if (rms < 0.001) {
                    return 1.0f;
                }

                float gain = (float) (targetRMS / rms);
                return Math.min(gain, 8.0f);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 1.0f;
        }
    }

    private static void decodePcm16(byte[] buffer, float[] target, int sampleCount, boolean bigEndian) {
        for (int i = 0; i < sampleCount; i++) {
            int idx = i * 2;
            int sample;
            if (bigEndian) {
                sample = (buffer[idx] << 8) | (buffer[idx + 1] & 0xFF);
            } else {
                sample = (buffer[idx + 1] << 8) | (buffer[idx] & 0xFF);
            }
            target[i] = sample / 32768.0f;
        }
    }

    private static void encodePcm16(float[] samples, int sampleCount, boolean bigEndian, byte[] target) {
        for (int i = 0; i < sampleCount; i++) {
            float clamped = Math.max(-1.0f, Math.min(1.0f, samples[i]));
            int sample = (int) (clamped * 32767.0f);

            int idx = i * 2;
            if (bigEndian) {
                target[idx] = (byte) (sample >> 8);
                target[idx + 1] = (byte) sample;
            } else {
                target[idx] = (byte) sample;
                target[idx + 1] = (byte) (sample >> 8);
            }
        }
    }

    private static double calculateDb(float[] samples, int sampleCount) {
        double sumSquares = 0.0;
        for (int i = 0; i < sampleCount; i++) {
            double sample = samples[i];
            sumSquares += sample * sample;
        }
        double rms = Math.sqrt(sumSquares / Math.max(1, sampleCount));
        if (rms < 0.00001) {
            return -60.0;
        }
        double db = 20.0 * Math.log10(rms);
        return Math.max(-60.0, Math.min(0.0, db));
    }

    private static int[] calculateBarHeights(double[] magnitudes, int numBars) {
        int[] heights = new int[numBars];
        int binsPerBar = Math.max(1, magnitudes.length / numBars);

        for (int i = 0; i < numBars; i++) {
            double total = 0.0;
            int start = i * binsPerBar;
            int end = Math.min(magnitudes.length, start + binsPerBar);
            for (int j = start; j < end; j++) {
                total += magnitudes[j];
            }
            heights[i] = (int) Math.min(200, total / binsPerBar);
        }
        return heights;
    }

    private static double[] extractSpectrum(double[] magnitudes) {
        int len = magnitudes.length / 2;
        double[] spectrum = new double[len];
        System.arraycopy(magnitudes, 0, spectrum, 0, len);
        return spectrum;
    }

    private static double dbToLinear(double db) {
        return Math.pow(10.0, db / 20.0);
    }
}

package pleaseload;

import javax.sound.sampled.*;
import javax.swing.*;
import com.github.psambit9791.jdsp.transform.FastFourier;
import java.io.*; 

/**
 * The AudioProcessor class handles audio playback with equalization (EQ) effects.
 */
public class AudioProcessor {

    static EQGUI eq = new EQGUI();
    private static boolean stopPlayback = false;
    private static boolean isPaused = false;
    private static final Object pauseLock = new Object();

    // Stateful biquad filters that persist between audio frames
    private static BiquadFilter[] bassFilters;
    private static BiquadFilter[] midHighPassFilters;
    private static BiquadFilter[] midLowPassFilters;
    private static BiquadFilter[] trebleFilters;
    private static float lastSampleRate = -1;
    private static int lastChannels = -1;

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

        public void reset() {
            z1 = 0;
            z2 = 0;
        }

        public double process(double input) {
            double output = b0 * input + z1;
            z1 = b1 * input - a1 * output + z2;
            z2 = b2 * input - a2 * output;
            return output;
        }

        public static BiquadFilter lowPass(double sampleRate, double cutoff) {
            double w0 = 2.0 * Math.PI * cutoff / sampleRate;
            double alpha = Math.sin(w0) / (2.0 * Math.sqrt(2.0)); // Q = 1/sqrt(2) for Butterworth
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
     *
     * @param stop true to stop playback, false otherwise
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
            pauseLock.notifyAll(); // Resume playback
        }
    }

    /**
     * Plays the audio with the applied EQ.
     *
     * @param filePath the path to the audio file
     * @param initialBassGain the initial gain for the bass frequency
     * @param initialMidGain the initial gain for the mid frequency
     * @param initialTrebleGain the initial gain for the treble frequency
     * @param visualizer the VisualizerPanel to constantly update visualizations
     * @param sPanel the SpectrumPanel to constantly update the spectrum
     */
    public static void playAudioWithEQ(String filePath, float initialBassGain, float initialMidGain, float initialTrebleGain, VisualizerPanel visualizer, SpectrumPanel sPanel) {
        File audioFile = new File(filePath);
        if (!audioFile.exists()) {
            System.out.println("Error: File not found at " + filePath);
            return;
        }
        
        float[] gains = new float[] {initialBassGain, initialMidGain, initialTrebleGain};

        // Reset filters for new playback session
        resetFilters();

        // tries to open the audio file and gets its format
        try (AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile)) { //"Opens" file
            AudioFormat format = audioInputStream.getFormat(); //Used later in the project

            //Creates line Object to check if this computer is able to play this audio
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format); //Specifies SourceDataLine for playback
            if (!AudioSystem.isLineSupported(info)) {
                throw new UnsupportedAudioFileException("Audio format not supported");
            }

            // Prepares system for audio output by opening a line (Channel for audio data)
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(); //Prepares line to receive audio data
            line.start();
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            int numBars = 10;

            //reads small pieces of the audio file and for each piece applies changes, then plays it
            while ((bytesRead = audioInputStream.read(buffer, 0, buffer.length)) != -1) {

                    // Check for stop request
                    if (stopPlayback) {
                        line.stop();
                        line.close();
                        break;
                    }

                    // Pause handling
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

                // Apply EQ to the current buffer
                byte[] adjustedBuffer = applyEQ(buffer, format, gains[0], gains[1], gains[2]);
                int[] barHeights = calculateBarHeights(adjustedBuffer, numBars, format);

                SwingUtilities.invokeLater(() -> visualizer.updateVisualizer(barHeights));
                SwingUtilities.invokeLater(() -> sPanel.updateSpectrum(notSorted(buffer, numBars, format)));
                //SwingUtilities.invokeLater(() -> sPanel.updateSpectrum(byteToDouble(buffer, format)));
                line.write(adjustedBuffer, 0, bytesRead);
            }

            //cleanup
            line.drain();
            line.close();

        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    /**
     * Calculates the heights of bars based on the frequency magnitudes.
     *
     * @param buffer The audio buffer in byte format.
     * @param numBars The number of bars to calculate.
     * @param format The audio format of the buffer.
     * @return An array of bar heights.
     */
    private static int[] calculateBarHeights(byte[] buffer, int numBars, AudioFormat format) {

        double[] newBuffer = byteToDouble(buffer, format);

        FastFourier fft = new FastFourier(newBuffer);

        fft.transform();
        double[] perFreqMagnitude = fft.getMagnitude(true);
        int numBinsPerBar = perFreqMagnitude.length/numBars;

        int[] heights = new int[numBars];

        for (int i = 0; i < heights.length; i++) {
            double barWeight = 0;
            int start = i * numBinsPerBar + 1;
            int end = (i+1) * numBinsPerBar;
            for(int j = start; j < end; j++){
                barWeight += perFreqMagnitude[j];
            }
            heights[i] = (int)(barWeight);
        }

        return heights;
    }    

    /**
     * Returns a non-sorted array of frequency magnitudes.
     *
     * @param buffer The audio buffer in byte format.
     * @param numBars The number of bars to calculate.
     * @param format The audio format of the buffer.
     * @return An array of frequency magnitudes.
     */
    private static double[] notSorted(byte[] buffer, int numBars, AudioFormat format){
        double[] newBuffer = byteToDouble(buffer, format);
        
        FastFourier fft = new FastFourier(newBuffer);
        fft.transform();
        double[] perFreqMagnitude = fft.getMagnitude(true);
        double[] arr = new double[perFreqMagnitude.length/2];
        for(int i = 0; i < arr.length; i++){
            arr[i] = perFreqMagnitude[i];
        }
        return arr;
    }

    /**
     * Converts the buffer from byte to double array.
     *
     * @param buffer The audio buffer in byte format.
     * @param format The audio format of the buffer.
     * @return The audio buffer in double format.
     */
    private static double[] byteToDouble(byte[] buffer, AudioFormat format) {
        int sampleSizeInBytes = format.getSampleSizeInBits() / 8; // Calculate sample size in bytes. 
        boolean isBigEndian = format.isBigEndian();

        double[] audioData = new double[buffer.length / sampleSizeInBytes]; // Initialize double array for audio data
        for (int i = 0; i < audioData.length; i++) {
            int sampleIndex = i * sampleSizeInBytes; // Calculate sample index in the buffer
            int sample = 0;

            if (sampleSizeInBytes == 2) { //16 bit
                if (isBigEndian) {
                    sample = (buffer[sampleIndex] << 8) | (buffer[sampleIndex + 1] & 0xFF); // Convert big-endian bytes to sample
                } else {
                    sample = (buffer[sampleIndex + 1] << 8) | (buffer[sampleIndex] & 0xFF); // Convert little-endian bytes to sample
                }
            } else if (sampleSizeInBytes == 1) { //8 bit
                sample = buffer[sampleIndex]; // Assign byte value to sample
            }

            audioData[i] = sample / 32768.0; // Normalize sample to -1.0 to 1.0 range
        }
        return audioData; // Return the double array of audio data
    }

    /**
     * Converts the buffer back to a byte array for playback.
     *
     * @param input The audio buffer in double format.
     * @param format The audio format of the buffer.
     * @return The audio buffer in byte format.
     */
    private static byte[] doubleToByte(double[] input, AudioFormat format) {
        int sampleSizeInBytes = format.getSampleSizeInBits() / 8; // Calculate sample size in bytes
        boolean isBigEndian = format.isBigEndian(); // Check if the format is big-endian

        byte[] outputBuffer = new byte[input.length * sampleSizeInBytes]; // Initialize byte array for output buffer
        for (int i = 0; i < input.length; i++) {
            int sample = (int) (input[i] * 32768); // Convert double sample back to integer
            sample = Math.max(-32768, Math.min(32767, sample)); // Clamp sample to valid range

            int sampleIndex = i * sampleSizeInBytes; // Calculate sample index in the output buffer
            if (sampleSizeInBytes == 2) { // If sample size is 2 bytes
                if (isBigEndian) {
                    outputBuffer[sampleIndex] = (byte) (sample >> 8); // Convert sample to big-endian bytes
                    outputBuffer[sampleIndex + 1] = (byte) sample;
                } else {
                    outputBuffer[sampleIndex] = (byte) sample; // Convert sample to little-endian bytes
                    outputBuffer[sampleIndex + 1] = (byte) (sample >> 8);
                }
            } else if (sampleSizeInBytes == 1) { // If sample size is 1 byte
                outputBuffer[sampleIndex] = (byte) sample; // Assign sample value to byte
            }
        }

        return outputBuffer; // Return the byte array of output buffer
    }

    /**
     * Applies equalization to an audio buffer using stateful biquad filters for bass, mid, and treble frequencies.
     * Filters maintain their internal state between frames to prevent discontinuities and distortion.
     *
     * @param buffer The audio buffer to be equalized.
     * @param format The audio format of the buffer.
     * @param bassGain The gain to apply to the bass frequencies (in dB).
     * @param midGain The gain to apply to the mid frequencies (in dB).
     * @param trebleGain The gain to apply to the treble frequencies (in dB).
     * @return The equalized audio buffer.
     */
    private static byte[] applyEQ(byte[] buffer, AudioFormat format, float bassGain, float midGain, float trebleGain) {
        float sampleRate = format.getSampleRate();
        int channels = format.getChannels();

        // Initialize filters (only recreates if format changed)
        initFilters(sampleRate, channels);

        // Convert byte buffer to double array
        double[] audioData = byteToDouble(buffer, format);

        // Pre-compute linear gains from dB
        double bassLinear = Math.pow(10, bassGain / 20.0);
        double midLinear = Math.pow(10, midGain / 20.0);
        double trebleLinear = Math.pow(10, trebleGain / 20.0);

        // Process sample-by-sample with stateful filters (handles channels correctly)
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
            // Clamp signal within the valid range
            combined[i] = Math.max(-1.0, Math.min(1.0, combined[i]));
        }

        // Convert double array back to byte buffer
        byte[] outputBuffer = doubleToByte(combined, format);

        return outputBuffer;
    }
}
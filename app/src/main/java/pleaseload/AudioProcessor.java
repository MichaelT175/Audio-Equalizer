package pleaseload;

import javax.sound.sampled.*;
import javax.swing.*;
import java.io.File; 
import java.io.IOException;
import com.github.psambit9791.jdsp.filter.Butterworth;
import com.github.psambit9791.jdsp.transform.FastFourier;

public class AudioProcessor {

    static EQGUI eq = new EQGUI();

    public static void playAudioWithEQ(String filePath, float initialBassGain, float initialMidGain, float initialTrebleGain, VisualizerPanel visualizer) {
        File audioFile = new File(filePath);
        if (!audioFile.exists()) {
            System.out.println("Error: File not found at " + filePath);
            return;
        }
    
        float[] gains = new float[] {initialBassGain, initialMidGain, initialTrebleGain};

        new Thread(() -> {
            try (AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile)) {
                AudioFormat format = audioInputStream.getFormat();
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
    
                while ((bytesRead = audioInputStream.read(buffer, 0, buffer.length)) != -1) {
                    gains[0] = eq.getBassSliderValue();
                    gains[1] = eq.getMidSliderValue();
                    gains[2] = eq.getTrebleSliderValue();
    
                    // Apply EQ to the current buffer
                    byte[] adjustedBuffer = applyEQ(buffer, format, gains[0], gains[1], gains[2]);
                    int[] barHeights = calculateBarHeights(adjustedBuffer, numBars, format);
                    SwingUtilities.invokeLater(() -> visualizer.updateVisualizer(barHeights));
                    line.write(adjustedBuffer, 0, bytesRead);
                }
    
                line.drain();
                line.close();
            } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
                e.printStackTrace();
            }
        }).start();
    }
    
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
            System.out.println(heights[i]);
        }

        return heights;
    }    
    
    private static double[] byteToDouble(byte[] buffer, AudioFormat format){
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

            audioData[i] = sample / 32768.0; // Normalize to -1.0 to 1.0
        }
        return audioData;
    }

    private static byte[] doubleToByte(double[] combined, AudioFormat format, byte[] buffer){
        
        int sampleSizeInBytes = format.getSampleSizeInBits() / 8;
        boolean isBigEndian = format.isBigEndian();

        byte[] outputBuffer = new byte[buffer.length];
        for (int i = 0; i < combined.length; i++) {
            int sample = (int) (combined[i] * 32768);
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


    private static byte[] applyEQ(byte[] buffer, AudioFormat format, float bassGain, float midGain, float trebleGain) {
        float sampleRate = format.getSampleRate();
        
        // Convert byte buffer to double array
        double[] audioData = byteToDouble(buffer, format);
        
        // Apply Butterworth filters
        Butterworth butterworth = new Butterworth(sampleRate);

        // Bass: Low-pass filter
        double[] bassFiltered = butterworth.lowPassFilter(audioData, 2, 200.0);
        for (int i = 0; i < bassFiltered.length; i++) {
            bassFiltered[i] *= Math.pow(10, bassGain / 20);
        }

        // Mid: Band-pass filter
        double[] midFiltered = butterworth.bandPassFilter(audioData, 2, 200.0, 2000.0);
        for (int i = 0; i < midFiltered.length; i++) {
            midFiltered[i] *= Math.pow(10, midGain / 20);
        }

        // Treble: High-pass filter
        double[] trebleFiltered = butterworth.highPassFilter(audioData, 2, 2000.0);
        for (int i = 0; i < trebleFiltered.length; i++) {
            trebleFiltered[i] *= Math.pow(10, trebleGain / 20);
        }

        // Combine filtered signals
        double[] combined = new double[audioData.length];
        for (int i = 0; i < combined.length; i++) {
            combined[i] = bassFiltered[i] + midFiltered[i] + trebleFiltered[i];
            // Ensure the combined signal is within the valid range
            combined[i] = Math.max(-1.0, Math.min(1.0, combined[i]));
        }

        // Convert double array back to byte buffer
        byte[] outputBuffer = doubleToByte(combined, format, buffer);

        return outputBuffer;
    }
}


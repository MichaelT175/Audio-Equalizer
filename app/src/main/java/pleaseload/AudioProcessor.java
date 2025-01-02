package pleaseload;

import javax.sound.sampled.*;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import com.github.psambit9791.jdsp.filter.Butterworth;

public class AudioProcessor {

    static EQGUI eq = new EQGUI();

    public static void playAudioWithEQ(float bassGain, float midGain, float trebleGain, VisualizerPanel visualizer) {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(null);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }
    
        File audioFile = fileChooser.getSelectedFile();
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
                int numBars = 50;
    
                while ((bytesRead = audioInputStream.read(buffer, 0, buffer.length)) != -1) {
                    // Update visualizer
                    int[] barHeights = calculateBarHeights(buffer, numBars);
                    SwingUtilities.invokeLater(() -> visualizer.updateVisualizer(barHeights));
    
                    // Apply EQ and play audio
                    byte[] adjustedBuffer = applyEQ(buffer, format, bassGain, midGain, trebleGain);
                    line.write(adjustedBuffer, 0, bytesRead);
                }
    
                line.drain();
                line.close();
            } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    private static int[] calculateBarHeights(byte[] buffer, int numBars) {
        int[] heights = new int[numBars];
        for (int i = 0; i < heights.length; i++) {
            heights[i] = (int) (Math.random() * 200); // Replace with real amplitude or frequency data
        }
        return heights;
    }    
    
    private static byte[] applyEQ(byte[] buffer, AudioFormat format, float bassGain, float midGain, float trebleGain) {
        int sampleSizeInBytes = format.getSampleSizeInBits() / 8;
        boolean isBigEndian = format.isBigEndian();
        float sampleRate = format.getSampleRate();

        // Convert byte buffer to double array
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
}


package pleaseload;

import javax.sound.sampled.*;
import javax.swing.*;
import java.io.*; 

public class AudioProcessor {

    //Method which plays the audio with the applied EQ
    public static void playAudioWithEQ() {

        //Allows usr to choose file from the pc
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(null);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File audioFile = fileChooser.getSelectedFile();

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
            line.open(format); //Prepares line to receive audio data
            line.start();
            
            byte[] buffer = new byte[4096];
            
            //reads small pieces of the audio file and for each piece applies changes, then plays it
            int bytesRead;
            while ((bytesRead = audioInputStream.read(buffer, 0, buffer.length)) != -1) {
                byte[] adjustedBuffer = applyEQ(buffer, format); 
                line.write(adjustedBuffer, 0, bytesRead);
            }

            //cleanup
            line.drain();
            line.close();

        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }
  
    // Method to convert buffer from byte to double array, because methods in applyEQ expect double
    private static double[] byteToDouble(byte[] buffer, AudioFormat format) {
        int sampleSizeInBytes = format.getSampleSizeInBits() / 8; // Calculate sample size in bytes
        boolean isBigEndian = format.isBigEndian(); // Check if the format is big-endian

        double[] audioData = new double[buffer.length / sampleSizeInBytes]; // Initialize double array for audio data
        for (int i = 0; i < audioData.length; i++) {
            int sampleIndex = i * sampleSizeInBytes; // Calculate sample index in the buffer
            int sample = 0; // Initialize sample variable

            if (sampleSizeInBytes == 2) { // If sample size is 2 bytes
                if (isBigEndian) {
                    sample = (buffer[sampleIndex] << 8) | (buffer[sampleIndex + 1] & 0xFF); // Convert big-endian bytes to sample
                } else {
                    sample = (buffer[sampleIndex + 1] << 8) | (buffer[sampleIndex] & 0xFF); // Convert little-endian bytes to sample
                }
            } else if (sampleSizeInBytes == 1) { // If sample size is 1 byte
                sample = buffer[sampleIndex]; // Assign byte value to sample
            }

            audioData[i] = sample / 32768.0; // Normalize sample to -1.0 to 1.0 range
        }
        return audioData; // Return the double array of audio data
    }

    // Converts the buffer back to a byte array for playback
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



    private static byte[] applyEQ(byte[] buffer, AudioFormat format) {

        return buffer;
    }
}



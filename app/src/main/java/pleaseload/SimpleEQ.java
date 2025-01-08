package pleaseload;

import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;

public class SimpleEQ {

    private static float bassGain = 0.0f; // Bass adjustment in decibels
    private static float midGain = 0.0f; // Mid adjustment in decibels
    private static float trebleGain = 0.0f; // Treble adjustment in decibels

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SimpleEQ::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("SimpleEQ");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(4, 2));

        //Creates the three sliders and labels
        JLabel bassLabel = new JLabel("Bass:");
        JSlider bassSlider = new JSlider(-10, 10, 0);
        bassSlider.setMajorTickSpacing(5);
        bassSlider.setPaintTicks(true);
        bassSlider.setPaintLabels(true);
        bassSlider.addChangeListener(e -> bassGain = bassSlider.getValue());

        JLabel midLabel = new JLabel("Mid:");
        JSlider midSlider = new JSlider(-10, 10, 0);
        midSlider.setMajorTickSpacing(5);
        midSlider.setPaintTicks(true);
        midSlider.setPaintLabels(true);
        midSlider.addChangeListener(e -> midGain = midSlider.getValue());

        JLabel trebleLabel = new JLabel("Treble:");
        JSlider trebleSlider = new JSlider(-10, 10, 0);
        trebleSlider.setMajorTickSpacing(5);
        trebleSlider.setPaintTicks(true);
        trebleSlider.setPaintLabels(true);
        trebleSlider.addChangeListener(e -> trebleGain = trebleSlider.getValue());

        //Create the play button
        JButton playButton = new JButton("Play");
        playButton.addActionListener(e -> playAudioWithEQ());

        //Adds all the components to the panel
        panel.add(bassLabel);
        panel.add(bassSlider);
        panel.add(midLabel);
        panel.add(midSlider);
        panel.add(trebleLabel);
        panel.add(trebleSlider);
        panel.add(playButton);

        frame.add(panel);
        frame.setVisible(true);
    }

    private static void playAudioWithEQ() {

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

    private static byte[] applyEQ(byte[] buffer, AudioFormat format) {
        
        //All the logic for applying the EQ will go here

        return buffer;
    }
}

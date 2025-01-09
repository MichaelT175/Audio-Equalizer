package pleaseload;

import java.awt.*;
import javax.swing.*;

public class EQGUI {

    private static float bassGain = 0.0f; // Bass adjustment in decibels
    private static float midGain = 0.0f; // Mid adjustment in decibels
    private static float trebleGain = 0.0f; // Treble adjustment in decibels

    public static void main(String[] args) {
        SwingUtilities.invokeLater(EQGUI::createAndShowGUI);
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
        playButton.addActionListener(e -> AudioProcessor.playAudioWithEQ());

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

}
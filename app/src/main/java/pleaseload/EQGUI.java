package pleaseload;

import javax.swing.*;
import java.awt.*;

public class EQGUI {
    private static float bassGain = 0.0f; // Bass adjustment in decibels
    private static float midGain = 0.0f; // Mid adjustment in decibels
    private static float trebleGain = 0.0f; // Treble adjustment in decibels
    
    static int max = 20;
    static int min = -20;

    static JLabel bassLabel = new JLabel("Bass:");
    static JSlider bassSlider = new JSlider(JSlider.VERTICAL, min, max, 0);

    static JLabel midLabel = new JLabel("Mid:");
    static JSlider midSlider = new JSlider(JSlider.VERTICAL, min, max, 0);

    static JLabel trebleLabel = new JLabel("Treble:");
    static JSlider trebleSlider = new JSlider(JSlider.VERTICAL, min, max, 0);
    
    public static void createAndShowGUI() {
        JFrame frame = new JFrame("Audio Equalizer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 500);
    
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
    
        // Create visualizer panel
        VisualizerPanel visualizer = new VisualizerPanel(3);
        visualizer.setPreferredSize(new Dimension(800, 200));
    
        // Controls panel with vertical sliders
        JPanel controlsPanel = new JPanel();
        controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.X_AXIS));
    
        // Create slider panels for better alignment
        JPanel bassPanel = createSliderPanel(bassLabel, bassSlider);
        JPanel midPanel = createSliderPanel(midLabel, midSlider);
        JPanel treblePanel = createSliderPanel(trebleLabel, trebleSlider);
    
        // Add play button
        JButton playButton = new JButton("Play");
        playButton.addActionListener(e -> {
            // Play audio and update visualizer in real-time
            new Thread(() -> AudioProcessor.playAudioWithEQ(bassGain, midGain, trebleGain, visualizer)).start();
        });

        // Add slider panels and button to the controls panel
        controlsPanel.add(bassPanel);
        controlsPanel.add(Box.createRigidArea(new Dimension(10, 0))); // Add some spacing
        controlsPanel.add(midPanel);
        controlsPanel.add(Box.createRigidArea(new Dimension(10, 0))); // Add some spacing
        controlsPanel.add(treblePanel);
        controlsPanel.add(Box.createRigidArea(new Dimension(10, 0))); // Add spacing for aesthetics
        controlsPanel.add(playButton);
    
        panel.add(visualizer, BorderLayout.NORTH); // Visualizer at the top
        panel.add(controlsPanel, BorderLayout.CENTER); // Controls in the center
    
        frame.add(panel);
        frame.setVisible(true);
    }
    
    // Helper method to create a panel for each slider
    private static JPanel createSliderPanel(JLabel label, JSlider slider) {
        JPanel sliderPanel = new JPanel();
        sliderPanel.setLayout(new BoxLayout(sliderPanel, BoxLayout.Y_AXIS));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        slider.setAlignmentX(Component.CENTER_ALIGNMENT);
        slider.setMajorTickSpacing(10);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        sliderPanel.add(label);
        sliderPanel.add(slider);
        return sliderPanel;
    }

    // Get Values from sliders
    public float getBassSliderValue() {
        bassSlider.addChangeListener(e -> bassGain = bassSlider.getValue());
        return bassGain;
    }

    public float getMidSliderValue() {
        midSlider.addChangeListener(e -> midGain = midSlider.getValue());
        return midGain;
    }

    public float getTrebleSliderValue() {
        trebleSlider.addChangeListener(e -> trebleGain = trebleSlider.getValue());
        return trebleGain;
    }
}

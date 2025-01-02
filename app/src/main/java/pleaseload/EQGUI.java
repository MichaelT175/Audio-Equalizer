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
    static JSlider bassSlider = new JSlider(min, max, 0);

    static JLabel midLabel = new JLabel("Mid:");
    static JSlider midSlider = new JSlider(min, max, 0);

    static JLabel trebleLabel = new JLabel("Treble:");
    static JSlider trebleSlider = new JSlider(min, max, 0);
    
    public static void createAndShowGUI() {
        JFrame frame = new JFrame("Audio Equalizer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 500);
    
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
    
        // Create visualizer panel
        VisualizerPanel visualizer = new VisualizerPanel(3); // 50 bars
        visualizer.setPreferredSize(new Dimension(800, 200));
    
        JPanel controlsPanel = new JPanel();
        controlsPanel.setLayout(new GridLayout(4, 2));
    
        // Add sliders and play button
        bassSlider.setMajorTickSpacing(5);
        bassSlider.setPaintTicks(true);
        bassSlider.setPaintLabels(true);
        bassSlider.addChangeListener(e -> bassGain = bassSlider.getValue());
    
        midSlider.setMajorTickSpacing(5);
        midSlider.setPaintTicks(true);
        midSlider.setPaintLabels(true);
        midSlider.addChangeListener(e -> midGain = midSlider.getValue());
    
        trebleSlider.setMajorTickSpacing(5);
        trebleSlider.setPaintTicks(true);
        trebleSlider.setPaintLabels(true);
        trebleSlider.addChangeListener(e -> trebleGain = trebleSlider.getValue());
    
        JButton playButton = new JButton("Play");
        playButton.addActionListener(e -> {
            // Play audio and update visualizer in real-time
            new Thread(() -> AudioProcessor.playAudioWithEQ(bassGain, midGain, trebleGain, visualizer)).start();
        });
    
        controlsPanel.add(bassLabel);
        controlsPanel.add(bassSlider);
        controlsPanel.add(midLabel);
        controlsPanel.add(midSlider);
        controlsPanel.add(trebleLabel);
        controlsPanel.add(trebleSlider);
        controlsPanel.add(playButton);
    
        panel.add(visualizer, BorderLayout.NORTH);
        panel.add(controlsPanel, BorderLayout.CENTER);
    
        frame.add(panel);
        frame.setVisible(true);
    }
    
    //Get Values from sliders
    public float getBassSliderValue(){
        bassSlider.addChangeListener(e -> bassGain = bassSlider.getValue());
        return bassGain;
    }

    public float getMidSliderValue(){
        midSlider.addChangeListener(e -> midGain = midSlider.getValue());
        return midGain;
    }

    public float getTrebleSliderValue(){
        trebleSlider.addChangeListener(e -> trebleGain = trebleSlider.getValue());
        return trebleGain;
    }
}


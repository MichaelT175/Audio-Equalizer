package pleaseload;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

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
    
    // Song paths and dropdown menu
    static Map<String, String> songMap = new HashMap<>();
    static JComboBox<String> songDropdown;

    public static void createAndShowGUI() {
        // Populate the song map with file paths
        songMap.put("Imperial March", "C:\\Users\\Michael Jr\\Music\\DEMO SONGS\\ImperialMarch60.wav");
        songMap.put("Star Wars", "C:\\Users\\Michael Jr\\Music\\DEMO SONGS\\StarWars60.wav");
        songMap.put("Baby Elephant Walk", "C:\\Users\\Michael Jr\\Music\\DEMO SONGS\\BabyElephantWalk60.wav");
        songMap.put("Pink Panther", "C:\\Users\\Michael Jr\\Music\\DEMO SONGS\\PinkPanther60.wav");
        
        JFrame frame = new JFrame("Audio Equalizer + Visualizer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 500);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        // Title label
        JLabel titleLabel = new JLabel("Audio Equalizer + Visualizer", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        panel.add(titleLabel, BorderLayout.NORTH);

        // Create visualizer panel
        VisualizerPanel visualizer = new VisualizerPanel(3);
        visualizer.setPreferredSize(new Dimension(800, 200));
        panel.add(visualizer, BorderLayout.CENTER);

        // Controls panel with sliders side by side
        JPanel controlsPanel = new JPanel();
        controlsPanel.setLayout(new GridLayout(1, 3, 20, 0)); // 1 row, 3 columns, with spacing

        // Create slider panels for better alignment
        JPanel bassPanel = createSliderPanel(bassLabel, bassSlider);
        JPanel midPanel = createSliderPanel(midLabel, midSlider);
        JPanel treblePanel = createSliderPanel(trebleLabel, trebleSlider);

        controlsPanel.add(bassPanel);
        controlsPanel.add(midPanel);
        controlsPanel.add(treblePanel);

        panel.add(controlsPanel, BorderLayout.WEST);

        // Create the song dropdown menu
        songDropdown = new JComboBox<>(songMap.keySet().toArray(new String[0]));

        // Add play button
        JButton playButton = new JButton("Play");
        playButton.addActionListener(e -> {
            String selectedSong = (String) songDropdown.getSelectedItem();
            if (selectedSong != null) {
                String filePath = songMap.get(selectedSong);
                new Thread(() -> AudioProcessor.playAudioWithEQ(filePath, bassGain, midGain, trebleGain, visualizer)).start();
            }
        });

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(songDropdown);
        bottomPanel.add(playButton);

        panel.add(bottomPanel, BorderLayout.SOUTH);

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
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

        // Custom colors
        Color lightPurple = new Color(200, 172, 214);
        Color purple = new Color(23, 21, 59);
        
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
        JLabel titleLabel = new JLabel("AUDIO EQUALIZER + VISUALIZER", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        panel.add(titleLabel, BorderLayout.NORTH);

        // Create visualizer panel
        VisualizerPanel visualizer = new VisualizerPanel(3);
        visualizer.setPreferredSize(new Dimension(800, 200));
        visualizer.setBackground(new Color(23, 21, 59));;
        panel.add(visualizer, BorderLayout.CENTER);

        // Create spectrum panel
        SpectrumPanel spectrumPanel = new SpectrumPanel(1024); // Adjust number of bins as needed
        spectrumPanel.setPreferredSize(new Dimension(800, 200));
        panel.add(spectrumPanel, BorderLayout.NORTH);


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
                new Thread(() -> AudioProcessor.playAudioWithEQ(filePath, bassGain, midGain, trebleGain, visualizer, spectrumPanel)).start();
            }
        });

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(songDropdown);
        bottomPanel.add(playButton);

        panel.add(bottomPanel, BorderLayout.SOUTH);

        // Set background color for components
        panel.setBackground(purple);
        bassPanel.setBackground(purple);
        bassSlider.setBackground(purple);
        midPanel.setBackground(purple);
        midSlider.setBackground(purple);
        treblePanel.setBackground(purple);
        trebleSlider.setBackground(purple);
        controlsPanel.setBackground(purple);
        songDropdown.setBackground(purple);
        playButton.setBackground(purple);

        // Customize text and border colors
        bassSlider.setForeground(Color.WHITE);
        midSlider.setForeground(Color.WHITE);
        trebleSlider.setForeground(Color.WHITE);
        songDropdown.setForeground(Color.WHITE);
        playButton.setForeground(Color.WHITE);

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
        label.setForeground(Color.WHITE);
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
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

    static Map<String, String> songMap = new HashMap<>();
    static JComboBox<String> songDropdown;

    public static void createAndShowGUI() {
        JFrame frame = setupMainFrame();
        JPanel mainPanel = setupMainPanel();

        // Visualizer and Spectrum Panels
        JPanel centerPanel = setupCenterPanel();
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Controls Panel
        JPanel slidersPanel = setupSlidersPanel();
        mainPanel.add(slidersPanel, BorderLayout.WEST);

        // Bottom Panel for Song Selection and Play Button
        JPanel bottomPanel = setupBottomPanel(centerPanel);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        frame.add(mainPanel);
        frame.setVisible(true);
    }

    private static JFrame setupMainFrame() {
        JFrame frame = new JFrame("Audio Equalizer + Visualizer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 500);
        return frame;
    }

    private static JPanel setupMainPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(23, 21, 59));

        JLabel titleLabel = new JLabel("AUDIO EQUALIZER + VISUALIZER", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        panel.add(titleLabel, BorderLayout.NORTH);

        return panel;
    }

    private static JPanel setupCenterPanel() {
        VisualizerPanel visualizer = new VisualizerPanel(3);
        visualizer.setPreferredSize(new Dimension(600, 100));
        visualizer.setBackground(new Color(23, 21, 59));

        SpectrumPanel spectrumPanel = new SpectrumPanel(1024);
        spectrumPanel.setPreferredSize(new Dimension(600, 150));
        spectrumPanel.setBackground(new Color(23, 21, 59));

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(spectrumPanel, BorderLayout.NORTH);
        centerPanel.add(visualizer, BorderLayout.CENTER);

        return centerPanel;
    }

    private static JPanel setupSlidersPanel() {
        JPanel slidersPanel = new JPanel(new GridLayout(1, 3, 10, 10));

        JPanel bassPanel = createSliderPanel(bassLabel, bassSlider);
        JPanel midPanel = createSliderPanel(midLabel, midSlider);
        JPanel treblePanel = createSliderPanel(trebleLabel, trebleSlider);

        slidersPanel.add(bassPanel);
        slidersPanel.add(midPanel);
        slidersPanel.add(treblePanel);

        slidersPanel.setBackground(new Color(23, 21, 59));
        return slidersPanel;
    }

    private static PresetManager presetManager = new PresetManager();
    private static Map<String, float[]> loadedPresets;

    private static JPanel setupBottomPanel(JPanel centerPanel) {
        SongMenu songMenu = new SongMenu();

        // Initialize songDropdown with songs from SongMenu
        songDropdown = songMenu.getDropdown();
        songDropdown.setBackground(new Color(23, 21, 59));
        songDropdown.setForeground(Color.WHITE);

        JButton playButton = new JButton("Play");
        playButton.setBackground(new Color(23, 21, 59));
        playButton.setForeground(Color.WHITE);
        playButton.addActionListener(e -> {
            String filePath = songMenu.getSelectedSongPath();
            if (filePath != null) {
                new Thread(() -> AudioProcessor.playAudioWithEQ(filePath, bassGain, midGain, trebleGain, (VisualizerPanel) centerPanel.getComponent(1), (SpectrumPanel) centerPanel.getComponent(0))).start();
            }
        });

        JButton stopButton = new JButton("Stop");
        stopButton.setBackground(new Color(23, 21, 59));
        stopButton.setForeground(Color.WHITE);
        stopButton.addActionListener(e -> {
            AudioProcessor.stopAudioPlayback(true);
        });

        JButton newSongButton = new JButton("Add Song");
        newSongButton.setBackground(new Color(23, 21, 59));
        newSongButton.setForeground(Color.WHITE);
        newSongButton.addActionListener(e -> {
            songMenu.addNewFile();
        });

        JButton savePresetButton = new JButton("Save Preset");
        savePresetButton.setBackground(new Color(23, 21, 59));
        savePresetButton.setForeground(Color.WHITE);
        savePresetButton.addActionListener(e -> {
            String selectedSong = (String) songDropdown.getSelectedItem();
            if (selectedSong != null) {
                presetManager.savePreset(selectedSong, bassSlider.getValue(), midSlider.getValue(), trebleSlider.getValue());
            } else {
                System.err.println("No song selected for saving a preset.");
            }
        });
    
        JButton loadPresetButton = new JButton("Load Preset");
        loadPresetButton.setBackground(new Color(23, 21, 59));
        loadPresetButton.setForeground(Color.WHITE);
        loadPresetButton.addActionListener(e -> {
            String selectedSong = (String) songDropdown.getSelectedItem();
            if (selectedSong != null && loadedPresets.containsKey(selectedSong)) {
                float[] preset = loadedPresets.get(selectedSong);
                bassSlider.setValue((int) preset[0]);
                midSlider.setValue((int) preset[1]);
                trebleSlider.setValue((int) preset[2]);
                System.out.println("Preset loaded for song: " + selectedSong);
            } else {
                System.err.println("No preset found for the selected song.");
            }
        });
    
        // Load presets when the app starts
        loadedPresets = presetManager.loadPresets();
    
        // Bottom panel layout
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setBackground(new Color(23, 21, 59));
        bottomPanel.add(savePresetButton);
        bottomPanel.add(loadPresetButton);
        bottomPanel.add(newSongButton);
        bottomPanel.add(songDropdown);
        bottomPanel.add(playButton);
        bottomPanel.add(stopButton);


    
        return bottomPanel;
    }

    private static JPanel createSliderPanel(JLabel label, JSlider slider) {
        JPanel sliderPanel = new JPanel();
        sliderPanel.setLayout(new BoxLayout(sliderPanel, BoxLayout.Y_AXIS));
        sliderPanel.setBackground(new Color(23, 21, 59));

        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setForeground(Color.WHITE);
        slider.setAlignmentX(Component.CENTER_ALIGNMENT);
        slider.setMajorTickSpacing(10);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setBackground(new Color(23, 21, 59));
        slider.setForeground(Color.WHITE);

        sliderPanel.add(label);
        sliderPanel.add(slider);
        return sliderPanel;
    }

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

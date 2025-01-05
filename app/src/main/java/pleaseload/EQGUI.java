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
        frame.setSize(800, 500);
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
        spectrumPanel.setPreferredSize(new Dimension(600, 100));
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

    
    private static JPanel setupBottomPanel(JPanel centerPanel) {
        SongMenu songMenu = new SongMenu();

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

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setBackground(new Color(23, 21, 59));
        bottomPanel.add(songMenu.getDropdown());
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

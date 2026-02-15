package com.audioeq;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import java.io.File;

/**
 * Modern JavaFX-based Audio Equalizer Application with professional UI design.
 * Features gradient backgrounds, smooth controls, and polished visualizations.
 */
public class EqualizerApp extends Application {
    
    private static float bassGain = 0.0f;
    private static float midGain = 0.0f;
    private static float trebleGain = 0.0f;
    
    private Slider bassSlider;
    private Slider midSlider;
    private Slider trebleSlider;
    private Slider volumeSlider;
    
    private Label bassValueLabel;
    private Label midValueLabel;
    private Label trebleValueLabel;
    private Label volumeValueLabel;
    
    private ComboBox<String> songDropdown;
    private SongMenuFX songMenu;
    
    private CheckBox autoLevelCheckbox;
    private CheckBox echoCancelCheckbox;
    
    private VisualizerCanvasFX visualizerCanvas;
    private SpectrumCanvasFX spectrumCanvas;
    private DBMeterCanvasFX dbMeterCanvas;
    
    private PresetManager presetManager = new PresetManager();
    
    private Button playButton;
    private Button pauseButton;
    private Button stopButton;
    
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Audio Equalizer + Visualizer");
        
        // Initialize components
        songMenu = new SongMenuFX();
        
        // Main layout
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #1a1a2e, #16213e);");
        
        // Top bar
        root.setTop(createTopBar());
        
        // Center (visualizers)
        root.setCenter(createVisualizerSection());
        
        // Left (sliders)
        root.setLeft(createSlidersSection());
        
        // Bottom (controls)
        root.setBottom(createControlsSection());
        
        Scene scene = new Scene(root, 1200, 650);
        //scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // Start update threads
        startUpdateThreads();
    }
    
    /**
     * Creates the top bar with title and help button.
     */
    private HBox createTopBar() {
        HBox topBar = new HBox();
        topBar.setAlignment(Pos.CENTER);
        topBar.setPadding(new Insets(15, 20, 15, 20));
        topBar.setStyle("-fx-background-color: rgba(0, 0, 0, 0.3);");
        
        Label titleLabel = new Label("AUDIO EQUALIZER + VISUALIZER");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        titleLabel.setTextFill(Color.web("#00d9ff"));
        titleLabel.setStyle("-fx-effect: dropshadow(gaussian, rgba(0, 217, 255, 0.6), 10, 0, 0, 0);");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button helpButton = new Button("?");
        helpButton.setStyle(
            "-fx-background-color: #0f3460; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 16px; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 20; " +
            "-fx-min-width: 40; " +
            "-fx-min-height: 40;"
        );
        helpButton.setOnAction(e -> showHelp());
        
        topBar.getChildren().addAll(titleLabel, spacer, helpButton);
        return topBar;
    }
    
    /**
     * Creates the visualizer section with spectrum and bar visualizers.
     */
    private VBox createVisualizerSection() {
        VBox visualizerBox = new VBox(15);
        visualizerBox.setPadding(new Insets(20));
        visualizerBox.setAlignment(Pos.CENTER);
        
        // Spectrum analyzer
        spectrumCanvas = new SpectrumCanvasFX(1024);
        spectrumCanvas.setWidth(800);
        spectrumCanvas.setHeight(200);
        
        // Bar visualizer
        visualizerCanvas = new VisualizerCanvasFX(10);
        visualizerCanvas.setWidth(800);
        visualizerCanvas.setHeight(150);
        
        // Add styling containers
        StackPane spectrumContainer = createVisualizerContainer(spectrumCanvas, "SPECTRUM ANALYZER");
        StackPane visualizerContainer = createVisualizerContainer(visualizerCanvas, "FREQUENCY BARS");
        
        visualizerBox.getChildren().addAll(spectrumContainer, visualizerContainer);
        return visualizerBox;
    }
    
    /**
     * Creates a styled container for visualizer components.
     */
    private StackPane createVisualizerContainer(javafx.scene.canvas.Canvas canvas, String title) {
        VBox container = new VBox(5);
        container.setAlignment(Pos.CENTER);
        
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));
        titleLabel.setTextFill(Color.web("#8892b0"));
        
        container.getChildren().addAll(titleLabel, canvas);
        
        StackPane wrapper = new StackPane(container);
        wrapper.setStyle(
            "-fx-background-color: rgba(15, 52, 96, 0.3); " +
            "-fx-background-radius: 15; " +
            "-fx-border-color: rgba(0, 217, 255, 0.2); " +
            "-fx-border-width: 1; " +
            "-fx-border-radius: 15; " +
            "-fx-padding: 15;"
        );
        
        return wrapper;
    }
    
    /**
     * Creates the sliders section for EQ, volume, and dB meter.
     */
    private HBox createSlidersSection() {
        HBox slidersBox = new HBox(20);
        slidersBox.setPadding(new Insets(20, 20, 20, 30));
        slidersBox.setAlignment(Pos.CENTER);
        
        // Create sliders
        VBox bassBox = createSliderBox("BASS", -20, 20, 0, bassValueLabel = new Label("0 dB"));
        bassSlider = (Slider) bassBox.getChildren().get(1);
        bassSlider.valueProperty().addListener((obs, old, val) -> {
            bassGain = val.floatValue();
            bassValueLabel.setText(String.format("%.0f dB", val.doubleValue()));
        });
        
        VBox midBox = createSliderBox("MID", -20, 20, 0, midValueLabel = new Label("0 dB"));
        midSlider = (Slider) midBox.getChildren().get(1);
        midSlider.valueProperty().addListener((obs, old, val) -> {
            midGain = val.floatValue();
            midValueLabel.setText(String.format("%.0f dB", val.doubleValue()));
        });
        
        VBox trebleBox = createSliderBox("TREBLE", -20, 20, 0, trebleValueLabel = new Label("0 dB"));
        trebleSlider = (Slider) trebleBox.getChildren().get(1);
        trebleSlider.valueProperty().addListener((obs, old, val) -> {
            trebleGain = val.floatValue();
            trebleValueLabel.setText(String.format("%.0f dB", val.doubleValue()));
        });
        
        VBox volumeBox = createSliderBox("VOLUME", 0, 200, 100, volumeValueLabel = new Label("100%"));
        volumeSlider = (Slider) volumeBox.getChildren().get(1);
        volumeSlider.valueProperty().addListener((obs, old, val) -> {
            AudioProcessor.setMasterVolume(val.floatValue() / 100.0f);
            volumeValueLabel.setText(String.format("%.0f%%", val.doubleValue()));
        });
        
        // dB Meter
        VBox dbMeterBox = createDBMeterBox();
        
        slidersBox.getChildren().addAll(bassBox, midBox, trebleBox, volumeBox, dbMeterBox);
        return slidersBox;
    }
    
    /**
     * Creates a styled slider box with label and value display.
     */
    private VBox createSliderBox(String title, double min, double max, double initial, Label valueLabel) {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPrefWidth(100);
        
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        titleLabel.setTextFill(Color.web("#e0e0e0"));
        
        Slider slider = new Slider(min, max, initial);
        slider.setOrientation(javafx.geometry.Orientation.VERTICAL);
        slider.setPrefHeight(250);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit((max - min) / 4);
        slider.setStyle(
            "-fx-control-inner-background: #0f3460; " +
            "-fx-accent: #00d9ff;"
        );
        
        valueLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));
        valueLabel.setTextFill(Color.web("#00d9ff"));
        
        box.getChildren().addAll(titleLabel, slider, valueLabel);
        return box;
    }
    
    /**
     * Creates the dB meter display box.
     */
    private VBox createDBMeterBox() {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPrefWidth(100);
        
        Label titleLabel = new Label("dB METER");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        titleLabel.setTextFill(Color.web("#e0e0e0"));
        
        dbMeterCanvas = new DBMeterCanvasFX();
        dbMeterCanvas.setWidth(60);
        dbMeterCanvas.setHeight(250);
        
        box.getChildren().addAll(titleLabel, dbMeterCanvas);
        return box;
    }
    
    /**
     * Creates the bottom controls section.
     */
    private VBox createControlsSection() {
        VBox controlsBox = new VBox(15);
        controlsBox.setPadding(new Insets(15, 20, 20, 20));
        controlsBox.setStyle("-fx-background-color: rgba(0, 0, 0, 0.3);");
        
        // Song selection and playback controls
        HBox row1 = new HBox(15);
        row1.setAlignment(Pos.CENTER);
        
        songDropdown = songMenu.getDropdown();
        songDropdown.setPrefWidth(250);
        styleComboBox(songDropdown);
        
        playButton = createControlButton("▶ Play", "#4CAF50");
        pauseButton = createControlButton("⏸ Pause", "#FF9800");
        stopButton = createControlButton("⏹ Stop", "#F44336");
        
        playButton.setOnAction(e -> playSelectedSong());
        pauseButton.setOnAction(e -> AudioProcessor.pauseAudioPlayback());
        stopButton.setOnAction(e -> AudioProcessor.stopAudioPlayback(true));
        
        row1.getChildren().addAll(songDropdown, playButton, pauseButton, stopButton);
        
        // Feature controls and file management
        HBox row2 = new HBox(15);
        row2.setAlignment(Pos.CENTER);
        
        autoLevelCheckbox = createStyledCheckbox("Auto Level");
        autoLevelCheckbox.setSelected(true);
        autoLevelCheckbox.setOnAction(e -> 
            AudioProcessor.setAutoLevelEnabled(autoLevelCheckbox.isSelected())
        );
        
        echoCancelCheckbox = createStyledCheckbox("Echo Cancel");
        echoCancelCheckbox.setOnAction(e -> 
            AudioProcessor.setEchoCancelEnabled(echoCancelCheckbox.isSelected())
        );
        
        Button savePresetBtn = createControlButton("💾 Save Preset", "#2196F3");
        Button loadPresetBtn = createControlButton("📂 Load Preset", "#2196F3");
        Button addSongBtn = createControlButton("➕ Add Song", "#9C27B0");
        Button removeSongBtn = createControlButton("➖ Remove", "#E91E63");
        
        savePresetBtn.setOnAction(e -> savePreset());
        loadPresetBtn.setOnAction(e -> loadPreset());
        addSongBtn.setOnAction(e -> songMenu.addNewFile());
        removeSongBtn.setOnAction(e -> songMenu.removeSelectedFile());
        
        row2.getChildren().addAll(
            autoLevelCheckbox, echoCancelCheckbox,
            savePresetBtn, loadPresetBtn,
            addSongBtn, removeSongBtn
        );
        
        controlsBox.getChildren().addAll(row1, row2);
        return controlsBox;
    }
    
    /**
     * Creates a styled control button.
     */
    private Button createControlButton(String text, String color) {
        Button button = new Button(text);
        button.setStyle(
            "-fx-background-color: " + color + "; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 13px; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 8; " +
            "-fx-padding: 10 20; " +
            "-fx-cursor: hand;"
        );
        
        button.setOnMouseEntered(e -> 
            button.setStyle(button.getStyle() + "-fx-opacity: 0.8;")
        );
        button.setOnMouseExited(e -> 
            button.setStyle(button.getStyle() + "-fx-opacity: 1.0;")
        );
        
        return button;
    }
    
    /**
     * Creates a styled checkbox.
     */
    private CheckBox createStyledCheckbox(String text) {
        CheckBox checkBox = new CheckBox(text);
        checkBox.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));
        checkBox.setTextFill(Color.web("#e0e0e0"));
        checkBox.setStyle("-fx-text-fill: #e0e0e0;");
        return checkBox;
    }
    
    /**
     * Styles the ComboBox.
     */
    private void styleComboBox(ComboBox<String> comboBox) {
        comboBox.setStyle(
            "-fx-background-color: #0f3460; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 13px; " +
            "-fx-background-radius: 8;"
        );
    }
    
    /**
     * Plays the selected song.
     */
    private void playSelectedSong() {
        String filePath = songMenu.getSelectedSongPath();
        if (filePath != null) {
            new Thread(() -> AudioProcessor.playAudioWithEQ(
                filePath, bassGain, midGain, trebleGain,
                visualizerCanvas, spectrumCanvas
            )).start();
        }
    }
    
    /**
     * Saves a preset for the current song.
     */
    private void savePreset() {
        String selectedSong = songDropdown.getValue();
        if (selectedSong != null) {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Save Preset");
            dialog.setHeaderText("Enter a name for the preset:");
            dialog.setContentText("Preset name:");
            
            dialog.showAndWait().ifPresent(name -> {
                if (!name.trim().isEmpty()) {
                    presetManager.savePreset(selectedSong, name.trim(),
                        (float)bassSlider.getValue(), (float)midSlider.getValue(), (float)trebleSlider.getValue());
                    showInfo("Preset Saved", "Preset '" + name + "' saved successfully!");
                }
            });
        }
    }
    
    /**
     * Loads a preset for the current song.
     */
    private void loadPreset() {
        String selectedSong = songDropdown.getValue();
        if (selectedSong != null) {
            var presets = presetManager.getPresetsForSong(selectedSong);
            if (presets.isEmpty()) {
                showInfo("No Presets", "No presets available for this song.");
            } else {
                ChoiceDialog<String> dialog = new ChoiceDialog<>(
                    presets.keySet().iterator().next(),
                    presets.keySet()
                );
                dialog.setTitle("Load Preset");
                dialog.setHeaderText("Choose a preset to load:");
                dialog.setContentText("Preset:");
                
                dialog.showAndWait().ifPresent(preset -> {
                    float[] values = presets.get(preset);
                    bassSlider.setValue(values[0]);
                    midSlider.setValue(values[1]);
                    trebleSlider.setValue(values[2]);
                });
            }
        }
    }
    
    /**
     * Shows help dialog.
     */
    private void showHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Help");
        alert.setHeaderText("Audio Equalizer + Visualizer");
        alert.setContentText(
            "Controls:\n" +
            "• Use sliders to adjust Bass, Mid, Treble, and Volume\n" +
            "• Auto Level: Normalizes volume across different tracks\n" +
            "• Echo Cancel: Reduces echo artifacts\n" +
            "• dB Meter: Shows real-time audio level\n" +
            "• Save/Load Presets: Store your EQ settings per song\n" +
            "• Add/Remove Songs: Manage your music library\n\n" +
            "Visualizers show real-time frequency spectrum and bars."
        );
        alert.showAndWait();
    }
    
    /**
     * Shows an info dialog.
     */
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Starts the update threads for visualizers and dB meter.
     */
    private void startUpdateThreads() {
        // dB meter updater
        Thread dbThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(50);
                    double db = AudioProcessor.getCurrentdB();
                    Platform.runLater(() -> dbMeterCanvas.updateLevel(db));
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        dbThread.setDaemon(true);
        dbThread.start();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
    
    // Getters for AudioProcessor
    public float getBassSliderValue() {
        return bassGain;
    }
    
    public float getMidSliderValue() {
        return midGain;
    }
    
    public float getTrebleSliderValue() {
        return trebleGain;
    }
}

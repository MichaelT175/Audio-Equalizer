package com.audioeq;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * PROFESSIONAL AUDIO EQUALIZER - Redesigned with exceptional UI/UX
 * Features: Theme system, waveform display, glassmorphism, smooth animations
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
    private Label nowPlayingLabel;
    private Label searchStatusLabel;
    
    private ComboBox<String> songDropdown;
    private Button songSelectButton;
    private String selectedSongName;
    private OnlineSongResult selectedOnlineSong;
    private SongMenuFX songMenu;
    private ListView<String> localSongsList;
    private Stage songPickerStage;
    private final ObservableList<String> localSongItems = FXCollections.observableArrayList();
    private TextField searchField;
    private Button searchButton;
    private ListView<OnlineSongResult> searchResultsList;
    private ListView<SongQueueItem> queueList;
    private final ObservableList<OnlineSongResult> searchResults = FXCollections.observableArrayList();
    private final ObservableList<SongQueueItem> queueItems = FXCollections.observableArrayList();
    private final Deque<SongQueueItem> queueHistory = new ArrayDeque<>();
    private SongQueueItem currentQueueItem;
    private OnlineSongSearchService onlineSearchService;
    private OnlineAudioDownloader onlineAudioDownloader;
    private ExecutorService onlineExecutor;
    
    private CheckBox autoLevelCheckbox;
    private CheckBox echoCancelCheckbox;
    
    private VisualizerCanvasFX visualizerCanvas;
    private SpectrumCanvasFX spectrumCanvas;
    private DBMeterCanvasFX dbMeterCanvas;
    private WaveformCanvasFX waveformCanvas;
    private StackPane waveformContainer;
    private StackPane spectrumContainer;
    private StackPane visualizerContainer;
    
    private PresetManager presetManager = new PresetManager();
    
    private Button playButton;
    private Button pauseButton;
    private Button stopButton;
    
    // Theme system
    private Theme currentTheme = Theme.CYBER_BLUE;
    private BorderPane root;
    private VBox centerContent;
    private ScrollPane centerScroll;
    
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Audio Equalizer Pro");
        
        // Initialize components
        songMenu = new SongMenuFX();
        onlineSearchService = new OnlineSongSearchService();
        onlineAudioDownloader = new OnlineAudioDownloader();
        searchStatusLabel = new Label(" ");
        onlineExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "online-audio-worker");
            t.setDaemon(true);
            return t;
        });
        
        // Main layout
        root = new BorderPane();
        applyThemeBackground();
        
        // Create all sections
        root.setTop(createTopBar());
        centerContent = createCenterContent();
        centerScroll = new ScrollPane(centerContent);
        centerScroll.setFitToWidth(true);
        centerScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        centerScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        centerScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        root.setCenter(centerScroll);
        root.setLeft(createLeftPanel());
        root.setBottom(createBottomControls());
        
        Scene scene = new Scene(root, 1400, 800);
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.setOnCloseRequest(e -> {
            if (onlineExecutor != null) {
                onlineExecutor.shutdownNow();
            }
        });
        
        primaryStage.show();
        
        // Entrance animation
        playEntranceAnimation();
        
        // Start update threads
        startUpdateThreads();
    }
    
    /**
     * Creates the top navigation bar with branding and theme selector
     */
    private HBox createTopBar() {
        HBox topBar = new HBox(20);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(20, 30, 20, 30));
        topBar.setStyle(
            "-fx-background-color: rgba(0, 0, 0, 0.4);" +
            "-fx-backdrop-filter: blur(20px);" +
            "-fx-border-color: " + currentTheme.accentColor + "33;" +
            "-fx-border-width: 0 0 1 0;"
        );
        
        // Logo/Branding
        VBox branding = new VBox(2);
        Label brandTitle = new Label("AUDIO IN MOTION");
        brandTitle.setFont(Font.font("Consolas", FontWeight.BOLD, 24));
        brandTitle.setTextFill(Paint.valueOf(currentTheme.accentColor));
        
        Label brandSubtitle = new Label("LISTEN YOUR WAY • PRO AUDIO SUITE");
        brandSubtitle.setFont(Font.font("Consolas", FontWeight.LIGHT, 9));
        brandSubtitle.setTextFill(Paint.valueOf(currentTheme.textSecondary));
        brandSubtitle.setStyle("-fx-letter-spacing: 2px;");
        
        // Create compact now playing display
        VBox nowPlayingCompact = new VBox(2);
        nowPlayingCompact.setAlignment(Pos.CENTER);
        Label nowPlayingTitle = new Label("NOW PLAYING");
        nowPlayingTitle.setFont(Font.font("Consolas", FontWeight.SEMI_BOLD, 8));
        nowPlayingTitle.setTextFill(Paint.valueOf(currentTheme.textSecondary));
        nowPlayingTitle.setStyle("-fx-letter-spacing: 1.5px;");

        nowPlayingLabel = new Label("No track selected");
        nowPlayingLabel.setFont(Font.font("Consolas", FontWeight.BOLD, 14));
        nowPlayingLabel.setTextFill(Paint.valueOf(currentTheme.accentColor));

        nowPlayingCompact.getChildren().addAll(nowPlayingTitle, nowPlayingLabel);


        branding.getChildren().addAll(brandTitle, brandSubtitle);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        // Theme selector
        HBox themeSelector = createThemeSelector();
        
        // Settings button
        Button settingsBtn = createIconButton("⚙", "Settings");
        settingsBtn.setOnAction(e -> showSettings());
        
        // Help button  
        Button helpBtn = createIconButton("?", "Help");
        helpBtn.setOnAction(e -> showHelp());
        
        topBar.getChildren().addAll(branding, spacer, nowPlayingCompact, spacer2, themeSelector, settingsBtn, helpBtn);
        return topBar;
    }
    
    /**
     * Creates theme selector buttons
     */
    private HBox createThemeSelector() {
        HBox themeBox = new HBox(8);
        themeBox.setAlignment(Pos.CENTER);
        
        Label themeLabel = new Label("THEME");
        themeLabel.setFont(Font.font("Consolas", FontWeight.SEMI_BOLD, 10));
        themeLabel.setTextFill(Paint.valueOf(currentTheme.textSecondary));
        themeLabel.setStyle("-fx-letter-spacing: 1px;");
        
        for (Theme theme : Theme.values()) {
            Circle themeCircle = new Circle(10);
            themeCircle.setFill(Paint.valueOf(theme.accentColor));
            themeCircle.setStroke(Paint.valueOf("#ffffff"));
            themeCircle.setStrokeWidth(currentTheme == theme ? 2 : 0);
            themeCircle.setStyle("-fx-cursor: hand;");
            
            DropShadow glow = new DropShadow();
            glow.setColor(Color.web(theme.accentColor));
            glow.setRadius(10);
            themeCircle.setEffect(glow);
            
            themeCircle.setOnMouseClicked(e -> switchTheme(theme));
            
            Tooltip tooltip = new Tooltip(theme.name);
            Tooltip.install(themeCircle, tooltip);
            
            themeBox.getChildren().add(themeCircle);
        }
        
        themeBox.getChildren().add(0, themeLabel);
        return themeBox;
    }
    
    /**
     * Creates the center content area with visualizers
     */
    private VBox createCenterContent() {
        VBox center = new VBox(20);
        center.setPadding(new Insets(30, 30, 20, 30));
        center.setAlignment(Pos.CENTER);
                
        // Visualizers container
        VBox visualizersBox = new VBox(15);
        visualizersBox.setAlignment(Pos.CENTER);
        
        // Waveform display
        waveformCanvas = new WaveformCanvasFX();
        waveformCanvas.setWidth(1000);
        waveformCanvas.setHeight(80);
        waveformContainer = createGlassPanel(waveformCanvas, "WAVEFORM");
        
        // Spectrum analyzer
        spectrumCanvas = new SpectrumCanvasFX(1024);
        spectrumCanvas.setWidth(1000);
        spectrumCanvas.setHeight(120);
        spectrumContainer = createGlassPanel(spectrumCanvas, "FREQUENCY SPECTRUM");
        
        // Bar visualizer
        visualizerCanvas = new VisualizerCanvasFX(10);
        visualizerCanvas.setWidth(1000);
        visualizerCanvas.setHeight(100);
        visualizerContainer = createGlassPanel(visualizerCanvas, "AMPLITUDE BARS");
        
        visualizersBox.getChildren().addAll(
            waveformContainer,
            spectrumContainer,
            visualizerContainer
        );

        center.getChildren().addAll(visualizersBox);
        return center;
    }
    
    /**
     * Creates a glassmorphic panel container
     */
    private StackPane createGlassPanel(javafx.scene.canvas.Canvas canvas, String title) {
        VBox container = new VBox(10);
        container.setAlignment(Pos.CENTER);
        
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Consolas", FontWeight.SEMI_BOLD, 11));
        titleLabel.setTextFill(Paint.valueOf(currentTheme.textSecondary));
        titleLabel.setStyle("-fx-letter-spacing: 1.5px;");
        
        container.getChildren().addAll(titleLabel, canvas);
        
        StackPane wrapper = new StackPane(container);
        wrapper.setPadding(new Insets(15));
        applyGlassPanelStyle(wrapper);
        
        return wrapper;
    }

    private void applyGlassPanelStyle(StackPane panel) {
        panel.setStyle(
            "-fx-background-color: " + currentTheme.cardBackground + ";" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: " + currentTheme.accentColor + "22;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 12;"
        );
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.web(currentTheme.accentColor, 0.2));
        shadow.setRadius(15);
        panel.setEffect(shadow);

        if (!panel.getChildren().isEmpty() && panel.getChildren().get(0) instanceof VBox) {
            VBox container = (VBox) panel.getChildren().get(0);
            if (!container.getChildren().isEmpty() && container.getChildren().get(0) instanceof Label) {
                Label titleLabel = (Label) container.getChildren().get(0);
                titleLabel.setTextFill(Paint.valueOf(currentTheme.textSecondary));
            }
        }
    }
    
    /**
     * Creates the left control panel with EQ and volume
     */
    private VBox createLeftPanel() {
        VBox leftPanel = new VBox(20);
        leftPanel.setPadding(new Insets(30, 20, 30, 30));
        leftPanel.setPrefWidth(280);
        leftPanel.setStyle(
            "-fx-background-color: rgba(0, 0, 0, 0.3);" +
            "-fx-border-color: " + currentTheme.accentColor + "22;" +
            "-fx-border-width: 0 1 0 0;"
        );
        
        // EQ Section
        VBox eqSection = createControlSection("EQUALIZER");
        
        VBox bassBox = createModernSlider("BASS", -20, 20, 0, bassValueLabel = new Label("0 dB"));
        bassSlider = (Slider) ((VBox)bassBox.getChildren().get(1)).getChildren().get(0);
        bassSlider.valueProperty().addListener((obs, old, val) -> {
            bassGain = val.floatValue();
            bassValueLabel.setText(String.format("%.0f dB", val.doubleValue()));
        });
        
        VBox midBox = createModernSlider("MID", -20, 20, 0, midValueLabel = new Label("0 dB"));
        midSlider = (Slider) ((VBox)midBox.getChildren().get(1)).getChildren().get(0);
        midSlider.valueProperty().addListener((obs, old, val) -> {
            midGain = val.floatValue();
            midValueLabel.setText(String.format("%.0f dB", val.doubleValue()));
        });
        
        VBox trebleBox = createModernSlider("TREBLE", -20, 20, 0, trebleValueLabel = new Label("0 dB"));
        trebleSlider = (Slider) ((VBox)trebleBox.getChildren().get(1)).getChildren().get(0);
        trebleSlider.valueProperty().addListener((obs, old, val) -> {
            trebleGain = val.floatValue();
            trebleValueLabel.setText(String.format("%.0f dB", val.doubleValue()));
        });
        
        eqSection.getChildren().addAll(bassBox, midBox, trebleBox);
        
        // Volume Section
        VBox volumeSection = createControlSection("MASTER VOLUME");
        
        VBox volumeBox = createModernSlider("VOLUME", 0, 200, 100, volumeValueLabel = new Label("100%"));
        volumeSlider = (Slider) ((VBox)volumeBox.getChildren().get(1)).getChildren().get(0);
        volumeSlider.valueProperty().addListener((obs, old, val) -> {
            AudioProcessor.setMasterVolume(val.floatValue() / 100.0f);
            volumeValueLabel.setText(String.format("%.0f%%", val.doubleValue()));
        });
        
        volumeSection.getChildren().add(volumeBox);
        
        // dB Meter Section
        VBox dbSection = createControlSection("OUTPUT LEVEL");
        dbMeterCanvas = new DBMeterCanvasFX();
        dbMeterCanvas.setWidth(220);
        dbMeterCanvas.setHeight(180);
        dbSection.getChildren().add(dbMeterCanvas);
        
        leftPanel.getChildren().addAll(eqSection, volumeSection, dbSection);
        return leftPanel;
    }
    
    public float getBassSliderValue() {
        return (float) bassSlider.getValue();
    }
    
    public float getMidSliderValue() {
        return (float) midSlider.getValue();
    }
    
    public float getTrebleSliderValue() {
        return (float) trebleSlider.getValue();
    }


    
    /**
     * Creates a section container for controls
     */
    private VBox createControlSection(String title) {
        VBox section = new VBox(15);
        
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Consolas", FontWeight.BOLD, 11));
        titleLabel.setTextFill(Paint.valueOf(currentTheme.accentColor));
        titleLabel.setStyle("-fx-letter-spacing: 1.5px;");
        
        Separator separator = new Separator();
        separator.setStyle("-fx-background-color: " + currentTheme.accentColor + "44;");
        
        section.getChildren().addAll(titleLabel, separator);
        return section;
    }
    
    /**
     * Creates a modern styled slider with label
     */
    private VBox createModernSlider(String name, double min, double max, double initial, Label valueLabel) {
        VBox container = new VBox(8);
        
        HBox labelRow = new HBox();
        labelRow.setAlignment(Pos.CENTER);
        HBox.setHgrow(labelRow, Priority.ALWAYS);
        
        Label nameLabel = new Label(name);
        nameLabel.setFont(Font.font("Consolas", FontWeight.SEMI_BOLD, 10));
        nameLabel.setTextFill(Paint.valueOf(currentTheme.textPrimary));
        
        valueLabel.setFont(Font.font("Consolas", FontWeight.BOLD, 10));
        valueLabel.setTextFill(Paint.valueOf(currentTheme.accentColor));
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        labelRow.getChildren().addAll(nameLabel, spacer, valueLabel);
        
        VBox sliderBox = new VBox();
        Slider slider = new Slider(min, max, initial);
        slider.setShowTickLabels(false);
        slider.setShowTickMarks(false);
        slider.setStyle(
            "-fx-control-inner-background: " + currentTheme.sliderBackground + ";" +
            "-fx-accent: " + currentTheme.accentColor + ";"
        );
        
        sliderBox.getChildren().add(slider);
        
        container.getChildren().addAll(labelRow, sliderBox);
        return container;
    }
    
    /**
     * Creates the bottom control panel with playback and features
     */
    private VBox createBottomControls() {
        VBox bottom = new VBox(15);
        bottom.setPadding(new Insets(20, 30, 25, 30));
        bottom.setStyle(
            "-fx-background-color: rgba(0, 0, 0, 0.4);" +
            "-fx-border-color: " + currentTheme.accentColor + "22;" +
            "-fx-border-width: 1 0 0 0;"
        );
        
        // Row 1: Song selection and playback
        HBox row1 = new HBox(15);
        row1.setAlignment(Pos.CENTER);
        
        songDropdown = songMenu.getDropdown();
        selectedSongName = songDropdown.getValue();
        songSelectButton = new Button();
        songSelectButton.setPrefWidth(300);
        songSelectButton.setPrefHeight(34);
        styleSongSelectButton(songSelectButton);
        updateSongSelectButtonText();
        songSelectButton.setOnAction(e -> showSongPicker());
        
        playButton = createModernButton("▶", "Play", currentTheme.playColor);
        pauseButton = createModernButton("⏸", "Pause", currentTheme.pauseColor);
        stopButton = createModernButton("⏹", "Stop", currentTheme.stopColor);
        Button prevButton = createSecondaryButton("⏮ Prev");
        Button nextButton = createSecondaryButton("⏭ Next");
        
        playButton.setOnAction(e -> {
            playSelectedSong();
            String songName = songDropdown.getValue();
            if (songName != null) {
                nowPlayingLabel.setText(songName);
                animateNowPlaying();
            }
        });
        pauseButton.setOnAction(e -> AudioProcessor.pauseAudioPlayback());
        stopButton.setOnAction(e -> {
            AudioProcessor.stopAudioPlayback(true);
            nowPlayingLabel.setText("Stopped");
        });
        prevButton.setOnAction(e -> playPreviousFromQueue());
        nextButton.setOnAction(e -> playNextFromQueue(true));

        autoLevelCheckbox = createStyledCheckbox("Auto Level");
        autoLevelCheckbox.setSelected(true);
        autoLevelCheckbox.setOnAction(e -> 
            AudioProcessor.setAutoLevelEnabled(autoLevelCheckbox.isSelected())
        );
        
        echoCancelCheckbox = createStyledCheckbox("Echo Cancel");
        echoCancelCheckbox.setOnAction(e -> 
            AudioProcessor.setEchoCancelEnabled(echoCancelCheckbox.isSelected())
        );
        
        Button savePresetBtn = createSecondaryButton("💾 Save Preset");
        Button loadPresetBtn = createSecondaryButton("📂 Load Preset");
        
        savePresetBtn.setOnAction(e -> savePreset());
        loadPresetBtn.setOnAction(e -> loadPreset());
    
        row1.getChildren().addAll(songSelectButton, playButton, pauseButton, stopButton, prevButton, nextButton, autoLevelCheckbox, echoCancelCheckbox, savePresetBtn, loadPresetBtn);

        bottom.getChildren().addAll(row1);
        return bottom;
    }
    
    /**
     * Creates a modern playback control button
     */
    private Button createModernButton(String icon, String text, String color) {
        Button btn = new Button(icon);
        btn.setPrefSize(60, 60);
        btn.setFont(Font.font("Segoe UI Symbol", 20));
        btn.setStyle(
            "-fx-background-color: linear-gradient(to bottom, " + color + ", " + adjustBrightness(color, -20) + ");" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 30;" +
            "-fx-cursor: hand;" +
            "-fx-border-color: " + adjustBrightness(color, 20) + ";" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 30;"
        );
        
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.web(color, 0.5));
        shadow.setRadius(15);
        btn.setEffect(shadow);
        
        Tooltip tooltip = new Tooltip(text);
        Tooltip.install(btn, tooltip);
        
        btn.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), btn);
            st.setToX(1.1);
            st.setToY(1.1);
            st.play();
        });
        
        btn.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), btn);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });
        
        return btn;
    }
    
    /**
     * Creates a secondary action button
     */
    private Button createSecondaryButton(String text) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Consolas", FontWeight.SEMI_BOLD, 11));
        btn.setStyle(
            "-fx-background-color: " + currentTheme.buttonBackground + ";" +
            "-fx-text-fill: " + currentTheme.textPrimary + ";" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 10 18;" +
            "-fx-cursor: hand;" +
            "-fx-border-color: " + currentTheme.accentColor + "44;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 8;"
        );
        
        btn.setOnMouseEntered(e -> 
            btn.setStyle(btn.getStyle() + "-fx-background-color: " + currentTheme.accentColor + "33;")
        );
        btn.setOnMouseExited(e -> 
            btn.setStyle(btn.getStyle().replace("-fx-background-color: " + currentTheme.accentColor + "33;", ""))
        );
        
        return btn;
    }
    
    /**
     * Creates an icon button
     */
    private Button createIconButton(String icon, String tooltip) {
        Button btn = new Button(icon);
        btn.setPrefSize(36, 36);
        btn.setFont(Font.font("Consolas", FontWeight.BOLD, 16));
        btn.setStyle(
            "-fx-background-color: " + currentTheme.buttonBackground + ";" +
            "-fx-text-fill: " + currentTheme.accentColor + ";" +
            "-fx-background-radius: 18;" +
            "-fx-cursor: hand;" +
            "-fx-border-color: " + currentTheme.accentColor + "44;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 18;"
        );
        
        Tooltip tip = new Tooltip(tooltip);
        Tooltip.install(btn, tip);
        
        return btn;
    }
    
    /**
     * Creates a styled checkbox
     */
    private CheckBox createStyledCheckbox(String text) {
        CheckBox cb = new CheckBox(text);
        cb.setFont(Font.font("Consolas", FontWeight.SEMI_BOLD, 11));
        cb.setTextFill(Paint.valueOf(currentTheme.textPrimary));
        return cb;
    }
    
    /**
     * Styles the combo box
     */
    private void styleComboBox(ComboBox<String> cb) {
        cb.setStyle(
            "-fx-background-color: " + currentTheme.buttonBackground + ";" +
            "-fx-text-fill: " + currentTheme.textPrimary + ";" +
            "-fx-font-family: 'Consolas';" +
            "-fx-font-size: 12px;" +
            "-fx-background-radius: 8;" +
            "-fx-border-color: " + currentTheme.accentColor + "44;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 8;"
        );
    }

    private void styleSongSelectButton(Button button) {
        button.setStyle(
            "-fx-background-color: " + currentTheme.buttonBackground + ";" +
            "-fx-text-fill: " + currentTheme.textPrimary + ";" +
            "-fx-font-family: 'Consolas';" +
            "-fx-font-size: 12px;" +
            "-fx-background-radius: 8;" +
            "-fx-border-color: " + currentTheme.accentColor + "44;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 8;" +
            "-fx-alignment: center-left;" +
            "-fx-padding: 6 12;"
        );
    }

    private void updateSongSelectButtonText() {
        String label;
        if (selectedOnlineSong != null) {
            label = "Online: " + selectedOnlineSong.getDisplayName();
        } else {
            label = selectedSongName != null ? selectedSongName : "Select Song";
        }
        if (songSelectButton != null) {
            songSelectButton.setText(label + "  ▼");
        }
    }

    private void setSelectedSong(String songName) {
        if (songName == null || songName.isBlank()) {
            return;
        }
        selectedOnlineSong = null;
        selectedSongName = songName;
        songMenu.setSelectedSong(songName);
        updateSongSelectButtonText();
    }

    private void setSelectedOnlineSong(OnlineSongResult song) {
        if (song == null) {
            return;
        }
        selectedOnlineSong = song;
        updateSongSelectButtonText();
    }

    private void styleTextField(TextField field) {
        field.setStyle(
            "-fx-background-color: " + currentTheme.buttonBackground + ";" +
            "-fx-text-fill: " + currentTheme.textPrimary + ";" +
            "-fx-prompt-text-fill: " + currentTheme.textSecondary + ";" +
            "-fx-font-family: 'Consolas';" +
            "-fx-font-size: 12px;" +
            "-fx-background-radius: 8;" +
            "-fx-border-color: " + currentTheme.accentColor + "44;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 8;"
        );
    }

    private void styleListView(ListView<?> listView) {
        listView.setStyle(
            "-fx-background-color: " + currentTheme.cardBackground + ";" +
            "-fx-control-inner-background: " + currentTheme.cardBackground + ";" +
            "-fx-border-color: " + currentTheme.accentColor + "22;" +
            "-fx-border-radius: 8;" +
            "-fx-background-radius: 8;" +
            "-fx-selection-bar: " + selectionBackground() + ";" +
            "-fx-selection-bar-non-focused: " + selectionBackground() + ";" +
            "-fx-selection-bar-text: " + selectionTextColor() + ";"
        );
    }

    private String selectionBackground() {
        return currentTheme.accentColor + "55";
    }

    private String selectionTextColor() {
        return "#ffffff";
    }

    private void showSongPicker() {
        if (songPickerStage != null && songPickerStage.isShowing()) {
            songPickerStage.toFront();
            return;
        }

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-tab-min-width: 180;" +
            "-fx-tab-min-height: 36;" +
            "-fx-tab-max-width: 240;"
        );

        Tab localTab = new Tab("Local Library", createLocalLibraryPane());
        Tab onlineTab = new Tab("Online Search", createOnlineSearchRow());
        localTab.setClosable(false);
        onlineTab.setClosable(false);
        tabs.getTabs().addAll(localTab, onlineTab);

        Label headerTitle = new Label("SONG SELECTION");
        headerTitle.setFont(Font.font("Consolas", FontWeight.BOLD, 16));
        headerTitle.setTextFill(Paint.valueOf(currentTheme.accentColor));
        headerTitle.setStyle("-fx-letter-spacing: 2px;");

        Label headerSubtitle = new Label("Local library and online search");
        headerSubtitle.setFont(Font.font("Consolas", FontWeight.SEMI_BOLD, 10));
        headerSubtitle.setTextFill(Paint.valueOf(currentTheme.textSecondary));

        VBox headerBox = new VBox(4, headerTitle, headerSubtitle);
        headerBox.setPadding(new Insets(0, 0, 10, 0));

        BorderPane container = new BorderPane(tabs);
        container.setPadding(new Insets(20));
        container.setTop(headerBox);
        BorderPane.setMargin(tabs, new Insets(10, 0, 0, 0));
        container.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, " +
            currentTheme.bgPrimary + ", " + currentTheme.bgSecondary + ");"
        );

        Scene scene = new Scene(container, 1180, 700);
        songPickerStage = new Stage();
        songPickerStage.setTitle("Select a Song");
        songPickerStage.initModality(Modality.APPLICATION_MODAL);
        songPickerStage.setScene(scene);
        songPickerStage.setOnHidden(e -> songPickerStage = null);
        songPickerStage.show();
    }

    private VBox createLocalLibraryPane() {
        VBox container = new VBox(12);
        container.setPadding(new Insets(20));
        container.setStyle(
            "-fx-background-color: " + currentTheme.cardBackground + ";" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: " + currentTheme.accentColor + "22;" +
            "-fx-border-radius: 12;"
        );

        Label title = new Label("LOCAL LIBRARY");
        title.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
        title.setTextFill(Paint.valueOf(currentTheme.accentColor));
        title.setStyle("-fx-letter-spacing: 1.5px;");

        Label subtitle = new Label("Select, queue, or manage local files");
        subtitle.setFont(Font.font("Consolas", FontWeight.SEMI_BOLD, 9));
        subtitle.setTextFill(Paint.valueOf(currentTheme.textSecondary));

        localSongsList = new ListView<>();
        localSongsList.setItems(localSongItems);
        localSongsList.setPrefHeight(460);
        styleListView(localSongsList);
        localSongsList.setCellFactory(list -> createLocalSongCell());
        refreshLocalSongsList();

        localSongsList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                selectLocalSongAndClose();
            }
        });

        HBox buttons = new HBox(10);
        Button selectBtn = createSecondaryButton("✅ Select");
        Button addBtn = createSecondaryButton("➕ Add File");
        Button queueBtn = createSecondaryButton("➕ Queue");
        Button removeBtn = createSecondaryButton("🗑 Remove");

        selectBtn.setOnAction(e -> selectLocalSongAndClose());
        addBtn.setOnAction(e -> {
            songMenu.addNewFile();
            refreshLocalSongsList();
            syncSelectedSongFromMenu();
        });
        queueBtn.setOnAction(e -> addSelectedLocalToQueue());
        removeBtn.setOnAction(e -> removeSelectedLocalSong());

        buttons.getChildren().addAll(selectBtn, queueBtn, addBtn, removeBtn);
        container.getChildren().addAll(title, subtitle, localSongsList, buttons);
        return container;
    }

    private void refreshLocalSongsList() {
        localSongItems.setAll(songMenu.getSongNames());
        if (selectedSongName != null) {
            localSongsList.getSelectionModel().select(selectedSongName);
        }
    }

    private void syncSelectedSongFromMenu() {
        String current = songDropdown.getValue();
        selectedSongName = current;
        updateSongSelectButtonText();
    }

    private void selectLocalSongAndClose() {
        if (localSongsList == null) {
            return;
        }
        String selected = localSongsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Select Song", "Choose a local song to continue.");
            return;
        }
        setSelectedSong(selected);
        if (songPickerStage != null) {
            songPickerStage.close();
        }
    }

    private void removeSelectedLocalSong() {
        if (localSongsList == null) {
            return;
        }
        String selected = localSongsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Remove Song", "Select a song to remove.");
            return;
        }

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirm Removal");
        confirmDialog.setHeaderText("Remove \"" + selected + "\"?");
        confirmDialog.setContentText("This will remove the song from the library (not delete the file).");

        if (confirmDialog.showAndWait().filter(btn -> btn == javafx.scene.control.ButtonType.OK).isPresent()) {
            boolean removed = songMenu.removeSongEntry(selected);
            if (removed) {
                refreshLocalSongsList();
                if (selected.equals(selectedSongName)) {
                    syncSelectedSongFromMenu();
                }
            }
        }
    }

    private VBox createOnlineSearchRow() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(20));
        root.setStyle(
            "-fx-background-color: " + currentTheme.cardBackground + ";" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: " + currentTheme.accentColor + "22;" +
            "-fx-border-radius: 12;"
        );

        VBox searchBox = new VBox(10);
        Label searchLabel = new Label("ONLINE SEARCH");
        searchLabel.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
        searchLabel.setTextFill(Paint.valueOf(currentTheme.accentColor));
        searchLabel.setStyle("-fx-letter-spacing: 1.5px;");

        Label searchSubtitle = new Label("Find tracks on YouTube Music");
        searchSubtitle.setFont(Font.font("Consolas", FontWeight.SEMI_BOLD, 9));
        searchSubtitle.setTextFill(Paint.valueOf(currentTheme.textSecondary));

        HBox searchInputRow = new HBox(10);
        searchField = new TextField();
        searchField.setPromptText("Search YouTube Music...");
        searchField.setPrefWidth(520);
        styleTextField(searchField);
        searchField.setOnAction(e -> searchOnlineSongs());
        HBox.setHgrow(searchField, Priority.ALWAYS);

        searchButton = createSecondaryButton("🔍 Search");
        searchButton.setOnAction(e -> searchOnlineSongs());

        searchInputRow.getChildren().addAll(searchField, searchButton);

        searchStatusLabel = new Label(" ");
        searchStatusLabel.setFont(Font.font("Consolas", FontWeight.SEMI_BOLD, 10));
        searchStatusLabel.setTextFill(Paint.valueOf(currentTheme.textSecondary));

        searchBox.getChildren().addAll(searchLabel, searchSubtitle, searchInputRow, searchStatusLabel);

        HBox listsRow = new HBox(20);

        VBox resultsBox = new VBox(10);
        Label resultsLabel = new Label("RESULTS");
        resultsLabel.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
        resultsLabel.setTextFill(Paint.valueOf(currentTheme.accentColor));
        resultsLabel.setStyle("-fx-letter-spacing: 1.5px;");

        searchResultsList = new ListView<>();
        searchResultsList.setItems(searchResults);
        searchResultsList.setCellFactory(list -> createOnlineSongCell());
        searchResultsList.setPrefSize(620, 380);
        styleListView(searchResultsList);
        searchResultsList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                selectOnlineSong();
            }
        });

        HBox resultsButtons = new HBox(10);
        Button selectBtn = createSecondaryButton("✅ Select");
        Button addQueueBtn = createSecondaryButton("➕ Queue");
        Button importBtn = createSecondaryButton("⬇ Import");
        selectBtn.setOnAction(e -> selectOnlineSong());
        addQueueBtn.setOnAction(e -> addSelectedToQueue());
        importBtn.setOnAction(e -> importSelectedSong());
        resultsButtons.getChildren().addAll(selectBtn, addQueueBtn, importBtn);

        resultsBox.getChildren().addAll(resultsLabel, searchResultsList, resultsButtons);

        VBox queueBox = new VBox(10);
        Label queueLabel = new Label("QUEUE");
        queueLabel.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
        queueLabel.setTextFill(Paint.valueOf(currentTheme.accentColor));
        queueLabel.setStyle("-fx-letter-spacing: 1.5px;");

        queueList = new ListView<>();
        queueList.setItems(queueItems);
        queueList.setCellFactory(list -> createQueueItemCell());
        queueList.setPrefSize(420, 380);
        styleListView(queueList);
        queueList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                removeSelectedQueueItem();
            }
        });

        HBox queueButtons = new HBox(10);
        Button removeQueueBtn = createSecondaryButton("🗑 Remove");
        Button clearQueueBtn = createSecondaryButton("🧹 Clear");
        removeQueueBtn.setOnAction(e -> removeSelectedQueueItem());
        clearQueueBtn.setOnAction(e -> queueItems.clear());
        queueButtons.getChildren().addAll(removeQueueBtn, clearQueueBtn);

        queueBox.getChildren().addAll(queueLabel, queueList, queueButtons);

        HBox.setHgrow(resultsBox, Priority.ALWAYS);
        listsRow.getChildren().addAll(resultsBox, queueBox);

        root.getChildren().addAll(searchBox, listsRow);
        return root;
    }

    private ListCell<OnlineSongResult> createOnlineSongCell() {
        return new ListCell<>() {
            private final ImageView thumbnail = new ImageView();
            private final Label titleLabel = new Label();
            private final Label artistLabel = new Label();
            private final Label durationLabel = new Label();
            private final VBox textBox = new VBox(2, titleLabel, artistLabel);
            private final Region spacer = new Region();
            private final HBox content = new HBox(10, thumbnail, textBox, spacer, durationLabel);

            {
                thumbnail.setFitWidth(48);
                thumbnail.setFitHeight(48);
                thumbnail.setPreserveRatio(true);
                titleLabel.setFont(Font.font("Consolas", FontWeight.BOLD, 11));
                titleLabel.setTextFill(Paint.valueOf(currentTheme.textPrimary));
                artistLabel.setFont(Font.font("Consolas", FontWeight.SEMI_BOLD, 9));
                artistLabel.setTextFill(Paint.valueOf(currentTheme.textSecondary));
                durationLabel.setFont(Font.font("Consolas", FontWeight.SEMI_BOLD, 9));
                durationLabel.setTextFill(Paint.valueOf(currentTheme.textSecondary));
                HBox.setHgrow(spacer, Priority.ALWAYS);
                setStyle("-fx-padding: 6;");
            }

            @Override
            protected void updateItem(OnlineSongResult item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    if (item.getThumbnailUrl() != null && !item.getThumbnailUrl().isEmpty()) {
                        thumbnail.setImage(new Image(item.getThumbnailUrl(), true));
                    } else {
                        thumbnail.setImage(null);
                    }
                    titleLabel.setText(item.getTitle());
                    artistLabel.setText(item.getArtist().isEmpty() ? "YouTube Music" : item.getArtist());
                    durationLabel.setText(item.getDuration());

                    if (isSelected()) {
                        setStyle("-fx-background-color: " + selectionBackground() + "; -fx-background-radius: 8;");
                        titleLabel.setTextFill(Paint.valueOf(selectionTextColor()));
                        artistLabel.setTextFill(Paint.valueOf(selectionTextColor()));
                        durationLabel.setTextFill(Paint.valueOf(selectionTextColor()));
                    } else {
                        setStyle("");
                        titleLabel.setTextFill(Paint.valueOf(currentTheme.textPrimary));
                        artistLabel.setTextFill(Paint.valueOf(currentTheme.textSecondary));
                        durationLabel.setTextFill(Paint.valueOf(currentTheme.textSecondary));
                    }
                    setGraphic(content);
                }
            }
        };
    }

    private ListCell<SongQueueItem> createQueueItemCell() {
        return new ListCell<>() {
            private final ImageView thumbnail = new ImageView();
            private final Label titleLabel = new Label();
            private final Label sourceLabel = new Label();
            private final VBox textBox = new VBox(2, titleLabel, sourceLabel);
            private final HBox content = new HBox(10, thumbnail, textBox);

            {
                thumbnail.setFitWidth(40);
                thumbnail.setFitHeight(40);
                thumbnail.setPreserveRatio(true);
                titleLabel.setFont(Font.font("Consolas", FontWeight.BOLD, 11));
                sourceLabel.setFont(Font.font("Consolas", FontWeight.SEMI_BOLD, 9));
                sourceLabel.setTextFill(Paint.valueOf(currentTheme.textSecondary));
            }

            @Override
            protected void updateItem(SongQueueItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    if (item.isOnline() && item.getOnlineSource() != null && !item.getOnlineSource().getThumbnailUrl().isEmpty()) {
                        thumbnail.setImage(new Image(item.getOnlineSource().getThumbnailUrl(), true));
                    } else {
                        thumbnail.setImage(null);
                    }
                    titleLabel.setText(item.getDisplayName());
                    sourceLabel.setText(item.isOnline() ? "Online" : "Local");

                    if (isSelected()) {
                        setStyle("-fx-background-color: " + selectionBackground() + "; -fx-background-radius: 8;");
                        titleLabel.setTextFill(Paint.valueOf(selectionTextColor()));
                        sourceLabel.setTextFill(Paint.valueOf(selectionTextColor()));
                    } else {
                        setStyle("");
                        titleLabel.setTextFill(Paint.valueOf(currentTheme.textPrimary));
                        sourceLabel.setTextFill(Paint.valueOf(currentTheme.textSecondary));
                    }
                    setGraphic(content);
                }
            }
        };
    }

    private ListCell<String> createLocalSongCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (isSelected()) {
                        setStyle("-fx-background-color: " + selectionBackground() + "; -fx-text-fill: " + selectionTextColor() + "; -fx-background-radius: 8;");
                    } else {
                        setStyle("-fx-text-fill: " + currentTheme.textPrimary + ";");
                    }
                }
            }
        };
    }
    
    /**
     * Switches to a new theme with animation
     */
    private void switchTheme(Theme newTheme) {
        currentTheme = newTheme;
        
        // Fade out
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), root);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.7);
        
        fadeOut.setOnFinished(e -> {
            applyThemeBackground();
            updateThemeColors();
            
            // Fade in
            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), root);
            fadeIn.setFromValue(0.7);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
        
        fadeOut.play();
    }
    
    /**
     * Applies theme background
     */
    private void applyThemeBackground() {
        root.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, " + 
            currentTheme.bgPrimary + ", " + currentTheme.bgSecondary + ");"
        );
    }
    
    /**
     * Updates all UI colors to match current theme
     */
    private void updateThemeColors() {
        // Recreate UI sections with new theme
        root.setTop(createTopBar());
        root.setLeft(createLeftPanel());
        root.setBottom(createBottomControls());
        
        // Update center content styles without recreating visualizers
        applyThemeBackground();
        if (waveformContainer != null) {
            applyGlassPanelStyle(waveformContainer);
        }
        if (spectrumContainer != null) {
            applyGlassPanelStyle(spectrumContainer);
        }
        if (visualizerContainer != null) {
            applyGlassPanelStyle(visualizerContainer);
        }
    }
    
    /**
     * Plays entrance animation
     */
    private void playEntranceAnimation() {
        root.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(600), root);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }
    
    /**
     * Animates the now playing label
     */
    private void animateNowPlaying() {
        ScaleTransition pulse = new ScaleTransition(Duration.millis(200), nowPlayingLabel);
        pulse.setFromX(1.0);
        pulse.setFromY(1.0);
        pulse.setToX(1.05);
        pulse.setToY(1.05);
        pulse.setCycleCount(2);
        pulse.setAutoReverse(true);
        pulse.play();
    }
    
    /**
     * Utility to adjust color brightness
     */
    private String adjustBrightness(String hexColor, int percent) {
        Color color = Color.web(hexColor);
        double factor = 1 + (percent / 100.0);
        double r = Math.min(1.0, color.getRed() * factor);
        double g = Math.min(1.0, color.getGreen() * factor);
        double b = Math.min(1.0, color.getBlue() * factor);
        return String.format("#%02X%02X%02X", 
            (int)(r * 255), (int)(g * 255), (int)(b * 255));
    }

    private void searchOnlineSongs() {
        String query = searchField != null ? searchField.getText().trim() : "";
        if (query.isEmpty()) {
            showInfo("Search", "Enter a song title or artist to search.");
            return;
        }

        searchButton.setDisable(true);
        searchStatusLabel.setText("Searching...");

        Task<List<OnlineSongResult>> task = new Task<>() {
            @Override
            protected List<OnlineSongResult> call() throws Exception {
                return onlineSearchService.search(query, 12);
            }
        };

        task.setOnSucceeded(e -> {
            List<OnlineSongResult> results = task.getValue();
            searchResults.setAll(results);
            searchStatusLabel.setText("Found " + results.size() + " results");
            searchButton.setDisable(false);
        });

        task.setOnFailed(e -> {
            searchStatusLabel.setText("Search failed");
            searchButton.setDisable(false);
            showError("Online Search Error", task.getException() != null ? task.getException().getMessage() : "Search failed.");
        });

        onlineExecutor.submit(task);
    }

    private void playSelectedSearchResult() {
        OnlineSongResult selected = searchResultsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Play Online", "Select a song from the search results.");
            return;
        }
        playOnlineSong(selected, true);
    }

    private void selectOnlineSong() {
        OnlineSongResult selected = searchResultsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Select Song", "Select a song from the search results.");
            return;
        }
        setSelectedOnlineSong(selected);
        if (songPickerStage != null) {
            songPickerStage.close();
        }
    }

    private void addSelectedToQueue() {
        OnlineSongResult selected = searchResultsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Queue", "Select a song to add to the queue.");
            return;
        }
        queueItems.add(SongQueueItem.forOnline(selected));
        searchStatusLabel.setText("Added to queue");
    }

    private void addSelectedLocalToQueue() {
        if (localSongsList == null) {
            return;
        }
        String selected = localSongsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Queue", "Select a local song to queue.");
            return;
        }
        String path = songMenu.getSongPath(selected);
        if (path == null) {
            showError("Queue", "Selected song path was not found.");
            return;
        }
        queueItems.add(SongQueueItem.forLocal(selected, path));
        showInfo("Queue", "Added \"" + selected + "\" to the queue.");
    }

    private void importSelectedSong() {
        OnlineSongResult selected = searchResultsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Import", "Select a song to import.");
            return;
        }

        downloadOnlineSong(selected, path -> {
            String baseName = selected.getDisplayName();
            String uniqueName = ensureUniqueSongName(baseName);
            boolean added = songMenu.addSongEntry(uniqueName, path.toString());
            if (added) {
                if (localSongsList != null) {
                    refreshLocalSongsList();
                }
                showInfo("Import Complete", "Added \"" + uniqueName + "\" to your library.");
            } else {
                showInfo("Import", "That song is already in your library.");
            }
        });
    }

    private void playSelectedQueueItem() {
        SongQueueItem selected = queueList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Queue", "Select a queued song to play.");
            return;
        }
        queueItems.remove(selected);
        playQueueItem(selected, true);
    }

    private void removeSelectedQueueItem() {
        SongQueueItem selected = queueList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Queue", "Select a queued song to remove.");
            return;
        }
        queueItems.remove(selected);
    }

    private void playNextFromQueue() {
        playNextFromQueue(true);
    }

    private void playNextFromQueue(boolean userInitiated) {
        if (queueItems.isEmpty()) {
            if (userInitiated) {
                showInfo("Queue", "The queue is empty.");
            }
            return;
        }
        SongQueueItem next = queueItems.remove(0);
        playQueueItem(next, true);
    }

    private void playQueueItem(SongQueueItem item, boolean continueQueue) {
        if (currentQueueItem != null && currentQueueItem != item) {
            queueHistory.push(currentQueueItem);
        }
        currentQueueItem = item;

        if (item.isOnline() && item.getOnlineSource() != null) {
            setSelectedOnlineSong(item.getOnlineSource());
            playOnlineSong(item.getOnlineSource(), continueQueue);
        } else if (item.getFilePath() != null) {
            setSelectedSong(item.getDisplayName());
            nowPlayingLabel.setText(item.getDisplayName());
            startPlayback(item.getFilePath(), continueQueue ? () -> playNextFromQueue(false) : null);
        }
    }

    private void playPreviousFromQueue() {
        if (queueHistory.isEmpty()) {
            showInfo("Queue", "No previous song in queue history.");
            return;
        }
        if (currentQueueItem != null) {
            queueItems.add(0, currentQueueItem);
        }
        SongQueueItem previous = queueHistory.pop();
        playQueueItem(previous, true);
    }

    private void playOnlineSong(OnlineSongResult song, boolean continueQueue) {
        searchStatusLabel.setText("Downloading...");
        setSelectedOnlineSong(song);
        downloadOnlineSong(song, path -> {
            nowPlayingLabel.setText(song.getDisplayName());
            searchStatusLabel.setText("Playing");
            startPlayback(path.toString(), continueQueue ? () -> playNextFromQueue(false) : null);
        });
    }

    private void downloadOnlineSong(OnlineSongResult song, Consumer<Path> onSuccess) {
        Task<Path> task = new Task<>() {
            @Override
            protected Path call() throws Exception {
                return onlineAudioDownloader.getOrDownload(song);
            }
        };

        task.setOnSucceeded(e -> {
            Path result = task.getValue();
            if (result != null) {
                onSuccess.accept(result);
            } else {
                searchStatusLabel.setText("Download failed");
                showError("Download Error", "Downloaded file was not available.");
            }
        });

        task.setOnFailed(e -> {
            searchStatusLabel.setText("Download failed");
            showError("Download Error", task.getException() != null ? task.getException().getMessage() : "Download failed.");
        });

        onlineExecutor.submit(task);
    }

    private String ensureUniqueSongName(String baseName) {
        String name = baseName;
        int counter = 2;
        while (songMenu.containsSong(name)) {
            name = baseName + " (" + counter + ")";
            counter++;
        }
        return name;
    }

    private void startPlayback(String filePath, Runnable onFinished) {
        AudioProcessor.stopAudioPlayback(true);
        AudioProcessor.unpauseAudioPlayback();
        Thread playbackThread = new Thread(() -> AudioProcessor.playAudioWithEQ(
            filePath, bassGain, midGain, trebleGain,
            visualizerCanvas, spectrumCanvas, waveformCanvas, this, onFinished
        ));
        playbackThread.setDaemon(true);
        playbackThread.start();
    }
    
    // Existing methods from original code
    
    private void playSelectedSong() {
        if (selectedOnlineSong != null) {
            currentQueueItem = null;
            playOnlineSong(selectedOnlineSong, true);
            return;
        }
        String filePath = songMenu.getSelectedSongPath();
        if (filePath != null) {
            currentQueueItem = null;
            startPlayback(filePath, null);
        }
    }
    
    private void savePreset() {
        String selectedSong = songDropdown.getValue();
        if (selectedSong != null) {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Save Preset");
            dialog.setHeaderText("Enter preset name:");
            dialog.setContentText("Name:");
            
            dialog.showAndWait().ifPresent(name -> {
                if (!name.trim().isEmpty()) {
                    presetManager.savePreset(selectedSong, name.trim(),
                        (float)bassSlider.getValue(), (float)midSlider.getValue(), (float)trebleSlider.getValue());
                    showInfo("Success", "Preset '" + name + "' saved!");
                }
            });
        }
    }
    
    private void loadPreset() {
        String selectedSong = songDropdown.getValue();
        if (selectedSong != null) {
            var presets = presetManager.getPresetsForSong(selectedSong);
            if (presets.isEmpty()) {
                showInfo("No Presets", "No saved presets for this song.");
            } else {
                ChoiceDialog<String> dialog = new ChoiceDialog<>(
                    presets.keySet().iterator().next(), presets.keySet()
                );
                dialog.setTitle("Load Preset");
                dialog.setHeaderText("Choose preset:");
                
                dialog.showAndWait().ifPresent(preset -> {
                    float[] values = presets.get(preset);
                    bassSlider.setValue(values[0]);
                    midSlider.setValue(values[1]);
                    trebleSlider.setValue(values[2]);
                });
            }
        }
    }
    
    private void showSettings() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Settings");
        alert.setHeaderText("Audio Settings");
        alert.setContentText(
            "Current Configuration:\n\n" +
            "• Sample Rate: 44.1 kHz\n" +
            "• Bit Depth: 16-bit\n" +
            "• Channels: Stereo\n" +
            "• Buffer Size: 4096 bytes\n\n" +
            "Theme: " + currentTheme.name
        );
        alert.showAndWait();
    }
    
    private void showHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Help");
        alert.setHeaderText("Equalizer Pro - Quick Guide");
        alert.setContentText(
            "CONTROLS:\n" +
            "• Bass/Mid/Treble: Adjust frequency bands (-20 to +20 dB)\n" +
            "• Master Volume: Control overall output (0-200%)\n" +
            "• Auto Level: Normalize volume across tracks\n" +
            "• Echo Cancel: Reduce echo artifacts\n\n" +
            "SONG SELECTION:\n" +
            "• Use Select Song to open local + online picker\n\n" +
            "VISUALIZERS:\n" +
            "• Waveform: Time-domain audio representation\n" +
            "• Spectrum: Frequency analysis in real-time\n" +
            "• Bars: Amplitude visualization\n" +
            "• dB Meter: Output level monitoring\n\n" +
            "ONLINE SEARCH:\n" +
            "• Search YouTube Music and queue or import tracks\n" +
            "• Install yt-dlp: pip install yt-dlp\n" +
            "• Install ffmpeg and add it to PATH for WAV extraction\n" +
            "• Imported tracks appear in the song dropdown\n\n" +
            "THEMES:\n" +
            "Click colored circles in top bar to change theme\n\n" +
            "PRESETS:\n" +
            "Save and load custom EQ settings per song"
        );
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
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
}

/**
 * Theme definitions with color schemes
 */
enum Theme {
    CYBER_BLUE(
        "Cyber Blue",
        "#0a0e1a", "#1a1f3a",
        "#00d9ff", "#ffffff", "#8892b0",
        "rgba(15, 25, 45, 0.6)", "rgba(20, 30, 50, 0.7)", "rgba(30, 40, 60, 0.5)",
        "#4CAF50", "#FF9800", "#F44336"
    ),
    NEON_PURPLE(
        "Neon Purple",
        "#1a0a2e", "#2d1b4e",
        "#b84fff", "#ffffff", "#c4b5d6",
        "rgba(25, 10, 45, 0.6)", "rgba(35, 20, 60, 0.7)", "rgba(45, 30, 70, 0.5)",
        "#00ff88", "#ffaa00", "#ff4466"
    ),
    SUNSET_ORANGE(
        "Sunset Orange",
        "#1a0f0a", "#3a1f0e",
        "#ff6b35", "#ffffff", "#d4a574",
        "rgba(30, 15, 10, 0.6)", "rgba(50, 25, 15, 0.7)", "rgba(60, 35, 20, 0.5)",
        "#4CAF50", "#FFB300", "#E53935"
    ),
    MATRIX_GREEN(
        "Matrix Green",
        "#0a1a0a", "#0e2e0e",
        "#00ff41", "#ffffff", "#88cc88",
        "rgba(10, 25, 10, 0.6)", "rgba(15, 35, 15, 0.7)", "rgba(20, 45, 20, 0.5)",
        "#66BB6A", "#FFA726", "#EF5350"
    ),
    MIDNIGHT_BLUE(
        "Midnight Blue",
        "#0c1821", "#1b2a3e",
        "#00adb5", "#ffffff", "#a8c5d1",
        "rgba(12, 24, 33, 0.6)", "rgba(20, 35, 50, 0.7)", "rgba(30, 45, 60, 0.5)",
        "#26C6DA", "#FFCA28", "#FF7043"
    );
    
    final String name;
    final String bgPrimary, bgSecondary;
    final String accentColor, textPrimary, textSecondary;
    final String cardBackground, buttonBackground, sliderBackground;
    final String playColor, pauseColor, stopColor;
    
    Theme(String name, String bg1, String bg2, String accent, String text1, String text2,
          String card, String button, String slider, String play, String pause, String stop) {
        this.name = name;
        this.bgPrimary = bg1;
        this.bgSecondary = bg2;
        this.accentColor = accent;
        this.textPrimary = text1;
        this.textSecondary = text2;
        this.cardBackground = card;
        this.buttonBackground = button;
        this.sliderBackground = slider;
        this.playColor = play;
        this.pauseColor = pause;
        this.stopColor = stop;
    }
}

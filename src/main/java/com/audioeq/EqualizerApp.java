package com.audioeq;

import java.util.concurrent.CompletableFuture;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
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
import javafx.scene.media.AudioEqualizer;
import javafx.scene.media.EqualizerBand;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

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
    private Slider reverbSlider;
    private Slider widthSlider;
    
    private Label bassValueLabel;
    private Label midValueLabel;
    private Label trebleValueLabel;
    private Label volumeValueLabel;
    private Label reverbValueLabel;
    private Label widthValueLabel;
    private Label nowPlayingLabel;
    
    private ComboBox<String> songDropdown;
    private SongMenuFX songMenu;
    
    private CheckBox autoLevelCheckbox;
    private CheckBox limiterCheckbox;
    private ComboBox<ReverbPreset> reverbPresetBox;
    private ComboBox<MoodPreset> moodPresetBox;
    
    private VisualizerCanvasFX visualizerCanvas;
    private SpectrumCanvasFX spectrumCanvas;
    private DBMeterCanvasFX dbMeterCanvas;
    private WaveformCanvasFX waveformCanvas;
    
    private PresetManager presetManager = new PresetManager();
    
    private Button playButton;
    private Button pauseButton;
    private Button stopButton;

    private TextField searchField;
    private ProgressIndicator searchSpinner;
    private ListView<OnlineTrack> onlineResultsList;
    private ListView<OnlineTrack> onlineQueueList;
    private ObservableList<OnlineTrack> onlineResults = FXCollections.observableArrayList();
    private ObservableList<OnlineTrack> onlineQueue = FXCollections.observableArrayList();
    private OnlineSearchService onlineSearchService = new OnlineSearchService();
    private MediaPlayer onlinePlayer;
    
    // Theme system
    private Theme currentTheme = Theme.CYBER_BLUE;
    private BorderPane root;
    private VBox centerContent;
    private StackPane rootStack;
    private StackPane landingOverlay;
    private AnimatedBackdrop landingBackdrop;
    
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Audio Equalizer Pro");
        
        // Initialize components
        songMenu = new SongMenuFX();
        
        // Main layout
        root = new BorderPane();
        applyThemeBackground();
        
        // Create all sections
        root.setTop(createTopBar());
        centerContent = createCenterContent();
        root.setCenter(centerContent);
        root.setLeft(createLeftPanel());
        root.setBottom(createBottomControls());
        root.setRight(createRightPanel());

        rootStack = new StackPane(root);
        landingOverlay = createLandingOverlay();
        rootStack.getChildren().add(landingOverlay);
        
        Scene scene = new Scene(rootStack, 1400, 800);
        scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        
        primaryStage.show();
        
        // Entrance animation
        playEntranceAnimation();
        landingBackdrop.start();
        
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
        Label brandTitle = new Label("EQUALIZER");
        brandTitle.setFont(Font.font("Consolas", FontWeight.BOLD, 24));
        brandTitle.setTextFill(Paint.valueOf(currentTheme.accentColor));
        
        Label brandSubtitle = new Label("PRO AUDIO SUITE");
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

        // Mood selector
        HBox moodSelector = createMoodSelector();

        // Theme selector
        HBox themeSelector = createThemeSelector();
        
        // Settings button
        Button settingsBtn = createIconButton("⚙", "Settings");
        settingsBtn.setOnAction(e -> showSettings());
        
        // Help button  
        Button helpBtn = createIconButton("?", "Help");
        helpBtn.setOnAction(e -> showHelp());
        
        topBar.getChildren().addAll(branding, spacer, nowPlayingCompact, spacer2, moodSelector, themeSelector, settingsBtn, helpBtn);
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

    private HBox createMoodSelector() {
        HBox moodBox = new HBox(8);
        moodBox.setAlignment(Pos.CENTER);

        Label moodLabel = new Label("MOOD");
        moodLabel.setFont(Font.font("Consolas", FontWeight.SEMI_BOLD, 10));
        moodLabel.setTextFill(Paint.valueOf(currentTheme.textSecondary));
        moodLabel.setStyle("-fx-letter-spacing: 1px;");

        moodPresetBox = new ComboBox<>();
        moodPresetBox.getItems().addAll(MoodPreset.values());
        moodPresetBox.setValue(MoodPreset.CYBERPUNK);
        moodPresetBox.setPrefWidth(150);
        styleComboBox(moodPresetBox);
        moodPresetBox.setOnAction(e -> applyMood(moodPresetBox.getValue()));

        moodBox.getChildren().addAll(moodLabel, moodPresetBox);
        return moodBox;
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
        waveformCanvas.setAccentColor(currentTheme.accentColor);
        waveformCanvas.setWidth(1000);
        waveformCanvas.setHeight(80);
        StackPane waveformContainer = createGlassPanel(waveformCanvas, "WAVEFORM");
        
        // Spectrum analyzer
        spectrumCanvas = new SpectrumCanvasFX(1024);
        spectrumCanvas.setAccentColor(currentTheme.accentColor);
        spectrumCanvas.setWidth(1000);
        spectrumCanvas.setHeight(120);
        StackPane spectrumContainer = createGlassPanel(spectrumCanvas, "FREQUENCY SPECTRUM");
        
        // Bar visualizer
        visualizerCanvas = new VisualizerCanvasFX(12);
        visualizerCanvas.setAccentColor(currentTheme.accentColor);
        visualizerCanvas.setWidth(1000);
        visualizerCanvas.setHeight(100);
        StackPane visualizerContainer = createGlassPanel(visualizerCanvas, "AMPLITUDE BARS");
        
        visualizersBox.getChildren().addAll(
            waveformContainer,
            spectrumContainer,
            visualizerContainer
        );
        
        center.getChildren().addAll(visualizersBox);
        return center;
    }

    private StackPane createLandingOverlay() {
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6);");

        landingBackdrop = new AnimatedBackdrop();
        landingBackdrop.widthProperty().bind(root.widthProperty());
        landingBackdrop.heightProperty().bind(root.heightProperty());

        VBox content = new VBox(18);
        content.setAlignment(Pos.CENTER);

        Label title = new Label("AUDIO NEXUS");
        title.setFont(Font.font("Consolas", FontWeight.BOLD, 48));
        title.setTextFill(Paint.valueOf(currentTheme.accentColor));
        title.setStyle("-fx-letter-spacing: 6px;");

        Label subtitle = new Label("IMMERSIVE EQUALIZER EXPERIENCE");
        subtitle.setFont(Font.font("Consolas", FontWeight.LIGHT, 14));
        subtitle.setTextFill(Paint.valueOf(currentTheme.textSecondary));
        subtitle.setStyle("-fx-letter-spacing: 3px;");

        Button enterBtn = createModernButton("▶", "Enter", currentTheme.playColor);
        enterBtn.setText("ENTER EXPERIENCE");
        enterBtn.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
        enterBtn.setOnAction(e -> dismissLandingOverlay());

        content.getChildren().addAll(title, subtitle, enterBtn);
        overlay.getChildren().addAll(landingBackdrop, content);
        return overlay;
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
        wrapper.setStyle(
            "-fx-background-color: " + currentTheme.cardBackground + ";" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: " + currentTheme.accentColor + "22;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 12;"
        );
        
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.web(currentTheme.accentColor, 0.2));
        shadow.setRadius(15);
        wrapper.setEffect(shadow);
        
        return wrapper;
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
            syncOnlineEqualizer();
        });
        
        VBox midBox = createModernSlider("MID", -20, 20, 0, midValueLabel = new Label("0 dB"));
        midSlider = (Slider) ((VBox)midBox.getChildren().get(1)).getChildren().get(0);
        midSlider.valueProperty().addListener((obs, old, val) -> {
            midGain = val.floatValue();
            midValueLabel.setText(String.format("%.0f dB", val.doubleValue()));
            syncOnlineEqualizer();
        });
        
        VBox trebleBox = createModernSlider("TREBLE", -20, 20, 0, trebleValueLabel = new Label("0 dB"));
        trebleSlider = (Slider) ((VBox)trebleBox.getChildren().get(1)).getChildren().get(0);
        trebleSlider.valueProperty().addListener((obs, old, val) -> {
            trebleGain = val.floatValue();
            trebleValueLabel.setText(String.format("%.0f dB", val.doubleValue()));
            syncOnlineEqualizer();
        });
        
        eqSection.getChildren().addAll(bassBox, midBox, trebleBox);
        
        // Volume Section
        VBox volumeSection = createControlSection("MASTER VOLUME");
        
        VBox volumeBox = createModernSlider("VOLUME", 0, 200, 100, volumeValueLabel = new Label("100%"));
        volumeSlider = (Slider) ((VBox)volumeBox.getChildren().get(1)).getChildren().get(0);
        volumeSlider.valueProperty().addListener((obs, old, val) -> {
            AudioProcessor.setMasterVolume(val.floatValue() / 100.0f);
            volumeValueLabel.setText(String.format("%.0f%%", val.doubleValue()));
            if (onlinePlayer != null) {
                onlinePlayer.setVolume(val.doubleValue() / 100.0);
            }
        });
        
        volumeSection.getChildren().add(volumeBox);
        
        // Spatial Effects Section
        VBox effectsSection = createControlSection("SPATIAL FX");
        
        VBox reverbBox = createModernSlider("REVERB", 0, 100, 0, reverbValueLabel = new Label("0%"));
        reverbSlider = (Slider) ((VBox)reverbBox.getChildren().get(1)).getChildren().get(0);
        Tooltip reverbTip = new Tooltip("Reverb is applied to local file playback.");
        Tooltip.install(reverbSlider, reverbTip);
        reverbSlider.valueProperty().addListener((obs, old, val) -> {
            AudioProcessor.setReverbIntensity(val.floatValue() / 100.0f);
            reverbValueLabel.setText(String.format("%.0f%%", val.doubleValue()));
        });
        
        reverbPresetBox = new ComboBox<>();
        reverbPresetBox.getItems().addAll(ReverbPreset.values());
        reverbPresetBox.setValue(ReverbPreset.STUDIO);
        AudioProcessor.setReverbPreset(ReverbPreset.STUDIO);
        styleComboBox(reverbPresetBox);
        reverbPresetBox.setOnAction(e -> AudioProcessor.setReverbPreset(reverbPresetBox.getValue()));
        
        Label environmentLabel = new Label("ENVIRONMENT");
        environmentLabel.setFont(Font.font("Consolas", FontWeight.SEMI_BOLD, 9));
        environmentLabel.setTextFill(Paint.valueOf(currentTheme.textSecondary));
        environmentLabel.setStyle("-fx-letter-spacing: 1.2px;");
        
        VBox widthBox = createModernSlider("WIDTH", 60, 200, 100, widthValueLabel = new Label("100%"));
        widthSlider = (Slider) ((VBox)widthBox.getChildren().get(1)).getChildren().get(0);
        Tooltip widthTip = new Tooltip("Stereo width enhancement for local playback.");
        Tooltip.install(widthSlider, widthTip);
        AudioProcessor.setStereoWidth(1.0f);
        widthSlider.valueProperty().addListener((obs, old, val) -> {
            AudioProcessor.setStereoWidth(val.floatValue() / 100.0f);
            widthValueLabel.setText(String.format("%.0f%%", val.doubleValue()));
        });
        
        effectsSection.getChildren().addAll(reverbBox, environmentLabel, reverbPresetBox, widthBox);
        
        // dB Meter Section
        VBox dbSection = createControlSection("OUTPUT LEVEL");
        dbMeterCanvas = new DBMeterCanvasFX();
        dbMeterCanvas.setAccentColor(currentTheme.accentColor);
        dbMeterCanvas.setWidth(220);
        dbMeterCanvas.setHeight(180);
        dbSection.getChildren().add(dbMeterCanvas);
        
        leftPanel.getChildren().addAll(eqSection, volumeSection, effectsSection, dbSection);
        return leftPanel;
    }

    private VBox createRightPanel() {
        VBox rightPanel = new VBox(15);
        rightPanel.setPadding(new Insets(25, 30, 25, 20));
        rightPanel.setPrefWidth(340);
        rightPanel.setStyle(
            "-fx-background-color: rgba(0, 0, 0, 0.25);" +
            "-fx-border-color: " + currentTheme.accentColor + "22;" +
            "-fx-border-width: 0 0 0 1;"
        );

        Label title = new Label("ONLINE DISCOVERY");
        title.setFont(Font.font("Consolas", FontWeight.BOLD, 11));
        title.setTextFill(Paint.valueOf(currentTheme.accentColor));
        title.setStyle("-fx-letter-spacing: 1.5px;");

        searchField = new TextField();
        searchField.setPromptText("Search iTunes previews...");
        searchField.setPrefWidth(200);
        searchField.setOnAction(e -> performOnlineSearch());

        Button searchBtn = createSecondaryButton("🔍 Search");
        searchBtn.setOnAction(e -> performOnlineSearch());

        searchSpinner = new ProgressIndicator();
        searchSpinner.setPrefSize(20, 20);
        searchSpinner.setVisible(false);

        HBox searchRow = new HBox(8, searchField, searchBtn, searchSpinner);
        searchRow.setAlignment(Pos.CENTER_LEFT);

        onlineResultsList = new ListView<>(onlineResults);
        onlineResultsList.setCellFactory(list -> new OnlineTrackCell());
        onlineResultsList.setPrefHeight(260);
        styleListView(onlineResultsList);
        onlineResultsList.setOnMouseClicked(event -> {
            if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY && event.getClickCount() == 2) {
                playSelectedOnline();
            }
        });

        HBox resultsActions = new HBox(10);
        Button playPreviewBtn = createSecondaryButton("▶ Play Preview");
        playPreviewBtn.setOnAction(e -> playSelectedOnline());
        Button queueBtn = createSecondaryButton("➕ Queue");
        queueBtn.setOnAction(e -> queueSelectedOnline());
        resultsActions.getChildren().addAll(playPreviewBtn, queueBtn);

        Label queueLabel = new Label("QUEUE");
        queueLabel.setFont(Font.font("Consolas", FontWeight.SEMI_BOLD, 10));
        queueLabel.setTextFill(Paint.valueOf(currentTheme.textSecondary));
        queueLabel.setStyle("-fx-letter-spacing: 1.2px;");

        onlineQueueList = new ListView<>(onlineQueue);
        onlineQueueList.setCellFactory(list -> new OnlineTrackCell());
        onlineQueueList.setPrefHeight(160);
        styleListView(onlineQueueList);

        rightPanel.getChildren().addAll(title, searchRow, onlineResultsList, resultsActions, queueLabel, onlineQueueList);
        return rightPanel;
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
        songDropdown.setPrefWidth(300);
        styleComboBox(songDropdown);
        
        playButton = createModernButton("▶", "Play", currentTheme.playColor);
        pauseButton = createModernButton("⏸", "Pause", currentTheme.pauseColor);
        stopButton = createModernButton("⏹", "Stop", currentTheme.stopColor);
        
        playButton.setOnAction(e -> {
            playSelectedSong();
            String songName = songDropdown.getValue();
            if (songName != null) {
                nowPlayingLabel.setText(songName);
                animateNowPlaying();
            }
        });
        pauseButton.setOnAction(e -> {
            AudioProcessor.pauseAudioPlayback();
            if (onlinePlayer != null) {
                onlinePlayer.pause();
            }
        });
        stopButton.setOnAction(e -> {
            AudioProcessor.stopAudioPlayback(true);
            stopOnlinePlayback();
            nowPlayingLabel.setText("Stopped");
        });

        autoLevelCheckbox = createStyledCheckbox("Auto Level");
        autoLevelCheckbox.setSelected(true);
        AudioProcessor.setAutoLevelEnabled(true);
        autoLevelCheckbox.setOnAction(e -> 
            AudioProcessor.setAutoLevelEnabled(autoLevelCheckbox.isSelected())
        );

        limiterCheckbox = createStyledCheckbox("Limiter");
        limiterCheckbox.setSelected(true);
        AudioProcessor.setLimiterEnabled(true);
        limiterCheckbox.setOnAction(e ->
            AudioProcessor.setLimiterEnabled(limiterCheckbox.isSelected())
        );
        
        Button savePresetBtn = createSecondaryButton("💾 Save Preset");
        Button loadPresetBtn = createSecondaryButton("📂 Load Preset");
        Button addSongBtn = createSecondaryButton("➕ Add Song");
        Button removeSongBtn = createSecondaryButton("➖ Remove");
        
        savePresetBtn.setOnAction(e -> savePreset());
        loadPresetBtn.setOnAction(e -> loadPreset());
        addSongBtn.setOnAction(e -> songMenu.addNewFile());
        removeSongBtn.setOnAction(e -> songMenu.removeSelectedFile());
    
        row1.getChildren().addAll(songDropdown, playButton, pauseButton, stopButton, autoLevelCheckbox, limiterCheckbox, savePresetBtn, loadPresetBtn,
        addSongBtn, removeSongBtn);

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
    private void styleComboBox(ComboBox<?> cb) {
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

    private void styleListView(ListView<?> listView) {
        listView.setStyle(
            "-fx-background-color: " + currentTheme.cardBackground + ";" +
            "-fx-control-inner-background: " + currentTheme.cardBackground + ";" +
            "-fx-border-color: " + currentTheme.accentColor + "22;" +
            "-fx-border-radius: 8;" +
            "-fx-background-radius: 8;"
        );
    }

    private void setSpatialControlsDisabled(boolean disabled) {
        if (reverbSlider != null) {
            reverbSlider.setDisable(disabled);
        }
        if (reverbPresetBox != null) {
            reverbPresetBox.setDisable(disabled);
        }
        if (widthSlider != null) {
            widthSlider.setDisable(disabled);
        }
    }
    
    /**
     * Switches to a new theme with animation
     */
    private void switchTheme(Theme newTheme) {
        switchTheme(newTheme, null);
    }

    private void switchTheme(Theme newTheme, Runnable afterUpdate) {
        currentTheme = newTheme;

        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), root);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.7);

        fadeOut.setOnFinished(e -> {
            applyThemeBackground();
            updateThemeColors();

            if (afterUpdate != null) {
                afterUpdate.run();
            }

            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), root);
            fadeIn.setFromValue(0.7);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });

        fadeOut.play();
    }

    private void applyMood(MoodPreset mood) {
        if (mood == null) {
            return;
        }

        switchTheme(mood.theme, () -> {
            if (reverbPresetBox != null) {
                reverbPresetBox.setValue(mood.reverbPreset);
            }
            AudioProcessor.setReverbPreset(mood.reverbPreset);

            Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(bassSlider.valueProperty(), bassSlider.getValue()),
                    new KeyValue(midSlider.valueProperty(), midSlider.getValue()),
                    new KeyValue(trebleSlider.valueProperty(), trebleSlider.getValue()),
                    new KeyValue(reverbSlider.valueProperty(), reverbSlider.getValue()),
                    new KeyValue(widthSlider.valueProperty(), widthSlider.getValue())
                ),
                new KeyFrame(Duration.millis(600),
                    new KeyValue(bassSlider.valueProperty(), mood.bassDb),
                    new KeyValue(midSlider.valueProperty(), mood.midDb),
                    new KeyValue(trebleSlider.valueProperty(), mood.trebleDb),
                    new KeyValue(reverbSlider.valueProperty(), mood.reverbIntensity * 100.0),
                    new KeyValue(widthSlider.valueProperty(), mood.stereoWidth * 100.0)
                )
            );
            timeline.play();
        });
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
        double bass = bassSlider != null ? bassSlider.getValue() : 0;
        double mid = midSlider != null ? midSlider.getValue() : 0;
        double treble = trebleSlider != null ? trebleSlider.getValue() : 0;
        double volume = volumeSlider != null ? volumeSlider.getValue() : 100;
        double reverb = reverbSlider != null ? reverbSlider.getValue() : 0;
        double width = widthSlider != null ? widthSlider.getValue() : 100;
        String searchText = searchField != null ? searchField.getText() : "";
        MoodPreset mood = moodPresetBox != null ? moodPresetBox.getValue() : MoodPreset.CYBERPUNK;
        ReverbPreset reverbPreset = reverbPresetBox != null ? reverbPresetBox.getValue() : ReverbPreset.STUDIO;
        boolean autoLevel = autoLevelCheckbox != null && autoLevelCheckbox.isSelected();
        boolean limiter = limiterCheckbox != null && limiterCheckbox.isSelected();
        String selectedSong = songDropdown != null ? songDropdown.getValue() : null;
        boolean spatialDisabled = reverbSlider != null && reverbSlider.isDisable();
        String nowPlaying = nowPlayingLabel != null ? nowPlayingLabel.getText() : null;

        root.setTop(createTopBar());
        root.setLeft(createLeftPanel());
        root.setBottom(createBottomControls());
        root.setRight(createRightPanel());

        centerContent.getChildren().clear();
        VBox newCenter = createCenterContent();
        centerContent.getChildren().addAll(newCenter.getChildren());

        bassSlider.setValue(bass);
        midSlider.setValue(mid);
        trebleSlider.setValue(treble);
        volumeSlider.setValue(volume);
        reverbSlider.setValue(reverb);
        widthSlider.setValue(width);
        if (moodPresetBox != null) {
            moodPresetBox.setValue(mood);
        }
        if (reverbPresetBox != null) {
            reverbPresetBox.setValue(reverbPreset);
            AudioProcessor.setReverbPreset(reverbPreset);
        }
        if (autoLevelCheckbox != null) {
            autoLevelCheckbox.setSelected(autoLevel);
            AudioProcessor.setAutoLevelEnabled(autoLevel);
        }
        if (limiterCheckbox != null) {
            limiterCheckbox.setSelected(limiter);
            AudioProcessor.setLimiterEnabled(limiter);
        }
        if (searchField != null) {
            searchField.setText(searchText);
        }
        if (songDropdown != null && selectedSong != null) {
            songDropdown.setValue(selectedSong);
        }
        if (nowPlayingLabel != null && nowPlaying != null) {
            nowPlayingLabel.setText(nowPlaying);
        }
        setSpatialControlsDisabled(spatialDisabled);
        if (onlinePlayer != null) {
            configureOnlineEqualizer();
        }

        visualizerCanvas.setAccentColor(currentTheme.accentColor);
        spectrumCanvas.setAccentColor(currentTheme.accentColor);
        waveformCanvas.setAccentColor(currentTheme.accentColor);
        dbMeterCanvas.setAccentColor(currentTheme.accentColor);
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

    private void dismissLandingOverlay() {
        if (landingOverlay == null) {
            return;
        }

        FadeTransition fadeOut = new FadeTransition(Duration.millis(500), landingOverlay);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(500), landingOverlay);
        scaleOut.setToX(1.05);
        scaleOut.setToY(1.05);

        fadeOut.setOnFinished(e -> {
            rootStack.getChildren().remove(landingOverlay);
            landingBackdrop.stop();
        });

        fadeOut.play();
        scaleOut.play();
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
    
    // Existing methods from original code
    
    private void playSelectedSong() {
        String filePath = songMenu.getSelectedSongPath();
        if (filePath != null) {
            AudioProcessor.stopAudioPlayback(true);
            stopOnlinePlayback();
            setSpatialControlsDisabled(false);
            new Thread(() -> AudioProcessor.playAudioWithEQ(
                filePath, bassGain, midGain, trebleGain,
                visualizerCanvas, spectrumCanvas, waveformCanvas, this
            )).start();
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

    private void performOnlineSearch() {
        String query = searchField.getText() != null ? searchField.getText().trim() : "";
        if (query.isEmpty()) {
            return;
        }

        searchSpinner.setVisible(true);
        CompletableFuture
            .supplyAsync(() -> {
                try {
                    return onlineSearchService.search(query);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            })
            .whenComplete((results, error) -> Platform.runLater(() -> {
                searchSpinner.setVisible(false);
                if (error != null) {
                    showInfo("Search Error", "Unable to reach iTunes search right now.");
                    return;
                }
                onlineResults.setAll(results);
            }));
    }

    private void playSelectedOnline() {
        OnlineTrack selected = onlineResultsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            selected = onlineQueueList.getSelectionModel().getSelectedItem();
        }
        if (selected != null) {
            playOnlineTrack(selected);
        }
    }

    private void queueSelectedOnline() {
        OnlineTrack selected = onlineResultsList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            onlineQueue.add(selected);
        }
    }

    private void playOnlineTrack(OnlineTrack track) {
        AudioProcessor.stopAudioPlayback(true);
        stopOnlinePlayback();
        setSpatialControlsDisabled(true);

        try {
            Media media = new Media(track.getPreviewUrl());
            onlinePlayer = new MediaPlayer(media);
            onlinePlayer.setVolume(volumeSlider.getValue() / 100.0);
            configureOnlineEqualizer();
            configureOnlineSpectrum();

            onlinePlayer.setOnEndOfMedia(() -> {
                if (!onlineQueue.isEmpty()) {
                    OnlineTrack next = onlineQueue.remove(0);
                    playOnlineTrack(next);
                }
            });

            onlinePlayer.play();
            nowPlayingLabel.setText(track.getTitle());
            animateNowPlaying();
        } catch (Exception e) {
            showInfo("Playback Error", "Unable to stream the preview right now.");
        }
    }

    private void stopOnlinePlayback() {
        if (onlinePlayer != null) {
            onlinePlayer.stop();
            onlinePlayer.dispose();
            onlinePlayer = null;
        }
        setSpatialControlsDisabled(false);
    }

    private void configureOnlineEqualizer() {
        if (onlinePlayer == null) {
            return;
        }

        AudioEqualizer eq = onlinePlayer.getAudioEqualizer();
        eq.setEnabled(true);

        if (eq.getBands().isEmpty() || eq.getBands().size() != 3) {
            eq.getBands().clear();
            eq.getBands().addAll(
                new EqualizerBand(120, 120, bassSlider.getValue()),
                new EqualizerBand(1000, 1000, midSlider.getValue()),
                new EqualizerBand(8000, 2000, trebleSlider.getValue())
            );
        } else if (eq.getBands().size() >= 3) {
            eq.getBands().get(0).setGain(bassSlider.getValue());
            eq.getBands().get(1).setGain(midSlider.getValue());
            eq.getBands().get(2).setGain(trebleSlider.getValue());
        }
    }

    private void syncOnlineEqualizer() {
        if (onlinePlayer != null) {
            configureOnlineEqualizer();
        }
    }

    private void configureOnlineSpectrum() {
        if (onlinePlayer == null) {
            return;
        }

        onlinePlayer.setAudioSpectrumInterval(0.04);
        onlinePlayer.setAudioSpectrumNumBands(64);
        onlinePlayer.setAudioSpectrumThreshold(-60);

        onlinePlayer.setAudioSpectrumListener((timestamp, duration, magnitudes, phases) -> {
            double[] spectrum = new double[magnitudes.length];
            int[] bars = new int[12];

            double sum = 0.0;
            for (int i = 0; i < magnitudes.length; i++) {
                double value = Math.max(0, magnitudes[i] + 60);
                spectrum[i] = value;
                sum += magnitudes[i];
            }

            int binsPerBar = Math.max(1, magnitudes.length / bars.length);
            for (int i = 0; i < bars.length; i++) {
                double total = 0.0;
                int start = i * binsPerBar;
                int end = Math.min(magnitudes.length, start + binsPerBar);
                for (int j = start; j < end; j++) {
                    total += Math.max(0, magnitudes[j] + 60);
                }
                bars[i] = (int) (total / binsPerBar);
            }

            visualizerCanvas.updateVisualizer(bars);
            spectrumCanvas.updateSpectrum(spectrum);
            waveformCanvas.updateWaveformFromMagnitudes(magnitudes);
            dbMeterCanvas.updateLevel(Math.max(-60.0, Math.min(0.0, sum / magnitudes.length)));
        });
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
            "• Buffer Size: 1024 frames\n" +
            "• Limiter: " + (limiterCheckbox.isSelected() ? "On" : "Off") + "\n" +
            "• Reverb: " + reverbPresetBox.getValue() + " (" + reverbValueLabel.getText() + ")\n\n" +
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
            "• Limiter: Prevent clipping when boosting\n" +
            "• Reverb/Width: Add space and stereo depth\n\n" +
            "VISUALIZERS:\n" +
            "• Waveform: Time-domain audio representation\n" +
            "• Spectrum: Frequency analysis in real-time\n" +
            "• Bars: Amplitude visualization\n" +
            "• dB Meter: Output level monitoring\n\n" +
            "THEMES:\n" +
            "Click colored circles in top bar to change theme\n" +
            "Select a Mood to animate EQ, effects, and visuals\n\n" +
            "ONLINE SEARCH:\n" +
            "Search iTunes previews (30s) and stream directly\n\n" +
            "PRESETS:\n" +
            "Save and load custom EQ settings per song"
        );
        alert.showAndWait();
    }
    
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private class OnlineTrackCell extends ListCell<OnlineTrack> {
        private final HBox content = new HBox(10);
        private final ImageView artwork = new ImageView();
        private final VBox textBox = new VBox(2);
        private final Label title = new Label();
        private final Label artist = new Label();
        private final Label album = new Label();

        OnlineTrackCell() {
            artwork.setFitWidth(42);
            artwork.setFitHeight(42);
            artwork.setSmooth(true);
            artwork.setPreserveRatio(true);

            title.setFont(Font.font("Consolas", FontWeight.SEMI_BOLD, 11));
            artist.setFont(Font.font("Consolas", FontWeight.NORMAL, 9));
            album.setFont(Font.font("Consolas", FontWeight.NORMAL, 8));

            textBox.getChildren().addAll(title, artist, album);
            content.setAlignment(Pos.CENTER_LEFT);
            content.getChildren().addAll(artwork, textBox);
        }

        @Override
        protected void updateItem(OnlineTrack item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                return;
            }

            title.setText(item.getTitle());
            title.setTextFill(Paint.valueOf(currentTheme.textPrimary));
            artist.setText(item.getArtist());
            artist.setTextFill(Paint.valueOf(currentTheme.textSecondary));
            album.setText(item.getAlbum());
            album.setTextFill(Paint.valueOf(currentTheme.textSecondary));

            if (item.getArtworkUrl() != null && !item.getArtworkUrl().isEmpty()) {
                artwork.setImage(new Image(item.getArtworkUrl(), 42, 42, true, true, true));
            } else {
                artwork.setImage(null);
            }

            setGraphic(content);
            setText(null);
        }
    }
    
    private void startUpdateThreads() {
        // dB meter updater
        Thread dbThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(50);
                    if (onlinePlayer != null) {
                        continue;
                    }
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
    ),
    RETRO_AMBER(
        "Retro Amber",
        "#1b1208", "#2b1d0e",
        "#ffb84d", "#fff5e6", "#d6b48c",
        "rgba(35, 22, 12, 0.6)", "rgba(45, 28, 14, 0.7)", "rgba(55, 35, 18, 0.5)",
        "#4CAF50", "#FFB300", "#E53935"
    ),
    DEEP_INDIGO(
        "Deep Indigo",
        "#0c0f24", "#1a1f3f",
        "#6c63ff", "#ffffff", "#aab0d6",
        "rgba(15, 20, 45, 0.6)", "rgba(20, 30, 60, 0.7)", "rgba(30, 40, 70, 0.5)",
        "#26C6DA", "#FFCA28", "#FF7043"
    ),
    CONCERT_RED(
        "Concert Red",
        "#1a0a0a", "#2d0f15",
        "#ff3b3b", "#ffffff", "#d9a1a1",
        "rgba(30, 10, 10, 0.6)", "rgba(45, 15, 20, 0.7)", "rgba(55, 20, 25, 0.5)",
        "#4CAF50", "#FFB300", "#E53935"
    ),
    NEON_MAGENTA(
        "Neon Magenta",
        "#120716", "#2a0f2e",
        "#ff4fd8", "#ffffff", "#e0a7d7",
        "rgba(25, 8, 30, 0.6)", "rgba(35, 15, 40, 0.7)", "rgba(45, 20, 50, 0.5)",
        "#00ff88", "#ffaa00", "#ff4466"
    ),
    SPACE_TEAL(
        "Space Teal",
        "#05131a", "#0a1f2e",
        "#00e6d0", "#e8ffff", "#9fd6d1",
        "rgba(8, 20, 28, 0.6)", "rgba(12, 28, 38, 0.7)", "rgba(18, 36, 50, 0.5)",
        "#26C6DA", "#FFCA28", "#FF7043"
    ),
    OCEAN_BLUE(
        "Ocean Blue",
        "#071522", "#0b2740",
        "#4db8ff", "#f2fbff", "#9cbad6",
        "rgba(10, 25, 38, 0.6)", "rgba(15, 32, 48, 0.7)", "rgba(20, 40, 60, 0.5)",
        "#4CAF50", "#FFB300", "#E53935"
    ),
    BASS_STORM(
        "Bass Storm",
        "#0a0b16", "#1a1b2a",
        "#7d5cff", "#ffffff", "#b2b2d6",
        "rgba(15, 15, 30, 0.6)", "rgba(20, 20, 40, 0.7)", "rgba(30, 30, 50, 0.5)",
        "#4CAF50", "#FF9800", "#F44336"
    ),
    CHILL_PLUM(
        "Chill Plum",
        "#140a1d", "#241233",
        "#c77dff", "#ffffff", "#c9b1d6",
        "rgba(25, 12, 35, 0.6)", "rgba(35, 18, 50, 0.7)", "rgba(45, 25, 60, 0.5)",
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

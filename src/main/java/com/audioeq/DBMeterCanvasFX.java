package com.audioeq;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * ENHANCED dB meter with theme support and professional styling
 */
public class DBMeterCanvasFX extends Canvas {
    
    private double currentdB = -60.0;
    private double smoothedDB = -60.0;
    private double peakDB = -60.0;
    private static final double MIN_DB = -60.0;
    private static final double MAX_DB = 0.0;
    private static final double SMOOTHING = 0.15;
    private static final double PEAK_DECAY = 0.998;
    
    private String accentColor = "#00d9ff";
    
    // Color thresholds
    private static final double GREEN_THRESHOLD = -20.0;
    private static final double YELLOW_THRESHOLD = -10.0;
    private static final double ORANGE_THRESHOLD = -5.0;
    
    public DBMeterCanvasFX() {
        // Size set by parent
    }
    
    /**
     * Sets accent color for theme support
     */
    public void setAccentColor(String color) {
        this.accentColor = color;
    }
    
    /**
     * Updates the dB level
     */
    public void updateLevel(double db) {
        this.currentdB = Math.max(MIN_DB, Math.min(MAX_DB, db));
        this.smoothedDB = smoothedDB * (1 - SMOOTHING) + currentdB * SMOOTHING;
        
        // Update peak hold
        if (currentdB > peakDB) {
            peakDB = currentdB;
        } else {
            peakDB = peakDB * PEAK_DECAY - 0.02;
            peakDB = Math.max(peakDB, MIN_DB);
        }
        
        draw();
    }
    
    /**
     * Draws the dB meter
     */
    private void draw() {
        GraphicsContext gc = getGraphicsContext2D();
        double width = getWidth();
        double height = getHeight();
        
        gc.clearRect(0, 0, width, height);
        
        double meterWidth = width - 30;
        double meterHeight = height - 50;
        double meterX = 15;
        double meterY = 25;
        
        // Draw background
        gc.setFill(Color.web("#000000", 0.4));
        gc.fillRoundRect(meterX, meterY, meterWidth, meterHeight, 12, 12);
        
        // Draw inner glow border
        gc.setEffect(new GaussianBlur(3));
        gc.setStroke(Color.web(accentColor, 0.4));
        gc.setLineWidth(2);
        gc.strokeRoundRect(meterX + 1, meterY + 1, meterWidth - 2, meterHeight - 2, 12, 12);
        gc.setEffect(null);
        
        // Calculate fill height
        double normalizedLevel = (smoothedDB - MIN_DB) / (MAX_DB - MIN_DB);
        double fillHeight = meterHeight * normalizedLevel;
        double fillY = meterY + meterHeight - fillHeight;
        
        // Create multi-color gradient
        LinearGradient gradient = createMeterGradient(fillY, meterY + meterHeight);
        
        // Draw filled portion with outer glow
        gc.setEffect(new GaussianBlur(15));
        gc.setFill(gradient);
        gc.fillRoundRect(meterX - 3, fillY - 3, meterWidth + 6, fillHeight + 6, 12, 12);
        
        gc.setEffect(null);
        gc.setFill(gradient);
        gc.fillRoundRect(meterX, fillY, meterWidth, fillHeight, 10, 10);
        
        // Draw peak indicator line
        if (peakDB > MIN_DB) {
            double peakNormalized = (peakDB - MIN_DB) / (MAX_DB - MIN_DB);
            double peakY = meterY + meterHeight - (meterHeight * peakNormalized);
            
            gc.setEffect(new GaussianBlur(4));
            gc.setStroke(Color.web("#ffffff", 0.9));
            gc.setLineWidth(3);
            gc.strokeLine(meterX, peakY, meterX + meterWidth, peakY);
            
            gc.setEffect(null);
            gc.setStroke(getPeakColor());
            gc.setLineWidth(2);
            gc.strokeLine(meterX, peakY, meterX + meterWidth, peakY);
        }
        
        // Draw scale markers
        gc.setFont(Font.font("Consolas", FontWeight.NORMAL, 9));
        gc.setTextAlign(TextAlignment.RIGHT);
        drawMarker(gc, meterX, meterY, meterWidth, meterHeight, 0, "0");
        drawMarker(gc, meterX, meterY, meterWidth, meterHeight, -10, "-10");
        drawMarker(gc, meterX, meterY, meterWidth, meterHeight, -20, "-20");
        drawMarker(gc, meterX, meterY, meterWidth, meterHeight, -30, "-30");
        drawMarker(gc, meterX, meterY, meterWidth, meterHeight, -40, "-40");
        drawMarker(gc, meterX, meterY, meterWidth, meterHeight, -60, "-60");
        
        // Draw outer border
        gc.setStroke(Color.web(accentColor, 0.6));
        gc.setLineWidth(2);
        gc.strokeRoundRect(meterX, meterY, meterWidth, meterHeight, 12, 12);
        
        // Draw current value display
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
        gc.setFill(getCurrentLevelColor());
        gc.setTextAlign(TextAlignment.CENTER);
        String dbText = String.format("%.1f dB", smoothedDB);
        gc.fillText(dbText, width / 2, height - 10);
        
        // Draw peak value
        gc.setFont(Font.font("Consolas", FontWeight.NORMAL, 9));
        gc.setFill(Color.web("#ffffff", 0.6));
        String peakText = String.format("PEAK: %.1f", peakDB);
        gc.fillText(peakText, width / 2, height - 25);
    }
    
    /**
     * Creates the meter gradient
     */
    private LinearGradient createMeterGradient(double startY, double endY) {
        return new LinearGradient(
            0, startY, 0, endY,
            false, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#ff0000")),
            new Stop(0.15, Color.web("#ff4400")),
            new Stop(0.3, Color.web("#ff8800")),
            new Stop(0.45, Color.web("#ffcc00")),
            new Stop(0.6, Color.web("#ffff00")),
            new Stop(0.75, Color.web("#88ff00")),
            new Stop(1, Color.web("#00ff00"))
        );
    }
    
    /**
     * Gets color for current level display
     */
    private Color getCurrentLevelColor() {
        if (smoothedDB > ORANGE_THRESHOLD) {
            return Color.web("#ff0000");
        } else if (smoothedDB > YELLOW_THRESHOLD) {
            return Color.web("#ff8800");
        } else if (smoothedDB > GREEN_THRESHOLD) {
            return Color.web("#ffff00");
        } else {
            return Color.web("#00ff00");
        }
    }
    
    /**
     * Gets color for peak indicator
     */
    private Color getPeakColor() {
        if (peakDB > ORANGE_THRESHOLD) {
            return Color.web("#ff0000");
        } else if (peakDB > YELLOW_THRESHOLD) {
            return Color.web("#ffaa00");
        } else {
            return Color.web("#ffffff");
        }
    }
    
    /**
     * Draws a scale marker
     */
    private void drawMarker(GraphicsContext gc, double meterX, double meterY, 
                           double meterWidth, double meterHeight, double dbLevel, String label) {
        double normalizedPos = (dbLevel - MIN_DB) / (MAX_DB - MIN_DB);
        double markerY = meterY + meterHeight - (meterHeight * normalizedPos);
        
        // Tick mark
        gc.setStroke(Color.web("#ffffff", 0.5));
        gc.setLineWidth(1);
        gc.strokeLine(meterX - 4, markerY, meterX - 1, markerY);
        
        // Label
        gc.setFill(Color.web("#ffffff", 0.6));
        gc.fillText(label, meterX - 6, markerY + 3);
    }
}
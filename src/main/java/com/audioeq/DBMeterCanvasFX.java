package com.audioeq;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.scene.effect.GaussianBlur;

/**
 * Modern JavaFX-based dB meter with smooth gradients and professional styling.
 */
public class DBMeterCanvasFX extends Canvas {
    
    private double currentdB = -60.0;
    private double smoothedDB = -60.0;
    private static final double MIN_DB = -60.0;
    private static final double MAX_DB = 0.0;
    private static final double SMOOTHING = 0.2;
    
    // Color thresholds
    private static final double GREEN_THRESHOLD = -20.0;
    private static final double YELLOW_THRESHOLD = -10.0;
    private static final double ORANGE_THRESHOLD = -5.0;
    
    /**
     * Constructs a new DBMeterCanvasFX.
     */
    public DBMeterCanvasFX() {
        // Canvas size set by parent
    }
    
    /**
     * Updates the dB level and redraws the meter.
     *
     * @param db The new dB level (-60 to 0)
     */
    public void updateLevel(double db) {
        this.currentdB = Math.max(MIN_DB, Math.min(MAX_DB, db));
        // Smooth the dB display
        this.smoothedDB = smoothedDB * (1 - SMOOTHING) + currentdB * SMOOTHING;
        draw();
    }
    
    /**
     * Draws the dB meter with modern styling.
     */
    private void draw() {
        GraphicsContext gc = getGraphicsContext2D();
        double width = getWidth();
        double height = getHeight();
        
        // Clear canvas
        gc.clearRect(0, 0, width, height);
        
        double meterWidth = width - 30;
        double meterHeight = height - 40;
        double meterX = 15;
        double meterY = 20;
        
        // Draw background with inner shadow
        gc.setFill(Color.web("#0a0a0a"));
        gc.fillRoundRect(meterX, meterY, meterWidth, meterHeight, 10, 10);
        
        // Draw inner glow
        gc.setEffect(new GaussianBlur(3));
        gc.setStroke(Color.web("#00d9ff", 0.3));
        gc.setLineWidth(2);
        gc.strokeRoundRect(meterX + 1, meterY + 1, meterWidth - 2, meterHeight - 2, 10, 10);
        gc.setEffect(null);
        
        // Calculate fill height
        double normalizedLevel = (smoothedDB - MIN_DB) / (MAX_DB - MIN_DB);
        double fillHeight = meterHeight * normalizedLevel;
        double fillY = meterY + meterHeight - fillHeight;
        
        // Create gradient based on level zones
        LinearGradient gradient = createMeterGradient(fillY, meterY + meterHeight);
        
        // Draw filled portion with glow
        gc.setEffect(new GaussianBlur(10));
        gc.setFill(gradient);
        gc.fillRoundRect(meterX - 2, fillY - 2, meterWidth + 4, fillHeight + 4, 10, 10);
        
        gc.setEffect(null);
        gc.setFill(gradient);
        gc.fillRoundRect(meterX, fillY, meterWidth, fillHeight, 8, 8);
        
        // Draw level markers
        gc.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 9));
        gc.setTextAlign(TextAlignment.RIGHT);
        drawMarker(gc, meterX, meterY, meterWidth, meterHeight, 0, "0");
        drawMarker(gc, meterX, meterY, meterWidth, meterHeight, -10, "-10");
        drawMarker(gc, meterX, meterY, meterWidth, meterHeight, -20, "-20");
        drawMarker(gc, meterX, meterY, meterWidth, meterHeight, -30, "-30");
        drawMarker(gc, meterX, meterY, meterWidth, meterHeight, -40, "-40");
        drawMarker(gc, meterX, meterY, meterWidth, meterHeight, -60, "-60");
        
        // Draw border
        gc.setStroke(Color.web("#00d9ff", 0.5));
        gc.setLineWidth(2);
        gc.strokeRoundRect(meterX, meterY, meterWidth, meterHeight, 10, 10);
        
        // Draw current dB value at bottom
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        gc.setFill(getCurrentLevelColor());
        gc.setTextAlign(TextAlignment.CENTER);
        String dbText = String.format("%.1f dB", smoothedDB);
        gc.fillText(dbText, width / 2, height - 5);
    }
    
    /**
     * Creates a gradient for the meter based on dB zones.
     */
    private LinearGradient createMeterGradient(double startY, double endY) {
        return new LinearGradient(
            0, startY, 0, endY,
            false, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#ff0000")),      // Red at top (0 dB)
            new Stop(0.2, Color.web("#ff8800")),    // Orange
            new Stop(0.4, Color.web("#ffff00")),    // Yellow
            new Stop(0.6, Color.web("#88ff00")),    // Yellow-green
            new Stop(1, Color.web("#00ff00"))       // Green at bottom (-60 dB)
        );
    }
    
    /**
     * Gets the color for the current dB level display.
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
     * Draws a level marker on the meter.
     */
    private void drawMarker(GraphicsContext gc, double meterX, double meterY, 
                           double meterWidth, double meterHeight, double dbLevel, String label) {
        double normalizedPos = (dbLevel - MIN_DB) / (MAX_DB - MIN_DB);
        double markerY = meterY + meterHeight - (meterHeight * normalizedPos);
        
        // Draw tick mark
        gc.setStroke(Color.web("#8892b0", 0.6));
        gc.setLineWidth(1);
        gc.strokeLine(meterX - 3, markerY, meterX - 1, markerY);
        
        // Draw label
        gc.setFill(Color.web("#8892b0"));
        gc.fillText(label, meterX - 5, markerY + 3);
    }
}

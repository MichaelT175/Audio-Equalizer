package com.audioeq;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.effect.GaussianBlur;
import java.util.Arrays;

/**
 * Modern JavaFX-based visualizer with smooth bar animations and gradients.
 * Features include rounded bars, glow effects, and smooth interpolation.
 */
public class VisualizerCanvasFX extends Canvas {
    
    private int[] barHeights;
    private double[] smoothedHeights; // For smooth animation
    private final double SMOOTHING_FACTOR = 0.3; // Lower = smoother
    private int numBars;
    
    /**
     * Constructs a VisualizerCanvasFX with a specified number of bars.
     *
     * @param numBars The number of bars to display.
     */
    public VisualizerCanvasFX(int numBars) {
        this.numBars = numBars;
        this.barHeights = new int[numBars];
        this.smoothedHeights = new double[numBars];
        
        // Initialize with zeros
        Arrays.fill(barHeights, 0);
        Arrays.fill(smoothedHeights, 0.0);
    }
    
    /**
     * Updates the visualizer with new heights for the bars.
     *
     * @param newHeights An array of new heights for the bars.
     */
    public void updateVisualizer(int[] newHeights) {
        if (newHeights.length != numBars) {
            return;
        }
        
        // Smooth the transition
        for (int i = 0; i < numBars; i++) {
            smoothedHeights[i] = smoothedHeights[i] * (1 - SMOOTHING_FACTOR) + 
                                newHeights[i] * SMOOTHING_FACTOR;
            barHeights[i] = (int) smoothedHeights[i];
        }
        
        draw();
    }
    
    /**
     * Draws the visualizer bars with modern styling.
     */
    private void draw() {
        GraphicsContext gc = getGraphicsContext2D();
        double width = getWidth();
        double height = getHeight();
        
        // Clear canvas
        gc.clearRect(0, 0, width, height);
        
        // Calculate bar dimensions
        double barWidth = (width / numBars) * 0.8; // 80% width, 20% gap
        double gap = (width / numBars) * 0.2;
        double maxBarHeight = height - 20;
        
        for (int i = 0; i < numBars; i++) {
            double barHeight = Math.min(barHeights[i], maxBarHeight);
            double x = i * (barWidth + gap) + gap / 2;
            double y = height - barHeight - 10;
            
            // Calculate color based on height (gradient from green to red)
            Color barColor = getColorForHeight(barHeight, maxBarHeight);
            
            // Draw glow effect
            gc.setEffect(new GaussianBlur(10));
            gc.setFill(barColor.deriveColor(0, 1.0, 1.0, 0.3));
            gc.fillRoundRect(x - 2, y - 2, barWidth + 4, barHeight + 4, 8, 8);
            
            // Draw main bar with gradient
            gc.setEffect(null);
            LinearGradient gradient = new LinearGradient(
                0, y, 0, y + barHeight,
                false, CycleMethod.NO_CYCLE,
                new Stop(0, barColor),
                new Stop(1, barColor.darker())
            );
            gc.setFill(gradient);
            gc.fillRoundRect(x, y, barWidth, barHeight, 6, 6);
            
            // Draw highlight on top
            gc.setFill(barColor.brighter());
            gc.fillRoundRect(x, y, barWidth, Math.min(6, barHeight), 6, 6);
            
            // Draw reflection at bottom
            if (barHeight > 20) {
                LinearGradient reflection = new LinearGradient(
                    0, height - 5, 0, height,
                    false, CycleMethod.NO_CYCLE,
                    new Stop(0, barColor.deriveColor(0, 1.0, 1.0, 0.2)),
                    new Stop(1, Color.TRANSPARENT)
                );
                gc.setFill(reflection);
                gc.fillRoundRect(x, height - 5, barWidth, 5, 6, 6);
            }
        }
    }
    
    /**
     * Calculates the color of a bar based on its height.
     * Uses a smooth gradient from cyan to green to yellow to orange to red.
     *
     * @param height The height of the bar.
     * @param maxHeight The maximum possible height.
     * @return The color for the bar.
     */
    private Color getColorForHeight(double height, double maxHeight) {
        double ratio = height / maxHeight;
        
        if (ratio < 0.2) {
            // Cyan to blue
            return Color.web("#00d9ff").interpolate(Color.web("#0099ff"), ratio * 5);
        } else if (ratio < 0.4) {
            // Blue to green
            return Color.web("#0099ff").interpolate(Color.web("#00ff88"), (ratio - 0.2) * 5);
        } else if (ratio < 0.6) {
            // Green to yellow
            return Color.web("#00ff88").interpolate(Color.web("#ffff00"), (ratio - 0.4) * 5);
        } else if (ratio < 0.8) {
            // Yellow to orange
            return Color.web("#ffff00").interpolate(Color.web("#ff8800"), (ratio - 0.6) * 5);
        } else {
            // Orange to red
            return Color.web("#ff8800").interpolate(Color.web("#ff0000"), (ratio - 0.8) * 5);
        }
    }
}

package com.audioeq;

import java.util.Arrays;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;

/**
 * ENHANCED Visualizer with theme-aware colors and improved effects
 * Features: Smooth animations, reflection effects, dynamic gradients
 */
public class VisualizerCanvasFX extends Canvas {
    
    private int[] barHeights;
    private double[] smoothedHeights;
    private double[] peakHeights;
    private final double SMOOTHING_FACTOR = 0.25;
    private final double PEAK_DECAY = 0.95;
    private int numBars;
    private String accentColor = "#00d9ff";
    
    public VisualizerCanvasFX(int numBars) {
        this.numBars = numBars;
        this.barHeights = new int[numBars];
        this.smoothedHeights = new double[numBars];
        this.peakHeights = new double[numBars];
        
        Arrays.fill(barHeights, 0);
        Arrays.fill(smoothedHeights, 0.0);
        Arrays.fill(peakHeights, 0.0);
    }
    
    /**
     * Sets the accent color for theme support
     */
    public void setAccentColor(String color) {
        this.accentColor = color;
    }
    
    /**
     * Updates the visualizer with new bar heights
     */
    public void updateVisualizer(int[] newHeights) {
        if (newHeights.length != numBars) {
            return;
        }
        
        for (int i = 0; i < numBars; i++) {
            // Smooth the transition
            smoothedHeights[i] = smoothedHeights[i] * (1 - SMOOTHING_FACTOR) + 
                                newHeights[i] * SMOOTHING_FACTOR;
            barHeights[i] = (int) smoothedHeights[i];
            
            // Update peaks
            if (smoothedHeights[i] > peakHeights[i]) {
                peakHeights[i] = smoothedHeights[i];
            } else {
                peakHeights[i] *= PEAK_DECAY;
            }
        }
        
        draw();
    }
    
    /**
     * Draws the visualizer with enhanced effects
     */
    private void draw() {
        GraphicsContext gc = getGraphicsContext2D();
        double width = getWidth();
        double height = getHeight();
        
        gc.clearRect(0, 0, width, height);
        
        double barWidth = (width / numBars) * 0.75;
        double gap = (width / numBars) * 0.25;
        double maxBarHeight = height - 30;
        
        for (int i = 0; i < numBars; i++) {
            double barHeight = Math.min(barHeights[i], maxBarHeight);
            double x = i * (barWidth + gap) + gap / 2;
            double y = height - barHeight - 15;
            
            // Get color based on height
            Color barColor = getColorForHeight(barHeight, maxBarHeight);
            
            // Draw glow effect
            gc.setEffect(new GaussianBlur(12));
            gc.setFill(barColor.deriveColor(0, 1.0, 1.0, 0.4));
            gc.fillRoundRect(x - 3, y - 3, barWidth + 6, barHeight + 6, 10, 10);
            
            // Draw main bar with gradient
            gc.setEffect(null);
            LinearGradient gradient = new LinearGradient(
                0, y, 0, y + barHeight,
                false, CycleMethod.NO_CYCLE,
                new Stop(0, barColor.brighter().brighter()),
                new Stop(0.3, barColor.brighter()),
                new Stop(0.7, barColor),
                new Stop(1, barColor.darker())
            );
            gc.setFill(gradient);
            gc.fillRoundRect(x, y, barWidth, barHeight, 8, 8);
            
            // Draw highlight shimmer on top
            if (barHeight > 20) {
                LinearGradient highlight = new LinearGradient(
                    0, y, 0, y + 15,
                    false, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#ffffff", 0.6)),
                    new Stop(1, Color.TRANSPARENT)
                );
                gc.setFill(highlight);
                gc.fillRoundRect(x, y, barWidth, Math.min(15, barHeight), 8, 8);
            }
            
            // Draw peak indicator
            if (peakHeights[i] > 5) {
                double peakY = height - peakHeights[i] - 15;
                gc.setFill(Color.web("#ffffff", 0.9));
                gc.fillRoundRect(x, peakY - 2, barWidth, 3, 2, 2);
                
                gc.setEffect(new GaussianBlur(4));
                gc.setFill(barColor.brighter());
                gc.fillRoundRect(x, peakY - 2, barWidth, 3, 2, 2);
                gc.setEffect(null);
            }
            
            // Draw floor reflection
            if (barHeight > 30) {
                LinearGradient reflection = new LinearGradient(
                    0, height - 10, 0, height,
                    false, CycleMethod.NO_CYCLE,
                    new Stop(0, barColor.deriveColor(0, 1.0, 1.0, 0.15)),
                    new Stop(1, Color.TRANSPARENT)
                );
                gc.setFill(reflection);
                gc.fillRoundRect(x, height - 10, barWidth, 10, 8, 8);
            }
        }
    }
    
    /**
     * Gets dynamic color based on bar height and current theme
     */
    private Color getColorForHeight(double height, double maxHeight) {
        double ratio = height / maxHeight;
        Color accent = Color.web(accentColor);
        
        if (ratio < 0.2) {
            return accent.deriveColor(0, 0.8, 1.2, 1.0);
        } else if (ratio < 0.4) {
            return accent;
        } else if (ratio < 0.6) {
            return accent.interpolate(Color.web("#ffff00"), 0.3);
        } else if (ratio < 0.8) {
            return accent.interpolate(Color.web("#ff8800"), 0.5);
        } else {
            return accent.interpolate(Color.web("#ff0000"), 0.7);
        }
    }
}
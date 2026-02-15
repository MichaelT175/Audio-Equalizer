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
 * Modern JavaFX-based spectrum analyzer with smooth curves and gradient effects.
 * Features anti-aliased lines, glow effects, and smooth frequency visualization.
 */
public class SpectrumCanvasFX extends Canvas {
    
    private double[] spectrumData;
    private double[] smoothedData; // For smooth animation
    private final double SMOOTHING_FACTOR = 0.25;
    private int numBins;
    
    /**
     * Constructs a SpectrumCanvasFX with the specified number of frequency bins.
     *
     * @param numBins The number of bins (frequencies) in the spectrum.
     */
    public SpectrumCanvasFX(int numBins) {
        this.numBins = numBins;
        this.spectrumData = new double[numBins];
        this.smoothedData = new double[numBins];
        Arrays.fill(spectrumData, 0.0);
        Arrays.fill(smoothedData, 0.0);
    }
    
    /**
     * Updates the spectrum data with new values.
     *
     * @param newSpectrumData The new spectrum data to be visualized.
     */
    public void updateSpectrum(double[] newSpectrumData) {
        if (newSpectrumData == null || newSpectrumData.length == 0) {
            return;
        }
        
        // Copy and smooth the data
        int minLen = Math.min(numBins, newSpectrumData.length);
        for (int i = 0; i < minLen; i++) {
            smoothedData[i] = smoothedData[i] * (1 - SMOOTHING_FACTOR) + 
                             newSpectrumData[i] * SMOOTHING_FACTOR;
        }
        
        this.spectrumData = Arrays.copyOf(smoothedData, smoothedData.length);
        draw();
    }
    
    /**
     * Draws the spectrum analyzer with modern styling.
     */
    private void draw() {
        GraphicsContext gc = getGraphicsContext2D();
        double width = getWidth();
        double height = getHeight();
        
        // Clear canvas
        gc.clearRect(0, 0, width, height);
        
        if (spectrumData == null || spectrumData.length == 0) {
            return;
        }
        
        // Find max magnitude for scaling
        double maxMagnitude = Arrays.stream(spectrumData).max().orElse(1.0);
        if (maxMagnitude < 0.001) maxMagnitude = 1.0;
        
        double scaleY = (height - 40) / maxMagnitude;
        double binWidth = width / (double) spectrumData.length;
        
        // Draw grid lines
        drawGrid(gc, width, height);
        
        // Draw filled area under the curve (gradient)
        gc.setEffect(new GaussianBlur(3));
        LinearGradient fillGradient = new LinearGradient(
            0, 0, 0, height,
            false, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#00d9ff", 0.4)),
            new Stop(0.5, Color.web("#0099ff", 0.2)),
            new Stop(1, Color.TRANSPARENT)
        );
        gc.setFill(fillGradient);
        
        gc.beginPath();
        gc.moveTo(0, height);
        
        for (int i = 0; i < spectrumData.length; i++) {
            double x = i * binWidth;
            double y = height - 20 - (spectrumData[i] * scaleY);
            
            if (i == 0) {
                gc.lineTo(x, y);
            } else {
                // Use quadratic curves for smoothness
                double prevX = (i - 1) * binWidth;
                double prevY = height - 20 - (spectrumData[i - 1] * scaleY);
                double ctrlX = (prevX + x) / 2;
                double ctrlY = (prevY + y) / 2;
                gc.quadraticCurveTo(ctrlX, ctrlY, x, y);
            }
        }
        
        gc.lineTo(width, height);
        gc.lineTo(0, height);
        gc.closePath();
        gc.fill();
        
        // Draw main spectrum line with glow
        gc.setEffect(new GaussianBlur(8));
        gc.setStroke(Color.web("#00d9ff", 0.6));
        gc.setLineWidth(3);
        gc.beginPath();
        
        for (int i = 0; i < spectrumData.length; i++) {
            double x = i * binWidth;
            double y = height - 20 - (spectrumData[i] * scaleY);
            
            if (i == 0) {
                gc.moveTo(x, y);
            } else {
                double prevX = (i - 1) * binWidth;
                double prevY = height - 20 - (spectrumData[i - 1] * scaleY);
                double ctrlX = (prevX + x) / 2;
                double ctrlY = (prevY + y) / 2;
                gc.quadraticCurveTo(ctrlX, ctrlY, x, y);
            }
        }
        gc.stroke();
        
        // Draw sharp line on top
        gc.setEffect(null);
        gc.setStroke(Color.web("#00ffff"));
        gc.setLineWidth(2);
        gc.beginPath();
        
        for (int i = 0; i < spectrumData.length; i++) {
            double x = i * binWidth;
            double y = height - 20 - (spectrumData[i] * scaleY);
            
            if (i == 0) {
                gc.moveTo(x, y);
            } else {
                double prevX = (i - 1) * binWidth;
                double prevY = height - 20 - (spectrumData[i - 1] * scaleY);
                double ctrlX = (prevX + x) / 2;
                double ctrlY = (prevY + y) / 2;
                gc.quadraticCurveTo(ctrlX, ctrlY, x, y);
            }
        }
        gc.stroke();
        
        // Draw peaks
        drawPeaks(gc, binWidth, height, scaleY);
    }
    
    /**
     * Draws subtle grid lines for reference.
     */
    private void drawGrid(GraphicsContext gc, double width, double height) {
        gc.setStroke(Color.web("#ffffff", 0.05));
        gc.setLineWidth(1);
        
        // Horizontal lines
        for (int i = 1; i < 5; i++) {
            double y = (height / 5.0) * i;
            gc.strokeLine(0, y, width, y);
        }
        
        // Vertical lines
        for (int i = 1; i < 10; i++) {
            double x = (width / 10.0) * i;
            gc.strokeLine(x, 0, x, height);
        }
    }
    
    /**
     * Draws peak indicators for prominent frequencies.
     */
    private void drawPeaks(GraphicsContext gc, double binWidth, double height, double scaleY) {
        // Find peaks
        for (int i = 1; i < spectrumData.length - 1; i++) {
            if (spectrumData[i] > spectrumData[i - 1] && 
                spectrumData[i] > spectrumData[i + 1] &&
                spectrumData[i] > 50) { // Threshold for peak detection
                
                double x = i * binWidth;
                double y = height - 20 - (spectrumData[i] * scaleY);
                
                // Draw peak dot with glow
                gc.setEffect(new GaussianBlur(5));
                gc.setFill(Color.web("#ffff00", 0.6));
                gc.fillOval(x - 4, y - 4, 8, 8);
                
                gc.setEffect(null);
                gc.setFill(Color.web("#ffffff"));
                gc.fillOval(x - 2, y - 2, 4, 4);
            }
        }
    }
}

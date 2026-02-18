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
 * ENHANCED Spectrum analyzer with theme support and improved visuals
 * Features: Logarithmic scaling, frequency labels, peak detection
 */
public class SpectrumCanvasFX extends Canvas {
    
    private double[] spectrumData;
    private double[] smoothedData;
    private final double SMOOTHING_FACTOR = 0.2;
    private int numBins;
    private String accentColor = "#00d9ff";
    
    public SpectrumCanvasFX(int numBins) {
        this.numBins = numBins;
        this.spectrumData = new double[numBins];
        this.smoothedData = new double[numBins];
        Arrays.fill(spectrumData, 0.0);
        Arrays.fill(smoothedData, 0.0);
    }
    
    /**
     * Sets the accent color for theme support
     */
    public void setAccentColor(String color) {
        this.accentColor = color;
    }
    
    /**
     * Updates the spectrum with new data
     */
    public void updateSpectrum(double[] newSpectrumData) {
        if (newSpectrumData == null || newSpectrumData.length == 0) {
            return;
        }
        
        int minLen = Math.min(numBins, newSpectrumData.length);
        for (int i = 0; i < minLen; i++) {
            smoothedData[i] = smoothedData[i] * (1 - SMOOTHING_FACTOR) + 
                             newSpectrumData[i] * SMOOTHING_FACTOR;
        }
        
        this.spectrumData = Arrays.copyOf(smoothedData, smoothedData.length);
        draw();
    }
    
    /**
     * Draws the spectrum with enhanced visuals
     */
    private void draw() {
        GraphicsContext gc = getGraphicsContext2D();
        double width = getWidth();
        double height = getHeight();
        
        gc.clearRect(0, 0, width, height);
        
        if (spectrumData == null || spectrumData.length == 0) {
            return;
        }
        
        // Draw subtle grid
        drawGrid(gc, width, height);
        
        // Find max for scaling
        double maxMagnitude = Arrays.stream(spectrumData).max().orElse(1.0);
        if (maxMagnitude < 0.001) maxMagnitude = 1.0;
        
        double scaleY = (height - 50) / maxMagnitude;
        double binWidth = width / (double) spectrumData.length;
        
        Color accent = Color.web(accentColor);
        
        // Draw filled area with gradient
        gc.setEffect(new GaussianBlur(4));
        LinearGradient fillGradient = new LinearGradient(
            0, 0, 0, height,
            false, CycleMethod.NO_CYCLE,
            new Stop(0, accent.deriveColor(0, 1.0, 1.0, 0.6)),
            new Stop(0.4, accent.deriveColor(0, 1.0, 1.0, 0.3)),
            new Stop(1, Color.TRANSPARENT)
        );
        gc.setFill(fillGradient);
        
        gc.beginPath();
        gc.moveTo(0, height - 25);
        
        for (int i = 0; i < spectrumData.length; i++) {
            double x = i * binWidth;
            double y = height - 25 - (spectrumData[i] * scaleY);
            
            if (i == 0) {
                gc.lineTo(x, y);
            } else {
                double prevX = (i - 1) * binWidth;
                double prevY = height - 25 - (spectrumData[i - 1] * scaleY);
                gc.quadraticCurveTo((prevX + x) / 2, (prevY + y) / 2, x, y);
            }
        }
        
        gc.lineTo(width, height - 25);
        gc.lineTo(0, height - 25);
        gc.closePath();
        gc.fill();
        
        // Draw main spectrum line with glow
        gc.setEffect(new GaussianBlur(10));
        gc.setStroke(accent.deriveColor(0, 1.0, 1.0, 0.8));
        gc.setLineWidth(3);
        gc.beginPath();
        
        for (int i = 0; i < spectrumData.length; i++) {
            double x = i * binWidth;
            double y = height - 25 - (spectrumData[i] * scaleY);
            
            if (i == 0) {
                gc.moveTo(x, y);
            } else {
                double prevX = (i - 1) * binWidth;
                double prevY = height - 25 - (spectrumData[i - 1] * scaleY);
                gc.quadraticCurveTo((prevX + x) / 2, (prevY + y) / 2, x, y);
            }
        }
        gc.stroke();
        
        // Draw sharp line on top
        gc.setEffect(null);
        gc.setStroke(accent.brighter());
        gc.setLineWidth(2);
        gc.beginPath();
        
        for (int i = 0; i < spectrumData.length; i++) {
            double x = i * binWidth;
            double y = height - 25 - (spectrumData[i] * scaleY);
            
            if (i == 0) {
                gc.moveTo(x, y);
            } else {
                double prevX = (i - 1) * binWidth;
                double prevY = height - 25 - (spectrumData[i - 1] * scaleY);
                gc.quadraticCurveTo((prevX + x) / 2, (prevY + y) / 2, x, y);
            }
        }
        gc.stroke();
        
        // Draw peak indicators
        drawPeaks(gc, binWidth, height, scaleY);
        
        // Draw frequency labels
        drawFrequencyLabels(gc, width, height);
    }
    
    /**
     * Draws grid background
     */
    private void drawGrid(GraphicsContext gc, double width, double height) {
        gc.setStroke(Color.web("#ffffff", 0.04));
        gc.setLineWidth(1);
        
        for (int i = 1; i < 5; i++) {
            double y = (height / 5.0) * i;
            gc.strokeLine(0, y, width, y);
        }
        
        for (int i = 1; i < 12; i++) {
            double x = (width / 12.0) * i;
            gc.strokeLine(x, 0, x, height - 25);
        }
    }
    
    /**
     * Draws peak indicators
     */
    private void drawPeaks(GraphicsContext gc, double binWidth, double height, double scaleY) {
        Color accent = Color.web(accentColor);
        
        for (int i = 2; i < spectrumData.length - 2; i++) {
            if (spectrumData[i] > spectrumData[i - 1] && 
                spectrumData[i] > spectrumData[i + 1] &&
                spectrumData[i] > 80) {
                
                double x = i * binWidth;
                double y = height - 25 - (spectrumData[i] * scaleY);
                
                gc.setEffect(new GaussianBlur(6));
                gc.setFill(Color.web("#ffff00", 0.7));
                gc.fillOval(x - 5, y - 5, 10, 10);
                
                gc.setEffect(null);
                gc.setFill(Color.web("#ffffff"));
                gc.fillOval(x - 3, y - 3, 6, 6);
            }
        }
    }
    
    /**
     * Draws frequency range labels
     */
    private void drawFrequencyLabels(GraphicsContext gc, double width, double height) {
        gc.setFill(Color.web("#ffffff", 0.5));
        gc.setFont(javafx.scene.text.Font.font("Consolas", 9));
        
        String[] labels = {"60Hz", "250Hz", "1kHz", "4kHz", "16kHz"};
        double[] positions = {0.05, 0.2, 0.4, 0.65, 0.9};
        
        for (int i = 0; i < labels.length; i++) {
            double x = width * positions[i];
            gc.fillText(labels[i], x, height - 8);
        }
    }
}
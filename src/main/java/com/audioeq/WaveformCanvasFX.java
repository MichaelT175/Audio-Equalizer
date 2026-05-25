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
 * Waveform display showing audio amplitude over time
 * Features smooth lines, gradient fills, and stereo channel visualization
 */
public class WaveformCanvasFX extends Canvas {
    
    private double[] audioSamples;
    private double[] smoothedSamples;
    private final int SAMPLE_BUFFER_SIZE = 4000;
    private final double SMOOTHING_FACTOR = 0.3;
    private String accentColor = "#00d9ff";
    
    public WaveformCanvasFX() {
        this.audioSamples = new double[SAMPLE_BUFFER_SIZE];
        this.smoothedSamples = new double[SAMPLE_BUFFER_SIZE];
        Arrays.fill(audioSamples, 0.0);
        Arrays.fill(smoothedSamples, 0.0);
    }

    public void setAccentColor(String color) {
        this.accentColor = color;
    }
    
    /**
     * Updates the waveform with new audio data
     */
    public void updateWaveform(byte[] buffer, int sampleSizeInBytes) {
        if (buffer == null || buffer.length == 0) {
            return;
        }
        
        // Convert bytes to samples
        int numSamples = buffer.length / sampleSizeInBytes;
        double[] newSamples = new double[numSamples];
        
        for (int i = 0; i < numSamples && i * sampleSizeInBytes < buffer.length - 1; i++) {
            int sampleIndex = i * sampleSizeInBytes;
            int sample = 0;
            
            if (sampleSizeInBytes == 2) {
                // 16-bit audio
                sample = (buffer[sampleIndex + 1] << 8) | (buffer[sampleIndex] & 0xFF);
            } else if (sampleSizeInBytes == 1) {
                // 8-bit audio
                sample = buffer[sampleIndex];
            }
            
            newSamples[i] = sample / 32768.0;
        }
        
        // SHIFT existing samples to the left (scroll effect)
        int shift = newSamples.length;
        System.arraycopy(audioSamples, shift, audioSamples, 0, SAMPLE_BUFFER_SIZE - shift);
        
        // Add new samples at the end
        for (int i = 0; i < Math.min(newSamples.length, SAMPLE_BUFFER_SIZE); i++) {
            int targetIndex = SAMPLE_BUFFER_SIZE - newSamples.length + i;
            if (targetIndex >= 0 && targetIndex < SAMPLE_BUFFER_SIZE) {
                smoothedSamples[targetIndex] = smoothedSamples[targetIndex] * (1 - SMOOTHING_FACTOR) + 
                                               newSamples[i] * SMOOTHING_FACTOR;
                audioSamples[targetIndex] = smoothedSamples[targetIndex];
            }
        }
        
        draw();
    }

    public void updateWaveformFromMagnitudes(double[] magnitudes) {
        if (magnitudes == null || magnitudes.length == 0) {
            return;
        }

        int chunk = Math.min(magnitudes.length, 240);
        double[] newSamples = new double[chunk];

        for (int i = 0; i < chunk; i++) {
            double norm = (magnitudes[i] + 60.0) / 60.0;
            norm = Math.max(0.0, Math.min(1.0, norm));
            newSamples[i] = Math.sin(i * 0.35) * norm;
        }

        int shift = newSamples.length;
        System.arraycopy(audioSamples, shift, audioSamples, 0, SAMPLE_BUFFER_SIZE - shift);

        for (int i = 0; i < newSamples.length; i++) {
            int targetIndex = SAMPLE_BUFFER_SIZE - newSamples.length + i;
            if (targetIndex >= 0 && targetIndex < SAMPLE_BUFFER_SIZE) {
                smoothedSamples[targetIndex] = smoothedSamples[targetIndex] * (1 - SMOOTHING_FACTOR) +
                                               newSamples[i] * SMOOTHING_FACTOR;
                audioSamples[targetIndex] = smoothedSamples[targetIndex];
            }
        }

        draw();
    }

    public void updateWaveformFromMagnitudes(float[] magnitudes) {
        if (magnitudes == null || magnitudes.length == 0) {
            return;
        }

        double[] asDouble = new double[magnitudes.length];
        for (int i = 0; i < magnitudes.length; i++) {
            asDouble[i] = magnitudes[i];
        }
        updateWaveformFromMagnitudes(asDouble);
    }
    
    /**
     * Draws the waveform visualization
     */
    private void draw() {
        GraphicsContext gc = getGraphicsContext2D();
        double width = getWidth();
        double height = getHeight();
        
        // Clear canvas
        gc.clearRect(0, 0, width, height);
        
        if (audioSamples == null || audioSamples.length == 0) {
            drawEmptyState(gc, width, height);
            return;
        }
        
        double centerY = height / 2;
        double amplitude = (height / 2) - 10;
        
        // Draw center line
        gc.setStroke(Color.web("#ffffff", 0.1));
        gc.setLineWidth(1);
        gc.strokeLine(0, centerY, width, centerY);
        
        // Draw gradient grid
        drawGrid(gc, width, height);
        
        // Calculate sample width
        double sampleWidth = width / (double) audioSamples.length;
        
        // Draw filled waveform area with gradient
        gc.setEffect(new GaussianBlur(3));
        LinearGradient fillGradient = new LinearGradient(
            0, 0, 0, height,
            false, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web(accentColor, 0.3)),
            new Stop(0.5, Color.web(accentColor, 0.55)),
            new Stop(1, Color.web(accentColor, 0.3))
        );
        gc.setFill(fillGradient);
        
        gc.beginPath();
        gc.moveTo(0, centerY);
        
        for (int i = 0; i < audioSamples.length; i++) {
            double x = i * sampleWidth;
            double y = centerY - (audioSamples[i] * amplitude);
            
            if (i == 0) {
                gc.lineTo(x, y);
            } else {
                // Smooth curves
                double prevX = (i - 1) * sampleWidth;
                double prevY = centerY - (audioSamples[i - 1] * amplitude);
                double ctrlX = (prevX + x) / 2;
                double ctrlY = (prevY + y) / 2;
                gc.quadraticCurveTo(ctrlX, ctrlY, x, y);
            }
        }
        
        // Complete the fill shape
        for (int i = audioSamples.length - 1; i >= 0; i--) {
            double x = i * sampleWidth;
            double y = centerY + (Math.abs(audioSamples[i]) * amplitude);
            gc.lineTo(x, y);
        }
        
        gc.closePath();
        gc.fill();
        
        // Draw main waveform line with glow
        gc.setEffect(new GaussianBlur(6));
        gc.setStroke(Color.web(accentColor, 0.8));
        gc.setLineWidth(2.5);
        gc.beginPath();
        
        for (int i = 0; i < audioSamples.length; i++) {
            double x = i * sampleWidth;
            double y = centerY - (audioSamples[i] * amplitude);
            
            if (i == 0) {
                gc.moveTo(x, y);
            } else {
                double prevX = (i - 1) * sampleWidth;
                double prevY = centerY - (audioSamples[i - 1] * amplitude);
                double ctrlX = (prevX + x) / 2;
                double ctrlY = (prevY + y) / 2;
                gc.quadraticCurveTo(ctrlX, ctrlY, x, y);
            }
        }
        gc.stroke();
        
        // Draw sharp line on top
        gc.setEffect(null);
        gc.setStroke(Color.web(accentColor).brighter());
        gc.setLineWidth(1.5);
        gc.beginPath();
        
        for (int i = 0; i < audioSamples.length; i++) {
            double x = i * sampleWidth;
            double y = centerY - (audioSamples[i] * amplitude);
            
            if (i == 0) {
                gc.moveTo(x, y);
            } else {
                double prevX = (i - 1) * sampleWidth;
                double prevY = centerY - (audioSamples[i - 1] * amplitude);
                double ctrlX = (prevX + x) / 2;
                double ctrlY = (prevY + y) / 2;
                gc.quadraticCurveTo(ctrlX, ctrlY, x, y);
            }
        }
        gc.stroke();
        
        // Draw mirrored waveform below center line
        gc.setEffect(new GaussianBlur(6));
        gc.setStroke(Color.web(accentColor, 0.4));
        gc.setLineWidth(2.5);
        gc.beginPath();
        
        for (int i = 0; i < audioSamples.length; i++) {
            double x = i * sampleWidth;
            double y = centerY + (Math.abs(audioSamples[i]) * amplitude);
            
            if (i == 0) {
                gc.moveTo(x, y);
            } else {
                double prevX = (i - 1) * sampleWidth;
                double prevY = centerY + (Math.abs(audioSamples[i - 1]) * amplitude);
                double ctrlX = (prevX + x) / 2;
                double ctrlY = (prevY + y) / 2;
                gc.quadraticCurveTo(ctrlX, ctrlY, x, y);
            }
        }
        gc.stroke();
        
        gc.setEffect(null);
    }
    
    /**
     * Draws a subtle grid background
     */
    private void drawGrid(GraphicsContext gc, double width, double height) {
        gc.setStroke(Color.web("#ffffff", 0.03));
        gc.setLineWidth(1);
        
        // Horizontal lines
        int horizontalLines = 6;
        for (int i = 0; i <= horizontalLines; i++) {
            double y = (height / horizontalLines) * i;
            gc.strokeLine(0, y, width, y);
        }
        
        // Vertical lines
        int verticalLines = 20;
        for (int i = 0; i <= verticalLines; i++) {
            double x = (width / verticalLines) * i;
            gc.strokeLine(x, 0, x, height);
        }
    }
    
    /**
     * Draws empty state when no audio is playing
     */
    private void drawEmptyState(GraphicsContext gc, double width, double height) {
        double centerY = height / 2;
        
        // Draw center line
        gc.setStroke(Color.web(accentColor, 0.2));
        gc.setLineWidth(1);
        gc.strokeLine(0, centerY, width, centerY);
        
        // Draw placeholder text
        gc.setFill(Color.web(accentColor, 0.45));
        gc.setFont(javafx.scene.text.Font.font("Consolas", 12));
        String text = "WAVEFORM - Awaiting audio signal";
        double textWidth = gc.getFont().getSize() * text.length() * 0.5;
        gc.fillText(text, (width - textWidth) / 2, centerY - 5);
    }
}

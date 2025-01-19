package pleaseload;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

/**
 * A custom JPanel that visualizes audio data as a series of bars, with each bar corresponding
 * to a specific frequency bin in the audio spectrum.
 * The height of each bar is determined by the frequency's magnitude in the spectrum.
 */
public class VisualizerPanel extends JPanel {
    
    // An array of heights for the bars representing the audio spectrum. 
    private int[] barHeights;

    /**
     * Constructs a VisualizerPanel with a specified number of bars.
     *
     * @param numBars The number of bars to display.
     */
    public VisualizerPanel(int numBars) {
        this.barHeights = new int[numBars];
    }

    /**
     * Updates the visualizer with new heights for the bars.
     *
     * @param newHeights An array of new heights for the bars which are constantly updated by the audio data.
     */
    public void updateVisualizer(int[] newHeights) {
        this.barHeights = Arrays.copyOf(newHeights, newHeights.length);
        repaint();
    }

    /**
     * Paints the visualizer on the screen, drawing bars that represent the audio data.
     * Each bar's height corresponds to the magnitude of a frequency bin in the spectrum.
     *
     * @param g The Graphics object used for drawing the visualizer.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Calculate the width of each bar and the maximum height for the bars
        int barWidth = getWidth() / barHeights.length;
        int maxBarHeight = getHeight();

        for (int i = 0; i < barHeights.length; i++) {
            int barHeight = Math.min(barHeights[i], maxBarHeight);

            // Calculate color based on the bar's height
            Color barColor = getColorForHeight(barHeight, maxBarHeight);
            g.setColor(barColor);

            // Draw the bar (positioning it at the correct x-coordinate and setting its height)
            g.fillRect(i * barWidth, getHeight() - barHeight, barWidth - 2, barHeight);
        }
    }

    /**
     * Helper method to calculate the color of a bar based on its height.
     * The color transitions smoothly from green (low height) to yellow (medium height) to red (high height).
     *
     * @param height The height of the bar.
     * @param maxHeight The maximum possible height (used to calculate the color ratio).
     * @return The color for the bar based on its height.
     */
    private Color getColorForHeight(int height, int maxHeight) {
        // Calculate the ratio of the height relative to the maximum height
        float ratio = (float) height / maxHeight;

        // Calculate the RGB values for the color, transitioning from green to yellow to red
        int red = (int) (ratio * 255);
        int green = (int) ((1 - ratio) * 255);

        return new Color(red, green, 0);  // Return a color from green to red
    }
}

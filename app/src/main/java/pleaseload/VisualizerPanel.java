package pleaseload;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class VisualizerPanel extends JPanel {
    private int[] barHeights;

    public VisualizerPanel(int numBars) {
        this.barHeights = new int[numBars];
    }

    public void updateVisualizer(int[] newHeights) {
        this.barHeights = Arrays.copyOf(newHeights, newHeights.length);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int barWidth = getWidth() / barHeights.length;
        int maxBarHeight = getHeight();

        for (int i = 0; i < barHeights.length; i++) {
            int barHeight = Math.min(barHeights[i], maxBarHeight);

            // Calculate color based on bar height
            Color barColor = getColorForHeight(barHeight, maxBarHeight);
            g.setColor(barColor);

            // Draw the bar
            g.fillRect(i * barWidth, getHeight() - barHeight, barWidth - 2, barHeight);
        }
    }

    // Helper method to calculate the color based on height
    private Color getColorForHeight(int height, int maxHeight) {
        float ratio = (float) height / maxHeight;

        // Smooth gradient from green to yellow to red
        int red = (int) (ratio * 255);
        int green = (int) ((1 - ratio) * 255);

        return new Color(red, green, 0);
    }
}

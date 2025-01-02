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
        g.setColor(Color.GREEN);

        int barWidth = getWidth() / barHeights.length;
        for (int i = 0; i < barHeights.length; i++) {
            int barHeight = barHeights[i];
            g.fillRect(i * barWidth, getHeight() - barHeight, barWidth - 2, barHeight);
        }
    }
}

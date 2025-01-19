package pleaseload;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

/**
 * A custom JPanel that displays a visual representation of spectrum data.
 * The spectrum is drawn as a series of connected lines representing frequency bins.
 */
public class SpectrumPanel extends JPanel {
    
    private double[] spectrumData;

    /**
     * Constructs a new SpectrumPanel with the specified number of frequency bins.
     * 
     * @param numBins The number of bins (frequencies) in the spectrum.
     */
    public SpectrumPanel(int numBins) {
        this.spectrumData = new double[numBins];
    }

    /**
     * Updates the spectrum data with the new values and repaints the panel.
     * 
     * @param newSpectrumData The new spectrum data to be visualized.
     */
    public void updateSpectrum(double[] newSpectrumData) {
        this.spectrumData = Arrays.copyOf(newSpectrumData, newSpectrumData.length);
        repaint();
    }

    /**
     * Paints the component by rendering the spectrum data.
     * If the spectrum data is empty or null, no drawing occurs.
     * 
     * @param g The Graphics object used for rendering.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Get the current dimensions of the panel
        int width = getWidth();
        int height = getHeight();

        // Set background color 
        g.setColor(new Color(23, 21, 59));
        g.fillRect(0, 0, width, height);

        // If no spectrum data exists, skip painting
        if (spectrumData == null || spectrumData.length == 0) {
            return;
        }

        // Calculate the scaling factor for the Y axis
        double maxMagnitude = Arrays.stream(spectrumData).max().orElse(1);
        double scaleY = height / maxMagnitude;

        // Calculate the width of each bin
        int binWidth = width / spectrumData.length;

        // Set the color for drawing the spectrum lines
        g.setColor(Color.CYAN);

        // Loop through the spectrum data and draw connecting lines
        for (int i = 0; i < spectrumData.length - 1; i++) {
            int x1 = i * binWidth;
            int y1 = height - (int) (spectrumData[i] * scaleY);
            int x2 = (i + 1) * binWidth;
            int y2 = height - (int) (spectrumData[i + 1] * scaleY);

            // Draw a line between consecutive points in the spectrum data
            g.drawLine(x1, y1, x2, y2);
        }
    }
}

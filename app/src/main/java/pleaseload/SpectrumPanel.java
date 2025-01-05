package pleaseload;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class SpectrumPanel extends JPanel {
    private double[] spectrumData;

    public SpectrumPanel(int numBins) {
        this.spectrumData = new double[numBins];
    }

    public void updateSpectrum(double[] newSpectrumData) {
        this.spectrumData = Arrays.copyOf(newSpectrumData, newSpectrumData.length);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int width = getWidth();
        int height = getHeight();

        g.setColor(new Color(23, 21, 59));
        g.fillRect(0, 0, width, height);

        if (spectrumData == null || spectrumData.length == 0) {
            return;
        }

        // Calculate the scaling
        double maxMagnitude = Arrays.stream(spectrumData).max().orElse(1);
        double scaleY = height / maxMagnitude;

        int binWidth = width / spectrumData.length;

        g.setColor(Color.CYAN);
        for (int i = 0; i < spectrumData.length - 1; i++) {
            int x1 = i * binWidth;
            int y1 = height - (int) (spectrumData[i] * scaleY);
            int x2 = (i + 1) * binWidth;
            int y2 = height - (int) (spectrumData[i + 1] * scaleY);

            g.drawLine(x1, y1, x2, y2);
        }
    }
}


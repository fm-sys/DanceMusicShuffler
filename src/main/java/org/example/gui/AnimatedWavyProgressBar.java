package org.example.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.concurrent.atomic.AtomicReference;

public class AnimatedWavyProgressBar extends JComponent {
    private static final int SCALE = 5;

    private float progress = 0.0f; // Current progress (0.0 to 1.0)
    private float waveOffset = 0;

    public AnimatedWavyProgressBar() {
        setPreferredSize(new Dimension(500, 80));

        Timer timer = new Timer(16, e -> {
            waveOffset += 0.01f; // Slow wave movement
            repaint();
        });
        timer.start();
    }

    public void setProgress(float p) {
        this.progress = Math.max(0, Math.min(1, p));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        int width = getWidth();
        int height = getHeight();
        int centerY = height / 2;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        float progressX = width * progress;

        // Draw sine wave (white)
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(SCALE));

        float wavelength = SCALE * 10f;

        Path2D wavePath = new Path2D.Float();
        for (int x = 0; x < progressX; x++) {
            double radians = (x + waveOffset * wavelength) / wavelength * 2 * Math.PI;
            float y = Math.round(centerY + (float) Math.sin(radians) * SCALE);

            if (x == 0) {
                wavePath.moveTo(x, y);
            } else {
                wavePath.lineTo(x, y);
            }
        }
        g2.draw(wavePath);

        // Draw remaining gray straight line
        g2.setColor(new Color(100, 100, 100));
        g2.drawLine((int) progressX, centerY, width, centerY);

        // Draw vertical capsule-shaped thumb
        int thumbX = (int) progressX;
        int thumbHeight = 30;
        int thumbWidth = 8;
        int thumbY = centerY - thumbHeight / 2;

        g2.setColor(Color.WHITE);
        g2.fill(new RoundRectangle2D.Float(
                thumbX - thumbWidth / 2f, thumbY,
                thumbWidth, thumbHeight,
                thumbWidth, thumbWidth
        ));

        g2.dispose();
    }

    // Demo
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Material 3 Wavy Progress (Thumb Bar)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getContentPane().setBackground(new Color(0x20223A));
            frame.setLayout(new FlowLayout());

            AnimatedWavyProgressBar waveBar = new AnimatedWavyProgressBar();
            waveBar.setProgress(0.6f);
            frame.add(waveBar);

            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            AtomicReference<Float> progress = new AtomicReference<>((float) 0.0);
            new Timer(16, e -> waveBar.setProgress(progress.updateAndGet(v ->  (v + 0.0005f)))).start();
        });
    }
}


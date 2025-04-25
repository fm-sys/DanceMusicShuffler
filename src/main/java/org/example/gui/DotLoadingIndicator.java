package org.example.gui;

import javax.swing.*;
import java.awt.*;

public class DotLoadingIndicator extends JPanel {
    private int dotCount = 1;

    public DotLoadingIndicator() {
        setPreferredSize(new Dimension(300, 100));
        setOpaque(false);

        Timer timer = new Timer(500, e -> {
            dotCount = (dotCount) % 3 + 1;
            repaint();
        });
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        String dots = " o ".repeat(dotCount);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2.setFont(new Font("Sans", Font.BOLD, 16));
        FontMetrics fmDots = g2.getFontMetrics();
        int x2 = (getWidth() - fmDots.stringWidth(dots)) / 2;
        int y2 = getHeight() / 2;
        g2.drawString(dots, x2, y2);
    }
}

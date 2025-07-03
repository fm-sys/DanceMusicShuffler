package org.example;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class OverlayWindow extends JWindow {
    private final List<Rectangle> boxes;

    public OverlayWindow(List<Rectangle> boxes, int x, int y, int width, int height) {
        super();
        this.boxes = boxes;
        setBounds(x, y, width, height);
        setAlwaysOnTop(true);
        setBackground(new Color(0, 0, 0, 0));

        add(new OverlayPanel());
        setVisible(true);
    }

    private class OverlayPanel extends JPanel {
        public OverlayPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(new Color(255, 0, 0, 180)); // Semi-transparent red
            g2.setStroke(new BasicStroke(2));

            for (Rectangle rect : boxes) {
                g2.drawRect(rect.x, rect.y, rect.width, rect.height);
            }
        }
    }
}

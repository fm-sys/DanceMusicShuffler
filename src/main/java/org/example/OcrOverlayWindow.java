package org.example;

import net.sourceforge.tess4j.Word;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class OcrOverlayWindow extends JWindow {
    private final List<Rectangle> boxes = new ArrayList<>();

    public OcrOverlayWindow() {
        super();
        setAlwaysOnTop(true);
        setBackground(new Color(0, 0, 0, 0));
        add(new OverlayPanel());
        setVisible(true);
    }

    public void clearOverlay() {
        boxes.clear();
        repaint();
    }

    public void updateBoxes(List<Word> lines, int x, int y, int width, int height) {
        setBounds(x, y, width, height);

        boxes.clear();
        boxes.addAll(lines.stream()
                .map(word -> new Rectangle(
                        (int) word.getBoundingBox().getX(),
                        (int) word.getBoundingBox().getY(),
                        (int) word.getBoundingBox().getWidth(),
                        (int) word.getBoundingBox().getHeight()))
                .toList());

        repaint();

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

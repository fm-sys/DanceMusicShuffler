package org.example.gui;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import java.awt.*;

public class HalfHeightLeftBorder extends AbstractBorder {
    private final Color color;
    private final int thickness;

    public HalfHeightLeftBorder(Color color, int thickness) {
        this.color = color;
        this.thickness = thickness;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(color);
        g2.setStroke(new BasicStroke(thickness));

        int halfHeight = height / 2;
        int startY = height / 4;  // Start at 1/4th of the height
        int endY = startY + halfHeight;

        g2.drawLine(x, startY, x, endY);  // Draws only half-height on the left side
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(0, thickness, 0, 0); // Space for the left border
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.left = thickness;
        insets.top = 0;
        insets.bottom = 0;
        insets.right = 0;
        return insets;
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Custom Border Example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);

        JPanel panel = new JPanel();
        panel.setBackground(Color.BLACK);
        panel.setBorder(new HalfHeightLeftBorder(Color.WHITE, 2)); // Apply the custom border
        panel.setPreferredSize(new Dimension(100, 300));

        frame.add(panel, BorderLayout.WEST);
        frame.setVisible(true);
    }
}

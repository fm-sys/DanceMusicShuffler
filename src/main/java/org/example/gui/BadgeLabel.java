package org.example.gui;

import javax.swing.*;
import java.awt.*;

public class BadgeLabel extends JLabel {

    private Color badgeColor = Color.darkGray;

    public BadgeLabel(String text) {
        super(text);
        setOpaque(false); // Ensure transparency
        setForeground(Color.WHITE); // Set text color
        //setFont(new Font("Arial", Font.BOLD, 16));
        setHorizontalAlignment(SwingConstants.CENTER);
        setVerticalAlignment(SwingConstants.CENTER);
        setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10)); // Padding for better spacing
    }

    public Color getBadgeColor() {
        return badgeColor;
    }

    public void setBadgeColor(Color badgeColor) {
        this.badgeColor = badgeColor;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Set background color
        g2d.setColor(badgeColor);

        // Get label size
        int width = getWidth();
        int height = getHeight();
        // Rounded corners

        // Draw rounded rectangle
        g2d.fillRoundRect(0, 0, width, height, height, height);

        super.paintComponent(g);
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        int width = fm.stringWidth(getText()) + 20; // Add padding
        int height = fm.getHeight() + 10;
        return new Dimension(width, height);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Badge JLabel");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(300, 200);
            frame.setLayout(null);

            BadgeLabel badgeLabel = new BadgeLabel("Sample Badge");
            badgeLabel.setBounds(80, 50, badgeLabel.getPreferredSize().width, badgeLabel.getPreferredSize().height);

            frame.add(badgeLabel);
            frame.setVisible(true);
        });
    }
}

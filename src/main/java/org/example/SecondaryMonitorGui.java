package org.example;

import org.example.gui.AlignHelper;
import org.example.gui.BadgeLabel;
import org.example.gui.HalfHeightLeftBorder;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;

public class SecondaryMonitorGui {

    JLabel cover;
    JLabel titleLabel;
    JLabel artistLabel;
    JPanel badgesPanel;

    JPanel sidePanel;

    public SecondaryMonitorGui() {

        // Create a JFrame
        JFrame frame = new JFrame("Fullscreen on Secondary Monitor");
        frame.setUndecorated(true);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);




        JPanel coverPanel = new JPanel();
        coverPanel.setBackground(Color.BLACK);
        coverPanel.setLayout(new BoxLayout(coverPanel, BoxLayout.Y_AXIS));

        coverPanel.add(Box.createVerticalGlue());

        cover = new JLabel();
        coverPanel.add(AlignHelper.center(cover));

        coverPanel.add(Box.createVerticalStrut(50));

        titleLabel = new JLabel("Song Title");
        titleLabel.setFont(titleLabel.getFont().deriveFont(48.0f));
        titleLabel.setForeground(Color.WHITE);
        coverPanel.add(AlignHelper.center(titleLabel));

        coverPanel.add(Box.createVerticalStrut(10));

        artistLabel = new JLabel("Artist");
        artistLabel.setFont(artistLabel.getFont().deriveFont(Font.ITALIC).deriveFont(24.0f));
        artistLabel.setForeground(Color.LIGHT_GRAY);
        coverPanel.add(AlignHelper.center(artistLabel));

        coverPanel.add(Box.createVerticalStrut(30));

        badgesPanel = new JPanel();
        badgesPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 20));
        badgesPanel.setBackground(Color.BLACK);
        badgesPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 0));
        coverPanel.add(badgesPanel);

        coverPanel.add(Box.createVerticalGlue());

        frame.getContentPane().add(coverPanel, BorderLayout.CENTER);


        sidePanel = new JPanel();
        sidePanel.setBackground(Color.BLACK);
        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
        sidePanel.setPreferredSize(new Dimension(500, 0));
        sidePanel.setBorder(new HalfHeightLeftBorder(Color.WHITE, 2));
        frame.getContentPane().add(sidePanel, BorderLayout.LINE_END);



        // Get the available screen devices (monitors)
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] screens = ge.getScreenDevices();

        // Ensure there is more than one monitor
        if (screens.length < 2) {
            System.out.println("Secondary monitor not detected.");
            return;
        }

        // Select the secondary monitor (index 1)
        GraphicsDevice secondaryScreen = screens[1];
//        secondaryScreen.setFullScreenWindow(frame);
        Rectangle screenBounds = secondaryScreen.getDefaultConfiguration().getBounds();
        frame.setBounds(screenBounds);
        frame.setVisible(true);

        // Close operation
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public void update(BufferedImage coverImage, String trackName, String artistName, List<String> badges) {
        SwingUtilities.invokeLater(() -> {
            try {
                BufferedImage roundedImage = makeRoundedCorner(coverImage, 50);
                cover.setIcon(new ImageIcon(roundedImage));
                titleLabel.setText(trackName);
                artistLabel.setText(artistName);

                badgesPanel.removeAll();

                for (String badge : badges) {
                    BadgeLabel badgeLabel = new BadgeLabel(badge);
                    badgeLabel.setFont(badgeLabel.getFont().deriveFont(36.0f));
                    badgesPanel.add(badgeLabel);
                }

                badgesPanel.revalidate();
                badgesPanel.repaint();

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void updateSidePanel(List<String> badges) {
        sidePanel.removeAll();

        sidePanel.add(Box.createVerticalGlue());

        JLabel label = new JLabel("Nächste Tänze");
        label.setFont(titleLabel.getFont().deriveFont(36.0f));
        label.setForeground(Color.WHITE);
        sidePanel.add(AlignHelper.center(label));

        sidePanel.add(Box.createVerticalStrut(20));

        for (String badge : badges) {
            BadgeLabel badgeLabel = new BadgeLabel(badge);
            badgeLabel.setFont(badgeLabel.getFont().deriveFont(24.0f));
            sidePanel.add(AlignHelper.center(badgeLabel));
        }

        sidePanel.add(Box.createVerticalGlue());

        sidePanel.revalidate();
        sidePanel.repaint();
    }

    private static BufferedImage makeRoundedCorner(BufferedImage image, int cornerRadius) {
        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = output.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setClip(new RoundRectangle2D.Float(0, 0, width, height, cornerRadius, cornerRadius));

        // Draw the original image within the rounded mask
        g2.drawImage(image, 0, 0, null);
        g2.dispose();

        return output;
    }

}

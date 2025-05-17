package org.example;

import org.example.gui.AlignHelper;
import org.example.gui.AnimatedWavyProgressBar;
import org.example.gui.BadgeLabel;
import org.example.gui.HalfHeightLeftBorder;
import org.example.util.PreventSleep;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.Track;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SecondaryMonitorGui {

    private final JFrame frame;
    private final JLabel cover;
    private final JLabel titleLabel;
    private final JLabel artistLabel;
    private final JPanel badgesPanel;

    private final JPanel sidePanel;

    private final AnimatedWavyProgressBar progressBar;
    private String currentTrackId = null;
    private long startTimestamp = 0;
    private long duration = 0;
    private boolean paused = false;


    public SecondaryMonitorGui() {

        // Create a JFrame
        frame = new JFrame("Fullscreen on Secondary Monitor");
        frame.setUndecorated(true);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.getContentPane().setBackground(Color.BLACK);


        var icon = getClass().getResource("/icon48.png");
        if (icon != null) {
            frame.setIconImage(new ImageIcon(icon).getImage());
        }


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


        progressBar = new AnimatedWavyProgressBar();
        frame.getContentPane().add(progressBar, BorderLayout.PAGE_END);

        launchSecondaryMonitorGui(false);
    }

    public boolean launchSecondaryMonitorGui(boolean force) {
        if (frame.isVisible()) {
            frame.toFront();
            return true;
        }

        // Get the available screen devices (monitors)
        GraphicsDevice[] screens = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();

        // Ensure there is more than one monitor
        if (screens.length < 2 && !force) {
            System.out.println("Secondary monitor not detected.");
            return false;
        }

        if (screens.length > 1) {
            // Select the secondary monitor (index 1)
            GraphicsDevice secondaryScreen = screens[1];
            Rectangle screenBounds = secondaryScreen.getDefaultConfiguration().getBounds();
            frame.setBounds(screenBounds);
        }

        frame.setVisible(true);

        // Close operation
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        PreventSleep.startPreventingSleepLoop();

        new Timer(16, e -> {
            progressBar.setPaused(paused);
            if (!paused) {
                long elapsedTime = System.currentTimeMillis() - startTimestamp;
                progressBar.setProgress((float) elapsedTime / (float) duration);
            }
        }).start();

        return true;
    }

    public void update(BufferedImage coverImage, Track track, List<String> badges) {
        SwingUtilities.invokeLater(() -> {
            try {
                BufferedImage roundedImage = makeRoundedCorner(coverImage);
                cover.setIcon(new ImageIcon(roundedImage));
                titleLabel.setText(track.getName());
                artistLabel.setText(Arrays.stream(track.getArtists()).map(ArtistSimplified::getName).collect(Collectors.joining(", ")));

                if (!Objects.equals(currentTrackId, track.getId())) {
                    startTimestamp = System.currentTimeMillis();
                    currentTrackId = track.getId();
                    duration = track.getDurationMs();
                }

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

        JLabel label = new JLabel("N\u00e4chste T\u00e4nze");
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

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public void setProgress(long progressMs, long durationMs) {
        this.startTimestamp = System.currentTimeMillis() - progressMs;
        this.duration = durationMs;
    }

    private static BufferedImage makeRoundedCorner(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = output.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setClip(new RoundRectangle2D.Float(0, 0, width, height, 50, 50));

        // Draw the original image within the rounded mask
        g2.drawImage(image, 0, 0, null);
        g2.dispose();

        return output;
    }

}

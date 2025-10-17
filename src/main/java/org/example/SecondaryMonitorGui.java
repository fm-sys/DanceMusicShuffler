package org.example;

import org.example.gui.*;
import org.example.worker.PreventSleep;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.Track;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SecondaryMonitorGui {

    private final BackgroundPanel backgroundPanel;

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

    private BufferedImage backgroundImage;
    private boolean coloredBackground = true;

    public SecondaryMonitorGui() {

        // Create a JFrame
        frame = new JFrame("Fullscreen on Secondary Monitor");
        frame.setUndecorated(true);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);

        backgroundPanel = new BackgroundPanel();
        backgroundPanel.setLayout(new BorderLayout());
        frame.setContentPane(backgroundPanel);

        var icon = getClass().getResource("/icon48.png");
        if (icon != null) {
            frame.setIconImage(new ImageIcon(icon).getImage());
        }


        JPanel coverPanel = new JPanel();
        coverPanel.setOpaque(false);
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
        artistLabel.setForeground(new Color(255, 255, 255, 192));
        coverPanel.add(AlignHelper.center(artistLabel));

        coverPanel.add(Box.createVerticalStrut(30));

        badgesPanel = new JPanel();
        badgesPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 20));
        badgesPanel.setOpaque(false);
        badgesPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 0));
        coverPanel.add(badgesPanel);

        coverPanel.add(Box.createVerticalGlue());

        frame.getContentPane().add(coverPanel, BorderLayout.CENTER);


        sidePanel = new JPanel();
        sidePanel.setOpaque(false);
        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
        sidePanel.setPreferredSize(new Dimension(500, 0));
        sidePanel.setBorder(new HalfHeightLeftBorder(Color.WHITE, 2));
        frame.getContentPane().add(sidePanel, BorderLayout.LINE_END);


        progressBar = new AnimatedWavyProgressBar();
        var progressBarContainer = new JPanel(new BorderLayout());
        progressBarContainer.setOpaque(false);
        progressBarContainer.setBorder(BorderFactory.createEmptyBorder(0, 0, 35, 0));
        progressBarContainer.add(progressBar, BorderLayout.CENTER);
        frame.getContentPane().add(progressBarContainer, BorderLayout.PAGE_END);
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

        // Close operation - don't exit the whole application
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        PreventSleep.startPreventingSleepLoop();

        new Timer(16, e -> {
            progressBar.setPaused(paused);
            if (!paused) {
                long elapsedTime = System.currentTimeMillis() - startTimestamp;
                progressBar.setProgress((float) elapsedTime / (float) duration);
            }
        }).start();

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                PreventSleep.stopPreventingSleepLoop();
            }
        });

        SwingUtilities.invokeLater(frame::toFront);

        return true;
    }

    public void update(BufferedImage coverImage, BufferedImage background, Track track, List<String> badges) {
        SwingUtilities.invokeLater(() -> {
            try {
                cover.setIcon(new ImageIcon(coverImage));

                backgroundImage = background;
                if (coloredBackground) {
                    backgroundPanel.setBackgroundImage(backgroundImage);
                }

                titleLabel.setText(track.getName());
                artistLabel.setText(Arrays.stream(track.getArtists()).map(ArtistSimplified::getName).collect(Collectors.joining(", ")));

                if (currentTrackId == null) {
                    currentTrackId = track.getId();
                } else if (!currentTrackId.equals(track.getId())) {
                    startTimestamp = System.currentTimeMillis();
                    currentTrackId = track.getId();
                    duration = track.getDurationMs();
                }

                badgesPanel.removeAll();

                for (String badge : badges) {
                    BadgeLabel badgeLabel = new BadgeLabel(badge);
                    badgeLabel.setBadgeColor(new Color(255, 255, 255, 64));
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

    public void updateSidePanel(List<List<String>> badges) {
        sidePanel.removeAll();

        sidePanel.add(Box.createVerticalGlue());

        JLabel label = new JLabel("N\u00e4chste T\u00e4nze");
        label.setFont(titleLabel.getFont().deriveFont(36.0f));
        label.setForeground(Color.WHITE);
        sidePanel.add(AlignHelper.center(label));

        sidePanel.add(Box.createVerticalStrut(20));

        for (List<String> badge : badges) {
            Box b = Box.createHorizontalBox();
            b.add(Box.createHorizontalGlue());

            BadgeLabel badgeLabel = new BadgeLabel(badge.isEmpty() ? "  ?  " : badge.getFirst());
            badgeLabel.setBadgeColor(new Color(255, 255, 255, 64));
            badgeLabel.setFont(badgeLabel.getFont().deriveFont(24.0f));
            b.add(badgeLabel);

            if (badge.size() > 1) {
                JLabel moreLabel = new JLabel(" + " + (badge.size() - 1));
                moreLabel.setForeground(Color.WHITE);
                moreLabel.setFont(moreLabel.getFont().deriveFont(24.0f));
                b.add(moreLabel);
            }

            b.add(Box.createHorizontalGlue());
            sidePanel.add(b);
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

    public void setSidePanelVisible(boolean visible) {
        sidePanel.setPreferredSize(new Dimension(visible ? 500 : 0, 0));
        sidePanel.revalidate();
    }

    public void setColoredBackground(boolean selected) {
        this.coloredBackground = selected;
        if (coloredBackground) {
            backgroundPanel.setBackgroundImage(backgroundImage);
        } else {
            backgroundPanel.setBackgroundImage(null);
        }
    }
}

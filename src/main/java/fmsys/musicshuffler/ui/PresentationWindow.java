package fmsys.musicshuffler.ui;

import fmsys.musicshuffler.store.PlayerState;
import fmsys.musicshuffler.store.PlayerStore;
import fmsys.musicshuffler.store.PreferenceParams;
import fmsys.musicshuffler.store.PreferencesStore;
import fmsys.musicshuffler.platform.PreventSleep;
import fmsys.musicshuffler.service.ShuffleAlgorithm;
import fmsys.musicshuffler.ui.components.*;
import se.michaelthelin.spotify.model_objects.interfaces.IArtist;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.font.TextAttribute;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("UnnecessaryUnicodeEscape")
public class PresentationWindow {

    private static final Map<TextAttribute, Float> FONT_BADGE_WEIGHT = Collections.singletonMap(TextAttribute.WEIGHT, TextAttribute.WEIGHT_DEMIBOLD);
    private static final Color BADGE_BACKGROUND = new Color(255, 255, 255, 64);

    private final PlayerStore playerStore;
    private final PreferencesStore preferencesStore;

    private final ShuffleAlgorithm shuffleAlgorithm;

    private final BackgroundPanel backgroundPanel;

    private final JFrame frame;
    private final JLabel cover;
    private final JLabel titleLabel;
    private final JLabel artistLabel;
    private final JPanel badgesPanel;

    private final JPanel sidePanel;

    private final AnimatedWavyProgressBar progressBar;
    private final Timer timer;


    public PresentationWindow(PlayerStore playerStore, PreferencesStore preferencesStore, ShuffleAlgorithm shuffleAlgorithm) {

        this.playerStore = playerStore;
        this.preferencesStore = preferencesStore;
        this.shuffleAlgorithm = shuffleAlgorithm;

        // Create a JFrame
        frame = new JFrame("Dance Floor Display");
        frame.setUndecorated(true);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);

        backgroundPanel = new BackgroundPanel();
        backgroundPanel.setLayout(new BorderLayout());
        frame.setContentPane(backgroundPanel);

        JRootPane rootPane = frame.getRootPane();
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(KeyStroke.getKeyStroke("ESCAPE"), "closeWindow");
        rootPane.getActionMap().put("closeWindow", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                frame.dispose();
            }
        });

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
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 48.0f));
        titleLabel.setForeground(Color.WHITE);
        coverPanel.add(AlignHelper.center(titleLabel));

        coverPanel.add(Box.createVerticalStrut(10));

        artistLabel = new JLabel("Artist");
        artistLabel.setFont(artistLabel.getFont().deriveFont(Font.ITALIC, 24.0f));
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

        update(playerStore.get());
        playerStore.subscribe(this::update);
        applyPrefs(preferencesStore.get());
        preferencesStore.subscribe(this::applyPrefs);

         timer = new Timer(16, e -> {
             PlayerState state = playerStore.get();
            progressBar.setPaused(!state.isPlaying());
            progressBar.setProgress(state.getProgressPercentage());
        });

        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                PreventSleep.stopPreventingSleepLoop();
                timer.stop();
            }
        });

        launch(false);
    }

    public boolean launch(boolean force) {
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

        PreventSleep.startPreventingSleepLoop();
        timer.start();

        SwingUtilities.invokeLater(frame::toFront);

        return true;
    }

    private void update(PlayerState playerState) {
        SwingUtilities.invokeLater(() -> {
            try {
                if (playerState.track() == null) {
                    return;
                }

                if (playerState.coverImage() != null) {
                    cover.setIcon(new ImageIcon(playerState.coverImage()));
                }

                if (preferencesStore.get().showBackground()) {
                    backgroundPanel.setBackgroundImage(playerState.backgroundImage());
                }

                titleLabel.setText(playerState.track().getName());
                artistLabel.setText(Arrays.stream(playerState.track().getArtists()).map(IArtist::getName).collect(Collectors.joining(", ")));

                badgesPanel.removeAll();

                for (String badge : shuffleAlgorithm.getBadges(playerState.track())) {
                    BadgeLabel badgeLabel = new BadgeLabel(badge);
                    badgeLabel.setBadgeColor(BADGE_BACKGROUND);
                    badgeLabel.setFont(badgeLabel.getFont().deriveFont(FONT_BADGE_WEIGHT).deriveFont(36.0f));
                    badgesPanel.add(badgeLabel);
                }

                badgesPanel.revalidate();
                badgesPanel.repaint();

                updateSidePanel(playerState.queue().stream().map(shuffleAlgorithm::getBadges).limit(5).toList());

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void updateSidePanel(List<List<String>> badges) {
        sidePanel.removeAll();

        sidePanel.add(Box.createVerticalGlue());

        JLabel label = new JLabel("N\u00e4chste T\u00e4nze");
        label.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 36.0f));
        label.setForeground(Color.WHITE);
        sidePanel.add(AlignHelper.center(label));

        sidePanel.add(Box.createVerticalStrut(20));

        for (List<String> badge : badges) {
            Box b = Box.createHorizontalBox();
            b.add(Box.createHorizontalGlue());

            BadgeLabel badgeLabel = new BadgeLabel(badge.isEmpty() ? "  ?  " : badge.getFirst());
            badgeLabel.setBadgeColor(BADGE_BACKGROUND);
            badgeLabel.setFont(badgeLabel.getFont().deriveFont(FONT_BADGE_WEIGHT).deriveFont(24.0f));
            b.add(badgeLabel);

            if (badge.size() > 1) {
                JLabel moreLabel = new JLabel(" + " + (badge.size() - 1));
                moreLabel.setForeground(Color.WHITE);
                moreLabel.setFont(moreLabel.getFont().deriveFont(FONT_BADGE_WEIGHT).deriveFont(24.0f));
                b.add(moreLabel);
            }

            b.add(Box.createHorizontalGlue());
            sidePanel.add(b);
        }

        sidePanel.add(Box.createVerticalGlue());

        sidePanel.revalidate();
        sidePanel.repaint();
    }
    private void setSidePanelVisible(boolean visible) {
        sidePanel.setPreferredSize(new Dimension(visible ? 500 : 0, 0));
        sidePanel.revalidate();
    }

    private void setCoverVisible(boolean visible) {
        cover.setVisible(visible);
        cover.revalidate();
    }

    private void setColoredBackground(boolean enabled) {
        if (enabled) {
            backgroundPanel.setBackgroundImage(playerStore.get().backgroundImage());
        } else {
            backgroundPanel.setBackgroundImage(null);
        }
    }

    private void applyPrefs(PreferenceParams prefs) {
        setSidePanelVisible(prefs.showSidePanel());
        setCoverVisible(prefs.showCover());
        setColoredBackground(prefs.showBackground());
    }
}

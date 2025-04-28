package org.example;

import org.example.api.Api;
import org.example.worker.PersistentPreferences;
import org.example.worker.PlaylistLoader;
import org.example.api.SpotifyWindowTitle;
import org.example.gui.AlignHelper;
import org.example.gui.BadgeLabel;
import org.example.gui.HintTextField;
import org.example.gui.TristateCheckBox;
import org.example.models.PlaylistModel;
import org.example.worker.ShuffleAlgorithm;
import se.michaelthelin.spotify.model_objects.IPlaylistItem;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;
import se.michaelthelin.spotify.model_objects.specification.Track;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

public class MainGui {

    final SecondaryMonitorGui secondaryMonitorGui = new SecondaryMonitorGui();

    ArrayList<PlaylistModel> playlists = new ArrayList<>();
    ArrayList<PlaylistModel> playlistsFiltered = new ArrayList<>();
    ShuffleAlgorithm shuffleAlgorithm;

    JFrame frame;

    String filterText;

    TristateCheckBox selectAllCheckbox;
    HintTextField playlistsFilterTextField;
    JPanel playlistsListPanel;

    JPanel queueListPanel;

    JPanel nowPlayingPanel;

    JSpinner songNumberSpinner;
    JSpinner cooldownSpinner;
    JButton loadAndShuffleButton;

    JPanel[] sidePanels = new JPanel[2];

    public MainGui(Collection<PlaylistSimplified> lists) {

        lists
//                .stream()
//                .sorted(Comparator.comparing(PlaylistSimplified::getName))
                .forEach(playlist -> playlists.add(new PlaylistModel(playlist)));
        playlistsFiltered.addAll(playlists);
        shuffleAlgorithm = new ShuffleAlgorithm(playlists);

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                 UnsupportedLookAndFeelException e) {
            throw new RuntimeException(e);
        }

        frame = new JFrame();
        frame.setLayout(new BorderLayout());

//        panel = new JPanel();
//        panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
//        //panel.setLayout(new GridLayout(0, 1));
//
//        panel.add(label, BorderLayout.CENTER);
//
        createPlaylistList();
        createQueueList();
        createCenterOptionsPanel();
        createNowPlaying();

        new Timer(1000, evt -> {
            if (SpotifyWindowTitle.titleChanged()) {
                Timer timer = new Timer(3000, e -> updateNowPlaying());
                timer.setRepeats(false);
                timer.start();
            }
        }).start();

//        JPanel badgePanel = new JPanel();
//        BadgeLabel label1 = new BadgeLabel("bla");
//        label1.setText("This is a lable");
//        badgePanel.add(label);

        //frame.add(badgePanel, BorderLayout.LINE_END);


        frame.addWindowFocusListener(new WindowFocusListener() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                updateNowPlaying();
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
                // not interested
            }
        });

        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                for (JPanel panel : sidePanels) {
                    panel.setPreferredSize(new Dimension((frame.getWidth() - 400) / 2, 0));
                    panel.revalidate();
                }
            }
        });

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setTitle("Dance Music Shuffler");
        var icon = getClass().getResource("/icon48.png");
        if (icon != null) {
            frame.setIconImage(new ImageIcon(icon).getImage());
        }
        frame.setMinimumSize(new Dimension(600, 300));
        frame.setSize(new Dimension(1000, 600));
        frame.setLocationByPlatform(true);
        frame.setVisible(true);
        loadAndShuffleButton.requestFocusInWindow();
        SwingUtilities.invokeLater(frame::toFront);

    }

    private void createCenterOptionsPanel() {
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        frame.add(centerPanel, BorderLayout.CENTER);

        centerPanel.add(Box.createVerticalGlue());

        JPanel labeledPanelOptions = new JPanel();
        labeledPanelOptions.setLayout(new BoxLayout(labeledPanelOptions, BoxLayout.Y_AXIS));
        labeledPanelOptions.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Options"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        centerPanel.add(labeledPanelOptions);

        JLabel songNumberLabel = new JLabel("Number of songs to add to queue:");
        labeledPanelOptions.add(AlignHelper.left(songNumberLabel));

        songNumberSpinner = new JSpinner(new SpinnerNumberModel(10, 0, Integer.MAX_VALUE, 1));
        songNumberSpinner.setMaximumSize(new Dimension(songNumberSpinner.getPreferredSize().width, songNumberSpinner.getPreferredSize().height));
        labeledPanelOptions.add(AlignHelper.left(songNumberSpinner));

        labeledPanelOptions.add(Box.createVerticalStrut(10));

        JLabel cooldownLabel = new JLabel("Number of songs a playlist should not be reused:");
        labeledPanelOptions.add(AlignHelper.left(cooldownLabel));

        cooldownSpinner = new JSpinner(new SpinnerNumberModel(3, 0, Integer.MAX_VALUE, 1));
        cooldownSpinner.setMaximumSize(new Dimension(cooldownSpinner.getPreferredSize().width, cooldownSpinner.getPreferredSize().height));
        labeledPanelOptions.add(AlignHelper.left(cooldownSpinner));

        labeledPanelOptions.add(Box.createVerticalStrut(10));

        JLabel exclusiveLabel = new JLabel("These playlists are not allowed to be played directly after each other:");
        labeledPanelOptions.add(AlignHelper.left(exclusiveLabel));

        JButton exclusiveButton = new JButton("Select exclusive playlists");
        exclusiveButton.addActionListener(e -> showExclusivePoolDialog());
        labeledPanelOptions.add(AlignHelper.left(exclusiveButton));

        labeledPanelOptions.add(Box.createVerticalStrut(10));

        JLabel weightsLabel = new JLabel("Configure how often a playlists is chosen:");
        labeledPanelOptions.add(AlignHelper.left(weightsLabel));

        JButton weightsButton = new JButton("Adjust weights");
        weightsButton.addActionListener(e -> showWeightsDialog());
        labeledPanelOptions.add(AlignHelper.left(weightsButton));

        centerPanel.add(Box.createVerticalStrut(10));

        JPanel labeledPanelConfig = new JPanel(new GridLayout(0, 2));
        labeledPanelConfig.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Store/Load Configuration"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        labeledPanelConfig.setMaximumSize(new Dimension(Integer.MAX_VALUE, 0));
        centerPanel.add(labeledPanelConfig);

        JButton storeButton = new JButton("Store configuration");
        storeButton.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(frame, "This will overwrite any existing configuration. Do you want to continue?", "Warning", JOptionPane.YES_NO_OPTION);
            if (result != JOptionPane.YES_OPTION) {
                return;
            }
            PersistentPreferences.store(playlists, (int) songNumberSpinner.getValue(), (int) cooldownSpinner.getValue());
        });
        labeledPanelConfig.add(storeButton);

        JButton loadButton = new JButton("Load configuration");
        loadButton.addActionListener(e -> {
            PersistentPreferences.MainGuiParams params = PersistentPreferences.load(playlists);
            if (params != null) {
                songNumberSpinner.setValue(params.count);
                cooldownSpinner.setValue(params.cooldown);
                recreatePlaylistsList();
            }
        });
        labeledPanelConfig.add(loadButton);

        centerPanel.add(Box.createVerticalStrut(10));

        loadAndShuffleButton = new JButton("Load Playlists and Shuffle");
        loadAndShuffleButton.addActionListener(e -> {
            if (playlistsFiltered.stream().noneMatch(PlaylistModel::isChecked)) {
                JOptionPane.showMessageDialog(frame, "Please select at least one playlist to shuffle.");
                return;
            }

            loadAndShuffleButton.setEnabled(false);
            loadAndShuffleButton.setText("Loading...");

            PlaylistLoader.loadPlaylistsAsync(playlists)
                    .thenAccept(success -> {
                        if (success) {
                            shuffleAlgorithm.shuffleAsync((int) songNumberSpinner.getValue(), (int) cooldownSpinner.getValue()).thenAccept(result -> restoreLoadAndShuffleButton());
                        } else {
                            JOptionPane.showMessageDialog(frame, "Error loading playlists. Please try again.");
                            restoreLoadAndShuffleButton();
                        }
                    })
                    .whenComplete((res, ex) -> {
                        if (ex != null) {
                            System.err.println("Caught Exception: " + ex.getMessage());
                            ex.printStackTrace();
                        }
                    });
        });
        JPanel expandedButtonPanel = new JPanel(new GridLayout(0, 1));
        expandedButtonPanel.setPreferredSize(new Dimension(0, 50));
        expandedButtonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 0));
        expandedButtonPanel.add(loadAndShuffleButton);
        centerPanel.add(expandedButtonPanel);

        centerPanel.add(Box.createVerticalGlue());

    }

    private void restoreLoadAndShuffleButton() {
        loadAndShuffleButton.setEnabled(true);
        loadAndShuffleButton.setText("Load Playlists and Shuffle");
        updateNowPlaying(); // Cause there may be new badges available
    }

    private void showExclusivePoolDialog() {
        if (playlistsFiltered.stream().noneMatch(PlaylistModel::isChecked)) {
            JOptionPane.showMessageDialog(frame, "Please select playlists for the general pool first.");
            return;
        }

        JDialog dialog = new JDialog(frame, "Select exclusive playlists", true);
        dialog.setLayout(new BoxLayout(dialog.getContentPane(), BoxLayout.Y_AXIS));
        dialog.setSize(300, 200);
        dialog.setLocationRelativeTo(frame);

        JPanel checkboxesPanel = new JPanel();
        checkboxesPanel.setLayout(new BoxLayout(checkboxesPanel, BoxLayout.Y_AXIS));

        playlists.stream().filter(PlaylistModel::isChecked).forEach(playlist -> {
            JCheckBox checkBox = new JCheckBox(playlist.getPlaylist().getName());
            checkBox.setSelected(playlist.isExclusive());
            checkBox.addActionListener(event -> playlist.setExclusive(checkBox.isSelected()));
            checkboxesPanel.add(checkBox);
        });

        JScrollPane scrollPane = new JScrollPane(checkboxesPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        dialog.add(scrollPane);

        dialog.setVisible(true);
    }

    private void showWeightsDialog() {
        if (playlistsFiltered.stream().noneMatch(PlaylistModel::isChecked)) {
            JOptionPane.showMessageDialog(frame, "Please select playlists for the general pool first.");
            return;
        }

        JDialog dialog = new JDialog(frame, "Adjust weights", true);
        dialog.setLayout(new BoxLayout(dialog.getContentPane(), BoxLayout.Y_AXIS));
        dialog.setSize(300, 200);
        dialog.setLocationRelativeTo(frame);

        JPanel weightsPanel = new JPanel();
        weightsPanel.setLayout(new BoxLayout(weightsPanel, BoxLayout.Y_AXIS));

        playlists.stream().filter(PlaylistModel::isChecked).forEach(playlist -> {
            JLabel label = new JLabel(playlist.getPlaylist().getName());
            JSpinner spinner = new JSpinner();
            spinner.setModel(new SpinnerNumberModel(playlist.getWeight(), 0, 10, 0.1));
            spinner.addChangeListener(e -> playlist.setWeight((double) spinner.getValue()));
            spinner.setPreferredSize(new Dimension(50, spinner.getPreferredSize().height));
            spinner.setMaximumSize(new Dimension(50, spinner.getPreferredSize().height));

            Box b = Box.createHorizontalBox();
            b.add(label);
            b.add(Box.createHorizontalGlue());
            b.add(spinner);

            weightsPanel.add(b);
            weightsPanel.add(Box.createVerticalStrut(5));
        });

        JScrollPane scrollPane = new JScrollPane(weightsPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        dialog.add(scrollPane);

        dialog.setVisible(true);
    }

    private void createQueueList() {

        JPanel outerPanel = new JPanel();
        outerPanel.setLayout(new BoxLayout(outerPanel, BoxLayout.Y_AXIS));

        JLabel queueLabel = new JLabel("Queue");
        queueLabel.setFont(queueLabel.getFont().deriveFont(Font.BOLD));
        outerPanel.add(AlignHelper.center(queueLabel));

        queueListPanel = new JPanel();
        queueListPanel.setLayout(new BoxLayout(queueListPanel, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(queueListPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        outerPanel.add(scrollPane);

        sidePanels[1] = outerPanel;
        frame.add(outerPanel, BorderLayout.LINE_END);

    }

    private int calculateBadgeHeight() {
        return new BadgeLabel("Dummy").getPreferredSize().height;
    }

    private void recreateQueueList(java.util.List<IPlaylistItem> queue) {
        queueListPanel.removeAll();

        int lineHeight = calculateBadgeHeight();
        ArrayList<String> nextPlaylists = new ArrayList<>();

        queue.forEach(item -> {
            Box b = Box.createHorizontalBox();
            b.add(Box.createRigidArea(new Dimension(5, lineHeight)));
            JLabel label = new JLabel(item.getName());
            if (!shuffleAlgorithm.wasAddedByShuffleAlgorithm(item)) {
                label.setForeground(Color.GRAY);
            }
            b.add(label);
            for (String badge : getBadges(item)) {
                nextPlaylists.add(badge);
                b.add(Box.createHorizontalStrut(5));
                b.add(new BadgeLabel(badge));
            }
            b.add(Box.createHorizontalGlue());
            queueListPanel.add(b);
        });
        queueListPanel.revalidate(); // Updates layout
        queueListPanel.repaint();    // Redraws panel

        secondaryMonitorGui.updateSidePanel(nextPlaylists.stream().limit(5).toList());
    }

    private void createPlaylistList() {

        JPanel outerPanel = new JPanel();
        outerPanel.setLayout(new BoxLayout(outerPanel, BoxLayout.Y_AXIS));

        playlistsFilterTextField = new HintTextField("Filter playlists by title/description/owner");
        playlistsFilterTextField.setMaximumSize(new Dimension(Integer.MAX_VALUE, playlistsFilterTextField.getPreferredSize().height));
        playlistsFilterTextField.addCaretListener(e -> {

            if (playlistsFilterTextField.getTextWithoutHint().equals(filterText)) {
                return; //Text hasn't changed
            }

            filterText = playlistsFilterTextField.getTextWithoutHint();
            if (filterText.isBlank()) {
                playlistsFiltered.clear();
                playlistsFiltered.addAll(playlists);
            } else {
                playlistsFiltered.clear();
                for (PlaylistModel playlist : playlists) {
                    if (playlist.getPlaylist().getName().toLowerCase().contains(filterText.toLowerCase().trim()) ||
                            playlist.getPlaylist().getDescription().toLowerCase().contains(filterText.toLowerCase().trim()) ||
                            playlist.getPlaylist().getOwner().getDisplayName().toLowerCase().contains(filterText.toLowerCase().trim())) {
                        playlistsFiltered.add(playlist);
                    }
                }
            }
            recreatePlaylistsList();
        });
        outerPanel.add(playlistsFilterTextField);

        selectAllCheckbox = new TristateCheckBox();
        selectAllCheckbox.addActionListener(e -> {
            boolean isSelected = selectAllCheckbox.isSelected();
            playlistsFiltered.forEach(playlist -> playlist.setChecked(isSelected));
            recreatePlaylistsList();
        });


        outerPanel.add(AlignHelper.left(selectAllCheckbox));

        playlistsListPanel = new JPanel();
        playlistsListPanel.setLayout(new BoxLayout(playlistsListPanel, BoxLayout.Y_AXIS));

        recreatePlaylistsList();

        JScrollPane scrollPane = new JScrollPane(playlistsListPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        outerPanel.add(scrollPane);

        sidePanels[0] = outerPanel;
        frame.add(outerPanel, BorderLayout.LINE_START);

    }

    private void recreatePlaylistsList() {
        playlistsListPanel.removeAll();

        playlistsFiltered.forEach(playlist -> {
            JCheckBox checkBox = new JCheckBox(playlist.getPlaylist().getName() + " (" + playlist.getPlaylist().getTracks().getTotal() + " Lieder)");
            checkBox.setSelected(playlist.isChecked());
            checkBox.addActionListener(e -> {
                playlist.setChecked(checkBox.isSelected());
                updateSelectAllCheckbox();
            });
            playlistsListPanel.add(checkBox);
        });
        playlistsListPanel.revalidate(); // Updates layout
        playlistsListPanel.repaint();    // Redraws panel
        updateSelectAllCheckbox();
    }

    private void updateSelectAllCheckbox() {
        int selectedCount = (int) playlistsFiltered.stream().filter(PlaylistModel::isChecked).count();
        selectAllCheckbox.setText(selectedCount + " von " + playlistsFiltered.size() + " ausgew\u00e4hlt");
        if (selectedCount == 0) {
            selectAllCheckbox.setSelected(false);
            selectAllCheckbox.setHalfSelected(false);
        } else if (selectedCount != playlistsFiltered.size()) {
            selectAllCheckbox.setSelected(false);
            selectAllCheckbox.setHalfSelected(true);
        } else {
            selectAllCheckbox.setSelected(true);
            selectAllCheckbox.setHalfSelected(false);
            selectAllCheckbox.setText("Alle ausgew\u00e4hlt");
        }
    }

    private void createNowPlaying() {
        nowPlayingPanel = new JPanel();
        nowPlayingPanel.setLayout(new BoxLayout(nowPlayingPanel, BoxLayout.X_AXIS));
        nowPlayingPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        nowPlayingPanel.setBackground(Color.lightGray);
        frame.add(nowPlayingPanel, BorderLayout.PAGE_END);
        updateNowPlaying();
    }

    private void updateNowPlaying() {
        Api.INSTANCE.getTheUsersQueue().build().executeAsync().thenAccept(response -> {
            recreateQueueList(response.getQueue());

            if (!SpotifyWindowTitle.initialized()) {
                SpotifyWindowTitle.searchSpotifyWindowInitial(response.getCurrentlyPlaying());
            }

            nowPlayingPanel.removeAll();
            nowPlayingPanel.add(Box.createHorizontalGlue());
            nowPlayingPanel.add(Box.createRigidArea(new Dimension(0, calculateBadgeHeight())));
            nowPlayingPanel.add(new JLabel("Now playing: "));
            if (response.getCurrentlyPlaying() != null) {
                StringBuilder label = new StringBuilder();
                label.append(response.getCurrentlyPlaying().getName());

                ArrayList<String> badges = getBadges(response.getCurrentlyPlaying());

                if (response.getCurrentlyPlaying() instanceof Track track) {
                    if (track.getArtists() != null) {
                        label.append(" by ");

                        ArtistSimplified[] artists = track.getArtists();
                        for (int i = 0; i < artists.length; i++) {
                            label.append(artists[i].getName());
                            if (i < artists.length - 1) {
                                label.append(", ");
                            }
                        }
                    }

                    if (track.getAlbum() != null && track.getAlbum().getImages() != null) {
                        Arrays.stream(track.getAlbum().getImages()).findFirst().ifPresent(image -> EventQueue.invokeLater(() -> {
                            try {
                                URI url = new URI(image.getUrl());
                                BufferedImage image1 = ImageIO.read(url.toURL());
                                secondaryMonitorGui.update(image1, track.getName(), Arrays.stream(track.getArtists()).map(ArtistSimplified::getName).collect(Collectors.joining(", ")), badges);
                            } catch (Exception exp) {
                                exp.printStackTrace();
                            }

                        }));
                    }
                }

                JLabel l = new JLabel(label.toString());
                l.setFont(l.getFont().deriveFont(Font.BOLD));
                nowPlayingPanel.add(l);

                for (String badge : badges) {
                    nowPlayingPanel.add(Box.createHorizontalStrut(5));
                    nowPlayingPanel.add(new BadgeLabel(badge));
                }

            } else {
                nowPlayingPanel.add(new JLabel("- - -"));
            }

            nowPlayingPanel.add(Box.createHorizontalGlue());
            nowPlayingPanel.revalidate();
            nowPlayingPanel.repaint();
        }).whenComplete((res, ex) -> {
            if (ex != null) {
                System.err.println("Caught Exception: " + ex.getMessage());
            }
        });
    }

    private ArrayList<String> getBadges(IPlaylistItem track) {
        ArrayList<String> badges = new ArrayList<>();
        for (PlaylistModel playlist : playlists) {
            if (!playlist.isChecked() || playlist.getTracks() == null) {
                continue;
            }
            if (playlist.getTracks().stream().anyMatch(playlistTrack -> track.getId().equals(playlistTrack.getTrack().getId()))) {
                badges.add(playlist.getPlaylist().getName());
            }
        }
        return badges;
    }
}

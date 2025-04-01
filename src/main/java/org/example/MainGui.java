package org.example;

import org.example.api.Api;
import org.example.api.PlaylistLoader;
import org.example.api.SpotifyWindowTitle;
import org.example.gui.AlignHelper;
import org.example.gui.BadgeLabel;
import org.example.gui.HintTextField;
import org.example.gui.TristateCheckBox;
import org.example.models.PlaylistModel;
import se.michaelthelin.spotify.model_objects.IPlaylistItem;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;
import se.michaelthelin.spotify.model_objects.specification.Track;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
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

    public MainGui(Collection<PlaylistSimplified> lists) {

        lists
//                .stream()
//                .sorted(Comparator.comparing(PlaylistSimplified::getName))
                .forEach(playlist -> playlists.add(new PlaylistModel(playlist)));
        playlistsFiltered.addAll(playlists);

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

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setTitle("Dance Music Shuffler");
        frame.setMinimumSize(new Dimension(600, 300));
        frame.setSize(new Dimension(900, 600));
        frame.setLocationByPlatform(true);
        frame.setVisible(true);
        loadAndShuffleButton.requestFocusInWindow();
    }

    private void createCenterOptionsPanel() {
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        frame.add(centerPanel, BorderLayout.CENTER);

        centerPanel.add(Box.createVerticalGlue());

        JPanel labeledPanel = new JPanel();
        labeledPanel.setLayout(new BoxLayout(labeledPanel, BoxLayout.Y_AXIS));
        labeledPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Options"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        centerPanel.add(labeledPanel);

        JLabel songNumberLabel = new JLabel("Number of songs to add to queue:");
        labeledPanel.add(AlignHelper.left(songNumberLabel));

        songNumberSpinner = new JSpinner(new SpinnerNumberModel(10, 0, Integer.MAX_VALUE, 1));
        songNumberSpinner.setMaximumSize(new Dimension(songNumberSpinner.getPreferredSize().width, songNumberSpinner.getPreferredSize().height));
        labeledPanel.add(AlignHelper.left(songNumberSpinner));

        labeledPanel.add(Box.createVerticalStrut(10));

        JLabel cooldownLabel = new JLabel("Number of songs a playlist should not be reused:");
        labeledPanel.add(AlignHelper.left(cooldownLabel));

        cooldownSpinner = new JSpinner(new SpinnerNumberModel(3, 0, Integer.MAX_VALUE, 1));
        cooldownSpinner.setMaximumSize(new Dimension(cooldownSpinner.getPreferredSize().width, cooldownSpinner.getPreferredSize().height));
        labeledPanel.add(AlignHelper.left(cooldownSpinner));

        labeledPanel.add(Box.createVerticalStrut(10));

        JLabel exclusiveLabel = new JLabel("These playlists are not allowed to be played directly after each other:");
        labeledPanel.add(AlignHelper.left(exclusiveLabel));

        JButton exclusiveButton = new JButton("Select exclusive playlists");
        exclusiveButton.addActionListener(e -> showExclusivePoolDialog());
        labeledPanel.add(AlignHelper.left(exclusiveButton));

        centerPanel.add(Box.createVerticalStrut(10));

        loadAndShuffleButton = new JButton("Load Playlists and Shuffle");
        loadAndShuffleButton.addActionListener(e -> {
            if (playlistsFiltered.stream().noneMatch(PlaylistModel::isChecked)) {
                JOptionPane.showMessageDialog(frame, "Please select at least one playlist to shuffle.");
                return;
            }

            loadAndShuffleButton.setEnabled(false);
            loadAndShuffleButton.setText("Loading...");

            PlaylistLoader.loadPlaylistsAsync(playlists).thenAccept(success -> {
                if (success) {
                    // Shuffle playlists

                } else {
                    JOptionPane.showMessageDialog(frame, "Error loading playlists. Please try again.");
                }

                loadAndShuffleButton.setEnabled(true);
                loadAndShuffleButton.setText("Load Playlists and Shuffle");
                updateNowPlaying(); // Cause there may be new badges available
            });
        });
        JPanel expandedButtonPanel = new JPanel(new GridLayout(0, 1));
        expandedButtonPanel.setPreferredSize(new Dimension(0, 50));
        expandedButtonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 0));
        expandedButtonPanel.add(loadAndShuffleButton);
        centerPanel.add(expandedButtonPanel);

        centerPanel.add(Box.createVerticalGlue());

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

        outerPanel.setPreferredSize(new Dimension(250, 0));

        outerPanel.add(scrollPane);

        frame.add(outerPanel, BorderLayout.LINE_END);

    }

    private int calculateBadgeHeight() {
        return new BadgeLabel("Dummy").getPreferredSize().height;
    }

    private void recreateQueueList(java.util.List<IPlaylistItem> queue) {
        queueListPanel.removeAll();

        int lineHeight = calculateBadgeHeight();

        queue.forEach(item -> {
            Box b = Box.createHorizontalBox();
            b.add(Box.createRigidArea(new Dimension(5, lineHeight)));
            b.add(new JLabel(item.getName()));
            for (String badge : getBadges(item)) {
                b.add(Box.createHorizontalStrut(5));
                b.add(new BadgeLabel(badge));
            }
            b.add(Box.createHorizontalGlue());
            queueListPanel.add(b);
        });
        queueListPanel.revalidate(); // Updates layout
        queueListPanel.repaint();    // Redraws panel
    }

    private void createPlaylistList() {

        JPanel outerPanel = new JPanel();
        outerPanel.setLayout(new BoxLayout(outerPanel, BoxLayout.Y_AXIS));

        playlistsFilterTextField = new HintTextField("Filter playlists by title/description");
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
                            playlist.getPlaylist().getDescription().toLowerCase().contains(filterText.toLowerCase().trim())) {
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

        outerPanel.setPreferredSize(new Dimension(250, 0));

        outerPanel.add(scrollPane);

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
        selectAllCheckbox.setText(selectedCount + " von " + playlistsFiltered.size() + " ausgewählt");
        if (selectedCount == 0) {
            selectAllCheckbox.setSelected(false);
            selectAllCheckbox.setHalfSelected(false);
        } else if (selectedCount != playlistsFiltered.size()) {
            selectAllCheckbox.setSelected(false);
            selectAllCheckbox.setHalfSelected(true);
        } else {
            selectAllCheckbox.setSelected(true);
            selectAllCheckbox.setHalfSelected(false);
            selectAllCheckbox.setText("Alle ausgewählt");
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
                nowPlayingPanel.add(new JLabel("--- No song playing ---"));
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

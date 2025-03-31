package org.example;

import org.example.api.Api;
import org.example.api.SpotifyWindowTitle;
import org.example.gui.AlignHelper;
import org.example.gui.BadgeLabel;
import org.example.gui.HintTextField;
import org.example.gui.TristateCheckBox;
import org.example.models.PlaylistModel;
import se.michaelthelin.spotify.SpotifyApi;
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

    JLabel label;
    JFrame frame;
//    JPanel panel;

    String filterText;

    TristateCheckBox selectAllCheckbox;
    HintTextField playlistsFilterTextField;
    JPanel playlistsListPanel;

    JPanel queueListPanel;

    JPanel nowPlayingPanel;

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

        label = new JLabel("Hello World");

//        panel = new JPanel();
//        panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
//        //panel.setLayout(new GridLayout(0, 1));
//
//        panel.add(label, BorderLayout.CENTER);
//
        createPlaylistList();
        createQueueList();
        initNowPlaying();

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

        frame.add(label, BorderLayout.CENTER);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setTitle("Dance Music Shuffler");
        frame.setMinimumSize(new Dimension(600, 300));
        frame.setSize(new Dimension(900, 600));
        frame.setLocationByPlatform(true);
        //frame.pack();
        frame.setVisible(true);
        frame.requestFocusInWindow();
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

    private void recreateQueueList(java.util.List<IPlaylistItem> queue) {
        queueListPanel.removeAll();
        queue.forEach(item -> {
            JLabel label = new JLabel(item.getName());
            queueListPanel.add(label);
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

    private void initNowPlaying() {
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
            StringBuilder label = new StringBuilder("Now playing: ");
            if (response.getCurrentlyPlaying() != null) {
                label.append(response.getCurrentlyPlaying().getName());

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
                                secondaryMonitorGui.update(image1, track.getName(), Arrays.stream(track.getArtists()).map(ArtistSimplified::getName).collect(Collectors.joining(", ")), null);
                            } catch (Exception exp) {
                                exp.printStackTrace();
                            }

                        }));
                    }
                }

            } else {
                label.append("---");
            }

            JLabel l = new JLabel(label.toString());
            l.setFont(l.getFont().deriveFont(Font.BOLD));

            nowPlayingPanel.add(l);
            nowPlayingPanel.add(Box.createHorizontalStrut(5));
            nowPlayingPanel.add(new BadgeLabel("badge"));
            nowPlayingPanel.add(Box.createHorizontalGlue());
            nowPlayingPanel.revalidate();
            nowPlayingPanel.revalidate();
            nowPlayingPanel.repaint();
        });
    }

    // Layout types:
    //  BorderLayout (for overall Gui)
    //  BoxLayout (like Android linear layout)
    //  GroupLayout (mal sehen ob benötigt, kann zum Gruppieren von Elementen verwendet werden)
}

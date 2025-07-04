package org.example;

import com.google.gson.JsonArray;
import org.apache.hc.core5.http.ParseException;
import org.example.api.Api;
import org.example.gui.*;
import org.example.models.DeviceDisplayable;
import org.example.models.UsedTrack;
import org.example.util.ImageUtils;
import org.example.util.PopupMenuOpenedListener;
import org.example.worker.PersistentPreferences;
import org.example.worker.PlaylistLoader;
import org.example.api.SpotifyWindowTitle;
import org.example.models.PlaylistModel;
import org.example.worker.ShuffleAlgorithm;
import org.example.worker.SpotifyOcrProcessor;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.IPlaylistItem;
import se.michaelthelin.spotify.model_objects.miscellaneous.Device;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;
import se.michaelthelin.spotify.model_objects.specification.Track;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;

public class MainGui {

    public static final int SPOTIFY_WEB_API_DELAY = 500;

    final SecondaryMonitorGui secondaryMonitorGui = new SecondaryMonitorGui();

    final OcrOverlayWindow ocrOverlayWindow = new OcrOverlayWindow();
    final SpotifyOcrProcessor spotifyOcrProcessor = new SpotifyOcrProcessor(ocrOverlayWindow);

    ArrayList<PlaylistModel> playlists = new ArrayList<>();
    ArrayList<PlaylistModel> playlistsFiltered = new ArrayList<>();
    ShuffleAlgorithm shuffleAlgorithm;

    String activeDeviceId = null;
    boolean comboboxListenersEnabled = true;

    JFrame frame;

    String filterText;

    TristateCheckBox selectAllCheckbox;
    HintTextField playlistsFilterTextField;
    JPanel playlistsListPanel;

    JPanel queueListPanel;

    JPanel nowPlayingPanel;

    JSpinner songNumberSpinner;
    JSpinner cooldownSpinner;
    JCheckBox secondaryGuiShowSideSheetCheckbox;
    JCheckBox secondaryGuiColoredBackgroundCheckbox;
    JButton loadAndShuffleButton;

    JPanel[] sidePanels = new JPanel[2];
    JPanel leftHelperPanel = new JPanel();
    JButton drawerButton = new JButton("\u2630");  // ☰
    Drawer drawer;

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

        createPlaylistList();
        createQueueList();
        createCenterOptionsPanel();
        createNowPlaying();

        new Timer(1000, evt -> {
            if (SpotifyWindowTitle.titleChanged()) {
                Timer timer = new Timer(SPOTIFY_WEB_API_DELAY, e -> updateNowPlaying());
                timer.setRepeats(false);
                timer.start();
            }
        }).start();


        frame.addWindowFocusListener(new WindowFocusListener() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                updateNowPlaying();
                updatePlaybackState();
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
                // not interested
            }
        });

        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {

                int width = frame.getWidth() < 1000 ? (frame.getWidth() - 450) : ((frame.getWidth() - 400) / 2);

                for (JPanel panel : sidePanels) {
                    panel.setPreferredSize(new Dimension(width, 0));
                    panel.revalidate();
                }

                    leftHelperPanel.removeAll();


                    if (frame.getWidth() < 1000) {
                        // Small screen: show toggle button
                        leftHelperPanel.add(drawerButton, BorderLayout.CENTER);
                        drawer.setContent(sidePanels[0]);
                        drawer.build();
                    } else {
                        drawer.setContent(null);
                        leftHelperPanel.add(sidePanels[0], BorderLayout.CENTER);
                    }

                    leftHelperPanel.revalidate();
                    leftHelperPanel.repaint();
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
                BorderFactory.createTitledBorder("Load/Store Configuration"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        labeledPanelConfig.setMaximumSize(new Dimension(Integer.MAX_VALUE, 0));
        centerPanel.add(labeledPanelConfig);

        JButton loadButton = new JButton("\u2191   Load configuration");
        loadButton.addActionListener(e -> {
            PersistentPreferences.MainGuiParams params = PersistentPreferences.load(playlists);
            if (params != null) {
                songNumberSpinner.setValue(params.count);
                cooldownSpinner.setValue(params.cooldown);
                playlistsFilterTextField.setText(params.searchString);
                secondaryGuiShowSideSheetCheckbox.setSelected(params.showSidePanel);
                secondaryMonitorGui.setSidePanelVisible(params.showSidePanel);
                secondaryGuiColoredBackgroundCheckbox.setSelected(params.colorBackground);
                secondaryMonitorGui.setColoredBackground(params.colorBackground);
                recreatePlaylistsList();
            }
        });
        labeledPanelConfig.add(loadButton);

        JButton storeButton = new JButton("\u2193   Store configuration");
        storeButton.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(frame, "This will overwrite any existing configuration. Do you want to continue?", "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (result != JOptionPane.YES_OPTION) {
                return;
            }
            PersistentPreferences.store(playlists, (int) songNumberSpinner.getValue(), (int) cooldownSpinner.getValue(), filterText, secondaryGuiShowSideSheetCheckbox.isSelected(), secondaryGuiColoredBackgroundCheckbox.isSelected());
        });
        labeledPanelConfig.add(storeButton);

        centerPanel.add(Box.createVerticalStrut(10));

        JPanel labeledPanelMonitor = new JPanel(new GridLayout(0, 1));
        labeledPanelMonitor.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Secondary Monitor"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        labeledPanelMonitor.setMaximumSize(new Dimension(Integer.MAX_VALUE, 0));
        centerPanel.add(labeledPanelMonitor);

        secondaryGuiShowSideSheetCheckbox = new JCheckBox("Show side sheet");
        secondaryGuiShowSideSheetCheckbox.setSelected(true);
        secondaryGuiShowSideSheetCheckbox.addActionListener(e -> secondaryMonitorGui.setSidePanelVisible(secondaryGuiShowSideSheetCheckbox.isSelected()));
        labeledPanelMonitor.add(secondaryGuiShowSideSheetCheckbox);

        secondaryGuiColoredBackgroundCheckbox = new JCheckBox("Use colored background");
        secondaryGuiColoredBackgroundCheckbox.setSelected(true);
        secondaryGuiColoredBackgroundCheckbox.addActionListener(e -> secondaryMonitorGui.setColoredBackground(secondaryGuiColoredBackgroundCheckbox.isSelected()));
        labeledPanelMonitor.add(secondaryGuiColoredBackgroundCheckbox);

        JButton launchGuiButton = new JButton("Open secondary monitor GUI");
        launchGuiButton.addActionListener(e -> {
            if (!secondaryMonitorGui.launchSecondaryMonitorGui(false)) {
                int result = JOptionPane.showConfirmDialog(frame, "Secondary monitor not detected. Open Anyway?", "No second monitor", JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    secondaryMonitorGui.launchSecondaryMonitorGui(true);
                }
            }
        });
        labeledPanelMonitor.add(launchGuiButton);

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
                            shuffleAlgorithm.shuffleAsync((int) songNumberSpinner.getValue(), (int) cooldownSpinner.getValue(), activeDeviceId).thenAccept(result -> {
                                Timer timer = new Timer(SPOTIFY_WEB_API_DELAY, e1 -> restoreLoadAndShuffleButton());
                                timer.setRepeats(false);
                                timer.start();
                            });
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

        centerPanel.add(Box.createVerticalStrut(10));

        JPanel labeledPanelExperimental = new JPanel(new GridLayout(0, 1));
        labeledPanelExperimental.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Experimental Features"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        labeledPanelExperimental.setMaximumSize(new Dimension(Integer.MAX_VALUE, 0));
        centerPanel.add(labeledPanelExperimental);

        JCheckBox ocrOverlayCheckbox = new JCheckBox("Enable OCR Overlay");
        ocrOverlayCheckbox.setSelected(false);
        ocrOverlayCheckbox.addActionListener(e -> {
            if (ocrOverlayCheckbox.isSelected()) {
                spotifyOcrProcessor.start();
            } else {
                spotifyOcrProcessor.stop();
            }
        });
        labeledPanelExperimental.add(ocrOverlayCheckbox);

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
        java.util.List<java.util.List<String>> nextPlaylists = new ArrayList<>();

        queue.forEach(item -> {
            Box b = Box.createHorizontalBox();
            b.add(Box.createRigidArea(new Dimension(5, lineHeight)));
            JLabel label = new JLabel(item.getName());
            if (shuffleAlgorithm.wasNotShuffled(item)) {
                label.setForeground(Color.GRAY);
            }
            b.add(label);
            ArrayList<String> badges = getBadges(item);
            if (!badges.isEmpty()) {
                nextPlaylists.add(badges);
            }
            for (String badge : badges) {
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
        frame.add(leftHelperPanel, BorderLayout.LINE_START);
        leftHelperPanel.setLayout(new BorderLayout());
        leftHelperPanel.add(outerPanel, BorderLayout.CENTER);

        drawer = new Drawer(frame);
        drawerButton.addActionListener(e -> drawer.toggle());
        drawerButton.setFocusable(false);
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
        nowPlayingPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        nowPlayingPanel.setBackground(Color.lightGray);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());

        JPanel buttonPanel = createPlaybackControlButtons();
        bottomPanel.add(buttonPanel, BorderLayout.NORTH);
        bottomPanel.add(nowPlayingPanel, BorderLayout.SOUTH);

        frame.add(bottomPanel, BorderLayout.PAGE_END);
        updateNowPlaying();
    }

    private JPanel createPlaybackControlButtons() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setBackground(Color.lightGray);

        JButton backButton = new JButton("\u23EE");   // ⏮
        backButton.setOpaque(false);
        backButton.addActionListener(e -> Api.INSTANCE.skipUsersPlaybackToPreviousTrack().build().executeAsync().thenAccept(
                response -> secondaryMonitorGui.setPaused(false)
        ));
        buttonPanel.add(backButton);

        JButton playButton = new JButton("\u25B6");   // ▶
        playButton.setOpaque(false);
        buttonPanel.add(playButton);
        playButton.addActionListener(e -> Api.INSTANCE.startResumeUsersPlayback().device_id(activeDeviceId).build().executeAsync().whenComplete(
                (res, ex) -> {
                    Timer timer = new Timer(SPOTIFY_WEB_API_DELAY, e1 -> updatePlaybackState());
                    timer.setRepeats(false);
                    timer.start();

                    if (ex != null) {
                        System.err.println("startResumeUsersPlayback: Caught Exception: " + ex.getMessage());
                    }
                }
        ));

        JButton pauseButton = new JButton("\u23F8");  // ⏸
        pauseButton.setOpaque(false);
        buttonPanel.add(pauseButton);
        pauseButton.addActionListener(e -> Api.INSTANCE.pauseUsersPlayback().build().executeAsync().whenComplete(
                (res, ex) -> secondaryMonitorGui.setPaused(true)
        ));

        JButton forwardButton = new JButton("\u23ED"); // ⏭
        forwardButton.setOpaque(false);
        forwardButton.addActionListener(e -> Api.INSTANCE.skipUsersPlaybackToNextTrack().build().executeAsync().thenAccept(
                response -> secondaryMonitorGui.setPaused(false)
        ));
        buttonPanel.add(forwardButton);

        JComboBox<DeviceDisplayable> devicesComboBox = new JComboBox<>();
        devicesComboBox.setOpaque(false);

        devicesComboBox.setPrototypeDisplayValue(new DeviceDisplayable(new Device.Builder().setName("A device name").build()));

        devicesComboBox.addPopupMenuListener(new PopupMenuOpenedListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                updateDevicesComboBox(devicesComboBox);
            }
        });

        devicesComboBox.addActionListener(e -> {
            if (!comboboxListenersEnabled) {
                return; // Prevents triggering action listeners while updating
            }

            DeviceDisplayable selected = (DeviceDisplayable) devicesComboBox.getSelectedItem();
            if (selected != null) {
                System.out.println("Selected device: " + selected.device().getName());
                activeDeviceId = selected.device().getId();
                JsonArray array = new JsonArray();
                array.add(selected.device().getId());
                Api.INSTANCE.transferUsersPlayback(array).build().executeAsync().whenComplete(
                        (res, ex) -> {
                            if (ex != null) {
                                System.err.println("Caught Exception: " + ex.getMessage());
                            } else {
                                System.out.println("Transferred playback to device: " + selected.device().getName());
                            }
                        }
                );
            }
        });

        updateDevicesComboBox(devicesComboBox);

        for (int i = 0; i < devicesComboBox.getItemCount(); i++) {
            DeviceDisplayable device = devicesComboBox.getItemAt(i);
            if (device.device().getType().equals("Computer")) {
                devicesComboBox.setSelectedItem(device);
                if (device.device().getIs_active()) {
                    break;
                }
            }
        }

        buttonPanel.add(devicesComboBox);
        return buttonPanel;
    }

    private void updateDevicesComboBox(JComboBox<DeviceDisplayable> devicesComboBox) {
        java.util.List<DeviceDisplayable> devices = new ArrayList<>(fetchDevices());

        comboboxListenersEnabled = false; // Prevents triggering action listeners while updating
        devicesComboBox.removeAllItems();
        devices.sort((d1, d2) -> { // "Computers" first, then other devices
            if (d1.device().getType().equals("Computer") && !d2.device().getType().equals("Computer")) {
                return -1;
            } else if (!d1.device().getType().equals("Computer") && d2.device().getType().equals("Computer")) {
                return 1;
            }
            return 0;
        });
        for (DeviceDisplayable device : devices) {
            devicesComboBox.addItem(device);
            if (device.device().getIs_active()) {
                devicesComboBox.setSelectedItem(device);
            }
        }
        comboboxListenersEnabled = true; // Re-enable listeners
    }

    private static java.util.List<DeviceDisplayable> fetchDevices() {
        try {
            Device[] devices = Api.INSTANCE.getUsersAvailableDevices().build().execute();
            return Stream.of(devices).map(DeviceDisplayable::new).toList();
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private void updatePlaybackState() {
        Api.INSTANCE.getUsersCurrentlyPlayingTrack().build().executeAsync().thenAccept(currentlyPlaying -> {
            if (currentlyPlaying != null) {
                secondaryMonitorGui.setPaused(!currentlyPlaying.getIs_playing());
                secondaryMonitorGui.setProgress(currentlyPlaying.getProgress_ms(), currentlyPlaying.getItem().getDurationMs());
            }
        }).whenComplete((res, ex) -> {
            if (ex != null) {
                System.err.println("Caught Exception: " + ex.getMessage());
            }
        });
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
                                BufferedImage rawImage = ImageIO.read(url.toURL());

                                BufferedImage coverImage = ImageUtils.makeRoundedCorner(rawImage, 50);
                                BufferedImage backgroundImage = ImageUtils.dimImage(ImageUtils.blurWithEdgeExtension(rawImage, 150), 0.5f);

                                secondaryMonitorGui.update(coverImage, backgroundImage, track, badges);
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

        UsedTrack usedTrack = shuffleAlgorithm.getUsedTrackIfExists(track);
        if (usedTrack != null) {
            badges.add(usedTrack.from().getPlaylist().getName());
        }

        for (PlaylistModel playlist : playlists) {
            if (!playlist.isChecked() || playlist.getTracks() == null) {
                continue;
            }
            if (usedTrack != null && usedTrack.from().getPlaylist().getId().equals(playlist.getPlaylist().getId())) {
                continue;
            }
            if (playlist.getTracks().stream().anyMatch(playlistTrack -> track.getId().equals(playlistTrack.getTrack().getId()))) {
                badges.add(playlist.getPlaylist().getName());
            }
        }
        return badges;
    }
}

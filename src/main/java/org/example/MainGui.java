package org.example;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.components.FlatTextField;
import com.formdev.flatlaf.extras.components.FlatTriStateCheckBox;
import com.formdev.flatlaf.icons.FlatSearchIcon;
import com.formdev.flatlaf.ui.FlatEmptyBorder;
import org.example.api.LocalSpotifyProvider;
import org.example.gui.*;
import org.example.models.DeviceDisplayable;
import org.example.models.PlaylistModel;
import org.example.models.TrackWithBadges;
import org.example.util.Scheduler;
import org.example.worker.PersistentPreferences;
import org.example.worker.PlaylistLoader;
import org.example.worker.SpotifyOcrIntegration;
import se.michaelthelin.spotify.model_objects.interfaces.IArtist;
import se.michaelthelin.spotify.model_objects.miscellaneous.Device;
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;
import se.michaelthelin.spotify.model_objects.specification.Track;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

@SuppressWarnings("UnnecessaryUnicodeEscape")
public class MainGui implements QueueView, NowPlayingView {


    final OcrOverlayWindow ocrOverlayWindow = new OcrOverlayWindow();
    final SpotifyOcrIntegration spotifyOcrProcessor = SpotifyOcrIntegration.create(ocrOverlayWindow);

    final MainController controller = new MainController(this);

    JFrame frame;

    FlatTriStateCheckBox selectAllCheckbox;
    FlatTextField playlistsFilterTextField;
    JPanel playlistsListPanel;

    JPanel queueListPanel;

    JPanel nowPlayingPanel;

    JSpinner songNumberSpinner;
    JSpinner cooldownSpinner;
    JCheckBox groupPlaylistsCheckbox;
    JCheckBox secondaryGuiShowSideSheetCheckbox;
    JCheckBox secondaryGuiCoverCheckbox;
    JCheckBox secondaryGuiColoredBackgroundCheckbox;
    JButton loadAndShuffleButton;

    JPanel[] sidePanels = new JPanel[2];
    JPanel leftHelperPanel = new JPanel();
    JButton drawerButton = new JButton("\u2630");  // ☰
    Drawer drawer;

    JComboBox<DeviceDisplayable> devicesComboBox;

    private final ActionListener deviceChangeListener = (ActionEvent e) -> {
        DeviceDisplayable selected = (DeviceDisplayable) devicesComboBox.getSelectedItem();
        if (selected != null) {
            controller.transferPlaybackToDevice(selected.device());
        }
    };

    public MainGui(Collection<PlaylistSimplified> lists) {
        controller.playlistStore.addPlaylists(lists.stream().map(PlaylistModel::new).toList());

        frame = new JFrame();
        frame.setLayout(new BorderLayout());

        createPlaylistList();
        createQueueList();
        createCenterOptionsPanel();
        createNowPlaying();

        new Timer(1000, evt -> {
            if (LocalSpotifyProvider.INSTANCE.titleHasChanged() || (controller.secondaryMonitorGui.getProgress() == 1.0 && !controller.secondaryMonitorGui.isPaused())) {
                if (LocalSpotifyProvider.INSTANCE.isInitialized()) {
                    controller.secondaryMonitorGui.setPaused(LocalSpotifyProvider.INSTANCE.isPaused());
                }
                Scheduler.waitForWebApiDelayAndRun(controller::refreshPlayerState);
            }
        }).start();


        frame.addWindowFocusListener(new WindowFocusListener() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                controller.refreshPlayerState();
                controller.updatePlaybackState();
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
        frame.setMinimumSize(new Dimension(600, 675));
        frame.setSize(new Dimension(1000, 675));
        frame.setLocationByPlatform(true);
        frame.setVisible(true);
        loadAndShuffleButton.requestFocusInWindow();
        SwingUtilities.invokeLater(frame::toFront);

    }

    private void createCenterOptionsPanel() {
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
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
        labeledPanelOptions.add(AlignHelper.pad(AlignHelper.left(songNumberSpinner), new Insets(4, 0, 4, 0)));

        labeledPanelOptions.add(Box.createVerticalStrut(10));

        JLabel cooldownLabel = new JLabel("Number of songs a playlist should not be reused:");
        labeledPanelOptions.add(AlignHelper.left(cooldownLabel));

        cooldownSpinner = new JSpinner(new SpinnerNumberModel(3, 0, Integer.MAX_VALUE, 1));
        cooldownSpinner.setMaximumSize(new Dimension(cooldownSpinner.getPreferredSize().width, cooldownSpinner.getPreferredSize().height));
        labeledPanelOptions.add(AlignHelper.pad(AlignHelper.left(cooldownSpinner), new Insets(4, 0, 4, 0)));

        groupPlaylistsCheckbox = new JCheckBox("Group playlists based on names");
        groupPlaylistsCheckbox.setSelected(false);
        labeledPanelOptions.add(groupPlaylistsCheckbox);
        labeledPanelOptions.add(AlignHelper.left(groupPlaylistsCheckbox));

        labeledPanelOptions.add(Box.createVerticalStrut(10));

        JLabel exclusiveLabel = new JLabel("Playlists that may not be played directly after each other:");
        labeledPanelOptions.add(AlignHelper.left(exclusiveLabel));

        JButton exclusiveButton = new JButton("Select exclusive playlists");
        exclusiveButton.addActionListener(e -> showExclusivePoolDialog());
        labeledPanelOptions.add(AlignHelper.pad(AlignHelper.left(exclusiveButton), new Insets(4, 0, 4, 0)));

        labeledPanelOptions.add(Box.createVerticalStrut(10));

        JLabel weightsLabel = new JLabel("Configure how often a playlists is chosen:");
        labeledPanelOptions.add(AlignHelper.pad(AlignHelper.left(weightsLabel), new Insets(4, 0, 4, 0)));

        JButton weightsButton = new JButton("Adjust weights");
        weightsButton.addActionListener(e -> showWeightsDialog());
        labeledPanelOptions.add(AlignHelper.left(weightsButton));

        centerPanel.add(Box.createVerticalStrut(10));

        JPanel labeledPanelConfig = new JPanel(new GridLayout(0, 2, 10, 0));
        labeledPanelConfig.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Load/Store Configuration"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        labeledPanelConfig.setMaximumSize(new Dimension(Integer.MAX_VALUE, 0));
        centerPanel.add(labeledPanelConfig);

        JButton loadButton = new JButton("\u2191   Load configuration");
        loadButton.addActionListener(e -> {
            loadButton.setText("Loading...");
            loadButton.setEnabled(false);
            PersistentPreferences.loadAsync(controller.playlistStore, "prefs.json").thenAccept(prefs -> SwingUtilities.invokeLater(() -> applyPreferences(loadButton, prefs)));
        });
        labeledPanelConfig.add(loadButton);

        JButton storeButton = new JButton("\u2193   Store configuration");
        storeButton.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(frame, "This will overwrite any existing configuration. Do you want to continue?", "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (result != JOptionPane.YES_OPTION) {
                return;
            }
            PersistentPreferences.store(controller.playlistStore, (int) songNumberSpinner.getValue(), (int) cooldownSpinner.getValue(), groupPlaylistsCheckbox.isSelected(), playlistsFilterTextField.getText(), secondaryGuiShowSideSheetCheckbox.isSelected(), secondaryGuiCoverCheckbox.isSelected(), secondaryGuiColoredBackgroundCheckbox.isSelected());
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
        secondaryGuiShowSideSheetCheckbox.addActionListener(e -> controller.secondaryMonitorGui.setSidePanelVisible(secondaryGuiShowSideSheetCheckbox.isSelected()));
        labeledPanelMonitor.add(secondaryGuiShowSideSheetCheckbox);

        secondaryGuiCoverCheckbox = new JCheckBox("Show cover");
        secondaryGuiCoverCheckbox.setSelected(true);
        secondaryGuiCoverCheckbox.addActionListener(e -> controller.secondaryMonitorGui.setCoverVisible(secondaryGuiCoverCheckbox.isSelected()));
        labeledPanelMonitor.add(secondaryGuiCoverCheckbox);

        secondaryGuiColoredBackgroundCheckbox = new JCheckBox("Use colored background");
        secondaryGuiColoredBackgroundCheckbox.setSelected(true);
        secondaryGuiColoredBackgroundCheckbox.addActionListener(e -> controller.secondaryMonitorGui.setColoredBackground(secondaryGuiColoredBackgroundCheckbox.isSelected()));
        labeledPanelMonitor.add(secondaryGuiColoredBackgroundCheckbox);

        JButton launchGuiButton = new JButton("Open secondary monitor GUI");
        launchGuiButton.addActionListener(e -> {
            if (!controller.secondaryMonitorGui.launchSecondaryMonitorGui(false)) {
                int result = JOptionPane.showConfirmDialog(frame, "Secondary monitor not detected. Open Anyway?", "No second monitor", JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    controller.secondaryMonitorGui.launchSecondaryMonitorGui(true);
                }
            }
        });
        labeledPanelMonitor.add(launchGuiButton);

        centerPanel.add(Box.createVerticalStrut(10));

        loadAndShuffleButton = new JButton("Load Playlists and Shuffle");
        loadAndShuffleButton.addActionListener(e -> {
            if (controller.playlistStore.getSelectedPlaylists().isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Please select at least one playlist to shuffle.");
                return;
            }

            loadAndShuffleButton.setEnabled(false);
            loadAndShuffleButton.setText("Loading...");

            PlaylistLoader.loadPlaylistsAsync(controller.playlistStore.getSelectedPlaylists())
                    .thenAccept(success -> {
                        if (success) {
                            controller.shuffleAlgorithm.shuffleAsync((int) songNumberSpinner.getValue(), (int) cooldownSpinner.getValue(), groupPlaylistsCheckbox.isSelected(), controller.activeDeviceId).thenAccept(result ->
                                    Scheduler.waitForWebApiDelayAndRun(this::restoreLoadAndShuffleButton));
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
        frame.getRootPane().setDefaultButton(loadAndShuffleButton);
        centerPanel.add(expandedButtonPanel);

        centerPanel.add(Box.createVerticalGlue());
    }

    private void applyPreferences(JButton loadPrefsButton, PersistentPreferences.MainGuiParams params) {
        if (params != null) {
            songNumberSpinner.setValue(params.count);
            cooldownSpinner.setValue(params.cooldown);
            groupPlaylistsCheckbox.setSelected(params.groupPlaylists);
            playlistsFilterTextField.setText(params.searchString);
            secondaryGuiShowSideSheetCheckbox.setSelected(params.showSidePanel);
            controller.secondaryMonitorGui.setSidePanelVisible(params.showSidePanel);
            secondaryGuiCoverCheckbox.setSelected(params.showSidePanel);
            controller.secondaryMonitorGui.setCoverVisible(params.showCover);
            secondaryGuiColoredBackgroundCheckbox.setSelected(params.colorBackground);
            controller.secondaryMonitorGui.setColoredBackground(params.colorBackground);
            recreatePlaylistsList();
        } else {
            int result = JOptionPane.showConfirmDialog(frame, "No user configuration stored, load default prefs?", "Load defaults", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (result == JOptionPane.YES_OPTION) {
                PersistentPreferences.loadAsync(controller.playlistStore, "default-prefs.json").thenAccept(prefs -> SwingUtilities.invokeLater(() -> applyPreferences(loadPrefsButton, prefs)));
                return;
            }
        }
        loadPrefsButton.setText("\u2191   Load configuration");
        loadPrefsButton.setEnabled(true);
    }

    private void restoreLoadAndShuffleButton() {
        loadAndShuffleButton.setEnabled(true);
        loadAndShuffleButton.setText("Load Playlists and Shuffle");
        controller.refreshPlayerState();
    }

    private void showExclusivePoolDialog() {
        if (controller.playlistStore.getSelectedPlaylists().isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please select playlists for the general pool first.");
            return;
        }

        JDialog dialog = new JDialog(frame, "Select exclusive playlists", true);
        dialog.setLayout(new BoxLayout(dialog.getContentPane(), BoxLayout.Y_AXIS));
        dialog.setSize(300, 200);
        dialog.setLocationRelativeTo(frame);

        JPanel checkboxesPanel = new JPanel();
        checkboxesPanel.setLayout(new BoxLayout(checkboxesPanel, BoxLayout.Y_AXIS));

        controller.playlistStore.getSelectedPlaylists().forEach(playlist -> {
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
        if (controller.playlistStore.getSelectedPlaylists().isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please select playlists for the general pool first.");
            return;
        }

        JDialog dialog = new JDialog(frame, "Adjust weights", true);
        dialog.setLayout(new BoxLayout(dialog.getContentPane(), BoxLayout.Y_AXIS));
        dialog.setSize(300, 200);
        dialog.setLocationRelativeTo(frame);

        JPanel weightsPanel = new JPanel();
        weightsPanel.setLayout(new BoxLayout(weightsPanel, BoxLayout.Y_AXIS));

        controller.playlistStore.getSelectedPlaylists().forEach(playlist -> {
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
        outerPanel.setBackground(Color.black);
        outerPanel.putClientProperty( FlatClientProperties.STYLE, "arc: 32" );
        outerPanel.setLayout(new BoxLayout(outerPanel, BoxLayout.Y_AXIS));
        outerPanel.add(Box.createVerticalStrut(5));

        JLabel queueLabel = new JLabel("Queue");
        queueLabel.setFont(queueLabel.getFont().deriveFont(Font.BOLD));
        outerPanel.add(AlignHelper.center(queueLabel));

        if (spotifyOcrProcessor.isSupported()) {
            JCheckBox ocrOverlayCheckbox = new JCheckBox("Enable Spotify In-App Overlay (experimental)");
            ocrOverlayCheckbox.setSelected(false);
            ocrOverlayCheckbox.addActionListener(e -> {
                if (ocrOverlayCheckbox.isSelected()) {
                    spotifyOcrProcessor.start();
                } else {
                    spotifyOcrProcessor.stop();
                }
            });
            outerPanel.add(AlignHelper.center(ocrOverlayCheckbox));
        }

        outerPanel.add(Box.createVerticalStrut(5));

        queueListPanel = new JPanel();
        queueListPanel.setLayout(new BoxLayout(queueListPanel, BoxLayout.Y_AXIS));
        queueListPanel.setBackground(Color.black);

        JScrollPane scrollPane = new JScrollPane(queueListPanel);
        scrollPane.setBorder(new FlatEmptyBorder());
        scrollPane.getVerticalScrollBar().setBackground(Color.black);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        outerPanel.add(AlignHelper.pad(scrollPane, new Insets(8, 8, 8, 0)));

        sidePanels[1] = outerPanel;
        frame.add(AlignHelper.pad(outerPanel, new Insets(0,8,8,8)), BorderLayout.LINE_END);

    }

    private int calculateBadgeHeight() {
        return new BadgeLabel("Dummy").getPreferredSize().height;
    }


    @Override
    public void showQueue(java.util.List<TrackWithBadges> queue) {
        queueListPanel.removeAll();
        int lineHeight = calculateBadgeHeight();

        queue.forEach(item -> {
            Box b = Box.createHorizontalBox();
            b.add(Box.createRigidArea(new Dimension(5, lineHeight)));
            JLabel label = new JLabel(item.track().getName());
            if (!item.fromShuffleAlgorithm()) {
                label.setForeground(Color.GRAY);
            }
            b.add(label);
            for (String badge : item.badges()) {
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
        outerPanel.setBackground(Color.black);
        outerPanel.putClientProperty( FlatClientProperties.STYLE, "arc: 32" );

        playlistsFilterTextField = new FlatTextField();
        playlistsFilterTextField.setPlaceholderText("Filter playlists by title, description or owner...");
        playlistsFilterTextField.setLeadingIcon(new FlatSearchIcon());
        playlistsFilterTextField.setMaximumSize(new Dimension(Integer.MAX_VALUE, playlistsFilterTextField.getPreferredSize().height));
        playlistsFilterTextField.addCaretListener(e -> {
            if (!controller.playlistStore.setFilterText(playlistsFilterTextField.getText())) {
                return; //Text hasn't changed
            }
            recreatePlaylistsList();
        });
        outerPanel.add(AlignHelper.pad(playlistsFilterTextField, new Insets(4, 4, 4, 4)));

        selectAllCheckbox = new FlatTriStateCheckBox();
        selectAllCheckbox.setAllowIndeterminate(false);
        selectAllCheckbox.addActionListener(e -> {
            boolean isSelected = selectAllCheckbox.isSelected();
            controller.playlistStore.getFilteredPlaylists().forEach(playlist -> playlist.setChecked(isSelected));
            recreatePlaylistsList();
        });

        outerPanel.add(AlignHelper.pad(AlignHelper.left(selectAllCheckbox), new Insets(0, 8, 8, 8)));

        playlistsListPanel = new JPanel();
        playlistsListPanel.setLayout(new BoxLayout(playlistsListPanel, BoxLayout.Y_AXIS));
        playlistsListPanel.setBackground(Color.black);

        recreatePlaylistsList();

        JScrollPane scrollPane = new JScrollPane(playlistsListPanel);
        scrollPane.setBorder(new FlatEmptyBorder());
        scrollPane.getVerticalScrollBar().setBackground(Color.black);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        outerPanel.add(AlignHelper.pad(scrollPane, new Insets(8, 8, 8, 0)));

        sidePanels[0] = outerPanel;
        frame.add(AlignHelper.pad(leftHelperPanel, new Insets(0,8,8,8)), BorderLayout.LINE_START);
        leftHelperPanel.setLayout(new BorderLayout());
        leftHelperPanel.add(outerPanel, BorderLayout.CENTER);

        drawer = new Drawer(frame);
        drawerButton.addActionListener(e -> drawer.toggle());
        drawerButton.setFocusable(false);
    }

    private void recreatePlaylistsList() {
        playlistsListPanel.removeAll();

        controller.playlistStore.getFilteredPlaylists().forEach(playlist -> {
            JCheckBox checkBox = new JCheckBox(playlist.getPlaylist().getName() + " (" + playlist.getPlaylist().getItems().getTotal() + " Lieder)" + (playlist.isFromConfig() ? " [playlist from config]" : ""));
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
        var playlistsFiltered = controller.playlistStore.getFilteredPlaylists();
        int selectedCount = (int) playlistsFiltered.stream().filter(PlaylistModel::isChecked).count();
        selectAllCheckbox.setText(selectedCount + " von " + playlistsFiltered.size() + " ausgew\u00e4hlt");

        if (selectedCount == 0) {
            selectAllCheckbox.setState(FlatTriStateCheckBox.State.UNSELECTED);
        } else if (selectedCount != playlistsFiltered.size()) {
            selectAllCheckbox.setState(FlatTriStateCheckBox.State.INDETERMINATE);
        } else {
            selectAllCheckbox.setState(FlatTriStateCheckBox.State.SELECTED);
            selectAllCheckbox.setText("Alle ausgew\u00e4hlt");
        }
    }

    private void createNowPlaying() {
        nowPlayingPanel = new JPanel();
        nowPlayingPanel.setLayout(new BoxLayout(nowPlayingPanel, BoxLayout.X_AXIS));
        nowPlayingPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        nowPlayingPanel.setBackground(Color.black);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());

        JPanel buttonPanel = createPlaybackControlButtons();
        bottomPanel.add(buttonPanel, BorderLayout.NORTH);
        bottomPanel.add(nowPlayingPanel, BorderLayout.SOUTH);

        frame.add(bottomPanel, BorderLayout.PAGE_END);
        controller.refreshPlayerState();
    }

    private JPanel createPlaybackControlButtons() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setBackground(Color.black);

        JButton restartButton = new JButton("\u275A\u25C0");
        restartButton.setOpaque(false);
        restartButton.setToolTipText("Restart the current song");
        restartButton.addActionListener(e -> controller.restartCurrentClicked());
        buttonPanel.add(restartButton);

        JButton playButton = new JButton("\u25B6\u275A\u275A");
        playButton.setOpaque(false);
        playButton.setToolTipText("Toggle play/pause");
        buttonPanel.add(playButton);
        playButton.addActionListener(e -> controller.playPauseClicked());

        JButton forwardButton = new JButton("\u25B6\u25B6\u275A");
        forwardButton.setOpaque(false);
        forwardButton.setToolTipText("Skip to the next song");
        forwardButton.addActionListener(e -> controller.skipToNextClicked());
        buttonPanel.add(forwardButton);

        devicesComboBox = new JComboBox<>();
        devicesComboBox.setOpaque(false);
        devicesComboBox.setToolTipText("Choose playback device");
        devicesComboBox.setPrototypeDisplayValue(new DeviceDisplayable(new Device.Builder().setName("A device name").build()));
        devicesComboBox.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                controller.refreshAvailablePlaybackDevices();
            }
        });
        controller.refreshAvailablePlaybackDevices();
        buttonPanel.add(devicesComboBox);

        return buttonPanel;
    }

    @Override
    public void updateDevicesComboBox(java.util.List<DeviceDisplayable> devices) {
        devicesComboBox.removeActionListener(deviceChangeListener);

        devicesComboBox.removeAllItems();
        for (DeviceDisplayable device : devices) {
            devicesComboBox.addItem(device);
            if (device.device().getIs_active()) {
                devicesComboBox.setSelectedItem(device);
            }
        }

        devicesComboBox.addActionListener(deviceChangeListener);
        devicesComboBox.setSelectedItem(devicesComboBox.getSelectedItem());

        if (devicesComboBox.isPopupVisible()) {
            // trigger re-rendering of popup
            devicesComboBox.hidePopup();
            devicesComboBox.showPopup();
        }
    }

    @Override
    public void showNowPlaying(Track track, java.util.List<String> badges) {
        nowPlayingPanel.removeAll();
        nowPlayingPanel.add(Box.createHorizontalGlue());
        nowPlayingPanel.add(Box.createRigidArea(new Dimension(0, calculateBadgeHeight())));
        nowPlayingPanel.add(new JLabel("Now playing: "));

        if (track != null) {
            String label = track.getName() +
                    " by " +
                    Arrays.stream(track.getArtists()).map(IArtist::getName).collect(Collectors.joining(", "));

            JLabel l = new JLabel(label);
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
    }

}
